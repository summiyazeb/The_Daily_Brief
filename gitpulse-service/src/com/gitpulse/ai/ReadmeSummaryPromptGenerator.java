package com.gitpulse.ai;

public class ReadmeSummaryPromptGenerator implements PromptGenerator {
    private final String readmeContent;

    // Constructor
    public ReadmeSummaryPromptGenerator(String readmeContent) {
        this.readmeContent = readmeContent;
    }

    // Method to generate prompt
    @Override
    public String generatePrompt() {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Analyze this GitHub README and give a short professional summary:\n\n");
        prompt.append("README Content:\n");
        prompt.append(readmeContent + "\n\n");
        prompt.append("Based on this README please provide:\n");
        prompt.append("1. What this project does in simple terms\n");
        prompt.append("2. Who it is for\n");
        prompt.append("3. Key features mentioned\n");
        prompt.append("Keep the response concise and professional.\n");
        prompt.append("Make sure that you give your answer in a well formated manner by using headings " +
                "and points by using roman numbers. Also avoid * and #, for Headings use numbers and for" +
                "points use romans");

        return prompt.toString();
    }
}
