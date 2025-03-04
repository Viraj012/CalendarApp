
import controller.CommandProcessor;
import model.Calendar;
import model.CalendarImpl;
import model.Event;
import org.junit.Before;
import org.junit.Test;
import view.TextUI;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test cases for the CommandProcessor class.
 */
public class CommandProcessorTest {
  private Calendar calendar;
  private MockTextUI mockUI;
  private CommandProcessor processor;

  @Before
  public void setUp() {
    calendar = new CalendarImpl();
    mockUI = new MockTextUI();
    processor = new CommandProcessor(calendar, mockUI);
  }

  @Test
  public void testProcessEmptyCommand() {
    processor.processCommand("");
    assertTrue("Should display error for empty command", mockUI.hasErrorMessage("Empty command"));
  }

  @Test
  public void testProcessUnknownCommand() {
    processor.processCommand("unknown command");
    assertTrue("Should display error for unknown command", mockUI.hasErrorMessage("Unknown command: unknown"));
  }

  @Test
  public void testProcessExitCommand() {
    boolean result = processor.processCommand("exit");
    assertFalse("Should return false for exit command", result);
  }

  @Test
  public void testProcessCreateEventCommand() {
    // Test creating a regular event
    processor.processCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    assertTrue("Should display success message", mockUI.hasMessage("Event created successfully: Meeting"));

    // Test creating a regular event with auto-decline
    processor.processCommand("create event --autoDecline Conflict from 2025-03-04T10:30 to 2025-03-04T11:30");
    assertTrue("Should display error for conflict", mockUI.hasErrorMessage("Failed to create event (conflict detected)"));

    // Clear messages for next test
    mockUI.clearMessages();

    // Test creating an all-day event
    processor.processCommand("create event Conference on 2025-03-05");
    assertTrue("Should display success message", mockUI.hasMessage("All-day event created successfully: Conference"));
  }

  @Test
  public void testProcessCreateRecurringEventCommand() {
    // Test creating a recurring event with N occurrences
    processor.processCommand("create event Standup from 2025-03-04T09:00 to 2025-03-04T09:30 repeats MWF for 3 times");
    assertTrue("Should display success message", mockUI.hasMessage("Recurring event created successfully: Standup"));

    // Test creating a recurring event until a specific date
    processor.processCommand("create event Weekly from 2025-03-04T14:00 to 2025-03-04T15:00 repeats T until 2025-03-25");
    assertTrue("Should display success message", mockUI.hasMessage("Recurring event created successfully: Weekly"));

    // Test recurring event that spans multiple days (should fail)
    processor.processCommand("create event Invalid from 2025-03-04T22:00 to 2025-03-05T01:00 repeats M for 3 times");
    assertTrue("Should display error for multi-day recurring event",
        mockUI.hasErrorMessage("Failed to create recurring event (conflict detected or spans multiple days)"));
  }

  @Test
  public void testProcessCreateRecurringAllDayEventCommand() {
    // Test creating a recurring all-day event
    processor.processCommand("create event Workshop on 2025-03-04 repeats TR for 2 times");
    assertTrue("Should display success message", mockUI.hasMessage("Recurring all-day event created successfully: Workshop"));
  }

  @Test
  public void testProcessEditEventCommand() {
    // Create an event first
    processor.processCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    mockUI.clearMessages();

    // Edit the event
    processor.processCommand("edit event name \"Meeting\" from 2025-03-04T10:00 to 2025-03-04T11:00 with \"Team Meeting\"");
    assertTrue("Should display success message", mockUI.hasMessage("Event updated successfully"));

    // Verify edit was successful by printing events
    mockUI.clearMessages();
    processor.processCommand("print events on 2025-03-04");
    assertTrue("Event name should be updated", mockUI.getLastMessage().contains("Team Meeting"));
  }

  @Test
  public void testProcessEditEventsFromCommand() {
    // Create recurring events
    processor.processCommand("create event Standup from 2025-03-04T09:00 to 2025-03-04T09:30 repeats MWF for 3 times");
    mockUI.clearMessages();

    // Edit events from a specific date
    processor.processCommand("edit events location \"Standup\" from 2025-03-05T09:00 with \"Conference Room\"");
    assertTrue("Should display success message", mockUI.hasMessage("Events updated successfully"));
  }

