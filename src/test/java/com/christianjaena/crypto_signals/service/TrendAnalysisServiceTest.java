package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
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
class TrendAnalysisServiceTest {

    @Mock
    private TechnicalIndicatorService technicalIndicatorService;

    private TrendAnalysisService trendAnalysisService;
    private List<CandleData> testCandles;

    @BeforeEach
    void setUp() {
        trendAnalysisService = new TrendAnalysisService(technicalIndicatorService);
        testCandles = createTestCandles();
    }

    @Test
    void determineTrend1D_BullishConditions_ReturnsBullish() {
        TechnicalIndicators indicators = createBullishIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Trend trend = trendAnalysisService.determineTrend1D(testCandles);

        assertEquals(Trend.BULLISH, trend);
    }

    @Test
    void determineTrend1D_BearishConditions_ReturnsBearish() {
        TechnicalIndicators indicators = createBearishIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Trend trend = trendAnalysisService.determineTrend1D(createBearishTestCandles());

        assertEquals(Trend.BEARISH, trend);
    }

    @Test
    void determineTrend1D_SidewaysConditions_ReturnsSideways() {
        TechnicalIndicators indicators = createSidewaysIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Trend trend = trendAnalysisService.determineTrend1D(testCandles);

        assertEquals(Trend.SIDEWAYS, trend);
    }

    @Test
    void determineTrend1D_NullCandles_ReturnsSideways() {
        Trend trend = trendAnalysisService.determineTrend1D(null);

        assertEquals(Trend.SIDEWAYS, trend);
    }

    @Test
    void determineTrend1D_EmptyCandles_ReturnsSideways() {
        Trend trend = trendAnalysisService.determineTrend1D(Arrays.asList());

        assertEquals(Trend.SIDEWAYS, trend);
    }

    @Test
    void isSidewaysRSI_RSI45_ReturnsTrue() {
        assertTrue(trendAnalysisService.isSidewaysRSI(45.0));
    }

    @Test
    void isSidewaysRSI_RSI55_ReturnsTrue() {
        assertTrue(trendAnalysisService.isSidewaysRSI(55.0));
    }

    @Test
    void isSidewaysRSI_RSI44_ReturnsFalse() {
        assertFalse(trendAnalysisService.isSidewaysRSI(44.0));
    }

    @Test
    void isSidewaysRSI_RSI56_ReturnsFalse() {
        assertFalse(trendAnalysisService.isSidewaysRSI(56.0));
    }

    @Test
    void calculateTrendStrength_ValidCandles_ReturnsPositiveValue() {
        TechnicalIndicators indicators = createBullishIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        double strength = trendAnalysisService.calculateTrendStrength(testCandles);

        assertTrue(strength > 0);
    }

    @Test
    void calculateTrendStrength_NullCandles_ReturnsZero() {
        double strength = trendAnalysisService.calculateTrendStrength(null);

        assertEquals(0.0, strength);
    }

    private TechnicalIndicators createBullishIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setEma50(105000.0);
        indicators.setEma200(100000.0);
        indicators.setRsi(60.0);
        return indicators;
    }

    private TechnicalIndicators createBearishIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setEma50(95000.0);
        indicators.setEma200(100000.0);
        indicators.setRsi(40.0);
        return indicators;
    }

    private TechnicalIndicators createSidewaysIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setEma50(100000.0);
        indicators.setEma200(100000.0);
        indicators.setRsi(50.0);
        return indicators;
    }

    private List<CandleData> createTestCandles() {
        return Arrays.asList(
            createCandle(100000.0),
            createCandle(101000.0),
            createCandle(102000.0)
        );
    }

    private List<CandleData> createBearishTestCandles() {
        return Arrays.asList(
            createCandle(98000.0),
            createCandle(99000.0),
            createCandle(95000.0)
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
