package net.moddedminecraft.mmcreboot.Tasks;

import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Main;

import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ShutdownTask extends TimerTask {

    private final Main plugin;

    public ShutdownTask(Main instance) {
        plugin = instance;
    }

    @Override
    public void run() {

        Runnable runnable = () -> {
            plugin.cancelRebootTimerTasks();
            plugin.removeScoreboard();
            plugin.removeBossBar();
            plugin.useCommandOnRestart();
        };

        Config config = plugin.getConfig();
        if (plugin.getTPSRestarting()) {
            if (plugin.getTPS() >= config.tps.tpsMinimum && config.tps.tpsRestartCancel) {
                plugin.cancelRebootTimerTasks();
                plugin.removeScoreboard();
                plugin.removeBossBar();
                plugin.isRestarting = false;
                plugin.setTPSRestarting(false);
                if (!config.tps.tpsRestartCancelMsg.isEmpty()) {
                    plugin.broadcastMessage("<white>[</white><gold>Restart</gold><white>] </white>" + config.tps.tpsRestartCancelMsg);
                }
            } else if (plugin.getTPS() < config.tps.tpsMinimum) {
                if (config.restart.restartUseCommand) {
                    plugin.getTaskManager().scheduleSingleTask(runnable, 1, TimeUnit.MILLISECONDS);
                } else {
                    plugin.getTaskManager().scheduleSingleTask(plugin::stopServer, 1, TimeUnit.MILLISECONDS);
                }
            }
        } else {
            if (config.restart.restartUseCommand) {
                plugin.getTaskManager().scheduleSingleTask(runnable, 1, TimeUnit.MILLISECONDS);
            } else {
                plugin.getTaskManager().scheduleSingleTask(plugin::stopServer, 1, TimeUnit.MILLISECONDS);
            }
        }
    }
}
