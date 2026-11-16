package com.gitpulse.parser;

import com.gitpulse.model.Repository;
import com.gitpulse.model.Commit;
import com.gitpulse.util.ErrorHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CommitsParser extends BaseJsonParser {
    @Override
    protected void process(String json, Repository repository) {
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();

        // Extract data using loop
        for (JsonElement element : arr) {
            JsonObject obj = element.getAsJsonObject();
            // Get sha
            String sha = obj.get("sha").getAsString();

            // Get commit
            JsonObject commit = obj.getAsJsonObject("commit");
            String message = commit.get("message").getAsString();

            // Get author info
            JsonObject author = commit.getAsJsonObject("author");
            String name = author.get("name").getAsString();
            String email = author.get("email").getAsString();
            String date = author.get("date").getAsString();

            // Add commit to repository
            repository.addCommit(new Commit(sha, name, email, date, message));
        }
    }
    @Override
    protected String getParserName() {
        return "CommitsParser";
    }
}
