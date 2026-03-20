package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.Signal;
import com.christianjaena.crypto_signals.model.TechnicalIndicators;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VolumeAnalysisService {

    private final TechnicalIndicatorService technicalIndicatorService;

    public VolumeAnalysisService(TechnicalIndicatorService technicalIndicatorService) {
        this.technicalIndicatorService = technicalIndicatorService;
    }

    public boolean isVolumeSufficient(List<CandleData> candles, Signal signal) {
        if (candles == null || candles.isEmpty()) {
            return false;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles);
        double currentVolume = candles.get(candles.size() - 1).getVolume();
        double volumeAverage = indicators.getVolumeAverage20();

        // Volume should be at least 1.2x the average for signal confirmation
        double volumeMultiplier = getVolumeMultiplier(signal);
        return currentVolume >= volumeAverage * volumeMultiplier;
    }

    public boolean hasVolumeSpike(List<CandleData> candles, double thresholdMultiplier) {
        if (candles == null || candles.size() < 5) {
            return false;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles);
        double currentVolume = candles.get(candles.size() - 1).getVolume();
        double volumeAverage = indicators.getVolumeAverage20();

        return currentVolume >= volumeAverage * thresholdMultiplier;
    }

    public boolean hasIncreasingVolume(List<CandleData> candles, int periods) {
        if (candles == null || candles.size() < periods + 1) {
            return false;
        }

        // Check if volume has been increasing over the specified periods
        for (int i = candles.size() - periods; i < candles.size() - 1; i++) {
            double currentVolume = candles.get(i + 1).getVolume();
            double previousVolume = candles.get(i).getVolume();
            
            // Allow for small fluctuations (within 5% tolerance)
            if (currentVolume < previousVolume * 0.95) {
                return false;
            }
        }

        return true;
    }

    public boolean hasVolumeConfirmation(List<CandleData> candles, Signal signal) {
        if (!isVolumeSufficient(candles, signal)) {
            return false;
        }

        // Additional volume confirmation based on signal type
        if (signal == Signal.BUY) {
            return hasBuyVolumeConfirmation(candles);
        } else if (signal == Signal.SELL) {
            return hasSellVolumeConfirmation(candles);
        }

        return false;
    }

    private boolean hasBuyVolumeConfirmation(List<CandleData> candles) {
        // For buy signals, we want to see volume spike and increasing volume
        return hasVolumeSpike(candles, 1.5) && hasIncreasingVolume(candles, 3);
    }

    private boolean hasSellVolumeConfirmation(List<CandleData> candles) {
        // For sell signals, we want to see volume spike (can be panic selling)
        return hasVolumeSpike(candles, 1.3);
    }

    public double getVolumeRatio(List<CandleData> candles) {
        if (candles == null || candles.isEmpty()) {
            return 0.0;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles);
        double currentVolume = candles.get(candles.size() - 1).getVolume();
        double volumeAverage = indicators.getVolumeAverage20();

        return volumeAverage > 0 ? currentVolume / volumeAverage : 0.0;
    }

    public boolean isVolumeDiverging(List<CandleData> candles, Signal signal) {
        if (candles == null || candles.size() < 10) {
            return false;
        }

        // Check for volume divergence (price moving one way, volume another)
        double currentPrice = candles.get(candles.size() - 1).getClose();
        double pastPrice = candles.get(candles.size() - 10).getClose();
        double priceChange = (currentPrice - pastPrice) / pastPrice;

        double currentVolume = candles.get(candles.size() - 1).getVolume();
        double pastVolume = candles.get(candles.size() - 10).getVolume();
        double volumeChange = (currentVolume - pastVolume) / pastVolume;

        // Bullish divergence: price up but volume down
        if (signal == Signal.BUY && priceChange > 0.02 && volumeChange < -0.2) {
            return true;
        }

        // Bearish divergence: price down but volume down (lack of selling pressure)
        if (signal == Signal.SELL && priceChange < -0.02 && volumeChange < -0.2) {
            return true;
        }

        return false;
    }

    public int getConfidenceBonus(List<CandleData> candles, Signal signal) {
        if (!hasVolumeConfirmation(candles, signal)) {
            return 0;
        }

        int bonus = 5; // Base bonus for volume confirmation

        // Additional bonus for strong volume
        if (hasVolumeSpike(candles, 2.0)) {
            bonus += 3; // Reduced bonus for strong volume spike
        }

        // Additional bonus for increasing volume trend
        if (hasIncreasingVolume(candles, 5)) {
            bonus += 2; // Reduced bonus for sustained volume increase
        }

        // Penalty for volume divergence
        if (isVolumeDiverging(candles, signal)) {
            bonus = 0; // No bonus for divergence (instead of reduction)
        }

        return Math.max(0, Math.min(bonus, 8)); // Clamp between 0 and 8
    }

    private double getVolumeMultiplier(Signal signal) {
        switch (signal) {
            case BUY:
                return 1.2; // Higher volume requirement for buy signals
            case SELL:
                return 1.1; // Slightly lower requirement for sell signals
            default:
                return 1.0;
        }
    }

    public boolean shouldSkipSignal(List<CandleData> candles, Signal signal) {
        // Skip signal if volume is too low
        if (!isVolumeSufficient(candles, signal)) {
            return true;
        }

        // Skip signal if there's negative volume divergence
        if (isVolumeDiverging(candles, signal)) {
            return true;
        }

        return false;
    }

    public String getVolumeAnalysis(List<CandleData> candles, Signal signal) {
        if (candles == null || candles.isEmpty()) {
            return "No volume data available";
        }

        StringBuilder analysis = new StringBuilder();
        double volumeRatio = getVolumeRatio(candles);

        analysis.append("Volume ratio: ").append(String.format("%.2f", volumeRatio)).append("x average");

        if (hasVolumeSpike(candles, 1.5)) {
            analysis.append(" | Volume spike detected");
        }

        if (hasIncreasingVolume(candles, 3)) {
            analysis.append(" | Increasing volume trend");
        }

        if (isVolumeDiverging(candles, signal)) {
            analysis.append(" | Volume divergence warning");
        }

        if (hasVolumeConfirmation(candles, signal)) {
            analysis.append(" | Volume confirms signal");
        } else {
            analysis.append(" | Volume does not confirm signal");
        }

        return analysis.toString();
    }
}
