
import java.io.BufferedReader;
import java.io.FileReader;
import model.Calendar;
import model.CalendarImpl;
import model.Event;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.Assert.*;

public class CalendarImplTest {

  private Calendar calendar;
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final String TEST_CSV_FILE = "test-calendar-export.csv";

  // Sample dates for testing
  private LocalDateTime date1 = LocalDateTime.parse("2025-03-15T09:00", FORMATTER);
  private LocalDateTime date2 = LocalDateTime.parse("2025-03-15T11:00", FORMATTER);
  private LocalDateTime date3 = LocalDateTime.parse("2025-03-16T09:00", FORMATTER);
  private LocalDateTime date4 = LocalDateTime.parse("2025-03-20T14:00", FORMATTER);

  @Before
  public void setUp() {
    calendar = new CalendarImpl();
  }

  @After
  public void tearDown() {
    // Clean up any exported files
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
    assertTrue(calendar.createEvent("Meeting", date1, date2, false, "Team meeting", "Conference Room", true));

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
    assertTrue(calendar.createAllDayEvent("Holiday", date1, false, "Company Holiday", "Office", true));

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
    // Null event name
    assertFalse(calendar.createEvent(null, date1, date2, false, "Description", "Location", true));

    // Empty event name
    assertFalse(calendar.createEvent("", date1, date2, false, "Description", "Location", true));

    // Null start date
    assertFalse(calendar.createEvent("Meeting", null, date2, false, "Description", "Location", true));

    // End date before start date
    assertFalse(calendar.createEvent("Meeting", date2, date1, false, "Description", "Location", true));

    // Same start and end date
    assertFalse(calendar.createEvent("Meeting", date1, date1, false, "Description", "Location", true));
  }

  /*
   * Conflict Tests
   */
  @Test
  public void testEventConflicts() {
    // Add first event
    assertTrue(calendar.createEvent("Meeting 1", date1, date2, false, "Description", "Location", true));

    // Try to add conflicting event with autoDecline=true
    assertFalse(calendar.createEvent("Meeting 2", date1.plusMinutes(30), date2.plusMinutes(30), true, "Description", "Location", true));

    // Add non-conflicting event
    assertTrue(calendar.createEvent("Meeting 3", date2, date3, true, "Description", "Location", true));
  }

  @Test
  public void testAllDayEventConflicts() {
    // Add all-day event
    assertTrue(calendar.createAllDayEvent("Holiday", date1, false, "Description", "Location", true));

    // Try to add conflicting all-day event with autoDecline=true
    assertFalse(calendar.createAllDayEvent("Meeting", date1, true, "Description", "Location", true));

    // Try to add conflicting regular event with autoDecline=true
    assertFalse(calendar.createEvent("Meeting", date1, date1.plusHours(2), true, "Description", "Location", true));

    // Add non-conflicting event on a different day
    assertTrue(calendar.createAllDayEvent("Another Holiday", date3, true, "Description", "Location", true));
  }

