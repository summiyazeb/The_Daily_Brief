package com.gitpulse.ai;


import com.gitpulse.util.Constants;
import com.gitpulse.util.ErrorHandler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AiSummaryService {
    // Gemini 2.5 Flash lite model URL
    public static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=";
    // Method to get Summary
    public String getSummary(PromptGenerator generator) {
        try {
            String prompt = generator.generatePrompt();
            return callGeminiApi(prompt);
        } catch (Exception e) {
            if (e.getMessage().contains("429")) {
                ErrorHandler.log("AiSummaryService", "Rate limit reached! Waiting 15 seconds");
                try {
                    Thread.sleep(15000);
                    // Try one more time after waiting
                    return callGeminiApi(generator.generatePrompt());
                } catch (Exception retryException) {
                    ErrorHandler.log("AiSummaryService", retryException.getMessage());
                    return "Summary unavailable! Rate limit exceeded.";
                }
            }
            ErrorHandler.log("AiSummaryService", e.getMessage());
            return "Summary unavailable at this time.";
        }
    }

    // Method to call Gemini API
    private String callGeminiApi(String prompt) throws Exception {
        URL url = new URL(GEMINI_URL + Constants.GEMINI_API_KEY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Setup request
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(Constants.TIMEOUT);
        connection.setReadTimeout(Constants.TIMEOUT);

        // Build request body
        String requestBody = "{" + "\"contents\": [{" + "\"parts\": [{"
                + "\"text\": \"" + escapeJson(prompt) + "\"" + "}]" + "}]" + "}";

        // Send request
        OutputStream os = connection.getOutputStream();
        os.write(requestBody.getBytes());
        os.close();

        // Check response
        int statusCode = connection.getResponseCode();
        if (statusCode != 200) {
            ErrorHandler.log("AiSummaryService", "HTTP error: " + statusCode);
            throw new Exception("Gemini API error: " + statusCode);
        }

        // Read response
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return extractTextFromResponse(response.toString());
    }

    // Extract the actual text from Gemini JSON response
    private String extractTextFromResponse(String json) {
        try {
            com.google.gson.JsonObject root = com.google.gson.JsonParser
                    .parseString(json)
                    .getAsJsonObject();

            return root
                    .getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

        } catch (Exception e) {
            ErrorHandler.log("AiSummaryService", "Failed to parse response: " + e.getMessage());
            return "Could not parse Gemini response.";
        }
    }

    // Escape special characters in prompt for JSON
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
