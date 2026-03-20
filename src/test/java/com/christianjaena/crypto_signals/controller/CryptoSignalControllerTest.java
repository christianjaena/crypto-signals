package com.christianjaena.crypto_signals.controller;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.CryptoSignal;
import com.christianjaena.crypto_signals.service.CryptoSignalService;
import com.christianjaena.crypto_signals.service.MexcApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CryptoSignalController.class)
class CryptoSignalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CryptoSignalService cryptoSignalService;

    @MockitoBean
    private MexcApiService mexcApiService;

    private CryptoSignal mockSignal;
    private List<CandleData> mockCandles1D;
    private List<CandleData> mockCandles4H;
    private List<CandleData> mockCandles15m;

    @BeforeEach
    void setUp() {
        // Create mock candle data
        mockCandles1D = createMockCandles("BTCUSDT", "1D", 200);
        mockCandles4H = createMockCandles("BTCUSDT", "4H", 100);
        mockCandles15m = createMockCandles("BTCUSDT", "15m", 50);
        
        // Create mock signal
        mockSignal = new CryptoSignal();
        mockSignal.setSymbol("BTCUSDT");
        mockSignal.setCurrentPrice(85000.50);
        mockSignal.setStopLoss(82000.00);
        mockSignal.setPredictionPriceGrowth(92000.00);
        mockSignal.setConfidence(85);
        mockSignal.setNotes(Arrays.asList("Strong uptrend", "High volume"));
    }

    @Test
    void health_ReturnsHealthStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/signals/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.service").value("crypto-signals"))
                .andExpect(jsonPath("$.exchange").value("MEXC"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void generateSignals_WithValidSymbols_ReturnsSignals() throws Exception {
        // Arrange
        when(mexcApiService.isSymbolValid("BTCUSDT")).thenReturn(true);
        when(mexcApiService.getKlineData("BTCUSDT", "1d", 200)).thenReturn(mockCandles1D);
        when(mexcApiService.getKlineData("BTCUSDT", "4h", 100)).thenReturn(mockCandles4H);
        when(mexcApiService.getKlineData("BTCUSDT", "15m", 50)).thenReturn(mockCandles15m);
        when(cryptoSignalService.generateSignal(eq("BTCUSDT"), eq(mockCandles1D), eq(mockCandles4H), eq(mockCandles15m)))
                .thenReturn(mockSignal);

        // Act & Assert
        mockMvc.perform(get("/api/signals/generate").param("symbols", "BTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$[0].currentPrice").value(85000.50))
                .andExpect(jsonPath("$[0].stopLoss").value(82000.00))
                .andExpect(jsonPath("$[0].predictionPriceGrowth").value(92000.00))
                .andExpect(jsonPath("$[0].confidence").value(85));

        verify(mexcApiService).isSymbolValid("BTCUSDT");
        verify(cryptoSignalService).generateSignal(eq("BTCUSDT"), eq(mockCandles1D), eq(mockCandles4H), eq(mockCandles15m));
    }

    @Test
    void generateSignals_WithMultipleSymbols_ReturnsSignals() throws Exception {
        // Arrange
        CryptoSignal ethSignal = new CryptoSignal();
        ethSignal.setSymbol("ETHUSDT");
        ethSignal.setCurrentPrice(3200.75);
        ethSignal.setStopLoss(3100.00);
        ethSignal.setPredictionPriceGrowth(3500.00);
        ethSignal.setConfidence(78);

        when(mexcApiService.isSymbolValid("BTCUSDT")).thenReturn(true);
        when(mexcApiService.isSymbolValid("ETHUSDT")).thenReturn(true);
        when(mexcApiService.getKlineData(anyString(), anyString(), anyInt())).thenReturn(mockCandles1D);
        when(cryptoSignalService.generateSignal(eq("BTCUSDT"), any(), any(), any())).thenReturn(mockSignal);
        when(cryptoSignalService.generateSignal(eq("ETHUSDT"), any(), any(), any())).thenReturn(ethSignal);

        // Act & Assert
        mockMvc.perform(get("/api/signals/generate").param("symbols", "BTC,ETH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$[1].symbol").value("ETHUSDT"));
    }

    @Test
    void generateSignals_WithInvalidSymbol_ReturnsErrorSignal() throws Exception {
        // Arrange
        when(mexcApiService.isSymbolValid("INVALIDUSDT")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/signals/generate").param("symbols", "INVALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("INVALIDUSDT"))
                .andExpect(jsonPath("$[0].notes[0]").value("Invalid symbol: INVALID"));

        verify(mexcApiService).isSymbolValid("INVALIDUSDT");
        verify(mexcApiService, never()).getKlineData(anyString(), anyString(), anyInt());
    }

    private List<CandleData> createMockCandles(String symbol, String timeframe, int count) {
        List<CandleData> candles = new java.util.ArrayList<>();
        double basePrice = 50000.0;
        LocalDateTime now = LocalDateTime.now();

        for (int i = count - 1; i >= 0; i--) {
            double randomChange = (Math.random() - 0.5) * 0.02;
            double open = basePrice * (1 + randomChange);
            double close = open * (1 + (Math.random() - 0.5) * 0.01);
            double high = Math.max(open, close) * (1 + Math.random() * 0.005);
            double low = Math.min(open, close) * (1 - Math.random() * 0.005);
            double volume = 1000000 + Math.random() * 5000000;

            CandleData candle = new CandleData();
            candle.setSymbol(symbol);
            candle.setTimestamp(now.minusHours(i * getTimeframeHours(timeframe)));
            candle.setOpen(open);
            candle.setHigh(high);
            candle.setLow(low);
            candle.setClose(close);
            candle.setVolume(volume);
            candle.setTimeframe(timeframe);

            candles.add(candle);
            basePrice = close;
        }

        return candles;
    }

    private int getTimeframeHours(String timeframe) {
        switch (timeframe) {
            case "1D": return 24;
            case "4H": return 4;
            case "15m": return 0;
            default: return 1;
        }
    }
}
