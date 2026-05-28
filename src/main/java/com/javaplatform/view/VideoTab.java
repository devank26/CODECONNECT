package com.javaplatform.view;

import com.javaplatform.SessionState;
import com.javaplatform.ThemeManager;
import com.javaplatform.service.VideoService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

/**
 * Video call tab.
 * Uses VideoService to connect to VideoRelayServer,
 * capture webcam frames, and display local + remote video.
 */
public class VideoTab extends Tab {

    private final ImageView localView  = new ImageView();
    private final FlowPane  videoGrid   = new FlowPane(javafx.geometry.Orientation.HORIZONTAL, 15, 15);
    private final ScrollPane gridScroll;
    private final java.util.Map<String, VBox> remoteVideoBoxes = new java.util.concurrent.ConcurrentHashMap<>();
    private final Label     statusLabel = new Label("Not connected");
    private final TextField roomField   = new TextField();
    private final Label     roomIdLabel = new Label("");
    private final Label     ipLabel     = new Label("");
    private final Button    muteBtn    = new Button("🎤  Mute");
    private final Button    camBtn     = new Button("📷  Camera Off");
    private final Button    createBtn  = new Button("Create Room");
    private final Button    joinBtn    = new Button("Join Room");
    private final Button    leaveBtn   = new Button("Leave Room");
    private final Button    reconnectBtn = new Button("🔄 Reconnect");

    private HBox header;
    private Label title;
    private VBox localBox;
    private Label localLabel;
    private HBox roomControls;
    private HBox callControls;
    private Label help;
    private StackPane helpBox;
    private VBox root;

    private VideoService videoService;
    private boolean muted = false;

    public VideoTab() {
        super("📹  Video Call");
        setClosable(false);
        gridScroll = new ScrollPane();
        setContent(buildUI());
        initVideoService();

        ThemeManager.getInstance().addThemeListener(this::applyTheme);
        applyTheme();
    }

    private Pane buildUI() {
        // ── Header ────────────────────────────────────────────────────────
        title = new Label("Video Call");

        reconnectBtn.setOnAction(e -> initVideoService());

        header = new HBox(16, title, statusLabel, reconnectBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));

        // ── Local video ───────────────────────────────────────────────────
        setupImageView(localView, 240, 180);
        localLabel = new Label("You — " + SessionState.getInstance().getUsername());
        localBox = new VBox(6, localLabel, localView);
        localBox.setAlignment(Pos.CENTER);
        localBox.setPadding(new Insets(10));
 
        // ── Video Grid (FlowPane inside ScrollPane) ───────────────────────
        videoGrid.setAlignment(Pos.CENTER);
        videoGrid.setPadding(new Insets(15));
        videoGrid.getChildren().add(localBox);
 
        gridScroll.setContent(videoGrid);
        gridScroll.setFitToWidth(true);
        gridScroll.setFitToHeight(true);
        VBox.setVgrow(gridScroll, Priority.ALWAYS);

        // ── Room controls ─────────────────────────────────────────────────
        createBtn.setOnAction(e -> {
            createBtn.setDisable(true);
            createBtn.setText("⏳ Resolving...");
            new Thread(() -> {
                String localIp = getLocalIPAddress();
                String publicIp = getPublicIPAddress();
                String myRoomId = encodeIpsToRoomId(localIp, publicIp);
                Platform.runLater(() -> {
                    createBtn.setDisable(false);
                    createBtn.setText("Create Room");
                    reconnectVideoAndChat("localhost", myRoomId, true);
                });
            }, "public-ip-resolver").start();
        });

        roomField.setPromptText("Room ID…");
        roomField.setMaxWidth(120);

        joinBtn.setOnAction(e -> {
            String enteredRoomId = roomField.getText().trim();
            if (!enteredRoomId.isEmpty()) {
                reconnectVideoAndChat("", enteredRoomId, false);
            }
        });

        leaveBtn.setOnAction(e -> videoService.leaveRoom());
        leaveBtn.setDisable(true);

        roomControls = new HBox(10, createBtn, new Separator(),
                roomField, joinBtn, new Separator(), leaveBtn, roomIdLabel, ipLabel);
        roomControls.setAlignment(Pos.CENTER);
        roomControls.setPadding(new Insets(10));

        new Thread(() -> {
            String localIp = getLocalIPAddress();
            Platform.runLater(() -> ipLabel.setText(" |  Your IP: " + localIp + " (LAN)"));
        }, "ui-ip-resolver").start();

