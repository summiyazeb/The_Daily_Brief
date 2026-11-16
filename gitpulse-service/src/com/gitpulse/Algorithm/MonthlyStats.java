package com.gitpulse.Algorithm;

import java.time.Month;

/*
  Stores  commit statistics for a single calendar month.
  Used to find most or least active months and build activity trends.
 */
public class MonthlyStats {

    private final int year;
    private final Month month;
    private final int commitCount;
    private final int uniqueContributors;
    private final double avgCommitsPerWeek;  // commitCount / number of weeks in that month
    private String activityTag;              // "Most Active", "Least Active", "Normal"

    public MonthlyStats(int year, Month month, int commitCount,
                        int uniqueContributors, double avgCommitsPerWeek) {
        this.year                = year;
        this.month               = month;
        this.commitCount         = commitCount;
        this.uniqueContributors  = uniqueContributors;
        this.avgCommitsPerWeek   = avgCommitsPerWeek;
        this.activityTag         = "Normal";
    }

    // Setter
    public void setActivityTag(String tag) { this.activityTag = tag; }
    // Getters
    public int    getYear()                { return year; }
    public Month  getMonth()               { return month; }
    public int    getCommitCount()         { return commitCount; }
    public int    getUniqueContributors()  { return uniqueContributors; }
    public double getAvgCommitsPerWeek()   { return avgCommitsPerWeek; }
    public String getActivityTag()         { return activityTag; }

    /** Human-readable label, e.g. "January 2024" */
    public String getLabel() {
        return month.name().charAt(0) + month.name().substring(1).toLowerCase() + " " + year;
    }

    @Override
    public String toString() {
        return String.format(
                "%-15s | Commits: %4d | Contributors: %3d | Avg/Week: %5.1f | [%s]",
                getLabel(), commitCount, uniqueContributors, avgCommitsPerWeek, activityTag
        );
    }
}