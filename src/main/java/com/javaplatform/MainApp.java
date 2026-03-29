package com.javaplatform;

import com.javaplatform.server.AppServer;
import com.javaplatform.view.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point for the Java Platform desktop application.
 *
 * Features:
 *  ☕ Java Compiler  — write and run Java code with error analysis
 *  💬 Real-time Chat — instant messaging over TCP sockets
 *  📹 Video Call     — peer-to-peer video via JPEG frame relay
 *  🤖 AI Assistant   — Gemini-powered Java coding help
 *  👤 User System    — username-based session management
 *
 * Run with:
 *   mvn clean javafx:run
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Start embedded chat and video relay servers
        AppServer.start();

        // Show login screen
        new LoginView(primaryStage).show();
    }

    @Override
    public void stop() {
        AppServer.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
