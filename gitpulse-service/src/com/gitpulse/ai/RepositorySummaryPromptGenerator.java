package com.gitpulse.ai;

import com.gitpulse.model.Contributor;
import com.gitpulse.model.Repository;

public class RepositorySummaryPromptGenerator implements PromptGenerator {
    private final Repository repository;

    // Constructor
    public RepositorySummaryPromptGenerator(Repository repository) {
        this.repository = repository;
    }
    // Defining method to generate prompt
    @Override
    public String generatePrompt() {
        StringBuilder prompt = new StringBuilder();
        // Giving prompt
        prompt.append("Analyze this GitHub repository and give a short professional summary:\n\n");

        // Basic repo info
        prompt.append("Repository: " + repository.getOwner() + "/" + repository.getName() + "\n");
        prompt.append("Description: " + repository.getDescription() + "\n");
        prompt.append("Language: " + repository.getLanguage() + "\n");
        prompt.append("Total Commits: " + repository.getCommits().size() + "\n");
        prompt.append("Total Contributors: " + repository.getContributors().size() + "\n\n");

        // Top contributors
        prompt.append("Contributors:\n");
        for (Contributor c : repository.getContributors()) {
            prompt.append("- " + c.getUsername() +
                    ": " + c.getTotalCommits() + " commits\n");
        }

        // Instructions
        prompt.append("\nBased on this data please provide:\n");
        prompt.append("1. A short overall summary of the repository\n");
        prompt.append("2. Team activity assessment\n");
        prompt.append("3. Any concerns about contributor balance\n");
        prompt.append("Keep the response concise and professional.");
        prompt.append("Make sure that you give your answer in a well formated manner by using headings " +
                "and points by using roman numbers. Also avoid * and #, for Headings use numbers and for" +
                "points use romans");
        return prompt.toString();
    }
}
