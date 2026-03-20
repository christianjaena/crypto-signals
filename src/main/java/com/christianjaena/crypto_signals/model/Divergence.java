package com.christianjaena.crypto_signals.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Divergence {
    public enum DivergenceType {
        RSI_BULLISH, RSI_BEARISH, MACD_BULLISH, MACD_BEARISH, NONE
    }
    
    public enum DivergenceStrength {
        WEAK, MODERATE, STRONG, VERY_STRONG
    }
    
    private DivergenceType divergenceType;
    private DivergenceStrength strength;
    private double confidence;
    private String description;
    
    public static Divergence createBullishRSIDivergence(DivergenceStrength strength, double confidence) {
        return new Divergence(DivergenceType.RSI_BULLISH, strength, confidence, 
            "Bullish RSI divergence detected");
    }
    
    public static Divergence createBearishRSIDivergence(DivergenceStrength strength, double confidence) {
        return new Divergence(DivergenceType.RSI_BEARISH, strength, confidence, 
            "Bearish RSI divergence detected");
    }
    
    public static Divergence createBullishMACDDivergence(DivergenceStrength strength, double confidence) {
        return new Divergence(DivergenceType.MACD_BULLISH, strength, confidence, 
            "Bullish MACD divergence detected");
    }
    
    public static Divergence createBearishMACDDivergence(DivergenceStrength strength, double confidence) {
        return new Divergence(DivergenceType.MACD_BEARISH, strength, confidence, 
            "Bearish MACD divergence detected");
    }
    
    public static Divergence createNone() {
        return new Divergence(DivergenceType.NONE, DivergenceStrength.WEAK, 0.0, 
            "No divergence detected");
    }
    
    public boolean hasDivergence() {
        return divergenceType != DivergenceType.NONE;
    }
    
    public boolean isBullishDivergence() {
        return divergenceType == DivergenceType.RSI_BULLISH || 
               divergenceType == DivergenceType.MACD_BULLISH;
    }
    
    public boolean isBearishDivergence() {
        return divergenceType == DivergenceType.RSI_BEARISH || 
               divergenceType == DivergenceType.MACD_BEARISH;
    }
    
    public boolean isStrongDivergence() {
        return strength == DivergenceStrength.STRONG || strength == DivergenceStrength.VERY_STRONG;
    }
    
    public boolean alignsWithSignal(Signal signal) {
        if (divergenceType == DivergenceType.NONE) {
            return true; // No divergence doesn't conflict
        }
        
        return (signal == Signal.BUY && isBullishDivergence()) ||
               (signal == Signal.SELL && isBearishDivergence());
    }
}
