package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.*;
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
class CryptoSignalServiceTest {

    @Mock
    private TrendAnalysisService trendAnalysisService;
    @Mock
    private SetupZoneService setupZoneService;
    @Mock
    private EntryConfirmationService entryConfirmationService;
    @Mock
    private ConfidenceScoringService confidenceScoringService;
    @Mock
    private SignalFilterService signalFilterService;

    private CryptoSignalService cryptoSignalService;

    @BeforeEach
    void setUp() {
        cryptoSignalService = new CryptoSignalService(
            trendAnalysisService,
            setupZoneService,
            entryConfirmationService,
            confidenceScoringService,
            signalFilterService
        );
    }

    @Test
    void generateSignal_SidewaysTrend_ReturnsHoldSignalWithCurrentPrice() {
        // Arrange
        String symbol = "BTCUSDT";
        List<CandleData> candles1D = createTestCandles();
        List<CandleData> candles4H = createTestCandles();
        List<CandleData> candles15m = createTestCandles();
        double expectedPrice = candles15m.get(candles15m.size() - 1).getClose();

        when(signalFilterService.isSymbolValid(symbol)).thenReturn(true);
        when(signalFilterService.hasSufficientData(candles1D, candles4H, candles15m)).thenReturn(true);
        when(trendAnalysisService.determineTrend1D(candles1D)).thenReturn(Trend.SIDEWAYS);

        // Act
        CryptoSignal signal = cryptoSignalService.generateSignal(symbol, candles1D, candles4H, candles15m);

        // Assert
        assertEquals(symbol, signal.getSymbol());
        assertEquals(Trend.SIDEWAYS, signal.getTrend1D());
        assertEquals(Setup.NONE, signal.getSetup4H());
        assertEquals(Signal.HOLD, signal.getEntry15m());
        assertEquals(0, signal.getConfidence());
        assertEquals(expectedPrice, signal.getCurrentPrice());
        assertEquals(0, signal.getStopLoss());
        assertEquals(0, signal.getPredictionPriceGrowth());
        assertTrue(signal.getNotes().get(0).contains("sideways"));
    }

    @Test
    void generateSignal_InvalidSymbol_ReturnsHoldSignalWithCurrentPrice() {
        // Arrange
        String symbol = "INVALID";
        List<CandleData> candles1D = createTestCandles();
        List<CandleData> candles4H = createTestCandles();
        List<CandleData> candles15m = createTestCandles();
        double expectedPrice = candles15m.get(candles15m.size() - 1).getClose();

        when(signalFilterService.isSymbolValid(symbol)).thenReturn(false);

        // Act
        CryptoSignal signal = cryptoSignalService.generateSignal(symbol, candles1D, candles4H, candles15m);

        // Assert
        assertEquals(symbol, signal.getSymbol());
        assertEquals(Trend.SIDEWAYS, signal.getTrend1D());
        assertEquals(Signal.HOLD, signal.getEntry15m());
        assertEquals(expectedPrice, signal.getCurrentPrice());
    }

    @Test
    void generateSignal_InsufficientData_ReturnsHoldSignalWithCurrentPrice() {
        // Arrange
        String symbol = "BTCUSDT";
        List<CandleData> candles1D = createTestCandles();
        List<CandleData> candles4H = createTestCandles();
        List<CandleData> candles15m = createTestCandles();
        double expectedPrice = candles15m.get(candles15m.size() - 1).getClose();

        when(signalFilterService.isSymbolValid(symbol)).thenReturn(true);
        when(signalFilterService.hasSufficientData(candles1D, candles4H, candles15m)).thenReturn(false);

        // Act
        CryptoSignal signal = cryptoSignalService.generateSignal(symbol, candles1D, candles4H, candles15m);

        // Assert
        assertEquals(symbol, signal.getSymbol());
        assertEquals(Signal.HOLD, signal.getEntry15m());
        assertEquals(expectedPrice, signal.getCurrentPrice());
    }

    @Test
    void generateSignal_NoSetupZone_ReturnsHoldSignalWithCurrentPrice() {
        // Arrange
        String symbol = "BTCUSDT";
        List<CandleData> candles1D = createTestCandles();
        List<CandleData> candles4H = createTestCandles();
        List<CandleData> candles15m = createTestCandles();
        double expectedPrice = candles15m.get(candles15m.size() - 1).getClose();

        when(signalFilterService.isSymbolValid(symbol)).thenReturn(true);
        when(signalFilterService.hasSufficientData(candles1D, candles4H, candles15m)).thenReturn(true);
        when(trendAnalysisService.determineTrend1D(candles1D)).thenReturn(Trend.BULLISH);
        when(setupZoneService.identifySetup4H(candles4H, Trend.BULLISH)).thenReturn(Setup.NONE);

        // Act
        CryptoSignal signal = cryptoSignalService.generateSignal(symbol, candles1D, candles4H, candles15m);

        // Assert
        assertEquals(symbol, signal.getSymbol());
        assertEquals(Trend.SIDEWAYS, signal.getTrend1D());  // createHoldSignal sets SIDEWAYS
        assertEquals(Setup.NONE, signal.getSetup4H());       // createHoldSignal sets NONE
        assertEquals(Signal.HOLD, signal.getEntry15m());
        assertEquals(expectedPrice, signal.getCurrentPrice());
    }

