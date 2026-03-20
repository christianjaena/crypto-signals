package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.FibonacciLevel;
import com.christianjaena.crypto_signals.model.Signal;
import com.christianjaena.crypto_signals.model.Trend;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FibonacciService {

    private static final double[] RETRACEMENT_LEVELS = {0.236, 0.382, 0.5, 0.618, 0.786};
    private static final double[] EXTENSION_LEVELS = {1.272, 1.618, 2.0, 2.618};
    private static final double[] PROJECTION_LEVELS = {0.618, 1.0, 1.618, 2.618};

    public List<FibonacciLevel> calculateRetracementLevels(List<CandleData> candles, Trend trend) {
        List<FibonacciLevel> levels = new ArrayList<>();
        
        if (candles == null || candles.size() < 10) {
            return levels;
        }

        double high = findSwingHigh(candles);
        double low = findSwingLow(candles);
        
        if (high <= low) {
            return levels;
        }

        for (double level : RETRACEMENT_LEVELS) {
            double retracementPrice = calculateRetracementPrice(high, low, level, trend);
            FibonacciLevel.LevelStrength strength = determineLevelStrength(level, candles);
            
            levels.add(FibonacciLevel.createRetracement(level, retracementPrice, strength));
        }

        return levels;
    }

    public List<FibonacciLevel> calculateExtensionLevels(List<CandleData> candles, Trend trend) {
        List<FibonacciLevel> levels = new ArrayList<>();
        
        if (candles == null || candles.size() < 20) {
            return levels;
        }

        double swingHigh = findSwingHigh(candles);
        double swingLow = findSwingLow(candles);
        double recentHigh = findRecentHigh(candles);
        double recentLow = findRecentLow(candles);

        for (double level : EXTENSION_LEVELS) {
            double extensionPrice = calculateExtensionPrice(swingHigh, swingLow, recentHigh, recentLow, level, trend);
            FibonacciLevel.LevelStrength strength = FibonacciLevel.LevelStrength.MODERATE;
            
            levels.add(FibonacciLevel.createExtension(level, extensionPrice, strength));
        }

        return levels;
    }

    public FibonacciLevel findNearestSupport(List<CandleData> candles, double currentPrice, Signal signal) {
        if (signal != Signal.BUY || candles == null || candles.size() < 10) {
            return null;
        }

        List<FibonacciLevel> retracementLevels = calculateRetracementLevels(candles, Trend.BULLISH);
        
        FibonacciLevel nearestSupport = null;
        double minDistance = Double.MAX_VALUE;
        final double tolerancePercent = 2.0; // 2% tolerance

        for (FibonacciLevel level : retracementLevels) {
            // Look for support levels below current price within tolerance
            if (level.getPrice() < currentPrice && level.isNearPrice(currentPrice, tolerancePercent)) {
                double distance = level.getDistanceFromPrice(currentPrice);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestSupport = level;
                }
            }
        }

        return nearestSupport;
    }

    public FibonacciLevel findNearestResistance(List<CandleData> candles, double currentPrice, Signal signal) {
        if (signal != Signal.SELL || candles == null || candles.size() < 10) {
            return null;
        }

        List<FibonacciLevel> retracementLevels = calculateRetracementLevels(candles, Trend.BEARISH);
        
        FibonacciLevel nearestResistance = null;
        double minDistance = Double.MAX_VALUE;
        final double tolerancePercent = 2.0; // 2% tolerance

        for (FibonacciLevel level : retracementLevels) {
            // Look for resistance levels above current price within tolerance
            if (level.getPrice() > currentPrice && level.isNearPrice(currentPrice, tolerancePercent)) {
                double distance = level.getDistanceFromPrice(currentPrice);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestResistance = level;
                }
            }
        }

        return nearestResistance;
    }

    public boolean isTradeNearKeyLevel(List<CandleData> candles, double currentPrice, Signal signal) {
        FibonacciLevel nearestLevel = (signal == Signal.BUY) ? 
            findNearestSupport(candles, currentPrice, signal) : 
            findNearestResistance(candles, currentPrice, signal);

        return nearestLevel != null && nearestLevel.isKeyLevel() && nearestLevel.isStrongLevel();
    }

    public int getConfidenceBonus(List<CandleData> candles, double currentPrice, Signal signal) {
        FibonacciLevel nearestLevel = (signal == Signal.BUY) ? 
            findNearestSupport(candles, currentPrice, signal) : 
            findNearestResistance(candles, currentPrice, signal);

        if (nearestLevel == null) {
            return 0; // No nearby Fibonacci levels
        }

        // Base bonus for being near any Fibonacci level
        int bonus = 5;
        
        // Additional bonus for key levels (38.2%, 50%, 61.8%)
        if (nearestLevel.isKeyLevel()) {
            bonus += 5;
        }
        
        // Additional bonus for strong levels
        if (nearestLevel.isStrongLevel()) {
            bonus += 5;
        }

        return Math.min(bonus, 10); // Cap at 10 per PLAN.md confidence scoring
    }

    private double findSwingHigh(List<CandleData> candles) {
        double maxHigh = Double.MIN_VALUE;
        // Look at the entire candle history for swing high
        for (CandleData candle : candles) {
            maxHigh = Math.max(maxHigh, candle.getHigh());
        }
        
        return maxHigh;
    }

    private double findSwingLow(List<CandleData> candles) {
        double minLow = Double.MAX_VALUE;
        // Look at the entire candle history for swing low
        for (CandleData candle : candles) {
            minLow = Math.min(minLow, candle.getLow());
        }
        
        return minLow;
    }

    private double findRecentHigh(List<CandleData> candles) {
        double maxHigh = Double.MIN_VALUE;
        int lookback = Math.min(10, candles.size() / 4);
        
        for (int i = candles.size() - lookback; i < candles.size(); i++) {
            maxHigh = Math.max(maxHigh, candles.get(i).getHigh());
        }
        
        return maxHigh;
    }

    private double findRecentLow(List<CandleData> candles) {
        double minLow = Double.MAX_VALUE;
        int lookback = Math.min(10, candles.size() / 4);
        
        for (int i = candles.size() - lookback; i < candles.size(); i++) {
            minLow = Math.min(minLow, candles.get(i).getLow());
        }
        
        return minLow;
    }

    private double calculateRetracementPrice(double high, double low, double level, Trend trend) {
        double range = high - low;
        
        if (trend == Trend.BULLISH) {
            // In uptrend, retracements are calculated from high down
            return high - (range * level);
        } else {
            // In downtrend, retracements are calculated from low up
            return low + (range * level);
        }
    }

    private double calculateExtensionPrice(double swingHigh, double swingLow, double recentHigh, 
                                         double recentLow, double level, Trend trend) {
        double swingRange = swingHigh - swingLow;
        
        if (trend == Trend.BULLISH) {
            // Extensions above swing high
            return swingHigh + (swingRange * (level - 1.0));
        } else {
            // Extensions below swing low
            return swingLow - (swingRange * (level - 1.0));
        }
    }

    private FibonacciLevel.LevelStrength determineLevelStrength(double level, List<CandleData> candles) {
        // Key levels get higher strength
        if (level == 0.382 || level == 0.5 || level == 0.618) {
            return FibonacciLevel.LevelStrength.STRONG;
        }
        
        // Other levels get moderate strength
        return FibonacciLevel.LevelStrength.MODERATE;
    }

    public boolean shouldSkipTrade(List<CandleData> candles, double currentPrice, Signal signal) {
        // Skip if trade is too far from any key Fibonacci level
        FibonacciLevel nearestLevel = (signal == Signal.BUY) ? 
            findNearestSupport(candles, currentPrice, signal) : 
            findNearestResistance(candles, currentPrice, signal);

        if (nearestLevel == null) {
            return true; // Skip if no nearby levels
        }

        // Skip if price is more than 3% away from nearest level
        return nearestLevel.getDistanceFromPrice(currentPrice) > 3.0;
    }
}
