import controller.CommandProcessor;
import model.CalendarManager;
import view.InteractiveUI;
import view.HeadlessUI;
import view.TextUI;
import view.gui.CalendarGUI;
import view.gui.SwingUI;

import javax.swing.*;
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
   *   <li>interactive - for interactive console mode</li>
   *   <li>headless FILE - for headless mode with a commands file</li>
   *   <li>gui - for graphical user interface mode</li>
   * </ul>
   * Exits with an error if arguments are incorrect.
   */
  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: java CalendarApp --mode [interactive|headless commands.txt|gui]");
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

    if (args[1].equalsIgnoreCase("interactive")) {
      TextUI ui = new InteractiveUI();
      runCommandProcessor(calendarManager, ui);
    } else if (args[1].equalsIgnoreCase("headless") && args.length >= 3) {
      try {
        TextUI ui = new HeadlessUI(args[2]);
        runCommandProcessor(calendarManager, ui);
      } catch (IOException e) {
        System.err.println("Error opening commands file: " + e.getMessage());
        System.exit(1);
      }
    } else if (args[1].equalsIgnoreCase("gui")) {
      launchGUI(calendarManager);
    } else {
      System.err.println("Invalid mode. Use 'interactive', 'headless FILE', or 'gui'");
      System.exit(1);
    }
  }

  /**
   * Runs the command processor with the specified UI.
   */
  private static void runCommandProcessor(CalendarManager calendarManager, TextUI ui) {
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

  /**
   * Launches the GUI version of the application.
   */
  private static void launchGUI(CalendarManager calendarManager) {
    // Use the system look and feel
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      System.err.println("Could not set system look and feel: " + e.getMessage());
    }

    // Create and show the GUI using SwingUtilities
    SwingUtilities.invokeLater(() -> {
      // Create main GUI
      CalendarGUI gui = new CalendarGUI(calendarManager);

      // Create SwingUI
      SwingUI swingUI = new SwingUI(gui);

      // Connect the GUI and SwingUI
      gui.setSwingUI(swingUI);

      // Create a command processor in a background thread
      Thread processorThread = new Thread(() -> {
        CommandProcessor processor = new CommandProcessor(calendarManager, swingUI);

        try {
          boolean keepRunning = true;
          while (keepRunning) {
            String command = swingUI.getCommand();
            keepRunning = processor.processCommand(command);
          }
        } finally {
          swingUI.close();
        }
      });

      // Set as daemon thread so it doesn't prevent JVM exit
      processorThread.setDaemon(true);
      processorThread.start();

      // Show the GUI
      gui.setVisible(true);
    });
  }
}