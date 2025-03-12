package controller;

import java.time.LocalDateTime;

/**
 * Command object for Edit Event commands.
 */
public class EditCommand extends Command {
  /**
   * Represents the type of edit operation for events.
   */
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
