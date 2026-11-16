package com.gitpulse.parser;

import java.time.*;
import java.util.*;
import com.gitpulse.model.Repository;
import com.gitpulse.model.Commit;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CommitStatsParser extends BaseJsonParser {

    @Override
    protected void process(String json, Repository repository) {
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();

        // TreeMap keeps keys sorted
        Map<LocalDate, List<Commit>> weeklyMap = new TreeMap<>();

        for (JsonElement element : array) {
            // Extreat
            JsonObject obj = element.getAsJsonObject();
            String sha = obj.get("sha").getAsString();
            JsonObject commitObj = obj.getAsJsonObject("commit");
            String message = commitObj.get("message").getAsString();
            JsonObject authorObj = commitObj.getAsJsonObject("author");
            String authorName = authorObj.get("name").getAsString();
            String authorEmail = authorObj.get("email").getAsString();
            String dateStr = authorObj.get("date").getAsString();

            // Convert string timestamp to Instant
            Instant instant = Instant.parse(dateStr);

            // Convert timestamp to LocalDate in UTC
            LocalDate weekStart = instant
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDate()
                    .with(DayOfWeek.MONDAY);  // Normalize date to the Monday of that week

            // Group commits by weekStart date:
            weeklyMap
                    .computeIfAbsent(weekStart, k -> new ArrayList<>())
                    .add(new Commit(sha, authorName, authorEmail, dateStr, message));
        }
        // Store computed weekly grouping inside repository object
        repository.setWeeklyActivity(weeklyMap);
    }

    @Override
    protected String getParserName() {
        return "CommitStatsParser";
    }
}