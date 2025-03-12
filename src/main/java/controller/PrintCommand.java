package controller;

import java.time.LocalDateTime;

/**
 * Command object for Print Events commands.
 */
public class PrintCommand extends Command {
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
