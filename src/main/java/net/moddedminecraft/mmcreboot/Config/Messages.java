package net.moddedminecraft.mmcreboot.Config;

import net.minecraft.util.Identifier;
import net.moddedminecraft.mmcreboot.Main;

import java.util.List;

public class Messages extends me.fzzyhmstrs.fzzy_config.config.Config {

    private static Main plugin;

    //private Path defaultMessage;


    public Messages(Main main) {
        super(new Identifier(Main.MOD_ID, "lang"));

        plugin = main;
        String language = plugin.getConfig().language;
        /*defaultMessage = plugin.configDir.resolve("localization/messages_" + language + ".conf");

        if (Files.notExists(defaultMessage)) {
            plugin.logger.warn("Localization was not found");
        }
        messageLoader = HoconConfigurationLoader.builder().setPath(defaultMessage).build();
        messages = messageLoader.load();
        checkLangAssetFiles();
        messageCheck();
        */
    }

    private static String[] restartVoteBroadcastDefault = {
            "<green>%playername$ </green><aqua>has voted that the server should be restarted</aqua>",
            "<gold>Type </gold><green>/reboot vote yes </green><gold>if you agree</gold>",
            "<gold>Type </gold><red>/reboot vote no </red><gold>if you do not agree</gold>",
            "<gold>If there are more yes votes than no, The server will be restarted! (minimum of %config.timerminplayers%)</gold>",
            "<aqua>You have </aqua><green>90 </green><aqua>seconds to vote!</aqua>"
    };

    private static String[] restartVoteBroadcastOnLoginDefault = {
            "<dark_aqua>There is a vote to restart the server.</dark_aqua>",
            "<gold>Type </gold><green>/reboot vote yes </green><gold>if you agree</gold>",
            "<gold>Type </gold><red>/reboot vote no </red><gold>if you do not agree</gold>",
            "<gold>If there are more yes votes than no, The server will be restarted! (minimum of %config.timerminplayers%)</gold>"
    };

    private static String chatprefix = "<white>[</white><gold>MMCReboot</gold><white>] </white>";

    //sidebar
    private static String sidebarTitle = "Restart Vote";
    private static String sidebarYes = "Yes";
    private static String sidebarNo = "No";
    private static String sidebarTimeleft = "Time Left";
    private static String sidebarRestartTimerTitle = "Restart Timer";

    //error
    private static String errorAlreadyVoted = "<dark_red>You have already voted!</dark_red>";
    private static String errorNoVoteRunning = "<dark_red>There is no vote running at the moment</dark_red>";
    private static String errorVoteToRestartDisabled = "<dark_red>Voting to restart is disabled</dark_red>";
    private static String errorVoteAlreadyRunning = "<dark_red>A vote is already running</dark_red>";
    private static String errorNotOnlineLongEnough = "<dark_red>The server needs to be online for %config.timerstartvote% minutes before starting a vote!</dark_red>";
    private static String errorMinPlayers = "<dark_red>There must be a minimum of %config.timerminplayers% players online to start a vote</dark_red>";
    private static String errorAlreadyRestarting = "<dark_red>The server is already restarting!</dark_red>";
    private static String errorWaitTime = "<dark_red>You need to wait %config.timerrevote% minutes before starting another vote!</dark_red>";
    private static String errorNoPermission = "<dark_red>You don't have permission to do this!</dark_red>";
    private static String errorNoTaskScheduled = "<red>There is no restart scheduled!</red>";
    private static String errorTookTooLong = "<red>You took too long to confirm the reboot.</red>";
    private static String errorInvalidTimescale = "<red>Invalid time scale!</red>";
    private static String errorNothingToConfirm = "<red>There is nothing to confirm.</red>";

