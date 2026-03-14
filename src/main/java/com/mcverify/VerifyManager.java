package com.mcverify;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class VerifyManager {

    private final MCVerifyPlugin plugin;
    private final BridgeClient   bridge;

    public VerifyManager(MCVerifyPlugin plugin, BridgeClient bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    /**
     * Sends the verify request to the Python bot bridge on a separate thread,
     * then replies to the player on the main thread.
     */
    public void attemptVerify(Player player, String code) {
        player.sendMessage(ChatColor.YELLOW + "⏳ Verifying your code, please wait...");

        new Thread(() -> {
            String result = bridge.sendVerify(player.getName(), code);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if ("verified".equals(result)) {
                    player.sendMessage(
                        ChatColor.GREEN + "✅ " + ChatColor.WHITE +
                        "Verification successful! Your Minecraft account is now linked to your Discord."
                    );
                } else if ("invalid".equals(result)) {
                    player.sendMessage(
                        ChatColor.RED + "❌ " + ChatColor.WHITE +
                        "Invalid or expired code. Please run " +
                        ChatColor.YELLOW + "!verify-mc" + ChatColor.WHITE +
                        " on Discord to get a new code and use " +
                        ChatColor.YELLOW + "/dc-verify <code>" + ChatColor.WHITE + " here."
                    );
                } else {
                    player.sendMessage(
                        ChatColor.RED + "⚠ " + ChatColor.WHITE +
                        "Could not reach the verification service. Please try again in a moment."
                    );
                }
            });
        }, "MCVerify-Verify").start();
    }
}
