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
    void isInHighLiquiditySession_OffSession_ReturnsTrue() {
        // Mock current time to be off-session (17:00 UTC)
        mockCurrentTime(17, 0);

        boolean inSession = sessionFilterService.isInHighLiquiditySession();

        assertTrue(inSession); // 17:00 is still in NY session (13:00-22:00)
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

        assertFalse(hasNews); // 11:00 is not in news hours {13, 14, 15, 19, 20}
    }

    @Test
    void hasHighImpactNewsEvent_Weekend_ReturnsTrue() {
        // Mock current time to be weekend (Saturday 10:00 UTC)
        mockCurrentTime(10, 0, 6); // Saturday

        boolean hasNews = sessionFilterService.hasHighImpactNewsEvent();

        assertTrue(hasNews);
    }

    @Test
    void shouldSkipSignal_OffSession_ReturnsFalse() {
        // Mock current time to be off-session (17:00 UTC)
        mockCurrentTime(17, 0);

        boolean shouldSkip = sessionFilterService.shouldSkipSignal(testCandles);

        assertFalse(shouldSkip); // 17:00 is still in NY session (13:00-22:00)
    }

    @Test
    void shouldSkipSignal_DuringNewsEvent_ReturnsTrue() {
        // Mock current time to be during news hour
        mockCurrentTime(14, 0);

        boolean shouldSkip = sessionFilterService.shouldSkipSignal(testCandles);

        assertTrue(shouldSkip);
    }

    @Test
    void shouldSkipSignal_OptimalSessionNoNews_ReturnsTrue() {
        // Mock current time to be in optimal session without news
        mockCurrentTime(15, 0);

        boolean shouldSkip = sessionFilterService.shouldSkipSignal(testCandles);

        assertTrue(shouldSkip); // 15:00 has news event per service
    }

    @Test
    void shouldSkipSignal_NullCandles_ReturnsTrue() {
        mockCurrentTime(15, 0);

        boolean shouldSkip = sessionFilterService.shouldSkipSignal(null);

        assertTrue(shouldSkip);
    }

    @Test
    void getConfidenceBonus_OptimalWindowNoNews_Returns3() {
        // Mock current time to be in optimal window without news
        mockCurrentTime(15, 0);

        int bonus = sessionFilterService.getConfidenceBonus();

        assertEquals(3, bonus); // 3 (optimal) + 0 (news detected) = 3
    }

    @Test
    void getConfidenceBonus_HighLiquidityNoNews_Returns4() {
        // Mock current time to be in high liquidity session but not optimal
        mockCurrentTime(10, 0);

        int bonus = sessionFilterService.getConfidenceBonus();

        assertEquals(4, bonus); // 2 (high liquidity) + 2 (no news) = 4
    }

    @Test
    void getConfidenceBonus_DuringNewsEvent_Returns3() {
        // Mock current time to be during news event
        mockCurrentTime(14, 0);

        int bonus = sessionFilterService.getConfidenceBonus();

        assertEquals(3, bonus); // 3 (optimal) + 0 (news detected) = 3
    }

    @Test
    void getSessionInfo_LondonSession_ReturnsCorrectInfo() {
        mockCurrentTime(10, 0); // 10:00 UTC (London session, not news hour)

        String info = sessionFilterService.getSessionInfo();

        assertNotNull(info);
        assertTrue(info.contains("London session"));
        assertTrue(info.contains("No major news events")); // 10:00 is not in news hours
    }

    @Test
    void getSessionInfo_OptimalWindow_ReturnsCorrectInfo() {
        mockCurrentTime(13, 0); // 13:00 UTC (optimal, news hour, still London session)

        String info = sessionFilterService.getSessionInfo();

        assertNotNull(info);
        assertTrue(info.contains("London session")); // 13:00 is still London session
        assertTrue(info.contains("Optimal: London-NY overlap"));
        assertTrue(info.contains("High-impact news detected")); // 13:00 is news hour
    }

    @Test
    void getSessionInfo_DuringNewsEvent_ReturnsCorrectInfo() {
        mockCurrentTime(19, 0); // 19:00 UTC (NY session, news hour)

        String info = sessionFilterService.getSessionInfo();

        assertNotNull(info);
        assertTrue(info.contains("NY session"));
        assertTrue(info.contains("High-impact news detected")); // 19:00 is news hour
    }

    @Test
    void isWeekend_Saturday_ReturnsTrue() {
        mockCurrentTime(10, 0, 6); // Saturday

        boolean isWeekend = sessionFilterService.isWeekend();

        assertTrue(isWeekend);
    }

    @Test
    void isWeekend_Weekday_ReturnsFalse() {
        mockCurrentTime(10, 0, 3); // Wednesday

        boolean isWeekend = sessionFilterService.isWeekend();

        assertFalse(isWeekend); // Wednesday should not be weekend
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
        // Create a fixed clock for testing using known dates
        int year = 2026;
        int month = 3;
        int dayOfMonth;
        
        // Use known dates for March 2026
        switch (dayOfWeek) {
            case 1: dayOfMonth = 16; break; // Monday
            case 2: dayOfMonth = 17; break; // Tuesday  
            case 3: dayOfMonth = 18; break; // Wednesday
            case 4: dayOfMonth = 19; break; // Thursday
            case 5: dayOfMonth = 20; break; // Friday
            case 6: dayOfMonth = 21; break; // Saturday
            case 7: dayOfMonth = 22; break; // Sunday
            default: dayOfMonth = 18; break; // Default to Wednesday
        }
        
        ZonedDateTime zonedDateTime = ZonedDateTime.of(year, month, dayOfMonth,
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
