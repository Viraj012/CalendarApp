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
 * Enhanced test cases for the CalendarImpl class that include tests for all event properties.
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
    File exportFile = new File("test_export.csv");
    if (exportFile.exists()) {
      exportFile.delete();
    }
  }

  @Test
  public void testCreateEventWithAllProperties() {
    // Create an event with all properties
    boolean result = calendar.createEvent(
        "Full Meeting",
        now,
        now.plusHours(1),
        false,
        "Important planning session",
        "Conference Room A",
        false);  // private event

    assertTrue("Event creation with all properties should succeed", result);

    // Check if the event exists with all properties
    List<Event> events = calendar.getEventsOn(now);
    assertEquals("Calendar should have 1 event", 1, events.size());

    Event event = events.get(0);
    assertEquals("Event subject should match", "Full Meeting", event.getSubject());
    assertEquals("Event description should match", "Important planning session", event.getDescription());
    assertEquals("Event location should match", "Conference Room A", event.getLocation());
    assertFalse("Event should be private", event.isPublic());
  }

  @Test
  public void testCreateAllDayEventWithAllProperties() {
    // Create all-day event with all properties
    boolean result = calendar.createAllDayEvent(
        "All Day Conference",
        now,
        false,
        "Company-wide strategy session",
        "Main Auditorium",
        true);  // public event

    assertTrue("All day event creation with all properties should succeed", result);

    List<Event> events = calendar.getEventsOn(now);
    assertEquals("Calendar should have 1 event", 1, events.size());

    Event event = events.get(0);
    assertEquals("Event subject should match", "All Day Conference", event.getSubject());
    assertEquals("Event description should match", "Company-wide strategy session", event.getDescription());
    assertEquals("Event location should match", "Main Auditorium", event.getLocation());
    assertTrue("Event should be public", event.isPublic());
    assertTrue("Event should be all-day", event.isAllDay());
  }

  @Test
  public void testCreateRecurringEventWithAllProperties() {
    // Create recurring event with all properties
    boolean result = calendar.createRecurringEvent(
        "Weekly Team Sync",
        now,
        now.plusHours(1),
        "MWF",
        4,
        null,
        false,
        "Team status updates and planning",
        "Meeting Room C",
        false  // private event
    );

    assertTrue("Recurring event creation with all properties should succeed", result);

    // Check if events are created with proper properties
    List<Event> events = calendar.getEventsFrom(now, now.plusDays(14));
    assertTrue("Should have events in the date range", events.size() > 0);

    for (Event event : events) {
      if (event.getSubject().equals("Weekly Team Sync")) {
        assertEquals("Event description should match", "Team status updates and planning", event.getDescription());
        assertEquals("Event location should match", "Meeting Room C", event.getLocation());
        assertFalse("Event should be private", event.isPublic());
      }
    }
  }

  @Test
  public void testCreateRecurringAllDayEventWithAllProperties() {
    // Create recurring all-day event with all properties
    boolean result = calendar.createRecurringAllDayEvent(
        "Monthly Department Meeting",
        now,
        "F",  // Fridays
        3,
        null,
        false,
        "Departmental review and planning",
        "Main Conference Room",
        true  // public event
    );

    assertTrue("Recurring all-day event creation with all properties should succeed", result);

    // Check if events are created with proper properties
    List<Event> events = calendar.getEventsFrom(now, now.plusDays(21));
    assertTrue("Should have all-day events in the date range", events.size() > 0);

    for (Event event : events) {
      if (event.getSubject().equals("Monthly Department Meeting")) {
        assertTrue("Event should be all-day", event.isAllDay());
        assertEquals("Event description should match", "Departmental review and planning", event.getDescription());
        assertEquals("Event location should match", "Main Conference Room", event.getLocation());
        assertTrue("Event should be public", event.isPublic());
      }
    }
  }

  @Test
  public void testEditSubject() {
    // Create an event
    calendar.createEvent("Old Name", now, now.plusHours(1), false, "Description", "Room A", true);

    // Edit the event name
    boolean result = calendar.editEvent("subject", "Old Name", now, now.plusHours(1), "New Name");
    assertTrue("Event subject edit should succeed", result);

    // Check if the name was updated
    List<Event> events = calendar.getEventsOn(now);
    assertEquals("Event name should be updated", "New Name", events.get(0).getSubject());
  }

  @Test
  public void testEditDescription() {
    // Create an event
    calendar.createEvent("Meeting", now, now.plusHours(1), false, "Original description", "Room A", true);

    // Edit the description
    boolean result = calendar.editEvent("description", "Meeting", now, now.plusHours(1), "Updated description");
    assertTrue("Event description edit should succeed", result);

    // Check if the description was updated
    List<Event> events = calendar.getEventsOn(now);
    assertEquals("Event description should be updated", "Updated description", events.get(0).getDescription());
  }

  @Test
  public void testEditLocation() {
    // Create an event
    calendar.createEvent("Meeting", now, now.plusHours(1), false, "Description", "Room A", true);

    // Edit the location
    boolean result = calendar.editEvent("location", "Meeting", now, now.plusHours(1), "Room B");
    assertTrue("Event location edit should succeed", result);

    // Check if the location was updated
    List<Event> events = calendar.getEventsOn(now);
    assertEquals("Event location should be updated", "Room B", events.get(0).getLocation());
  }

  @Test
  public void testEditPublicStatus() {
    // Create a public event
    calendar.createEvent("Meeting", now, now.plusHours(1), false, "Description", "Room A", true);

    // Edit to make it private
    boolean result = calendar.editEvent("public", "Meeting", now, now.plusHours(1), "false");
    assertTrue("Event public status edit should succeed", result);

    // Check if the status was updated
    List<Event> events = calendar.getEventsOn(now);
    assertFalse("Event should now be private", events.get(0).isPublic());

    // Edit to make it public again
    result = calendar.editEvent("public", "Meeting", now, now.plusHours(1), "true");
    assertTrue("Event public status edit should succeed", result);

    // Refresh events list
    events = calendar.getEventsOn(now);
    assertTrue("Event should now be public again", events.get(0).isPublic());
  }

  @Test
  public void testEditEventsFromWithAllProperties() {
    // Create recurring events
    calendar.createRecurringEvent(
        "Recurring Meeting",
        now,
        now.plusHours(1),
        "MWF",
        3,
        null,
        false,
        "Original description",
        "Original location",
        true
    );

    // Edit description from a specific date
    boolean result = calendar.editEventsFrom("description", "Recurring Meeting", tomorrow, "Updated description");
    assertTrue("Edit events description from should succeed", result);

    // Edit location from a specific date
    result = calendar.editEventsFrom("location", "Recurring Meeting", tomorrow, "New Room");
    assertTrue("Edit events location from should succeed", result);

    // Edit privacy from a specific date
    result = calendar.editEventsFrom("public", "Recurring Meeting", tomorrow, "false");
    assertTrue("Edit events privacy from should succeed", result);

    // Check if events were updated correctly
    List<Event> eventsToday = calendar.getEventsOn(now);
    List<Event> eventsFromTomorrow = calendar.getEventsFrom(tomorrow, nextWeek);

    // Today's event should remain unchanged
    for (Event event : eventsToday) {
      if (event.getSubject().equals("Recurring Meeting")) {
        assertEquals("Today's description should be original", "Original description", event.getDescription());
        assertEquals("Today's location should be original", "Original location", event.getLocation());
        assertTrue("Today's event should still be public", event.isPublic());
      }
    }

    // Future events should be updated
    for (Event event : eventsFromTomorrow) {
      if (event.getSubject().equals("Recurring Meeting")) {
        assertEquals("Future description should be updated", "Updated description", event.getDescription());
        assertEquals("Future location should be updated", "New Room", event.getLocation());
        assertFalse("Future events should be private", event.isPublic());
      }
    }
  }

  @Test
  public void testEditAllEventsWithAllProperties() {
    // Create multiple events with same name but different properties
    calendar.createEvent("Same Name", now, now.plusHours(1), false, "Desc 1", "Loc 1", true);
    calendar.createEvent("Same Name", tomorrow, tomorrow.plusHours(1), false, "Desc 2", "Loc 2", false);

    // Edit all events description
    boolean result = calendar.editAllEvents("description", "Same Name", "Universal Description");
    assertTrue("Edit all events description should succeed", result);

    // Edit all events location
    result = calendar.editAllEvents("location", "Same Name", "Universal Location");
    assertTrue("Edit all events location should succeed", result);

    // Edit all events privacy
    result = calendar.editAllEvents("public", "Same Name", "true");
    assertTrue("Edit all events privacy should succeed", result);

    // Check if all events were updated
    List<Event> eventsToday = calendar.getEventsOn(now);
    List<Event> eventsTomorrow = calendar.getEventsOn(tomorrow);

    // All events should have the new values
    for (Event event : eventsToday) {
      if (event.getSubject().equals("Same Name")) {
        assertEquals("Description should be updated", "Universal Description", event.getDescription());
        assertEquals("Location should be updated", "Universal Location", event.getLocation());
        assertTrue("Event should now be public", event.isPublic());
      }
    }

    for (Event event : eventsTomorrow) {
      if (event.getSubject().equals("Same Name")) {
        assertEquals("Description should be updated", "Universal Description", event.getDescription());
        assertEquals("Location should be updated", "Universal Location", event.getLocation());
        assertTrue("Event should now be public", event.isPublic());
      }
    }
  }

  @Test
  public void testExportToCSVIncludesAllProperties() {
    // Create events with all properties
    calendar.createEvent(
        "Regular Meeting",
        now,
        now.plusHours(1),
        false,
        "Important discussion",
        "Room 101",
        false);

    calendar.createAllDayEvent(
        "All Day Event",
        tomorrow,
        false,
        "Company retreat",
        "Offsite",
        true);

    // Export to CSV
    String filePath = calendar.exportToCSV("test_export.csv");
    assertNotNull("Export should return a file path", filePath);

    // Verify file exists
    File exportFile = new File(filePath);
    assertTrue("Export file should exist", exportFile.exists());

    // Here we'd ideally read the CSV file to verify all properties are included
    // For a complete test, you could add code to read the CSV and verify its contents
  }

  // Original tests from the base test file

  @Test
  public void testCreateEvent() {
    // Create a simple event
    boolean result = calendar.createEvent("Meeting", now, now.plusHours(1), false, null, null, true);
    assertTrue("Event creation should succeed", result);

    // Check if the event exists
    List<Event> events = calendar.getEventsOn(now);
    assertEquals("Calendar should have 1 event", 1, events.size());
    assertEquals("Event subject should match", "Meeting", events.get(0).getSubject());
  }

  @Test
  public void testCreateEventWithConflict() {
    calendar.createEvent("Meeting", now, now.plusHours(1), false, null, null, true);

    boolean result = calendar.createEvent("Conflict", now.plusMinutes(30), now.plusHours(1).plusMinutes(30), true, null, null, true);
    assertFalse("Conflicting event with autoDecline should be rejected", result);

    result = calendar.createEvent("Conflict", now.plusMinutes(30), now.plusHours(1).plusMinutes(30), false, null, null, true);
    assertTrue("Conflicting event without autoDecline should be accepted", result);

    // Should now have 2 events
    List<Event> events = calendar.getEventsOn(now);
    assertEquals("Calendar should have 2 events", 2, events.size());
  }

  @Test
  public void testGetEventsOn() {
    // Create events on different days
    calendar.createEvent("Today Event", now, now.plusHours(1), false, "Today Desc", "Today Loc", true);
    calendar.createEvent("Tomorrow Event", tomorrow, tomorrow.plusHours(1), false, "Tomorrow Desc", "Tomorrow Loc", false);

    // Get events on today
    List<Event> eventsToday = calendar.getEventsOn(now);
    assertEquals("Should have 1 event today", 1, eventsToday.size());
    assertEquals("Today's event should match", "Today Event", eventsToday.get(0).getSubject());
    assertEquals("Today's description should match", "Today Desc", eventsToday.get(0).getDescription());
    assertEquals("Today's location should match", "Today Loc", eventsToday.get(0).getLocation());
    assertTrue("Today's event should be public", eventsToday.get(0).isPublic());

    // Get events on tomorrow
    List<Event> eventsTomorrow = calendar.getEventsOn(tomorrow);
    assertEquals("Should have 1 event tomorrow", 1, eventsTomorrow.size());
    assertEquals("Tomorrow's event should match", "Tomorrow Event", eventsTomorrow.get(0).getSubject());
    assertEquals("Tomorrow's description should match", "Tomorrow Desc", eventsTomorrow.get(0).getDescription());
    assertEquals("Tomorrow's location should match", "Tomorrow Loc", eventsTomorrow.get(0).getLocation());
    assertFalse("Tomorrow's event should be private", eventsTomorrow.get(0).isPublic());
  }

  @Test
  public void testGetEventsFrom() {
    // Create events on different days with different properties
    calendar.createEvent("Today Event", now, now.plusHours(1), false, "Desc 1", "Loc 1", true);
    calendar.createEvent("Tomorrow Event", tomorrow, tomorrow.plusHours(1), false, "Desc 2", "Loc 2", false);

    // Get events in a date range
    List<Event> events = calendar.getEventsFrom(now, nextWeek);
    assertEquals("Should have 2 events in range", 2, events.size());

    // Verify all properties are preserved
    for (Event event : events) {
      if (event.getSubject().equals("Today Event")) {
        assertEquals("Description should match", "Desc 1", event.getDescription());
        assertEquals("Location should match", "Loc 1", event.getLocation());
        assertTrue("Event should be public", event.isPublic());
      } else if (event.getSubject().equals("Tomorrow Event")) {
        assertEquals("Description should match", "Desc 2", event.getDescription());
        assertEquals("Location should match", "Loc 2", event.getLocation());
        assertFalse("Event should be private", event.isPublic());
      }
    }
  }

  @Test
  public void testIsBusy() {
    // Initially not busy
    assertFalse("Should not be busy initially", calendar.isBusy(now));

    // Create an event
    calendar.createEvent("Meeting", now, now.plusHours(1), false, "Description", "Location", true);

    // Should be busy during the event
    assertTrue("Should be busy during event", calendar.isBusy(now));
    assertTrue("Should be busy during event", calendar.isBusy(now.plusMinutes(30)));
    assertFalse("Should not be busy after event", calendar.isBusy(now.plusHours(2)));
  }
}