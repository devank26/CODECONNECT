package com.javaplatform.service;

import com.javaplatform.core.ErrorAnalyzer;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.regex.*;

@Slf4j
@Service
public class AIAssistantService {
    
    @Autowired
    private ErrorAnalyzer errorAnalyzer;
    
    /**
     * Generate helpful suggestions for error messages
     */
    public Map<String, Object> analyzeError(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("error", errorMessage);
        response.put("explanation", errorAnalyzer.analyzeRuntimeError(errorMessage));
        response.put("suggestions", errorAnalyzer.suggestFixes(errorMessage));
        response.put("category", categorizeError(errorMessage));
        
        return response;
    }
    
    /**
     * Categorize error types
     */
    public String categorizeError(String error) {
        if (error.contains("Exception")) return "RUNTIME_ERROR";
        if (error.contains("Error")) return "SYSTEM_ERROR";
        if (error.contains("error:")) return "COMPILATION_ERROR";
        return "UNKNOWN";
    }
    
    /**
     * Get Java documentation link for a class/method
     */
    public String getDocumentationLink(String className) {
        return String.format(
            "https://docs.oracle.com/en/java/javase/21/docs/api/%s.html",
            className.replace(".", "/")
        );
    }
    
    /**
     * Provide code improvement suggestions
     */
    public List<String> suggestCodeImprovements(String sourceCode) {
        List<String> suggestions = new ArrayList<>();
        
        // Check for best practices
        if (sourceCode.contains("== null")) {
            suggestions.add("💡 Consider using Objects.isNull() or Optional instead of == null");
        }
        
        if (sourceCode.contains("System.out.println")) {
            suggestions.add("💡 Use a logging framework (SLF4J, Log4j) instead of System.out.println");
        }
        
        if (sourceCode.contains("catch (Exception e)")) {
            suggestions.add("💡 Avoid catching generic Exception. Catch specific exceptions instead");
        }
        
        if (!sourceCode.contains("@Override")) {
            suggestions.add("💡 Use @Override annotation when overriding methods");
        }
        
        if (sourceCode.contains("new Thread")) {
            suggestions.add("💡 Consider using ExecutorService instead of direct Thread creation");
        }
        
        if (sourceCode.toLowerCase().contains("synchronized")) {
            suggestions.add("💡 Prefer concurrent collections over synchronized keyword");
        }
        
        return suggestions;
    }
    
    /**
     * Get Java documentation for common methods
     */
    public Map<String, String> getCommonMethodDocs() {
        Map<String, String> docs = new HashMap<>();
        
        docs.put("String.length()", 
            "Returns the length of the string. For empty string, returns 0.");
        docs.put("String.charAt(int)", 
            "Returns the character at the specified index (0-based).");
        docs.put("String.substring(int, int)", 
            "Returns substring from start index (inclusive) to end index (exclusive).");
        docs.put("String.indexOf(String)", 
            "Returns the index of first occurrence of substring, or -1 if not found.");
        docs.put("String.replace(char, char)", 
            "Returns new string with all occurrences of old character replaced.");
        docs.put("String.split(String)", 
            "Splits string by regex pattern and returns array of strings.");
        docs.put("ArrayList.add(E)", 
            "Adds element to end of list. Resizes if necessary.");
        docs.put("ArrayList.get(int)", 
            "Returns element at specified index. Throws exception if out of bounds.");
        docs.put("HashMap.put(K, V)", 
            "Associates specified value with key. Overwrites if key already exists.");
        docs.put("HashMap.get(Object)", 
            "Returns value for key, or null if key doesn't exist.");
        
        return docs;
    }
    
    /**
     * Explain Java concept briefly
     */
    public String explainConcept(String concept) {
        Map<String, String> explanations = new HashMap<>();
        
        explanations.put("polymorphism", 
            "Ability of objects to take multiple forms. Allows same method call to behave differently based on object type.");
        explanations.put("encapsulation", 
            "Bundling data and methods together, hiding internal details from outside. Use private/public keywords.");
        explanations.put("inheritance", 
            "Creating new classes based on existing classes. Use 'extends' keyword. Subclass inherits methods from superclass.");
        explanations.put("abstraction", 
            "Hiding implementation details and showing only essential features. Use abstract classes/interfaces.");
        explanations.put("interface", 
            "Contract that defines what a class should do. Use 'implements' keyword to implement interface.");
        explanations.put("exception", 
            "Event that disrupts normal program flow. Use try-catch to handle exceptions gracefully.");
        explanations.put("generics", 
            "Allows classes/methods to work with different data types safely. Use angle brackets: List<String>");
        explanations.put("lambda", 
            "Anonymous function that provides concise syntax for functional interfaces. Use -> operator.");
        explanations.put("stream", 
            "Functional programming way to process collections. Use .stream().map().filter().collect()");
        explanations.put("recursion", 
            "Function calling itself. Must have base case to stop, otherwise causes StackOverflowError.");
        
        return explanations.getOrDefault(
            concept.toLowerCase(),
            "No explanation available for: " + concept
        );
    }
}
