package com.mcverify;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Tiny HTTP server inside the MC plugin that listens for
 * POST /discord-chat from the Discord bot bridge.
 * The bot sends: { "secret": "...", "player": "Steve", "message": "hello" }
 * We then broadcast "[Discord] <Steve> hello" in game chat.
 */
public class DiscordChatServer {

    private final MCVerifyPlugin plugin;
    private final String         secret;
    private final Logger         logger;
    private HttpServer           server;

    public DiscordChatServer(MCVerifyPlugin plugin, String secret, Logger logger) {
        this.plugin = plugin;
        this.secret = secret;
        this.logger = logger;
    }

    public void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/discord-chat", new ChatHandler());
            server.setExecutor(null);
            server.start();
            logger.info("DiscordChatServer listening on port " + port);
        } catch (Exception e) {
            logger.severe("Failed to start DiscordChatServer: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                String body = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)
                ).lines().collect(Collectors.joining("\n"));

                // Simple JSON parsing (no external lib needed)
                String parsedSecret  = extractJson(body, "secret");
                String parsedPlayer  = extractJson(body, "player");
                String parsedMessage = extractJson(body, "message");

                if (!secret.equals(parsedSecret)) {
                    send(exchange, 403, "Forbidden");
                    return;
                }

                // Broadcast on main thread
                String line = ChatColor.GRAY + "[Discord] " + ChatColor.WHITE
                    + "<" + parsedPlayer + "> " + parsedMessage;

                Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.broadcastMessage(line)
                );

                send(exchange, 200, "ok");
            } catch (Exception e) {
                logger.warning("DiscordChatHandler error: " + e.getMessage());
                try { exchange.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
            }
        }

        private void send(HttpExchange ex, int status, String body) throws Exception {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        }

        /** Very simple JSON string value extractor (no external deps). */
        private String extractJson(String json, String key) {
            String search = "\"" + key + "\"";
            int idx = json.indexOf(search);
            if (idx == -1) return "";
            int colon = json.indexOf(":", idx + search.length());
            if (colon == -1) return "";
            int q1 = json.indexOf("\"", colon + 1);
            if (q1 == -1) return "";
            int q2 = json.indexOf("\"", q1 + 1);
            if (q2 == -1) return "";
            return json.substring(q1 + 1, q2);
        }
    }
}
