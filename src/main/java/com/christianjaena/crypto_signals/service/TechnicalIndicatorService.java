package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import com.christianjaena.crypto_signals.model.TechnicalIndicators;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TechnicalIndicatorService {

    public TechnicalIndicators calculateIndicators(List<CandleData> candles) {
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candle data cannot be null or empty");
        }

        double[] closes = candles.stream().mapToDouble(CandleData::getClose).toArray();
        double[] volumes = candles.stream().mapToDouble(CandleData::getVolume).toArray();

        TechnicalIndicators indicators = new TechnicalIndicators();
        
        indicators.setEma50(calculateEMA(closes, 50));
        indicators.setEma200(calculateEMA(closes, 200));
        indicators.setRsi(calculateRSI(closes, 14));
        
        StochRSIResult stochRSI = calculateStochRSI(closes, 14, 14, 3, 3);
        indicators.setStochRsiK(stochRSI.getK());
        indicators.setStochRsiD(stochRSI.getD());
        
        indicators.setVolumeAverage20(calculateSMA(volumes, 20));

        return indicators;
    }

    protected double calculateEMA(double[] prices, int period) {
        if (prices.length < period) {
            return prices[prices.length - 1];
        }

        double multiplier = 2.0 / (period + 1);
        double ema = prices[0];

        for (int i = 1; i < prices.length; i++) {
            ema = (prices[i] * multiplier) + (ema * (1 - multiplier));
        }

        return ema;
    }

    protected double calculateSMA(double[] values, int period) {
        if (values.length < period) {
            return values[values.length - 1];
        }

        double sum = 0;
        for (int i = values.length - period; i < values.length; i++) {
            sum += values[i];
        }

        return sum / period;
    }

    protected double calculateRSI(double[] prices, int period) {
        if (prices.length < period + 1) {
            return 50.0;
        }

        double[] gains = new double[prices.length - 1];
        double[] losses = new double[prices.length - 1];

        for (int i = 1; i < prices.length; i++) {
            double change = prices[i] - prices[i - 1];
            gains[i - 1] = change > 0 ? change : 0;
            losses[i - 1] = change < 0 ? -change : 0;
        }

        double avgGain = calculateSMA(gains, period);
        double avgLoss = calculateSMA(losses, period);

        double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    private StochRSIResult calculateStochRSI(double[] prices, int rsiPeriod, int stochPeriod, int kPeriod, int dPeriod) {
        double[] rsiValues = new double[prices.length];
        
        for (int i = 0; i < prices.length; i++) {
            if (i < rsiPeriod) {
                rsiValues[i] = 50.0;
            } else {
                double[] subset = new double[rsiPeriod + 1];
                System.arraycopy(prices, i - rsiPeriod, subset, 0, rsiPeriod + 1);
                rsiValues[i] = calculateRSI(subset, rsiPeriod);
            }
        }

        if (rsiValues.length < stochPeriod) {
            return new StochRSIResult(50.0, 50.0);
        }

        double[] stochKValues = new double[rsiValues.length - stochPeriod + 1];
        
        for (int i = stochPeriod - 1; i < rsiValues.length; i++) {
            double highestRSI = Double.MIN_VALUE;
            double lowestRSI = Double.MAX_VALUE;
            
            for (int j = i - stochPeriod + 1; j <= i; j++) {
                highestRSI = Math.max(highestRSI, rsiValues[j]);
                lowestRSI = Math.min(lowestRSI, rsiValues[j]);
            }
            
            stochKValues[i - stochPeriod + 1] = ((rsiValues[i] - lowestRSI) / (highestRSI - lowestRSI)) * 100;
        }

        double currentK = stochKValues[stochKValues.length - 1];
        double currentD = calculateSMA(stochKValues, dPeriod);

        return new StochRSIResult(currentK, currentD);
    }

    private static class StochRSIResult {
        private final double k;
        private final double d;

        public StochRSIResult(double k, double d) {
            this.k = k;
            this.d = d;
        }

        public double getK() {
            return k;
        }

        public double getD() {
            return d;
        }
    }
}
