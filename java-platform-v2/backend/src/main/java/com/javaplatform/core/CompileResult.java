package com.javaplatform.core;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class CompileResult {
    private boolean compiled;
    private String output;
    private List<String> errors;
    private List<String> errorExplanations;
    private long executionTime;
    private boolean success;
    
    public CompileResult() {
        this.output = "";
        this.errors = new ArrayList<>();
        this.errorExplanations = new ArrayList<>();
        this.executionTime = 0;
        this.success = false;
        this.compiled = false;
    }
}
