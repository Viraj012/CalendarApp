import controller.Command;
import controller.CommandHandler;
import controller.CommandParser;
import model.Calendar;
import model.CalendarImpl;
import model.Event;
import org.junit.Before;
import org.junit.Test;
import view.TextUI;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test cases for the CommandHandler class.
 */
public class CommandHandlerTest {
  private Calendar calendar;
  private MockTextUI mockUI;
  private CommandHandler handler;
  private CommandParser parser;

  @Before
  public void setUp() {
    calendar = new CalendarImpl();
    mockUI = new MockTextUI();
    handler = new CommandHandler(calendar, mockUI);
    parser = new CommandParser();
  }

  @Test
  public void testHandleCreateCommand() {
    // Create a simple event command
    Command cmd = parser.parseCreateCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");

    // Handle the command
    handler.handleCommand(cmd);

    // Verify the event was created successfully
    assertTrue("Should display success message", mockUI.hasMessageContaining("created successfully"));
    assertEquals("Calendar should have the event", 1, calendar.getEventsOn(LocalDateTime.of(2025, 3, 4, 10, 0)).size());
  }

  @Test
  public void testHandleCreateAllDayCommand() {
    // Create an all-day event command
    Command cmd = parser.parseCreateCommand("create event Conference on 2025-03-04");

    // Handle the command
    handler.handleCommand(cmd);

    // Verify the event was created successfully
    assertTrue("Should display success message", mockUI.hasMessageContaining("created successfully"));
    assertTrue("Event should be all-day", calendar.getEventsOn(LocalDateTime.of(2025, 3, 4, 0, 0)).get(0).isAllDay());
  }

  @Test
  public void testHandleCreateWithConflict() {
    // Create first event
    Command cmd1 = parser.parseCreateCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    handler.handleCommand(cmd1);
    mockUI.clearMessages();

    // Try to create a conflicting event with auto-decline
    Command cmd2 = parser.parseCreateCommand("create event --autoDecline Conflict from 2025-03-04T10:30 to 2025-03-04T11:30");
    handler.handleCommand(cmd2);

    // Verify conflict was detected
    assertTrue("Should show conflict error", mockUI.hasErrorMessageContaining("conflict detected"));
  }

  @Test
  public void testHandleEditCommand() {
    // Create an event first
    Command createCmd = parser.parseCreateCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    handler.handleCommand(createCmd);
    mockUI.clearMessages();

    // Edit the event
    Command editCmd = parser.parseEditCommand("edit event name \"Meeting\" from 2025-03-04T10:00 to 2025-03-04T11:00 with \"Team Meeting\"");
    handler.handleCommand(editCmd);

    // Verify the edit was successful
    assertTrue("Should show success message", mockUI.hasMessageContaining("updated successfully"));

    // Check if the event name was updated
    List<Event> events = calendar.getEventsOn(LocalDateTime.of(2025, 3, 4, 10, 0));
    assertEquals("Event name should be updated", "Team Meeting", events.get(0).getSubject());
  }

  @Test
  public void testHandleEditNonexistentEvent() {
    // Try to edit an event that doesn't exist
    Command editCmd = parser.parseEditCommand("edit event name \"Nonexistent\" from 2025-03-04T10:00 to 2025-03-04T11:00 with \"New Name\"");
    handler.handleCommand(editCmd);

    // Verify appropriate error message
    assertTrue("Should show not found error", mockUI.hasErrorMessageContaining("not found"));
  }

  @Test
  public void testHandlePrintCommand() {
    // Create an event
    Command createCmd = parser.parseCreateCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    handler.handleCommand(createCmd);
    mockUI.clearMessages();

    // Print events on that day
    Command printCmd = parser.parsePrintCommand("print events on 2025-03-04");
    handler.handleCommand(printCmd);

    // Verify events are printed
    assertTrue("Should show events heading", mockUI.hasMessageContaining("Events on"));
    assertTrue("Should include event in output", mockUI.hasMessageContaining("Meeting"));
  }

  @Test
  public void testHandlePrintNoEvents() {
    // Print events on a day with no events
    Command printCmd = parser.parsePrintCommand("print events on 2025-03-04");
    handler.handleCommand(printCmd);

    // Verify appropriate message
    assertTrue("Should show no events message", mockUI.hasMessageContaining("No events on"));
  }

  @Test
  public void testHandleShowCommand() {
    // Create an event
    Command createCmd = parser.parseCreateCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    handler.handleCommand(createCmd);
    mockUI.clearMessages();

    // Check status during the event
    Command showCmd = parser.parseShowCommand("show status on 2025-03-04T10:30");
    handler.handleCommand(showCmd);

    // Verify busy status shown
    assertTrue("Should show busy status", mockUI.hasMessageContaining("busy"));

    // Check status outside the event
    mockUI.clearMessages();
    Command showCmd2 = parser.parseShowCommand("show status on 2025-03-04T12:00");
    handler.handleCommand(showCmd2);

    // Verify available status shown
    assertTrue("Should show available status", mockUI.hasMessageContaining("available"));
  }

  @Test
  public void testHandleExportCommand() {
    // Create an event
    Command createCmd = parser.parseCreateCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    handler.handleCommand(createCmd);
    mockUI.clearMessages();

    // Export calendar
    Command exportCmd = parser.parseExportCommand("export cal test_export.csv");
    handler.handleCommand(exportCmd);

    // Verify export was successful
    assertTrue("Should show export message", mockUI.hasMessageContaining("exported to"));

    // Clean up
    File exportFile = new File("test_export.csv");
    if (exportFile.exists()) {
      exportFile.delete();
    }
  }

  /**
   * Mock TextUI implementation for testing.
   */
  private static class MockTextUI implements TextUI {
    private List<String> messages = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

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
      return null; // Not used in these tests
    }

    @Override
    public void close() {
      // Not used in these tests
    }

    public boolean hasMessageContaining(String substring) {
      for (String message : messages) {
        if (message.contains(substring)) {
          return true;
        }
      }
      return false;
    }

    public boolean hasErrorMessageContaining(String substring) {
      for (String error : errors) {
        if (error.contains(substring)) {
          return true;
        }
      }
      return false;
    }

    public void clearMessages() {
      messages.clear();
      errors.clear();
    }
  }
}