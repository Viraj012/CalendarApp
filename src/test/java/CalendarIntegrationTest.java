import controller.CommandProcessor;
import model.Calendar;
import model.CalendarImpl;
import model.Event;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import view.TextUI;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for the Calendar application.
 * Tests the full flow of commands through the application.
 */
public class CalendarIntegrationTest {
  private Calendar calendar;
  private MockTextUI mockUI;
  private CommandProcessor processor;
  private final String EXPORT_FILE = "test_export_integration.csv";

  @Before
  public void setUp() {
    calendar = new CalendarImpl();
    mockUI = new MockTextUI();
    processor = new CommandProcessor(calendar, mockUI);
  }

  @After
  public void tearDown() {
    // Clean up exported files
    File exportFile = new File(EXPORT_FILE);
    if (exportFile.exists()) {
      exportFile.delete();
    }
  }

//  @Test
//  public void testFullApplicationFlow() {
//    // Test creating a regular event
//    assertTrue(processor.processCommand("create event Team Meeting from 2025-03-15T14:00 to 2025-03-15T15:30 --description \"Weekly team sync\" --location \"Conference Room A\""));
//    assertTrue(mockUI.getLastMessage().contains("Event created successfully"));
//    mockUI.clearMessages();
//
//    // Test creating an all-day event
//    assertTrue(processor.processCommand("create event Conference on 2025-03-20 --description \"Annual tech conference\" --location \"Convention Center\""));
//    assertTrue(mockUI.getLastMessage().contains("All-day event created successfully"));
//    mockUI.clearMessages();
//
//    // Test creating a recurring event
//    assertTrue(processor.processCommand("create event Weekly Status from 2025-03-10T10:00 to 2025-03-10T11:00 repeats M for 4 times --description \"Weekly team status\" --location \"Meeting Room B\""));
//    assertTrue(mockUI.getLastMessage().contains("Recurring event created successfully"));
//    mockUI.clearMessages();
//
//    // Test creating a private event with auto-decline
//    assertTrue(processor.processCommand("create event Private Meeting from 2025-03-16T09:00 to 2025-03-16T10:00 --private --autoDecline"));
//    assertTrue(mockUI.getLastMessage().contains("Event created successfully"));
//    mockUI.clearMessages();
//
//    // Test printing events for a specific day
//    assertTrue(processor.processCommand("print events on 2025-03-15"));
//    assertTrue(mockUI.getLastMessage().contains("Events on 2025-03-15"));
//    assertTrue(mockUI.getAllMessages().stream().anyMatch(msg -> msg.contains("Team Meeting")));
//    mockUI.clearMessages();
//
//    // Test printing events for a date range
//    assertTrue(processor.processCommand("print events from 2025-03-10 to 2025-03-20"));
//    assertTrue(mockUI.getLastMessage().contains("Events from 2025-03-10 to 2025-03-20"));
//    assertTrue(mockUI.getAllMessages().stream().anyMatch(msg -> msg.contains("Team Meeting")));
//    assertTrue(mockUI.getAllMessages().stream().anyMatch(msg -> msg.contains("Weekly Status")));
//    assertTrue(mockUI.getAllMessages().stream().anyMatch(msg -> msg.contains("Conference")));
//    mockUI.clearMessages();
//
//    // Test editing a specific event
//    assertTrue(processor.processCommand("edit event name Team Meeting from 2025-03-15T14:00 to 2025-03-15T15:30 with \"Strategy Meeting\""));
//    assertEquals("Event updated successfully", mockUI.getLastMessage());
//    mockUI.clearMessages();
//
//    // Verify the edit worked
//    assertTrue(processor.processCommand("print events on 2025-03-15"));
//    assertTrue(mockUI.getAllMessages().stream().anyMatch(msg -> msg.contains("Strategy Meeting")));
//    assertFalse(mockUI.getAllMessages().stream().anyMatch(msg -> msg.contains("Team Meeting")));
//    mockUI.clearMessages();
//
//    // Test editing all recurring events
//    assertTrue(processor.processCommand("edit events location \"Weekly Status\" with \"Virtual Meeting Room\""));
//    assertEquals("Events updated successfully", mockUI.getLastMessage());
//    mockUI.clearMessages();
//
//    // Test showing busy status
//    assertTrue(processor.processCommand("show status on 2025-03-15T14:30"));
//    assertEquals("busy", mockUI.getLastMessage());
//    mockUI.clearMessages();
//
//    assertTrue(processor.processCommand("show status on 2025-03-15T13:00"));
//    assertEquals("available", mockUI.getLastMessage());
//    mockUI.clearMessages();
//
//    // Test exporting the calendar
//    assertTrue(processor.processCommand("export cal " + EXPORT_FILE));
//    assertTrue(mockUI.getLastMessage().contains("Calendar exported to"));
//    mockUI.clearMessages();
//
//    // Verify export file exists and contains expected content
//    File exportFile = new File(EXPORT_FILE);
//    assertTrue(exportFile.exists());
//
//    try {
//      List<String> lines = Files.readAllLines(Paths.get(EXPORT_FILE));
//      assertTrue(lines.size() >= 5); // Header + at least 4 events
//
//      // Check header
//      assertTrue(lines.get(0).contains("Subject,Start Date"));
//
//      // Check events are in the file
//      boolean foundStrategyMeeting = false;
//      boolean foundConference = false;
//      boolean foundWeeklyStatus = false;
//      boolean foundPrivateMeeting = false;
//
//      for (String line : lines) {
//        if (line.contains("Strategy Meeting")) {
//          foundStrategyMeeting = true;
//          assertTrue(line.contains("03/15/2025"));
//        } else if (line.contains("Conference")) {
//          foundConference = true;
//          assertTrue(line.contains("03/20/2025"));
//          assertTrue(line.contains("True")); // All-day event
//        } else if (line.contains("Weekly Status")) {
//          foundWeeklyStatus = true;
//          assertTrue(line.contains("Virtual Meeting Room"));
//        } else if (line.contains("Private Meeting")) {
//          foundPrivateMeeting = true;
//          assertTrue(line.contains("True")); // Private flag
//        }
//      }
//
//      assertTrue(foundStrategyMeeting);
//      assertTrue(foundConference);
//      assertTrue(foundWeeklyStatus);
//      assertTrue(foundPrivateMeeting);
//
//    } catch (IOException e) {
//      fail("Error reading export file: " + e.getMessage());
//    }
//
//    // Test exiting the application
//    assertFalse(processor.processCommand("exit"));
//  }

