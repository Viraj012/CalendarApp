
import controller.CommandProcessor;
import model.Calendar;
import model.CalendarImpl;
import org.junit.Before;
import org.junit.Test;
import view.TextUI;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CommandProcessorTest {

  private Calendar calendar;
  private TestTextUI textUI;
  private CommandProcessor processor;

  @Before
  public void setUp() {
    calendar = new CalendarImpl();
    textUI = new TestTextUI();
    processor = new CommandProcessor(calendar, textUI);
  }

  @Test
  public void testEmptyCommand() {
    boolean result = processor.processCommand("");
    assertTrue(result);
    assertEquals("Empty command", textUI.getLastError());
  }

  @Test
  public void testWhitespaceCommand() {
    boolean result = processor.processCommand("   ");
    assertTrue(result);
    assertEquals("Empty command", textUI.getLastError());
  }

  @Test
  public void testUnknownCommand() {
    boolean result = processor.processCommand("unknown");
    assertTrue(result);
    assertEquals("Unknown command: unknown", textUI.getLastError());
  }

  @Test
  public void testExitCommand() {
    boolean result = processor.processCommand("exit");
    assertFalse(result);
    assertNull(textUI.getLastError());
  }

  @Test
  public void testInvalidCreateCommand() {
    boolean result = processor.processCommand("create something");
    assertTrue(result);
    assertEquals("Invalid create command. Expected 'create event'", textUI.getLastError());
  }

  @Test
  public void testInvalidCreateEventFormat() {
    boolean result = processor.processCommand("create event");
    assertTrue(result);
    assertEquals("Invalid create event command format", textUI.getLastError());
  }

  @Test
  public void testValidCreateCommand() {
    boolean result = processor.processCommand("create event Meeting on 2023-03-15");
    assertTrue(result);
    assertNull(textUI.getLastError());
    assertTrue(textUI.getLastMessage().contains("created successfully"));
  }

  @Test
  public void testInvalidEditCommand() {
    boolean result = processor.processCommand("edit something");
    assertTrue(result);
    assertEquals("Invalid edit command. Expected 'edit event' or 'edit events'", textUI.getLastError());
  }

  @Test
  public void testInvalidEditEventFormat() {
    boolean result = processor.processCommand("edit event");
    assertTrue(result);
    assertEquals("Invalid edit command format", textUI.getLastError());
  }

  @Test
  public void testValidEditSingleEventCommand() {
    // First create an event
    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00");
    textUI.reset();

    // Then edit it
    boolean result = processor.processCommand("edit event description Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 with \"Updated description\"");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testValidEditEventsFromCommand() {
    // First create a recurring event
    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 repeats MWF for 5 times");
    textUI.reset();

    // Then edit events from a specific date
    boolean result = processor.processCommand("edit events description Meeting from 2023-03-17 with \"Updated description\"");
    assertTrue(result);
    assertEquals("Events updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testValidEditAllEventsCommand() {
    // First create an event
    processor.processCommand("create event Meeting on 2023-03-15");
    textUI.reset();

    // Then edit all events with that name
    boolean result = processor.processCommand("edit events description \"Meeting\" \"Updated description\"");
    assertTrue(result);
    assertEquals("All events updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testInvalidPrintCommand() {
    boolean result = processor.processCommand("print something");
    assertTrue(result);
    assertEquals("Invalid print command. Expected 'print events'", textUI.getLastError());
  }

  @Test
  public void testInvalidPrintEventsFormat() {
    boolean result = processor.processCommand("print events");
    assertTrue(result);
    assertEquals("Invalid print command format", textUI.getLastError());
  }

  @Test
  public void testValidPrintEventsOnDate() {
    boolean result = processor.processCommand("print events on 2023-03-15");
    assertTrue(result);
    assertEquals("No events on 2023-03-15", textUI.getLastMessage());
  }

  @Test
  public void testValidPrintEventsDateRange() {
    boolean result = processor.processCommand("print events from 2023-03-15 to 2023-03-20");
    assertTrue(result);
    assertEquals("No events from 2023-03-15 to 2023-03-20", textUI.getLastMessage());
  }

  @Test
  public void testInvalidExportCommand() {
    boolean result = processor.processCommand("export something");
    assertTrue(result);
    assertEquals("Invalid export command. Expected 'export cal'", textUI.getLastError());
  }

  @Test
  public void testInvalidExportCalFormat() {
    boolean result = processor.processCommand("export cal");
    assertTrue(result);
    assertEquals("Invalid export command format", textUI.getLastError());
  }

  @Test
  public void testValidExportCommand() {
    boolean result = processor.processCommand("export cal events.csv");
    assertTrue(result);
    // The actual path will be system-dependent, so just check that we got a message
    assertNotNull(textUI.getLastMessage());
  }

  @Test
  public void testInvalidShowCommand() {
    boolean result = processor.processCommand("show something");
    assertTrue(result);
    assertEquals("Invalid show command. Expected 'show status'", textUI.getLastError());
  }

  @Test
  public void testInvalidShowStatusFormat() {
    boolean result = processor.processCommand("show status");
    assertTrue(result);
    assertEquals("Invalid show status command", textUI.getLastError());
  }

  @Test
  public void testValidShowStatusCommand() {
    boolean result = processor.processCommand("show status on 2023-03-15T10:00");
    assertTrue(result);
    assertEquals("available", textUI.getLastMessage());
  }

  @Test
  public void testCommandException() {
    // Create a malformed command that will cause an internal exception
    boolean result = processor.processCommand("create event Meeting from 2023-99-99T10:00 to 2023-03-15T11:00");
    assertTrue(result);
    assertEquals("Invalid create event command format", textUI.getLastError());
  }

  @Test
  public void testPrintCommandWithEventsOnDate() {
    // Create an event first
    processor.processCommand("create event Meeting on 2023-03-15");
    textUI.reset();

    // Then print events for that date
    boolean result = processor.processCommand("print events on 2023-03-15");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Meeting - 2023-03-15 All Day"));
  }

  @Test
  public void testPrintCommandWithEventsInRange() {
    // Create an event first
    processor.processCommand("create event Meeting on 2023-03-15");
    textUI.reset();

    // Then print events in a range that includes that date
    boolean result = processor.processCommand("print events from 2023-03-14 to 2023-03-16");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("1. Meeting - 2023-03-15 All Day"));
  }
  @Test
  public void testShowCommandBusy() {
    // Create an event first
    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00");
    textUI.reset();

    // Then check status at that time
    boolean result = processor.processCommand("show status on 2023-03-15T10:30");
    assertTrue(result);
    assertEquals("busy", textUI.getLastMessage());
  }

  private static class TestTextUI implements TextUI {
    private String lastMessage;
    private String lastError;
    private List<String> commands = new ArrayList<>();
    private int commandIndex = 0;

    @Override
    public void displayMessage(String message) {
      lastMessage = message;
    }

    @Override
    public void displayError(String error) {
      lastError = error;
    }

    @Override
    public String getCommand() {
      if (commandIndex < commands.size()) {
        return commands.get(commandIndex++);
      }
      return "";
    }

    @Override
    public void close() {
      // Do nothing
    }

    public void addCommand(String command) {
      commands.add(command);
    }

    public String getLastMessage() {
      return lastMessage;
    }

    public String getLastError() {
      return lastError;
    }

    public void reset() {
      lastMessage = null;
      lastError = null;
    }
  }

  @Test
  public void testHandleCreateWithRecurringEvent() {
    boolean result = processor.processCommand("create event Weekly Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 repeats W for 10 times");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Recurring event created successfully"));
  }

  @Test
  public void testHandleCreateWithConflict() {
    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --autoDecline");
    textUI.reset();
    boolean result = processor.processCommand("create event Another Meeting from 2023-03-15T10:30 to 2023-03-15T11:30 --autoDecline");
    assertTrue(result);
    assertTrue(textUI.getLastError().contains("Failed to create Event"));
  }

  @Test
  public void testHandleEditWithInvalidProperty() {
    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00");
    textUI.reset();
    boolean result = processor.processCommand("edit event invalidproperty Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 with \"New Value\"");
    assertTrue(result);
    assertEquals("Failed to update event (not found or invalid property)", textUI.getLastError());
  }

  @Test
  public void testHandleEditWithNonExistentEvent() {
    boolean result = processor.processCommand("edit event description \"Non-Existent Meeting\" from 2023-03-15T10:00 to 2023-03-15T11:00 with \"Updated description\"");
    assertTrue(result);
    assertEquals("Failed to update event (not found or invalid property)", textUI.getLastError());
  }

  @Test
  public void testHandlePrintWithMultipleEvents() {
    processor.processCommand("create event Meeting 1 on 2023-03-15");
    processor.processCommand("create event Meeting 2 on 2023-03-15");
    textUI.reset();
    boolean result = processor.processCommand("print events on 2023-03-15");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Meeting 2"));
  }

  @Test
  public void testHandlePrintWithDateRange() {
    processor.processCommand("create event Meeting 1 on 2023-03-15");
    processor.processCommand("create event Meeting 2 on 2023-03-16");
    textUI.reset();
    boolean result = processor.processCommand("print events from 2023-03-14 to 2023-03-17");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Meeting 2"));
  }

  @Test
  public void testHandleExportWithInvalidFilename() {
    // Use an invalid file path that should cause export to fail
    boolean result = processor.processCommand("export cal /\\:*?\"<>|.csv");
    assertTrue(result);
    assertEquals("Failed to export calendar", textUI.getLastError());
  }

  @Test
  public void testHandleExportWithEvents() {
    processor.processCommand("create event Meeting on 2023-03-15");
    textUI.reset();
    boolean result = processor.processCommand("export cal calendar.csv");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Calendar exported to:"));
  }

  @Test
  public void testHandleShowWithBusyStatus() {
    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00");
    textUI.reset();
    boolean result = processor.processCommand("show status on 2023-03-15T10:30");
    assertTrue(result);
    assertEquals("busy", textUI.getLastMessage());
  }

  @Test
  public void testHandleShowWithAvailableStatus() {
    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00");
    textUI.reset();
    boolean result = processor.processCommand("show status on 2023-03-15T12:00");
    assertTrue(result);
    assertEquals("available", textUI.getLastMessage());
  }


}