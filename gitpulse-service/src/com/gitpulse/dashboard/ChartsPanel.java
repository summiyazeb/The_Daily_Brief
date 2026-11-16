package com.gitpulse.dashboard;

import com.gitpulse.Algorithm.ContributorScore;
import com.gitpulse.Algorithm.MonthlyStats;
import com.gitpulse.Algorithm.RepositoryReport;
import com.gitpulse.model.Commit;
import com.gitpulse.model.Repository;

import javafx.geometry.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.util.*;
import java.util.List;

/*
  CHARTS PANEL

   1. DONUT CHART      — commit share per contributor
                         Shows who "owns" the codebase at a glance.

   2. COMMIT HEATMAP   — commits by day-of-week (rows) × hour-of-day (cols)
                         Reveals when the team is most productive.

   3. CUMULATIVE LINE  — running total of commits over monthly time axis
                         Shows the project's overall growth trajectory.
 */

public class ChartsPanel {

    // Design tokens
    private static final String NAVY    = "#0D1117";
    private static final String CYAN    = "#00D4FF";
    private static final String WHITE   = "#E6EDF3";
    private static final String SURFACE = "#161B22";
    private static final String BORDER  = "#21262D";

    // Contributor colour palette
    private static final String[] PALETTE = {
            "#00D4FF", "#3FB950", "#F0883E", "#BC8CFF",
            "#FF6B6B", "#FFD700", "#56D364", "#79C0FF"
    };

    private final Repository       repository;
    private final RepositoryReport report;

    public ChartsPanel(Repository repository, RepositoryReport report) {
        this.repository = repository;
        this.report     = report;
    }

    // Entry point
    public ScrollPane build() {
        VBox content = new VBox(32);
        content.setPadding(new Insets(36, 36, 36, 36));

        content.getChildren().addAll(
                pageHeader(),
                donutSection(),
                heatmapSection(),
                cumulativeSection()
        );

        return styledScroll(content);
    }

    // Page header
    private VBox pageHeader() {
        Label title = new Label("Visual Charts");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web(WHITE));

        Label sub = new Label(
                "Commit ownership · productivity heatmap · project growth — none of these are on GitHub");
        sub.setFont(Font.font("Segoe UI", 13));
        sub.setTextFill(Color.web(WHITE + "55"));

