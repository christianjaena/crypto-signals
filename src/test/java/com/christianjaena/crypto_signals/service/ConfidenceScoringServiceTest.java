package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.Setup;
import com.christianjaena.crypto_signals.model.Signal;
import com.christianjaena.crypto_signals.model.Trend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfidenceScoringServiceTest {

    @Mock
    private TechnicalIndicatorService technicalIndicatorService;

    @Mock
    private SetupZoneService setupZoneService;

    private ConfidenceScoringService confidenceScoringService;
    private List<CandleData> testCandles;

    @BeforeEach
    void setUp() {
        confidenceScoringService = new ConfidenceScoringService(technicalIndicatorService, setupZoneService);
        testCandles = createTestCandles();
    }

    @Test
    void calculateConfidence_HoldSignal_ReturnsZero() {
        int confidence = confidenceScoringService.calculateConfidence(
            testCandles, testCandles, testCandles, Trend.BULLISH, Setup.LONG, Signal.HOLD);

        assertEquals(0, confidence);
    }

    @Test
    void calculateConfidence_PerfectConditions_ReturnsHighConfidence() {
        when(setupZoneService.isPerfectSetup(anyDouble(), any(), any())).thenReturn(true);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(createStrongIndicators());

        int confidence = confidenceScoringService.calculateConfidence(
            testCandles, testCandles, testCandles, Trend.BULLISH, Setup.LONG, Signal.BUY);

        assertTrue(confidence > 80);
    }

    @Test
    void calculateConfidence_WeakConditions_ReturnsLowConfidence() {
        when(setupZoneService.isPerfectSetup(anyDouble(), any(), any())).thenReturn(false);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(createWeakIndicators());

        int confidence = confidenceScoringService.calculateConfidence(
            testCandles, testCandles, testCandles, Trend.BULLISH, Setup.LONG, Signal.BUY);

        assertTrue(confidence < 70);
    }

    @Test
    void calculateConfidence_NullCandles_ReturnsBaseConfidence() {
        int confidence = confidenceScoringService.calculateConfidence(
            null, null, null, Trend.BULLISH, Setup.LONG, Signal.BUY);

        assertEquals(50, confidence);
    }

    @Test
    void calculateConfidence_Over100_ReturnsCappedAt100() {
        when(setupZoneService.isPerfectSetup(anyDouble(), any(), any())).thenReturn(true);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(createVeryStrongIndicators());

        int confidence = confidenceScoringService.calculateConfidence(
            testCandles, testCandles, testCandles, Trend.BULLISH, Setup.LONG, Signal.BUY);

        assertEquals(100, confidence);
    }

    @Test
    void calculateConfidence_Under0_ReturnsCappedAt0() {
        when(setupZoneService.isPerfectSetup(anyDouble(), any(), any())).thenReturn(false);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(createVeryWeakIndicators());

        int confidence = confidenceScoringService.calculateConfidence(
            testCandles, testCandles, testCandles, Trend.BULLISH, Setup.LONG, Signal.BUY);

        assertEquals(0, confidence);
    }

    @Test
    void calculateHTFTrendBonus_StrongTrend_ReturnsHighBonus() {
        com.christianjaena.crypto_signals.model.TechnicalIndicators indicators = createStrongIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        int bonus = confidenceScoringService.calculateHTFTrendBonus(testCandles, Trend.BULLISH);

        assertEquals(20, bonus);
    }

    @Test
    void calculateHTFTrendBonus_SidewaysTrend_ReturnsZero() {
        int bonus = confidenceScoringService.calculateHTFTrendBonus(testCandles, Trend.SIDEWAYS);

        assertEquals(0, bonus);
    }

    @Test
    void calculateHTFTrendBonus_NullCandles_ReturnsZero() {
        int bonus = confidenceScoringService.calculateHTFTrendBonus(null, Trend.BULLISH);

        assertEquals(0, bonus);
    }

    @Test
    void calculateMTFSetupBonus_PerfectSetup_ReturnsHighBonus() {
        when(setupZoneService.isPerfectSetup(anyDouble(), any(), any())).thenReturn(true);

        int bonus = confidenceScoringService.calculateMTFSetupBonus(testCandles, Setup.LONG);

        assertEquals(15, bonus);
    }

    @Test
    void calculateMTFSetupBonus_NoneSetup_ReturnsZero() {
        int bonus = confidenceScoringService.calculateMTFSetupBonus(testCandles, Setup.NONE);

        assertEquals(0, bonus);
    }

    @Test
    void calculateLTFEntryBonus_StrongEntry_ReturnsHighBonus() {
        com.christianjaena.crypto_signals.model.TechnicalIndicators indicators = createStrongEntryIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        int bonus = confidenceScoringService.calculateLTFEntryBonus(testCandles, Signal.BUY);

        assertEquals(20, bonus);
    }

    @Test
    void calculateLTFEntryBonus_HoldSignal_ReturnsZero() {
        int bonus = confidenceScoringService.calculateLTFEntryBonus(testCandles, Signal.HOLD);

        assertEquals(0, bonus);
    }

    private com.christianjaena.crypto_signals.model.TechnicalIndicators createStrongIndicators() {
        com.christianjaena.crypto_signals.model.TechnicalIndicators indicators = 
            new com.christianjaena.crypto_signals.model.TechnicalIndicators();
        indicators.setEma50(105000.0);
        indicators.setEma200(100000.0);
        indicators.setRsi(60.0);
        indicators.setStochRsiK(30.0);
        indicators.setStochRsiD(20.0);
        indicators.setVolumeAverage20(1000000.0);
        return indicators;
    }

    private com.christianjaena.crypto_signals.model.TechnicalIndicators createWeakIndicators() {
        com.christianjaena.crypto_signals.model.TechnicalIndicators indicators = 
            new com.christianjaena.crypto_signals.model.TechnicalIndicators();
        indicators.setEma50(101000.0);
        indicators.setEma200(100000.0);
        indicators.setRsi(55.0);
        indicators.setStochRsiK(35.0);
        indicators.setStochRsiD(30.0);
        indicators.setVolumeAverage20(1000000.0);
        return indicators;
    }

    private com.christianjaena.crypto_signals.model.TechnicalIndicators createVeryStrongIndicators() {
        com.christianjaena.crypto_signals.model.TechnicalIndicators indicators = 
            new com.christianjaena.crypto_signals.model.TechnicalIndicators();
        indicators.setEma50(110000.0);
        indicators.setEma200(100000.0);
        indicators.setRsi(20.0);
        indicators.setStochRsiK(25.0);
        indicators.setStochRsiD(15.0);
        indicators.setVolumeAverage20(1000000.0);
        return indicators;
    }

    private com.christianjaena.crypto_signals.model.TechnicalIndicators createVeryWeakIndicators() {
        com.christianjaena.crypto_signals.model.TechnicalIndicators indicators = 
            new com.christianjaena.crypto_signals.model.TechnicalIndicators();
        indicators.setEma50(100500.0);
        indicators.setEma200(100000.0);
        indicators.setRsi(80.0);
        indicators.setStochRsiK(40.0);
        indicators.setStochRsiD(35.0);
        indicators.setVolumeAverage20(1000000.0);
        return indicators;
    }

    private com.christianjaena.crypto_signals.model.TechnicalIndicators createStrongEntryIndicators() {
        com.christianjaena.crypto_signals.model.TechnicalIndicators indicators = 
            new com.christianjaena.crypto_signals.model.TechnicalIndicators();
        indicators.setRsi(20.0);
        indicators.setStochRsiK(25.0);
        indicators.setStochRsiD(15.0);
        return indicators;
    }

    private List<CandleData> createTestCandles() {
        return Arrays.asList(
            createCandle(100000.0),
            createCandle(100500.0),
            createCandle(101000.0)
        );
    }

    private CandleData createCandle(double closePrice) {
        CandleData candle = new CandleData();
        candle.setSymbol("BTC/USDT");
        candle.setTimestamp(LocalDateTime.now());
        candle.setOpen(closePrice * 0.99);
        candle.setHigh(closePrice * 1.01);
        candle.setLow(closePrice * 0.98);
        candle.setClose(closePrice);
        candle.setVolume(1000000.0);
        candle.setTimeframe("15m");
        return candle;
    }
}
