package com.gitpulse.Algorithm;

import com.gitpulse.model.Repository;
import java.util.List;

/*
   REPOSITORY ANALYZER  ← Main entry point for the entire algorithm package

  This is the single class the rest of the application needs to call.
  It orchestrates all five sub-analyzers and returns one clean RepositoryReport.
  The report contains EVERYTHING:
    - Ranked contributors + top + inactive
    - Monthly activity breakdown
    - Most active least active months
    - Activity trend (GROWING / DECLINING / STABLE / SPORADIC / INACTIVE)
    - All unique insights not available on GitHub
    - A plain-English one-paragraph summary
 */
public class RepositoryAnalyzer {

    // Sub-analyzers — each does one job
    private final ContributorAnalyzer     contributorAnalyzer     = new ContributorAnalyzer();
    private final ActivityAnalyzer        activityAnalyzer        = new ActivityAnalyzer();
    private final TrendAnalyzer           trendAnalyzer           = new TrendAnalyzer();
    private final UniqueInsightsAnalyzer  uniqueInsightsAnalyzer  = new UniqueInsightsAnalyzer();
    private final PlainEnglishSummaryBuilder summaryBuilder        = new PlainEnglishSummaryBuilder();

    /*
      Runs all analyses on a Repository and returns a fully populated RepositoryReport.
     */
    public RepositoryReport analyze(Repository repository) {
        RepositoryReport report = new RepositoryReport();

        // Contributor Analysis
        List<ContributorScore> ranked = contributorAnalyzer.analyze(repository);
        report.setRankedContributors(ranked);
        report.setTopContributors(contributorAnalyzer.getTopContributors(ranked));
        report.setInactiveContributors(contributorAnalyzer.getInactiveContributors(ranked));

        // Monthly Activity Breakdown
        List<MonthlyStats> monthly = activityAnalyzer.buildMonthlyBreakdown(repository);
        report.setMonthlyBreakdown(monthly);
        report.setMostActiveMonths(activityAnalyzer.getMostActiveMonths(monthly, 3));
        report.setLeastActiveMonths(activityAnalyzer.getLeastActiveMonths(monthly, 3));

        // Activity Trend
        report.setActivityTrend(trendAnalyzer.detect(monthly));

        // Unique Insights
        var commits = repository.getCommits();
        report.setBusyHourOfDay(uniqueInsightsAnalyzer.getBusiestHourOfDay(commits));
        report.setBusyDayOfWeek(uniqueInsightsAnalyzer.getBusiestDayOfWeek(commits));
        report.setAvgTimeBetweenCommits(uniqueInsightsAnalyzer.getAvgTimeBetweenCommits(commits));
        report.setLongestGapDays(uniqueInsightsAnalyzer.getLongestGapDays(commits));
        report.setDominantContributor(uniqueInsightsAnalyzer.getDominantContributor(repository));
        report.setBusDriver(uniqueInsightsAnalyzer.isBusDriverRisk(repository));
        double collabIndex = uniqueInsightsAnalyzer.getCollaborationIndex(repository);
        report.setCollaborationIndex(collabIndex);
        report.setCollaborationLabel(uniqueInsightsAnalyzer.getCollaborationLabel(collabIndex));
        report.setPeakStreakDays(uniqueInsightsAnalyzer.getPeakStreakDays(commits));
        report.setProjectHealthLabel(uniqueInsightsAnalyzer.getProjectHealthLabel(repository));

        // Plain-English Summary
        report.setPlainEnglishSummary(summaryBuilder.build(repository, report));

        return report;
    }
}
