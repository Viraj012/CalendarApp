package model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the Calendar interface.
 */
public class CalendarImpl implements Calendar {
  private List<Event> events;

  /**
   * Creates a new calendar.
   */
  public CalendarImpl() {
    this.events = new ArrayList<>();
  }

  @Override
  public boolean createEvent(String eventName, LocalDateTime startDateTime,
      LocalDateTime endDateTime, boolean autoDecline, String description,
      String location, boolean isPublic) {

    // Validate inputs
    if (!validateEventInputs(eventName, startDateTime, endDateTime)) {
      return false;
    }

    // Check for conflicts
    if (autoDecline && hasConflicts(startDateTime, endDateTime, false)) {
      return false;
    }

    Event newEvent = createEventObject(eventName, startDateTime, endDateTime,
        description, location, isPublic, false, null, -1, null);

    events.add(newEvent);
    return true;
  }

  @Override
  public boolean createAllDayEvent(String eventName, LocalDateTime dateTime, boolean autoDecline,
      String description, String location, boolean isPublic) {

    // Validate inputs
    if (!validateEventInputs(eventName, dateTime, null)) {
      return false;
    }

    // Check for conflicts
    if (autoDecline && hasConflicts(dateTime, null, true)) {
      return false;
    }

    Event newEvent = createEventObject(eventName, dateTime, null,
        description, location, isPublic, true, null, -1, null);

    events.add(newEvent);
    return true;
  }

  @Override
  public boolean createRecurringEvent(String eventName, LocalDateTime startDateTime,
      LocalDateTime endDateTime, String weekdays,
      int occurrences, LocalDateTime untilDate,
      boolean autoDecline, String description, String location, boolean isPublic) {

    // Validate inputs
    if (!validateEventInputs(eventName, startDateTime, endDateTime)) {
      return false;
    }

    // Ensure event doesn't span multiple days
    if (!startDateTime.toLocalDate().equals(endDateTime.toLocalDate())) {
      return false;
    }

    // Create a temporary event to calculate recurrences
    Event tempEvent = createEventObject(eventName, startDateTime, endDateTime,
        description, location, isPublic, false, weekdays, occurrences, untilDate);

    // Check for conflicts on all recurrence dates
    if (autoDecline) {
      RecurrencePattern pattern = ((EventImpl)tempEvent).getRecurrence();
      List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(startDateTime);

      for (LocalDateTime date : recurrenceDates) {
        LocalDateTime start = date;
        LocalDateTime end = calculateRecurringEndTime(date, startDateTime, endDateTime);

        if (hasConflicts(start, end, false)) {
          return false;
        }
      }
    }

    events.add(tempEvent);
    return true;
  }

  @Override
  public boolean createRecurringAllDayEvent(String eventName, LocalDateTime dateTime,
      String weekdays, int occurrences,
      LocalDateTime untilDate, boolean autoDecline, String description,
      String location, boolean isPublic) {

    // Validate inputs
    if (!validateEventInputs(eventName, dateTime, null)) {
      return false;
    }

    // Create a temporary event to calculate recurrences
    Event tempEvent = createEventObject(eventName, dateTime, null,
        description, location, isPublic, true, weekdays, occurrences, untilDate);

    // Check for conflicts on all recurrence dates
    if (autoDecline) {
      RecurrencePattern pattern = ((EventImpl)tempEvent).getRecurrence();
      List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(dateTime);

      for (LocalDateTime date : recurrenceDates) {
        if (hasConflicts(date, null, true)) {
          return false;
        }
      }
    }

    events.add(tempEvent);
    return true;
  }

  @Override
  public boolean editEvent(String property, String eventName,
      LocalDateTime startDateTime, LocalDateTime endDateTime,
      String newValue) {

    // Handle quoted and unquoted event names
    String unquotedEventName = removeQuotes(eventName);

    for (Event event : events) {
      if (isMatchingEvent(event, eventName, unquotedEventName, startDateTime, endDateTime)) {
        return updateEventProperty((EventImpl) event, property, newValue);
      }
    }

    return false;
  }

