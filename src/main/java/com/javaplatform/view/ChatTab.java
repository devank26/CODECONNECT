package com.javaplatform.view;

import com.javaplatform.SessionState;
import com.javaplatform.ThemeManager;
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
    private final Button   reconnectBtn = new Button("🔄 Reconnect");

    private HBox header;
    private Label titleLabel;
    private Label youLabel;
    private ScrollPane scroll;
    private HBox inputBar;
    private Button sendBtn;

    private PrintWriter out;
    private Socket      currentSocket;
    private boolean     connected = false;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public ChatTab() {
        super("💬  Chat");
        setClosable(false);

        inputField  = new TextField();
        statusLabel = new Label("⚡ Connecting…");

        setContent(buildUI());
        connect();

        ThemeManager.getInstance().addThemeListener(this::applyTheme);
        applyTheme();
    }

    private Pane buildUI() {
        // ── Header ────────────────────────────────────────────────────────
        titleLabel = new Label("Live Chat");
        youLabel = new Label("You: " + username);

        reconnectBtn.setOnAction(e -> connect());

        header = new HBox(16, titleLabel, youLabel, statusLabel, reconnectBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));

        // ── Messages scroll area ──────────────────────────────────────────
        msgBox.setPadding(new Insets(16));
        scroll = new ScrollPane(msgBox);
        scroll.setFitToWidth(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        msgBox.heightProperty().addListener((obs, old, nw) -> scroll.setVvalue(1.0));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Input bar ─────────────────────────────────────────────────────
        inputField.setPromptText("Type a message… (Enter to send)");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        sendBtn = new Button("Send ➤");
        sendBtn.setOnMouseEntered(e -> {
            ThemeManager tm = ThemeManager.getInstance();
            sendBtn.setStyle(sendBtn.getStyle().replace(tm.accent(), tm.accentHover()));
        });
        sendBtn.setOnMouseExited(e -> {
            ThemeManager tm = ThemeManager.getInstance();
            sendBtn.setStyle(sendBtn.getStyle().replace(tm.accentHover(), tm.accent()));
        });

        inputBar = new HBox(10, inputField, sendBtn);
        inputBar.setPadding(new Insets(12, 16, 12, 16));
        inputBar.setAlignment(Pos.CENTER);

        Runnable sendAction = this::sendMessage;
        sendBtn.setOnAction(e -> sendAction.run());
        inputField.setOnAction(e -> sendAction.run());

        // ── Root ──────────────────────────────────────────────────────────
        VBox root = new VBox(header, scroll, inputBar);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return root;
    }

    private void applyTheme() {
        ThemeManager tm = ThemeManager.getInstance();

        // Header and layout
        header.setStyle("-fx-background-color: " + tm.bgCard() + "; -fx-border-color: " + tm.border() + "; -fx-border-width: 0 0 1 0;");
        titleLabel.setStyle(tm.getLabelStyle(18, true, false));
        youLabel.setStyle("-fx-text-fill: " + tm.accent() + "; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';");
        statusLabel.setStyle("-fx-text-fill: " + (connected ? tm.runColor() : tm.errorColor()) + "; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");

        reconnectBtn.setStyle(tm.getButtonStyle(tm.isDarkMode() ? "#374151" : "#cbd5e1") + " -fx-text-fill: " + tm.textPrimary() + "; -fx-font-size: 11px; -fx-padding: 4 8;");

        // Message Scroll area
        msgBox.setStyle("-fx-background-color: " + tm.bgApp() + ";");
        scroll.setStyle("-fx-background-color: " + tm.bgApp() + "; -fx-background: " + tm.bgApp() + ";");

        // Input bar
        inputBar.setStyle("-fx-background-color: " + tm.bgCard() + "; -fx-border-color: " + tm.border() + "; -fx-border-width: 1 0 0 0;");
        inputField.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: %s; -fx-prompt-text-fill: %s; -fx-border-color: %s; -fx-border-radius: 20; -fx-background-radius: 20; -fx-font-size: 13px; -fx-padding: 10 16;",
            tm.bgInput(), tm.textPrimary(), tm.textMuted(), tm.border()
        ));

        sendBtn.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 10 20; -fx-cursor: hand;",
            tm.accent()
        ));

        // Re-style all existing message bubbles in UI
        for (javafx.scene.Node rowNode : msgBox.getChildren()) {
            if (rowNode instanceof HBox) {
                HBox row = (HBox) rowNode;
                if (row.getChildren().size() == 1) {
                    javafx.scene.Node firstChild = row.getChildren().get(0);
                    if (firstChild instanceof Label) {
                        Label sysLabel = (Label) firstChild;
                        sysLabel.setStyle("-fx-text-fill: " + tm.textMuted() + "; -fx-font-size: 11px; -fx-font-family: 'Segoe UI';");
                    } else if (firstChild instanceof VBox) {
                        restyleBubbleVBox((VBox) firstChild, tm, row.getAlignment() == Pos.CENTER_RIGHT);
                    }
                }
            }
        }
    }

    private void restyleBubbleVBox(VBox bubble, ThemeManager tm, boolean isMe) {
        if (bubble.getChildren().size() == 3) {
            Label senderLabel = (Label) bubble.getChildren().get(0);
            Label msgLabel    = (Label) bubble.getChildren().get(1);
            Label timeLabel   = (Label) bubble.getChildren().get(2);

            senderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + (isMe ? tm.accent() : tm.cyan()) + ";");
            msgLabel.setStyle(String.format(
                "-fx-font-size: 13px; -fx-font-family: 'Segoe UI'; -fx-padding: 10 14; -fx-background-radius: 14; -fx-background-color: %s; -fx-text-fill: %s;",
                isMe ? tm.chatBubbleMeBg() : tm.chatBubblePeerBg(),
                isMe ? tm.chatBubbleMeFg() : tm.chatBubblePeerFg()
            ));
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + tm.textMuted() + ";");
        }
    }

    public void disconnect() {
        connected = false;
        if (currentSocket != null) {
            try { currentSocket.close(); } catch (Exception ignored) {}
            currentSocket = null;
        }
    }

    public void reconnectToHost(String host) {
        SessionState.getInstance().setServerHost(host);
        disconnect();
        connect();
    }

    private void connect() {
        Platform.runLater(() -> {
            ThemeManager tm = ThemeManager.getInstance();
            statusLabel.setStyle("-fx-text-fill: " + tm.textMuted() + "; -fx-font-size: 12px;");
            statusLabel.setText("⚡ Connecting…");
            reconnectBtn.setVisible(false);
            reconnectBtn.setManaged(false);
        });

        Thread t = new Thread(() -> {
            try {
                disconnect();
                Socket socket = new Socket();
                socket.connect(new java.net.InetSocketAddress(SessionState.getInstance().getServerHost(), SessionState.CHAT_PORT), 1500);
                currentSocket = socket;
                BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                connected = true;

                // Register username
                out.println("USERNAME:" + username);
                Platform.runLater(() -> {
                    ThemeManager tm = ThemeManager.getInstance();
                    statusLabel.setStyle("-fx-text-fill: " + tm.runColor() + "; -fx-font-size: 12px;");
                    statusLabel.setText("● Connected");
                    reconnectBtn.setVisible(false);
                    reconnectBtn.setManaged(false);
                });

                // Read loop
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    Platform.runLater(() -> showMessage(msg));
                }
                throw new IOException("Connection closed by server.");
            } catch (IOException e) {
                handleDisconnect(e);
            }
        }, "chat-reader");
        t.setDaemon(true);
        t.start();
    }

    private void handleDisconnect(IOException e) {
        connected = false;
        Platform.runLater(() -> {
            ThemeManager tm = ThemeManager.getInstance();
            statusLabel.setStyle("-fx-text-fill: " + tm.errorColor() + "; -fx-font-size: 12px;");
            statusLabel.setText("✘ Disconnected");
            reconnectBtn.setVisible(true);
            reconnectBtn.setManaged(true);
            showSystemMessage("Chat connection error: " + e.getMessage());
        });
    }

    public void sendRawMessage(String msg) {
        if (connected && out != null) {
            out.println(msg);
            if (out.checkError()) {
                handleDisconnect(new IOException("Write failed (connection lost)."));
            }
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || !connected) return;
        out.println("MSG:" + text);
        if (out.checkError()) {
            handleDisconnect(new IOException("Write failed (connection lost)."));
        } else {
            inputField.clear();
        }
    }

    private void showMessage(String rawMsg) {
        if (rawMsg.startsWith("SYSTEM:")) {
            String sysMsg = rawMsg.substring(7).trim();
            showSystemMessage(sysMsg);
            if (sysMsg.contains("joined the chat")) {
                if (MainWindow.getCompilerTab() != null) {
                    MainWindow.getCompilerTab().onPeerJoinedChat();
                }
            }
            return;
        }

        // Parse "sender: text"
        int colon = rawMsg.indexOf(": ");
        String sender = colon > 0 ? rawMsg.substring(0, colon) : "Unknown";
        String text   = colon > 0 ? rawMsg.substring(colon + 2) : rawMsg;

        if (text.startsWith("CODE_SHARE:")) {
            String encodedCode = text.substring(11);
            try {
                String decodedCode = java.net.URLDecoder.decode(encodedCode, "UTF-8");
                if (MainWindow.getCompilerTab() != null) {
                    MainWindow.getCompilerTab().showSharedCode(sender, decodedCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        if (text.equals("CODE_SHARE_STOP")) {
            if (MainWindow.getCompilerTab() != null) {
                MainWindow.getCompilerTab().hideSharedCode(sender);
            }
            return;
        }

        boolean isMe  = sender.equals(username);

        // ── Bubble Labels ───────────────────────────────────────────────────
        Label senderLabel = new Label(isMe ? "You" : sender);
        Label msgLabel = new Label(text);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(340);

        String timeStr = LocalTime.now().format(TIME_FMT);
        Label timeLabel = new Label(timeStr);

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

        // Style the bubble immediately according to the active theme
        restyleBubbleVBox(bubble, ThemeManager.getInstance(), isMe);
    }

    private void showSystemMessage(String text) {
        ThemeManager tm = ThemeManager.getInstance();
        Label l = new Label("— " + text + " —");
        l.setStyle("-fx-text-fill: " + tm.textMuted() + "; -fx-font-size: 11px; -fx-font-family: 'Segoe UI';");
        HBox row = new HBox(l);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(4));
        msgBox.getChildren().add(row);
    }
}
