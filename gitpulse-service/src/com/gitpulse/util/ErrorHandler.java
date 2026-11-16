package com.gitpulse.util;

public class ErrorHandler {
    // Handling all possible errors
    public static String handle(int statusCode) {
        switch (statusCode) {
            case 202: return "GitHub is computing stats. Try again in a few seconds";
            case 401: return "Invalid token! Check your GitHub token";
            case 403: return "Rate limit exceeded! Try again later";
            case 404: return "Repository not found! Check owner and repo name";
            case 500: return "GitHub server error! Try again later";
            default:  return "Unexpected error! HTTP code: " + statusCode;
        }
    }
    // Helper method to print error message
    public static void log(String source, String message) {
        System.err.println("ERROR " + source + ": " + message);
    }
}
