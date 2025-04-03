package controller;

import model.Calendar;
import model.CalendarManager;
import view.TextUI;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Processes user commands and interacts with the Calendar model.
 * Also provides date-time parsing functionality for the rest of the application.
 */
public class CommandProcessor {

  private final CalendarManager calendarManager;
  private final TextUI view;
  private final CommandParser parser;
  private final CommandHandler handler;

  /**
   * Creates a new command processor with a Calendar model.
   *
   * @param calendar the calendar model
   * @param view     the text UI view
   */
  public CommandProcessor(Calendar calendar, TextUI view) {
    this(new CalendarManager(), view);

    // Create a default calendar with the provided calendar
    String defaultName = calendar.getName() != null ? calendar.getName() : "Default";
    calendarManager.createCalendar(defaultName, calendar.getTimezone());
    calendarManager.useCalendar(defaultName);
  }

  /**
   * Creates a new command processor with a CalendarManager.
   *
   * @param calendarManager the calendar manager
   * @param view            the text UI view
   */
  public CommandProcessor(CalendarManager calendarManager, TextUI view) {
    this.calendarManager = calendarManager;
    this.view = view;
    this.parser = new CommandParser();
    this.handler = new CommandHandler(calendarManager, view);
  }

  /**
   * Process a command from the user.
   *
   * @param command the command to process
   * @return true if the application should continue running, false to exit
   */
  public boolean processCommand(String command) {
    String[] parts = command.trim().split("\\s+");

    if (parts.length == 0 || (parts.length == 1 && parts[0].isEmpty())) {
      view.displayError("Empty command");
      return true;
    }

    try {
      switch (parts[0].toLowerCase()) {
        case "create":
          if (parts.length < 2) {
            view.displayError("Invalid create command. " +
                    "Expected 'create event' or 'create calendar'");
            return true;
          }

          if (parts[1].equals("calendar")) {
            handleCreateCalendar(command);
            return true;
          } else if (parts[1].equals("event")) {
            handleCreate(command);
            return true;
          } else {
            view.displayError("Invalid create command. " +
                    "Expected 'create event' or 'create calendar'");
            return true;
          }

        case "edit":
          if (parts.length < 2) {
            view.displayError("Invalid edit command. " +
                    "Expected 'edit event', 'edit events', or 'edit calendar'");
            return true;
          }

          if (parts[1].equals("calendar")) {
            handleEditCalendar(command);
            return true;
          } else if (parts[1].equals("event") || parts[1].equals("events")) {
            handleEdit(command);
            return true;
          } else {
            view.displayError("Invalid edit command. Expected 'edit event'" +
                    ", 'edit events', or 'edit calendar'");
            return true;
          }

        case "use":
          if (parts.length < 2 || !parts[1].equals("calendar")) {
            view.displayError("Invalid use command. Expected 'use calendar'");
            return true;
          }
          handleUseCalendar(command);
          return true;

        case "print":
          if (parts.length < 2 || !parts[1].equals("events")) {
            view.displayError("Invalid print command. Expected 'print events'");
            return true;
          }

          handlePrint(command);
          return true;

        case "export":
          if (parts.length < 2 || !parts[1].equals("cal")) {
            view.displayError("Invalid export command. Expected 'export cal'");
            return true;
          }

          handleExport(command);
          return true;

        case "show":
          if (parts.length < 2 || !parts[1].equals("status")) {
            view.displayError("Invalid show command. Expected 'show status'");
            return true;
          }

          handleShow(command);
          return true;

        case "copy":
          if (parts.length < 2) {
            view.displayError("Invalid copy command. " +
                    "Expected 'copy event' or 'copy events'");
            return true;
          }


          if (parts[1].equals("event")) {
            handleCopyEvent(command);
            return true;
          } else if (parts[1].equals("events")) {
            handleCopyEvents(command);
            return true;
          } else {
            view.displayError("Invalid copy command. " +
                    "Expected 'copy event' or 'copy events'");
            return true;
          }

        case "import":
          handleImport(command);
          return true;

        case "exit":
          return false;



        default:
          view.displayError("Unknown command: " + parts[0]);
          return true;
      }
    } catch (Exception e) {
      view.displayError("Error processing command: " + e.getMessage());
      return true;
    }
  }

  /**
   * Parse a date-time string in the format "yyyy-MM-ddThh:mm" or "yyyy-MM-dd".
   * This functionality is made public for use by other packages.
   *
   * @param dateTimeStr the date-time string
   * @return the parsed LocalDateTime
   * @throws DateTimeParseException if the string cannot be parsed
   */
  public static LocalDateTime parseDateTime(String dateTimeStr) {
    if (dateTimeStr.contains("T")) {
      String[] parts = dateTimeStr.split("T");
      if (parts.length != 2) {
        throw new DateTimeParseException("Invalid date-time format", dateTimeStr, 0);
      }

      LocalDate date = LocalDate.parse(parts[0]);
      LocalTime time = LocalTime.parse(parts[1]);

      return LocalDateTime.of(date, time);
    } else {
      LocalDate date = LocalDate.parse(dateTimeStr);
      return LocalDateTime.of(date, LocalTime.MIDNIGHT);
    }
  }

