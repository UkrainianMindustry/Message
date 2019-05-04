package io.anuke.corebot;

import java.io.File;

public class CoreBot{
    public static final String releasesURL = "https://api.github.com/repos/Anuken/Mindustry/releases";
    public static final long guildID = 391020510269669376L;
    public static final File prefsFile = new File("prefs.properties");
    public static final long bugReportChannelID = 391073027309305856L;
    public static final long crashReportChannelID = 467033526018113546L;
    public static final long serverChannelID = 517896556029149214L;
    public static final long logChannelID = 568416809964011531L;
    public static final long commandChannelID = 574274730618781706L;
    public static final long announcementsChannelID = 391020997098340352L;
    public static final long zoneMapsChannelID = 574357612624347136L;
    public static final long standardMapsChannelID = 574357680706158711L;
    public static final boolean sendWelcomeMessages = false;

    public static final long messageDeleteTime = 20000;

    public static ServerBridge server = new ServerBridge();
    public static Messages messages = new Messages();
    public static Commands commands = new Commands();
    public static Net net = new Net();
    public static Prefs prefs = new Prefs(prefsFile);
    public static Reports reports = new Reports();
    public static Maps maps = new Maps();

    public static void main(String[] args){
        new CoreBot();
    }
}
