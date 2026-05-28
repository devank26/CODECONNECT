package com.javaplatform.service;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Manages webcam capture, JPEG encoding, frame sending to VideoRelayServer,
 * and receiving / decoding remote frames for display in the UI.
 */
public class VideoService {

    private static final int TYPE_TEXT  = 1;
    private static final int TYPE_FRAME = 2;
    private static final int FPS        = 12;

    private final String host;
    private final int    port;
    private final String username;

    private Socket           socket;
    private DataInputStream  in;
    private DataOutputStream out;

    private Webcam webcam;
    private ScheduledExecutorService captureScheduler;
    private ExecutorService          receiveExecutor;

    private volatile boolean cameraEnabled = true;
    private volatile boolean connected     = false;
    private volatile boolean capturing     = false;

    // Callbacks to update JavaFX UI
    private final Consumer<javafx.scene.image.Image> localFrameConsumer;
    private final java.util.function.BiConsumer<String, javafx.scene.image.Image> remoteFrameConsumer;
    private final Consumer<String>                   statusConsumer;

    public VideoService(String host, int port, String username,
                        Consumer<javafx.scene.image.Image> localFrameConsumer,
                        java.util.function.BiConsumer<String, javafx.scene.image.Image> remoteFrameConsumer,
                        Consumer<String>                   statusConsumer) {
        this.host                = host;
        this.port                = port;
        this.username            = username;
        this.localFrameConsumer  = localFrameConsumer;
        this.remoteFrameConsumer = remoteFrameConsumer;
        this.statusConsumer      = statusConsumer;
    }

    /** Connect to the relay server and register username. */
    public void connect() throws IOException {
        connect(host);
    }

