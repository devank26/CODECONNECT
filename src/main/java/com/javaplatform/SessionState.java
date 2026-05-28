package com.javaplatform;

/**
 * Singleton that holds the current user's session data across all features.
 */
public class SessionState {

    private static SessionState instance;

    private String username = "User";
    private String serverHost = "localhost";

    // Server ports (all embedded in same JVM)
    public static final int    CHAT_PORT     = 9001;
    public static final int    VIDEO_PORT    = 9002;

    private SessionState() {}

    public static synchronized SessionState getInstance() {
        if (instance == null) {
            instance = new SessionState();
        }
        return instance;
    }

    public String getUsername() { return username; }
    public void   setUsername(String u) { this.username = (u == null || u.isBlank()) ? "User" : u.trim(); }

    public String getServerHost() { return serverHost; }
    public void   setServerHost(String host) { this.serverHost = (host == null || host.isBlank()) ? "localhost" : host.trim(); }
}
