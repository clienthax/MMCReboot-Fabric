package net.moddedminecraft.mmcreboot.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;

import java.util.concurrent.TimeUnit;

public class RebootCancel {

    private final Main plugin;
    public RebootCancel(Main instance) {
        plugin = instance;
    }

    public int execute(ServerCommandSource source) {
        plugin.voteCancel = true;
        plugin.getTaskManager().scheduleSingleTask(() -> plugin.voteCancel = false, 15, TimeUnit.MINUTES);
        plugin.cancelRebootTimerTasks();
        plugin.removeScoreboard();
        plugin.removeBossBar();
        plugin.isRestarting = false;
        plugin.sendMessage(source, Messages.getRestartCancel());
        return 1;
    }
}
