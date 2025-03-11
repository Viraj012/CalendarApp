import controller.Command;
import controller.CommandParser;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.Assert.*;

/**
 * Test cases for the CommandParser class.
 */
public class CommandParserTest {
  private CommandParser parser;

  @Before
  public void setUp() {
    parser = new CommandParser();
  }

  // CREATE COMMAND TESTS

  @Test
  public void testParseCreateEventCommand() {
    Command cmd = parser.parseCreateCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00");
    assertNotNull("Should parse valid create command", cmd);
  }

  @Test
  public void testParseCreateEventWithQuotedSubject() {
    Command cmd = parser.parseCreateCommand("create event \"Team Status Meeting\" from 2025-03-04T10:00 to 2025-03-04T11:00");
    assertNotNull("Should parse event with quoted subject", cmd);
  }

  @Test
  public void testParseCreateWithAllOptions() {
    Command cmd = parser.parseCreateCommand("create event --autoDecline --private \"Planning Session\" from 2025-03-04T10:00 to 2025-03-04T11:00 --description \"Quarterly planning\" --location \"Conference Room B\"");
    assertNotNull("Should parse event with all options", cmd);
  }

  @Test
  public void testParseCreateAllDayEvent() {
    Command cmd = parser.parseCreateCommand("create event Conference on 2025-03-04");
    assertNotNull("Should parse all-day event", cmd);
  }

  @Test
  public void testParseCreateRecurringEvent() {
    Command cmd = parser.parseCreateCommand("create event Standup from 2025-03-04T09:00 to 2025-03-04T09:30 repeats MWF for 3 times");
    assertNotNull("Should parse recurring event", cmd);
  }

  @Test
  public void testParseCreateRecurringEventWithUntilDate() {
    Command cmd = parser.parseCreateCommand("create event Weekly from 2025-03-04T14:00 to 2025-03-04T15:00 repeats T until 2025-03-25");
    assertNotNull("Should parse recurring event with until date", cmd);
  }

  @Test
  public void testParseCreateRecurringAllDayEvent() {
    Command cmd = parser.parseCreateCommand("create event Workshop on 2025-03-04 repeats TR for 2 times");
    assertNotNull("Should parse recurring all-day event", cmd);
  }

  @Test
  public void testParseInvalidCreateCommand() {
    Command cmd = parser.parseCreateCommand("create event with invalid format");
    assertNull("Should return null for invalid create command", cmd);
  }

  @Test
  public void testParseCreateWithEndTimeBeforeStartTime() {
    Command cmd = parser.parseCreateCommand("create event Invalid from 2025-03-04T11:00 to 2025-03-04T10:00");
    assertNull("Should return null when end time is before start time", cmd);
  }

  // EDIT COMMAND TESTS

  @Test
  public void testParseEditSingleEventCommand() {
    Command cmd = parser.parseEditCommand("edit event name \"Meeting\" from 2025-03-04T10:00 to 2025-03-04T11:00 with \"Team Meeting\"");
    assertNotNull("Should parse valid edit single event command", cmd);
  }

  @Test
  public void testParseEditEventsFromCommand() {
    Command cmd = parser.parseEditCommand("edit events location \"Standup\" from 2025-03-05T09:00 with \"Conference Room\"");
    assertNotNull("Should parse valid edit events from command", cmd);
  }

  @Test
  public void testParseEditAllEventsCommand() {
    Command cmd = parser.parseEditCommand("edit events name \"Meeting\" \"Team Meeting\"");
    assertNotNull("Should parse valid edit all events command", cmd);
  }

  @Test
  public void testParseInvalidEditCommand() {
    Command cmd = parser.parseEditCommand("edit event with invalid format");
    assertNull("Should return null for invalid edit command", cmd);
  }

  // PRINT COMMAND TESTS

  @Test
  public void testParsePrintEventsOnCommand() {
    Command cmd = parser.parsePrintCommand("print events on 2025-03-04");
    assertNotNull("Should parse valid print events on command", cmd);
  }

  @Test
  public void testParsePrintEventsFromCommand() {
    Command cmd = parser.parsePrintCommand("print events from 2025-03-04 to 2025-03-05");
    assertNotNull("Should parse valid print events from command", cmd);
  }

  @Test
  public void testParseInvalidPrintCommand() {
    Command cmd = parser.parsePrintCommand("print events invalid format");
    assertNull("Should return null for invalid print command", cmd);
  }

  // EXPORT COMMAND TESTS

  @Test
  public void testParseExportCommand() {
    Command cmd = parser.parseExportCommand("export cal calendar.csv");
    assertNotNull("Should parse valid export command", cmd);
  }

  @Test
  public void testParseInvalidExportCommand() {
    Command cmd = parser.parseExportCommand("export calendar");
    assertNull("Should return null for invalid export command", cmd);
  }

  // SHOW COMMAND TESTS

  @Test
  public void testParseShowStatusCommand() {
    Command cmd = parser.parseShowCommand("show status on 2025-03-04T10:30");
    assertNotNull("Should parse valid show status command", cmd);
  }

  @Test
  public void testParseInvalidShowCommand() {
    Command cmd = parser.parseShowCommand("show status invalid");
    assertNull("Should return null for invalid show command", cmd);
  }

  // EDGE CASES

  @Test
  public void testParseCommandWithExcessiveWhitespace() {
    Command cmd = parser.parseCreateCommand("create   event    Meeting    from    2025-03-04T10:00    to    2025-03-04T11:00");
    assertNotNull("Should handle excessive whitespace", cmd);
  }

  @Test
  public void testParseCreateWithEmptySubject() {
    Command cmd = parser.parseCreateCommand("create event  from 2025-03-04T10:00 to 2025-03-04T11:00");
    assertNull("Should return null for empty subject", cmd);
  }

//  @Test
//  public void testParseCreateWithQuotedEmptyDescription() {
//    Command cmd = parser.parseCreateCommand("create event Meeting from 2025-03-04T10:00 to 2025-03-04T11:00 --description \"\"");
//    assertNotNull("Should handle empty quoted description", cmd);
//  }
}