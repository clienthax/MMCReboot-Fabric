package net.moddedminecraft.mmcreboot.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;

public class RebootCMD {

    private final Main plugin;

    public RebootCMD(Main instance) {
        plugin = instance;
    }

    public int execute(CommandContext<ServerCommandSource> ctx) {

        String timeValue = StringArgumentType.getString(ctx, "unit");
        int timeAmount = IntegerArgumentType.getInteger(ctx, "time");

        // Optional reason
        String reasonOP = "";
        try {
            reasonOP = StringArgumentType.getString(ctx, "reason");
        } catch (IllegalArgumentException ignored) {
        }

        double restartTime;

        if (!reasonOP.isBlank()) {
            plugin.reason = reasonOP;
        } else {
            plugin.reason = null;
        }

        switch (timeValue) {
            case "h":
                restartTime = timeAmount * 3600;
                break;
            case "m":
                restartTime = (timeAmount * 60) + 1;
                break;
            case "s":
                restartTime = timeAmount;
                break;
            default:
                plugin.sendMessage(ctx.getSource(), Messages.getRestartFormatMessage());
                ctx.getSource().sendMessage(Text.of(""));
                throw new CommandException(Main.fromPlaceholderAPI(Messages.getErrorInvalidTimescale()));
        }

        Main.logger.info("[MMCReboot] " + ctx.getSource().getName() + " is setting a new restart time...");

        if(plugin.tasksScheduled) {
            plugin.cancelRebootTimerTasks();
        }

        Config config = plugin.getConfig();
        config.autorestart.restartInterval = restartTime / 3600.0;

        Main.logger.info("[MMCReboot] scheduling restart tasks...");
        plugin.removeScoreboard();
        plugin.removeBossBar();
        plugin.scheduleTasks();
        plugin.isRestarting = true;

        if (restartTime <= 300 && config.timer.timerUseScoreboard) {
            plugin.displayRestart(config.autorestart.restartInterval * 3600);
        }

        double timeLeft = (config.autorestart.restartInterval * 3600) - ((double)(System.currentTimeMillis() - plugin.startTimestamp) / 1000);
        int hours = (int)(timeLeft / 3600);
        int minutes = (int)((timeLeft - hours * 3600) / 60);
        int seconds = (int)timeLeft % 60;

        if (!reasonOP.isBlank()) {
            plugin.sendMessage(ctx.getSource(), Messages.getRestartMessageWithReason()
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%seconds%", String.valueOf(seconds)));
            plugin.sendMessage(ctx.getSource(), "<gold>" + plugin.reason+"</gold>");
        } else {
            plugin.sendMessage(ctx.getSource(), Messages.getRestartMessageWithoutReason()
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%seconds%", String.valueOf(seconds)));
        }

        return 1;
    }
}
