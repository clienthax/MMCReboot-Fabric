package net.moddedminecraft.mmcreboot.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;

import java.util.Timer;
import java.util.TimerTask;

public class RebootNow {

    private final Main plugin;
    public RebootNow(Main instance) {
        plugin = instance;
    }

    Timer nowTimer;

    public int execute(ServerCommandSource src) {
        plugin.rebootConfirm = true;
        plugin.sendMessage(src, Messages.getRestartConfirmMessage());

        nowTimer = new Timer();
        nowTimer.schedule(new TimerTask() {
            public void run() {
                plugin.rebootConfirm = false;
                plugin.sendMessage(src, Messages.getErrorTookTooLong());
            }
        }, (60 * 1000));
        return 1;
    }
}
