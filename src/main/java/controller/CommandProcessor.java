package controller;

import model.Calendar;
import view.TextUI;

/**
 * Processes user commands and interacts with the Calendar model.
 */
public class CommandProcessor {
  private final Calendar calendar;
  private final TextUI view;
  private final CommandParser parser;
  private final CommandHandler handler;

  /**
   * Creates a new command processor.parseRegularCreateCommand
   *
   * @param calendar the calendar model
   * @param view the text UI view
   */
  public CommandProcessor(Calendar calendar, TextUI view) {
    this.calendar = calendar;
    this.view = view;
    this.parser = new CommandParser();
    this.handler = new CommandHandler(calendar, view);
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
          if (parts.length < 2 || !parts[1].equals("event")) {
            view.displayError("Invalid create command. Expected 'create event'");
            return true;
          }
          return handleCreate(command);

        case "edit":
          if (parts.length < 2 || (!parts[1].equals("event") && !parts[1].equals("events"))) {
            view.displayError("Invalid edit command. Expected 'edit event' or 'edit events'");
            return true;
          }
          return handleEdit(command);

        case "print":
          if (parts.length < 2 || !parts[1].equals("events")) {
            view.displayError("Invalid print command. Expected 'print events'");
            return true;
          }
          return handlePrint(command);

        case "export":
          if (parts.length < 2 || !parts[1].equals("cal")) {
            view.displayError("Invalid export command. Expected 'export cal'");
            return true;
          }
          return handleExport(command);

        case "show":
          if (parts.length < 2 || !parts[1].equals("status")) {
            view.displayError("Invalid show command. Expected 'show status'");
            return true;
          }
          return handleShow(command);

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
   * Handle create event commands.
   *
   * @param command the original command string
   * @return true to continue processing commands
   */
  private boolean handleCreate(String command) {
    Command createCommand = parser.parseCreateCommand(command);
    if (createCommand != null) {
      handler.handleCommand(createCommand);
    } else {
      view.displayError("Invalid create event command format");
    }
    return true;
  }

  /**
   * Handle edit commands.
   *
   * @param command the original command string
   * @return true to continue processing commands
   */
  private boolean handleEdit(String command) {
    Command editCommand = parser.parseEditCommand(command);
    if (editCommand != null) {
      handler.handleCommand(editCommand);
    } else {
      view.displayError("Invalid edit command format");
    }
    return true;
  }

  /**
   * Handle print commands.
   *
   * @param command the original command string
   * @return true to continue processing commands
   */
  private boolean handlePrint(String command) {
    Command printCommand = parser.parsePrintCommand(command);
    if (printCommand != null) {
      handler.handleCommand(printCommand);
    } else {
      view.displayError("Invalid print command format");
    }
    return true;
  }

  /**
   * Handle export commands.
   *
   * @param command the original command string
   * @return true to continue processing commands
   */
  private boolean handleExport(String command) {
    Command exportCommand = parser.parseExportCommand(command);
    if (exportCommand != null) {
      handler.handleCommand(exportCommand);
    } else {
      view.displayError("Invalid export command format");
    }
    return true;
  }

  /**
   * Handle show status commands.
   *
   * @param command the original command string
   * @return true to continue processing commands
   */
  private boolean handleShow(String command) {
    Command showCommand = parser.parseShowCommand(command);
    if (showCommand != null) {
      handler.handleCommand(showCommand);
    } else {
      view.displayError("Invalid show status command");
    }
    return true;
  }
}