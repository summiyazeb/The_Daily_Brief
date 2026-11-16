package com.gitpulse.dashboard;

import com.gitpulse.Algorithm.RepositoryReport;
import javafx.geometry.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.*;

public class InsightsPanel {

    private static final String NAVY    = "#0D1117";
    private static final String CYAN    = "#00D4FF";
    private static final String WHITE   = "#E6EDF3";
    private static final String SURFACE = "#161B22";
    private static final String BORDER  = "#21262D";

    private final RepositoryReport report;

    public InsightsPanel(RepositoryReport report) { this.report = report; }

    public ScrollPane build() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(36, 36, 36, 36));
        content.getChildren().addAll(
                pageHeader(),
                timingRow(),
                gapAndStreakRow(),
                collaborationGauge(),
                busDriverCard()
        );
        return styledScroll(content);
    }

    private VBox pageHeader() {
        Label title = new Label("Unique Insights");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web(WHITE));
        Label sub = new Label("Metrics you cannot see anywhere on GitHub");
        sub.setFont(Font.font("Segoe UI", 13));
        sub.setTextFill(Color.web(WHITE + "55"));
        return new VBox(6, title, sub);
    }

    // Busiest day card + clock + avg time
    private HBox timingRow() {
        HBox row = new HBox(16);

        // Busiest day
        VBox dayCard = card();
        dayCard.getChildren().addAll(
                sectionLabel("📅  Busiest Day of Week"),
                bigLabel(nvl(report.getBusyDayOfWeek()), CYAN),
                descLabel("Day with the highest commit frequency across the project's history.")
        );

        // Busiest hour — clock canvas
        VBox hourCard = card();
        int hour = (int) report.getBusyHourOfDay();
        hourCard.getChildren().addAll(
                sectionLabel("🕐  Busiest Hour (UTC)"),
                bigLabel(hour >= 0 ? formatHour(hour) : "—", "#F0883E"),
                buildClockCanvas(110, 110, hour),
                descLabel("Hour of day (UTC) when most commits are pushed.")
        );

        // Avg time
        VBox avgCard = card();
        avgCard.getChildren().addAll(
                sectionLabel("⏱  Avg Time Between Commits"),
                bigLabel(String.format("%.1fh", report.getAvgTimeBetweenCommits()), "#3FB950"),
                buildAvgTimeBar(report.getAvgTimeBetweenCommits()),
                descLabel("Average hours between consecutive commits — lower = more active.")
        );

        HBox.setHgrow(dayCard, Priority.ALWAYS);
        HBox.setHgrow(hourCard, Priority.ALWAYS);
        HBox.setHgrow(avgCard, Priority.ALWAYS);
        row.getChildren().addAll(dayCard, hourCard, avgCard);
        return row;
    }

    // Gap + streak
    private HBox gapAndStreakRow() {
        HBox row = new HBox(16);

        // Gap card — scaled bar chart (0–30+ days scale)
        VBox gapCard = card();
        int gap = report.getLongestGapDays();
        gapCard.getChildren().addAll(
                sectionLabel("🕳  Longest Commit Gap"),
                bigLabel(plural(gap, "day"), "#FF6B6B"),
                buildScaleBar(Math.min(gap / 30.0, 1.0), "#FF6B6B",
                        "0 days", "30+ days"),
                descLabel("The longest stretch with no commits — invisible on GitHub.")
        );

        // Streak card — dot heatmap
        VBox streakCard = card();
        int streak = report.getPeakStreakDays();
        streakCard.getChildren().addAll(
                sectionLabel("🔥  Peak Daily Commit Streak"),
                bigLabel(plural(streak, "consecutive day"), "#FFD700"),
                buildStreakDots(streak),
                descLabel("Longest run of days with at least one commit every day.")
        );

        HBox.setHgrow(gapCard, Priority.ALWAYS);
        HBox.setHgrow(streakCard, Priority.ALWAYS);
        row.getChildren().addAll(gapCard, streakCard);
        return row;
    }

    // Collaboration gauge — full radial dial
    private VBox collaborationGauge() {
        VBox card = card();
        card.getChildren().add(sectionLabel("🤝  Collaboration Index"));

        double index = report.getCollaborationIndex();
        String label = nvl(report.getCollaborationLabel());
        String color = collaborationColor(label);

        HBox content = new HBox(32);
        content.setAlignment(Pos.CENTER_LEFT);

        // Radial dial canvas
        Canvas dial = buildRadialDial(160, 160, index, color);

        // Text alongside
        VBox text = new VBox(10);
        Label val = new Label(String.format("%.1f / 100", index));
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        val.setTextFill(Color.web(CYAN));

        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        lbl.setTextFill(Color.web(color));
        lbl.setStyle("-fx-background-color:" + color + "22;-fx-border-color:" + color + "55;" +
                "-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:4 14 4 14;");

        // Zone legend
        VBox zones = new VBox(5);
        zones.getChildren().addAll(
                zoneLegend("80–100", "Well Distributed", "#3FB950"),
                zoneLegend("50–79",  "Small Team",       "#F0883E"),
                zoneLegend("0–49",   "Solo Project",     "#FF6B6B")
        );

        text.getChildren().addAll(val, lbl, zones);
        content.getChildren().addAll(dial, text);
        card.getChildren().add(content);
        card.getChildren().add(descLabel(
                "Based on the Gini coefficient — 100 = perfectly equal distribution, 0 = one person does everything."
        ));
        return card;
    }

    // Bus driver
    private VBox busDriverCard() {
        VBox card = card();
        boolean risk  = report.isBusDriver();
        String  color = risk ? "#FF6B6B" : "#3FB950";

        card.getChildren().addAll(
                sectionLabel("🚌  Bus-Driver Risk"),
                bigLabel(risk ? "⚠  Risk Detected" : "✔  No Single Point of Failure", color),
                new Label("Dominant contributor: " + nvl(report.getDominantContributor())) {{
                    setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                    setTextFill(Color.web(WHITE));
                }},
                descLabel(risk
                        ? "One person has made over 50% of all commits. If they leave, the project could stall significantly."
                        : "Commit work is distributed. The project is resilient to individual departures.")
        );
        return card;
    }

    // Clock face canvas
    private Canvas buildClockCanvas(double w, double h, int hour) {
        Canvas c = new Canvas(w, h);
        GraphicsContext gc = c.getGraphicsContext2D();
        double cx = w / 2, cy = h / 2, r = w * 0.42;

        // Outer glow ring
        gc.setStroke(Color.web(CYAN + "22")); gc.setLineWidth(6);
        gc.strokeOval(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2);

        // Face fill
        gc.setFill(Color.web(SURFACE));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Face border
        gc.setStroke(Color.web(CYAN + "88")); gc.setLineWidth(2);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

        // Hour ticks + numbers
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30 - 90);
            double tickOuter = r - 4, tickInner = r - 12;
            double tx1 = cx + Math.cos(angle) * tickOuter;
            double ty1 = cy + Math.sin(angle) * tickOuter;
            double tx2 = cx + Math.cos(angle) * tickInner;
            double ty2 = cy + Math.sin(angle) * tickInner;
            gc.setStroke(Color.web(WHITE + "44")); gc.setLineWidth(1.5);
            gc.strokeLine(tx1, ty1, tx2, ty2);

            // Hour number
            int num = i == 0 ? 12 : i;
            double numR = r - 22;
            gc.setFill(Color.web(WHITE + "66"));
            gc.setFont(Font.font("Segoe UI", 9));
            gc.fillText(String.valueOf(num),
                    cx + Math.cos(angle) * numR - 4,
                    cy + Math.sin(angle) * numR + 4);
        }

        // Hour hand
        if (hour >= 0) {
            double hAngle = Math.toRadians((hour % 12) * 30 + ((hour % 12) * 0.5) - 90);
            gc.setStroke(Color.web(CYAN)); gc.setLineWidth(3);
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            gc.strokeLine(cx, cy,
                    cx + Math.cos(hAngle) * r * 0.55,
                    cy + Math.sin(hAngle) * r * 0.55);

            // Minute hand (show :00)
            gc.setStroke(Color.web(WHITE + "66")); gc.setLineWidth(1.5);
            double mAngle = Math.toRadians(-90); // 0 minutes
            gc.strokeLine(cx, cy,
                    cx + Math.cos(mAngle) * r * 0.75,
                    cy + Math.sin(mAngle) * r * 0.75);
        }

        // Centre dot
        gc.setFill(Color.web(CYAN));
        gc.fillOval(cx - 4, cy - 4, 8, 8);

        return c;
    }

    // Avg time horizontal bar (0–24h scale)
    private VBox buildAvgTimeBar(double avgHours) {
        VBox box = new VBox(4);
        double fraction = Math.min(avgHours / 24.0, 1.0);

        StackPane track = new StackPane();
        track.setPrefHeight(10); track.setMaxWidth(Double.MAX_VALUE);
        track.setStyle("-fx-background-color:" + BORDER + ";-fx-background-radius:5;");

        Region fill = new Region();
        fill.setPrefHeight(10);
        String fillColor = avgHours < 4 ? "#3FB950" : (avgHours < 12 ? "#F0883E" : "#FF6B6B");
        fill.setStyle("-fx-background-color:" + fillColor + ";-fx-background-radius:5;");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);
        track.widthProperty().addListener((obs, old, w) -> fill.setPrefWidth(w.doubleValue() * fraction));

        HBox scale = new HBox();
        Label l = new Label("0h");
        l.setFont(Font.font("Segoe UI", 10)); l.setTextFill(Color.web(WHITE + "33"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label r = new Label("24h+");
        r.setFont(Font.font("Segoe UI", 10)); r.setTextFill(Color.web(WHITE + "33"));
        scale.getChildren().addAll(l, sp, r);

        box.getChildren().addAll(track, scale);
        return box;
    }

    // Scale bar with labels
    private VBox buildScaleBar(double fraction, String color, String minLabel, String maxLabel) {
        VBox box = new VBox(4);

        StackPane track = new StackPane();
        track.setPrefHeight(10); track.setMaxWidth(Double.MAX_VALUE);
        track.setStyle("-fx-background-color:" + BORDER + ";-fx-background-radius:5;");

        Region fill = new Region();
        fill.setPrefHeight(10);

        LinearGradient grad = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(color + "88")),
                new Stop(1, Color.web(color))
        );
        fill.setStyle("-fx-background-radius:5;");
        fill.setBackground(new Background(new BackgroundFill(grad,
                new CornerRadii(5), Insets.EMPTY)));

        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);
        track.widthProperty().addListener((obs, old, w) -> fill.setPrefWidth(w.doubleValue() * fraction));

        HBox scale = new HBox();
        Label l = new Label(minLabel); l.setFont(Font.font("Segoe UI", 10)); l.setTextFill(Color.web(WHITE + "33"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label r = new Label(maxLabel); r.setFont(Font.font("Segoe UI", 10)); r.setTextFill(Color.web(WHITE + "33"));
        scale.getChildren().addAll(l, sp, r);

        box.getChildren().addAll(track, scale);
        return box;
    }

    // Streak dot heatmap
    private VBox buildStreakDots(int streak) {
        VBox box = new VBox(6);
        int dotCount = Math.min(streak, 35);
        int cols = 7;

        for (int row = 0; row < Math.ceil(dotCount / (double) cols); row++) {
            HBox hrow = new HBox(5);
            for (int col = 0; col < cols; col++) {
                int idx = row * cols + col;
                if (idx >= dotCount) break;
                Region dot = new Region();
                dot.setPrefSize(12, 12);
                // Heat colour: early days yellow, mid orange, long red
                double heat = idx / (double) Math.max(dotCount - 1, 1);
                String dc = heat < 0.33 ? "#FFD700" : heat < 0.66 ? "#F0883E" : "#FF6B6B";
                dot.setStyle("-fx-background-color:" + dc + ";" +
                        "-fx-background-radius:3;" +
                        "-fx-effect:dropshadow(gaussian," + dc + ",3,0.4,0,0);");
                hrow.getChildren().add(dot);
            }
            box.getChildren().add(hrow);
        }

        if (streak > 35) {
            Label more = new Label("+ " + (streak - 35) + " more days");
            more.setFont(Font.font("Segoe UI", 11));
            more.setTextFill(Color.web(WHITE + "55"));
            box.getChildren().add(more);
        }
        return box;
    }

    // Radial dial canvas
    private Canvas buildRadialDial(double w, double h, double value, String color) {
        Canvas c = new Canvas(w, h);
        GraphicsContext gc = c.getGraphicsContext2D();
        double cx = w / 2, cy = h / 2 + 8, r = w * 0.38;

        // Zone arcs (background: red → orange → green, 180° sweep)
        String[] zoneColors = {"#FF6B6B", "#F0883E", "#3FB950"};
        double   zoneSpan   = 60; // each zone = 60°
        for (int z = 0; z < 3; z++) {
            gc.setStroke(Color.web(zoneColors[z] + "44"));
            gc.setLineWidth(14);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2,
                    180 + z * zoneSpan, zoneSpan, ArcType.OPEN);
        }

        // Tick marks
        for (int t = 0; t <= 10; t++) {
            double angle = Math.toRadians(180 + t * 18);
            double r1 = r + 10, r2 = r + 18;
            gc.setStroke(Color.web(WHITE + "44")); gc.setLineWidth(1.5);
            gc.strokeLine(cx + Math.cos(angle) * r1, cy + Math.sin(angle) * r1,
                    cx + Math.cos(angle) * r2, cy + Math.sin(angle) * r2);
        }

        // Scale labels
        int[] scaleVals = {0, 25, 50, 75, 100};
        for (int sv : scaleVals) {
            double angle = Math.toRadians(180 + sv * 1.8);
            double lr    = r + 24;
            gc.setFill(Color.web(WHITE + "55"));
            gc.setFont(Font.font("Segoe UI", 9));
            gc.fillText(String.valueOf(sv),
                    cx + Math.cos(angle) * lr - 7,
                    cy + Math.sin(angle) * lr + 4);
        }

        // Value arc (filled portion)
        double sweep = value / 100.0 * 180;
        gc.setStroke(Color.web(color));
        gc.setLineWidth(14);
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 180, sweep, ArcType.OPEN);

        // Needle
        double needleAngle = Math.toRadians(180 + value / 100.0 * 180);
        gc.setStroke(Color.web(WHITE)); gc.setLineWidth(2.5);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeLine(cx, cy,
                cx + Math.cos(needleAngle) * (r - 8),
                cy + Math.sin(needleAngle) * (r - 8));

        // Centre pivot
        gc.setFill(Color.web(SURFACE));
        gc.fillOval(cx - 7, cy - 7, 14, 14);
        gc.setStroke(Color.web(color)); gc.setLineWidth(2);
        gc.strokeOval(cx - 7, cy - 7, 14, 14);

        // Value text inside
        String valStr = String.format("%.0f", value);
        gc.setFill(Color.web(WHITE));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        gc.fillText(valStr, cx - valStr.length() * 6, cy + 30);

        return c;
    }

    // Helpers

    private HBox zoneLegend(String range, String label, String color) {
        Region dot = new Region(); dot.setPrefSize(10, 10);
        dot.setStyle("-fx-background-color:" + color + ";-fx-background-radius:3;");
        Label r = new Label(range + "  ");
        r.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11)); r.setTextFill(Color.web(color));
        Label l = new Label(label);
        l.setFont(Font.font("Segoe UI", 11)); l.setTextFill(Color.web(WHITE + "77"));
        HBox box = new HBox(6, dot, r, l); box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        l.setTextFill(Color.web(WHITE));
        return l;
    }

    private Label bigLabel(String text, String color) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        l.setTextFill(Color.web(color));
        return l;
    }

    private Label descLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", 12));
        l.setTextFill(Color.web(WHITE + "55"));
        l.setWrapText(true);
        return l;
    }

    private String formatHour(int hour) {
        if (hour == 0)  return "Midnight";
        if (hour == 12) return "Noon";
        if (hour < 12)  return hour + " AM";
        return (hour - 12) + " PM";
    }

    private String plural(int n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    private String collaborationColor(String label) {
        return switch (label) {
            case "Well Distributed" -> "#3FB950";
            case "Small Team"       -> "#F0883E";
            default -> "#FF6B6B";
        };
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    private VBox card() {
        VBox c = new VBox(14); c.setPadding(new Insets(22, 24, 22, 24));
        c.setStyle("-fx-background-color:" + SURFACE + ";-fx-border-color:" + BORDER +
                ";-fx-border-width:1;-fx-border-radius:14;-fx-background-radius:14;");
        return c;
    }

    private ScrollPane styledScroll(VBox content) {
        ScrollPane sp = new ScrollPane(content); sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + NAVY + ";-fx-background-color:" + NAVY + ";");
        return sp;
    }
}
