package net.moddedminecraft.mmcreboot;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;

import java.io.IOException;
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
            // TODO
            /*
            Sponge.getScheduler().createTaskBuilder().execute(new Runnable() {

                public void run() {
                    PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
                    List<Text> contents = new ArrayList<>();
                    if  (Config.timerUseVoteScoreboard) {
                        plugin.displayVotes();
                    }
                    for (String line : Messages.getRestartVoteBroadcastOnLogin()) {
                        String checkLine = line.replace("%config.timerminplayers%", String.valueOf(Config.timerMinplayers));
                        contents.add(Main.fromLegacy(checkLine));
                    }

                    if (!contents.isEmpty()) {
                        paginationService.builder()
                                .title(Main.fromLegacy("Restart"))
                                .contents(contents)
                                .padding(Text.of("="))
                                .sendTo(player);
                    }
                }
            }).delay(3, TimeUnit.SECONDS).name("mmcreboot-s-sendVoteOnLogin").submit(plugin);
        */}
    }

}
