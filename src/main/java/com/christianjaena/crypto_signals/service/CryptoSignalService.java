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
            return createHoldSignal(symbol, "Invalid symbol format");
        }

        if (!signalFilterService.hasSufficientData(candles1D, candles4H, candles15m)) {
            return createHoldSignal(symbol, "Insufficient data for analysis");
        }

        Trend trend1D = trendAnalysisService.determineTrend1D(candles1D);

        if (trend1D == Trend.SIDEWAYS) {
            return createHoldSignal(symbol, "1D trend is sideways");
        }

        Setup setup4H = setupZoneService.identifySetup4H(candles4H, trend1D);

        if (setup4H == Setup.NONE) {
            return createHoldSignal(symbol, "No valid setup zone identified on 4H timeframe");
        }

        Signal entry15m = entryConfirmationService.confirmEntry15m(candles15m, setup4H);

        if (entry15m == Signal.HOLD) {
            return createHoldSignal(symbol, "Entry conditions not met on 15m timeframe");
        }

        if (signalFilterService.shouldFilterSignal(symbol, candles1D, candles4H, entry15m)) {
            return createHoldSignal(symbol, "Signal filtered due to sideways RSI, low volume, or frequency limit");
        }

        int confidence = confidenceScoringService.calculateConfidence(
            candles1D, candles4H, candles15m, trend1D, setup4H, entry15m);

        CryptoSignal signal = new CryptoSignal();
        signal.setSymbol(symbol);
        signal.setTrend1D(trend1D);
        signal.setSetup4H(setup4H);
        signal.setEntry15m(entry15m);
        signal.setConfidence(confidence);
        signal.setNotes(generateNotes(trend1D, setup4H, entry15m, candles1D, candles4H, candles15m));

        signalFilterService.recordSignal(symbol);

        return signal;
    }

    private CryptoSignal createHoldSignal(String symbol, String reason) {
        CryptoSignal signal = new CryptoSignal();
        signal.setSymbol(symbol);
        signal.setTrend1D(Trend.SIDEWAYS);
        signal.setSetup4H(Setup.NONE);
        signal.setEntry15m(Signal.HOLD);
        signal.setConfidence(0);
        signal.setNotes(List.of(reason));
        return signal;
    }

    private List<String> generateNotes(Trend trend1D, Setup setup4H, Signal entry15m,
                                       List<CandleData> candles1D, List<CandleData> candles4H, List<CandleData> candles15m) {
        List<String> notes = new ArrayList<>();

        notes.add(String.format("1D trend %s", trend1D.toString().toLowerCase()));

        if (setup4H != Setup.NONE) {
            notes.add(String.format("4H setup %s", setup4H.toString().toLowerCase()));
        }

        if (entry15m != Signal.HOLD) {
            notes.add(String.format("15m entry %s confirmed", entry15m.toString().toLowerCase()));
        }

        return notes;
    }

    public void clearSignalHistory(String symbol) {
        signalFilterService.clearSignalHistory(symbol);
    }

    public void clearAllSignalHistory() {
        signalFilterService.clearAllSignalHistory();
    }
}
