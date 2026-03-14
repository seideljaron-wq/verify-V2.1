package com.mcverify;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final MCVerifyPlugin plugin;
    private final BridgeClient   bridge;

    public ChatListener(MCVerifyPlugin plugin, BridgeClient bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player  = event.getPlayer();
        String name    = player.getName();
        String message = event.getMessage();

        // Try to get a rank/prefix from the display name or a permissions plugin.
        // We parse the display name: it may look like "§6[Supervisor] §fSteve"
        // Strip colour codes, extract bracket content as the rank.
        String rank = extractRank(player.getDisplayName());

        bridge.sendChat(name, rank, message);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Tries to extract a rank prefix from a display name.
     * E.g. "§6[Supervisor] §fSteve" → "Supervisor"
     * Falls back to an empty string if no bracket prefix is found.
     */
    private static String extractRank(String displayName) {
        // Strip Minecraft colour/formatting codes (§X)
        String clean = displayName.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();

        // Look for [Rank] pattern at the start
        if (clean.startsWith("[")) {
            int end = clean.indexOf("]");
            if (end > 1) {
                return clean.substring(1, end);
            }
        }
        return "";
    }
}
