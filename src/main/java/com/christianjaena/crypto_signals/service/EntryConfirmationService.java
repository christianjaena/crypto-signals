package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.Setup;
import com.christianjaena.crypto_signals.model.Signal;
import com.christianjaena.crypto_signals.model.TechnicalIndicators;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntryConfirmationService {

    private final TechnicalIndicatorService technicalIndicatorService;

    public EntryConfirmationService(TechnicalIndicatorService technicalIndicatorService) {
        this.technicalIndicatorService = technicalIndicatorService;
    }

    public Signal confirmEntry15m(List<CandleData> candles15m, Setup setup4H) {
        if (candles15m == null || candles15m.isEmpty() || setup4H == Setup.NONE) {
            return Signal.HOLD;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles15m);

        switch (setup4H) {
            case LONG:
                return validateLongEntry(indicators);
            case SHORT:
                return validateShortEntry(indicators);
            default:
                return Signal.HOLD;
        }
    }

    private Signal validateLongEntry(TechnicalIndicators indicators) {
        boolean rsiOversold = indicators.getRsi() < 30;
        boolean stochRSICrossUp = indicators.getStochRsiK() > indicators.getStochRsiD();

        if (rsiOversold && stochRSICrossUp) {
            return Signal.BUY;
        }

        return Signal.HOLD;
    }

    private Signal validateShortEntry(TechnicalIndicators indicators) {
        boolean rsiOverbought = indicators.getRsi() > 70;
        boolean stochRSICrossDown = indicators.getStochRsiK() < indicators.getStochRsiD();

        if (rsiOverbought && stochRSICrossDown) {
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    public boolean hasStochRSICrossUp(List<CandleData> candles15m) {
        if (candles15m == null || candles15m.size() < 2) {
            return false;
        }

        TechnicalIndicators current = technicalIndicatorService.calculateIndicators(candles15m);
        
        List<CandleData> previousCandles = candles15m.subList(0, candles15m.size() - 1);
        TechnicalIndicators previous = technicalIndicatorService.calculateIndicators(previousCandles);

        return previous.getStochRsiK() <= previous.getStochRsiD() && 
               current.getStochRsiK() > current.getStochRsiD();
    }

    public boolean hasStochRSICrossDown(List<CandleData> candles15m) {
        if (candles15m == null || candles15m.size() < 2) {
            return false;
        }

        TechnicalIndicators current = technicalIndicatorService.calculateIndicators(candles15m);
        
        List<CandleData> previousCandles = candles15m.subList(0, candles15m.size() - 1);
        TechnicalIndicators previous = technicalIndicatorService.calculateIndicators(previousCandles);

        return previous.getStochRsiK() >= previous.getStochRsiD() && 
               current.getStochRsiK() < current.getStochRsiD();
    }
}
