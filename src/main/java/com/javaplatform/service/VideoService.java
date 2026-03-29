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

    // Callbacks to update JavaFX UI
    private final Consumer<javafx.scene.image.Image> localFrameConsumer;
    private final Consumer<javafx.scene.image.Image> remoteFrameConsumer;
    private final Consumer<String>                   statusConsumer;

    public VideoService(String host, int port, String username,
                        Consumer<javafx.scene.image.Image> localFrameConsumer,
                        Consumer<javafx.scene.image.Image> remoteFrameConsumer,
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
        socket = new Socket(host, port);
        in     = new DataInputStream(socket.getInputStream());
        out    = new DataOutputStream(socket.getOutputStream());
        connected = true;
        sendText("REGISTER:" + username);
        startReceiving();
    }

    /** Create a new room; returns the room ID via the server TEXT response. */
    public void createRoom() { sendText("CREATE_ROOM"); }

    /** Join an existing room. */
    public void joinRoom(String roomId) { sendText("JOIN_ROOM:" + roomId.trim()); }

    /** Leave the current room. */
    public void leaveRoom() {
        sendText("LEAVE_ROOM");
        stopCapture();
    }

    /** Start sending webcam frames. */
    public void startCapture() {
        if (captureScheduler != null && !captureScheduler.isShutdown()) return; // Already running

        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                notifyStatus("⚠ No webcam detected.");
                return;
            }
            if (!webcam.isOpen()) {
                webcam.setViewSize(WebcamResolution.QVGA.getSize());
                webcam.open();
            }
        } catch (Exception e) {
            notifyStatus("⚠ Webcam error: " + e.getMessage());
            return;
        }

        captureScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "webcam-capture");
            t.setDaemon(true);
            return t;
        });
        long intervalMs = 1000L / FPS;
        captureScheduler.scheduleAtFixedRate(this::captureAndSend, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    /** Toggle camera on/off without disconnecting. */
    public void toggleCamera() {
        cameraEnabled = !cameraEnabled;
        notifyStatus(cameraEnabled ? "📷 Camera On" : "🎥 Camera Off (Privacy Mode)");
    }

    public boolean isCameraEnabled() { return cameraEnabled; }

    private void captureAndSend() {
        if (!connected || webcam == null || !webcam.isOpen()) return;
        try {
            BufferedImage frame = cameraEnabled ? webcam.getImage() : blankFrame();
            if (frame == null) return;

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

    private BufferedImage blankFrame() {
        BufferedImage img = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(0, 0, 320, 240);
        g.setColor(java.awt.Color.DARK_GRAY);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        g.drawString("Camera Off", 110, 125);
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
                        // Decode JPEG → JavaFX Image
                        BufferedImage bImg = ImageIO.read(new ByteArrayInputStream(payload));
                        if (bImg != null) {
                            javafx.scene.image.Image fxImg = SwingFXUtils.toFXImage(bImg, null);
                            Platform.runLater(() -> remoteFrameConsumer.accept(fxImg));
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
        } else if (msg.equals("PEER_CONNECTED")) {
            notifyStatus("Peer connected! Starting capture...");
            startCapture();
        } else if (msg.equals("PEER_DISCONNECTED")) {
            notifyStatus("Peer disconnected.");
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
        } catch (IOException ignored) {}
    }

    private synchronized void sendFrame(byte[] frame) {
        try {
            out.writeInt(TYPE_FRAME);
            out.writeInt(frame.length);
            out.write(frame);
            out.flush();
        } catch (IOException ignored) {}
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
        if (captureScheduler != null) captureScheduler.shutdownNow();
        if (webcam != null && webcam.isOpen()) webcam.close();
    }
}