  @Override
  public boolean editEventsFrom(String property, String eventName,
      LocalDateTime startDateTime, String newValue) {
    boolean modified = false;
    List<Event> eventsToAdd = new ArrayList<>();
    List<Event> eventsToRemove = new ArrayList<>();

    for (Event event : events) {
      if (event.getSubject().equals(eventName) && event.isRecurring()) {
        EventImpl originalEvent = (EventImpl) event;
        RecurrencePattern originalPattern = originalEvent.getRecurrence();

        // Calculate the recurrence dates
        List<LocalDateTime> recurrenceDates = originalPattern.calculateRecurrences(originalEvent.getStartDateTime());

        // Check if any occurrences fall after startDateTime
        boolean hasOccurrencesAfter = recurrenceDates.stream()
            .anyMatch(date -> !date.isBefore(startDateTime));

        if (hasOccurrencesAfter) {
          // Create a shortened version of the original event that ends just before startDateTime
          EventImpl shortenedOriginal;

          if (originalEvent.isAllDay()) {
            shortenedOriginal = new EventImpl(
                originalEvent.getSubject(),
                originalEvent.getStartDateTime(),
                weekdaysToString(originalPattern.getWeekdays()),
                -1,  // Use untilDate instead of occurrences
                startDateTime.minusDays(1)
            );
          } else {
            shortenedOriginal = new EventImpl(
                originalEvent.getSubject(),
                originalEvent.getStartDateTime(),
                originalEvent.getEndDateTime(),
                weekdaysToString(originalPattern.getWeekdays()),
                -1,
                startDateTime.minusDays(1)
            );
          }

          // Copy other properties
          shortenedOriginal.setDescription(originalEvent.getDescription());
          shortenedOriginal.setLocation(originalEvent.getLocation());
          shortenedOriginal.setPublic(originalEvent.isPublic());

          // Create a modified version starting from startDateTime
          EventImpl modifiedEvent;

          if (originalEvent.isAllDay()) {
            modifiedEvent = new EventImpl(
                originalEvent.getSubject(),
                startDateTime,
                weekdaysToString(originalPattern.getWeekdays()),
                originalPattern.getOccurrences(),
                originalPattern.getUntilDate()
            );
          } else {
            // For non-all-day events, calculate the equivalent end time
            LocalDateTime newEndTime = calculateEquivalentEndTime(
                originalEvent.getStartDateTime(),
                originalEvent.getEndDateTime(),
                startDateTime
            );

            modifiedEvent = new EventImpl(
                originalEvent.getSubject(),
                startDateTime,
                newEndTime,
                weekdaysToString(originalPattern.getWeekdays()),
                originalPattern.getOccurrences(),
                originalPattern.getUntilDate()
            );
          }

          // Copy other properties
          modifiedEvent.setDescription(originalEvent.getDescription());
          modifiedEvent.setLocation(originalEvent.getLocation());
          modifiedEvent.setPublic(originalEvent.isPublic());

          if (updateEventProperty(modifiedEvent, property, newValue)) {
            // Mark the original for removal and the new events for addition
            eventsToRemove.add(originalEvent);
            eventsToAdd.add(shortenedOriginal);
            eventsToAdd.add(modifiedEvent);
            modified = true;
          } else {
          }
        }
      } else if (event.getSubject().equals(eventName) && !event.isRecurring()) {
        // For non-recurring events, check if they start after startDateTime
        if (!event.getStartDateTime().isBefore(startDateTime)) {
          if (updateEventProperty((EventImpl) event, property, newValue)) {
            modified = true;
          } else {
            return false; // Invalid property
          }
        }
      }
    }

    // Apply the changes
    events.removeAll(eventsToRemove);
    events.addAll(eventsToAdd);

    return modified;
  }

  // Helper method to convert Set<DayOfWeek> to weekday string format
  private String weekdaysToString(Set<DayOfWeek> weekdays) {
    StringBuilder sb = new StringBuilder();

    for (DayOfWeek day : weekdays) {
      switch (day) {
        case MONDAY: sb.append("M"); break;
        case TUESDAY: sb.append("T"); break;
        case WEDNESDAY: sb.append("W"); break;
        case THURSDAY: sb.append("R"); break;
        case FRIDAY: sb.append("F"); break;
        case SATURDAY: sb.append("S"); break;
        case SUNDAY: sb.append("U"); break;
      }
    }

    return sb.toString();
  }

