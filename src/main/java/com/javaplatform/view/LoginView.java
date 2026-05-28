package com.javaplatform.view;

import com.javaplatform.SessionState;
import com.javaplatform.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

/**
 * Login screen that prompts for a username before entering the platform.
 * Stores the name in SessionState and launches MainWindow.
 */
public class LoginView {

    private final Stage primaryStage;

    public LoginView(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void show() {
        primaryStage.setTitle("Java Platform — Welcome");

        ThemeManager tm = ThemeManager.getInstance();

        // ── Root ──────────────────────────────────────────────────────────
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: " + tm.bgApp() + ";");

        // ── Logo / Title ──────────────────────────────────────────────────
        Label logo = new Label("{ }");
        logo.setStyle("-fx-font-size: 52px; -fx-font-weight: bold; " +
                      "-fx-text-fill: " + tm.accent() + "; -fx-font-family: 'Consolas';");

        Label title = new Label("Java Platform");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; " +
                       "-fx-text-fill: " + tm.textPrimary() + "; -fx-font-family: 'Segoe UI';");

        Label subtitle = new Label("Compiler · Chat · Video · AI Assistant");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: " + tm.textMuted() + "; " +
                          "-fx-font-family: 'Segoe UI';");

        // ── Card ──────────────────────────────────────────────────────────
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 48, 36, 48));
        card.setMaxWidth(400);
        card.setStyle("-fx-background-color: " + tm.bgCard() + "; " +
                      "-fx-background-radius: 16; " +
                      "-fx-border-color: " + tm.border() + "; " +
                      "-fx-border-radius: 16; " +
                      "-fx-border-width: 1;");

        Label enterLabel = new Label("Enter your name to get started");
        enterLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: " + tm.textMuted() + "; " +
                            "-fx-font-family: 'Segoe UI';");

        TextField nameField = new TextField();
        nameField.setPromptText("Your name…");
        nameField.setStyle(tm.getTextFieldStyle(15));
        nameField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: " + tm.errorColor() + "; -fx-font-size: 12px;");

        Button enterBtn = new Button("Enter Platform →");
        enterBtn.setMaxWidth(Double.MAX_VALUE);
        enterBtn.setStyle(tm.getButtonStyle(tm.accent()) + " -fx-font-size: 15px; -fx-padding: 12 20;");
        enterBtn.setOnMouseEntered(e -> enterBtn.setStyle(tm.getButtonStyle(tm.accentHover()) + " -fx-font-size: 15px; -fx-padding: 12 20;"));
        enterBtn.setOnMouseExited( e -> enterBtn.setStyle(tm.getButtonStyle(tm.accent()) + " -fx-font-size: 15px; -fx-padding: 12 20;"));

        card.getChildren().addAll(enterLabel, nameField, errorLabel, enterBtn);

        // ── Layout ────────────────────────────────────────────────────────
        root.getChildren().addAll(logo, title, subtitle, card);

        // ── Actions ───────────────────────────────────────────────────────
        Runnable proceed = () -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    errorLabel.setText("Please enter a name.");
                    return;
                }
                SessionState.getInstance().setUsername(name);
                SessionState.getInstance().setServerHost("localhost");
                System.out.println("[LoginView] Entering platform as: " + name);
                new MainWindow(primaryStage).show();
            } catch (Exception ex) {
                System.err.println("[LoginView] CRITICAL ERROR during transition:");
                ex.printStackTrace();
                errorLabel.setText("Error: " + ex.getMessage());
            }
        };

        enterBtn.setOnAction(e -> proceed.run());
        nameField.setOnAction(e -> proceed.run());

        // ── Scene ─────────────────────────────────────────────────────────
        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        nameField.requestFocus();
    }
}
