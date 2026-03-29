package com.javaplatform.view;

import com.javaplatform.SessionState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Main window — contains the top navigation bar and a TabPane
 * with all four feature tabs.
 */
public class MainWindow {

    private final Stage primaryStage;

    public MainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void show() {
        String username = SessionState.getInstance().getUsername();
        primaryStage.setTitle("Java Platform — " + username);

        // ── Top bar ───────────────────────────────────────────────────────
        Label logo = new Label("{ } Java Platform");
        logo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                      "-fx-text-fill: #7c3aed; -fx-font-family: 'Consolas';");

        Label userLabel = new Label("👤 " + username);
        userLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';");

        Label portInfo = new Label("Chat:9001  Video:9002");
        portInfo.setStyle("-fx-text-fill: #444; -fx-font-size: 11px; -fx-font-family: 'Consolas';");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(16, logo, spacer, portInfo, userLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setStyle("-fx-background-color: #141428; " +
                        "-fx-border-color: #2d2d45; -fx-border-width: 0 0 1 0;");

        // ── Tab pane ──────────────────────────────────────────────────────
        System.out.println("[MainWindow] Initializing tabs...");
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                new CompilerTab(),
                new ChatTab(),
                new VideoTab(),
                new AIAssistantTab()
        );
        System.out.println("[MainWindow] TabPane created with " + tabs.getTabs().size() + " tabs.");
        tabs.setStyle(
                "-fx-tab-min-width: 140; " +
                "-fx-tab-min-height: 38; " +
                "-fx-background-color: #0f0f1a;");
        applyCSSWorkaround(tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        // ── Root ──────────────────────────────────────────────────────────
        VBox root = new VBox(topBar, tabs);
        root.setStyle("-fx-background-color: #0f0f1a;");
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Scene scene = new Scene(root, 1100, 780);
        styleScene(scene);

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    /**
     * Apply inline styles to the TabPane since we're not using external CSS files.
     */
    private void applyCSSWorkaround(TabPane tabs) {
        // JavaFX tab styling through setStyle on tabs themselves
        for (Tab tab : tabs.getTabs()) {
            tab.setStyle(
                    "-fx-background-color: #1e1e2e; " +
                    "-fx-text-fill: #c0c0d0;");
        }
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            if (old != null) old.setStyle("-fx-background-color: #1e1e2e; -fx-text-fill: #c0c0d0;");
            if (nw  != null) nw.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white;");
        });
        // Highlight first tab by default
        if (!tabs.getTabs().isEmpty()) {
            tabs.getTabs().get(0).setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white;");
        }
    }

    private void styleScene(Scene scene) {
        // Global scene background; JavaFX respects this for unoccupied areas
        scene.setFill(javafx.scene.paint.Color.web("#0f0f1a"));
    }
}
