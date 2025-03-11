import controller.CommandProcessor;
import model.Calendar;
import model.CalendarImpl;
import model.Event;
import org.junit.Test;
import view.TextUI;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class to specifically verify MVC pattern implementation in the Calendar application.
 * Tests the interactions between Model (Calendar), View (TextUI), and Controller (CommandProcessor).
 */
public class MVCTest {

  /**
   * Test the MVC pattern in action by verifying that:
   * 1. Controller properly processes user commands via the View
   * 2. Controller correctly manipulates the Model based on those commands
   * 3. Controller communicates results back to the View
   */
  @Test
  public void testMVCInteraction() {
    // Create the MVC components
    Calendar model = new CalendarImpl();
    MockTextUI view = new MockTextUI();
    CommandProcessor controller = new CommandProcessor(model, view);

    // Set up test data
    view.addCommand("create event Team Meeting from 2025-03-15T14:00 to 2025-03-15T15:30 --description \"Weekly team sync\" --location \"Conference Room A\"");
    view.addCommand("print events on 2025-03-15");
    view.addCommand("edit event name Team Meeting from 2025-03-15T14:00 to 2025-03-15T15:30 with \"Strategy Meeting\"");
    view.addCommand("print events on 2025-03-15");
    view.addCommand("exit");

    // Run the application loop
    boolean keepRunning = true;
    while (keepRunning) {
      String command = view.getCommand();
      keepRunning = controller.processCommand(command);
    }

    // Verify the view received expected messages
    List<String> messages = view.getAllMessages();
    List<String> errors = view.getAllErrors();

    // Check that there are no errors
    assertTrue(errors.isEmpty());

    // Verify create event success message
    assertTrue(messages.stream().anyMatch(msg -> msg.contains("Event created successfully") && msg.contains("Team Meeting")));

    // Verify initial print output shows the created event
    assertTrue(messages.stream().anyMatch(msg -> msg.contains("Events on 2025-03-15")));
    assertTrue(messages.stream().anyMatch(msg -> msg.contains("Team Meeting")));

    // Verify edit success message
    assertTrue(messages.stream().anyMatch(msg -> msg.contains("Event updated successfully")));

    // Verify second print output shows the edited event
    assertTrue(messages.stream().anyMatch(msg -> msg.contains("Strategy Meeting")));

    // Confirm the event doesn't still have the old name
    assertFalse(messages.stream().filter(msg -> msg.contains("Events on 2025-03-15:")).anyMatch(msg -> msg.contains("Team Meeting")));
  }

  /**
   * Test that Model maintains integrity regardless of View or Controller.
   * The Model should enforce its own validation rules.
   */
  @Test
  public void testModelIntegrity() {
    // Create the MVC components
    Calendar model = new CalendarImpl();
    MockTextUI view = new MockTextUI();
    CommandProcessor controller = new CommandProcessor(model, view);

    // Attempt to create an event with invalid time (end before start)
    view.addCommand("create event Invalid Meeting from 2025-03-15T15:30 to 2025-03-15T14:00");
    controller.processCommand(view.getCommand());

    // Verify model integrity by checking that no events were created
    List<Event> events = model.getEventsOn(LocalDateTime.of(2025, 3, 15, 0, 0));
    assertTrue(events.isEmpty());

    // Verify view received appropriate error message
    assertTrue(view.getAllErrors().stream().anyMatch(err -> err.contains("Invalid create event command format")));
  }

  /**
   * Test that View is passive and only displays what the Controller tells it to.
   */
  @Test
  public void testViewPassivity() {
    // Create the MVC components
    Calendar model = new CalendarImpl();
    MockTextUI view = new MockTextUI();
    CommandProcessor controller = new CommandProcessor(model, view);

    // Create an event directly in the model
    model.createEvent("Direct Event",
        LocalDateTime.of(2025, 3, 15, 14, 0),
        LocalDateTime.of(2025, 3, 15, 15, 0),
        false, "Description", "Location", true);

    // Verify view hasn't received any messages yet
    assertTrue(view.getAllMessages().isEmpty());

    // Use controller to print events
    view.addCommand("print events on 2025-03-15");
    controller.processCommand(view.getCommand());

    // Now view should have received messages
    assertFalse(view.getAllMessages().isEmpty());
    assertTrue(view.getAllMessages().stream().anyMatch(msg -> msg.contains("Direct Event")));
  }

  /**
   * Test that Controller coordinates all interactions between Model and View.
   */
  @Test
  public void testControllerCoordination() {
    // Create the MVC components
    Calendar model = new CalendarImpl();
    MockTextUI view = new MockTextUI();
    CommandProcessor controller = new CommandProcessor(model, view);

    // Create two events that conflict
    view.addCommand("create event First Meeting from 2025-03-15T14:00 to 2025-03-15T15:30");
    view.addCommand("create event Second Meeting from 2025-03-15T14:30 to 2025-03-15T16:00 --autoDecline");

    controller.processCommand(view.getCommand()); // Process first event
    controller.processCommand(view.getCommand()); // Process second event

    // Verify controller correctly coordinated between model and view
    // Model should have only the first event
    List<Event> events = model.getEventsOn(LocalDateTime.of(2025, 3, 15, 0, 0));
    assertEquals(1, events.size());
    assertEquals("First Meeting", events.get(0).getSubject());

    // View should have received a success message for the first event
    assertTrue(view.getAllMessages().stream().anyMatch(msg -> msg.contains("Event created successfully") && msg.contains("First Meeting")));

    // View should have received an error message for the second event
    assertTrue(view.getAllErrors().stream().anyMatch(err -> err.contains("Failed to create") && err.contains("conflict detected")));
  }

  /**
   * Mock implementation of TextUI that allows pre-loading commands for testing.
   */
  private static class MockTextUI implements TextUI {
    private List<String> commands = new ArrayList<>();
    private List<String> messages = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private int currentCommandIndex = 0;

    public void addCommand(String command) {
      commands.add(command);
    }

    @Override
    public void displayMessage(String message) {
      messages.add(message);
    }

    @Override
    public void displayError(String error) {
      errors.add(error);
    }

    @Override
    public String getCommand() {
      if (currentCommandIndex < commands.size()) {
        return commands.get(currentCommandIndex++);
      }
      return "exit"; // Default to exit if no more commands
    }

    @Override
    public void close() {
      // Not used in these tests
    }

    public List<String> getAllMessages() {
      return new ArrayList<>(messages);
    }

    public List<String> getAllErrors() {
      return new ArrayList<>(errors);
    }
  }
}