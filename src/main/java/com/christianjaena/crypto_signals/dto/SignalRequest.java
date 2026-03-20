package com.christianjaena.crypto_signals.dto;

import com.christianjaena.crypto_signals.model.CandleData;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Signal generation request")
public class SignalRequest {
    @Schema(description = "Trading symbols (e.g., BTC, ETH, SOL)", example = "[\"BTC\", \"ETH\"]")
    private List<String> symbols;
    
    @Schema(description = "1D candlestick data")
    private List<CandleData> candles1D;
    
    @Schema(description = "4H candlestick data")
    private List<CandleData> candles4H;
    
    @Schema(description = "15m candlestick data")
    private List<CandleData> candles15m;

    public List<String> getSymbols() { return symbols; }
    public void setSymbols(List<String> symbols) { this.symbols = symbols; }

    public List<CandleData> getCandles1D() { return candles1D; }
    public void setCandles1D(List<CandleData> candles1D) { this.candles1D = candles1D; }

    public List<CandleData> getCandles4H() { return candles4H; }
    public void setCandles4H(List<CandleData> candles4H) { this.candles4H = candles4H; }

    public List<CandleData> getCandles15m() { return candles15m; }
    public void setCandles15m(List<CandleData> candles15m) { this.candles15m = candles15m; }
}
