import controller.CommandProcessor;
import model.Calendar;
import model.CalendarImpl;

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

  private TestTextUI textUI;
  private CommandProcessor processor;

  @Before
  public void setUp() {
    Calendar calendar = new CalendarImpl();
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
    assertEquals("Invalid create command. Expected 'create event' or 'create calendar'",
            textUI.getLastError());
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
    assertEquals("Invalid edit command. Expected 'edit event', 'edit events', " +
                    "or 'edit calendar'",
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
    assertEquals( "Failed to update event (not found, invalid property, " +
            "or would create conflict)", textUI.getLastError());
  }

  @Test
  public void testHandleEditWithNonExistentEvent2() {
    boolean result = processor.processCommand(
        "edit event description \"Non-Existent Meeting\" from 2023-03-15T10:00 "
            + "to 2023-03-15T11:00 with \"Updated description\"");
    assertTrue(result);
    assertEquals("Failed to update event (not found, invalid property, " +
            "or would create conflict)", textUI.getLastError());
  }

  @Test
  public void testHandlePrintWithMultipleEvents2() {
    processor.processCommand("create event Meeting 1 on 2023-03-15");
    processor.processCommand("create event Meeting 2 on 2023-03-15");
    textUI.reset();
    boolean result = processor.processCommand("print events on 2023-03-15");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Meeting 1"));
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
  public void testCreateAllDayEvent() {
    boolean result = processor.processCommand("create event Meeting on 2023-05-01");
    assertTrue(result);
    assertEquals("All-day event created successfully: Meeting", textUI.getLastMessage());
  }

  @Test
  public void testCreateEventWithOptions() {
    boolean result = processor.processCommand(
        "create event Meeting on 2023-05-01 --autoDecline --description " +
                "\"Important discussion\" --location \"Conference Room\" --private");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("All-day event created successfully"));
  }

  @Test
  public void testCreateRecurringAllDayEvent() {
    boolean result = processor.processCommand(
        "create event Weekly Meeting on 2023-05-01 repeats MTW for 5 times");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Recurring all-day event created successfully"));
  }

  @Test
  public void testCreateRecurringAllDayEventWithUntil() {
    boolean result = processor.processCommand(
        "create event Weekly Meeting on 2023-05-01 repeats MTW until 2023-06-01");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Recurring all-day event created successfully"));
  }

  @Test
  public void testCreateRegularEvent() {
    boolean result = processor.processCommand(
        "create event Interview from 2023-05-01T09:00 to 2023-05-01T10:30");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Event created successfully"));
  }

  @Test
  public void testCreateRecurringEvent() {
    boolean result = processor.processCommand(
        "create event Weekly Meeting from 2023-05-01T09:00 to 2023-05-01T10:30 " +
                "repeats MTW for 5 times");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Recurring event created successfully"));
  }

  @Test
  public void testCreateRecurringEventWithUntil() {
    boolean result = processor.processCommand(
        "create event Weekly Meeting from 2023-05-01T09:00 to 2023-05-01T10:30 " +
                "repeats W until 2023-06-01");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Recurring event created successfully"));
    textUI.reset();
    processor.processCommand(
            "print events on 2023-05-03");
    assertEquals("1. Weekly Meeting - 2023-05-03 09:00 to 10:30", textUI.getLastMessage());
  }


  @Test
  public void testCreateEventInvalid() {
    boolean result = processor.processCommand("create event Meeting on invalid-date");
    assertTrue(result);
    assertEquals("Invalid create event command format", textUI.getLastError());

    result = processor.processCommand("create event  on 2023-05-01");
    assertTrue(result);
    assertEquals("Invalid create event command format", textUI.getLastError());

    result = processor.processCommand(
        "create event Meeting from 2023-05-01T10:00 to 2023-05-01T09:00");
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

    boolean result = processor.processCommand(
        "edit event name \"Team Meeting\" from 2023-05-01T10:00 "
            + "to 2023-05-01T11:00 with \"Product Discussion\"");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testEditEventsFrom() {
    processor.processCommand(
        "create event Team Meeting from 2023-05-01T10:00 " +
                "to 2023-05-01T11:00 repeats MWF for 5 times");
    textUI.reset();

    boolean result = processor.processCommand(
        "edit events location \"Team Meeting\" from 2023-05-03T10:00 with \"Conference Room B\"");
    assertTrue(result);
    assertEquals("Events updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testEditAllEvents() {
    processor.processCommand("create event Team Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand(
        "edit events description \"Team Meeting\" \"Updated agenda for all meetings\"");
    assertTrue(result);
    assertEquals("All events updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testEditInvalid() {
    boolean result = processor.processCommand("edit something name Meeting with \"New Name\"");
    assertTrue(result);
    assertEquals("Invalid edit command. Expected 'edit event', " +
                    "'edit events', or 'edit calendar'",
        textUI.getLastError());

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
    boolean result = processor.processCommand(
        "create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --autoDecline");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Event created successfully"));
  }

  @Test
  public void testCreateCommandWithDescription() {
    boolean result = processor.processCommand(
        "create event Meeting from 2023-03-15T10:00 "
            + "to 2023-03-15T11:00 --description \"Team meeting\"");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Event created successfully"));
  }

  @Test
  public void testCreateCommandWithLocation() {
    boolean result = processor.processCommand(
        "create event Meeting from 2023-03-15T10:00 "
            + "to 2023-03-15T11:00 --location \"Conference Room\"");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Event created successfully"));
    textUI.reset();
    boolean result1 = processor.processCommand("print events on 2023-03-15");
    assertTrue(result1);
    assertTrue(textUI.getLastMessage().contains("Conference Room"));
  }

  @Test
  public void testCreateCommandWithPrivate() {
    boolean result = processor.processCommand(
        "create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --private");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Event created successfully"));
  }

  @Test
  public void testHandleCreateWithRecurringEvent() {
    boolean result = processor.processCommand(
        "create event Weekly Meeting from 2023-03-15T10:00 "
            + "to 2023-03-15T11:00 repeats W for 10 times");
    assertTrue(result);
    assertTrue(textUI.getLastMessage().contains("Recurring event created successfully"));
  }

  @Test
  public void testHandleCreateWithConflict() {
    processor.processCommand(
        "create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --autoDecline");
    textUI.reset();
    boolean result = processor.processCommand(
        "create event Another Meeting from 2023-03-15T10:30 to 2023-03-15T11:30 --autoDecline");
    assertTrue(result);
    assertTrue(textUI.getLastError().contains("Failed to create Event"));
  }

  @Test
  public void testHandleEditWithInvalidProperty() {
    processor.processCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00");
    textUI.reset();
    boolean result = processor.processCommand(
        "edit event invalidproperty Meeting from 2023-03-15T10:00 " +
                "to 2023-03-15T11:00 with \"New Value\"");
    assertTrue(result);
    assertEquals("Failed to update event (not found, invalid property, " +
            "or would create conflict)", textUI.getLastError());
  }

  @Test
  public void testHandleEditWithNonExistentEvent() {
    boolean result = processor.processCommand(
        "edit event description \"Non-Existent Meeting\" from 2023-03-15T10:00 " +
                "to 2023-03-15T11:00 with \"Updated description\"");
    assertTrue(result);
    assertEquals("Failed to update event (not found, invalid property, " +
            "or would create conflict)", textUI.getLastError());
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

    boolean result = processor.processCommand(
        "edit events description \"Meeting\" with \"Updated agenda\"");
    assertTrue(result);
    assertEquals("All events updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testEditTimeProperty() {
    processor.processCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand(
        "edit event STARTTIME Meeting from 2023-05-01T10:00 to 2023-05-01T11:00 with \"14:00\"");
    assertTrue(result);
  }

  @Test
  public void testEditDateProperty() {
    processor.processCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand(
        "edit event STARTDATE Meeting from 2023-05-01T10:00 " +
                "to 2023-05-01T11:00 with \"2023-06-01\"");
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

  }

  @Test
  public void testEventSortingByDate() {
    processor.processCommand("create event Day2 on 2023-05-02");
    processor.processCommand("create event Day1 on 2023-05-01");
    textUI.reset();

    boolean result = processor.processCommand("print events from 2023-05-01 to 2023-05-03");
    assertTrue(result);

    String message = textUI.getLastMessage();

  }

  @Test
  public void testEventSortingAllDayVsTimedEvents() {
    processor.processCommand("create event AllDay on 2023-05-01");
    processor.processCommand("create event TimedEvent from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("print events on 2023-05-01");
    assertTrue(result);

    String message = textUI.getLastMessage();

  }

  @Test
  public void testEventSortingByStartTime() {
    processor.processCommand("create event Later from 2023-05-01T14:00 to 2023-05-01T15:00");
    processor.processCommand("create event Earlier from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand("print events on 2023-05-01");
    assertTrue(result);

    String message = textUI.getLastMessage();

  }

  @Test
  public void testEventNumberingInOutput() {
    processor.processCommand("create event Event1 from 2023-05-01T10:00 to 2023-05-01T11:00");
    processor.processCommand("create event Event2 from 2023-05-01T14:00 to 2023-05-01T15:00");
    processor.processCommand("create event Event3 from 2023-05-01T16:00 to 2023-05-01T17:00");
    textUI.reset();

    boolean result = processor.processCommand("print events on 2023-05-01");
    assertTrue(result);

    List<String> message = textUI.getAllMessages();
    assertEquals( "1. Event1 - 2023-05-01 10:00 to 11:00", message.get(4));
    assertEquals( "2. Event2 - 2023-05-01 14:00 to 15:00", message.get(5));
    assertEquals( "3. Event3 - 2023-05-01 16:00 to 17:00", message.get(6));
  }


  @Test
  public void testEventSortingDifferentDays() {

    processor.processCommand("create event SecondDay from 2023-05-02T10:00 to 2023-05-02T11:00");
    processor.processCommand("create event FirstDay from 2023-05-01T14:00 to 2023-05-01T15:00");
    textUI.reset();

    processor.processCommand("print events from 2023-05-01 to 2023-05-03");

    String output = textUI.getLastMessage();
    int firstDayIndex = output.indexOf("FirstDay");
    int secondDayIndex = output.indexOf("SecondDay");
    assertTrue(firstDayIndex < secondDayIndex);
  }

  @Test
  public void testEventSortingAllDayBeforeTimedSameDay() {

    processor.processCommand("create event TimedEvent from 2023-05-01T08:00 to 2023-05-01T09:00");
    processor.processCommand("create event AllDayEvent on 2023-05-01");
    textUI.reset();

    processor.processCommand("print events on 2023-05-01");

    String output = textUI.getLastMessage();
    int allDayIndex = output.indexOf("AllDayEvent");
    int timedIndex = output.indexOf("TimedEvent");
    assertTrue(allDayIndex < timedIndex);
  }

  @Test
  public void testEventSortingTimedBeforeAllDaySameDay() {

    processor.processCommand("create event AllDayEvent on 2023-05-01");
    processor.processCommand("create event TimedEvent from 2023-05-01T08:00 to 2023-05-01T09:00");
    textUI.reset();

    processor.processCommand("print events on 2023-05-01");

    String output = textUI.getLastMessage();
    int allDayIndex = output.indexOf("AllDayEvent");
    int timedIndex = output.indexOf("TimedEvent");
    assertTrue(allDayIndex > timedIndex);
  }

  @Test
  public void testEventSortingByStartTimeSameDay() {

    processor.processCommand("create event LaterEvent from 2023-05-01T14:00 to 2023-05-01T15:00");
    processor.processCommand("create event EarlierEvent from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    processor.processCommand("print events on 2023-05-01");

    String output = textUI.getLastMessage();
    int earlierIndex = output.indexOf("EarlierEvent");
    int laterIndex = output.indexOf("LaterEvent");
    assertTrue(earlierIndex < laterIndex);
  }


  @Test
  public void testParseEditSingleEventCommandWithEndDate() {

    processor.processCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand(
        "edit event enddate Meeting from 2023-05-01T10:00 to 2023-05-01T11:00 with \"2023-05-02\"");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testParseEditSingleEventCommandWithInvalidFromKeyword() {

    boolean result = processor.processCommand(
        "edit event name Meeting something 2023-05-01T10:00 to 2023-05-01T11:00 with \"New Name\"");
    assertTrue(result);
    assertEquals("Invalid edit command format", textUI.getLastError());
  }

  @Test
  public void testParseEditSingleEventCommandWithMissingToOrWith() {

    boolean result = processor.processCommand(
        "edit event name Meeting from 2023-05-01T10:00 something " +
                "2023-05-01T11:00 something \"New Name\"");
    assertTrue(result);
    assertEquals("Invalid edit command format", textUI.getLastError());
  }

  @Test
  public void testParseEditSingleEventCommandWithQuotedEventName() {

    processor.processCommand(
        "create event \"Team Meeting\" from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand(
        "edit event name \"Team Meeting\" from 2023-05-01T10:00 to " +
                "2023-05-01T11:00 with \"Product Discussion\"");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testParseEditSingleEventCommandWithQuotedNewValue() {

    processor.processCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand(
        "edit event description Meeting from 2023-05-01T10:00 to 2023-05-01T11:00 " +
                "with \"This is a \"special\" meeting\"");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());
  }

  @Test
  public void testParseEditSingleEventCommandWithoutQuotes() {

    processor.processCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand(
        "edit event public Meeting from 2023-05-01T10:00 to 2023-05-01T11:00 with true");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());
  }


  @Test
  public void testCompleteEditSingleEventFlowWithAllComponents() {

    processor.processCommand(
        "create event \"Important Meeting\" from 2023-05-01T10:00 to 2023-05-01T11:00");
    textUI.reset();

    boolean result = processor.processCommand(
        "edit event description \"Important Meeting\" from 2023-05-01T10:00 " +
                "to 2023-05-01T11:00 with \"This is a \"critical\" update\"");
    assertTrue(result);
    assertEquals("Event updated successfully", textUI.getLastMessage());

    textUI.reset();
    processor.processCommand("print events on 2023-05-01");
    String output = textUI.getLastMessage();
    assertTrue(output.contains("Important Meeting"));
  }

  // Calendar commands
  @Test
  public void testCreateCalendarSuccessfully() {
    // Create NYC Calendar
    processor.processCommand("create calendar --name NYC --timezone America/New_York");

    // Verify success message
    assertEquals("Calendar created: NYC (America/New_York)", textUI.getLastMessage());
    assertNull(textUI.getLastError());
  }

  @Test
  public void testCreateDuplicateCalendarFails() {
    // Create initial calendar
    processor.processCommand("create calendar --name NYC --timezone America/New_York");
    textUI.reset();

    // Try to create calendar with same name
    processor.processCommand("create calendar --name NYC --timezone Europe/London");

    // Verify error message
    assertEquals("Failed to create calendar (name already exists)", textUI.getLastError());
  }

  @Test
  public void testUseCalendarSuccessfully() {
    // Create calendar first
    processor.processCommand("create calendar --name NYC --timezone America/New_York");
    processor.processCommand("create calendar --name London --timezone Europe/London");
    textUI.reset();

    // Use NYC calendar
    processor.processCommand("use calendar --name NYC");

    // Verify success message
    assertEquals("Now using calendar: NYC", textUI.getLastMessage());
    assertNull(textUI.getLastError());
  }

  @Test
  public void testUseNonexistentCalendarFails() {
    // Attempt to use non-existent calendar
    processor.processCommand("use calendar --name Nonexistent");

    // Verify error message
    assertEquals("Calendar not found: Nonexistent", textUI.getLastError());
  }

  @Test
  public void testCopySingleEventMessageSuccessfully() {
    // Create calendars
    processor.processCommand("create calendar --name NYC --timezone America/New_York");
    textUI.reset();
    processor.processCommand("create calendar --name London --timezone Europe/London");
    textUI.reset();

    // Use NYC calendar
    processor.processCommand("use calendar --name NYC");
    assertEquals("Now using calendar: NYC", textUI.getLastMessage());
    textUI.reset();

    // Create event
    processor.processCommand(
            "create event \"International Meeting\" from 2023-03-21T13:00 " +
                    "to 2023-03-21T14:30 " +
                    "--description \"Conference call with London office\" " +
                    "--location \"Conference Room A\""
    );
    assertEquals("Event created successfully: \"International Meeting\"",
            textUI.getLastMessage());
    textUI.reset();

    // Copy event
    boolean result = processor.processCommand(
            "copy event \"International Meeting\" on 2023-03-21T13:00 --target " +
                    "London to 2023-03-21T13:00"
    );

    // Verify success message and result
    assertTrue("Command should return true to continue processing", result);
    assertEquals("Event copied successfully to London", textUI.getLastMessage());
    assertNull(textUI.getLastError());
  }

  @Test
  public void testCopyEventToNonexistentCalendarFails() {
    // Create calendars
    processor.processCommand("create calendar --name NYC --timezone America/New_York");
    textUI.reset();

    // Use NYC calendar
    processor.processCommand("use calendar --name NYC");
    assertEquals("Now using calendar: NYC", textUI.getLastMessage());
    textUI.reset();

    // Create event
    processor.processCommand(
            "create event \"International Meeting\" from 2023-03-21T13:00 to 2023-03-21T14:30 " +
                    "--description \"Conference call with London office\" " +
                    "--location \"Conference Room A\""
    );
    assertEquals("Event created successfully: \"International Meeting\"",
            textUI.getLastMessage());
    textUI.reset();

    // Attempt to copy to non-existent calendar
    boolean result = processor.processCommand(
            "copy event \"International Meeting\" on 2023-03-21T13:00 --target " +
                    "Nonexistent to 2023-03-21T13:00"
    );

    // Verify error message
    assertTrue("Command should return true", result);
    assertEquals(
            "Failed to copy event (event not found, target calendar not found, " +
                    "or would create conflict)",
            textUI.getLastError()
    );
  }

  @Test
  public void testCopyEventsOnDay() {
    // Create calendars
    processor.processCommand("create calendar --name NYC --timezone America/New_York");
    textUI.reset();
    processor.processCommand("create calendar --name London --timezone Europe/London");
    textUI.reset();

    // Use NYC calendar
    processor.processCommand("use calendar --name NYC");
    assertEquals("Now using calendar: NYC", textUI.getLastMessage());
    textUI.reset();

    // Create multiple events on same day
    processor.processCommand(
            "create event \"Morning Meeting\" from 2023-03-21T09:00 to 2023-03-21T10:00 " +
                    "--description \"Daily standup\""
    );
    assertEquals("Event created successfully: \"Morning Meeting\"",
            textUI.getLastMessage());
    textUI.reset();

    processor.processCommand(
            "create event \"Afternoon Meeting\" from 2023-03-21T14:00 to 2023-03-21T15:00 " +
                    "--description \"Project review\""
    );
    assertEquals("Event created successfully: \"Afternoon Meeting\"",
            textUI.getLastMessage());
    textUI.reset();

    // Copy events to London calendar
    boolean result = processor.processCommand(
            "copy events on 2023-03-21 --target London to 2023-03-21"
    );

    // Verify success message
    assertTrue("Command should return true to continue processing", result);
    assertEquals("Events copied successfully to London", textUI.getLastMessage());
    assertNull(textUI.getLastError());
  }

  @Test
  public void testEditCalendarSuccessMessage() {
    // Create a calendar first
    processor.processCommand("create calendar --name NYC --timezone America/New_York");
    textUI.reset();

    // Edit calendar successfully
    processor.processCommand("edit calendar --name NYC --property name --value BOS");

    // Verify success message
    assertEquals("Calendar updated successfully", textUI.getLastMessage());
    assertNull(textUI.getLastError());
  }

  @Test
  public void testEditCalendarFailureMessage() {
    // Attempt to edit non-existent calendar
    processor.processCommand("edit calendar --name Nonexistent --property " +
            "timezone --value Europe/London");

    // Verify error message
    assertEquals(
            "Failed to update calendar (invalid name, property, or value)",
            textUI.getLastError()
    );
  }

  @Test
  public void testPrintEventsMessage() {
    // Create a calendar and add an event
    processor.processCommand("create calendar --name NYC --timezone America/New_York");
    processor.processCommand("use calendar --name NYC");
    processor.processCommand(
            "create event \"Team Meeting\" from 2024-03-25T10:00 to 2024-03-25T11:00"
    );
    textUI.reset();

    // Print events for a specific date
    processor.processCommand("print events on 2024-03-25");

    // Verify events message header
    assertEquals("1. \"Team Meeting\" - 2024-03-25 10:00 to 11:00", textUI.getLastMessage());
  }

  @Test
  public void testEditEventSingleFailureMessage() {
    // Create a calendar
    processor.processCommand("create calendar --name NYC --timezone America/New_York");
    processor.processCommand("use calendar --name NYC");
    textUI.reset();

    // Attempt to edit non-existent event
    processor.processCommand(
            "edit event name \"Original Meeting\" from 2023-04-25T10:00 " +
                    "to 2023-04-25T11:00 with \"Updated Meeting\""
    );

    // Verify error message
    assertEquals(
            "Failed to update event (not found, invalid property, or would create conflict)",
            textUI.getLastError()
    );
  }

  @Test
  public void testEditAllEventsFailureMessage() {
    // Create a calendar
    processor.processCommand("create calendar --name NYC --timezone America/New_York");
    processor.processCommand("use calendar --name NYC");
    textUI.reset();

    // Attempt to edit all events for non-existent event name
    processor.processCommand(
            "edit events name \"Team Sync\" \"Team Synchronization\""
    );

    // Verify error message
    assertEquals(
            "Failed to update events (not found, invalid property, or would create conflict)",
            textUI.getLastError()
    );
  }

  @Test
  public void testCopyEventsInRangeFailureMessage() {
    // Create calendars
    processor.processCommand("create calendar --name NYC --timezone America/New_York");
    processor.processCommand("create calendar --name London --timezone Europe/London");
    textUI.reset();

    // Attempt to copy events from empty range
    processor.processCommand(
            "copy events on 2024-03-20 --target London to 2024-04-20"
    );

    // Verify failure message
    assertEquals(
            "Failed to copy events (no events found on that day, " +
                    "target calendar not found, or would create conflicts)",
            textUI.getLastError()
    );
  }

  @Test
  public void testUseCalendar() {
    processor.processCommand("create calendar --name London --timezone Europe/London");
    textUI.reset();

    boolean result = processor.processCommand("use calendar --name London");
    assertTrue(result);
  }

  @Test
  public void testCreateCalendarWithValidParameters() {
    boolean result = processor.processCommand("create calendar --name " +
            "WorkCalendar --timezone America/New_York");

    assertTrue("Command should return true to continue processing", result);
    assertNull("No error should be displayed", textUI.getLastError());
  }

  // 🔍 Mutation Test: Handling invalid calendar creation command format
  @Test
  public void testCreateCalendarWithInvalidFormat() {
    boolean result = processor.processCommand("create calendar WorkCalendar");

    assertTrue("Command should return true to continue processing", result);
    assertEquals("Correct error message should be displayed",
            "Invalid create calendar command format. Expected: " +
                    "create calendar --name <calName> --timezone area/location",
            textUI.getLastError());
  }

  // 🔍 Mutation Test: Handling missing required parameters
  @Test
  public void testCreateCalendarWithMissingParameters() {
    boolean result = processor.processCommand("create calendar --name");

    assertTrue("Command should return true to continue processing", result);
    assertEquals("Correct error message should be displayed",
            "Invalid create calendar command format. Expected: " +
                    "create calendar --name <calName> --timezone area/location",
            textUI.getLastError());
  }

  // 🔍 Mutation Test: Editing calendar name
  @Test
  public void testEditCalendarNameSuccessfully() {
    // First create a calendar
    processor.processCommand("create calendar --name OriginalCal --timezone America/Chicago");
    textUI.reset();

    boolean result = processor.processCommand("edit calendar --name OriginalCal " +
            "--property name NewCalendarName");

    assertTrue("Command should return true to continue processing", result);
    assertNull("No error should be displayed", textUI.getLastError());
  }

  // 🔍 Mutation Test: Editing calendar with invalid format
  @Test
  public void testEditCalendarWithInvalidFormat() {
    boolean result = processor.processCommand("edit calendar OriginalCal");

    assertTrue("Command should return true to continue processing", result);
    assertEquals("Correct error message should be displayed",
            "Invalid edit calendar command format. Expected: " +
                    "edit calendar --name <name-of-calendar> --property " +
                    "<property-name> <new-property-value>",
            textUI.getLastError());
  }

  // 🔍 Mutation Test: Using an existing calendar
  @Test
  public void testUseExistingCalendar() {
    // First create a calendar
    processor.processCommand("create calendar --name TravelCal --timezone Europe/London");
    textUI.reset();

    boolean result = processor.processCommand("use calendar --name TravelCal");

    assertTrue("Command should return true to continue processing", result);
    assertNull("No error should be displayed", textUI.getLastError());
  }

  // 🔍 Mutation Test: Using a non-existent calendar
  @Test
  public void testUseNonExistentCalendar() {
    boolean result = processor.processCommand("use calendar --name NonExistentCal");

    assertTrue("Command should return true to continue processing", result);
    assertEquals("Correct error message should be displayed",
            "Calendar not found: NonExistentCal",
            textUI.getLastError());
  }

  // 🔍 Mutation Test: Use calendar with invalid command format
  @Test
  public void testUseCalendarWithInvalidFormat() {
    boolean result = processor.processCommand("use calendar NonExistentCal");

    assertTrue("Command should return true to continue processing", result);
    assertEquals("Correct error message should be displayed",
            "Invalid use calendar command format. Expected: " +
                    "use calendar --name <name-of-calendar>",
            textUI.getLastError());
  }

  // 🔍 Comprehensive Calendar Edit Test
  @Test
  public void testCompleteCalendarEditFlowWithAllComponents() {
    // Create initial calendar
    processor.processCommand("create calendar --name WorkCal --timezone America/New_York");
    textUI.reset();

    // Edit calendar name and verify
    boolean result = processor.processCommand("edit calendar --name " +
            "WorkCal --property name PersonalCal");
    assertTrue("Command should return true to continue processing", result);
    assertNull("No error should be displayed", textUI.getLastError());
  }

  @Test
  public void testCopySingleEventSuccessfully() {
    processor.processCommand(
            "create calendar --name NYC --timezone America/New_York"
    );
    textUI.reset();
    processor.processCommand(
            "create calendar --name London --timezone Europe/London"
    );
    textUI.reset();
    processor.processCommand(
            "use calendar --name NYC"
    );
    textUI.reset();
    processor.processCommand(
            "create event \"International Meeting\" from 2023-03-21T13:00 " +
                    "to 2023-03-21T14:30 --description \"Conference call with London office\" " +
                    "--location \"Conference Room A\""
    );
    textUI.reset();
    boolean result = processor.processCommand(
            "copy event \"International Meeting\" on 2023-03-21T13:00 " +
                    "--target London to 2023-03-21T13:00"
    );

    assertEquals(
            "Event copied successfully to London",
            textUI.getLastMessage()
    );
    assertTrue("Command should return true to continue processing", result);
    assertNull("No error should be displayed", textUI.getLastError());
  }

  // 🔍 Mutation Test: Copy single event with invalid format
  @Test
  public void testCopySingleEventWithInvalidFormat() {
    boolean result = processor.processCommand(
            "copy event Meeting 2023-05-01T10:00"
    );

    assertTrue("Command should return true to continue processing", result);
    assertEquals("Correct error message should be displayed",
            "Invalid copy event command format. Expected: " +
                    "copy event <eventName> on <dateStringTtimeString> " +
                    "--target <calendarName> to <dateStringTtimeString>",
            textUI.getLastError());
  }

  // 🔍 Mutation Test: Copy multiple events with invalid format
  @Test
  public void testCopyEventsWithInvalidFormat() {
    boolean result = processor.processCommand(
            "copy events 2023-05-01"
    );

    assertTrue("Command should return true to continue processing", result);
    assertEquals("Correct error message should be displayed",
            "Invalid copy events command format. Expected: " +
                    "copy events on <dateString> --target <calendarName> to <dateString> " +
                    "OR copy events between <dateString> and <dateString> " +
                    "--target <calendarName> to <dateString>",
            textUI.getLastError());
  }

  @Test
  public void testCopyEventsWithInvalidDate() {
    boolean result = processor.processCommand(
            "copy events on invalid-date --target TargetCal to 2023-05-03"
    );

    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Error should be displayed", textUI.getLastError());
  }

  // 🔍 Mutation Test: Copy single event to non-existent target calendar
  @Test
  public void testCopySingleEventToNonExistentCalendar() {
    boolean result = processor.processCommand(
            "copy event \"Meeting\" on 2023-05-01T10:00 --target NonExistentCal to 2023-05-03T10:00"
    );

    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Error should be displayed", textUI.getLastError());
  }

  // 🔍 Mutation Test: Copy multiple events to non-existent target calendar
  @Test
  public void testCopyEventsToNonExistentCalendar() {
    boolean result = processor.processCommand(
            "copy events on 2023-05-01 --target NonExistentCal to 2023-05-03"
    );

    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Error should be displayed", textUI.getLastError());
  }

  @Test
  public void testInvalidCreateEventCommandErrorMessage() {
    boolean result = processor.processCommand("create");
    assertTrue("Command should return true to continue processing", result);
    assertEquals("Invalid create command. Expected 'create event' or 'create calendar'",
            textUI.getLastError());
  }

  @Test
  public void testInvalidEditEventCommandErrorMessage() {
    boolean result = processor.processCommand("edit");
    assertTrue("Command should return true to continue processing", result);
    assertEquals("Invalid edit command. Expected 'edit event', " +
                    "'edit events', or 'edit calendar'",
            textUI.getLastError());
  }

  @Test
  public void testInvalidUseEventCommandErrorMessage() {
    boolean result = processor.processCommand("use");
    assertTrue("Command should return true to continue processing", result);
    assertEquals("Invalid use command. Expected 'use calendar'",
            textUI.getLastError());

    boolean result1 = processor.processCommand("use event");
    assertTrue("Command should return true to continue processing", result1);
    assertEquals("Invalid use command. Expected 'use calendar'",
            textUI.getLastError());
  }

  @Test
  public void testInvalidCopyEventCommandErrorMessage() {
    boolean result = processor.processCommand("copy");
    assertTrue("Command should return true to continue processing", result);
    assertEquals("Invalid copy command. Expected 'copy event' or 'copy events'",
            textUI.getLastError());

    textUI.reset();
    boolean result1 = processor.processCommand("copy calendar");
    assertTrue("Command should return true to continue processing", result1);
    assertEquals("Invalid copy command. Expected 'copy event' or 'copy events'",
            textUI.getLastError());
  }

  @Test
  public void testCopyEventsBetweenWithInvalidFormat() {
    boolean result = processor.processCommand(
            "copy events between 2023-05-01"
    );
    assertTrue("Command should return true to continue processing", result);
    assertEquals("Correct error message should be displayed",
            "Invalid copy events command format. Expected: " +
                    "copy events on <dateString> --target <calendarName> to <dateString> " +
                    "OR copy events between <dateString> and <dateString> " +
                    "--target <calendarName> to <dateString>",
            textUI.getLastError());
  }

  @Test
  public void testCopyEventsBetweenWithIncompleteCommand() {
    boolean result = processor.processCommand(
            "copy events between 2023-05-01 and 2023-05-05"
    );
    assertTrue("Command should return true to continue processing", result);
    assertEquals("Correct error message should be displayed",
            "Invalid copy events command format. Expected: " +
                    "copy events on <dateString> --target <calendarName> to <dateString> " +
                    "OR copy events between <dateString> and <dateString> " +
                    "--target <calendarName> to <dateString>",
            textUI.getLastError());
  }

  @Test
  public void testCopyEventsBetweenWithInvalidStartDate() {
    boolean result = processor.processCommand(
            "copy events between invalid-date and 2023-05-05 --target TargetCal to 2023-06-05"
    );
    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Error should be displayed for invalid start date", textUI.getLastError());
  }

  @Test
  public void testCopyEventsBetweenWithInvalidEndDate() {
    boolean result = processor.processCommand(
            "copy events between 2023-05-01 and invalid-date --target TargetCal to 2023-06-05"
    );
    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Error should be displayed for invalid end date", textUI.getLastError());
  }

  @Test
  public void testCopyEventsBetweenWithInvalidTargetDate() {
    boolean result = processor.processCommand(
            "copy events between 2023-05-01 and 2023-05-05 --target TargetCal to invalid-date"
    );
    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Error should be displayed for invalid target date", textUI.getLastError());
  }

  @Test
  public void testCopyEventsBetweenWithStartDateAfterEndDate() {
    boolean result = processor.processCommand(
            "copy events between 2023-05-10 and 2023-05-05 --target TargetCal to 2023-06-05"
    );
    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Error should be displayed when start date is after end date", textUI.getLastError());
  }

  @Test
  public void testCopyEventsBetweenWithInvalidTargetCalendar() {
    boolean result = processor.processCommand(
            "copy events between 2023-05-01 and 2023-05-05 --target NonExistentCal to 2023-06-05"
    );
    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Error should be displayed for non-existent target calendar", textUI.getLastError());
  }

  @Test
  public void testCopyEventsBetweenWithSameStartAndEndDate() {
    boolean result = processor.processCommand(
            "copy events between 2023-05-01 and 2023-05-01 --target WorkCal to 2023-06-05"
    );
    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Command should be processed without errors", textUI.getLastError());
  }

  @Test
  public void testCopyEventsBetweenWithLongDateRange() {
    boolean result = processor.processCommand(
            "copy events between 2023-01-01 and 2023-12-31 --target WorkCal to 2024-01-01"
    );
    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Command should be processed without errors for long date range", textUI.getLastError());
  }

  @Test
  public void testEditEventName() {
    // First create the event
    boolean createResult = processor.processCommand(
            "create event \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00"
    );
    assertTrue("Event creation should succeed", createResult);

    // Then edit the event
    boolean result = processor.processCommand(
            "edit event name \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00 with \"All Hands Meeting\""
    );
    assertTrue("Command should return true to continue processing", result);
    assertNull("No error should be displayed", textUI.getLastError());
  }

  @Test
  public void testEditEventLocation() {
    // First create the event
    boolean createResult = processor.processCommand(
            "create event \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00"
    );
    assertTrue("Event creation should succeed", createResult);

    // Then edit the event
    boolean result = processor.processCommand(
            "edit event location \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00 with \"Main Auditorium\""
    );
    assertTrue("Command should return true to continue processing", result);
    assertNull("No error should be displayed", textUI.getLastError());
  }

  @Test
  public void testEditEventDescription() {
    // First create the event
    boolean createResult = processor.processCommand(
            "create event \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00"
    );
    assertTrue("Event creation should succeed", createResult);

    // Then edit the event
    boolean result = processor.processCommand(
            "edit event description \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00 with \"Updated team sync description\""
    );
    assertTrue("Command should return true to continue processing", result);
    assertNull("No error should be displayed", textUI.getLastError());
  }

  @Test
  public void testEditEventStartTime() {
    // First create the event
    boolean createResult = processor.processCommand(
            "create event \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00"
    );
    assertTrue("Event creation should succeed", createResult);

    // Then edit the event
    boolean result = processor.processCommand(
            "edit event starttime \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00 with 2023-05-15T08:30"
    );
    assertTrue("Command should return true to continue processing", result);
    assertNull("No error should be displayed", textUI.getLastError());
  }

  @Test
  public void testEditEventEndTime() {
    // First create the event
    boolean createResult = processor.processCommand(
            "create event \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00"
    );
    assertTrue("Event creation should succeed", createResult);

    // Then edit the event
    boolean result = processor.processCommand(
            "edit event endtime \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00 with 2023-05-15T10:30"
    );
    assertTrue("Command should return true to continue processing", result);
    assertNull("No error should be displayed", textUI.getLastError());
  }

  @Test
  public void testEditEventWithQuotedNewValue() {
    // First create the event
    boolean createResult = processor.processCommand(
            "create event \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00"
    );
    assertTrue("Event creation should succeed", createResult);

    // Then edit the event
    boolean result = processor.processCommand(
            "edit event name \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00 with \"All Hands Meeting\""
    );
    assertTrue("Command should return true to continue processing", result);
    assertNull("No error should be displayed", textUI.getLastError());
  }

  @Test
  public void testEditEventWithInvalidEventName() {
    // No need to create an event for this test as it specifically checks non-existent event

    boolean result = processor.processCommand(
            "edit event name \"Nonexistent Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00 with \"New Meeting Name\""
    );
    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Error should be displayed for nonexistent event", textUI.getLastError());
  }

  @Test
  public void testEditEventWithInvalidDateRange() {
    // First create the event
    boolean createResult = processor.processCommand(
            "create event \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00"
    );
    assertTrue("Event creation should succeed", createResult);

    // Then attempt to edit with an incorrect date range
    boolean result = processor.processCommand(
            "edit event name \"Team Meeting\" from 2023-05-16T09:00 to 2023-05-16T10:00 with \"Different Meeting\""
    );
    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Error should be displayed for invalid date range", textUI.getLastError());
  }

  @Test
  public void testEditEventWithInvalidProperty() {
    // First create the event
    boolean createResult = processor.processCommand(
            "create event \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00"
    );
    assertTrue("Event creation should succeed", createResult);

    // Then attempt to edit with an invalid property
    boolean result = processor.processCommand(
            "edit event priority \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00 with \"High\""
    );
    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Error should be displayed for invalid property", textUI.getLastError());
  }

  @Test
  public void testEditEventWithInvalidDateFormat() {
    // First create the event
    boolean createResult = processor.processCommand(
            "create event \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00"
    );
    assertTrue("Event creation should succeed", createResult);

    // Then attempt to edit with an invalid date format
    boolean result = processor.processCommand(
            "edit event starttime \"Team Meeting\" from 2023-05-15T09:00 to 2023-05-15T10:00 with invalid-date"
    );
    assertTrue("Command should return true to continue processing", result);
    assertNotNull("Error should be displayed for invalid date format", textUI.getLastError());
  }



  private static class TestTextUI implements TextUI {
    private String lastMessage;
    private String lastError;
    private final List<String> commands = new ArrayList<>();
    private final List<String> allMessages = new ArrayList<>(); // Added list to store all messages
    private int commandIndex = 0;

    @Override
    public void displayMessage(String message) {
      lastMessage = message;
      allMessages.add(message); // Store each message as it's displayed
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
      // Implementation unchanged
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
      // Don't clear allMessages here to allow checking after reset
    }

    // New method to get all messages
    public List<String> getAllMessages() {
      return new ArrayList<>(allMessages); // Return a copy to prevent modification
    }

    // Additional method to clear all messages if needed
    public void clearAllMessages() {
      allMessages.clear();
    }
  }

}