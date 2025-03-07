package model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
      LocalDateTime endDateTime, boolean autoDecline) {
    // Check for conflicts if autoDecline is true
    if (autoDecline && hasConflicts(startDateTime, endDateTime)) {
      return false;
    }

    // Create and add the event
    Event newEvent = new EventImpl(eventName, startDateTime, endDateTime);
    events.add(newEvent);
    return true;
  }

  @Override
  public boolean createAllDayEvent(String eventName, LocalDateTime dateTime, boolean autoDecline) {
    // Check for conflicts if autoDecline is true
    if (autoDecline && hasAllDayConflicts(dateTime)) {
      return false;
    }

    // Create and add the event
    Event newEvent = new EventImpl(eventName, dateTime);
    events.add(newEvent);
    return true;
  }

  @Override
  public boolean createRecurringEvent(String eventName, LocalDateTime startDateTime,
      LocalDateTime endDateTime, String weekdays,
      int occurrences, LocalDateTime untilDate,
      boolean autoDecline) {
    // Validate: recurring events must start and end on the same day
    if (!startDateTime.toLocalDate().equals(endDateTime.toLocalDate())) {
      return false;
    }

    // Create a temporary event to check conflicts
    Event tempEvent = new EventImpl(eventName, startDateTime, endDateTime,
        weekdays, occurrences, untilDate);

    // Check for conflicts with existing events if autoDecline is true
    if (autoDecline) {
      RecurrencePattern pattern = ((EventImpl)tempEvent).getRecurrence();
      List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(startDateTime);

      for (LocalDateTime date : recurrenceDates) {
        LocalDateTime start = date;
        LocalDateTime end = date.plusHours(endDateTime.getHour() - startDateTime.getHour())
            .plusMinutes(endDateTime.getMinute() - startDateTime.getMinute());

        if (hasConflicts(start, end)) {
          return false;
        }
      }
    }

    // Add the recurring event
    events.add(tempEvent);
    return true;
  }

  @Override
  public boolean createRecurringAllDayEvent(String eventName, LocalDateTime dateTime,
      String weekdays, int occurrences,
      LocalDateTime untilDate, boolean autoDecline) {
    // Create a temporary event to check conflicts
    Event tempEvent = new EventImpl(eventName, dateTime, weekdays, occurrences, untilDate);

    // Check for conflicts with existing events if autoDecline is true
    if (autoDecline) {
      RecurrencePattern pattern = ((EventImpl)tempEvent).getRecurrence();
      List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(dateTime);

      for (LocalDateTime date : recurrenceDates) {
        if (hasAllDayConflicts(date)) {
          return false;
        }
      }
    }

    // Add the recurring all-day event
    events.add(tempEvent);
    return true;
  }

  @Override
  public boolean editEvent(String property, String eventName,
      LocalDateTime startDateTime, LocalDateTime endDateTime,
      String newValue) {
    // Find the specific event
    for (Event event : events) {
      if (event.getSubject().equals(eventName) &&
          event.getStartDateTime().equals(startDateTime) &&
          ((event.getEndDateTime() == null && endDateTime == null) ||
              (event.getEndDateTime() != null && event.getEndDateTime().equals(endDateTime)))) {

        // Update the property
        EventImpl eventImpl = (EventImpl) event;
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
            return false; // Property not recognized
        }
      }
    }

    return false; // Event not found
  }

  @Override
  public boolean editEventsFrom(String property, String eventName,
      LocalDateTime startDateTime, String newValue) {
    boolean modified = false;

    // Find all events with the name
    for (Event event : events) {
      if (event.getSubject().equals(eventName)) {
        boolean shouldEdit = false;

        // For non-recurring events, check start time directly
        if (!event.isRecurring()) {
          shouldEdit = !event.getStartDateTime().isBefore(startDateTime);
        }
        // For recurring events, check if any recurrences are on or after the start date
        else {
          RecurrencePattern pattern = ((EventImpl)event).getRecurrence();
          List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(event.getStartDateTime());

          // Check if any occurrences happen on or after the target date
          shouldEdit = recurrenceDates.stream()
              .anyMatch(dt -> !dt.isBefore(startDateTime));
        }

        if (shouldEdit) {
          // Update the property
          EventImpl eventImpl = (EventImpl) event;
          switch (property.toLowerCase()) {
            case "name":
            case "subject":
              eventImpl.setSubject(newValue);
              modified = true;
              break;
            case "description":
              eventImpl.setDescription(newValue);
              modified = true;
              break;
            case "location":
              eventImpl.setLocation(newValue);
              modified = true;
              break;
            case "public":
              eventImpl.setPublic(Boolean.parseBoolean(newValue));
              modified = true;
              break;
            default:
              return false; // Property not recognized
          }
        }
      }
    }

    return modified;
  }

  @Override
  public boolean editAllEvents(String property, String eventName, String newValue) {
    boolean modified = false;

    // Find all events with the given name
    for (Event event : events) {
      if (event.getSubject().equals(eventName)) {

        // Update the property
        EventImpl eventImpl = (EventImpl) event;
        switch (property.toLowerCase()) {
          case "name":
          case "subject":
            eventImpl.setSubject(newValue);
            modified = true;
            break;
          case "description":
            eventImpl.setDescription(newValue);
            modified = true;
            break;
          case "location":
            eventImpl.setLocation(newValue);
            modified = true;
            break;
          case "public":
            eventImpl.setPublic(Boolean.parseBoolean(newValue));
            modified = true;
            break;
          default:
            return false; // Property not recognized
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
    return events.stream()
        .filter(event -> isEventInDateRange(event, startDateTime, endDateTime))
        .collect(Collectors.toList());
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

      // Write events
      for (Event event : events) {
        StringBuilder line = new StringBuilder();

        // Subject
        line.append(escapeCSV(event.getSubject())).append(",");

        // Start Date
        line.append(event.getStartDateTime().format(dateFormatter)).append(",");

        // Start Time
        if (event.isAllDay()) {
          line.append(",");
        } else {
          line.append(event.getStartDateTime().format(timeFormatter)).append(",");
        }

        // End Date
        if (event.getEndDateTime() == null) {
          line.append(event.getStartDateTime().format(dateFormatter)).append(",");
        } else {
          line.append(event.getEndDateTime().format(dateFormatter)).append(",");
        }

        // End Time
        if (event.isAllDay() || event.getEndDateTime() == null) {
          line.append(",");
        } else {
          line.append(event.getEndDateTime().format(timeFormatter)).append(",");
        }

        // All Day Event
        line.append(event.isAllDay() ? "True" : "False").append(",");

        // Description
        line.append(escapeCSV(event.getDescription())).append(",");

        // Location
        line.append(escapeCSV(event.getLocation())).append(",");

        // Private
        line.append(!event.isPublic() ? "True" : "False");

        line.append("\n");
        writer.write(line.toString());
      }

      return file.getAbsolutePath();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
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
      RecurrencePattern pattern = ((EventImpl)event).getRecurrence();
      List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(event.getStartDateTime());

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
      RecurrencePattern pattern = ((EventImpl)event).getRecurrence();
      List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(event.getStartDateTime());

      return recurrenceDates.stream()
          .anyMatch(dt -> !dt.isBefore(startDateTime) && !dt.isAfter(endDateTime));
    }

    return !event.getStartDateTime().isAfter(endDateTime) &&
        (event.getEndDateTime() == null || !event.getEndDateTime().isBefore(startDateTime));
  }

  /**
   * Check if there are any conflicts with existing events in the given time range.
   */
  private boolean hasConflicts(LocalDateTime start, LocalDateTime end) {
    return events.stream()
        .anyMatch(event -> {
          if (event.isAllDay()) {
            return start.toLocalDate().equals(event.getStartDateTime().toLocalDate());
          }

          return !(end.isBefore(event.getStartDateTime()) ||
              start.isAfter(event.getEndDateTime()));
        });
  }

  /**
   * Check if there are any conflicts with existing all-day events on the given date.
   */
  private boolean hasAllDayConflicts(LocalDateTime date) {
    return events.stream()
        .anyMatch(event -> event.isAllDay() &&
            event.getStartDateTime().toLocalDate().equals(date.toLocalDate()));
  }
}