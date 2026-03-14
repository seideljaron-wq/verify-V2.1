package com.mcverify;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class BridgeClient {

    private final String baseUrl;
    private final String secret;
    private final Logger logger;

    public BridgeClient(String baseUrl, String secret, Logger logger) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.secret  = secret;
        this.logger  = logger;
    }

    /** Send a chat message to the Discord bridge (async via new thread). */
    public void sendChat(String player, String rank, String message) {
        String json = String.format(
            "{\"secret\":\"%s\",\"player\":\"%s\",\"rank\":\"%s\",\"message\":\"%s\"}",
            escape(secret), escape(player), escape(rank), escape(message)
        );
        postAsync("/chat", json);
    }

    /**
     * Send a verify attempt to the Discord bridge.
     * @return "verified", "invalid", or null on error.
     */
    public String sendVerify(String player, String code) {
        String json = String.format(
            "{\"secret\":\"%s\",\"player\":\"%s\",\"code\":\"%s\"}",
            escape(secret), escape(player), escape(code)
        );
        return postSync("/verify", json);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void postAsync(String endpoint, String json) {
        new Thread(() -> {
            try {
                postSync(endpoint, json);
            } catch (Exception e) {
                logger.warning("Bridge async POST failed: " + e.getMessage());
            }
        }, "MCVerify-Bridge").start();
    }

    private String postSync(String endpoint, String json) {
        try {
            URL url = new URL(baseUrl + endpoint);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setConnectTimeout(3000);
            con.setReadTimeout(5000);
            con.setDoOutput(true);

            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = con.getOutputStream()) {
                os.write(body);
            }

            int status = con.getResponseCode();
            if (status == 200) {
                return new String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                logger.warning("Bridge POST " + endpoint + " returned HTTP " + status);
                return null;
            }
        } catch (Exception e) {
            logger.warning("Bridge POST " + endpoint + " error: " + e.getMessage());
            return null;
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
