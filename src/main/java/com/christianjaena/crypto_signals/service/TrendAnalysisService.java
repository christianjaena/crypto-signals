package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.TechnicalIndicators;
import com.christianjaena.crypto_signals.model.Trend;
import org.springframework.stereotype.Service;

@Service
public class TrendAnalysisService {

    private final TechnicalIndicatorService technicalIndicatorService;

    public TrendAnalysisService(TechnicalIndicatorService technicalIndicatorService) {
        this.technicalIndicatorService = technicalIndicatorService;
    }

    public Trend determineTrend1D(java.util.List<CandleData> candles1D) {
        if (candles1D == null || candles1D.isEmpty()) {
            return Trend.SIDEWAYS;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles1D);
        double currentPrice = candles1D.get(candles1D.size() - 1).getClose();

        boolean bullishConditions = 
            currentPrice > indicators.getEma200() &&
            indicators.getEma50() > indicators.getEma200() &&
            indicators.getRsi() > 50;

        boolean bearishConditions = 
            currentPrice < indicators.getEma200() &&
            indicators.getEma50() < indicators.getEma200() &&
            indicators.getRsi() < 50;

        if (bullishConditions) {
            return Trend.BULLISH;
        } else if (bearishConditions) {
            return Trend.BEARISH;
        } else {
            return Trend.SIDEWAYS;
        }
    }

    public boolean isSidewaysRSI(double rsi) {
        return rsi >= 45 && rsi <= 55;
    }

    public double calculateTrendStrength(java.util.List<CandleData> candles1D) {
        if (candles1D == null || candles1D.isEmpty()) {
            return 0.0;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles1D);
        double currentPrice = candles1D.get(candles1D.size() - 1).getClose();
        
        double distanceFromEMA200 = Math.abs(currentPrice - indicators.getEma200()) / indicators.getEma200() * 100;
        double emaSpread = Math.abs(indicators.getEma50() - indicators.getEma200()) / indicators.getEma200() * 100;
        
        return (distanceFromEMA200 + emaSpread) / 2;
    }
}
