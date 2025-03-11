
import model.EventImpl;
import model.RecurrencePattern;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Enhanced test class for EventImpl.
 */
public class EventTest {
  private EventImpl singleEvent;
  private EventImpl allDayEvent;
  private EventImpl recurringEvent;
  private EventImpl recurringAllDayEvent;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;
  private LocalDateTime untilDate;

  @Before
  public void setUp() {
    startDateTime = LocalDateTime.of(2025, 3, 10, 14, 30);
    endDateTime = LocalDateTime.of(2025, 3, 10, 16, 0);
    untilDate = LocalDateTime.of(2025, 4, 10, 0, 0);

    singleEvent = new EventImpl("Team Meeting", startDateTime, endDateTime);
    allDayEvent = new EventImpl("Conference Day", startDateTime);
    recurringEvent = new EventImpl("Weekly Status", startDateTime, endDateTime, "M", 4, null);
    recurringAllDayEvent = new EventImpl("Monthly Training", startDateTime, "F", 3, null);
  }

  @Test
  public void testSingleEventCreation() {
    assertEquals("Team Meeting", singleEvent.getSubject());
    assertEquals(startDateTime, singleEvent.getStartDateTime());
    assertEquals(endDateTime, singleEvent.getEndDateTime());
    assertFalse(singleEvent.isAllDay());
    assertFalse(singleEvent.isRecurring());
    assertTrue(singleEvent.isPublic());
    assertEquals("", singleEvent.getDescription());
    assertEquals("", singleEvent.getLocation());
  }

  @Test
  public void testAllDayEventCreation() {
    assertEquals("Conference Day", allDayEvent.getSubject());
    assertEquals(startDateTime, allDayEvent.getStartDateTime());
    assertNull(allDayEvent.getEndDateTime());
    assertTrue(allDayEvent.isAllDay());
    assertFalse(allDayEvent.isRecurring());
  }

  @Test
  public void testRecurringEventCreation() {
    assertEquals("Weekly Status", recurringEvent.getSubject());
    assertTrue(recurringEvent.isRecurring());
    assertFalse(recurringEvent.isAllDay());

    RecurrencePattern pattern = recurringEvent.getRecurrence();
    assertNotNull(pattern);
    assertEquals(4, pattern.getOccurrences());
    assertTrue(pattern.getWeekdays().contains(java.time.DayOfWeek.MONDAY));
  }

  @Test
  public void testRecurringAllDayEventCreation() {
    assertEquals("Monthly Training", recurringAllDayEvent.getSubject());
    assertTrue(recurringAllDayEvent.isRecurring());
    assertTrue(recurringAllDayEvent.isAllDay());

    RecurrencePattern pattern = recurringAllDayEvent.getRecurrence();
    assertNotNull(pattern);
    assertEquals(3, pattern.getOccurrences());
    assertTrue(pattern.getWeekdays().contains(java.time.DayOfWeek.FRIDAY));
  }

  @Test
  public void testRecurringEventWithUntilDate() {
    EventImpl event = new EventImpl("Meeting", startDateTime, endDateTime, "MWF", -1, untilDate);

    assertTrue(event.isRecurring());
    RecurrencePattern pattern = event.getRecurrence();
    assertEquals(-1, pattern.getOccurrences());
    assertEquals(untilDate, pattern.getUntilDate());

    Set<DayOfWeek> weekdays = pattern.getWeekdays();
    assertEquals(3, weekdays.size());
    assertTrue(weekdays.contains(DayOfWeek.MONDAY));
    assertTrue(weekdays.contains(DayOfWeek.WEDNESDAY));
    assertTrue(weekdays.contains(DayOfWeek.FRIDAY));
  }

  @Test
  public void testSetSubject() {
    // Test empty subject
    singleEvent.setSubject("");
    assertEquals("", singleEvent.getSubject());

    // Test null subject
    singleEvent.setSubject(null);
    assertNull(singleEvent.getSubject());

    // Test normal case
    singleEvent.setSubject("Updated Meeting");
    assertEquals("Updated Meeting", singleEvent.getSubject());
  }

  @Test
  public void testSetDescription() {
    // Test empty description
    singleEvent.setDescription("");
    assertEquals("", singleEvent.getDescription());

    // Test null description
    singleEvent.setDescription(null);
    assertNull(singleEvent.getDescription());

    // Test normal case
    singleEvent.setDescription("Discuss Q1 goals");
    assertEquals("Discuss Q1 goals", singleEvent.getDescription());

    // Test long description
    String longDesc = "This is a very long description that contains multiple sentences. " +
        "It should be properly stored and retrieved from the event object. " +
        "The description can contain special characters like: !@#$%^&*()_+";
    singleEvent.setDescription(longDesc);
    assertEquals(longDesc, singleEvent.getDescription());
  }

