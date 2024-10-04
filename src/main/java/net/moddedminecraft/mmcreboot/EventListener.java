package net.moddedminecraft.mmcreboot;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EventListener {

    private final Main plugin;
    public EventListener(Main instance) {
        plugin = instance;
    }

    public void onPlayerLogin(ServerPlayNetworkHandler event, PacketSender player) {
        if (plugin.voteStarted) {
            plugin.getTaskManager().scheduleSingleTask(() -> {
                List<Text> contents = new ArrayList<>();
                Config config = plugin.getConfig();
                if  (config.timer.timerUseVoteScoreboard) {
                    plugin.displayVotes();
                }
                for (String line : Messages.getRestartVoteBroadcastOnLogin()) {
                    String checkLine = line.replace("%config.timerminplayers%", String.valueOf(config.timer.timerMinplayers));
                    contents.add(Main.fromPlaceholderAPI(checkLine));
                }

                if (!contents.isEmpty()) {
                    event.getPlayer().sendMessage(Main.fromPlaceholderAPI("Restart"));
                    for (Text content : contents) {
                        event.getPlayer().sendMessage(content);
                    }
                }
            }, 3, TimeUnit.SECONDS);
        }

    }

}