  @Test
  public void testProcessEditAllEventsCommand() {
    // Create multiple events with the same name
    processor.processCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    processor.processCommand("create event Meeting from 2025-03-05T10:00 to 2025-03-05T11:00");
    mockUI.clearMessages();

    // Edit all events with that name
    processor.processCommand("edit events name \"Meeting\" \"Team Meeting\"");
    assertTrue("Should display success message", mockUI.hasMessage("All events updated successfully"));
  }

  @Test
  public void testProcessPrintEventsOnCommand() {
    // Create an event
    processor.processCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    mockUI.clearMessages();

    // Print events on that day
    processor.processCommand("print events on 2025-03-04");
    assertTrue("Should display events", mockUI.hasMessage("Events on 2025-03-04:"));
    assertTrue("Should display event details", mockUI.getLastMessage().contains("Meeting"));

    // Print events on a day with no events
    mockUI.clearMessages();
    processor.processCommand("print events on 2025-03-05");
    assertTrue("Should display message for no events", mockUI.hasMessage("No events on 2025-03-05"));
  }

  @Test
  public void testProcessPrintEventsFromCommand() {
    // Create events on different days
    processor.processCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    processor.processCommand("create event Conference from 2025-03-05T09:00 to 2025-03-05T17:00");
    mockUI.clearMessages();

    // Print events in date range
    processor.processCommand("print events from 2025-03-04T00:00 to 2025-03-05T23:59");
    assertTrue("Should display events in range", mockUI.getLastMessage().contains("Meeting"));
    assertTrue("Should display events in range", mockUI.getLastMessage().contains("Conference"));
  }

  @Test
  public void testProcessShowStatusCommand() {
    // Create an event
    processor.processCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    mockUI.clearMessages();

    // Check status during event
    processor.processCommand("show status on 2025-03-04T10:30");
    assertTrue("Should show busy status", mockUI.hasMessage("busy"));

    // Check status outside event
    mockUI.clearMessages();
    processor.processCommand("show status on 2025-03-04T12:00");
    assertTrue("Should show available status", mockUI.hasMessage("available"));
  }

  @Test
  public void testProcessExportCommand() {
    // Create some events
    processor.processCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    mockUI.clearMessages();

    // Export the calendar
    processor.processCommand("export cal test_export.csv");
    assertTrue("Should display export message", mockUI.hasMessage("Calendar exported to:"));
  }

  @Test
  public void testProcessInvalidCreateCommand() {
    processor.processCommand("create invalid");
    assertTrue("Should display error for invalid create command",
        mockUI.hasErrorMessage("Invalid create command. Expected 'create event'"));
  }

  @Test
  public void testProcessInvalidEditCommand() {
    processor.processCommand("edit invalid");
    assertTrue("Should display error for invalid edit command",
        mockUI.hasErrorMessage("Invalid edit command. Expected 'edit event' or 'edit events'"));
  }

  @Test
  public void testProcessInvalidPrintCommand() {
    processor.processCommand("print invalid");
    assertTrue("Should display error for invalid print command",
        mockUI.hasErrorMessage("Invalid print command. Expected 'print events'"));
  }

  @Test
  public void testProcessInvalidExportCommand() {
    processor.processCommand("export invalid");
    assertTrue("Should display error for invalid export command",
        mockUI.hasErrorMessage("Invalid export command. Expected 'export cal'"));
  }

  @Test
  public void testProcessInvalidShowCommand() {
    processor.processCommand("show invalid");
    assertTrue("Should display error for invalid show command",
        mockUI.hasErrorMessage("Invalid show command. Expected 'show status'"));
  }

  @Test
  public void testProcessInvalidDateFormat() {
    processor.processCommand("create event Meeting from invalid to 2025-03-04T11:00");
    assertTrue("Should display error for invalid date format",
        mockUI.hasErrorMessage("Invalid date/time format"));
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
      return null; // Not used in tests
    }

    @Override
    public void close() {
      // Not used in tests
    }

    public boolean hasMessage(String message) {
      for (String msg : messages) {
        if (msg.contains(message)) {
          return true;
        }
      }
      return false;
    }

    public boolean hasErrorMessage(String error) {
      for (String err : errors) {
        if (err.contains(error)) {
          return true;
        }
      }
      return false;
    }

    public String getLastMessage() {
      if (messages.isEmpty()) {
        return "";
      }
      return messages.get(messages.size() - 1);
    }

    public void clearMessages() {
      messages.clear();
      errors.clear();
    }
  }
}