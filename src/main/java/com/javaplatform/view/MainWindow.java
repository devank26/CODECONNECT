package com.javaplatform.view;

import com.javaplatform.SessionState;
import com.javaplatform.ThemeManager;
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
    private final Button themeToggleBtn = new Button();

    private static CompilerTab compilerTabInstance;
    private static ChatTab chatTabInstance;
    private static VideoTab videoTabInstance;
    private static AIAssistantTab aiAssistantTabInstance;

    public static CompilerTab getCompilerTab() { return compilerTabInstance; }
    public static ChatTab getChatTab() { return chatTabInstance; }
    public static VideoTab getVideoTab() { return videoTabInstance; }
    public static AIAssistantTab getAIAssistantTab() { return aiAssistantTabInstance; }

    private HBox topBar;
    private TabPane tabs;
    private Label logo;
    private Label userLabel;
    private Label portInfo;
    private VBox root;
    private Scene scene;

    public MainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void show() {
        String username = SessionState.getInstance().getUsername();
        primaryStage.setTitle("Java Platform — " + username);

        // ── Top bar ───────────────────────────────────────────────────────
        logo = new Label("{ } Java Platform");
        userLabel = new Label("👤 " + username);
        portInfo = new Label("Chat:9001  Video:9002");

        themeToggleBtn.setOnAction(e -> ThemeManager.getInstance().toggleTheme());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar = new HBox(16, logo, spacer, portInfo, userLabel, themeToggleBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 20, 10, 20));

        // ── Tab pane ──────────────────────────────────────────────────────
        System.out.println("[MainWindow] Initializing tabs...");
        compilerTabInstance = new CompilerTab();
        chatTabInstance = new ChatTab();
        videoTabInstance = new VideoTab();
        aiAssistantTabInstance = new AIAssistantTab();

        tabs = new TabPane();
        tabs.getTabs().addAll(
                compilerTabInstance,
                chatTabInstance,
                videoTabInstance,
                aiAssistantTabInstance
        );
        System.out.println("[MainWindow] TabPane created with " + tabs.getTabs().size() + " tabs.");
        applyCSSWorkaround(tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        // ── Root ──────────────────────────────────────────────────────────
        root = new VBox(topBar, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        scene = new Scene(root, 1100, 780);

        ThemeManager.getInstance().addThemeListener(this::applyTheme);
        applyTheme();

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private void applyTheme() {
        ThemeManager tm = ThemeManager.getInstance();

        // Style the Toggle Button
        themeToggleBtn.setText(tm.isDarkMode() ? "☀  Light Mode" : "🌙  Dark Mode");
        themeToggleBtn.setStyle(
            "-fx-background-color: " + (tm.isDarkMode() ? "#252840;" : "#e2e8f0;") +
            "-fx-text-fill: " + (tm.isDarkMode() ? "#f1f3f9;" : "#0f172a;") +
            "-fx-font-size: 12px; -fx-background-radius: 8; -fx-padding: 6 12; -fx-cursor: hand;"
        );

        // Style Top Bar
        topBar.setStyle("-fx-background-color: " + tm.bgCard() + "; " +
                        "-fx-border-color: " + tm.border() + "; -fx-border-width: 0 0 1 0;");

        // Style Labels
        logo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                      "-fx-text-fill: " + tm.accent() + "; -fx-font-family: 'Consolas';");
        userLabel.setStyle("-fx-text-fill: " + tm.textPrimary() + "; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';");
        portInfo.setStyle("-fx-text-fill: " + tm.textMuted() + "; -fx-font-size: 11px; -fx-font-family: 'Consolas';");

        // Style TabPane and Root
        tabs.setStyle(
                "-fx-tab-min-width: 140; " +
                "-fx-tab-min-height: 38; " +
                "-fx-background-color: " + tm.bgApp() + ";");
        root.setStyle("-fx-background-color: " + tm.bgApp() + ";");

        // Tab styling workaround
        for (Tab tab : tabs.getTabs()) {
            boolean isSelected = tab.isSelected();
            tab.setStyle(
                "-fx-background-color: " + (isSelected ? tm.accent() : tm.bgCard()) + "; " +
                "-fx-text-fill: " + (isSelected ? "#ffffff" : tm.textMuted()) + "; " +
                "-fx-font-size: 13px; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; " +
                "-fx-border-color: " + tm.border() + ";"
            );
        }

        if (scene != null) {
            scene.setFill(javafx.scene.paint.Color.web(tm.bgApp()));
        }
    }

    private void applyCSSWorkaround(TabPane tabs) {
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            applyTheme();
        });
    }
}
