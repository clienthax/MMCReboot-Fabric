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
        contents.add(Main.fromPlaceholderAPI(Messages.getHelpHelp()));
        if (Permissions.check(source, RebootPermisssions.COMMAND_NOW, 4)) contents.add(Main.fromPlaceholderAPI(Messages.getHelpNow()));
        if (Permissions.check(source, RebootPermisssions.COMMAND_START, 4)) contents.add(Main.fromPlaceholderAPI(Messages.getHelpStart()));
        if (Permissions.check(source, RebootPermisssions.COMMAND_CANCEL, 4)) contents.add(Main.fromPlaceholderAPI(Messages.getHelpCancel()));
        if (Permissions.check(source, RebootPermisssions.COMMAND_VOTE, 4)) contents.add(Main.fromPlaceholderAPI(Messages.getHelpVote()));
        if (Permissions.check(source, RebootPermisssions.COMMAND_TIME, 4)) contents.add(Main.fromPlaceholderAPI(Messages.getHelpTime()));
        contents.add(Main.fromPlaceholderAPI(Messages.getHelpVoteYea()));
        contents.add(Main.fromPlaceholderAPI(Messages.getHelpVoteNo()));

        source.sendMessage(Main.fromPlaceholderAPI("<gold>MMCReboot Help</gold>"));
        for (Text text : contents) {
            source.sendMessage(text);
        }

        return 1;
    }
}
