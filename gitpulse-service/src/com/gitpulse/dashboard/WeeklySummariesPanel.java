package com.gitpulse.dashboard;

import com.gitpulse.model.Repository;
import com.gitpulse.model.WeeklySummary;
import com.gitpulse.service.DataService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
  WEEKLY SUMMARIES PANEL

  Displays per-week commit activity with:
   1. Summary header cards  (total weeks, total commits, avg commits/week, top contributor)
   2. Horizontal bar chart  (commits per week, colour-coded by volume)
   3. AI-generated summary cards for each week (loaded asynchronously off the UI thread)

 */
public class WeeklySummariesPanel {

    // Design tokens
    private static final String NAVY    = "#0D1117";
    private static final String CYAN    = "#00D4FF";
    private static final String WHITE   = "#E6EDF3";
    private static final String SURFACE = "#161B22";
    private static final String BORDER  = "#21262D";
    private static final String GREEN   = "#3FB950";
    private static final String ORANGE  = "#F0883E";
    private static final String PURPLE  = "#BC8CFF";
    private static final String RED     = "#FF6B6B";
    private static final String GOLD    = "#FFD700";

    // Colour ramp used for bar chart
    private static final String BAR_LOW  = "#00D4FF";  // cyan
    private static final String BAR_MID  = "#F0883E";  // orange
    private static final String BAR_HIGH = "#3FB950";  // green

    private static final DateTimeFormatter WEEK_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");

    // Build entry point

    /**
      Builds and returns the full panel as a ScrollPane.
      AI summaries are loaded asynchronously — the chart and header cards
      are always shown immediately.

      @param repository the already-loaded Repository object
     */

    public ScrollPane build(Repository repository) {
        VBox content = new VBox(28);
        content.setPadding(new Insets(36, 36, 36, 36));

        // Page header
        content.getChildren().add(pageHeader());

        // Fetch weekly data synchronously
        DataService dataService = new DataService();

        // We load a lightweight version without AI first so the bar chart
        // renders instantly, then enrich with AI text in the background.
        var weeklyMap = repository.getWeeklyActivity();

        if (weeklyMap == null || weeklyMap.isEmpty()) {
            content.getChildren().add(emptyState());
            return styledScroll(content);
        }

        // Build bare WeeklySummary list (no AI text yet) for immediate rendering
        com.gitpulse.service.WeeklySummaryService wss =
                new com.gitpulse.service.WeeklySummaryService();
        List<WeeklySummary> summaries = wss.generate(weeklyMap);

        if (summaries.isEmpty()) {
            content.getChildren().add(emptyState());
            return styledScroll(content);
        }

        // Sort oldest → newest for the chart; newest first for card list
        summaries.sort(java.util.Comparator.comparing(WeeklySummary::getWeekStart));

        // Header stat cards
        content.getChildren().add(headerStatsRow(summaries));

        // Bar chart (always visible immediately)
        content.getChildren().add(weeklyBarChart(summaries));

        // Per-week AI summary cards (rendered with loading placeholders,
        //       then filled in once the background Task completes)
        VBox aiCardsContainer = new VBox(14);
        Label aiHeader = sectionLabel("🤖  AI Weekly Summaries");
        aiCardsContainer.getChildren().add(aiHeader);

        // Render newest-first in the card list
        List<WeeklySummary> displayOrder = new java.util.ArrayList<>(summaries);
        java.util.Collections.reverse(displayOrder);

        // Create a card per week immediately (shows spinner while loading)
        for (WeeklySummary ws : displayOrder) {
            VBox card = weeklyAiCard(ws, null /* loading */);
            aiCardsContainer.getChildren().add(card);
        }
        content.getChildren().add(aiCardsContainer);

        // Background task: fetch AI summaries, update cards progressively
        Task<List<WeeklySummary>> aiTask = new Task<>() {
            @Override
            protected List<WeeklySummary> call() {
                return dataService.getWeeklySummaries(repository);
            }
        };

        aiTask.setOnSucceeded(e -> Platform.runLater(() -> {
            List<WeeklySummary> enriched = aiTask.getValue();
            if (enriched == null) return;
            enriched.sort(java.util.Comparator.comparing(WeeklySummary::getWeekStart)
                    .reversed());

            // Replace placeholder cards with real AI content
            // Remove all cards except the header label (index 0)
            while (aiCardsContainer.getChildren().size() > 1) {
                aiCardsContainer.getChildren().remove(1);
            }
            for (WeeklySummary ws : enriched) {
                aiCardsContainer.getChildren().add(weeklyAiCard(ws, ws.getSummaryText()));
            }
        }));

        aiTask.setOnFailed(e -> Platform.runLater(() -> {
            while (aiCardsContainer.getChildren().size() > 1) {
                aiCardsContainer.getChildren().remove(1);
            }
            Label err = new Label("⚠  Could not load AI summaries: "
                    + aiTask.getException().getMessage());
            err.setFont(Font.font("Segoe UI", 13));
            err.setTextFill(Color.web(RED));
            err.setWrapText(true);
            aiCardsContainer.getChildren().add(err);
        }));

        Thread aiThread = new Thread(aiTask);
        aiThread.setDaemon(true);
        aiThread.start();

        return styledScroll(content);
    }

