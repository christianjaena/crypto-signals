package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.config.MexcConfig;
import com.christianjaena.crypto_signals.model.CandleData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MexcApiServiceTest {

    @Mock
    private MexcConfig mexcConfig;

    @Mock
    private RestClient restClient;

    private MexcApiService mexcApiService;

    @BeforeEach
    void setUp() {
        when(mexcConfig.getBaseUrl()).thenReturn("https://api.mexc.com");
        mexcApiService = new MexcApiService(mexcConfig, restClient);
    }

    @Test
    void getKlineData_WithValidResponse_ReturnsCandleData() throws Exception {
        String symbol = "BTCUSDT";
        String interval = "1d";
        int limit = 5;
        String mockResponse = "[[1640995200000,\"47000.0\",\"47500.0\",\"46800.0\",\"47200.0\",\"1000.5\"]]";

        // Mock RestClient fluent API
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(mockResponse);

        List<CandleData> result = mexcApiService.getKlineData(symbol, interval, limit);

        assertNotNull(result);
        assertEquals(1, result.size());
        
        CandleData firstCandle = result.get(0);
        assertEquals("BTCUSDT", firstCandle.getSymbol());
        assertEquals(47000.0, firstCandle.getOpen());
        assertEquals(47500.0, firstCandle.getHigh());
        assertEquals(46800.0, firstCandle.getLow());
        assertEquals(47200.0, firstCandle.getClose());
        assertEquals(1000.5, firstCandle.getVolume());
        assertEquals("1d", firstCandle.getTimeframe());

        verify(restClient).get();
        verify(requestHeadersUriSpec).uri("https://api.mexc.com/api/v3/klines?symbol=BTCUSDT&interval=1d&limit=5");
        verify(requestHeadersUriSpec).retrieve();
        verify(responseSpec).body(String.class);
    }

}