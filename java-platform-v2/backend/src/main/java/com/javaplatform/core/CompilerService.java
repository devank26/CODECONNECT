package com.javaplatform.core;

import jakarta.tools.*;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class CompilerService {
    
    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private static final StandardJavaFileManager fileManager = 
        compiler.getStandardFileManager(null, null, null);
    
    private final SandboxExecutor sandboxExecutor;
    private final ErrorAnalyzer errorAnalyzer;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    public CompilerService(SandboxExecutor sandboxExecutor, ErrorAnalyzer errorAnalyzer) {
        this.sandboxExecutor = sandboxExecutor;
        this.errorAnalyzer = errorAnalyzer;
    }
    
    /**
     * Compiles Java source code and returns result with errors/output
     */
    public CompileResult compile(String sourceCode, String className, String userInput) {
        CompileResult result = new CompileResult();
        
        try {
            // Step 1: Write source to temp file
            Path tempDir = Files.createTempDirectory("java-compile-");
            Path sourceFile = tempDir.resolve(className + ".java");
            Files.writeString(sourceFile, sourceCode);
            
            // Step 2: Compile
            List<String> options = Arrays.asList(
                "-d", tempDir.toString(),
                "-encoding", "UTF-8"
            );
            
            Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromFiles(
                    Collections.singletonList(sourceFile.toFile())
                );
            
            DiagnosticCollector<JavaFileObject> diagnostics = 
                new DiagnosticCollector<>();
            
            boolean success = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                options,
                null,
                compilationUnits
            ).call();
            
            // Step 3: Handle compilation errors
            if (!success) {
                List<String> errors = new ArrayList<>();
                for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                    String errorMsg = String.format(
                        "Line %d: %s",
                        diagnostic.getLineNumber(),
                        diagnostic.getMessage(Locale.getDefault())
                    );
                    errors.add(errorMsg);
                }
                
                result.setCompiled(false);
                result.setErrors(errors);
                result.setErrorExplanations(errorAnalyzer.analyzeErrors(errors));
                return result;
            }
            
            // Step 4: Execute compiled code (with timeout)
            result.setCompiled(true);
            sandboxExecutor.executeCode(
                tempDir.toString(),
                className,
                userInput,
                result
            );
            
            result.setSuccess(true);
            return result;
            
        } catch (IOException e) {
            result.setCompiled(false);
            result.setErrors(Arrays.asList("IO Error: " + e.getMessage()));
            log.error("Compilation error", e);
            return result;
        }
    }
    
    /**
     * Compiles asynchronously
     */
    public CompletableFuture<CompileResult> compileAsync(
        String sourceCode, String className, String userInput) {
        return CompletableFuture.supplyAsync(
            () -> compile(sourceCode, className, userInput),
            executorService
        );
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}
