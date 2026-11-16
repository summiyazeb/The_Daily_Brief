package com.gitpulse.Algorithm;

import com.gitpulse.model.Commit;
import com.gitpulse.model.Repository;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/*
  UNIQUE INSIGHTS ANALYZER
  Computes everything GitHub does NOT show us.
  Insights produced:
   1.  Busiest Hour of Day         – "Most commits happen around 3 PM"
   2.  Busiest Day of Week         – "Tuesday is the most productive day"
   3.  Average Time Between Commits– "On average a new commit appears every 6 hours"
   4.  Longest Commit Gap          – "The longest quiet period was 12 days"
   5.  Dominant Contributor        – person with >50% of commits (bus-factor risk)
   6.  Bus Driver Risk Flag        – true/false: "one person is critical to this project"
   7.  Collaboration Index (0–100) – how evenly the work is distributed
   8.  Peak Commit Streak          – "The longest daily commit streak was 14 days"
   9.  Project Health Label        – "Healthy / At Risk / Stale / Abandoned"
 */

public class UniqueInsightsAnalyzer {

    // Public entry points

    /**
      Returns the hour of day (0–23 UTC) that has the most commits.
      Output English: "Most commits happen around 2 PM."
     */
    public double getBusiestHourOfDay(List<Commit> commits) {
        Map<Integer, Integer> hourCount = new HashMap<>();
        for (Commit c : commits) {
            try {
                int hour = Instant.parse(c.getDate()).atZone(ZoneId.of("UTC")).getHour();
                hourCount.merge(hour, 1, Integer::sum);
            } catch (Exception ignored) {}
        }
        return hourCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);
    }

    /*
      Returns the day of week with the most commits.
      e.g. "Tuesday"
     */
    public String getBusiestDayOfWeek(List<Commit> commits) {
        Map<DayOfWeek, Integer> dayCount = new EnumMap<>(DayOfWeek.class);
        for (Commit c : commits) {
            try {
                DayOfWeek day = Instant.parse(c.getDate()).atZone(ZoneId.of("UTC")).getDayOfWeek();
                dayCount.merge(day, 1, Integer::sum);
            } catch (Exception ignored) {}
        }
        return dayCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> capitalize(e.getKey().name()))
                .orElse("Unknown");
    }

    /*
      Returns the average number of hours between consecutive commits.
     */
    public double getAvgTimeBetweenCommits(List<Commit> commits) {
        if (commits.size() < 2) return 0;

        List<Instant> sorted = commits.stream()
                .map(c -> { try { return Instant.parse(c.getDate()); } catch (Exception e) { return null; } })
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (sorted.size() < 2) return 0;

        long totalHours = 0;
        for (int i = 1; i < sorted.size(); i++) {
            totalHours += ChronoUnit.HOURS.between(sorted.get(i - 1), sorted.get(i));
        }
        return (double) totalHours / (sorted.size() - 1);
    }

    /*
      Returns the longest gap (in days) between two consecutive commits.
     */
    public int getLongestGapDays(List<Commit> commits) {
        List<Instant> sorted = commits.stream()
                .map(c -> { try { return Instant.parse(c.getDate()); } catch (Exception e) { return null; } })
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        int maxGap = 0;
        for (int i = 1; i < sorted.size(); i++) {
            int gap = (int) ChronoUnit.DAYS.between(sorted.get(i - 1), sorted.get(i));
            if (gap > maxGap) maxGap = gap;
        }
        return maxGap;
    }

    /*
      Returns the username of the contributor who made the most commits.
      If that person made >50% of all commits they are flagged as a "bus driver" —
      meaning the project is at risk if this person leaves.
     */
    public String getDominantContributor(Repository repository) {
        List<com.gitpulse.model.Contributor> contributors = repository.getContributors();
        if (contributors == null || contributors.isEmpty()) return "Unknown";

        return contributors.stream()
                .max(Comparator.comparingInt(com.gitpulse.model.Contributor::getTotalCommits))
                .map(com.gitpulse.model.Contributor::getUsername)
                .orElse("Unknown");
    }

    /*
      Bus Driver Risk: returns true if a single contributor made more than 50% of all commits.
      Plain English: "This project depends heavily on one person.
                      If they stop contributing, the project may stall."
     */
    public boolean isBusDriverRisk(Repository repository) {
        List<com.gitpulse.model.Contributor> contributors = repository.getContributors();
        if (contributors == null || contributors.isEmpty()) return false;

        int total = contributors.stream().mapToInt(com.gitpulse.model.Contributor::getTotalCommits).sum();
        if (total == 0) return false;

        int topCommits = contributors.stream()
                .mapToInt(com.gitpulse.model.Contributor::getTotalCommits)
                .max().orElse(0);

        return (topCommits * 100.0 / total) > 50.0;
    }

    /*
    Note:I used Ai to  calculate insights by giving the idea to ai
      COLLABORATION INDEX (0–100)
      Measures how evenly commit work is shared among contributors.

      FORMULA — based on the Gini coefficient (inverted):
        Gini = 0  → perfectly equal (everyone commits the same amount)  → index = 100
        Gini = 1  → completely unequal (one person does everything)      → index = 0
        collaborationIndex = (1 – Gini) × 100
      Labels:
        80–100 → "Well Distributed"   (healthy team)
        50–79  → "Small Team"         (a few key players)
        0–49   → "Solo Project"       (one person dominates)
     */

    public double getCollaborationIndex(Repository repository) {
        List<com.gitpulse.model.Contributor> contributors = repository.getContributors();
        if (contributors == null || contributors.size() <= 1) return 0;

        int[] counts = contributors.stream()
                .mapToInt(com.gitpulse.model.Contributor::getTotalCommits)
                .toArray();

        Arrays.sort(counts);
        int n = counts.length;
        double sumNumerator = 0;
        for (int i = 0; i < n; i++) {
            sumNumerator += (2.0 * (i + 1) - n - 1) * counts[i];
        }
        double total = Arrays.stream(counts).sum();
        if (total == 0) return 0;
        double gini = sumNumerator / (n * total);
        return Math.max(0, Math.min(100, (1 - gini) * 100));
    }

    /* Turns a collaboration index number into a label. */
    public String getCollaborationLabel(double collaborationIndex) {
        if (collaborationIndex >= 80) return "Well Distributed";
        if (collaborationIndex >= 50) return "Small Team";
        return "Solo Project";
    }

    /*
      PEAK COMMIT STREAK
      The longest number of CONSECUTIVE CALENDAR DAYS on which at least one commit was made.
      Plain English: "The team had a 14-day run where they committed every single day."
      This highlights periods of intense focused work — not shown anywhere on GitHub.
     */
    public int getPeakStreakDays(List<Commit> commits) {
        Set<LocalDate> commitDays = new HashSet<>();
        for (Commit c : commits) {
            try {
                LocalDate date = Instant.parse(c.getDate()).atZone(ZoneId.of("UTC")).toLocalDate();
                commitDays.add(date);
            } catch (Exception ignored) {}
        }

        if (commitDays.isEmpty()) return 0;

        List<LocalDate> sorted = commitDays.stream().sorted().collect(Collectors.toList());
        int maxStreak = 1, currentStreak = 1;

        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        return maxStreak;
    }

    /*
      PROJECT HEALTH LABEL
      A single easy-to-understand label based on recency + frequency.
      Rules (checked in order):
        "Healthy"   – committed in last 14 days AND ≥ 10 commits/month average
        "Active"    – committed in last 30 days
        "At Risk"   – last commit was 31–90 days ago
        "Stale"     – last commit was 91–365 days ago
        "Abandoned" – no commit in over a year
     */
    public String getProjectHealthLabel(Repository repository) {
        String lastDateStr = repository.getLastCommitDate();
        if (lastDateStr == null || lastDateStr.isEmpty()) return "Unknown";

        try {
            Instant lastCommit = Instant.parse(lastDateStr);
            long daysSince = ChronoUnit.DAYS.between(lastCommit, Instant.now());

            List<Commit> commits = repository.getCommits();
            double avgMonthly = 0;
            if (commits != null && !commits.isEmpty()) {
                // rough: total commits / months spanned
                Instant earliest = commits.stream()
                        .map(c -> { try { return Instant.parse(c.getDate()); } catch (Exception e) { return null; } })
                        .filter(Objects::nonNull).min(Instant::compareTo).orElse(lastCommit);
                long months = Math.max(1, ChronoUnit.DAYS.between(earliest, lastCommit) / 30);
                avgMonthly = (double) commits.size() / months;
            }

            if (daysSince <= 14 && avgMonthly >= 10) return "Healthy";
            if (daysSince <= 30)  return "Active";
            if (daysSince <= 90)  return "At Risk";
            if (daysSince <= 365) return "Stale";
            return "Abandoned";

        } catch (Exception e) {
            return "Unknown";
        }
    }

    // Utility
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
