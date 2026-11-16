package com.gitpulse.model;

public class Commit {
    // Declaring attributes of the Commits to store its data
    private final String sha;
    private final String authorName;
    private final String authorEmail;
    private final String date;
    private final String message;

    // Constructor
    public Commit (String sha, String authorName, String authorEmail,
                   String date, String message) {
        this.sha = sha;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.date = date;
        this.message = message;
    }

    // Getters
    public String getSha() { return sha; }
    public String getAuthorName() { return authorName; }
    public String getAuthorEmail() { return authorEmail; }
    public String getDate() { return date; }
    public String getMessage() { return message; }

    // toString method to view the Commit details
    @Override
    public String toString() {
        return "Commit{" + "sha='" + sha + '\'' + ", author='" + authorName + '\'' +
                ", date='" + date + '\'' + ", message='" + message + '\'' + '}';
    }
}
