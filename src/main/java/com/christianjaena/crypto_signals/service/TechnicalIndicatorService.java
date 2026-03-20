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
        
        // Calculate MACD
        MACDResult macd = calculateMACD(closes, 12, 26, 9);
        indicators.setMacdLine(macd.getMacdLine());
        indicators.setMacdSignal(macd.getSignalLine());
        indicators.setMacdHistogram(macd.getHistogram());
        
        // Calculate Bollinger Bands
        BollingerBandsResult bb = calculateBollingerBands(closes, 20, 2);
        indicators.setBollingerUpper(bb.getUpper());
        indicators.setBollingerMiddle(bb.getMiddle());
        indicators.setBollingerLower(bb.getLower());
        indicators.setBollingerWidth(bb.getWidth());

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
            
            stochKValues[i - stochPeriod + 1] = (highestRSI == lowestRSI) ? 50.0 : 
                ((rsiValues[i] - lowestRSI) / (highestRSI - lowestRSI)) * 100;
        }

        double currentK = stochKValues[stochKValues.length - 1];
        double currentD = calculateSMA(stochKValues, dPeriod);

        // Ensure values are within bounds
        currentK = Math.max(0, Math.min(100, currentK));
        currentD = Math.max(0, Math.min(100, currentD));

        return new StochRSIResult(currentK, currentD);
    }

    private MACDResult calculateMACD(double[] prices, int fastPeriod, int slowPeriod, int signalPeriod) {
        double[] fastEMA = new double[prices.length];
        double[] slowEMA = new double[prices.length];
        
        // Calculate fast EMA
        fastEMA[0] = prices[0];
        double fastMultiplier = 2.0 / (fastPeriod + 1);
        for (int i = 1; i < prices.length; i++) {
            fastEMA[i] = (prices[i] * fastMultiplier) + (fastEMA[i - 1] * (1 - fastMultiplier));
        }
        
        // Calculate slow EMA
        slowEMA[0] = prices[0];
        double slowMultiplier = 2.0 / (slowPeriod + 1);
        for (int i = 1; i < prices.length; i++) {
            slowEMA[i] = (prices[i] * slowMultiplier) + (slowEMA[i - 1] * (1 - slowMultiplier));
        }
        
        // Calculate MACD line
        double[] macdLine = new double[prices.length];
        for (int i = 0; i < prices.length; i++) {
            macdLine[i] = fastEMA[i] - slowEMA[i];
        }
        
        // Calculate signal line
        double[] signalLine = new double[prices.length];
        double signalMultiplier = 2.0 / (signalPeriod + 1);
        signalLine[0] = macdLine[0];
        for (int i = 1; i < prices.length; i++) {
            signalLine[i] = (macdLine[i] * signalMultiplier) + (signalLine[i - 1] * (1 - signalMultiplier));
        }
        
        // Calculate histogram
        double histogram = macdLine[macdLine.length - 1] - signalLine[signalLine.length - 1];
        
        return new MACDResult(macdLine[macdLine.length - 1], signalLine[signalLine.length - 1], histogram);
    }
    
    private BollingerBandsResult calculateBollingerBands(double[] prices, int period, double multiplier) {
        double[] sma = new double[prices.length];
        double[] stdDev = new double[prices.length];
        
        for (int i = period - 1; i < prices.length; i++) {
            double sum = 0;
            for (int j = i - period + 1; j <= i; j++) {
                sum += prices[j];
            }
            sma[i] = sum / period;
            
            double variance = 0;
            for (int j = i - period + 1; j <= i; j++) {
                variance += Math.pow(prices[j] - sma[i], 2);
            }
            stdDev[i] = Math.sqrt(variance / period);
        }
        
        int lastIndex = prices.length - 1;
        double middle = sma[lastIndex];
        double upper = middle + (multiplier * stdDev[lastIndex]);
        double lower = middle - (multiplier * stdDev[lastIndex]);
        double width = (upper - lower) / middle;
        
        return new BollingerBandsResult(upper, middle, lower, width);
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
    
    private static class MACDResult {
        private final double macdLine;
        private final double signalLine;
        private final double histogram;
        
        public MACDResult(double macdLine, double signalLine, double histogram) {
            this.macdLine = macdLine;
            this.signalLine = signalLine;
            this.histogram = histogram;
        }
        
        public double getMacdLine() {
            return macdLine;
        }
        
        public double getSignalLine() {
            return signalLine;
        }
        
        public double getHistogram() {
            return histogram;
        }
    }
    
    private static class BollingerBandsResult {
        private final double upper;
        private final double middle;
        private final double lower;
        private final double width;
        
        public BollingerBandsResult(double upper, double middle, double lower, double width) {
            this.upper = upper;
            this.middle = middle;
            this.lower = lower;
            this.width = width;
        }
        
        public double getUpper() {
            return upper;
        }
        
        public double getMiddle() {
            return middle;
        }
        
        public double getLower() {
            return lower;
        }
        
        public double getWidth() {
            return width;
        }
    }
}
