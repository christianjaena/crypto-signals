package com.christianjaena.crypto_signals.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalIndicators {
    private double ema50;
    private double ema200;
    private double rsi;
    private double stochRsiK;
    private double stochRsiD;
    private double volumeAverage20;
    
    // MACD indicators
    private double macdLine;
    private double macdSignal;
    private double macdHistogram;
    
    // Bollinger Bands
    private double bollingerUpper;
    private double bollingerMiddle;
    private double bollingerLower;
    private double bollingerWidth;
}
