package controller;

import java.time.LocalDateTime;

/**
 * Command object for Create Event commands.
 */
public class CreateCommand extends Command {
  private String eventName;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;
  private boolean autoDecline;
  private String description;
  private String location;
  private boolean isPublic;
  private boolean isAllDay;
  private boolean isRecurring;
  private String weekdays;
  private int occurrences = -1;
  private LocalDateTime untilDate;

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public LocalDateTime getStartDateTime() {
    return startDateTime;
  }

  public void setStartDateTime(LocalDateTime startDateTime) {
    this.startDateTime = startDateTime;
  }

  public LocalDateTime getEndDateTime() {
    return endDateTime;
  }

  public void setEndDateTime(LocalDateTime endDateTime) {
    this.endDateTime = endDateTime;
  }

  public boolean isAutoDecline() {
    return autoDecline;
  }

  public void setAutoDecline(boolean autoDecline) {
    this.autoDecline = autoDecline;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public void setPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

  public boolean isAllDay() {
    return isAllDay;
  }

  public void setAllDay(boolean isAllDay) {
    this.isAllDay = isAllDay;
  }

  public boolean isRecurring() {
    return isRecurring;
  }

  public void setRecurring(boolean isRecurring) {
    this.isRecurring = isRecurring;
  }

  public String getWeekdays() {
    return weekdays;
  }

  public void setWeekdays(String weekdays) {
    this.weekdays = weekdays;
  }

  public int getOccurrences() {
    return occurrences;
  }

  public void setOccurrences(int occurrences) {
    this.occurrences = occurrences;
  }

  public LocalDateTime getUntilDate() {
    return untilDate;
  }

  public void setUntilDate(LocalDateTime untilDate) {
    this.untilDate = untilDate;
  }
}
