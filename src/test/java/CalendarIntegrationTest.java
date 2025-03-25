import controller.CommandProcessor;
import model.CalendarManager;
import model.Event;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import view.TextUI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Integration tests for the calendar application.
 */
public class CalendarIntegrationTest {

  private CalendarManager manager;
  private MockTextUI mockUI;
  private CommandProcessor processor;

  @Before
  public void setUp() {
    manager = new CalendarManager();
    mockUI = new MockTextUI();
    processor = new CommandProcessor(manager, mockUI);

    manager.createCalendar("Default", ZoneId.systemDefault());
    manager.useCalendar("Default");
  }

  @Test
  public void testCreateCalendarCommand() {

    mockUI.setNextCommand("create calendar --name Work --timezone America/New_York");
    processor.processCommand(mockUI.getCommand());

    assertEquals("Calendar created: Work (America/New_York)", mockUI.getLastMessage());
    assertNotNull("Work calendar should exist", manager.getCalendar("Work"));

    mockUI.setNextCommand("create calendar --name Work --timezone Europe/Paris");
    processor.processCommand(mockUI.getCommand());

    assertEquals("Failed to create calendar (name already exists)", mockUI.getLastError());
  }

  @Test
  public void testEditCalendarCommand() {

    manager.createCalendar("Personal", ZoneId.of("America/Chicago"));

    mockUI.setNextCommand("edit calendar --name Personal --property name Family");
    processor.processCommand(mockUI.getCommand());

    assertEquals("Calendar updated successfully", mockUI.getLastMessage());
    assertNull("Personal calendar should no longer exist",
            manager.getCalendar("Personal"));
    assertNotNull("Family calendar should exist",
            manager.getCalendar("Family"));

    mockUI.setNextCommand("edit calendar --name Family --property timezone Europe/Berlin");
    processor.processCommand(mockUI.getCommand());

    assertEquals("Calendar updated successfully", mockUI.getLastMessage());
    assertEquals("Timezone should be updated",
        ZoneId.of("Europe/Berlin"), manager.getCalendar("Family").getTimezone());
  }

  @Test
  public void testUseCalendarCommand() {

    manager.createCalendar("Work", ZoneId.of("America/New_York"));
    manager.createCalendar("Home", ZoneId.of("America/Los_Angeles"));

    mockUI.setNextCommand("use calendar --name Work");
    processor.processCommand(mockUI.getCommand());

    assertEquals("Now using calendar: Work", mockUI.getLastMessage());
    assertEquals("Current calendar should be Work",
        "Work", manager.getCurrentCalendar().getName());

    mockUI.setNextCommand("use calendar --name NonExistent");
    processor.processCommand(mockUI.getCommand());

    assertEquals("Calendar not found: NonExistent", mockUI.getLastError());
  }

  @Test
  public void testCopyEventCommand() {

    manager.createCalendar("Source", ZoneId.of("America/New_York"));
    manager.createCalendar("Target", ZoneId.of("Europe/Paris"));

    manager.useCalendar("Source");
    LocalDateTime eventTime = LocalDateTime
            .of(2023, 5, 15, 10, 0);
    manager.getCurrentCalendar().createEvent("Meeting", eventTime, eventTime.plusHours(1),
        true, "Team meeting", "Conference room", true);

    mockUI.setNextCommand(
        "copy event \"Meeting\" on 2023-05-15T10:00 --target Target to 2023-05-22T10:00");
    processor.processCommand(mockUI.getCommand());

    assertEquals("Event copied successfully to Target", mockUI.getLastMessage());

    manager.useCalendar("Target");
    List<Event> events = manager.getCurrentCalendar()
        .getEventsOn(LocalDateTime.of(2023, 5, 22, 10, 0));
    assertEquals("There should be one event on the target date", 1, events.size());
    assertEquals("Copied event should have the correct name", "Meeting",
        events.get(0).getSubject());
  }

