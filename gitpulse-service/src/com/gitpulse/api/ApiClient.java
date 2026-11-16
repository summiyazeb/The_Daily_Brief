package com.gitpulse.api;

public abstract class ApiClient {
    // Declaring Attributes
    protected final String token;
    protected final String baseUrl;

    // Constructor
    public ApiClient(String token, String baseUrl) {
        this.token = token;
        this.baseUrl = baseUrl;
    }

    // Abstract methods
    public abstract String fetchRepositoryInfo(String owner, String repo) throws Exception;
    public abstract String fetchCommits(String owner, String repo) throws Exception;
    public abstract String fetchContributors(String owner, String repo) throws Exception;
    public abstract String fetchReadme(String owner, String repo) throws Exception;
    public abstract String fetchCommitsByAuthor(String owner, String repo, String author) throws Exception;
    public abstract String fetchLastCommit(String owner, String repo) throws Exception;

    // Concrete method to check if request is successful
    protected boolean isValidResponse(int statusCode) {
        return statusCode == 200;
    }
}
