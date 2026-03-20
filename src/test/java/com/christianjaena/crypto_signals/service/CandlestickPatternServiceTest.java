package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.CandlestickPattern;
import com.christianjaena.crypto_signals.model.Signal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CandlestickPatternServiceTest {

    private CandlestickPatternService candlestickPatternService;
    private List<CandleData> testCandles;

    @BeforeEach
    void setUp() {
        candlestickPatternService = new CandlestickPatternService();
        testCandles = createTestCandles();
    }

    @Test
    void detectPattern_BullishEngulfingPattern_ReturnsBullishEngulfing() {
        List<CandleData> bullishEngulfingCandles = createBullishEngulfingCandles();

        CandlestickPattern pattern = candlestickPatternService.detectPattern(bullishEngulfingCandles, Signal.BUY);

        assertEquals(CandlestickPattern.PatternType.BULLISH_ENGULFING, pattern.getPatternType());
        assertEquals(CandlestickPattern.SignalDirection.BULLISH, pattern.getSignalDirection());
        assertTrue(pattern.getConfidence() > 0.5);
        assertTrue(pattern.isBullish());
        assertTrue(pattern.isReversalPattern());
    }

    @Test
    void detectPattern_BearishEngulfingPattern_ReturnsBearishEngulfing() {
        List<CandleData> bearishEngulfingCandles = createBearishEngulfingCandles();

        CandlestickPattern pattern = candlestickPatternService.detectPattern(bearishEngulfingCandles, Signal.SELL);

        assertEquals(CandlestickPattern.PatternType.BEARISH_ENGULFING, pattern.getPatternType());
        assertEquals(CandlestickPattern.SignalDirection.BEARISH, pattern.getSignalDirection());
        assertTrue(pattern.getConfidence() > 0.5);
        assertTrue(pattern.isBearish());
        assertTrue(pattern.isReversalPattern());
    }

    @Test
    void detectPattern_HammerPattern_ReturnsHammer() {
        List<CandleData> hammerCandles = createHammerCandles();

        CandlestickPattern pattern = candlestickPatternService.detectPattern(hammerCandles, Signal.BUY);

        assertEquals(CandlestickPattern.PatternType.HAMMER, pattern.getPatternType());
        assertEquals(CandlestickPattern.SignalDirection.BULLISH, pattern.getSignalDirection());
        assertTrue(pattern.getConfidence() > 0.5);
        assertTrue(pattern.isBullish());
    }

    @Test
    void detectPattern_ShootingStarPattern_ReturnsShootingStar() {
        List<CandleData> shootingStarCandles = createShootingStarCandles();

        CandlestickPattern pattern = candlestickPatternService.detectPattern(shootingStarCandles, Signal.SELL);

        assertEquals(CandlestickPattern.PatternType.SHOOTING_STAR, pattern.getPatternType());
        assertEquals(CandlestickPattern.SignalDirection.BEARISH, pattern.getSignalDirection());
        assertTrue(pattern.getConfidence() > 0.5);
        assertTrue(pattern.isBearish());
    }

    @Test
    void detectPattern_MorningStarPattern_ReturnsMorningStar() {
        List<CandleData> morningStarCandles = createMorningStarCandles();

        CandlestickPattern pattern = candlestickPatternService.detectPattern(morningStarCandles, Signal.BUY);

        assertEquals(CandlestickPattern.PatternType.MORNING_STAR, pattern.getPatternType());
        assertEquals(CandlestickPattern.SignalDirection.BULLISH, pattern.getSignalDirection());
        assertTrue(pattern.getConfidence() > 0.5);
        assertTrue(pattern.isBullish());
        assertTrue(pattern.isReversalPattern());
    }

    @Test
    void detectPattern_EveningStarPattern_ReturnsEveningStar() {
        List<CandleData> eveningStarCandles = createEveningStarCandles();

        CandlestickPattern pattern = candlestickPatternService.detectPattern(eveningStarCandles, Signal.SELL);

        assertEquals(CandlestickPattern.PatternType.EVENING_STAR, pattern.getPatternType());
        assertEquals(CandlestickPattern.SignalDirection.BEARISH, pattern.getSignalDirection());
        assertTrue(pattern.getConfidence() > 0.5);
        assertTrue(pattern.isBearish());
        assertTrue(pattern.isReversalPattern());
    }

    @Test
    void detectPattern_NoClearPattern_ReturnsUnknown() {
        CandlestickPattern pattern = candlestickPatternService.detectPattern(testCandles, Signal.BUY);

        assertEquals(CandlestickPattern.PatternType.UNKNOWN, pattern.getPatternType());
        assertEquals(CandlestickPattern.SignalDirection.NEUTRAL, pattern.getSignalDirection());
        assertEquals(0.0, pattern.getConfidence());
    }

    @Test
    void detectPattern_NullCandles_ReturnsUnknown() {
        CandlestickPattern pattern = candlestickPatternService.detectPattern(null, Signal.BUY);

        assertEquals(CandlestickPattern.PatternType.UNKNOWN, pattern.getPatternType());
        assertEquals(CandlestickPattern.SignalDirection.NEUTRAL, pattern.getSignalDirection());
        assertEquals(0.0, pattern.getConfidence());
    }

    @Test
    void detectPattern_InsufficientCandles_ReturnsUnknown() {
        List<CandleData> insufficientCandles = testCandles.subList(0, 2);
        CandlestickPattern pattern = candlestickPatternService.detectPattern(insufficientCandles, Signal.BUY);

        assertEquals(CandlestickPattern.PatternType.UNKNOWN, pattern.getPatternType());
        assertEquals(CandlestickPattern.SignalDirection.NEUTRAL, pattern.getSignalDirection());
        assertEquals(0.0, pattern.getConfidence());
    }

    @Test
    void getConfidenceBonus_StrongAlignedPattern_Returns10() {
        CandlestickPattern strongPattern = new CandlestickPattern(
            CandlestickPattern.PatternType.BULLISH_ENGULFING,
            CandlestickPattern.SignalDirection.BULLISH,
            0.8,
            "Strong bullish engulfing"
        );

        int bonus = candlestickPatternService.getConfidenceBonus(strongPattern, Signal.BUY);

        assertEquals(10, bonus);
    }

    @Test
    void getConfidenceBonus_WeakAlignedPattern_Returns5() {
        CandlestickPattern weakPattern = new CandlestickPattern(
            CandlestickPattern.PatternType.HAMMER,
            CandlestickPattern.SignalDirection.BULLISH,
            0.6,
            "Weak hammer"
        );

        int bonus = candlestickPatternService.getConfidenceBonus(weakPattern, Signal.BUY);

        assertEquals(5, bonus);
    }

    @Test
    void getConfidenceBonus_MisalignedPattern_Returns0() {
        CandlestickPattern bullishPattern = new CandlestickPattern(
            CandlestickPattern.PatternType.BULLISH_ENGULFING,
            CandlestickPattern.SignalDirection.BULLISH,
            0.8,
            "Bullish engulfing"
        );

        int bonus = candlestickPatternService.getConfidenceBonus(bullishPattern, Signal.SELL);

        assertEquals(0, bonus);
    }

    @Test
    void getConfidenceBonus_UnknownPattern_Returns0() {
        CandlestickPattern unknownPattern = CandlestickPattern.createUnknown();

        int bonus = candlestickPatternService.getConfidenceBonus(unknownPattern, Signal.BUY);

        assertEquals(0, bonus);
    }

    @Test
    void confirmsSignal_BullishPatternWithBuySignal_ReturnsTrue() {
        CandlestickPattern bullishPattern = new CandlestickPattern(
            CandlestickPattern.PatternType.BULLISH_ENGULFING,
            CandlestickPattern.SignalDirection.BULLISH,
            0.8,
            "Bullish engulfing"
        );

        assertTrue(candlestickPatternService.confirmsSignal(bullishPattern, Signal.BUY));
    }

    @Test
    void confirmsSignal_BearishPatternWithSellSignal_ReturnsTrue() {
        CandlestickPattern bearishPattern = new CandlestickPattern(
            CandlestickPattern.PatternType.BEARISH_ENGULFING,
            CandlestickPattern.SignalDirection.BEARISH,
            0.8,
            "Bearish engulfing"
        );

        assertTrue(candlestickPatternService.confirmsSignal(bearishPattern, Signal.SELL));
    }

    @Test
    void confirmsSignal_MisalignedPattern_ReturnsFalse() {
        CandlestickPattern bullishPattern = new CandlestickPattern(
            CandlestickPattern.PatternType.BULLISH_ENGULFING,
            CandlestickPattern.SignalDirection.BULLISH,
            0.8,
            "Bullish engulfing"
        );

        assertFalse(candlestickPatternService.confirmsSignal(bullishPattern, Signal.SELL));
    }

    @Test
    void confirmsSignal_UnknownPattern_ReturnsFalse() {
        CandlestickPattern unknownPattern = CandlestickPattern.createUnknown();

        assertFalse(candlestickPatternService.confirmsSignal(unknownPattern, Signal.BUY));
    }

    private List<CandleData> createTestCandles() {
        return Arrays.asList(
            createCandle(100000.0),
            createCandle(101000.0),
            createCandle(102000.0)
        );
    }

    private List<CandleData> createBullishEngulfingCandles() {
        return Arrays.asList(
            createCandleWithOHLC(100000.0, 101000.0, 99000.0, 99500.0), // First candle (not used in pattern)
            createCandleWithOHLC(100000.0, 101000.0, 99000.0, 99500.0), // Bearish candle
            createCandleWithOHLC(99000.0, 102000.0, 98500.0, 101500.0)  // Bullish engulfing
        );
    }

    private List<CandleData> createBearishEngulfingCandles() {
        return Arrays.asList(
            createCandleWithOHLC(100000.0, 101000.0, 99000.0, 100500.0), // First candle (not used in pattern)
            createCandleWithOHLC(100000.0, 101000.0, 99000.0, 100500.0), // Bullish candle
            createCandleWithOHLC(101000.0, 101500.0, 98000.0, 98500.0)  // Bearish engulfing
        );
    }

    private List<CandleData> createHammerCandles() {
        return Arrays.asList(
            createCandleWithOHLC(100000.0, 100500.0, 99000.0, 99500.0), // First candle (not used in pattern)
            createCandleWithOHLC(100000.0, 100500.0, 99000.0, 99500.0), // Bearish candle
            createCandleWithOHLC(99000.0, 99500.0, 97000.0, 99200.0)  // Hammer
        );
    }

    private List<CandleData> createShootingStarCandles() {
        return Arrays.asList(
            createCandleWithOHLC(100000.0, 100500.0, 99000.0, 100500.0), // First candle (not used in pattern)
            createCandleWithOHLC(100000.0, 100500.0, 99000.0, 100500.0), // Bullish candle
            createCandleWithOHLC(100500.0, 105000.0, 100200.0, 100300.0)  // Shooting star
        );
    }

    private List<CandleData> createMorningStarCandles() {
        return Arrays.asList(
            createCandleWithOHLC(100000.0, 100500.0, 99000.0, 99500.0), // First candle (not used in pattern)
            createCandleWithOHLC(100000.0, 100500.0, 99000.0, 99500.0), // Bearish candle
            createCandleWithOHLC(99000.0, 99500.0, 98500.0, 99000.0),  // Small gap down
            createCandleWithOHLC(99500.0, 102000.0, 99000.0, 101500.0)  // Bullish recovery
        );
    }

    private List<CandleData> createEveningStarCandles() {
        return Arrays.asList(
            createCandleWithOHLC(100000.0, 101000.0, 99000.0, 100500.0), // First candle (not used in pattern)
            createCandleWithOHLC(100000.0, 101000.0, 99000.0, 100500.0), // Bullish candle
            createCandleWithOHLC(100500.0, 101000.0, 100300.0, 100800.0), // Small gap up
            createCandleWithOHLC(100800.0, 101000.0, 98000.0, 98500.0)  // Bearish reversal
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

    private CandleData createCandleWithOHLC(double open, double high, double low, double close) {
        CandleData candle = new CandleData();
        candle.setSymbol("BTC/USDT");
        candle.setTimestamp(LocalDateTime.now());
        candle.setOpen(open);
        candle.setHigh(high);
        candle.setLow(low);
        candle.setClose(close);
        candle.setVolume(1000000.0);
        candle.setTimeframe("15m");
        return candle;
    }
}
