package com.likanug.dual.server;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Entry point for the Dual relay server.
 *
 * <p>Usage:
 * <pre>
 *   java -jar dual-server-all.jar [port]
 *   # default port: 7777
 * </pre>
 *
 * <p>Build fat JAR:
 * <pre>
 *   ./gradlew :server:fatJar
 *   # output: server/build/libs/dual-server-all.jar
 * </pre>
 */
public class ServerApp {

    static final int DEFAULT_PORT = 7777;

    public static void main(String[] args) {
        setupLogging();

        int port = DEFAULT_PORT;
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1 || port > 65535) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.err.println("Invalid port: " + args[0] + ". Using default " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }

        RelayServer server = new RelayServer(port);

        // Graceful shutdown on Ctrl+C
        final int finalPort = port;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down relay server on port " + finalPort + "...");
            server.stop();
        }, "shutdown-hook"));

        try {
            server.start(); // blocks until stop() is called
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void setupLogging() {
        // Use a compact single-line log format
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tT] [%4$s] %5$s%6$s%n");

        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        for (var h : root.getHandlers()) root.removeHandler(h);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new SimpleFormatter());
        root.addHandler(handler);
    }
}
