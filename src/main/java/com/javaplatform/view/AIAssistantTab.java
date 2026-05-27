package com.javaplatform.view;

import com.javaplatform.SessionState;
import com.javaplatform.service.AIService;
import com.javaplatform.holyai.service.AICodeAssistant;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

/**
 * AI Assistant tab — chat-style interface for Java coding help powered by HolyAI.
 */
public class AIAssistantTab extends Tab {

    private final VBox    chatBox     = new VBox(10);
    private final TextField questionField = new TextField();
    private final TextArea  codeContext   = new TextArea();
    private final Label     statusLabel   = new Label();
    private final Button    askBtn        = new Button("Ask AI ✦");

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ai-thread");
        t.setDaemon(true);
        return t;
    });

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    
    // Core HolyAI Engine
    private final AICodeAssistant assistant = new AICodeAssistant();

    public AIAssistantTab() {
        super("🤖  AI Assistant");
        setClosable(false);
        setContent(buildUI());
        showWelcome();
    }

    private Pane buildUI() {
        // ── Header ────────────────────────────────────────────────────────
        Label title = new Label("AI Coding Assistant");
        title.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        String mode = AIService.hasApiKey() ? "● Gemini Connected" : "● Offline Mode";
        statusLabel.setText(mode);
        statusLabel.setStyle("-fx-text-fill: " + (AIService.hasApiKey() ? "#22c55e" : "#fbbf24") +
                             "; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");

        // API key field
        TextField apiKeyField = new TextField();
        apiKeyField.setPromptText("Paste Gemini API key here to enable full AI…");
        apiKeyField.setStyle("-fx-background-color: #1e1e2e; -fx-text-fill: #e0e0e0; " +
                              "-fx-border-color: #444; -fx-border-radius: 8; -fx-background-radius: 8; " +
                              "-fx-font-size: 12px; -fx-padding: 7 12;");
        apiKeyField.setMaxWidth(320);
        Button saveKeyBtn = new Button("Save Key");
        saveKeyBtn.setStyle("-fx-background-color: #374151; -fx-text-fill: #d0d0d0; " +
                             "-fx-font-size: 12px; -fx-background-radius: 8; -fx-padding: 7 14; -fx-cursor: hand;");
        saveKeyBtn.setOnAction(e -> {
            String key = apiKeyField.getText().trim();
            if (!key.isEmpty()) {
                AIService.setApiKey(key);
                statusLabel.setText("● Gemini Connected");
                statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px;");
                apiKeyField.clear();
            }
        });

        HBox keyRow = new HBox(8, apiKeyField, saveKeyBtn);
        keyRow.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(20, title, statusLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox headerBox = new VBox(8, header, keyRow);
        headerBox.setPadding(new Insets(12, 16, 12, 16));
        headerBox.setStyle("-fx-background-color: #1e1e2e; -fx-border-color: #2d2d45; -fx-border-width: 0 0 1 0;");

        // ── Chat scroll ───────────────────────────────────────────────────
        chatBox.setPadding(new Insets(16));
        chatBox.setStyle("-fx-background-color: #0f0f1a;");
        ScrollPane scroll = new ScrollPane(chatBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #0f0f1a; -fx-background: #0f0f1a;");
        chatBox.heightProperty().addListener((obs, o, n) -> scroll.setVvalue(1.0));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Input area ────────────────────────────────────────────────────
        questionField.setPromptText("Ask a Java question… e.g. 'Why does this throw NPE?'");
        questionField.setStyle("-fx-background-color: #1e1e2e; -fx-text-fill: #e0e0e0; " +
                                "-fx-border-color: #444; -fx-border-radius: 10; -fx-background-radius: 10; " +
                                "-fx-font-size: 13px; -fx-padding: 10 14;");
        HBox.setHgrow(questionField, Priority.ALWAYS);

        codeContext.setPromptText("Optional: paste your Java code here for context…");
        codeContext.setPrefRowCount(4);
        codeContext.setStyle("-fx-control-inner-background: #0d0d1a; -fx-text-fill: #c0c0d0; " +
                              "-fx-font-family: 'Consolas'; -fx-font-size: 12px; " +
                              "-fx-border-color: #333; -fx-border-radius: 8; -fx-background-radius: 8;");

        askBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; " +
                        "-fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 10; -fx-padding: 11 24; -fx-cursor: hand;");
        askBtn.setOnMouseEntered(e -> askBtn.setStyle(askBtn.getStyle().replace("#7c3aed", "#6d28d9")));
        askBtn.setOnMouseExited( e -> askBtn.setStyle(askBtn.getStyle().replace("#6d28d9", "#7c3aed")));
        askBtn.setOnAction(e -> ask());
        questionField.setOnAction(e -> ask());

        Label codeLabel = new Label("Code context (optional):");
        codeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        HBox questionRow = new HBox(10, questionField, askBtn);
        questionRow.setAlignment(Pos.CENTER);
        VBox inputArea = new VBox(6, questionRow, codeLabel, codeContext);
        inputArea.setPadding(new Insets(12, 16, 12, 16));
        inputArea.setStyle("-fx-background-color: #1e1e2e; -fx-border-color: #2d2d45; -fx-border-width: 1 0 0 0;");

        // ── Root ──────────────────────────────────────────────────────────
        VBox root = new VBox(headerBox, scroll, inputArea);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setStyle("-fx-background-color: #0f0f1a;");
        return root;
    }

    private void ask() {
        String question = questionField.getText().trim();
        if (question.isEmpty()) return;
        String code = codeContext.getText().trim();
        
        String fullQuery = question;
        if (!code.isEmpty()) {
            fullQuery += "\n\nCode Context:\n" + code;
        }

        addUserBubble(question);
        questionField.clear();
        askBtn.setDisable(true);
        askBtn.setText("⏳ Thinking…");

        executor.submit(() -> {
            String answer = assistant.ask(fullQuery);
            Platform.runLater(() -> processAIResponse(answer));
        });
    }

    private void processAIResponse(String answer) {
        if (answer != null && answer.startsWith("[TOOL_REQUEST]")) {
            String toolJson = answer.substring(14).trim();
            addToolRequestBubble(toolJson);
        } else {
            addAIBubble(answer);
            askBtn.setDisable(false);
            askBtn.setText("Ask AI ✦");
        }
    }

    private void addUserBubble(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(380);
        label.setStyle("-fx-background-color: #4c1d95; -fx-text-fill: #e0d4ff; " +
                       "-fx-font-size: 13px; -fx-font-family: 'Segoe UI'; " +
                       "-fx-padding: 10 14; -fx-background-radius: 14;");

        Label nameLabel = new Label("You · " + LocalTime.now().format(TIME_FMT));
        nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #a78bfa;");

        VBox bubble = new VBox(3, nameLabel, label);
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(0, 8, 0, 80));
        chatBox.getChildren().add(row);
    }

    private void addAIBubble(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(460);
        label.setStyle("-fx-background-color: #1a2240; -fx-text-fill: #c8d8f0; " +
                       "-fx-font-size: 13px; -fx-font-family: 'Segoe UI'; " +
                       "-fx-padding: 12 16; -fx-background-radius: 14;");

        Label nameLabel = new Label("🤖 HolyAI · " + LocalTime.now().format(TIME_FMT));
        nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #60a5fa;");

        VBox bubble = new VBox(3, nameLabel, label);
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 80, 0, 8));
        chatBox.getChildren().add(row);
    }

    private void addToolRequestBubble(String toolJson) {
        Label alertLabel = new Label("⚠️ AI wants to execute a tool:");
        alertLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold; -fx-font-size: 13px;");
        
        Label toolLabel = new Label(toolJson);
        toolLabel.setWrapText(true);
        toolLabel.setMaxWidth(440);
        toolLabel.setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: #e0e0e0; -fx-font-size: 12px;");
        
        Button approveBtn = new Button("Approve");
        approveBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-cursor: hand;");
        Button denyBtn = new Button("Deny");
        denyBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");
        
        HBox btnRow = new HBox(10, approveBtn, denyBtn);
        VBox bubble = new VBox(6, alertLabel, toolLabel, btnRow);
        bubble.setStyle("-fx-background-color: #2d1b1e; -fx-padding: 12 16; -fx-background-radius: 10; -fx-border-color: #ef4444; -fx-border-radius: 10;");
        
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 80, 0, 8));
        chatBox.getChildren().add(row);
        
        // Handle interactions
        approveBtn.setOnAction(e -> {
            approveBtn.setDisable(true);
            denyBtn.setDisable(true);
            approveBtn.setText("Executing...");
            executor.submit(() -> {
                String nextAnswer = assistant.executeToolAndContinue(toolJson);
                Platform.runLater(() -> processAIResponse(nextAnswer));
            });
        });
        
        denyBtn.setOnAction(e -> {
            approveBtn.setDisable(true);
            denyBtn.setDisable(true);
            denyBtn.setText("Denied");
            executor.submit(() -> {
                String nextAnswer = assistant.denyToolAndContinue();
                Platform.runLater(() -> processAIResponse(nextAnswer));
            });
        });
    }

    private void showWelcome() {
        String welcome = "👋 Hello, " + SessionState.getInstance().getUsername() + "!\n\n" +
                         "I'm your Java coding assistant. Ask me anything:\n" +
                         "• Why is my code throwing an exception?\n" +
                         "• How do I fix a NullPointerException?\n" +
                         "• How do I implement a linked list?\n\n" +
                         (AIService.hasApiKey()
                             ? "✨ Gemini AI is active — I can answer complex questions!"
                             : "⚠ Running in offline mode. Add your Gemini API key above for full AI.");
        addAIBubble(welcome);
    }
}
