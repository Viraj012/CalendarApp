package model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a pattern for recurring events.
 */
public class RecurrencePattern {
  private Set<DayOfWeek> weekdays;
  private int occurrences;
  private LocalDateTime untilDate;

  /**
   * Creates a new recurrence pattern.
   *
   * @param weekdaysStr string representation of weekdays (e.g., "MRU")
   * @param occurrences number of occurrences (-1 if using untilDate)
   * @param untilDate the end date for recurrence (null if using occurrences)
   */
  public RecurrencePattern(String weekdaysStr, int occurrences, LocalDateTime untilDate) {
    this.weekdays = parseWeekdays(weekdaysStr);
    this.occurrences = occurrences;
    this.untilDate = untilDate;
  }

  /**
   * Parse weekday string into a set of DayOfWeek values.
   *
   * @param weekdaysStr string representation (M=Monday, T=Tuesday, W=Wednesday,
   *                    R=Thursday, F=Friday, S=Saturday, U=Sunday)
   * @return set of DayOfWeek values
   */
  private Set<DayOfWeek> parseWeekdays(String weekdaysStr) {
    Set<DayOfWeek> days = new HashSet<>();
    for (char c : weekdaysStr.toCharArray()) {
      switch (c) {
        case 'M':
          days.add(DayOfWeek.MONDAY);
          break;
        case 'T':
          days.add(DayOfWeek.TUESDAY);
          break;
        case 'W':
          days.add(DayOfWeek.WEDNESDAY);
          break;
        case 'R':
          days.add(DayOfWeek.THURSDAY);
          break;
        case 'F':
          days.add(DayOfWeek.FRIDAY);
          break;
        case 'S':
          days.add(DayOfWeek.SATURDAY);
          break;
        case 'U':
          days.add(DayOfWeek.SUNDAY);
          break;
        default:
          throw new IllegalArgumentException("Invalid weekday character: " + c);
      }
    }
    return days;
  }

  /**
   * Get the set of weekdays for this recurrence.
   *
   * @return set of DayOfWeek values
   */
  public Set<DayOfWeek> getWeekdays() {
    return new HashSet<>(weekdays);
  }

  /**
   * Get the number of occurrences.
   *
   * @return number of occurrences (-1 if using untilDate)
   */
  public int getOccurrences() {
    return occurrences;
  }

  /**
   * Get the end date for recurrence.
   *
   * @return end date (null if using occurrences)
   */
  public LocalDateTime getUntilDate() {
    return untilDate;
  }

  /**
   * Calculate all instances of this recurring event starting from a base date.
   *
   * @param baseDate the starting date
   * @return list of dates for this recurrence
   */
  public List<LocalDateTime> calculateRecurrences(LocalDateTime baseDate) {
    List<LocalDateTime> dates = new ArrayList<>();
    LocalDateTime currentDate = baseDate;

    // Ensure the base date is included if it falls on a valid weekday
    if (weekdays.contains(baseDate.getDayOfWeek())) {
      dates.add(baseDate);
    }

    // Avoid infinite loops - set a maximum number of days to check
    LocalDateTime maxEndDate = baseDate.plusYears(5); // 5 years is a reasonable limit
    if (untilDate != null && untilDate.isBefore(maxEndDate)) {
      maxEndDate = untilDate;
    }

    // Move to the next day after the base date
    currentDate = baseDate.plusDays(1);

    while (true) {
      // Check if we've reached the occurrence limit
      if (occurrences != -1 && dates.size() >= occurrences) {
        break;
      }

      // Check if we've reached the until date
      if (untilDate != null && currentDate.isAfter(untilDate)) {
        break;
      }

      // Check if we've gone beyond the reasonable limit
      if (currentDate.isAfter(maxEndDate)) {
        break;
      }

      // Check if the current day is in our weekday set
      if (weekdays.contains(currentDate.getDayOfWeek())) {
        // Create a datetime with the same time as the base event but on this date
        LocalDateTime recurrenceDateTime = LocalDateTime.of(
            currentDate.toLocalDate(),
            baseDate.toLocalTime()
        );
        dates.add(recurrenceDateTime);
      }

      // Move to the next day
      currentDate = currentDate.plusDays(1);
    }

    return dates;
  }
}