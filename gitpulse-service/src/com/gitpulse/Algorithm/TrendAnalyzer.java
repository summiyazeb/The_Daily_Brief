package com.gitpulse.Algorithm;

import java.util.List;

/*
  This checks if the projecting is growing , inactive etc
  like in our repo
  ALGORITHM — Linear Regression Slope + Volatility Check:
  Step 1 – Collect monthly commit counts as a simple number sequence.
  Step 2 – Run linear regression to get the SLOPE.
  Step 3 – Calculate VOLATILITY (standard deviation of monthly counts).
  Step 4 – If the last 3 months are all zero → INACTIVE.
  Step 5 – If fewer than 3 months of data exist → INSUFFICIENT_DATA.
 */
public class TrendAnalyzer {

    private static final double SLOPE_THRESHOLD = 0.5;
    private static final double SPORADIC_FACTOR = 1.5;
    private static final int    INACTIVE_MONTHS = 3;

    // Minimum months needed for a statistically meaningful trend
    private static final int MIN_MONTHS_FOR_TREND = 3;

    /*
      Detects the trend from a chronological list of MonthlyStats.
      @param monthlyStats ordered oldest → newest
     */
    public ActivityTrend detect(List<MonthlyStats> monthlyStats) {
        if (monthlyStats == null || monthlyStats.isEmpty()) {
            return new ActivityTrend(ActivityTrend.TrendType.INACTIVE, 0, 0,
                    "No commit history found. The project has not started or data is unavailable.");
        }

        int n = monthlyStats.size();
        double[] counts = new double[n];
        for (int i = 0; i < n; i++) {
            counts[i] = monthlyStats.get(i).getCommitCount();
        }

        // Inactive check (takes priority)
        if (isInactive(counts)) {
            return new ActivityTrend(ActivityTrend.TrendType.INACTIVE, 0, stdDev(counts),
                    "This project appears to be inactive — no commits have been made recently.");
        }

        // Insufficient data check
        // A single month (or two) cannot produce a meaningful trend. Linear regression
        // on 1 point always returns slope = 0, falsely reporting "STABLE".
        if (n < MIN_MONTHS_FOR_TREND) {
            return new ActivityTrend(ActivityTrend.TrendType.STABLE, 0, stdDev(counts),
                    "Not enough history to determine a trend — only " + n + " "
                            + (n == 1 ? "month" : "months") + " of data available. Check back after more activity.");
        }

        double slope      = linearRegressionSlope(counts);
        double volatility = stdDev(counts);
        double mean       = mean(counts);

        // Sporadic check
        if (mean > 0 && volatility > SPORADIC_FACTOR * mean && Math.abs(slope) < SLOPE_THRESHOLD) {
            return new ActivityTrend(ActivityTrend.TrendType.SPORADIC, slope, volatility,
                    "Development happens in bursts — busy for a while, then quiet for a while.");
        }

        // ── Step 2: Growth / Decline / Stable
        if (slope >= SLOPE_THRESHOLD) {
            return new ActivityTrend(ActivityTrend.TrendType.GROWING, slope, volatility,
                    String.format("The project is growing — roughly %.0f more commits per month over time.", slope));
        }
        if (slope <= -SLOPE_THRESHOLD) {
            return new ActivityTrend(ActivityTrend.TrendType.DECLINING, slope, volatility,
                    String.format("Activity is slowing down — about %.0f fewer commits per month recently.", Math.abs(slope)));
        }
        return new ActivityTrend(ActivityTrend.TrendType.STABLE, slope, volatility,
                "Development is steady and consistent. The team maintains a reliable work pace.");
    }

    // Math helpers

    private double linearRegressionSlope(double[] y) {
        int n = y.length;
        if (n < 2) return 0;

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX  += i;
            sumY  += y[i];
            sumXY += i * y[i];
            sumX2 += (double) i * i;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) return 0;
        return (n * sumXY - sumX * sumY) / denominator;
    }

    private double mean(double[] y) {
        if (y.length == 0) return 0;
        double sum = 0;
        for (double v : y) sum += v;
        return sum / y.length;
    }

    private double stdDev(double[] y) {
        if (y.length == 0) return 0;
        double m = mean(y);
        double variance = 0;
        for (double v : y) variance += (v - m) * (v - m);
        return Math.sqrt(variance / y.length);
    }

    private boolean isInactive(double[] counts) {
        if (counts.length < INACTIVE_MONTHS) return false;
        for (int i = counts.length - INACTIVE_MONTHS; i < counts.length; i++) {
            if (counts[i] > 0) return false;
        }
        return true;
    }
}
