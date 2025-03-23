
import model.Calendar;
import model.CalendarImpl;
import model.Event;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the CalendarImpl class.
 */
public class CalendarImplTest {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd'T'HH:mm");
  private static final String TEST_CSV_FILE = "test-calendar-export.csv";
  private Calendar calendar;
  private final LocalDateTime date1 = LocalDateTime.parse("2025-03-15T09:00", FORMATTER);
  private final LocalDateTime date2 = LocalDateTime.parse("2025-03-15T11:00", FORMATTER);
  private final LocalDateTime date3 = LocalDateTime.parse("2025-03-16T09:00", FORMATTER);
  private final LocalDateTime date4 = LocalDateTime.parse("2025-03-20T14:00", FORMATTER);

  @Before
  public void setUp() {
    calendar = new CalendarImpl();
  }

  @After
  public void tearDown() {

    File exportFile = new File(TEST_CSV_FILE);
    if (exportFile.exists()) {
      exportFile.delete();
    }
  }

  /*
   * Basic Event Creation Tests
   */
  @Test
  public void testCreateSimpleEvent() {
    assertTrue(
        calendar.createEvent("Meeting", date1, date2, false, "Team meeting", "Conference Room",
            true));

    List<Event> events = calendar.getEventsOn(date1);
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getSubject());
    assertEquals(date1, events.get(0).getStartDateTime());
    assertEquals(date2, events.get(0).getEndDateTime());
    assertEquals("Team meeting", events.get(0).getDescription());
    assertEquals("Conference Room", events.get(0).getLocation());
    assertTrue(events.get(0).isPublic());
    assertFalse(events.get(0).isAllDay());
    assertFalse(events.get(0).isRecurring());
  }

  @Test
  public void testCreateAllDayEvent() {
    assertTrue(
        calendar.createAllDayEvent("Holiday", date1, false, "Company Holiday", "Office", true));

    List<Event> events = calendar.getEventsOn(date1);
    assertEquals(1, events.size());
    assertEquals("Holiday", events.get(0).getSubject());
    assertEquals(date1, events.get(0).getStartDateTime());
    assertNull(events.get(0).getEndDateTime());
    assertTrue(events.get(0).isAllDay());
  }

  /*
   * Input Validation Tests
   */
  @Test
  public void testCreateEventWithInvalidInputs() {

    assertFalse(calendar.createEvent(null, date1, date2, false, "Description", "Location", true));

    assertFalse(calendar.createEvent("", date1, date2, false, "Description", "Location", true));

    assertFalse(
        calendar.createEvent("Meeting", null, date2, false, "Description", "Location", true));

    assertFalse(
        calendar.createEvent("Meeting", date2, date1, false, "Description", "Location", true));

    assertTrue(
        calendar.createEvent("Meeting", date1, date1, false, "Description", "Location", true));
  }

  /*
   * Conflict Tests
   */
  @Test
  public void testEventConflicts() {

    assertTrue(
        calendar.createEvent("Meeting 1", date1, date2
            , false, "Description"
            , "Location", true));

    assertFalse(
        calendar.createEvent("Meeting 2", date1.plusMinutes(30)
            , date2.plusMinutes(30), true
            , "Description", "Location", true));

    assertTrue(
        calendar.createEvent("Meeting 3", date2, date3, true, "Description", "Location", true));
  }

  @Test
  public void testAllDayEventConflicts() {

    assertTrue(
        calendar.createAllDayEvent("Holiday", date1, false, "Description", "Location", true));

    assertFalse(
        calendar.createAllDayEvent("Meeting", date1, true, "Description", "Location", true));

    assertFalse(
        calendar.createEvent("Meeting", date1, date1.plusHours(2)
            , true, "Description", "Location"
            , true));

    assertTrue(calendar.createAllDayEvent("Another Holiday", date3, true
        , "Description", "Location", true));
  }

  /*
   * Recurring Event Tests
   */
  @Test
  public void testCreateRecurringEvent() {

    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        "MW",
        2,
        null,
        false, "Team Sync", "Conference Room", true));

    List<Event> events = calendar.getEventsOn(LocalDateTime.parse("2025-03-10T10:00", FORMATTER));
    assertEquals(1, events.size());

    events = calendar.getEventsOn(LocalDateTime.parse("2025-03-12T10:00", FORMATTER));
    assertEquals(1, events.size());

    events = calendar.getEventsOn(LocalDateTime.parse("2025-03-17T10:00", FORMATTER));
    assertEquals(0, events.size());
  }

  @Test
  public void testCreateRecurringAllDayEvent() {

    LocalDateTime untilDate = LocalDateTime.parse("2025-03-30T00:00", FORMATTER);

    assertTrue(calendar.createRecurringAllDayEvent(
        "Weekend Check-in",
        LocalDateTime.parse("2025-03-15T00:00", FORMATTER),
        "SU",
        -1,
        untilDate,
        false, "Weekend work", "Remote", true));

    List<Event> events = calendar.getEventsOn(LocalDateTime.parse("2025-03-15T12:00", FORMATTER));
    assertEquals(1, events.size());
    events = calendar.getEventsOn(LocalDateTime.parse("2025-03-16T12:00", FORMATTER));
    assertEquals(1, events.size());

    events = calendar.getEventsOn(LocalDateTime.parse("2025-03-22T12:00", FORMATTER));
    assertEquals(1, events.size());

    events = calendar.getEventsOn(LocalDateTime.parse("2025-04-05T12:00", FORMATTER));
    assertEquals(0, events.size());
  }

  @Test
  public void testRecurringEventConflicts() {

    assertTrue(calendar.createEvent("Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        false, "Description", "Location", true));

    assertFalse(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:30", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:30", FORMATTER),
        "M",
        2,
        null,
        true, "Description", "Location", true));

    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T14:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T15:00", FORMATTER),
        "M",
        2,
        null,
        true, "Description", "Location", true));
  }

  @Test
  public void testMultiDayRecurringEventRestriction() {

    assertFalse(calendar.createRecurringEvent(
        "Overnight Workshop",
        LocalDateTime.parse("2025-03-10T14:00", FORMATTER),
        LocalDateTime.parse("2025-03-11T10:00", FORMATTER),
        "M",
        2,
        null,
        false, "Description", "Location", true));
  }

  /*
   * Edit Event Tests
   */
  @Test
  public void testEditSingleEvent() {

    assertTrue(
        calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));

    assertTrue(calendar.editEvent("subject", "Meeting", date1, date2, "Updated Meeting"));
    assertTrue(
        calendar.editEvent("description", "Updated Meeting", date1, date2, "New description"));
    assertTrue(calendar.editEvent("location", "Updated Meeting", date1, date2, "New location"));
    assertTrue(calendar.editEvent("public", "Updated Meeting", date1, date2, "false"));

    List<Event> events = calendar.getEventsOn(date1);
    assertEquals(1, events.size());
    Event event = events.get(0);
    assertEquals("Updated Meeting", event.getSubject());
    assertEquals("New description", event.getDescription());
    assertEquals("New location", event.getLocation());
    assertFalse(event.isPublic());

    assertFalse(
        calendar.editEvent("invalidProperty", "Updated Meeting", date1, date2, "New value"));

    assertFalse(calendar.editEvent("subject", "Non-existent", date1, date2, "New value"));
  }

  @Test
  public void testEditEventsFrom() {

    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        "MW",
        4,
        null,
        false, "Description", "Location", true));

    LocalDateTime secondWeekStart = LocalDateTime.parse("2025-03-17T00:00", FORMATTER);
    assertTrue(
        calendar.editEventsFrom("subject", "Weekly Meeting"
            , secondWeekStart, "Updated Meeting"));

    List<Event> events = calendar.getEventsOn(LocalDateTime.parse("2025-03-10T10:00", FORMATTER));
    assertEquals(1, events.size());
    assertEquals("Weekly Meeting [Recurring]", events.get(0).getSubject());

    events = calendar.getEventsOn(LocalDateTime.parse("2025-03-17T10:00", FORMATTER));
    assertEquals(1, events.size());
    assertTrue(events.get(0).getSubject().contains("Updated Meeting"));

    assertFalse(
        calendar.editEventsFrom("invalidProperty", "Weekly Meeting"
            , secondWeekStart, "New value"));
  }

  @Test
  public void testEditAllEvents() {

    assertTrue(
        calendar.createEvent("Meeting", date1, date2
            , false, "Description", "Location", true));
    assertTrue(
        calendar.createEvent("Meeting", date3, date4
            , false, "Description", "Location", true));

    assertTrue(calendar.editAllEvents("subject", "Meeting", "Team Meeting"));
    assertTrue(calendar.editAllEvents("description", "Team Meeting", "Important discussion"));

    List<Event> events = calendar.getEventsFrom(date1, date4);
    assertEquals(2, events.size());
    for (Event event : events) {
      assertEquals("Team Meeting", event.getSubject());
      assertEquals("Important discussion", event.getDescription());
    }

    assertFalse(calendar.editAllEvents("invalidProperty", "Team Meeting", "New value"));
  }

  /*
   * Query Tests
   */
  @Test
  public void testGetEventsOn() {

    assertTrue(
        calendar.createEvent("Meeting 1", date1, date2
            , false, "Description", "Location", true));
    assertTrue(
        calendar.createEvent("Meeting 2", date3, date4
            , false, "Description", "Location", true));

    List<Event> events = calendar.getEventsOn(date1);
    assertEquals(1, events.size());
    assertEquals("Meeting 1", events.get(0).getSubject());

    events = calendar.getEventsOn(date1.plusDays(7));
    assertEquals(0, events.size());
  }

  @Test
  public void testGetEventsFrom() {

    assertTrue(
        calendar.createEvent("Meeting 1", date1, date2
            , false, "Description"
            , "Location", true));
    assertTrue(
        calendar.createEvent("Meeting 2", date3, date4
            , false, "Description"
            , "Location", true));

    List<Event> events = calendar.getEventsFrom(date1, date4);
    assertEquals(2, events.size());

    events = calendar.getEventsFrom(date1, date1.plusHours(3));
    assertEquals(1, events.size());
    assertEquals("Meeting 1", events.get(0).getSubject());

    events = calendar.getEventsFrom(date4.plusDays(1), date4.plusDays(2));
    assertEquals(0, events.size());
  }

  @Test
  public void testIsBusy() {

    assertTrue(
        calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));

    assertTrue(calendar.isBusy(date1));
    assertTrue(calendar.isBusy(date1.plusMinutes(30)));
    assertTrue(calendar.isBusy(date2.minusMinutes(1)));

    assertFalse(calendar.isBusy(date1.minusMinutes(1)));
    assertFalse(calendar.isBusy(date2.plusMinutes(1)));
    assertFalse(calendar.isBusy(date3));
  }

  @Test
  public void testIsBusyWithAllDayEvent() {

    assertTrue(
        calendar.createAllDayEvent("Holiday", date1, false, "Description", "Location", true));

    assertTrue(calendar.isBusy(date1));
    assertTrue(calendar.isBusy(date1.plusHours(12)));

    assertFalse(calendar.isBusy(date1.minusDays(1)));
    assertFalse(calendar.isBusy(date1.plusDays(1)));
  }

  @Test
  public void testIsBusyWithRecurringEvent() {

    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        "MW",
        2,
        null,
        false, "Description", "Location", true));

    assertTrue(calendar.isBusy(LocalDateTime.parse("2025-03-10T10:30", FORMATTER)));

    assertTrue(calendar.isBusy(LocalDateTime.parse("2025-03-12T10:30", FORMATTER)));

    assertFalse(calendar.isBusy(LocalDateTime.parse("2025-03-10T09:30", FORMATTER)));
    assertFalse(calendar.isBusy(LocalDateTime.parse("2025-03-12T11:30", FORMATTER)));

    assertFalse(calendar.isBusy(LocalDateTime.parse("2025-03-11T10:30", FORMATTER)));
  }

  /*
   * Export Tests
   */
