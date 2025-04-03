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

  private final Map<String, Calendar> calendars = new HashMap<>();
  private Calendar currentCalendar;

  /**
   * Creates a new calendar with the specified name and timezone.
   *
   * @param name     the unique name for the calendar
   * @param timezone the timezone for the calendar
   * @return true if the calendar was created successfully, false if a calendar with the same name
   *     already exists
   */
  public boolean createCalendar(String name, ZoneId timezone) {
    if (calendars.containsKey(name)) {
      return false;
    }
    calendars.put(name, new CalendarImpl(name, timezone));
    return true;
  }

  /**
   * Sets the current calendar to the specified calendar.
   *
   * @param calendarName the name of the calendar to use
   * @return true if the calendar exists and was set as current, false if the calendar doesn't exist
   */
  public boolean useCalendar(String calendarName) {
    if (!calendars.containsKey(calendarName)) {
      return false;
    }
    currentCalendar = calendars.get(calendarName);
    return true;
  }

  /**
   * Gets the currently active calendar.
   *
   * @return the current calendar, or null if no calendar is currently set
   */

  public Calendar getCurrentCalendar() {
    return currentCalendar;
  }

  /**
   * Checks if a calendar with the specified name exists.
   *
   * @param calendarName the name to check
   * @return true if a calendar with the specified name exists, false otherwise
   */

  public boolean calendarExists(String calendarName) {
    return calendars.containsKey(calendarName);
  }

  /**
   * Gets a calendar by name.
   *
   * @param calendarName the name of the calendar to retrieve
   * @return the calendar with the specified name, or null if no such calendar exists
   */
  public Calendar getCalendar(String calendarName) {
    return calendars.get(calendarName);
  }

  /**
   * Edits a property of the specified calendar. Supported properties are "name" and "timezone".
   *
   * @param calendarName the name of the calendar to edit
   * @param property     the property to edit ("name" or "timezone")
   * @param newValue     the new value for the property
   * @return true if the property was successfully updated, false if the calendar doesn't exist, the
   *     property is invalid, or the new value is invalid
   */

  public boolean editCalendar(String calendarName, String property, String newValue) {
    if (!calendars.containsKey(calendarName)) {
      return false;
    }
    Calendar calendar = calendars.get(calendarName);

    switch (property.toLowerCase()) {
      case "name":
        if (calendars.containsKey(newValue)) {
          return false;
        }
        calendars.remove(calendarName);
        ((CalendarImpl) calendar).setName(newValue);
        calendars.put(newValue, calendar);
        if (currentCalendar == calendar) {
          currentCalendar = calendar;
        }
        return true;

      case "timezone":
        try {
          ZoneId oldZone = calendar.getTimezone();
          ZoneId newZone = ZoneId.of(newValue);
          updateEventsForTimezoneChange((CalendarImpl) calendar, oldZone, newZone);
          ((CalendarImpl) calendar).setTimezone(newZone);
          return true;
        } catch (Exception e) {
          return false;
        }

      default:
        return false;
    }
  }

  private void updateEventsForTimezoneChange(CalendarImpl calendar, ZoneId oldZone,
      ZoneId newZone) {
    List<Event> originalEvents = new ArrayList<>(calendar.getAllEvents());
    List<Event> convertedEvents = new ArrayList<>();

    for (Event event : originalEvents) {
      EventImpl originalImpl = (EventImpl) event;
      if (event.isAllDay()) {
        convertedEvents.add(handleAllDayEvent(originalImpl));
      } else {
        convertedEvents.add(handleTimedEvent(originalImpl, oldZone, newZone));
      }
    }

    calendar.clearEvents();
    for (Event convertedEvent : convertedEvents) {
      calendar.addEvent(convertedEvent);
    }
  }

  private Event handleAllDayEvent(EventImpl event) {
    if (!event.isRecurring()) {
      return event;
    }

    RecurrencePattern pattern = event.getRecurrence();
    EventImpl newEvent = new EventImpl(
        event.getSubject(),
        event.getStartDateTime(),
        weekdaysToString(pattern.getWeekdays()),
        pattern.getOccurrences(),
        pattern.getUntilDate()
    );
    copyEventProperties(event, newEvent);
    return newEvent;
  }

  private Event handleTimedEvent(EventImpl event, ZoneId oldZone, ZoneId newZone) {
    LocalDateTime newStart = convertDateTime(event.getStartDateTime(), oldZone, newZone);
    LocalDateTime newEnd = event.getEndDateTime() != null ?
        convertDateTime(event.getEndDateTime(), oldZone, newZone) : null;

    if (!event.isRecurring()) {
      return createSimpleEvent(event, newStart, newEnd);
    }

    RecurrencePattern pattern = event.getRecurrence();
    LocalDateTime newUntil = pattern.getUntilDate() != null ?
        convertDateTime(pattern.getUntilDate(), oldZone, newZone) : null;

    EventImpl newEvent = new EventImpl(
        event.getSubject(),
        newStart,
        newEnd,
        weekdaysToString(pattern.getWeekdays()),
        pattern.getOccurrences(),
        newUntil
    );
    copyEventProperties(event, newEvent);
    return newEvent;
  }

  private LocalDateTime convertDateTime(LocalDateTime dt, ZoneId from, ZoneId to) {
    return dt.atZone(from).withZoneSameInstant(to).toLocalDateTime();
  }

  private Event createSimpleEvent(EventImpl source, LocalDateTime start, LocalDateTime end) {
    EventImpl newEvent = new EventImpl(source.getSubject(), start, end);
    copyEventProperties(source, newEvent);
    return newEvent;
  }

  private void copyEventProperties(EventImpl source, EventImpl target) {
    target.setDescription(source.getDescription());
    target.setLocation(source.getLocation());
    target.setPublic(source.isPublic());
  }

  /**
   * Copies a single event from the current calendar to a target calendar.
   *
   * @param eventName          the name of the event to copy
   * @param startDateTime      the start date and time of the event to copy
   * @param targetCalendarName the name of the calendar to copy the event to
   * @param targetDateTime     the new start date and time for the copied event
   * @return true if the event was successfully copied, false if the current calendar is not set,
   *     the target calendar doesn't exist, or the event is not found
   */

  public boolean copyEvent(String eventName, LocalDateTime startDateTime,
      String targetCalendarName, LocalDateTime targetDateTime) {
    if (currentCalendar == null || !calendars.containsKey(targetCalendarName)) {
      return false;
    }

    Event eventToCopy = findEventToCopy(eventName, startDateTime);
    if (eventToCopy == null) {
      return false;
    }

    Calendar targetCalendar = calendars.get(targetCalendarName);
    return eventToCopy.isAllDay() ?
        copyAllDayEvent((EventImpl) eventToCopy, targetCalendar, targetDateTime) :
        copyTimedEvent((EventImpl) eventToCopy, targetCalendar, targetDateTime);
  }

  private Event findEventToCopy(String eventName, LocalDateTime startDateTime) {

    for (Event event : currentCalendar.getAllEvents()) {
      if (event.isRecurring() && isEventNameMatch(event.getSubject(), eventName)) {

        RecurrencePattern pattern = ((EventImpl) event).getRecurrence();
        List<LocalDateTime> occurrences = pattern.calculateRecurrences(event.getStartDateTime());

        for (LocalDateTime occurrence : occurrences) {
          if (occurrence.toLocalDate().equals(startDateTime.toLocalDate()) &&
              occurrence.getHour() == startDateTime.getHour() &&
              occurrence.getMinute() == startDateTime.getMinute()) {
            return event;
          }
        }
      }
    }

    List<Event> events = currentCalendar.getEventsOn(startDateTime);
    for (Event event : events) {
      if (isEventNameMatch(event.getSubject(), eventName) &&
          event.getStartDateTime().toLocalDate().equals(startDateTime.toLocalDate()) &&
          event.getStartDateTime().getHour() == startDateTime.getHour() &&
          event.getStartDateTime().getMinute() == startDateTime.getMinute()) {
        return event;
      }
    }

    return null;
  }

  private boolean copyAllDayEvent(EventImpl event, Calendar target, LocalDateTime targetStart) {
    LocalDateTime newStart = targetStart.toLocalDate().atStartOfDay();

    if (!event.isRecurring()) {
      return target.createAllDayEvent(
          event.getSubject(),
          newStart,
          true,
          event.getDescription(),
          event.getLocation(),
          event.isPublic()
      );
    }

    RecurrencePattern pattern = event.getRecurrence();
    LocalDateTime newUntilDate = null;
    if (pattern.getUntilDate() != null) {
      long daysBetween = ChronoUnit.DAYS.between(
          event.getStartDateTime().toLocalDate(),
          pattern.getUntilDate().toLocalDate()
      );
      newUntilDate = targetStart.toLocalDate().plusDays(daysBetween).atStartOfDay();
    }

    return target.createRecurringAllDayEvent(
        event.getSubject(),
        newStart,
        weekdaysToString(pattern.getWeekdays()),
        pattern.getOccurrences(),
        newUntilDate,
        true,
        event.getDescription(),
        event.getLocation(),
        event.isPublic()
    );
  }

  private boolean copyTimedEvent(EventImpl event, Calendar target, LocalDateTime targetStart) {
    LocalDateTime newEndTime = null;
    if (event.getEndDateTime() != null) {
      long durationMinutes = ChronoUnit.MINUTES.between(
          event.getStartDateTime(),
          event.getEndDateTime()
      );
      newEndTime = targetStart.plusMinutes(durationMinutes);
    }

    if (!event.isRecurring()) {
      return target.createEvent(
          event.getSubject(),
          targetStart,
          newEndTime,
          true,
          event.getDescription(),
          event.getLocation(),
          event.isPublic()
      );
    }

    RecurrencePattern pattern = event.getRecurrence();
    LocalDateTime newUntilDate = null;
    if (pattern.getUntilDate() != null) {
      long daysBetween = ChronoUnit.DAYS.between(
          event.getStartDateTime().toLocalDate(),
          pattern.getUntilDate().toLocalDate()
      );
      newUntilDate = targetStart.toLocalDate().plusDays(daysBetween)
          .atTime(targetStart.toLocalTime());
    }

    return target.createRecurringEvent(
        event.getSubject(),
        targetStart,
        newEndTime,
        weekdaysToString(pattern.getWeekdays()),
        pattern.getOccurrences(),
        newUntilDate,
        true,
        event.getDescription(),
        event.getLocation(),
        event.isPublic()
    );
  }

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

  private boolean isEventNameMatch(String storedName, String searchName) {

    if (storedName.equals(searchName)) {
      return true;
    }

    if (!searchName.startsWith("\"") && !searchName.endsWith("\"")) {
      if (storedName.equals("\"" + searchName + "\"")) {
        return true;
      }
    }

    if (searchName.startsWith("\"") && searchName.endsWith("\"")) {
      String unquotedName = searchName.substring(1, searchName.length() - 1);
      if (storedName.equals(unquotedName)) {
        return true;
      }
    }

    return false;
  }


  /**
   * Copies all events from a specific day in the current calendar to a target calendar. Event times
   * are adjusted according to the timezone differences between calendars.
   *
   * @param sourceDate         the date containing events to copy
   * @param targetCalendarName the name of the calendar to copy events to
   * @param targetDate         the target date for the copied events
   * @return true if at least one event was copied successfully, false if the current calendar is
   *     not set, the target calendar doesn't exist, or no events exist on the source date
   */
  public boolean copyEventsOnDay(LocalDateTime sourceDate, String targetCalendarName,
      LocalDateTime targetDate) {
    if (currentCalendar == null || !calendars.containsKey(targetCalendarName)) {
      return false;
    }

    Calendar sourceCalendar = currentCalendar;
    Calendar targetCalendar = calendars.get(targetCalendarName);

    List<Event> events = sourceCalendar.getEventsOn(sourceDate);

    if (events.isEmpty()) {
      return false;
    }

    boolean atLeastOneCopied = false;

    for (Event event : events) {
      boolean success = false;

      if (event.isAllDay()) {

        LocalDateTime newStartDateTime = targetDate.toLocalDate().atStartOfDay();
        success = copyEvent(event.getSubject(), event.getStartDateTime(), targetCalendarName,
            newStartDateTime);
      } else {

        ZonedDateTime sourceZDT = event.getStartDateTime().atZone(sourceCalendar.getTimezone());
        ZonedDateTime targetZDT = sourceZDT.withZoneSameInstant(targetCalendar.getTimezone());

        LocalDateTime newStartDateTime;

        if (targetZDT.getHour() < sourceZDT.getHour() &&
            targetZDT.toLocalDate().isAfter(sourceZDT.toLocalDate())) {

          newStartDateTime = targetDate.toLocalDate().plusDays(1)
              .atTime(targetZDT.getHour(), targetZDT.getMinute());
        } else if (targetZDT.getHour() > sourceZDT.getHour() &&
            targetZDT.toLocalDate().isBefore(sourceZDT.toLocalDate())) {

          newStartDateTime = targetDate.toLocalDate().minusDays(1)
              .atTime(targetZDT.getHour(), targetZDT.getMinute());
        } else {

          newStartDateTime = targetDate.toLocalDate()
              .atTime(targetZDT.getHour(), targetZDT.getMinute());
        }

        success = copyEvent(event.getSubject(), event.getStartDateTime(), targetCalendarName,
            newStartDateTime);
      }

      if (success) {
        atLeastOneCopied = true;
      }
    }

    return atLeastOneCopied;
  }

  /**
   * Copies all events within a date range from the current calendar to a target calendar. Preserves
   * the relative positioning of events while adjusting for timezone differences.
   *
   * @param startDate          the start date of the range to copy from (inclusive)
   * @param endDate            the end date of the range to copy from (inclusive)
   * @param targetCalendarName the name of the calendar to copy events to
   * @param targetStartDate    the target start date for the copied events
   * @return true if at least one event was copied successfully, false if the current calendar is
   *     not set, the target calendar doesn't exist, or no events exist in the source range
   */

  public boolean copyEventsInRange(LocalDateTime startDate, LocalDateTime endDate,
      String targetCalendarName, LocalDateTime targetStartDate) {
    if (currentCalendar == null || !calendars.containsKey(targetCalendarName)) {
      return false;
    }

    Calendar sourceCalendar = currentCalendar;
    Calendar targetCalendar = calendars.get(targetCalendarName);

    List<Event> events = sourceCalendar.getEventsFrom(startDate, endDate);

    if (events.isEmpty()) {
      return false;
    }

    boolean atLeastOneCopied = false;

    long baseDayOffset = ChronoUnit.DAYS.between(
        startDate.toLocalDate(),
        targetStartDate.toLocalDate()
    );

    for (Event event : events) {

      long eventDayOffset = ChronoUnit.DAYS.between(
          startDate.toLocalDate(),
          event.getStartDateTime().toLocalDate()
      );

      LocalDate targetEventDate = targetStartDate.toLocalDate().plusDays(eventDayOffset);
      boolean success = false;

      if (event.isAllDay()) {

        LocalDateTime newStartDateTime = targetEventDate.atStartOfDay();
        success = copyEvent(event.getSubject(), event.getStartDateTime(), targetCalendarName,
            newStartDateTime);
      } else {

        ZonedDateTime sourceZDT = event.getStartDateTime().atZone(sourceCalendar.getTimezone());
        ZonedDateTime targetZDT = sourceZDT.withZoneSameInstant(targetCalendar.getTimezone());

        LocalDateTime newStartDateTime;

        if (targetZDT.getHour() < sourceZDT.getHour() &&
            targetZDT.toLocalDate().isAfter(sourceZDT.toLocalDate())) {

          newStartDateTime = targetEventDate.plusDays(1)
              .atTime(targetZDT.getHour(), targetZDT.getMinute());
        } else if (targetZDT.getHour() > sourceZDT.getHour() &&
            targetZDT.toLocalDate().isBefore(sourceZDT.toLocalDate())) {

          newStartDateTime = targetEventDate.minusDays(1)
              .atTime(targetZDT.getHour(), targetZDT.getMinute());
        } else {

          newStartDateTime = targetEventDate
              .atTime(targetZDT.getHour(), targetZDT.getMinute());
        }

        success = copyEvent(event.getSubject(), event.getStartDateTime(), targetCalendarName,
            newStartDateTime);
      }

      if (success) {
        atLeastOneCopied = true;
      }
    }

    return atLeastOneCopied;
  }

  /**
   * Gets a list of all calendar names.
   *
   * @return a list of calendar names
   */
  public java.util.List<String> getCalendarNames() {
    return new java.util.ArrayList<>(calendars.keySet());
  }
}