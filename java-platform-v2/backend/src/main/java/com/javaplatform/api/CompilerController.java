package com.javaplatform.api;

import com.javaplatform.core.CompileResult;
import com.javaplatform.service.CodeCompilationService;
import com.javaplatform.util.JwtTokenProvider;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/compile")
@CrossOrigin("*")
public class CompilerController {
    
    @Autowired
    private CodeCompilationService codeCompilationService;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    /**
     * Compile and run Java code
     */
    @PostMapping("/run")
    public Map<String, Object> compileAndRun(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody Map<String, String> request) {
        
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            
            String sourceCode = request.get("sourceCode");
            String className = request.get("className");
            String userInput = request.getOrDefault("input", "");
            
            // Validate input
            if (sourceCode == null || sourceCode.trim().isEmpty()) {
                return errorResponse("Source code is required");
            }
            if (className == null || className.trim().isEmpty()) {
                return errorResponse("Class name is required");
            }
            
            // Compile and execute
            CompileResult result = codeCompilationService.compileAndRun(
                Long.parseLong(userId), 
                sourceCode, 
                className, 
                userInput
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("compiled", result.isCompiled());
            response.put("output", result.getOutput());
            response.put("errors", result.getErrors());
            response.put("errorExplanations", result.getErrorExplanations());
            response.put("executionTime", result.getExecutionTime());
            
            return response;
        } catch (Exception e) {
            log.error("Compilation error", e);
            return errorResponse("Compilation failed: " + e.getMessage());
        }
    }
    
    /**
     * Get user's execution history
     */
    @GetMapping("/history")
    public Map<String, Object> getExecutionHistory(
        @RequestHeader("Authorization") String authHeader) {
        
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            
            var executions = codeCompilationService.getUserExecutions(Long.parseLong(userId));
            var stats = codeCompilationService.getUserStats(Long.parseLong(userId));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("executions", executions);
            response.put("stats", stats);
            
            return response;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }
    
    /**
     * Get user statistics
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats(
        @RequestHeader("Authorization") String authHeader) {
        
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            
            var stats = codeCompilationService.getUserStats(Long.parseLong(userId));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.putAll(stats);
            
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
