package net.moddedminecraft.mmcreboot.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;

import java.util.Timer;
import java.util.TimerTask;

public class RebootCancel {

    private final Main plugin;
    public RebootCancel(Main instance) {
        plugin = instance;
    }

    public int execute(ServerCommandSource source) {
        plugin.voteCancel = true;
        Timer voteCancelimer = new Timer();
        voteCancelimer.schedule(new TimerTask() {
            public void run() {
                plugin.voteCancel = false;
            }
        }, (long) (15 * 60000.0));
        plugin.cancelTasks();
        plugin.removeScoreboard();
        plugin.removeBossBar();
        plugin.isRestarting = false;
        plugin.sendMessage(source, Messages.getRestartCancel());
        return 1;
    }
}