  // Helper method to calculate the equivalent end time for a new start time
  private LocalDateTime calculateEquivalentEndTime(
      LocalDateTime originalStart,
      LocalDateTime originalEnd,
      LocalDateTime newStart) {

    // Calculate the duration of the original event
    long hoursDifference = originalEnd.getHour() - originalStart.getHour();
    long minutesDifference = originalEnd.getMinute() - originalStart.getMinute();

    // Apply the same duration to the new start time
    return newStart.plusHours(hoursDifference).plusMinutes(minutesDifference);
  }
  @Override
  public boolean editAllEvents(String property, String eventName, String newValue) {
    boolean modified = false;

    for (Event event : events) {
      if (event.getSubject().equals(eventName)) {
        if (updateEventProperty((EventImpl) event, property, newValue)) {
          modified = true;
        } else {
          return false; // Invalid property
        }
      }
    }

    return modified;
  }

  @Override
  public List<Event> getEventsOn(LocalDateTime dateTime) {
    return events.stream()
        .filter(event -> isEventOnDate(event, dateTime.toLocalDate()))
        .collect(Collectors.toList());
  }

  @Override
  public List<Event> getEventsFrom(LocalDateTime startDateTime, LocalDateTime endDateTime) {
    List<Event> result = new ArrayList<>();

    for (Event event : events) {
      // For regular events, add them if they're in the date range
      if (!event.isRecurring() && isEventInDateRange(event, startDateTime, endDateTime)) {
        result.add(event);
      }
      // For recurring events, add them only once if any occurrence is in range
      else if (event.isRecurring()) {
        RecurrencePattern pattern = ((EventImpl)event).getRecurrence();
        List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(event.getStartDateTime());

        // Check if any recurrence falls within the date range
        boolean hasOccurrenceInRange = recurrenceDates.stream()
            .anyMatch(date -> !date.isBefore(startDateTime) && !date.isAfter(endDateTime));

        if (hasOccurrenceInRange) {
          result.add(event);
        }
      }
    }

    return result;
  }
  @Override
  public boolean isBusy(LocalDateTime dateTime) {
    return events.stream()
        .anyMatch(event -> isEventActiveAt(event, dateTime));
  }

