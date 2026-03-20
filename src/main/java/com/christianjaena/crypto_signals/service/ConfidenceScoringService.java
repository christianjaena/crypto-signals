package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.CandlestickPattern;
import com.christianjaena.crypto_signals.model.Divergence;
import com.christianjaena.crypto_signals.model.ElliottWave;
import com.christianjaena.crypto_signals.model.Setup;
import com.christianjaena.crypto_signals.model.Signal;
import com.christianjaena.crypto_signals.model.Trend;
import com.christianjaena.crypto_signals.model.TechnicalIndicators;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfidenceScoringService {

    private final TechnicalIndicatorService technicalIndicatorService;
    private final SetupZoneService setupZoneService;
    private final ElliottWaveService elliottWaveService;
    private final CandlestickPatternService candlestickPatternService;
    private final FibonacciService fibonacciService;
    private final VolumeAnalysisService volumeAnalysisService;
    private final DivergenceService divergenceService;
    private final SessionFilterService sessionFilterService;

    public ConfidenceScoringService(TechnicalIndicatorService technicalIndicatorService, 
                                  SetupZoneService setupZoneService,
                                  ElliottWaveService elliottWaveService,
                                  CandlestickPatternService candlestickPatternService,
                                  FibonacciService fibonacciService,
                                  VolumeAnalysisService volumeAnalysisService,
                                  DivergenceService divergenceService,
                                  SessionFilterService sessionFilterService) {
        this.technicalIndicatorService = technicalIndicatorService;
        this.setupZoneService = setupZoneService;
        this.elliottWaveService = elliottWaveService;
        this.candlestickPatternService = candlestickPatternService;
        this.fibonacciService = fibonacciService;
        this.volumeAnalysisService = volumeAnalysisService;
        this.divergenceService = divergenceService;
        this.sessionFilterService = sessionFilterService;
    }

    public int calculateConfidence(List<CandleData> candles1D, 
                                 List<CandleData> candles4H, 
                                 List<CandleData> candles15m,
                                 Trend trend1D,
                                 Setup setup4H,
                                 Signal entry15m) {
        
        int confidence = 50;

        if (entry15m == Signal.HOLD) {
            return 0;
        }

        // Check for empty data
        if ((candles1D == null || candles1D.isEmpty()) && 
            (candles4H == null || candles4H.isEmpty()) && 
            (candles15m == null || candles15m.isEmpty())) {
            return 50; // Return base confidence for completely empty data
        }

        // Core confidence factors (original implementation)
        try {
            confidence += calculateHTFTrendBonus(candles1D, trend1D);
            confidence += calculateMTFSetupBonus(candles4H, setup4H);
            confidence += calculateLTFEntryBonus(candles15m, entry15m);
        } catch (Exception e) {
            // If core calculations fail, return base confidence
            return 50;
        }

        // Advanced confidence factors (new implementation)
        try {
            confidence += calculateMultiTimeframeAlignmentBonus(candles1D, candles4H, candles15m, trend1D, setup4H, entry15m);
            confidence += calculateElliottWaveBonus(candles1D, trend1D);
            confidence += calculateCandlestickPatternBonus(candles15m, entry15m);
            confidence += calculateFibonacciLevelBonus(candles4H, entry15m);
            confidence += calculateVolumeConfirmationBonus(candles4H, entry15m);
            confidence += calculateMACDConfirmationBonus(candles15m, entry15m);
            confidence += calculateDivergenceBonus(candles1D, entry15m);
            confidence += calculateVolatilityBonus(candles1D);
            confidence += calculateSessionBonus();
        } catch (Exception e) {
            // If advanced calculations fail, continue with core confidence only
        }

        return Math.min(100, Math.max(0, confidence));
    }

    protected int calculateHTFTrendBonus(List<CandleData> candles1D, Trend trend1D) {
        if (trend1D == Trend.SIDEWAYS || candles1D == null || candles1D.isEmpty()) {
            return 0;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles1D);
        double currentPrice = candles1D.get(candles1D.size() - 1).getClose();
        
        double distanceFromEMA200 = Math.abs(currentPrice - indicators.getEma200()) / indicators.getEma200() * 100;
        
        if (distanceFromEMA200 > 5.0) {
            return 15; // Reduced from 20 to balance with new factors
        } else if (distanceFromEMA200 > 2.0) {
            return 10;
        } else if (distanceFromEMA200 >= 1.0) {
            return 5;
        }

        return 0;
    }

    protected int calculateMTFSetupBonus(List<CandleData> candles4H, Setup setup4H) {
        if (setup4H == Setup.NONE || candles4H == null || candles4H.isEmpty()) {
            return 0;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles4H);
        double currentPrice = candles4H.get(candles4H.size() - 1).getClose();

        if (setupZoneService.isPerfectSetup(currentPrice, indicators, setup4H)) {
            return 15;
        }

        boolean priceNearEMA50 = isPriceNearEMA(currentPrice, indicators.getEma50(), 2.0);
        
        if (priceNearEMA50) {
            return 10;
        }

        return 5;
    }

    protected int calculateLTFEntryBonus(List<CandleData> candles15m, Signal entry15m) {
        if (entry15m == Signal.HOLD || candles15m == null || candles15m.isEmpty()) {
            return 0;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles15m);

        if (entry15m == Signal.BUY) {
            if (indicators.getRsi() < 25) {
                return 15; // Reduced from 20 to balance with new factors
            } else if (indicators.getRsi() < 30) {
                return 10;
            } else if (indicators.getRsi() < 35) {
                return 5;
            }
        } else if (entry15m == Signal.SELL) {
            if (indicators.getRsi() > 75) {
                return 15;
            } else if (indicators.getRsi() > 70) {
                return 10;
            } else if (indicators.getRsi() > 65) {
                return 5;
            }
        }

        double stochSpread = Math.abs(indicators.getStochRsiK() - indicators.getStochRsiD());
        if (stochSpread > 10) {
            return 5;
        }

        return 0;
    }

    protected int calculateMultiTimeframeAlignmentBonus(List<CandleData> candles1D, List<CandleData> candles4H, 
                                                      List<CandleData> candles15m, Trend trend1D, Setup setup4H, Signal entry15m) {
        if (candles1D == null || candles4H == null || candles15m == null) {
            return 0;
        }

        int alignmentScore = 0;

        // Check if all timeframes agree on direction
        boolean bullishAlignment = trend1D == Trend.BULLISH && setup4H == Setup.LONG && entry15m == Signal.BUY;
        boolean bearishAlignment = trend1D == Trend.BEARISH && setup4H == Setup.SHORT && entry15m == Signal.SELL;

        if (bullishAlignment || bearishAlignment) {
            alignmentScore += 15; // Perfect alignment
        } else if ((setup4H == Setup.LONG && entry15m == Signal.BUY) || 
                   (setup4H == Setup.SHORT && entry15m == Signal.SELL)) {
            alignmentScore += 5; // Partial alignment (MTF + LTF)
        } else if ((trend1D == Trend.BULLISH && setup4H == Setup.LONG) || 
                   (trend1D == Trend.BEARISH && setup4H == Setup.SHORT)) {
            alignmentScore += 10; // Partial alignment (HTF + MTF)
        }

        return alignmentScore;
    }

    protected int calculateElliottWaveBonus(List<CandleData> candles1D, Trend trend1D) {
        if (candles1D == null || candles1D.isEmpty()) {
            return 0;
        }

        ElliottWave wave = elliottWaveService.detectElliottWave(candles1D, trend1D);
        return elliottWaveService.getConfidenceBonus(wave, trend1D);
    }

    protected int calculateCandlestickPatternBonus(List<CandleData> candles15m, Signal entry15m) {
        if (candles15m == null || candles15m.isEmpty()) {
            return 0;
        }

        CandlestickPattern pattern = candlestickPatternService.detectPattern(candles15m, entry15m);
        return candlestickPatternService.getConfidenceBonus(pattern, entry15m);
    }

    protected int calculateFibonacciLevelBonus(List<CandleData> candles4H, Signal entry15m) {
        if (candles4H == null || candles4H.isEmpty()) {
            return 0;
        }

        double currentPrice = candles4H.get(candles4H.size() - 1).getClose();
        return fibonacciService.getConfidenceBonus(candles4H, currentPrice, entry15m);
    }

    protected int calculateVolumeConfirmationBonus(List<CandleData> candles4H, Signal entry15m) {
        if (candles4H == null || candles4H.isEmpty()) {
            return 0;
        }

        return volumeAnalysisService.getConfidenceBonus(candles4H, entry15m);
    }

    protected int calculateMACDConfirmationBonus(List<CandleData> candles15m, Signal entry15m) {
        if (candles15m == null || candles15m.isEmpty()) {
            return 0;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles15m);
        
        if (entry15m == Signal.BUY) {
            // MACD bullish confirmation
            if (indicators.getMacdLine() > indicators.getMacdSignal() && indicators.getMacdHistogram() > 0) {
                return 10;
            } else if (indicators.getMacdLine() > indicators.getMacdSignal()) {
                return 5;
            }
        } else if (entry15m == Signal.SELL) {
            // MACD bearish confirmation
            if (indicators.getMacdLine() < indicators.getMacdSignal() && indicators.getMacdHistogram() < 0) {
                return 10;
            } else if (indicators.getMacdLine() < indicators.getMacdSignal()) {
                return 5;
            }
        }

        return 0;
    }

    protected int calculateDivergenceBonus(List<CandleData> candles1D, Signal entry15m) {
        if (candles1D == null || candles1D.isEmpty()) {
            return 0;
        }

        Divergence rsiDivergence = divergenceService.detectRSIDivergence(candles1D, entry15m);
        Divergence macdDivergence = divergenceService.detectMACDDivergence(candles1D, entry15m);
        
        return divergenceService.getConfidenceBonus(rsiDivergence, macdDivergence, entry15m);
    }

    protected int calculateVolatilityBonus(List<CandleData> candles1D) {
        if (candles1D == null || candles1D.isEmpty()) {
            return 0;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles1D);
        
        // Use Bollinger Band width as volatility measure
        double bandWidth = indicators.getBollingerWidth();
        
        if (bandWidth > 0.05) { // High volatility
            return 5;
        } else if (bandWidth > 0.02) { // Moderate volatility
            return 3;
        } else if (bandWidth < 0.01) { // Very low volatility - penalty
            return -2;
        }

        return 0;
    }

    protected int calculateSessionBonus() {
        return sessionFilterService.getConfidenceBonus();
    }

    private boolean isPriceNearEMA(double price, double ema, double tolerancePercent) {
        double difference = Math.abs(price - ema);
        double tolerance = ema * (tolerancePercent / 100.0);
        return difference <= tolerance;
    }
}
