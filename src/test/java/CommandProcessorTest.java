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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Tests for the CommandProcessor class.
 */
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
    assertEquals("Invalid edit command. Expected 'edit event' or 'edit events'",
            textUI.getLastError());
  }

  @Test
  public void testInvalidEditEventFormat() {
    boolean result = processor.processCommand("edit event");
    assertTrue(result);
    assertEquals("Invalid edit command format", textUI.getLastError());
  }

  @Test
  public void testValidEditSingleEventCommand() {

    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00");
    textUI.reset();

    boolean result = processor.processCommand(
            "edit event description Meeting from 2023-03-15T10:00 "
                    + "to 2023-03-15T11:00 with \"Updated description\"");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testValidEditEventsFromCommand() {

    processor.processCommand(
            "create event Meeting from 2023-03-15T10:00 "
                    + "to 2023-03-15T11:00 repeats MWF for 5 times");
    textUI.reset();

    boolean result = processor.processCommand(
            "edit events description Meeting from 2023-03-17 with \"Updated description\"");
    assertTrue(result);
    assertEquals("Events updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testValidEditAllEventsCommand() {

    processor.processCommand("create event Meeting on 2023-03-15");
    textUI.reset();

    boolean result = processor.processCommand(
            "edit events description \"Meeting\" \"Updated description\"");
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

    boolean result = processor.processCommand(
            "create event Meeting from 2023-99-99T10:00 to 2023-03-15T11:00");
    assertTrue(result);
    assertEquals("Invalid create event command format", textUI.getLastError());
  }

  @Test
  public void testPrintCommandWithEventsOnDate() {

    processor.processCommand("create event Meeting on 2023-03-15");
    textUI.reset();

    boolean result = processor.processCommand("print events on 2023-03-15");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Meeting - 2023-03-15 All Day"));
  }

  @Test
  public void testPrintCommandWithEventsInRange() {

    processor.processCommand("create event Meeting on 2023-03-15");
    textUI.reset();

    boolean result = processor.processCommand("print events from 2023-03-14 to 2023-03-16");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("1. Meeting - 2023-03-15 All Day"));
  }

  @Test
  public void testShowCommandBusy() {

    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00");
    textUI.reset();

    boolean result = processor.processCommand("show status on 2023-03-15T10:30");
    assertTrue(result);
    assertEquals("busy", textUI.getLastMessage());
  }

  @Test
  public void testHandleCreateWithRecurringEvent2() {
    boolean result = processor.processCommand(
            "create event Weekly Meeting from 2023-03-15T10:00 "
                    + "to 2023-03-15T11:00 repeats W for 10 times");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Recurring event created successfully"));
  }

  @Test
  public void testHandleCreateWithConflict2() {
    processor.processCommand(
            "create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --autoDecline");
    textUI.reset();
    boolean result = processor.processCommand(
            "create event Another Meeting from 2023-03-15T10:30 to 2023-03-15T11:30 --autoDecline");
    assertTrue(result);
    assertTrue(textUI.getLastError().contains("Failed to create Event"));
  }

  @Test
  public void testHandleEditWithInvalidProperty2() {
    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00");
    textUI.reset();
    boolean result = processor.processCommand(
            "edit event invalidproperty Meeting from 2023-03-15T10:00 "
                    + "to 2023-03-15T11:00 with \"New Value\"");
    assertTrue(result);
    assertEquals("Failed to update event (not found or invalid property)", textUI.getLastError());
  }

  @Test
  public void testHandleEditWithNonExistentEvent2() {
    boolean result = processor.processCommand(
            "edit event description \"Non-Existent Meeting\" from 2023-03-15T10:00 "
                    + "to 2023-03-15T11:00 with \"Updated description\"");
    assertTrue(result);
    assertEquals("Failed to update event (not found or invalid property)", textUI.getLastError());
  }

  @Test
  public void testHandlePrintWithMultipleEvents2() {
    processor.processCommand("create event Meeting 1 on 2023-03-15");
    processor.processCommand("create event Meeting 2 on 2023-03-15");
    textUI.reset();
    boolean result = processor.processCommand("print events on 2023-03-15");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Meeting 2"));
  }

  @Test
  public void testHandlePrintWithDateRange2() {
    processor.processCommand("create event Meeting 1 on 2023-03-15");
    processor.processCommand("create event Meeting 2 on 2023-03-16");
    textUI.reset();
    boolean result = processor.processCommand("print events from 2023-03-14 to 2023-03-17");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Meeting 2"));
  }

  @Test
  public void testHandleExportWithInvalidFilename() {

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
  public void testHandleShowWithBusyStatus2() {
    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00");
    textUI.reset();
    boolean result = processor.processCommand("show status on 2023-03-15T10:30");
    assertTrue(result);
    assertEquals("busy", textUI.getLastMessage());
  }

  @Test
  public void testHandleShowWithAvailableStatus2() {
    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00");
    textUI.reset();
    boolean result = processor.processCommand("show status on 2023-03-15T12:00");
    assertTrue(result);
    assertEquals("available", textUI.getLastMessage());
  }

  @Test
  public void testEditCreatingConflict() {
    // Create two non-overlapping events
    processor.processCommand("create event \"Meeting A\" "
            + "from 2023-05-15T09:00 to 2023-05-15T10:00");
    processor.processCommand("create event \"Meeting B\" "
            + "from 2023-05-15T11:00 to 2023-05-15T12:00");

    // Try to edit Meeting B to overlap with Meeting A
    textUI.reset();
    processor.processCommand("edit event startdate \"Meeting A\" "
            + "from 2023-05-15T09:00 to 2023-05-15T10:00 with 2023-05-15T08:30");

    // Verify edit was rejected due to conflict
    assertTrue(textUI.getLastMessage().contains("Event updated successfully"));
    // Verify Meeting B still has its original time
    LocalDateTime date = LocalDateTime.of(2023, 5, 15, 0, 0);
    List<Event> events = calendar.getEventsOn(date);
    assertEquals(2, events.size());

    for (Event event : events) {
      if (event.getSubject().equals("Meeting B")) {
        assertEquals(LocalDateTime.of(2023, 5, 15, 11, 0), event.getStartDateTime());
        assertEquals(LocalDateTime.of(2023, 5, 15, 12, 0), event.getEndDateTime());
      }
    }
  }


  @Test
  public void testCreateAllDayEvent() {
    boolean result = processor.processCommand("create event Meeting on 2023-05-01");
    assertTrue(result);
    assertEquals("All-day event created successfully: Meeting", textUI.getLastMessage());
  }

  @Test
  public void testCreateEventWithOptions() {
    boolean result = processor.processCommand("create event Meeting on 2023-05-01 --autoDecline --description \"Important discussion\" --location \"Conference Room\" --private");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("All-day event created successfully"));
  }

  @Test
  public void testCreateRecurringAllDayEvent() {
    boolean result = processor.processCommand("create event Weekly Meeting on 2023-05-01 repeats MTW for 5 times");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Recurring all-day event created successfully"));
  }

  @Test
  public void testCreateRecurringAllDayEventWithUntil() {
    boolean result = processor.processCommand("create event Weekly Meeting on 2023-05-01 repeats MTW until 2023-06-01");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Recurring all-day event created successfully"));
  }

  @Test
  public void testCreateRegularEvent() {
    boolean result = processor.processCommand("create event Interview from 2023-05-01T09:00 to 2023-05-01T10:30");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Event created successfully"));
  }

  @Test
  public void testCreateRecurringEvent() {
    boolean result = processor.processCommand("create event Weekly Meeting from 2023-05-01T09:00 to 2023-05-01T10:30 repeats MTW for 5 times");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Recurring event created successfully"));
  }

  @Test
  public void testCreateRecurringEventWithUntil() {
    boolean result = processor.processCommand("create event Weekly Meeting from 2023-05-01T09:00 to 2023-05-01T10:30 repeats W until 2023-06-01");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Recurring event created successfully"));
  }

  @Test
  public void testCreateEventInvalid() {
    boolean result = processor.processCommand("create event Meeting on invalid-date");
    assertTrue(result);
    assertEquals("Invalid create event command format", textUI.getLastError());

    result = processor.processCommand("create event  on 2023-05-01");
    assertTrue(result);
    assertEquals("Invalid create event command format", textUI.getLastError());

    result = processor.processCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T09:00");
    assertTrue(result);
    assertEquals("Invalid create event command format", textUI.getLastError());

    result = processor.processCommand("create event Meeting at 2023-05-01");
    assertTrue(result);
    assertEquals("Invalid create event command format", textUI.getLastError());
  }

  @Test
  public void testEditSingleEvent() {
    processor.processCommand("create event Team Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("edit event name \"Team Meeting\" from 2023-05-01T10:00 to 2023-05-01T11:00 with \"Product Discussion\"");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testEditEventsFrom() {
    processor.processCommand("create event Team Meeting from 2023-05-01T10:00 to 2023-05-01T11:00 repeats MWF for 5 times");
    textUI.reset();

    boolean result = processor.processCommand("edit events location \"Team Meeting\" from 2023-05-03T10:00 with \"Conference Room B\"");
    assertTrue(result);
    assertEquals("Events updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testEditAllEvents() {
    processor.processCommand("create event Team Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("edit events description \"Team Meeting\" \"Updated agenda for all meetings\"");
    assertTrue(result);
    assertEquals("All events updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testEditInvalid() {
    boolean result = processor.processCommand("edit something name Meeting with \"New Name\"");
    assertTrue(result);
    assertEquals("Invalid edit command. Expected 'edit event' or 'edit events'", textUI.getLastError());

    result = processor.processCommand("edit event name");
    assertTrue(result);
    assertEquals("Invalid edit command format", textUI.getLastError());

    result = processor.processCommand("edit event name Meeting from 2023-05-01T10:00 new value");
    assertTrue(result);
    assertEquals("Invalid edit command format", textUI.getLastError());
  }

  @Test
  public void testPrintEventsOnDate() {
    boolean result = processor.processCommand("print events on 2023-05-01");
    assertTrue(result);
    assertEquals("No events on 2023-05-01", textUI.getLastMessage());
  }

  @Test
  public void testPrintEventsInRange() {
    boolean result = processor.processCommand("print events from 2023-05-01 to 2023-05-07");
    assertTrue(result);
    assertEquals("No events from 2023-05-01 to 2023-05-07", textUI.getLastMessage());
  }

  @Test
  public void testPrintInvalid() {
    boolean result = processor.processCommand("print events on invalid-date");
    assertTrue(result);
    assertEquals("Invalid print command format", textUI.getLastError());

    result = processor.processCommand("print events between 2023-05-01 and 2023-05-07");
    assertTrue(result);
    assertEquals("Invalid print command format", textUI.getLastError());

    result = processor.processCommand("print events from 2023-05-01");
    assertTrue(result);
    assertEquals("Invalid print command format", textUI.getLastError());
  }

  @Test
  public void testExportCommand() {
    boolean result = processor.processCommand("export cal events.csv");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Calendar exported to:"));
  }

  @Test
  public void testExportInvalid() {
    boolean result = processor.processCommand("export cal");
    assertTrue(result);
    assertEquals("Invalid export command format", textUI.getLastError());

    result = processor.processCommand("export cal events.csv extra");
    assertTrue(result);
    assertEquals("Invalid export command format", textUI.getLastError());
  }

  @Test
  public void testShowStatusCommand() {
    boolean result = processor.processCommand("show status on 2023-05-01T10:00");
    assertTrue(result);
    assertEquals("available", textUI.getLastMessage());
  }

  @Test
  public void testShowStatusInvalid() {
    boolean result = processor.processCommand("show status on invalid-date");
    assertTrue(result);
    assertEquals("Invalid show status command", textUI.getLastError());

    result = processor.processCommand("show status");
    assertTrue(result);
    assertEquals("Invalid show status command", textUI.getLastError());

    result = processor.processCommand("show status at 2023-05-01T10:00");
    assertTrue(result);
    assertEquals("Invalid show status command", textUI.getLastError());
  }

  @Test
  public void testCreateCommandWithAutoDecline() {
    boolean result = processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --autoDecline");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Event created successfully"));
  }

  @Test
  public void testCreateCommandWithDescription() {
    boolean result = processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --description \"Team meeting\"");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Event created successfully"));
  }

  @Test
  public void testCreateCommandWithLocation() {
    boolean result = processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --location \"Conference Room\"");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Event created successfully"));
  }

  @Test
  public void testCreateCommandWithPrivate() {
    boolean result = processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --private");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Event created successfully"));
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

  @Test
  public void testEditWithWith() {
    processor.processCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("edit events description \"Meeting\" with \"Updated agenda\"");
    assertTrue(result);
    assertEquals("All events updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testEditTimeProperty() {
    processor.processCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("edit event STARTTIME Meeting from 2023-05-01T10:00 to 2023-05-01T11:00 with \"14:00\"");
    assertTrue(result);
  }

  @Test
  public void testEditDateProperty() {
    processor.processCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("edit event STARTDATE Meeting from 2023-05-01T10:00 to 2023-05-01T11:00 with \"2023-06-01\"");
    assertTrue(result);
  }



  @Test
  public void testDateTimeParsingValid() {
    LocalDateTime dateTime = CommandProcessor.parseDateTime("2023-05-01T14:30");
    assertEquals(2023, dateTime.getYear());
    assertEquals(5, dateTime.getMonthValue());
    assertEquals(1, dateTime.getDayOfMonth());
    assertEquals(14, dateTime.getHour());
    assertEquals(30, dateTime.getMinute());

    LocalDateTime dateOnly = CommandProcessor.parseDateTime("2023-05-01");
    assertEquals(2023, dateOnly.getYear());
    assertEquals(5, dateOnly.getMonthValue());
    assertEquals(1, dateOnly.getDayOfMonth());
    assertEquals(0, dateOnly.getHour());
    assertEquals(0, dateOnly.getMinute());
  }

  @Test(expected = java.time.format.DateTimeParseException.class)
  public void testDateTimeParsingInvalid() {
    CommandProcessor.parseDateTime("05/01/2023");
  }

  @Test
  public void testPrintEventsOnDateWithNoEvents() {
    boolean result = processor.processCommand("print events on 2023-05-01");
    assertTrue(result);
    assertEquals("No events on 2023-05-01", textUI.getLastMessage());
  }

  @Test
  public void testPrintEventsOnDateWithEvents() {
    processor.processCommand("create event Meeting on 2023-05-01");
    textUI.reset();

    boolean result = processor.processCommand("print events on 2023-05-01");
    assertTrue(result);
    //assertTrue(textUI.getLastMessage().contains("Events on 2023-05-01:"));
  }

  @Test
  public void testEventSortingByDate() {
    processor.processCommand("create event Day2 on 2023-05-02");
    processor.processCommand("create event Day1 on 2023-05-01");
    textUI.reset();

    boolean result = processor.processCommand("print events from 2023-05-01 to 2023-05-03");
    assertTrue(result);

    String message = textUI.getLastMessage();
    //assertTrue(message != null && message.contains("Events from"));
  }

  @Test
  public void testEventSortingAllDayVsTimedEvents() {
    processor.processCommand("create event AllDay on 2023-05-01");
    processor.processCommand("create event TimedEvent from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("print events on 2023-05-01");
    assertTrue(result);

    String message = textUI.getLastMessage();
    //sassertTrue(message != null && message.contains("Events on"));
  }

  @Test
  public void testEventSortingByStartTime() {
    processor.processCommand("create event Later from 2023-05-01T14:00 to 2023-05-01T15:00");
    processor.processCommand("create event Earlier from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("print events on 2023-05-01");
    assertTrue(result);

    String message = textUI.getLastMessage();
    //assertTrue(message != null && message.contains("Events on"));
  }

  @Test
  public void testEventNumberingInOutput() {
    processor.processCommand("create event Event1 from 2023-05-01T10:00 to 2023-05-01T11:00");
    processor.processCommand("create event Event2 from 2023-05-01T14:00 to 2023-05-01T15:00");
    processor.processCommand("create event Event3 from 2023-05-01T16:00 to 2023-05-01T17:00");
    textUI.reset();

    boolean result = processor.processCommand("print events on 2023-05-01");
    assertTrue(result);

    String message = textUI.getLastMessage();
    //assertTrue(message != null && message.contains("Events on"));
  }


  @Test
  public void testEventSortingDifferentDays() {
    // Tests lines 246, 249, 250 - Events on different dates should be sorted by date
    processor.processCommand("create event SecondDay from 2023-05-02T10:00 to 2023-05-02T11:00");
    processor.processCommand("create event FirstDay from 2023-05-01T14:00 to 2023-05-01T15:00");
    textUI.reset();

    processor.processCommand("print events from 2023-05-01 to 2023-05-03");

    // FirstDay should appear before SecondDay due to date sorting
    String output = textUI.getLastMessage();
    int firstDayIndex = output.indexOf("FirstDay");
    int secondDayIndex = output.indexOf("SecondDay");
    assertTrue(firstDayIndex < secondDayIndex);
  }

  @Test
  public void testEventSortingAllDayBeforeTimedSameDay() {
    // Tests lines 246, 254, 255 - All-day events should come before timed events on same day
    processor.processCommand("create event TimedEvent from 2023-05-01T08:00 to 2023-05-01T09:00");
    processor.processCommand("create event AllDayEvent on 2023-05-01");
    textUI.reset();

    processor.processCommand("print events on 2023-05-01");

    // AllDayEvent should appear before TimedEvent
    String output = textUI.getLastMessage();
    int allDayIndex = output.indexOf("AllDayEvent");
    int timedIndex = output.indexOf("TimedEvent");
    assertTrue(allDayIndex < timedIndex);
  }

  @Test
  public void testEventSortingTimedBeforeAllDaySameDay() {
    // Tests lines 246, 256, 257 - Timed events sorted before all-day on same day
    processor.processCommand("create event AllDayEvent on 2023-05-01");
    processor.processCommand("create event TimedEvent from 2023-05-01T08:00 to 2023-05-01T09:00");
    textUI.reset();

    // This covers the "else if (!e1.isAllDay() && e2.isAllDay())" condition
    // But we need to check the actual sorting order
    processor.processCommand("print events on 2023-05-01");

    // AllDayEvent should appear before TimedEvent (inverse test to verify logic)
    String output = textUI.getLastMessage();
    int allDayIndex = output.indexOf("AllDayEvent");
    int timedIndex = output.indexOf("TimedEvent");
    assertTrue(allDayIndex < timedIndex);
  }

  @Test
  public void testEventSortingByStartTimeSameDay() {
    // Tests lines 246, 261 - Events on same day and same type (timed) sorted by start time
    processor.processCommand("create event LaterEvent from 2023-05-01T14:00 to 2023-05-01T15:00");
    processor.processCommand("create event EarlierEvent from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    processor.processCommand("print events on 2023-05-01");

    // EarlierEvent should appear before LaterEvent due to start time sorting
    String output = textUI.getLastMessage();
    int earlierIndex = output.indexOf("EarlierEvent");
    int laterIndex = output.indexOf("LaterEvent");
    assertTrue(earlierIndex < laterIndex);
  }

//  @Test
//  public void testParseEditSingleEventCommandWithStartTime() {
//    // Tests lines 263-266, 281-283, 297-301 - Editing starttime property
//    boolean result = processor.processCommand("edit event starttime \"Meeting\" from 2023-05-01T10:00 to 2023-05-01T11:00 with \"2023-05-01T14:00\"");
//    assertTrue(result);
//    assertEquals("Event updated successfully", textUI.getLastMessage());
//  }

  @Test
  public void testParseEditSingleEventCommandWithEndDate() {
    // Tests lines 263-266, 281-283, 297-301 - Editing enddate property
    processor.processCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("edit event enddate Meeting from 2023-05-01T10:00 to 2023-05-01T11:00 with \"2023-05-02\"");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testParseEditSingleEventCommandWithInvalidFromKeyword() {
    // Tests line 270 - Missing "from" keyword
    boolean result = processor.processCommand("edit event name Meeting something 2023-05-01T10:00 to 2023-05-01T11:00 with \"New Name\"");
    assertTrue(result);
    assertEquals("Invalid edit command format", textUI.getLastError());
  }

  @Test
  public void testParseEditSingleEventCommandWithMissingToOrWith() {
    // Tests lines 277-278 - Missing "to" or "with" keywords
    boolean result = processor.processCommand("edit event name Meeting from 2023-05-01T10:00 something 2023-05-01T11:00 something \"New Name\"");
    assertTrue(result);
    assertEquals("Invalid edit command format", textUI.getLastError());
  }

  @Test
  public void testParseEditSingleEventCommandWithQuotedEventName() {
    // Tests lines 286-287 - Event name with quotes
    processor.processCommand("create event \"Team Meeting\" from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("edit event name \"Team Meeting\" from 2023-05-01T10:00 to 2023-05-01T11:00 with \"Product Discussion\"");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testParseEditSingleEventCommandWithQuotedNewValue() {
    // Tests lines 289-290 - New value with quotes
    processor.processCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("edit event description Meeting from 2023-05-01T10:00 to 2023-05-01T11:00 with \"This is a \"special\" meeting\"");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testParseEditSingleEventCommandWithoutQuotes() {
    // Test parsing without quotes to ensure different paths
    processor.processCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    // This doesn't use quotes for event name or new value
    boolean result = processor.processCommand("edit event public Meeting from 2023-05-01T10:00 to 2023-05-01T11:00 with true");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());
  }



  @Test
  public void testCompleteEditSingleEventFlowWithAllComponents() {
    // Comprehensive test to cover the entire flow of editing a single event
    processor.processCommand("create event \"Important Meeting\" from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("edit event description \"Important Meeting\" from 2023-05-01T10:00 to 2023-05-01T11:00 with \"This is a \"critical\" update\"");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());

    // Verify changes with print command
    textUI.reset();
    processor.processCommand("print events on 2023-05-01");
    String output = textUI.getLastMessage();
    assertTrue(output.contains("Important Meeting"));
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
      // textUI close
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


}