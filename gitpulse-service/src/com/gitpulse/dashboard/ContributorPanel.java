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

public class ContributorPanel {

    private static final String NAVY    = "#0D1117";
    private static final String CYAN    = "#00D4FF";
    private static final String WHITE   = "#E6EDF3";
    private static final String SURFACE = "#161B22";
    private static final String BORDER  = "#21262D";

    private static final String[] PALETTE = {
            "#00D4FF", "#3FB950", "#F0883E", "#BC8CFF",
            "#FF6B6B", "#FFD700", "#56D364", "#79C0FF"
    };

    private final RepositoryReport report;

    public ContributorPanel(RepositoryReport report) { this.report = report; }

    public ScrollPane build() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(36, 36, 36, 36));
        List<ContributorScore> ranked = report.getRankedContributors();
        content.getChildren().addAll(
                pageHeader(ranked),
                commitShareBarChart(ranked),
                scoreGroupedBarChart(ranked),
                rankedTable(ranked)
        );
        return styledScroll(content);
    }

    private VBox pageHeader(List<ContributorScore> ranked) {
        Label title = new Label("Contributor Analysis");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web(WHITE));
        int total = ranked == null ? 0 : ranked.size();
        Label sub = new Label(total + " contributor(s) analysed  ·  scores out of 100");
        sub.setFont(Font.font("Segoe UI", 13));
        sub.setTextFill(Color.web(WHITE + "55"));
        return new VBox(6, title, sub);
    }

    // Commit share and vertical bar chart with full axes
    private VBox commitShareBarChart(List<ContributorScore> ranked) {
        VBox card = card();
        Label heading = new Label("Commit Share per Contributor  (%)");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        heading.setTextFill(Color.web(WHITE));
        card.getChildren().add(heading);

        if (ranked == null || ranked.isEmpty()) { card.getChildren().add(emptyLabel()); return card; }

        int    n     = ranked.size();
        double padL  = 58, padR = 24, padT = 24, padB = 72;
        double cw    = Math.max(600, n * 110 + padL + padR);
        double ch    = 300;
        double plotW = cw - padL - padR;
        double plotH = ch - padT - padB;
        double barW  = Math.min(60, plotW / n - 16);
        int    gridN = 5;

        Canvas canvas = new Canvas(cw, ch);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(NAVY)); gc.fillRect(0, 0, cw, ch);

        // Grid + Y labels (0–100%)
        for (int g = 0; g <= gridN; g++) {
            double y   = padT + plotH - g * plotH / gridN;
            int    val = g * 100 / gridN;
            if (g > 0) {
                gc.setStroke(Color.web(BORDER)); gc.setLineWidth(1);
                gc.setLineDashes(5, 5);
                gc.strokeLine(padL, y, cw - padR, y);
                gc.setLineDashes();
            }
            gc.setFill(Color.web(WHITE + "77"));
            gc.setFont(Font.font("Segoe UI", 11));
            String lbl = val + "%";
            gc.fillText(lbl, padL - 8 - lbl.length() * 6.5, y + 4);
        }

        // Axes
        gc.setStroke(Color.web(WHITE + "44")); gc.setLineWidth(1.5);
        gc.strokeLine(padL, padT, padL, padT + plotH);
        gc.strokeLine(padL, padT + plotH, cw - padR, padT + plotH);

        // Bars
        for (int i = 0; i < n; i++) {
            ContributorScore cs = ranked.get(i);
            String hex          = PALETTE[i % PALETTE.length];
            double slotW        = plotW / n;
            double x            = padL + i * slotW + (slotW - barW) / 2.0;
            double barH         = cs.getCommitShare() / 100.0 * plotH;
            double y            = padT + plotH - barH;

            LinearGradient grad = new LinearGradient(
                    0, y + barH, 0, y, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web(hex + "55")),
                    new Stop(1, Color.web(hex))
            );
            gc.setFill(grad);
            gc.fillRoundRect(x, y, barW, barH, 7, 7);

            // Top highlight
            gc.setStroke(Color.web(hex)); gc.setLineWidth(1.5);
            gc.strokeLine(x + 3, y + 1, x + barW - 3, y + 1);

            // Value above bar
            String val = String.format("%.1f%%", cs.getCommitShare());
            gc.setFill(Color.web(WHITE));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            gc.fillText(val, x + barW / 2.0 - val.length() * 3.5, y - 7);

            // Tick + rotated label
            gc.setStroke(Color.web(WHITE + "33")); gc.setLineWidth(1);
            gc.strokeLine(x + barW / 2.0, padT + plotH, x + barW / 2.0, padT + plotH + 5);
            gc.save();
            gc.translate(x + barW / 2.0, padT + plotH + 16);
            gc.rotate(-30);
            gc.setFill(Color.web(hex));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            gc.fillText(cs.getUsername(), 0, 0);
            gc.restore();
        }

        // Axis titles
        gc.save();
        gc.translate(13, padT + plotH / 2.0); gc.rotate(-90);
        gc.setFill(Color.web(WHITE + "55")); gc.setFont(Font.font("Segoe UI", 11));
        gc.fillText("Share %", -26, 0); gc.restore();
        gc.setFill(Color.web(WHITE + "55")); gc.setFont(Font.font("Segoe UI", 11));
        gc.fillText("Contributor", cw / 2.0 - 36, ch - 4);

        card.getChildren().add(scrollableCanvas(canvas, ch));
        return card;
    }

    // Score breakdown — grouped horizontal bar chart
    private VBox scoreGroupedBarChart(List<ContributorScore> ranked) {
        VBox card = card();
        Label heading = new Label("Score Breakdown per Contributor  (0–100)");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        heading.setTextFill(Color.web(WHITE));

        HBox legend = new HBox(20);
        legend.getChildren().addAll(
                legendItem("Consistency", "#BC8CFF"),
                legendItem("Recency",     "#F0883E"),
                legendItem("Overall",     "#00D4FF")
        );
        card.getChildren().addAll(heading, legend);

        if (ranked == null || ranked.isEmpty()) { card.getChildren().add(emptyLabel()); return card; }

        int    n      = ranked.size();
        double padL   = 170, padR = 60, padT = 20, padB = 40;
        double cw     = 800;
        double ch     = padT + n * 58 + padB;
        double plotW  = cw - padL - padR;
        double plotH  = ch - padT - padB;
        double groupH = plotH / n;
        double barH   = 12;
        int    gridN  = 5;

        Canvas canvas = new Canvas(cw, ch);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(NAVY)); gc.fillRect(0, 0, cw, ch);

        // Vertical grid lines + X labels (0,20,40,60,80,100)
        for (int g = 0; g <= gridN; g++) {
            double x   = padL + g * plotW / gridN;
            int    val = g * 100 / gridN;
            gc.setStroke(g == 0 ? Color.web(WHITE + "44") : Color.web(BORDER));
            gc.setLineWidth(g == 0 ? 1.5 : 1);
            if (g > 0) gc.setLineDashes(5, 5);
            gc.strokeLine(x, padT, x, padT + plotH);
            gc.setLineDashes();
            gc.setFill(Color.web(WHITE + "77"));
            gc.setFont(Font.font("Segoe UI", 11));
            gc.fillText(String.valueOf(val), x - 8, padT + plotH + 18);
        }

        // X axis
        gc.setStroke(Color.web(WHITE + "44")); gc.setLineWidth(1.5);
        gc.strokeLine(padL, padT + plotH, cw - padR, padT + plotH);

        // Y axis
        gc.strokeLine(padL, padT, padL, padT + plotH);

        // Grouped bars per contributor
        String[] scoreColors = {"#BC8CFF", "#F0883E", "#00D4FF"};

        for (int i = 0; i < n; i++) {
            ContributorScore cs = ranked.get(i);
            double[] scores = {cs.getConsistencyScore(), cs.getRecencyScore(), cs.getOverallScore()};
            double groupY   = padT + i * groupH;

            // Contributor name (left y-axis label)
            gc.setFill(Color.web(PALETTE[i % PALETTE.length]));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            gc.fillText(cs.getUsername(), 4, groupY + groupH / 2.0 + 4);

            // Separator line
            if (i > 0) {
                gc.setStroke(Color.web(BORDER)); gc.setLineWidth(1);
                gc.strokeLine(padL, groupY, cw - padR, groupY);
            }

            // Three bars
            for (int j = 0; j < 3; j++) {
                double barY = groupY + (groupH - 3 * barH - 6) / 2.0 + j * (barH + 3);
                double barW = scores[j] / 100.0 * plotW;

                // Track
                gc.setFill(Color.web(scoreColors[j] + "20"));
                gc.fillRoundRect(padL, barY, plotW, barH, 4, 4);

                // Fill with gradient
                if (barW > 0) {
                    LinearGradient grad = new LinearGradient(
                            padL, 0, padL + barW, 0, false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.web(scoreColors[j] + "88")),
                            new Stop(1, Color.web(scoreColors[j]))
                    );
                    gc.setFill(grad);
                    gc.fillRoundRect(padL, barY, barW, barH, 4, 4);
                }

                // Score label at end of bar
                String val = String.format("%.0f", scores[j]);
                gc.setFill(Color.web(scoreColors[j]));
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
                gc.fillText(val, padL + barW + 5, barY + barH - 1);
            }
        }

        // X-axis title
        gc.setFill(Color.web(WHITE + "55")); gc.setFont(Font.font("Segoe UI", 11));
        gc.fillText("Score (0–100)", cw / 2.0 - 40, ch - 4);

        card.getChildren().add(scrollableCanvas(canvas, ch));
        return card;
    }

    // Ranked table
    private VBox rankedTable(List<ContributorScore> ranked) {
        VBox card = card();
        Label heading = new Label("Full Ranked Table");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        heading.setTextFill(Color.web(WHITE));
        card.getChildren().add(heading);

        if (ranked == null || ranked.isEmpty()) { card.getChildren().add(emptyLabel()); return card; }

        card.getChildren().add(tableRow(
                "#", "Username", "Commits", "Share", "Consist.", "Recency", "Score", "Label",
                WHITE + "55", true
        ));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:" + BORDER + ";");
        card.getChildren().add(sep);

        for (int i = 0; i < ranked.size(); i++) {
            ContributorScore cs = ranked.get(i);
            card.getChildren().add(tableRow(
                    String.valueOf(i + 1),
                    cs.getUsername(),
                    String.valueOf(cs.getTotalCommits()),
                    String.format("%.1f%%", cs.getCommitShare()),
                    String.format("%.0f", cs.getConsistencyScore()),
                    String.format("%.0f", cs.getRecencyScore()),
                    String.format("%.1f", cs.getOverallScore()),
                    cs.getActivityLabel(),
                    labelColor(cs.getActivityLabel()), false
            ));
        }
        return card;
    }

    private HBox tableRow(String rank, String name, String commits, String share,
                          String consist, String recency, String score, String label,
                          String color, boolean isHeader) {
        HBox row = new HBox();
        row.setPadding(new Insets(7, 0, 7, 0));
        FontWeight fw = isHeader ? FontWeight.BOLD : FontWeight.NORMAL;
        row.getChildren().addAll(
                col(rank, 40, color, fw), col(name, 185, color, fw),
                col(commits, 72, color, fw), col(share, 72, color, fw),
                col(consist, 82, color, fw), col(recency, 82, color, fw),
                col(score, 72, color, fw),
                col("[" + label + "]", 155, labelColor(label), fw)
        );
        return row;
    }

    private Label col(String text, double width, String color, FontWeight fw) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", fw, 13));
        l.setTextFill(Color.web(color));
        l.setPrefWidth(width);
        return l;
    }

    // Helpers

    private ScrollPane scrollableCanvas(Canvas c, double h) {
        ScrollPane sp = new ScrollPane(c);
        sp.setFitToHeight(true); sp.setPrefHeight(h + 20);
        sp.setStyle("-fx-background:" + NAVY + ";-fx-background-color:" + NAVY + ";");
        return sp;
    }

    private HBox legendItem(String label, String color) {
        Region dot = new Region(); dot.setPrefSize(10, 10);
        dot.setStyle("-fx-background-color:" + color + ";-fx-background-radius:3;");
        Label l = new Label(label); l.setFont(Font.font("Segoe UI", 12));
        l.setTextFill(Color.web(WHITE + "88"));
        HBox box = new HBox(6, dot, l); box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private String labelColor(String label) {
        if (label == null) return WHITE;
        return switch (label) {
            case "Top Contributor" -> "#FFD700";
            case "Active"          -> "#3FB950";
            case "Occasional"      -> "#F0883E";
            case "Inactive"        -> "#FF6B6B";
            default -> WHITE;
        };
    }

    private Label emptyLabel() {
        Label l = new Label("No contributor data available.");
        l.setFont(Font.font("Segoe UI", 13)); l.setTextFill(Color.web(WHITE + "44"));
        return l;
    }

    private VBox card() {
        VBox c = new VBox(12); c.setPadding(new Insets(22, 24, 22, 24));
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
