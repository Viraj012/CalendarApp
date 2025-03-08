import controller.CommandProcessor;
import model.Calendar;
import model.CalendarImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import view.TextUI;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for the Calendar application.
 * These tests verify the interaction between multiple components.
 */
public class CalendarIntegrationTest {

  private Calendar calendar;
  private MockTextUI mockUI;
  private CommandProcessor processor;
  private PrintStream originalOut;
  private PrintStream originalErr;
  private ByteArrayOutputStream outContent;
  private ByteArrayOutputStream errContent;

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
  }

  @After
  public void tearDown() {
    // Restore original output streams
    System.setOut(originalOut);
    System.setErr(originalErr);

    // Clean up any exported files
    File exportFile = new File("integration_test_export.csv");
    if (exportFile.exists()) {
      exportFile.delete();
    }
  }

  @Test
  public void testCreateAndQueryEvent() {
    // Create an event
    assertTrue(processor.processCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00"));

    // Query events on that day
    assertTrue(processor.processCommand("print events on 2025-03-04"));

    // Verify the event was found
    assertTrue("Should display the created event",
        mockUI.hasMessageContaining("Meeting") && mockUI.hasMessageContaining("10:00"));
  }

  @Test
  public void testCreateEditAndQueryEvent() {
    // Create an event
    assertTrue(processor.processCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00"));

    // Edit the event
    assertTrue(processor.processCommand("edit event name \"Meeting\" from 2025-03-04T10:00 to 2025-03-04T11:00 with \"Team Meeting\""));

    // Query events
    assertTrue(processor.processCommand("print events on 2025-03-04"));

    // Verify the edit was applied
    assertTrue("Should display edited event name", mockUI.hasMessageContaining("Team Meeting"));
  }


  @Test
  public void testCreateEventsAndCheckStatus() {
    // Create an event
    assertTrue(processor.processCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00"));

    // Check status during the event
    assertTrue(processor.processCommand("show status on 2025-03-04T10:30"));
    assertTrue("Should show busy status", mockUI.hasMessageContaining("busy"));

    // Check status outside the event
    mockUI.clearMessages();
    assertTrue(processor.processCommand("show status on 2025-03-04T12:00"));
    assertTrue("Should show available status", mockUI.hasMessageContaining("available"));
  }

  @Test
  public void testCreateEventsWithConflict() {
    // Create first event
    assertTrue(processor.processCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00"));

    // Try to create a conflicting event with autoDecline
    assertTrue(processor.processCommand("create event --autoDecline Conflict from 2025-03-04T10:30 to 2025-03-04T11:30"));
    assertTrue("Should reject conflicting event", mockUI.hasErrorMessageContaining("conflict detected"));

    // Try to create a conflicting event without autoDecline
    mockUI.clearMessages();
    assertTrue(processor.processCommand("create event Conflict from 2025-03-04T10:30 to 2025-03-04T11:30"));
    assertTrue("Should accept conflicting event without autoDecline", mockUI.hasMessageContaining("created successfully"));

    // Verify both events exist
    mockUI.clearMessages();
    assertTrue(processor.processCommand("print events on 2025-03-04"));
    assertTrue("Should display both events",
        mockUI.hasMessageContaining("Meeting") && mockUI.hasMessageContaining("Conflict"));
  }

  @Test
  public void testCreateAndExport() {
    // Create some events
    assertTrue(processor.processCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00"));
    assertTrue(processor.processCommand("create event Conference on 2025-03-05"));

    // Export to CSV
    assertTrue(processor.processCommand("export cal integration_test_export.csv"));
    assertTrue("Should confirm export", mockUI.hasMessageContaining("exported to"));

    // Verify file exists
    File exportFile = new File("integration_test_export.csv");
    assertTrue("Export file should exist", exportFile.exists());
  }

  @Test
  public void testEditNonexistentEvent() {
    // Try to edit an event that doesn't exist
    assertTrue(processor.processCommand("edit event name \"Nonexistent\" from 2025-03-04T10:00 to 2025-03-04T11:00 with \"New Name\""));
    assertTrue("Should display error for nonexistent event",
        mockUI.hasErrorMessageContaining("not found"));
  }

  @Test
  public void testExitCommand() {
    // Process exit command
    assertFalse("Exit command should return false", processor.processCommand("exit"));
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