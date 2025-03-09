import controller.CommandProcessor;
import model.Calendar;
import model.CalendarImpl;
import model.Event;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import view.TextUI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests edge cases and boundary conditions for the Calendar application.
 */
public class EdgeCasesTest {
  private Calendar calendar;
  private MockTextUI mockUI;
  private CommandProcessor processor;
  private PrintStream originalOut;
  private PrintStream originalErr;
  private ByteArrayOutputStream outContent;
  private ByteArrayOutputStream errContent;
  private LocalDateTime now;

  @Before
  public void setUp() {
    calendar = new CalendarImpl();
    mockUI = new MockTextUI();
    processor = new CommandProcessor(calendar, mockUI);

    // Set up output capturing
    originalOut = System.out;
    originalErr = System.err;
    outContent = new ByteArrayOutputStream();
    errContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));

    now = LocalDateTime.of(2025, 3, 4, 10, 0);
  }

  @After
  public void tearDown() {
    // Restore original output streams
    System.setOut(originalOut);
    System.setErr(originalErr);

    // Clean up any exported files
    File exportFile = new File("edge_case_test.csv");
    if (exportFile.exists()) {
      exportFile.delete();
    }
  }

  /**
   * Test creating events that span midnight.
   */
  @Test
  public void testCreateEventSpanningMidnight() {
    LocalDateTime startTime = LocalDateTime.of(2025, 3, 4, 22, 0);
    LocalDateTime endTime = LocalDateTime.of(2025, 3, 5, 2, 0);

    // Create event that spans midnight
    processor.processCommand("create event \"Late Night Work\" from " +
        startTime.toString() + " to " +
        endTime.toString());

    assertTrue("Should create event spanning midnight",
        mockUI.hasMessageContaining("Event created successfully"));

    // Verify event appears on both days
    mockUI.clearMessages();
    processor.processCommand("print events on 2025-03-04");
    assertTrue("Event should appear on start date",
        mockUI.hasMessageContaining("Late Night Work"));

    mockUI.clearMessages();
    processor.processCommand("print events on 2025-03-05");
    assertTrue("Event should appear on end date",
        mockUI.hasMessageContaining("Late Night Work"));
  }

  /**
   * Test creating events with special characters.
   */
  @Test
  public void testEventsWithSpecialCharacters() {
    // Create event with special characters
    processor.processCommand("create event \"Meeting: Q1 Results (2025) @ HQ\" from 2025-03-04T10:00 to 2025-03-04T11:00");

    assertTrue("Should create event with special characters",
        mockUI.hasMessageContaining("Event created successfully"));

    // Verify we can print and find it
    mockUI.clearMessages();
    processor.processCommand("print events on 2025-03-04");
    assertTrue("Should display event with special characters",
        mockUI.hasMessageContaining("Meeting: Q1 Results (2025) @ HQ"));
  }

  /**
   * Test creating events with extremely long names/descriptions.
   */
  @Test
  public void testEventsWithLongText() {
    // Create long event name (over 100 characters)
    String longName = "This is an extremely long event name that exceeds typical field length limits to test how the system handles long text inputs";
    processor.processCommand("create event \"" + longName + "\" from 2025-03-04T10:00 to 2025-03-04T11:00");

    assertTrue("Should create event with long name",
        mockUI.hasMessageContaining("Event created successfully"));

    // Create very long description
    mockUI.clearMessages();
    StringBuilder longDesc = new StringBuilder();
    for (int i = 0; i < 10; i++) {
      longDesc.append("This is paragraph ").append(i + 1).append(" of the description. It contains multiple sentences to make it longer. ");
    }

    processor.processCommand("create event \"Meeting with Description\" from 2025-03-04T14:00 to 2025-03-04T15:00 --description \"" + longDesc.toString() + "\"");

    assertTrue("Should create event with long description",
        mockUI.hasMessageContaining("Event created successfully"));
  }

  /**
   * Test creating events at the boundaries of a day.
   */
  @Test
  public void testEventsAtDayBoundaries() {
    // Event at midnight exactly
    processor.processCommand("create event \"Midnight Event\" from 2025-03-04T00:00 to 2025-03-04T01:00");
    assertTrue("Should create midnight event",
        mockUI.hasMessageContaining("Event created successfully"));

    // Event ending at midnight
    mockUI.clearMessages();
    processor.processCommand("create event \"Late Event\" from 2025-03-04T23:00 to 2025-03-05T00:00");
    assertTrue("Should create event ending at midnight",
        mockUI.hasMessageContaining("Event created successfully"));
  }

  /**
   * Test creating events with exactly the same start and end dates (invalid).
   */
  @Test
  public void testEventWithIdenticalStartAndEndTimes() {
    processor.processCommand("create event \"Zero Duration\" from 2025-03-04T10:00 to 2025-03-04T10:00");
    assertTrue("Should reject event with identical start and end times",
        mockUI.hasErrorMessageContaining("Invalid create event command format"));
  }

  /**
   * Test extreme recurrence patterns.
   */
  @Test
  public void testExtremeRecurrencePatterns() {
    // Event repeating on all days of the week
    processor.processCommand("create event \"Daily Meeting\" from 2025-03-04T10:00 to 2025-03-04T10:30 repeats MTWRFSU for 7 times");
    assertTrue("Should create event repeating on all days",
        mockUI.hasMessageContaining("Recurring event created successfully"));

    // Event with large number of occurrences
    mockUI.clearMessages();
    processor.processCommand("create event \"Many Occurrences\" from 2025-03-04T14:00 to 2025-03-04T14:30 repeats M for 52 times");
    assertTrue("Should create event with many occurrences",
        mockUI.hasMessageContaining("Recurring event created successfully"));

    // Event recurring for a very long time
    mockUI.clearMessages();
    processor.processCommand("create event \"Long Range\" from 2025-03-04T15:00 to 2025-03-04T15:30 repeats F until 2035-12-31");
    assertTrue("Should create event with far-future end date",
        mockUI.hasMessageContaining("Recurring event created successfully"));
  }

  /**
   * Test creating multiple events with the same name.
   */
  @Test
  public void testMultipleEventsWithSameName() {
    // Create multiple events with same name
    processor.processCommand("create event \"Team Meeting\" from 2025-03-04T10:00 to 2025-03-04T11:00");
    processor.processCommand("create event \"Team Meeting\" from 2025-03-05T10:00 to 2025-03-05T11:00");
    processor.processCommand("create event \"Team Meeting\" from 2025-03-06T10:00 to 2025-03-06T11:00");

    // Verify they're all created
    mockUI.clearMessages();
    processor.processCommand("print events from 2025-03-04 to 2025-03-07");

    String output = String.join("\n", mockUI.getAllMessages());


    int count = output.split("Team Meeting").length - 1;
    assertTrue("Should find multiple events with same name", count >= 3);

    mockUI.clearMessages();
    processor.processCommand("edit events name \"Team Meeting\" \"Department Meeting\"");
    processor.processCommand("print events from 2025-03-04 to 2025-03-07");
    String output2 = String.join("\n", mockUI.getAllMessages());
    assertTrue("Should update all events with same name",
        mockUI.hasMessageContaining("All events updated successfully"));
  }

  /**
   * Test handling of malformed commands.
   */
  @Test
  public void testMalformedCommands() {
    // Command with unbalanced quotes
    processor.processCommand("create event \"Unbalanced Quotes from 2025-03-04T10:00 to 2025-03-04T11:00");
    assertTrue("Should reject command with unbalanced quotes",
        mockUI.hasErrorMessageContaining("Invalid create event command format"));

    // Command with invalid date format
    mockUI.clearMessages();
    processor.processCommand("create event \"Invalid Date\" from 04-03-2025T10:00 to 04-03-2025T11:00");
    assertTrue("Should reject command with invalid date format",
        mockUI.hasErrorMessageContaining("Invalid create event command format"));

    // Command with missing parts
    mockUI.clearMessages();
    processor.processCommand("create event from 2025-03-04T10:00 to 2025-03-04T11:00");
    assertTrue("Should reject command missing event name",
        mockUI.hasErrorMessageContaining("Invalid create event command format"));
  }

  /**
   * Test calendar export with various event types.
   */
  @Test
  public void testCalendarExportWithVariousEvents() {
    // Create regular event
    processor.processCommand("create event \"Regular Meeting\" from 2025-03-04T10:00 to 2025-03-04T11:00");

    // Create all-day event
    processor.processCommand("create event \"All-day Conference\" on 2025-03-05");

    // Create recurring event
    processor.processCommand("create event \"Weekly Review\" from 2025-03-06T14:00 to 2025-03-06T15:00 repeats F for 4 times");

    // Create event with special characters and long description
    processor.processCommand("create event \"Q1 Report: Analysis & Future Plans\" from 2025-03-07T09:00 to 2025-03-07T10:00 " +
        "--description \"Quarterly financial review with detailed analysis of performance metrics, variance explanations, and forecasting for the next quarter.\"");

    // Export and verify
    mockUI.clearMessages();
    processor.processCommand("export cal edge_case_test.csv");
    assertTrue("Should export calendar successfully", mockUI.hasMessageContaining("Calendar exported to:"));

    // Verify the file exists
    File exportFile = new File("edge_case_test.csv");
    assertTrue("Export file should exist", exportFile.exists());
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

    public List<String> getAllMessages() {
      return new ArrayList<>(messages);
    }

    public void clearMessages() {
      messages.clear();
      errors.clear();
    }
  }
}