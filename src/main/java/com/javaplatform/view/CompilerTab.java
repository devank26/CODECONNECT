package com.javaplatform.view;

import com.javaplatform.SessionState;
import com.javaplatform.service.CompilerResult;
import com.javaplatform.service.CompilerService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Compiler tab — a code editor with run button, stdin, output, and error panels.
 */
public class CompilerTab extends Tab {

    private static final String SAMPLE_CODE =
            "public class Main {\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello, " +
            SessionState.getInstance().getUsername() + "! 🎉\");\n" +
            "    }\n" +
            "}";

    private final TextArea codeArea    = new TextArea(SAMPLE_CODE);
    private final TextArea stdinArea   = new TextArea();
    private final TextArea outputArea  = new TextArea();
    private final TextArea errorArea   = new TextArea();
    private final TextArea hintArea    = new TextArea();
    private final Button   runBtn      = new Button("▶  Run");
    private final Label    statusLabel = new Label("Ready");

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "compiler-thread");
        t.setDaemon(true);
        return t;
    });

    public CompilerTab() {
        super("☕  Compiler");
        setClosable(false);
        setContent(buildUI());
    }

    private Node buildUI() {
        // ── Code editor ───────────────────────────────────────────────────
        styleTextArea(codeArea, "#0d0d1a", "#e0e0e0", "Consolas, 'Courier New'", 13);
        codeArea.setWrapText(false);
        codeArea.setPrefRowCount(20);

        Label codeLabel = sectionLabel("📝 Java Code Editor");
        VBox codeBox    = withLabel(codeLabel, codeArea);

        // ── Stdin ─────────────────────────────────────────────────────────
        styleTextArea(stdinArea, "#1a1a2e", "#c0c0c0", "Consolas", 12);
        stdinArea.setPromptText("Optional: provide stdin input here…");
        stdinArea.setPrefRowCount(3);
        Label stdinLabel = sectionLabel("⌨  Standard Input (optional)");
        VBox stdinBox    = withLabel(stdinLabel, stdinArea);

        // ── Run button ────────────────────────────────────────────────────
        runBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; " +
                        "-fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 8; -fx-padding: 10 28; -fx-cursor: hand;");
        runBtn.setOnMouseEntered(e -> runBtn.setStyle(runBtn.getStyle().replace("#16a34a", "#15803d")));
        runBtn.setOnMouseExited( e -> runBtn.setStyle(runBtn.getStyle().replace("#15803d", "#16a34a")));
        runBtn.setOnAction(e -> run());

        Button clearBtn = new Button("✕  Clear");
        clearBtn.setStyle("-fx-background-color: #374151; -fx-text-fill: #d0d0d0; " +
                          "-fx-font-size: 13px; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> { outputArea.clear(); errorArea.clear(); hintArea.clear(); statusLabel.setText("Ready"); });

        statusLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        HBox btnBar = new HBox(12, runBtn, clearBtn, statusLabel);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        // ── Output ────────────────────────────────────────────────────────
        styleTextArea(outputArea, "#0a1a0a", "#22c55e", "Consolas", 12);
        outputArea.setEditable(false);
        outputArea.setPromptText("Program output appears here…");
        outputArea.setPrefRowCount(8);
        VBox outputBox = withLabel(sectionLabel("✅ Output"), outputArea);

        // ── Errors ────────────────────────────────────────────────────────
        styleTextArea(errorArea, "#1a0a0a", "#ef4444", "Consolas", 12);
        errorArea.setEditable(false);
        errorArea.setPromptText("Errors appear here…");
        errorArea.setPrefRowCount(5);
        VBox errorBox = withLabel(sectionLabel("❌ Errors / Exceptions"), errorArea);

        // ── Hints ─────────────────────────────────────────────────────────
        styleTextArea(hintArea, "#1a1200", "#fbbf24", "Segoe UI", 12);
        hintArea.setEditable(false);
        hintArea.setPromptText("Friendly error explanations appear here…");
        hintArea.setPrefRowCount(5);
        hintArea.setWrapText(true);
        VBox hintBox = withLabel(sectionLabel("💡 Error Analysis & Hints"), hintArea);

        // ── Bottom split ──────────────────────────────────────────────────
        HBox bottomRow = new HBox(12, errorBox, hintBox);
        HBox.setHgrow(errorBox, Priority.ALWAYS);
        HBox.setHgrow(hintBox,  Priority.ALWAYS);

        // ── Main layout ───────────────────────────────────────────────────
        VBox root = new VBox(12,
                codeBox, stdinBox, btnBar, outputBox, bottomRow);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #0f0f1a;");
        VBox.setVgrow(codeArea,   Priority.ALWAYS);
        VBox.setVgrow(codeBox,    Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #0f0f1a; -fx-background: #0f0f1a;");
        return scroll;
    }

    private void run() {
        String code  = codeArea.getText();
        String stdin = stdinArea.getText();

        runBtn.setDisable(true);
        runBtn.setText("⏳ Running…");
        statusLabel.setText("Compiling & running…");
        outputArea.clear(); errorArea.clear(); hintArea.clear();

        executor.submit(() -> {
            CompilerResult result = CompilerService.compileAndRun(code, stdin);
            Platform.runLater(() -> {
                outputArea.setText(result.getOutput());
                errorArea.setText(result.getRawErrors());
                hintArea.setText(result.getFriendlyHint());

                if (result.isSuccess()) {
                    statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px;");
                    statusLabel.setText("✔ Completed in " + result.getExecutionTimeMs() + " ms");
                } else {
                    statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                    statusLabel.setText("✘ Failed (exit " + result.getExitCode() + ")");
                }

                runBtn.setDisable(false);
                runBtn.setText("▶  Run");
            });
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private static void styleTextArea(TextArea ta, String bg, String fg, String font, int size) {
        ta.setStyle(String.format(
                "-fx-control-inner-background: %s; -fx-text-fill: %s; " +
                "-fx-font-family: '%s'; -fx-font-size: %dpx; " +
                "-fx-background-color: %s; -fx-border-color: #2d2d45; " +
                "-fx-border-radius: 8; -fx-background-radius: 8;",
                bg, fg, font, size, bg));
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");
        return l;
    }

    private static VBox withLabel(Label label, TextArea area) {
        VBox box = new VBox(4, label, area);
        VBox.setVgrow(area, Priority.ALWAYS);
        return box;
    }
}
