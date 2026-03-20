package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.Setup;
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
class SetupZoneServiceTest {

    @Mock
    private TechnicalIndicatorService technicalIndicatorService;

    private SetupZoneService setupZoneService;
    private List<CandleData> testCandles;

    @BeforeEach
    void setUp() {
        setupZoneService = new SetupZoneService(technicalIndicatorService);
        testCandles = createTestCandles();
    }

    @Test
    void identifySetup4H_BullishTrendWithValidSetup_ReturnsLong() {
        TechnicalIndicators indicators = createLongSetupIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Setup setup = setupZoneService.identifySetup4H(testCandles, Trend.BULLISH);

        assertEquals(Setup.LONG, setup);
    }

    @Test
    void identifySetup4H_BearishTrendWithValidSetup_ReturnsShort() {
        TechnicalIndicators indicators = createShortSetupIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Setup setup = setupZoneService.identifySetup4H(testCandles, Trend.BEARISH);

        assertEquals(Setup.SHORT, setup);
    }

    @Test
    void identifySetup4H_BullishTrendWithInvalidRSI_ReturnsNone() {
        TechnicalIndicators indicators = createInvalidRSIIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Setup setup = setupZoneService.identifySetup4H(testCandles, Trend.BULLISH);

        assertEquals(Setup.NONE, setup);
    }

    @Test
    void identifySetup4H_BullishTrendWithPriceFarFromEMA_ReturnsNone() {
        TechnicalIndicators indicators = createPriceFarFromEMAIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Setup setup = setupZoneService.identifySetup4H(testCandles, Trend.BULLISH);

        assertEquals(Setup.NONE, setup);
    }

    @Test
    void identifySetup4H_SidewaysTrend_ReturnsNone() {
        Setup setup = setupZoneService.identifySetup4H(testCandles, Trend.SIDEWAYS);

        assertEquals(Setup.NONE, setup);
    }

    @Test
    void identifySetup4H_NullCandles_ReturnsNone() {
        Setup setup = setupZoneService.identifySetup4H(null, Trend.BULLISH);

        assertEquals(Setup.NONE, setup);
    }

    @Test
    void identifySetup4H_EmptyCandles_ReturnsNone() {
        Setup setup = setupZoneService.identifySetup4H(Arrays.asList(), Trend.BULLISH);

        assertEquals(Setup.NONE, setup);
    }

    @Test
    void isPerfectSetup_ValidLongSetup_ReturnsTrue() {
        TechnicalIndicators indicators = createLongSetupIndicators();
        double currentPrice = 100000.0;

        boolean isPerfect = setupZoneService.isPerfectSetup(currentPrice, indicators, Setup.LONG);

        assertTrue(isPerfect);
    }

    @Test
    void isPerfectSetup_ValidShortSetup_ReturnsTrue() {
        TechnicalIndicators indicators = createShortSetupIndicators();
        double currentPrice = 100000.0;

        boolean isPerfect = setupZoneService.isPerfectSetup(currentPrice, indicators, Setup.SHORT);

        assertTrue(isPerfect);
    }

    @Test
    void isPerfectSetup_InvalidSetup_ReturnsFalse() {
        TechnicalIndicators indicators = createInvalidRSIIndicators();
        double currentPrice = 100000.0;

        boolean isPerfect = setupZoneService.isPerfectSetup(currentPrice, indicators, Setup.LONG);

        assertFalse(isPerfect);
    }

    @Test
    void isPerfectSetup_NoneSetup_ReturnsFalse() {
        TechnicalIndicators indicators = createLongSetupIndicators();
        double currentPrice = 100000.0;

        boolean isPerfect = setupZoneService.isPerfectSetup(currentPrice, indicators, Setup.NONE);

        assertFalse(isPerfect);
    }

    private TechnicalIndicators createLongSetupIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setEma50(100000.0);
        indicators.setRsi(35.0);
        return indicators;
    }

    private TechnicalIndicators createShortSetupIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setEma50(100000.0);
        indicators.setRsi(65.0);
        return indicators;
    }

    private TechnicalIndicators createInvalidRSIIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setEma50(100000.0);
        indicators.setRsi(50.0);
        return indicators;
    }

    private TechnicalIndicators createPriceFarFromEMAIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setEma50(100000.0);
        indicators.setRsi(35.0);
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
        candle.setTimeframe("4H");
        return candle;
    }
}
