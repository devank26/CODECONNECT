package com.javaplatform.service;

/**
 * Holds the result from CompilerService.
 */
public class CompilerResult {

    private final String output;
    private final String rawErrors;
    private final String friendlyHint;
    private final int    exitCode;
    private final long   executionTimeMs;

    public CompilerResult(String output, String rawErrors,
                          String friendlyHint, int exitCode, long executionTimeMs) {
        this.output          = output;
        this.rawErrors       = rawErrors;
        this.friendlyHint    = friendlyHint;
        this.exitCode        = exitCode;
        this.executionTimeMs = executionTimeMs;
    }

    public boolean isSuccess()         { return exitCode == 0 && rawErrors.isEmpty(); }
    public String  getOutput()         { return output; }
    public String  getRawErrors()      { return rawErrors; }
    public String  getFriendlyHint()   { return friendlyHint; }
    public int     getExitCode()       { return exitCode; }
    public long    getExecutionTimeMs(){ return executionTimeMs; }
}
