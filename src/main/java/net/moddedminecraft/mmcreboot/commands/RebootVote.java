package net.moddedminecraft.mmcreboot.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Config.RebootPermisssions;
import net.moddedminecraft.mmcreboot.Main;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class RebootVote {

    private final Main plugin;
    public RebootVote(Main instance) {
        plugin = instance;
    }

    public int executeBase(CommandContext<ServerCommandSource> ctx) {

        // Default vote command

        double timeLeft = 0;
        Config config = plugin.getConfig();
        ServerCommandSource src = ctx.getSource();

        if (config.autorestart.restartInterval > 0) {
            timeLeft = (config.autorestart.restartInterval * 3600) - ((double) (System.currentTimeMillis() - plugin.startTimestamp) / 1000);
        } else if (plugin.nextRealTimeRestart > 0){
            timeLeft = (plugin.nextRealTimeRestart) - ((double) (System.currentTimeMillis() - plugin.startTimestamp) / 1000);
        }
        int hours = (int) (timeLeft / 3600);
        int minutes = (int) ((timeLeft - hours * 3600) / 60);

        if (!Permissions.check(src, RebootPermisssions.BYPASS, 4) && !Permissions.check(src, RebootPermisssions.COMMAND_VOTE, 4)) {
            throw new CommandException(Main.fromPlaceholderAPI(Messages.getErrorNoPermission()));
        }
        if (!Permissions.check(src, RebootPermisssions.BYPASS, 4) && !config.voting.voteEnabled) {
            throw new CommandException(Main.fromPlaceholderAPI(Messages.getErrorVoteToRestartDisabled()));
        }
        if (src.getEntity() instanceof PlayerEntity && plugin.hasVoted.contains(src.getEntity().getUuid())) {
            throw new CommandException(Main.fromPlaceholderAPI(Messages.getErrorAlreadyVoted()));
        }
        if (plugin.voteStarted) {
            throw new CommandException(Main.fromPlaceholderAPI(Messages.getErrorVoteAlreadyRunning()));
        }
        if (!Permissions.check(src, RebootPermisssions.BYPASS) && plugin.justStarted) {
            throw new CommandException(Main.fromPlaceholderAPI(Messages.getErrorNotOnlineLongEnough()));
        }
        if (!Permissions.check(src, RebootPermisssions.BYPASS) && plugin.getOnlinePlayerCount() < config.timer.timerMinplayers) {
            throw new CommandException(Main.fromPlaceholderAPI(Messages.getErrorMinPlayers()));
        }
        if (plugin.isRestarting && timeLeft != 0 && (hours == 0 && minutes <= 10)) {
            throw new CommandException(Main.fromPlaceholderAPI(Messages.getErrorAlreadyRestarting()));
        }
        if (plugin.cdTimer) {
            throw new CommandException(Main.fromPlaceholderAPI(Messages.getErrorWaitTime()));
        }

        if (src.getEntity() instanceof PlayerEntity player) {
            plugin.voteStarted = true;
            plugin.voteCancel = false;
            plugin.hasVoted.add(player.getUuid());
            plugin.yesVotes += 1;
            plugin.noVotes = 0;
            plugin.voteSeconds = 90;
            plugin.displayVotes();
        } else {
            plugin.voteStarted = true;
            plugin.displayVotes();
        }

        List<Text> contents = new ArrayList<>();
        List<String> broadcast = Messages.getRestartVoteBroadcast();
        if (broadcast != null) {
            for (String line : broadcast) {
                String checkLine = line.replace("%playername$", src.getName()).replace("%config.timerminplayers%", String.valueOf(config.timer.timerMinplayers));
                contents.add(Main.fromPlaceholderAPI(checkLine));
            }
        }

        if (!contents.isEmpty()) {
            for (Text line : contents) {
                plugin.broadcastMessage(line);
            }
        }

        plugin.getTaskManager().scheduleSingleTask(() -> {
                int Online = plugin.getOnlinePlayerCount();
                int percentage = plugin.yesVotes/Online *100;

                boolean yesAboveNo = plugin.yesVotes > plugin.noVotes;
                boolean yesAboveMin = plugin.yesVotes >= config.timer.timerMinplayers;
                boolean requiredPercent = percentage >= config.timer.timerVotepercent;

                if (yesAboveNo && yesAboveMin && !plugin.voteCancel && requiredPercent) {
                    plugin.isRestarting = true;
                    config.autorestart.restartInterval = (config.timer.timerVotepassed + 1) / 3600.0;
                    Main.logger.info("[MMCReboot] scheduling restart tasks...");
                    plugin.reason = Messages.getRestartPassed();
                    plugin.scheduleTasks();
                } else {
                    if (!plugin.voteCancel) {
                        plugin.broadcastMessage("<white>[</white><gold>Restart</gold><white>] </white>" + Messages.getRestartVoteNotEnoughVoted());
                    }
                    plugin.voteCancel = false;
                    plugin.getTaskManager().scheduleSingleTask(() -> plugin.cdTimer = false, 60, TimeUnit.SECONDS);
                }
                plugin.removeScoreboard();
                plugin.removeBossBar();
                plugin.yesVotes = 0;
                plugin.cdTimer = true;
                plugin.voteStarted = false;
                plugin.hasVoted.clear();
        }, 90, TimeUnit.SECONDS);
        return 1;
    }

    public int executeSubcommand(CommandContext<ServerCommandSource> ctx) {

        Config config = plugin.getConfig();
        ServerCommandSource src = ctx.getSource();

        String op = StringArgumentType.getString(ctx, "op");

            switch (op) {
                case "on":
                    if (Permissions.check(src, RebootPermisssions.TOGGLE_VOTE, 4)) {
                        config.voting.voteEnabled = true;
                        return 1;
                    } else {
                        return -1;
                    }

                case "off":
                    if (Permissions.check(src, RebootPermisssions.TOGGLE_VOTE, 4)) {
                        config.voting.voteEnabled = false;
                        return 1;
                    } else {
                        return -1;
                    }

                case "yes":
                    if (src.getEntity() instanceof PlayerEntity && plugin.hasVoted.contains(src.getEntity().getUuid())) {
                        src.sendError(Main.fromPlaceholderAPI(Messages.getErrorAlreadyVoted()));
                        return -1;
                    }
                    if (plugin.voteStarted) {
                        plugin.yesVotes += 1;
                        if (src.getEntity() instanceof PlayerEntity) {
                            plugin.hasVoted.add(src.getEntity().getUuid());
                        }
                        plugin.displayVotes();
                        plugin.sendMessage(src, Messages.getVotedYes());
                        return 1;
                    } else {
                        src.sendError(Main.fromPlaceholderAPI(Messages.getErrorNoVoteRunning()));
                        return -1;
                    }

                case "no":
                    if (src.getEntity() instanceof PlayerEntity && plugin.hasVoted.contains(src.getEntity().getUuid())) {
                        src.sendError(Main.fromPlaceholderAPI(Messages.getErrorAlreadyVoted()));
                        return -1;
                    }
                    if (plugin.voteStarted) {
                        plugin.noVotes += 1;
                        if (src.getEntity() instanceof PlayerEntity) {
                            plugin.hasVoted.add(src.getEntity().getUuid());
                        }
                        plugin.displayVotes();
                        plugin.sendMessage(src, Messages.getVotedNo());
                        return 1;

                    } else {
                        src.sendError(Main.fromPlaceholderAPI(Messages.getErrorNoVoteRunning()));
                        return -1;
                    }

                default:
                    return -1;
                    //break;

            }




    }
}
