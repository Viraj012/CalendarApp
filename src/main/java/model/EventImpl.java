package model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Implementation of the Event interface.
 */
public class EventImpl implements Event {
  private String subject;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;
  private String description;
  private String location;
  private boolean isPublic;
  private boolean isAllDay;
  private RecurrencePattern recurrence; // null for non-recurring events

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
    return !(this.endDateTime.isBefore(other.getStartDateTime()) ||
        this.startDateTime.isAfter(other.getEndDateTime()));
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
    return subject + " - " +
        (isAllDay ? "All Day" : startDateTime.toLocalTime() + " to " +
            (endDateTime != null ? endDateTime.toLocalTime() : "")) +
        (location.isEmpty() ? "" : " at " + location);
  }
}