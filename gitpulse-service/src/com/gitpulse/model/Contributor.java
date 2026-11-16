package com.gitpulse.model;

public class Contributor {
    // Declaring attributes to store the data
    private final String username;
    private final String avatarUrl;
    private final int totalCommits;
    private double commitShare; // Calculated by the formula of the algorithm

    // Constructor
    public Contributor(String username, String avatarUrl, int totalCommits) {
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.totalCommits = totalCommits;
    }

    // Setter
    public void setCommitShare(double commitShare) { this.commitShare = commitShare; }

    // Getters
    public String getUsername() { return username; }
    public String getAvatarUrl() { return avatarUrl; }
    public int getTotalCommits() { return totalCommits; }
    public double getCommitShare() { return commitShare; }

    // toString method to view the Contributor details
    @Override
    public String toString() {
        return "Contributor{" + "username='" + username + '\'' + ", totalCommits=" + totalCommits +
                ", commitShare=" + commitShare + "%" + '}';
    }
}
