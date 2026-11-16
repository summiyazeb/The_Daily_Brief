package com.gitpulse.parser;

import com.gitpulse.model.Repository;
import com.gitpulse.util.ErrorHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LastCommitDateParser extends BaseJsonParser {

    @Override
    protected void process(String json, Repository repository) {
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        if (array.size() == 0) return;

        JsonObject obj = array.get(0).getAsJsonObject();
        JsonObject commit = obj.getAsJsonObject("commit");
        JsonObject author = commit.getAsJsonObject("author");

        repository.addLastCommitDate(author.get("date").getAsString());
    }

    @Override
    protected String getParserName() {
        return "LastCommitDateParser";
    }
}