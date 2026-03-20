package com.christianjaena.crypto_signals.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandlestickPattern {
    public enum PatternType {
        BULLISH_ENGULFING,
        BEARISH_ENGULFING,
        HAMMER,
        SHOOTING_STAR,
        DOJI,
        MORNING_STAR,
        EVENING_STAR,
        UNKNOWN
    }
    
    public enum SignalDirection {
        BULLISH, BEARISH, NEUTRAL
    }
    
    private PatternType patternType;
    private SignalDirection signalDirection;
    private double confidence;
    private String description;
    
    public static CandlestickPattern createBullishPattern(PatternType patternType, double confidence) {
        return new CandlestickPattern(patternType, SignalDirection.BULLISH, confidence, 
            "Bullish " + patternType + " pattern detected");
    }
    
    public static CandlestickPattern createBearishPattern(PatternType patternType, double confidence) {
        return new CandlestickPattern(patternType, SignalDirection.BEARISH, confidence, 
            "Bearish " + patternType + " pattern detected");
    }
    
    public static CandlestickPattern createNeutralPattern(PatternType patternType, double confidence) {
        return new CandlestickPattern(patternType, SignalDirection.NEUTRAL, confidence, 
            "Neutral " + patternType + " pattern detected");
    }
    
    public static CandlestickPattern createUnknown() {
        return new CandlestickPattern(PatternType.UNKNOWN, SignalDirection.NEUTRAL, 0.0, 
            "No clear candlestick pattern detected");
    }
    
    public boolean isBullish() {
        return signalDirection == SignalDirection.BULLISH;
    }
    
    public boolean isBearish() {
        return signalDirection == SignalDirection.BEARISH;
    }
    
    public boolean isStrongPattern() {
        return confidence >= 0.7;
    }
    
    public boolean isReversalPattern() {
        return patternType == PatternType.BULLISH_ENGULFING ||
               patternType == PatternType.BEARISH_ENGULFING ||
               patternType == PatternType.MORNING_STAR ||
               patternType == PatternType.EVENING_STAR;
    }
}