  @Test
  public void testCopyEventsCommand() {

    manager.createCalendar("Source", ZoneId.of("America/New_York"));
    manager.createCalendar("Target", ZoneId.of("Europe/Paris"));

    manager.useCalendar("Source");

    LocalDateTime day1Morning = LocalDateTime
            .of(2023, 5, 15, 9, 0);
    LocalDateTime day1Afternoon = LocalDateTime
            .of(2023, 5, 15, 14, 0);
    LocalDateTime day2Morning = LocalDateTime
            .of(2023, 5, 16, 9, 0);

    manager.getCurrentCalendar()
        .createEvent("Morning Meeting", day1Morning, day1Morning.plusHours(1),
            true, "Morning Description",
                "Morning Location", true);
    manager.getCurrentCalendar()
        .createEvent("Afternoon Meeting", day1Afternoon, day1Afternoon.plusHours(1),
            true, "Afternoon Description",
                "Afternoon Location", true);
    manager.getCurrentCalendar()
        .createEvent("Next Day Meeting", day2Morning, day2Morning.plusHours(1),
            true, "Next Day Description",
                "Next Day Location", true);

    mockUI.setNextCommand("copy events on 2023-05-15 --target Target to 2023-05-22");
    processor.processCommand(mockUI.getCommand());

    assertEquals("Events copied successfully to Target", mockUI.getLastMessage());

    manager.useCalendar("Target");
    List<Event> targetEvents = manager.getCurrentCalendar()
        .getEventsOn(LocalDateTime.of(2023, 5, 22, 0, 0));
    assertEquals("There should be two events on the target date",
            2, targetEvents.size());

    manager.useCalendar("Source");
    mockUI.setNextCommand(
        "copy events between 2023-05-15 and 2023-05-16 --target Target to 2023-06-01");
    processor.processCommand(mockUI.getCommand());

    assertEquals("Events copied successfully to Target", mockUI.getLastMessage());
  }

  @Test
  public void testEventOperationsWithoutCalendarContext() {

    CalendarManager emptyManager = new CalendarManager();
    CommandProcessor emptyProcessor = new CommandProcessor(emptyManager, mockUI);

    mockUI.setNextCommand("create event Test on 2023-05-15");
    emptyProcessor.processCommand(mockUI.getCommand());

    assertEquals("No calendar in use. Please use a calendar first.",
            mockUI.getLastError());

    mockUI.setNextCommand(
        "edit event name \"Test\" from 2023-05-15T10:00 to 2023-05-15T11:00 with \"Updated Test\"");
    emptyProcessor.processCommand(mockUI.getCommand());

    assertEquals("No calendar in use. Please use a calendar first.",
            mockUI.getLastError());
  }

  @Test
  public void testAutoDeclineAndConflictHandling() {

    manager.createCalendar("Conflicts", ZoneId.of("UTC"));
    manager.useCalendar("Conflicts");

    mockUI.setNextCommand(
        "create event \"First Meeting\" from 2023-05-15T10:00 to 2023-05-15T11:00");
    processor.processCommand(mockUI.getCommand());
    assertTrue("First event should be created successfully",
        mockUI.getLastMessage().contains("Event created successfully"));

    mockUI.setNextCommand(
        "create event \"Second Meeting\" from 2023-05-15T10:30 to 2023-05-15T11:30");
    processor.processCommand(mockUI.getCommand());
    assertTrue("Conflicting event should be declined",
        mockUI.getLastError().contains("Failed to create"));

    mockUI.setNextCommand(
        "create event \"Third Meeting\" from 2023-05-15T11:00 to 2023-05-15T12:00");
    processor.processCommand(mockUI.getCommand());
    assertTrue("Non-conflicting event should be created successfully",
        mockUI.getLastMessage().contains("Event created successfully"));

    mockUI.setNextCommand(
        "edit event starttime \"Third Meeting\" from 2023-05-15T11:00 " +
                "to 2023-05-15T12:00 with 2023-05-15T10:45");
    processor.processCommand(mockUI.getCommand());
    assertTrue("Edit creating conflict should fail",
        mockUI.getLastError().contains("Failed to update event"));
  }

