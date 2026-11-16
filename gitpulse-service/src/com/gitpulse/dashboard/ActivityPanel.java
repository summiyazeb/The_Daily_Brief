package com.gitpulse.dashboard;

import com.gitpulse.Algorithm.*;
import javafx.geometry.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.text.*;
import java.util.List;

public class ActivityPanel {

    private static final String NAVY    = "#0D1117";
    private static final String CYAN    = "#00D4FF";
    private static final String WHITE   = "#E6EDF3";
    private static final String SURFACE = "#161B22";
    private static final String BORDER  = "#21262D";

    private final RepositoryReport report;

    public ActivityPanel(RepositoryReport report) { this.report = report; }

    public ScrollPane build() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(36, 36, 36, 36));
        List<MonthlyStats> monthly = report.getMonthlyBreakdown();
        content.getChildren().addAll(
                pageHeader(),
                monthlyBarChart(monthly),
                commitsPerWeekLineChart(monthly),
                trendCard(),
                extremeMonthsRow()
        );
        return styledScroll(content);
    }

    private VBox pageHeader() {
        Label title = new Label("Activity Analysis");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web(WHITE));
        Label sub = new Label("Monthly commit history, trend direction, and peak/quiet periods");
        sub.setFont(Font.font("Segoe UI", 13));
        sub.setTextFill(Color.web(WHITE + "55"));
        return new VBox(6, title, sub);
    }

    // Monthly commit bar chart with full axes
    private VBox monthlyBarChart(List<MonthlyStats> monthly) {
        VBox card = card();
        Label heading = new Label("Monthly Commit Volume");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        heading.setTextFill(Color.web(WHITE));

        HBox legend = new HBox(20);
        legend.getChildren().addAll(
                legendItem("Most Active", "#F0883E"),
                legendItem("Least Active", "#BC8CFF"),
                legendItem("Normal", CYAN)
        );
        card.getChildren().addAll(heading, legend);

        if (monthly == null || monthly.isEmpty()) { card.getChildren().add(emptyLabel()); return card; }

        int    n     = monthly.size();
        double padL  = 58, padR = 24, padT = 24, padB = 72;
        double cw    = Math.max(720, n * 72 + padL + padR);
        double ch    = 320;
        double plotW = cw - padL - padR;
        double plotH = ch - padT - padB;
        double barW  = Math.min(48, plotW / n - 12);
        int    maxC  = monthly.stream().mapToInt(MonthlyStats::getCommitCount).max().orElse(1);
        int    gridN = 5;

        Canvas canvas = new Canvas(cw, ch);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(NAVY));
        gc.fillRect(0, 0, cw, ch);

        // Dashed horizontal grid lines + Y labels
        for (int g = 0; g <= gridN; g++) {
            double y   = padT + plotH - g * plotH / gridN;
            int    val = (int) Math.round(maxC * g / (double) gridN);

            if (g > 0) {
                gc.setStroke(Color.web(BORDER));
                gc.setLineWidth(1);
                gc.setLineDashes(5, 5);
                gc.strokeLine(padL, y, cw - padR, y);
                gc.setLineDashes();
            }

            gc.setFill(Color.web(WHITE + "77"));
            gc.setFont(Font.font("Segoe UI", 11));
            String label = String.valueOf(val);
            gc.fillText(label, padL - 8 - label.length() * 6.5, y + 4);
        }

        // Y axis
        gc.setStroke(Color.web(WHITE + "44"));
        gc.setLineWidth(1.5);
        gc.strokeLine(padL, padT, padL, padT + plotH);

        // X axis
        gc.strokeLine(padL, padT + plotH, cw - padR, padT + plotH);

        // Bars with gradient fill
        for (int i = 0; i < n; i++) {
            MonthlyStats m   = monthly.get(i);
            double slotW     = plotW / n;
            double x         = padL + i * slotW + (slotW - barW) / 2.0;
            double barHeight = m.getCommitCount() * plotH / maxC;
            double y         = padT + plotH - barHeight;

            String hex = switch (m.getActivityTag()) {
                case "Most Active"  -> "#F0883E";
                case "Least Active" -> "#BC8CFF";
                default             -> CYAN;
            };

            // Gradient fill bottom→top
            LinearGradient grad = new LinearGradient(
                    0, y + barHeight, 0, y, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web(hex + "55")),
                    new Stop(1, Color.web(hex))
            );
            gc.setFill(grad);
            gc.fillRoundRect(x, y, barW, barHeight, 7, 7);

            // Bright top edge
            gc.setStroke(Color.web(hex));
            gc.setLineWidth(1.5);
            gc.strokeLine(x + 3, y + 1, x + barW - 3, y + 1);

            // Commit count above bar
            String countStr = String.valueOf(m.getCommitCount());
            gc.setFill(Color.web(WHITE));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            gc.fillText(countStr, x + barW / 2.0 - countStr.length() * 3.5, y - 7);

            // X-axis tick
            gc.setStroke(Color.web(WHITE + "33"));
            gc.setLineWidth(1);
            gc.strokeLine(x + barW / 2.0, padT + plotH, x + barW / 2.0, padT + plotH + 5);

            // Rotated X label
            gc.save();
            gc.translate(x + barW / 2.0, padT + plotH + 16);
            gc.rotate(-40);
            gc.setFill(Color.web(WHITE + "AA"));
            gc.setFont(Font.font("Segoe UI", 10));
            gc.fillText(m.getLabel(), 0, 0);
            gc.restore();
        }

        // Y-axis title (rotated)
        gc.save();
        gc.translate(13, padT + plotH / 2.0);
        gc.rotate(-90);
        gc.setFill(Color.web(WHITE + "55"));
        gc.setFont(Font.font("Segoe UI", 11));
        gc.fillText("Commits", -26, 0);
        gc.restore();

        // X-axis title
        gc.setFill(Color.web(WHITE + "55"));
        gc.setFont(Font.font("Segoe UI", 11));
        gc.fillText("Month", cw / 2.0 - 18, ch - 4);

        card.getChildren().add(scrollableCanvas(canvas, ch));
        return card;
    }

    // Commits-per-week line chart with area fill
    private VBox commitsPerWeekLineChart(List<MonthlyStats> monthly) {
        VBox card = card();
        Label heading = new Label("Commits / Week  (pace indicator)");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        heading.setTextFill(Color.web(WHITE));
        card.getChildren().add(heading);

        if (monthly == null || monthly.size() < 2) { card.getChildren().add(emptyLabel()); return card; }

        int    n     = monthly.size();
        double padL  = 58, padR = 24, padT = 28, padB = 72;
        double cw    = Math.max(720, n * 72 + padL + padR);
        double ch    = 270;
        double plotW = cw - padL - padR;
        double plotH = ch - padT - padB;
        double maxW  = monthly.stream().mapToDouble(MonthlyStats::getAvgCommitsPerWeek).max().orElse(1);
        int    gridN = 4;

        Canvas canvas = new Canvas(cw, ch);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(NAVY));
        gc.fillRect(0, 0, cw, ch);

        // Grid + Y labels
        for (int g = 0; g <= gridN; g++) {
            double y   = padT + plotH - g * plotH / gridN;
            double val = maxW * g / gridN;
            if (g > 0) {
                gc.setStroke(Color.web(BORDER));
                gc.setLineWidth(1);
                gc.setLineDashes(5, 5);
                gc.strokeLine(padL, y, cw - padR, y);
                gc.setLineDashes();
            }
            gc.setFill(Color.web(WHITE + "77"));
            gc.setFont(Font.font("Segoe UI", 11));
            String label = String.format("%.1f", val);
            gc.fillText(label, padL - 8 - label.length() * 6.5, y + 4);
        }

        // Axes
        gc.setStroke(Color.web(WHITE + "44")); gc.setLineWidth(1.5);
        gc.strokeLine(padL, padT, padL, padT + plotH);
        gc.strokeLine(padL, padT + plotH, cw - padR, padT + plotH);

        // Point positions
        double[] px = new double[n];
        double[] py = new double[n];
        for (int i = 0; i < n; i++) {
            double slotW = plotW / n;
            px[i] = padL + i * slotW + slotW / 2.0;
            py[i] = padT + plotH - (monthly.get(i).getAvgCommitsPerWeek() / maxW) * plotH;
        }

        // Shaded area under line
        gc.beginPath();
        gc.moveTo(px[0], padT + plotH);
        for (int i = 0; i < n; i++) gc.lineTo(px[i], py[i]);
        gc.lineTo(px[n-1], padT + plotH);
        gc.closePath();
        LinearGradient areaGrad = new LinearGradient(
                0, padT, 0, padT + plotH, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(CYAN + "40")),
                new Stop(1, Color.web(CYAN + "05"))
        );
        gc.setFill(areaGrad);
        gc.fill();

        // Line
        gc.setStroke(Color.web(CYAN));
        gc.setLineWidth(2.5);
        gc.beginPath();
        gc.moveTo(px[0], py[0]);
        for (int i = 1; i < n; i++) gc.lineTo(px[i], py[i]);
        gc.stroke();

        // Dots + labels
        for (int i = 0; i < n; i++) {
            // Glow ring
            gc.setFill(Color.web(CYAN + "30"));
            gc.fillOval(px[i] - 8, py[i] - 8, 16, 16);
            // White centre dot
            gc.setFill(Color.web(WHITE));
            gc.fillOval(px[i] - 4, py[i] - 4, 8, 8);
            // Cyan outline
            gc.setStroke(Color.web(CYAN));
            gc.setLineWidth(2);
            gc.strokeOval(px[i] - 4, py[i] - 4, 8, 8);

            // Value above dot
            String val = String.format("%.1f", monthly.get(i).getAvgCommitsPerWeek());
            gc.setFill(Color.web(WHITE));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.fillText(val, px[i] - val.length() * 3.0, py[i] - 12);

            // X-axis tick + label
            gc.setStroke(Color.web(WHITE + "33")); gc.setLineWidth(1);
            gc.strokeLine(px[i], padT + plotH, px[i], padT + plotH + 5);
            gc.save();
            gc.translate(px[i], padT + plotH + 16);
            gc.rotate(-40);
            gc.setFill(Color.web(WHITE + "AA"));
            gc.setFont(Font.font("Segoe UI", 10));
            gc.fillText(monthly.get(i).getLabel(), 0, 0);
            gc.restore();
        }

        // Axis titles
        gc.save();
        gc.translate(13, padT + plotH / 2.0);
        gc.rotate(-90);
        gc.setFill(Color.web(WHITE + "55"));
        gc.setFont(Font.font("Segoe UI", 11));
        gc.fillText("Commits/Week", -44, 0);
        gc.restore();
        gc.setFill(Color.web(WHITE + "55"));
        gc.setFont(Font.font("Segoe UI", 11));
        gc.fillText("Month", cw / 2.0 - 18, ch - 4);

        card.getChildren().add(scrollableCanvas(canvas, ch));
        return card;
    }

    // Trend card
    private VBox trendCard() {
        VBox card = card();
        Label heading = new Label("Activity Trend");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        heading.setTextFill(Color.web(WHITE));
        card.getChildren().add(heading);

        ActivityTrend trend = report.getActivityTrend();
        if (trend == null) { card.getChildren().add(emptyLabel()); return card; }

        String tc = trendColor(trend.getTrendType().name());
        HBox trendRow = new HBox(24);
        trendRow.setAlignment(Pos.CENTER_LEFT);

        Label trendType = new Label(trendIcon(trend.getTrendType().name()) + "  " + trend.getTrendType().name());
        trendType.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        trendType.setTextFill(Color.web(tc));
        trendType.setStyle("-fx-background-color:" + tc + "22;-fx-border-color:" + tc + "55;" +
                "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20 10 20;");

        VBox stats = new VBox(8);
        stats.getChildren().addAll(
                trendStat("Slope",      String.format("%+.2f commits/month", trend.getSlopePerMonth()), tc),
                trendStat("Volatility", String.format("%.2f std dev",         trend.getVolatility()),   WHITE)
        );
        trendRow.getChildren().addAll(trendType, stats);

        Label plain = new Label("\"" + trend.getPlainEnglish() + "\"");
        plain.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 13));
        plain.setTextFill(Color.web(WHITE + "BB"));
        plain.setWrapText(true);

        card.getChildren().addAll(trendRow, plain);
        return card;
    }

    // Extreme months
    private HBox extremeMonthsRow() {
        HBox row = new HBox(16);
        VBox mostCard  = extremeCard("🔥  Busiest Period",  report.getMostActiveMonths(),  "#F0883E");
        VBox leastCard = extremeCard("🧊  Quietest Period", report.getLeastActiveMonths(), "#BC8CFF");
        HBox.setHgrow(mostCard, Priority.ALWAYS);
        HBox.setHgrow(leastCard, Priority.ALWAYS);
        row.getChildren().addAll(mostCard, leastCard);
        return row;
    }

    private VBox extremeCard(String title, List<MonthlyStats> months, String color) {
        VBox card = card();
        Label heading = new Label(title);
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        heading.setTextFill(Color.web(color));
        card.getChildren().add(heading);
        if (months == null || months.isEmpty()) {
            Label l = new Label("Not enough data");
            l.setTextFill(Color.web(WHITE + "44")); l.setFont(Font.font("Segoe UI", 12));
            card.getChildren().add(l); return card;
        }
        for (MonthlyStats m : months) {
            Label month = new Label(m.getLabel());
            month.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            month.setTextFill(Color.web(WHITE));
            Label detail = new Label(m.getCommitCount() + " commits  ·  "
                    + String.format("%.1f", m.getAvgCommitsPerWeek()) + "/week  ·  "
                    + m.getUniqueContributors() + " contributor(s)");
            detail.setFont(Font.font("Segoe UI", 12));
            detail.setTextFill(Color.web(color));
            card.getChildren().addAll(month, detail);
        }
        return card;
    }

    // Helpers
    private ScrollPane scrollableCanvas(Canvas c, double h) {
        ScrollPane sp = new ScrollPane(c);
        sp.setFitToHeight(true); sp.setPrefHeight(h + 20);
        sp.setStyle("-fx-background:" + NAVY + ";-fx-background-color:" + NAVY + ";");
        return sp;
    }

    private HBox trendStat(String key, String value, String color) {
        Label k = new Label(key + ": "); k.setFont(Font.font("Segoe UI", 13)); k.setTextFill(Color.web(WHITE + "66"));
        Label v = new Label(value); v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13)); v.setTextFill(Color.web(color));
        HBox row = new HBox(4, k, v); row.setAlignment(Pos.CENTER_LEFT); return row;
    }

    private HBox legendItem(String label, String color) {
        Region dot = new Region(); dot.setPrefSize(10, 10);
        dot.setStyle("-fx-background-color:" + color + ";-fx-background-radius:3;");
        Label l = new Label(label); l.setFont(Font.font("Segoe UI", 12)); l.setTextFill(Color.web(WHITE + "88"));
        HBox box = new HBox(6, dot, l); box.setAlignment(Pos.CENTER_LEFT); return box;
    }

    private String trendColor(String t) {
        return switch (t) {
            case "GROWING" -> "#3FB950"; case "DECLINING" -> "#FF6B6B";
            case "STABLE"  -> "#00D4FF"; case "SPORADIC"  -> "#F0883E";
            default -> "#8B949E";
        };
    }

    private String trendIcon(String t) {
        return switch (t) {
            case "GROWING" -> "📈"; case "DECLINING" -> "📉";
            case "STABLE"  -> "➡";  case "SPORADIC"  -> "〰";
            default -> "❓";
        };
    }

    private Label emptyLabel() {
        Label l = new Label("No activity data available."); l.setFont(Font.font("Segoe UI", 13));
        l.setTextFill(Color.web(WHITE + "44")); return l;
    }

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
