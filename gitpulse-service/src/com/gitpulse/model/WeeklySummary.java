package com.gitpulse.model;

import java.time.LocalDate;

public class WeeklySummary {
    // Declaring attributes
    private LocalDate weekStart;
    private int totalCommits;
    private String topAuthor;
    private String summaryText;

    // Constructor
    public WeeklySummary(LocalDate weekStart, int totalCommits, String topAuthor) {
        this.weekStart = weekStart;
        this.totalCommits = totalCommits;
        this.topAuthor = topAuthor;
    }

    // Setter
    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    // Getters
    public LocalDate getWeekStart() { return weekStart; }
    public int getTotalCommits() { return totalCommits; }
    public String getTopAuthor() { return topAuthor; }
    public String getSummaryText() { return summaryText; }
}