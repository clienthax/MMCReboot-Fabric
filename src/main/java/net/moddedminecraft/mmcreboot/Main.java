package net.moddedminecraft.mmcreboot;

import com.mojang.brigadier.CommandDispatcher;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.moddedminecraft.mmcreboot.Tasks.MainThreadTaskScheduler;
import org.slf4j.Logger;

import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Tasks.ShutdownTask;
import net.moddedminecraft.mmcreboot.commands.*;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Leelawd93
public class Main implements ModInitializer {

    public static final String MOD_ID = "mmcreboot";
    public static final Logger logger = LoggerFactory.getLogger(MOD_ID);

    private final ScheduledExecutorService asyncExecutorService = Executors.newScheduledThreadPool(10);

    public boolean voteCancel = false;
    public boolean cdTimer = false;
    public boolean voteStarted = false;
    public int yesVotes = 0;
    public int noVotes = 0;
    public ArrayList<UUID> hasVoted = new ArrayList<>();
    public static ArrayList<Integer> realTimeTimes = new ArrayList<>();

    public int voteSeconds;
    public String reason;

    public long startTimestamp;
    public boolean justStarted = true;
    public boolean isRestarting = false;
    public boolean TPSRestarting = false;
    public boolean rebootConfirm = false;
    public boolean tasksScheduled = false;
    public double nextRealTimeRestart;

    // Timers
    private final ArrayList<Timer> warningTimers = new ArrayList<Timer>();
    private Timer rebootTimer;
    private Timer justStartedTimer;

    private boolean playSoundNow = false;
    private int soundLocX;
    private int soundLocY;
    private int soundLocZ;

    private Config config;
    private Messages messages;

    private Scoreboard board;

    private ServerBossBar bar;

    private MinecraftServer server;


    @Override
    public void onInitialize() {
        // Tests
        onInitializeServer();
    }

    public void onInitializeServer() {

        //this.config = new Config(this);
        this.config = ConfigApiJava.registerAndLoadConfig(Config::new);

        this.messages = new Messages(this);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);

