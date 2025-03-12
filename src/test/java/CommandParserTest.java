
import controller.Command;
import controller.CommandParser;
import controller.CreateCommand;
import controller.EditCommand;
import controller.ExportCommand;
import controller.PrintCommand;
import controller.ShowCommand;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class CommandParserTest {

  private CommandParser parser;

  @Before
  public void setUp() {
    parser = new CommandParser();
  }

  // CREATE EVENT TESTS

  @Test
  public void testParseCreateAllDayEvent() {
    String command = "create event Meeting on 2023-05-01";
    Command result = parser.parseCreateCommand(command);

    assertNotNull(result);
    assertTrue(result instanceof CreateCommand);

    CreateCommand createCmd = (CreateCommand) result;
    assertEquals("Meeting", createCmd.getEventName());
    assertEquals(
        LocalDateTime.of(LocalDate.of(2023, 5, 1), LocalTime.MIDNIGHT),
        createCmd.getStartDateTime()
    );
    assertTrue(createCmd.isAllDay());
    assertFalse(createCmd.isRecurring());
  }

  @Test
  public void testParseCreateEventWithOptions() {
    String command = "create event Meeting on 2023-05-01 --autoDecline --description \"Important discussion\" --location \"Conference Room\" --private";
    Command result = parser.parseCreateCommand(command);

    assertNotNull(result);
    CreateCommand createCmd = (CreateCommand) result;

    assertEquals("Meeting", createCmd.getEventName());
    assertTrue(createCmd.isAutoDecline());
    assertEquals("Important discussion", createCmd.getDescription());
    assertEquals("Conference Room", createCmd.getLocation());
    assertFalse(createCmd.isPublic());
  }

  @Test
  public void testParseCreateRecurringAllDayEvent() {
    String command = "create event Weekly Meeting on 2023-05-01 repeats MTW for 5 times";
    Command result = parser.parseCreateCommand(command);

    assertNotNull(result);
    CreateCommand createCmd = (CreateCommand) result;

    assertEquals("Weekly Meeting", createCmd.getEventName());
    assertTrue(createCmd.isAllDay());
    assertTrue(createCmd.isRecurring());
    assertEquals("MTW", createCmd.getWeekdays());
    assertEquals(5, createCmd.getOccurrences());
  }

  @Test
  public void testParseCreateRecurringAllDayEventWithUntil() {
    String command = "create event Weekly Meeting on 2023-05-01 repeats MTW until 2023-06-01";
    Command result = parser.parseCreateCommand(command);

    assertNotNull(result);
    CreateCommand createCmd = (CreateCommand) result;

    assertTrue(createCmd.isRecurring());
    assertEquals("MTW", createCmd.getWeekdays());
    assertEquals(
        LocalDateTime.of(LocalDate.of(2023, 6, 1), LocalTime.MIDNIGHT),
        createCmd.getUntilDate()
    );
  }

  @Test
  public void testParseCreateRegularEvent() {
    String command = "create event Interview from 2023-05-01T09:00 to 2023-05-01T10:30";
    Command result = parser.parseCreateCommand(command);

    assertNotNull(result);
    CreateCommand createCmd = (CreateCommand) result;

    assertEquals("Interview", createCmd.getEventName());
    assertEquals(
        LocalDateTime.of(2023, 5, 1, 9, 0),
        createCmd.getStartDateTime()
    );
    assertEquals(
        LocalDateTime.of(2023, 5, 1, 10, 30),
        createCmd.getEndDateTime()
    );
    assertFalse(createCmd.isAllDay());
    assertFalse(createCmd.isRecurring());
  }

  @Test
  public void testParseCreateRecurringEvent() {
    String command = "create event Weekly Meeting from 2023-05-01T09:00 to 2023-05-01T10:30 repeats MTW for 5 times";
    Command result = parser.parseCreateCommand(command);

    assertNotNull(result);
    CreateCommand createCmd = (CreateCommand) result;

    assertEquals("Weekly Meeting", createCmd.getEventName());
    assertFalse(createCmd.isAllDay());
    assertTrue(createCmd.isRecurring());
    assertEquals("MTW", createCmd.getWeekdays());
    assertEquals(5, createCmd.getOccurrences());
  }

  @Test
  public void testParseCreateRecurringEventWithUntil() {
    String command = "create event Weekly Meeting from 2023-05-01T09:00 to 2023-05-01T10:30 repeats W until 2023-06-01";
    Command result = parser.parseCreateCommand(command);

    assertNotNull(result);
    CreateCommand createCmd = (CreateCommand) result;

    assertTrue(createCmd.isRecurring());
    assertEquals("W", createCmd.getWeekdays());
    assertEquals(
        LocalDateTime.of(LocalDate.of(2023, 6, 1), LocalTime.MIDNIGHT),
        createCmd.getUntilDate()
    );
  }

  @Test
  public void testParseCreateEventInvalid() {
    // Invalid date
    assertNull(parser.parseCreateCommand("create event Meeting on invalid-date"));

    // Missing event name
    assertNull(parser.parseCreateCommand("create event  on 2023-05-01"));

    // End time before start time
    assertNull(parser.parseCreateCommand("create event Meeting from 2023-05-01T10:00 to 2023-05-01T09:00"));

    // Invalid command format
    assertNull(parser.parseCreateCommand("create event Meeting at 2023-05-01"));

    // Invalid recurring format
    assertNull(parser.parseCreateCommand("create event Meeting on 2023-05-01 repeats MTW without 5 times"));
  }

  // EDIT EVENT TESTS

  @Test
  public void testParseEditSingleEvent() {
    String command = "edit event name Team Meeting from 2023-05-01T10:00 to 2023-05-01T11:00 with \"Product Discussion\"";
    Command result = parser.parseEditCommand(command);

    assertNotNull(result);
    assertTrue(result instanceof EditCommand);

    EditCommand editCmd = (EditCommand) result;
    assertEquals(EditCommand.EditType.SINGLE, editCmd.getEditType());
    assertEquals("name", editCmd.getProperty());
    assertEquals("Team Meeting", editCmd.getEventName());
    assertEquals(
        LocalDateTime.of(2023, 5, 1, 10, 0),
        editCmd.getStartDateTime()
    );
    assertEquals(
        LocalDateTime.of(2023, 5, 1, 11, 0),
        editCmd.getEndDateTime()
    );
    assertEquals("Product Discussion", editCmd.getNewValue());
  }

  @Test
  public void testParseEditEventsFrom() {
    String command = "edit events location \"Team Meeting\" from 2023-05-01T10:00 with \"Conference Room B\"";
    Command result = parser.parseEditCommand(command);

    assertNotNull(result);
    EditCommand editCmd = (EditCommand) result;

    assertEquals(EditCommand.EditType.FROM_DATE, editCmd.getEditType());
    assertEquals("location", editCmd.getProperty());
    assertEquals("Team Meeting", editCmd.getEventName());
    assertEquals(
        LocalDateTime.of(2023, 5, 1, 10, 0),
        editCmd.getStartDateTime()
    );
    assertEquals("Conference Room B", editCmd.getNewValue());
  }

  @Test
  public void testParseEditAllEvents() {
    String command = "edit events description \"Team Meeting\" \"Updated agenda for all meetings\"";
    Command result = parser.parseEditCommand(command);

    assertNotNull(result);
    EditCommand editCmd = (EditCommand) result;

    assertEquals(EditCommand.EditType.ALL, editCmd.getEditType());
    assertEquals("description", editCmd.getProperty());
    assertEquals("Team Meeting", editCmd.getEventName());
    assertEquals("Updated agenda for all meetings", editCmd.getNewValue());
  }

  @Test
  public void testParseEditInvalid() {
    // Invalid edit type
    assertNull(parser.parseEditCommand("edit something name Meeting with \"New Name\""));

    // Missing parts
    assertNull(parser.parseEditCommand("edit event name"));

    // Invalid format for single event edit
    assertNull(parser.parseEditCommand("edit event name Meeting from 2023-05-01T10:00 new value"));

    // Invalid format for events from edit
    assertNull(parser.parseEditCommand("edit events location Meeting on 2023-05-01T10:00 with \"New Location\""));

    // Invalid format for all events edit
    assertNull(parser.parseEditCommand("edit events description Meeting Updated"));
  }

  // PRINT EVENTS TESTS

  @Test
  public void testParsePrintEventsOnDate() {
    String command = "print events on 2023-05-01";
    Command result = parser.parsePrintCommand(command);

    assertNotNull(result);
    assertTrue(result instanceof PrintCommand);

    PrintCommand printCmd = (PrintCommand) result;
    assertEquals(
        LocalDateTime.of(LocalDate.of(2023, 5, 1), LocalTime.MIDNIGHT),
        printCmd.getStartDateTime()
    );
    assertFalse(printCmd.isDateRange());
  }

  @Test
  public void testParsePrintEventsInRange() {
    String command = "print events from 2023-05-01 to 2023-05-07";
    Command result = parser.parsePrintCommand(command);

    assertNotNull(result);
    PrintCommand printCmd = (PrintCommand) result;

    assertEquals(
        LocalDateTime.of(LocalDate.of(2023, 5, 1), LocalTime.MIDNIGHT),
        printCmd.getStartDateTime()
    );
    assertEquals(
        LocalDateTime.of(LocalDate.of(2023, 5, 7), LocalTime.MIDNIGHT),
        printCmd.getEndDateTime()
    );
    assertTrue(printCmd.isDateRange());
  }

  @Test
  public void testParsePrintInvalid() {
    // Invalid date
    assertNull(parser.parsePrintCommand("print events on invalid-date"));

    // Invalid format
    assertNull(parser.parsePrintCommand("print events between 2023-05-01 and 2023-05-07"));

    // Missing end date
    assertNull(parser.parsePrintCommand("print events from 2023-05-01"));
  }

  // EXPORT TESTS

  @Test
  public void testParseExportCommand() {
    String command = "export cal events.csv";
    Command result = parser.parseExportCommand(command);

    assertNotNull(result);
    assertTrue(result instanceof ExportCommand);

    ExportCommand exportCmd = (ExportCommand) result;
    assertEquals("events.csv", exportCmd.getFileName());
  }

  @Test
  public void testParseExportInvalid() {
    // Missing filename
    assertNull(parser.parseExportCommand("export cal"));

    // Too many arguments
    assertNull(parser.parseExportCommand("export cal events.csv extra"));
  }

  // SHOW STATUS TESTS

  @Test
  public void testParseShowStatusCommand() {
    String command = "show status on 2023-05-01T10:00";
    Command result = parser.parseShowCommand(command);

    assertNotNull(result);
    assertTrue(result instanceof ShowCommand);

    ShowCommand showCmd = (ShowCommand) result;
    assertEquals(
        LocalDateTime.of(2023, 5, 1, 10, 0),
        showCmd.getDateTime()
    );
  }

  @Test
  public void testParseShowStatusInvalid() {
    // Invalid date
    assertNull(parser.parseShowCommand("show status on invalid-date"));

    // Missing date
    assertNull(parser.parseShowCommand("show status"));

    // Invalid format
    assertNull(parser.parseShowCommand("show status at 2023-05-01T10:00"));
  }

  // EXTRACTION TESTS

  @Test
  public void testExtractQuotedParameter() {
    String command = "command --description \"This is a description\" other";
    Command createCmd = parser.parseCreateCommand("create event Test on 2023-05-01 --description \"This is a description\"");

    assertNotNull(createCmd);
    assertEquals("This is a description", ((CreateCommand)createCmd).getDescription());
  }

  @Test
  public void testEmptyDescriptionAndLocation() {
    Command createCmd = parser.parseCreateCommand("create event Test on 2023-05-01");

    assertNotNull(createCmd);
    assertEquals("", ((CreateCommand)createCmd).getDescription());
    assertEquals("", ((CreateCommand)createCmd).getLocation());
  }

  @Test
  public void testParseCreateCommandWithAutoDecline() {
    Command cmd = parser.parseCreateCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --autoDecline");
    assertTrue(cmd instanceof CreateCommand);
    CreateCommand createCmd = (CreateCommand) cmd;
    assertTrue(createCmd.isAutoDecline());
  }

  @Test
  public void testParseCreateCommandWithDescription() {
    Command cmd = parser.parseCreateCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --description \"Team meeting\"");
    assertTrue(cmd instanceof CreateCommand);
    CreateCommand createCmd = (CreateCommand) cmd;
    assertEquals("Team meeting", createCmd.getDescription());
  }

  @Test
  public void testParseCreateCommandWithLocation() {
    Command cmd = parser.parseCreateCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --location \"Conference Room\"");
    assertTrue(cmd instanceof CreateCommand);
    CreateCommand createCmd = (CreateCommand) cmd;
    assertEquals("Conference Room", createCmd.getLocation());
  }

  @Test
  public void testParseCreateCommandWithPrivate() {
    Command cmd = parser.parseCreateCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 --private");
    assertTrue(cmd instanceof CreateCommand);
    CreateCommand createCmd = (CreateCommand) cmd;
    assertFalse(createCmd.isPublic());
  }

  @Test
  public void testParseCreateRecurringEvent2() {
    Command cmd = parser.parseCreateCommand("create event Meeting from 2023-03-15T10:00 to 2023-03-15T11:00 repeats MWF for 5 times");
    assertTrue(cmd instanceof CreateCommand);
    CreateCommand createCmd = (CreateCommand) cmd;
    assertTrue(createCmd.isRecurring());
  }
}