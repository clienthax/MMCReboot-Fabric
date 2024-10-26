package net.moddedminecraft.mmcreboot;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import eu.pb4.placeholders.api.TextParserUtils;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.boss.BossBar;
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
import net.moddedminecraft.mmcreboot.Config.RebootPermisssions;
import net.moddedminecraft.mmcreboot.Tasks.ScheduledTaskManager;
import net.moddedminecraft.mmcreboot.Tasks.ServerTickMixinAccessor;
import org.slf4j.Logger;

import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Tasks.ShutdownTask;
import net.moddedminecraft.mmcreboot.commands.*;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Leelawd93
public class Main implements ModInitializer {

    public static final String MOD_ID = "mmcreboot";
    public static final Logger logger = LoggerFactory.getLogger(MOD_ID);

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

    private boolean playSoundNow = false;
    private int soundLocX;
    private int soundLocY;
    private int soundLocZ;

    private Config config;
    private Messages messages;

    private Scoreboard board;

    private ServerBossBar bar;

    private MinecraftServer server;

    private List<Runnable> rebootTimerTasks = new ArrayList<>();

    @Override
    public void onInitialize() {

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
            getTaskManager().scheduleSingleTask(() -> justStarted = false, config.timer.timerStartvote, TimeUnit.MINUTES);
        }

        // mmcreboot-a-sendAction
        getTaskManager().scheduleRepeatingTask(this::action, 250, 500, TimeUnit.MILLISECONDS);

        // mmcreboot-a-reduceVoteCount
        getTaskManager().scheduleRepeatingTask(this::reduceVote, 0, 1, TimeUnit.SECONDS);

        // mmcreboot-a-checkRealTimeRestart
        getTaskManager().scheduleRepeatingTask(this::checkRealTimeRestart, 60, 15, TimeUnit.MINUTES);

        // mmcreboot-a-checkTPSForRestart
        getTaskManager().scheduleRepeatingTask(this::CheckTPSForRestart, config.tps.tpsCheckDelay, 30, TimeUnit.SECONDS);

