package com.christianjaena.crypto_signals.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElliottWave {
    public enum WaveType {
        IMPULSE, CORRECTIVE, UNKNOWN
    }
    
    public enum WaveNumber {
        ONE, TWO, THREE, FOUR, FIVE,
        A, B, C,
        UNKNOWN
    }
    
    private WaveType waveType;
    private WaveNumber waveNumber;
    private double confidence;
    private String description;
    
    public static ElliottWave createImpulseWave(WaveNumber waveNumber, double confidence) {
        return new ElliottWave(WaveType.IMPULSE, waveNumber, confidence, 
            "Impulse wave " + waveNumber + " detected");
    }
    
    public static ElliottWave createCorrectiveWave(WaveNumber waveNumber, double confidence) {
        return new ElliottWave(WaveType.CORRECTIVE, waveNumber, confidence, 
            "Corrective wave " + waveNumber + " detected");
    }
    
    public static ElliottWave createUnknown() {
        return new ElliottWave(WaveType.UNKNOWN, WaveNumber.UNKNOWN, 0.0, 
            "No clear Elliott Wave pattern detected");
    }
    
    public boolean isImpulseWave() {
        return waveType == WaveType.IMPULSE;
    }
    
    public boolean isCorrectiveWave() {
        return waveType == WaveType.CORRECTIVE;
    }
    
    public boolean isStrongImpulseWave() {
        return isImpulseWave() && 
               (waveNumber == WaveNumber.THREE || waveNumber == WaveNumber.FIVE) &&
               confidence >= 0.7;
    }
}
