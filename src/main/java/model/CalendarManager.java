package model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages multiple calendars in the application.
 */
public class CalendarManager {
  private Map<String, Calendar> calendars;
  private Calendar currentCalendar;

  /**
   * Creates a new calendar manager.
   */
  public CalendarManager() {
    this.calendars = new HashMap<>();
    this.currentCalendar = null;
  }

  /**
   * Creates a new calendar.
   *
   * @param name     the unique name for the calendar
   * @param timezone the timezone for the calendar
   * @return true if the calendar was created successfully, false if the name is already used
   */
  public boolean createCalendar(String name, ZoneId timezone) {
    if (calendars.containsKey(name)) {
      return false;
    }
    Calendar calendar = new CalendarImpl(name, timezone);
    calendars.put(name, calendar);
    return true;
  }

  /**
   * Sets the current calendar context.
   *
   * @param calendarName the name of the calendar to use
   * @return true if the calendar exists and was set, false otherwise
   */
  public boolean useCalendar(String calendarName) {
    if (!calendars.containsKey(calendarName)) {
      return false;
    }
    currentCalendar = calendars.get(calendarName);
    return true;
  }

  /**
   * Gets the current calendar.
   *
   * @return the current calendar or null if none is set
   */
  public Calendar getCurrentCalendar() {
    return currentCalendar;
  }

  /**
   * Checks if a calendar exists.
   *
   * @param calendarName the name to check
   * @return true if the calendar exists
   */
  public boolean calendarExists(String calendarName) {
    return calendars.containsKey(calendarName);
  }

  /**
   * Gets a calendar by name.
   *
   * @param calendarName the name of the calendar
   * @return the calendar or null if not found
   */
  public Calendar getCalendar(String calendarName) {
    return calendars.get(calendarName);
  }

  // Other methods...

