package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.Setup;
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
class EntryConfirmationServiceTest {

    @Mock
    private TechnicalIndicatorService technicalIndicatorService;

    private EntryConfirmationService entryConfirmationService;
    private List<CandleData> testCandles;

    @BeforeEach
    void setUp() {
        entryConfirmationService = new EntryConfirmationService(technicalIndicatorService);
        testCandles = createTestCandles();
    }

    @Test
    void confirmEntry15m_LongSetupWithValidConditions_ReturnsBuy() {
        TechnicalIndicators indicators = createBuySignalIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Signal signal = entryConfirmationService.confirmEntry15m(testCandles, Setup.LONG);

        assertEquals(Signal.BUY, signal);
    }

    @Test
    void confirmEntry15m_ShortSetupWithValidConditions_ReturnsSell() {
        TechnicalIndicators indicators = createSellSignalIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Signal signal = entryConfirmationService.confirmEntry15m(testCandles, Setup.SHORT);

        assertEquals(Signal.SELL, signal);
    }

    @Test
    void confirmEntry15m_LongSetupWithInvalidRSI_ReturnsHold() {
        TechnicalIndicators indicators = createInvalidRSIIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Signal signal = entryConfirmationService.confirmEntry15m(testCandles, Setup.LONG);

        assertEquals(Signal.HOLD, signal);
    }

    @Test
    void confirmEntry15m_LongSetupWithInvalidStochRSI_ReturnsHold() {
        TechnicalIndicators indicators = createInvalidStochRSIIndicators();
        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        Signal signal = entryConfirmationService.confirmEntry15m(testCandles, Setup.LONG);

        assertEquals(Signal.HOLD, signal);
    }

    @Test
    void confirmEntry15m_NoneSetup_ReturnsHold() {
        Signal signal = entryConfirmationService.confirmEntry15m(testCandles, Setup.NONE);

        assertEquals(Signal.HOLD, signal);
    }

    @Test
    void confirmEntry15m_NullCandles_ReturnsHold() {
        Signal signal = entryConfirmationService.confirmEntry15m(null, Setup.LONG);

        assertEquals(Signal.HOLD, signal);
    }

    @Test
    void confirmEntry15m_EmptyCandles_ReturnsHold() {
        Signal signal = entryConfirmationService.confirmEntry15m(Arrays.asList(), Setup.LONG);

        assertEquals(Signal.HOLD, signal);
    }

    @Test
    void hasStochRSICrossUp_ValidCrossUp_ReturnsTrue() {
        List<CandleData> candles = createCandlesWithStochRSICrossUp();
        TechnicalIndicators currentIndicators = createStochRSICrossUpIndicators();
        TechnicalIndicators previousIndicators = createStochRSIBeforeCrossIndicators();

        when(technicalIndicatorService.calculateIndicators(candles)).thenReturn(currentIndicators);
        when(technicalIndicatorService.calculateIndicators(candles.subList(0, candles.size() - 1)))
            .thenReturn(previousIndicators);

        boolean hasCross = entryConfirmationService.hasStochRSICrossUp(candles);

        assertTrue(hasCross);
    }

    @Test
    void hasStochRSICrossUp_NoCross_ReturnsFalse() {
        List<CandleData> candles = createTestCandles();
        TechnicalIndicators indicators = createNoCrossIndicators();

        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean hasCross = entryConfirmationService.hasStochRSICrossUp(candles);

        assertFalse(hasCross);
    }

    @Test
    void hasStochRSICrossUp_InsufficientData_ReturnsFalse() {
        List<CandleData> insufficientCandles = Arrays.asList(createCandle(100000.0));

        boolean hasCross = entryConfirmationService.hasStochRSICrossUp(insufficientCandles);

        assertFalse(hasCross);
    }

    @Test
    void hasStochRSICrossDown_ValidCrossDown_ReturnsTrue() {
        List<CandleData> candles = createCandlesWithStochRSICrossDown();
        TechnicalIndicators currentIndicators = createStochRSICrossDownIndicators();
        TechnicalIndicators previousIndicators = createStochRSIBeforeCrossDownIndicators();

        when(technicalIndicatorService.calculateIndicators(candles)).thenReturn(currentIndicators);
        when(technicalIndicatorService.calculateIndicators(candles.subList(0, candles.size() - 1)))
            .thenReturn(previousIndicators);

        boolean hasCross = entryConfirmationService.hasStochRSICrossDown(candles);

        assertTrue(hasCross);
    }

    @Test
    void hasStochRSICrossDown_NoCross_ReturnsFalse() {
        List<CandleData> candles = createTestCandles();
        TechnicalIndicators indicators = createNoCrossIndicators();

        when(technicalIndicatorService.calculateIndicators(any())).thenReturn(indicators);

        boolean hasCross = entryConfirmationService.hasStochRSICrossDown(candles);

        assertFalse(hasCross);
    }

    private TechnicalIndicators createBuySignalIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setRsi(25.0);
        indicators.setStochRsiK(30.0);
        indicators.setStochRsiD(20.0);
        return indicators;
    }

    private TechnicalIndicators createSellSignalIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setRsi(75.0);
        indicators.setStochRsiK(20.0);
        indicators.setStochRsiD(30.0);
        return indicators;
    }

    private TechnicalIndicators createInvalidRSIIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setRsi(50.0);
        indicators.setStochRsiK(30.0);
        indicators.setStochRsiD(20.0);
        return indicators;
    }

    private TechnicalIndicators createInvalidStochRSIIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setRsi(25.0);
        indicators.setStochRsiK(20.0);
        indicators.setStochRsiD(30.0);
        return indicators;
    }

    private TechnicalIndicators createStochRSICrossUpIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setStochRsiK(30.0);
        indicators.setStochRsiD(20.0);
        return indicators;
    }

    private TechnicalIndicators createStochRSIBeforeCrossIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setStochRsiK(20.0);
        indicators.setStochRsiD(30.0);
        return indicators;
    }

    private TechnicalIndicators createStochRSICrossDownIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setStochRsiK(20.0);
        indicators.setStochRsiD(30.0);
        return indicators;
    }

    private TechnicalIndicators createStochRSIBeforeCrossDownIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setStochRsiK(30.0);
        indicators.setStochRsiD(20.0);
        return indicators;
    }

    private TechnicalIndicators createNoCrossIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setStochRsiK(25.0);
        indicators.setStochRsiD(20.0);
        return indicators;
    }

    private List<CandleData> createTestCandles() {
        return Arrays.asList(
            createCandle(100000.0),
            createCandle(100500.0),
            createCandle(101000.0)
        );
    }

    private List<CandleData> createCandlesWithStochRSICrossUp() {
        return Arrays.asList(
            createCandle(100000.0),
            createCandle(100500.0)
        );
    }

    private List<CandleData> createCandlesWithStochRSICrossDown() {
        return Arrays.asList(
            createCandle(100000.0),
            createCandle(100500.0)
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
