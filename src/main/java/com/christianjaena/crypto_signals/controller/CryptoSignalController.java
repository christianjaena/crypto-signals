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
import java.util.ArrayList;
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

    @Operation(summary = "Generate signals for multiple symbols", description = "Generate trading signals by fetching real-time data from MEXC exchange. Symbols should be provided without USDT suffix (e.g., BTC,ETH,SOL).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signals generated successfully",
            content = @Content(schema = @Schema(implementation = CryptoSignal.class))),
        @ApiResponse(responseCode = "400", description = "Invalid symbols or API error")
    })
    @GetMapping("/generate")
    public ResponseEntity<List<CryptoSignal>> generateSignals(
            @Parameter(description = "Trading symbols without USDT suffix (e.g., BTC,ETH,SOL)", required = true)
            @RequestParam String symbols) {
        try {
            String[] symbolArray = symbols.split(",");
            List<CryptoSignal> signals = new ArrayList<>();
            
            for (String rawSymbol : symbolArray) {
                String symbol = rawSymbol.trim().toUpperCase();
                String fullSymbol = symbol + "USDT";
                
                if (!mexcApiService.isSymbolValid(fullSymbol)) {
                    CryptoSignal errorSignal = new CryptoSignal();
                    errorSignal.setSymbol(fullSymbol);
                    errorSignal.setNotes(List.of("Invalid symbol: " + symbol));
                    signals.add(errorSignal);
                    continue;
                }

                List<CandleData> candles1D = mexcApiService.getKlineData(fullSymbol, "1d", 200);
                List<CandleData> candles4H = mexcApiService.getKlineData(fullSymbol, "4h", 100);
                List<CandleData> candles15m = mexcApiService.getKlineData(fullSymbol, "15m", 50);

                CryptoSignal signal = cryptoSignalService.generateSignal(fullSymbol, candles1D, candles4H, candles15m);
                signals.add(signal);
            }
            
            return ResponseEntity.ok(signals);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
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
}
