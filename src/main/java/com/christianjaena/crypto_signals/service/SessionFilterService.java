package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class SessionFilterService {

    private final Clock clock;

    // Trading session windows (UTC)
    private static final int LONDON_OPEN = 8;   // 8:00 UTC
    private static final int LONDON_CLOSE = 16; // 16:00 UTC
    private static final int NY_OPEN = 13;       // 13:00 UTC
    private static final int NY_CLOSE = 22;      // 22:00 UTC
    private static final int ASIA_OPEN = 23;     // 23:00 UTC (previous day)
    private static final int ASIA_CLOSE = 7;     // 7:00 UTC

    // High-impact news event times (simplified - in real implementation, would use news API)
    private static final int[] NEWS_HOURS = {13, 14, 15, 19, 20}; // FOMC, CPI, etc.

    public SessionFilterService() {
        this.clock = Clock.systemUTC();
    }

    public SessionFilterService(Clock clock) {
        this.clock = clock;
    }

    public boolean isInHighLiquiditySession() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("UTC"));
        int hour = now.getHour();

        // Check if in London session
        boolean inLondonSession = hour >= LONDON_OPEN && hour < LONDON_CLOSE;
        
        // Check if in NY session
        boolean inNYSession = hour >= NY_OPEN && hour < NY_CLOSE;
        
        // Check if in Asia session
        boolean inAsiaSession = hour >= ASIA_OPEN || hour < ASIA_CLOSE;

        // High liquidity sessions: London, NY, Asia, or London-NY overlap
        return inLondonSession || inNYSession || inAsiaSession;
    }

    public boolean isInOptimalTradingWindow() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("UTC"));
        int hour = now.getHour();

        // Optimal windows: London-NY overlap (13:00-16:00 UTC)
        return hour >= 13 && hour < 16;
    }

    public boolean hasHighImpactNewsEvent() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("UTC"));
        int hour = now.getHour();
        int dayOfWeek = now.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday

        // Skip weekends
        if (dayOfWeek == 6 || dayOfWeek == 7) { // Saturday or Sunday
            return true; // Consider weekends as "news events" (low liquidity)
        }

        // Check for high-impact news hours
        for (int newsHour : NEWS_HOURS) {
            if (hour == newsHour) {
                return true;
            }
        }

        // Additional check for first Friday of month (NFP)
        if (dayOfWeek == 5 && isFirstFridayOfMonth(now)) {
            return hour >= 13 && hour <= 15; // NFP release window
        }

        return false;
    }

    public boolean shouldSkipSignal(List<CandleData> candles) {
        if (!isInHighLiquiditySession()) {
            return true; // Skip outside high liquidity sessions
        }

        if (hasHighImpactNewsEvent()) {
            return true; // Skip during high-impact news
        }

        // Additional check: skip if candle is from low-volume period
        if (candles != null && !candles.isEmpty()) {
            LocalDateTime candleTime = candles.get(candles.size() - 1).getTimestamp();
            if (isFromLowVolumePeriod(candleTime)) {
                return true;
            }
        }

        return false;
    }

    public int getConfidenceBonus() {
        int bonus = 0;

        if (isInOptimalTradingWindow()) {
            bonus += 3; // Bonus for optimal trading window
        } else if (isInHighLiquiditySession()) {
            bonus += 2; // Smaller bonus for other high liquidity sessions
        }

        if (!hasHighImpactNewsEvent()) {
            bonus += 2; // Bonus for no news events
        }

        return Math.min(bonus, 5); // Cap at 5
    }

    public String getSessionInfo() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("UTC"));
        int hour = now.getHour();
        int dayOfWeek = now.getDayOfWeek().getValue();

        StringBuilder info = new StringBuilder();
        
        // Session information
        if (hour >= LONDON_OPEN && hour < LONDON_CLOSE) {
            info.append("London session");
        } else if (hour >= NY_OPEN && hour < NY_CLOSE) {
            info.append("NY session");
        } else if (hour >= ASIA_OPEN || hour < ASIA_CLOSE) {
            info.append("Asia session");
        } else {
            info.append("Off-session");
        }

        // Optimal window
        if (isInOptimalTradingWindow()) {
            info.append(" (Optimal: London-NY overlap)");
        }

        // News events
        if (hasHighImpactNewsEvent()) {
            info.append(" | High-impact news detected");
        } else {
            info.append(" | No major news events");
        }

        // Day of week
        String[] days = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        info.append(" | ").append(days[dayOfWeek]);

        return info.toString();
    }

    private boolean isFromLowVolumePeriod(LocalDateTime candleTime) {
        ZonedDateTime zonedTime = candleTime.atZone(ZoneId.of("UTC"));
        int hour = zonedTime.getHour();
        int dayOfWeek = zonedTime.getDayOfWeek().getValue();

        // Weekend is low volume
        if (dayOfWeek == 6 || dayOfWeek == 7) {
            return true;
        }

        // Late night/early morning is low volume
        if (hour < 6 || hour > 22) {
            return true;
        }

        return false;
    }

    private boolean isFirstFridayOfMonth(ZonedDateTime date) {
        // Simplified check for first Friday of month
        return date.getDayOfMonth() <= 7 && date.getDayOfWeek().getValue() == 5;
    }

    public boolean isWeekend() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("UTC"));
        int dayOfWeek = now.getDayOfWeek().getValue();
        return dayOfWeek == 6 || dayOfWeek == 7; // Saturday or Sunday
    }

    public String getNextOptimalSession() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("UTC"));
        int hour = now.getHour();

        if (hour < 13) {
            return "Next optimal session: London-NY overlap (13:00-16:00 UTC)";
        } else if (hour < 16) {
            return "Currently in optimal session (London-NY overlap)";
        } else if (hour < 23) {
            return "Next optimal session: Tomorrow 13:00-16:00 UTC";
        } else {
            return "Next optimal session: Today 13:00-16:00 UTC";
        }
    }

    // Advanced session analysis for specific trading pairs
    public boolean isOptimalForPair(String symbol) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("UTC"));
        int hour = now.getHour();

        // Different pairs have different optimal times
        if (symbol != null && symbol.contains("BTC")) {
            // BTC/USDT is most active during NY and London sessions
            return (hour >= NY_OPEN && hour < NY_CLOSE) || 
                   (hour >= LONDON_OPEN && hour < LONDON_CLOSE);
        } else if (symbol != null && symbol.contains("ETH")) {
            // ETH/USDT follows similar pattern to BTC
            return (hour >= NY_OPEN && hour < NY_CLOSE) || 
                   (hour >= LONDON_OPEN && hour < LONDON_CLOSE);
        } else if (symbol != null && (symbol.contains("JPY") || symbol.contains("AUD"))) {
            // Asian pairs are more active during Asian session
            return hour >= ASIA_OPEN || hour < ASIA_CLOSE;
        }

        // Default to general high liquidity session
        return isInHighLiquiditySession();
    }
}