        EventListener eventListener = new EventListener(this);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            eventListener.onPlayerLogin(handler, sender);
        });

    }

    public void onServerStart(MinecraftServer server) {

        // Hacky?
        this.server = server;

        soundLocX = 0;
        soundLocY = 64;
        soundLocZ = 0;

        if(config.autorestart.restartType.equalsIgnoreCase("fixed")) {
            logger.info("[MMCReboot] Using fixed restart scheduler");
            scheduleTasks();
            if (!config.restart.defaultRestartReason.isEmpty()) {
                reason = config.restart.defaultRestartReason;
            }
        } else if(config.autorestart.restartType.equalsIgnoreCase("realtime")) {
            logger.info("[MMCReboot] Using realtime restart scheduler");
            scheduleRealTimeRestart();
            if (!config.restart.defaultRestartReason.isEmpty()) {
                reason = config.restart.defaultRestartReason;
            }
            config.autorestart.restartInterval = 0;
        } else {
            logger.info("[MMCReboot] No automatic restarts scheduled!");
        }

        if (config.voting.voteEnabled) {
            justStartedTimer = new Timer();
            this.justStartedTimer.schedule(new TimerTask() {
                public void run() {
                    justStarted = false;
                }
            }, (long) config.timer.timerStartvote * 60 * 1000);
        }

        // mmcreboot-a-sendAction
        asyncExecutorService.scheduleAtFixedRate(this::action, 250, 500, TimeUnit.MILLISECONDS);

        // mmcreboot-a-reduceVoteCount
        asyncExecutorService.scheduleAtFixedRate(this::reduceVote, 0, 1, TimeUnit.SECONDS);

        // mmcreboot-a-checkRealTimeRestart
        asyncExecutorService.scheduleAtFixedRate(this::checkRealTimeRestart, 60, 15, TimeUnit.MINUTES);

        // mmcreboot-a-checkTPSForRestart
        asyncExecutorService.scheduleAtFixedRate(this::CheckTPSForRestart, config.tps.tpsCheckDelay, 30, TimeUnit.SECONDS);

        MainThreadTaskScheduler.init();

        logger.info("MMCReboot Loaded");
    }

    public void onServerStop(MinecraftServer server) {
        cancelTasks();
        logger.info("MMCReboot Disabled");
    }

    // No reload for fabric plugins??
    // TODO hook to a command?
    public void onPluginReload() {
        cancelTasks();
        removeScoreboard();
        removeBossBar();
        isRestarting = false;

        this.config = ConfigApiJava.registerAndLoadConfig(Config::new);
        this.messages = new Messages(this);

        if(config.autorestart.restartType.equalsIgnoreCase("fixed")) {
            scheduleTasks();
            if (!config.restart.defaultRestartReason.isEmpty()) {
                reason = config.restart.defaultRestartReason;
            }
        } else if(config.autorestart.restartType.equalsIgnoreCase("realtime")) {
            scheduleRealTimeRestart();
            if (!config.restart.defaultRestartReason.isEmpty()) {
                reason = config.restart.defaultRestartReason;
            }
            config.autorestart.restartInterval = 0;
        } else {
            logger.info("[MMCReboot] No automatic restarts scheduled!");
        }

    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {

        // /Reboot help
        dispatcher.register(CommandManager.literal("reboot")
                .then(CommandManager.literal("help")
                        .executes(context -> {
                            new RebootHelp(this).execute(context.getSource());
                            return 1;
                        })
                )
        );

        /*
        // /Reboot vote
        CommandSpec vote = CommandSpec.builder()
                .description(Text.of("Submit a vote to reboot the server"))
                .executor(new RebootVote(this))
                .arguments(GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("optional"))))
                .build();

        // /Reboot cancel
        CommandSpec cancel = CommandSpec.builder()
                .description(Text.of("Cancel the current timed reboot"))
                .permission(RebootPermisssions.COMMAND_CANCEL)
                .executor(new RebootCancel(this))
                .build();

        // /Reboot time
        CommandSpec time = CommandSpec.builder()
                .description(Text.of("Get the time remaining until the next restart"))
                .permission(RebootPermisssions.COMMAND_TIME)
                .executor(new RebootTime(this))
                .build();

        // /Reboot confirm
        CommandSpec confirm = CommandSpec.builder()
                .description(Text.of("Reboot the server immediately"))
                .permission(RebootPermisssions.COMMAND_NOW)
                .executor(new RebootConfirm(this))
                .build();
        // /Reboot now
        CommandSpec now = CommandSpec.builder()
                .description(Text.of("Reboot the server immediately"))
                .permission(RebootPermisssions.COMMAND_NOW)
                .executor(new RebootNow(this))
                .build();

        Map<String, String> choices = new HashMap<>() {
            {
                put("h", "h");
                put("m", "m");
                put("s", "s");
            }
        };

        // /Reboot start h/m/s time reason
        CommandSpec start = CommandSpec.builder()
                .description(Text.of("Reboot base command"))
                .permission(RebootPermisssions.COMMAND_START)
                .arguments(GenericArguments.choices(Text.of("h/m/s"), choices),
                        GenericArguments.integer(Text.of("time")),
                        GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("reason"))))
                .executor(new RebootCMD(this))
                .build();

        CommandSpec rbootmain = CommandSpec.builder()
                .description(Text.of("Reboot base command"))
                .child(start, "start")
                .child(now, "now")
                .child(confirm, "confirm")
                .child(time, "time")
                .child(cancel, "cancel")
                .child(vote, "vote")
                .child(help, "help")
                .build();

        cmdManager.register(this, rbootmain, "reboot", "restart");*/
    }

    // TODO: How can we do this in fabric?
    public Double getTPS() {
        //return Sponge.getServer().getTicksPerSecond();
        return 20d;
    }

    public boolean getTPSRestarting() {
        return TPSRestarting;
    }

    public void setTPSRestarting(boolean bool) {
        TPSRestarting = bool;
    }

    public void CheckTPSForRestart() {
        if (getTPS() < config.tps.tpsMinimum && config.tps.tpsEnabled && !getTPSRestarting()) {
            double timeLeft = config.autorestart.restartInterval * 3600 - ((double) (System.currentTimeMillis() - startTimestamp) / 1000);
            int hours = (int) (timeLeft / 3600);
            int minutes = (int) ((timeLeft - hours * 3600) / 60);
            if (hours == 0 && minutes > 20 || hours > 0) {
                setTPSRestarting(true);
                Timer warnTimer = new Timer();
                warningTimers.add(warnTimer);
                warnTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (getTPS() < config.tps.tpsMinimum) {
                            isRestarting = true;
                            config.autorestart.restartInterval = (config.tps.tpsTimer + 1) / 3600.0;
                            logger.info("[MMCReboot] scheduling restart tasks...");
                            if (config.tps.tpsUseReason) {
                                reason = config.tps.tpsMessage;
                            }
                            scheduleTasks();
                        } else {
                            setTPSRestarting(false);
                        }
                    }
                }, 15000);
            }
        }
    }

    public void action() {
        if (isRestarting && config.timer.timerUseScoreboard) {
            if (config.autorestart.restartInterval > 0) {
                displayRestart(config.autorestart.restartInterval * 3600);
            } else if (nextRealTimeRestart > 0){
                displayRestart(nextRealTimeRestart);
            }
        }
        if (voteStarted && voteCancel && config.timer.timerUseVoteScoreboard) {
            displayVotes();
        }
    }

    public void reduceVote() {
        if (voteStarted && !voteCancel) {
            if (voteSeconds > 0) {
                voteSeconds -= 1;
            }
            if (voteSeconds < 0) {
                voteSeconds = 0;
            }
        }
    }

    public void checkRealTimeRestart() {
        if(config.autorestart.restartType.equalsIgnoreCase("realtime")) {
            if (nextRealTimeRestart == 0 && !isRestarting && !voteCancel) {
                scheduleRealTimeRestart();
            }
        }
    }

    public void scheduleRealTimeRestart() {
        cancelTasks();
        nextRealTimeRestart = getNextRealTimeFromConfig();
        double rInterval = nextRealTimeRestart;
        if (config.timer.timerBroadcast != null) {
            warningMessages(rInterval);
        }
        rebootTimer = new Timer();
        rebootTimer.schedule(new ShutdownTask(this), (long) (nextRealTimeRestart * 1000.0));

        logger.info("[MMCReboot] RebootCMD scheduled for " + (long)(nextRealTimeRestart) + " seconds from now!");
        tasksScheduled = true;
        startTimestamp = System.currentTimeMillis();
        isRestarting = true;
    }

    public void scheduleTasks() {
        boolean wasTPSRestarting = getTPSRestarting();
        cancelTasks();
        if (wasTPSRestarting) {
            setTPSRestarting(true);
        } else {
            setTPSRestarting(false);
        }
        double rInterval = config.autorestart.restartInterval * 3600;
        if (config.timer.timerBroadcast != null) {
            warningMessages(rInterval);
        }
        rebootTimer = new Timer();
        rebootTimer.schedule(new ShutdownTask(this), (long) (config.autorestart.restartInterval * 3600000.0));

        logger.info("[MMCReboot] RebootCMD scheduled for " + (long)(config.autorestart.restartInterval  * 3600.0) + " seconds from now!");
        tasksScheduled = true;
        startTimestamp = System.currentTimeMillis();
        isRestarting = true;
    }

    private void warningMessages(double rInterval) {
        config.timer.timerBroadcast.stream().filter(aTimerBroadcast -> rInterval * 60 - aTimerBroadcast > 0).forEach(aTimerBroadcast -> {
            Timer warnTimer = new Timer();
            warningTimers.add(warnTimer);
            if (aTimerBroadcast <= rInterval) {
                warnTimer.schedule(new TimerTask() {
                    public void run() {
                        double timeLeft = rInterval - ((double) (System.currentTimeMillis() - startTimestamp) / 1000);
                        int hours = (int) (timeLeft / 3600);
                        int minutes = (int) ((timeLeft - hours * 3600) / 60);
                        int seconds = (int) timeLeft % 60;

                        NumberFormat formatter = new DecimalFormat("00");
                        String s = formatter.format(seconds);
                        if (config.timer.timerUseChat) {
                            if (minutes > 1) {
                                String message = Messages.getRestartNotificationMinutes().replace("%minutes%", "" + minutes).replace("%seconds%", "" + s);
                                broadcastMessage("&f[&6Restart&f] " + message);
                            } else if (minutes == 1) {
                                String message = Messages.getRestartNotificationMinute().replace("%minutes%", "" + minutes).replace("%seconds%", "" + s);
                                broadcastMessage("&f[&6Restart&f] " + message);
                            } else {
                                String message = Messages.getRestartNotificationSeconds().replace("%minutes%", "" + minutes).replace("%seconds%", "" + s);
                                broadcastMessage("&f[&6Restart&f] " + message);
                            }
                        }
                        logger.info("[MMCReboot] " + "&bThe server will be restarting in &f" + hours + "h" + minutes + "m" + seconds + "s");
                        if (!playSoundNow && config.timer.playSoundFirstTime >= aTimerBroadcast) {
                            playSoundNow = true;
                        }

                        playGlobalSound();
                        updateTitles(hours, minutes, s);

                        if (reason != null) {
                            broadcastMessage("&f[&6Restart&f] &d" + reason);
                        }
                        isRestarting = true;
                    }
                }, (long) ((rInterval - aTimerBroadcast) * 1000.0));
                logger.info("[MMCReboot] warning scheduled for " + (long) (rInterval - aTimerBroadcast) + " seconds from now!");
            }
        });
    }

    private void updateTitles(int hours, int minutes, String s) {
        if (!config.timer.titleEnabled) {
            return;
        }

        Text title = fromLegacy(config.timer.titleMessage.replace("{hours}", "" + hours).replace("{minutes}", "" + minutes).replace("{seconds}", s));
        Text subtitle = Text.empty();
        if (reason != null) {
            subtitle = fromLegacy(reason);
        }

        TitleS2CPacket titlePacket = new TitleS2CPacket(title);
        SubtitleS2CPacket subtitlePacket = new SubtitleS2CPacket(subtitle);
        TitleFadeS2CPacket fadePacket = new TitleFadeS2CPacket(10, config.timer.titleStayTime * 20, 10);

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.networkHandler.sendPacket(fadePacket);
            p.networkHandler.sendPacket(titlePacket);
            p.networkHandler.sendPacket(subtitlePacket);
        }
    }

    public void playGlobalSound() {

        if (!config.timer.playSoundEnabled || !playSoundNow) return;

        Identifier sound = new Identifier(config.timer.playSoundString);
        SoundEvent soundEvent = SoundEvent.of(sound);

        for (World world : server.getWorlds()) {
                world.playSound(null, soundLocX, soundLocY, soundLocZ, soundEvent, SoundCategory.MASTER, 1, 1);
        }
    }

    public void cancelTasks() {
        for (Timer warningTimer : warningTimers) warningTimer.cancel();
        warningTimers.clear();
        if(rebootTimer != null) {
            rebootTimer.cancel();
        }
        rebootTimer = new Timer();
        tasksScheduled = false;
        isRestarting = false;
        TPSRestarting = false;
        nextRealTimeRestart = 0;
    }


    public void stopServer() {
        logger.info("[MMCReboot] Restarting...");
        isRestarting = false;
        broadcastMessage("&cServer is restarting, we'll be right back!");
        try {
            if (config.kickmessage.isEmpty()) {
                Text reason = Text.of("Server is restarting, we'll be right back!");
                server.getPlayerManager().getPlayerList().forEach(player -> player.networkHandler.disconnect(reason));
            } else {
                server.getPlayerManager().getPlayerList().forEach(player -> player.networkHandler.disconnect(fromLegacy(config.kickmessage)));
            }
            // mmcreboot-s-shutdown
            MainThreadTaskScheduler.scheduleTask(this::shutdown, 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.info("[MMCReboot] Something went wrong while saving & stopping!");
            logger.info("Exception: " + e);
            broadcastMessage("&cServer has encountered an error while restarting.");
        }
    }

    public void shutdown() {
        server.saveAll(false, true, true);
        server.shutdown();
    }

    public int getNextRealTimeFromConfig() {
        realTimeTimes = new ArrayList<>();
        for (String realTime : config.autorestart.realTimeInterval) {
            int time = getTimeUntil(realTime);
            realTimeTimes.add(time);
        }
        return Collections.min(realTimeTimes);
    }

    private int getTimeUntil(String time) {
        Calendar cal = Calendar.getInstance();
        int nowHour = cal.get(Calendar.HOUR_OF_DAY);
        int nowMin  = cal.get(Calendar.MINUTE);
        return getTimeTill(nowHour, nowMin, time);
    }

    private int getTimeTill(int nowHour, int nowMin, String endTime) {
        Matcher m = Pattern.compile("(\\d{2}):(\\d{2})").matcher(endTime);
        if (! m.matches()) {
            throw new IllegalArgumentException("Invalid time format: " + endTime);
        }
        int endHour = Integer.parseInt(m.group(1));
        int endMin  = Integer.parseInt(m.group(2));
        if (endHour >= 24 || endMin >= 60) {
            throw new IllegalArgumentException("Invalid time format: " + endTime);
        }
        int timeTill = (endHour * 60 + endMin - (nowHour * 60 + nowMin)) * 60;
        if (timeTill < 0) {
            timeTill += 24 * 60 * 60;
        }
        return timeTill;
    }

    public void useCommandOnRestart() {
        logger.info("[MMCReboot] Running Command");
        isRestarting = false;
        List<String> cmds = config.restart.restartCommands;
        for (String cmd : cmds) {
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), cmd);
        }
    }

    public int getTimeLeftInSeconds() {
        return voteSeconds;
    }

    public void displayRestart(double rInterval) {

        // Sidebar

        double timeLeft = rInterval - ((double)(System.currentTimeMillis() - startTimestamp) / 1000);
        int hours = (int)(timeLeft / 3600);
        int minutes = (int)((timeLeft - hours * 3600) / 60);
        int seconds = (int)timeLeft % 60;

        NumberFormat formatter = new DecimalFormat("00");
        String s = formatter.format(seconds);

        board = new Scoreboard();
        ScoreboardObjective obj = board.addObjective("restart", ScoreboardCriterion.DUMMY, Text.of(Messages.getSidebarRestartTimerTitle()), ScoreboardCriterion.RenderType.INTEGER);

        board.setObjectiveSlot(Scoreboard.SIDEBAR_DISPLAY_SLOT_ID, obj);
        board.getPlayerScore(Text.literal(minutes +":" + s).formatted(Formatting.GREEN).getString(), obj).setScore(0);

        // Boss bar
        int mSec = (minutes * 60);
        double val = ((double) ((mSec + seconds) * 100) / 300);
        float percent = (float)val / 100.0f;



        // TODO: Implement this
        /*
        Sponge.getServer().getOnlinePlayers().stream().filter(player -> minutes < 5 && hours == 0).forEach(player -> {
            player.setScoreboard(board);
            if (Config.bossbarEnabled) {
                if (bar == null) {
                    bar = ServerBossBar.builder()
                            .name(Text.of(Config.bossbarTitle.replace("{minutes}", Integer.toString(minutes)).replace("{seconds}", s)))
                            .color(BossBarColors.GREEN)
                            .overlay(BossBarOverlays.PROGRESS)
                            .percent(percent)
                            .build();
                } else {
                    bar.setPercent(percent);
                }
                bar.addPlayer(player);
            }
        });*/
    }


    public void displayVotes() {
        /*
        board = Scoreboard.builder().build();

        ScoreboardObjective obj = ScoreboardObjective.builder().name("vote").criterion(Criteria.DUMMY).displayName(Text.of(Messages.getSidebarTitle())).build();

        board.addObjective(obj);
        board.updateDisplaySlot(obj, DisplaySlots.SIDEBAR);

        obj.getOrCreateScore(Text.builder(Messages.getSidebarYes() + ":").color(TextColors.GREEN).build()).setScore(yesVotes);
        obj.getOrCreateScore(Text.builder(Messages.getSidebarNo() + ":").color(TextColors.AQUA).build()).setScore(noVotes);
        obj.getOrCreateScore(Text.builder(Messages.getSidebarTimeleft() + ":").color(TextColors.RED).build()).setScore(getTimeLeftInSeconds());


        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            player.setScoreboard(board);
        }

         */
        // TODO: Implement this
    }

    public  void removeScoreboard() {
        // TODO: Implement this

        //for (Player player : Sponge.getServer().getOnlinePlayers()) {
        //    player.getScoreboard().clearSlot(DisplaySlots.SIDEBAR);
        //}
    }

    public  void removeBossBar() {
        // TODO: Implement this
        //if (bar != null) {
        //    bar.removePlayers(bar.getPlayers());
        //}
    }

    public void broadcastMessage(String message) {
        server.sendMessage(fromLegacy(message));
    }

    public void broadcastMessage(Text message) {
        server.sendMessage(message);
    }

    public void sendMessage(ServerCommandSource sender, String message) {
        sender.sendMessage(fromLegacy(message));
    }

    public static Text fromLegacy(String legacy) {
        // TODO: Maybe?
        return Text.of(legacy);
    }

    public Config getConfig() {
        return config;
    }

    public int getOnlinePlayerCount() {
        return server.getPlayerManager().getPlayerList().size();
    }

}
