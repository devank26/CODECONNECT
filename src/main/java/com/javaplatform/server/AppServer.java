package com.javaplatform.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Starts all embedded servers in background threads.
 * Also manages an optional SSH reverse tunnel to serveo.net for WAN connectivity.
 */
public class AppServer {

    private static ChatServer       chatServer;
    private static VideoRelayServer videoServer;
    private static org.bitlet.weupnp.GatewayDevice activeDevice;

    // ── Relay tunnel state ────────────────────────────────────────────────────
    /** The relay address that remote WAN peers can connect to, e.g. "serveo.net:12345". Null if no tunnel. */
    private static volatile String relayAddress = null;
    private static volatile Process sshTunnelProcess = null;

    public static VideoRelayServer getVideoServer() { return videoServer; }

    /** Returns the active WAN relay address (host:port) or null if none established. */
    public static String getRelayAddress() { return relayAddress; }

    public static void start() {
        chatServer  = new ChatServer(9001);
        videoServer = new VideoRelayServer(9002);

        Thread t1 = new Thread(chatServer,  "server-chat");
        Thread t2 = new Thread(videoServer, "server-video");
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        System.out.println("[AppServer] Chat server  -> port 9001");
        System.out.println("[AppServer] Video server -> port 9002");

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

                    org.bitlet.weupnp.PortMappingEntry entry9001 = new org.bitlet.weupnp.PortMappingEntry();
                    if (device.getSpecificPortMappingEntry(9001, "TCP", entry9001)) {
                        System.out.println("[AppServer] UPnP Port 9001 mapping already exists.");
                    } else {
                        boolean ok = device.addPortMapping(9001, 9001, localIp, "TCP", "CodeConnect Chat");
                        System.out.println("[AppServer] UPnP Port 9001 mapping result: " + ok);
                    }

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

    // -------------------------------------------------------------------------
    // SSH Relay Tunnel -- WAN Connectivity
    // -------------------------------------------------------------------------

    /**
     * Establishes an SSH reverse tunnel to serveo.net so remote WAN peers can
     * reach this machine's VideoRelayServer on port 9002.
     *
     * The tunnel is created on-demand when the host creates a room.
     * It is torn down automatically when stopRelayTunnel() is called (Leave Room).
     *
     * @param callback  Called with the relay address "host:port" on success, or null on failure.
     */
    public static void startRelayTunnel(java.util.function.Consumer<String> callback) {
        // Reuse existing tunnel if still alive
        if (relayAddress != null && sshTunnelProcess != null && sshTunnelProcess.isAlive()) {
            callback.accept(relayAddress);
            return;
        }

        new Thread(() -> {
            try {
                System.out.println("[AppServer] Starting SSH relay tunnel via serveo.net...");
                // serveo.net assigns a random public TCP port and echoes:
                // "Forwarding TCP connections from tcp://serveo.net:PORT"
                ProcessBuilder pb = new ProcessBuilder(
                    "ssh",
                    "-o", "StrictHostKeyChecking=no",
                    "-o", "ServerAliveInterval=30",
                    "-o", "ExitOnForwardFailure=yes",
                    "-R", "0:localhost:9002",
                    "serveo.net"
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                sshTunnelProcess = proc;

                AtomicReference<String> found = new AtomicReference<>(null);
                CountDownLatch latch = new CountDownLatch(1);

                Thread reader = new Thread(() -> {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.out.println("[SSH] " + line);
                            // Parse: "Forwarding TCP connections from tcp://serveo.net:PORT"
                            if (line.contains("Forwarding TCP") && line.contains("tcp://")) {
                                String url = line.replaceAll(".*tcp://", "").trim();
                                found.set(url); // "serveo.net:PORT"
                                latch.countDown();
                            }
                        }
                    } catch (Exception ignored) {}
                    latch.countDown(); // unblock if SSH exits without printing
                }, "ssh-reader");
                reader.setDaemon(true);
                reader.start();

                // Wait up to 15 seconds for serveo.net to confirm the tunnel
                boolean gotAddress = latch.await(15, TimeUnit.SECONDS);
                if (gotAddress && found.get() != null) {
                    relayAddress = found.get();
                    System.out.println("[AppServer] WAN relay tunnel established: tcp://" + relayAddress);
                    callback.accept(relayAddress);
                } else {
                    System.out.println("[AppServer] Relay tunnel failed or timed out.");
                    proc.destroy();
                    sshTunnelProcess = null;
                    callback.accept(null);
                }
            } catch (Exception e) {
                System.out.println("[AppServer] Could not start relay tunnel: " + e.getMessage());
                callback.accept(null);
            }
        }, "relay-tunnel-setup").start();
    }

    /** Tears down the SSH relay tunnel. Called automatically when the host leaves the room. */
    public static void stopRelayTunnel() {
        relayAddress = null;
        if (sshTunnelProcess != null && sshTunnelProcess.isAlive()) {
            sshTunnelProcess.destroy();
            System.out.println("[AppServer] WAN relay tunnel closed.");
        }
        sshTunnelProcess = null;
    }

    // -------------------------------------------------------------------------

    /**
     * Silently adds Windows Firewall inbound TCP rules for ports 9001 and 9002.
     * Allows peers on the same LAN / hotspot to connect without manual configuration.
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
                        + (exitCode == 0 ? " -> opened" : " -> failed (exit " + exitCode + ", may need admin)"));
            } catch (Exception e) {
                System.out.println("[AppServer] Could not set firewall rule for port " + port + ": " + e.getMessage());
            }
        }
    }

    public static void stop() {
        if (chatServer  != null) chatServer.stop();
        if (videoServer != null) videoServer.stop();
        stopRelayTunnel();

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
