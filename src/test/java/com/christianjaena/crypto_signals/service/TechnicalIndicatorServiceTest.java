package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.TechnicalIndicators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TechnicalIndicatorServiceTest {

    private TechnicalIndicatorService technicalIndicatorService;
    private List<CandleData> testCandles;

    @BeforeEach
    void setUp() {
        technicalIndicatorService = new TechnicalIndicatorService();
        testCandles = createTestCandles();
    }

    @Test
    void calculateIndicators_ValidCandles_ReturnsIndicators() {
        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(testCandles);

        assertNotNull(indicators);
        assertTrue(indicators.getEma50() > 0);
        assertTrue(indicators.getEma200() > 0);
        assertTrue(indicators.getRsi() >= 0 && indicators.getRsi() <= 100);
        assertTrue(indicators.getStochRsiK() >= 0 && indicators.getStochRsiK() <= 100);
        assertTrue(indicators.getStochRsiD() >= 0 && indicators.getStochRsiD() <= 100);
        assertTrue(indicators.getVolumeAverage20() > 0);
    }

    @Test
    void calculateIndicators_NullCandles_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            technicalIndicatorService.calculateIndicators(null);
        });
    }

    @Test
    void calculateIndicators_EmptyCandles_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            technicalIndicatorService.calculateIndicators(Arrays.asList());
        });
    }

    @Test
    void calculateIndicators_InsufficientData_ReturnsDefaultValues() {
        List<CandleData> insufficientCandles = testCandles.subList(0, 10);
        TechnicalIndicators indicators = technicalIndicatorService.calculateIndicators(insufficientCandles);

        assertNotNull(indicators);
        assertEquals(50.0, indicators.getRsi(), 0.1);
    }

    @Test
    void calculateEMA_GrowingPrices_EmaIncreases() {
        double[] growingPrices = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110};
        double ema = technicalIndicatorService.calculateEMA(growingPrices, 5);

        assertTrue(ema > 100 && ema < 110);
    }

    @Test
    void calculateRSI_OversoldCondition_ReturnsLowRSI() {
        double[] fallingPrices = {110, 109, 108, 107, 106, 105, 104, 103, 102, 101, 100};
        double rsi = technicalIndicatorService.calculateRSI(fallingPrices, 5);

        assertTrue(rsi < 50);
    }

    @Test
    void calculateRSI_OverboughtCondition_ReturnsHighRSI() {
        double[] risingPrices = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110};
        double rsi = technicalIndicatorService.calculateRSI(risingPrices, 5);

        assertTrue(rsi > 50);
    }

    @Test
    void calculateSMA_ValidValues_ReturnsAverage() {
        double[] values = {10, 20, 30, 40, 50};
        double sma = technicalIndicatorService.calculateSMA(values, 3);

        assertEquals(40.0, sma, 0.1);
    }

    @Test
    void calculateSMA_InsufficientData_ReturnsLastValue() {
        double[] values = {10, 20};
        double sma = technicalIndicatorService.calculateSMA(values, 3);

        assertEquals(20.0, sma);
    }

    private List<CandleData> createTestCandles() {
        List<CandleData> candles = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        double basePrice = 100.0;

        for (int i = 0; i < 250; i++) {
            double price = basePrice + (Math.random() - 0.5) * 10;
            CandleData candle = new CandleData();
            candle.setSymbol("BTC/USDT");
            candle.setTimestamp(now.minusHours(i));
            candle.setOpen(price);
            candle.setHigh(price * 1.02);
            candle.setLow(price * 0.98);
            candle.setClose(price);
            candle.setVolume(1000000 + Math.random() * 500000);
            candle.setTimeframe("1h");
            candles.add(candle);
            basePrice = price;
        }

        return candles;
    }
}