    public void connect(String targetHost) throws IOException {
        socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(targetHost, port), 1500);
        in     = new DataInputStream(socket.getInputStream());
        out    = new DataOutputStream(socket.getOutputStream());
        connected = true;
        sendText("REGISTER:" + username);
        startReceiving();
    }

    /**
     * Connect to a WAN relay at an arbitrary host:port (SSH tunnel endpoint).
     * Use this when the peer's Room ID contains a relay suffix (e.g. serveo.net:PORT).
     */
    public void connectToRelay(String relayHost, int relayPort) throws IOException {
        socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(relayHost, relayPort), 8000);
        in     = new DataInputStream(socket.getInputStream());
        out    = new DataOutputStream(socket.getOutputStream());
        connected = true;
        sendText("REGISTER:" + username);
        startReceiving();
    }

    /** Create a new room; returns the room ID via the server TEXT response. */
    public void createRoom(String roomId) { sendText("CREATE_ROOM:" + roomId.trim()); }

    /** Join an existing room. */
    public void joinRoom(String roomId) { sendText("JOIN_ROOM:" + roomId.trim()); }

    /** Leave the current room. */
    public void leaveRoom() {
        sendText("LEAVE_ROOM");
        stopCapture();
    }

    /** Start sending webcam frames. */
    public void startCapture() {
        if (capturing) return;
        capturing = true;

        Thread initThread = new Thread(() -> {
            try {
                webcam = Webcam.getDefault();
                if (webcam != null && !webcam.isOpen()) {
                    webcam.setViewSize(WebcamResolution.QVGA.getSize());
                    webcam.open();
                }
            } catch (Exception e) {
                notifyStatus("⚠ Webcam error: " + e.getMessage());
            }

            synchronized (this) {
                if (capturing && (captureScheduler == null || captureScheduler.isShutdown())) {
                    captureScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "webcam-capture");
                        t.setDaemon(true);
                        return t;
                    });
                    long intervalMs = 1000L / FPS;
                    captureScheduler.scheduleAtFixedRate(this::captureAndSend, 0, intervalMs, TimeUnit.MILLISECONDS);
                }
            }
        }, "webcam-init");
        initThread.setDaemon(true);
        initThread.start();
    }

    /** Toggle camera on/off without disconnecting. */
    public void toggleCamera() {
        cameraEnabled = !cameraEnabled;
        notifyStatus(cameraEnabled ? "📷 Camera On" : "🎥 Camera Off (Privacy Mode)");
    }

    public boolean isCameraEnabled() { return cameraEnabled; }

    private void captureAndSend() {
        if (!connected) return;
        try {
            BufferedImage frame;
            if (cameraEnabled && webcam != null && webcam.isOpen()) {
                frame = webcam.getImage();
            } else if (!cameraEnabled) {
                frame = blankFrame("Camera Off");
            } else if (webcam != null) {
                frame = blankFrame("Camera In Use");
            } else {
                frame = blankFrame("No Webcam");
            }
            if (frame == null) frame = blankFrame("No Webcam");

            // Update local preview
            if (localFrameConsumer != null) {
                javafx.scene.image.Image fxImg = SwingFXUtils.toFXImage(frame, null);
                Platform.runLater(() -> localFrameConsumer.accept(fxImg));
            }

            // Encode as JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(frame, "JPEG", baos);
            byte[] jpeg = baos.toByteArray();

            sendFrame(jpeg);
        } catch (Exception e) {
            // best-effort; ignore transient frame failures
        }
    }

    private BufferedImage blankFrame(String text) {
        BufferedImage img = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(0, 0, 320, 240);
        g.setColor(java.awt.Color.DARK_GRAY);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        int x = 160 - (text.length() * 4);
        g.drawString(text, x, 125);
        g.dispose();
        return img;
    }

    private void startReceiving() {
        receiveExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "video-receive");
            t.setDaemon(true);
            return t;
        });
        receiveExecutor.submit(() -> {
            try {
                while (connected) {
                    int type   = in.readInt();
                    int length = in.readInt();
                    byte[] payload = new byte[length];
                    in.readFully(payload);

                    if (type == TYPE_TEXT) {
                        String msg = new String(payload, "UTF-8");
                        handleServerMessage(msg);
                    } else if (type == TYPE_FRAME && remoteFrameConsumer != null) {
                        try {
                            DataInputStream payloadStream = new DataInputStream(new ByteArrayInputStream(payload));
                            int nameLength = payloadStream.readInt();
                            byte[] nameBytes = new byte[nameLength];
                            payloadStream.readFully(nameBytes);
                            String peerName = new String(nameBytes, "UTF-8");

                            int frameLength = length - 4 - nameLength;
                            byte[] frameBytes = new byte[frameLength];
                            payloadStream.readFully(frameBytes);

                            BufferedImage bImg = ImageIO.read(new ByteArrayInputStream(frameBytes));
                            if (bImg != null) {
                                javafx.scene.image.Image fxImg = SwingFXUtils.toFXImage(bImg, null);
                                Platform.runLater(() -> remoteFrameConsumer.accept(peerName, fxImg));
                            }
                        } catch (Exception decodeEx) {
                            // Ignore frame decoding errors so the stream doesn't crash
                        }
                    }
                }
            } catch (IOException e) {
                if (connected) notifyStatus("Disconnected from video server.");
            }
        });
    }

    private void handleServerMessage(String msg) {
        if (msg.startsWith("ROOM_ID:")) {
            String roomId = msg.substring(8);
            notifyStatus("Room created. ID: " + roomId);
            startCapture();
        } else if (msg.startsWith("PEER_CONNECTED")) {
            notifyStatus(msg);
            startCapture();
        } else if (msg.startsWith("PEER_DISCONNECTED")) {
            notifyStatus(msg);
            stopCapture();
        } else if (msg.equals("ROOM_NOT_FOUND")) {
            notifyStatus("Room not found. Check the room ID.");
        } else if (msg.equals("ROOM_LEFT")) {
            notifyStatus("You left the room.");
        }
    }

    private synchronized void sendText(String text) {
        try {
            byte[] data = text.getBytes("UTF-8");
            out.writeInt(TYPE_TEXT);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        } catch (IOException e) {
            handleSendError(e);
        }
    }

    private synchronized void sendFrame(byte[] frame) {
        try {
            out.writeInt(TYPE_FRAME);
            out.writeInt(frame.length);
            out.write(frame);
            out.flush();
        } catch (IOException e) {
            handleSendError(e);
        }
    }

    private void handleSendError(IOException e) {
        if (connected) {
            disconnect();
            notifyStatus("Disconnected from video server: " + e.getMessage());
        }
    }

    private void notifyStatus(String msg) {
        if (statusConsumer != null) {
            Platform.runLater(() -> statusConsumer.accept(msg));
        }
    }

    /** Full disconnect and cleanup. */
    public void disconnect() {
        connected = false;
        stopCapture();
        if (receiveExecutor != null) receiveExecutor.shutdownNow();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private void stopCapture() {
        capturing = false;
        synchronized (this) {
            if (captureScheduler != null) {
                captureScheduler.shutdownNow();
                captureScheduler = null;
            }
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }
}
