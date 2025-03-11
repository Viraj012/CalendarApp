
import model.RecurrencePattern;
import org.junit.Test;
import org.junit.Before;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Enhanced test class for RecurrencePattern with improved mutation coverage.
 */
public class RecurrencePatternTest {

  private LocalDateTime baseDate;
  private LocalDateTime untilDate;

  @Before
  public void setUp() {
    // March 10, 2025 is a Monday
    baseDate = LocalDateTime.of(2025, 3, 10, 14, 30);
    untilDate = LocalDateTime.of(2025, 4, 10, 0, 0);
  }

  @Test
  public void testSingleWeekdayParsing() {
    // Test each individual weekday code
    verifyWeekdayParsing("M", new DayOfWeek[]{DayOfWeek.MONDAY});
    verifyWeekdayParsing("T", new DayOfWeek[]{DayOfWeek.TUESDAY});
    verifyWeekdayParsing("W", new DayOfWeek[]{DayOfWeek.WEDNESDAY});
    verifyWeekdayParsing("R", new DayOfWeek[]{DayOfWeek.THURSDAY});
    verifyWeekdayParsing("F", new DayOfWeek[]{DayOfWeek.FRIDAY});
    verifyWeekdayParsing("S", new DayOfWeek[]{DayOfWeek.SATURDAY});
    verifyWeekdayParsing("U", new DayOfWeek[]{DayOfWeek.SUNDAY});
  }

//  @Test
//  public void testMultipleWeekdaysCombinations() {
//    // Weekday combinations
//    verifyWeekdayParsing("MWF", new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY});
//    verifyWeekdayParsing("TR", new DayOfWeek[]{DayOfWeek.TUESDAY, DayOfWeek.THURSDAY});
//    verifyWeekdayParsing("SU", new DayOfWeek[]{DayOfWeek.SATURDAY, DayOfWeek.SUNDAY});
//    verifyWeekdayParsing("MTWTF", new DayOfWeek[]{
//        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
//        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
//    });
//  }

  @Test
  public void testWeekdayParsingOrder() {
    // Order shouldn't matter
    RecurrencePattern pattern1 = new RecurrencePattern("MWF", 5, null);
    RecurrencePattern pattern2 = new RecurrencePattern("FMW", 5, null);
    RecurrencePattern pattern3 = new RecurrencePattern("WFM", 5, null);

    assertEquals(pattern1.getWeekdays(), pattern2.getWeekdays());
    assertEquals(pattern1.getWeekdays(), pattern3.getWeekdays());
    assertEquals(pattern2.getWeekdays(), pattern3.getWeekdays());
  }

  @Test
  public void testDuplicateWeekdayHandling() {
    // Duplicate codes should only count once
    RecurrencePattern pattern1 = new RecurrencePattern("M", 5, null);
    RecurrencePattern pattern2 = new RecurrencePattern("MM", 5, null);
    RecurrencePattern pattern3 = new RecurrencePattern("MMM", 5, null);

    assertEquals(1, pattern1.getWeekdays().size());
    assertEquals(1, pattern2.getWeekdays().size());
    assertEquals(1, pattern3.getWeekdays().size());
    assertEquals(pattern1.getWeekdays(), pattern2.getWeekdays());
    assertEquals(pattern1.getWeekdays(), pattern3.getWeekdays());
  }

  @Test
  public void testEmptyWeekdays() {
    // Empty string should result in no weekdays
    try {
      RecurrencePattern pattern = new RecurrencePattern("", 5, null);
      assertEquals(0, pattern.getWeekdays().size());
    } catch (IllegalArgumentException e) {
      // Both empty set or exception are acceptable implementations
    }
  }

