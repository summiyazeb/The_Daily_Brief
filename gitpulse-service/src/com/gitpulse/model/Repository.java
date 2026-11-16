package com.gitpulse.model;

import java.util.Map;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Repository {
    // Declaring attributes
    private final String owner;
    private final String name;
    private String description;
    private String language;
    private String readme;
    private String lastCommitDate;
    private String repositorySummary;
    private String readmeSummary;
    private List<WeeklySummary> weeklySummaries;
    private List<Commit> commits;           // Storing all the commits of the repository
    private List<Contributor> contributors; // Storing all the Contributors of the repository
    private Map<LocalDate, List<Commit>> weeklyActivity; // Storing weeklyActivity in groupwise format

    // Constructor
    public Repository(String owner, String name) {
        this.owner = owner;
        this.name = name;
        this.commits = new ArrayList<>();
        this.contributors = new ArrayList<>();
    }

    // Setters for repository data
    public void addCommit(Commit c) { commits.add(c); }
    public void addContributor(Contributor c) { contributors.add(c); }
    public void addReadme(String readme) { this.readme = readme; }
    public void addLastCommitDate(String date) { this.lastCommitDate = date; }
    public void setWeeklyActivity(Map<LocalDate, List<Commit>> data) { this.weeklyActivity = data; }

    // Setters for fields fetched from API
    public void addDescription(String description) { this.description = description; }
    public void addLanguage(String language) { this.language = language; }

    // Setters to store summary returned from Gemini API
    public void setRepositorySummary(String summary) { this.repositorySummary = summary; }
    public void setReadmeSummary(String summary) { this.readmeSummary = summary; }
    public void setWeeklySummaries(List<WeeklySummary> summaries) { this.weeklySummaries = summaries; }

    // Getters for all fields
    public String getOwner() { return owner; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLanguage() { return language; }
    public String getReadme() { return readme; }
    public String getLastCommitDate() { return lastCommitDate; }
    public String getRepositorySummary() { return repositorySummary; }
    public String getReadmeSummary() { return readmeSummary; }
    public List<WeeklySummary> getWeeklySummaries() { return weeklySummaries; }
    public List<Commit> getCommits() { return commits; }
    public List<Contributor> getContributors() { return contributors; }
    public Map<LocalDate, List<Commit>> getWeeklyActivity() { return weeklyActivity; }

    // toString method to view the Repository details
    @Override
    public String toString() {
        return "Repository{" + "owner='" + owner + '\'' + ", name='" + name + '\'' +
                ", language='" + language + '\'' + ", commits=" + commits.size() +
                ", contributors=" + contributors.size() + '}';
    }
}
