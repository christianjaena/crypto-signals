package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.Signal;
import com.christianjaena.crypto_signals.model.TechnicalIndicators;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SignalFilterService {

    private final TechnicalIndicatorService technicalIndicatorService;
    private final Map<String, LocalDateTime> lastSignalTimes = new ConcurrentHashMap<>();
    private static final long MIN_SIGNAL_INTERVAL_MINUTES = 30;

    public SignalFilterService(TechnicalIndicatorService technicalIndicatorService) {
        this.technicalIndicatorService = technicalIndicatorService;
    }

    public boolean shouldFilterSignal(String symbol, 
                                    List<CandleData> candles1D, 
                                    List<CandleData> candles4H,
                                    Signal signal) {
        
        if (signal == Signal.HOLD) {
            return true;
        }

        if (isSidewaysRSI1D(candles1D)) {
            return true;
        }

        if (isLowVolume4H(candles4H)) {
            return true;
        }

        if (isTooSoonSinceLastSignal(symbol)) {
            return true;
        }

        return false;
    }

    private boolean isSidewaysRSI1D(List<CandleData> candles1D) {
        if (candles1D == null || candles1D.isEmpty()) {
            return true;
        }

        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(candles1D);
        double rsi = indicators.getRsi();

        return rsi >= 45 && rsi <= 55;
    }

    private boolean isLowVolume4H(List<CandleData> candles4H) {
        if (candles4H == null || candles4H.size() < 21) {
            return true;
        }

        double currentVolume = candles4H.get(candles4H.size() - 1).getVolume();
        double volumeAverage = technicalIndicatorService.calculateIndicators(candles4H).getVolumeAverage20();

        return currentVolume < volumeAverage;
    }

    private boolean isTooSoonSinceLastSignal(String symbol) {
        LocalDateTime lastSignalTime = lastSignalTimes.get(symbol);
        if (lastSignalTime == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return lastSignalTime.plusMinutes(MIN_SIGNAL_INTERVAL_MINUTES).isAfter(now);
    }

    public void recordSignal(String symbol) {
        lastSignalTimes.put(symbol, LocalDateTime.now());
    }

    public void clearSignalHistory(String symbol) {
        lastSignalTimes.remove(symbol);
    }

    public void clearAllSignalHistory() {
        lastSignalTimes.clear();
    }

    public boolean isSymbolValid(String symbol) {
        return symbol != null && !symbol.trim().isEmpty() && symbol.contains("/");
    }

    public boolean hasSufficientData(List<CandleData> candles1D, 
                                   List<CandleData> candles4H, 
                                   List<CandleData> candles15m) {
        int min1D = 200;
        int min4H = 100;
        int min15m = 50;

        boolean sufficient1D = candles1D != null && candles1D.size() >= min1D;
        boolean sufficient4H = candles4H != null && candles4H.size() >= min4H;
        boolean sufficient15m = candles15m != null && candles15m.size() >= min15m;

        return sufficient1D && sufficient4H && sufficient15m;
    }
}
