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
  /**
   * Edits a calendar property.
   *
   * @param calendarName the name of the calendar to edit
   * @param property     the property to edit ("name" or "timezone")
   * @param newValue     the new value for the property
   * @return true if successful, false otherwise
   */
  public boolean editCalendar(String calendarName, String property, String newValue) {
    if (!calendars.containsKey(calendarName)) {
      return false;
    }

    Calendar calendar = calendars.get(calendarName);

    switch (property.toLowerCase()) {
      case "name":
        if (calendars.containsKey(newValue)) {
          return false; // New name already exists
        }
        // Update the map key
        calendars.remove(calendarName);
        ((CalendarImpl) calendar).setName(newValue);
        calendars.put(newValue, calendar);

        // Update current calendar reference if needed
        if (currentCalendar == calendar) {
          currentCalendar = calendar;
        }
        return true;

      case "timezone":
        try {
          ZoneId oldZone = calendar.getTimezone();
          ZoneId newZone = ZoneId.of(newValue);

          // Update all events for the timezone change
          updateEventsForTimezoneChange((CalendarImpl) calendar, oldZone, newZone);

          // Set the new timezone
          ((CalendarImpl) calendar).setTimezone(newZone);
          return true;
        } catch (Exception e) {
          return false; // Invalid timezone
        }

      default:
        return false; // Unknown property
    }
  }

  /**
   * Updates all events in a calendar when the timezone is changed.
   * Preserves all event properties including recurrence while adjusting times.
   *
   * @param calendar the calendar being updated
   * @param oldZone the old timezone
   * @param newZone the new timezone
   */
  private void updateEventsForTimezoneChange(CalendarImpl calendar, ZoneId oldZone, ZoneId newZone) {
    // Get all current events
    List<Event> originalEvents = new ArrayList<>(calendar.getAllEvents());

    // Create a new list for converted events
    List<Event> convertedEvents = new ArrayList<>();

    for (Event event : originalEvents) {
      EventImpl originalImpl = (EventImpl) event;

      if (event.isAllDay()) {
        // For all-day events, keep the same date
        if (event.isRecurring()) {
          RecurrencePattern pattern = originalImpl.getRecurrence();

          // Create new all-day recurring event with same properties
          EventImpl newEvent = new EventImpl(
              originalImpl.getSubject(),
              originalImpl.getStartDateTime(),
              weekdaysToString(pattern.getWeekdays()),
              pattern.getOccurrences(),
              pattern.getUntilDate()
          );

          // Copy descriptive properties
          newEvent.setDescription(originalImpl.getDescription());
          newEvent.setLocation(originalImpl.getLocation());
          newEvent.setPublic(originalImpl.isPublic());

          convertedEvents.add(newEvent);
        } else {
          // For regular all-day events, we can keep them as is
          convertedEvents.add(event);
        }
      } else {
        // For regular events, adjust the time based on timezone difference

        // Convert start time
        LocalDateTime oldStartDateTime = event.getStartDateTime();
        ZonedDateTime oldStartZDT = oldStartDateTime.atZone(oldZone);
        ZonedDateTime newStartZDT = oldStartZDT.withZoneSameInstant(newZone);
        LocalDateTime newStartDateTime = newStartZDT.toLocalDateTime();

        // Convert end time if present
        LocalDateTime newEndDateTime = null;
        if (event.getEndDateTime() != null) {
          ZonedDateTime oldEndZDT = event.getEndDateTime().atZone(oldZone);
          ZonedDateTime newEndZDT = oldEndZDT.withZoneSameInstant(newZone);
          newEndDateTime = newEndZDT.toLocalDateTime();
        }

        if (event.isRecurring()) {
          RecurrencePattern pattern = originalImpl.getRecurrence();

          // Convert until date if present
          LocalDateTime newUntilDate = null;
          if (pattern.getUntilDate() != null) {
            ZonedDateTime oldUntilZDT = pattern.getUntilDate().atZone(oldZone);
            ZonedDateTime newUntilZDT = oldUntilZDT.withZoneSameInstant(newZone);
            newUntilDate = newUntilZDT.toLocalDateTime();
          }

          // Create recurring event with adjusted times but same recurrence pattern
          EventImpl newEvent = new EventImpl(
              originalImpl.getSubject(),
              newStartDateTime,
              newEndDateTime,
              weekdaysToString(pattern.getWeekdays()),
              pattern.getOccurrences(),
              newUntilDate
          );

          // Copy descriptive properties
          newEvent.setDescription(originalImpl.getDescription());
          newEvent.setLocation(originalImpl.getLocation());
          newEvent.setPublic(originalImpl.isPublic());

          convertedEvents.add(newEvent);
        } else {
          // Create regular event with adjusted times
          EventImpl newEvent = new EventImpl(
              originalImpl.getSubject(),
              newStartDateTime,
              newEndDateTime
          );

          // Copy descriptive properties
          newEvent.setDescription(originalImpl.getDescription());
          newEvent.setLocation(originalImpl.getLocation());
          newEvent.setPublic(originalImpl.isPublic());

          convertedEvents.add(newEvent);
        }
      }
    }

    // Replace all events in the calendar
    calendar.clearEvents();
    for (Event convertedEvent : convertedEvents) {
      calendar.addEvent(convertedEvent);
    }
  }
  public boolean copyEvent(String eventName, LocalDateTime startDateTime,
      String targetCalendarName, LocalDateTime targetDateTime) {
    if (currentCalendar == null || !calendars.containsKey(targetCalendarName)) {
      return false;
    }

    Calendar sourceCalendar = currentCalendar;
    Calendar targetCalendar = calendars.get(targetCalendarName);

    // Try to find the event with both approaches
    Event eventToCopy = null;

    // First, check all events to find a recurring event with a matching name
    List<Event> allEvents = sourceCalendar.getAllEvents();
    for (Event event : allEvents) {
      if (event.isRecurring() && isEventNameMatch(event.getSubject(), eventName)) {
        // For recurring events, check if the specified date is one of its occurrences
        RecurrencePattern pattern = ((EventImpl) event).getRecurrence();
        List<LocalDateTime> occurrences = pattern.calculateRecurrences(event.getStartDateTime());

        for (LocalDateTime occurrence : occurrences) {
          if (occurrence.toLocalDate().equals(startDateTime.toLocalDate()) &&
              occurrence.getHour() == startDateTime.getHour() &&
              occurrence.getMinute() == startDateTime.getMinute()) {
            eventToCopy = event;
            break;
          }
        }

        if (eventToCopy != null) {
          break;
        }
      }
    }

    // If no recurring event found, try the regular approach
    if (eventToCopy == null) {
      List<Event> events = sourceCalendar.getEventsOn(startDateTime);
      for (Event event : events) {
        if (isEventNameMatch(event.getSubject(), eventName) &&
            event.getStartDateTime().toLocalDate().equals(startDateTime.toLocalDate()) &&
            event.getStartDateTime().getHour() == startDateTime.getHour() &&
            event.getStartDateTime().getMinute() == startDateTime.getMinute()) {
          eventToCopy = event;
          break;
        }
      }
    }

    if (eventToCopy == null) {
      return false;
    }

    // Copy the event to the target calendar with the specified target datetime
    if (eventToCopy.isAllDay()) {
      if (eventToCopy.isRecurring()) {
        RecurrencePattern pattern = ((EventImpl) eventToCopy).getRecurrence();

        // Calculate new until date
        LocalDateTime newUntilDate = null;
        if (pattern.getUntilDate() != null) {
          long daysBetween = ChronoUnit.DAYS.between(
              eventToCopy.getStartDateTime().toLocalDate(),
              pattern.getUntilDate().toLocalDate());

          newUntilDate = targetDateTime.toLocalDate().plusDays(daysBetween).atTime(0, 0);
        }

        return targetCalendar.createRecurringAllDayEvent(
            eventToCopy.getSubject().replace(" [Recurring]", ""), // Remove suffix when copying
            targetDateTime,
            weekdaysToString(pattern.getWeekdays()),
            pattern.getOccurrences(),
            newUntilDate,
            true,
            eventToCopy.getDescription(),
            eventToCopy.getLocation(),
            eventToCopy.isPublic()
        );
      } else {
        return targetCalendar.createAllDayEvent(
            eventToCopy.getSubject(),
            targetDateTime,
            true,
            eventToCopy.getDescription(),
            eventToCopy.getLocation(),
            eventToCopy.isPublic()
        );
      }
    } else {
      // For regular event - calculate new end time maintaining the same duration
      LocalDateTime newEndTime = null;
      if (eventToCopy.getEndDateTime() != null) {
        long durationMinutes = ChronoUnit.MINUTES.between(
            eventToCopy.getStartDateTime(),
            eventToCopy.getEndDateTime());

        newEndTime = targetDateTime.plusMinutes(durationMinutes);
      }

      if (eventToCopy.isRecurring()) {
        RecurrencePattern pattern = ((EventImpl) eventToCopy).getRecurrence();

        // Calculate new until date
        LocalDateTime newUntilDate = null;
        if (pattern.getUntilDate() != null) {
          long daysBetween = ChronoUnit.DAYS.between(
              eventToCopy.getStartDateTime().toLocalDate(),
              pattern.getUntilDate().toLocalDate());

          newUntilDate = targetDateTime.toLocalDate().plusDays(daysBetween)
              .atTime(targetDateTime.toLocalTime());
        }

        return targetCalendar.createRecurringEvent(
            eventToCopy.getSubject().replace(" [Recurring]", ""), // Remove suffix when copying
            targetDateTime,
            newEndTime,
            weekdaysToString(pattern.getWeekdays()),
            pattern.getOccurrences(),
            newUntilDate,
            true,
            eventToCopy.getDescription(),
            eventToCopy.getLocation(),
            eventToCopy.isPublic()
        );
      } else {
        return targetCalendar.createEvent(
            eventToCopy.getSubject(),
            targetDateTime,
            newEndTime,
            true,
            eventToCopy.getDescription(),
            eventToCopy.getLocation(),
            eventToCopy.isPublic()
        );
      }
    }
  }
  /**
   * Copies all events from the current calendar on a specific date to a target calendar.
   * The times are converted to the timezone of the target calendar, preserving the absolute moment.
   *
   * @param sourceDate the date of events to copy
   * @param targetCalendarName the name of the target calendar
   * @param targetDate the target date in the target calendar's timezone
   * @return true if at least one event was copied successfully
   */
  public boolean copyEventsOnDay(LocalDateTime sourceDate, String targetCalendarName,
      LocalDateTime targetDate) {
    if (currentCalendar == null || !calendars.containsKey(targetCalendarName)) {
      return false;
    }

    Calendar sourceCalendar = currentCalendar;
    Calendar targetCalendar = calendars.get(targetCalendarName);

    // Find all events on the source date
    List<Event> events = sourceCalendar.getEventsOn(sourceDate);

    if (events.isEmpty()) {
      return false;
    }

    boolean atLeastOneCopied = false;

    // The day offset between source date and target date
    long dayOffset = ChronoUnit.DAYS.between(
        sourceDate.toLocalDate(),
        targetDate.toLocalDate()
    );

    for (Event event : events) {
      boolean success = false;

      if (event.isAllDay()) {
        // All-day events just shift to the target date
        LocalDateTime newStartDateTime = targetDate.toLocalDate().atStartOfDay();

        if (event.isRecurring()) {
          RecurrencePattern pattern = ((EventImpl) event).getRecurrence();

          // Adjust the until date if present
          LocalDateTime newUntilDate = null;
          if (pattern.getUntilDate() != null) {
            long daysToUntilDate = ChronoUnit.DAYS.between(
                event.getStartDateTime().toLocalDate(),
                pattern.getUntilDate().toLocalDate());

            newUntilDate = targetDate.toLocalDate().plusDays(daysToUntilDate).atStartOfDay();
          }

          success = targetCalendar.createRecurringAllDayEvent(
              event.getSubject(),
              newStartDateTime,
              weekdaysToString(pattern.getWeekdays()),
              pattern.getOccurrences(),
              newUntilDate,
              true,
              event.getDescription(),
              event.getLocation(),
              event.isPublic()
          );
        } else {
          success = targetCalendar.createAllDayEvent(
              event.getSubject(),
              newStartDateTime,
              true,
              event.getDescription(),
              event.getLocation(),
              event.isPublic()
          );
        }
      } else {
        // Convert the time between timezones
        ZonedDateTime sourceZDT = event.getStartDateTime().atZone(sourceCalendar.getTimezone());
        ZonedDateTime targetZDT = sourceZDT.withZoneSameInstant(targetCalendar.getTimezone());

        // Use the converted time, but adjust the date to be on the target date + any day rollover
        LocalDateTime newStartDateTime;

        // If the hour in the target timezone has rolled to the next day, add a day
        if (targetZDT.getHour() < sourceZDT.getHour() &&
            targetZDT.toLocalDate().isAfter(sourceZDT.toLocalDate())) {
          // Time conversion moved us to the next day
          newStartDateTime = targetDate.toLocalDate().plusDays(1)
              .atTime(targetZDT.getHour(), targetZDT.getMinute());
        } else if (targetZDT.getHour() > sourceZDT.getHour() &&
            targetZDT.toLocalDate().isBefore(sourceZDT.toLocalDate())) {
          // Time conversion moved us to the previous day
          newStartDateTime = targetDate.toLocalDate().minusDays(1)
              .atTime(targetZDT.getHour(), targetZDT.getMinute());
        } else {
          // Same day after conversion
          newStartDateTime = targetDate.toLocalDate()
              .atTime(targetZDT.getHour(), targetZDT.getMinute());
        }

        // Handle end time conversion
        LocalDateTime newEndTime = null;
        if (event.getEndDateTime() != null) {
          ZonedDateTime sourceEndZDT = event.getEndDateTime().atZone(sourceCalendar.getTimezone());
          ZonedDateTime targetEndZDT = sourceEndZDT.withZoneSameInstant(targetCalendar.getTimezone());

          // Calculate how many days difference between start and end in target zone
          long endDaysDiff = ChronoUnit.DAYS.between(
              targetZDT.toLocalDate(),
              targetEndZDT.toLocalDate()
          );

          newEndTime = newStartDateTime.toLocalDate().plusDays(endDaysDiff)
              .atTime(targetEndZDT.getHour(), targetEndZDT.getMinute());
        }

        if (event.isRecurring()) {
          RecurrencePattern pattern = ((EventImpl) event).getRecurrence();

          // Adjust the until date if present
          LocalDateTime newUntilDate = null;
          if (pattern.getUntilDate() != null) {
            ZonedDateTime sourceUntilZDT = pattern.getUntilDate().atZone(sourceCalendar.getTimezone());
            ZonedDateTime targetUntilZDT = sourceUntilZDT.withZoneSameInstant(targetCalendar.getTimezone());

            // Calculate day difference between source until and source start
            long untilDaysDiff = ChronoUnit.DAYS.between(
                event.getStartDateTime().toLocalDate(),
                pattern.getUntilDate().toLocalDate()
            );

            newUntilDate = newStartDateTime.toLocalDate().plusDays(untilDaysDiff)
                .atTime(targetUntilZDT.getHour(), targetUntilZDT.getMinute());
          }

          success = targetCalendar.createRecurringEvent(
              event.getSubject(),
              newStartDateTime,
              newEndTime,
              weekdaysToString(pattern.getWeekdays()),
              pattern.getOccurrences(),
              newUntilDate,
              true,
              event.getDescription(),
              event.getLocation(),
              event.isPublic()
          );
        } else {
          success = targetCalendar.createEvent(
              event.getSubject(),
              newStartDateTime,
              newEndTime,
              true,
              event.getDescription(),
              event.getLocation(),
              event.isPublic()
          );
        }
      }

      if (success) {
        atLeastOneCopied = true;
      }
    }

    return atLeastOneCopied;
  }

  /**
   * Copies all events from the current calendar within a date range to a target calendar.
   * The times are converted to the timezone of the target calendar, preserving the absolute moment.
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

    // Find all events in the date range
    List<Event> events = sourceCalendar.getEventsFrom(startDate, endDate);

    if (events.isEmpty()) {
      return false;
    }

    boolean atLeastOneCopied = false;

    // Calculate the base day offset between source and target start dates
    long baseDayOffset = ChronoUnit.DAYS.between(
        startDate.toLocalDate(),
        targetStartDate.toLocalDate()
    );

    for (Event event : events) {
      // Calculate event's day offset from the start of the source range
      long eventDayOffset = ChronoUnit.DAYS.between(
          startDate.toLocalDate(),
          event.getStartDateTime().toLocalDate()
      );

      // Calculated target date (before timezone conversion)
      LocalDate targetEventDate = targetStartDate.toLocalDate().plusDays(eventDayOffset);

      boolean success = false;

      if (event.isAllDay()) {
        // All-day events just shift to the corresponding target date
        LocalDateTime newStartDateTime = targetEventDate.atStartOfDay();

        if (event.isRecurring()) {
          RecurrencePattern pattern = ((EventImpl) event).getRecurrence();

          // Adjust the until date if present
          LocalDateTime newUntilDate = null;
          if (pattern.getUntilDate() != null) {
            // Calculate relative position of until date from event start
            long daysToUntilDate = ChronoUnit.DAYS.between(
                event.getStartDateTime().toLocalDate(),
                pattern.getUntilDate().toLocalDate()
            );

            newUntilDate = targetEventDate.plusDays(daysToUntilDate).atStartOfDay();
          }

          success = targetCalendar.createRecurringAllDayEvent(
              event.getSubject(),
              newStartDateTime,
              weekdaysToString(pattern.getWeekdays()),
              pattern.getOccurrences(),
              newUntilDate,
              true,
              event.getDescription(),
              event.getLocation(),
              event.isPublic()
          );
        } else {
          success = targetCalendar.createAllDayEvent(
              event.getSubject(),
              newStartDateTime,
              true,
              event.getDescription(),
              event.getLocation(),
              event.isPublic()
          );
        }
      } else {
        // Convert the time between timezones
        ZonedDateTime sourceZDT = event.getStartDateTime().atZone(sourceCalendar.getTimezone());
        ZonedDateTime targetZDT = sourceZDT.withZoneSameInstant(targetCalendar.getTimezone());

        // Use the converted time on the calculated target date, adjusting for day shifts due to timezone
        LocalDateTime newStartDateTime;

        // If the hour in the target timezone has rolled to the next day, add a day
        if (targetZDT.getHour() < sourceZDT.getHour() &&
            targetZDT.toLocalDate().isAfter(sourceZDT.toLocalDate())) {
          // Time conversion moved us to the next day
          newStartDateTime = targetEventDate.plusDays(1)
              .atTime(targetZDT.getHour(), targetZDT.getMinute());
        } else if (targetZDT.getHour() > sourceZDT.getHour() &&
            targetZDT.toLocalDate().isBefore(sourceZDT.toLocalDate())) {
          // Time conversion moved us to the previous day
          newStartDateTime = targetEventDate.minusDays(1)
              .atTime(targetZDT.getHour(), targetZDT.getMinute());
        } else {
          // Same day after conversion
          newStartDateTime = targetEventDate
              .atTime(targetZDT.getHour(), targetZDT.getMinute());
        }

        // Handle end time conversion
        LocalDateTime newEndTime = null;
        if (event.getEndDateTime() != null) {
          ZonedDateTime sourceEndZDT = event.getEndDateTime().atZone(sourceCalendar.getTimezone());
          ZonedDateTime targetEndZDT = sourceEndZDT.withZoneSameInstant(targetCalendar.getTimezone());

          // Calculate how many days difference between start and end in target zone
          long endDaysDiff = ChronoUnit.DAYS.between(
              targetZDT.toLocalDate(),
              targetEndZDT.toLocalDate()
          );

          newEndTime = newStartDateTime.toLocalDate().plusDays(endDaysDiff)
              .atTime(targetEndZDT.getHour(), targetEndZDT.getMinute());
        }

        if (event.isRecurring()) {
          RecurrencePattern pattern = ((EventImpl) event).getRecurrence();

          // Adjust the until date if present
          LocalDateTime newUntilDate = null;
          if (pattern.getUntilDate() != null) {
            ZonedDateTime sourceUntilZDT = pattern.getUntilDate().atZone(sourceCalendar.getTimezone());
            ZonedDateTime targetUntilZDT = sourceUntilZDT.withZoneSameInstant(targetCalendar.getTimezone());

            // Calculate day difference between source until and source start
            long untilDaysDiff = ChronoUnit.DAYS.between(
                event.getStartDateTime().toLocalDate(),
                pattern.getUntilDate().toLocalDate()
            );

            newUntilDate = newStartDateTime.toLocalDate().plusDays(untilDaysDiff)
                .atTime(targetUntilZDT.getHour(), targetUntilZDT.getMinute());
          }

          success = targetCalendar.createRecurringEvent(
              event.getSubject(),
              newStartDateTime,
              newEndTime,
              weekdaysToString(pattern.getWeekdays()),
              pattern.getOccurrences(),
              newUntilDate,
              true,
              event.getDescription(),
              event.getLocation(),
              event.isPublic()
          );
        } else {
          success = targetCalendar.createEvent(
              event.getSubject(),
              newStartDateTime,
              newEndTime,
              true,
              event.getDescription(),
              event.getLocation(),
              event.isPublic()
          );
        }
      }

      if (success) {
        atLeastOneCopied = true;
      }
    }

    return atLeastOneCopied;
  }
  /**
   * Helper method to copy an event preserving its type (all-day vs regular) and properties.
   * Used for the copy events commands.
   *
   * @param event the event to copy
   * @param sourceCalendar the source calendar
   * @param targetCalendar the target calendar
   * @param newStartTime the new start time in the target calendar
   * @param daysDifference the day difference between source and target dates
   * @return true if copied successfully
   */
