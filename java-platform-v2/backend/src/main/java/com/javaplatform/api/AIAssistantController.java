package com.javaplatform.api;

import com.javaplatform.service.AIAssistantService;
import com.javaplatform.util.JwtTokenProvider;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin("*")
public class AIAssistantController {
    
    @Autowired
    private AIAssistantService aiAssistantService;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    /**
     * Analyze error and get suggestions
     */
    @PostMapping("/analyze-error")
    public Map<String, Object> analyzeError(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody Map<String, String> request) {
        
        try {
            String error = request.get("error");
            
            if (error == null || error.trim().isEmpty()) {
                return errorResponse("Error message is required");
            }
            
            var analysis = aiAssistantService.analyzeError(error);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.putAll(analysis);
            
            return response;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }
    
    /**
     * Get code improvement suggestions
     */
    @PostMapping("/suggest-improvements")
    public Map<String, Object> suggestImprovements(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody Map<String, String> request) {
        
        try {
            String sourceCode = request.get("sourceCode");
            
            if (sourceCode == null || sourceCode.trim().isEmpty()) {
                return errorResponse("Source code is required");
            }
            
            var suggestions = aiAssistantService.suggestCodeImprovements(sourceCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("suggestions", suggestions);
            
            return response;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }
    
    /**
     * Explain Java concept
     */
    @GetMapping("/explain/{concept}")
    public Map<String, Object> explainConcept(
        @PathVariable String concept,
        @RequestHeader("Authorization") String authHeader) {
        
        try {
            String explanation = aiAssistantService.explainConcept(concept);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("concept", concept);
            response.put("explanation", explanation);
            
            return response;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }
    
    /**
     * Get documentation
     */
    @GetMapping("/docs/{className}")
    public Map<String, Object> getDocumentation(
        @PathVariable String className,
        @RequestHeader("Authorization") String authHeader) {
        
        try {
            String link = aiAssistantService.getDocumentationLink(className);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("className", className);
            response.put("documentationLink", link);
            
            return response;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }
    
    /**
     * Get common methods reference
     */
    @GetMapping("/common-methods")
    public Map<String, Object> getCommonMethods(
        @RequestHeader("Authorization") String authHeader) {
        
        try {
            var methods = aiAssistantService.getCommonMethodDocs();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("methods", methods);
            
            return response;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }
    
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}
