package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
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

    public ConfidenceScoringService(TechnicalIndicatorService technicalIndicatorService, 
                                  SetupZoneService setupZoneService) {
        this.technicalIndicatorService = technicalIndicatorService;
        this.setupZoneService = setupZoneService;
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

        confidence += calculateHTFTrendBonus(candles1D, trend1D);
        confidence += calculateMTFSetupBonus(candles4H, setup4H);
        confidence += calculateLTFEntryBonus(candles15m, entry15m);

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
            return 20;
        } else if (distanceFromEMA200 > 2.0) {
            return 15;
        } else if (distanceFromEMA200 >= 1.0) {
            return 10;
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
                return 20;
            } else if (indicators.getRsi() < 30) {
                return 15;
            } else if (indicators.getRsi() < 35) {
                return 10;
            }
        } else if (entry15m == Signal.SELL) {
            if (indicators.getRsi() > 75) {
                return 20;
            } else if (indicators.getRsi() > 70) {
                return 15;
            } else if (indicators.getRsi() > 65) {
                return 10;
            }
        }

        double stochSpread = Math.abs(indicators.getStochRsiK() - indicators.getStochRsiD());
        if (stochSpread > 10) {
            return 5;
        }

        return 0;
    }

    private boolean isPriceNearEMA(double price, double ema, double tolerancePercent) {
        double difference = Math.abs(price - ema);
        double tolerance = ema * (tolerancePercent / 100.0);
        return difference <= tolerance;
    }
}
