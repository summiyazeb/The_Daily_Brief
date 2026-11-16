package com.gitpulse.model;

import java.time.LocalDate;
import java.util.List;

public class WeeklyActivity {
    // Declaring attributes
    private LocalDate weekStart;
    private List<Commit> commits;

    // Constructor
    public WeeklyActivity(LocalDate weekStart, List<Commit> commits) {
        this.weekStart = weekStart;
        this.commits = commits;
    }

    // Getters
    public LocalDate getWeekStart() { return weekStart; }
    public List<Commit> getCommits() { return commits; }
}