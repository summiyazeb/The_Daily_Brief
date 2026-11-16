package com.gitpulse.api;

import com.gitpulse.util.Constants;
import com.gitpulse.util.ErrorHandler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GitHubApiClient extends ApiClient {
    // Constructor
    public GitHubApiClient() {
        super(Constants.TOKEN, Constants.BASE_URL);
    }

    // Method to make HTTP request
    private String get(String endpoint) throws Exception {
        // Building a URL for unique repository
        URL url = new URL(baseUrl + endpoint);
        // Setting up the connection to the repository through url
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Sending request to the connection
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "token " + token);
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        connection.setRequestProperty("User-Agent", "GitPulse-App");
        connection.setConnectTimeout(Constants.TIMEOUT);
        connection.setReadTimeout(Constants.TIMEOUT);

        // Storing the response code from the connection
        int responseCode = connection.getResponseCode();

        // Handling invalid responses
        if(!isValidResponse(responseCode)) {
            handleError(responseCode);
        }

        // Reading the response from the connection and returning it to display
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    // Error handling
    private void handleError(int statusCode) throws Exception {
        String message = ErrorHandler.handle(statusCode);
        ErrorHandler.log("GitHubApiClient", message);
        if (statusCode == 403) {
            // Rate limit — wait 10 seconds and throw so caller can retry
            try { Thread.sleep(10000); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        throw new Exception(message);
    }

    // Fetch all commits
    @Override
    public String fetchCommits(String owner, String repo) throws Exception {
        StringBuilder allCommits = new StringBuilder("[");
        int page = 1;
        boolean firstPage = true;

        while (true) {
            String endpoint = "/repos/" + owner + "/" + repo +
                    "/commits?per_page=100&page=" + page;
            String json = get(endpoint);

            // Remove outer brackets from each page
            String trimmed = json.trim();
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();

            // Stop if page is empty
            if (trimmed.isEmpty()) break;

            if (!firstPage) allCommits.append(",");
            allCommits.append(trimmed);

            firstPage = false;
            page++;
        }

        allCommits.append("]");
        return allCommits.toString();
    }

    // Implementing abstract methods from ApiClient
    @Override
    public String fetchRepositoryInfo(String owner, String repository) throws Exception {
        return get("/repos/" + owner + "/" + repository);
    }

    @Override
    public String fetchContributors(String owner, String repository) throws Exception {
        return get("/repos/" + owner + "/" + repository + "/contributors");
    }

    @Override
    public String fetchReadme(String owner, String repository) throws Exception {
        return get("/repos/" + owner + "/" + repository + "/readme");
    }

    @Override
    public String fetchCommitsByAuthor(String owner, String repo, String author) throws Exception {
        return get("/repos/" + owner + "/" + repo + "/commits?author=" + author + "&per_page=1");
    }

    @Override
    public String fetchLastCommit(String owner, String repo) throws Exception {
        return get("/repos/" + owner + "/" + repo + "/commits?per_page=1");
    }
}