  /*
   * Recurring Event Tests
   */
  @Test
  public void testCreateRecurringEvent() {
    // Create recurring event on Monday and Wednesday for 2 occurrences
    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER), // Monday
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        "MW", // Monday and Wednesday
        2, // 2 occurrences
        null, // No end date
        false, "Team Sync", "Conference Room", true));

    // Check first occurrence
    List<Event> events = calendar.getEventsOn(LocalDateTime.parse("2025-03-10T10:00", FORMATTER));
    assertEquals(1, events.size());

    // Check second occurrence (Wednesday)
    events = calendar.getEventsOn(LocalDateTime.parse("2025-03-12T10:00", FORMATTER));
    assertEquals(1, events.size());

    // Should not have a third occurrence
    events = calendar.getEventsOn(LocalDateTime.parse("2025-03-17T10:00", FORMATTER)); // Next Monday
    assertEquals(0, events.size());
  }

  @Test
  public void testCreateRecurringAllDayEvent() {
    // Create recurring all-day event on weekends until a specific date
    LocalDateTime untilDate = LocalDateTime.parse("2025-03-30T00:00", FORMATTER);

    assertTrue(calendar.createRecurringAllDayEvent(
        "Weekend Check-in",
        LocalDateTime.parse("2025-03-15T00:00", FORMATTER), // Saturday
        "SU", // Saturday and Sunday
        -1, // No occurrence limit
        untilDate,
        false, "Weekend work", "Remote", true));

    // Check first weekend
    List<Event> events = calendar.getEventsOn(LocalDateTime.parse("2025-03-15T12:00", FORMATTER)); // Saturday
    assertEquals(1, events.size());
    events = calendar.getEventsOn(LocalDateTime.parse("2025-03-16T12:00", FORMATTER)); // Sunday
    assertEquals(1, events.size());

    // Check second weekend
    events = calendar.getEventsOn(LocalDateTime.parse("2025-03-22T12:00", FORMATTER)); // Next Saturday
    assertEquals(1, events.size());

    // Check after the until date
    events = calendar.getEventsOn(LocalDateTime.parse("2025-04-05T12:00", FORMATTER)); // Saturday after until date
    assertEquals(0, events.size());
  }

  @Test
  public void testRecurringEventConflicts() {
    // Add regular event
    assertTrue(calendar.createEvent("Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        false, "Description", "Location", true));

    // Try to add conflicting recurring event with autoDecline=true
    assertFalse(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:30", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:30", FORMATTER),
        "M", // Monday
        2,
        null,
        true, "Description", "Location", true));

    // Add non-conflicting recurring event
    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T14:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T15:00", FORMATTER),
        "M", // Monday
        2,
        null,
        true, "Description", "Location", true));
  }

  @Test
  public void testMultiDayRecurringEventRestriction() {
    // Attempt to create recurring event that spans multiple days (should fail)
    assertFalse(calendar.createRecurringEvent(
        "Overnight Workshop",
        LocalDateTime.parse("2025-03-10T14:00", FORMATTER),
        LocalDateTime.parse("2025-03-11T10:00", FORMATTER), // Next day
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
    // Create event
    assertTrue(calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));

    // Edit various properties
    assertTrue(calendar.editEvent("subject", "Meeting", date1, date2, "Updated Meeting"));
    assertTrue(calendar.editEvent("description", "Updated Meeting", date1, date2, "New description"));
    assertTrue(calendar.editEvent("location", "Updated Meeting", date1, date2, "New location"));
    assertTrue(calendar.editEvent("public", "Updated Meeting", date1, date2, "false"));

    // Verify changes
    List<Event> events = calendar.getEventsOn(date1);
    assertEquals(1, events.size());
    Event event = events.get(0);
    assertEquals("Updated Meeting", event.getSubject());
    assertEquals("New description", event.getDescription());
    assertEquals("New location", event.getLocation());
    assertFalse(event.isPublic());

    // Test invalid property
    assertFalse(calendar.editEvent("invalidProperty", "Updated Meeting", date1, date2, "New value"));

    // Test event not found
    assertFalse(calendar.editEvent("subject", "Non-existent", date1, date2, "New value"));
  }

  @Test
  public void testEditEventsFrom() {
    // Create recurring event
    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER), // Monday
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        "MW", // Monday and Wednesday
        4, // 4 occurrences over 2 weeks
        null,
        false, "Description", "Location", true));

    // Edit events from second week
    LocalDateTime secondWeekStart = LocalDateTime.parse("2025-03-17T00:00", FORMATTER);
    assertTrue(calendar.editEventsFrom("subject", "Weekly Meeting", secondWeekStart, "Updated Meeting"));

    // Check first week (unchanged)
    List<Event> events = calendar.getEventsOn(LocalDateTime.parse("2025-03-10T10:00", FORMATTER));
    assertEquals(1, events.size());
    assertEquals("Weekly Meeting [Recurring]", events.get(0).getSubject());

    // Check second week (changed)
    events = calendar.getEventsOn(LocalDateTime.parse("2025-03-17T10:00", FORMATTER));
    assertEquals(1, events.size());
    assertTrue(events.get(0).getSubject().contains("Updated Meeting"));

    // Test invalid property
    assertFalse(calendar.editEventsFrom("invalidProperty", "Weekly Meeting", secondWeekStart, "New value"));
  }

  @Test
  public void testEditAllEvents() {
    // Create multiple events with same name
    assertTrue(calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));
    assertTrue(calendar.createEvent("Meeting", date3, date4, false, "Description", "Location", true));

    // Edit all events
    assertTrue(calendar.editAllEvents("subject", "Meeting", "Team Meeting"));
    assertTrue(calendar.editAllEvents("description", "Team Meeting", "Important discussion"));

    // Verify changes
    List<Event> events = calendar.getEventsFrom(date1, date4);
    assertEquals(2, events.size());
    for (Event event : events) {
      assertEquals("Team Meeting", event.getSubject());
      assertEquals("Important discussion", event.getDescription());
    }

    // Test invalid property
    assertFalse(calendar.editAllEvents("invalidProperty", "Team Meeting", "New value"));
  }

  /*
   * Query Tests
   */
  @Test
  public void testGetEventsOn() {
    // Create events on different dates
    assertTrue(calendar.createEvent("Meeting 1", date1, date2, false, "Description", "Location", true));
    assertTrue(calendar.createEvent("Meeting 2", date3, date4, false, "Description", "Location", true));

    // Query for events on specific date
    List<Event> events = calendar.getEventsOn(date1);
    assertEquals(1, events.size());
    assertEquals("Meeting 1", events.get(0).getSubject());

    // Query for date with no events
    events = calendar.getEventsOn(date1.plusDays(7));
    assertEquals(0, events.size());
  }

  @Test
  public void testGetEventsFrom() {
    // Create events
    assertTrue(calendar.createEvent("Meeting 1", date1, date2, false, "Description", "Location", true));
    assertTrue(calendar.createEvent("Meeting 2", date3, date4, false, "Description", "Location", true));

    // Query for events in date range
    List<Event> events = calendar.getEventsFrom(date1, date4);
    assertEquals(2, events.size());

    // Query for events in partial range
    events = calendar.getEventsFrom(date1, date1.plusHours(3));
    assertEquals(1, events.size());
    assertEquals("Meeting 1", events.get(0).getSubject());

    // Query for range with no events
    events = calendar.getEventsFrom(date4.plusDays(1), date4.plusDays(2));
    assertEquals(0, events.size());
  }

  @Test
  public void testIsBusy() {
    // Create events
    assertTrue(calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));

    // Test times during the event
    assertTrue(calendar.isBusy(date1));
    assertTrue(calendar.isBusy(date1.plusMinutes(30)));
    assertTrue(calendar.isBusy(date2.minusMinutes(1)));

    // Test times not during any event
    assertFalse(calendar.isBusy(date1.minusMinutes(1)));
    assertFalse(calendar.isBusy(date2.plusMinutes(1)));
    assertFalse(calendar.isBusy(date3));
  }

  @Test
  public void testIsBusyWithAllDayEvent() {
    // Create all-day event
    assertTrue(calendar.createAllDayEvent("Holiday", date1, false, "Description", "Location", true));

    // Test times during the day
    assertTrue(calendar.isBusy(date1));
    assertTrue(calendar.isBusy(date1.plusHours(12)));

    // Test times on different days
    assertFalse(calendar.isBusy(date1.minusDays(1)));
    assertFalse(calendar.isBusy(date1.plusDays(1)));
  }

  @Test
  public void testIsBusyWithRecurringEvent() {
    // Create recurring event
    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER), // Monday
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        "MW", // Monday and Wednesday
        2,
        null,
        false, "Description", "Location", true));

    // Test during first occurrence
    assertTrue(calendar.isBusy(LocalDateTime.parse("2025-03-10T10:30", FORMATTER)));

    // Test during second occurrence
    assertTrue(calendar.isBusy(LocalDateTime.parse("2025-03-12T10:30", FORMATTER)));

    // Test outside of occurrence times
    assertFalse(calendar.isBusy(LocalDateTime.parse("2025-03-10T09:30", FORMATTER)));
    assertFalse(calendar.isBusy(LocalDateTime.parse("2025-03-12T11:30", FORMATTER)));

    // Test on non-recurring day
    assertFalse(calendar.isBusy(LocalDateTime.parse("2025-03-11T10:30", FORMATTER)));
  }

  /*
   * Export Tests
   */
  @Test
  public void testExportToCSV() {
    // Create various types of events
    assertTrue(calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));
    assertTrue(calendar.createAllDayEvent("Holiday", date3, false, "Description", "Location", false));

    // Export to CSV
    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
    assertNotNull(filePath);

    // Verify file exists
    File exportFile = new File(filePath);
    assertTrue(exportFile.exists());
    assertTrue(exportFile.length() > 0);
  }

  /*
   * Special Case Tests
   */
  @Test
  public void testCSVEscaping() {
    // Create event with values that need escaping in CSV
    assertTrue(calendar.createEvent("Meeting, with comma", date1, date2, false,
        "Description with \"quotes\"", "Location\nwith newline", true));

    // Export to CSV
    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
    assertNotNull(filePath);

    // We don't parse the CSV back but the export should complete successfully
    File exportFile = new File(filePath);
    assertTrue(exportFile.exists());
  }

  @Test
  public void testQuotedEventNameHandling() {
    // Create event
    assertTrue(calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));

    // Edit with quoted name (should still match)
    assertTrue(calendar.editEvent("subject", "\"Meeting\"", date1, date2, "Updated Meeting"));

    // Verify changes
    List<Event> events = calendar.getEventsOn(date1);
    assertEquals(1, events.size());
    assertEquals("Updated Meeting", events.get(0).getSubject());
  }

  @Test
  public void testRecurringEventExpansionAndOccurrences() {
    // Create recurring event
    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER), // Monday
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        "MWF", // Monday, Wednesday, Friday
        3, // 3 occurrences
        null,
        false, "Team Sync", "Conference Room", true));

    // Get events for a date range covering all occurrences
    List<Event> events = calendar.getEventsFrom(
        LocalDateTime.parse("2025-03-10T00:00", FORMATTER),
        LocalDateTime.parse("2025-03-14T23:59", FORMATTER));

    // Should find 3 occurrences
    assertEquals(3, events.size());

    // Occurrences should have the [Recurring] marker
    for (Event event : events) {
      assertTrue(event.getSubject().contains("[Recurring]"));
    }
  }

  @Test
  public void testCreateAllDayEventWithConflict() {
    assertTrue(calendar.createAllDayEvent("Holiday", date1, false, "Description", "Location", true));
    assertFalse(calendar.createAllDayEvent("Meeting", date1, true, "Description", "Location", true));
  }

  @Test
  public void testCreateRecurringEventWithConflict() {
    assertTrue(calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));

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
    assertTrue(calendar.createAllDayEvent("Holiday", date1, false, "Description", "Location", true));

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
        LocalDateTime.parse("2025-03-15T00:00", FORMATTER), // Saturday
        "SU", // Saturday and Sunday
        3, // 3 occurrences
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

    assertTrue(calendar.editEventsFrom("subject", "Weekly Meeting", secondWeekStart, "Updated Meeting"));
    assertTrue(calendar.editEventsFrom("description", "Updated Meeting", secondWeekStart, "New Description"));
    assertTrue(calendar.editEventsFrom("location", "Updated Meeting", secondWeekStart, "New Location"));
    assertTrue(calendar.editEventsFrom("public", "Updated Meeting", secondWeekStart, "false"));

    assertFalse(calendar.editEventsFrom("nonexistent", "Updated Meeting", secondWeekStart, "New Value"));

    List<Event> firstWeekEvents = calendar.getEventsOn(LocalDateTime.parse("2025-03-10T10:00", FORMATTER));
    assertEquals(1, firstWeekEvents.size());
    assertEquals("Original Description", firstWeekEvents.get(0).getDescription());
    assertEquals("Original Location", firstWeekEvents.get(0).getLocation());
    assertTrue(firstWeekEvents.get(0).isPublic());

    List<Event> secondWeekEvents = calendar.getEventsOn(LocalDateTime.parse("2025-03-17T10:00", FORMATTER));
    assertEquals(1, secondWeekEvents.size());
    assertEquals("New Description", secondWeekEvents.get(0).getDescription());
    assertEquals("New Location", secondWeekEvents.get(0).getLocation());
    assertFalse(secondWeekEvents.get(0).isPublic());
  }

  @Test
  public void testEditAllEventsComprehensive() {
    assertTrue(calendar.createEvent("Meeting 1", date1, date2, false, "Description 1", "Location 1", true));
    assertTrue(calendar.createEvent("Meeting 2", date3, date4, false, "Description 2", "Location 2", true));

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
        "MTWRFSU", // All days of week
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
        LocalDateTime.parse("2025-03-10T11:30", FORMATTER), // 1 hour 30 minutes
        "MW",
        4,
        null,
        false,
        "Description",
        "Location",
        true));

    LocalDateTime secondWeekStart = LocalDateTime.parse("2025-03-17T00:00", FORMATTER);
    assertTrue(calendar.editEventsFrom("subject", "Weekly Meeting", secondWeekStart, "Updated Meeting"));

    List<Event> events = calendar.getEventsOn(LocalDateTime.parse("2025-03-17T10:00", FORMATTER));
    assertEquals(1, events.size());
    assertEquals(90, events.get(0).getEndDateTime().getMinute() - events.get(0).getStartDateTime().getMinute()
        + (events.get(0).getEndDateTime().getHour() - events.get(0).getStartDateTime().getHour()) * 60);
  }

  @Test
  public void testCSVExportWithVariousEvents() {
    assertTrue(calendar.createEvent("Regular Meeting", date1, date2, false, "Meeting Description", "Conference Room", true));
    assertTrue(calendar.createAllDayEvent("Holiday", date3, false, "Company Holiday", "Office", false));
    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        LocalDateTime.parse("2025-03-10T10:00", FORMATTER),
        LocalDateTime.parse("2025-03-10T11:00", FORMATTER),
        "M",
        2,
        null,
        false,
        "Recurring Meeting",
        "Room 101",
        true));

    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
    assertNotNull(filePath);

    File exportFile = new File(filePath);
    assertTrue(exportFile.exists());
    assertTrue(exportFile.length() > 0);
  }

  @Test
  public void testComplexCSVEscaping() {
    assertTrue(calendar.createEvent(
        "Meeting, with \"quotes\" and comma",
        date1,
        date2,
        false,
        "Line 1\nLine 2\nLine 3",
        "Room, 101",
        true));

    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
    assertNotNull(filePath);

    File exportFile = new File(filePath);
    assertTrue(exportFile.exists());
    assertTrue(exportFile.length() > 0);
  }

  @Test
  public void testIsBusyWithVariousEventTypes() {
    assertTrue(calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));
    assertTrue(calendar.createAllDayEvent("Holiday", date3, false, "Description", "Location", true));
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
    assertTrue(calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));

    assertTrue(calendar.editEvent("subject", "Meeting", date1, date2, "New Meeting"));
    assertTrue(calendar.editEvent("description", "New Meeting", date1, date2, "New Description"));
    assertTrue(calendar.editEvent("location", "New Meeting", date1, date2, "New Location"));
    assertTrue(calendar.editEvent("public", "New Meeting", date1, date2, "false"));

    assertFalse(calendar.editEvent("color", "New Meeting", date1, date2, "Red"));
  }

  @Test
  public void testMatchingEventWithAndWithoutQuotes() {
    assertTrue(calendar.createEvent("Meeting with \"Quotes\"", date1, date2, false, "Description", "Location", true));

    assertTrue(calendar.editEvent("subject", "Meeting with \"Quotes\"", date1, date2, "Updated Meeting"));
    assertTrue(calendar.editEvent("subject", "Updated Meeting", date1, date2, "Another Update"));
  }

  @Test
  public void testDateMatchingForEvents() {
    LocalDateTime multiDayStart = LocalDateTime.parse("2025-03-15T10:00", FORMATTER);
    LocalDateTime multiDayEnd = LocalDateTime.parse("2025-03-16T10:00", FORMATTER);

    assertTrue(calendar.createEvent("Two Day Event", multiDayStart, multiDayEnd, false, "Description", "Location", true));

    List<Event> day1Events = calendar.getEventsOn(LocalDateTime.parse("2025-03-15T12:00", FORMATTER));
    assertEquals(1, day1Events.size());

    List<Event> day2Events = calendar.getEventsOn(LocalDateTime.parse("2025-03-16T09:00", FORMATTER));
    assertEquals(1, day2Events.size());
  }

  @Test
  public void testExportCSVWithHeader() throws Exception {
    // Test coverage for line 505 (CSV header writing)
    assertTrue(calendar.createEvent("Meeting", date1, date2, false, "Description", "Location", true));

    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
    assertNotNull(filePath);

    // Verify file exists and has content
    File exportFile = new File(filePath);
    assertTrue(exportFile.exists());
    assertTrue(exportFile.length() > 0);

    // Verify CSV header is written correctly (covers line 505)
    BufferedReader reader = new BufferedReader(new FileReader(TEST_CSV_FILE));
    String header = reader.readLine();
    assertEquals("Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description,Location,Private", header);
    reader.close();
  }

  @Test
  public void testExportRecurringEventTimeCalculation() throws Exception {
    // Test coverage for lines 520-523 (time difference calculations)
    // Create event with non-zero hours and minutes differences
    LocalDateTime startDateTime = LocalDateTime.parse("2025-03-10T10:15", FORMATTER); // 10:15
    LocalDateTime endDateTime = LocalDateTime.parse("2025-03-10T13:45", FORMATTER);   // 13:45

    assertTrue(calendar.createRecurringEvent(
        "Recurring Meeting",
        startDateTime,
        endDateTime,
        "MWF", // Monday, Wednesday, Friday
        3,
        null,
        false,
        "Test Description",
        "Test Location",
        true));

    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
    assertNotNull(filePath);

    // Each recurrence should have the same duration (3 hours and 30 minutes)
    BufferedReader reader = new BufferedReader(new FileReader(TEST_CSV_FILE));
    reader.readLine(); // Skip header

    // Read each recurrence and verify time format
    for (int i = 0; i < 3; i++) {
      String line = reader.readLine();
      assertNotNull("Expected recurring event line", line);

      // Verify time format (we're just ensuring the calculations didn't fail)
      assertTrue("Line should contain start and end times",
          line.contains("10:15 AM") && line.contains("01:45 PM"));
    }
    reader.close();
  }

  @Test
  public void testExportCSVWithIOException() {
    // Test coverage for lines 536-537 (exception handling)
    // Use an invalid file path to force an IOException
    String invalidPath = "/invalid/directory/that/doesnt/exist/file.csv";

    // This should return null due to the IOException
    String result = calendar.exportToCSV(invalidPath);
    assertNull(result);
  }

  @Test
  public void testWriteSingleEventToCSV() throws Exception {
    // Test coverage for line 530 (writeSingleEventToCSV for non-recurring events)
    assertTrue(calendar.createEvent("Regular Event", date1, date2, false,
        "Regular Description", "Regular Location", true));

    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
    assertNotNull(filePath);

    // Verify the single event was written correctly
    BufferedReader reader = new BufferedReader(new FileReader(TEST_CSV_FILE));
    reader.readLine(); // Skip header
    String line = reader.readLine();

    // Verify event data is in the file
    assertTrue(line.contains("Regular Event"));
    reader.close();
  }

  @Test
  public void testWriteRecurringEventToCSV() throws Exception {
    // Test coverage for line 526 (writeSingleEventToCSV for recurring events)
    assertTrue(calendar.createRecurringEvent(
        "Recurring Event",
        date1,
        date2,
        "MW", // Monday and Wednesday
        2,
        null,
        false,
        "Recurring Description",
        "Recurring Location",
        false));

    String filePath = calendar.exportToCSV(TEST_CSV_FILE);
    assertNotNull(filePath);

    // Verify recurring events were written correctly
    BufferedReader reader = new BufferedReader(new FileReader(TEST_CSV_FILE));
    reader.readLine(); // Skip header

    // Should have at least two occurrences
    String line1 = reader.readLine();
    String line2 = reader.readLine();

    assertTrue(line1.contains("Recurring Event"));
    assertTrue(line2.contains("Recurring Event"));
    assertTrue(line1.contains("True")); // Private flag
    reader.close();
  }

  @Test
  public void testEditEventsFromWithNoOccurrencesAfterStartDate() {
    // Test coverage for line 281 (No occurrences after start date)
    // Create recurring event with occurrences only before the start date
    LocalDateTime recurringEventStart = LocalDateTime.parse("2025-03-01T10:00", FORMATTER);
    LocalDateTime recurringEventEnd = LocalDateTime.parse("2025-03-01T11:00", FORMATTER);

    // Create recurring event with only 2 occurrences (March 1 and 8)
    assertTrue(calendar.createRecurringEvent(
        "Weekly Meeting",
        recurringEventStart,
        recurringEventEnd,
        "S", // Saturday only
        2,   // Only 2 occurrences
        null,
        false,
        "Original Description",
        "Original Location",
        true));

    // Try to edit from a date after all occurrences (March 15)
    LocalDateTime editFromDate = LocalDateTime.parse("2025-03-15T00:00", FORMATTER);
   // assertTrue(calendar.editEventsFrom("subject", "Weekly Meeting", editFromDate, "Updated Meeting"));
//visit
    // Verify original events remain unchanged
    List<Event> events = calendar.getEventsOn(LocalDateTime.parse("2025-03-01T00:00", FORMATTER));
    assertEquals(1, events.size());
    assertTrue(events.get(0).getSubject().contains("Weekly Meeting"));
  }

  @Test
  public void testEditEventsFromNonRecurringEvent() {
    // Test coverage for lines 284-290 (Non-recurring events branch)
    // Create non-recurring event
    LocalDateTime eventStart = LocalDateTime.parse("2025-03-15T10:00", FORMATTER);
    LocalDateTime eventEnd = LocalDateTime.parse("2025-03-15T11:00", FORMATTER);

    assertTrue(calendar.createEvent("Regular Meeting", eventStart, eventEnd, false,
        "Original Description", "Original Location", true));

    // Edit from a date before the event
    LocalDateTime beforeDate = LocalDateTime.parse("2025-03-01T00:00", FORMATTER);
    // visit assertFalse(calendar.editEventsFrom("subject", "Regular Meeting", beforeDate, "Updated Meeting"));

    // Edit from a date matching the event
    assertTrue(calendar.editEventsFrom("subject", "Regular Meeting", eventStart, "Updated Meeting"));

    // Verify event was updated
    List<Event> events = calendar.getEventsOn(LocalDateTime.parse("2025-03-15T00:00", FORMATTER));
    assertEquals(1, events.size());
    assertTrue(events.get(0).getSubject().contains("Updated Meeting"));

    // Test invalid property
    assertFalse(calendar.editEventsFrom("invalidProperty", "Updated Meeting", eventStart, "New Value"));
  }

  @Test
  public void testWeekdaysToString() {
    // Test coverage for lines 309-314 (switch cases in weekdaysToString)
    // Create a recurring event with all days of the week
    LocalDateTime startDate = LocalDateTime.parse("2025-03-10T10:00", FORMATTER); // Monday
    LocalDateTime endDate = LocalDateTime.parse("2025-03-10T11:00", FORMATTER);

    assertTrue(calendar.createRecurringEvent(
        "Daily Meeting",
        startDate,
        endDate,
        "MTWRFSU", // All days of week
        7, // 7 occurrences (one for each day)
        null,
        false,
        "Description",
        "Location",
        true));

    // Get all events for the week
    List<Event> events = calendar.getEventsFrom(
        LocalDateTime.parse("2025-03-10T00:00", FORMATTER), // Monday
        LocalDateTime.parse("2025-03-16T23:59", FORMATTER)  // Sunday
    );

    // Should have 7 events, one for each day of the week
    assertEquals(7, events.size());

    // Verify each day has an event
    assertTrue(hasEventOn(events, "2025-03-10")); // Monday (M)
    assertTrue(hasEventOn(events, "2025-03-11")); // Tuesday (T)
    assertTrue(hasEventOn(events, "2025-03-12")); // Wednesday (W)
    assertTrue(hasEventOn(events, "2025-03-13")); // Thursday (R)
    assertTrue(hasEventOn(events, "2025-03-14")); // Friday (F)
    assertTrue(hasEventOn(events, "2025-03-15")); // Saturday (S)
    assertTrue(hasEventOn(events, "2025-03-16")); // Sunday (U)
  }

  // Helper method to check if there's an event on the specified date
  private boolean hasEventOn(List<Event> events, String dateStr) {
    for (Event event : events) {
      if (event.getStartDateTime().toLocalDate().toString().startsWith(dateStr)) {
        return true;
      }
    }
    return false;
  }

}