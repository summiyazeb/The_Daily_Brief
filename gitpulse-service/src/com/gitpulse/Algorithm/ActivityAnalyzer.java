package com.gitpulse.Algorithm;

import com.gitpulse.model.Commit;
import com.gitpulse.model.Repository;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/*
  ACTIVITY ANALYZER
 It Answers: "When was this project most alive, and when did it go quiet?"
 */
public class ActivityAnalyzer {

    public List<MonthlyStats> buildMonthlyBreakdown(Repository repository) {
        List<Commit> commits = repository.getCommits();
        if (commits == null || commits.isEmpty()) return Collections.emptyList();

        Map<YearMonth, List<Commit>> grouped = new TreeMap<>();
        for (Commit commit : commits) {
            try {
                Instant instant = Instant.parse(commit.getDate());
                YearMonth ym    = YearMonth.from(instant.atZone(ZoneId.of("UTC")));
                grouped.computeIfAbsent(ym, k -> new ArrayList<>()).add(commit);
            } catch (Exception ignored) {}
        }

        List<MonthlyStats> result = new ArrayList<>();
        for (Map.Entry<YearMonth, List<Commit>> entry : grouped.entrySet()) {
            YearMonth    ym           = entry.getKey();
            List<Commit> monthCommits = entry.getValue();

            int    commitCount        = monthCommits.size();
            int    uniqueContributors = countUniqueAuthors(monthCommits);
            double avgCommitsPerWeek  = commitCount / weeksInMonth(ym);

            result.add(new MonthlyStats(
                    ym.getYear(), ym.getMonth(),
                    commitCount, uniqueContributors, avgCommitsPerWeek
            ));
        }

        tagExtremes(result);
        return result;
    }

    /** Returns the top N most active months, sorted busiest first. */
    public List<MonthlyStats> getMostActiveMonths(List<MonthlyStats> breakdown, int topN) {
        return breakdown.stream()
                .sorted((a, b) -> Integer.compare(b.getCommitCount(), a.getCommitCount()))
                .limit(topN)
                .collect(Collectors.toList());
    }
    /*
     Returns the top N leas active months, sorted quietest first.
     */
    public List<MonthlyStats> getLeastActiveMonths(List<MonthlyStats> breakdown, int topN) {
        // Only show a least-active list if there are at least 2 distinct months.
        if (breakdown.size() < 2) return Collections.emptyList();

        return breakdown.stream()
                .filter(m -> m.getCommitCount() > 0)
                .filter(m -> !m.getActivityTag().equals("Most Active")) // FIX #4
                .sorted(Comparator.comparingInt(MonthlyStats::getCommitCount))
                .limit(topN)
                .collect(Collectors.toList());
    }



    private int countUniqueAuthors(List<Commit> commits) {
        Set<String> authors = new HashSet<>();
        for (Commit c : commits) {
            if (c.getAuthorName() != null) authors.add(c.getAuthorName());
        }
        return authors.size();
    }

    private double weeksInMonth(YearMonth ym) {
        return ym.lengthOfMonth() / 7.0;
    }

    private void tagExtremes(List<MonthlyStats> stats) {
        if (stats.isEmpty()) return;

        stats.stream()
                .sorted((a, b) -> Integer.compare(b.getCommitCount(), a.getCommitCount()))
                .limit(3)
                .forEach(m -> m.setActivityTag("Most Active"));

        stats.stream()
                .filter(m -> m.getActivityTag().equals("Normal"))
                .filter(m -> m.getCommitCount() > 0)
                .sorted(Comparator.comparingInt(MonthlyStats::getCommitCount))
                .limit(3)
                .forEach(m -> m.setActivityTag("Least Active"));
    }
}
