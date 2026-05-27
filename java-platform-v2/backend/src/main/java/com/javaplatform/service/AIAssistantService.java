package com.javaplatform.service;

import com.javaplatform.core.ErrorAnalyzer;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
@Service
public class AIAssistantService {
    
    @Autowired
    private UniversalAIProvider aiProvider;
    
    @Autowired
    private ErrorAnalyzer errorAnalyzer;
    
    /**
     * Generate helpful suggestions for error messages using Real AI
     */
    public Map<String, Object> analyzeError(String errorMessage) {
        String prompt = "You are an expert Java developer. Analyze the following error, explain why it happened, and suggest a fix:\n\n" + errorMessage;
        String aiResponse = extractJsonContent(aiProvider.generateResponse(prompt));
        
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
        String aiResponse = extractJsonContent(aiProvider.generateResponse(prompt));
        
        return Arrays.asList(aiResponse.split("\n"));
    }
    
    /**
     * Explain Java concept briefly using Real AI
     */
    public String explainConcept(String concept) {
        String prompt = "Explain the following Java concept briefly and clearly to a beginner:\n\n" + concept;
        return extractJsonContent(aiProvider.generateResponse(prompt));
    }
    
    /**
     * Generic query endpoint allowing custom prompts and modes
     */
    public String genericQuery(String query, String mode) {
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
        
        return extractJsonContent(aiProvider.generateResponse(prompt));
    }
    
    /**
     * Parses the naive JSON string to extract the content field
     */
    private String extractJsonContent(String raw) {
        if (raw.startsWith("[ERROR]")) {
            return raw;
        }

        StringBuilder result = new StringBuilder();
        String[] lines = raw.split("\n");
        for (String line : lines) {
            if (line.contains("\"content\":\"")) {
                try {
                    String part = line.split("\"content\":\"")[1].split("\"")[0];
                    result.append(part);
                } catch (Exception ignored) {}
            }
        }
        
        if (result.length() == 0) {
            return "Failed to parse JSON. Raw response:\n" + raw;
        }

        return result.toString()
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }
    
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
