package controller;

import java.time.LocalDateTime;

/**
 * Base class for all commands. Contains inner classes for specific command types.
 */
public abstract class Command {

  /**
   * Command object for Create Event commands.
   */
  static class CreateCommand extends Command {
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

    protected String getEventName() {
      return eventName;
    }

    protected void setEventName(String eventName) {
      this.eventName = eventName;
    }

    protected LocalDateTime getStartDateTime() {
      return startDateTime;
    }

    protected void setStartDateTime(LocalDateTime startDateTime) {
      this.startDateTime = startDateTime;
    }

    protected LocalDateTime getEndDateTime() {
      return endDateTime;
    }

    protected void setEndDateTime(LocalDateTime endDateTime) {
      this.endDateTime = endDateTime;
    }

    protected boolean isAutoDecline() {
      return autoDecline;
    }

    protected void setAutoDecline(boolean autoDecline) {
      this.autoDecline = autoDecline;
    }

    protected String getDescription() {
      return description;
    }

    protected void setDescription(String description) {
      this.description = description;
    }

    protected String getLocation() {
      return location;
    }

    protected void setLocation(String location) {
      this.location = location;
    }

    protected boolean isPublic() {
      return isPublic;
    }

    protected void setPublic(boolean isPublic) {
      this.isPublic = isPublic;
    }

    protected boolean isAllDay() {
      return isAllDay;
    }

    protected void setAllDay(boolean isAllDay) {
      this.isAllDay = isAllDay;
    }

    protected boolean isRecurring() {
      return isRecurring;
    }

    protected void setRecurring(boolean isRecurring) {
      this.isRecurring = isRecurring;
    }

    protected String getWeekdays() {
      return weekdays;
    }

    protected void setWeekdays(String weekdays) {
      this.weekdays = weekdays;
    }

    protected int getOccurrences() {
      return occurrences;
    }

    protected void setOccurrences(int occurrences) {
      this.occurrences = occurrences;
    }

    protected LocalDateTime getUntilDate() {
      return untilDate;
    }

    protected void setUntilDate(LocalDateTime untilDate) {
      this.untilDate = untilDate;
    }
  }

  /**
   * Command object for Edit Event commands.
   */
  static class EditCommand extends Command {
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

    EditCommand(EditType editType) {
      this.editType = editType;
    }

    protected EditType getEditType() {
      return editType;
    }

    protected String getProperty() {
      return property;
    }

    protected void setProperty(String property) {
      this.property = property;
    }

    protected String getEventName() {
      return eventName;
    }

    protected void setEventName(String eventName) {
      this.eventName = eventName;
    }

    protected LocalDateTime getStartDateTime() {
      return startDateTime;
    }

    protected void setStartDateTime(LocalDateTime startDateTime) {
      this.startDateTime = startDateTime;
    }

    protected LocalDateTime getEndDateTime() {
      return endDateTime;
    }

    protected void setEndDateTime(LocalDateTime endDateTime) {
      this.endDateTime = endDateTime;
    }

    protected String getNewValue() {
      return newValue;
    }

    protected void setNewValue(String newValue) {
      this.newValue = newValue;
    }
  }

  /**
   * Command object for Print Events commands.
   */
  static class PrintCommand extends Command {
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private boolean isDateRange;

    protected LocalDateTime getStartDateTime() {
      return startDateTime;
    }

    protected void setStartDateTime(LocalDateTime startDateTime) {
      this.startDateTime = startDateTime;
    }

    protected LocalDateTime getEndDateTime() {
      return endDateTime;
    }

    protected void setEndDateTime(LocalDateTime endDateTime) {
      this.endDateTime = endDateTime;
    }

    protected boolean isDateRange() {
      return isDateRange;
    }

    protected void setDateRange(boolean isDateRange) {
      this.isDateRange = isDateRange;
    }
  }

  /**
   * Command object for Export Calendar commands.
   */
  static class ExportCommand extends Command {
    private String fileName;

    protected String getFileName() {
      return fileName;
    }

    protected void setFileName(String fileName) {
      this.fileName = fileName;
    }
  }

  /**
   * Command object for Show Status commands.
   */
  static class ShowCommand extends Command {
    private LocalDateTime dateTime;

    protected LocalDateTime getDateTime() {
      return dateTime;
    }

    protected void setDateTime(LocalDateTime dateTime) {
      this.dateTime = dateTime;
    }
  }
}