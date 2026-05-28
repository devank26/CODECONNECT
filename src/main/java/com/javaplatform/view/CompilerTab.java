package com.javaplatform.view;

import com.javaplatform.SessionState;
import com.javaplatform.ThemeManager;
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

    private final TextArea sharedCodeArea = new TextArea();
    private final Label    sharedCodeLabel = new Label("📺 Live Shared Code");
    private final Button   shareBtn    = new Button("📡 Share Code");

    private SplitPane editorSplitPane;
    private boolean isSharing = false;

    private final java.util.concurrent.ScheduledExecutorService shareExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "code-share-debounce");
        t.setDaemon(true);
        return t;
    });
    private java.util.concurrent.ScheduledFuture<?> pendingTask = null;

    private final javafx.beans.value.ChangeListener<String> codeListener = (obs, oldVal, newVal) -> queueCodeShare();

    private Label codeLabel;
    private Label stdinLabel;
    private Label outputLabel;
    private Label errorLabel;
    private Label hintLabel;
    private Button clearBtn;

    private VBox root;
    private ScrollPane scroll;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "compiler-thread");
        t.setDaemon(true);
        return t;
    });

    public CompilerTab() {
        super("☕  Compiler");
        setClosable(false);
        setContent(buildUI());
        ThemeManager.getInstance().addThemeListener(this::applyTheme);
        applyTheme();
    }

    private Node buildUI() {
        // ── Code editor ───────────────────────────────────────────────────
        codeArea.setWrapText(false);
        codeArea.setPrefRowCount(20);

        codeLabel = new Label("📝 Java Code Editor");
        VBox codeBox = withLabel(codeLabel, codeArea);

        // ── Shared Code Editor ────────────────────────────────────────────
        sharedCodeArea.setEditable(false);
        sharedCodeArea.setWrapText(false);
        sharedCodeArea.setPrefRowCount(20);
        sharedCodeArea.setPromptText("Live shared code from other peers will appear here when they share...");
        VBox sharedCodeBox = withLabel(sharedCodeLabel, sharedCodeArea);

        // ── Editor SplitPane ──────────────────────────────────────────────
        editorSplitPane = new SplitPane(codeBox, sharedCodeBox);
        editorSplitPane.setDividerPositions(1.0); // Hide shared code by default

        // ── Stdin ─────────────────────────────────────────────────────────
        stdinArea.setPromptText("Optional: provide stdin input here…");
        stdinArea.setPrefRowCount(3);
        stdinLabel = new Label("⌨  Standard Input (optional)");
        VBox stdinBox = withLabel(stdinLabel, stdinArea);

        // ── Buttons ────────────────────────────────────────────────────
        runBtn.setOnAction(e -> run());

        clearBtn = new Button("✕  Clear");
        clearBtn.setOnAction(e -> { outputArea.clear(); errorArea.clear(); hintArea.clear(); statusLabel.setText("Ready"); });

        shareBtn.setOnAction(e -> toggleSharing());

        HBox btnBar = new HBox(12, runBtn, clearBtn, shareBtn, statusLabel);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        // ── Output ────────────────────────────────────────────────────────
        outputArea.setEditable(false);
        outputArea.setPromptText("Program output appears here…");
        outputArea.setPrefRowCount(8);
        outputLabel = new Label("✅ Output");
        VBox outputBox = withLabel(outputLabel, outputArea);

        // ── Errors ────────────────────────────────────────────────────────
        errorArea.setEditable(false);
        errorArea.setPromptText("Errors appear here…");
        errorArea.setPrefRowCount(5);
        errorLabel = new Label("❌ Errors / Exceptions");
        VBox errorBox = withLabel(errorLabel, errorArea);

        // ── Hints ─────────────────────────────────────────────────────────
        hintArea.setEditable(false);
        hintArea.setPromptText("Friendly error explanations appear here…");
        hintArea.setPrefRowCount(5);
        hintArea.setWrapText(true);
        hintLabel = new Label("💡 Error Analysis & Hints");
        VBox hintBox = withLabel(hintLabel, hintArea);

        // ── Bottom split ──────────────────────────────────────────────────
        HBox bottomRow = new HBox(12, errorBox, hintBox);
        HBox.setHgrow(errorBox, Priority.ALWAYS);
        HBox.setHgrow(hintBox,  Priority.ALWAYS);

        // ── Main layout ───────────────────────────────────────────────────
        root = new VBox(12, editorSplitPane, stdinBox, btnBar, outputBox, bottomRow);
        root.setPadding(new Insets(16));
        VBox.setVgrow(editorSplitPane, Priority.ALWAYS);

        scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private void applyTheme() {
        ThemeManager tm = ThemeManager.getInstance();

        // Backgrounds
        root.setStyle("-fx-background-color: " + tm.bgApp() + ";");
        scroll.setStyle("-fx-background-color: " + tm.bgApp() + "; -fx-background: " + tm.bgApp() + ";");

        // Text Areas
        codeArea.setStyle(tm.getTextAreaStyle("Consolas", 13));
        stdinArea.setStyle(tm.getTextAreaStyle("Consolas", 12));
        sharedCodeArea.setStyle(tm.getTextAreaStyle("Consolas", 13));

        // Labels
        codeLabel.setStyle(tm.getLabelStyle(12, true, true));
        stdinLabel.setStyle(tm.getLabelStyle(12, true, true));
        outputLabel.setStyle(tm.getLabelStyle(12, true, true));
        errorLabel.setStyle(tm.getLabelStyle(12, true, true));
        hintLabel.setStyle(tm.getLabelStyle(12, true, true));
        sharedCodeLabel.setStyle(tm.getLabelStyle(12, true, true));

        // Output Areas with special backgrounds/colors
        String outputBg = tm.isDarkMode() ? "#0a0f1d" : tm.bgInput();
        String outputFg = tm.isDarkMode() ? tm.cyan() : tm.accent();
        outputArea.setStyle(String.format(
            "-fx-control-inner-background: %s; -fx-text-fill: %s; -fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 8; -fx-background-radius: 8;",
            outputBg, outputFg, outputBg, tm.border()
        ));

        String errorBg = tm.isDarkMode() ? "#1a0a0f" : tm.bgInput();
        String errorFg = tm.isDarkMode() ? tm.errorColor() : tm.errorColor();
        errorArea.setStyle(String.format(
            "-fx-control-inner-background: %s; -fx-text-fill: %s; -fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 8; -fx-background-radius: 8;",
            errorBg, errorFg, errorBg, tm.border()
        ));

        String hintBg = tm.isDarkMode() ? "#1a150a" : tm.bgInput();
        String hintFg = tm.isDarkMode() ? tm.warningColor() : tm.warningColor();
        hintArea.setStyle(String.format(
            "-fx-control-inner-background: %s; -fx-text-fill: %s; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 8; -fx-background-radius: 8;",
            hintBg, hintFg, hintBg, tm.border()
        ));

        runBtn.setStyle(tm.getButtonStyle(tm.runColor()));
        String clearBg = tm.isDarkMode() ? "#374151" : "#cbd5e1";
        clearBtn.setStyle(tm.getButtonStyle(clearBg) + " -fx-text-fill: " + tm.textPrimary() + ";");

        String shareBg = tm.isDarkMode() ? "#374151" : "#cbd5e1";
        if (isSharing) {
            shareBtn.setStyle(tm.getButtonStyle(tm.errorColor()));
        } else {
            shareBtn.setStyle(tm.getButtonStyle(shareBg) + " -fx-text-fill: " + tm.textPrimary() + ";");
        }

        // Hover bindings
        runBtn.setOnMouseEntered(e -> runBtn.setStyle(tm.getButtonStyle(tm.runColorHover())));
        runBtn.setOnMouseExited(e -> runBtn.setStyle(tm.getButtonStyle(tm.runColor())));

        String clearBgHover = tm.isDarkMode() ? "#4b5563" : "#94a3b8";
        clearBtn.setOnMouseEntered(e -> clearBtn.setStyle(tm.getButtonStyle(clearBgHover) + " -fx-text-fill: " + tm.textPrimary() + ";"));
        clearBtn.setOnMouseExited(e -> clearBtn.setStyle(tm.getButtonStyle(clearBg) + " -fx-text-fill: " + tm.textPrimary() + ";"));

        String shareBgHover = tm.isDarkMode() ? "#4b5563" : "#94a3b8";
        shareBtn.setOnMouseEntered(e -> {
            if (isSharing) {
                shareBtn.setStyle(tm.getButtonStyle(tm.errorColor()));
            } else {
                shareBtn.setStyle(tm.getButtonStyle(shareBgHover) + " -fx-text-fill: " + tm.textPrimary() + ";");
            }
        });
        shareBtn.setOnMouseExited(e -> {
            if (isSharing) {
                shareBtn.setStyle(tm.getButtonStyle(tm.errorColor()));
            } else {
                shareBtn.setStyle(tm.getButtonStyle(shareBg) + " -fx-text-fill: " + tm.textPrimary() + ";");
            }
        });

        // Status Label
        statusLabel.setStyle("-fx-text-fill: " + tm.textMuted() + "; -fx-font-size: 12px;");
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

                ThemeManager tm = ThemeManager.getInstance();
                if (result.isSuccess()) {
                    statusLabel.setStyle("-fx-text-fill: " + tm.runColor() + "; -fx-font-size: 12px;");
                    statusLabel.setText("✔ Completed in " + result.getExecutionTimeMs() + " ms");
                } else {
                    statusLabel.setStyle("-fx-text-fill: " + tm.errorColor() + "; -fx-font-size: 12px;");
                    statusLabel.setText("✘ Failed (exit " + result.getExitCode() + ")");
                }

                runBtn.setDisable(false);
                runBtn.setText("▶  Run");
            });
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
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

    private void toggleSharing() {
        isSharing = !isSharing;
        ThemeManager tm = ThemeManager.getInstance();
        if (isSharing) {
            shareBtn.setText("📡 Sharing Code...");
            shareBtn.setStyle(tm.getButtonStyle(tm.errorColor()));
            codeArea.textProperty().addListener(codeListener);
            queueCodeShare();
        } else {
            shareBtn.setText("📡 Share Code");
            shareBtn.setStyle(tm.getButtonStyle(tm.isDarkMode() ? "#374151" : "#cbd5e1") + " -fx-text-fill: " + tm.textPrimary() + ";");
            codeArea.textProperty().removeListener(codeListener);
            sendStopSharing();
        }
    }

    private void queueCodeShare() {
        if (!isSharing) return;
        synchronized (this) {
            if (pendingTask != null) {
                pendingTask.cancel(false);
            }
            pendingTask = shareExecutor.schedule(this::sendCodeUpdate, 300, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    private void sendCodeUpdate() {
        try {
            String code = codeArea.getText();
            String encoded = java.net.URLEncoder.encode(code, "UTF-8");
            if (MainWindow.getChatTab() != null) {
                MainWindow.getChatTab().sendRawMessage("MSG:CODE_SHARE:" + encoded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendStopSharing() {
        if (MainWindow.getChatTab() != null) {
            MainWindow.getChatTab().sendRawMessage("MSG:CODE_SHARE_STOP");
        }
    }

    public void showSharedCode(String peerName, String code) {
        Platform.runLater(() -> {
            sharedCodeArea.setText(code);
            sharedCodeLabel.setText("📺 Live: Viewing " + peerName + "'s Code");
            if (editorSplitPane.getDividerPositions()[0] > 0.95) {
                editorSplitPane.setDividerPositions(0.5);
            }
        });
    }

    public void hideSharedCode(String peerName) {
        Platform.runLater(() -> {
            sharedCodeArea.setText("");
            sharedCodeLabel.setText("📺 Live Shared Code");
            editorSplitPane.setDividerPositions(1.0);
        });
    }

    public void onPeerJoinedChat() {
        if (isSharing) {
            sendCodeUpdate();
        }
    }
}
