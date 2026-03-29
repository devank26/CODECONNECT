package com.javaplatform.service;

import com.javaplatform.core.CompileResult;
import com.javaplatform.core.CompilerService;
import com.javaplatform.core.ErrorAnalyzer;
import com.javaplatform.core.SandboxExecutor;
import com.javaplatform.model.CodeExecution;
import com.javaplatform.model.User;
import com.javaplatform.repository.CodeExecutionRepository;
import com.javaplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class CodeCompilationService {
    
    @Autowired
    private CompilerService compilerService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CodeExecutionRepository codeExecutionRepository;
    
    @Autowired
    private SandboxExecutor sandboxExecutor;
    
    @Autowired
    private ErrorAnalyzer errorAnalyzer;
    
    /**
     * Compile and execute code synchronously
     */
    public CompileResult compileAndRun(Long userId, String sourceCode, 
                                       String className, String userInput) {
        // Verify user exists
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            CompileResult result = new CompileResult();
            result.setOutput("ERROR: User not found");
            return result;
        }
        
        // Check for unsafe code
        if (!sandboxExecutor.isSafeCode(sourceCode)) {
            CompileResult result = new CompileResult();
            result.setOutput("ERROR: Code contains unsafe operations (file I/O, exec, reflection, etc.)");
            return result;
        }
        
        // Compile and execute
        CompileResult result = compilerService.compile(sourceCode, className, userInput);
        
        // Save execution history
        CodeExecution execution = new CodeExecution();
        execution.setUser(userOpt.get());
        execution.setCode(sourceCode);
        execution.setOutput(result.getOutput());
        execution.setSuccess(result.isSuccess());
        execution.setExecutionTime(result.getExecutionTime());
        
        codeExecutionRepository.save(execution);
        
        log.info("Code executed for user {}: success={}, time={}ms", 
                 userId, result.isSuccess(), result.getExecutionTime());
        
        return result;
    }
    
    /**
     * Compile and run asynchronously (for web requests)
     */
    @Async
    public CompletableFuture<CompileResult> compileAndRunAsync(
        Long userId, String sourceCode, String className, String userInput) {
        
        return CompletableFuture.supplyAsync(
            () -> compileAndRun(userId, sourceCode, className, userInput)
        );
    }
    
    /**
     * Get execution history for user
     */
    public List<CodeExecution> getUserExecutions(Long userId) {
        return codeExecutionRepository.findByUserId(userId);
    }
    
    /**
     * Get successful executions only
     */
    public List<CodeExecution> getSuccessfulExecutions(Long userId) {
        return codeExecutionRepository.findByUserIdAndSuccessTrue(userId);
    }
    
    /**
     * Get user statistics
     */
    public Map<String, Long> getUserStats(Long userId) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalExecutions", 
                 codeExecutionRepository.countByUserId(userId));
        stats.put("successfulExecutions", 
                 codeExecutionRepository.countByUserIdAndSuccessTrue(userId));
        stats.put("failedExecutions", 
                 stats.get("totalExecutions") - stats.get("successfulExecutions"));
        return stats;
    }
    
    /**
     * Clear execution history for user
     */
    @Async
    public CompletableFuture<Void> clearExecutionHistory(Long userId) {
        List<CodeExecution> executions = getUserExecutions(userId);
        codeExecutionRepository.deleteAll(executions);
        log.info("Cleared {} executions for user {}", executions.size(), userId);
        return CompletableFuture.completedFuture(null);
    }
}
