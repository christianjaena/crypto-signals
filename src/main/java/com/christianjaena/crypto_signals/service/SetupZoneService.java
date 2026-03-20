package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.Setup;
import com.christianjaena.crypto_signals.model.Trend;
import com.christianjaena.crypto_signals.model.TechnicalIndicators;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SetupZoneService {

    private final TechnicalIndicatorService technicalIndicatorService;

    public SetupZoneService(TechnicalIndicatorService technicalIndicatorService) {
        this.technicalIndicatorService = technicalIndicatorService;
    }

    public Setup identifySetup4H(List<CandleData> candles4H, Trend trend1D) {
        if (candles4H == null || candles4H.isEmpty()) {
            return Setup.NONE;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles4H);
        double currentPrice = candles4H.get(candles4H.size() - 1).getClose();

        switch (trend1D) {
            case BULLISH:
                return validateLongSetup(currentPrice, indicators);
            case BEARISH:
                return validateShortSetup(currentPrice, indicators);
            default:
                return Setup.NONE;
        }
    }

    private Setup validateLongSetup(double currentPrice, TechnicalIndicators indicators) {
        boolean priceNearEMA50 = isPriceNearEMA(currentPrice, indicators.getEma50(), 1.0);
        boolean rsiInRange = indicators.getRsi() >= 30 && indicators.getRsi() <= 40;

        if (priceNearEMA50 && rsiInRange) {
            return Setup.LONG;
        }

        return Setup.NONE;
    }

    private Setup validateShortSetup(double currentPrice, TechnicalIndicators indicators) {
        boolean priceNearEMA50 = isPriceNearEMA(currentPrice, indicators.getEma50(), 2.0);
        boolean rsiInRange = indicators.getRsi() >= 60 && indicators.getRsi() <= 70;

        if (priceNearEMA50 && rsiInRange) {
            return Setup.SHORT;
        }

        return Setup.NONE;
    }

    private boolean isPriceNearEMA(double price, double ema, double tolerancePercent) {
        double difference = Math.abs(price - ema);
        double tolerance = ema * (tolerancePercent / 100.0);
        return difference <= tolerance;
    }

    public boolean isPerfectSetup(double currentPrice, TechnicalIndicators indicators, Setup setup) {
        if (setup == Setup.NONE) {
            return false;
        }

        boolean priceNearEMA50 = isPriceNearEMA(currentPrice, indicators.getEma50(), 2.0);
        
        if (setup == Setup.LONG) {
            return priceNearEMA50 && indicators.getRsi() >= 30 && indicators.getRsi() <= 40;
        } else if (setup == Setup.SHORT) {
            return priceNearEMA50 && indicators.getRsi() >= 60 && indicators.getRsi() <= 70;
        }

        return false;
    }
}
