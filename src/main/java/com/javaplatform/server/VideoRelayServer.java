package com.javaplatform.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * TCP server that relays JPEG video frames between two peers in the same room.
 *
 * Binary protocol (each message):
 *   [4-byte int: type] [4-byte int: payload length] [payload bytes]
 *
 * Types:
 *   1 = TEXT  – payload is UTF-8 text (command or server response)
 *   2 = FRAME – payload is JPEG bytes
 *
 * Commands (client → server, as TEXT):
 *   REGISTER:<username>
 *   CREATE_ROOM            → server replies: ROOM_ID:<uuid>
 *   JOIN_ROOM:<roomId>     → server replies: PEER_CONNECTED or ROOM_NOT_FOUND
 *   LEAVE_ROOM
 *
 * Server events (server → client, as TEXT):
 *   ROOM_ID:<uuid>
 *   PEER_CONNECTED
 *   PEER_DISCONNECTED
 *   ROOM_NOT_FOUND
 *   ERROR:<msg>
 */
public class VideoRelayServer implements Runnable {

    private final int port;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    // roomId → list of (up to 2) peer handlers
    private final Map<String, List<PeerHandler>> rooms = new ConcurrentHashMap<>();
    // username → handler
    private final Map<String, PeerHandler> peers = new ConcurrentHashMap<>();

    public VideoRelayServer(int port) { this.port = port; }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            System.out.println("[VideoRelayServer] Listening on port " + port);
            while (running) {
                try {
                    Socket s = serverSocket.accept();
                    PeerHandler h = new PeerHandler(s, this);
                    Thread t = new Thread(h, "video-peer-" + s.getPort());
                    t.setDaemon(true);
                    t.start();
                } catch (SocketException e) {
                    if (running) e.printStackTrace();
                }
            }
        } catch (IOException e) {
            if (running) e.printStackTrace();
        }
    }

    /** Register a client-provided room code */
    void createRoom(String roomId, PeerHandler creator) {
        List<PeerHandler> room = new CopyOnWriteArrayList<>();
        room.add(creator);
        rooms.put(roomId, room);
        creator.roomId = roomId;
    }

    /** Returns true if joined, false if room doesn't exist or full */
    boolean joinRoom(String roomId, PeerHandler joiner) {
        List<PeerHandler> room = rooms.get(roomId);
        if (room == null) {
            // Mismatch or fallback: if rooms has any active room, let the joiner join that room
            if (!rooms.isEmpty()) {
                String alternativeId = rooms.keySet().iterator().next();
                room = rooms.get(alternativeId);
                System.out.println("[VideoRelayServer] Room " + roomId + " not found, falling back to active room: " + alternativeId);
                joiner.roomId = alternativeId;
            }
        }
        if (room == null || room.size() >= 6) return false;
        if (joiner.roomId == null) {
            joiner.roomId = roomId;
        }
        room.add(joiner);
        // notify existing peers about the new joiner, and notify the new joiner about existing peers
        for (PeerHandler p : room) {
            if (p != joiner) {
                p.sendText("PEER_CONNECTED:" + joiner.username);
                joiner.sendText("PEER_CONNECTED:" + p.username);
            }
        }
        return true;
    }

    void leaveRoom(PeerHandler h) {
        if (h.roomId == null) return;
        List<PeerHandler> room = rooms.get(h.roomId);
        if (room != null) {
            room.remove(h);
            for (PeerHandler p : room) p.sendText("PEER_DISCONNECTED:" + h.username);
            if (room.isEmpty()) rooms.remove(h.roomId);
        }
        h.roomId = null;
    }

    /** Relay a frame to the other peer in the room (runs on sender's thread). */
    void relayFrame(PeerHandler sender, byte[] frame) {
        if (sender.roomId == null) return;
        List<PeerHandler> room = rooms.get(sender.roomId);
        if (room == null) return;
        try {
            byte[] senderNameBytes = sender.username.getBytes("UTF-8");
            for (PeerHandler p : room) {
                if (p != sender) {
                    p.sendRelayedFrame(senderNameBytes, frame);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void registerPeer(String username, PeerHandler h) { peers.put(username, h); }
    void removePeer(PeerHandler h) {
        if (h.username != null) peers.remove(h.username);
        leaveRoom(h);
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        for (PeerHandler p : peers.values()) {
            p.close();
        }
    }

    // ── Inner: per-peer handler ─────────────────────────────────────────────
    static class PeerHandler implements Runnable {
        private final Socket            socket;
        private final VideoRelayServer  server;
        private DataOutputStream        out;
        String username;
        String roomId;

        private static final int TYPE_TEXT  = 1;
        private static final int TYPE_FRAME = 2;

        PeerHandler(Socket socket, VideoRelayServer server) {
            this.socket = socket;
            this.server = server;
        }

        synchronized void sendText(String text) {
            try {
                byte[] data = text.getBytes("UTF-8");
                out.writeInt(TYPE_TEXT);
                out.writeInt(data.length);
                out.write(data);
                out.flush();
            } catch (IOException ignored) {}
        }

        synchronized void sendFrame(byte[] frame) {
            try {
                out.writeInt(TYPE_FRAME);
                out.writeInt(frame.length);
                out.write(frame);
                out.flush();
            } catch (IOException ignored) {}
        }

        synchronized void sendRelayedFrame(byte[] senderNameBytes, byte[] frame) {
            try {
                out.writeInt(TYPE_FRAME);
                int length = 4 + senderNameBytes.length + frame.length;
                out.writeInt(length);
                out.writeInt(senderNameBytes.length);
                out.write(senderNameBytes);
                out.write(frame);
                out.flush();
            } catch (IOException ignored) {}
        }

        @Override
        public void run() {
            try {
                DataInputStream  in  = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                while (true) {
                    int type   = in.readInt();
                    int length = in.readInt();
                    byte[] payload = new byte[length];
                    in.readFully(payload);

                    if (type == TYPE_TEXT) {
                        String cmd = new String(payload, "UTF-8").trim();
                        handleCommand(cmd);
                    } else if (type == TYPE_FRAME) {
                        server.relayFrame(this, payload);
                    }
                }
            } catch (IOException e) {
                // peer disconnected
            } finally {
                server.removePeer(this);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void handleCommand(String cmd) {
            if (cmd.startsWith("REGISTER:")) {
                username = cmd.substring(9).trim();
                server.registerPeer(username, this);
            } else if (cmd.startsWith("CREATE_ROOM:")) {
                server.leaveRoom(this);
                String id = cmd.substring(12).trim();
                server.createRoom(id, this);
                sendText("ROOM_ID:" + id);
            } else if (cmd.startsWith("JOIN_ROOM:")) {
                server.leaveRoom(this);
                String id = cmd.substring(10).trim();
                boolean ok = server.joinRoom(id, this);
                if (!ok) sendText("ROOM_NOT_FOUND");
            } else if (cmd.equals("LEAVE_ROOM")) {
                server.leaveRoom(this);
                sendText("ROOM_LEFT");
            }
        }

        void close() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
