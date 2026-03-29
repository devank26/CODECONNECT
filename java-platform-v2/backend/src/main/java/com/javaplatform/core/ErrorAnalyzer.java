package com.javaplatform.core;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.regex.*;

@Slf4j
@Service
public class ErrorAnalyzer {
    
    private static final Map<String, String> ERROR_PATTERNS = new HashMap<>();
    
    static {
        // Common compilation errors
        ERROR_PATTERNS.put(
            "cannot find symbol",
            "The variable or method you're using hasn't been defined. Check spelling and scope."
        );
        ERROR_PATTERNS.put(
            "class.*is public.*should be declared in a file named",
            "Public class name must match the filename (without .java extension)."
        );
        ERROR_PATTERNS.put(
            "unexpected token",
            "Syntax error. Check for missing semicolons, braces, or parentheses."
        );
        ERROR_PATTERNS.put(
            "method.*cannot be applied",
            "The method you called doesn't match any signatures. Check parameter types."
        );
        ERROR_PATTERNS.put(
            "cannot assign",
            "Type mismatch. You're trying to assign incompatible types."
        );
        ERROR_PATTERNS.put(
            "possible loss of precision",
            "Implicit conversion would lose data. Use explicit casting if needed."
        );
        ERROR_PATTERNS.put(
            "non-static variable.*cannot be referenced from a static context",
            "Instance variables must be accessed from object instances, not static methods."
        );
        ERROR_PATTERNS.put(
            "variable.*might not have been initialized",
            "Use a variable before assigning it a value. Initialize it before use."
        );
        ERROR_PATTERNS.put(
            "incompatible types",
            "Type mismatch between expected and actual types."
        );
        ERROR_PATTERNS.put(
            "';' expected",
            "Missing semicolon at end of statement."
        );
        ERROR_PATTERNS.put(
            "reached end of file while parsing",
            "Missing closing brace } somewhere in your code."
        );
        ERROR_PATTERNS.put(
            "'{' expected",
            "Missing opening brace { (probably after method or class declaration)."
        );
    }
    
    /**
     * Analyzes error messages and provides helpful explanations
     */
    public List<String> analyzeErrors(List<String> errors) {
        List<String> explanations = new ArrayList<>();
        
        for (String error : errors) {
            String explanation = findExplanation(error);
            explanations.add(explanation);
        }
        
        return explanations;
    }
    
    /**
     * Finds explanation for a specific error
     */
    private String findExplanation(String error) {
        for (Map.Entry<String, String> pattern : ERROR_PATTERNS.entrySet()) {
            if (error.toLowerCase().contains(pattern.getKey().toLowerCase())) {
                return "💡 " + pattern.getValue();
            }
        }
        return "❓ Refer to Java documentation for more details on this error.";
    }
    
    /**
     * Analyzes runtime errors
     */
    public String analyzeRuntimeError(String errorMessage) {
        if (errorMessage.contains("NullPointerException")) {
            return "💡 A NullPointerException means you're trying to use an object that hasn't been created. Check where you might be calling methods on null values.";
        }
        if (errorMessage.contains("ArrayIndexOutOfBoundsException")) {
            return "💡 You're trying to access an array index that doesn't exist. Check your loop bounds.";
        }
        if (errorMessage.contains("StringIndexOutOfBoundsException")) {
            return "💡 You're trying to access a string character at an invalid position.";
        }
        if (errorMessage.contains("NumberFormatException")) {
            return "💡 You're trying to convert a non-numeric string to a number. Check the input format.";
        }
        if (errorMessage.contains("ClassCastException")) {
            return "💡 You're trying to cast an object to an incompatible type.";
        }
        if (errorMessage.contains("StackOverflowError")) {
            return "💡 Infinite recursion detected. Check your recursive method's base case.";
        }
        if (errorMessage.contains("OutOfMemoryError")) {
            return "💡 The program has used all available memory. Check for memory leaks or infinitely growing data structures.";
        }
        return "❓ Unknown runtime error. Check the stack trace above for details.";
    }
    
    /**
     * Suggests fixes based on error patterns
     */
    public List<String> suggestFixes(String error) {
        List<String> suggestions = new ArrayList<>();
        
        if (error.contains("cannot find symbol")) {
            suggestions.add("• Check if the variable/method is spelled correctly");
            suggestions.add("• Ensure the variable is declared before use");
            suggestions.add("• Import the required class if using external libraries");
        } else if (error.contains("unexpected token")) {
            suggestions.add("• Look for missing or extra semicolons");
            suggestions.add("• Check bracket/brace matching");
            suggestions.add("• Ensure proper method syntax");
        } else if (error.contains("cannot assign")) {
            suggestions.add("• Check that types match exactly");
            suggestions.add("• Use explicit casting if needed");
            suggestions.add("• Review Java type compatibility rules");
        }
        
        return suggestions;
    }
}