  @Test
  public void testSetLocation() {
    // Test empty location
    singleEvent.setLocation("");
    assertEquals("", singleEvent.getLocation());

    // Test null location
    singleEvent.setLocation(null);
    assertNull(singleEvent.getLocation());

    // Test normal case
    singleEvent.setLocation("Conference Room A");
    assertEquals("Conference Room A", singleEvent.getLocation());

    // Test with special characters
    singleEvent.setLocation("Room #123 - 5th Floor (Building B)");
    assertEquals("Room #123 - 5th Floor (Building B)", singleEvent.getLocation());
  }

  @Test
  public void testSetPublic() {
    // Test default
    assertTrue(singleEvent.isPublic());

    // Test setting to private
    singleEvent.setPublic(false);
    assertFalse(singleEvent.isPublic());

    // Test setting back to public
    singleEvent.setPublic(true);
    assertTrue(singleEvent.isPublic());
  }

  @Test
  public void testConflictsWithSameTimeRange() {
    // Create an event with exact same time as singleEvent
    EventImpl identicalEvent = new EventImpl("Identical", startDateTime, endDateTime);

    assertTrue(singleEvent.conflictsWith(identicalEvent));
    assertTrue(identicalEvent.conflictsWith(singleEvent));
  }

  @Test
  public void testConflictsWithOverlappingStart() {
    // Create an event that starts before and ends during singleEvent
    LocalDateTime overlapStartStart = startDateTime.minusHours(1);
    LocalDateTime overlapStartEnd = startDateTime.plusMinutes(30);
    EventImpl overlapStartEvent = new EventImpl("Overlap Start", overlapStartStart, overlapStartEnd);

    assertTrue(singleEvent.conflictsWith(overlapStartEvent));
    assertTrue(overlapStartEvent.conflictsWith(singleEvent));
  }

  @Test
  public void testConflictsWithOverlappingEnd() {
    // Create an event that starts during and ends after singleEvent
    LocalDateTime overlapEndStart = endDateTime.minusMinutes(30);
    LocalDateTime overlapEndEnd = endDateTime.plusHours(1);
    EventImpl overlapEndEvent = new EventImpl("Overlap End", overlapEndStart, overlapEndEnd);

    assertTrue(singleEvent.conflictsWith(overlapEndEvent));
    assertTrue(overlapEndEvent.conflictsWith(singleEvent));
  }

  @Test
  public void testConflictsWithContainedEvent() {
    // Create an event that is completely contained within singleEvent
    LocalDateTime containedStart = startDateTime.plusMinutes(15);
    LocalDateTime containedEnd = endDateTime.minusMinutes(15);
    EventImpl containedEvent = new EventImpl("Contained", containedStart, containedEnd);

    assertTrue(singleEvent.conflictsWith(containedEvent));
    assertTrue(containedEvent.conflictsWith(singleEvent));
  }

  @Test
  public void testConflictsWithContainingEvent() {
    // Create an event that completely contains singleEvent
    LocalDateTime containerStart = startDateTime.minusHours(1);
    LocalDateTime containerEnd = endDateTime.plusHours(1);
    EventImpl containerEvent = new EventImpl("Container", containerStart, containerEnd);

    assertTrue(singleEvent.conflictsWith(containerEvent));
    assertTrue(containerEvent.conflictsWith(singleEvent));
  }

//  @Test
//  public void testNoConflictBeforeEvent() {
//    // Create an event that ends exactly when singleEvent starts
//    LocalDateTime beforeStart = startDateTime.minusHours(2);
//    LocalDateTime beforeEnd = startDateTime;
//    EventImpl beforeEvent = new EventImpl("Before", beforeStart, beforeEnd);
//
//    assertFalse(singleEvent.conflictsWith(beforeEvent));
//    assertFalse(beforeEvent.conflictsWith(singleEvent));
//  }

//  @Test
//  public void testNoConflictAfterEvent() {
//    // Create an event that starts exactly when singleEvent ends
//    LocalDateTime afterStart = endDateTime;
//    LocalDateTime afterEnd = endDateTime.plusHours(2);
//    EventImpl afterEvent = new EventImpl("After", afterStart, afterEnd);
//
//    assertFalse(singleEvent.conflictsWith(afterEvent));
//    assertFalse(afterEvent.conflictsWith(singleEvent));
//  }

  @Test
  public void testConflictsWithAllDayEvent() {
    // Create a regular event on the same day as an all-day event
    LocalDateTime sameDayStart = startDateTime.withHour(9).withMinute(0);
    LocalDateTime sameDayEnd = startDateTime.withHour(10).withMinute(0);
    EventImpl sameDayEvent = new EventImpl("Same Day", sameDayStart, sameDayEnd);

    assertTrue(allDayEvent.conflictsWith(sameDayEvent));
    assertTrue(sameDayEvent.conflictsWith(allDayEvent));

    // Different day should not conflict
    LocalDateTime differentDayStart = startDateTime.plusDays(1).withHour(9).withMinute(0);
    LocalDateTime differentDayEnd = startDateTime.plusDays(1).withHour(10).withMinute(0);
    EventImpl differentDayEvent = new EventImpl("Different Day", differentDayStart, differentDayEnd);

    assertFalse(allDayEvent.conflictsWith(differentDayEvent));
    assertFalse(differentDayEvent.conflictsWith(allDayEvent));
  }

