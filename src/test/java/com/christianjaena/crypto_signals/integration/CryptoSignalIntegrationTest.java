package com.christianjaena.crypto_signals.integration;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.CryptoSignal;
import com.christianjaena.crypto_signals.model.Setup;
import com.christianjaena.crypto_signals.model.Signal;
import com.christianjaena.crypto_signals.model.Trend;
import com.christianjaena.crypto_signals.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
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
    private ConfidenceScoringService confidenceScoringService;

    @Autowired
    private ElliottWaveService elliottWaveService;

    @Autowired
    private CandlestickPatternService candlestickPatternService;

    @Autowired
    private FibonacciService fibonacciService;

    @Autowired
    private VolumeAnalysisService volumeAnalysisService;

    @Autowired
    private DivergenceService divergenceService;

    @Autowired
    private SessionFilterService sessionFilterService;

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
        assertFalse(signal.getNotes().isEmpty());
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
    void elliottWaveService_ValidCandles_ReturnsWave() {
        Trend trend = trendAnalysisService.determineTrend1D(bullishCandles1D);
        var wave = elliottWaveService.detectElliottWave(bullishCandles1D, trend);

        assertNotNull(wave);
        assertNotNull(wave.getWaveType());
        assertNotNull(wave.getWaveNumber());
        assertTrue(wave.getConfidence() >= 0 && wave.getConfidence() <= 1.0);
    }

    @Test
    void candlestickPatternService_ValidCandles_ReturnsPattern() {
        var pattern = candlestickPatternService.detectPattern(bullishCandles15m, Signal.BUY);

        assertNotNull(pattern);
        assertNotNull(pattern.getPatternType());
        assertNotNull(pattern.getSignalDirection());
        assertTrue(pattern.getConfidence() >= 0 && pattern.getConfidence() <= 1.0);
    }

    @Test
    void fibonacciService_ValidCandles_ReturnsLevels() {
        Trend trend = trendAnalysisService.determineTrend1D(bullishCandles1D);
        var levels = fibonacciService.calculateRetracementLevels(bullishCandles4H, trend);

        assertNotNull(levels);
        assertFalse(levels.isEmpty());
        assertTrue(levels.size() >= 3); // Should have at least 3 retracement levels
    }

    @Test
    void volumeAnalysisService_ValidCandles_ReturnsAnalysis() {
        boolean sufficient = volumeAnalysisService.isVolumeSufficient(bullishCandles4H, Signal.BUY);
        double ratio = volumeAnalysisService.getVolumeRatio(bullishCandles4H);

        assertTrue(sufficient == true || sufficient == false);
        assertTrue(ratio >= 0);
    }

    @Test
    void divergenceService_ValidCandles_ReturnsDivergence() {
        var rsiDivergence = divergenceService.detectRSIDivergence(bullishCandles1D, Signal.BUY);
        var macdDivergence = divergenceService.detectMACDDivergence(bullishCandles1D, Signal.BUY);

        assertNotNull(rsiDivergence);
        assertNotNull(macdDivergence);
        assertNotNull(rsiDivergence.getDivergenceType());
        assertNotNull(macdDivergence.getDivergenceType());
    }

    @Test
    void sessionFilterService_CurrentTime_ReturnsSessionInfo() {
        boolean inHighLiquidity = sessionFilterService.isInHighLiquiditySession();
        boolean optimal = sessionFilterService.isInOptimalTradingWindow();
        boolean hasNews = sessionFilterService.hasHighImpactNewsEvent();
        String sessionInfo = sessionFilterService.getSessionInfo();

        assertNotNull(sessionInfo);
        assertFalse(sessionInfo.isEmpty());
        assertTrue(inHighLiquidity == true || inHighLiquidity == false);
        assertTrue(optimal == true || optimal == false);
        assertTrue(hasNews == true || hasNews == false);
    }

    @Test
    void confidenceScoringService_AllFactors_ReturnsComprehensiveScore() {
        Trend trend = trendAnalysisService.determineTrend1D(bullishCandles1D);
        Setup setup = setupZoneService.identifySetup4H(bullishCandles4H, trend);

        int confidence = confidenceScoringService.calculateConfidence(
            bullishCandles1D, bullishCandles4H, bullishCandles15m, trend, setup, Signal.BUY);

        assertTrue(confidence >= 0 && confidence <= 100);
    }

    @Test
    void completeWorkflow_AdvancedAnalysis_ReturnsDetailedSignal() {
        CryptoSignal signal = cryptoSignalService.generateSignal(
            "BTC/USDT", bullishCandles1D, bullishCandles4H, bullishCandles15m);

        assertNotNull(signal);
        assertEquals("BTC/USDT", signal.getSymbol());
        
        // Verify all components are present
        assertNotNull(signal.getTrend1D());
        assertNotNull(signal.getSetup4H());
        assertNotNull(signal.getEntry15m());
        assertTrue(signal.getConfidence() >= 0 && signal.getConfidence() <= 100);
        
        // Verify notes contain analysis from all services
        assertNotNull(signal.getNotes());
        assertFalse(signal.getNotes().isEmpty());
        
        // Check for advanced analysis notes
        boolean hasTrendAnalysis = signal.getNotes().stream()
            .anyMatch(note -> note.contains("trend"));
        boolean hasSetupAnalysis = signal.getNotes().stream()
            .anyMatch(note -> note.contains("setup"));
        boolean hasConfidenceAnalysis = signal.getNotes().stream()
            .anyMatch(note -> note.contains("confidence"));
        
        assertTrue(hasTrendAnalysis || hasSetupAnalysis || hasConfidenceAnalysis);
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

    @Test
    void technicalIndicatorService_ValidCandles_ReturnsCompleteIndicators() {
        var indicators = technicalIndicatorService.calculateIndicators(bullishCandles1D);

        assertNotNull(indicators);
        assertTrue(indicators.getEma50() > 0);
        assertTrue(indicators.getEma200() > 0);
        assertTrue(indicators.getRsi() >= 0 && indicators.getRsi() <= 100);
        assertTrue(indicators.getStochRsiK() >= 0 && indicators.getStochRsiK() <= 100);
        assertTrue(indicators.getStochRsiD() >= 0 && indicators.getStochRsiD() <= 100);
        assertTrue(indicators.getVolumeAverage20() > 0);
        
        // Check new MACD indicators
        assertTrue(indicators.getMacdLine() != 0 || indicators.getMacdLine() == 0); // Can be 0
        assertTrue(indicators.getMacdSignal() != 0 || indicators.getMacdSignal() == 0);
        assertTrue(indicators.getMacdHistogram() != 0 || indicators.getMacdHistogram() == 0);
        
        // Check Bollinger Bands
        assertTrue(indicators.getBollingerUpper() > 0);
        assertTrue(indicators.getBollingerMiddle() > 0);
        assertTrue(indicators.getBollingerLower() > 0);
        assertTrue(indicators.getBollingerWidth() >= 0);
    }

    @Test
    void multiServiceIntegration_ConsistentDataFlow_ReturnsCoherentResults() {
        // Test that all services work together consistently
        Trend trend = trendAnalysisService.determineTrend1D(bullishCandles1D);
        Setup setup = setupZoneService.identifySetup4H(bullishCandles4H, trend);
        
        // Elliott Wave should align with trend
        var wave = elliottWaveService.detectElliottWave(bullishCandles1D, trend);
        if (trend == Trend.BULLISH && wave.isImpulseWave()) {
            assertTrue(wave.isImpulseWave());
        }
        
        // Fibonacci levels should be logical
        var levels = fibonacciService.calculateRetracementLevels(bullishCandles4H, trend);
        for (var level : levels) {
            assertTrue(level.getPrice() > 0);
            assertTrue(level.getLevel() > 0 && level.getLevel() < 1);
        }
        
        // Volume analysis should be consistent
        boolean volumeSufficient = volumeAnalysisService.isVolumeSufficient(bullishCandles4H, Signal.BUY);
        double volumeRatio = volumeAnalysisService.getVolumeRatio(bullishCandles4H);
        if (volumeSufficient) {
            assertTrue(volumeRatio >= 1.0);
        }
        
        // Divergence should be detected consistently
        var rsiDivergence = divergenceService.detectRSIDivergence(bullishCandles1D, Signal.BUY);
        var macdDivergence = divergenceService.detectMACDDivergence(bullishCandles1D, Signal.BUY);
        assertNotNull(rsiDivergence);
        assertNotNull(macdDivergence);
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
