import controller.DateTimeUtil;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.junit.Assert.*;

/**
 * Test cases for the DateTimeUtil class.
 */
public class DateTimeUtilTest {

  @Test
  public void testParseDateTimeWithTime() {
    String dateTimeStr = "2025-03-04T10:30";
    LocalDateTime expected = LocalDateTime.of(2025, 3, 4, 10, 30);

    LocalDateTime actual = DateTimeUtil.parseDateTime(dateTimeStr);
    assertEquals("Should parse date time correctly", expected, actual);
  }

  @Test
  public void testParseDateOnly() {
    String dateStr = "2025-03-04";
    LocalDateTime expected = LocalDateTime.of(
        LocalDate.of(2025, 3, 4),
        LocalTime.MIDNIGHT
    );

    LocalDateTime actual = DateTimeUtil.parseDateTime(dateStr);
    assertEquals("Should parse date only correctly", expected, actual);
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseInvalidDateFormat() {
    String invalidDateStr = "03/04/2025";
    DateTimeUtil.parseDateTime(invalidDateStr);
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseInvalidTimeFormat() {
    String invalidTimeStr = "2025-03-04T10.30";
    DateTimeUtil.parseDateTime(invalidTimeStr);
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseDateTimeWithInvalidSeparator() {
    String invalidStr = "2025-03-04/10:30";
    DateTimeUtil.parseDateTime(invalidStr);
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseEmptyString() {
    DateTimeUtil.parseDateTime("");
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseIncompleteDate() {
    DateTimeUtil.parseDateTime("2025-03");
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseIncompleteTime() {
    DateTimeUtil.parseDateTime("2025-03-04T10");
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseOutOfRangeDate() {
    // February 30 doesn't exist
    DateTimeUtil.parseDateTime("2025-02-30");
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseOutOfRangeTime() {
    // 25 hours doesn't exist
    DateTimeUtil.parseDateTime("2025-03-04T25:00");
  }

  @Test
  public void testParseValidEdgeCases() {
    // Test last day of months
    assertNotNull("Should parse last day of month",
        DateTimeUtil.parseDateTime("2025-01-31"));
    assertNotNull("Should parse last day of February in non-leap year",
        DateTimeUtil.parseDateTime("2025-02-28"));
    assertNotNull("Should parse last day of February in leap year",
        DateTimeUtil.parseDateTime("2024-02-29"));

    // Test midnight and last minute of day
    assertNotNull("Should parse midnight time",
        DateTimeUtil.parseDateTime("2025-03-04T00:00"));
    assertNotNull("Should parse last minute of day",
        DateTimeUtil.parseDateTime("2025-03-04T23:59"));
  }
}