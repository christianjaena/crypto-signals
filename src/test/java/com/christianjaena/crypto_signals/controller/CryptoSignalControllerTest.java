package com.christianjaena.crypto_signals.controller;

import com.christianjaena.crypto_signals.dto.SignalRequest;
import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.CryptoSignal;
import com.christianjaena.crypto_signals.service.CryptoSignalService;
import com.christianjaena.crypto_signals.service.MexcApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
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

    @Autowired
    private ObjectMapper objectMapper;

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
        // Configure ObjectMapper for Java 8 time support
        objectMapper.registerModule(new JavaTimeModule());
        
        // Create mock candle data
        mockCandles1D = createMockCandles("BTC/USDT", "1D", 200);
        mockCandles4H = createMockCandles("BTC/USDT", "4H", 100);
        mockCandles15m = createMockCandles("BTC/USDT", "15m", 50);
        
        // Create mock signal
        mockSignal = new CryptoSignal();
        mockSignal.setSymbol("BTC/USDT");
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
    void generateSignalForSymbol_WithValidSymbol_ReturnsSignal() throws Exception {
        // Arrange
        when(mexcApiService.isSymbolValid("BTC/USDT")).thenReturn(true);
        when(mexcApiService.getKlineData("BTC/USDT", "1d", 200)).thenReturn(mockCandles1D);
        when(mexcApiService.getKlineData("BTC/USDT", "4h", 100)).thenReturn(mockCandles4H);
        when(mexcApiService.getKlineData("BTC/USDT", "15m", 50)).thenReturn(mockCandles15m);
        when(cryptoSignalService.generateSignal(eq("BTC/USDT"), eq(mockCandles1D), eq(mockCandles4H), eq(mockCandles15m)))
                .thenReturn(mockSignal);

        // Act & Assert
        mockMvc.perform(get("/api/signals/generate").param("symbol", "BTC/USDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC/USDT"))
                .andExpect(jsonPath("$.confidence").value(85));

        verify(mexcApiService).isSymbolValid("BTC/USDT");
        verify(cryptoSignalService).generateSignal(eq("BTC/USDT"), eq(mockCandles1D), eq(mockCandles4H), eq(mockCandles15m));
    }

    @Test
    void generateSignalForSymbol_WithInvalidSymbol_ReturnsNotFound() throws Exception {
        // Arrange
        when(mexcApiService.isSymbolValid("INVALID")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/signals/generate").param("symbol", "INVALID"))
                .andExpect(status().isNotFound());

        verify(mexcApiService).isSymbolValid("INVALID");
        verify(mexcApiService, never()).getKlineData(anyString(), anyString(), anyInt());
    }

    @Test
    void generateSignal_WithValidRequest_ReturnsSignal() throws Exception {
        // Arrange
        SignalRequest request = new SignalRequest();
        request.setSymbol("BTC/USDT");
        request.setCandles1D(mockCandles1D);
        request.setCandles4H(mockCandles4H);
        request.setCandles15m(mockCandles15m);

        when(cryptoSignalService.generateSignal(eq("BTC/USDT"), eq(mockCandles1D), eq(mockCandles4H), eq(mockCandles15m)))
                .thenReturn(mockSignal);

        // Act & Assert
        mockMvc.perform(post("/api/signals/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC/USDT"))
                .andExpect(jsonPath("$.confidence").value(85))
                .andExpect(jsonPath("$.notes").isArray());

        verify(cryptoSignalService).generateSignal(eq("BTC/USDT"), eq(mockCandles1D), eq(mockCandles4H), eq(mockCandles15m));
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
