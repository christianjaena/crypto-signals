# Ultimate Crypto Signal Generator Plan

---

## Inputs

- **Symbols** (e.g., BTC, ETH, SOL) — USDT is automatically appended
- Multiple symbols can be passed in a single request
- **Candle data** (OHLCV) for three timeframes:
  - HTF = 1D
  - MTF = 4H
  - LTF = 15m
- **Indicators:**
  - EMA50, EMA200
  - RSI(14)
  - StochRSI (%K/%D)
  - MACD (line, signal, histogram)
  - Bollinger Bands (optional for volatility)
  - Optional: Elliott Wave pattern detection (Wave 1–5, ABC correction)
- **Optional filters:**
  - 1D RSI 45–55 → sideways → skip signal
  - Candle volume < 20-period average → skip signal
  - Too close to previous signal → skip signal
  - High-impact news events → skip signal

---

## Step 1 — Determine HTF Trend (1D)

**Logic:**

- **Bullish trend:**
  - Price > EMA200
  - EMA50 > EMA200
  - RSI > 50

- **Bearish trend:**
  - Price < EMA200
  - EMA50 < EMA200
  - RSI < 50

- **Sideways trend:**
  - Otherwise → trend = SIDEWAYS → HOLD

**Checklist:**
- [ ] Trend determined
- [ ] Skip if sideways

---

## Step 2 — Identify Setup Zone (4H)

**Logic:**

- **Bullish trend → LONG setup:**
  - Price near EMA50 (~±2%)
  - RSI 30–40

- **Bearish trend → SHORT setup:**
  - Price near EMA50 (~±2%)
  - RSI 60–70

- **Otherwise:** setup = None → HOLD

**Checklist:**
- [ ] Setup validated
- [ ] Skip if no setup

---

## Step 3 — Confirm Entry (15m)

**Logic:**

- **LONG setup:**
  - RSI < 30
  - StochRSI %K crosses above %D

- **SHORT setup:**
  - RSI > 70
  - StochRSI %K crosses below %D

- **Otherwise:** HOLD

**Checklist:**
- [ ] Entry confirmed
- [ ] Signal = BUY / SELL / HOLD

---

## Step 4 — Multi-Timeframe Alignment (Optional Confidence Boost)

**Logic:**

- All timeframes agree (HTF + MTF + LTF) → stronger signal
- Partial alignment → moderate confidence

**Checklist:**
- [ ] Confirm trend alignment across timeframes

---

## Step 5 — Elliott Wave Filter

**Logic:**

- **Impulse waves (1–5)** → align with trend → increase confidence
- **Corrective waves (A, B, C)** → opposite trend → skip signal or reduce confidence
- Wave 3 / Wave C → strongest confirmation

**Checklist:**
- [ ] Elliott Wave detected
- [ ] Confirm trade aligns with impulse wave
- [ ] Skip or reduce confidence if corrective wave

---

## Step 6 — Candlestick Pattern Confirmation

**Logic:**

- Bullish engulfing / hammer for LONG
- Bearish engulfing / shooting star for SHORT
- Patterns must coincide with support/resistance or trend

**Checklist:**
- [ ] Entry candlestick pattern confirmed

---

## Step 7 — Support & Resistance / Fibonacci Levels

**Logic:**

- LONG near strong support or Fibonacci retracement (38–61.8%)
- SHORT near strong resistance or Fibonacci retracement
- Skip trades at weak levels or overextended moves

**Checklist:**
- [ ] Confirm trade aligns with key levels

---

## Step 8 — Volume & Momentum Confirmation

**Logic:**

- Volume > 20-period average → stronger signal
- MACD aligns with RSI/StochRSI:
  - LONG: MACD line > signal, histogram increasing
  - SHORT: MACD line < signal, histogram decreasing

**Checklist:**
- [ ] Volume sufficient
- [ ] MACD confirms momentum

---

## Step 9 — Divergence & Volatility Filter

**Logic:**

- RSI / MACD divergence → higher predictive value
- Bollinger Band width → skip if too narrow (low volatility)

**Checklist:**
- [ ] Check divergence
- [ ] Confirm volatility is adequate

---

## Step 10 — Time/Session & News Filter

**Logic:**

- Only trade during high-liquidity sessions
  - BTC/USDT: London + NY overlap preferred
- Skip trades during high-impact news events

**Checklist:**
- [ ] Session filter passed
- [ ] No news events

---

## Step 11 — Confidence Scoring

**Base:** 50%

| Factor | Confidence Boost |
|--------|----------------|
| Trend alignment HTF/MTF/LTF | +10–15% |
| EMA + RSI setup | +15% |
| StochRSI + MACD confirmation | +15% |
| Elliott Wave impulse | +10–15% |
| Volume & Support/Resistance | +10% |
| Candlestick pattern confirmation | +10% |
| Divergence | +5% |
| Volatility adequate | +5% |
| Session/time filter | +5% |
| **Total max confidence** | 100% |

**Checklist:**
- [ ] Calculate total confidence
- [ ] Cap at 100%

---

## Step 12 — Optional Filters

- Skip signal if:
  - 1D RSI 45–55 (sideways)
  - Candle volume < 20-period average
  - Too close to previous signal
  - Elliott Wave unclear / corrective wave
  - Overextended price outside key levels

**Checklist:**
- [ ] Filter conditions checked

---

## Step 13 — Generate Signal Output

**Example JSON:**

```json
{
  "symbol": "BTCUSDT",
  "currentPrice": 85000.50,
  "stopLoss": 82000.00,
  "predictionPriceGrowth": 92000.00,
  "trend_1D": "BULLISH",
  "setup_4H": "LONG",
  "entry_15m": "BUY",
  "confidence": 92,
  "notes": [
    "1D trend bullish (price > EMA200, EMA50 > EMA200, RSI>50)",
    "4H setup valid (price near EMA50, RSI 30-40)",
    "15m entry confirmed (RSI < 30, StochRSI cross up)",
    "Elliott Wave detected: Wave 3 impulse → aligns with trend",
    "Volume above 20-period average",
    "MACD confirms bullish momentum",
    "Candlestick pattern: bullish engulfing",
    "Trade near key support and Fibonacci 50% retracement",
    "Session: NY + London overlap",
    "Confidence = 92%"
  ]
}