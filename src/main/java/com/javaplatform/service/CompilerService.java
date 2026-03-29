package com.javaplatform.service;

import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Compiles and runs Java source code supplied as a String.
 * Uses javax.tools (JDK Compiler API) for compilation and
 * a sandboxed ProcessBuilder subprocess for execution.
 */
public class CompilerService {

    private static final int TIMEOUT_SECONDS = 10;

    /**
     * Compile and run the given Java source code.
     *
     * @param sourceCode Full Java source code (must contain a public class)
     * @param stdin      Optional stdin to feed the program (may be null)
     * @return CompilerResult with output, errors, and friendly hints
     */
    public static CompilerResult compileAndRun(String sourceCode, String stdin) {
        Path tempDir = null;
        long startTime = System.currentTimeMillis();

        try {
            // ── 1. Extract class name from source ──────────────────────────
            String className = extractClassName(sourceCode);
            if (className == null) {
                String err = "error: Cannot determine public class name.\n" +
                             "Make sure your class is declared as: public class ClassName { ... }";
                return new CompilerResult("", err, ErrorAnalyser.analyse(err), 1, 0);
            }

            // ── 2. Write source to temp directory ─────────────────────────
            tempDir = Files.createTempDirectory("javaplatform_");
            Path sourceFile = tempDir.resolve(className + ".java");
            Files.writeString(sourceFile, sourceCode);

            // ── 3. Compile with javax.tools ───────────────────────────────
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                String javaHome = System.getProperty("java.home");
                String err = "error: Java compiler (javac) not found.\n" +
                             "Currently running on: " + javaHome + "\n" +
                             "Please ensure you have a JDK installed and JAVA_HOME is configured.\n" +
                             "Hint: A JRE is not enough for code compilation.";
                return new CompilerResult("", err, ErrorAnalyser.analyse(err), 1, 0);
            }

            StringWriter compileErrors = new StringWriter();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, null)) {
                Iterable<? extends JavaFileObject> units =
                        fm.getJavaFileObjects(sourceFile.toFile());

                List<String> options = Arrays.asList("-d", tempDir.toString());
                JavaCompiler.CompilationTask task =
                        compiler.getTask(compileErrors, fm, diagnostics, options, null, units);

                boolean   success = task.call();
                if (!success) {
                    StringBuilder sb = new StringBuilder();
                    for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                        if (d.getKind() == Diagnostic.Kind.ERROR ||
                            d.getKind() == Diagnostic.Kind.WARNING) {
                            sb.append(d.getKind())
                              .append(" at line ").append(d.getLineNumber())
                              .append(": ").append(d.getMessage(null))
                              .append("\n");
                        }
                    }
                    String errors = sb.toString();
                    long elapsed = System.currentTimeMillis() - startTime;
                    return new CompilerResult("", errors, ErrorAnalyser.analyse(errors), 1, elapsed);
                }
            }

            // ── 4. Execute in subprocess with timeout ─────────────────────
            String javaExe = ProcessHandle.current().info().command()
                    .orElse("java");
            // Use same java executable that's running us
            ProcessBuilder pb = new ProcessBuilder(
                    javaExe, "-cp", tempDir.toString(), className);
            pb.directory(tempDir.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Feed stdin
            if (stdin != null && !stdin.isBlank()) {
                try (PrintWriter pw = new PrintWriter(process.getOutputStream())) {
                    pw.print(stdin);
                }
            } else {
                process.getOutputStream().close();
            }

            // Read stdout / stderr in parallel
            Future<String> stdoutFuture = readStreamAsync(process.getInputStream());
            Future<String> stderrFuture = readStreamAsync(process.getErrorStream());

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                String err = "Runtime Error: Program exceeded time limit (" + TIMEOUT_SECONDS + " seconds).\n" +
                             "This may indicate an infinite loop.";
                long elapsed = System.currentTimeMillis() - startTime;
                return new CompilerResult("", err, ErrorAnalyser.analyse(err), 1, elapsed);
            }

            String output   = stdoutFuture.get(2, TimeUnit.SECONDS);
            String errOutput = stderrFuture.get(2, TimeUnit.SECONDS);
            int    exitCode  = process.exitValue();
            long   elapsed   = System.currentTimeMillis() - startTime;

            String hint = ErrorAnalyser.analyse(errOutput);
            return new CompilerResult(output, errOutput, hint, exitCode, elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String err = "Internal error: " + e.getMessage();
            return new CompilerResult("", err, ErrorAnalyser.analyse(err), 1, elapsed);
        } finally {
            // Clean up temp directory
            if (tempDir != null) {
                try {
                    deleteDir(tempDir.toFile());
                } catch (Exception ignored) {}
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Extracts the public class name from source using simple regex. */
    private static String extractClassName(String source) {
        // Match: public class Foo  or  public   class   Foo
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("public\\s+class\\s+(\\w+)").matcher(source);
        if (m.find()) return m.group(1);
        // Fall back to first class
        m = java.util.regex.Pattern.compile("(?:^|\\s)class\\s+(\\w+)").matcher(source);
        if (m.find()) return m.group(1);
        return null;
    }

    private static final ExecutorService EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "stream-reader");
        t.setDaemon(true);
        return t;
    });

    private static Future<String> readStreamAsync(InputStream is) {
        return EXEC.submit(() -> {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            return sb.toString();
        });
    }

    private static void deleteDir(File dir) {
        if (dir.isDirectory()) {
            for (File child : Objects.requireNonNull(dir.listFiles())) {
                deleteDir(child);
            }
        }
        dir.delete();
    }
}
