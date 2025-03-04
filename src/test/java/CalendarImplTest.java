
import model.Calendar;
import model.CalendarImpl;
import model.Event;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test cases for the CalendarImpl class.
 */
public class CalendarImplTest {
  private Calendar calendar;
  private LocalDateTime now;
  private LocalDateTime tomorrow;
  private LocalDateTime nextWeek;

  @Before
  public void setUp() {
    calendar = new CalendarImpl();
    now = LocalDateTime.of(2025, 3, 4, 10, 0);
    tomorrow = now.plusDays(1);
    nextWeek = now.plusDays(7);
  }

  @After
  public void tearDown() {
    // Clean up any exported files
    File exportFile = new File("test_export.csv");
    if (exportFile.exists()) {
      exportFile.delete();
    }
  }

  @Test
  public void testCreateEvent() {
    // Create a simple event
    boolean result = calendar.createEvent("Meeting", now, now.plusHours(1), false);
    assertTrue("Event creation should succeed", result);

    // Check if the event exists
    List<Event> events = calendar.getEventsOn(now);
    assertEquals("Calendar should have 1 event", 1, events.size());
    assertEquals("Event subject should match", "Meeting", events.get(0).getSubject());
  }

  @Test
  public void testCreateEventWithConflict() {
    // Create first event
    calendar.createEvent("Meeting", now, now.plusHours(1), false);

    // Create conflicting event with autoDecline = true
    boolean result = calendar.createEvent("Conflict", now.plusMinutes(30), now.plusHours(1).plusMinutes(30), true);
    assertFalse("Conflicting event with autoDecline should be rejected", result);

    // Create conflicting event with autoDecline = false
    result = calendar.createEvent("Conflict", now.plusMinutes(30), now.plusHours(1).plusMinutes(30), false);
    assertTrue("Conflicting event without autoDecline should be accepted", result);

    // Should now have 2 events
    List<Event> events = calendar.getEventsOn(now);
    assertEquals("Calendar should have 2 events", 2, events.size());
  }

  @Test
  public void testCreateAllDayEvent() {
    boolean result = calendar.createAllDayEvent("All Day Meeting", now, false);
    assertTrue("All day event creation should succeed", result);

    List<Event> events = calendar.getEventsOn(now);
    assertEquals("Calendar should have 1 event", 1, events.size());
    assertEquals("Event subject should match", "All Day Meeting", events.get(0).getSubject());
    assertTrue("Event should be all-day", events.get(0).isAllDay());
  }

  @Test
  public void testCreateAllDayEventWithConflict() {
    // Create first all-day event
    calendar.createAllDayEvent("All Day Meeting", now, false);

    // Create conflicting all-day event with autoDecline = true
    boolean result = calendar.createAllDayEvent("Conflict", now, true);
    assertFalse("Conflicting all-day event with autoDecline should be rejected", result);
  }

  @Test
  public void testCreateRecurringEvent() {
    // Create recurring event, repeating on Monday and Wednesday for 3 times
    boolean result = calendar.createRecurringEvent(
        "Weekly Meeting",
        now,
        now.plusHours(1),
        "MW",
        3,
        null,
        false
    );
    assertTrue("Recurring event creation should succeed", result);

    // Check if events are created on appropriate days
    List<Event> events = calendar.getEventsFrom(now, now.plusDays(14));
    assertTrue("Should have events in the date range", events.size() > 0);
  }

  @Test
  public void testCreateRecurringEventWithConflict() {
    // Create a regular event
    calendar.createEvent("Regular Meeting", now, now.plusHours(1), false);

    // Create conflicting recurring event with autoDecline = true
    boolean result = calendar.createRecurringEvent(
        "Weekly Meeting",
        now.minusMinutes(30),
        now.plusMinutes(30),
        "MW",
        3,
        null,
        true
    );
    assertFalse("Conflicting recurring event with autoDecline should be rejected", result);
  }

//  @Test
//  public void testCreateRecurringAllDayEvent() {
//    // Create recurring all-day event
//    boolean result = calendar.createRecurringAllDayEvent(
//        "Weekly All Day",
//        now,
//        "TR",  // Tuesday and Thursday
//        null,
//        now.plusDays(14),
//        false
//    );
//    assertTrue("Recurring all-day event creation should succeed", result);
//  }

