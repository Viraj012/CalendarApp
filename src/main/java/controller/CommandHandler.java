package controller;

import model.Calendar;
import model.Event;
import view.TextUI;

import java.util.List;

/**
 * Handles the execution of commands on the calendar model.
 */
public class CommandHandler {
  private final Calendar calendar;
  private final TextUI view;

  /**
   * Creates a new command handler.
   *
   * @param calendar the calendar model
   * @param view the text UI view
   */
  public CommandHandler(Calendar calendar, TextUI view) {
    this.calendar = calendar;
    this.view = view;
  }

  /**
   * Handle a command by dispatching to the appropriate handler.
   *
   * @param command the command to handle
   */
  public void handleCommand(Command command) {
    if (command instanceof CreateCommand) {
      handleCreateCommand((CreateCommand) command);
    } else if (command instanceof EditCommand) {
      handleEditCommand((EditCommand) command);
    } else if (command instanceof PrintCommand) {
      handlePrintCommand((PrintCommand) command);
    } else if (command instanceof ExportCommand) {
      handleExportCommand((ExportCommand) command);
    } else if (command instanceof ShowCommand) {
      handleShowCommand((ShowCommand) command);
    }
  }

  /**
   * Handle a create command.
   *
   * @param cmd the create command
   */
  private void handleCreateCommand(CreateCommand cmd) {
    boolean success;

    if (cmd.isAllDay()) {
      success = createAllDayEvent(cmd);
    } else {
      success = createRegularEvent(cmd);
    }
    boolean isSingleEvent = !cmd.isRecurring() && !cmd.isAllDay();
    boolean isFirstItem = true;

    if (success) {

      StringBuilder message = new StringBuilder();

      if (cmd.isRecurring()) {
        message.append("Recurring ");
        isFirstItem = false;
      }

      if (cmd.isAllDay()) {
        message.append(isFirstItem ? "All-day " : "all-day ");
        isFirstItem = false;
      }

      message.append(isFirstItem || isSingleEvent ? "Event" : "event");
      message.append(" created successfully: ").append(cmd.getEventName());

      view.displayMessage(message.toString());
    } else {

      StringBuilder message = new StringBuilder("Failed to create ");

      if (cmd.isRecurring()) {
        message.append("recurring ");
        isFirstItem = false;
      }

      if (cmd.isAllDay()) {
        message.append(isFirstItem ? "All-day " : "all-day ");
        isFirstItem = false;
      }

      message.append(isFirstItem || isSingleEvent ? "Event" : "event");

      if (cmd.isRecurring() && !cmd.isAllDay()) {
        message.append(" (conflict detected or spans multiple days)");
      } else {
        message.append(" (conflict detected)");
      }

      view.displayError(message.toString());
    }
  }

  /**
   * Create an all-day event.
   *
   * @param cmd the create command
   * @return true if successful
   */
  private boolean createAllDayEvent(CreateCommand cmd) {
    if (cmd.isRecurring()) {
      return calendar.createRecurringAllDayEvent(
          cmd.getEventName(),
          cmd.getStartDateTime(),
          cmd.getWeekdays(),
          cmd.getOccurrences(),
          cmd.getUntilDate(),
          cmd.isAutoDecline(),
          cmd.getDescription(),
          cmd.getLocation(),
          cmd.isPublic()
      );
    } else {
      return calendar.createAllDayEvent(
          cmd.getEventName(),
          cmd.getStartDateTime(),
          cmd.isAutoDecline(),
          cmd.getDescription(),
          cmd.getLocation(),
          cmd.isPublic()
      );
    }
  }

  /**
   * Create a regular event.
   *
   * @param cmd the create command
   * @return true if successful
   */
  private boolean createRegularEvent(CreateCommand cmd) {
    if (cmd.isRecurring()) {
      return calendar.createRecurringEvent(
          cmd.getEventName(),
          cmd.getStartDateTime(),
          cmd.getEndDateTime(),
          cmd.getWeekdays(),
          cmd.getOccurrences(),
          cmd.getUntilDate(),
          cmd.isAutoDecline(),
          cmd.getDescription(),
          cmd.getLocation(),
          cmd.isPublic()
      );
    } else {
      return calendar.createEvent(
          cmd.getEventName(),
          cmd.getStartDateTime(),
          cmd.getEndDateTime(),
          cmd.isAutoDecline(),
          cmd.getDescription(),
          cmd.getLocation(),
          cmd.isPublic()
      );
    }
  }

