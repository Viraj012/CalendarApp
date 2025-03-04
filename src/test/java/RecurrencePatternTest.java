
import model.RecurrencePattern;
import org.junit.Test;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test cases for the RecurrencePattern class.
 */
public class RecurrencePatternTest {

  @Test
  public void testParseWeekdays() {
    // Test with all weekday codes
    RecurrencePattern pattern = new RecurrencePattern("MTWRFSU", -1, null);
    Set<DayOfWeek> weekdays = pattern.getWeekdays();

    assertEquals("Should have 7 days", 7, weekdays.size());
    assertTrue("Should include Monday", weekdays.contains(DayOfWeek.MONDAY));
    assertTrue("Should include Tuesday", weekdays.contains(DayOfWeek.TUESDAY));
    assertTrue("Should include Wednesday", weekdays.contains(DayOfWeek.WEDNESDAY));
    assertTrue("Should include Thursday", weekdays.contains(DayOfWeek.THURSDAY));
    assertTrue("Should include Friday", weekdays.contains(DayOfWeek.FRIDAY));
    assertTrue("Should include Saturday", weekdays.contains(DayOfWeek.SATURDAY));
    assertTrue("Should include Sunday", weekdays.contains(DayOfWeek.SUNDAY));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidWeekdayCode() {
    // Using 'X' which is not a valid weekday code
    new RecurrencePattern("MX", -1, null);
  }

  @Test
  public void testRecurrenceWithOccurrences() {
    // Create a pattern that repeats on Mondays and Wednesdays for 3 occurrences
    LocalDateTime startDate = LocalDateTime.of(2025, 3, 3, 10, 0); // Monday
    RecurrencePattern pattern = new RecurrencePattern("MW", 3, null);

    List<LocalDateTime> occurrences = pattern.calculateRecurrences(startDate);

    assertEquals("Should have 3 occurrences", 3, occurrences.size());
    assertEquals("First occurrence should be the start date", startDate, occurrences.get(0));

    // Check that the days are correct (should be Mondays and Wednesdays)
    for (LocalDateTime occurrence : occurrences) {
      DayOfWeek day = occurrence.getDayOfWeek();
      assertTrue("Day should be Monday or Wednesday",
          day == DayOfWeek.MONDAY || day == DayOfWeek.WEDNESDAY);
    }
  }

  @Test
  public void testRecurrenceWithEndDate() {
    // Create a pattern that repeats on Tuesdays and Thursdays until a specific date
    LocalDateTime startDate = LocalDateTime.of(2025, 3, 4, 10, 0); // Tuesday
    LocalDateTime endDate = startDate.plusDays(10); // 10 days later

    RecurrencePattern pattern = new RecurrencePattern("TR", -1, endDate);

    List<LocalDateTime> occurrences = pattern.calculateRecurrences(startDate);

    // Check that no occurrences are after the end date
    for (LocalDateTime occurrence : occurrences) {
      assertFalse("Occurrence should not be after end date", occurrence.isAfter(endDate));
    }

    // Check that the days are correct (should be Tuesdays and Thursdays)
    for (LocalDateTime occurrence : occurrences) {
      DayOfWeek day = occurrence.getDayOfWeek();
      assertTrue("Day should be Tuesday or Thursday",
          day == DayOfWeek.TUESDAY || day == DayOfWeek.THURSDAY);
    }
  }

  @Test
  public void testSingleDayRecurrence() {
    // Create a pattern that repeats only on Fridays
    LocalDateTime startDate = LocalDateTime.of(2025, 3, 7, 10, 0); // Friday
    RecurrencePattern pattern = new RecurrencePattern("F", 4, null);

    List<LocalDateTime> occurrences = pattern.calculateRecurrences(startDate);

    assertEquals("Should have 4 occurrences", 4, occurrences.size());

    // Check that all days are Fridays
    for (LocalDateTime occurrence : occurrences) {
      assertEquals("Day should be Friday", DayOfWeek.FRIDAY, occurrence.getDayOfWeek());
    }

    // Check spacing between occurrences
    assertEquals("Should be 7 days between occurrences",
        7, occurrences.get(1).getDayOfMonth() - occurrences.get(0).getDayOfMonth());
  }

  @Test
  public void testWeekendRecurrence() {
    // Create a pattern that repeats on weekends
    LocalDateTime startDate = LocalDateTime.of(2025, 3, 1, 10, 0); // Saturday
    RecurrencePattern pattern = new RecurrencePattern("SU", 4, null);

    List<LocalDateTime> occurrences = pattern.calculateRecurrences(startDate);

    assertEquals("Should have 4 occurrences", 4, occurrences.size());

    // Check that all days are weekends
    for (LocalDateTime occurrence : occurrences) {
      DayOfWeek day = occurrence.getDayOfWeek();
      assertTrue("Day should be Saturday or Sunday",
          day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
    }
  }

  @Test
  public void testStartDateNotInPattern() {
    // Start date is Wednesday, but pattern is for Mondays and Fridays
    LocalDateTime startDate = LocalDateTime.of(2025, 3, 5, 10, 0); // Wednesday
    RecurrencePattern pattern = new RecurrencePattern("MF", 3, null);

    List<LocalDateTime> occurrences = pattern.calculateRecurrences(startDate);

    // First occurrence should be on the first Monday or Friday after the start date
    LocalDateTime firstOccurrence = occurrences.get(0);
    DayOfWeek day = firstOccurrence.getDayOfWeek();
    assertTrue("First occurrence should be Monday or Friday",
        day == DayOfWeek.MONDAY || day == DayOfWeek.FRIDAY);
    assertFalse("First occurrence shouldn't be on start date", firstOccurrence.equals(startDate));
  }

  @Test
  public void testGettersAndConstructors() {
    // Test occurrences constructor
    RecurrencePattern pattern1 = new RecurrencePattern("MWF", 5, null);
    assertEquals("Occurrences should match", 5, pattern1.getOccurrences());
    assertNull("Until date should be null", pattern1.getUntilDate());

    // Test until date constructor
    LocalDateTime untilDate = LocalDateTime.of(2025, 4, 1, 0, 0);
    RecurrencePattern pattern2 = new RecurrencePattern("TR", -1, untilDate);
    assertEquals("Occurrences should be -1", -1, pattern2.getOccurrences());
    assertEquals("Until date should match", untilDate, pattern2.getUntilDate());
  }
}