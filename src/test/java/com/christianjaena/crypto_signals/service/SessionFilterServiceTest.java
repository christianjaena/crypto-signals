package com.christianjaena.crypto_signals.service;

import com.christianjaena.crypto_signals.model.CandleData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SessionFilterServiceTest {

    private SessionFilterService sessionFilterService;
    private List<CandleData> testCandles;

    @BeforeEach
    void setUp() {
        sessionFilterService = new SessionFilterService();
        testCandles = createTestCandles();
    }

    @Test
    void isInHighLiquiditySession_LondonSession_ReturnsTrue() {
        // Mock current time to be in London session (10:00 UTC)
        mockCurrentTime(10, 0);

        boolean inSession = sessionFilterService.isInHighLiquiditySession();

        assertTrue(inSession);
    }

    @Test
    void isInHighLiquiditySession_NYSession_ReturnsTrue() {
        // Mock current time to be in NY session (15:00 UTC)
        mockCurrentTime(15, 0);

        boolean inSession = sessionFilterService.isInHighLiquiditySession();

        assertTrue(inSession);
    }

    @Test
    void isInHighLiquiditySession_AsiaSession_ReturnsTrue() {
        // Mock current time to be in Asia session (23:00 UTC)
        mockCurrentTime(23, 0);

        boolean inSession = sessionFilterService.isInHighLiquiditySession();

        assertTrue(inSession);
    }

    @Test
    void isInHighLiquiditySession_OffSession_ReturnsFalse() {
        // Mock current time to be off-session (17:00 UTC)
        mockCurrentTime(17, 0);

        boolean inSession = sessionFilterService.isInHighLiquiditySession();

        assertFalse(inSession);
    }

    @Test
    void isInOptimalTradingWindow_LondonNYOverlap_ReturnsTrue() {
        // Mock current time to be in optimal window (14:00 UTC)
        mockCurrentTime(14, 0);

        boolean optimal = sessionFilterService.isInOptimalTradingWindow();

        assertTrue(optimal);
    }

    @Test
    void isInOptimalTradingWindow_NonOptimalTime_ReturnsFalse() {
        // Mock current time to be outside optimal window (10:00 UTC)
        mockCurrentTime(10, 0);

        boolean optimal = sessionFilterService.isInOptimalTradingWindow();

        assertFalse(optimal);
    }

    @Test
    void hasHighImpactNewsEvent_NewsHour_ReturnsTrue() {
        // Mock current time to be during news hour (14:00 UTC)
        mockCurrentTime(14, 0);

        boolean hasNews = sessionFilterService.hasHighImpactNewsEvent();

        assertTrue(hasNews);
    }

    @Test
    void hasHighImpactNewsEvent_NonNewsHour_ReturnsFalse() {
        // Mock current time to be outside news hours (11:00 UTC)
        mockCurrentTime(11, 0);

        boolean hasNews = sessionFilterService.hasHighImpactNewsEvent();

        assertFalse(hasNews);
    }

    @Test
    void hasHighImpactNewsEvent_Weekend_ReturnsTrue() {
        // Mock current time to be weekend (Saturday 10:00 UTC)
        mockCurrentTime(10, 0, 6); // Saturday

        boolean hasNews = sessionFilterService.hasHighImpactNewsEvent();

        assertTrue(hasNews);
    }

    @Test
    void shouldSkipSignal_OffSession_ReturnsTrue() {
        // Mock current time to be off-session
        mockCurrentTime(17, 0);

        boolean shouldSkip = sessionFilterService.shouldSkipSignal(testCandles);

        assertTrue(shouldSkip);
    }

    @Test
    void shouldSkipSignal_DuringNewsEvent_ReturnsTrue() {
        // Mock current time to be during news hour
        mockCurrentTime(14, 0);

        boolean shouldSkip = sessionFilterService.shouldSkipSignal(testCandles);

        assertTrue(shouldSkip);
    }

    @Test
    void shouldSkipSignal_OptimalSessionNoNews_ReturnsFalse() {
        // Mock current time to be in optimal session without news
        mockCurrentTime(15, 0);

        boolean shouldSkip = sessionFilterService.shouldSkipSignal(testCandles);

        assertFalse(shouldSkip);
    }

    @Test
    void shouldSkipSignal_NullCandles_ReturnsTrue() {
        mockCurrentTime(15, 0);

        boolean shouldSkip = sessionFilterService.shouldSkipSignal(null);

        assertTrue(shouldSkip);
    }

    @Test
    void getConfidenceBonus_OptimalWindowNoNews_Returns5() {
        // Mock current time to be in optimal window without news
        mockCurrentTime(15, 0);

        int bonus = sessionFilterService.getConfidenceBonus();

        assertEquals(5, bonus); // 3 (optimal) + 2 (no news)
    }

    @Test
    void getConfidenceBonus_HighLiquidityNoNews_Returns2() {
        // Mock current time to be in high liquidity session but not optimal
        mockCurrentTime(10, 0);

        int bonus = sessionFilterService.getConfidenceBonus();

        assertEquals(2, bonus); // 0 (not optimal) + 2 (no news)
    }

    @Test
    void getConfidenceBonus_DuringNewsEvent_Returns0() {
        // Mock current time to be during news event
        mockCurrentTime(14, 0);

        int bonus = sessionFilterService.getConfidenceBonus();

        assertEquals(0, bonus); // No bonus during news
    }

    @Test
    void getSessionInfo_LondonSession_ReturnsCorrectInfo() {
        mockCurrentTime(10, 0);

        String info = sessionFilterService.getSessionInfo();

        assertNotNull(info);
        assertTrue(info.contains("London session"));
        assertTrue(info.contains("No major news events"));
    }

    @Test
    void getSessionInfo_OptimalWindow_ReturnsCorrectInfo() {
        mockCurrentTime(15, 0);

        String info = sessionFilterService.getSessionInfo();

        assertNotNull(info);
        assertTrue(info.contains("NY session"));
        assertTrue(info.contains("Optimal: London-NY overlap"));
    }

    @Test
    void getSessionInfo_DuringNewsEvent_ReturnsCorrectInfo() {
        mockCurrentTime(14, 0);

        String info = sessionFilterService.getSessionInfo();

        assertNotNull(info);
        assertTrue(info.contains("NY session"));
        assertTrue(info.contains("High-impact news detected"));
    }

    @Test
    void isWeekend_Saturday_ReturnsTrue() {
        mockCurrentTime(10, 0, 6); // Saturday

        boolean isWeekend = sessionFilterService.isWeekend();

        assertTrue(isWeekend);
    }

    @Test
    void isWeekend_Weekday_ReturnsFalse() {
        mockCurrentTime(10, 0, 2); // Tuesday

        boolean isWeekend = sessionFilterService.isWeekend();

        assertFalse(isWeekend);
    }

    @Test
    void isOptimalForPair_BTC_USDTInLondonSession_ReturnsTrue() {
        mockCurrentTime(10, 0);

        boolean optimal = sessionFilterService.isOptimalForPair("BTC/USDT");

        assertTrue(optimal);
    }

    @Test
    void isOptimalForPair_BTC_USDTInAsiaSession_ReturnsFalse() {
        mockCurrentTime(2, 0);

        boolean optimal = sessionFilterService.isOptimalForPair("BTC/USDT");

        assertFalse(optimal);
    }

    @Test
    void isOptimalForPair_JPYPairInAsiaSession_ReturnsTrue() {
        mockCurrentTime(2, 0);

        boolean optimal = sessionFilterService.isOptimalForPair("USD/JPY");

        assertTrue(optimal);
    }

    @Test
    void isOptimalForPair_NullSymbol_ReturnsGeneralSession() {
        mockCurrentTime(10, 0);

        boolean optimal = sessionFilterService.isOptimalForPair(null);

        assertTrue(optimal);
    }

    @Test
    void getNextOptimalSession_BeforeOptimalWindow_ReturnsCorrectMessage() {
        mockCurrentTime(11, 0);

        String nextSession = sessionFilterService.getNextOptimalSession();

        assertNotNull(nextSession);
        assertTrue(nextSession.contains("13:00-16:00 UTC"));
    }

    @Test
    void getNextOptimalSession_DuringOptimalWindow_ReturnsCurrentMessage() {
        mockCurrentTime(15, 0);

        String nextSession = sessionFilterService.getNextOptimalSession();

        assertNotNull(nextSession);
        assertTrue(nextSession.contains("Currently in optimal session"));
    }

    @Test
    void getNextOptimalSession_AfterOptimalWindow_ReturnsTomorrowMessage() {
        mockCurrentTime(18, 0);

        String nextSession = sessionFilterService.getNextOptimalSession();

        assertNotNull(nextSession);
        assertTrue(nextSession.contains("Tomorrow 13:00-16:00 UTC"));
    }

    private void mockCurrentTime(int hour, int minute) {
        mockCurrentTime(hour, minute, 2); // Default to Tuesday
    }

    private void mockCurrentTime(int hour, int minute, int dayOfWeek) {
        // Create a fixed clock for testing
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2026, 3, 
            dayOfWeek == 6 ? 21 : dayOfWeek == 7 ? 22 : 20 + (dayOfWeek - 1), // Adjust for weekend
            hour, minute, 0, 0, ZoneId.of("UTC"));
        
        Instant fixedInstant = zonedDateTime.toInstant();
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        
        // Create new service instance with mocked clock
        sessionFilterService = new SessionFilterService(fixedClock);
    }

    private List<CandleData> createTestCandles() {
        return Arrays.asList(
            createCandle(100000.0),
            createCandle(101000.0),
            createCandle(102000.0),
            createCandle(103000.0),
            createCandle(104000.0)
        );
    }

    private CandleData createCandle(double closePrice) {
        CandleData candle = new CandleData();
        candle.setSymbol("BTC/USDT");
        candle.setTimestamp(LocalDateTime.now());
        candle.setOpen(closePrice * 0.99);
        candle.setHigh(closePrice * 1.01);
        candle.setLow(closePrice * 0.98);
        candle.setClose(closePrice);
        candle.setVolume(1000000.0);
        candle.setTimeframe("4H");
        return candle;
    }
}
