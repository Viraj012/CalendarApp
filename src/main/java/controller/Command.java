package controller;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Base class for all commands. Contains inner classes for specific command types.
 */
abstract class Command {

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

  /**
   * Command object for Create Calendar commands.
   */
  static class CreateCalendarCommand extends Command {
    private String name;
    private ZoneId timezone;

    protected String getName() {
      return name;
    }

    protected void setName(String name) {
      this.name = name;
    }

    protected ZoneId getTimezone() {
      return timezone;
    }

    protected void setTimezone(ZoneId timezone) {
      this.timezone = timezone;
    }
  }

  /**
   * Command object for Edit Calendar commands.
   */
  static class EditCalendarCommand extends Command {
    private String name;
    private String property;
    private String newValue;

    protected String getName() {
      return name;
    }

    protected void setName(String name) {
      this.name = name;
    }

    protected String getProperty() {
      return property;
    }

    protected void setProperty(String property) {
      this.property = property;
    }

    protected String getNewValue() {
      return newValue;
    }

    protected void setNewValue(String newValue) {
      this.newValue = newValue;
    }
  }

  /**
   * Command object for Use Calendar commands.
   */
  static class UseCalendarCommand extends Command {
    private String name;

    protected String getName() {
      return name;
    }

    protected void setName(String name) {
      this.name = name;
    }
  }

  /**
   * Command object for Copy Event commands.
   */
  static class CopyEventCommand extends Command {
    private String eventName;
    private LocalDateTime startDateTime;
    private String targetCalendar;
    private LocalDateTime targetDateTime;

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

    protected String getTargetCalendar() {
      return targetCalendar;
    }

    protected void setTargetCalendar(String targetCalendar) {
      this.targetCalendar = targetCalendar;
    }

    protected LocalDateTime getTargetDateTime() {
      return targetDateTime;
    }

    protected void setTargetDateTime(LocalDateTime targetDateTime) {
      this.targetDateTime = targetDateTime;
    }
  }

  /**
   * Command object for Copy Events commands.
   */
  static class CopyEventsCommand extends Command {
    public enum CopyType {
      DAY,        // Copy events on a specific day
      DATE_RANGE  // Copy events in a date range
    }

    private final CopyType copyType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;  // Only for DATE_RANGE type
    private String targetCalendar;
    private LocalDateTime targetDate;

    CopyEventsCommand(CopyType copyType) {
      this.copyType = copyType;
    }

    protected CopyType getCopyType() {
      return copyType;
    }

    protected LocalDateTime getStartDate() {
      return startDate;
    }

    protected void setStartDate(LocalDateTime startDate) {
      this.startDate = startDate;
    }

    protected LocalDateTime getEndDate() {
      return endDate;
    }

    protected void setEndDate(LocalDateTime endDate) {
      this.endDate = endDate;
    }

    protected String getTargetCalendar() {
      return targetCalendar;
    }

    protected void setTargetCalendar(String targetCalendar) {
      this.targetCalendar = targetCalendar;
    }

    protected LocalDateTime getTargetDate() {
      return targetDate;
    }

    protected void setTargetDate(LocalDateTime targetDate) {
      this.targetDate = targetDate;
    }
  }
}