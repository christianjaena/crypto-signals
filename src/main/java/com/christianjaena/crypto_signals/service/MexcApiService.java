package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.config.MexcConfig;
import com.christianjaena.crypto_signals.model.CandleData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class MexcApiService {

    private final MexcConfig mexcConfig;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public MexcApiService(MexcConfig mexcConfig) {
        this(mexcConfig, RestClient.create());
    }

    public MexcApiService(MexcConfig mexcConfig, RestClient restClient) {
        this.mexcConfig = mexcConfig;
        this.restClient = restClient;
        this.objectMapper = new ObjectMapper();
    }

    private List<CandleData> parseKlineData(String response, String symbol, String interval) {
        List<CandleData> candleDataList = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(response);
            
            for (JsonNode candle : root) {
                CandleData candleData = new CandleData();
                candleData.setSymbol(symbol);
                
                long timestamp = candle.get(0).asLong();
                candleData.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC));
                
                candleData.setOpen(candle.get(1).asDouble());
                candleData.setHigh(candle.get(2).asDouble());
                candleData.setLow(candle.get(3).asDouble());
                candleData.setClose(candle.get(4).asDouble());
                candleData.setVolume(candle.get(5).asDouble());
                candleData.setTimeframe(interval);
                
                candleDataList.add(candleData);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse kline data", e);
        }
        
        return candleDataList;
    }

    public boolean isSymbolValid(String symbol) {
        try {
            String cleanSymbol = symbol.replace("/", "");
            String url = String.format("%s/api/v3/exchangeInfo?symbol=%s",
                mexcConfig.getBaseUrl(), cleanSymbol);
            
            String response = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode symbols = root.get("symbols");
            
            if (symbols != null && symbols.isArray()) {
                for (JsonNode symbolInfo : symbols) {
                    if (cleanSymbol.equals(symbolInfo.get("symbol").asText())) {
                        return true;
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public List<CandleData> getKlineData(String symbol, String interval, int limit) {
        try {
            String url = String.format("%s/api/v3/klines?symbol=%s&interval=%s&limit=%d",
                    mexcConfig.getBaseUrl(), symbol.replace("/", ""), interval, limit);

            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            return parseKlineData(response, symbol, interval);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch kline data from MEXC", e);
        }
    }
}
