package net.moddedminecraft.mmcreboot.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;

public class RebootNow {

    private final Main plugin;
    public RebootNow(Main instance) {
        plugin = instance;
    }

    public int execute(ServerCommandSource src) {
        plugin.rebootConfirm = true;
        plugin.sendMessage(src, Messages.getRestartConfirmMessage());
        plugin.startRebootConfirmTimer(src);
        return 1;
    }

}
