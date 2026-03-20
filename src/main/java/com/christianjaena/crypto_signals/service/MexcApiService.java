package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.config.MexcConfig;
import com.christianjaena.crypto_signals.model.CandleData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MexcApiService {

    private final MexcConfig mexcConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MexcApiService(MexcConfig mexcConfig) {
        this.mexcConfig = mexcConfig;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<CandleData> getKlineData(String symbol, String interval, int limit) {
        try {
            String url = String.format("%s/api/v3/klines?symbol=%s&interval=%s&limit=%d",
                mexcConfig.getBaseUrl(), symbol.replace("/", ""), interval, limit);

            String response = restTemplate.getForObject(url, String.class);
            return parseKlineData(response, symbol, interval);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch kline data from MEXC", e);
        }
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

    public List<String> getAvailableSymbols() {
        try {
            String url = mexcConfig.getBaseUrl() + "/api/v3/ticker/24hr";
            String response = restTemplate.getForObject(url, String.class);
            
            List<String> symbols = new ArrayList<>();
            JsonNode root = objectMapper.readTree(response);
            
            for (JsonNode ticker : root) {
                String symbol = ticker.get("symbol").asText();
                if (symbol.endsWith("USDT")) {
                    symbols.add(symbol.replace("USDT", "/USDT"));
                }
            }
            
            return symbols;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch symbols from MEXC", e);
        }
    }

    public boolean isSymbolValid(String symbol) {
        try {
            String cleanSymbol = symbol.replace("/", "");
            String url = String.format("%s/api/v3/exchangeInfo?symbol=%s",
                mexcConfig.getBaseUrl(), cleanSymbol);
            
            String response = restTemplate.getForObject(url, String.class);
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

    private String generateSignature(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                mexcConfig.getSecretKey().getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
