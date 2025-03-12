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

    if (!validateEventInputs(eventName, startDateTime, endDateTime)) {
      return false;
    }

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

    if (!validateEventInputs(eventName, dateTime, null)) {
      return false;
    }

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

    if (!validateEventInputs(eventName, startDateTime, endDateTime)) {
      return false;
    }

    if (!startDateTime.toLocalDate().equals(endDateTime.toLocalDate())) {
      return false;
    }

    if (hasConflicts(startDateTime, endDateTime, false)) {
      return false;
    }

    Event tempEvent = createEventObject(eventName, startDateTime, endDateTime,
        description, location, isPublic, false, weekdays, occurrences, untilDate);

    RecurrencePattern pattern = ((EventImpl) tempEvent).getRecurrence();
    List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(startDateTime);

    for (int i = 1; i < recurrenceDates.size(); i++) {
      LocalDateTime date = recurrenceDates.get(i);
      LocalDateTime recurrenceEnd = calculateRecurringEndTime(date, startDateTime, endDateTime);

      if (hasConflicts(date, recurrenceEnd, false)) {
        return false;
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

    if (!validateEventInputs(eventName, dateTime, null)) {
      return false;
    }

    Event tempEvent = createEventObject(eventName, dateTime, null,
        description, location, isPublic, true, weekdays, occurrences, untilDate);

    RecurrencePattern pattern = ((EventImpl) tempEvent).getRecurrence();
    List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(dateTime);

    for (LocalDateTime date : recurrenceDates) {
      if (hasConflicts(date, null, true)) {
        return false;
      }
    }

    events.add(tempEvent);
    return true;
  }

  @Override
  public boolean editEvent(String property, String eventName,
      LocalDateTime startDateTime, LocalDateTime endDateTime,
      String newValue) {

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

        List<LocalDateTime> recurrenceDates = originalPattern.calculateRecurrences(
            originalEvent.getStartDateTime());

        boolean hasOccurrencesAfter = recurrenceDates.stream()
            .anyMatch(date -> !date.isBefore(startDateTime));

        if (hasOccurrencesAfter) {

          List<LocalDateTime> occurrencesBefore = recurrenceDates.stream()
              .filter(date -> date.isBefore(startDateTime))
              .collect(Collectors.toList());

          List<LocalDateTime> occurrencesAfter = recurrenceDates.stream()
              .filter(date -> !date.isBefore(startDateTime))
              .collect(Collectors.toList());

          if (!occurrencesBefore.isEmpty()) {

            EventImpl shortenedOriginal;

            int shortenedOccurrences = occurrencesBefore.size();
            LocalDateTime shortenedUntil = startDateTime.minusDays(1);

            if (originalEvent.isAllDay()) {
              shortenedOriginal = new EventImpl(
                  originalEvent.getSubject(),
                  originalEvent.getStartDateTime(),
                  weekdaysToString(originalPattern.getWeekdays()),
                  shortenedOccurrences,
                  shortenedUntil
              );
            } else {
              shortenedOriginal = new EventImpl(
                  originalEvent.getSubject(),
                  originalEvent.getStartDateTime(),
                  originalEvent.getEndDateTime(),
                  weekdaysToString(originalPattern.getWeekdays()),
                  shortenedOccurrences,
                  shortenedUntil
              );
            }

            shortenedOriginal.setDescription(originalEvent.getDescription());
            shortenedOriginal.setLocation(originalEvent.getLocation());
            shortenedOriginal.setPublic(originalEvent.isPublic());

            eventsToAdd.add(shortenedOriginal);
          }

          if (!occurrencesAfter.isEmpty()) {

            EventImpl modifiedEvent;

            LocalDateTime firstOccurrenceAfter = occurrencesAfter.get(0);

            int modifiedOccurrences = occurrencesAfter.size();
            LocalDateTime modifiedUntil = originalPattern.getUntilDate();

            if (originalEvent.isAllDay()) {
              modifiedEvent = new EventImpl(
                  originalEvent.getSubject(),
                  firstOccurrenceAfter,
                  weekdaysToString(originalPattern.getWeekdays()),
                  modifiedOccurrences,
                  modifiedUntil
              );
            } else {

              LocalDateTime newEndTime = calculateEquivalentEndTime(
                  originalEvent.getStartDateTime(),
                  originalEvent.getEndDateTime(),
                  firstOccurrenceAfter
              );

              modifiedEvent = new EventImpl(
                  originalEvent.getSubject(),
                  firstOccurrenceAfter,
                  newEndTime,
                  weekdaysToString(originalPattern.getWeekdays()),
                  modifiedOccurrences,
                  modifiedUntil
              );
            }

            modifiedEvent.setDescription(originalEvent.getDescription());
            modifiedEvent.setLocation(originalEvent.getLocation());
            modifiedEvent.setPublic(originalEvent.isPublic());

            if (updateEventProperty(modifiedEvent, property, newValue)) {
              eventsToAdd.add(modifiedEvent);
              eventsToRemove.add(originalEvent);
              modified = true;
            } else {

              return false;
            }
          } else {

            eventsToRemove.add(originalEvent);
          }
        }
      } else if (event.getSubject().equals(eventName) && !event.isRecurring()) {

        if (!event.getStartDateTime().isBefore(startDateTime)) {
          if (updateEventProperty((EventImpl) event, property, newValue)) {
            modified = true;
          } else {
            return false;
          }
        }
      }
    }

    events.removeAll(eventsToRemove);
    events.addAll(eventsToAdd);

    return modified;
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


  private LocalDateTime calculateEquivalentEndTime(
      LocalDateTime originalStart,
      LocalDateTime originalEnd,
      LocalDateTime newStart) {

    long hoursDifference = originalEnd.getHour() - originalStart.getHour();
    long minutesDifference = originalEnd.getMinute() - originalStart.getMinute();

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
          return false;
        }
      }
    }

    return modified;
  }

  @Override
  public List<Event> getEventsOn(LocalDateTime dateTime) {
    List<Event> result = new ArrayList<>();

    for (Event event : events) {
      if (!event.isRecurring()) {

        if (isEventOnDate(event, dateTime.toLocalDate())) {
          result.add(event);
        }
      } else {

        RecurrencePattern pattern = ((EventImpl) event).getRecurrence();

        List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(
            event.getStartDateTime());

        boolean hasOccurrenceOnDate = recurrenceDates.stream()
            .anyMatch(date -> date.toLocalDate().equals(dateTime.toLocalDate()));

        if (hasOccurrenceOnDate) {

          LocalDateTime occurrenceDateTime = recurrenceDates.stream()
              .filter(date -> date.toLocalDate().equals(dateTime.toLocalDate()))
              .findFirst()
              .orElse(null);

          if (occurrenceDateTime != null) {

            Event occurrenceEvent = createOccurrenceEvent(event, occurrenceDateTime);
            result.add(occurrenceEvent);
          }
        }
      }
    }

    return result;
  }


  @Override
  public List<Event> getEventsFrom(LocalDateTime startDateTime, LocalDateTime endDateTime) {
    List<Event> result = new ArrayList<>();

    for (Event event : events) {
      if (!event.isRecurring()) {
        if (isEventInDateRange(event, startDateTime, endDateTime)) {
          result.add(event);
        }
      } else {

        RecurrencePattern pattern = ((EventImpl) event).getRecurrence();

        List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(
            event.getStartDateTime());

        List<LocalDateTime> occurrencesInRange = recurrenceDates.stream()
            .filter(date -> !date.toLocalDate().isBefore(startDateTime.toLocalDate()) &&
                !date.toLocalDate().isAfter(endDateTime.toLocalDate()))
            .collect(Collectors.toList());

        for (LocalDateTime occurrenceDate : occurrencesInRange) {
          Event occurrenceEvent = createOccurrenceEvent(event, occurrenceDate);
          result.add(occurrenceEvent);
        }
      }
    }

    return result;
  }

  /**
   * Creates a non-recurring event instance for a specific occurrence of a recurring event.
   *
   * @param templateEvent  the recurring event template
   * @param occurrenceDate the date of this specific occurrence
   * @return a new Event instance representing this occurrence
   */
  private Event createOccurrenceEvent(Event templateEvent, LocalDateTime occurrenceDate) {
    EventImpl original = (EventImpl) templateEvent;
    EventImpl occurrence;

    if (original.isAllDay()) {

      occurrence = new EventImpl(original.getSubject(), occurrenceDate);
    } else {

      LocalDateTime occurrenceEndTime = calculateRecurringEndTime(
          occurrenceDate, original.getStartDateTime(), original.getEndDateTime());
      occurrence = new EventImpl(original.getSubject(), occurrenceDate, occurrenceEndTime);
    }

    occurrence.setDescription(original.getDescription());
    occurrence.setLocation(original.getLocation());
    occurrence.setPublic(original.isPublic());

    String subject = occurrence.getSubject();
    if (!subject.contains("[Recurring]")) {
      occurrence.setSubject(subject + " [Recurring]");
    }

    return occurrence;
  }

  @Override
  public boolean isBusy(LocalDateTime dateTime) {
    return events.stream()
        .anyMatch(event -> {
          if (!event.isRecurring()) {

            return isEventActiveAt(event, dateTime);
          } else {

            RecurrencePattern pattern = ((EventImpl) event).getRecurrence();
            List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(
                event.getStartDateTime());

            for (LocalDateTime recurrenceDate : recurrenceDates) {

              LocalDateTime occurrenceStart = recurrenceDate;
              LocalDateTime occurrenceEnd = null;

              if (!event.isAllDay() && event.getEndDateTime() != null) {

                long hoursDifference =
                    event.getEndDateTime().getHour() - event.getStartDateTime().getHour();
                long minutesDifference =
                    event.getEndDateTime().getMinute() - event.getStartDateTime().getMinute();
                occurrenceEnd = occurrenceStart.plusHours(hoursDifference)
                    .plusMinutes(minutesDifference);
              }

              if (event.isAllDay()) {
                if (dateTime.toLocalDate().equals(occurrenceStart.toLocalDate())) {
                  return true;
                }
              } else if (occurrenceEnd != null &&
                  !dateTime.isBefore(occurrenceStart) &&
                  !dateTime.isAfter(occurrenceEnd)) {
                return true;
              }
            }
            return false;
          }
        });
  }

  @Override
  public String exportToCSV(String fileName) {
    File file = new File(fileName);

    try (FileWriter writer = new FileWriter(file)) {

      writer.write(
          "Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description,Location,Private\n");

      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
      DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

      for (Event event : events) {
        if (event.isRecurring()) {

          RecurrencePattern pattern = ((EventImpl) event).getRecurrence();
          List<LocalDateTime> occurrences = pattern.calculateRecurrences(event.getStartDateTime());

          for (LocalDateTime occurrence : occurrences) {

            LocalDateTime occurrenceEndTime = null;
            if (!event.isAllDay() && event.getEndDateTime() != null) {
              long hoursDifference =
                  event.getEndDateTime().getHour() - event.getStartDateTime().getHour();
              long minutesDifference =
                  event.getEndDateTime().getMinute() - event.getStartDateTime().getMinute();
              occurrenceEndTime = occurrence.plusHours(hoursDifference)
                  .plusMinutes(minutesDifference);
            }

            writeSingleEventToCSV(writer, event, occurrence, occurrenceEndTime, dateFormatter,
                timeFormatter);
          }
        } else {

          writeSingleEventToCSV(writer, event, event.getStartDateTime(), event.getEndDateTime(),
              dateFormatter, timeFormatter);
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

    line.append(escapeCSV(event.getSubject())).append(",");

    line.append(startDateTime.format(dateFormatter)).append(",");

    if (event.isAllDay()) {
      line.append(",");
    } else {
      line.append(startDateTime.format(timeFormatter)).append(",");
    }

    if (endDateTime == null) {
      line.append(startDateTime.format(dateFormatter)).append(",");
    } else {
      line.append(endDateTime.format(dateFormatter)).append(",");
    }

    if (event.isAllDay() || endDateTime == null) {
      line.append(",");
    } else {
      line.append(endDateTime.format(timeFormatter)).append(",");
    }

    line.append(event.isAllDay() ? "True" : "False").append(",");

    line.append(escapeCSV(event.getDescription())).append(",");

    line.append(escapeCSV(event.getLocation())).append(",");

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
   * @param eventName     the event name
   * @param startDateTime the start date/time
   * @param endDateTime   the end date/time (can be null for all-day events)
   * @return true if inputs are valid
   */
  private boolean validateEventInputs(String eventName, LocalDateTime startDateTime,
      LocalDateTime endDateTime) {
    if (eventName == null || eventName.trim().isEmpty()) {
      return false;
    }

    if (startDateTime == null) {
      return false;
    }

    if (endDateTime != null && endDateTime.isBefore(startDateTime)) {
      return false;
    }

    return true;
  }

  /**
   * Creates a new Event object.
   */
  private Event createEventObject(String eventName, LocalDateTime startDateTime,
      LocalDateTime endDateTime,
      String description, String location, boolean isPublic, boolean isAllDay,
      String weekdays, int occurrences, LocalDateTime untilDate) {

    Event newEvent;

    if (isAllDay) {
      if (weekdays != null) {

        newEvent = new EventImpl(eventName, startDateTime, weekdays, occurrences, untilDate);
      } else {

        newEvent = new EventImpl(eventName, startDateTime);
      }
    } else {
      if (weekdays != null) {

        newEvent = new EventImpl(eventName, startDateTime, endDateTime, weekdays, occurrences,
            untilDate);
      } else {

        newEvent = new EventImpl(eventName, startDateTime, endDateTime);
      }
    }

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
   * @param property  the property name
   * @param newValue  the new value
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
    boolean nameMatches =
        storedSubject.equals(eventName) || storedSubject.equals(unquotedEventName);

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
  private boolean isEventInDateRange(Event event, LocalDateTime startDateTime,
      LocalDateTime endDateTime) {
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
    RecurrencePattern pattern = ((EventImpl) event).getRecurrence();
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
   * @param start    the start time
   * @param end      the end time (null for all-day events)
   * @param isAllDay whether this is an all-day event
   * @return true if there are conflicts
   */
  private boolean hasConflicts(LocalDateTime start, LocalDateTime end, boolean isAllDay) {
    return events.stream()
        .anyMatch(event -> {
          if (!event.isRecurring()) {

            if (isAllDay || event.isAllDay()) {

              return start.toLocalDate().equals(event.getStartDateTime().toLocalDate());
            }

            return start.compareTo(event.getEndDateTime()) < 0 &&
                end.compareTo(event.getStartDateTime()) > 0;
          } else {

            RecurrencePattern pattern = ((EventImpl) event).getRecurrence();
            List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(
                event.getStartDateTime());

            for (LocalDateTime recurrenceDate : recurrenceDates) {
              if (isAllDay || event.isAllDay()) {

                if (start.toLocalDate().equals(recurrenceDate.toLocalDate())) {
                  return true;
                }
              } else {

                LocalDateTime recurrenceEnd = calculateRecurringEndTime(
                    recurrenceDate, event.getStartDateTime(), event.getEndDateTime());

                if (start.compareTo(recurrenceEnd) < 0 && end.compareTo(recurrenceDate) > 0) {
                  return true;
                }
              }
            }
            return false;
          }
        });
  }
}