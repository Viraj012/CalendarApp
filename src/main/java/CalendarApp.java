import controller.CommandProcessor;
import model.CalendarManager;
import view.InteractiveUI;
import view.HeadlessUI;
import view.TextUI;

import java.io.IOException;
import java.time.ZoneId;

/**
 * Main entry point for the Calendar application.
 */
public class CalendarApp {

  /**
   * Main method for running the Calendar application.
   * Accepts command-line arguments to choose the mode of operation:
   * <ul>
   *   <li>interactive - for interactive mode</li>
   *   <li>headless FILE - for headless mode with a commands file</li>
   * </ul>
   * Exits with an error if arguments are incorrect.
   */
  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: java CalendarApp --mode [interactive|headless commands.txt]");
      System.exit(1);
    }

    if (!args[0].equalsIgnoreCase("--mode")) {
      System.err.println("Expected '--mode' as first argument");
      System.exit(1);
    }

    // Create calendar manager
    CalendarManager calendarManager = new CalendarManager();

    // Create a default calendar
    calendarManager.createCalendar("Default", ZoneId.systemDefault());
    calendarManager.useCalendar("Default");

    TextUI ui;
    if (args[1].equalsIgnoreCase("interactive")) {
      ui = new InteractiveUI();
    } else if (args[1].equalsIgnoreCase("headless") && args.length >= 3) {
      try {
        ui = new HeadlessUI(args[2]);

      } catch (IOException e) {
        System.err.println("Error opening commands file: " + e.getMessage());
        System.exit(1);
        return;
      }
    } else {
      System.err.println("Invalid mode. Use 'interactive' or 'headless FILE'");
      System.exit(1);
      return;
    }

    CommandProcessor processor = new CommandProcessor(calendarManager, ui);

    try {
      boolean keepRunning = true;
      while (keepRunning) {
        String command = ui.getCommand();
        keepRunning = processor.processCommand(command);
      }
    } finally {
      ui.close();
    }
  }
}