    // Page header

    private VBox pageHeader() {
        Label title = new Label("Weekly Summaries");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web(WHITE));

        Label sub = new Label(
                "Per-week commit volume, top contributors, and AI-generated insights");
        sub.setFont(Font.font("Segoe UI", 13));
        sub.setTextFill(Color.web(WHITE + "55"));

        return new VBox(6, title, sub);
    }

    // Header stat cards

    private HBox headerStatsRow(List<WeeklySummary> summaries) {
        int totalWeeks   = summaries.size();
        int totalCommits = summaries.stream().mapToInt(WeeklySummary::getTotalCommits).sum();
        double avgPerWeek = totalWeeks > 0 ? (double) totalCommits / totalWeeks : 0;

        // Find the overall top contributor (most frequent topAuthor across weeks)
        java.util.Map<String, Long> authorFreq = new java.util.HashMap<>();
        for (WeeklySummary ws : summaries) {
            if (ws.getTopAuthor() != null && !ws.getTopAuthor().equals("Unknown")) {
                authorFreq.merge(ws.getTopAuthor(), 1L, Long::sum);
            }
        }
        String overallTop = authorFreq.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("Unknown");

        // Peak week
        WeeklySummary peak = summaries.stream()
                .max(java.util.Comparator.comparingInt(WeeklySummary::getTotalCommits))
                .orElse(null);
        String peakLabel = peak != null
                ? peak.getTotalCommits() + " commits"
                : "—";

        HBox row = new HBox(16);
        row.getChildren().addAll(
                miniStatCard("📅  Total Weeks",       String.valueOf(totalWeeks),   CYAN),
                miniStatCard("📝  Total Commits",      String.valueOf(totalCommits), GREEN),
                miniStatCard("📊  Avg / Week",          String.format("%.1f", avgPerWeek), ORANGE),
                miniStatCard("🏆  Peak Week",           peakLabel,                  GOLD),
                miniStatCard("👑  Most Active Person",  overallTop,                 PURPLE)
        );
        return row;
    }

    private VBox miniStatCard(String label, String value, String color) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(
                "-fx-background-color:" + SURFACE + ";-fx-border-color:" + BORDER + ";" +
                        "-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;"
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 11));
        lbl.setTextFill(Color.web(WHITE + "55"));

        Label val = new Label(value);
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        val.setTextFill(Color.web(color));

        card.getChildren().addAll(lbl, val);
        return card;
    }

    // Weekly bar chart

    /**
      Horizontal canvas bar chart — one bar per week, coloured by volume tier.
      Low / Mid / High thresholds based on the dataset's own percentiles.
     */
    private VBox weeklyBarChart(List<WeeklySummary> summaries) {
        VBox card = card();

        Label heading = new Label("Commits Per Week");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        heading.setTextFill(Color.web(WHITE));

        HBox legend = new HBox(20);
        legend.getChildren().addAll(
                legendItem("High Volume", BAR_HIGH),
                legendItem("Mid Volume",  BAR_MID),
                legendItem("Low Volume",  BAR_LOW)
        );
        card.getChildren().addAll(heading, legend);

        if (summaries.isEmpty()) { card.getChildren().add(emptyLabel()); return card; }

        int    n     = summaries.size();
        double padL  = 110;   // left margin for week labels
        double padR  = 60;    // right margin for commit count labels
        double padT  = 20;
        double padB  = 36;
        double cw    = Math.max(750, n * 52 + padL + padR);
        double ch    = padT + n * 38 + padB;
        double plotW = cw - padL - padR;

        int maxC = summaries.stream().mapToInt(WeeklySummary::getTotalCommits).max().orElse(1);

        // Percentile thresholds for colour coding
        List<Integer> sorted = summaries.stream()
                .map(WeeklySummary::getTotalCommits)
                .sorted()
                .toList();
        int p33 = sorted.get(Math.max(0, (int)(sorted.size() * 0.33)));
        int p66 = sorted.get(Math.max(0, (int)(sorted.size() * 0.66)));

        Canvas canvas = new Canvas(cw, ch);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(NAVY));
        gc.fillRect(0, 0, cw, ch);

        // Vertical grid lines + X axis labels
        int gridSteps = 5;
        for (int g = 0; g <= gridSteps; g++) {
            double x   = padL + g * plotW / gridSteps;
            int    val = (int) Math.round(maxC * g / (double) gridSteps);

            gc.setStroke(g == 0 ? Color.web(WHITE + "44") : Color.web(BORDER));
            gc.setLineWidth(g == 0 ? 1.5 : 1);
            if (g > 0) gc.setLineDashes(5, 5);
            gc.strokeLine(x, padT, x, padT + n * 38);
            gc.setLineDashes();

            gc.setFill(Color.web(WHITE + "66"));
            gc.setFont(Font.font("Segoe UI", 10));
            String lbl = String.valueOf(val);
            gc.fillText(lbl, x - lbl.length() * 3.0, padT + n * 38 + 18);
        }

        // Horizontal axis line
        gc.setStroke(Color.web(WHITE + "44")); gc.setLineWidth(1.5);
        gc.strokeLine(padL, padT + n * 38, cw - padR, padT + n * 38);

        // Bars
        double barH = 24;
        for (int i = 0; i < n; i++) {
            WeeklySummary ws = summaries.get(i);
            int count = ws.getTotalCommits();
            double barW = count * plotW / maxC;
            double y    = padT + i * 38 + (38 - barH) / 2.0;

            // Colour tier
            String hex = count >= p66 ? BAR_HIGH : (count >= p33 ? BAR_MID : BAR_LOW);

            // Track (background)
            gc.setFill(Color.web(hex + "18"));
            gc.fillRoundRect(padL, y, plotW, barH, 6, 6);

            // Gradient fill
            if (barW > 0) {
                LinearGradient grad = new LinearGradient(
                        padL, 0, padL + barW, 0, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web(hex + "77")),
                        new Stop(1, Color.web(hex))
                );
                gc.setFill(grad);
                gc.fillRoundRect(padL, y, barW, barH, 6, 6);

                // Bright right edge
                gc.setStroke(Color.web(hex)); gc.setLineWidth(1.5);
                gc.strokeLine(padL + barW - 1, y + 3, padL + barW - 1, y + barH - 3);
            }

            // Week start label (left)
            gc.setFill(Color.web(WHITE + "AA"));
            gc.setFont(Font.font("Segoe UI", 10));
            String weekLbl = ws.getWeekStart() != null
                    ? ws.getWeekStart().format(WEEK_FMT) : "—";
            gc.fillText(weekLbl, 4, y + barH / 2.0 + 4);

            // Commit count (right of bar)
            gc.setFill(Color.web(hex));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            String cnt = String.valueOf(count);
            gc.fillText(cnt, padL + barW + 6, y + barH / 2.0 + 4);

            // Separator between rows
            if (i < n - 1) {
                gc.setStroke(Color.web(BORDER)); gc.setLineWidth(0.5);
                gc.strokeLine(0, padT + (i + 1) * 38, cw, padT + (i + 1) * 38);
            }
        }

        // X-axis title
        gc.setFill(Color.web(WHITE + "44"));
        gc.setFont(Font.font("Segoe UI", 10));
        gc.fillText("Number of Commits", cw / 2.0 - 50, ch - 6);

        // Scrollable wrapper — fix height to show ~10 rows at a time
        ScrollPane sp = new ScrollPane(canvas);
        sp.setFitToWidth(false);
        sp.setPrefHeight(Math.min(ch + 20, 400));
        sp.setStyle("-fx-background:" + NAVY + ";-fx-background-color:" + NAVY + ";");
        card.getChildren().add(sp);

        return card;
    }

    // Per-week AI card

    /**
      Renders a single week's card.
      If {@code aiText} is null the card shows a loading spinner;
      once called with real text it displays the full breakdown.
     */
    private VBox weeklyAiCard(WeeklySummary ws, String aiText) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18, 22, 18, 22));
        card.setStyle(
                "-fx-background-color:" + SURFACE + ";-fx-border-color:" + BORDER + ";" +
                        "-fx-border-width:1;-fx-border-radius:14;-fx-background-radius:14;"
        );

        // ── Card header row
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        // Week range chip
        String weekStr = ws.getWeekStart() != null
                ? "Week of " + ws.getWeekStart().format(WEEK_FMT)
                : "Unknown Week";
        // Compute week-end for display
        String rangeStr = ws.getWeekStart() != null
                ? ws.getWeekStart().format(WEEK_FMT) + " – "
                + ws.getWeekStart().plusDays(6).format(WEEK_FMT)
                : "—";

        Label weekLbl = new Label("📅  " + rangeStr);
        weekLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        weekLbl.setTextFill(Color.web(CYAN));
        weekLbl.setStyle(
                "-fx-background-color:" + CYAN + "18;" +
                        "-fx-border-color:" + CYAN + "44;-fx-border-width:1;" +
                        "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:4 12 4 12;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Commit count badge
        String countColor = commitCountColor(ws.getTotalCommits());
        Label countBadge = new Label("📝  " + ws.getTotalCommits() + " commits");
        countBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        countBadge.setTextFill(Color.web(countColor));
        countBadge.setStyle(
                "-fx-background-color:" + countColor + "22;" +
                        "-fx-border-color:" + countColor + "55;-fx-border-width:1;" +
                        "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:4 12 4 12;"
        );

        // Top author badge
        Label authorBadge = new Label("👑  " + nvl(ws.getTopAuthor()));
        authorBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        authorBadge.setTextFill(Color.web(GOLD));
        authorBadge.setStyle(
                "-fx-background-color:" + GOLD + "22;" +
                        "-fx-border-color:" + GOLD + "44;-fx-border-width:1;" +
                        "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:4 12 4 12;"
        );

        header.getChildren().addAll(weekLbl, spacer, countBadge, authorBadge);

        // ── Inline mini-bar for commit volume
        VBox volumeRow = buildVolumeBar(ws.getTotalCommits(), countColor);

        // ── AI summary section
        VBox aiSection = new VBox(8);

        if (aiText == null) {
            // Loading state
            HBox loadingRow = new HBox(10);
            loadingRow.setAlignment(Pos.CENTER_LEFT);

            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setMaxSize(18, 18);
            spinner.setStyle("-fx-accent:" + CYAN + ";");

            Label loadingLbl = new Label("Generating AI summary…");
            loadingLbl.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 12));
            loadingLbl.setTextFill(Color.web(WHITE + "44"));

            loadingRow.getChildren().addAll(spinner, loadingLbl);
            aiSection.getChildren().add(loadingRow);

        } else if (aiText.isBlank() || aiText.startsWith("Summary unavailable")
                || aiText.startsWith("Rate limit")) {
            // Graceful degradation
            Label errLbl = new Label("⚠  " + (aiText.isBlank() ? "No summary available." : aiText));
            errLbl.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 12));
            errLbl.setTextFill(Color.web(ORANGE));
            errLbl.setWrapText(true);
            aiSection.getChildren().add(errLbl);

        } else {
            // Real AI content
            Label aiLabel = new Label("🤖  AI Analysis");
            aiLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            aiLabel.setTextFill(Color.web(WHITE + "55"));

            Label aiContent = new Label(aiText);
            aiContent.setFont(Font.font("Segoe UI", 13));
            aiContent.setTextFill(Color.web(WHITE + "CC"));
            aiContent.setWrapText(true);
            aiContent.setLineSpacing(3);

            aiSection.getChildren().addAll(aiLabel, aiContent);
        }

        // Divider
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:" + BORDER + ";");

        card.getChildren().addAll(header, volumeRow, sep, aiSection);
        return card;
    }

    // Inline volume bar

    private VBox buildVolumeBar(int commits, String color) {
        VBox box = new VBox(4);

        Label label = new Label("Volume");
        label.setFont(Font.font("Segoe UI", 10));
        label.setTextFill(Color.web(WHITE + "44"));

        // We use 50 commits as "full bar" reference — anything above caps at 100%
        double fraction = Math.min(commits / 50.0, 1.0);

        StackPane track = new StackPane();
        track.setPrefHeight(7);
        track.setMaxWidth(Double.MAX_VALUE);
        track.setStyle("-fx-background-color:" + BORDER + ";-fx-background-radius:4;");

        Region fill = new Region();
        fill.setPrefHeight(7);
        fill.setStyle("-fx-background-color:" + color + ";-fx-background-radius:4;");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);

        track.widthProperty().addListener((obs, old, w) ->
                fill.setPrefWidth(w.doubleValue() * fraction));

        box.getChildren().addAll(label, track);
        return box;
    }

    // Helpers

    // Colour based on commit count (raw, not percentile — good for consistent colouring).
    private String commitCountColor(int count) {
        if (count >= 20) return GREEN;
        if (count >= 8)  return ORANGE;
        return BAR_LOW;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        l.setTextFill(Color.web(WHITE));
        return l;
    }

    private HBox legendItem(String label, String color) {
        Region dot = new Region();
        dot.setPrefSize(10, 10);
        dot.setStyle("-fx-background-color:" + color + ";-fx-background-radius:3;");
        Label l = new Label(label);
        l.setFont(Font.font("Segoe UI", 12));
        l.setTextFill(Color.web(WHITE + "88"));
        HBox box = new HBox(6, dot, l);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox emptyState() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));

        Label icon = new Label("📭");
        icon.setFont(Font.font("Segoe UI", 48));

        Label msg = new Label("No weekly activity data available.");
        msg.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        msg.setTextFill(Color.web(WHITE + "44"));

        Label hint = new Label(
                "This may mean the repository has no commits, or the data could not be loaded.");
        hint.setFont(Font.font("Segoe UI", 13));
        hint.setTextFill(Color.web(WHITE + "28"));
        hint.setWrapText(true);
        hint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        box.getChildren().addAll(icon, msg, hint);
        return box;
    }

    private Label emptyLabel() {
        Label l = new Label("No data available.");
        l.setFont(Font.font("Segoe UI", 13));
        l.setTextFill(Color.web(WHITE + "44"));
        return l;
    }

    private VBox card() {
        VBox c = new VBox(14);
        c.setPadding(new Insets(22, 24, 22, 24));
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

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}