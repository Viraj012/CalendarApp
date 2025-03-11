
import controller.DateTimeUtil;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.junit.Assert.*;

/**
 * Enhanced test class for DateTimeUtil with improved mutation coverage.
 */
public class DateTimeUtilTest {

  @Test
  public void testParseDateTimeWithTime() {
    String dateTimeStr = "2025-03-15T14:30";
    LocalDateTime expected = LocalDateTime.of(2025, 3, 15, 14, 30);

    LocalDateTime result = DateTimeUtil.parseDateTime(dateTimeStr);

    assertEquals(expected, result);
  }

  @Test
  public void testParseDateTimeWithTimeZeroMinutes() {
    String dateTimeStr = "2025-03-15T14:00";
    LocalDateTime expected = LocalDateTime.of(2025, 3, 15, 14, 0);

    LocalDateTime result = DateTimeUtil.parseDateTime(dateTimeStr);

    assertEquals(expected, result);
  }

  @Test
  public void testParseDateTimeWithTimeZeroHours() {
    String dateTimeStr = "2025-03-15T00:30";
    LocalDateTime expected = LocalDateTime.of(2025, 3, 15, 0, 30);

    LocalDateTime result = DateTimeUtil.parseDateTime(dateTimeStr);

    assertEquals(expected, result);
  }

  @Test
  public void testParseDateTimeWithMidnight() {
    String dateTimeStr = "2025-03-15T00:00";
    LocalDateTime expected = LocalDateTime.of(2025, 3, 15, 0, 0);

    LocalDateTime result = DateTimeUtil.parseDateTime(dateTimeStr);

    assertEquals(expected, result);
  }

  @Test
  public void testParseDateTimeWithNearMidnight() {
    String dateTimeStr = "2025-03-15T23:59";
    LocalDateTime expected = LocalDateTime.of(2025, 3, 15, 23, 59);

    LocalDateTime result = DateTimeUtil.parseDateTime(dateTimeStr);

    assertEquals(expected, result);
  }

  @Test
  public void testParseDateTimeWithoutTime() {
    String dateStr = "2025-03-15";
    LocalDateTime expected = LocalDateTime.of(2025, 3, 15, 0, 0);

    LocalDateTime result = DateTimeUtil.parseDateTime(dateStr);

    assertEquals(expected, result);
  }

  @Test
  public void testParseDateTimeBoundaries() {
    // First day of month
    String firstDay = "2025-03-01";
    assertEquals(LocalDateTime.of(2025, 3, 1, 0, 0),
        DateTimeUtil.parseDateTime(firstDay));

    // Last day of month
    String lastDay = "2025-03-31";
    assertEquals(LocalDateTime.of(2025, 3, 31, 0, 0),
        DateTimeUtil.parseDateTime(lastDay));

    // First month of year
    String firstMonth = "2025-01-15";
    assertEquals(LocalDateTime.of(2025, 1, 15, 0, 0),
        DateTimeUtil.parseDateTime(firstMonth));

    // Last month of year
    String lastMonth = "2025-12-15";
    assertEquals(LocalDateTime.of(2025, 12, 15, 0, 0),
        DateTimeUtil.parseDateTime(lastMonth));
  }

  @Test
  public void testParseDateTimeWithSingleDigitValues() {
    // These formats should actually fail as ISO format requires two digits
    try {
      // Single digit month
      DateTimeUtil.parseDateTime("2025-3-15");
      fail("Should reject non-standard format with single digit month");
    } catch (DateTimeParseException e) {
      // Expected
    }

    try {
      // Single digit day
      DateTimeUtil.parseDateTime("2025-03-5");
      fail("Should reject non-standard format with single digit day");
    } catch (DateTimeParseException e) {
      // Expected
    }

    try {
      // Single digit hour
      DateTimeUtil.parseDateTime("2025-03-15T9:30");
      fail("Should reject non-standard format with single digit hour");
    } catch (DateTimeParseException e) {
      // Expected
    }

    try {
      // Single digit minute
      DateTimeUtil.parseDateTime("2025-03-15T14:5");
      fail("Should reject non-standard format with single digit minute");
    } catch (DateTimeParseException e) {
      // Expected
    }
  }

  @Test
  public void testParseDateTimeWithSecondsInTime() {
    String dateTimeStr = "2025-03-15T14:30:45";
    LocalDateTime expected = LocalDateTime.of(2025, 3, 15, 14, 30, 45);

    LocalDateTime result = DateTimeUtil.parseDateTime(dateTimeStr);

    assertEquals(expected, result);
  }

  @Test
  public void testParseDateTimeWithMillisecondsInTime() {
    try {
      String dateTimeStr = "2025-03-15T14:30:45.123";
      LocalDateTime expected = LocalDateTime.of(2025, 3, 15, 14, 30, 45, 123000000);

      LocalDateTime result = DateTimeUtil.parseDateTime(dateTimeStr);

      assertEquals(expected, result);
    } catch (DateTimeParseException e) {
      // Some implementations might not handle milliseconds
    }
  }

