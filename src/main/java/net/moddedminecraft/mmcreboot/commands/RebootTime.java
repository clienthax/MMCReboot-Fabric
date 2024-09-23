package net.moddedminecraft.mmcreboot.commands;

import net.minecraft.command.CommandException;
import net.minecraft.server.command.ServerCommandSource;
import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;

public class RebootTime {

    private final Main plugin;
    public RebootTime(Main instance) {
        plugin = instance;
    }

    public int execute(ServerCommandSource src) {
        if(!plugin.tasksScheduled) {
            throw new CommandException(Main.fromLegacy(Messages.getErrorNoTaskScheduled()));
        }

        Config config = plugin.getConfig();
        if(config.autorestart.restartType.equalsIgnoreCase("fixed") || (config.autorestart.restartInterval > 0 && plugin.isRestarting && (plugin.nextRealTimeRestart > config.autorestart.restartInterval || plugin.nextRealTimeRestart == 0))) {
            double timeLeft = (config.autorestart.restartInterval * 3600) - ((double)(System.currentTimeMillis() - plugin.startTimestamp) / 1000);
            int hours = (int)(timeLeft / 3600);
            int minutes = (int)((timeLeft - hours * 3600) / 60);
            int seconds = (int)timeLeft % 60;

            plugin.sendMessage(src, Messages.getRestartMessageWithoutReason()
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%seconds%", String.valueOf(seconds)));
            return 1;
        } else if(config.autorestart.restartType.equalsIgnoreCase("realtime")) {
            double timeLeft = plugin.nextRealTimeRestart - ((double)(System.currentTimeMillis() - plugin.startTimestamp) / 1000);
            int hours = (int)(timeLeft / 3600);
            int minutes = (int)((timeLeft - hours * 3600) / 60);
            int seconds = (int)timeLeft % 60;

            plugin.sendMessage(src, Messages.getRestartMessageWithoutReason()
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%seconds%", String.valueOf(seconds)));
            return 1;
        } else {
            throw new CommandException(plugin.fromLegacy(Messages.getErrorNoTaskScheduled()));
        }
    }
}
