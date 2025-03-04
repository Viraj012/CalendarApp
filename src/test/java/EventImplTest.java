
import model.Event;
import model.EventImpl;
import model.RecurrencePattern;
import org.junit.Before;
import org.junit.Test;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * Test cases for the EventImpl class.
 */
public class EventImplTest {
  private LocalDateTime now;
  private LocalDateTime oneHourLater;
  private LocalDateTime tomorrow;

  @Before
  public void setUp() {
    now = LocalDateTime.of(2025, 3, 4, 10, 0);
    oneHourLater = now.plusHours(1);
    tomorrow = now.plusDays(1);
  }

  @Test
  public void testCreateRegularEvent() {
    Event event = new EventImpl("Regular Event", now, oneHourLater);

    assertEquals("Subject should match", "Regular Event", event.getSubject());
    assertEquals("Start time should match", now, event.getStartDateTime());
    assertEquals("End time should match", oneHourLater, event.getEndDateTime());
    assertFalse("Should not be all-day", event.isAllDay());
    assertFalse("Should not be recurring", event.isRecurring());
    assertTrue("Should be public by default", event.isPublic());
    assertEquals("Description should be empty", "", event.getDescription());
    assertEquals("Location should be empty", "", event.getLocation());
  }

  @Test
  public void testCreateAllDayEvent() {
    Event event = new EventImpl("All Day Event", now);

    assertEquals("Subject should match", "All Day Event", event.getSubject());
    assertEquals("Start time should match", now, event.getStartDateTime());
    assertNull("End time should be null for all-day events", event.getEndDateTime());
    assertTrue("Should be all-day", event.isAllDay());
    assertFalse("Should not be recurring", event.isRecurring());
  }

  @Test
  public void testCreateRecurringEvent() {
    Event event = new EventImpl("Recurring Event", now, oneHourLater, "MWF", 3, null);

    assertEquals("Subject should match", "Recurring Event", event.getSubject());
    assertTrue("Should be recurring", event.isRecurring());
    assertFalse("Should not be all-day", event.isAllDay());

    // Check that we can get the recurrence pattern
    RecurrencePattern pattern = ((EventImpl)event).getRecurrence();
    assertNotNull("Recurrence pattern should not be null", pattern);
  }

  @Test
  public void testCreateRecurringAllDayEvent() {
    Event event = new EventImpl("Recurring All Day", now, "TR", 4, null);

    assertEquals("Subject should match", "Recurring All Day", event.getSubject());
    assertTrue("Should be recurring", event.isRecurring());
    assertTrue("Should be all-day", event.isAllDay());

    // Check that we can get the recurrence pattern
    RecurrencePattern pattern = ((EventImpl)event).getRecurrence();
    assertNotNull("Recurrence pattern should not be null", pattern);
  }

  @Test
  public void testSetSubject() {
    EventImpl event = new EventImpl("Old Name", now, oneHourLater);
    event.setSubject("New Name");

    assertEquals("Subject should be updated", "New Name", event.getSubject());
  }

  @Test
  public void testSetDescription() {
    EventImpl event = new EventImpl("Event", now, oneHourLater);
    event.setDescription("Test Description");

    assertEquals("Description should be updated", "Test Description", event.getDescription());
  }

  @Test
  public void testSetLocation() {
    EventImpl event = new EventImpl("Event", now, oneHourLater);
    event.setLocation("Room 101");

    assertEquals("Location should be updated", "Room 101", event.getLocation());
  }

  @Test
  public void testSetPublic() {
    EventImpl event = new EventImpl("Event", now, oneHourLater);
    // Events are public by default
    assertTrue("Event should be public by default", event.isPublic());

    event.setPublic(false);
    assertFalse("Event should now be private", event.isPublic());
  }

  @Test
  public void testConflictsWith() {
    // Create two overlapping events
    Event event1 = new EventImpl("Event 1", now, oneHourLater);
    Event event2 = new EventImpl("Event 2", now.plusMinutes(30), oneHourLater.plusMinutes(30));

    assertTrue("Events should conflict", event1.conflictsWith(event2));
    assertTrue("Conflict should be symmetric", event2.conflictsWith(event1));

    // Create non-overlapping events
    Event event3 = new EventImpl("Event 3", oneHourLater.plusMinutes(5), oneHourLater.plusHours(1));

    assertFalse("Events should not conflict", event1.conflictsWith(event3));
    assertFalse("Non-conflict should be symmetric", event3.conflictsWith(event1));
  }

  @Test
  public void testAllDayEventConflict() {
    // Create an all-day event
    Event allDayEvent = new EventImpl("All Day", now);

    // Create a regular event on the same day
    Event regularEvent = new EventImpl("Regular", now, oneHourLater);

    assertTrue("All-day event should conflict with regular events on same day",
        allDayEvent.conflictsWith(regularEvent));
    assertTrue("Regular event should conflict with all-day event on same day",
        regularEvent.conflictsWith(allDayEvent));

    // Create a regular event on a different day
    Event tomorrowEvent = new EventImpl("Tomorrow", tomorrow, tomorrow.plusHours(1));

    assertFalse("All-day event should not conflict with events on different days",
        allDayEvent.conflictsWith(tomorrowEvent));
  }

  @Test
  public void testEquals() {
    Event event1 = new EventImpl("Same Event", now, oneHourLater);
    Event event2 = new EventImpl("Same Event", now, oneHourLater);
    Event event3 = new EventImpl("Different Event", now, oneHourLater);
    Event event4 = new EventImpl("Same Event", tomorrow, tomorrow.plusHours(1));

    assertTrue("Events with same subject and start time should be equal", event1.equals(event2));
    assertFalse("Events with different subjects should not be equal", event1.equals(event3));
    assertFalse("Events with different start times should not be equal", event1.equals(event4));
  }

  @Test
  public void testHashCode() {
    Event event1 = new EventImpl("Same Event", now, oneHourLater);
    Event event2 = new EventImpl("Same Event", now, oneHourLater);

    assertEquals("Equal events should have same hash code", event1.hashCode(), event2.hashCode());
  }

  @Test
  public void testToString() {
    EventImpl event = new EventImpl("Test Event", now, oneHourLater);
    event.setLocation("Room 101");

    String eventString = event.toString();

    assertTrue("toString should contain event subject", eventString.contains("Test Event"));
    assertTrue("toString should contain event time", eventString.contains(now.toLocalTime().toString()));
    assertTrue("toString should contain location", eventString.contains("Room 101"));

    // Test all-day event toString
    Event allDayEvent = new EventImpl("All Day", now);
    String allDayString = allDayEvent.toString();

    assertTrue("All-day event toString should contain 'All Day'", allDayString.contains("All Day"));
  }
}