    //general
    private static String restartCancel = "<dark_aqua>Restarts have been canceled.</dark_aqua>";
    private static String restartPassed = "Players have voted to restart the server.";
    private static String restartVoteNotEnoughVoted = "<dark_aqua>The server will not be restarted. Not enough people have voted.</dark_aqua>";
    private static String votedYes = "You Voted Yes!";
    private static String votedNo = "You Voted No!";
    private static String restartMessageWithReason = "<dark_aqua>The server will now be restarting in </dark_aqua><white>%hours%h%minutes%m%seconds%s </white><dark_aqua>with the reason:</dark_aqua>";
    private static String restartMessageWithoutReason = "<dark_aqua>The server will now be restarting in </dark_aqua><white>%hours%h%minutes%m%seconds%s</white>";
    private static String restartFormatMessage = "<aqua>Use 'h' for time in hours, 'm' for minutes and 's' for seconds</aqua>";
    private static String restartConfirm = "<red>Ok, you asked for it!</red>";
    private static String restartConfirmMessage = "<red>Please type: </red><gold>/Reboot Confirm </gold><red>if you are sure you want to do this.</red>";

    //vote notification
    private static List<String> restartVoteBroadcast;
    private static List<String> restartVoteBroadcastOnLogin;

    //restart notification
    private static String restartNotificationMinutes = "<aqua>The server will be restarting in </aqua><white>%minutes%:%seconds% </white><aqua>minutes</aqua>";
    private static String restartNotificationMinute = "<aqua>The server will be restarting in </aqua><white>%minutes% </white><aqua>minute</aqua>";
    private static String restartNotificationSeconds = "<aqua>The server will be restarting in </aqua><white>%seconds% </white><aqua>seconds</aqua>";