        return new VBox(6, title, sub);
    }

    // Donut Chart
    private VBox donutSection() {
        VBox card = card();

        Label heading = new Label("🍩  Commit Ownership");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        heading.setTextFill(Color.web(WHITE));

        Label sub = new Label("Who owns what percentage of the codebase");
        sub.setFont(Font.font("Segoe UI", 12));
        sub.setTextFill(Color.web(WHITE + "44"));

        card.getChildren().addAll(heading, sub);

        List<ContributorScore> ranked = report.getRankedContributors();
        if (ranked == null || ranked.isEmpty()) {
            card.getChildren().add(emptyLabel("No contributor data available."));
            return card;
        }

        // Canvas dimensions
        double cw = 700, ch = 380;
        double cx = 220, cy = ch / 2.0;   // donut centre
        double outerR = 150, innerR = 80;  // ring thickness

        Canvas canvas = new Canvas(cw, ch);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(NAVY));
        gc.fillRect(0, 0, cw, ch);

        // Outer glow ring
        gc.setStroke(Color.web(CYAN + "11"));
        gc.setLineWidth(20);
        gc.strokeOval(cx - outerR - 10, cy - outerR - 10,
                (outerR + 10) * 2, (outerR + 10) * 2);

        // Calculate total for percentage
        double total = ranked.stream().mapToDouble(ContributorScore::getCommitShare).sum();
        if (total == 0) total = 1;

        // Draw segments
        double startAngle = -90; // start at top
        int n = ranked.size();

        double[] midAngles = new double[n]; // for label lines

        for (int i = 0; i < n; i++) {
            ContributorScore cs = ranked.get(i);
            double share  = cs.getCommitShare() / total * 100.0;
            double sweep  = share / 100.0 * 360.0;
            double midAng = startAngle + sweep / 2.0;
            midAngles[i]  = midAng;

            String hex = PALETTE[i % PALETTE.length];

            // Shadow / depth layer
            gc.setFill(Color.web(hex + "22"));
            gc.fillArc(cx - outerR - 4, cy - outerR - 4,
                    (outerR + 4) * 2, (outerR + 4) * 2,
                    -startAngle - sweep, sweep,
                    javafx.scene.shape.ArcType.ROUND);

            // Main segment
            gc.setFill(Color.web(hex));
            gc.fillArc(cx - outerR, cy - outerR,
                    outerR * 2, outerR * 2,
                    -startAngle - sweep, sweep,
                    javafx.scene.shape.ArcType.ROUND);

            // Gap between segments (thin dark line)
            gc.setStroke(Color.web(NAVY));
            gc.setLineWidth(2);
            gc.strokeArc(cx - outerR, cy - outerR,
                    outerR * 2, outerR * 2,
                    -startAngle - sweep, sweep,
                    javafx.scene.shape.ArcType.ROUND);

            startAngle += sweep;
        }

        // Punch out the inner hole → makes it a donut
        gc.setFill(Color.web(NAVY));
        gc.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

        // Inner ring border
        gc.setStroke(Color.web(BORDER));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

        // Centre text — total commits
        int totalCommits = ranked.stream().mapToInt(ContributorScore::getTotalCommits).sum();
        gc.setFill(Color.web(WHITE));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        String totalStr = String.valueOf(totalCommits);
        gc.fillText(totalStr, cx - totalStr.length() * 7, cy + 8);
        gc.setFill(Color.web(WHITE + "55"));
        gc.setFont(Font.font("Segoe UI", 11));
        gc.fillText("commits", cx - 22, cy + 24);

        // Legend on the right side
        double legendX = cx + outerR + 50;
        double legendY = cy - (n * 34) / 2.0;

        for (int i = 0; i < n; i++) {
            ContributorScore cs = ranked.get(i);
            String hex = PALETTE[i % PALETTE.length];
            double ly   = legendY + i * 38;

            // Colour swatch
            gc.setFill(Color.web(hex));
            gc.fillRoundRect(legendX, ly, 14, 14, 4, 4);

            // Username
            gc.setFill(Color.web(WHITE));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            gc.fillText(cs.getUsername(), legendX + 22, ly + 12);

            // Percentage + commit count
            gc.setFill(Color.web(hex));
            gc.setFont(Font.font("Segoe UI", 12));
            gc.fillText(String.format("%.1f%%  (%d commits)",
                            cs.getCommitShare(), cs.getTotalCommits()),
                    legendX + 22, ly + 27);
        }

        card.getChildren().add(centredCanvas(canvas, ch));
        return card;
    }

    // Commit HeatMap (Day of Week × Hour of Day)
    private VBox heatmapSection() {
        VBox card = card();

        Label heading = new Label("🔥  Commit Heatmap  —  When Does the Team Work?");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        heading.setTextFill(Color.web(WHITE));

        Label sub = new Label("Rows = day of week  ·  Columns = hour of day (UTC)  ·  Darker = more commits");
        sub.setFont(Font.font("Segoe UI", 12));
        sub.setTextFill(Color.web(WHITE + "44"));

        card.getChildren().addAll(heading, sub);

        List<Commit> commits = repository.getCommits();
        if (commits == null || commits.isEmpty()) {
            card.getChildren().add(emptyLabel("No commit data available."));
            return card;
        }

        // Build 7×24 matrix  [dayOfWeek 0=MON..6=SUN][hour 0..23]
        int[][] matrix = new int[7][24];
        int maxVal = 0;

        for (Commit c : commits) {
            try {
                Instant inst = Instant.parse(c.getDate());
                var zdt  = inst.atZone(ZoneId.of("UTC"));
                int day  = zdt.getDayOfWeek().getValue() - 1; // Mon=0 … Sun=6
                int hour = zdt.getHour();
                matrix[day][hour]++;
                if (matrix[day][hour] > maxVal) maxVal = matrix[day][hour];
            } catch (Exception ignored) {}
        }

        if (maxVal == 0) {
            card.getChildren().add(emptyLabel("Could not parse commit dates."));
            return card;
        }

        // Canvas layout
        double padL  = 72;   // space for day labels
        double padT  = 32;   // space for hour labels
        double padR  = 20;
        double padB  = 48;   // space for colour scale legend
        double cellW = 28;
        double cellH = 28;
        double gap   = 3;
        double cw    = padL + 24 * (cellW + gap) + padR;
        double ch    = padT + 7  * (cellH + gap) + padB;

        Canvas canvas = new Canvas(cw, ch);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(NAVY));
        gc.fillRect(0, 0, cw, ch);

        String[] days  = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        String[] hours = {"0","1","2","3","4","5","6","7","8","9","10","11",
                "12","13","14","15","16","17","18","19","20","21","22","23"};

        // Hour labels (top)
        gc.setFill(Color.web(WHITE + "55"));
        gc.setFont(Font.font("Segoe UI", 9));
        for (int h = 0; h < 24; h++) {
            double x = padL + h * (cellW + gap) + cellW / 2.0 - hours[h].length() * 3.0;
            gc.fillText(hours[h], x, padT - 8);
        }

        // Day labels (left) + cells
        for (int d = 0; d < 7; d++) {
            double y = padT + d * (cellH + gap);

            // Day label
            gc.setFill(Color.web(WHITE + "88"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            gc.fillText(days[d], 4, y + cellH / 2.0 + 4);

            for (int h = 0; h < 24; h++) {
                double x        = padL + h * (cellW + gap);
                double intensity = (double) matrix[d][h] / maxVal;
                int    count     = matrix[d][h];

                // Cell background (always render empty cells faintly)
                gc.setFill(Color.web(BORDER));
                gc.fillRoundRect(x, y, cellW, cellH, 5, 5);

                if (count > 0) {
                    // Heat colour: low=dark cyan, mid=orange, high=bright green
                    Color cellColor = heatColor(intensity);
                    gc.setFill(cellColor);
                    gc.fillRoundRect(x, y, cellW, cellH, 5, 5);

                    // Commit count inside cell (only if space allows)
                    if (count > 0) {
                        gc.setFill(intensity > 0.5
                                ? Color.web(NAVY + "CC")
                                : Color.web(WHITE + "CC"));
                        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
                        String cnt = String.valueOf(count);
                        gc.fillText(cnt,
                                x + cellW / 2.0 - cnt.length() * 3.0,
                                y + cellH / 2.0 + 4);
                    }
                }
            }
        }

        // Colour scale legend at bottom
        double scaleY  = padT + 7 * (cellH + gap) + 12;
        double scaleW  = 200;
        double scaleX  = padL;

        gc.setFill(Color.web(WHITE + "44"));
        gc.setFont(Font.font("Segoe UI", 10));
        gc.fillText("Less", scaleX - 28, scaleY + 10);
        gc.fillText("More", scaleX + scaleW + 6, scaleY + 10);

        // Gradient bar
        LinearGradient scaleGrad = new LinearGradient(
                scaleX, 0, scaleX + scaleW, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0,  Color.web(BORDER)),
                new Stop(0.33, Color.web("#00D4FF88")),
                new Stop(0.66, Color.web("#F0883E")),
                new Stop(1.0,  Color.web("#3FB950"))
        );
        gc.setFill(scaleGrad);
        gc.fillRoundRect(scaleX, scaleY, scaleW, 10, 5, 5);

        // Peak annotation
        int[] peak = findPeak(matrix);
        String peakStr = String.format("Peak: %s %s:00 UTC  (%d commits)",
                days[peak[0]], hours[peak[1]], matrix[peak[0]][peak[1]]);
        gc.setFill(Color.web(CYAN));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        gc.fillText(peakStr, scaleX + scaleW + 30, scaleY + 10);

        card.getChildren().add(centredCanvas(canvas, ch));
        return card;
    }

    // CUMULATIVE COMMITS LINE CHART
    private VBox cumulativeSection() {
        VBox card = card();

        Label heading = new Label("📈  Cumulative Commits  —  Project Growth Over Time");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        heading.setTextFill(Color.web(WHITE));

        Label sub = new Label(
                "Running total of commits month by month — steeper slope = faster growth");
        sub.setFont(Font.font("Segoe UI", 12));
        sub.setTextFill(Color.web(WHITE + "44"));

        card.getChildren().addAll(heading, sub);

        List<MonthlyStats> monthly = report.getMonthlyBreakdown();
        if (monthly == null || monthly.size() < 2) {
            card.getChildren().add(emptyLabel("Not enough monthly data (need at least 2 months)."));
            return card;
        }

        int    n       = monthly.size();
        double padL    = 72, padR = 24, padT = 28, padB = 72;
        double cw      = Math.max(760, n * 72 + padL + padR);
        double ch      = 320;
        double plotW   = cw - padL - padR;
        double plotH   = ch - padT - padB;
        int    gridN   = 5;

        // Build cumulative array
        int[] cumulative = new int[n];
        int running = 0;
        for (int i = 0; i < n; i++) {
            running += monthly.get(i).getCommitCount();
            cumulative[i] = running;
        }
        int maxC = cumulative[n - 1];
        if (maxC == 0) maxC = 1;

        Canvas canvas = new Canvas(cw, ch);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(NAVY));
        gc.fillRect(0, 0, cw, ch);

        // Grid + Y labels
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
            gc.setFill(Color.web(WHITE + "66"));
            gc.setFont(Font.font("Segoe UI", 11));
            String lbl = String.valueOf(val);
            gc.fillText(lbl, padL - 8 - lbl.length() * 6.5, y + 4);
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
            py[i] = padT + plotH - (cumulative[i] / (double) maxC) * plotH;
        }

        // Shaded area under curve (gradient fill)
        gc.beginPath();
        gc.moveTo(px[0], padT + plotH);
        gc.lineTo(px[0], py[0]);
        for (int i = 1; i < n; i++) {
            // Smooth bezier curve between points
            double cpx = (px[i - 1] + px[i]) / 2.0;
            gc.bezierCurveTo(cpx, py[i - 1], cpx, py[i], px[i], py[i]);
        }
        gc.lineTo(px[n - 1], padT + plotH);
        gc.closePath();

        LinearGradient areaGrad = new LinearGradient(
                0, padT, 0, padT + plotH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web(CYAN + "50")),
                new Stop(1.0, Color.web(CYAN + "05"))
        );
        gc.setFill(areaGrad);
        gc.fill();

        // Main line (smooth bezier)
        gc.setStroke(Color.web(CYAN));
        gc.setLineWidth(2.5);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.beginPath();
        gc.moveTo(px[0], py[0]);
        for (int i = 1; i < n; i++) {
            double cpx = (px[i - 1] + px[i]) / 2.0;
            gc.bezierCurveTo(cpx, py[i - 1], cpx, py[i], px[i], py[i]);
        }
        gc.stroke();

        // Monthly increment bars (thin, behind the line — shows per-month contribution)
        for (int i = 0; i < n; i++) {
            int monthCount = monthly.get(i).getCommitCount();
            double barH    = monthCount / (double) maxC * plotH;
            double barW    = Math.min(plotW / n - 8, 20);
            double bx      = px[i] - barW / 2.0;
            double by      = padT + plotH - barH;

            gc.setFill(Color.web(CYAN + "20"));
            gc.fillRoundRect(bx, by, barW, barH, 3, 3);
        }

        // Dots on curve
        for (int i = 0; i < n; i++) {
            // Glow
            gc.setFill(Color.web(CYAN + "30"));
            gc.fillOval(px[i] - 9, py[i] - 9, 18, 18);
            // White fill
            gc.setFill(Color.web(WHITE));
            gc.fillOval(px[i] - 4, py[i] - 4, 8, 8);
            // Cyan border
            gc.setStroke(Color.web(CYAN)); gc.setLineWidth(2);
            gc.strokeOval(px[i] - 4, py[i] - 4, 8, 8);

            // Cumulative count above dot
            String val = String.valueOf(cumulative[i]);
            gc.setFill(Color.web(WHITE));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.fillText(val, px[i] - val.length() * 3.0, py[i] - 13);

            // X-axis tick + rotated month label
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

        // Milestone annotation — steepest growth period
        int steepestIdx = steepestMonth(cumulative);
        if (steepestIdx > 0) {
            double ax = px[steepestIdx];
            double ay = py[steepestIdx] - 22;
            int    delta = cumulative[steepestIdx] - cumulative[steepestIdx - 1];

            // Vertical dashed line
            gc.setStroke(Color.web("#FFD700" + "88")); gc.setLineWidth(1);
            gc.setLineDashes(4, 4);
            gc.strokeLine(ax, ay + 16, ax, padT + plotH);
            gc.setLineDashes();

            // Badge
            String badge = "+" + delta + " commits";
            double bw = badge.length() * 7.0 + 16;
            gc.setFill(Color.web("#FFD700" + "22"));
            gc.fillRoundRect(ax - bw / 2.0, ay - 16, bw, 18, 6, 6);
            gc.setStroke(Color.web("#FFD700" + "88")); gc.setLineWidth(1);
            gc.strokeRoundRect(ax - bw / 2.0, ay - 16, bw, 18, 6, 6);
            gc.setFill(Color.web("#FFD700"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.fillText(badge, ax - bw / 2.0 + 8, ay - 2);
        }

        // Axis titles
        gc.save();
        gc.translate(13, padT + plotH / 2.0); gc.rotate(-90);
        gc.setFill(Color.web(WHITE + "55")); gc.setFont(Font.font("Segoe UI", 11));
        gc.fillText("Total Commits", -44, 0); gc.restore();
        gc.setFill(Color.web(WHITE + "55")); gc.setFont(Font.font("Segoe UI", 11));
        gc.fillText("Month", cw / 2.0 - 18, ch - 4);

        // Summary stat chips below chart
        HBox chips = new HBox(16);
        chips.setPadding(new Insets(12, 0, 0, 0));
        chips.setAlignment(Pos.CENTER_LEFT);
        chips.getChildren().addAll(
                chip("Total Commits",    String.valueOf(maxC),          CYAN),
                chip("Months Active",    String.valueOf(n),              "#3FB950"),
                chip("Avg / Month",      String.format("%.1f", (double) maxC / n), "#F0883E"),
                chip("Peak Month",       peakMonthLabel(monthly),       "#FFD700")
        );

        card.getChildren().addAll(scrollableCanvas(canvas, ch), chips);
        return card;
    }

    // Maths helpers

    // Returns [day, hour] of the heatmap cell with the most commits.
    private int[] findPeak(int[][] matrix) {
        int maxVal = 0, pd = 0, ph = 0;
        for (int d = 0; d < 7; d++)
            for (int h = 0; h < 24; h++)
                if (matrix[d][h] > maxVal) { maxVal = matrix[d][h]; pd = d; ph = h; }
        return new int[]{pd, ph};
    }

    // Returns the index with the largest single-month increment in cumulative array.
    private int steepestMonth(int[] cumulative) {
        int maxDelta = 0, idx = 1;
        for (int i = 1; i < cumulative.length; i++) {
            int delta = cumulative[i] - cumulative[i - 1];
            if (delta > maxDelta) { maxDelta = delta; idx = i; }
        }
        return idx;
    }

    private String peakMonthLabel(List<MonthlyStats> monthly) {
        return monthly.stream()
                .max(Comparator.comparingInt(MonthlyStats::getCommitCount))
                .map(MonthlyStats::getLabel)
                .orElse("—");
    }

    /*
      Maps a 0–1 intensity to a heat colour:
        0.0  → dark border grey  (no activity)
        0.33 → cyan              (low)
        0.66 → orange            (mid)
        1.0  → bright green      (peak)
     */
    private Color heatColor(double t) {
        if (t <= 0)    return Color.web(BORDER);
        if (t <= 0.33) return interpolate(Color.web("#00D4FF44"), Color.web("#00D4FF"), t / 0.33);
        if (t <= 0.66) return interpolate(Color.web("#00D4FF"),   Color.web("#F0883E"), (t - 0.33) / 0.33);
        return             interpolate(Color.web("#F0883E"),   Color.web("#3FB950"), (t - 0.66) / 0.34);
    }

    private Color interpolate(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                a.getRed()   + (b.getRed()   - a.getRed())   * t,
                a.getGreen() + (b.getGreen() - a.getGreen()) * t,
                a.getBlue()  + (b.getBlue()  - a.getBlue())  * t,
                a.getOpacity()+ (b.getOpacity()- a.getOpacity()) * t
        );
    }

    // UI helpers

    private HBox chip(String label, String value, String color) {
        VBox box = new VBox(3);
        box.setPadding(new Insets(8, 14, 8, 14));
        box.setStyle(
                "-fx-background-color:" + color + "18;" +
                        "-fx-border-color:" + color + "44;-fx-border-width:1;" +
                        "-fx-border-radius:8;-fx-background-radius:8;"
        );
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 10));
        lbl.setTextFill(Color.web(WHITE + "55"));
        Label val = new Label(value);
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        val.setTextFill(Color.web(color));
        box.getChildren().addAll(lbl, val);
        return new HBox(box);
    }

    // Wraps a canvas in a centred HBox (for fixed-width canvases like the donut)
    private HBox centredCanvas(Canvas c, double h) {
        ScrollPane sp = new ScrollPane(c);
        sp.setFitToHeight(true);
        sp.setPrefHeight(h + 20);
        sp.setStyle("-fx-background:" + NAVY + ";-fx-background-color:" + NAVY + ";");
        HBox box = new HBox(sp);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // Wraps a canvas in a horizontally scrollable pane
    private ScrollPane scrollableCanvas(Canvas c, double h) {
        ScrollPane sp = new ScrollPane(c);
        sp.setFitToHeight(true);
        sp.setPrefHeight(h + 20);
        sp.setStyle("-fx-background:" + NAVY + ";-fx-background-color:" + NAVY + ";");
        return sp;
    }

    private Label emptyLabel(String msg) {
        Label l = new Label(msg);
        l.setFont(Font.font("Segoe UI", 13));
        l.setTextFill(Color.web(WHITE + "44"));
        l.setPadding(new Insets(16, 0, 8, 0));
        return l;
    }

    private VBox card() {
        VBox c = new VBox(14);
        c.setPadding(new Insets(24, 26, 24, 26));
        c.setStyle(
                "-fx-background-color:" + SURFACE + ";-fx-border-color:" + BORDER + ";" +
                        "-fx-border-width:1;-fx-border-radius:14;-fx-background-radius:14;"
        );
        return c;
    }

    private ScrollPane styledScroll(VBox content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + NAVY + ";-fx-background-color:" + NAVY + ";");
        return sp;
    }
}