package net.moddedminecraft.mmcreboot.Config;

import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import net.minecraft.util.Identifier;
import net.moddedminecraft.mmcreboot.Main;

import java.util.List;

public class Config extends me.fzzyhmstrs.fzzy_config.config.Config {

    public Config() {
        super(new Identifier(Main.MOD_ID, "config"));
    }

    private static final Integer[] timerBroadcastList = {
            600,
            300,
            240,
            180,
            120,
            60,
            30
    };

    private static final String[] RealTimeList = {
            "00:00",
            "06:00",
            "12:00",
            "18:00"
    };

    private static final String[] restartCmdList = {
            "/changeme",
            "/me too"
    };

    public AutoRestartSection autorestart = new AutoRestartSection();
    public static class AutoRestartSection extends ConfigSection{
        @Comment("Values: Fixed, Realtime or None. The value chooses here represents what timer will be used.")
        public String restartType = "Fixed";

        @Comment("How long in hours should the auto restart timer be set for?")
        public double restartInterval = 6;

        @Comment("Set times for server restarts (24h time eg: 18:30)")
        public List<String> realTimeInterval = List.of(RealTimeList);
    }


    public TimerSection timer = new TimerSection();
    public static class TimerSection extends ConfigSection {

        @Comment("Whether or not the scoreboard should be shown during a vote")
        public boolean timerUseVoteScoreboard = true;

        @Comment("Whether or not the scoreboard should be shown during the last 5 minute countdown to a restart")
        public boolean timerUseScoreboard = true;

        @Comment("warning times before reboot in seconds")
        public List<Integer> timerBroadcast = List.of(timerBroadcastList);

        @Comment("Whether or not the warning should be broadcast in the chat.")
        public boolean timerUseChat = true;

        @Comment("Time before another vote to restart can begin. (In minutes)")
        public int timerRevote = 10;

        @Comment("How long should it be before players are allowed to start a vote after the server has restarted (In minutes)")
        public int timerStartvote = 60;

        @Comment("% of online players to vote yes before a restart is triggered.")
        public int timerVotepercent = 60;

        @Comment("Time until the restart after a vote has passed in seconds (default 300 = 5 minutes)")
        public int timerVotepassed = 300;

        @Comment("The required amount of players online to start a vote")
        public int timerMinplayers = 5;

        @Comment("Should a sound be played when a restart broadcast is sent?")
        public boolean playSoundEnabled = true;

        @Comment("The sound that should play for the notification.")
        public String playSoundString = "block.note_block.pling";

        @Comment("When should the sound notification start? (This should be the same as one of your broadcast timers)")
        public double playSoundFirstTime = 600;

        @Comment("Should a title message pop up in the middle of the screen")
        public boolean titleEnabled = true;

        @Comment("How long should the title message show up for before disappearing? (in seconds)")
        public int titleStayTime = 2;

        @Comment("The title message to be displayed ({hours},{minutes},{seconds} will be replaced")
        public String titleMessage = "The server will be restarting in {minutes}:{seconds}";

    }

    public BossBarSection bossBar = new BossBarSection();
    public static class BossBarSection extends ConfigSection {
        @Comment("If true, A bossbar will display with a countdown until restart.")
        public boolean bossbarEnabled = false;

        @Comment("Title displayed above the boss bar, Can use {minutes} and {seconds} to display time")
        public String bossbarTitle = "Restart";
    }

    public VotingSection voting = new VotingSection();
    public static class VotingSection extends ConfigSection {
        @Comment("Enable or Disable the ability for players to vote for a server restart")
        public boolean voteEnabled = true;
    }

    public RestartSection restart = new RestartSection();
    public static class RestartSection extends ConfigSection {
        @Comment("If enabled, This will run the configured command instead of restarting the server.")
        public boolean restartUseCommand = false;

        @Comment("The command(s) to run if 'use-command' has been enabled")
        public List<String> restartCommands = List.of(restartCmdList);

        @Comment("The default reason shown for a restart (automated and manual), Leave blank for no reason.")
        public String defaultRestartReason = "";
    }

    public TPSSection tps = new TPSSection();
    public static class TPSSection extends ConfigSection {

        @Comment("If enabled, the server will initiate a restart timer if the TPS is below the minimum set.")
        public boolean tpsEnabled = false;

        @Comment("The minimum TPS to initiate a restart timer")
        public int tpsMinimum = 10;

        @Comment("Time until the restart after a TPS check has failed, in seconds (default 300 = 5 minutes)")
        public int tpsTimer = 300;

        @Comment("If enabled, there will be a reason broadcast alongside the countdown for the restart.")
        public boolean tpsUseReason = true;

        @Comment("The reason to broadcast if 'use-reason' is enabled")
        public String tpsMessage = "Server TPS is below the minimum.";

        @Comment("If set to true, When the restart timer reaches 0, The TPS will be checked again \n"
                + "If the TPS is above the minimum, the restart is canceled")
        public boolean tpsRestartCancel = false;

        @Comment("The broadcast message sent to everyone if the restart was canceled")
        public String tpsRestartCancelMsg = "&bThe server will not restart. The TPS is now above the minimum";

        @Comment("How long after the server starts until the TPS check initiates. (In minutes)")
        public int tpsCheckDelay = 15;
    }

    @Comment("Localization to be used, All available translations are in the 'localization' folder")
    public String language = "EN";

    @Comment("The message that is sent to all players as the server shuts down.")
    public String kickmessage = "The server is restarting.";

}
