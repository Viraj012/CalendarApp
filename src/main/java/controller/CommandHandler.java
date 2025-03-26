package controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import model.Calendar;
import model.CalendarManager;
import model.Event;
import model.EventImpl;
import model.RecurrencePattern;
import view.TextUI;

import java.util.Comparator;
import java.util.List;

/**
 * Handles the execution of commands on the calendar model.
 */
class CommandHandler {
  private final CalendarManager calendarManager;
  private final TextUI view;

  /**
   * Creates a new command handler.
   *
   * @param calendarManager the calendar manager
   * @param view            the text UI view
   */
  CommandHandler(CalendarManager calendarManager, TextUI view) {
    this.calendarManager = calendarManager;
    this.view = view;
  }

  /**
   * Handle a command by dispatching to the appropriate handler.
   *
   * @param command the command to handle
   */
  void handleCommand(Command command) {
    // Handle calendar-level commands
    if (command instanceof Command.CreateCalendarCommand) {
      handleCreateCalendarCommand((Command.CreateCalendarCommand) command);
      return;
    } else if (command instanceof Command.EditCalendarCommand) {
      handleEditCalendarCommand((Command.EditCalendarCommand) command);
      return;
    } else if (command instanceof Command.UseCalendarCommand) {
      handleUseCalendarCommand((Command.UseCalendarCommand) command);
      return;
    } else if (command instanceof Command.CopyEventCommand) {
      handleCopyEventCommand((Command.CopyEventCommand) command);
      return;
    } else if (command instanceof Command.CopyEventsCommand) {
      handleCopyEventsCommand((Command.CopyEventsCommand) command);
      return;
    }

    // Get current calendar for event-level commands
    Calendar calendar = calendarManager.getCurrentCalendar();
    if (calendar == null) {
      view.displayError("No calendar in use. Please use a calendar first.");
      return;
    }

    if (command instanceof Command.CreateCommand) {
      handleCreateCommand((Command.CreateCommand) command, calendar);
    } else if (command instanceof Command.EditCommand) {
      handleEditCommand((Command.EditCommand) command, calendar);
    } else if (command instanceof Command.PrintCommand) {
      handlePrintCommand((Command.PrintCommand) command, calendar);
    } else if (command instanceof Command.ExportCommand) {
      handleExportCommand((Command.ExportCommand) command, calendar);
    } else if (command instanceof Command.ShowCommand) {
      handleShowCommand((Command.ShowCommand) command, calendar);
    }
  }

  /**
   * Handle a create calendar command.
   *
   * @param cmd the create calendar command
   */
  private void handleCreateCalendarCommand(Command.CreateCalendarCommand cmd) {
    boolean success = calendarManager.createCalendar(cmd.getName(), cmd.getTimezone());

    if (success) {
      view.displayMessage("Calendar created: " + cmd.getName() + " (" + cmd.getTimezone() + ")");
    } else {
      view.displayError("Failed to create calendar (name already exists)");
    }
  }

  /**
   * Handle an edit calendar command.
   *
   * @param cmd the edit calendar command
   */
  private void handleEditCalendarCommand(Command.EditCalendarCommand cmd) {
    boolean success
            = calendarManager.editCalendar(
            cmd.getName(), cmd.getProperty(), cmd.getNewValue()
    );

    if (success) {
      view.displayMessage("Calendar updated successfully");
    } else {
      view.displayError("Failed to update calendar (invalid name, property, or value)");
    }
  }

  /**
   * Handle a use calendar command.
   *
   * @param cmd the use calendar command
   */
  private void handleUseCalendarCommand(Command.UseCalendarCommand cmd) {
    boolean success = calendarManager.useCalendar(cmd.getName());

    if (success) {
      view.displayMessage("Now using calendar: " + cmd.getName());
    } else {
      view.displayError("Calendar not found: " + cmd.getName());
    }
  }