        // ── Call controls ─────────────────────────────────────────────────
        muteBtn.setOnAction(e -> toggleMute());

        camBtn.setOnAction(e -> {
            if (videoService != null) {
                videoService.toggleCamera();
                boolean on = videoService.isCameraEnabled();
                camBtn.setText(on ? "📷  Camera Off" : "📷  Camera On");
                applyTheme();
            }
        });

        callControls = new HBox(12, muteBtn, camBtn);
        callControls.setAlignment(Pos.CENTER);
        callControls.setPadding(new Insets(6, 10, 10, 10));

        // ── Help text ─────────────────────────────────────────────────────
        help = new Label(
                "1. Click 'Create Room' → share the Room ID with your peer\n" +
                "2. Your peer pastes the ID, clicks 'Join Room'\n" +
                "3. Both users see each other's video automatically");
        help.setWrapText(true);
        helpBox = new StackPane(help);
        helpBox.setPadding(new Insets(8, 16, 8, 16));

        // ── Root ──────────────────────────────────────────────────────────
        root = new VBox(header, gridScroll, roomControls, callControls, helpBox);
        VBox.setVgrow(gridScroll, Priority.ALWAYS);
        return root;
    }

    private void applyTheme() {
        ThemeManager tm = ThemeManager.getInstance();

        // Containers
        root.setStyle("-fx-background-color: " + tm.bgApp() + ";");
        header.setStyle("-fx-background-color: " + tm.bgCard() + "; -fx-border-color: " + tm.border() + "; -fx-border-width: 0 0 1 0;");

        // Labels
        title.setStyle(tm.getLabelStyle(18, true, false));
        statusLabel.setStyle("-fx-text-fill: " + tm.textMuted() + "; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");

        localLabel.setStyle("-fx-text-fill: " + tm.accent() + "; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");

        localBox.setStyle("-fx-background-color: " + tm.bgCard() + "; -fx-background-radius: 12; -fx-border-color: " + tm.border() + "; -fx-border-radius: 12;");
        videoGrid.setStyle("-fx-background-color: " + tm.bgApp() + ";");
        gridScroll.setStyle("-fx-background-color: " + tm.bgApp() + "; -fx-background: " + tm.bgApp() + ";");
 
        for (VBox rBox : remoteVideoBoxes.values()) {
            rBox.setStyle("-fx-background-color: " + tm.bgCard() + "; -fx-background-radius: 12; -fx-border-color: " + tm.border() + "; -fx-border-radius: 12;");
            if (rBox.getChildren().size() > 0 && rBox.getChildren().get(0) instanceof Label) {
                Label lbl = (Label) rBox.getChildren().get(0);
                lbl.setStyle("-fx-text-fill: " + tm.cyan() + "; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");
            }
        }

        // Room Controls
        roomControls.setStyle("-fx-background-color: " + tm.bgApp() + ";");
        roomField.setStyle(tm.getTextFieldStyle(13));
        roomIdLabel.setStyle("-fx-text-fill: " + tm.warningColor() + "; -fx-font-size: 13px; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");
        ipLabel.setStyle("-fx-text-fill: " + tm.textMuted() + "; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");

        // Buttons
        createBtn.setStyle(tm.getButtonStyle(tm.runColor()));
        joinBtn.setStyle(tm.getButtonStyle(tm.accent()));
        leaveBtn.setStyle(tm.getButtonStyle(tm.errorColor()));
        reconnectBtn.setStyle(tm.getButtonStyle(tm.isDarkMode() ? "#374151" : "#cbd5e1") + " -fx-text-fill: " + tm.textPrimary() + "; -fx-font-size: 11px; -fx-padding: 4 8;");

        // Hover bindings
        createBtn.setOnMouseEntered(e -> createBtn.setStyle(tm.getButtonStyle(tm.runColorHover())));
        createBtn.setOnMouseExited(e -> createBtn.setStyle(tm.getButtonStyle(tm.runColor())));

        joinBtn.setOnMouseEntered(e -> joinBtn.setStyle(tm.getButtonStyle(tm.accentHover())));
        joinBtn.setOnMouseExited(e -> joinBtn.setStyle(tm.getButtonStyle(tm.accent())));

        // Call Controls
        callControls.setStyle("-fx-background-color: " + tm.bgApp() + ";");
        String callBg = tm.isDarkMode() ? "#374151" : "#cbd5e1";
        String callBgHover = tm.isDarkMode() ? "#4b5563" : "#94a3b8";

        muteBtn.setStyle(tm.getButtonStyle(muted ? tm.accent() : callBg) + " -fx-text-fill: " + (muted ? "white" : tm.textPrimary()) + ";");
        muteBtn.setOnMouseEntered(e -> muteBtn.setStyle(tm.getButtonStyle(muted ? tm.accentHover() : callBgHover) + " -fx-text-fill: " + (muted ? "white" : tm.textPrimary()) + ";"));
        muteBtn.setOnMouseExited(e -> muteBtn.setStyle(tm.getButtonStyle(muted ? tm.accent() : callBg) + " -fx-text-fill: " + (muted ? "white" : tm.textPrimary()) + ";"));

        boolean camOn = videoService == null || videoService.isCameraEnabled();
        camBtn.setStyle(tm.getButtonStyle(camOn ? callBg : tm.accent()) + " -fx-text-fill: " + (camOn ? tm.textPrimary() : "white") + ";");
        camBtn.setOnMouseEntered(e -> camBtn.setStyle(tm.getButtonStyle(camOn ? callBgHover : tm.accentHover()) + " -fx-text-fill: " + (camOn ? tm.textPrimary() : "white") + ";"));
        camBtn.setOnMouseExited(e -> camBtn.setStyle(tm.getButtonStyle(camOn ? callBg : tm.accent()) + " -fx-text-fill: " + (camOn ? tm.textPrimary() : "white") + ";"));

        // Help
        help.setStyle("-fx-text-fill: " + tm.textMuted() + "; -fx-font-size: 11px; -fx-font-family: 'Segoe UI';");
        helpBox.setStyle("-fx-background-color: " + tm.bgApp() + ";");
    }

    private void setupImageView(ImageView iv, double w, double h) {
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(true);
        iv.setStyle("-fx-background-color: #000;");
    }

    private void initVideoService() {
        Platform.runLater(() -> {
            ThemeManager tm = ThemeManager.getInstance();
            statusLabel.setText("⚡ Connecting to video server…");
            statusLabel.setStyle("-fx-text-fill: " + tm.textMuted() + "; -fx-font-size: 12px;");
            reconnectBtn.setVisible(false);
            reconnectBtn.setManaged(false);
        });

        if (videoService != null) {
            videoService.disconnect();
        }

        String username = SessionState.getInstance().getUsername();
        videoService = new VideoService(
                SessionState.getInstance().getServerHost(), SessionState.VIDEO_PORT, username,
                img -> localView.setImage(img),
                (peerName, img) -> updateRemoteFrame(peerName, img),
                msg -> {
                    Platform.runLater(() -> statusLabel.setText(msg));
                    if (msg.startsWith("Room created")) {
                        String[] parts = msg.split("ID: ");
                        if (parts.length > 1) {
                            String id = parts[1].trim();
                            Platform.runLater(() -> {
                                roomIdLabel.setText("Room ID: " + id);
                                leaveBtn.setDisable(false);
                            });
                        }
                    } else if (msg.startsWith("PEER_CONNECTED:")) {
                        String peerName = msg.substring(15);
                        Platform.runLater(() -> {
                            addRemoteVideoView(peerName);
                            leaveBtn.setDisable(false);
                            if (roomIdLabel.getText().isEmpty() && !roomField.getText().trim().isEmpty()) {
                                roomIdLabel.setText("Room ID: " + roomField.getText().trim());
                            }
                        });
                    } else if (msg.startsWith("PEER_DISCONNECTED:")) {
                        String peerName = msg.substring(18);
                        Platform.runLater(() -> removeRemoteVideoView(peerName));
                    } else if (msg.contains("left") || msg.contains("Disconnected") || msg.contains("unreachable")) {
                        Platform.runLater(() -> {
                            leaveBtn.setDisable(true);
                            roomIdLabel.setText("");
                            localView.setImage(null);
                            clearAllRemoteViews();
                            reconnectBtn.setVisible(true);
                            reconnectBtn.setManaged(true);
                        });
                    }
                }
        );

        // Connect in background
        final VideoService serviceInstance = videoService;
        new Thread(() -> {
            try {
                serviceInstance.connect();
                Platform.runLater(() -> {
                    ThemeManager tm = ThemeManager.getInstance();
                    statusLabel.setText("● Connected to video server");
                    statusLabel.setStyle("-fx-text-fill: " + tm.runColor() + "; -fx-font-size: 12px;");
                    reconnectBtn.setVisible(false);
                    reconnectBtn.setManaged(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    ThemeManager tm = ThemeManager.getInstance();
                    statusLabel.setText("✘ Video server unreachable: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: " + tm.errorColor() + "; -fx-font-size: 12px;");
                    reconnectBtn.setVisible(true);
                    reconnectBtn.setManaged(true);
                    localView.setImage(null);
                    clearAllRemoteViews();
                });
            }
        }, "video-connect").start();
    }

    private void toggleMute() {
        muted = !muted;
        muteBtn.setText(muted ? "🔇  Unmute" : "🎤  Mute");
        applyTheme();
    }

    private void addRemoteVideoView(String peerName) {
        if (remoteVideoBoxes.containsKey(peerName)) return;

        ImageView view = new ImageView();
        setupImageView(view, 240, 180);
        Label label = new Label(peerName);

        VBox box = new VBox(6, label, view);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));

        remoteVideoBoxes.put(peerName, box);
        Platform.runLater(() -> {
            videoGrid.getChildren().add(box);
            applyTheme();
        });
    }

    private void removeRemoteVideoView(String peerName) {
        VBox box = remoteVideoBoxes.remove(peerName);
        if (box != null) {
            Platform.runLater(() -> videoGrid.getChildren().remove(box));
        }
    }

    private void clearAllRemoteViews() {
        remoteVideoBoxes.clear();
        Platform.runLater(() -> {
            videoGrid.getChildren().clear();
            videoGrid.getChildren().add(localBox);
        });
    }

    private void updateRemoteFrame(String peerName, javafx.scene.image.Image img) {
        VBox box = remoteVideoBoxes.get(peerName);
        if (box == null) {
            addRemoteVideoView(peerName);
            box = remoteVideoBoxes.get(peerName);
        }
        if (box != null && box.getChildren().size() > 1) {
            ImageView iv = (ImageView) box.getChildren().get(1);
            Platform.runLater(() -> iv.setImage(img));
        }
    }

    public static String getPublicIPAddress() {
        String[] services = {
            "https://api.ipify.org",
            "http://checkip.amazonaws.com",
            "https://ipv4.icanhazip.com"
        };
        for (String service : services) {
            try (java.io.BufferedReader in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.net.URL(service).openStream(), "UTF-8"))) {
                String ip = in.readLine().trim();
                if (ip.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
                    System.out.println("[VideoTab] Successfully resolved public IP: " + ip + " via " + service);
                    return ip;
                }
            } catch (Exception ignored) {}
        }
        System.out.println("[VideoTab] Public IP resolution failed. Falling back to active local IP.");
        return getLocalIPAddress();
    }

    public static String getLocalIPAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                String name = iface.getName().toLowerCase();
                String displayName = iface.getDisplayName().toLowerCase();
                if (name.contains("vbox") || name.contains("virtual") || name.contains("vmnet") || name.contains("wsl") ||
                    name.contains("vpn") || name.contains("tun") || name.contains("tap") || name.contains("ppp") || name.contains("proton") ||
                    displayName.contains("vbox") || displayName.contains("virtual") || displayName.contains("vmnet") || displayName.contains("wsl") ||
                    displayName.contains("vpn") || displayName.contains("tun") || displayName.contains("tap") || displayName.contains("ppp") || displayName.contains("proton")) {
                    continue;
                }
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (!ip.equals("127.0.0.1") && !ip.equals("0.0.0.0")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            socket.connect(java.net.InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            if (ip != null && !ip.isEmpty() && !ip.equals("0.0.0.0")) {
                return ip;
            }
        } catch (Exception ignored) {}
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public static String encodeIpsToRoomId(String localIp, String publicIp) {
        try {
            byte[] ip1 = ipToBytes(publicIp);
            byte[] ip2 = ipToBytes(localIp);
            byte[] salt = new byte[2];
            new java.security.SecureRandom().nextBytes(salt);
            
            byte[] combined = new byte[11];
            System.arraycopy(ip1, 0, combined, 1, 4);
            System.arraycopy(ip2, 0, combined, 5, 4);
            System.arraycopy(salt, 0, combined, 9, 2);
            
            java.math.BigInteger bi = new java.math.BigInteger(combined);
            return bi.toString(36).toUpperCase();
        } catch (Exception e) {
            return "LOCAL";
        }
    }

    public static String[] decodeRoomIdToIps(String roomId) {
        try {
            roomId = roomId.trim().toUpperCase();
            if (roomId.length() <= 8) {
                long val = Long.parseUnsignedLong(roomId.toLowerCase(), 36);
                String ip = longToIp(val);
                return new String[] { ip, ip };
            }
            java.math.BigInteger bi = new java.math.BigInteger(roomId.toLowerCase(), 36);
            byte[] bytes = bi.toByteArray();
            byte[] target = new byte[11];
            if (bytes.length <= 11) {
                System.arraycopy(bytes, 0, target, 11 - bytes.length, bytes.length);
            } else {
                System.arraycopy(bytes, bytes.length - 11, target, 0, 11);
            }
            String publicIp = bytesToIp(target, 1);
            String localIp = bytesToIp(target, 5);
            return new String[] { publicIp, localIp };
        } catch (Exception e) {
            return new String[] { "127.0.0.1", "127.0.0.1" };
        }
    }

    private static byte[] ipToBytes(String ip) {
        try {
            String[] parts = ip.split("\\.");
            byte[] bytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                bytes[i] = (byte) (Integer.parseInt(parts[i]) & 0xFF);
            }
            return bytes;
        } catch (Exception e) {
            return new byte[]{127, 0, 0, 1};
        }
    }

    private static String bytesToIp(byte[] bytes, int offset) {
        int p1 = bytes[offset] & 0xFF;
        int p2 = bytes[offset + 1] & 0xFF;
        int p3 = bytes[offset + 2] & 0xFF;
        int p4 = bytes[offset + 3] & 0xFF;
        return p1 + "." + p2 + "." + p3 + "." + p4;
    }

    private static java.util.List<String> discoverLocalHosts() {
        java.util.List<String> found = new java.util.concurrent.CopyOnWriteArrayList<>();
        String localIp = getLocalIPAddress();
        if (localIp == null || localIp.equals("127.0.0.1") || !localIp.contains(".")) {
            return found;
        }
        int lastDot = localIp.lastIndexOf('.');
        String prefix = localIp.substring(0, lastDot + 1);
        
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(64);
        java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 1; i <= 254; i++) {
            final String targetIp = prefix + i;
            if (targetIp.equals(localIp)) continue;
            futures.add(executor.submit(() -> {
                try (java.net.Socket s = new java.net.Socket()) {
                    s.connect(new java.net.InetSocketAddress(targetIp, SessionState.VIDEO_PORT), 300);
                    found.add(targetIp);
                } catch (Exception ignored) {}
            }));
        }
        for (java.util.concurrent.Future<?> f : futures) {
            try {
                f.get(400, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {}
        }
        executor.shutdownNow();
        return found;
    }

    private static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long val = 0;
        for (int i = 0; i < 4; i++) {
            val = (val << 8) | (Integer.parseInt(parts[i]) & 0xFF);
        }
        return val;
    }

    private static String longToIp(long val) {
        int p1 = (int) ((val >> 24) & 0xFF);
        int p2 = (int) ((val >> 16) & 0xFF);
        int p3 = (int) ((val >> 8) & 0xFF);
        int p4 = (int) (val & 0xFF);
        return p1 + "." + p2 + "." + p3 + "." + p4;
    }

    private void reconnectVideoAndChat(String host, String roomId, boolean isCreate) {
        if (videoService != null) {
            videoService.disconnect();
        }

        Platform.runLater(() -> {
            ThemeManager tm = ThemeManager.getInstance();
            statusLabel.setText("⚡ Connecting to video server…");
            statusLabel.setStyle("-fx-text-fill: " + tm.textMuted() + "; -fx-font-size: 12px;");
            reconnectBtn.setVisible(false);
            reconnectBtn.setManaged(false);
        });

        String username = SessionState.getInstance().getUsername();
        
        final String localTarget;
        final String publicTarget;
        final String actualRoomId;
        if (isCreate || host.equals("localhost")) {
            localTarget = "localhost";
            publicTarget = "localhost";
            actualRoomId = roomId;
        } else if (roomId.matches("^(\\d{1,3}\\.){3}\\d{1,3}$") || roomId.equalsIgnoreCase("localhost")) {
            localTarget = roomId;
            publicTarget = roomId;
            actualRoomId = "DIRECT_" + roomId.replace(".", "_");
        } else {
            String[] decoded = decodeRoomIdToIps(roomId);
            publicTarget = decoded[0];
            localTarget = decoded[1];
            actualRoomId = roomId;
        }

        final VideoService serviceInstance = new VideoService(
                "localhost", SessionState.VIDEO_PORT, username,
                img -> localView.setImage(img),
                (peerName, img) -> updateRemoteFrame(peerName, img),
                msg -> {
                    Platform.runLater(() -> statusLabel.setText(msg));
                    if (msg.startsWith("Room created")) {
                        String[] parts = msg.split("ID: ");
                        if (parts.length > 1) {
                            String id = parts[1].trim();
                            Platform.runLater(() -> {
                                roomIdLabel.setText("Room ID: " + id);
                                leaveBtn.setDisable(false);
                            });
                        }
                    } else if (msg.startsWith("PEER_CONNECTED:")) {
                        String peerName = msg.substring(15);
                        Platform.runLater(() -> {
                            addRemoteVideoView(peerName);
                            leaveBtn.setDisable(false);
                            if (roomIdLabel.getText().isEmpty() && !roomField.getText().trim().isEmpty()) {
                                roomIdLabel.setText("Room ID: " + roomField.getText().trim());
                            }
                        });
                    } else if (msg.startsWith("PEER_DISCONNECTED:")) {
                        String peerName = msg.substring(18);
                        Platform.runLater(() -> removeRemoteVideoView(peerName));
                    } else if (msg.contains("left") || msg.contains("Disconnected") || msg.contains("unreachable")) {
                        Platform.runLater(() -> {
                            leaveBtn.setDisable(true);
                            roomIdLabel.setText("");
                            localView.setImage(null);
                            clearAllRemoteViews();
                            reconnectBtn.setVisible(true);
                            reconnectBtn.setManaged(true);
                        });
                    }
                }
        );
        videoService = serviceInstance;

        new Thread(() -> {
            boolean connected = false;
            String workingHost = "localhost";

            // 1. Try local IP first
            try {
                System.out.println("[VideoTab] Attempting connection to local target: " + localTarget);
                serviceInstance.connect(localTarget);
                connected = true;
                workingHost = localTarget;
            } catch (Exception localEx) {
                System.out.println("[VideoTab] Local connection failed: " + localEx.getMessage());
            }

            // 2. Try public IP if local failed and they are different (and not 0.0.0.0)
            if (!connected && !publicTarget.equals(localTarget) && !publicTarget.equals("0.0.0.0")) {
                try {
                    System.out.println("[VideoTab] Attempting connection to public target: " + publicTarget);
                    serviceInstance.connect(publicTarget);
                    connected = true;
                    workingHost = publicTarget;
                } catch (Exception publicEx) {
                    System.out.println("[VideoTab] Public connection failed: " + publicEx.getMessage());
                }
            }

            // 3. Fallback: discover local hosts on the subnet if both direct connections failed
            if (!connected && !isCreate) {
                System.out.println("[VideoTab] Direct connections failed. Attempting local network discovery...");
                java.util.List<String> candidates = discoverLocalHosts();
                for (String candidate : candidates) {
                    try {
                        System.out.println("[VideoTab] Discovered local candidate: " + candidate + ". Attempting connection...");
                        serviceInstance.connect(candidate);
                        connected = true;
                        workingHost = candidate;
                        break;
                    } catch (Exception candidateEx) {
                        System.out.println("[VideoTab] Candidate connection failed: " + candidateEx.getMessage());
                    }
                }
            }

            final boolean success = connected;
            final String resolvedHost = workingHost;

            Platform.runLater(() -> {
                if (success) {
                    ThemeManager tm = ThemeManager.getInstance();
                    statusLabel.setText("● Connected to video server at " + resolvedHost);
                    statusLabel.setStyle("-fx-text-fill: " + tm.runColor() + "; -fx-font-size: 12px;");
                    
                    SessionState.getInstance().setServerHost(resolvedHost);
                    if (MainWindow.getChatTab() != null) {
                        MainWindow.getChatTab().reconnectToHost(resolvedHost);
                    }

                    if (isCreate) {
                        serviceInstance.createRoom(actualRoomId);
                    } else {
                        serviceInstance.joinRoom(actualRoomId);
                    }
                } else {
                    ThemeManager tm = ThemeManager.getInstance();
                    statusLabel.setText("✘ Video server unreachable on both local & public IPs");
                    statusLabel.setStyle("-fx-text-fill: " + tm.errorColor() + "; -fx-font-size: 12px;");
                    reconnectBtn.setVisible(true);
                    reconnectBtn.setManaged(true);
                    localView.setImage(null);
                    clearAllRemoteViews();
                }
            });
        }, "video-reconnect-join").start();
    }
}
