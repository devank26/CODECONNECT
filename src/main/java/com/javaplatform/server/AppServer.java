package com.javaplatform.server;

/**
 * Starts all embedded servers in background threads.
 */
public class AppServer {

    private static ChatServer       chatServer;
    private static VideoRelayServer videoServer;
    private static org.bitlet.weupnp.GatewayDevice activeDevice;

    public static VideoRelayServer getVideoServer() { return videoServer; }

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

        // Try UPnP port mapping in background
        new Thread(() -> {
            try {
                System.out.println("[AppServer] Attempting UPnP port mapping for ports 9001 and 9002...");
                org.bitlet.weupnp.GatewayDiscover discover = new org.bitlet.weupnp.GatewayDiscover();
                discover.discover();
                org.bitlet.weupnp.GatewayDevice device = discover.getValidGateway();
                if (device != null) {
                    System.out.println("[AppServer] Found UPnP Gateway: " + device.getModelName());
                    String localIp = device.getLocalAddress().getHostAddress();
                    activeDevice = device;
                    
                    // Port 9001
                    org.bitlet.weupnp.PortMappingEntry entry9001 = new org.bitlet.weupnp.PortMappingEntry();
                    if (device.getSpecificPortMappingEntry(9001, "TCP", entry9001)) {
                        System.out.println("[AppServer] UPnP Port 9001 mapping already exists.");
                    } else {
                        boolean ok = device.addPortMapping(9001, 9001, localIp, "TCP", "CodeConnect Chat");
                        System.out.println("[AppServer] UPnP Port 9001 mapping result: " + ok);
                    }
                    
                    // Port 9002
                    org.bitlet.weupnp.PortMappingEntry entry9002 = new org.bitlet.weupnp.PortMappingEntry();
                    if (device.getSpecificPortMappingEntry(9002, "TCP", entry9002)) {
                        System.out.println("[AppServer] UPnP Port 9002 mapping already exists.");
                    } else {
                        boolean ok = device.addPortMapping(9002, 9002, localIp, "TCP", "CodeConnect Video");
                        System.out.println("[AppServer] UPnP Port 9002 mapping result: " + ok);
                    }
                } else {
                    System.out.println("[AppServer] No valid UPnP Gateway found. Dynamic port forwarding skipped.");
                }
            } catch (Exception e) {
                System.out.println("[AppServer] UPnP port mapping failed: " + e.getMessage());
            }
        }, "upnp-mapping").start();

        // Open Windows Firewall inbound rules for LAN/hotspot connectivity
        new Thread(AppServer::ensureWindowsFirewallRules, "firewall-setup").start();
    }

    /**
     * Silently adds Windows Firewall inbound TCP rules for ports 9001 and 9002.
     * This allows peers on the same LAN / hotspot to connect without manual
     * firewall configuration.  The command is a no-op if the rule already exists.
     * Runs only on Windows; silently skipped on other platforms.
     */
    private static void ensureWindowsFirewallRules() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        String[][] rules = {
            {"CodeConnect-Chat-9001",  "9001"},
            {"CodeConnect-Video-9002", "9002"}
        };
        for (String[] rule : rules) {
            String name = rule[0];
            String port = rule[1];
            try {
                // Delete any stale rule first (ignore errors), then add fresh
                String deleteCmd = "netsh advfirewall firewall delete rule name=\"" + name + "\" protocol=TCP dir=in";
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", deleteCmd}).waitFor();

                String addCmd = "netsh advfirewall firewall add rule"
                        + " name=\"" + name + "\""
                        + " protocol=TCP dir=in action=allow"
                        + " localport=" + port
                        + " profile=any";
                Process p = Runtime.getRuntime().exec(new String[]{"cmd", "/c", addCmd});
                int exitCode = p.waitFor();
                System.out.println("[AppServer] Firewall rule '" + name + "' port " + port
                        + (exitCode == 0 ? " → opened ✓" : " → failed (exit " + exitCode + ", may need admin)"));
            } catch (Exception e) {
                System.out.println("[AppServer] Could not set firewall rule for port " + port + ": " + e.getMessage());
            }
        }
    }

    public static void stop() {
        if (chatServer  != null) chatServer.stop();
        if (videoServer != null) videoServer.stop();

        if (activeDevice != null) {
            new Thread(() -> {
                try {
                    System.out.println("[AppServer] Cleaning up UPnP port mappings...");
                    activeDevice.deletePortMapping(9001, "TCP");
                    activeDevice.deletePortMapping(9002, "TCP");
                    System.out.println("[AppServer] UPnP port mappings deleted.");
                } catch (Exception ignored) {}
            }, "upnp-cleanup").start();
        }
    }
}
