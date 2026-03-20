package com.christianjaena.crypto_signals.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandleData {
    private String symbol;
    private LocalDateTime timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
    private String timeframe; // "1D", "4H", "15m"
}
