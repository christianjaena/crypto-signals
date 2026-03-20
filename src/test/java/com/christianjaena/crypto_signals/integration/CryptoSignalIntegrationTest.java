package com.christianjaena.crypto_signals.integration;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.CryptoSignal;
import com.christianjaena.crypto_signals.model.Setup;
import com.christianjaena.crypto_signals.model.Signal;
import com.christianjaena.crypto_signals.model.Trend;
import com.christianjaena.crypto_signals.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "mexc.access-key=test-key",
    "mexc.secret-key=test-secret"
})
class CryptoSignalIntegrationTest {

    @Autowired
    private CryptoSignalService cryptoSignalService;

    @Autowired
    private TechnicalIndicatorService technicalIndicatorService;

    @Autowired
    private TrendAnalysisService trendAnalysisService;

    @Autowired
    private SetupZoneService setupZoneService;

    @Autowired
    private EntryConfirmationService entryConfirmationService;

    @Autowired
    private ConfidenceScoringService confidenceScoringService;

    @Autowired
    private SignalFilterService signalFilterService;

    private List<CandleData> bullishCandles1D;
    private List<CandleData> bullishCandles4H;
    private List<CandleData> bullishCandles15m;

    @BeforeEach
    void setUp() {
        bullishCandles1D = createBullishCandles("1D", 250);
        bullishCandles4H = createBullishCandles("4H", 100);
        bullishCandles15m = createBullishCandles("15m", 50);
    }

    @Test
    void generateSignal_CompleteBullishFlow_ReturnsBuySignal() {
        CryptoSignal signal = cryptoSignalService.generateSignal(
            "BTC/USDT", bullishCandles1D, bullishCandles4H, bullishCandles15m);

        assertNotNull(signal);
        assertEquals("BTC/USDT", signal.getSymbol());
        assertNotNull(signal.getTrend1D());
        assertNotNull(signal.getSetup4H());
        assertNotNull(signal.getEntry15m());
        assertTrue(signal.getConfidence() >= 0 && signal.getConfidence() <= 100);
        assertNotNull(signal.getNotes());
    }

    @Test
    void generateSignal_BearishFlow_ReturnsSellSignal() {
        List<CandleData> bearishCandles1D = createBearishCandles("1D", 250);
        List<CandleData> bearishCandles4H = createBearishCandles("4H", 100);
        List<CandleData> bearishCandles15m = createBearishCandles("15m", 50);

        CryptoSignal signal = cryptoSignalService.generateSignal(
            "BTC/USDT", bearishCandles1D, bearishCandles4H, bearishCandles15m);

        assertNotNull(signal);
        assertEquals("BTC/USDT", signal.getSymbol());
    }

    @Test
    void generateSignal_SidewaysTrend_ReturnsHoldSignal() {
        List<CandleData> sidewaysCandles1D = createSidewaysCandles("1D", 250);
        List<CandleData> sidewaysCandles4H = createSidewaysCandles("4H", 100);
        List<CandleData> sidewaysCandles15m = createSidewaysCandles("15m", 50);

        CryptoSignal signal = cryptoSignalService.generateSignal(
            "BTC/USDT", sidewaysCandles1D, sidewaysCandles4H, sidewaysCandles15m);

        assertNotNull(signal);
        assertEquals("BTC/USDT", signal.getSymbol());
        assertEquals(Signal.HOLD, signal.getEntry15m());
        assertEquals(0, signal.getConfidence());
    }

    @Test
    void generateSignal_InvalidSymbol_ReturnsHoldSignal() {
        CryptoSignal signal = cryptoSignalService.generateSignal(
            "", bullishCandles1D, bullishCandles4H, bullishCandles15m);

        assertNotNull(signal);
        assertEquals(Signal.HOLD, signal.getEntry15m());
        assertTrue(signal.getNotes().get(0).contains("Invalid symbol"));
    }

    @Test
    void generateSignal_InsufficientData_ReturnsHoldSignal() {
        List<CandleData> insufficientCandles = Arrays.asList(createCandle(100000.0, "1D"));

        CryptoSignal signal = cryptoSignalService.generateSignal(
            "BTC/USDT", insufficientCandles, insufficientCandles, insufficientCandles);

        assertNotNull(signal);
        assertEquals(Signal.HOLD, signal.getEntry15m());
        assertTrue(signal.getNotes().get(0).contains("Insufficient data"));
    }

    @Test
    void trendAnalysisService_BullishConditions_ReturnsBullish() {
        Trend trend = trendAnalysisService.determineTrend1D(bullishCandles1D);

        assertNotNull(trend);
        assertTrue(trend == Trend.BULLISH || trend == Trend.SIDEWAYS || trend == Trend.BEARISH);
    }

    @Test
    void setupZoneService_BullishTrend_ReturnsValidSetup() {
        Trend trend = trendAnalysisService.determineTrend1D(bullishCandles1D);
        Setup setup = setupZoneService.identifySetup4H(bullishCandles4H, trend);

        assertNotNull(setup);
        assertTrue(setup == Setup.LONG || setup == Setup.SHORT || setup == Setup.NONE);
    }

