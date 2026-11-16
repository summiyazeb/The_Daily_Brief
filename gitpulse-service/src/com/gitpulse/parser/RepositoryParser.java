package com.gitpulse.parser;

import com.gitpulse.model.Repository;
import com.gitpulse.util.ErrorHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RepositoryParser extends BaseJsonParser {
    @Override
    protected void process(String json, Repository repository) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        if (obj.has("description") && !obj.get("description").isJsonNull()) {
            repository.addDescription(obj.get("description").getAsString());
        }
        if (obj.has("language") && !obj.get("language").isJsonNull()) {
            repository.addLanguage(obj.get("language").getAsString());
        }
    }
    @Override
    protected String getParserName() {
        return "RepositoryInfoParser";
    }
}
