package com.christianjaena.crypto_signals.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FibonacciLevel {
    public enum LevelType {
        RETRACEMENT, EXTENSION, PROJECTION, UNKNOWN
    }
    
    public enum LevelStrength {
        WEAK, MODERATE, STRONG, VERY_STRONG
    }
    
    private LevelType levelType;
    private double level; // e.g., 0.382, 0.5, 0.618
    private double price;
    private LevelStrength strength;
    private String description;
    
    public static FibonacciLevel createRetracement(double level, double price, LevelStrength strength) {
        return new FibonacciLevel(LevelType.RETRACEMENT, level, price, strength, 
            "Fibonacci retracement at " + (level * 100) + "%");
    }
    
    public static FibonacciLevel createExtension(double level, double price, LevelStrength strength) {
        return new FibonacciLevel(LevelType.EXTENSION, level, price, strength, 
            "Fibonacci extension at " + (level * 100) + "%");
    }
    
    public static FibonacciLevel createProjection(double level, double price, LevelStrength strength) {
        return new FibonacciLevel(LevelType.PROJECTION, level, price, strength, 
            "Fibonacci projection at " + (level * 100) + "%");
    }
    
    public boolean isRetracement() {
        return levelType == LevelType.RETRACEMENT;
    }
    
    public boolean isKeyLevel() {
        return levelType == LevelType.RETRACEMENT && 
               (level == 0.382 || level == 0.5 || level == 0.618);
    }
    
    public boolean isStrongLevel() {
        return strength == LevelStrength.STRONG || strength == LevelStrength.VERY_STRONG;
    }
    
    public double getDistanceFromPrice(double currentPrice) {
        return Math.abs(currentPrice - price) / currentPrice * 100;
    }
    
    public boolean isNearPrice(double currentPrice, double tolerancePercent) {
        return getDistanceFromPrice(currentPrice) <= tolerancePercent;
    }
}