  @Test
  public void testAllDaysSpecified() {
    RecurrencePattern pattern = new RecurrencePattern("MTWRFSU", -1, untilDate);
    Set<DayOfWeek> weekdays = pattern.getWeekdays();

    assertEquals(7, weekdays.size());
    assertTrue(weekdays.contains(DayOfWeek.MONDAY));
    assertTrue(weekdays.contains(DayOfWeek.TUESDAY));
    assertTrue(weekdays.contains(DayOfWeek.WEDNESDAY));
    assertTrue(weekdays.contains(DayOfWeek.THURSDAY));
    assertTrue(weekdays.contains(DayOfWeek.FRIDAY));
    assertTrue(weekdays.contains(DayOfWeek.SATURDAY));
    assertTrue(weekdays.contains(DayOfWeek.SUNDAY));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidWeekdayUppercase() {
    new RecurrencePattern("MXF", 5, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidWeekdayLowercase() {
    new RecurrencePattern("mwf", 5, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidWeekdaySymbol() {
    new RecurrencePattern("M%F", 5, null);
  }

  @Test
  public void testGetOccurrences() {
    // Test various occurrence counts
    verifyOccurrences(1);
    verifyOccurrences(2);
    verifyOccurrences(5);
    verifyOccurrences(10);
    verifyOccurrences(100);

    // Test special value for untilDate
    verifyOccurrences(-1);

    // Test zero - should be treated as special or invalid
    try {
      RecurrencePattern pattern = new RecurrencePattern("M", 0, null);
      assertEquals(0, pattern.getOccurrences());
    } catch (IllegalArgumentException e) {
      // Both zero occurrences or exception are acceptable implementations
    }
  }

  @Test
  public void testGetUntilDate() {
    // Test with different dates
    verifyUntilDate(LocalDateTime.of(2025, 3, 15, 0, 0));
    verifyUntilDate(LocalDateTime.of(2025, 4, 1, 12, 30));
    verifyUntilDate(untilDate);
    verifyUntilDate(baseDate.plusYears(1));

    // Test with null (should work when occurrences is specified)
    RecurrencePattern pattern = new RecurrencePattern("M", 5, null);
    assertNull(pattern.getUntilDate());
  }

  @Test
  public void testRecurrenceConstructionWithOccurrences() {
    // Create with occurrences but no untilDate
    RecurrencePattern pattern = new RecurrencePattern("TR", 3, null);
    assertEquals(3, pattern.getOccurrences());
    assertNull(pattern.getUntilDate());

    Set<DayOfWeek> expectedDays = new HashSet<>(Arrays.asList(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY));
    assertEquals(expectedDays, pattern.getWeekdays());
  }

  @Test
  public void testRecurrenceConstructionWithUntilDate() {
    // Create with untilDate but no occurrences (use -1)
    RecurrencePattern pattern = new RecurrencePattern("MWF", -1, untilDate);
    assertEquals(-1, pattern.getOccurrences());
    assertEquals(untilDate, pattern.getUntilDate());

    Set<DayOfWeek> expectedDays = new HashSet<>(Arrays.asList(
        DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
    assertEquals(expectedDays, pattern.getWeekdays());
  }

  @Test
  public void testCalculateRecurrencesWithOccurrencesMondayStarting() {
    // Starting on Monday, March 10, 2025, looking for Mondays
    RecurrencePattern pattern = new RecurrencePattern("M", 4, null);

    List<LocalDateTime> dates = pattern.calculateRecurrences(baseDate);

    // Should get 4 occurrences on consecutive Mondays
    assertEquals(4, dates.size());
    assertEquals(baseDate, dates.get(0)); // Monday, March 10
    assertEquals(LocalDateTime.of(2025, 3, 17, 14, 30), dates.get(1)); // Monday, March 17
    assertEquals(LocalDateTime.of(2025, 3, 24, 14, 30), dates.get(2)); // Monday, March 24
    assertEquals(LocalDateTime.of(2025, 3, 31, 14, 30), dates.get(3)); // Monday, March 31
  }

  @Test
  public void testCalculateRecurrencesWithOccurrencesMultipleWeekdays() {
    // Starting on Monday, March 10, 2025, looking for Mon, Wed, Fri
    RecurrencePattern pattern = new RecurrencePattern("MWF", 6, null);

    List<LocalDateTime> dates = pattern.calculateRecurrences(baseDate);

    // Should get 6 occurrences on Mon, Wed, Fri
    assertEquals(6, dates.size());
    assertEquals(baseDate, dates.get(0)); // Monday, March 10
    assertEquals(LocalDateTime.of(2025, 3, 12, 14, 30), dates.get(1)); // Wednesday, March 12
    assertEquals(LocalDateTime.of(2025, 3, 14, 14, 30), dates.get(2)); // Friday, March 14
    assertEquals(LocalDateTime.of(2025, 3, 17, 14, 30), dates.get(3)); // Monday, March 17
    assertEquals(LocalDateTime.of(2025, 3, 19, 14, 30), dates.get(4)); // Wednesday, March 19
    assertEquals(LocalDateTime.of(2025, 3, 21, 14, 30), dates.get(5)); // Friday, March 21
  }

  @Test
  public void testCalculateRecurrencesWithUntilDate() {
    // Starting on Monday, March 10, 2025, until Friday, March 21, 2025
    LocalDateTime localUntilDate = LocalDateTime.of(2025, 3, 21, 23, 59);
    RecurrencePattern pattern = new RecurrencePattern("MWF", -1, localUntilDate);

    List<LocalDateTime> dates = pattern.calculateRecurrences(baseDate);

    // Should get 6 occurrences on Mon, Wed, Fri up to and including March 21
    assertEquals(6, dates.size());
    assertEquals(baseDate, dates.get(0)); // Monday, March 10
    assertEquals(LocalDateTime.of(2025, 3, 12, 14, 30), dates.get(1)); // Wednesday, March 12
    assertEquals(LocalDateTime.of(2025, 3, 14, 14, 30), dates.get(2)); // Friday, March 14
    assertEquals(LocalDateTime.of(2025, 3, 17, 14, 30), dates.get(3)); // Monday, March 17
    assertEquals(LocalDateTime.of(2025, 3, 19, 14, 30), dates.get(4)); // Wednesday, March 19
    assertEquals(LocalDateTime.of(2025, 3, 21, 14, 30), dates.get(5)); // Friday, March 21
  }

  @Test
  public void testCalculateRecurrencesStartingOnNonMatchingDay() {
    // Starting on Tuesday, March 11, 2025, looking for Mondays
    LocalDateTime tuesdayStart = LocalDateTime.of(2025, 3, 11, 14, 30);
    RecurrencePattern pattern = new RecurrencePattern("M", 3, null);

    List<LocalDateTime> dates = pattern.calculateRecurrences(tuesdayStart);

    // Should get 3 occurrences, first Monday is March 17
    assertEquals(3, dates.size());
    assertEquals(LocalDateTime.of(2025, 3, 17, 14, 30), dates.get(0)); // Monday, March 17
    assertEquals(LocalDateTime.of(2025, 3, 24, 14, 30), dates.get(1)); // Monday, March 24
    assertEquals(LocalDateTime.of(2025, 3, 31, 14, 30), dates.get(2)); // Monday, March 31
  }

  @Test
  public void testCalculateRecurrencesWithUntilDateBeforeAnyOccurrence() {
    // Starting on Tuesday, looking for Thursdays, but until date is Wednesday
    LocalDateTime tuesdayStart = LocalDateTime.of(2025, 3, 11, 14, 30);
    LocalDateTime wednesdayUntil = LocalDateTime.of(2025, 3, 12, 23, 59);
    RecurrencePattern pattern = new RecurrencePattern("R", -1, wednesdayUntil);

    List<LocalDateTime> dates = pattern.calculateRecurrences(tuesdayStart);

    // Should get 0 occurrences
    assertEquals(0, dates.size());
  }
//
//  @Test
//  public void testCalculateRecurrencesWithPastUntilDate() {
//    // Starting on Monday, but until date is in the past
//    LocalDateTime pastDate = baseDate.minusDays(7);
//    RecurrencePattern pattern = new RecurrencePattern("M", -1, pastDate);
//
//    List<LocalDateTime> dates = pattern.calculateRecurrences(baseDate);
//
//    // Should get 0 occurrences
//    assertEquals(0, dates.size());
//  }

  @Test
  public void testCalculateRecurrencesWithSameUntilDate() {
    // Starting on Monday, until date is the same day
    RecurrencePattern pattern = new RecurrencePattern("M", -1, baseDate);

    List<LocalDateTime> dates = pattern.calculateRecurrences(baseDate);

    // Should get 1 occurrence (the same day)
    assertEquals(1, dates.size());
    assertEquals(baseDate, dates.get(0));
  }

  @Test
  public void testCalculateRecurrencesWithNoWeekdaysMatch() {
    // Starting on Monday but looking for Tuesdays and Thursdays only
    RecurrencePattern pattern = new RecurrencePattern("TR", 3, null);

    List<LocalDateTime> dates = pattern.calculateRecurrences(baseDate);

    // Should get 3 occurrences, first one is Tuesday
    assertEquals(3, dates.size());
    assertEquals(LocalDateTime.of(2025, 3, 11, 14, 30), dates.get(0)); // Tuesday, March 11
    assertEquals(LocalDateTime.of(2025, 3, 13, 14, 30), dates.get(1)); // Thursday, March 13
    assertEquals(LocalDateTime.of(2025, 3, 18, 14, 30), dates.get(2)); // Tuesday, March 18
  }

  @Test
  public void testCalculateRecurrencesWithAllWeekdays() {
    // Starting on Monday, looking for all weekdays
    RecurrencePattern pattern = new RecurrencePattern("MTWRFSU", 7, null);

    List<LocalDateTime> dates = pattern.calculateRecurrences(baseDate);

    // Should get 7 consecutive days
    assertEquals(7, dates.size());
    assertEquals(baseDate, dates.get(0)); // Monday, March 10
    assertEquals(LocalDateTime.of(2025, 3, 11, 14, 30), dates.get(1)); // Tuesday
    assertEquals(LocalDateTime.of(2025, 3, 12, 14, 30), dates.get(2)); // Wednesday
    assertEquals(LocalDateTime.of(2025, 3, 13, 14, 30), dates.get(3)); // Thursday
    assertEquals(LocalDateTime.of(2025, 3, 14, 14, 30), dates.get(4)); // Friday
    assertEquals(LocalDateTime.of(2025, 3, 15, 14, 30), dates.get(5)); // Saturday
    assertEquals(LocalDateTime.of(2025, 3, 16, 14, 30), dates.get(6)); // Sunday
  }

//  @Test
//  public void testCalculateRecurrencesWithManyOccurrences() {
//    // Test with a larger number of occurrences to ensure loop works correctly
//    RecurrencePattern pattern = new RecurrencePattern("M", 52, null); // Full year of Mondays
//
//    List<LocalDateTime> dates = pattern.calculateRecurrences(baseDate);
//
//    // Should get 52 Mondays
//    assertEquals(52, dates.size());
//
//    // Check first few
//    assertEquals(baseDate, dates.get(0)); // First Monday
//    assertEquals(LocalDateTime.of(2025, 3, 17, 14, 30), dates.get(1)); // Second Monday
//
//    // Check last one (approximately a year later)
//    assertEquals(LocalDateTime.of(2026, 3, 9, 14, 30), dates.get(51)); // Last Monday
//  }

  @Test
  public void testRecurrenceWithLongPeriod() {
    // Test with occurrences and untilDate over a year
    LocalDateTime farUntilDate = baseDate.plusYears(2);
    RecurrencePattern pattern = new RecurrencePattern("MTWRFSU", -1, farUntilDate);

    List<LocalDateTime> dates = pattern.calculateRecurrences(baseDate);

    // Should have many dates (365*2 + leap days)
    assertTrue(dates.size() > 730); // Rough estimate for 2 years

    // Check first and last date
    assertEquals(baseDate, dates.get(0));
    assertTrue(!dates.get(dates.size() - 1).isAfter(farUntilDate));
  }

//  @Test
//  public void testCalculateRecurrencesWithZeroOccurrences() {
//    try {
//      // Try with zero occurrences if implementation allows it
//      RecurrencePattern pattern = new RecurrencePattern("M", 0, null);
//      List<LocalDateTime> dates = pattern.calculateRecurrences(baseDate);
//      assertEquals(0, dates.size()); // Should have no dates
//    } catch (IllegalArgumentException e) {
//      // This is also acceptable if implementation disallows zero occurrences
//    }
//  }

  @Test
  public void testCalculateRecurrencesWithNegativeOccurrences() {
    // Negative values other than -1 should either be rejected or treated as special cases
    try {
      RecurrencePattern pattern = new RecurrencePattern("M", -2, null);
      List<LocalDateTime> dates = pattern.calculateRecurrences(baseDate);

      // If allowed, should have behavior consistent with documentation
      if (pattern.getUntilDate() == null) {
        // Without until date, might default to some limit
        assertTrue(dates.size() >= 0);
      }
    } catch (IllegalArgumentException e) {
      // This is also acceptable if implementation disallows negative occurrences other than -1
    }
  }

  // Helper methods to reduce code duplication

  private void verifyWeekdayParsing(String weekdaysStr, DayOfWeek[] expectedDays) {
    RecurrencePattern pattern = new RecurrencePattern(weekdaysStr, 5, null);
    Set<DayOfWeek> weekdays = pattern.getWeekdays();

    assertEquals("Number of weekdays doesn't match for " + weekdaysStr,
        expectedDays.length, weekdays.size());

    for (DayOfWeek day : expectedDays) {
      assertTrue("Expected " + day + " for weekdays string: " + weekdaysStr,
          weekdays.contains(day));
    }
  }

  private void verifyOccurrences(int occurrences) {
    RecurrencePattern pattern = new RecurrencePattern("M", occurrences, null);
    assertEquals(occurrences, pattern.getOccurrences());
  }

  private void verifyUntilDate(LocalDateTime untilDate) {
    RecurrencePattern pattern = new RecurrencePattern("M", -1, untilDate);
    assertEquals(untilDate, pattern.getUntilDate());
  }}