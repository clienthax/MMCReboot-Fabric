package net.moddedminecraft.mmcreboot.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;

public class RebootConfirm {

    private final Main plugin;
    public RebootConfirm(Main instance) {
        plugin = instance;
    }

    public int execute(ServerCommandSource src) {

        if (plugin.rebootConfirm) {
            plugin.sendMessage(src, Messages.getRestartConfirm());
            plugin.stopServer();
            return 1;
        }

        src.sendError(Main.fromPlaceholderAPI(Messages.getErrorNothingToConfirm()));
        return -1;
    }

}
