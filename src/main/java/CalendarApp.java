

import controller.CommandProcessor;
import model.Calendar;
import model.CalendarImpl;
import view.ConsoleUI;
import view.FileUI;
import view.TextUI;

import java.io.IOException;

/**
 * Main entry point for the Calendar application.
 */
public class CalendarApp {

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: java CalendarApp --mode [interactive|headless commands.txt]");
      System.exit(1);
    }

    if (!args[0].equalsIgnoreCase("--mode")) {
      System.err.println("Expected '--mode' as first argument");
      System.exit(1);
    }

    // Create the model
    Calendar calendar = new CalendarImpl();

    // Create the appropriate view
    TextUI ui;
    if (args[1].equalsIgnoreCase("interactive")) {
      ui = new ConsoleUI();
    } else if (args[1].equalsIgnoreCase("headless") && args.length >= 3) {
      try {
        ui = new FileUI(args[2]);
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

    // Create the controller
    CommandProcessor processor = new CommandProcessor(calendar, ui);

    // Run the application
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