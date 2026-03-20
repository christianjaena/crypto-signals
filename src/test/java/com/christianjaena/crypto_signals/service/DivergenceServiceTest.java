package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.Divergence;
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
class DivergenceServiceTest {

    @Mock
    private TechnicalIndicatorService technicalIndicatorService;

    private DivergenceService divergenceService;
    private List<CandleData> testCandles;

    @BeforeEach
    void setUp() {
        divergenceService = new DivergenceService(technicalIndicatorService);
        testCandles = createTestCandles();
    }

    @Test
    void detectRSIDivergence_BullishDivergence_ReturnsBullishDivergence() {
        List<CandleData> bullishDivergenceCandles = createBullishRSIDivergenceCandles();
        TechnicalIndicators indicators = createIndicatorsWithRSI(45.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Divergence divergence = divergenceService.detectRSIDivergence(bullishDivergenceCandles, Signal.BUY);

        assertEquals(Divergence.DivergenceType.RSI_BULLISH, divergence.getDivergenceType());
        assertTrue(divergence.isBullishDivergence());
        assertTrue(divergence.hasDivergence());
        assertTrue(divergence.getConfidence() > 0.5);
    }

    @Test
    void detectRSIDivergence_BearishDivergence_ReturnsBearishDivergence() {
        List<CandleData> bearishDivergenceCandles = createBearishRSIDivergenceCandles();
        TechnicalIndicators indicators = createIndicatorsWithRSI(55.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Divergence divergence = divergenceService.detectRSIDivergence(bearishDivergenceCandles, Signal.SELL);

        assertEquals(Divergence.DivergenceType.RSI_BEARISH, divergence.getDivergenceType());
        assertTrue(divergence.isBearishDivergence());
        assertTrue(divergence.hasDivergence());
        assertTrue(divergence.getConfidence() > 0.5);
    }

    @Test
    void detectRSIDivergence_NoDivergence_ReturnsNone() {
        TechnicalIndicators indicators = createIndicatorsWithRSI(50.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Divergence divergence = divergenceService.detectRSIDivergence(testCandles, Signal.BUY);

        assertEquals(Divergence.DivergenceType.NONE, divergence.getDivergenceType());
        assertFalse(divergence.hasDivergence());
        assertEquals(0.0, divergence.getConfidence());
    }

    @Test
    void detectRSIDivergence_NullCandles_ReturnsNone() {
        Divergence divergence = divergenceService.detectRSIDivergence(null, Signal.BUY);

        assertEquals(Divergence.DivergenceType.NONE, divergence.getDivergenceType());
        assertFalse(divergence.hasDivergence());
        assertEquals(0.0, divergence.getConfidence());
    }

    @Test
    void detectRSIDivergence_InsufficientCandles_ReturnsNone() {
        List<CandleData> insufficientCandles = testCandles.subList(0, 10);
        Divergence divergence = divergenceService.detectRSIDivergence(insufficientCandles, Signal.BUY);

        assertEquals(Divergence.DivergenceType.NONE, divergence.getDivergenceType());
        assertFalse(divergence.hasDivergence());
        assertEquals(0.0, divergence.getConfidence());
    }

    @Test
    void detectMACDDivergence_BullishDivergence_ReturnsBullishDivergence() {
        List<CandleData> bullishDivergenceCandles = createBullishMACDDivergenceCandles();
        TechnicalIndicators indicators = createIndicatorsWithMACDHistogram(0.001);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Divergence divergence = divergenceService.detectMACDDivergence(bullishDivergenceCandles, Signal.BUY);

        assertEquals(Divergence.DivergenceType.MACD_BULLISH, divergence.getDivergenceType());
        assertTrue(divergence.isBullishDivergence());
        assertTrue(divergence.hasDivergence());
        assertTrue(divergence.getConfidence() > 0.5);
    }

    @Test
    void detectMACDDivergence_BearishDivergence_ReturnsBearishDivergence() {
        List<CandleData> bearishDivergenceCandles = createBearishMACDDivergenceCandles();
        TechnicalIndicators indicators = createIndicatorsWithMACDHistogram(-0.001);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Divergence divergence = divergenceService.detectMACDDivergence(bearishDivergenceCandles, Signal.SELL);

        assertEquals(Divergence.DivergenceType.MACD_BEARISH, divergence.getDivergenceType());
        assertTrue(divergence.isBearishDivergence());
        assertTrue(divergence.hasDivergence());
        assertTrue(divergence.getConfidence() > 0.5);
    }

    @Test
    void detectMACDDivergence_NoDivergence_ReturnsNone() {
        TechnicalIndicators indicators = createIndicatorsWithMACDHistogram(0.0);
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Divergence divergence = divergenceService.detectMACDDivergence(testCandles, Signal.BUY);

        assertEquals(Divergence.DivergenceType.NONE, divergence.getDivergenceType());
        assertFalse(divergence.hasDivergence());
        assertEquals(0.0, divergence.getConfidence());
    }

    @Test
    void getConfidenceBonus_BothDivergencesAligned_Returns5() {
        Divergence rsiDivergence = new Divergence(
            Divergence.DivergenceType.RSI_BULLISH,
            Divergence.DivergenceStrength.STRONG,
            0.8,
            "Strong bullish RSI divergence"
        );
        Divergence macdDivergence = new Divergence(
            Divergence.DivergenceType.MACD_BULLISH,
            Divergence.DivergenceStrength.MODERATE,
            0.6,
            "Moderate bullish MACD divergence"
        );

        int bonus = divergenceService.getConfidenceBonus(rsiDivergence, macdDivergence, Signal.BUY);

        assertEquals(5, bonus); // 3 (RSI strong) + 2 (MACD moderate)
    }

    @Test
    void getConfidenceBonus_OnlyRSIDivergenceAligned_Returns3() {
        Divergence rsiDivergence = new Divergence(
            Divergence.DivergenceType.RSI_BULLISH,
            Divergence.DivergenceStrength.STRONG,
            0.8,
            "Strong bullish RSI divergence"
        );
        Divergence macdDivergence = Divergence.createNone();

        int bonus = divergenceService.getConfidenceBonus(rsiDivergence, macdDivergence, Signal.BUY);

        assertEquals(3, bonus);
    }

    @Test
    void getConfidenceBonus_MisalignedDivergences_Returns0() {
        Divergence rsiDivergence = new Divergence(
            Divergence.DivergenceType.RSI_BULLISH,
            Divergence.DivergenceStrength.STRONG,
            0.8,
            "Strong bullish RSI divergence"
        );
        Divergence macdDivergence = new Divergence(
            Divergence.DivergenceType.MACD_BEARISH,
            Divergence.DivergenceStrength.STRONG,
            0.8,
            "Strong bearish MACD divergence"
        );

        int bonus = divergenceService.getConfidenceBonus(rsiDivergence, macdDivergence, Signal.SELL);

        assertEquals(0, bonus); // RSI is misaligned, MACD is aligned but RSI gets 0
    }

    @Test
    void getConfidenceBonus_NoDivergences_Returns0() {
        Divergence rsiDivergence = Divergence.createNone();
        Divergence macdDivergence = Divergence.createNone();

        int bonus = divergenceService.getConfidenceBonus(rsiDivergence, macdDivergence, Signal.BUY);

        assertEquals(0, bonus);
    }

    @Test
    void getConfidenceBonus_CappedAtMaximum_Returns5() {
        Divergence rsiDivergence = new Divergence(
            Divergence.DivergenceType.RSI_BULLISH,
            Divergence.DivergenceStrength.VERY_STRONG,
            0.9,
            "Very strong bullish RSI divergence"
        );
        Divergence macdDivergence = new Divergence(
            Divergence.DivergenceType.MACD_BULLISH,
            Divergence.DivergenceStrength.VERY_STRONG,
            0.9,
            "Very strong bullish MACD divergence"
        );

        int bonus = divergenceService.getConfidenceBonus(rsiDivergence, macdDivergence, Signal.BUY);

        assertEquals(5, bonus); // Capped at 5
    }

    private TechnicalIndicators createIndicatorsWithRSI(double rsi) {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setRsi(rsi);
        indicators.setMacdLine(0.0);
        indicators.setMacdSignal(0.0);
        indicators.setMacdHistogram(0.0);
        return indicators;
    }

    private TechnicalIndicators createIndicatorsWithMACDHistogram(double histogram) {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setRsi(50.0);
        indicators.setMacdLine(0.001);
        indicators.setMacdSignal(0.0);
        indicators.setMacdHistogram(histogram);
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
            createCandle(120000.0),
            createCandle(121000.0),
            createCandle(122000.0),
            createCandle(123000.0),
            createCandle(124000.0),
            createCandle(125000.0)
        );
    }

    private List<CandleData> createBullishRSIDivergenceCandles() {
        // Create candles that show lower lows in price but higher lows in RSI
        return Arrays.asList(
            createCandle(120000.0), // Higher low
            createCandle(119000.0),
            createCandle(118000.0),
            createCandle(117000.0),
            createCandle(116000.0),
            createCandle(115000.0),
            createCandle(114000.0),
            createCandle(113000.0),
            createCandle(112000.0),
            createCandle(111000.0),
            createCandle(110000.0), // Lower low price
            createCandle(109000.0),
            createCandle(108000.0),
            createCandle(107000.0),
            createCandle(106000.0),
            createCandle(105000.0),
            createCandle(104000.0),
            createCandle(103000.0),
            createCandle(102000.0),
            createCandle(101000.0),
            createCandle(100000.0),
            createCandle(99000.0),
            createCandle(98000.0),
            createCandle(97000.0),
            createCandle(96000.0),
            createCandle(95000.0)
        );
    }

    private List<CandleData> createBearishRSIDivergenceCandles() {
        // Create candles that show higher highs in price but lower highs in RSI
        return Arrays.asList(
            createCandle(100000.0), // Lower high
            createCandle(101000.0),
            createCandle(102000.0),
            createCandle(103000.0),
            createCandle(104000.0),
            createCandle(105000.0),
            createCandle(106000.0),
            createCandle(107000.0),
            createCandle(108000.0),
            createCandle(109000.0),
            createCandle(110000.0), // Higher high price
            createCandle(111000.0),
            createCandle(112000.0),
            createCandle(113000.0),
            createCandle(114000.0),
            createCandle(115000.0),
            createCandle(116000.0),
            createCandle(117000.0),
            createCandle(118000.0),
            createCandle(119000.0),
            createCandle(120000.0),
            createCandle(121000.0),
            createCandle(122000.0),
            createCandle(123000.0),
            createCandle(124000.0),
            createCandle(125000.0)
        );
    }

    private List<CandleData> createBullishMACDDivergenceCandles() {
        // Similar to RSI divergence but for MACD
        return createBullishRSIDivergenceCandles();
    }

    private List<CandleData> createBearishMACDDivergenceCandles() {
        // Similar to RSI divergence but for MACD
        return createBearishRSIDivergenceCandles();
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
