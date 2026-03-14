package com.mcverify;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class MCVerifyPlugin extends JavaPlugin {

    private static MCVerifyPlugin instance;
    private BridgeClient      bridgeClient;
    private VerifyManager     verifyManager;
    private DiscordChatServer discordChatServer;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        FileConfiguration config = getConfig();

        String bridgeUrl    = config.getString("bridge.url",    "http://eu-ey.purpify.host:26047");
        String bridgeSecret = config.getString("bridge.secret", "RUNEMC100");
        int    listenPort   = config.getInt("listen-port", 26048);

        bridgeClient  = new BridgeClient(bridgeUrl, bridgeSecret, getLogger());
        verifyManager = new VerifyManager(this, bridgeClient);

        // Start HTTP listener for !chat messages from Discord bot
        discordChatServer = new DiscordChatServer(this, bridgeSecret, getLogger());
        discordChatServer.start(listenPort);

        // Register listeners & commands
        getServer().getPluginManager().registerEvents(new ChatListener(this, bridgeClient), this);
        getCommand("dc-verify").setExecutor(new VerifyCommand(verifyManager));

        getLogger().info("MCVerify plugin enabled! Listening on port " + listenPort);
    }

    @Override
    public void onDisable() {
        if (discordChatServer != null) discordChatServer.stop();
        getLogger().info("MCVerify plugin disabled.");
    }

    public static MCVerifyPlugin getInstance() { return instance; }
    public BridgeClient getBridgeClient()       { return bridgeClient; }
    public VerifyManager getVerifyManager()     { return verifyManager; }
}