  @Test
  public void testAllDayEventsConflict() {
    // Two all-day events on the same day
    EventImpl anotherAllDayEvent = new EventImpl("Another All Day", startDateTime);

    assertTrue(allDayEvent.conflictsWith(anotherAllDayEvent));
    assertTrue(anotherAllDayEvent.conflictsWith(allDayEvent));

    // All-day events on different days
    EventImpl differentDayAllDay = new EventImpl("Different Day All Day", startDateTime.plusDays(1));

    assertFalse(allDayEvent.conflictsWith(differentDayAllDay));
    assertFalse(differentDayAllDay.conflictsWith(allDayEvent));
  }

  @Test
  public void testToString() {
    String eventString = singleEvent.toString();

    // Check basic components
    assertTrue(eventString.contains("Team Meeting"));
    assertTrue(eventString.contains(startDateTime.toLocalDate().toString()));
    assertTrue(eventString.contains("14:30"));
    assertTrue(eventString.contains("16:00"));

    // Check all-day event string
    String allDayString = allDayEvent.toString();
    assertTrue(allDayString.contains("Conference Day"));
    assertTrue(allDayString.contains("All Day"));

    // Check recurring event string
    String recurringString = recurringEvent.toString();
    assertTrue(recurringString.contains("Weekly Status"));
    assertTrue(recurringString.contains("Repeats on"));
    assertTrue(recurringString.contains("Mon"));

    // Event with location
    singleEvent.setLocation("Meeting Room A");
    eventString = singleEvent.toString();
    assertTrue(eventString.contains("at Meeting Room A"));
  }

  @Test
  public void testMultiDayEventToString() {
    // Create a multi-day event
    LocalDateTime multiDayStart = startDateTime;
    LocalDateTime multiDayEnd = startDateTime.plusDays(2).withHour(12).withMinute(0);
    EventImpl multiDayEvent = new EventImpl("Multi-day Conference", multiDayStart, multiDayEnd);

    String eventString = multiDayEvent.toString();
    assertTrue(eventString.contains("Multi-day Conference"));
    assertTrue(eventString.contains(multiDayStart.toLocalDate().toString()));
    assertTrue(eventString.contains(multiDayEnd.toLocalDate().toString()));
    assertTrue(eventString.contains("14:30"));
    assertTrue(eventString.contains("12:00"));
  }

  @Test
  public void testEqualsAndHashCode() {
    // Same subject and start time should be equal
    EventImpl duplicateEvent = new EventImpl("Team Meeting", startDateTime, endDateTime);
    assertEquals(singleEvent, duplicateEvent);
    assertEquals(singleEvent.hashCode(), duplicateEvent.hashCode());

    // Different subject
    EventImpl differentSubjectEvent = new EventImpl("Different Meeting", startDateTime, endDateTime);
    assertNotEquals(singleEvent, differentSubjectEvent);
    assertNotEquals(singleEvent.hashCode(), differentSubjectEvent.hashCode());

    // Different start time
    LocalDateTime differentStart = startDateTime.plusHours(1);
    EventImpl differentStartEvent = new EventImpl("Team Meeting", differentStart, endDateTime);
    assertNotEquals(singleEvent, differentStartEvent);
    assertNotEquals(singleEvent.hashCode(), differentStartEvent.hashCode());

    // Different end time shouldn't affect equality
    LocalDateTime differentEnd = endDateTime.plusHours(1);
    EventImpl differentEndEvent = new EventImpl("Team Meeting", startDateTime, differentEnd);
    assertEquals(singleEvent, differentEndEvent);
    assertEquals(singleEvent.hashCode(), differentEndEvent.hashCode());

    // Different type
    assertNotEquals(singleEvent, "Not an event");

    // Null comparison
    assertNotEquals(singleEvent, null);

    // Self comparison
    assertEquals(singleEvent, singleEvent);
  }

  @Test
  public void testRecurringEventWithDifferentWeekdays() {
    // Test multiple weekdays
    EventImpl multiDayEvent = new EventImpl("Multi-day Recurring", startDateTime, endDateTime, "MWF", 4, null);
    RecurrencePattern pattern = multiDayEvent.getRecurrence();
    Set<DayOfWeek> weekdays = pattern.getWeekdays();

    assertEquals(3, weekdays.size());
    assertTrue(weekdays.contains(DayOfWeek.MONDAY));
    assertTrue(weekdays.contains(DayOfWeek.WEDNESDAY));
    assertTrue(weekdays.contains(DayOfWeek.FRIDAY));

    // Test another combination
    EventImpl weekendEvent = new EventImpl("Weekend Recurring", startDateTime, endDateTime, "SU", 4, null);
    pattern = weekendEvent.getRecurrence();
    weekdays = pattern.getWeekdays();

    assertEquals(2, weekdays.size());
    assertTrue(weekdays.contains(DayOfWeek.SATURDAY));
    assertTrue(weekdays.contains(DayOfWeek.SUNDAY));
  }
}