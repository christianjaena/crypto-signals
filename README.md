# Crypto Signals Generator

A sophisticated Spring Boot application that generates cryptocurrency trading signals using multi-timeframe technical analysis, Elliott Wave theory, and advanced confidence scoring.

## 🚀 Features

### Core Signal Generation
- **Multi-Timeframe Analysis**: 1D (HTF), 4H (MTF), 15m (LTF) candle data analysis
- **Technical Indicators**: EMA50/200, RSI(14), StochRSI, MACD, Bollinger Bands
- **Trend Detection**: Bullish, Bearish, or Sideways trend identification
- **Setup Zones**: Price near EMA50 with RSI validation
- **Entry Confirmation**: RSI and StochRSI crossover signals

### Advanced Analysis
- **Elliott Wave Detection**: Impulse waves (1-5) and corrective waves (ABC)
- **Candlestick Patterns**: Bullish/Bearish engulfing, Hammer, Shooting Star
- **Support & Resistance**: Fibonacci retracement levels (38.2%, 50%, 61.8%)
- **Volume Analysis**: 20-period moving average validation
- **Divergence Detection**: RSI/MACD divergence identification

### Confidence Scoring
- **Base Score**: 50%
- **Dynamic Boosters**: Trend alignment, setup quality, momentum confirmation
- **Advanced Factors**: Elliott Wave alignment, pattern confirmation, volume
- **Final Range**: 0-100% with intelligent capping

### Risk Management
- **Session Filtering**: High-liquidity trading sessions only
- **News Event Filtering**: Skip trades during high-impact news
- **Signal Spacing**: Prevent signal spam with minimum time intervals
- **Volatility Filtering**: Avoid low-volatility conditions

## 📋 Prerequisites

- Java 25
- Maven 3.6 or higher
- MEXC API account with API keys

## 🛠️ Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/crypto-signals.git
   cd crypto-signals
   ```

2. **Configure API Keys**
   
   Copy the environment template:
   ```bash
   cp .env.example .env
   ```
   
   Edit `.env` with your MEXC API credentials:
   ```env
   MEXC_ACCESS_KEY=your_mexc_access_key_here
   MEXC_SECRET_KEY=your_mexc_secret_key_here
   ```
   
   For detailed setup instructions, see [SETUP.md](SETUP.md).

3. **Build and Run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

## 📊 API Endpoints

### Generate Signals
```http
GET /api/signals/generate?symbols=BTC,ETH,SOL
```

**Parameters:**
- **symbols** (required): Comma-separated list of symbols without USDT suffix (e.g., `BTC,ETH,SOL`)
- USDT is automatically appended to each symbol internally

**Response:**
Returns an array of signals, one for each requested symbol:

```json
[
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
    ],
    "timestamp": "2026-03-20T11:23:00Z"
  },
  {
    "symbol": "ETHUSDT",
    "currentPrice": 3200.75,
    "stopLoss": 3100.00,
    "predictionPriceGrowth": 3500.00,
    "trend_1D": "BEARISH",
    "setup_4H": "SHORT",
    "entry_15m": "SELL",
    "confidence": 78,
    "notes": [
      "1D trend bearish",
      "4H setup short",
      "15m entry sell confirmed",
      "Confidence = 78%"
    ],
    "timestamp": "2026-03-20T11:23:00Z"
  }
]
```

### Get Historical Signals
```http
GET /api/signals/history?symbol=BTC/USDT&limit=10
```

### Health Check
```http
GET /api/health
```

## 🔧 Configuration

### Application Properties
Key configuration options in `application.properties`:

```properties
# Server
server.port=8080

# MEXC API
mexc.base-url=https://api.mexc.com
mexc.timeout=10000

# Logging
logging.level.com.christianjaena.crypto_signals=DEBUG
```

### Signal Parameters
Customize signal generation behavior:

- **RSI Thresholds**: Entry RSI levels (30 for BUY, 70 for SELL)
- **EMA Tolerance**: Price distance from EMA50 (default: ±2%)
- **Volume Filter**: Minimum volume multiplier (default: 1.0x average)
- **Session Windows**: Trading session time windows
- **Confidence Weights**: Individual factor contributions to total confidence

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn test -Dtest=*IntegrationTest
```

### Test Coverage
```bash
mvn jacoco:report
```

## 📈 Algorithm Overview

### Signal Generation Pipeline

1. **Trend Analysis (1D)**
   - Evaluate EMA relationships
   - Check RSI positioning
   - Determine overall market direction

2. **Setup Identification (4H)**
   - Find price near EMA50 zones
   - Validate RSI ranges (30-40 for LONG, 60-70 for SHORT)
   - Confirm trend alignment

3. **Entry Confirmation (15m)**
   - RSI oversold/overbought conditions
   - StochRSI crossover signals
   - Volume confirmation

4. **Advanced Validation**
   - Elliott Wave pattern detection
   - Candlestick pattern confirmation
   - Support/Resistance level validation
   - Divergence analysis

5. **Risk Filtering**
   - Session time windows
   - News event avoidance
   - Volatility requirements
   - Signal spacing rules

6. **Confidence Calculation**
   - Base confidence: 50%
   - Add bonuses for each validated factor
   - Cap final score at 100%

## 🏗️ Architecture

### Service Layer
- **TrendAnalysisService**: Multi-timeframe trend detection
- **SetupZoneService**: Setup zone identification and validation
- **ConfidenceScoringService**: Dynamic confidence calculation
- **TechnicalIndicatorService**: Technical indicator calculations
- **MexcApiService**: MEXC API integration

### Model Layer
- **Signal**: Trading signal with confidence score
- **Trend**: Market trend enumeration (BULLISH/BEARISH/SIDEWAYS)
- **Setup**: Setup zone enumeration (LONG/SHORT/NONE)
- **TechnicalIndicators**: Container for all calculated indicators

### Configuration
- **MexcConfig**: MEXC API configuration with environment variable support
- **OpenApiConfig**: Swagger/OpenAPI documentation setup

## 🔒 Security

- API keys stored in environment variables
- No sensitive data in version control
- HTTPS-only API communication
- Input validation and sanitization

## 📝 Logging

Debug-level logging for:
- API requests/responses
- Technical indicator calculations
- Signal generation decisions
- Confidence scoring breakdown

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- MEXC Exchange for API access
- Spring Boot framework
- Technical analysis community
- Elliott Wave practitioners

## 📞 Support

For questions or support:
- Create an issue in the GitHub repository
- Check the [SETUP.md](SETUP.md) for configuration help
- Review the API documentation at `/swagger-ui.html`

---

**⚠️ Disclaimer**: This software is for educational and research purposes only. Cryptocurrency trading involves substantial risk of loss. Always do your own research and consult with a financial advisor before making any investment decisions.