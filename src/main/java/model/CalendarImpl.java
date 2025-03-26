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
   * @param name     the name of the calendar
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

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public ZoneId getTimezone() {
    return timezone;
  }

  public void setTimezone(ZoneId timezone) {
    this.timezone = timezone;
  }

  @Override
  public ZonedDateTime toZonedDateTime(LocalDateTime localDateTime) {
    return localDateTime.atZone(timezone);
  }

  @Override
  public LocalDateTime toLocalDateTime(ZonedDateTime zonedDateTime) {
    return zonedDateTime.withZoneSameInstant(timezone).toLocalDateTime();
  }

  @Override
  public boolean createEvent(String eventName, LocalDateTime startDateTime,
      LocalDateTime endDateTime, boolean autoDecline,
      String description, String location, boolean isPublic) {
    return createGenericEvent(eventName, startDateTime, endDateTime, autoDecline, description,
        location, isPublic, false, null, -1, null);
  }

  @Override
  public boolean createAllDayEvent(String eventName, LocalDateTime dateTime,
      boolean autoDecline, String description,
      String location, boolean isPublic) {
    return createGenericEvent(eventName, dateTime, null, autoDecline, description,
        location, isPublic, true, null, -1, null);
  }

  @Override
  public boolean createRecurringEvent(String eventName, LocalDateTime startDateTime,
      LocalDateTime endDateTime, String weekdays,
      int occurrences, LocalDateTime untilDate,
      boolean autoDecline, String description,
      String location, boolean isPublic) {
    if (!validateEventInputs(eventName, startDateTime, endDateTime) ||
        !startDateTime.toLocalDate().equals(endDateTime.toLocalDate())) {
      return false;
    }

    if (hasConflicts(startDateTime, endDateTime, false)) {
      return false;
    }

    return handleRecurringEvent(eventName, startDateTime, endDateTime, description, location,
        isPublic, false, weekdays, occurrences, untilDate);
  }

  @Override
  public boolean createRecurringAllDayEvent(String eventName, LocalDateTime dateTime,
      String weekdays, int occurrences,
      LocalDateTime untilDate, boolean autoDecline,
      String description, String location, boolean isPublic) {
    if (!validateEventInputs(eventName, dateTime, null)) {
      return false;
    }

    return handleRecurringEvent(eventName, dateTime, null, description, location,
        isPublic, true, weekdays, occurrences, untilDate);
  }

  private boolean createGenericEvent(String eventName, LocalDateTime start, LocalDateTime end,
      boolean autoDecline, String description, String location,
      boolean isPublic, boolean isAllDay, String weekdays,
      int occurrences, LocalDateTime untilDate) {
    if (!validateEventInputs(eventName, start, end) ||
        (isAllDay ? hasConflicts(start, null, true) : hasConflicts(start, end, false))) {
      return false;
    }

    Event event = createEventObject(eventName, start, end, description, location, isPublic,
        isAllDay, weekdays, occurrences, untilDate);
    events.add(event);
    return true;
  }

  private boolean handleRecurringEvent(String eventName, LocalDateTime start, LocalDateTime end,
      String description, String location, boolean isPublic,
      boolean isAllDay, String weekdays, int occurrences,
      LocalDateTime untilDate) {
    Event tempEvent = createEventObject(eventName, start, end, description, location, isPublic,
        isAllDay, weekdays, occurrences, untilDate);
    RecurrencePattern pattern = ((EventImpl) tempEvent).getRecurrence();
    List<LocalDateTime> dates = pattern.calculateRecurrences(start);

    List<LocalDateTime> checkDates = isAllDay ? dates : dates.subList(1, dates.size());
    if (checkRecurringDatesForConflicts(checkDates, start, end, isAllDay)) {
      return false;
    }

    events.add(tempEvent);
    return true;
  }

  @Override
  public boolean editEvent(String property, String eventName,
      LocalDateTime startDateTime, LocalDateTime endDateTime,
      String newValue) {
    String unquotedName = removeQuotes(eventName);
    for (Event event : events) {
      if (isMatchingEvent(event, eventName, unquotedName, startDateTime, endDateTime)) {
        return updateEventWithConflictCheck((EventImpl) event, property, newValue);
      }
    }
    return false;
  }

  @Override
  public boolean editEventsFrom(String property, String eventName,
      LocalDateTime startDateTime, String newValue) {
    List<Event> modifiedEvents = new ArrayList<>();
    List<Event> eventsToAdd = new ArrayList<>();
    List<Event> eventsToRemove = new ArrayList<>();

    processEventsForEditFrom(eventName, startDateTime, property, newValue,
        modifiedEvents, eventsToAdd, eventsToRemove);

    events.removeAll(eventsToRemove);
    events.addAll(eventsToAdd);
    return !modifiedEvents.isEmpty();
  }

  private void processEventsForEditFrom(String eventName, LocalDateTime startDateTime,
      String property, String newValue,
      List<Event> modifiedEvents,
      List<Event> eventsToAdd,
      List<Event> eventsToRemove) {
    for (Event event : events) {
      if (event.getSubject().equals(eventName)) {
        if (event.isRecurring()) {
          handleRecurringEventEdit(event, startDateTime, property, newValue,
              modifiedEvents, eventsToAdd, eventsToRemove);
        } else if (!event.getStartDateTime().isBefore(startDateTime)) {
          // Handle non-recurring event after start date
          EventImpl eventImpl = (EventImpl) event;
          if (updateEventWithConflictCheck(eventImpl, property, newValue)) {
            modifiedEvents.add(event);
          }
        }
      }
    }
  }

  private void handleRecurringEventEdit(Event event, LocalDateTime startDateTime,
      String property, String newValue,
      List<Event> modifiedEvents,
      List<Event> eventsToAdd,
      List<Event> eventsToRemove) {
    EventImpl original = (EventImpl) event;
    RecurrencePattern pattern = original.getRecurrence();
    List<LocalDateTime> dates = pattern.calculateRecurrences(original.getStartDateTime());

    List<LocalDateTime> beforeDates = dates.stream()
        .filter(d -> d.isBefore(startDateTime))
        .collect(Collectors.toList());
    List<LocalDateTime> afterDates = dates.stream()
        .filter(d -> !d.isBefore(startDateTime))
        .collect(Collectors.toList());

    if (!beforeDates.isEmpty()) {
      EventImpl shortened = createShortenedEvent(original, beforeDates);
      if (shortened != null) {
        eventsToAdd.add(shortened);
      }
    }

    if (!afterDates.isEmpty()) {
      EventImpl modifiedEvent = createModifiedEvent(original, afterDates, property, newValue);
      if (modifiedEvent != null) {
        eventsToAdd.add(modifiedEvent);
        eventsToRemove.add(original);
        modifiedEvents.add(modifiedEvent);
      }
    } else {
      eventsToRemove.add(original);
    }
  }

  private EventImpl createShortenedEvent(EventImpl original, List<LocalDateTime> beforeDates) {
    LocalDateTime untilDate = beforeDates.get(beforeDates.size() - 1);
    EventImpl shortened;

    if (original.isAllDay()) {
      shortened = new EventImpl(
          original.getSubject(),
          original.getStartDateTime(),
          weekdaysToString(original.getRecurrence().getWeekdays()),
          beforeDates.size(),
          untilDate
      );
    } else {
      shortened = new EventImpl(
          original.getSubject(),
          original.getStartDateTime(),
          original.getEndDateTime(),
          weekdaysToString(original.getRecurrence().getWeekdays()),
          beforeDates.size(),
          untilDate
      );
    }

    shortened.setDescription(original.getDescription());
    shortened.setLocation(original.getLocation());
    shortened.setPublic(original.isPublic());

    return shortened;
  }

  private EventImpl createModifiedEvent(EventImpl original, List<LocalDateTime> dates,
      String property, String newValue) {
    LocalDateTime firstDate = dates.get(0);
    EventImpl modified;

    if (original.isAllDay()) {
      modified = new EventImpl(
          original.getSubject(),
          firstDate,
          weekdaysToString(original.getRecurrence().getWeekdays()),
          dates.size(),
          original.getRecurrence().getUntilDate()
      );
    } else {
      LocalDateTime newEndTime = calculateEquivalentEndTime(
          original.getStartDateTime(),
          original.getEndDateTime(),
          firstDate
      );

      modified = new EventImpl(
          original.getSubject(),
          firstDate,
          newEndTime,
          weekdaysToString(original.getRecurrence().getWeekdays()),
          dates.size(),
          original.getRecurrence().getUntilDate()
      );
    }

    modified.setDescription(original.getDescription());
    modified.setLocation(original.getLocation());
    modified.setPublic(original.isPublic());

    if (updateEventWithConflictCheck(modified, property, newValue)) {
      return modified;
    }
    return null;
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
    List<Event> matchingEvents = events.stream()
        .filter(e -> e.getSubject().equals(eventName))
        .collect(Collectors.toList());

    if (matchingEvents.isEmpty()) {
      return false;
    }

    // Check if all events can be updated without conflicts
    for (Event event : matchingEvents) {
      EventImpl copy = createEventCopy((EventImpl) event);
      if (!updateEventProperty(copy, property, newValue)) {
        return false;
      }

      if (isTimeProperty(property) && hasConflictsExcluding(
          copy.getStartDateTime(), copy.getEndDateTime(),
          copy.isAllDay(), event)) {
        return false;
      }
    }

    // If all can be updated, apply the changes
    for (Event event : matchingEvents) {
      updateEventProperty((EventImpl) event, property, newValue);
    }

    return true;
  }

  private boolean updateEventWithConflictCheck(EventImpl event, String property, String newValue) {
    EventImpl copy = createEventCopy(event);
    if (!updateEventProperty(copy, property, newValue)) {
      return false;
    }

    if (isTimeProperty(property) && hasConflictsExcluding(
        copy.getStartDateTime(), copy.getEndDateTime(),
        copy.isAllDay(), event)) {
      return false;
    }

    return updateEventProperty(event, property, newValue);
  }

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
  public List<Event> getEventsFrom(LocalDateTime start, LocalDateTime end) {
    List<Event> result = new ArrayList<>();

    for (Event event : events) {
      if (!event.isRecurring()) {
        if (isEventInRange(event, start, end)) {
          result.add(event);
        }
      } else {
        RecurrencePattern pattern = ((EventImpl) event).getRecurrence();
        List<LocalDateTime> recurrenceDates = pattern.calculateRecurrences(
            event.getStartDateTime());

        List<LocalDateTime> occurrencesInRange = recurrenceDates.stream()
            .filter(date -> !date.toLocalDate().isBefore(start.toLocalDate())
                && !date.toLocalDate().isAfter(end.toLocalDate()))
            .collect(Collectors.toList());

        for (LocalDateTime occurrenceDate : occurrencesInRange) {
          Event occurrenceEvent = createOccurrenceEvent(event, occurrenceDate);
          result.add(occurrenceEvent);
        }
      }
    }

    return result;
  }

  private boolean isEventInRange(Event event, LocalDateTime start, LocalDateTime end) {
    return !event.getStartDateTime().isAfter(end) &&
        (event.getEndDateTime() == null || !event.getEndDateTime().isBefore(start));
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

  // Helper methods from original implementation with minimal changes
  private boolean validateEventInputs(String eventName, LocalDateTime start, LocalDateTime end) {
    return eventName != null && !eventName.trim().isEmpty() && start != null &&
        (end == null || !end.isBefore(start));
  }

  private Event createEventObject(String name, LocalDateTime start, LocalDateTime end,
      String desc, String loc, boolean isPublic,
      boolean allDay, String weekdays,
      int occurrences, LocalDateTime until) {
    Event newEvent;

    if (allDay) {
      if (weekdays != null) {
        newEvent = new EventImpl(name, start, weekdays, occurrences, until);
      } else {
        newEvent = new EventImpl(name, start);
      }
    } else {
      if (weekdays != null) {
        newEvent = new EventImpl(name, start, end, weekdays, occurrences, until);
      } else {
        newEvent = new EventImpl(name, start, end);
      }
    }

    EventImpl eventImpl = (EventImpl) newEvent;

    if (desc != null && !desc.isEmpty()) {
      eventImpl.setDescription(desc);
    }

    if (loc != null && !loc.isEmpty()) {
      eventImpl.setLocation(loc);
    }

    eventImpl.setPublic(isPublic);

    return newEvent;
  }

  private boolean checkRecurringDatesForConflicts(List<LocalDateTime> dates,
      LocalDateTime originalStart,
      LocalDateTime originalEnd,
      boolean isAllDay) {
    return dates.stream().anyMatch(d -> {
      LocalDateTime end = isAllDay ? null :
          calculateRecurringEndTime(d, originalStart, originalEnd);
      return hasConflicts(d, end, isAllDay);
    });
  }

  private boolean isTimeProperty(String property) {
    String p = property.toLowerCase();
    return p.equals("starttime") || p.equals("startdate") ||
        p.equals("endtime") || p.equals("enddate");
  }

  private EventImpl createEventCopy(EventImpl original) {
    EventImpl copy;

    if (original.isAllDay()) {
      copy = new EventImpl(original.getSubject(), original.getStartDateTime());
    } else {
      copy = new EventImpl(original.getSubject(),
              original.getStartDateTime(), original.getEndDateTime());
    }

    copy.setDescription(original.getDescription());
    copy.setLocation(original.getLocation());
    copy.setPublic(original.isPublic());

    return copy;
  }

  private boolean isMatchingEvent(Event event, String eventName, String unquotedName,
      LocalDateTime start, LocalDateTime end) {
    // Name matching - handle quotes and [Recurring] suffix
    String storedSubject = event.getSubject();
    boolean nameMatches = false;

    // Direct comparison
    if (storedSubject.equals(eventName) || storedSubject.equals(unquotedName)) {
      nameMatches = true;
    }

    // Check with quotes
    if (!nameMatches && !eventName.startsWith("\"") && !eventName.endsWith("\"")) {
      if (storedSubject.equals("\"" + eventName + "\"")) {
        nameMatches = true;
      }
    }

    //    // Check without [Recurring] suffix
    //    if (!nameMatches && storedSubject.contains(" [Recurring]")) {
    //      String nameWithoutSuffix = storedSubject.replace(" [Recurring]", "");
    //      if (nameWithoutSuffix.equals(eventName) || nameWithoutSuffix.equals(unquotedName)) {
    //        nameMatches = true;
    //      }
    //
    //      // Also check with quotes
    //      if (!nameMatches && !eventName.startsWith("\"") && !eventName.endsWith("\"")) {
    //        if (nameWithoutSuffix.equals("\"" + eventName + "\"")) {
    //          nameMatches = true;
    //        }
    //      }
    //    }

    if (!nameMatches) {
      return false;
    }

    // Date matching
    boolean dateMatches = event.getStartDateTime().toLocalDate().equals(start.toLocalDate());

    if (!dateMatches) {
      return false;
    }

    // Time matching - check hour and minute only
    boolean timeMatches = event.getStartDateTime().getHour() == start.getHour() &&
        event.getStartDateTime().getMinute() == start.getMinute();

    return timeMatches;
  }

  private String removeQuotes(String text) {
    if (text.startsWith("\"") && text.endsWith("\"")) {
      return text.substring(1, text.length() - 1);
    }
    return text;
  }

  private boolean isEventActiveAt(Event e, LocalDateTime time) {
    if (e.isAllDay()) {
      return time.toLocalDate().equals(e.getStartDateTime().toLocalDate());
    }

    return !time.isBefore(e.getStartDateTime()) && !time.isAfter(e.getEndDateTime());
  }

  private boolean isEventOnDate(Event e, java.time.LocalDate date) {
    return e.getStartDateTime().toLocalDate().equals(date) ||
        (e.getEndDateTime() != null && e.getEndDateTime().toLocalDate().equals(date));
  }

  private LocalDateTime calculateRecurringEndTime(LocalDateTime date,
      LocalDateTime originalStart,
      LocalDateTime originalEnd) {
    long hoursDifference = originalEnd.getHour() - originalStart.getHour();
    long minutesDifference = originalEnd.getMinute() - originalStart.getMinute();

    return date.plusHours(hoursDifference).plusMinutes(minutesDifference);
  }

  private boolean hasConflicts(LocalDateTime start, LocalDateTime end, boolean allDay) {
    return events.stream().anyMatch(e -> checkEventConflict(e, start, end, allDay));
  }

  private boolean hasConflictsExcluding(LocalDateTime start, LocalDateTime end,
      boolean allDay, Event exclude) {
    return events.stream()
        .filter(e -> e != exclude)
        .anyMatch(e -> checkEventConflict(e, start, end, allDay));
  }

  private boolean checkEventConflict(Event e, LocalDateTime start,
                                     LocalDateTime end, boolean allDay) {
    if (e.isRecurring()) {
      return ((EventImpl) e).getRecurrence().calculateRecurrences(e.getStartDateTime()).stream()
          .anyMatch(d -> checkOccurrenceConflict(d, e, start, end, allDay));
    }

    if (allDay || e.isAllDay()) {
      return start.toLocalDate().equals(e.getStartDateTime().toLocalDate());
    }

    return start.isBefore(e.getEndDateTime()) && end.isAfter(e.getStartDateTime());
  }

  private boolean checkOccurrenceConflict(LocalDateTime occurrence, Event e,
      LocalDateTime start, LocalDateTime end, boolean allDay) {
    if (allDay || e.isAllDay()) {
      return occurrence.toLocalDate().equals(start.toLocalDate());
    }

    LocalDateTime occurrenceEnd = calculateRecurringEndTime(occurrence,
        e.getStartDateTime(), e.getEndDateTime());
    return start.isBefore(occurrenceEnd) && end.isAfter(occurrence);
  }

  private Event createOccurrenceEvent(Event template, LocalDateTime date) {
    EventImpl original = (EventImpl) template;
    EventImpl occ;

    if (template.isAllDay()) {
      occ = new EventImpl(original.getSubject(), date);
    } else {
      LocalDateTime occurrenceEnd = calculateRecurringEndTime(
          date, original.getStartDateTime(), original.getEndDateTime());
      occ = new EventImpl(original.getSubject(), date, occurrenceEnd);
    }

    occ.setDescription(original.getDescription());
    occ.setLocation(original.getLocation());
    occ.setPublic(original.isPublic());

    //    String subject = occ.getSubject();
    //    if (!subject.contains("[Recurring]")) {
    //      occ.setSubject(subject + " [Recurring]");
    //    }

    return occ;
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

  void clearEvents() {
    events.clear();
  }

  void addEvent(Event e) {
    events.add(e);
  }
}