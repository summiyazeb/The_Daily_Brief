package com.gitpulse.service;

import com.gitpulse.model.Commit;
import com.gitpulse.model.WeeklySummary;
import java.time.LocalDate;
import java.util.*;

public class WeeklySummaryService {
    // Method to generate weekly summaries from grouped commit data
    public List<WeeklySummary> generate(Map<LocalDate, List<Commit>> weeklyMap) {

        List<WeeklySummary> summaries = new ArrayList<>();

        if (weeklyMap == null || weeklyMap.isEmpty()) return summaries;

        for (var entry : weeklyMap.entrySet()) {
            LocalDate week = entry.getKey();
            List<Commit> commits = entry.getValue();
            int total = commits.size(); // Count total commits

            // Find top author
            Map<String, Integer> countMap = new HashMap<>();

            // Count commits per author
            for (Commit c : commits) {
                countMap.put(c.getAuthorName(),
                        countMap.getOrDefault(c.getAuthorName(), 0) + 1);
            }

            // Find top author
            String topAuthor = countMap.entrySet().stream().max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse("Unknown");

            // Add summary
            summaries.add(new WeeklySummary(week, total, topAuthor));
        }

        return summaries;
    }
}