  /**
   * Handle create event commands.
   *
   * @param command the original command string
   */
  private void handleCreate(String command) {
    Command createCommand = parser.parseCreateCommand(command);
    if (createCommand != null) {
      handler.handleCommand(createCommand);
    } else {
      view.displayError("Invalid create event command format");
    }
  }

  /**
   * Handle edit commands.
   *
   * @param command the original command string
   */
  private void handleEdit(String command) {
    Command editCommand = parser.parseEditCommand(command);
    if (editCommand != null) {
      handler.handleCommand(editCommand);
    } else {
      view.displayError("Invalid edit command format");
    }
  }

  /**
   * Handle print commands.
   *
   * @param command the original command string
   */
  private void handlePrint(String command) {
    Command printCommand = parser.parsePrintCommand(command);
    if (printCommand != null) {
      handler.handleCommand(printCommand);
    } else {
      view.displayError("Invalid print command format");
    }
  }

  /**
   * Handle export commands.
   *
   * @param command the original command string
   */
  private void handleExport(String command) {
    Command exportCommand = parser.parseExportCommand(command);
    if (exportCommand != null) {
      handler.handleCommand(exportCommand);
    } else {
      view.displayError("Invalid export command format");
    }
  }

  /**
   * Handle show status commands.
   *
   * @param command the original command string
   */
  private void handleShow(String command) {
    Command showCommand = parser.parseShowCommand(command);
    if (showCommand != null) {
      handler.handleCommand(showCommand);
    } else {
      view.displayError("Invalid show status command");
    }
  }

  /**
   * Handle create calendar commands.
   *
   * @param command the original command string
   */
  private void handleCreateCalendar(String command) {
    Command createCalendarCommand = parser.parseCreateCalendarCommand(command);
    if (createCalendarCommand != null) {
      handler.handleCommand(createCalendarCommand);
    } else {
      view.displayError("Invalid create calendar command format. " +
              "Expected: create calendar --name <calName> --timezone area/location");
    }
  }

  /**
   * Handle edit calendar commands.
   *
   * @param command the original command string
   */
  private void handleEditCalendar(String command) {
    Command editCalendarCommand = parser.parseEditCalendarCommand(command);
    if (editCalendarCommand != null) {
      handler.handleCommand(editCalendarCommand);
    } else {
      view.displayError("Invalid edit calendar command format. " +
              "Expected: edit calendar --name <name-of-calendar> " +
              "--property <property-name> <new-property-value>");
    }
  }

  /**
   * Handle use calendar commands.
   *
   * @param command the original command string
   */
  private void handleUseCalendar(String command) {
    Command useCalendarCommand = parser.parseUseCalendarCommand(command);
    if (useCalendarCommand != null) {
      handler.handleCommand(useCalendarCommand);
    } else {
      view.displayError("Invalid use calendar command format. " +
              "Expected: use calendar --name <name-of-calendar>");
    }
  }

  /**
   * Handle copy event commands.
   *
   * @param command the original command string
   */
  private void handleCopyEvent(String command) {
    Command copyEventCommand = parser.parseCopyEventCommand(command);
    if (copyEventCommand != null) {
      handler.handleCommand(copyEventCommand);
    } else {
      view.displayError("Invalid copy event command format. " +
              "Expected: copy event <eventName> on <dateStringTtimeString> " +
              "--target <calendarName> to <dateStringTtimeString>");
    }
  }

  /**
   * Handle copy events commands.
   *
   * @param command the original command string
   */
  private void handleCopyEvents(String command) {
    Command copyEventsCommand = parser.parseCopyEventsCommand(command);
    if (copyEventsCommand != null) {
      handler.handleCommand(copyEventsCommand);
    } else {
      view.displayError("Invalid copy events command format. " +
              "Expected: copy events on <dateString> --target <calendarName> to <dateString> " +
              "OR copy events between <dateString> and <dateString> " +
              "--target <calendarName> to <dateString>");
    }
  }

  /**
   * Handle import command.
   *
   * @param command the import command string
   */
  private void handleImport(String command) {
    String[] parts = command.split("\\s+", 2);

    if (parts.length < 2) {
      view.displayError("Invalid import command. Expected: import file.csv");
      return;
    }

    String filePath = parts[1].trim();

    if (calendarManager.getCurrentCalendar() == null) {
      view.displayError("No calendar selected. Please use a calendar first.");
      return;
    }

    int count = CSVImporter.importFromCSV(
        filePath,
        calendarManager,
        calendarManager.getCurrentCalendar().getName()
    );

    if (count >= 0) {
      view.displayMessage("Successfully imported " + count + " events.");
    } else {
      view.displayError("Failed to import events from " + filePath);
    }
  }
}