    @Test
    void generateSignal_EntryNotConfirmed_ReturnsHoldSignalWithCurrentPrice() {
        // Arrange
        String symbol = "BTCUSDT";
        List<CandleData> candles1D = createTestCandles();
        List<CandleData> candles4H = createTestCandles();
        List<CandleData> candles15m = createTestCandles();
        double expectedPrice = candles15m.get(candles15m.size() - 1).getClose();

        when(signalFilterService.isSymbolValid(symbol)).thenReturn(true);
        when(signalFilterService.hasSufficientData(candles1D, candles4H, candles15m)).thenReturn(true);
        when(trendAnalysisService.determineTrend1D(candles1D)).thenReturn(Trend.BULLISH);
        when(setupZoneService.identifySetup4H(candles4H, Trend.BULLISH)).thenReturn(Setup.LONG);
        when(entryConfirmationService.confirmEntry15m(candles15m, Setup.LONG)).thenReturn(Signal.HOLD);

        // Act
        CryptoSignal signal = cryptoSignalService.generateSignal(symbol, candles1D, candles4H, candles15m);

        // Assert
        assertEquals(symbol, signal.getSymbol());
        assertEquals(Setup.NONE, signal.getSetup4H());  // createHoldSignal sets NONE
        assertEquals(Signal.HOLD, signal.getEntry15m());
        assertEquals(expectedPrice, signal.getCurrentPrice());
    }

    @Test
    void generateSignal_SignalFiltered_ReturnsHoldSignalWithCurrentPrice() {
        // Arrange
        String symbol = "BTCUSDT";
        List<CandleData> candles1D = createTestCandles();
        List<CandleData> candles4H = createTestCandles();
        List<CandleData> candles15m = createTestCandles();
        double expectedPrice = candles15m.get(candles15m.size() - 1).getClose();

        when(signalFilterService.isSymbolValid(symbol)).thenReturn(true);
        when(signalFilterService.hasSufficientData(candles1D, candles4H, candles15m)).thenReturn(true);
        when(trendAnalysisService.determineTrend1D(candles1D)).thenReturn(Trend.BULLISH);
        when(setupZoneService.identifySetup4H(candles4H, Trend.BULLISH)).thenReturn(Setup.LONG);
        when(entryConfirmationService.confirmEntry15m(candles15m, Setup.LONG)).thenReturn(Signal.BUY);
        when(signalFilterService.shouldFilterSignal(symbol, candles1D, candles4H, Signal.BUY)).thenReturn(true);

        // Act
        CryptoSignal signal = cryptoSignalService.generateSignal(symbol, candles1D, candles4H, candles15m);

        // Assert
        assertEquals(symbol, signal.getSymbol());
        assertEquals(Signal.HOLD, signal.getEntry15m());
        assertEquals(expectedPrice, signal.getCurrentPrice());
    }

    @Test
    void generateSignal_NullCandles15m_ReturnsHoldSignalWithZeroPrice() {
        // Arrange
        String symbol = "BTCUSDT";
        List<CandleData> candles1D = createTestCandles();
        List<CandleData> candles4H = createTestCandles();

        when(signalFilterService.isSymbolValid(symbol)).thenReturn(true);
        when(signalFilterService.hasSufficientData(candles1D, candles4H, null)).thenReturn(true);
        when(trendAnalysisService.determineTrend1D(candles1D)).thenReturn(Trend.SIDEWAYS);

        // Act
        CryptoSignal signal = cryptoSignalService.generateSignal(symbol, candles1D, candles4H, null);

        // Assert
        assertEquals(0, signal.getCurrentPrice());
    }

    @Test
    void generateSignal_EmptyCandles15m_ReturnsHoldSignalWithZeroPrice() {
        // Arrange
        String symbol = "BTCUSDT";
        List<CandleData> candles1D = createTestCandles();
        List<CandleData> candles4H = createTestCandles();
        List<CandleData> candles15m = Arrays.asList();

        when(signalFilterService.isSymbolValid(symbol)).thenReturn(true);
        when(signalFilterService.hasSufficientData(candles1D, candles4H, candles15m)).thenReturn(true);
        when(trendAnalysisService.determineTrend1D(candles1D)).thenReturn(Trend.SIDEWAYS);

        // Act
        CryptoSignal signal = cryptoSignalService.generateSignal(symbol, candles1D, candles4H, candles15m);

        // Assert
        assertEquals(0, signal.getCurrentPrice());
    }

    private List<CandleData> createTestCandles() {
        return Arrays.asList(
            createCandle(80000.0),
            createCandle(81000.0),
            createCandle(82000.0)
        );
    }

    private CandleData createCandle(double closePrice) {
        CandleData candle = new CandleData();
        candle.setSymbol("BTCUSDT");
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
