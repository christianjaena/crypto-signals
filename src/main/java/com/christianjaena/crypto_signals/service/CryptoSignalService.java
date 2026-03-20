package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CryptoSignalService {

    private final TrendAnalysisService trendAnalysisService;
    private final SetupZoneService setupZoneService;
    private final EntryConfirmationService entryConfirmationService;
    private final ConfidenceScoringService confidenceScoringService;
    private final SignalFilterService signalFilterService;

    public CryptoSignalService(TrendAnalysisService trendAnalysisService,
                              SetupZoneService setupZoneService,
                              EntryConfirmationService entryConfirmationService,
                              ConfidenceScoringService confidenceScoringService,
                              SignalFilterService signalFilterService) {
        this.trendAnalysisService = trendAnalysisService;
        this.setupZoneService = setupZoneService;
        this.entryConfirmationService = entryConfirmationService;
        this.confidenceScoringService = confidenceScoringService;
        this.signalFilterService = signalFilterService;
    }

    public CryptoSignal generateSignal(String symbol,
                                     List<CandleData> candles1D,
                                     List<CandleData> candles4H,
                                     List<CandleData> candles15m) {

        if (!signalFilterService.isSymbolValid(symbol)) {
            return createHoldSignal(symbol, candles15m, "Invalid symbol format");
        }

        if (!signalFilterService.hasSufficientData(candles1D, candles4H, candles15m)) {
            return createHoldSignal(symbol, candles15m, "Insufficient data for analysis");
        }

        Trend trend1D = trendAnalysisService.determineTrend1D(candles1D);

        if (trend1D == Trend.SIDEWAYS) {
            return createHoldSignal(symbol, candles15m, "1D trend is sideways");
        }

        Setup setup4H = setupZoneService.identifySetup4H(candles4H, trend1D);

        if (setup4H == Setup.NONE) {
            return createHoldSignal(symbol, candles15m, "No valid setup zone identified on 4H timeframe");
        }

        Signal entry15m = entryConfirmationService.confirmEntry15m(candles15m, setup4H);

        if (entry15m == Signal.HOLD) {
            return createHoldSignal(symbol, candles15m, "Entry conditions not met on 15m timeframe");
        }

        if (signalFilterService.shouldFilterSignal(symbol, candles1D, candles4H, entry15m)) {
            return createHoldSignal(symbol, candles15m, "Signal filtered due to sideways RSI, low volume, or frequency limit");
        }

        int confidence = confidenceScoringService.calculateConfidence(
            candles1D, candles4H, candles15m, trend1D, setup4H, entry15m);

        // Calculate current price and price targets
        double currentPrice = candles15m.get(candles15m.size() - 1).getClose();
        double stopLoss = calculateStopLoss(candles4H, trend1D, setup4H, currentPrice);
        double predictionPriceGrowth = calculatePredictionPrice(candles4H, trend1D, entry15m, currentPrice, stopLoss);

        CryptoSignal signal = new CryptoSignal();
        signal.setSymbol(symbol);
        signal.setCurrentPrice(currentPrice);
        signal.setStopLoss(stopLoss);
        signal.setPredictionPriceGrowth(predictionPriceGrowth);
        signal.setTrend1D(trend1D);
        signal.setSetup4H(setup4H);
        signal.setEntry15m(entry15m);
        signal.setConfidence(confidence);
        signal.setNotes(generateNotes(trend1D, setup4H, entry15m, candles1D, candles4H, candles15m, currentPrice, stopLoss, predictionPriceGrowth));

        signalFilterService.recordSignal(symbol);

        return signal;
    }

    private CryptoSignal createHoldSignal(String symbol, List<CandleData> candles15m, String reason) {
        CryptoSignal signal = new CryptoSignal();
        signal.setSymbol(symbol);
        signal.setTrend1D(Trend.SIDEWAYS);
        signal.setSetup4H(Setup.NONE);
        signal.setEntry15m(Signal.HOLD);
        signal.setConfidence(0);
        
        // Set current price from latest candle if available
        double currentPrice = 0;
        if (candles15m != null && !candles15m.isEmpty()) {
            currentPrice = candles15m.get(candles15m.size() - 1).getClose();
        }
        signal.setCurrentPrice(currentPrice);
        signal.setStopLoss(0);
        signal.setPredictionPriceGrowth(0);
        
        signal.setNotes(List.of(reason));
        return signal;
    }

    private List<String> generateNotes(Trend trend1D, Setup setup4H, Signal entry15m,
                                       List<CandleData> candles1D, List<CandleData> candles4H, List<CandleData> candles15m,
                                       double currentPrice, double stopLoss, double predictionPriceGrowth) {
        List<String> notes = new ArrayList<>();

        notes.add(String.format("1D trend %s", trend1D.toString().toLowerCase()));
        notes.add(String.format("Current price: %.2f", currentPrice));
        notes.add(String.format("Stop loss: %.2f", stopLoss));
        notes.add(String.format("Target price: %.2f", predictionPriceGrowth));

        if (setup4H != Setup.NONE) {
            notes.add(String.format("4H setup %s", setup4H.toString().toLowerCase()));
        }

        if (entry15m != Signal.HOLD) {
            notes.add(String.format("15m entry %s confirmed", entry15m.toString().toLowerCase()));
        }

        return notes;
    }

    private double calculateStopLoss(List<CandleData> candles4H, Trend trend1D, Setup setup4H, double currentPrice) {
        if (candles4H == null || candles4H.isEmpty()) {
            return currentPrice * 0.95; // Default 5% stop loss
        }

        // Find recent swing low/high based on lookback period
        int lookback = Math.min(20, candles4H.size());
        
        if (setup4H == Setup.LONG) {
            // For LONG: stop loss below recent swing low
            double recentLow = Double.MAX_VALUE;
            for (int i = candles4H.size() - lookback; i < candles4H.size(); i++) {
                recentLow = Math.min(recentLow, candles4H.get(i).getLow());
            }
            // Stop loss 1% below recent low
            return recentLow * 0.99;
        } else if (setup4H == Setup.SHORT) {
            // For SHORT: stop loss above recent swing high
            double recentHigh = Double.MIN_VALUE;
            for (int i = candles4H.size() - lookback; i < candles4H.size(); i++) {
                recentHigh = Math.max(recentHigh, candles4H.get(i).getHigh());
            }
            // Stop loss 1% above recent high
            return recentHigh * 1.01;
        }
        
        return currentPrice * 0.95; // Default 5% stop loss
    }

    private double calculatePredictionPrice(List<CandleData> candles4H, Trend trend1D, Signal entry15m, 
                                            double currentPrice, double stopLoss) {
        // Calculate risk (distance from entry to stop loss)
        double risk = Math.abs(currentPrice - stopLoss);
        
        // Use 1:2 risk-reward ratio as minimum target
        // For higher confidence, use Fibonacci extension levels
        double rewardMultiplier = 2.0;
        
        if (entry15m == Signal.BUY) {
            return currentPrice + (risk * rewardMultiplier);
        } else if (entry15m == Signal.SELL) {
            return currentPrice - (risk * rewardMultiplier);
        }
        
        return currentPrice; // Default: no prediction
    }

    public void clearSignalHistory(String symbol) {
        signalFilterService.clearSignalHistory(symbol);
    }

    public void clearAllSignalHistory() {
        signalFilterService.clearAllSignalHistory();
    }
}
