package com.javaplatform.dto;

import lombok.Data;

@Data
public class CompileRequest {
    private String sourceCode;
    private String className;
    private String input;
}
