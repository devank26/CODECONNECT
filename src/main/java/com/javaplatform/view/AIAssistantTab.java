package com.javaplatform.view;

import com.javaplatform.SessionState;
import com.javaplatform.ThemeManager;
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

    private VBox headerBox;
    private Label title;
    private TextField apiKeyField;
    private Button saveKeyBtn;
    private ScrollPane scroll;
    private Label codeLabel;
    private VBox inputArea;
    private VBox root;

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

        ThemeManager.getInstance().addThemeListener(this::applyTheme);
        applyTheme();
    }

    private Pane buildUI() {
        // ── Header ────────────────────────────────────────────────────────
        title = new Label("AI Coding Assistant");
        String mode = AIService.hasApiKey() ? "● Gemini Connected" : "● Offline Mode";
        statusLabel.setText(mode);

        // API key field
        apiKeyField = new TextField();
        apiKeyField.setPromptText("Paste Gemini API key here to enable full AI…");
        apiKeyField.setMaxWidth(320);
        saveKeyBtn = new Button("Save Key");
        saveKeyBtn.setOnAction(e -> {
            String key = apiKeyField.getText().trim();
            if (!key.isEmpty()) {
                AIService.setApiKey(key);
                statusLabel.setText("● Gemini Connected");
                applyTheme();
                apiKeyField.clear();
            }
        });

        HBox keyRow = new HBox(8, apiKeyField, saveKeyBtn);
        keyRow.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(20, title, statusLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        headerBox = new VBox(8, header, keyRow);
        headerBox.setPadding(new Insets(12, 16, 12, 16));

        // ── Chat scroll ───────────────────────────────────────────────────
        chatBox.setPadding(new Insets(16));
        scroll = new ScrollPane(chatBox);
        scroll.setFitToWidth(true);
        chatBox.heightProperty().addListener((obs, o, n) -> scroll.setVvalue(1.0));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Input area ────────────────────────────────────────────────────
        questionField.setPromptText("Ask a Java question… e.g. 'Why does this throw NPE?'");
        HBox.setHgrow(questionField, Priority.ALWAYS);

        codeContext.setPromptText("Optional: paste your Java code here for context…");
        codeContext.setPrefRowCount(4);

        questionField.setOnAction(e -> ask());
        askBtn.setOnAction(e -> ask());

        codeLabel = new Label("Code context (optional):");

        HBox questionRow = new HBox(10, questionField, askBtn);
        questionRow.setAlignment(Pos.CENTER);
        inputArea = new VBox(6, questionRow, codeLabel, codeContext);
        inputArea.setPadding(new Insets(12, 16, 12, 16));

        // ── Root ──────────────────────────────────────────────────────────
        root = new VBox(headerBox, scroll, inputArea);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return root;
    }

    private void applyTheme() {
        ThemeManager tm = ThemeManager.getInstance();

        // Containers
        root.setStyle("-fx-background-color: " + tm.bgApp() + ";");
        headerBox.setStyle("-fx-background-color: " + tm.bgCard() + "; -fx-border-color: " + tm.border() + "; -fx-border-width: 0 0 1 0;");
        scroll.setStyle("-fx-background-color: " + tm.bgApp() + "; -fx-background: " + tm.bgApp() + ";");
        chatBox.setStyle("-fx-background-color: " + tm.bgApp() + ";");
        inputArea.setStyle("-fx-background-color: " + tm.bgCard() + "; -fx-border-color: " + tm.border() + "; -fx-border-width: 1 0 0 0;");

        // Labels & Fields
        title.setStyle(tm.getLabelStyle(18, true, false));
        statusLabel.setStyle("-fx-text-fill: " + (statusLabel.getText().contains("Connected") ? tm.runColor() : tm.warningColor()) + "; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");
        codeLabel.setStyle(tm.getLabelStyle(11, false, true));

        apiKeyField.setStyle(tm.getTextFieldStyle(12));
        saveKeyBtn.setStyle(tm.getButtonStyle(tm.isDarkMode() ? "#374151" : "#cbd5e1") + " -fx-text-fill: " + tm.textPrimary() + ";");

        questionField.setStyle(tm.getTextFieldStyle(13) + " -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10 14;");
        codeContext.setStyle(tm.getTextAreaStyle("Consolas", 12));

        // Ask Btn style
        askBtn.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 11 24; -fx-cursor: hand;",
            tm.accent()
        ));

        // Hover bindings
        askBtn.setOnMouseEntered(e -> askBtn.setStyle(askBtn.getStyle().replace(tm.accent(), tm.accentHover())));
        askBtn.setOnMouseExited(e -> askBtn.setStyle(askBtn.getStyle().replace(tm.accentHover(), tm.accent())));

        // Restyle existing bubbles in chatBox
        for (javafx.scene.Node rowNode : chatBox.getChildren()) {
            if (rowNode instanceof HBox) {
                HBox row = (HBox) rowNode;
                if (row.getChildren().size() == 1 && row.getChildren().get(0) instanceof VBox) {
                    VBox bubble = (VBox) row.getChildren().get(0);
                    restyleBubbleVBox(bubble, tm, row.getAlignment() == Pos.CENTER_RIGHT);
                }
            }
        }
    }

    private void restyleBubbleVBox(VBox bubble, ThemeManager tm, boolean isMe) {
        if (bubble.getChildren().size() == 3) {
            javafx.scene.Node firstNode = bubble.getChildren().get(0);
            javafx.scene.Node secondNode = bubble.getChildren().get(1);
            javafx.scene.Node thirdNode = bubble.getChildren().get(2);

            if (firstNode instanceof Label && secondNode instanceof Label && thirdNode instanceof Label) {
                Label nameLabel = (Label) firstNode;
                Label msgLabel = (Label) secondNode;
                Label timeLabel = (Label) thirdNode;

                if (isMe) {
                    nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + tm.accent() + ";");
                    msgLabel.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13px; -fx-font-family: 'Segoe UI'; -fx-padding: 10 14; -fx-background-radius: 14;",
                        tm.chatBubbleMeBg(), tm.chatBubbleMeFg()
                    ));
                } else {
                    nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + tm.cyan() + ";");
                    msgLabel.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13px; -fx-font-family: 'Segoe UI'; -fx-padding: 12 16; -fx-background-radius: 14;",
                        tm.chatBubblePeerBg(), tm.chatBubblePeerFg()
                    ));
                }
                timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + tm.textMuted() + ";");
            } else if (firstNode instanceof Label && secondNode instanceof Label && thirdNode instanceof HBox) {
                // Tool request bubble: alertLabel, toolLabel, btnRow (HBox)
                Label alertLabel = (Label) firstNode;
                Label toolLabel  = (Label) secondNode;
                HBox btnRow      = (HBox) thirdNode;

                alertLabel.setStyle("-fx-text-fill: " + tm.warningColor() + "; -fx-font-weight: bold; -fx-font-size: 13px;");
                toolLabel.setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: " + tm.textPrimary() + "; -fx-font-size: 12px;");

                if (btnRow.getChildren().size() == 2) {
                    Button approveBtn = (Button) btnRow.getChildren().get(0);
                    Button denyBtn    = (Button) btnRow.getChildren().get(1);

                    approveBtn.setStyle(tm.getButtonStyle(tm.runColor()) + " -fx-padding: 6 12;");
                    denyBtn.setStyle(tm.getButtonStyle(tm.errorColor()) + " -fx-padding: 6 12;");
                }

                bubble.setStyle(String.format(
                    "-fx-background-color: %s; -fx-padding: 12 16; -fx-background-radius: 10; -fx-border-color: %s; -fx-border-radius: 10;",
                    tm.bgInput(), tm.errorColor()
                ));
            }
        }
    }

    private void ask() {
        String question = questionField.getText().trim();
        if (question.isEmpty()) return;
        String code = codeContext.getText().trim();
        
        String fullQuery = question;
        if (!code.isEmpty()) {
            fullQuery += "\n\nCode Context:\n" + code;
        }
        final String finalQuery = fullQuery;

        addUserBubble(question);
        questionField.clear();
        askBtn.setDisable(true);
        askBtn.setText("⏳ Thinking…");

        executor.submit(() -> {
            String answer = assistant.ask(finalQuery);
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

        Label nameLabel = new Label("You · " + LocalTime.now().format(TIME_FMT));

        Label timeLabel = new Label(""); // Placeholder to match restyle check
        VBox bubble = new VBox(3, nameLabel, label, timeLabel);
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(0, 8, 0, 80));
        chatBox.getChildren().add(row);

        restyleBubbleVBox(bubble, ThemeManager.getInstance(), true);
    }

    private void addAIBubble(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(460);

        Label nameLabel = new Label("🤖 HolyAI · " + LocalTime.now().format(TIME_FMT));

        Label timeLabel = new Label(""); // Placeholder to match restyle check
        VBox bubble = new VBox(3, nameLabel, label, timeLabel);
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 80, 0, 8));
        chatBox.getChildren().add(row);

        restyleBubbleVBox(bubble, ThemeManager.getInstance(), false);
    }

    private void addToolRequestBubble(String toolJson) {
        Label alertLabel = new Label("⚠️ AI wants to execute a tool:");
        
        Label toolLabel = new Label(toolJson);
        toolLabel.setWrapText(true);
        toolLabel.setMaxWidth(440);
        
        Button approveBtn = new Button("Approve");
        Button denyBtn = new Button("Deny");
        
        HBox btnRow = new HBox(10, approveBtn, denyBtn);
        VBox bubble = new VBox(6, alertLabel, toolLabel, btnRow);
        
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 80, 0, 8));
        chatBox.getChildren().add(row);

        restyleBubbleVBox(bubble, ThemeManager.getInstance(), false);
        
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
