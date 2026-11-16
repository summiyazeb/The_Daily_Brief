package com.gitpulse.Algorithm;


  // this class holds the computed score and rank for a single contributor.
 
public class ContributorScore {

    private final String username;
    private final int totalCommits;
    private final double commitShare;        // % of all commits this person made
    private final double consistencyScore;   // how regularly they commit (0–100)
    private final double recencyScore;       // how recently they were active (0–100)
    private final double overallScore;       // weighted final score (0–100)
    private final String activityLabel;      // "Top Contributor", "Active", "Occasional", "Inactive"
    public ContributorScore(String username, int totalCommits, double commitShare,
                            double consistencyScore, double recencyScore,
                            double overallScore, String activityLabel) {
        this.username         = username;
        this.totalCommits     = totalCommits;
        this.commitShare      = commitShare;
        this.consistencyScore = consistencyScore;
        this.recencyScore     = recencyScore;
        this.overallScore     = overallScore;
        this.activityLabel    = activityLabel;
    }

    public String getUsername()         { return username; }
    public int    getTotalCommits()     { return totalCommits; }
    public double getCommitShare()      { return commitShare; }
    public double getConsistencyScore() { return consistencyScore; }
    public double getRecencyScore()     { return recencyScore; }
    public double getOverallScore()     { return overallScore; }
    public String getActivityLabel()    { return activityLabel; }

    @Override
    public String toString() {
        return String.format(
                "%-20s | Commits: %4d | Share: %5.1f%% | Consistency: %5.1f | Recency: %5.1f | Score: %5.1f | [%s]",
                username, totalCommits, commitShare, consistencyScore, recencyScore, overallScore, activityLabel
        );
    }
}
