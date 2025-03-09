package controller;

import java.time.LocalDateTime;

/**
 * Base class for all commands.
 */
public abstract class Command {
  // This is a marker interface to represent all commands
}

/**
 * Command object for Create Event commands.
 */
class CreateCommand extends Command {
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

/**
 * Command object for Edit Event commands.
 */
class EditCommand extends Command {
  public enum EditType {
    SINGLE,     // Edit a single event
    FROM_DATE,  // Edit events from a date
    ALL         // Edit all events
  }

  private final EditType editType;
  private String property;
  private String eventName;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;
  private String newValue;

  public EditCommand(EditType editType) {
    this.editType = editType;
  }

  public EditType getEditType() {
    return editType;
  }

  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

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

  public String getNewValue() {
    return newValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }
}

/**
 * Command object for Print Events commands.
 */
class PrintCommand extends Command {
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;
  private boolean isDateRange;

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

  public boolean isDateRange() {
    return isDateRange;
  }

  public void setDateRange(boolean isDateRange) {
    this.isDateRange = isDateRange;
  }
}

/**
 * Command object for Export Calendar commands.
 */
class ExportCommand extends Command {
  private String fileName;

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }
}

/**
 * Command object for Show Status commands.
 */
class ShowCommand extends Command {
  private LocalDateTime dateTime;

  public LocalDateTime getDateTime() {
    return dateTime;
  }

  public void setDateTime(LocalDateTime dateTime) {
    this.dateTime = dateTime;
  }
}