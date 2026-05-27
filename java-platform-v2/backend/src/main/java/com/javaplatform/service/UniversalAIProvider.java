package com.javaplatform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Slf4j
@Service
public class UniversalAIProvider {

    @Value("${ai.url:http://localhost:11434/api/chat}")
    private String urlStr;

    @Value("${ai.model:phi3}")
    private String model;

    @Value("${ai.api_key:}")
    private String apiKey;

    @Value("${ai.startup_command:}")
    private String startupCommand;

    public String generateResponse(String prompt) {
        try {
            return sendHttpRequest(prompt);
        } catch (java.net.ConnectException e) {
            if (startupCommand != null && !startupCommand.trim().isEmpty()) {
                log.info("Connection failed. Attempting to start local AI provider using: " + startupCommand);
                try {
                    ProcessBuilder pb = new ProcessBuilder(startupCommand.split(" "));
                    pb.start();
                    log.info("Waiting 5 seconds for the AI provider to start up...");
                    Thread.sleep(5000);
                    
                    return sendHttpRequest(prompt);
                } catch (Exception retryException) {
                    return "[ERROR] Failed to auto-start the local AI provider: " + retryException.getMessage();
                }
            } else {
                return "[ERROR] Connection refused. The AI provider is unreachable.";
            }
        } catch (Exception e) {
            return "[ERROR] " + e.getMessage();
        }
    }

    private String sendHttpRequest(String prompt) throws Exception {
        URL url = new URI(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        
        conn.setDoOutput(true);

        String safePrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");

        String json = "{"
                + "\"model\": \"" + model + "\","
                + "\"messages\": ["
                + "{ \"role\": \"user\", \"content\": \"" + safePrompt + "\" }"
                + "]"
                + "}";

        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes());
        os.flush();

        if (conn.getResponseCode() != 200) {
            return "[ERROR] HTTP " + conn.getResponseCode();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line).append("\n");
        }
        return response.toString();
    }
}
