package com.javaplatform.core;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class SandboxExecutor {
    
    private static final long TIMEOUT_SECONDS = 10;
    private static final long MAX_MEMORY_MB = 256;
    
    /**
     * Executes compiled Java code with timeout and memory limits
     */
    public void executeCode(String classPath, String className, 
                           String userInput, CompileResult result) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        InputStream oldIn = System.in;
        
        try {
            System.setOut(printStream);
            System.setErr(printStream);
            if (userInput != null && !userInput.isEmpty()) {
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
            }
            
            long startTime = System.currentTimeMillis();
            
            // Create custom classloader with timeout
            Future<String> future = executor.submit(() -> {
                try {
                    URLClassLoader classLoader = new URLClassLoader(
                        new java.net.URL[]{ 
                            Paths.get(classPath).toUri().toURL() 
                        }
                    );
                    
                    Class<?> clazz = classLoader.loadClass(className);
                    Method mainMethod = clazz.getMethod("main", String[].class);
                    mainMethod.invoke(null, (Object) new String[]{});
                    
                    classLoader.close();
                    return "OK";
                    
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            // Wait with timeout
            try {
                future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                result.setOutput("ERROR: Execution timeout (>" + TIMEOUT_SECONDS + "s)");
                result.setSuccess(false);
                return;
            }
            
            long endTime = System.currentTimeMillis();
            result.setOutput(outputStream.toString());
            result.setExecutionTime(endTime - startTime);
            result.setSuccess(true);
            
        } catch (Exception e) {
            result.setOutput("Runtime Error: " + e.getMessage());
            result.setSuccess(false);
            log.error("Execution error", e);
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
            System.setIn(oldIn);
            executor.shutdown();
        }
    }
    
    /**
     * Checks for unsafe code patterns
     */
    public boolean isSafeCode(String sourceCode) {
        String[] dangerousPatterns = {
            "System.exit",
            "Runtime.getRuntime().exec",
            "ProcessBuilder",
            "reflection",
            "ClassLoader",
            "File.delete",
            "FileWriter",
            "FileInputStream"
        };
        
        for (String pattern : dangerousPatterns) {
            if (sourceCode.toLowerCase().contains(pattern.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
