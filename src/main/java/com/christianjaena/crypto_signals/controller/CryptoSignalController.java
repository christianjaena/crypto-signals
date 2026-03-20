package com.christianjaena.crypto_signals.controller;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.CryptoSignal;
import com.christianjaena.crypto_signals.service.CryptoSignalService;
import com.christianjaena.crypto_signals.service.MexcApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/signals")
@CrossOrigin(origins = "*")
@Tag(name = "Crypto Signal Generator", description = "API for generating cryptocurrency trading signals")
public class CryptoSignalController {

    private final CryptoSignalService cryptoSignalService;
    private final MexcApiService mexcApiService;

    public CryptoSignalController(CryptoSignalService cryptoSignalService, MexcApiService mexcApiService) {
        this.cryptoSignalService = cryptoSignalService;
        this.mexcApiService = mexcApiService;
    }

    @Operation(summary = "Generate signal with custom data", description = "Generate trading signal using provided candle data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signal generated successfully",
            content = @Content(schema = @Schema(implementation = CryptoSignal.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping("/generate")
    public ResponseEntity<CryptoSignal> generateSignal(@RequestBody SignalRequest request) {
        try {
            CryptoSignal signal = cryptoSignalService.generateSignal(
                request.getSymbol(),
                request.getCandles1D(),
                request.getCandles4H(),
                request.getCandles15m()
            );
            return ResponseEntity.ok(signal);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Generate signal using MEXC data", description = "Generate trading signal by fetching real-time data from MEXC exchange")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signal generated successfully",
            content = @Content(schema = @Schema(implementation = CryptoSignal.class))),
        @ApiResponse(responseCode = "400", description = "Invalid symbol or API error"),
        @ApiResponse(responseCode = "404", description = "Symbol not found")
    })
    @GetMapping("/generate/{symbol}")
    public ResponseEntity<CryptoSignal> generateSignalForSymbol(
            @Parameter(description = "Trading symbol (e.g., BTC/USDT)", required = true)
            @PathVariable String symbol) {
        try {
            if (!mexcApiService.isSymbolValid(symbol)) {
                return ResponseEntity.notFound().build();
            }

            List<CandleData> candles1D = mexcApiService.getKlineData(symbol, "1d", 200);
            List<CandleData> candles4H = mexcApiService.getKlineData(symbol, "4h", 100);
            List<CandleData> candles15m = mexcApiService.getKlineData(symbol, "15m", 50);

            CryptoSignal signal = cryptoSignalService.generateSignal(symbol, candles1D, candles4H, candles15m);
            return ResponseEntity.ok(signal);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Generate signal with mock data", description = "Generate trading signal using simulated data for testing")
    @GetMapping("/generate/{symbol}/mock")
    public ResponseEntity<CryptoSignal> generateMockSignal(
            @Parameter(description = "Trading symbol (e.g., BTC/USDT)", required = true)
            @PathVariable String symbol) {
        try {
            List<CandleData> candles1D = generateMockCandleData(symbol, "1D", 200);
            List<CandleData> candles4H = generateMockCandleData(symbol, "4H", 100);
            List<CandleData> candles15m = generateMockCandleData(symbol, "15m", 50);

            CryptoSignal signal = cryptoSignalService.generateSignal(symbol, candles1D, candles4H, candles15m);
            return ResponseEntity.ok(signal);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get available symbols", description = "Retrieve list of available trading symbols from MEXC")
    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getAvailableSymbols() {
        try {
            List<String> symbols = mexcApiService.getAvailableSymbols();
            return ResponseEntity.ok(symbols);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Clear signal history", description = "Clear signal history for a specific symbol")
    @DeleteMapping("/history/{symbol}")
    public ResponseEntity<Void> clearSignalHistory(
            @Parameter(description = "Trading symbol", required = true)
            @PathVariable String symbol) {
        cryptoSignalService.clearSignalHistory(symbol);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Clear all signal history", description = "Clear signal history for all symbols")
    @DeleteMapping("/history")
    public ResponseEntity<Void> clearAllSignalHistory() {
        cryptoSignalService.clearAllSignalHistory();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Health check", description = "Check if the service is running")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "timestamp", LocalDateTime.now().toString(),
            "service", "crypto-signals",
            "exchange", "MEXC"
        ));
    }

    private List<CandleData> generateMockCandleData(String symbol, String timeframe, int count) {
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

    @Schema(description = "Signal generation request")
    public static class SignalRequest {
        @Schema(description = "Trading symbol", example = "BTC/USDT")
        private String symbol;
        
        @Schema(description = "1D candlestick data")
        private List<CandleData> candles1D;
        
        @Schema(description = "4H candlestick data")
        private List<CandleData> candles4H;
        
        @Schema(description = "15m candlestick data")
        private List<CandleData> candles15m;

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public List<CandleData> getCandles1D() { return candles1D; }
        public void setCandles1D(List<CandleData> candles1D) { this.candles1D = candles1D; }

        public List<CandleData> getCandles4H() { return candles4H; }
        public void setCandles4H(List<CandleData> candles4H) { this.candles4H = candles4H; }

        public List<CandleData> getCandles15m() { return candles15m; }
        public void setCandles15m(List<CandleData> candles15m) { this.candles15m = candles15m; }
    }
}