//  @Test
//  public void testExportToCSV() {
//
//    assertTrue(
//        calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));
//    assertTrue(
//        calendar.createAllDayEvent("Holiday", date3, false, "Description", "Location", false));
//
//    String filePath = Controller.exportToCSV(TEST_CSV_FILE);
//    assertNotNull(filePath);
//
//    File exportFile = new File(filePath);
//    assertTrue(exportFile.exists());
//    assertTrue(exportFile.length() > 0);
//  }

  /*
   * Special Case Tests
   */
//  @Test
//  public void testCSVEscaping() {
//
//    assertTrue(calendar.createEvent("Meeting, with comma", date1, date2, false,
//        "Description with \"quotes\"", "Location\nwith newline", true));
//
//    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
//    assertNotNull(filePath);
//
//    File exportFile = new File(filePath);
//    assertTrue(exportFile.exists());
//  }

  @Test
  public void testQuotedEventNameHandling() {

    assertTrue(
        calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));

    assertTrue(calendar.editEvent("subject", "\"Meeting\"", date1, date2, "Updated Meeting"));

    List<Event> events = calendar.getEventsOn(date1);
    assertEquals(1, events.size());
    assertEquals("Updated Meeting", events.get(0).getSubject());
  }

  @Test
  public void testRecurringEventExpansionAndOccurrences() {

    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        "MWF",
        3,
        null,
        false, "Team Sync", "Conference Room", true));

    List<Event> events = calendar.getEventsFrom(
        LocalDateTime.parse("2025-03-10T00:00", FORMATTER),
        LocalDateTime.parse("2025-03-14T23:59", FORMATTER));

    assertEquals(3, events.size());

    for (Event event : events) {
      assertTrue(event.getSubject().contains("[Recurring]"));
    }
  }

  @Test
  public void testCreateAllDayEventWithConflict() {
    assertTrue(
        calendar.createAllDayEvent("Holiday", date1, false, "Description", "Location", true));
    assertFalse(
        calendar.createAllDayEvent("Meeting", date1, true, "Description", "Location", true));
  }

  @Test
  public void testCreateRecurringEventWithConflict() {
    assertTrue(
        calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));

    assertFalse(calendar.createRecurringEvent(
        "Weekly Meeting",
        date1.plusMinutes(30),
        date2.minusMinutes(30),
        "MW",
        2,
        null,
        true,
        "Description",
        "Location",
        true));
  }

  @Test
  public void testCreateRecurringAllDayEventWithConflict() {
    assertTrue(
        calendar.createAllDayEvent("Holiday", date1, false, "Description", "Location", true));

    assertFalse(calendar.createRecurringAllDayEvent(
        "Weekly Holiday",
        date1,
        "SU",
        2,
        null,
        true,
        "Description",
        "Location",
        true));
  }

  @Test
  public void testRecurringAllDayEventOccurrences() {
    assertTrue(calendar.createRecurringAllDayEvent(
        "Weekend Event",
        LocalDateTime.parse("2025-03-15T00:00", FORMATTER),
        "SU",
        3,
        null,
        false,
        "Description",
        "Location",
        true));

    assertTrue(calendar.isBusy(LocalDateTime.parse("2025-03-15T12:00", FORMATTER)));
    assertTrue(calendar.isBusy(LocalDateTime.parse("2025-03-16T12:00", FORMATTER)));
  }

  @Test
  public void testEditEventsFromComprehensive() {
    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        "MW",
        4,
        null,
        false,
        "Original Description",
        "Original Location",
        true));

    LocalDateTime secondWeekStart = LocalDateTime.parse("2025-03-17T00:00", FORMATTER);

    assertTrue(
        calendar.editEventsFrom("subject", "Weekly Meeting"
            , secondWeekStart, "Updated Meeting"));
    assertTrue(calendar.editEventsFrom("description", "Updated Meeting"
        , secondWeekStart,
        "New Description"));
    assertTrue(
        calendar.editEventsFrom("location", "Updated Meeting"
            , secondWeekStart, "New Location"));
    assertTrue(calendar.editEventsFrom("public", "Updated Meeting"
        , secondWeekStart, "false"));

    assertFalse(
        calendar.editEventsFrom("nonexistent"
            , "Updated Meeting", secondWeekStart
            , "New Value"));

    List<Event> firstWeekEvents = calendar.getEventsOn(
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER));
    assertEquals(1, firstWeekEvents.size());
    assertEquals("Original Description", firstWeekEvents.get(0).getDescription());
    assertEquals("Original Location", firstWeekEvents.get(0).getLocation());
    assertTrue(firstWeekEvents.get(0).isPublic());

    List<Event> secondWeekEvents = calendar.getEventsOn(
        LocalDateTime.parse("2025-03-17T10:00", FORMATTER));
    assertEquals(1, secondWeekEvents.size());
    assertEquals("New Description", secondWeekEvents.get(0).getDescription());
    assertEquals("New Location", secondWeekEvents.get(0).getLocation());
    assertFalse(secondWeekEvents.get(0).isPublic());
  }

  @Test
  public void testEditAllEventsComprehensive() {
    assertTrue(calendar.createEvent("Meeting 1", date1, date2, false, "Description 1", "Location 1",
        true));
    assertTrue(calendar.createEvent("Meeting 2", date3, date4, false, "Description 2", "Location 2",
        true));

    assertTrue(calendar.editAllEvents("subject", "Meeting 1", "Updated Meeting"));

    List<Event> events = calendar.getEventsOn(date1);
    assertEquals(1, events.size());
    assertEquals("Updated Meeting", events.get(0).getSubject());

    assertFalse(calendar.editAllEvents("nonexistent", "Updated Meeting", "New Value"));
  }

  @Test
  public void testWeekdaysToStringAllDays() {
    assertTrue(calendar.createRecurringEvent(
        "Daily Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        "MTWRFSU",
        7,
        null,
        false,
        "Description",
        "Location",
        true));

    List<Event> events = calendar.getEventsFrom(
        LocalDateTime.parse("2025-03-10T00:00", FORMATTER),
        LocalDateTime.parse("2025-03-16T23:59", FORMATTER));

    assertEquals(7, events.size());
  }

  @Test
  public void testCalculateEquivalentEndTimeEdgeCases() {
    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:30", FORMATTER),
        "MW",
        4,
        null,
        false,
        "Description",
        "Location",
        true));

    LocalDateTime secondWeekStart = LocalDateTime.parse("2025-03-17T00:00", FORMATTER);
    assertTrue(
        calendar.editEventsFrom("subject", "Weekly Meeting"
            , secondWeekStart, "Updated Meeting"));

    List<Event> events = calendar.getEventsOn(LocalDateTime.parse("2025-03-17T10:00", FORMATTER));
    assertEquals(1, events.size());
    assertEquals(90,
        events.get(0).getEndDateTime().getMinute()
            - events.get(0).getStartDateTime().getMinute()
            +
            (events.get(0).getEndDateTime().getHour()
                - events.get(0).getStartDateTime().getHour()) * 60);
  }

