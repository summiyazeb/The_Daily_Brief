package com.gitpulse.parser;

import com.gitpulse.model.Contributor;
import com.gitpulse.model.Repository;
import com.gitpulse.util.ErrorHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ContributorsParser extends BaseJsonParser {
    @Override
    protected void process(String json, Repository repository) {
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();

        for (JsonElement element : arr) {
            JsonObject obj = element.getAsJsonObject();

            // Get contributor info
            String username = obj.get("login").getAsString();
            String avatarUrl = obj.get("avatar_url").getAsString();
            int totalCommits = obj.get("contributions").getAsInt();

            // Add contributor to repository
            repository.addContributor(new Contributor(username, avatarUrl, totalCommits));
        }
    }
    @Override
    protected String getParserName() {
        return "ContributorsParser";
    }
}