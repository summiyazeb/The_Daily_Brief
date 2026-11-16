package com.gitpulse.Algorithm;

import java.util.List;

/*
  The single output object that holds EVERY insight computed by the algorithm package.
 In other classes , I've used this class to extract any type of data I want
 */
public class RepositoryReport {

    // Contributor insights
    private List<ContributorScore> rankedContributors;  // sorted best → worst
    private List<ContributorScore> topContributors;     // top 20 % by score
    private List<ContributorScore> inactiveContributors;// no commit in 30+ days

    //  Activity insights
    private List<MonthlyStats>  monthlyBreakdown;       // one entry per month
    private List<MonthlyStats>  mostActiveMonths;       // top 3 busiest months
    private List<MonthlyStats>  leastActiveMonths;      // top 3 quietest months
    private ActivityTrend       activityTrend;          // overall project trend

    //  Unique / non-GitHub insights(which is the main purpose of our project)
    private double  busyHourOfDay;         // 0-23, hour when most commits happen
    private String  busyDayOfWeek;         // e.g. "Tuesday"
    private double  avgTimeBetweenCommits; // in hours
    private int     longestGapDays;        // longest dry spell with no commits
    private String  dominantContributor;   // person who wrote >50 % of code
    private boolean isBusDriver;           // true if one person holds >50 % of commits
    private double  collaborationIndex;    // 0–100: how evenly spread the work is
    private String  collaborationLabel;    // "Solo Project", "Small Team", "Well Distributed"
    private int     peakStreakDays;        // longest daily commit streak
    private String  projectHealthLabel;    // "Healthy", "At Risk", "Stale", "Abandoned"

    // Plain-English summary
    private String plainEnglishSummary;    // one paragraph, no jargon

    // Setters
    public void setRankedContributors(List<ContributorScore> v)   { rankedContributors = v; }
    public void setTopContributors(List<ContributorScore> v)      { topContributors = v; }
    public void setInactiveContributors(List<ContributorScore> v) { inactiveContributors = v; }
    public void setMonthlyBreakdown(List<MonthlyStats> v)         { monthlyBreakdown = v; }
    public void setMostActiveMonths(List<MonthlyStats> v)         { mostActiveMonths = v; }
    public void setLeastActiveMonths(List<MonthlyStats> v)        { leastActiveMonths = v; }
    public void setActivityTrend(ActivityTrend v)                 { activityTrend = v; }
    public void setBusyHourOfDay(double v)                        { busyHourOfDay = v; }
    public void setBusyDayOfWeek(String v)                        { busyDayOfWeek = v; }
    public void setAvgTimeBetweenCommits(double v)                { avgTimeBetweenCommits = v; }
    public void setLongestGapDays(int v)                          { longestGapDays = v; }
    public void setDominantContributor(String v)                  { dominantContributor = v; }
    public void setBusDriver(boolean v)                           { isBusDriver = v; }
    public void setCollaborationIndex(double v)                   { collaborationIndex = v; }
    public void setCollaborationLabel(String v)                   { collaborationLabel = v; }
    public void setPeakStreakDays(int v)                          { peakStreakDays = v; }
    public void setProjectHealthLabel(String v)                   { projectHealthLabel = v; }
    public void setPlainEnglishSummary(String v)                  { plainEnglishSummary = v; }

    // Getters
    public List<ContributorScore> getRankedContributors()   { return rankedContributors; }
    public List<ContributorScore> getTopContributors()      { return topContributors; }
    public List<ContributorScore> getInactiveContributors() { return inactiveContributors; }
    public List<MonthlyStats>     getMonthlyBreakdown()     { return monthlyBreakdown; }
    public List<MonthlyStats>     getMostActiveMonths()     { return mostActiveMonths; }
    public List<MonthlyStats>     getLeastActiveMonths()    { return leastActiveMonths; }
    public ActivityTrend          getActivityTrend()        { return activityTrend; }
    public double   getBusyHourOfDay()          { return busyHourOfDay; }
    public String   getBusyDayOfWeek()          { return busyDayOfWeek; }
    public double   getAvgTimeBetweenCommits()  { return avgTimeBetweenCommits; }
    public int      getLongestGapDays()         { return longestGapDays; }
    public String   getDominantContributor()    { return dominantContributor; }
    public boolean  isBusDriver()               { return isBusDriver; }
    public double   getCollaborationIndex()     { return collaborationIndex; }
    public String   getCollaborationLabel()     { return collaborationLabel; }
    public int      getPeakStreakDays()         { return peakStreakDays; }
    public String   getProjectHealthLabel()     { return projectHealthLabel; }
    public String   getPlainEnglishSummary()    { return plainEnglishSummary; }
}