  @Test
  public void testEditEvent() {
    // Create an event
    calendar.createEvent("Old Name", now, now.plusHours(1), false);

    // Edit the event name
    boolean result = calendar.editEvent("name", "Old Name", now, now.plusHours(1), "New Name");
    assertTrue("Event edit should succeed", result);

    // Check if the name was updated
    List<Event> events = calendar.getEventsOn(now);
    assertEquals("Event name should be updated", "New Name", events.get(0).getSubject());
  }

  @Test
  public void testEditEventsFrom() {
    // Create recurring events
    calendar.createRecurringEvent(
        "Recurring Meeting",
        now,
        now.plusHours(1),
        "MWF",
        3,
        null,
        false
    );

    // Edit events from a specific date
    boolean result = calendar.editEventsFrom("location", "Recurring Meeting", tomorrow, "Room 101");
    assertTrue("Edit events from should succeed", result);

    // Check if events were updated
    List<Event> events = calendar.getEventsFrom(tomorrow, nextWeek);
    for (Event event : events) {
      if (event.getSubject().equals("Recurring Meeting")) {
        assertEquals("Location should be updated", "Room 101", event.getLocation());
      }
    }
  }

  @Test
  public void testEditAllEvents() {
    // Create multiple events with same name
    calendar.createEvent("Same Name", now, now.plusHours(1), false);
    calendar.createEvent("Same Name", tomorrow, tomorrow.plusHours(1), false);

    // Edit all events with that name
    boolean result = calendar.editAllEvents("description", "Same Name", "Updated Description");
    assertTrue("Edit all events should succeed", result);

    // Check if all events were updated
    List<Event> eventsToday = calendar.getEventsOn(now);
    List<Event> eventsTomorrow = calendar.getEventsOn(tomorrow);

    for (Event event : eventsToday) {
      if (event.getSubject().equals("Same Name")) {
        assertEquals("Description should be updated", "Updated Description", event.getDescription());
      }
    }

    for (Event event : eventsTomorrow) {
      if (event.getSubject().equals("Same Name")) {
        assertEquals("Description should be updated", "Updated Description", event.getDescription());
      }
    }
  }

  @Test
  public void testGetEventsOn() {
    // Create events on different days
    calendar.createEvent("Today Event", now, now.plusHours(1), false);
    calendar.createEvent("Tomorrow Event", tomorrow, tomorrow.plusHours(1), false);

    // Get events on today
    List<Event> eventsToday = calendar.getEventsOn(now);
    assertEquals("Should have 1 event today", 1, eventsToday.size());
    assertEquals("Today's event should match", "Today Event", eventsToday.get(0).getSubject());

    // Get events on tomorrow
    List<Event> eventsTomorrow = calendar.getEventsOn(tomorrow);
    assertEquals("Should have 1 event tomorrow", 1, eventsTomorrow.size());
    assertEquals("Tomorrow's event should match", "Tomorrow Event", eventsTomorrow.get(0).getSubject());
  }

  @Test
  public void testGetEventsFrom() {
    // Create events on different days
    calendar.createEvent("Today Event", now, now.plusHours(1), false);
    calendar.createEvent("Tomorrow Event", tomorrow, tomorrow.plusHours(1), false);

    // Get events in a date range
    List<Event> events = calendar.getEventsFrom(now, nextWeek);
    assertEquals("Should have 2 events in range", 2, events.size());
  }

  @Test
  public void testIsBusy() {
    // Initially not busy
    assertFalse("Should not be busy initially", calendar.isBusy(now));

    // Create an event
    calendar.createEvent("Meeting", now, now.plusHours(1), false);

    // Should be busy during the event
    assertTrue("Should be busy during event", calendar.isBusy(now));
    assertTrue("Should be busy during event", calendar.isBusy(now.plusMinutes(30)));
    assertFalse("Should not be busy after event", calendar.isBusy(now.plusHours(2)));
  }

  @Test
  public void testExportToCSV() {
    // Create some events
    calendar.createEvent("Regular Meeting", now, now.plusHours(1), false);
    calendar.createAllDayEvent("All Day Event", tomorrow, false);

    // Export to CSV
    String filePath = calendar.exportToCSV("test_export.csv");
    assertNotNull("Export should return a file path", filePath);

    // Verify file exists
    File exportFile = new File(filePath);
    assertTrue("Export file should exist", exportFile.exists());
  }
}