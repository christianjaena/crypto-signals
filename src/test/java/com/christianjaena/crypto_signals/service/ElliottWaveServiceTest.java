package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.ElliottWave;
import com.christianjaena.crypto_signals.model.TechnicalIndicators;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElliottWaveServiceTest {

    @Mock
    private TechnicalIndicatorService technicalIndicatorService;

    private ElliottWaveService elliottWaveService;
    private List<CandleData> testCandles;

    @BeforeEach
    void setUp() {
        elliottWaveService = new ElliottWaveService(technicalIndicatorService);
        testCandles = createTestCandles();
    }

    @Test
    void detectElliottWave_BullishTrendWithStrongMomentum_ReturnsWave3() {
        TechnicalIndicators indicators = createStrongBullishIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        ElliottWave wave = elliottWaveService.detectElliottWave(testCandles, Trend.BULLISH);

        assertEquals(ElliottWave.WaveType.IMPULSE, wave.getWaveType());
        assertEquals(ElliottWave.WaveNumber.THREE, wave.getWaveNumber());
        assertTrue(wave.getConfidence() >= 0.7);
        assertTrue(wave.isStrongImpulseWave());
    }

    @Test
    void detectElliottWave_BearishTrendWithStrongMomentum_ReturnsWave3() {
        TechnicalIndicators indicators = createStrongBearishIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        // Create bearish candles for this test
        List<CandleData> bearishCandles = createBearishTestCandles();
        ElliottWave wave = elliottWaveService.detectElliottWave(bearishCandles, Trend.BEARISH);

        assertEquals(ElliottWave.WaveType.IMPULSE, wave.getWaveType());
        assertEquals(ElliottWave.WaveNumber.THREE, wave.getWaveNumber());
        assertTrue(wave.getConfidence() >= 0.7);
        assertTrue(wave.isStrongImpulseWave());
    }

    @Test
    void detectElliottWave_BullishTrendWithDivergence_ReturnsWave5() {
        TechnicalIndicators indicators = createDivergentBullishIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        ElliottWave wave = elliottWaveService.detectElliottWave(testCandles, Trend.BULLISH);

        assertEquals(ElliottWave.WaveType.IMPULSE, wave.getWaveType());
        assertEquals(ElliottWave.WaveNumber.FIVE, wave.getWaveNumber());
        assertTrue(wave.getConfidence() >= 0.6);
    }

    @Test
    void detectElliottWave_SidewaysTrend_ReturnsUnknown() {
        ElliottWave wave = elliottWaveService.detectElliottWave(testCandles, Trend.SIDEWAYS);

        assertEquals(ElliottWave.WaveType.UNKNOWN, wave.getWaveType());
        assertEquals(ElliottWave.WaveNumber.UNKNOWN, wave.getWaveNumber());
        assertEquals(0.0, wave.getConfidence());
    }

    @Test
    void detectElliottWave_NullCandles_ReturnsUnknown() {
        ElliottWave wave = elliottWaveService.detectElliottWave(null, Trend.BULLISH);

        assertEquals(ElliottWave.WaveType.UNKNOWN, wave.getWaveType());
        assertEquals(ElliottWave.WaveNumber.UNKNOWN, wave.getWaveNumber());
        assertEquals(0.0, wave.getConfidence());
    }

    @Test
    void detectElliottWave_InsufficientCandles_ReturnsUnknown() {
        List<CandleData> insufficientCandles = testCandles.subList(0, 5);
        ElliottWave wave = elliottWaveService.detectElliottWave(insufficientCandles, Trend.BULLISH);

        assertEquals(ElliottWave.WaveType.UNKNOWN, wave.getWaveType());
        assertEquals(ElliottWave.WaveNumber.UNKNOWN, wave.getWaveNumber());
        assertEquals(0.0, wave.getConfidence());
    }

    @Test
    void shouldSkipSignal_CorrectiveWave_ReturnsTrue() {
        ElliottWave correctiveWave = ElliottWave.createCorrectiveWave(
            ElliottWave.WaveNumber.A, 0.8);

        assertTrue(elliottWaveService.shouldSkipSignal(correctiveWave, Trend.BULLISH));
    }

    @Test
    void shouldSkipSignal_WeakImpulseWave_ReturnsTrue() {
        ElliottWave weakWave = ElliottWave.createImpulseWave(
            ElliottWave.WaveNumber.ONE, 0.5);

        assertTrue(elliottWaveService.shouldSkipSignal(weakWave, Trend.BULLISH));
    }

    @Test
    void shouldSkipSignal_StrongImpulseWave_ReturnsFalse() {
        ElliottWave strongWave = ElliottWave.createImpulseWave(
            ElliottWave.WaveNumber.THREE, 0.8);

        assertFalse(elliottWaveService.shouldSkipSignal(strongWave, Trend.BULLISH));
    }

    @Test
    void getConfidenceBonus_StrongImpulseWave_Returns15() {
        ElliottWave strongWave = ElliottWave.createImpulseWave(
            ElliottWave.WaveNumber.THREE, 0.8);

        int bonus = elliottWaveService.getConfidenceBonus(strongWave, Trend.BULLISH);

        assertEquals(15, bonus);
    }

    @Test
    void getConfidenceBonus_RegularImpulseWave_Returns10() {
        ElliottWave regularWave = ElliottWave.createImpulseWave(
            ElliottWave.WaveNumber.ONE, 0.7);

        int bonus = elliottWaveService.getConfidenceBonus(regularWave, Trend.BULLISH);

        assertEquals(10, bonus);
    }

    @Test
    void getConfidenceBonus_CorrectiveWave_Returns0() {
        ElliottWave correctiveWave = ElliottWave.createCorrectiveWave(
            ElliottWave.WaveNumber.A, 0.8);

        int bonus = elliottWaveService.getConfidenceBonus(correctiveWave, Trend.BULLISH);

        assertEquals(0, bonus);
    }

    @Test
    void getConfidenceBonus_UnknownWave_Returns0() {
        ElliottWave unknownWave = ElliottWave.createUnknown();

        int bonus = elliottWaveService.getConfidenceBonus(unknownWave, Trend.BULLISH);

        assertEquals(0, bonus);
    }

    private TechnicalIndicators createStrongBullishIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setRsi(65.0); // Strong momentum but not overbought
        return indicators;
    }

    private TechnicalIndicators createStrongBearishIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setRsi(35.0); // Strong momentum but not oversold
        return indicators;
    }

    private TechnicalIndicators createDivergentBullishIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setRsi(55.0); // Moderate RSI suggesting divergence
        return indicators;
    }

    private List<CandleData> createTestCandles() {
        return Arrays.asList(
            createCandle(100000.0),
            createCandle(101000.0),
            createCandle(102000.0),
            createCandle(103000.0),
            createCandle(104000.0),
            createCandle(105000.0),
            createCandle(106000.0),
            createCandle(107000.0),
            createCandle(108000.0),
            createCandle(109000.0),
            createCandle(110000.0),
            createCandle(111000.0),
            createCandle(112000.0),
            createCandle(113000.0),
            createCandle(114000.0),
            createCandle(115000.0),
            createCandle(116000.0),
            createCandle(117000.0),
            createCandle(118000.0),
            createCandle(119000.0),
            createCandle(120000.0)
        );
    }

    private List<CandleData> createBearishTestCandles() {
        return Arrays.asList(
            createCandle(120000.0),
            createCandle(119000.0),
            createCandle(118000.0),
            createCandle(117000.0),
            createCandle(116000.0),
            createCandle(115000.0),
            createCandle(114000.0),
            createCandle(113000.0),
            createCandle(112000.0),
            createCandle(110000.0), // Strong bearish move
            createCandle(108000.0),
            createCandle(105000.0),
            createCandle(100000.0)
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
        candle.setTimeframe("1D");
        return candle;
    }
}