  //  @Test
  //  public void testFullIntegrationFlow() {
  //
  //    mockUI.setNextCommand("create calendar --name Work --timezone America/New_York");
  //    processor.processCommand(mockUI.getCommand());
  //
  //    mockUI.setNextCommand("create calendar --name Personal --timezone Europe/Berlin");
  //    processor.processCommand(mockUI.getCommand());
  //
  //    mockUI.setNextCommand("use calendar --name Work");
  //    processor.processCommand(mockUI.getCommand());
  //
  //    mockUI.setNextCommand(
  //        "create event \"Team Meeting\" from 2023-05-15T10:00 to 2023-05-15T11:00
  //        --description \"Weekly team sync\" --location \"Conference Room A\"");
  //    processor.processCommand(mockUI.getCommand());
  //
  //    mockUI.setNextCommand("create event \"Client Call\" from 2023-05-15T14:00
  //    to 2023-05-15T15:00");
  //    processor.processCommand(mockUI.getCommand());
  //
  //    mockUI.setNextCommand("print events on 2023-05-15");
  //    processor.processCommand(mockUI.getCommand());
  //
  //    assertTrue("Output should show both events",
  //        mockUI.getLastMessage().contains("Team Meeting") ||
  //            mockUI.getLastMessage().contains("Client Call"));
  //
  //    mockUI.setNextCommand("copy events on 2023-05-15 --target Personal to 2023-05-22");
  //    processor.processCommand(mockUI.getCommand());
  //
  //    assertEquals("Events copied successfully to Personal", mockUI.getLastMessage());
  //
  //    mockUI.setNextCommand("use calendar --name Personal");
  //    processor.processCommand(mockUI.getCommand());
  //
  //    mockUI.setNextCommand("print events on 2023-05-22");
  //    processor.processCommand(mockUI.getCommand());
  //
  //    assertTrue("Output should show both copied events",
  //        mockUI.getLastMessage().contains("Team Meeting") ||
  //            mockUI.getLastMessage().contains("Client Call"));
  //
  //    mockUI.setNextCommand(
  //        "edit event name \"Team Meeting\" from 2023-05-22T10:00
  //        to 2023-05-22T11:00 with \"Team Sync\"");
  //    boolean result = processor.processCommand(mockUI.getCommand());
  //
  //    assertEquals("Event updated successfully", mockUI.getLastMessage());
  //
  //    mockUI.setNextCommand("print events on 2023-05-22");
  //    processor.processCommand(mockUI.getCommand());
  //
  //    assertTrue("Output should show edited event name",
  //        mockUI.getLastMessage().contains("Team Sync") &&
  //            !mockUI.getLastMessage().contains("Team Meeting"));
  //
  //    mockUI.setNextCommand("use calendar --name Work");
  //    processor.processCommand(mockUI.getCommand());
  //
  //    mockUI.setNextCommand("print events on 2023-05-15");
  //    processor.processCommand(mockUI.getCommand());
  //
  //    assertTrue("Output should show original event name",
  //        mockUI.getLastMessage().contains("Team Meeting") &&
  //            !mockUI.getLastMessage().contains("Team Sync"));
  //  }

  /**
   * A simple mock implementation of TextUI for testing.
   */
  private class MockTextUI implements TextUI {

    private String lastMessage;
    private String lastError;
    private String nextCommand;

    @Override
    public void displayMessage(String message) {
      this.lastMessage = message;
    }

    @Override
    public void displayError(String error) {
      this.lastError = error;
    }

    @Override
    public String getCommand() {
      return nextCommand;
    }

    @Override
    public void close() {
      // close the ui
    }

    public void setNextCommand(String command) {
      this.nextCommand = command;
    }

    public String getLastMessage() {
      return lastMessage;
    }

    public String getLastError() {
      return lastError;
    }
  }
}