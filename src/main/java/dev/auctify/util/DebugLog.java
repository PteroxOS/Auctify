package dev.auctify.util;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

public final class DebugLog {
    private static final String SESSION_ID = "d37756";
    private static final File LOG_FILE = new File("D:/Auctify/debug-d37756.log");

    private DebugLog() {
    }

    public static void log(String runId, String hypothesisId, String location, String message, Map<String, Object> data) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            long now = System.currentTimeMillis();
            String payload = "{\"sessionId\":\"" + esc(SESSION_ID) + "\","
                    + "\"runId\":\"" + esc(runId) + "\","
                    + "\"hypothesisId\":\"" + esc(hypothesisId) + "\","
                    + "\"location\":\"" + esc(location) + "\","
                    + "\"message\":\"" + esc(message) + "\","
                    + "\"timestamp\":" + now + ","
                    + "\"data\":" + toJsonMap(data) + "}\n";
            writer.write(payload);
        } catch (Exception ignored) {
        }
    }

    private static String toJsonMap(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(esc(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(esc(String.valueOf(value))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