//  @Test
//  public void testCSVExportWithVariousEvents() {
//    assertTrue(calendar.createEvent("Regular Meeting", date1, date2, false, "Meeting Description",
//        "Conference Room", true));
//    assertTrue(
//        calendar.createAllDayEvent("Holiday", date3, false
//            , "Company Holiday", "Office", false));
//    assertTrue(calendar.createRecurringEvent(
//        "Weekly Meeting",
//        LocalDateTime.parse("2025-03-10T10:00", FORMATTER),
//        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
//        "M",
//        2,
//        null,
//        false,
//        "Recurring Meeting",
//        "Room 101",
//        true));
//
//    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
//    assertNotNull(filePath);
//
//    File exportFile = new File(filePath);
//    assertTrue(exportFile.exists());
//    assertTrue(exportFile.length() > 0);
//  }

//  @Test
//  public void testComplexCSVEscaping() {
//    assertTrue(calendar.createEvent(
//        "Meeting, with \"quotes\" and comma",
//        date1,
//        date2,
//        false,
//        "Line 1\nLine 2\nLine 3",
//        "Room, 101",
//        true));
//
//    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
//    assertNotNull(filePath);
//
//    File exportFile = new File(filePath);
//    assertTrue(exportFile.exists());
//    assertTrue(exportFile.length() > 0);
//  }

  @Test
  public void testIsBusyWithVariousEventTypes() {
    assertTrue(
        calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));
    assertTrue(
        calendar.createAllDayEvent("Holiday", date3, false, "Description", "Location", true));
    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        "MW",
        4,
        null,
        false,
        "Description",
        "Location",
        true));

    assertTrue(calendar.isBusy(date1));
    assertTrue(calendar.isBusy(date3));
    assertTrue(calendar.isBusy(LocalDateTime.parse("2025-03-10T10:30", FORMATTER)));
    assertTrue(calendar.isBusy(LocalDateTime.parse("2025-03-12T10:30", FORMATTER)));

    assertFalse(calendar.isBusy(LocalDateTime.parse("2025-03-11T10:30", FORMATTER)));
    assertFalse(calendar.isBusy(LocalDateTime.parse("2025-03-12T11:30", FORMATTER)));
  }

  @Test
  public void testEventPropertyValidation() {
    assertTrue(
        calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));

    assertTrue(calendar.editEvent("subject", "Meeting", date1, date2, "New Meeting"));
    assertTrue(calendar.editEvent("description", "New Meeting", date1, date2, "New Description"));
    assertTrue(calendar.editEvent("location", "New Meeting", date1, date2, "New Location"));
    assertTrue(calendar.editEvent("public", "New Meeting", date1, date2, "false"));

    assertFalse(calendar.editEvent("color", "New Meeting", date1, date2, "Red"));
  }

  @Test
  public void testMatchingEventWithAndWithoutQuotes() {
    assertTrue(calendar.createEvent("Meeting with \"Quotes\""
        , date1, date2, false, "Description",
        "Location", true));

    assertTrue(
        calendar.editEvent("subject", "Meeting with \"Quotes\""
            , date1, date2, "Updated Meeting"));
    assertTrue(calendar.editEvent("subject", "Updated Meeting"
        , date1, date2, "Another Update"));
  }

  @Test
  public void testDateMatchingForEvents() {
    LocalDateTime multiDayStart = LocalDateTime.parse("2025-03-15T10:00", FORMATTER);
    LocalDateTime multiDayEnd = LocalDateTime.parse("2025-03-16T10:00", FORMATTER);

    assertTrue(
        calendar.createEvent("Two Day Event", multiDayStart, multiDayEnd, false, "Description",
            "Location", true));

    List<Event> day1Events = calendar.getEventsOn(
        LocalDateTime.parse("2025-03-15T12:00", FORMATTER));
    assertEquals(1, day1Events.size());

    List<Event> day2Events = calendar.getEventsOn(
        LocalDateTime.parse("2025-03-16T09:00", FORMATTER));
    assertEquals(1, day2Events.size());
  }

