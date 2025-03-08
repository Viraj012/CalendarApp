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
  private int occurrences; // -1 if using untilDate
  private LocalDateTime untilDate; // null if using occurrences

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

    int count = 0;

    while (true) {
      if (weekdays.contains(currentDate.getDayOfWeek())) {
        dates.add(currentDate);
        count++;

        if (occurrences != -1 && count >= occurrences) {
          break;
        }
      }

      currentDate = currentDate.plusDays(1);

      if (untilDate != null && currentDate.isAfter(untilDate)) {
        break;
      }
    }

    return dates;
  }
}