  @Override
  public String exportToCSV(String fileName) {
    File file = new File(fileName);

    try (FileWriter writer = new FileWriter(file)) {
      // Write CSV header
      writer.write("Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description,Location,Private\n");

      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
      DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

      // Export each event
      for (Event event : events) {
        if (event.isRecurring()) {
          // For recurring events, create individual entries for each occurrence
          RecurrencePattern pattern = ((EventImpl)event).getRecurrence();
          List<LocalDateTime> occurrences = pattern.calculateRecurrences(event.getStartDateTime());

          for (LocalDateTime occurrence : occurrences) {
            // Calculate the equivalent end time for this occurrence
            LocalDateTime occurrenceEndTime = null;
            if (!event.isAllDay() && event.getEndDateTime() != null) {
              long hoursDifference = event.getEndDateTime().getHour() - event.getStartDateTime().getHour();
              long minutesDifference = event.getEndDateTime().getMinute() - event.getStartDateTime().getMinute();
              occurrenceEndTime = occurrence.plusHours(hoursDifference).plusMinutes(minutesDifference);
            }

            writeSingleEventToCSV(writer, event, occurrence, occurrenceEndTime, dateFormatter, timeFormatter);
          }
        } else {
          // For non-recurring events, just write a single entry
          writeSingleEventToCSV(writer, event, event.getStartDateTime(), event.getEndDateTime(), dateFormatter, timeFormatter);
        }
      }

      return file.getAbsolutePath();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Helper method to write a single event entry to the CSV file.
   */
  private void writeSingleEventToCSV(FileWriter writer, Event event, LocalDateTime startDateTime,
      LocalDateTime endDateTime, DateTimeFormatter dateFormatter, DateTimeFormatter timeFormatter)
      throws IOException {

    StringBuilder line = new StringBuilder();

    // Subject
    line.append(escapeCSV(event.getSubject())).append(",");

    // Start Date
    line.append(startDateTime.format(dateFormatter)).append(",");

    // Start Time (empty for all-day events)
    if (event.isAllDay()) {
      line.append(",");
    } else {
      line.append(startDateTime.format(timeFormatter)).append(",");
    }

    // End Date
    if (endDateTime == null) {
      line.append(startDateTime.format(dateFormatter)).append(",");
    } else {
      line.append(endDateTime.format(dateFormatter)).append(",");
    }

    // End Time (empty for all-day events)
    if (event.isAllDay() || endDateTime == null) {
      line.append(",");
    } else {
      line.append(endDateTime.format(timeFormatter)).append(",");
    }

    // All-day flag
    line.append(event.isAllDay() ? "True" : "False").append(",");

    // Description
    line.append(escapeCSV(event.getDescription())).append(",");

    // Location
    line.append(escapeCSV(event.getLocation())).append(",");

    // Private flag
    line.append(!event.isPublic() ? "True" : "False");

    line.append("\n");
    writer.write(line.toString());
  }

  /**
   * Helper method to escape CSV fields.
   */
  private String escapeCSV(String field) {
    if (field == null) {
      return "";
    }

    if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
      return "\"" + field.replace("\"", "\"\"") + "\"";
    }

    return field;
  }

  /**
   * Validates basic event inputs.
   *
   * @param eventName the event name
   * @param startDateTime the start date/time
   * @param endDateTime the end date/time (can be null for all-day events)
   * @return true if inputs are valid
   */
  private boolean validateEventInputs(String eventName, LocalDateTime startDateTime, LocalDateTime endDateTime) {
    if (eventName == null || eventName.trim().isEmpty()) {
      return false;
    }

    if (startDateTime == null) {
      return false;
    }

    if (endDateTime != null && (endDateTime.isBefore(startDateTime) || endDateTime.equals(startDateTime))) {
      return false;
    }

    return true;
  }

  /**
   * Creates a new Event object.
   */
  private Event createEventObject(String eventName, LocalDateTime startDateTime, LocalDateTime endDateTime,
      String description, String location, boolean isPublic, boolean isAllDay,
      String weekdays, int occurrences, LocalDateTime untilDate) {

    Event newEvent;

    if (isAllDay) {
      if (weekdays != null) {
        // Recurring all-day event
        newEvent = new EventImpl(eventName, startDateTime, weekdays, occurrences, untilDate);
      } else {
        // Single all-day event
        newEvent = new EventImpl(eventName, startDateTime);
      }
    } else {
      if (weekdays != null) {
        // Recurring regular event
        newEvent = new EventImpl(eventName, startDateTime, endDateTime, weekdays, occurrences, untilDate);
      } else {
        // Single regular event
        newEvent = new EventImpl(eventName, startDateTime, endDateTime);
      }
    }

    // Set additional properties
    EventImpl eventImpl = (EventImpl) newEvent;

    if (description != null && !description.isEmpty()) {
      eventImpl.setDescription(description);
    }

    if (location != null && !location.isEmpty()) {
      eventImpl.setLocation(location);
    }

    eventImpl.setPublic(isPublic);

    return newEvent;
  }

  /**
   * Updates a property on an event.
   *
   * @param eventImpl the event to update
   * @param property the property name
   * @param newValue the new value
   * @return true if successful
   */
  private boolean updateEventProperty(EventImpl eventImpl, String property, String newValue) {
    switch (property.toLowerCase()) {
      case "name":
      case "subject":
        eventImpl.setSubject(newValue);
        return true;
      case "description":
        eventImpl.setDescription(newValue);
        return true;
      case "location":
        eventImpl.setLocation(newValue);
        return true;
      case "public":
        eventImpl.setPublic(Boolean.parseBoolean(newValue));
        return true;
      default:
        return false;
    }
  }

  /**
   * Check if an event matches the specified criteria.
   */
  private boolean isMatchingEvent(Event event, String eventName, String unquotedEventName,
      LocalDateTime startDateTime, LocalDateTime endDateTime) {

    String storedSubject = event.getSubject();
    boolean nameMatches = storedSubject.equals(eventName) || storedSubject.equals(unquotedEventName);

    boolean timeMatches = event.getStartDateTime().equals(startDateTime) &&
        ((event.getEndDateTime() == null && endDateTime == null) ||
            (event.getEndDateTime() != null && event.getEndDateTime().equals(endDateTime)));

    return nameMatches && timeMatches;
  }

  /**
   * Removes quotes from a string if present.
   */
  private String removeQuotes(String text) {
    if (text.startsWith("\"") && text.endsWith("\"")) {
      return text.substring(1, text.length() - 1);
    }
    return text;
  }

  /**
   * Check if the event is active at the specified date and time.
   */
  private boolean isEventActiveAt(Event event, LocalDateTime dateTime) {
    if (event.isAllDay()) {
      return dateTime.toLocalDate().equals(event.getStartDateTime().toLocalDate());
    }

    return !dateTime.isBefore(event.getStartDateTime()) &&
        !dateTime.isAfter(event.getEndDateTime());
  }

  /**
   * Check if the event occurs on the specified date.
   */
  private boolean isEventOnDate(Event event, java.time.LocalDate date) {
    if (event.isRecurring()) {
      List<LocalDateTime> recurrenceDates = getRecurrenceDates(event);
      return recurrenceDates.stream()
          .anyMatch(dt -> dt.toLocalDate().equals(date));
    }

    return event.getStartDateTime().toLocalDate().equals(date) ||
        (event.getEndDateTime() != null &&
            event.getEndDateTime().toLocalDate().equals(date));
  }

  /**
   * Check if the event occurs within the specified date range.
   */
  private boolean isEventInDateRange(Event event, LocalDateTime startDateTime, LocalDateTime endDateTime) {
    if (event.isRecurring()) {
      List<LocalDateTime> recurrenceDates = getRecurrenceDates(event);
      return recurrenceDates.stream()
          .anyMatch(dt -> !dt.isBefore(startDateTime) && !dt.isAfter(endDateTime));
    }

    return !event.getStartDateTime().isAfter(endDateTime) &&
        (event.getEndDateTime() == null || !event.getEndDateTime().isBefore(startDateTime));
  }

  /**
   * Check if an event starts after a given date.
   */
  private boolean isEventAfterDate(Event event, LocalDateTime startDateTime) {
    if (!event.isRecurring()) {
      return !event.getStartDateTime().isBefore(startDateTime);
    } else {
      List<LocalDateTime> recurrenceDates = getRecurrenceDates(event);
      return recurrenceDates.stream()
          .anyMatch(dt -> !dt.isBefore(startDateTime));
    }
  }

  /**
   * Get recurrence dates for an event.
   */
  private List<LocalDateTime> getRecurrenceDates(Event event) {
    RecurrencePattern pattern = ((EventImpl)event).getRecurrence();
    return pattern.calculateRecurrences(event.getStartDateTime());
  }

  /**
   * Calculate the end time for a recurring event occurrence.
   */
  private LocalDateTime calculateRecurringEndTime(LocalDateTime date,
      LocalDateTime originalStart, LocalDateTime originalEnd) {

    return date.plusHours(originalEnd.getHour() - originalStart.getHour())
        .plusMinutes(originalEnd.getMinute() - originalStart.getMinute());
  }

  /**
   * Check if there are any conflicts with existing events in the given time range.
   *
   * @param start the start time
   * @param end the end time (null for all-day events)
   * @param isAllDay whether this is an all-day event
   * @return true if there are conflicts
   */
  private boolean hasConflicts(LocalDateTime start, LocalDateTime end, boolean isAllDay) {
    return events.stream()
        .anyMatch(event -> {
          if (isAllDay || event.isAllDay()) {
            // All-day events conflict with any event on the same day
            return start.toLocalDate().equals(event.getStartDateTime().toLocalDate());
          }

          // Regular events conflict if their time ranges overlap
          return !(end.isBefore(event.getStartDateTime()) ||
              start.isAfter(event.getEndDateTime()));
        });
  }
}