    //help
    private static String helpHeader = "<dark_aqua>[] = required  () = optional</dark_aqua>";
    private static String helpHelp = "<dark_aqua>/reboot </dark_aqua><aqua>help - </aqua><gray>shows this help</gray>";
    private static String helpNow = "<dark_aqua>/reboot </dark_aqua><aqua>now - </aqua><gray>restarts the server instantly</gray>";
    private static String helpStart = "<dark_aqua>/reboot start </dark_aqua><gray>[</gray><aqua>h</aqua><gray>|</gray><aqua>m</aqua><gray>|</gray><aqua>s</aqua><gray>] </gray><gray>[</gray><aqua>time</aqua><gray>] </gray><gray>(</gray><aqua>reason</aqua><gray>) </gray><aqua>- </aqua><gray>restart the server after a given time</gray>";
    private static String helpCancel = "<dark_aqua>/reboot </dark_aqua><aqua>cancel - </aqua><gray>cancel any current restart timer</gray>";
    private static String helpVote = "<dark_aqua>/reboot </dark_aqua><aqua>vote - </aqua><gray>starts a vote to restart the server</gray>";
    private static String helpTime = "<dark_aqua>/reboot </dark_aqua><aqua>time - </aqua><gray>informs you how much time is left before restarting</gray>";
    private static String helpVoteYea = "<dark_aqua>/reboot </dark_aqua><aqua>vote yes - </aqua><gray>vote yes to restart the server</gray>";
    private static String helpVoteNo = "<dark_aqua>/reboot </dark_aqua><aqua>vote no - </aqua><gray>vote no to restart the server</gray>";

/*



    private void messageCheck() {

        //sidebar
        sidebarTitle = check(messages.getNode("sidebar", "vote", "title"), sidebarTitle).getString();
        sidebarYes = check(messages.getNode("sidebar", "vote", "yes"), sidebarYes).getString();
        sidebarNo = check(messages.getNode("sidebar", "vote", "no"), sidebarNo).getString();
        sidebarTimeleft = check(messages.getNode("sidebar", "restart", "time-left"), sidebarTimeleft).getString();
        sidebarRestartTimerTitle = check(messages.getNode("sidebar", "restart", "title"), sidebarRestartTimerTitle).getString();

        //error
        errorAlreadyVoted = check(messages.getNode("error", "already-voted"), errorAlreadyVoted).getString();
        errorNoVoteRunning = check(messages.getNode("error", "no-vote-running"), errorNoVoteRunning).getString();
        errorVoteToRestartDisabled = check(messages.getNode("error", "vote-restart-disabled"), errorVoteToRestartDisabled).getString();
        errorVoteAlreadyRunning = check(messages.getNode("error", "vote-already-running"), errorVoteAlreadyRunning).getString();
        errorNotOnlineLongEnough = check(messages.getNode("error", "not-online-long-enough"), errorNotOnlineLongEnough).getString();
        errorMinPlayers = check(messages.getNode("error", "min-players"), errorMinPlayers).getString();
        errorAlreadyRestarting = check(messages.getNode("error", "already-restarting"), errorAlreadyRestarting).getString();
        errorWaitTime = check(messages.getNode("error", "wait-time"), errorWaitTime).getString();
        errorNoPermission = check(messages.getNode("error", "no-permission"), errorNoPermission).getString();
        errorNoTaskScheduled = check(messages.getNode("error", "no-task-scheduled"), errorNoTaskScheduled).getString();
        errorTookTooLong = check(messages.getNode("error", "took-too-long"), errorTookTooLong).getString();
        errorInvalidTimescale = check(messages.getNode("error", "invalid-time-scale"), errorInvalidTimescale).getString();
        errorNothingToConfirm = check(messages.getNode("error", "nothing-to-confirm"), errorNothingToConfirm).getString();

        //general
        restartCancel = check(messages.getNode("general", "restart-canceled"), restartCancel).getString();
        restartPassed = check(messages.getNode("general", "restart-passed"), restartPassed).getString();
        restartVoteNotEnoughVoted = check(messages.getNode("general", "not-enough-voted"), restartVoteNotEnoughVoted).getString();
        votedYes = check(messages.getNode("general", "voted-yes"), votedYes).getString();
        votedNo = check(messages.getNode("general", "voted-no"), votedNo).getString();
        restartMessageWithReason = check(messages.getNode("general", "restart-with-reason"), restartMessageWithReason).getString();
        restartMessageWithoutReason = check(messages.getNode("general", "restart-no-reason"), restartMessageWithoutReason).getString();
        restartFormatMessage = check(messages.getNode("general", "restart-format"), restartFormatMessage).getString();
        restartConfirm = check(messages.getNode("general", "restart-confirmed"), restartConfirm).getString();
        restartConfirmMessage = check(messages.getNode("general", "confirm-restart"), restartConfirmMessage).getString();

        //vote notification
        restartVoteBroadcast = checkList(messages.getNode("vote-notification", "after-command"), restartVoteBroadcastDefault).getList(TypeToken.of(String.class));
        restartVoteBroadcastOnLogin = checkList(messages.getNode("vote-notification", "on-login"), restartVoteBroadcastOnLoginDefault).getList(TypeToken.of(String.class));

        //restart notification
        restartNotificationMinutes = check(messages.getNode("restart-notification", "more-than-1-minute-remaining"), restartNotificationMinutes).getString();
        restartNotificationMinute = check(messages.getNode("restart-notification", "only-1-minute-remaining"), restartNotificationMinute).getString();
        restartNotificationSeconds = check(messages.getNode("restart-notification", "less-than-1-minute-remaining"), restartNotificationSeconds).getString();

        //help
        helpHeader = check(messages.getNode("help", "header"), helpHeader).getString();
        helpHelp = check(messages.getNode("help", "help"), helpHelp).getString();
        helpNow = check(messages.getNode("help", "now"), helpNow).getString();
        helpStart = check(messages.getNode("help", "start"), helpStart).getString();
        helpCancel = check(messages.getNode("help", "cancel"), helpCancel).getString();
        helpVote = check(messages.getNode("help", "vote"), helpVote).getString();
        helpTime = check(messages.getNode("help", "time"), helpTime).getString();
        helpVoteYea = check(messages.getNode("help", "vote-yes"), helpVoteYea).getString();
        helpVoteNo = check(messages.getNode("help", "vote-no"), helpVoteNo).getString();

        messageLoader.save(messages);
    }

    private void checkLangAssetFiles() throws IOException {
        if (!Files.isDirectory(plugin.configDir.resolve("localization"))) {
            Files.createDirectory(plugin.configDir.resolve("localization"));
        }
        String[] assets = {
                "messages_EN.conf",
                "messages_RU.conf"
        };
        for (String asset : assets) {
            if (!Files.exists(plugin.configDir.resolve("localization/" +asset))) {
                if (Sponge.getAssetManager().getAsset(plugin, asset).isPresent()) {
                    Sponge.getAssetManager().getAsset(plugin, asset).get().copyToFile(plugin.configDir.resolve("localization/" +asset));
                }
            }
        }
    }

    private CommentedConfigurationNode check(CommentedConfigurationNode node, Object defaultValue) {
        if (node.isVirtual()) {
            node.setValue(defaultValue);
        }
        return node;
    }

    private CommentedConfigurationNode checkList(CommentedConfigurationNode node, String[] defaultValue) {
        if (node.isVirtual()) {
            node.setValue(Arrays.asList(defaultValue));
        }
        return node;
    }*/

