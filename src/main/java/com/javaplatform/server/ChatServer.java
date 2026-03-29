package com.javaplatform.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple TCP chat server.
 * Protocol (line-based):
 *   Client → Server:  USERNAME:<name>
 *                     MSG:<text>
 *   Server → Clients: <name>: <text>
 *                     SYSTEM: <event>
 */
public class ChatServer implements Runnable {

    private final int port;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public ChatServer(int port) { this.port = port; }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            System.out.println("[ChatServer] Listening on port " + port);
            while (running) {
                try {
                    Socket s = serverSocket.accept();
                    ClientHandler h = new ClientHandler(s, this);
                    clients.add(h);
                    Thread t = new Thread(h, "chat-client-" + s.getPort());
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

    void broadcastAll(String msg) {
        for (ClientHandler c : clients) c.send(msg);
    }

    void broadcast(String msg, ClientHandler exclude) {
        for (ClientHandler c : clients) {
            if (c != exclude) c.send(msg);
        }
    }

    void remove(ClientHandler h) { clients.remove(h); }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }

    // ── Inner: per-client handler ──────────────────────────────────────────
    private static class ClientHandler implements Runnable {
        private final Socket     socket;
        private final ChatServer server;
        private PrintWriter      out;
        private String           username;

        ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }

        void send(String msg) {
            if (out != null) {
                out.println(msg);
            }
        }

        @Override
        public void run() {
            try {
                BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("USERNAME:")) {
                        username = line.substring(9).trim();
                        server.broadcastAll("SYSTEM: ✔ " + username + " joined the chat");
                    } else if (line.startsWith("MSG:")) {
                        String msg = line.substring(4);
                        server.broadcastAll(username + ": " + msg);
                    }
                }
            } catch (IOException e) {
                // client disconnected
            } finally {
                if (username != null) {
                    server.remove(this);
                    server.broadcastAll("SYSTEM: " + username + " left the chat");
                }
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}
