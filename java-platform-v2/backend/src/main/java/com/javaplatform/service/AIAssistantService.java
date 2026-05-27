package com.javaplatform.service;

import com.javaplatform.core.ErrorAnalyzer;
import com.javaplatform.holyai.service.AICodeAssistant;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AIAssistantService {
    
    // Maintain stateful HolyAI sessions for the REST API
    private final Map<String, AICodeAssistant> activeSessions = new ConcurrentHashMap<>();
    
    @Autowired
    private ErrorAnalyzer errorAnalyzer;
    
    /**
     * Generate helpful suggestions for error messages using Real AI
     */
    public Map<String, Object> analyzeError(String errorMessage) {
        String prompt = "You are an expert Java developer. Analyze the following error, explain why it happened, and suggest a fix:\n\n" + errorMessage;
        
        // Single-shot stateless request
        AICodeAssistant assistant = new AICodeAssistant();
        String aiResponse = assistant.ask(prompt);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", errorMessage);
        response.put("explanation", "AI Analysis:\n" + aiResponse);
        response.put("category", categorizeError(errorMessage));
        
        return response;
    }
    
    /**
     * Categorize error types (Keep simple static categorization)
     */
    public String categorizeError(String error) {
        if (error.contains("Exception")) return "RUNTIME_ERROR";
        if (error.contains("Error")) return "SYSTEM_ERROR";
        if (error.contains("error:")) return "COMPILATION_ERROR";
        return "UNKNOWN";
    }
    
    /**
     * Provide code improvement suggestions using Real AI
     */
    public List<String> suggestCodeImprovements(String sourceCode) {
        String prompt = "You are a senior Java reviewer. Review this code and provide a bulleted list of improvements (e.g. security, performance, best practices):\n\n" + sourceCode;
        
        AICodeAssistant assistant = new AICodeAssistant();
        String aiResponse = assistant.ask(prompt);
        
        return Arrays.asList(aiResponse.split("\n"));
    }
    
    /**
     * Explain Java concept briefly using Real AI
     */
    public String explainConcept(String concept) {
        String prompt = "Explain the following Java concept briefly and clearly to a beginner:\n\n" + concept;
        AICodeAssistant assistant = new AICodeAssistant();
        return assistant.ask(prompt);
    }
    
    /**
     * Generic query endpoint allowing custom prompts and modes (Stateful for HolyAI)
     */
    public Map<String, Object> genericQuery(String query, String mode, String sessionId) {
        String systemPrompt = "You are a strict Java coding assistant. Rules:\n- Be concise\n- Explain your reasoning\n- Focus only on given input\n\n";
        String prompt = systemPrompt;
        
        switch (mode.toUpperCase()) {
            case "DEBUG":
                prompt += "Fix errors in this code:\n" + query;
                break;
            case "SECURITY":
                prompt += "Find security issues in this code:\n" + query;
                break;
            case "OPTIMIZE":
                prompt += "Optimize this code:\n" + query;
                break;
            default:
                prompt += "Answer this query:\n" + query;
                break;
        }
        
        AICodeAssistant assistant = activeSessions.computeIfAbsent(sessionId, k -> new AICodeAssistant());
        String aiResponse = assistant.ask(prompt);
        
        return processToolResponse(aiResponse, sessionId);
    }
    
    /**
     * Handle the user's approval or denial of a tool from the Web Frontend
     */
    public Map<String, Object> handleToolCallback(String sessionId, String toolJson, boolean approved) {
        AICodeAssistant assistant = activeSessions.get(sessionId);
        if (assistant == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Session expired or invalid");
            return err;
        }
        
        String nextAnswer;
        if (approved) {
            nextAnswer = assistant.executeToolAndContinue(toolJson);
        } else {
            nextAnswer = assistant.denyToolAndContinue();
        }
        
        return processToolResponse(nextAnswer, sessionId);
    }
    
    private Map<String, Object> processToolResponse(String rawResponse, String sessionId) {
        Map<String, Object> result = new HashMap<>();
        
        if (rawResponse != null && rawResponse.startsWith("[TOOL_REQUEST]")) {
            String toolJson = rawResponse.substring(14).trim();
            result.put("status", "TOOL_REQUEST");
            result.put("toolJson", toolJson);
            result.put("sessionId", sessionId);
        } else {
            result.put("status", "SUCCESS");
            result.put("response", rawResponse);
            result.put("sessionId", sessionId);
        }
        
        return result;
    }
    
    // JSON parsing removed as HolyAI's AICodeAssistant handles it internally
    
    // Keep documentation methods as static lookups to save LLM tokens/latency
    
    public String getDocumentationLink(String className) {
        return String.format(
            "https://docs.oracle.com/en/java/javase/21/docs/api/%s.html",
            className.replace(".", "/")
        );
    }
    
    public Map<String, String> getCommonMethodDocs() {
        Map<String, String> docs = new HashMap<>();
        docs.put("String.length()", "Returns the length of the string.");
        docs.put("ArrayList.add(E)", "Adds element to end of list.");
        return docs;
    }
}
