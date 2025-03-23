package model;
import controller.CommandProcessor;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the Calendar interface.
 */
public class CalendarImpl implements Calendar {

  private String name;
  private ZoneId timezone;
  private List<Event> events;

  /**
   * Creates a new calendar with default timezone.
   */
  public CalendarImpl() {
    this("Default", ZoneId.systemDefault());
  }

  /**
   * Creates a new calendar with specified name and default timezone.
   *
   * @param name the name of the calendar
   */
  public CalendarImpl(String name) {
    this(name, ZoneId.systemDefault());
  }

  /**
   * Creates a new calendar with specified name and timezone.
   *
   * @param name the name of the calendar
   * @param timezone the timezone for the calendar
   */
  public CalendarImpl(String name, ZoneId timezone) {
    this.name = name;
    this.timezone = timezone;
    this.events = new ArrayList<>();
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the calendar.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public ZoneId getTimezone() {
    return timezone;
  }

  /**
   * Sets the timezone of the calendar.
   *
   * @param timezone the new timezone
   */
  public void setTimezone(ZoneId timezone) {
    this.timezone = timezone;
  }

  @Override
  public ZonedDateTime toZonedDateTime(LocalDateTime localDateTime) {
    return localDateTime.atZone(this.timezone);
  }

  @Override
  public LocalDateTime toLocalDateTime(ZonedDateTime zonedDateTime) {
    return zonedDateTime.withZoneSameInstant(this.timezone).toLocalDateTime();
  }

  @Override
  public boolean createEvent(String eventName, LocalDateTime startDateTime,
      LocalDateTime endDateTime, boolean autoDecline, String description,
      String location, boolean isPublic) {

    if (!validateEventInputs(eventName, startDateTime, endDateTime)) {
      return false;
    }

    // Always check for conflicts (regardless of autoDecline value)
    if (hasConflicts(startDateTime, endDateTime, false)) {
      return false;
    }

    Event newEvent = createEventObject(eventName, startDateTime, endDateTime,
        description, location, isPublic, false, null, -1, null);

    events.add(newEvent);
    return true;
  }

  @Override
  public boolean createAllDayEvent(String eventName, LocalDateTime dateTime,
      boolean autoDecline, String description, String location, boolean isPublic) {

    if (!validateEventInputs(eventName, dateTime, null)) {
      return false;
    }

    // Always check for conflicts
    if (hasConflicts(dateTime, null, true)) {
      return false;
    }

    Event newEvent = createEventObject(eventName, dateTime, null,
        description, location, isPublic, true, null, -1, null);

    events.add(newEvent);
    return true;
  }

  @Override
  public boolean createRecurringEvent(String eventName
      , LocalDateTime startDateTime, LocalDateTime endDateTime
      , String weekdays, int occurrences, LocalDateTime untilDate
      , boolean autoDecline, String description
      , String location, boolean isPublic) {

    if (!validateEventInputs(eventName, startDateTime, endDateTime)) {
      return false;
    }

    if (!startDateTime.toLocalDate().equals(endDateTime.toLocalDate())) {
      return false;
    }

    // Always check for conflicts
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
  public boolean createRecurringAllDayEvent(String eventName, LocalDateTime dateTime
      , String weekdays, int occurrences
      , LocalDateTime untilDate, boolean autoDecline
      , String description, String location, boolean isPublic) {

    if (!validateEventInputs(eventName, dateTime, null)) {
      return false;
    }

    // Always check for conflicts
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
        // Check if edit would create conflicts
        EventImpl eventCopy = new EventImpl(
            event.getSubject(),
            event.getStartDateTime(),
            event.getEndDateTime()
        );
        eventCopy.setDescription(((EventImpl)event).getDescription());
        eventCopy.setLocation(((EventImpl)event).getLocation());
        eventCopy.setPublic(event.isPublic());

        if (!updateEventProperty(eventCopy, property, newValue)) {
          return false;
        }

        // Check for conflicts with the updated event
        if (property.equalsIgnoreCase("starttime") ||
            property.equalsIgnoreCase("startdate") ||
            property.equalsIgnoreCase("endtime") ||
            property.equalsIgnoreCase("enddate")) {

          if (hasConflictsExcluding(
              eventCopy.getStartDateTime(),
              eventCopy.getEndDateTime(),
              eventCopy.isAllDay(),
              event)) {
            return false;
          }
        }

        // If no conflicts, apply the changes to the actual event
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

            // Check for conflicts with the modified event after property change
            EventImpl modifiedEventCopy = new EventImpl(
                modifiedEvent.getSubject(),
                modifiedEvent.getStartDateTime(),
                modifiedEvent.getEndDateTime(),
                weekdaysToString(originalPattern.getWeekdays()),
                modifiedOccurrences,
                modifiedUntil
            );
            modifiedEventCopy.setDescription(modifiedEvent.getDescription());
            modifiedEventCopy.setLocation(modifiedEvent.getLocation());
            modifiedEventCopy.setPublic(modifiedEvent.isPublic());

            if (!updateEventProperty(modifiedEventCopy, property, newValue)) {
              return false;
            }

            // Check for conflicts with all recurrences
            RecurrencePattern modifiedPattern = modifiedEventCopy.getRecurrence();
            List<LocalDateTime> modifiedDates = modifiedPattern.calculateRecurrences(
                modifiedEventCopy.getStartDateTime());

            boolean hasConflict = false;
            for (LocalDateTime date : modifiedDates) {
              LocalDateTime endTime = null;
              if (!modifiedEventCopy.isAllDay()) {
                endTime = calculateRecurringEndTime(
                    date,
                    modifiedEventCopy.getStartDateTime(),
                    modifiedEventCopy.getEndDateTime()
                );
              }

              if (hasConflictsExcluding(date, endTime, modifiedEventCopy.isAllDay(), originalEvent)) {
                hasConflict = true;
                break;
              }
            }

            if (hasConflict) {
              return false;
            }

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
          // Check for conflicts with the modified event
          EventImpl eventCopy = new EventImpl(
              event.getSubject(),
              event.getStartDateTime(),
              event.getEndDateTime()
          );
          eventCopy.setDescription(((EventImpl)event).getDescription());
          eventCopy.setLocation(((EventImpl)event).getLocation());
          eventCopy.setPublic(event.isPublic());

          if (!updateEventProperty(eventCopy, property, newValue)) {
            return false;
          }

          if (property.equalsIgnoreCase("starttime") ||
              property.equalsIgnoreCase("startdate") ||
              property.equalsIgnoreCase("endtime") ||
              property.equalsIgnoreCase("enddate")) {

            if (hasConflictsExcluding(
                eventCopy.getStartDateTime(),
                eventCopy.getEndDateTime(),
                eventCopy.isAllDay(),
                event)) {
              return false;
            }
          }

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

  @Override
  public boolean editAllEvents(String property, String eventName, String newValue) {
    List<Event> matchingEvents = new ArrayList<>();

    // Find all matching events
    for (Event event : events) {
      if (event.getSubject().equals(eventName)) {
        matchingEvents.add(event);
      }
    }

    if (matchingEvents.isEmpty()) {
      return false;
    }

    // First check if all events can be updated without conflicts
    for (Event event : matchingEvents) {
      EventImpl eventCopy = new EventImpl(
          event.getSubject(),
          event.getStartDateTime(),
          event.getEndDateTime()
      );
      eventCopy.setDescription(((EventImpl)event).getDescription());
      eventCopy.setLocation(((EventImpl)event).getLocation());
      eventCopy.setPublic(event.isPublic());

      if (!updateEventProperty(eventCopy, property, newValue)) {
        return false;
      }

      if (property.equalsIgnoreCase("starttime") ||
          property.equalsIgnoreCase("startdate") ||
          property.equalsIgnoreCase("endtime") ||
          property.equalsIgnoreCase("enddate")) {

        if (hasConflictsExcluding(
            eventCopy.getStartDateTime(),
            eventCopy.getEndDateTime(),
            eventCopy.isAllDay(),
            event)) {
          return false;
        }
      }
    }

    // If all can be updated, apply the changes
    for (Event event : matchingEvents) {
      if (!updateEventProperty((EventImpl) event, property, newValue)) {
        return false;
      }
    }

    return true;
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
            .filter(date -> !date.toLocalDate().isBefore(startDateTime.toLocalDate())
                && !date.toLocalDate().isAfter(endDateTime.toLocalDate()))
            .collect(Collectors.toList());

        for (LocalDateTime occurrenceDate : occurrencesInRange) {
          Event occurrenceEvent = createOccurrenceEvent(event, occurrenceDate);
          result.add(occurrenceEvent);
        }
      }
    }

    return result;
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
                    event.getEndDateTime().getHour()
                        - event.getStartDateTime().getHour();
                long minutesDifference =
                    event.getEndDateTime().getMinute()
                        - event.getStartDateTime().getMinute();
                occurrenceEnd = occurrenceStart.plusHours(hoursDifference)
                    .plusMinutes(minutesDifference);
              }

              if (event.isAllDay()) {
                if (dateTime.toLocalDate().equals(occurrenceStart.toLocalDate())) {
                  return true;
                }
              } else if (occurrenceEnd != null
                  && !dateTime.isBefore(occurrenceStart)
                  && !dateTime.isAfter(occurrenceEnd)) {
                return true;
              }
            }
            return false;
          }
        });
  }

  @Override
  public List<Event> getAllEvents() {
    return new ArrayList<>(events);
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

    return endDateTime == null || !endDateTime.isBefore(startDateTime);
  }

  /**
   * Creates a new Event object.
   */
  private Event createEventObject(String eventName, LocalDateTime startDateTime
      , LocalDateTime endDateTime, String description, String location
      , boolean isPublic, boolean isAllDay, String weekdays
      , int occurrences, LocalDateTime untilDate) {

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
      case "starttime":
      case "startdate":
        try {
          LocalDateTime newStart = CommandProcessor.parseDateTime(newValue);

          // For non-all-day events, verify end time is after new start time
          if (!eventImpl.isAllDay() && eventImpl.getEndDateTime() != null
              && newStart.isAfter(eventImpl.getEndDateTime())) {
            return false; // Invalid time range
          }

          // No conflict check, just update the time
          eventImpl.setStartDateTime(newStart);
          return true;
        } catch (Exception e) {
          return false;
        }
      case "endtime":
      case "enddate":
        if (eventImpl.isAllDay()) {
          return false; // Cannot set end time for all-day events
        }

        try {
          LocalDateTime newEnd = CommandProcessor.parseDateTime(newValue);
          // Validate that end time is after start time
          if (newEnd.isBefore(eventImpl.getStartDateTime())
              || newEnd.equals(eventImpl.getStartDateTime())) {
            return false; // End time must be after start time
          }

          // No conflict check, just update the time
          eventImpl.setEndDateTime(newEnd);
          return true;
        } catch (Exception e) {
          return false;
        }
      default:
        return false;
    }
  }

  /**
   * Check if an event matches the specified criteria.
   */
  private boolean isMatchingEvent(Event event, String eventName, String unquotedEventName,
      LocalDateTime startDateTime, LocalDateTime endDateTime) {

    // Step 1: Name matching - handle quotes and [Recurring] suffix
    String storedSubject = event.getSubject();
    boolean nameMatches = false;

    // Direct comparison
    if (storedSubject.equals(eventName) || storedSubject.equals(unquotedEventName)) {
      nameMatches = true;
    }

    // Check with quotes
    if (!nameMatches && !eventName.startsWith("\"") && !eventName.endsWith("\"")) {
      if (storedSubject.equals("\"" + eventName + "\"")) {
        nameMatches = true;
      }
    }

    // Check without [Recurring] suffix
    if (!nameMatches && storedSubject.contains(" [Recurring]")) {
      String nameWithoutSuffix = storedSubject.replace(" [Recurring]", "");
      if (nameWithoutSuffix.equals(eventName) || nameWithoutSuffix.equals(unquotedEventName)) {
        nameMatches = true;
      }

      // Also check with quotes
      if (!nameMatches && !eventName.startsWith("\"") && !eventName.endsWith("\"")) {
        if (nameWithoutSuffix.equals("\"" + eventName + "\"")) {
          nameMatches = true;
        }
      }
    }

    if (!nameMatches) {
      return false;
    }

    // Step 2: Date matching
    boolean dateMatches = event.getStartDateTime().toLocalDate()
        .equals(startDateTime.toLocalDate());

    if (!dateMatches) {
      return false;
    }

    // Step 3: Time matching - check hour and minute only
    boolean timeMatches = event.getStartDateTime().getHour() == startDateTime.getHour() &&
        event.getStartDateTime().getMinute() == startDateTime.getMinute();

    return timeMatches;
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

    return !dateTime.isBefore(event.getStartDateTime())
        && !dateTime.isAfter(event.getEndDateTime());
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

    return event.getStartDateTime().toLocalDate().equals(date)
        || (event.getEndDateTime() != null
        && event.getEndDateTime().toLocalDate().equals(date));
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

    return !event.getStartDateTime().isAfter(endDateTime)
        && (event.getEndDateTime() == null || !event.getEndDateTime().isBefore(startDateTime));
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
  private LocalDateTime calculateRecurringEndTime(LocalDateTime date
      , LocalDateTime originalStart, LocalDateTime originalEnd) {
    return calculateEquivalentEndTime(originalStart, originalEnd, date);
  }

  /**
   * Calculate equivalent end time based on duration.
   */
  private LocalDateTime calculateEquivalentEndTime(
      LocalDateTime originalStart,
      LocalDateTime originalEnd,
      LocalDateTime newStart) {

    long hoursDifference = originalEnd.getHour() - originalStart.getHour();
    long minutesDifference = originalEnd.getMinute() - originalStart.getMinute();

    return newStart.plusHours(hoursDifference).plusMinutes(minutesDifference);
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

            return start.compareTo(event.getEndDateTime()) < 0
                && end.compareTo(event.getStartDateTime()) > 0;
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

  /**
   * Check if there are any conflicts with existing events, excluding a specific event.
   * This is used when checking if an edit would create conflicts.
   */
  private boolean hasConflictsExcluding(LocalDateTime start, LocalDateTime end,
      boolean isAllDay, Event excludeEvent) {
    return events.stream()
        .filter(event -> event != excludeEvent)
        .anyMatch(event -> {
          if (!event.isRecurring()) {
            if (isAllDay || event.isAllDay()) {
              return start.toLocalDate().equals(event.getStartDateTime().toLocalDate());
            }

            return start.compareTo(event.getEndDateTime()) < 0
                && end.compareTo(event.getStartDateTime()) > 0;
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

  /**
   * Helper method to convert weekdays to string.
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

  /**
   * Clears all events from the calendar.
   * Used internally for timezone changes.
   */
  void clearEvents() {
    this.events.clear();
  }

  /**
   * Adds an event to the calendar.
   * Used internally for timezone changes.
   *
   * @param event the event to add
   */
  void addEvent(Event event) {
    this.events.add(event);
  }
}