package com.gitpulse.Algorithm;

/**
 Represents the overall activity trend of the repository over time.
 English meaning of each trend:
 GROWING   – the project is getting more commits over time (team is picking up speed)
 DECLINING – fewer commits lately (project may be winding down or team is busy)
 STABLE    – consistent commit pace (healthy, predictable workflow)
 SPORADIC  – commits happen in bursts with long gaps (irregular development)
 INACTIVE  – no commits in a long time (project appears abandoned or on hold)
 */
public class ActivityTrend {

    public enum TrendType {
        GROWING, DECLINING, STABLE, SPORADIC, INACTIVE
    }

    private final TrendType trendType;
    private final double    slopePerMonth;   // positive = growing, negative = declining
    private final double    volatility;      // standard deviation of monthly commits (high = sporadic)
    private final String    plainEnglish;    

    public ActivityTrend(TrendType trendType, double slopePerMonth,
                         double volatility, String plainEnglish) {
        this.trendType    = trendType;
        this.slopePerMonth = slopePerMonth;
        this.volatility   = volatility;
        this.plainEnglish = plainEnglish;
    }

    public TrendType getTrendType()     { return trendType; }
    public double    getSlopePerMonth() { return slopePerMonth; }
    public double    getVolatility()    { return volatility; }
    public String    getPlainEnglish()  { return plainEnglish; }

    @Override
    public String toString() {
        return String.format("Trend: %-10s | Slope: %+.2f commits/month | Volatility: %.2f | %s",
                trendType, slopePerMonth, volatility, plainEnglish);
    }
}
