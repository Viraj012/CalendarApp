package controller;

import java.time.LocalDateTime; /**
 * Command object for Show Status commands.
 */
public class ShowCommand extends Command {
  private LocalDateTime dateTime;

  public LocalDateTime getDateTime() {
    return dateTime;
  }

  public void setDateTime(LocalDateTime dateTime) {
    this.dateTime = dateTime;
  }
}