        logger.info("MMCReboot Loaded");
    }

    public void onServerStop(MinecraftServer server) {
        cancelRebootTimerTasks();
        logger.info("MMCReboot Disabled");
    }

    // No reload for fabric plugins, uses a command instead
    public void onPluginReload() {
        cancelRebootTimerTasks();
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
        RebootHelp rebootHelp = new RebootHelp(this);
        dispatcher.register(CommandManager.literal("reboot")
                .then(CommandManager.literal("help")
                        .executes(context -> rebootHelp.execute(context.getSource()))
                )
        );

        RebootVote rebootVote = new RebootVote(this);
        dispatcher.register(CommandManager.literal("reboot")
                .then(CommandManager.literal("vote")
                        .then(CommandManager.argument("op", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"on", "off", "yes", "no"}, builder))
                                .executes(rebootVote::executeSubcommand))
                        .executes(rebootVote::executeBase)

                )
        );

        // /Reboot cancel
        RebootCancel rebootCancel = new RebootCancel(this);
        dispatcher.register(CommandManager.literal("reboot")
                .then(CommandManager.literal("cancel")
                        .requires(source -> Permissions.check(source, RebootPermisssions.COMMAND_CANCEL, 4))
                        .executes(context -> rebootCancel.execute(context.getSource()))
                )
        );

        // /Reboot time
        RebootTime rebootTime = new RebootTime(this);
        dispatcher.register(CommandManager.literal("reboot")
                .then(CommandManager.literal("time")
                        .requires(source -> Permissions.check(source, RebootPermisssions.COMMAND_TIME, 4))
                        .executes(context -> rebootTime.execute(context.getSource()))
                )
        );

        // /Reboot confirm
        RebootConfirm rebootConfirm = new RebootConfirm(this);
        dispatcher.register(CommandManager.literal("reboot")
                .then(CommandManager.literal("confirm")
                        .requires(source -> Permissions.check(source, RebootPermisssions.COMMAND_NOW, 4))
                        .executes(context -> rebootConfirm.execute(context.getSource()))
                )
        );

        // /Reboot now
        RebootNow rebootNow = new RebootNow(this);
        dispatcher.register(CommandManager.literal("reboot")
                .then(CommandManager.literal("now")
                        .requires(source -> Permissions.check(source, RebootPermisssions.COMMAND_NOW, 4))
                        .executes(context -> rebootNow.execute(context.getSource()))
                )
        );

        // /Reboot start h/m/s time reason
        RebootCMD rebootCMD = new RebootCMD(this);
        dispatcher.register(CommandManager.literal("reboot")
                .then(CommandManager.literal("start")
                        .requires(source -> Permissions.check(source, RebootPermisssions.COMMAND_START, 4))
                        .then(CommandManager.argument("unit", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"h", "m", "s"}, builder))
                                .then(CommandManager.argument("time", IntegerArgumentType.integer())
                                        .then(CommandManager.argument("reason", StringArgumentType.greedyString())
                                                .executes(rebootCMD::execute))
                                        .executes(rebootCMD::execute)
                                )
                        )
                )
        );

        // /Reboot reload
        dispatcher.register(CommandManager.literal("reboot")
                .then(CommandManager.literal("reload")
                        .requires(source -> Permissions.check(source, RebootPermisssions.COMMAND_RELOAD, 4))
                        .executes(context -> {
                            onPluginReload();
                            context.getSource().sendFeedback(() -> fromPlaceholderAPI("<green>Reloaded MMCReboot</green>"), true);
                            return 1;
                        }
                ))
        );
    }

    public Double getTPS() {
        try {
            return Objects.requireNonNull(SparkProvider.get().tps()).poll(StatisticWindow.TicksPerSecond.MINUTES_5);
        } catch (Exception e) {
            // Spark is not loaded
            return 20d;
        }
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
                Runnable runnable = () -> {
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
                };
                rebootTimerTasks.add(runnable);
                getTaskManager().scheduleSingleTask(runnable, 15, TimeUnit.SECONDS);
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
        cancelRebootTimerTasks();
        nextRealTimeRestart = getNextRealTimeFromConfig();
        double rInterval = nextRealTimeRestart;
        if (config.timer.timerBroadcast != null) {
            warningMessages(rInterval);
        }
        ShutdownTask task = new ShutdownTask(this);
        rebootTimerTasks.add(task);
        getTaskManager().scheduleSingleTask(task, nextRealTimeRestart, TimeUnit.SECONDS);

        logger.info("[MMCReboot] RebootCMD scheduled for " + (long)(nextRealTimeRestart) + " seconds from now!");
        tasksScheduled = true;
        startTimestamp = System.currentTimeMillis();
        isRestarting = true;
    }

    public void scheduleTasks() {
        boolean wasTPSRestarting = getTPSRestarting();
        cancelRebootTimerTasks();
        setTPSRestarting(wasTPSRestarting);
        double rInterval = config.autorestart.restartInterval * 3600;
        if (config.timer.timerBroadcast != null) {
            warningMessages(rInterval);
        }
        ShutdownTask task = new ShutdownTask(this);
        rebootTimerTasks.add(task);
        getTaskManager().scheduleSingleTask(task, config.autorestart.restartInterval, TimeUnit.HOURS);

        logger.info("[MMCReboot] RebootCMD scheduled for " + (long)(config.autorestart.restartInterval  * 3600.0) + " seconds from now!");
        tasksScheduled = true;
        startTimestamp = System.currentTimeMillis();
        isRestarting = true;
    }

    private void warningMessages(double rInterval) {

        config.timer.timerBroadcast.stream().filter(aTimerBroadcast -> rInterval * 60 - aTimerBroadcast > 0).forEach(aTimerBroadcast -> {
            if (aTimerBroadcast <= rInterval) {
                Runnable runnable = () -> {
                    double timeLeft = rInterval - ((double) (System.currentTimeMillis() - startTimestamp) / 1000);
                    timeLeft += 1; // Hack for the 1 tick delay in the scheduler
                    int hours = (int) (timeLeft / 3600);
                    int minutes = (int) ((timeLeft - hours * 3600) / 60);
                    int seconds = (int) timeLeft % 60;

                    NumberFormat formatter = new DecimalFormat("00");
                    String s = formatter.format(seconds);
                    if (config.timer.timerUseChat) {
                        if (minutes > 1) {
                            String message = Messages.getRestartNotificationMinutes().replace("%minutes%", "" + minutes).replace("%seconds%", s);
                            broadcastMessage("<white>[</white><gold>Restart</gold><white>] </white>" + message);
                        } else if (minutes == 1) {
                            String message = Messages.getRestartNotificationMinute().replace("%minutes%", "" + minutes).replace("%seconds%", s);
                            broadcastMessage("<white>[</white><gold>Restart</gold><white>] </white>" + message);
                        } else {
                            String message = Messages.getRestartNotificationSeconds().replace("%minutes%", "" + minutes).replace("%seconds%", s);
                            broadcastMessage("<white>[</white><gold>Restart</gold><white>] </white>" + message);
                        }
                    }
                    logger.info("[MMCReboot] " + "The server will be restarting in " + hours + "h" + minutes + "m" + seconds + "s");
                    if (!playSoundNow && config.timer.playSoundFirstTime >= aTimerBroadcast) {
                        playSoundNow = true;
                    }

                    playGlobalSound();
                    updateTitles(hours, minutes, s);

                    if (reason != null) {
                        broadcastMessage("<white>[</white><gold>Restart</gold><white>] </white><light_purple>" + reason + "</light_purple>");
                    }
                    isRestarting = true;
                };
                rebootTimerTasks.add(runnable);
                getTaskManager().scheduleSingleTask(runnable, (long) ((rInterval - aTimerBroadcast)), TimeUnit.SECONDS);
                logger.info("[MMCReboot] warning scheduled for " + (long) (rInterval - aTimerBroadcast) + " seconds from now!");
            }
        });
    }

    private void updateTitles(int hours, int minutes, String s) {
        if (!config.timer.titleEnabled) {
            return;
        }

        Text title = fromPlaceholderAPI(config.timer.titleMessage.replace("{hours}", "" + hours).replace("{minutes}", "" + minutes).replace("{seconds}", s));;
        Text subtitle;
        if (reason != null && !reason.isEmpty()) {
            subtitle = fromPlaceholderAPI(reason);
        } else {
            title = Text.of("");
            subtitle = title;
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

        server.getPlayerManager().getPlayerList().forEach(player -> player.playSound(soundEvent, SoundCategory.MASTER, 1, 1));
    }

    public void cancelRebootTimerTasks() {
        getTaskManager().removeTasks(rebootTimerTasks);
        tasksScheduled = false;
        isRestarting = false;
        TPSRestarting = false;
        nextRealTimeRestart = 0;
    }


    public void stopServer() {
        logger.info("[MMCReboot] Restarting...");

        isRestarting = false;
        broadcastMessage("<red>Server is restarting, we'll be right back!</red>");
        try {
            if (config.kickmessage.isEmpty()) {
                Text reason = Text.of("Server is restarting, we'll be right back!");
                server.getPlayerManager().getPlayerList().forEach(player -> player.networkHandler.disconnect(reason));
            } else {
                server.getPlayerManager().getPlayerList().forEach(player -> player.networkHandler.disconnect(fromPlaceholderAPI(config.kickmessage)));
            }
            // mmcreboot-s-shutdown
            getTaskManager().scheduleSingleTask(() -> server.stop(false), 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.info("[MMCReboot] Something went wrong while saving & stopping!");
            logger.info("Exception: " + e);
            broadcastMessage("<red>Server has encountered an error while restarting.</red>");
            e.printStackTrace();
        }
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

        board = server.getScoreboard();

        // Only show minutes if it's less than 5 minutes
        if (!(minutes <= 4 && hours == 0)) {
            return;
        }

        NumberFormat formatter = new DecimalFormat("00");
        String s = formatter.format(seconds);

        ScoreboardObjective obj;
        if (board.getObjective("restart") != null) {
            obj = board.getObjective("restart");
            board.removeObjective(obj);
        }
        obj = board.addObjective("restart", ScoreboardCriterion.DUMMY, Text.of(Messages.getSidebarRestartTimerTitle()), ScoreboardCriterion.RenderType.INTEGER);

        board.setObjectiveSlot(Scoreboard.SIDEBAR_DISPLAY_SLOT_ID, obj);
        board.getPlayerScore(Text.literal(minutes +":" + s).formatted(Formatting.GREEN).getString(), obj).setScore(0);

        // Boss bar
        int totalTimeLeftInSeconds = (hours * 3600) + (minutes * 60) + seconds;
        int maxTime = 300; // 5 minutes
        int effectiveInterval = (int) Math.min(rInterval, maxTime); // Account for < 5m restarts
        int timeConsidered = Math.min(totalTimeLeftInSeconds, effectiveInterval);
        float percent = (float) timeConsidered / effectiveInterval * 100.0f;

        if (config.bossBar.bossbarEnabled) {
            if (bar == null) {
                bar = new ServerBossBar(Text.of(config.bossBar.bossbarTitle.replace("{minutes}", Integer.toString(minutes)).replace("{seconds}", s)), BossBar.Color.GREEN, BossBar.Style.PROGRESS);
            } else {
                bar.setPercent(percent/100f);
            }
        }

        server.getPlayerManager().getPlayerList().forEach(player -> {
            if (!config.bossBar.bossbarEnabled) {
                return;
            }

            bar.addPlayer(player);
        });
    }


    public void displayVotes() {
        if (board == null) {
            board = new Scoreboard();
        }

        // TODO test this
        ScoreboardObjective obj = board.addObjective("vote", ScoreboardCriterion.DUMMY, Text.of(Messages.getSidebarTitle()), ScoreboardCriterion.RenderType.INTEGER);
        board.getPlayerScore(Messages.getSidebarYes() + ":", obj).setScore(yesVotes);
        board.getPlayerScore(Messages.getSidebarNo() + ":", obj).setScore(noVotes);
        board.getPlayerScore(Messages.getSidebarTimeleft() + ":", obj).setScore(getTimeLeftInSeconds());

        server.getPlayerManager().getPlayerList().forEach(player -> player.getScoreboard().setObjectiveSlot(1, obj)); // 1 for SIDEBAR
    }

    public void removeScoreboard() {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.getScoreboard().setObjectiveSlot(1, null); // Clear sidebar
        }
    }

    public void removeBossBar() {
        if (bar != null) {
            bar.clearPlayers();
        }
    }

    public void broadcastMessage(String message) {
        Text text = fromPlaceholderAPI(message);
        server.getPlayerManager().getPlayerList().forEach(player -> player.sendMessage(text));
    }

    public void broadcastMessage(Text message) {
        server.getPlayerManager().getPlayerList().forEach(player -> player.sendMessage(message));
    }

    public void sendMessage(ServerCommandSource sender, String message) {
        sender.sendMessage(fromPlaceholderAPI(message));
    }

    public static Text fromPlaceholderAPI(String message) {
        return TextParserUtils.formatText(message);
    }

    public Config getConfig() {
        return config;
    }

    public int getOnlinePlayerCount() {
        return server.getPlayerManager().getPlayerList().size();
    }

    public void startRebootConfirmTimer(ServerCommandSource src) {
        Runnable rebootConfirmationTask = () -> {
            rebootConfirm = false;
            sendMessage(src, Messages.getErrorTookTooLong());
        };
        getTaskManager().scheduleSingleTask(rebootConfirmationTask, 60, TimeUnit.SECONDS);
    }

    public ScheduledTaskManager getTaskManager() {
        return ((ServerTickMixinAccessor) server).getTaskManager();
    }

    public void clearScoreboard() {
        board = server.getScoreboard();
        if (board != null) {
            // Clear restart scoreboard as its persisted otherwise
            ScoreboardObjective obj;
            if ((obj = board.getObjective("restart")) != null) {
                board.removeObjective(obj);
            }
        }
    }

}
