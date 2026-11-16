package com.gitpulse.parser;

import com.gitpulse.model.Repository;
import com.gitpulse.util.ErrorHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ReadmeParser extends BaseJsonParser {
    @Override
    protected void process(String json, Repository repository) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String encodedContent = obj.get("content").getAsString().replace("\n", "");
        byte[] decodedBytes = java.util.Base64.getDecoder().decode(encodedContent);

        repository.addReadme(new String(decodedBytes));
    }
    @Override
    protected String getParserName() {
        return "ReadmeParser";
    }
}