//  @Test
//  public void testExportCSVWithHeader() throws Exception {
//
//    assertTrue(
//        calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));
//
//    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
//    assertNotNull(filePath);
//
//    File exportFile = new File(filePath);
//    assertTrue(exportFile.exists());
//    assertTrue(exportFile.length() > 0);
//
//    BufferedReader reader = new BufferedReader(new FileReader(TEST_CSV_FILE));
//    String header = reader.readLine();
//    assertEquals(
//        "Subject,Start Date,Start Time,End Date,End Time," +
//            "All Day Event,Description,Location,Private",
//        header);
//    reader.close();
//  }

//  @Test
//  public void testExportRecurringEventTimeCalculation() throws Exception {
//
//    LocalDateTime startDateTime = LocalDateTime.parse("2025-03-10T10:15", FORMATTER);
//    LocalDateTime endDateTime = LocalDateTime.parse("2025-03-10T13:45", FORMATTER);
//
//    assertTrue(calendar.createRecurringEvent(
//        "Recurring Meeting",
//        startDateTime,
//        endDateTime,
//        "MWF",
//        3,
//        null,
//        false,
//        "Test Description",
//        "Test Location",
//        true));
//
//    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
//    assertNotNull(filePath);
//
//    BufferedReader reader = new BufferedReader(new FileReader(TEST_CSV_FILE));
//    reader.readLine();
//
//    for (int i = 0; i < 3; i++) {
//      String line = reader.readLine();
//      assertNotNull("Expected recurring event line", line);
//
//      assertTrue("Line should contain start and end times",
//          line.contains("10:15 AM") && line.contains("01:45 PM"));
//    }
//    reader.close();
//  }

