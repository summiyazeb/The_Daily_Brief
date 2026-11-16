package com.gitpulse.dashboard;

import com.gitpulse.Algorithm.*;
import com.gitpulse.model.Repository;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

/**
  SUMMARY PANEL

  Shows: plain-English paragraph, health badge, trend badge,
  collaboration index bar, and a top-level stats row.
 */
public class SummaryPanel {

    private static final String NAVY    = "#0D1117";
    private static final String CYAN    = "#00D4FF";
    private static final String WHITE   = "#E6EDF3";
    private static final String SURFACE = "#161B22";
    private static final String BORDER  = "#21262D";

    private final Repository       repository;
    private final RepositoryReport report;

    public SummaryPanel(Repository repository, RepositoryReport report) {
        this.repository = repository;
        this.report     = report;
    }

    public ScrollPane build() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(36, 36, 36, 36));

        content.getChildren().addAll(
                pageHeader(),
                statsRow(),
                summaryCard(),
                badgeRow(),
                collaborationBar()
        );

        return styledScroll(content);
    }

    // Page header
    private VBox pageHeader() {
        Label title = new Label("Repository Summary");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web(WHITE));

        String desc = repository.getDescription() != null && !repository.getDescription().isBlank()
                ? repository.getDescription()
                : "No description available";
        Label sub = new Label(repository.getOwner() + "/" + repository.getName() + "  ·  " + desc);
        sub.setFont(Font.font("Segoe UI", 13));
        sub.setTextFill(Color.web(WHITE + "55"));
        sub.setWrapText(true);

        VBox box = new VBox(6, title, sub);
        return box;
    }

    // Four quick-stat cards
    private HBox statsRow() {
        HBox row = new HBox(16);
        row.getChildren().addAll(
                statCard("📝  Total Commits",
                        String.valueOf(repository.getCommits().size()),
                        CYAN),
                statCard("👥  Contributors",
                        String.valueOf(repository.getContributors().size()),
                        "#3FB950"),
                statCard("🔥  Busiest Day",
                        nvl(report.getBusyDayOfWeek()),
                        "#F0883E"),
                statCard("⏱  Avg Gap",
                        String.format("%.1fh", report.getAvgTimeBetweenCommits()),
                        "#BC8CFF")
        );
        return row;
    }

    // Plain-English summary card
    private VBox summaryCard() {
        VBox card = card();

        Label heading = new Label("AI-Generated Summary");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        heading.setTextFill(Color.web(CYAN));

        Label body = new Label(nvl(report.getPlainEnglishSummary()));
        body.setFont(Font.font("Segoe UI", 13));
        body.setTextFill(Color.web(WHITE + "CC"));
        body.setWrapText(true);
        body.setLineSpacing(4);

        card.getChildren().addAll(heading, body);
        return card;
    }

    // Health + trend badges side-by-side
    private HBox badgeRow() {
        HBox row = new HBox(16);

        // Health card
        String health = nvl(report.getProjectHealthLabel());
        String hc     = healthColor(health);
        VBox healthCard = badgeCard("Project Health", health, hc, healthIcon(health));

        // Trend card
        String trend = report.getActivityTrend() != null
                ? report.getActivityTrend().getTrendType().name() : "Unknown";
        String tc    = trendColor(trend);
        VBox trendCard = badgeCard("Activity Trend", trend, tc, trendIcon(trend));

        // Bus driver card
        String busDriver = report.isBusDriver()
                ? "⚠  Risk Detected" : "✔  Distributed";
        String bc = report.isBusDriver() ? "#FF6B6B" : "#3FB950";
        VBox busCard = badgeCard("Bus-Driver Risk", busDriver, bc, report.isBusDriver() ? "⚠" : "✔");

        HBox.setHgrow(healthCard, Priority.ALWAYS);
        HBox.setHgrow(trendCard,  Priority.ALWAYS);
        HBox.setHgrow(busCard,    Priority.ALWAYS);
        row.getChildren().addAll(healthCard, trendCard, busCard);
        return row;
    }

    // Collaboration index bar
    private VBox collaborationBar() {
        VBox card = card();

        double index = report.getCollaborationIndex();
        String label = nvl(report.getCollaborationLabel());

        Label heading = new Label("Collaboration Index  ·  " + label);
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        heading.setTextFill(Color.web(WHITE));

        Label pct = new Label(String.format("%.1f / 100", index));
        pct.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        pct.setTextFill(Color.web(CYAN));

        // Track
        StackPane track = new StackPane();
        track.setMaxWidth(Double.MAX_VALUE);
        track.setPrefHeight(10);
        track.setStyle("-fx-background-color:" + BORDER + ";-fx-background-radius:5;");

        // Fill
        double fillPct = Math.min(index / 100.0, 1.0);
        Region fill = new Region();
        fill.setPrefHeight(10);
        fill.setMaxWidth(Double.MAX_VALUE);
        fill.setStyle("-fx-background-color:" + CYAN + ";-fx-background-radius:5;");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        // Animate fill after layout
        fill.setScaleX(0);
        fill.setTranslateX(-fill.getWidth() / 2);
        track.getChildren().add(fill);

        track.widthProperty().addListener((obs, old, w) -> {
            fill.setPrefWidth(w.doubleValue() * fillPct);
            fill.setScaleX(1);
            fill.setTranslateX(0);
        });

        Label desc = new Label("Measures how evenly commit work is shared (0 = one person, 100 = perfectly equal).");
        desc.setFont(Font.font("Segoe UI", 12));
        desc.setTextFill(Color.web(WHITE + "55"));
        desc.setWrapText(true);

        card.getChildren().addAll(heading, pct, track, desc);
        return card;
    }

    // Component helpers

    private VBox statCard(String label, String value, String color) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18, 22, 18, 22));
        card.setStyle(
                "-fx-background-color:" + SURFACE + ";-fx-border-color:" + BORDER + ";" +
                        "-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;"
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 12));
        lbl.setTextFill(Color.web(WHITE + "66"));

        Label val = new Label(value);
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        val.setTextFill(Color.web(color));

        card.getChildren().addAll(lbl, val);
        return card;
    }

    private VBox badgeCard(String title, String value, String color, String icon) {
        VBox card = card();
        card.setAlignment(Pos.CENTER_LEFT);

        Label t = new Label(title);
        t.setFont(Font.font("Segoe UI", 12));
        t.setTextFill(Color.web(WHITE + "66"));

        Label ic = new Label(icon);
        ic.setFont(Font.font("Segoe UI", 28));
        ic.setTextFill(Color.web(color));

        Label v = new Label(value);
        v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        v.setTextFill(Color.web(color));

        card.getChildren().addAll(t, ic, v);
        return card;
    }

    private VBox card() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setStyle(
                "-fx-background-color:" + SURFACE + ";-fx-border-color:" + BORDER + ";" +
                        "-fx-border-width:1;-fx-border-radius:14;-fx-background-radius:14;"
        );
        return card;
    }

    private ScrollPane styledScroll(VBox content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + NAVY + ";-fx-background-color:" + NAVY + ";");
        return sp;
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    private String healthColor(String h) {
        return switch (h) {
            case "Healthy" -> "#3FB950"; case "Active" -> "#00D4FF";
            case "At Risk" -> "#F0883E"; case "Stale"  -> "#FF6B6B";
            default -> "#8B949E";
        };
    }
    private String healthIcon(String h) {
        return switch (h) {
            case "Healthy" -> "💚"; case "Active" -> "🔵";
            case "At Risk" -> "🟠"; case "Stale"  -> "🔴";
            default -> "⚫";
        };
    }
    private String trendColor(String t) {
        return switch (t) {
            case "GROWING"  -> "#3FB950"; case "DECLINING" -> "#FF6B6B";
            case "STABLE"   -> "#00D4FF"; case "SPORADIC"  -> "#F0883E";
            default -> "#8B949E";
        };
    }
    private String trendIcon(String t) {
        return switch (t) {
            case "GROWING"  -> "📈"; case "DECLINING" -> "📉";
            case "STABLE"   -> "➡"; case "SPORADIC"  -> "〰";
            default -> "❓";
        };
    }
}
