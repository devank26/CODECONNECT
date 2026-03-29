package com.javaplatform.server;

/**
 * Starts all embedded servers in background threads.
 */
public class AppServer {

    private static ChatServer       chatServer;
    private static VideoRelayServer videoServer;

    public static void start() {
        chatServer  = new ChatServer(9001);
        videoServer = new VideoRelayServer(9002);

        Thread t1 = new Thread(chatServer,  "server-chat");
        Thread t2 = new Thread(videoServer, "server-video");
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        System.out.println("[AppServer] Chat server  → port 9001");
        System.out.println("[AppServer] Video server → port 9002");
    }

    public static void stop() {
        if (chatServer  != null) chatServer.stop();
        if (videoServer != null) videoServer.stop();
    }
}
