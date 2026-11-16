package com.gitpulse.ai;

import com.gitpulse.model.WeeklySummary;
public class WeeklySummaryPromptGenerator implements PromptGenerator {
    private final WeeklySummary summary;

    // Constructor
    public WeeklySummaryPromptGenerator(WeeklySummary summary) {
        this.summary = summary;
    }

    @Override
    public String generatePrompt() {
        return """
            Analyze this week's Git activity:
            - Week: %s
            - Total commits: %d
            - Top contributor: %s
            - Give a concise summary and description of commits in that week and what happened.
            - Don't use * or #. Give me formatted reply using headings and points. For points 
            use roman numbers and for heading use normal numbers.
            """.formatted(
                summary.getWeekStart(),
                summary.getTotalCommits(),
                summary.getTopAuthor()
        );
    }
}