package com.gitpulse.Algorithm;

import com.gitpulse.model.Repository;
import java.util.List;

/**
  PLAIN ENGLISH SUMMARY BUILDER:

  Takes all the computed insights and stitches them into a single paragraph
  that a non-technical person can read (This was one of the main purpose of
  this project to make things easier for project managers)and understand immediately.
 */
public class PlainEnglishSummaryBuilder {

    public String build(Repository repository, RepositoryReport report) {
        StringBuilder sb = new StringBuilder();

        // Opening
        sb.append("\"").append(repository.getName()).append("\"");
        if (repository.getDescription() != null && !repository.getDescription().isBlank()) {
            sb.append(" — ").append(repository.getDescription().trim());
        }
        sb.append(". ");

        // Contributors
        List<ContributorScore> top      = report.getTopContributors();
        List<ContributorScore> inactive = report.getInactiveContributors();

        if (top != null && !top.isEmpty()) {
            sb.append("The project has ")
                    .append(plural(report.getRankedContributors().size(), "contributor"))
                    .append(" in total. ");

            if (top.size() == 1) {
                sb.append(top.get(0).getUsername())
                        .append(" is the standout contributor, responsible for about ")
                        .append(String.format("%.0f%%", top.get(0).getCommitShare()))
                        .append(" of all code changes. ");
            } else {
                sb.append("The top contributors are ");
                int limit = Math.min(top.size(), 3);
                for (int i = 0; i < limit; i++) {
                    if (i > 0) sb.append(i == limit - 1 ? " and " : ", ");
                    sb.append(top.get(i).getUsername());
                }
                sb.append(". ");
            }
        }

        // Bus driver(It means a contributor is a driver if more than 50% commits are
        // made by him)risk
        if (report.isBusDriver()) {
            sb.append("⚠ Heads up: ")
                    .append(report.getDominantContributor())
                    .append(" has made over half of all commits — if they step away, ")
                    .append("the project could slow down significantly. ");
        }

        //  Inactive contributors
        // relative to the repo's last commit, not today.
        if (inactive != null && !inactive.isEmpty()) {
            String lastActive = repository.getLastCommitDate() != null
                    ? " as of " + formatDate(repository.getLastCommitDate())
                    : "";
            sb.append(plural(inactive.size(), "contributor"))
                    .append(inactive.size() == 1 ? " had" : " had")
                    .append(" not made any commits in the final phase of the project")
                    .append(lastActive)
                    .append(". ");
        }

        // Collaboration
        sb.append("The teamwork style is classified as \"")
                .append(report.getCollaborationLabel())
                .append("\" (score: ")
                .append(String.format("%.0f", report.getCollaborationIndex()))
                .append("/100). ");

        // Activity trend
        if (report.getActivityTrend() != null) {
            sb.append(report.getActivityTrend().getPlainEnglish()).append(" ");
        }

        // Most active month
        List<MonthlyStats> mostActive   = report.getMostActiveMonths();
        List<MonthlyStats> leastActive  = report.getLeastActiveMonths();

        if (mostActive != null && !mostActive.isEmpty()) {
            sb.append("The busiest period was ")
                    .append(mostActive.get(0).getLabel())
                    .append(" with ")
                    .append(plural(mostActive.get(0).getCommitCount(), "commit"))
                    .append(". ");
        }

        // FIX #4: only emit "quietest period" if it is actually different from
        // the busiest period (prevents "quietest: Nov 2025 with 75 commits").
        if (leastActive != null && !leastActive.isEmpty()
                && mostActive != null && !mostActive.isEmpty()
                && !leastActive.get(0).getLabel().equals(mostActive.get(0).getLabel())) {
            sb.append("The quietest period was ")
                    .append(leastActive.get(0).getLabel())
                    .append(" with only ")
                    .append(plural(leastActive.get(0).getCommitCount(), "commit"))
                    .append(". ");
        }

        // Timing insights
        if (report.getBusyDayOfWeek() != null && !report.getBusyDayOfWeek().equals("Unknown")) {
            sb.append("Most commits tend to happen on ").append(report.getBusyDayOfWeek());
            int hour = (int) report.getBusyHourOfDay();
            if (hour >= 0) sb.append(" around ").append(formatHour(hour));
            sb.append(" (UTC). ");
        }

        // Commit gap
        if (report.getLongestGapDays() > 0) {
            sb.append("The longest stretch with no commits was ")
                    .append(plural(report.getLongestGapDays(), "day"))
                    .append(". ");
        }

        // Streak
        if (report.getPeakStreakDays() > 1) {
            sb.append("The team's longest daily commit streak was ")
                    .append(plural(report.getPeakStreakDays(), "consecutive day"))
                    .append(" — showing a period of intense, focused work. ");
        }

        // ── Health label
        String health = report.getProjectHealthLabel();
        if (health != null && !health.equals("Unknown")) {
            sb.append("Overall, the project is currently rated as: ").append(health).append(".");
        } else {
            sb.append("Overall health could not be determined (last commit date unavailable).");
        }

        return sb.toString();
    }

    // ── Helpers
    private String plural(int n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    /** Converts 0–23 hour to friendly format. */
    private String formatHour(int hour) {
        if (hour == 0)  return "midnight";
        if (hour == 12) return "noon";
        if (hour < 12)  return hour + " AM";
        return (hour - 12) + " PM";
    }

    /**
     Converts an ISO-8601 date string like "2025-11-30T18:57:55Z" to "November 2025".
     */
    private String formatDate(String isoDate) {
        try {
            java.time.Instant instant = java.time.Instant.parse(isoDate);
            java.time.YearMonth ym = java.time.YearMonth.from(
                    instant.atZone(java.time.ZoneId.of("UTC")));
            return ym.getMonth().getDisplayName(
                    java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                    + " " + ym.getYear();
        } catch (Exception e) {
            return isoDate; // fallback to raw string
        }
    }
}