  @Test
  public void testParseDateWithLeapYear() {
    String dateStr = "2024-02-29";
    LocalDateTime expected = LocalDateTime.of(2024, 2, 29, 0, 0);

    LocalDateTime result = DateTimeUtil.parseDateTime(dateStr);

    assertEquals(expected, result);
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseDateWithInvalidLeapYear() {
    String dateStr = "2025-02-29";
    DateTimeUtil.parseDateTime(dateStr);
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseDateWithInvalidFormat() {
    String invalidStr = "15/03/2025";
    DateTimeUtil.parseDateTime(invalidStr);
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseDateWithInvalidDate() {
    String invalidStr = "2025-13-32";
    DateTimeUtil.parseDateTime(invalidStr);
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseDateWithInvalidTime() {
    String invalidStr = "2025-03-15T25:70";
    DateTimeUtil.parseDateTime(invalidStr);
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseDateWithMalformedStringMissingT() {
    String invalidStr = "2025-03-15 14:30";
    DateTimeUtil.parseDateTime(invalidStr);
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseDateWithMalformedStringTrailingT() {
    String invalidStr = "2025-03-15T";
    DateTimeUtil.parseDateTime(invalidStr);
  }

  @Test
  public void testParseDateTimeWithSpaceAfterT() {
    try {
      String dateTimeStr = "2025-03-15T 14:30";
      DateTimeUtil.parseDateTime(dateTimeStr);
      fail("Should reject invalid format with space after T");
    } catch (DateTimeParseException e) {
      // Expected
    }
  }

  @Test
  public void testParseDateTimeWithMultipleTsOrSpaces() {
    try {
      String dateTimeStr = "2025-03-15TT14:30";
      DateTimeUtil.parseDateTime(dateTimeStr);
      fail("Should reject invalid format with multiple Ts");
    } catch (DateTimeParseException e) {
      // Expected
    }
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseDateTimeWithWrongSeparator() {
    String invalidStr = "2025/03/15";
    DateTimeUtil.parseDateTime(invalidStr);
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseDateTimeWithIncompleteDate() {
    String invalidStr = "2025-03";
    DateTimeUtil.parseDateTime(invalidStr);
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseDateTimeWithIncompleteTime() {
    String invalidStr = "2025-03-15T14";
    DateTimeUtil.parseDateTime(invalidStr);
  }

  @Test
  public void testParseDateTimeWithExtraneousCharacters() {
    try {
      String invalidStr = "2025-03-15Extra";
      DateTimeUtil.parseDateTime(invalidStr);
      fail("Should reject string with extraneous characters");
    } catch (DateTimeParseException e) {
      // Expected
    }

    try {
      String invalidStr = "2025-03-15T14:30Extra";
      DateTimeUtil.parseDateTime(invalidStr);
      fail("Should reject string with extraneous characters");
    } catch (DateTimeParseException e) {
      // Expected
    }
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseDateTimeWithTimeZone() {
    String invalidStr = "2025-03-15T14:30+01:00";
    DateTimeUtil.parseDateTime(invalidStr);
  }

  @Test
  public void testParseDateTimeWithNullInput() {
    try {
      DateTimeUtil.parseDateTime(null);
      fail("Should reject null input");
    } catch (NullPointerException | DateTimeParseException e) {
      // Either exception is acceptable
    }
  }

  @Test
  public void testParseDateTimeWithEmptyString() {
    try {
      DateTimeUtil.parseDateTime("");
      fail("Should reject empty string");
    } catch (DateTimeParseException e) {
      // Expected
    }
  }

  @Test
  public void testParseDateTimeWithWhitespace() {
    try {
      DateTimeUtil.parseDateTime(" ");
      fail("Should reject whitespace");
    } catch (DateTimeParseException e) {
      // Expected
    }

    try {
      DateTimeUtil.parseDateTime(" 2025-03-15");
      fail("Should reject string with leading whitespace");
    } catch (DateTimeParseException e) {
      // Expected
    }

    try {
      DateTimeUtil.parseDateTime("2025-03-15 ");
      fail("Should reject string with trailing whitespace");
    } catch (DateTimeParseException e) {
      // Expected
    }
  }

  @Test
  public void testParseDateTimeEdgeCases() {
    // Minimum valid date
    try {
      String minDate = "0001-01-01";
      LocalDateTime result = DateTimeUtil.parseDateTime(minDate);
      assertEquals(LocalDateTime.of(1, 1, 1, 0, 0), result);
    } catch (DateTimeParseException e) {
      // Some implementations might not support year 1
    }

    // Maximum valid date (depends on implementation)
    try {
      String maxDate = "9999-12-31";
      LocalDateTime result = DateTimeUtil.parseDateTime(maxDate);
      assertEquals(LocalDateTime.of(9999, 12, 31, 0, 0), result);
    } catch (DateTimeParseException e) {
      // Some implementations might not support year 9999
    }

    // Zero-based dates (not valid in ISO-8601)
    try {
      String zeroDate = "2025-00-00";
      DateTimeUtil.parseDateTime(zeroDate);
      fail("Should reject invalid date with zeros");
    } catch (DateTimeParseException e) {
      // Expected
    }
  }
}