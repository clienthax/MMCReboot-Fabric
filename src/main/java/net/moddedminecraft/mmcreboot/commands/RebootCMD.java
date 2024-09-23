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

    public int execute(ServerCommandSource src, CommandContext args) {

        String timeValue = StringArgumentType.getString(args, "h/m/s");
        int timeAmount = IntegerArgumentType.getInteger(args, "time");
        String reasonOP = StringArgumentType.getString(args, "reason");
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
                plugin.sendMessage(src, Messages.getRestartFormatMessage());
                src.sendMessage(Text.of(""));
                throw new CommandException(Main.fromLegacy(Messages.getErrorInvalidTimescale()));
        }

        Main.logger.info("[MMCReboot] " + src.getName() + " is setting a new restart time...");

        if(plugin.tasksScheduled) {
            plugin.cancelTasks();
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
            plugin.sendMessage(src, Messages.getRestartMessageWithReason()
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%seconds%", String.valueOf(seconds)));
            plugin.sendMessage(src, "&6" + plugin.reason);
        } else {
            plugin.sendMessage(src, Messages.getRestartMessageWithoutReason()
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%seconds%", String.valueOf(seconds)));
        }

        return 1;
    }
}
