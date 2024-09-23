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
            plugin.cancelTasks();
            plugin.removeScoreboard();
            plugin.removeBossBar();
            plugin.useCommandOnRestart();
        };

        Config config = plugin.getConfig();
        if (plugin.getTPSRestarting()) {
            if (plugin.getTPS() >= config.tps.tpsMinimum && config.tps.tpsRestartCancel) {
                plugin.cancelTasks();
                plugin.removeScoreboard();
                plugin.removeBossBar();
                plugin.isRestarting = false;
                plugin.setTPSRestarting(false);
                if (!config.tps.tpsRestartCancelMsg.isEmpty()) {
                    plugin.broadcastMessage("&f[&6Restart&f] " + config.tps.tpsRestartCancelMsg);
                }
            } else if (plugin.getTPS() < config.tps.tpsMinimum) {
                if (config.restart.restartUseCommand) {
                    MainThreadTaskScheduler.scheduleTask(runnable, 1, TimeUnit.MILLISECONDS);
                } else {
                    MainThreadTaskScheduler.scheduleTask(plugin::stopServer, 1, TimeUnit.MILLISECONDS);
                }
            }
        } else {
            if (config.restart.restartUseCommand) {
                MainThreadTaskScheduler.scheduleTask(runnable, 1, TimeUnit.MILLISECONDS);
            } else {
                MainThreadTaskScheduler.scheduleTask(plugin::stopServer, 1, TimeUnit.MILLISECONDS);
            }
        }
    }
}
