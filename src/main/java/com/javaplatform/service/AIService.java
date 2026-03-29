package com.javaplatform.service;

import org.json.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.*;
import java.time.Duration;
import java.util.Properties;

/**
 * Calls Google Gemini 1.5 Flash API to answer Java coding questions.
 * Falls back to offline rule-based hints if no API key is configured.
 *
 * Config: place a file named  config.properties  in the working directory:
 *   gemini.api.key=YOUR_KEY_HERE
 */
public class AIService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
            "gemini-1.5-flash:generateContent?key=";

    private static final String SYSTEM_PROMPT =
            "You are a friendly Java programming tutor for beginners. " +
            "When given a question or Java code with an error, provide:\n" +
            "1. A clear explanation of what is wrong or what is being asked\n" +
            "2. A corrected/improved code snippet if relevant\n" +
            "3. Tips to avoid this mistake in future\n" +
            "Keep answers concise (under 300 words). Format code in plain text.";

    private static String apiKey = null;

    static {
        loadApiKey();
    }

    private static void loadApiKey() {
        // Try config.properties in working directory
        try {
            Path cfg = Paths.get("config.properties");
            if (Files.exists(cfg)) {
                Properties p = new Properties();
                p.load(Files.newBufferedReader(cfg));
                String k = p.getProperty("gemini.api.key", "").trim();
                if (!k.isBlank() && !k.startsWith("YOUR")) {
                    apiKey = k;
                    System.out.println("[AIService] Gemini API key loaded.");
                }
            }
        } catch (Exception e) {
            System.out.println("[AIService] No config.properties found; using offline mode.");
        }
    }

    /**
     * Ask the AI a question, optionally with code context.
     * @param question The user's question
     * @param codeContext Optional Java code (may be empty)
     * @return AI answer or offline fallback
     */
    public static String ask(String question, String codeContext) {
        if (apiKey == null || apiKey.isBlank()) {
            return offlineAnswer(question, codeContext);
        }
        try {
            return callGemini(question, codeContext);
        } catch (Exception e) {
            return "⚠ Could not reach AI: " + e.getMessage() +
                   "\n\n" + offlineAnswer(question, codeContext);
        }
    }

    // ── Gemini API call ──────────────────────────────────────────────────────

    private static String callGemini(String question, String code) throws Exception {
        String prompt = SYSTEM_PROMPT + "\n\nUser question: " + question;
        if (code != null && !code.isBlank()) {
            prompt += "\n\nCode context:\n" + code;
        }

        JSONObject part    = new JSONObject().put("text", prompt);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject body    = new JSONObject().put("contents", new JSONArray().put(content));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + apiKey))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            return json.getJSONArray("candidates")
                       .getJSONObject(0)
                       .getJSONObject("content")
                       .getJSONArray("parts")
                       .getJSONObject(0)
                       .getString("text");
        } else {
            return "API Error " + response.statusCode() + ": " + response.body();
        }
    }

    // ── Offline fallback ─────────────────────────────────────────────────────

    private static String offlineAnswer(String question, String code) {
        String q = question.toLowerCase();
        StringBuilder sb = new StringBuilder();
        sb.append("🤖 Offline Mode — Gemini API key not configured.\n");
        sb.append("(Add your key to config.properties to enable full AI)\n\n");

        // Error-based hints
        if (!code.isBlank()) {
            String hint = ErrorAnalyser.analyse(code);
            if (!hint.isBlank()) {
                sb.append("Code Analysis:\n").append(hint).append("\n\n");
            }
        }

        // Keyword-based tips
        if (q.contains("null") || q.contains("npe")) {
            sb.append(ErrorAnalyser.analyse("NullPointerException"));
        } else if (q.contains("array") || q.contains("index")) {
            sb.append(ErrorAnalyser.analyse("ArrayIndexOutOfBoundsException"));
        } else if (q.contains("loop") || q.contains("for") || q.contains("while")) {
            sb.append("💡 Loop Tips:\n" +
                      "• for (int i = 0; i < arr.length; i++) — classic\n" +
                      "• for (String s : list) — enhanced for-each\n" +
                      "• while (condition) { ... break; } — add a break or progress to avoid infinite loops");
        } else if (q.contains("class") || q.contains("object")) {
            sb.append("💡 OOP Tips:\n" +
                      "• A class is a blueprint; an object is an instance\n" +
                      "• Constructor: public ClassName(params) { ... }\n" +
                      "• Use  this.field = field;  to avoid shadowing");
        } else if (q.contains("exception") || q.contains("error") || q.contains("catch")) {
            sb.append("💡 Exception Handling:\n" +
                      "• try { } catch (ExceptionType e) { } finally { }\n" +
                      "• Print details: e.getMessage() or e.printStackTrace()\n" +
                      "• Specific exceptions before general ones");
        } else {
            sb.append("💡 General Java Tip:\n" +
                      "• Read the error line number carefully\n" +
                      "• Add System.out.println() to trace values\n" +
                      "• Break the problem into smaller methods\n" +
                      "• Configure your Gemini API key for full AI-powered help!");
        }
        return sb.toString();
    }

    /** Allow runtime key update from the UI settings panel. */
    public static void setApiKey(String key) {
        apiKey = key;
    }

    public static boolean hasApiKey() { return apiKey != null && !apiKey.isBlank(); }
}