  /**
   * Copies all events from the current calendar within a date range to a target calendar.
   * The times are converted to the timezone of the target calendar, preserving the absolute moment.
   * Recurring events are copied as recurring events rather than individual occurrences.
   *
   * @param startDate the start date of the range (inclusive)
   * @param endDate the end date of the range (inclusive)
   * @param targetCalendarName the name of the target calendar
   * @param targetStartDate the target start date in the target calendar's timezone
   * @return true if at least one event was copied successfully
   */
  public boolean copyEventsInRange(LocalDateTime startDate, LocalDateTime endDate,
      String targetCalendarName, LocalDateTime targetStartDate) {
    if (currentCalendar == null || !calendars.containsKey(targetCalendarName)) {
      return false;
    }

    Calendar sourceCalendar = currentCalendar;
    Calendar targetCalendar = calendars.get(targetCalendarName);

    // Calculate day offset between source start date and target start date
    long dayOffset = ChronoUnit.DAYS.between(
        startDate.toLocalDate(),
        targetStartDate.toLocalDate()
    );

    // Get all events in the calendar (we'll filter them as needed)
    List<Event> allEvents = sourceCalendar.getAllEvents();

    if (allEvents.isEmpty()) {
      return false;
    }

    boolean atLeastOneCopied = false;
    Map<String, Boolean> processedRecurringEvents = new HashMap<>();

    for (Event event : allEvents) {
      // Check if the event occurs within our date range
      boolean inRange = false;
      String eventId = event.getSubject(); // Use subject as ID for tracking

      if (event.isRecurring()) {
        // For recurring events, check if any occurrence falls within our range
        RecurrencePattern pattern = ((EventImpl) event).getRecurrence();
        List<LocalDateTime> occurrences = pattern.calculateRecurrences(event.getStartDateTime());

        for (LocalDateTime occurrence : occurrences) {
          if (!occurrence.toLocalDate().isBefore(startDate.toLocalDate()) &&
              !occurrence.toLocalDate().isAfter(endDate.toLocalDate())) {
            inRange = true;
            break;
          }
        }

        // Skip if we've already processed this recurring event or it's not in range
        if (!inRange || processedRecurringEvents.containsKey(eventId)) {
          continue;
        }

        processedRecurringEvents.put(eventId, true);
      } else {
        // For regular events, check if the event date is in our range
        if (!event.getStartDateTime().toLocalDate().isBefore(startDate.toLocalDate()) &&
            !event.getStartDateTime().toLocalDate().isAfter(endDate.toLocalDate())) {
          inRange = true;
        }

        if (!inRange) {
          continue;
        }
      }

      boolean success = false;

      if (event.isRecurring()) {
        EventImpl originalEvent = (EventImpl) event;
        RecurrencePattern originalPattern = originalEvent.getRecurrence();

        // Calculate new start date in target calendar
        LocalDateTime originalStart = originalEvent.getStartDateTime();

        // Convert between timezones
        ZonedDateTime originalZDT = originalStart.atZone(sourceCalendar.getTimezone());
        ZonedDateTime targetZDT = originalZDT.withZoneSameInstant(targetCalendar.getTimezone());

        // Calculate the date in target calendar that corresponds to the first day of the pattern
        LocalDate targetDate = targetStartDate.toLocalDate().plusDays(
            ChronoUnit.DAYS.between(startDate.toLocalDate(), originalStart.toLocalDate())
        );

        // Create the new start datetime preserving the time component from timezone conversion
        LocalDateTime newStartDateTime = LocalDateTime.of(
            targetDate,
            targetZDT.toLocalTime()
        );

        // Handle end time conversion for regular events
        LocalDateTime newEndTime = null;
        if (!originalEvent.isAllDay() && originalEvent.getEndDateTime() != null) {
          ZonedDateTime originalEndZDT = originalEvent.getEndDateTime().atZone(sourceCalendar.getTimezone());
          ZonedDateTime targetEndZDT = originalEndZDT.withZoneSameInstant(targetCalendar.getTimezone());

          // Preserve duration
          long durationMinutes = ChronoUnit.MINUTES.between(
              originalZDT.toLocalDateTime(),
              originalEndZDT.toLocalDateTime()
          );

          newEndTime = newStartDateTime.plusMinutes(durationMinutes);
        }

        // Calculate new until date if present
        LocalDateTime newUntilDate = null;
        if (originalPattern.getUntilDate() != null) {
          // Calculate days offset between original start and until date
          long untilDaysOffset = ChronoUnit.DAYS.between(
              originalStart.toLocalDate(),
              originalPattern.getUntilDate().toLocalDate()
          );

          // Apply same offset to new start date
          newUntilDate = newStartDateTime.toLocalDate().plusDays(untilDaysOffset)
              .atTime(newStartDateTime.toLocalTime());
        }

        // Create the new recurring event preserving the pattern
        if (originalEvent.isAllDay()) {
          success = targetCalendar.createRecurringAllDayEvent(
              originalEvent.getSubject(),
              newStartDateTime,
              weekdaysToString(originalPattern.getWeekdays()),
              originalPattern.getOccurrences(),
              newUntilDate,
              true,
              originalEvent.getDescription(),
              originalEvent.getLocation(),
              originalEvent.isPublic()
          );
        } else {
          success = targetCalendar.createRecurringEvent(
              originalEvent.getSubject(),
              newStartDateTime,
              newEndTime,
              weekdaysToString(originalPattern.getWeekdays()),
              originalPattern.getOccurrences(),
              newUntilDate,
              true,
              originalEvent.getDescription(),
              originalEvent.getLocation(),
              originalEvent.isPublic()
          );
        }
      } else {
        // Handle non-recurring events (similar to original implementation)
        EventImpl originalEvent = (EventImpl) event;

        // Calculate offset from start of range
        long eventDayOffset = ChronoUnit.DAYS.between(
            startDate.toLocalDate(),
            originalEvent.getStartDateTime().toLocalDate()
        );

        // Calculate target date
        LocalDate targetEventDate = targetStartDate.toLocalDate().plusDays(eventDayOffset);

        if (originalEvent.isAllDay()) {
          // All-day events just shift to the corresponding target date
          LocalDateTime newStartDateTime = targetEventDate.atStartOfDay();

          success = targetCalendar.createAllDayEvent(
              originalEvent.getSubject(),
              newStartDateTime,
              true,
              originalEvent.getDescription(),
              originalEvent.getLocation(),
              originalEvent.isPublic()
          );
        } else {
          // Convert between timezones
          ZonedDateTime sourceZDT = originalEvent.getStartDateTime().atZone(sourceCalendar.getTimezone());
          ZonedDateTime targetZDT = sourceZDT.withZoneSameInstant(targetCalendar.getTimezone());

          // Create new start datetime on the target date with converted time
          LocalDateTime newStartDateTime = LocalDateTime.of(
              targetEventDate,
              targetZDT.toLocalTime()
          );

          // Handle end time conversion
          LocalDateTime newEndTime = null;
          if (originalEvent.getEndDateTime() != null) {
            ZonedDateTime sourceEndZDT = originalEvent.getEndDateTime().atZone(sourceCalendar.getTimezone());
            ZonedDateTime targetEndZDT = sourceEndZDT.withZoneSameInstant(targetCalendar.getTimezone());

            // Calculate duration
            long durationMinutes = ChronoUnit.MINUTES.between(
                sourceZDT.toLocalDateTime(),
                sourceEndZDT.toLocalDateTime()
            );

            newEndTime = newStartDateTime.plusMinutes(durationMinutes);
          }

          success = targetCalendar.createEvent(
              originalEvent.getSubject(),
              newStartDateTime,
              newEndTime,
              true,
              originalEvent.getDescription(),
              originalEvent.getLocation(),
              originalEvent.isPublic()
          );
        }
      }

      if (success) {
        atLeastOneCopied = true;
      }
    }

    return atLeastOneCopied;
  }

  // Helper method to convert weekdays set to string
  private String weekdaysToString(Set<DayOfWeek> weekdays) {
    StringBuilder sb = new StringBuilder();

    for (DayOfWeek day : weekdays) {
      switch (day) {
        case MONDAY:
          sb.append("M");
          break;
        case TUESDAY:
          sb.append("T");
          break;
        case WEDNESDAY:
          sb.append("W");
          break;
        case THURSDAY:
          sb.append("R");
          break;
        case FRIDAY:
          sb.append("F");
          break;
        case SATURDAY:
          sb.append("S");
          break;
        case SUNDAY:
          sb.append("U");
          break;
      }
    }

    return sb.toString();
  }

  // Other methods preserved from the original implementation...
}