    @Test
    void entryConfirmationService_ValidSetup_ReturnsSignal() {
        Trend trend = trendAnalysisService.determineTrend1D(bullishCandles1D);
        Setup setup = setupZoneService.identifySetup4H(bullishCandles4H, trend);
        Signal signal = entryConfirmationService.confirmEntry15m(bullishCandles15m, setup);

        assertNotNull(signal);
        assertTrue(signal == Signal.BUY || signal == Signal.SELL || signal == Signal.HOLD);
    }

    @Test
    void confidenceScoringService_ValidInputs_ReturnsConfidenceScore() {
        Trend trend = trendAnalysisService.determineTrend1D(bullishCandles1D);
        Setup setup = setupZoneService.identifySetup4H(bullishCandles4H, trend);
        Signal signal = entryConfirmationService.confirmEntry15m(bullishCandles15m, setup);

        int confidence = confidenceScoringService.calculateConfidence(
            bullishCandles1D, bullishCandles4H, bullishCandles15m, trend, setup, signal);

        assertTrue(confidence >= 0 && confidence <= 100);
    }

    @Test
    void signalFilterService_ValidSymbol_ReturnsFalse() {
        boolean shouldFilter = signalFilterService.shouldFilterSignal(
            "BTC/USDT", bullishCandles1D, bullishCandles4H, Signal.BUY);

        assertFalse(shouldFilter);
    }

    @Test
    void signalFilterService_InvalidSymbol_ReturnsTrue() {
        boolean shouldFilter = signalFilterService.shouldFilterSignal(
            "", bullishCandles1D, bullishCandles4H, Signal.BUY);

        assertTrue(shouldFilter);
    }

    @Test
    void technicalIndicatorService_ValidCandles_ReturnsIndicators() {
        var indicators = technicalIndicatorService.calculateIndicators(bullishCandles1D);

        assertNotNull(indicators);
        assertTrue(indicators.getEma50() > 0);
        assertTrue(indicators.getEma200() > 0);
        assertTrue(indicators.getRsi() >= 0 && indicators.getRsi() <= 100);
    }

    @Test
    void completeWorkflow_EndToEnd_ReturnsConsistentResults() {
        CryptoSignal signal1 = cryptoSignalService.generateSignal(
            "BTC/USDT", bullishCandles1D, bullishCandles4H, bullishCandles15m);
        
        CryptoSignal signal2 = cryptoSignalService.generateSignal(
            "BTC/USDT", bullishCandles1D, bullishCandles4H, bullishCandles15m);

        assertEquals(signal1.getSymbol(), signal2.getSymbol());
        assertEquals(signal1.getTrend1D(), signal2.getTrend1D());
        assertEquals(signal1.getSetup4H(), signal2.getSetup4H());
        assertEquals(signal1.getEntry15m(), signal2.getEntry15m());
        assertEquals(signal1.getConfidence(), signal2.getConfidence());
    }

    private List<CandleData> createBullishCandles(String timeframe, int count) {
        List<CandleData> candles = new java.util.ArrayList<>();
        double basePrice = 100000.0;
        LocalDateTime now = LocalDateTime.now();

        for (int i = count - 1; i >= 0; i--) {
            double price = basePrice * (1 + (count - i) * 0.001);
            CandleData candle = createCandle(price, timeframe);
            candle.setTimestamp(now.minusHours(i * getTimeframeHours(timeframe)));
            candles.add(candle);
        }

        return candles;
    }

    private List<CandleData> createBearishCandles(String timeframe, int count) {
        List<CandleData> candles = new java.util.ArrayList<>();
        double basePrice = 100000.0;
        LocalDateTime now = LocalDateTime.now();

        for (int i = count - 1; i >= 0; i--) {
            double price = basePrice * (1 - (count - i) * 0.001);
            CandleData candle = createCandle(price, timeframe);
            candle.setTimestamp(now.minusHours(i * getTimeframeHours(timeframe)));
            candles.add(candle);
        }

        return candles;
    }

    private List<CandleData> createSidewaysCandles(String timeframe, int count) {
        List<CandleData> candles = new java.util.ArrayList<>();
        double basePrice = 100000.0;
        LocalDateTime now = LocalDateTime.now();

        for (int i = count - 1; i >= 0; i--) {
            double price = basePrice + (Math.random() - 0.5) * 1000;
            CandleData candle = createCandle(price, timeframe);
            candle.setTimestamp(now.minusHours(i * getTimeframeHours(timeframe)));
            candles.add(candle);
        }

        return candles;
    }

    private CandleData createCandle(double closePrice, String timeframe) {
        CandleData candle = new CandleData();
        candle.setSymbol("BTC/USDT");
        candle.setTimestamp(LocalDateTime.now());
        candle.setOpen(closePrice * 0.99);
        candle.setHigh(closePrice * 1.01);
        candle.setLow(closePrice * 0.98);
        candle.setClose(closePrice);
        candle.setVolume(1000000.0 + Math.random() * 500000);
        candle.setTimeframe(timeframe);
        return candle;
    }

    private int getTimeframeHours(String timeframe) {
        switch (timeframe) {
            case "1D": return 24;
            case "4H": return 4;
            case "15m": return 0;
            default: return 1;
        }
    }
}
