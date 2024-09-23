package net.moddedminecraft.mmcreboot.commands;

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

public class RebootVote {

    private final Main plugin;
    public RebootVote(Main instance) {
        plugin = instance;
    }

    public int execute(ServerCommandSource src, CommandContext args) {

        Config config = plugin.getConfig();

        // TODO problem for later....
        /*
        Optional<String> optional = args.getOne("optional");

        if (optional.isPresent()) {
            String op = optional.get();
            switch (op) {
                case "on":
                    if (Permissions.check(src, RebootPermisssions.TOGGLE_VOTE)) {
                        config.voting.voteEnabled = true;
                        return 1;
                    } else {
                        return -1;
                    }

                case "off":
                    if (Permissions.check(src, RebootPermisssions.TOGGLE_VOTE)) {
                        config.voting.voteEnabled = false;
                        return 1;
                    } else {
                        return -1;
                    }

                case "yes":
                    if (plugin.hasVoted.contains(src)) {
                        throw new CommandException(Main.fromLegacy(Messages.getErrorAlreadyVoted()));
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
                        throw new CommandException(Main.fromLegacy(Messages.getErrorNoVoteRunning()));
                    }

                case "no":
                    if (plugin.hasVoted.contains(src)) {
                        throw new CommandException(Main.fromLegacy(Messages.getErrorAlreadyVoted()));
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
                        throw new CommandException(Main.fromLegacy(Messages.getErrorNoVoteRunning()));
                    }

                default:
                    return -1;
                    //break;

            }

        }*/

        // Default vote command

            double timeLeft = 0;
            if (config.autorestart.restartInterval > 0) {
                timeLeft = (config.autorestart.restartInterval * 3600) - ((double) (System.currentTimeMillis() - plugin.startTimestamp) / 1000);
            } else if (plugin.nextRealTimeRestart > 0){
                timeLeft = (plugin.nextRealTimeRestart) - ((double) (System.currentTimeMillis() - plugin.startTimestamp) / 1000);
            }
            int hours = (int) (timeLeft / 3600);
            int minutes = (int) ((timeLeft - hours * 3600) / 60);

            if (!Permissions.check(src, RebootPermisssions.BYPASS) && !Permissions.check(src, RebootPermisssions.COMMAND_VOTE)) {
                throw new CommandException(Main.fromLegacy(Messages.getErrorNoPermission()));
            }
            if (!Permissions.check(src, RebootPermisssions.BYPASS) && !config.voting.voteEnabled) {
                throw new CommandException(Main.fromLegacy(Messages.getErrorVoteToRestartDisabled()));
            }
            if (src.getEntity() instanceof PlayerEntity && plugin.hasVoted.contains(src.getEntity().getUuid())) {
                throw new CommandException(Main.fromLegacy(Messages.getErrorAlreadyVoted()));
            }
            if (plugin.voteStarted) {
                throw new CommandException(Main.fromLegacy(Messages.getErrorVoteAlreadyRunning()));
            }
            if (!Permissions.check(src, RebootPermisssions.BYPASS) && plugin.justStarted) {
                throw new CommandException(Main.fromLegacy(Messages.getErrorNotOnlineLongEnough()));
            }
            if (!Permissions.check(src, RebootPermisssions.BYPASS) && plugin.getOnlinePlayerCount() < config.timer.timerMinplayers) {
                throw new CommandException(Main.fromLegacy(Messages.getErrorMinPlayers()));
            }
            if (plugin.isRestarting && timeLeft != 0 && (hours == 0 && minutes <= 10)) {
                throw new CommandException(Main.fromLegacy(Messages.getErrorAlreadyRestarting()));
            }
            if (plugin.cdTimer) {
                throw new CommandException(Main.fromLegacy(Messages.getErrorWaitTime()));
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

            //PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            List<Text> contents = new ArrayList<>();
            List<String> broadcast = Messages.getRestartVoteBroadcast();
            if (broadcast != null) {
                for (String line : broadcast) {
                    String checkLine = line.replace("%playername$", src.getName()).replace("%config.timerminplayers%", String.valueOf(config.timer.timerMinplayers));
                    contents.add(Main.fromLegacy(checkLine));
                }
            }

            if (!contents.isEmpty()) {
                /*
                paginationService.builder()
                        .title(plugin.fromLegacy("Restart"))
                        .contents(contents)
                        .padding(Text.of("="))
                        .sendTo(MessageChannel.TO_ALL.getMembers());
                */
                for (Text line : contents) {
                    plugin.broadcastMessage(line);
                }
            }

            Timer voteTimer = new Timer();
            voteTimer.schedule(new TimerTask() {
                public void run() {
                    int Online = plugin.getOnlinePlayerCount();
                    int percentage = plugin.yesVotes/Online *100;

                    Config config = plugin.getConfig();
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
                            plugin.broadcastMessage("&f[&6Restart&f] " + Messages.getRestartVoteNotEnoughVoted());
                        }
                        plugin.voteCancel = false;
                        Timer voteTimer = new Timer();
                        voteTimer.schedule(new TimerTask() {
                            public void run() {
                                plugin.cdTimer = false;
                            }
                        }, (long) (config.timer.timerRevote * 60000.0));
                    }
                    plugin.removeScoreboard();
                    plugin.removeBossBar();
                    plugin.yesVotes = 0;
                    plugin.cdTimer = true;
                    plugin.voteStarted = false;
                    plugin.hasVoted.clear();
                }
            }, 90000);
            return 1;

    }
}
