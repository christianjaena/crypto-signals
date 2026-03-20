package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.Signal;
import com.christianjaena.crypto_signals.model.TechnicalIndicators;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolumeAnalysisServiceTest {

    @Mock
    private TechnicalIndicatorService technicalIndicatorService;

    private VolumeAnalysisService volumeAnalysisService;
    private List<CandleData> testCandles;

    @BeforeEach
    void setUp() {
        volumeAnalysisService = new VolumeAnalysisService(technicalIndicatorService);
        testCandles = createTestCandles();
    }

    @Test
    void isVolumeSufficient_BuySignalWithSufficientVolume_ReturnsTrue() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean sufficient = volumeAnalysisService.isVolumeSufficient(testCandles, Signal.BUY);

        assertTrue(sufficient);
    }

    @Test
    void isVolumeSufficient_BuySignalWithInsufficientVolume_ReturnsFalse() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(2000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean sufficient = volumeAnalysisService.isVolumeSufficient(testCandles, Signal.BUY);

        assertFalse(sufficient);
    }

    @Test
    void isVolumeSufficient_SellSignalWithSufficientVolume_ReturnsTrue() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(900000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean sufficient = volumeAnalysisService.isVolumeSufficient(testCandles, Signal.SELL);

        assertTrue(sufficient);
    }

    @Test
    void isVolumeSufficient_NullCandles_ReturnsFalse() {
        boolean sufficient = volumeAnalysisService.isVolumeSufficient(null, Signal.BUY);

        assertFalse(sufficient);
    }

    @Test
    void isVolumeSufficient_EmptyCandles_ReturnsFalse() {
        boolean sufficient = volumeAnalysisService.isVolumeSufficient(Arrays.asList(), Signal.BUY);

        assertFalse(sufficient);
    }

    @Test
    void hasVolumeSpike_VolumeSpikeDetected_ReturnsTrue() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean hasSpike = volumeAnalysisService.hasVolumeSpike(testCandles, 1.5);

        assertTrue(hasSpike);
    }

    @Test
    void hasVolumeSpike_NoVolumeSpike_ReturnsFalse() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(2000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean hasSpike = volumeAnalysisService.hasVolumeSpike(testCandles, 1.5);

        assertFalse(hasSpike);
    }

    @Test
    void hasIncreasingVolume_IncreasingVolume_ReturnsTrue() {
        List<CandleData> increasingVolumeCandles = createIncreasingVolumeCandles();

        boolean increasing = volumeAnalysisService.hasIncreasingVolume(increasingVolumeCandles, 3);

        assertTrue(increasing);
    }

    @Test
    void hasIncreasingVolume_NotIncreasingVolume_ReturnsFalse() {
        boolean increasing = volumeAnalysisService.hasIncreasingVolume(testCandles, 3);

        assertFalse(increasing);
    }

    @Test
    void hasVolumeConfirmation_BuySignalWithConfirmation_ReturnsTrue() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean confirmed = volumeAnalysisService.hasVolumeConfirmation(testCandles, Signal.BUY);

        assertTrue(confirmed);
    }

    @Test
    void hasVolumeConfirmation_SellSignalWithConfirmation_ReturnsTrue() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean confirmed = volumeAnalysisService.hasVolumeConfirmation(testCandles, Signal.SELL);

        assertTrue(confirmed);
    }

    @Test
    void hasVolumeConfirmation_InsufficientVolume_ReturnsFalse() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(2000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean confirmed = volumeAnalysisService.hasVolumeConfirmation(testCandles, Signal.BUY);

        assertFalse(confirmed);
    }

    @Test
    void getVolumeRatio_NormalVolume_ReturnsCorrectRatio() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        double ratio = volumeAnalysisService.getVolumeRatio(testCandles);

        assertEquals(1.5, ratio, 0.01);
    }

    @Test
    void getVolumeRatio_NullCandles_ReturnsZero() {
        double ratio = volumeAnalysisService.getVolumeRatio(null);

        assertEquals(0.0, ratio);
    }

    @Test
    void isVolumeDiverging_BullishDivergence_ReturnsTrue() {
        List<CandleData> divergingCandles = createBullishDivergenceCandles();
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean diverging = volumeAnalysisService.isVolumeDiverging(divergingCandles, Signal.BUY);

        assertTrue(diverging);
    }

    @Test
    void isVolumeDiverging_NoDivergence_ReturnsFalse() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean diverging = volumeAnalysisService.isVolumeDiverging(testCandles, Signal.BUY);

        assertFalse(diverging);
    }

    @Test
    void getConfidenceBonus_StrongConfirmation_Returns10() {
        List<CandleData> strongVolumeCandles = createStrongVolumeCandles();
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        int bonus = volumeAnalysisService.getConfidenceBonus(strongVolumeCandles, Signal.BUY);

        assertEquals(10, bonus);
    }

    @Test
    void getConfidenceBonus_WeakConfirmation_Returns5() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        int bonus = volumeAnalysisService.getConfidenceBonus(testCandles, Signal.BUY);

        assertEquals(5, bonus);
    }

    @Test
    void getConfidenceBonus_NoConfirmation_Returns0() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(2000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        int bonus = volumeAnalysisService.getConfidenceBonus(testCandles, Signal.BUY);

        assertEquals(0, bonus);
    }

    @Test
    void getConfidenceBonus_WithDivergence_Returns2() {
        List<CandleData> divergingCandles = createBullishDivergenceCandles();
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        int bonus = volumeAnalysisService.getConfidenceBonus(divergingCandles, Signal.BUY);

        assertEquals(2, bonus); // 5 base - 3 divergence penalty
    }

    @Test
    void shouldSkipSignal_InsufficientVolume_ReturnsTrue() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(2000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean shouldSkip = volumeAnalysisService.shouldSkipSignal(testCandles, Signal.BUY);

        assertTrue(shouldSkip);
    }

    @Test
    void shouldSkipSignal_SufficientVolume_ReturnsFalse() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean shouldSkip = volumeAnalysisService.shouldSkipSignal(testCandles, Signal.BUY);

        assertFalse(shouldSkip);
    }

    @Test
    void shouldSkipSignal_WithDivergence_ReturnsTrue() {
        List<CandleData> divergingCandles = createBullishDivergenceCandles();
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean shouldSkip = volumeAnalysisService.shouldSkipSignal(divergingCandles, Signal.BUY);

        assertTrue(shouldSkip);
    }

    @Test
    void getVolumeAnalysis_ComprehensiveAnalysis_ReturnsDetailedString() {
        TechnicalIndicators indicators = createIndicatorsWithVolumeAverage(1000000.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        String analysis = volumeAnalysisService.getVolumeAnalysis(testCandles, Signal.BUY);

        assertNotNull(analysis);
        assertTrue(analysis.contains("Volume ratio"));
        assertTrue(analysis.contains("Volume spike detected"));
        assertTrue(analysis.contains("Volume confirms signal"));
    }

    private TechnicalIndicators createIndicatorsWithVolumeAverage(double volumeAverage) {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setVolumeAverage20(volumeAverage);
        return indicators;
    }

    private List<CandleData> createTestCandles() {
        return Arrays.asList(
            createCandle(100000.0, 800000.0),
            createCandle(101000.0, 900000.0),
            createCandle(102000.0, 1000000.0),
            createCandle(103000.0, 1100000.0),
            createCandle(104000.0, 1200000.0),
            createCandle(105000.0, 1300000.0),
            createCandle(106000.0, 1400000.0),
            createCandle(107000.0, 1500000.0)
        );
    }

    private List<CandleData> createIncreasingVolumeCandles() {
        return Arrays.asList(
            createCandle(100000.0, 800000.0),
            createCandle(101000.0, 900000.0),
            createCandle(102000.0, 1000000.0),
            createCandle(103000.0, 1100000.0),
            createCandle(104000.0, 1200000.0)
        );
    }

    private List<CandleData> createStrongVolumeCandles() {
        return Arrays.asList(
            createCandle(100000.0, 800000.0),
            createCandle(101000.0, 900000.0),
            createCandle(102000.0, 1000000.0),
            createCandle(103000.0, 1500000.0), // Volume spike
            createCandle(104000.0, 2000000.0), // Increasing volume
            createCandle(105000.0, 2500000.0),
            createCandle(106000.0, 3000000.0),
            createCandle(107000.0, 3500000.0)
        );
    }

    private List<CandleData> createBullishDivergenceCandles() {
        return Arrays.asList(
            createCandle(100000.0, 1500000.0), // High volume
            createCandle(101000.0, 1400000.0),
            createCandle(102000.0, 1300000.0),
            createCandle(103000.0, 1200000.0),
            createCandle(104000.0, 1100000.0),
            createCandle(105000.0, 1000000.0),
            createCandle(106000.0, 900000.0),
            createCandle(107000.0, 800000.0), // Lower volume despite higher price
            createCandle(108000.0, 700000.0),
            createCandle(109000.0, 600000.0)
        );
    }

    private CandleData createCandle(double closePrice, double volume) {
        CandleData candle = new CandleData();
        candle.setSymbol("BTC/USDT");
        candle.setTimestamp(LocalDateTime.now());
        candle.setOpen(closePrice * 0.99);
        candle.setHigh(closePrice * 1.01);
        candle.setLow(closePrice * 0.98);
        candle.setClose(closePrice);
        candle.setVolume(volume);
        candle.setTimeframe("4H");
        return candle;
    }
}
