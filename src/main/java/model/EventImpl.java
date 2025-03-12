package model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Implementation of the Event interface.
 */
public class EventImpl implements Event {
  private String subject;
  private final LocalDateTime startDateTime;
  private final LocalDateTime endDateTime;
  private String description;
  private String location;
  private boolean isPublic;
  private final boolean isAllDay;
  private final RecurrencePattern recurrence; // null for non-recurring events

  /**
   * Constructor for a single event.
   *
   * @param subject the event name/subject
   * @param startDateTime the start date and time
   * @param endDateTime the end date and time
   */
  public EventImpl(String subject, LocalDateTime startDateTime, LocalDateTime endDateTime) {
    this.subject = subject;
    this.startDateTime = startDateTime;
    this.endDateTime = endDateTime;
    this.description = "";
    this.location = "";
    this.isPublic = true;
    this.isAllDay = false;
    this.recurrence = null;
  }

  /**
   * Constructor for an all-day event.
   *
   * @param subject the event name/subject
   * @param dateTime the date of the all-day event
   */
  public EventImpl(String subject, LocalDateTime dateTime) {
    this.subject = subject;
    this.startDateTime = dateTime;
    this.endDateTime = null; // For all-day events, end time is null
    this.description = "";
    this.location = "";
    this.isPublic = true;
    this.isAllDay = true;
    this.recurrence = null;
  }

  /**
   * Constructor for a recurring event.
   *
   * @param subject the event name/subject
   * @param startDateTime the start date and time
   * @param endDateTime the end date and time
   * @param weekdays the days of week the event repeats on
   * @param occurrences the number of occurrences (-1 if using untilDate)
   * @param untilDate the end date for recurrence (null if using occurrences)
   */
  public EventImpl(String subject, LocalDateTime startDateTime, LocalDateTime endDateTime,
      String weekdays, int occurrences, LocalDateTime untilDate) {
    this.subject = subject;
    this.startDateTime = startDateTime;
    this.endDateTime = endDateTime;
    this.description = "";
    this.location = "";
    this.isPublic = true;
    this.isAllDay = false;
    this.recurrence = new RecurrencePattern(weekdays, occurrences, untilDate);
  }

  /**
   * Constructor for a recurring all-day event.
   *
   * @param subject the event name/subject
   * @param dateTime the date of the all-day event
   * @param weekdays the days of week the event repeats on
   * @param occurrences the number of occurrences (-1 if using untilDate)
   * @param untilDate the end date for recurrence (null if using occurrences)
   */
  public EventImpl(String subject, LocalDateTime dateTime, String weekdays,
      int occurrences, LocalDateTime untilDate) {
    this.subject = subject;
    this.startDateTime = dateTime;
    this.endDateTime = null; // For all-day events, end time is null
    this.description = "";
    this.location = "";
    this.isPublic = true;
    this.isAllDay = true;
    this.recurrence = new RecurrencePattern(weekdays, occurrences, untilDate);
  }

  @Override
  public String getSubject() {
    return subject;
  }

  /**
   * Sets the subject of the event.
   *
   * @param subject the new subject
   */
  public void setSubject(String subject) {
    this.subject = subject;
  }

  @Override
  public LocalDateTime getStartDateTime() {
    return startDateTime;
  }

  @Override
  public LocalDateTime getEndDateTime() {
    return endDateTime;
  }

  @Override
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of the event.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getLocation() {
    return location;
  }

  /**
   * Sets the location of the event.
   *
   * @param location the new location
   */
  public void setLocation(String location) {
    this.location = location;
  }

  @Override
  public boolean isPublic() {
    return isPublic;
  }

  /**
   * Sets whether the event is public.
   *
   * @param isPublic true if public, false if private
   */
  public void setPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

  @Override
  public boolean isAllDay() {
    return isAllDay;
  }

  @Override
  public boolean isRecurring() {
    return recurrence != null;
  }

  /**
   * Gets the recurrence pattern of the event.
   *
   * @return the recurrence pattern, or null if not recurring
   */
  public RecurrencePattern getRecurrence() {
    return recurrence;
  }

  @Override
  public boolean conflictsWith(Event other) {
    // All-day events conflict with any event on that day
    if (this.isAllDay || other.isAllDay()) {
      return this.startDateTime.toLocalDate().equals(other.getStartDateTime().toLocalDate());
    }

    // Check if time intervals overlap
    return !(this.endDateTime.compareTo(other.getStartDateTime()) <= 0 ||
        this.startDateTime.compareTo(other.getEndDateTime()) >= 0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EventImpl event = (EventImpl) o;
    return Objects.equals(subject, event.subject) &&
        Objects.equals(startDateTime, event.startDateTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, startDateTime);
  }

  @Override
  public String toString() {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    StringBuilder sb = new StringBuilder();

    sb.append(subject).append(" - ");

    if (isAllDay) {
      // All-day event format
      sb.append(startDateTime.format(dateFormatter)).append(" All Day");
    } else if (endDateTime != null) {
      // Check if the event spans multiple days
      boolean isMultiDayEvent = !startDateTime.toLocalDate().equals(endDateTime.toLocalDate());

      if (isMultiDayEvent) {
        // For multi-day events, show both start and end dates with times
        sb.append(startDateTime.format(dateFormatter))
            .append(" ")
            .append(startDateTime.format(timeFormatter))
            .append(" to ")
            .append(endDateTime.format(dateFormatter))
            .append(" ")
            .append(endDateTime.format(timeFormatter));
      } else {
        // For same-day events, show only one date with start and end times
        sb.append(startDateTime.format(dateFormatter))
            .append(" ")
            .append(startDateTime.format(timeFormatter))
            .append(" to ")
            .append(endDateTime.format(timeFormatter));
      }
    } else {
      // Fallback for unexpected cases
      sb.append(startDateTime.format(dateFormatter));
    }

    // Add location if available
    if (!location.isEmpty()) {
      sb.append(" at ").append(location);
    }

    // Add recurrence information
    if (isRecurring()) {
      sb.append(" (Repeats on: ");

      // Convert weekday codes to readable format
      if (recurrence.getWeekdays().contains(DayOfWeek.MONDAY)) sb.append("Mon,");
      if (recurrence.getWeekdays().contains(DayOfWeek.TUESDAY)) sb.append("Tue,");
      if (recurrence.getWeekdays().contains(DayOfWeek.WEDNESDAY)) sb.append("Wed,");
      if (recurrence.getWeekdays().contains(DayOfWeek.THURSDAY)) sb.append("Thu,");
      if (recurrence.getWeekdays().contains(DayOfWeek.FRIDAY)) sb.append("Fri,");
      if (recurrence.getWeekdays().contains(DayOfWeek.SATURDAY)) sb.append("Sat,");
      if (recurrence.getWeekdays().contains(DayOfWeek.SUNDAY)) sb.append("Sun,");

      // Remove trailing comma
      if (sb.toString().endsWith(",")) {
        sb.deleteCharAt(sb.length() - 1);
      }

      if (recurrence.getOccurrences() != -1) {
        sb.append(" for ").append(recurrence.getOccurrences()).append(" times");
      } else if (recurrence.getUntilDate() != null) {
        sb.append(" until ").append(recurrence.getUntilDate().format(dateFormatter));
      }

      sb.append(")");
    }

    return sb.toString();
  }
}