package com.javaplatform.view;

import com.javaplatform.SessionState;
import com.javaplatform.service.VideoService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Video call tab.
 * Uses VideoService to connect to VideoRelayServer,
 * capture webcam frames, and display local + remote video.
 */
public class VideoTab extends Tab {

    private final ImageView localView  = new ImageView();
    private final ImageView remoteView = new ImageView();
    private final Label     statusLabel = new Label("Not connected");
    private final TextField roomField   = new TextField();
    private final Label     roomIdLabel = new Label("");
    private final Button    muteBtn    = new Button("🎤  Mute");
    private final Button    camBtn     = new Button("📷  Camera Off");
    private final Button    createBtn  = new Button("Create Room");
    private final Button    joinBtn    = new Button("Join Room");
    private final Button    leaveBtn   = new Button("Leave Room");

    private VideoService videoService;
    private boolean muted = false;

    public VideoTab() {
        super("📹  Video Call");
        setClosable(false);
        setContent(buildUI());
        initVideoService();
    }

    private Pane buildUI() {
        // ── Header ────────────────────────────────────────────────────────
        Label title = new Label("Video Call");
        title.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        statusLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");
        HBox header = new HBox(16, title, statusLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setStyle("-fx-background-color: #1e1e2e; -fx-border-color: #2d2d45; -fx-border-width: 0 0 1 0;");

        // ── Local video ───────────────────────────────────────────────────
        setupImageView(localView, 320, 240);
        Label localLabel = new Label("You — " + SessionState.getInstance().getUsername());
        localLabel.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");
        VBox localBox = new VBox(6, localLabel, localView);
        localBox.setAlignment(Pos.CENTER);
        localBox.setPadding(new Insets(10));
        localBox.setStyle("-fx-background-color: #12122a; -fx-background-radius: 12; " +
                          "-fx-border-color: #2d2d55; -fx-border-radius: 12;");

        // ── Remote video ──────────────────────────────────────────────────
        setupImageView(remoteView, 320, 240);
        Label remoteLabel = new Label("Remote Peer");
        remoteLabel.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");
        VBox remoteBox = new VBox(6, remoteLabel, remoteView);
        remoteBox.setAlignment(Pos.CENTER);
        remoteBox.setPadding(new Insets(10));
        remoteBox.setStyle("-fx-background-color: #12122a; -fx-background-radius: 12; " +
                           "-fx-border-color: #2d2d55; -fx-border-radius: 12;");

        HBox videoRow = new HBox(20, localBox, remoteBox);
        videoRow.setAlignment(Pos.CENTER);
        videoRow.setPadding(new Insets(20));

        // ── Room controls ─────────────────────────────────────────────────
        styleButton(createBtn, "#16a34a");
        createBtn.setOnAction(e -> videoService.createRoom());

        roomField.setPromptText("Room ID…");
        roomField.setMaxWidth(120);
        roomField.setStyle("-fx-background-color: #1e1e2e; -fx-text-fill: #e0e0e0; " +
                            "-fx-border-color: #444; -fx-border-radius: 8; -fx-background-radius: 8; " +
                            "-fx-font-size: 13px; -fx-padding: 8 12;");

        styleButton(joinBtn, "#2563eb");
        joinBtn.setOnAction(e -> videoService.joinRoom(roomField.getText()));

        styleButton(leaveBtn, "#dc2626");
        leaveBtn.setOnAction(e -> videoService.leaveRoom());
        leaveBtn.setDisable(true);

        roomIdLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 13px; -fx-font-family: 'Consolas'; " +
                              "-fx-font-weight: bold;");

        HBox roomControls = new HBox(10, createBtn, new Separator(),
                roomField, joinBtn, new Separator(), leaveBtn, roomIdLabel);
        roomControls.setAlignment(Pos.CENTER);
        roomControls.setPadding(new Insets(10));

        // ── Call controls ─────────────────────────────────────────────────
        styleButton(muteBtn, "#374151");
        muteBtn.setOnAction(e -> toggleMute());

        styleButton(camBtn, "#374151");
        camBtn.setOnAction(e -> {
            if (videoService != null) {
                videoService.toggleCamera();
                boolean on = videoService.isCameraEnabled();
                camBtn.setText(on ? "📷  Camera Off" : "📷  Camera On");
            }
        });

        HBox callControls = new HBox(12, muteBtn, camBtn);
        callControls.setAlignment(Pos.CENTER);
        callControls.setPadding(new Insets(6, 10, 10, 10));

        // ── Help text ─────────────────────────────────────────────────────
        Label help = new Label(
                "1. Click 'Create Room' → share the Room ID with your peer\n" +
                "2. Your peer pastes the ID, clicks 'Join Room'\n" +
                "3. Both users see each other's video automatically");
        help.setStyle("-fx-text-fill: #555; -fx-font-size: 11px; -fx-font-family: 'Segoe UI';");
        help.setWrapText(true);
        StackPane helpBox = new StackPane(help);
        helpBox.setPadding(new Insets(8, 16, 8, 16));

        // ── Root ──────────────────────────────────────────────────────────
        VBox root = new VBox(header, videoRow, roomControls, callControls, helpBox);
        VBox.setVgrow(videoRow, Priority.ALWAYS);
        root.setStyle("-fx-background-color: #0f0f1a;");
        return root;
    }

    private void setupImageView(ImageView iv, double w, double h) {
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(true);
        iv.setStyle("-fx-background-color: #000;");
        // Placeholder — draw a black rectangle
    }

    private void initVideoService() {
        String username = SessionState.getInstance().getUsername();
        videoService = new VideoService(
                SessionState.HOST, SessionState.VIDEO_PORT, username,
                img -> localView.setImage(img),
                img -> remoteView.setImage(img),
                msg -> {
                    statusLabel.setText(msg);
                    // Parse ROOM_ID from status for display
                    if (msg.startsWith("Room created")) {
                        String[] parts = msg.split("ID: ");
                        if (parts.length > 1) {
                            String id = parts[1].trim();
                            Platform.runLater(() -> {
                                roomIdLabel.setText("Room ID: " + id);
                                leaveBtn.setDisable(false);
                            });
                        }
                    } else if (msg.contains("Peer connected")) {
                        Platform.runLater(() -> leaveBtn.setDisable(false));
                    } else if (msg.contains("left") || msg.contains("Disconnected")) {
                        Platform.runLater(() -> leaveBtn.setDisable(true));
                    }
                }
        );

        // Connect in background
        new Thread(() -> {
            try {
                videoService.connect();
                Platform.runLater(() -> statusLabel.setText("● Connected to video server"));
                Platform.runLater(() -> statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px;"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("✘ Video server unreachable: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                });
            }
        }, "video-connect").start();
    }

    private void toggleMute() {
        muted = !muted;
        muteBtn.setText(muted ? "🔇  Unmute" : "🎤  Mute");
        muteBtn.setStyle(muteBtn.getStyle().replace(
                muted ? "#374151" : "#7c3aed",
                muted ? "#7c3aed" : "#374151"));
        // Note: actual mic muting implementation would involve javax.sound TargetDataLine
    }

    private static void styleButton(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                     "-fx-font-size: 13px; -fx-font-weight: bold; " +
                     "-fx-background-radius: 8; -fx-padding: 9 18; -fx-cursor: hand;");
    }
}
