import controller.CommandProcessor;
import model.CalendarManager;
import view.TextUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Test harness for testing the calendar application using commands.
 */
public class CalendarManagerCopyEventDebugTest {

  // A simple implementation of TextUI for testing
  private static class TestUI implements TextUI {
    private List<String> messages = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private String nextCommand;

    @Override
    public void displayMessage(String message) {
      System.out.println("SUCCESS: " + message);
      messages.add(message);
    }

    @Override
    public void displayError(String error) {
      System.err.println("ERROR: " + error);
      errors.add(error);
    }

    @Override
    public String getCommand() {
      System.out.println("> " + nextCommand);
      return nextCommand;
    }

    @Override
    public void close() {
      // No-op for testing
    }

    public void setNextCommand(String command) {
      this.nextCommand = command;
    }

    public List<String> getMessages() {
      return messages;
    }

    public List<String> getErrors() {
      return errors;
    }

    public void clearOutput() {
      messages.clear();
      errors.clear();
    }
  }

  public static void main(String[] args) {
    // Create the test environment
    CalendarManager manager = new CalendarManager();
    TestUI ui = new TestUI();
    CommandProcessor processor = new CommandProcessor(manager, ui);

    System.out.println("=== Starting Copy Event Diagnostic Test ===\n");

    // Run commands and check results
    executeCommand(processor, ui, "create calendar --name NYC --timezone America/New_York");
    executeCommand(processor, ui, "create calendar --name LA --timezone America/Los_Angeles");
    executeCommand(processor, ui, "use calendar --name NYC");

    // Test creating an event
    executeCommand(processor, ui, "create event \"Business Meeting\" from 2023-05-15T13:00 to 2023-05-15T15:00");

    // Print events to see what was created
    System.out.println("\n=== Verifying created event ===");
    executeCommand(processor, ui, "print events on 2023-05-15");

    // Try different approaches to copy the event
    System.out.println("\n=== Testing copy with double quotes ===");
    executeCommand(processor, ui, "copy event \"Business Meeting\" on 2023-05-15T13:00 --target LA to 2023-05-15T13:00");

//    System.out.println("\n=== Testing copy without quotes ===");
//    executeCommand(processor, ui, "copy event Business Meeting on 2023-05-15T13:00 --target LA to 2023-05-15T13:00");

    // Check if any events were created in LA
    System.out.println("\n=== Checking LA calendar ===");
    executeCommand(processor, ui, "use calendar --name LA");
    executeCommand(processor, ui, "print events on 2023-05-15");

    // Try creating an event directly in LA
    System.out.println("\n=== Creating event directly in LA ===");
    executeCommand(processor, ui, "create event \"LA Meeting\" from 2023-05-16T10:00 to 2023-05-16T11:00");
    executeCommand(processor, ui, "print events on 2023-05-16");

    // Final diagnostic information
    System.out.println("\n=== Diagnostic Summary ===");
    System.out.println("Total successful commands: " + ui.getMessages().size());
    System.out.println("Total errors: " + ui.getErrors().size());

    // Verify if all calendars were created correctly
    System.out.println("\nVerifying calendars:");
    System.out.println("NYC Calendar exists: " + manager.calendarExists("NYC"));
    System.out.println("LA Calendar exists: " + manager.calendarExists("LA"));

    // Debug the event copy command more precisely
    System.out.println("\n=== Detailed Copy Event Analysis ===");
    ui.clearOutput();
    executeCommand(processor, ui, "use calendar --name NYC");

    // Get and print all events in the source calendar
    System.out.println("\nAll events in NYC calendar:");
    for (model.Event event : manager.getCurrentCalendar().getAllEvents()) {
      System.out.println("Event: '" + event.getSubject() + "'");
      System.out.println("  Start: " + event.getStartDateTime());
      System.out.println("  End: " + event.getEndDateTime());
      System.out.println("  isAllDay: " + event.isAllDay());
      System.out.println("  isRecurring: " + event.isRecurring());
    }
  }

  private static void executeCommand(CommandProcessor processor, TestUI ui, String command) {
    ui.setNextCommand(command);
    boolean result = processor.processCommand(ui.getCommand());
    if (!result) {
      System.out.println("NOTE: Command resulted in exit signal (false return)");
    }
  }
}