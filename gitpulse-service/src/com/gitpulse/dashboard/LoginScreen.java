package com.gitpulse.dashboard;
import com.gitpulse.Home.*;
import com.gitpulse.Algorithm.RepositoryAnalyzer;
import com.gitpulse.Algorithm.RepositoryReport;
import com.gitpulse.model.Repository;
import com.gitpulse.service.DataService;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginScreen {

    private static final String NAVY    = "#0D1117";
    private static final String CYAN    = "#00D4FF";
    private static final String WHITE   = "#E6EDF3";
    private static final String SURFACE = "#161B22";
    private static final String BORDER  = "#21262D";

    public void show(Stage stage) {

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + NAVY + ";");

        VBox center = new VBox(32);
        center.setAlignment(Pos.CENTER);
        root.setPrefSize(1280,800);

        // Logo section
        Canvas logo = buildLogo(90, 90);

        Label gitLabel = new Label("Git");
        gitLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        gitLabel.setTextFill(Color.web(WHITE));

        Label pulseLabel = new Label("Pulse");
        pulseLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        pulseLabel.setTextFill(Color.web(CYAN));
        pulseLabel.setStyle("-fx-effect: dropshadow(gaussian, #00D4FF, 3, 0.15, 0, 0);");

        HBox brandBox = new HBox(0, gitLabel, pulseLabel);
        brandBox.setAlignment(Pos.CENTER);

        Label tagline = new Label("Connect your repository to get started");
        tagline.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        tagline.setTextFill(Color.web(WHITE + "77"));

        VBox logoSection = new VBox(10, logo, brandBox, tagline);
        logoSection.setAlignment(Pos.CENTER);

        // Login card
        VBox card = new VBox(18);
        card.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 14;" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 32;"
        );
        card.setMaxWidth(480);

        Label cardTitle = new Label("Repository Access");
        cardTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        cardTitle.setTextFill(Color.web(WHITE));

        Label cardSub = new Label("Enter the GitHub owner and repository name to analyse.");
        cardSub.setFont(Font.font("Segoe UI", 13));
        cardSub.setTextFill(Color.web(WHITE + "66"));
        cardSub.setWrapText(true);

        // Owner field
        Label ownerLabel = new Label("GitHub Owner / Organisation");
        ownerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        ownerLabel.setTextFill(Color.web(CYAN));

        TextField ownerField = new TextField();
        ownerField.setPromptText("e.g. Thomas-55");
        styleTextField(ownerField);
        ownerField.focusedProperty().addListener((obs, old, f) -> styleTextFieldFocus(ownerField, f));

        // Repo name field
        Label repoLabel = new Label("Repository Name");
        repoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        repoLabel.setTextFill(Color.web(CYAN));

        TextField repoField = new TextField();
        repoField.setPromptText("e.g. Fuel-Management-System");
        styleTextField(repoField);
        repoField.focusedProperty().addListener((obs, old, f) -> styleTextFieldFocus(repoField, f));

        // Error / status label
        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.web("#FF6B6B"));
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);

        // Progress indicator
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(28, 28);
        spinner.setStyle("-fx-accent: " + CYAN + ";");
        spinner.setVisible(false);

        // Analyse button
        Button analyzeBtn = new Button("⚡  Connect & Analyse");
        analyzeBtn.setMaxWidth(Double.MAX_VALUE);
        stylePrimaryButton(analyzeBtn, false);
        analyzeBtn.setOnMouseEntered(e -> stylePrimaryButton(analyzeBtn, true));
        analyzeBtn.setOnMouseExited(e -> stylePrimaryButton(analyzeBtn, false));

        analyzeBtn.setOnAction(e -> {
            String owner = ownerField.getText().trim();
            String repo  = repoField.getText().trim();

            if (owner.isEmpty() || repo.isEmpty()) {
                statusLabel.setText("⚠  Please fill in both fields.");
                statusLabel.setTextFill(Color.web("#FF6B6B"));
                statusLabel.setVisible(true);
                return;
            }

            // Disable UI while loading
            analyzeBtn.setDisable(true);
            spinner.setVisible(true);
            statusLabel.setText("Loading repository data…");
            statusLabel.setTextFill(Color.web(CYAN));
            statusLabel.setVisible(true);

            // Load on background thread so UI doesn't freeze
            Task<RepositoryReport> task = new Task<>() {
                Repository repository;
                @Override
                protected RepositoryReport call() {
                    DataService service = new DataService();
                    repository = service.loadRepository(owner, repo);
                    return new RepositoryAnalyzer().analyze(repository);
                }

                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        spinner.setVisible(false);
                        DashboardScreen dashboard = new DashboardScreen(repository, getValue());
                        dashboard.show(stage);
                    });
                }

                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        spinner.setVisible(false);
                        analyzeBtn.setDisable(false);
                        statusLabel.setText("⚠  Could not load repository. Check the name and try again.");
                        statusLabel.setTextFill(Color.web("#FF6B6B"));
                        statusLabel.setVisible(true);
                    });
                }
            };

            new Thread(task).start();
        });

        card.getChildren().addAll(
                cardTitle, cardSub,
                ownerLabel, ownerField,
                repoLabel, repoField,
                statusLabel, spinner, analyzeBtn
        );

        center.getChildren().addAll(logoSection, card);

        Label powered = new Label("Powered by GitPulse Analytics");
        powered.setFont(Font.font("Segoe UI", 13));
        powered.setTextFill(Color.web(CYAN + "55"));
        powered.setStyle("-fx-padding: 0 0 18 0;");
        StackPane.setAlignment(powered, Pos.BOTTOM_CENTER);

        root.getChildren().addAll(center, powered);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setTitle("GitPulse — Connect Repository");
        stage.show();

        // Entrance animation
        center.setOpacity(0);
        center.setTranslateY(20);
        FadeTransition fade = new FadeTransition(Duration.millis(500), center);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(500), center);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    // Helpers

    private Canvas buildLogo(double w, double h) {
        Canvas c = new Canvas(w, h);
        GraphicsContext gc = c.getGraphicsContext2D();
        double cx = w / 2, cy = h / 2, r = w * 0.42;
        gc.setStroke(Color.web(CYAN + "33")); gc.setLineWidth(8);
        gc.strokeOval(cx-r-6, cy-r-6, (r+6)*2, (r+6)*2);
        gc.setStroke(Color.web(CYAN)); gc.setLineWidth(3);
        gc.strokeOval(cx-r, cy-r, r*2, r*2);
        gc.setStroke(Color.web(CYAN)); gc.setLineWidth(2.8);
        gc.setLineCap(StrokeLineCap.ROUND);
        double[] xs = {cx-28,cx-16,cx-6,cx+4,cx+14,cx+22,cx+28};
        double[] ys = {cy,cy,cy-24,cy+18,cy-14,cy,cy};
        gc.beginPath(); gc.moveTo(xs[0],ys[0]);
        for (int i=1;i<xs.length;i++) gc.lineTo(xs[i],ys[i]);
        gc.stroke();
        gc.setFill(Color.web(CYAN));
        gc.fillOval(cx-28-4,cy-4,8,8); gc.fillOval(cx+28-4,cy-4,8,8);
        gc.fillOval(cx-4,cy-24-4,8,8);
        gc.setFill(Color.web(CYAN+"55"));
        gc.fillOval(cx-28-7,cy-7,14,14); gc.fillOval(cx+28-7,cy-7,14,14);
        return c;
    }

    private void styleTextField(TextField f) {
        f.setStyle("-fx-background-color:"+NAVY+";-fx-border-color:"+BORDER+
                ";-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;" +
                "-fx-text-fill:"+WHITE+";-fx-prompt-text-fill:#8B949E;" +
                "-fx-font-size:13px;-fx-padding:10 14 10 14;");
    }

    private void styleTextFieldFocus(TextField f, boolean focused) {
        f.setStyle("-fx-background-color:"+NAVY+";-fx-border-color:"+(focused?CYAN:BORDER)+
                ";-fx-border-width:"+(focused?"1.5":"1")+";-fx-border-radius:8;" +
                "-fx-background-radius:8;-fx-text-fill:"+WHITE+
                ";-fx-prompt-text-fill:#8B949E;-fx-font-size:13px;-fx-padding:10 14 10 14;");
    }

    private void stylePrimaryButton(Button btn, boolean hovered) {
        btn.setStyle("-fx-background-color:"+(hovered?"#33DDFF":CYAN)+
                ";-fx-text-fill:"+NAVY+";-fx-font-size:14px;-fx-font-weight:bold;" +
                "-fx-padding:12 0 12 0;-fx-border-radius:8;-fx-background-radius:8;" +
                "-fx-cursor:hand;"+(hovered?"-fx-effect:dropshadow(gaussian,#00D4FF,14,0.4,0,0);":""));
    }
}
