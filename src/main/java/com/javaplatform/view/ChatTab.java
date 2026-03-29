package com.javaplatform.view;

import com.javaplatform.SessionState;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Real-time chat tab.
 * Connects to ChatServer on port 9001 using a Java Socket.
 */
public class ChatTab extends Tab {

    private final String   username = SessionState.getInstance().getUsername();
    private final VBox     msgBox   = new VBox(8);
    private final TextField inputField;
    private final Label    statusLabel;

    private PrintWriter out;
    private boolean     connected = false;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public ChatTab() {
        super("💬  Chat");
        setClosable(false);

        inputField  = new TextField();
        statusLabel = new Label("⚡ Connecting…");

        setContent(buildUI());
        connect();
    }

    private Pane buildUI() {
        // ── Header ────────────────────────────────────────────────────────
        statusLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");
        Label title = new Label("Live Chat");
        title.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        Label you = new Label("You: " + username);
        you.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';");

        HBox header = new HBox(16, title, you, statusLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setStyle("-fx-background-color: #1e1e2e; -fx-border-color: #2d2d45; -fx-border-width: 0 0 1 0;");

        // ── Messages scroll area ──────────────────────────────────────────
        msgBox.setPadding(new Insets(16));
        msgBox.setStyle("-fx-background-color: #0f0f1a;");
        ScrollPane scroll = new ScrollPane(msgBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #0f0f1a; -fx-background: #0f0f1a;");
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // Auto-scroll on new content
        msgBox.heightProperty().addListener((obs, old, nw) ->
                scroll.setVvalue(1.0));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Input bar ─────────────────────────────────────────────────────
        inputField.setPromptText("Type a message… (Enter to send)");
        inputField.setStyle("-fx-background-color: #1e1e2e; -fx-text-fill: #e0e0e0; " +
                             "-fx-border-color: #444; -fx-border-radius: 20; " +
                             "-fx-background-radius: 20; -fx-font-size: 13px; -fx-padding: 10 16;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button sendBtn = new Button("Send ➤");
        sendBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; " +
                         "-fx-font-size: 13px; -fx-font-weight: bold; " +
                         "-fx-background-radius: 20; -fx-padding: 10 20; -fx-cursor: hand;");
        sendBtn.setOnMouseEntered(e -> sendBtn.setStyle(sendBtn.getStyle().replace("#7c3aed", "#6d28d9")));
        sendBtn.setOnMouseExited( e -> sendBtn.setStyle(sendBtn.getStyle().replace("#6d28d9", "#7c3aed")));

        HBox inputBar = new HBox(10, inputField, sendBtn);
        inputBar.setPadding(new Insets(12, 16, 12, 16));
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setStyle("-fx-background-color: #1e1e2e; -fx-border-color: #2d2d45; -fx-border-width: 1 0 0 0;");

        Runnable sendAction = this::sendMessage;
        sendBtn.setOnAction(e -> sendAction.run());
        inputField.setOnAction(e -> sendAction.run());

        // ── Root ──────────────────────────────────────────────────────────
        VBox root = new VBox(header, scroll, inputBar);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setStyle("-fx-background-color: #0f0f1a;");
        return root;
    }

    private void connect() {
        Thread t = new Thread(() -> {
            try {
                Socket socket = new Socket(SessionState.HOST, SessionState.CHAT_PORT);
                BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                connected = true;

                // Register username
                out.println("USERNAME:" + username);
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px;");
                    statusLabel.setText("● Connected");
                });

                // Read loop
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    Platform.runLater(() -> showMessage(msg));
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                    statusLabel.setText("✘ Disconnected");
                    showSystemMessage("Could not connect to chat server: " + e.getMessage());
                });
            }
        }, "chat-reader");
        t.setDaemon(true);
        t.start();
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || !connected) return;
        out.println("MSG:" + text);
        inputField.clear();
    }

    private void showMessage(String rawMsg) {
        if (rawMsg.startsWith("SYSTEM:")) {
            showSystemMessage(rawMsg.substring(7).trim());
            return;
        }

        // Parse "sender: text"
        int colon = rawMsg.indexOf(": ");
        String sender = colon > 0 ? rawMsg.substring(0, colon) : "Unknown";
        String text   = colon > 0 ? rawMsg.substring(colon + 2) : rawMsg;
        boolean isMe  = sender.equals(username);

        // ── Bubble ────────────────────────────────────────────────────────
        Label msgLabel = new Label(text);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(340);
        msgLabel.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI'; " +
                          "-fx-padding: 10 14; -fx-background-radius: 14; " +
                          (isMe ? "-fx-background-color: #4c1d95; -fx-text-fill: #e0d4ff;"
                                : "-fx-background-color: #1e2a3e; -fx-text-fill: #e0e0e0;"));

        String timeStr = LocalTime.now().format(TIME_FMT);
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");

        Label senderLabel = new Label(isMe ? "You" : sender);
        senderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; " +
                             (isMe ? "-fx-text-fill: #a78bfa;" : "-fx-text-fill: #60a5fa;"));

        VBox bubble = new VBox(2, senderLabel, msgLabel, timeLabel);
        bubble.setMaxWidth(360);

        HBox row = new HBox(bubble);
        row.setPadding(new Insets(2, 16, 2, 16));
        if (isMe) {
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
        }
        msgBox.getChildren().add(row);
    }

    private void showSystemMessage(String text) {
        Label l = new Label("— " + text + " —");
        l.setStyle("-fx-text-fill: #555; -fx-font-size: 11px; -fx-font-family: 'Segoe UI';");
        HBox row = new HBox(l);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(4));
        msgBox.getChildren().add(row);
    }
}
