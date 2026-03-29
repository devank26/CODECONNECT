package com.javaplatform.service;

import java.util.*;

/**
 * Analyses common Java compiler and runtime error messages
 * and produces beginner-friendly explanations.
 */
public class ErrorAnalyser {

    private static final List<Rule> RULES = new ArrayList<>();

    static {
        // ── Compilation errors ─────────────────────────────────────────────
        add("cannot find symbol",
            "🔍 Cannot Find Symbol\n" +
            "Java cannot find a variable, method, or class you referenced.\n" +
            "• Check spelling (Java is case-sensitive)\n" +
            "• Make sure the variable is declared before use\n" +
            "• Verify the class/method exists in scope");

        add("reached end of file",
            "🔧 Missing Closing Brace\n" +
            "The compiler hit the end of your file before finding a matching '}'.\n" +
            "• Count your '{' and '}' characters — they must match\n" +
            "• Every class and method needs an opening AND closing brace");

        add("';' expected",
            "⚠ Missing Semicolon\n" +
            "A statement is missing its semicolon at the end.\n" +
            "• Every statement in Java must end with ';'\n" +
            "• Example fix: System.out.println(\"hi\");");

        add("class.*public.*should be in",
            "📁 File / Class Name Mismatch\n" +
            "Your public class name must match your file name exactly.\n" +
            "• If the class is 'public class Hello', the file must be 'Hello.java'\n" +
            "• Java is case-sensitive — 'hello' ≠ 'Hello'");

        add("incompatible types",
            "🔄 Type Mismatch\n" +
            "You are assigning a value of the wrong type to a variable.\n" +
            "• Check that both sides of '=' have the same (or compatible) type\n" +
            "• Use casting if intentional: (int) myDouble");

        add("possible loss of precision",
            "🔢 Precision Loss\n" +
            "Assigning a larger type (e.g. double) into a smaller type (e.g. int).\n" +
            "• Use an explicit cast: int x = (int) myDouble;");

        add("variable .* might not have been initialized",
            "⚠ Uninitialised Variable\n" +
            "You declared a variable but never assigned it a value before using it.\n" +
            "• Initialise it: int count = 0;");

        add("method .* is not applicable",
            "🔧 Wrong Method Arguments\n" +
            "The arguments you passed to a method don't match its parameters.\n" +
            "• Check the method signature and the types/order of arguments");

        // ── Runtime errors ─────────────────────────────────────────────────
        add("NullPointerException",
            "💥 NullPointerException\n" +
            "You called a method or accessed a field on an object that is null.\n" +
            "• Add a null check: if (obj != null) { ... }\n" +
            "• Make sure objects are properly initialised before use");

        add("ArrayIndexOutOfBoundsException",
            "📏 Array Index Out of Bounds\n" +
            "You accessed an index that doesn't exist in the array.\n" +
            "• Array indices start at 0 and end at length-1\n" +
            "• Loop condition should be: i < array.length  (not <=)");

        add("StringIndexOutOfBoundsException",
            "🔤 String Index Out of Bounds\n" +
            "You accessed a character position outside the string's length.\n" +
            "• Valid indices: 0 to str.length()-1");

        add("ClassCastException",
            "🔀 Invalid Type Cast\n" +
            "You tried to cast an object to an incompatible type.\n" +
            "• Use instanceof to check the type before casting:\n" +
            "  if (obj instanceof Dog) { Dog d = (Dog) obj; }");

        add("StackOverflowError",
            "♾ Stack Overflow\n" +
            "A method keeps calling itself (or a cycle of methods) with no end.\n" +
            "• Check your recursion for a proper base case\n" +
            "• Verify recursive calls make progress toward the base case");

        add("NumberFormatException",
            "🔢 Number Format Exception\n" +
            "You tried to convert a String to a number, but it wasn't valid.\n" +
            "• Check the string before parsing: \"123\" is ok, \"abc\" is not\n" +
            "• Wrap in a try-catch block if the input may be invalid");

        add("ArithmeticException",
            "➗ Arithmetic Exception\n" +
            "An illegal arithmetic operation occurred (most commonly division by zero).\n" +
            "• Make sure the divisor is not zero before dividing");

        add("OutOfMemoryError",
            "💾 Out of Memory\n" +
            "The JVM ran out of heap space.\n" +
            "• Look for infinite loops that keep adding to a list/array\n" +
            "• Increase heap: add  -Xmx512m  to JVM arguments");
    }

    private static void add(String pattern, String explanation) {
        RULES.add(new Rule(pattern, explanation));
    }

    /**
     * Returns a friendly hint if any known pattern matches, or a generic message.
     */
    public static String analyse(String errorText) {
        if (errorText == null || errorText.isBlank()) return "";
        for (Rule rule : RULES) {
            if (errorText.matches("(?s).*" + rule.pattern + ".*")) {
                return rule.explanation;
            }
        }
        return "❓ Unknown Error\n" +
               "Read the error message carefully — it usually tells you exactly where the problem is.\n" +
               "• Line numbers in the error point to where Java got confused\n" +
               "• Search the exact error message online for more help";
    }

    private static class Rule {
        final String pattern;
        final String explanation;
        Rule(String p, String e) { pattern = p; explanation = e; }
    }
}
