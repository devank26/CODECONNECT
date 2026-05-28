package com.javaplatform;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    private static ThemeManager instance;
    private boolean isDarkMode = true; // Default to coder-centric dark

    private final List<Runnable> listeners = new ArrayList<>();

    private ThemeManager() {}

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }

    public void toggleTheme() {
        isDarkMode = !isDarkMode;
        notifyListeners();
    }

    public void addThemeListener(Runnable r) {
        listeners.add(r);
    }

    public void removeThemeListener(Runnable r) {
        listeners.remove(r);
    }

    private void notifyListeners() {
        for (Runnable r : new ArrayList<>(listeners)) {
            try {
                r.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ── Color Tokens (Hex Strings) ───────────────────────────────────────────
    public String bgApp() { return isDarkMode ? "#08090f" : "#f4f6fa"; }
    public String bgCard() { return isDarkMode ? "#121420" : "#ffffff"; }
    public String bgInput() { return isDarkMode ? "#1b1d2a" : "#e9ecf3"; }
    
    public String textPrimary() { return isDarkMode ? "#ffffff" : "#1e2538"; }
    public String textMuted() { return isDarkMode ? "#8c95b2" : "#64748b"; }
    public String border() { return isDarkMode ? "#2b304a" : "#d2d6e4"; }

    // Neon colors vs Soft colors
    public String accent() { return isDarkMode ? "#00f0ff" : "#0066cc"; } // Neon Blue vs Standard Blue
    public String accentHover() { return isDarkMode ? "#00b8d4" : "#0052a3"; }
    public String cyan() { return isDarkMode ? "#00ffff" : "#0ea5e9"; }
    
    public String runColor() { return isDarkMode ? "#00f0ff" : "#0066cc"; } // Run button - Neon Blue
    public String runColorHover() { return isDarkMode ? "#00b8d4" : "#0052a3"; }
    
    public String errorColor() { return isDarkMode ? "#ff0055" : "#d92638"; } // Neon Red vs Red
    public String warningColor() { return isDarkMode ? "#ffd700" : "#c68a00"; } // Neon Gold vs Gold

    // Chat Bubbles
    public String chatBubbleMeBg() { return isDarkMode ? "#0052cc" : "#e0f2fe"; }
    public String chatBubbleMeFg() { return isDarkMode ? "#ffffff" : "#0066cc"; }
    public String chatBubblePeerBg() { return isDarkMode ? "#1e2235" : "#f1f5f9"; }
    public String chatBubblePeerFg() { return isDarkMode ? "#f1f3f9" : "#1e293b"; }

    // ── Pre-formatted CSS Rules ─────────────────────────────────────────────
    public String getAppStyle() {
        return "-fx-background-color: " + bgApp() + ";";
    }

    public String getCardStyle() {
        return "-fx-background-color: " + bgCard() + "; -fx-border-color: " + border() + "; -fx-border-width: 0 0 1 0;";
    }

    public String getTextAreaStyle(String fontFamily, int size) {
        return String.format(
                "-fx-control-inner-background: %s; -fx-text-fill: %s; " +
                "-fx-font-family: '%s'; -fx-font-size: %dpx; " +
                "-fx-background-color: %s; -fx-border-color: %s; " +
                "-fx-border-radius: 8; -fx-background-radius: 8;",
                bgInput(), textPrimary(), fontFamily, size, bgInput(), border());
    }

    public String getTextFieldStyle(int size) {
        return String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; " +
                "-fx-prompt-text-fill: %s; -fx-border-color: %s; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-font-size: %dpx; -fx-padding: 8 12;",
                bgInput(), textPrimary(), textMuted(), border(), size);
    }

    public String getButtonStyle(String colorHex) {
        return String.format(
                "-fx-background-color: %s; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-padding: 9 18; -fx-cursor: hand;",
                colorHex);
    }

    public String getLabelStyle(int size, boolean bold, boolean isMuted) {
        return String.format(
                "-fx-text-fill: %s; -fx-font-size: %dpx; -fx-font-family: 'Segoe UI'; %s",
                isMuted ? textMuted() : textPrimary(), size, bold ? "-fx-font-weight: bold;" : "");
    }
}