  /**
   * Handle a create command.
   *
   * @param cmd      the create command
   * @param calendar the current calendar
   */
  private void handleCreateCommand(Command.CreateCommand cmd, Calendar calendar) {
    boolean success = createEvent(cmd, calendar);
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
      isFirstItem = true;

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
   * Create an event based on command parameters.
   *
   * @param cmd      the create command
   * @param calendar the calendar to create the event in
   * @return true if successful
   */
  private boolean createEvent(Command.CreateCommand cmd, Calendar calendar) {
    if (cmd.isAllDay()) {
      if (cmd.isRecurring()) {
        return calendar.createRecurringAllDayEvent(
                cmd.getEventName(),
                cmd.getStartDateTime(),
                cmd.getWeekdays(),
                cmd.getOccurrences(),
                cmd.getUntilDate(),
                cmd.isAutoDecline(), // Keep autoDecline parameter
                cmd.getDescription(),
                cmd.getLocation(),
                cmd.isPublic()
        );
      } else {
        return calendar.createAllDayEvent(
                cmd.getEventName(),
                cmd.getStartDateTime(),
                cmd.isAutoDecline(), // Keep autoDecline parameter
                cmd.getDescription(),
                cmd.getLocation(),
                cmd.isPublic()
        );
      }
    } else {
      if (cmd.isRecurring()) {
        return calendar.createRecurringEvent(
                cmd.getEventName(),
                cmd.getStartDateTime(),
                cmd.getEndDateTime(),
                cmd.getWeekdays(),
                cmd.getOccurrences(),
                cmd.getUntilDate(),
                cmd.isAutoDecline(), // Keep autoDecline parameter
                cmd.getDescription(),
                cmd.getLocation(),
                cmd.isPublic()
        );
      } else {
        return calendar.createEvent(
                cmd.getEventName(),
                cmd.getStartDateTime(),
                cmd.getEndDateTime(),
                cmd.isAutoDecline(), // Keep autoDecline parameter
                cmd.getDescription(),
                cmd.getLocation(),
                cmd.isPublic()
        );
      }
    }
  }

  /**
   * Handle an edit command.
   *
   * @param cmd      the edit command
   * @param calendar the current calendar
   */
  private void handleEditCommand(Command.EditCommand cmd, Calendar calendar) {
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
          view.displayError("Failed to update event " +
                  "(not found, invalid property, or would create conflict)");
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
          view.displayError("Failed to update events " +
                  "(not found, invalid property, or would create conflict)");
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
          view.displayError("Failed to update events " +
                  "(not found, invalid property, or would create conflict)");
        }
        break;

      default:
        break;
    }
  }

  /**
   * Handle a print command.
   *
   * @param cmd      the print command
   * @param calendar the current calendar
   */
  private void handlePrintCommand(Command.PrintCommand cmd, Calendar calendar) {
    List<Event> events;
    String dateDescription;

    if (cmd.isDateRange()) {
      events = calendar.getEventsFrom(cmd.getStartDateTime(), cmd.getEndDateTime());
      dateDescription = "from " + cmd.getStartDateTime().toLocalDate() +
              " to " + cmd.getEndDateTime().toLocalDate();
    } else {
      events = calendar.getEventsOn(cmd.getStartDateTime());
      dateDescription = "on " + cmd.getStartDateTime().toLocalDate();
    }

    if (events.isEmpty()) {
      view.displayMessage("No events " + dateDescription);
    } else {
      view.displayMessage("Events " + dateDescription + ":");
      printEvents(events);
    }
  }

  /**
   * Print a list of events.
   *
   * @param events the events to print
   */
  private void printEvents(List<Event> events) {
    // Sort events by start date and time
    events.sort(Comparator.comparing(Event::getStartDateTime));

    int eventNumber = 1;
    for (Event event : events) {
      view.displayMessage(eventNumber + ". " + event.toString());
      eventNumber++;
    }
  }


  /**
   * Handle an export command.
   *
   * @param cmd      the export command
   * @param calendar the current calendar
   */
  private void handleExportCommand(Command.ExportCommand cmd, Calendar calendar) {
    List<Event> events = calendar.getAllEvents();
    String path = exportToCSV(cmd.getFileName(), events, calendar);

    if (path != null) {
      view.displayMessage("Calendar exported to: " + path);
    } else {
      view.displayError("Failed to export calendar");
    }
  }

  /**
   * Exports calendar events to a CSV file.
   *
   * @param fileName the name of the file to export to
   * @param events   the list of events to export
   * @param calendar the calendar to export from
   * @return the absolute path of the generated CSV file, or null if export failed
   */
  private String exportToCSV(String fileName, List<Event> events, Calendar calendar) {
    File file = new File(fileName);

    try (FileWriter writer = new FileWriter(file)) {
      // Write CSV header
      writer.write("Subject,Start Date,Start Time,End Date,End Time," +
              "All Day Event,Description,Location,Private,Calendar,Timezone\n");

      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
      DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

      for (Event event : events) {
        List<LocalDateTime> occurrences = event.isRecurring()
                ? ((EventImpl) event).getRecurrence().calculateRecurrences(event.getStartDateTime())
                : List.of(event.getStartDateTime());

        for (LocalDateTime occurrence : occurrences) {
          LocalDateTime occurrenceEndTime = (event.isAllDay() || event.getEndDateTime() == null)
                  ? null
                  : occurrence.plus(Duration.between(event.getStartDateTime(), event.getEndDateTime()));

          writeSingleEventToCSV(writer, event, occurrence, occurrenceEndTime, dateFormatter, timeFormatter, calendar);
        }
      }

      return file.getAbsolutePath();
    } catch (IOException e) {
      return null;
    }
  }


  /**
   * Helper method to write a single event entry to the CSV file.
   */
  private void writeSingleEventToCSV(FileWriter writer, Event event,
                                     LocalDateTime startDateTime,
                                     LocalDateTime endDateTime,
                                     DateTimeFormatter dateFormatter,
                                     DateTimeFormatter timeFormatter,
                                     Calendar calendar) throws IOException {

    StringBuilder line = new StringBuilder();

    line.append(escapeCSV(event.getSubject())).append(",");
    line.append(startDateTime.format(dateFormatter)).append(",");

    if (event.isAllDay()) {
      line.append(",");
    } else {
      line.append(startDateTime.format(timeFormatter)).append(",");
    }

    if (endDateTime == null) {
      line.append(startDateTime.format(dateFormatter)).append(",");
    } else {
      line.append(endDateTime.format(dateFormatter)).append(",");
    }

    if (event.isAllDay() || endDateTime == null) {
      line.append(",");
    } else {
      line.append(endDateTime.format(timeFormatter)).append(",");
    }

    line.append(event.isAllDay() ? "True" : "False").append(",");
    line.append(escapeCSV(event.getDescription())).append(",");
    line.append(escapeCSV(event.getLocation())).append(",");
    line.append(!event.isPublic() ? "True" : "False").append(",");

    // Add calendar name and timezone
    line.append(escapeCSV(calendar.getName())).append(",");
    line.append(escapeCSV(calendar.getTimezone().toString()));

    line.append("\n");

    writer.write(line.toString());
  }

  /**
   * Helper method to escape CSV fields.
   */
  private String escapeCSV(String field) {
    if (field == null) {
      return "";
    }

    if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
      return "\"" + field.replace("\"", "\"\"") + "\"";
    }

    return field;
  }


  /**
   * Handle a show command.
   *
   * @param cmd      the show command
   * @param calendar the current calendar
   */
  private void handleShowCommand(Command.ShowCommand cmd, Calendar calendar) {
    boolean isBusy = calendar.isBusy(cmd.getDateTime());

    if (isBusy) {
      view.displayMessage("busy");
    } else {
      view.displayMessage("available");
    }
  }

  /**
   * Handle a copy event command.
   *
   * @param cmd the copy event command
   */
  private void handleCopyEventCommand(Command.CopyEventCommand cmd) {
    boolean success = calendarManager.copyEvent(
            cmd.getEventName(),
            cmd.getStartDateTime(),
            cmd.getTargetCalendar(),
            cmd.getTargetDateTime()
    );

    if (success) {
      view.displayMessage("Event copied successfully to " + cmd.getTargetCalendar());
    } else {
      view.displayError("Failed to copy event " +
              "(event not found, target calendar not found, or would create conflict)");
    }
  }

  /**
   * Handle a copy events command.
   *
   * @param cmd the copy events command
   */
  private void handleCopyEventsCommand(Command.CopyEventsCommand cmd) {
    boolean success;

    if (cmd.getCopyType() == Command.CopyEventsCommand.CopyType.DAY) {
      success = calendarManager.copyEventsOnDay(
              cmd.getStartDate(),
              cmd.getTargetCalendar(),
              cmd.getTargetDate()
      );

      if (success) {
        view.displayMessage("Events copied successfully to " + cmd.getTargetCalendar());
      } else {
        view.displayError("Failed to copy events (no events found on that day, " +
                "target calendar not found, or would create conflicts)");
      }
    } else { // DATE_RANGE
      success = calendarManager.copyEventsInRange(
              cmd.getStartDate(),
              cmd.getEndDate(),
              cmd.getTargetCalendar(),
              cmd.getTargetDate()
      );

      if (success) {
        view.displayMessage("Events copied successfully to " + cmd.getTargetCalendar());
      } else {
        view.displayError("Failed to copy events (no events found in that range, " +
                "target calendar not found, or would create conflicts)");
      }
    }
  }
}