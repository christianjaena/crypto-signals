package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.FibonacciLevel;
import com.christianjaena.crypto_signals.model.Signal;
import com.christianjaena.crypto_signals.model.Trend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FibonacciServiceTest {

    private FibonacciService fibonacciService;
    private List<CandleData> testCandles;

    @BeforeEach
    void setUp() {
        fibonacciService = new FibonacciService();
        testCandles = createTestCandles();
    }

    @Test
    void calculateRetracementLevels_BullishTrend_ReturnsCorrectLevels() {
        List<FibonacciLevel> levels = fibonacciService.calculateRetracementLevels(testCandles, Trend.BULLISH);

        assertFalse(levels.isEmpty());
        assertEquals(5, levels.size()); // Should have 5 retracement levels

        // Check that all levels are retracements
        for (FibonacciLevel level : levels) {
            assertTrue(level.isRetracement());
            assertNotNull(level.getPrice());
            assertTrue(level.getPrice() > 0);
        }

        // Check for key levels
        boolean has38_2 = levels.stream().anyMatch(l -> l.getLevel() == 0.382);
        boolean has50 = levels.stream().anyMatch(l -> l.getLevel() == 0.5);
        boolean has61_8 = levels.stream().anyMatch(l -> l.getLevel() == 0.618);

        assertTrue(has38_2);
        assertTrue(has50);
        assertTrue(has61_8);
    }

    @Test
    void calculateRetracementLevels_BearishTrend_ReturnsCorrectLevels() {
        List<FibonacciLevel> levels = fibonacciService.calculateRetracementLevels(testCandles, Trend.BEARISH);

        assertFalse(levels.isEmpty());
        assertEquals(5, levels.size());

        for (FibonacciLevel level : levels) {
            assertTrue(level.isRetracement());
            assertNotNull(level.getPrice());
            assertTrue(level.getPrice() > 0);
        }
    }

    @Test
    void calculateRetracementLevels_NullCandles_ReturnsEmptyList() {
        List<FibonacciLevel> levels = fibonacciService.calculateRetracementLevels(null, Trend.BULLISH);

        assertTrue(levels.isEmpty());
    }

    @Test
    void calculateRetracementLevels_InsufficientCandles_ReturnsEmptyList() {
        List<CandleData> insufficientCandles = testCandles.subList(0, 5);
        List<FibonacciLevel> levels = fibonacciService.calculateRetracementLevels(insufficientCandles, Trend.BULLISH);

        assertTrue(levels.isEmpty());
    }

    @Test
    void calculateExtensionLevels_BullishTrend_ReturnsCorrectLevels() {
        List<FibonacciLevel> levels = fibonacciService.calculateExtensionLevels(testCandles, Trend.BULLISH);

        assertFalse(levels.isEmpty());
        assertEquals(4, levels.size()); // Should have 4 extension levels

        for (FibonacciLevel level : levels) {
            assertEquals(FibonacciLevel.LevelType.EXTENSION, level.getLevelType());
            assertNotNull(level.getPrice());
            assertTrue(level.getPrice() > 0);
        }
    }

    @Test
    void findNearestSupport_BuySignalNearSupport_ReturnsSupportLevel() {
        List<CandleData> supportCandles = createSupportTestCandles();
        double currentPrice = 105000.0;

        FibonacciLevel support = fibonacciService.findNearestSupport(supportCandles, currentPrice, Signal.BUY);

        assertNotNull(support);
        assertTrue(support.getPrice() < currentPrice);
        assertTrue(support.isRetracement());
        assertTrue(support.getDistanceFromPrice(currentPrice) <= 2.0);
    }

    @Test
    void findNearestSupport_SellSignal_ReturnsNull() {
        double currentPrice = 105000.0;

        FibonacciLevel support = fibonacciService.findNearestSupport(testCandles, currentPrice, Signal.SELL);

        assertNull(support);
    }

    @Test
    void findNearestResistance_SellSignalNearResistance_ReturnsResistanceLevel() {
        List<CandleData> resistanceCandles = createResistanceTestCandles();
        double currentPrice = 117000.0; // Near the 61.8% retracement level (~118579)

        FibonacciLevel resistance = fibonacciService.findNearestResistance(resistanceCandles, currentPrice, Signal.SELL);

        assertNotNull(resistance);
        assertTrue(resistance.getPrice() > currentPrice);
        assertTrue(resistance.isRetracement());
        assertTrue(resistance.getDistanceFromPrice(currentPrice) <= 2.0);
    }

    @Test
    void findNearestResistance_BuySignal_ReturnsNull() {
        double currentPrice = 95000.0;

        FibonacciLevel resistance = fibonacciService.findNearestResistance(testCandles, currentPrice, Signal.BUY);

        assertNull(resistance);
    }

    @Test
    void isTradeNearKeyLevel_NearKeyLevel_ReturnsTrue() {
        List<CandleData> levelCandles = createSupportTestCandles();
        double currentPrice = 105000.0;

        boolean nearKeyLevel = fibonacciService.isTradeNearKeyLevel(levelCandles, currentPrice, Signal.BUY);

        assertTrue(nearKeyLevel);
    }

    @Test
    void isTradeNearKeyLevel_FarFromKeyLevel_ReturnsFalse() {
        double currentPrice = 95000.0; // Below swing low - far from key levels

        boolean nearKeyLevel = fibonacciService.isTradeNearKeyLevel(testCandles, currentPrice, Signal.BUY);

        assertFalse(nearKeyLevel);
    }

    @Test
    void getConfidenceBonus_NearKeyLevel_Returns10() {
        List<CandleData> levelCandles = createSupportTestCandles();
        double currentPrice = 105000.0;

        int bonus = fibonacciService.getConfidenceBonus(levelCandles, currentPrice, Signal.BUY);

        assertEquals(10, bonus);
    }

    @Test
    void getConfidenceBonus_NearNonKeyLevel_Returns5() {
        double currentPrice = 139000.0; // Above 23.6% retracement (~138874) - non-key level

        int bonus = fibonacciService.getConfidenceBonus(testCandles, currentPrice, Signal.BUY);

        assertEquals(5, bonus);
    }

    @Test
    void getConfidenceBonus_FarFromLevels_Returns0() {
        double currentPrice = 95000.0; // Below swing low (100000) - far from levels

        int bonus = fibonacciService.getConfidenceBonus(testCandles, currentPrice, Signal.BUY);

        assertEquals(0, bonus);
    }

    @Test
    void shouldSkipTrade_TooFarFromLevels_ReturnsTrue() {
        double currentPrice = 95000.0; // Below swing low - far from levels

        boolean shouldSkip = fibonacciService.shouldSkipTrade(testCandles, currentPrice, Signal.BUY);

        assertTrue(shouldSkip);
    }

    @Test
    void shouldSkipTrade_NearLevels_ReturnsFalse() {
        List<CandleData> levelCandles = createSupportTestCandles();
        double currentPrice = 105000.0;

        boolean shouldSkip = fibonacciService.shouldSkipTrade(levelCandles, currentPrice, Signal.BUY);

        assertFalse(shouldSkip);
    }

    @Test
    void shouldSkipTrade_NoLevels_ReturnsTrue() {
        List<CandleData> emptyCandles = Arrays.asList();
        double currentPrice = 100000.0;

        boolean shouldSkip = fibonacciService.shouldSkipTrade(emptyCandles, currentPrice, Signal.BUY);

        assertTrue(shouldSkip);
    }

    private List<CandleData> createTestCandles() {
        return Arrays.asList(
            createCandle(100000.0),
            createCandle(102000.0),
            createCandle(104000.0),
            createCandle(106000.0),
            createCandle(108000.0),
            createCandle(110000.0),
            createCandle(112000.0),
            createCandle(114000.0),
            createCandle(116000.0),
            createCandle(118000.0),
            createCandle(120000.0),
            createCandle(122000.0),
            createCandle(124000.0),
            createCandle(126000.0),
            createCandle(128000.0),
            createCandle(130000.0),
            createCandle(132000.0),
            createCandle(134000.0),
            createCandle(136000.0),
            createCandle(138000.0),
            createCandle(140000.0),
            createCandle(142000.0),
            createCandle(144000.0),
            createCandle(146000.0),
            createCandle(148000.0),
            createCandle(150000.0)
        );
    }

    private List<CandleData> createSupportTestCandles() {
        // Create candles with a clear swing high and low for retracement calculation
        return Arrays.asList(
            createCandle(100000.0), // Low
            createCandle(105000.0),
            createCandle(110000.0),
            createCandle(115000.0),
            createCandle(120000.0), // High
            createCandle(118000.0),
            createCandle(115000.0),
            createCandle(112000.0),
            createCandle(110000.0),
            createCandle(108000.0), // Current price near 61.8% retracement
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
            createCandle(95000.0),
            createCandle(94000.0),
            createCandle(93000.0),
            createCandle(92000.0),
            createCandle(91000.0),
            createCandle(90000.0)
        );
    }

    private List<CandleData> createResistanceTestCandles() {
        // Create candles with a clear swing low and high for bearish retracement
        return Arrays.asList(
            createCandle(120000.0), // High
            createCandle(115000.0),
            createCandle(110000.0),
            createCandle(105000.0),
            createCandle(100000.0), // Low
            createCandle(102000.0),
            createCandle(104000.0),
            createCandle(106000.0),
            createCandle(108000.0),
            createCandle(110000.0), // Current price near 61.8% retracement
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
            createCandle(125000.0),
            createCandle(126000.0),
            createCandle(127000.0),
            createCandle(128000.0),
            createCandle(129000.0),
            createCandle(130000.0)
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
        candle.setTimeframe("4H");
        return candle;
    }
}