  @Test
  public void testErrorHandling() {
    // Test invalid command format
    assertTrue(processor.processCommand("invalid command"));
    assertEquals("Unknown command: invalid", mockUI.getLastError());
    mockUI.clearMessages();

    // Test invalid create command
    assertTrue(processor.processCommand("create something"));
    assertEquals("Invalid create command. Expected 'create event'", mockUI.getLastError());
    mockUI.clearMessages();

    // Test malformed create command
    assertTrue(processor.processCommand("create event Team Meeting without proper format"));
    assertTrue(mockUI.getLastError().contains("Invalid create event command format"));
    mockUI.clearMessages();

    // Test conflict handling with autoDecline
    assertTrue(processor.processCommand("create event First Meeting from 2025-03-15T14:00 to 2025-03-15T15:30"));
    mockUI.clearMessages();

    assertTrue(processor.processCommand("create event Second Meeting from 2025-03-15T14:30 to 2025-03-15T16:00 --autoDecline"));
    assertTrue(mockUI.getLastError().contains("Failed to create") && mockUI.getLastError().contains("conflict detected"));
    mockUI.clearMessages();

    // Test editing non-existent event
    assertTrue(processor.processCommand("edit event name Non-existent Meeting from 2025-03-15T14:00 to 2025-03-15T15:30 with \"New Name\""));
    assertTrue(mockUI.getLastError().contains("Failed to update event"));
    mockUI.clearMessages();

    // Test invalid date format
    assertTrue(processor.processCommand("print events on 15/03/2025"));
    assertTrue(mockUI.getLastError().contains("Invalid print command format"));
    mockUI.clearMessages();
  }

  /**
   * Mock implementation of TextUI for testing.
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

    public String getLastMessage() {
      return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    public List<String> getAllMessages() {
      return new ArrayList<>(messages);
    }

    public String getLastError() {
      return errors.isEmpty() ? null : errors.get(errors.size() - 1);
    }

    public void clearMessages() {
      messages.clear();
      errors.clear();
    }
  }
}