//  @Test
//  public void testExportCSVWithIOException() {
//
//    String invalidPath = "/invalid/directory/that/doesnt/exist/file.csv";
//
//    String result = calendar.exportToCSV(invalidPath);
//    assertNull(result);
//  }

//  @Test
//  public void testWriteSingleEventToCSV() throws Exception {
//
//    assertTrue(calendar.createEvent("Regular Event", date1, date2, false,
//        "Regular Description", "Regular Location", true));
//
//    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
//    assertNotNull(filePath);
//
//    BufferedReader reader = new BufferedReader(new FileReader(TEST_CSV_FILE));
//    reader.readLine();
//    String line = reader.readLine();
//
//    assertTrue(line.contains("Regular Event"));
//    reader.close();
//  }

//  @Test
//  public void testWriteRecurringEventToCSV() throws Exception {
//
//    assertTrue(calendar.createRecurringEvent(
//        "Recurring Event",
//        date1,
//        date2,
//        "MW",
//        2,
//        null,
//        false,
//        "Recurring Description",
//        "Recurring Location",
//        false));
//
//    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
//    assertNotNull(filePath);
//
//    BufferedReader reader = new BufferedReader(new FileReader(TEST_CSV_FILE));
//    reader.readLine();
//
//    String line1 = reader.readLine();
//    String line2 = reader.readLine();
//
//    assertTrue(line1.contains("Recurring Event"));
//    assertTrue(line2.contains("Recurring Event"));
//    assertTrue(line1.contains("True"));
//    reader.close();
//  }

  @Test
  public void testEditEventsFromWithNoOccurrencesAfterStartDate() {

    LocalDateTime recurringEventStart = LocalDateTime.parse("2025-03-01T10:00", FORMATTER);
    LocalDateTime recurringEventEnd = LocalDateTime.parse("2025-03-01T11:00", FORMATTER);

    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        recurringEventStart,
        recurringEventEnd,
        "S",
        2,
        null,
        false,
        "Original Description",
        "Original Location",
        true));

    LocalDateTime editFromDate = LocalDateTime.parse("2025-03-15T00:00", FORMATTER);

    List<Event> events = calendar.getEventsOn(LocalDateTime.parse("2025-03-01T00:00", FORMATTER));
    assertEquals(1, events.size());
    assertTrue(events.get(0).getSubject().contains("Weekly Meeting"));
  }

  @Test
  public void testEditEventsFromNonRecurringEvent() {

    LocalDateTime eventStart = LocalDateTime.parse("2025-03-15T10:00", FORMATTER);
    LocalDateTime eventEnd = LocalDateTime.parse("2025-03-15T11:00", FORMATTER);

    assertTrue(calendar.createEvent("Regular Meeting", eventStart, eventEnd, false,
        "Original Description", "Original Location", true));

    LocalDateTime beforeDate = LocalDateTime.parse("2025-03-01T00:00", FORMATTER);

    assertTrue(
        calendar.editEventsFrom("subject", "Regular Meeting", eventStart, "Updated Meeting"));

    List<Event> events = calendar.getEventsOn(LocalDateTime.parse("2025-03-15T00:00", FORMATTER));
    assertEquals(1, events.size());
    assertTrue(events.get(0).getSubject().contains("Updated Meeting"));

    assertFalse(
        calendar.editEventsFrom("invalidProperty", "Updated Meeting", eventStart, "New Value"));
  }

  @Test
  public void testWeekdaysToString() {

    LocalDateTime startDate = LocalDateTime.parse("2025-03-10T10:00", FORMATTER);
    LocalDateTime endDate = LocalDateTime.parse("2025-03-10T11:00", FORMATTER);

    assertTrue(calendar.createRecurringEvent(
        "Daily Meeting",
        startDate,
        endDate,
        "MTWRFSU",
        7,
        null,
        false,
        "Description",
        "Location",
        true));

    List<Event> events = calendar.getEventsFrom(
        LocalDateTime.parse("2025-03-10T00:00", FORMATTER),
        LocalDateTime.parse("2025-03-16T23:59", FORMATTER)
    );

    assertEquals(7, events.size());

    assertTrue(hasEventOn(events, "2025-03-10"));
    assertTrue(hasEventOn(events, "2025-03-11"));
    assertTrue(hasEventOn(events, "2025-03-12"));
    assertTrue(hasEventOn(events, "2025-03-13"));
    assertTrue(hasEventOn(events, "2025-03-14"));
    assertTrue(hasEventOn(events, "2025-03-15"));
    assertTrue(hasEventOn(events, "2025-03-16"));
  }

  @Test
  public void testUpdateEventPropertySubject() {
    assertTrue(calendar.createEvent("Meeting", date1, date2
        , false, "Description", "Location", true));

    List<Event> events = calendar.getEventsOn(date1);
    assertEquals(1, events.size());

    assertTrue(calendar.editEvent("subject", "Meeting", date1, date2, "Updated Meeting"));

    events = calendar.getEventsOn(date1);
    assertEquals("Updated Meeting", events.get(0).getSubject());
  }

  @Test
  public void testUpdateEventPropertyDescription() {
    assertTrue(calendar.createEvent("Meeting", date1, date2
        , false, "Description", "Location", true));

    assertTrue(calendar.editEvent("description", "Meeting", date1, date2, "Updated Description"));

    List<Event> events = calendar.getEventsOn(date1);
    assertEquals("Updated Description", events.get(0).getDescription());
  }

  @Test
  public void testUpdateEventPropertyLocation() {
    assertTrue(calendar.createEvent("Meeting", date1, date2
        , false, "Description", "Location", true));

    assertTrue(calendar.editEvent("location", "Meeting", date1, date2, "Updated Location"));

    List<Event> events = calendar.getEventsOn(date1);
    assertEquals("Updated Location", events.get(0).getLocation());
  }

  @Test
  public void testUpdateEventPropertyPublic() {
    assertTrue(calendar.createEvent("Meeting", date1, date2
        , false, "Description", "Location", true));

    assertTrue(calendar.editEvent("public", "Meeting", date1, date2, "false"));

    List<Event> events = calendar.getEventsOn(date1);
    assertFalse(events.get(0).isPublic());

    assertTrue(calendar.editEvent("public", "Meeting", date1, date2, "true"));

    events = calendar.getEventsOn(date1);
    assertTrue(events.get(0).isPublic());
  }

  @Test
  public void testUpdateEventPropertyStartTime() {
    assertTrue(calendar.createEvent("Meeting", date1, date2
        , false, "Description", "Location", true));

    LocalDateTime newStart = date1.minusHours(1);
    assertTrue(calendar.editEvent("starttime", "Meeting", date1
        , date2, newStart.format(FORMATTER)));

    List<Event> events = calendar.getEventsOn(newStart);
    assertEquals(1, events.size());
    assertEquals(newStart, events.get(0).getStartDateTime());
  }

  @Test
  public void testUpdateEventPropertyStartTimeInvalidRange() {
    assertTrue(calendar.createEvent("Meeting", date1, date2
        , false, "Description", "Location", true));

    LocalDateTime invalidStart = date2.plusMinutes(30);
    assertFalse(calendar.editEvent("starttime", "Meeting", date1
        , date2, invalidStart.format(FORMATTER)));
  }

  @Test
  public void testUpdateEventPropertyStartTimeParseException() {
    assertTrue(calendar.createEvent("Meeting", date1, date2
        , false, "Description", "Location", true));

    assertFalse(calendar.editEvent("starttime", "Meeting"
        , date1, date2, "invalid-date-format"));
  }

  @Test
  public void testUpdateEventPropertyEndTimeForAllDayEvent() {
    LocalDateTime dateTime = LocalDateTime.parse("2025-03-15T00:00", FORMATTER);
    assertTrue(calendar.createAllDayEvent("Holiday", dateTime
        , false, "Description", "Location", true));

    assertFalse(calendar.editEvent("endtime", "Holiday"
        , dateTime, null, date2.format(FORMATTER)));
  }

  @Test
  public void testUpdateEventPropertyEndTime() {
    assertTrue(calendar.createEvent("Meeting", date1, date2
        , false, "Description", "Location", true));

    LocalDateTime newEnd = date2.plusHours(1);
    assertTrue(calendar.editEvent("endtime", "Meeting"
        , date1, date2, newEnd.format(FORMATTER)));

    List<Event> events = calendar.getEventsOn(date1);
    assertEquals(1, events.size());
    assertEquals(newEnd, events.get(0).getEndDateTime());
  }

  @Test
  public void testUpdateEventPropertyEndTimeInvalidRange() {
    assertTrue(calendar.createEvent("Meeting", date1, date2
        , false, "Description", "Location", true));

    LocalDateTime invalidEnd = date1.minusMinutes(30);
    assertFalse(calendar.editEvent("endtime", "Meeting"
        , date1, date2, invalidEnd.format(FORMATTER)));

    assertFalse(calendar.editEvent("endtime", "Meeting", date1
        , date2, date1.format(FORMATTER)));
  }

  @Test
  public void testUpdateEventPropertyEndTimeParseException() {
    assertTrue(calendar.createEvent("Meeting", date1, date2
        , false, "Description", "Location", true));

    assertFalse(calendar.editEvent("endtime", "Meeting", date1
        , date2, "invalid-date-format"));
  }

  @Test
  public void testUpdateEventPropertyInvalid() {
    assertTrue(calendar.createEvent("Meeting", date1, date2
        , false, "Description", "Location", true));

    assertFalse(calendar.editEvent("nonexistent", "Meeting", date1
        , date2, "New Value"));
  }

  @Test
  public void testHasConflictsStreaming() {
    assertTrue(calendar.createEvent("Meeting", date1, date2
        , false, "Description", "Location", true));
    assertFalse(calendar.createEvent("Conflicting Meeting", date1.plusMinutes(30)
        , date2.minusMinutes(30), true, "Description", "Location", true));
    assertTrue(calendar.createEvent("Non-Conflicting Meeting", date2.plusMinutes(5)
        , date3, false, "Description", "Location", true));
  }

  @Test
  public void testHasConflictsNonRecurringEvents() {
    assertTrue(calendar.createEvent("First Meeting", date1, date2
        , false, "Description", "Location", true));
    LocalDateTime startTime = date1.plusMinutes(30);
    LocalDateTime endTime = date2.minusMinutes(30);
    assertFalse(calendar.createEvent("Conflicting Meeting", startTime, endTime
        , true, "Description", "Location", true));
  }

  @Test
  public void testHasConflictsAllDayEvents() {
    assertTrue(calendar.createAllDayEvent("Holiday", date1, false
        , "Description", "Location", true));
    assertFalse(calendar.createAllDayEvent("Another Holiday", date1, true
        , "Description", "Location", true));
    assertTrue(calendar.createAllDayEvent("Next Day", date1.plusDays(1), true
        , "Description", "Location", true));
  }

  @Test
  public void testHasConflictsNormalEventsTimeComparison() {
    assertTrue(calendar.createEvent("First Meeting", date1, date2
        , false, "Description", "Location", true));

    // Event that ends exactly when first starts (should not conflict)
    assertTrue(calendar.createEvent("Before Meeting", date1.minusHours(2)
        , date1, false, "Description", "Location", true));

    // Event that starts exactly when first ends (should not conflict)
    assertTrue(calendar.createEvent("After Meeting", date2, date2.plusHours(2)
        , false, "Description", "Location", true));

    // Event that overlaps the beginning (should conflict)
    assertFalse(calendar.createEvent("Overlap Start", date1.minusHours(1)
        , date1.plusHours(1), true, "Description", "Location", true));

    // Event that overlaps the end (should conflict)
    assertFalse(calendar.createEvent("Overlap End", date2.minusHours(1)
        , date2.plusHours(1), true, "Description", "Location", true));
  }

  @Test
  public void testHasConflictsRecurringEvents() {
    LocalDateTime recurringStart = LocalDateTime.parse("2025-03-10T10:00", FORMATTER);
    LocalDateTime recurringEnd = LocalDateTime.parse("2025-03-10T11:00", FORMATTER);

    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        recurringStart,
        recurringEnd,
        "M", // Monday
        3,   // 3 occurrences
        null,
        false,
        "Description",
        "Location",
        true));

    // Test conflict with second occurrence (March 17)
    LocalDateTime conflictStart = LocalDateTime.parse("2025-03-17T10:30", FORMATTER);
    LocalDateTime conflictEnd = LocalDateTime.parse("2025-03-17T11:30", FORMATTER);
    assertFalse(calendar.createEvent("Conflicting Meeting", conflictStart, conflictEnd
        , true, "Description", "Location", true));

    // Test no conflict on a different day
    LocalDateTime nonConflictStart = LocalDateTime.parse("2025-03-18T10:00", FORMATTER);
    LocalDateTime nonConflictEnd = LocalDateTime.parse("2025-03-18T11:00", FORMATTER);
    assertTrue(calendar.createEvent("Non-Conflicting Meeting", nonConflictStart
        , nonConflictEnd, true, "Description", "Location", true));
  }

  @Test
  public void testHasConflictsRecurringAllDayEvents() {
    LocalDateTime recurringStart = LocalDateTime.parse("2025-03-08T00:00", FORMATTER);

    assertTrue(calendar.createRecurringAllDayEvent(
        "Weekend Meeting",
        recurringStart,
        "SU", // Saturday and Sunday
        3,    // 3 occurrences
        null,
        false,
        "Description",
        "Location",
        true));

    // Test conflict with recurrence (March 15)
    LocalDateTime conflictDate = LocalDateTime.parse("2025-03-15T12:00", FORMATTER);
    assertFalse(calendar.createAllDayEvent("Conflicting Holiday", conflictDate
        , true, "Description", "Location", true));

    // Test no conflict on a different day
    LocalDateTime nonConflictDate = LocalDateTime.parse("2025-03-14T00:00", FORMATTER);
    assertTrue(calendar.createAllDayEvent("Non-Conflicting Holiday", nonConflictDate
        , true, "Description", "Location", true));
  }

  @Test
  public void testHasConflictsRecurringWithTimeComparison() {
    LocalDateTime recurringStart = LocalDateTime.parse("2025-03-10T10:00", FORMATTER);
    LocalDateTime recurringEnd = LocalDateTime.parse("2025-03-10T12:00", FORMATTER);

    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        recurringStart,
        recurringEnd,
        "M", // Monday
        3,   // 3 occurrences
        null,
        false,
        "Description",
        "Location",
        true));

    // Test overlap at second occurrence (March 17)
    LocalDateTime overlapStart = LocalDateTime.parse("2025-03-17T09:00", FORMATTER);
    LocalDateTime overlapEnd = LocalDateTime.parse("2025-03-17T11:00", FORMATTER);
    assertFalse(calendar.createEvent("Overlapping Meeting", overlapStart, overlapEnd
        , true, "Description", "Location", true));

    // Test exact boundary case (ending when recurring starts) - should not conflict
    LocalDateTime boundaryStart = LocalDateTime.parse("2025-03-17T08:00", FORMATTER);
    LocalDateTime boundaryEnd = LocalDateTime.parse("2025-03-17T10:00", FORMATTER);
    assertTrue(calendar.createEvent("Boundary Meeting", boundaryStart, boundaryEnd
        , true, "Description", "Location", true));
  }

  @Test
  public void testHasConflictsNoConflict() {
    LocalDateTime event1Start = LocalDateTime.parse("2025-03-10T10:00", FORMATTER);
    LocalDateTime event1End = LocalDateTime.parse("2025-03-10T12:00", FORMATTER);

    LocalDateTime event2Start = LocalDateTime.parse("2025-03-10T14:00", FORMATTER);
    LocalDateTime event2End = LocalDateTime.parse("2025-03-10T16:00", FORMATTER);

    assertTrue(calendar.createEvent("First Meeting", event1Start, event1End
        , false, "Description", "Location", true));
    assertTrue(calendar.createEvent("Second Meeting", event2Start, event2End
        , true, "Description", "Location", true));
  }


  private boolean hasEventOn(List<Event> events, String dateStr) {
    for (Event event : events) {
      if (event.getStartDateTime().toLocalDate().toString().startsWith(dateStr)) {
        return true;
      }
    }
    return false;
  }

}