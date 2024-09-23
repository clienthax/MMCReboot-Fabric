package net.moddedminecraft.mmcreboot.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Config.RebootPermisssions;
import net.moddedminecraft.mmcreboot.Main;

import java.util.ArrayList;
import java.util.List;

import me.lucko.fabric.api.permissions.v0.Permissions;

public class RebootHelp {

    private final Main plugin;
    public RebootHelp(Main instance) {
        plugin = instance;
    }

    public int execute(ServerCommandSource source) {
        List<Text> contents = new ArrayList<>();
        contents.add(Main.fromLegacy(Messages.getHelpHelp()));
        if (Permissions.check(source, RebootPermisssions.COMMAND_NOW)) contents.add(Main.fromLegacy(Messages.getHelpNow()));
        if (Permissions.check(source, RebootPermisssions.COMMAND_START)) contents.add(Main.fromLegacy(Messages.getHelpStart()));
        if (Permissions.check(source, RebootPermisssions.COMMAND_CANCEL)) contents.add(Main.fromLegacy(Messages.getHelpCancel()));
        if (Permissions.check(source, RebootPermisssions.COMMAND_VOTE)) contents.add(Main.fromLegacy(Messages.getHelpVote()));
        if (Permissions.check(source, RebootPermisssions.COMMAND_TIME)) contents.add(Main.fromLegacy(Messages.getHelpTime()));
        contents.add(Main.fromLegacy(Messages.getHelpVoteYea()));
        contents.add(Main.fromLegacy(Messages.getHelpVoteNo()));

        source.sendMessage(Main.fromLegacy("&6MMCReboot Help"));
        for (Text text : contents) {
            source.sendMessage(text);
        }

        return 1;
    }
}