//  private boolean copyEventPreservingType(Event event, Calendar sourceCalendar,
//      Calendar targetCalendar, LocalDateTime newStartTime, long daysDifference) {
//
//    if (event.isAllDay()) {
//      if (event.isRecurring()) {
//        RecurrencePattern pattern = ((EventImpl) event).getRecurrence();
//
//        // Calculate new until date if present
//        LocalDateTime newUntilDate = null;
//        if (pattern.getUntilDate() != null) {
//          newUntilDate = pattern.getUntilDate().plusDays(daysDifference);
//        }
//
//        return targetCalendar.createRecurringAllDayEvent(
//            event.getSubject(),
//            newStartTime,
//            weekdaysToString(pattern.getWeekdays()),
//            pattern.getOccurrences(),
//            newUntilDate,
//            true,
//            event.getDescription(),
//            event.getLocation(),
//            event.isPublic()
//        );
//      } else {
//        return targetCalendar.createAllDayEvent(
//            event.getSubject(),
//            newStartTime,
//            true,
//            event.getDescription(),
//            event.getLocation(),
//            event.isPublic()
//        );
//      }
//    } else {
//      // For regular events, calculate the new end time preserving duration
//      LocalDateTime newEndTime = null;
//      if (event.getEndDateTime() != null) {
//        // Calculate duration in minutes
//        long durationMinutes = ChronoUnit.MINUTES.between(
//            event.getStartDateTime(),
//            event.getEndDateTime());
//
//        // Apply same duration to new start time
//        newEndTime = newStartTime.plusMinutes(durationMinutes);
//      }
//
//      if (event.isRecurring()) {
//        RecurrencePattern pattern = ((EventImpl) event).getRecurrence();
//
//        // Calculate new until date if present
//        LocalDateTime newUntilDate = null;
//        if (pattern.getUntilDate() != null) {
//          newUntilDate = pattern.getUntilDate().plusDays(daysDifference);
//        }
//
//        return targetCalendar.createRecurringEvent(
//            event.getSubject(),
//            newStartTime,
//            newEndTime,
//            weekdaysToString(pattern.getWeekdays()),
//            pattern.getOccurrences(),
//            newUntilDate,
//            true,
//            event.getDescription(),
//            event.getLocation(),
//            event.isPublic()
//        );
//      } else {
//        return targetCalendar.createEvent(
//            event.getSubject(),
//            newStartTime,
//            newEndTime,
//            true,
//            event.getDescription(),
//            event.getLocation(),
//            event.isPublic()
//        );
//      }
//    }
//  }

  /**
   * Helper method to check if an event name matches, handling quoted names and [Recurring] suffix.
   *
   * @param storedName the stored event name
   * @param searchName the name to search for
   * @return true if names match
   */
  private boolean isEventNameMatch(String storedName, String searchName) {
    // Check direct match
    if (storedName.equals(searchName)) {
      return true;
    }

    // Check with quotes added
    if (!searchName.startsWith("\"") && !searchName.endsWith("\"")) {
      if (storedName.equals("\"" + searchName + "\"")) {
        return true;
      }
    }

    // Check with quotes removed
    if (searchName.startsWith("\"") && searchName.endsWith("\"")) {
      String unquotedName = searchName.substring(1, searchName.length() - 1);
      if (storedName.equals(unquotedName)) {
        return true;
      }
    }

    // Check for [Recurring] suffix
    if (storedName.contains(" [Recurring]")) {
      String nameWithoutSuffix = storedName.replace(" [Recurring]", "");
      if (nameWithoutSuffix.equals(searchName)) {
        return true;
      }
      // Also check with quotes for the name without suffix
      if (!searchName.startsWith("\"") && !searchName.endsWith("\"")) {
        if (nameWithoutSuffix.equals("\"" + searchName + "\"")) {
          return true;
        }
      }
      // Check with quotes removed from search name
      if (searchName.startsWith("\"") && searchName.endsWith("\"")) {
        String unquotedName = searchName.substring(1, searchName.length() - 1);
        if (nameWithoutSuffix.equals(unquotedName)) {
          return true;
        }
      }
    }

    return false;
  }
  /**
   * Helper method to convert a set of weekdays to a string representation.
   */
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
}