  /**
   * Handle an edit command.
   *
   * @param cmd the edit command
   */
  private void handleEditCommand(EditCommand cmd) {
    boolean success = false;

    switch (cmd.getEditType()) {
      case SINGLE:
        success = calendar.editEvent(
            cmd.getProperty(),
            cmd.getEventName(),
            cmd.getStartDateTime(),
            cmd.getEndDateTime(),
            cmd.getNewValue()
        );

        if (success) {
          view.displayMessage("Event updated successfully");
        } else {
          view.displayError("Failed to update event (not found or invalid property)");
        }
        break;

      case FROM_DATE:
        success = calendar.editEventsFrom(
            cmd.getProperty(),
            cmd.getEventName(),
            cmd.getStartDateTime(),
            cmd.getNewValue()
        );

        if (success) {
          view.displayMessage("Events updated successfully");
        } else {
          view.displayError("Failed to update events (not found or invalid property)");
        }
        break;

      case ALL:
        success = calendar.editAllEvents(
            cmd.getProperty(),
            cmd.getEventName(),
            cmd.getNewValue()
        );

        if (success) {
          view.displayMessage("All events updated successfully");
        } else {
          view.displayError("Failed to update events (not found or invalid property)");
        }
        break;
    }
  }

  /**
   * Handle a print command.
   *
   * @param cmd the print command
   */
  private void handlePrintCommand(PrintCommand cmd) {
    List<Event> events;

    if (cmd.isDateRange()) {
      events = calendar.getEventsFrom(cmd.getStartDateTime(), cmd.getEndDateTime());

      if (events.isEmpty()) {
        view.displayMessage("No events from " + cmd.getStartDateTime().toLocalDate() +
            " to " + cmd.getEndDateTime().toLocalDate());
      } else {
        view.displayMessage("Events from " + cmd.getStartDateTime().toLocalDate() +
            " to " + cmd.getEndDateTime().toLocalDate() + ":");
        printEvents(events);
      }
    } else {
      events = calendar.getEventsOn(cmd.getStartDateTime());

      if (events.isEmpty()) {
        view.displayMessage("No events on " + cmd.getStartDateTime().toLocalDate());
      } else {
        view.displayMessage("Events on " + cmd.getStartDateTime().toLocalDate() + ":");
        printEvents(events);
      }
    }
  }

  /**
   * Print a list of events.
   *
   * @param events the events to print
   */
  private void printEvents(List<Event> events) {
    // Sort events by start time for better readability
    events.sort((e1, e2) -> {
      // First sort by date
      int dateCompare = e1.getStartDateTime().toLocalDate().compareTo(e2.getStartDateTime().toLocalDate());
      if (dateCompare != 0) {
        return dateCompare;
      }

      // Then sort all-day events before timed events
      if (e1.isAllDay() && !e2.isAllDay()) {
        return -1;
      } else if (!e1.isAllDay() && e2.isAllDay()) {
        return 1;
      }

      // Finally sort by start time for timed events
      return e1.getStartDateTime().compareTo(e2.getStartDateTime());
    });

    int eventNumber = 1;
    for (Event event : events) {
      view.displayMessage(eventNumber + ". " + event.toString());
      eventNumber++;
    }
  }

  /**
   * Handle an export command.
   *
   * @param cmd the export command
   */
  private void handleExportCommand(ExportCommand cmd) {
    String path = calendar.exportToCSV(cmd.getFileName());

    if (path != null) {
      view.displayMessage("Calendar exported to: " + path);
    } else {
      view.displayError("Failed to export calendar");
    }
  }

  /**
   * Handle a show command.
   *
   * @param cmd the show command
   */
  private void handleShowCommand(ShowCommand cmd) {
    boolean isBusy = calendar.isBusy(cmd.getDateTime());

    if (isBusy) {
      view.displayMessage("busy");
    } else {
      view.displayMessage("available");
    }
  }
}