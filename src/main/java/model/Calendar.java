package model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Interface representing core calendar functionality.
 */
public interface Calendar {
  /**
   * Gets the name of the calendar.
   * @return the calendar name
   */
  String getName();

  /**
   * Gets the timezone of the calendar.
   * @return the calendar timezone
   */
  ZoneId getTimezone();

  /**
   * Converts a local date-time to a zoned date-time using this calendar's timezone.
   * @param localDateTime the local date-time
   * @return the zoned date-time
   */
  ZonedDateTime toZonedDateTime(LocalDateTime localDateTime);

  /**
   * Converts a zoned date-time to a local date-time in this calendar's timezone.
   * @param zonedDateTime the zoned date-time
   * @return the local date-time
   */
  LocalDateTime toLocalDateTime(ZonedDateTime zonedDateTime);

  /**
   * Creates a single event in the calendar.
   * @param eventName the name of the event
   * @param startDateTime the start date and time
   * @param endDateTime the end date and time
   * @param autoDecline whether to automatically decline if conflicts exist (always treated as true)
   * @param description optional description for the event
   * @param location optional location for the event
   * @param isPublic whether the event is public (true) or private (false)
   * @return true if the event was created successfully, false otherwise
   */
  boolean createEvent(String eventName, LocalDateTime startDateTime,
      LocalDateTime endDateTime, boolean autoDecline, String description,
      String location, boolean isPublic);

  /**
   * Creates a single all-day event.
   * @param eventName the name of the event
   * @param dateTime the date of the all-day event
   * @param autoDecline whether to automatically decline if conflicts exist (always treated as true)
   * @param description optional description for the event
   * @param location optional location for the event
   * @param isPublic whether the event is public (true) or private (false)
   * @return true if the event was created successfully, false otherwise
   */
  boolean createAllDayEvent(String eventName, LocalDateTime dateTime,
      boolean autoDecline, String description, String location, boolean isPublic);

  /**
   * Creates a recurring event in the calendar.
   * @param eventName the name of the event
   * @param startDateTime the start date and time
   * @param endDateTime the end date and time
   * @param weekdays the days of week the event repeats on (e.g., "MRU")
   * @param occurrences the number of occurrences (used if untilDate is null)
   * @param untilDate the end date for recurrence (used if occurrences is -1)
   * @param autoDecline whether to automatically decline if conflicts exist (always treated as true)
   * @param description optional description for the event
   * @param location optional location for the event
   * @param isPublic whether the event is public (true) or private (false)
   * @return true if the event was created successfully, false otherwise
   */
  boolean createRecurringEvent(String eventName, LocalDateTime startDateTime,
      LocalDateTime endDateTime, String weekdays,
      int occurrences, LocalDateTime untilDate,
      boolean autoDecline, String description, String location, boolean isPublic);

  /**
   * Creates a recurring all-day event.
   * @param eventName the name of the event
   * @param dateTime the date of the all-day event
   * @param weekdays the days of week the event repeats on (e.g., "MRU")
   * @param occurrences the number of occurrences (used if untilDate is null)
   * @param untilDate the end date for recurrence (used if occurrences is -1)
   * @param autoDecline whether to automatically decline if conflicts exist (always treated as true)
   * @param description optional description for the event
   * @param location optional location for the event
   * @param isPublic whether the event is public (true) or private (false)
   * @return true if the event was created successfully, false otherwise
   */
  boolean createRecurringAllDayEvent(String eventName, LocalDateTime dateTime,
      String weekdays, int occurrences,
      LocalDateTime untilDate, boolean autoDecline, String description,
      String location, boolean isPublic);

  /**
   * Edits a specific property of a single event.
   * @param property the property to edit
   * @param eventName the name of the event
   * @param startDateTime the start date and time of the event
   * @param endDateTime the end date and time of the event
   * @param newValue the new value for the property
   * @return true if the event was edited successfully, false otherwise
   */
  boolean editEvent(String property, String eventName,
      LocalDateTime startDateTime, LocalDateTime endDateTime,
      String newValue);

  /**
   * Edits a specific property of all events in a series starting from a specific date/time.
   * @param property the property to edit
   * @param eventName the name of the event
   * @param startDateTime the start date and time to begin edits from
   * @param newValue the new value for the property
   * @return true if the events were edited successfully, false otherwise
   */
  boolean editEventsFrom(String property, String eventName,
      LocalDateTime startDateTime, String newValue);

  /**
   * Edits a specific property of all events with the same name.
   * @param property the property to edit
   * @param eventName the name of the events to edit
   * @param newValue the new value for the property
   * @return true if the events were edited successfully, false otherwise
   */
  boolean editAllEvents(String property, String eventName, String newValue);

  /**
   * Gets all events on a specific date.
   * @param dateTime the date to query
   * @return a list of events on the given date
   */
  List<Event> getEventsOn(LocalDateTime dateTime);

  /**
   * Gets all events within a date range.
   * @param startDateTime the start of the range
   * @param endDateTime the end of the range
   * @return a list of events within the given range
   */
  List<Event> getEventsFrom(LocalDateTime startDateTime, LocalDateTime endDateTime);

  /**
   * Checks if the user is busy at a specific time.
   * @param dateTime the date and time to check
   * @return true if the user has events at the given time, false otherwise
   */
  boolean isBusy(LocalDateTime dateTime);

  /**
   * Gets all the events from the event List.
   * @return the list of all the events.
   */
  List<Event> getAllEvents();
}