    public static String getChatprefix() {
        return chatprefix;
    }

    public static String getRestartCancel() {
        return restartCancel;
    }

    public static String getSidebarTitle() {
        return sidebarTitle;
    }

    public static String getSidebarNo() {
        return sidebarNo;
    }

    public static String getSidebarYes() {
        return sidebarYes;
    }

    public static String getSidebarTimeleft() {
        return sidebarTimeleft;
    }

    public static String getSidebarRestartTimerTitle() {
        return sidebarRestartTimerTitle;
    }

    public static String getRestartMessageWithoutReason() {
        return restartMessageWithoutReason;
    }

    public static String getRestartMessageWithReason() {
        return restartMessageWithReason;
    }

    public static String getErrorInvalidTimescale() {
        return errorInvalidTimescale;
    }

    public static String getRestartFormatMessage() {
        return restartFormatMessage;
    }

    public static String getRestartConfirm() {
        return restartConfirm;
    }

    public static String getErrorNothingToConfirm() {
        return errorNothingToConfirm;
    }

    public static String getHelpCancel() {
        return helpCancel;
    }

    public static String getHelpHeader() {
        return helpHeader;
    }

    public static String getHelpHelp() {
        return helpHelp;
    }

    public static String getHelpVote() {
        return helpVote;
    }

    public static String getHelpNow() {
        return helpNow;
    }

    public static String getHelpStart() {
        return helpStart;
    }

    public static String getHelpTime() {
        return helpTime;
    }

    public static String getHelpVoteNo() {
        return helpVoteNo;
    }

    public static String getHelpVoteYea() {
        return helpVoteYea;
    }

    public static String getErrorTookTooLong() {
        return errorTookTooLong;
    }

    public static String getRestartConfirmMessage() {
        return restartConfirmMessage;
    }

    public static String getErrorNoTaskScheduled() {
        return errorNoTaskScheduled;
    }

    public static String getErrorNoPermission() {
        return errorNoPermission;
    }

    public static String getErrorAlreadyRestarting() {
        return errorAlreadyRestarting;
    }

    public static String getErrorAlreadyVoted() {
        return errorAlreadyVoted;
    }

    public static String getErrorMinPlayers() {
        return errorMinPlayers.replace("%config.timerminplayers%", String.valueOf(plugin.getConfig().timer.timerMinplayers));
    }

    public static String getErrorNotOnlineLongEnough() {
        return errorNotOnlineLongEnough.replace("%config.timerstartvote%", String.valueOf(plugin.getConfig().timer.timerStartvote));
    }

    public static String getErrorNoVoteRunning() {
        return errorNoVoteRunning;
    }

    public static String getErrorVoteAlreadyRunning() {
        return errorVoteAlreadyRunning;
    }

    public static String getErrorVoteToRestartDisabled() {
        return errorVoteToRestartDisabled;
    }

    public static String getErrorWaitTime() {
        return errorWaitTime.replace("%config.timerrevote%", String.valueOf(plugin.getConfig().timer.timerRevote));
    }

    public static String getVotedNo() {
        return votedNo;
    }

    public static String getVotedYes() {
        return votedYes;
    }

    public static String getRestartVoteNotEnoughVoted() {
        return restartVoteNotEnoughVoted;
    }

    public static String getRestartPassed() {
        return restartPassed;
    }

    public static List<String> getRestartVoteBroadcast() {
        return restartVoteBroadcast;
    }

    public static List<String> getRestartVoteBroadcastOnLogin() {
        return restartVoteBroadcastOnLogin;
    }

    public static String getRestartNotificationMinute() {
        return restartNotificationMinute;
    }

    public static String getRestartNotificationMinutes() {
        return restartNotificationMinutes;
    }

    public static String getRestartNotificationSeconds() {
        return restartNotificationSeconds;
    }
}

