package com.christianjaena.crypto_signals.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CryptoSignal {
    private String symbol;
    private Trend trend1D;
    private Setup setup4H;
    private Signal entry15m;
    private int confidence;
    private List<String> notes;
}
