import java.util.List;
import java.util.Objects;
import model.EventImpl;
import model.RecurrencePattern;
import org.junit.Before;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the EventImpl class.
 */
public class EventImplTest {
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;
  private LocalDateTime untilDate;

  @Before
  public void setUp() {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    startDateTime = LocalDateTime.of(2023, 5, 15, 10, 0);
    endDateTime = LocalDateTime.of(2023, 5, 15, 11, 30);
    untilDate = LocalDateTime.of(2023, 6, 15, 0, 0);
  }

  @Test
  public void testToString_RegularEvent() {

    EventImpl event = new EventImpl("Team Meeting", startDateTime, endDateTime);
    event.setLocation("Conference Room A");

    String expected = "Team Meeting - 2023-05-15 10:00 to 11:30 at Conference Room A";
    assertEquals(expected, event.toString());
  }

  @Test
  public void testToString_RegularEventNoLocation() {

    EventImpl event = new EventImpl("Team Meeting", startDateTime, endDateTime);

    String expected = "Team Meeting - 2023-05-15 10:00 to 11:30";
    assertEquals(expected, event.toString());
  }

  @Test
  public void testToString_AllDayEvent() {

    EventImpl event = new EventImpl("Company Holiday", startDateTime);

    String expected = "Company Holiday - 2023-05-15 All Day";
    assertEquals(expected, event.toString());
  }

  @Test
  public void testToString_AllDayEventWithLocation() {

    EventImpl event = new EventImpl("Company Holiday", startDateTime);
    event.setLocation("Main Office");

    String expected = "Company Holiday - 2023-05-15 All Day at Main Office";
    assertEquals(expected, event.toString());
  }

  @Test
  public void testToString_MultiDayEvent() {

    LocalDateTime multiDayEnd = LocalDateTime.of(2023, 5, 16, 16, 0);
    EventImpl event = new EventImpl("Conference", startDateTime, multiDayEnd);

    String expected = "Conference - 2023-05-15 10:00 to 2023-05-16 16:00";
    assertEquals(expected, event.toString());
  }

  @Test
  public void testToString_RecurringEvent() {

    EventImpl event = new EventImpl("Weekly Standup", startDateTime, endDateTime, "MWF", 10, null);

    String result = event.toString();
    assertTrue(result.contains("Repeats on: Mon,Wed,Fri for 10 times"));
  }

  @Test
  public void testToString_RecurringEventWithUntilDate() {

    EventImpl event = new EventImpl("Weekly Standup", startDateTime, endDateTime, "MWF", -1,
        untilDate);

    String result = event.toString();
    assertTrue(result.contains("Repeats on: Mon,Wed,Fri until 2023-06-15"));
  }

  @Test
  public void testToString_RecurringAllDayEvent() {

    EventImpl event = new EventImpl("Team Offsite", startDateTime, "MWF", 5, null);

    String result = event.toString();
    assertTrue(result.contains("All Day"));
    assertTrue(result.contains("Repeats on: Mon,Wed,Fri for 5 times"));
  }

  @Test
  public void testToString_AllWeekdaysRecurringEvent() {

    EventImpl event = new EventImpl("Daily Check-in", startDateTime, endDateTime, "MTWRFSU", -1,
        untilDate);

    String result = event.toString();

    assertTrue(result.contains("Mon"));
    assertTrue(result.contains("Tue"));
    assertTrue(result.contains("Wed"));
    assertTrue(result.contains("Thu"));
    assertTrue(result.contains("Fri"));
    assertTrue(result.contains("Sat"));
    assertTrue(result.contains("Sun"));
  }

  @Test
  public void testToString_EdgeCaseNullEndDateTime() {

    EventImpl event = new EventImpl("Strange Event", startDateTime, null);

    String result = event.toString();

    assertEquals("Strange Event - 2023-05-15", result);
  }


  @Test
  public void testRecurrencePatternWeekdays() {
    EventImpl event = new EventImpl("Test Event", startDateTime, endDateTime, "MTWRFSU", -1,
        untilDate);
    RecurrencePattern pattern = event.getRecurrence();
    Set<DayOfWeek> weekdays = pattern.getWeekdays();

    assertEquals(7, weekdays.size());
    assertTrue(weekdays.contains(DayOfWeek.MONDAY));
    assertTrue(weekdays.contains(DayOfWeek.TUESDAY));
    assertTrue(weekdays.contains(DayOfWeek.WEDNESDAY));
    assertTrue(weekdays.contains(DayOfWeek.THURSDAY));
    assertTrue(weekdays.contains(DayOfWeek.FRIDAY));
    assertTrue(weekdays.contains(DayOfWeek.SATURDAY));
    assertTrue(weekdays.contains(DayOfWeek.SUNDAY));
  }

  @Test
  public void testConflictsWith_BothAllDayEvents_SameDay() {
    EventImpl event1 = new EventImpl("Event 1", LocalDateTime.of(2023, 5, 15, 0, 0));
    EventImpl event2 = new EventImpl("Event 2", LocalDateTime.of(2023, 5, 15, 0, 0));
    assertTrue(event1.conflictsWith(event2));
  }

  @Test
  public void testConflictsWith_ThisAllDayOtherRegular_SameDay() {
    EventImpl allDayEvent = new EventImpl("All Day", LocalDateTime.of(2023, 5, 15, 0, 0));
    EventImpl regularEvent = new EventImpl("Regular",
        LocalDateTime.of(2023, 5, 15, 10, 0),
        LocalDateTime.of(2023, 5, 15, 11, 0));
    assertTrue(allDayEvent.conflictsWith(regularEvent));
  }

  @Test
  public void testConflictsWith_ThisRegularOtherAllDay_SameDay() {
    EventImpl regularEvent = new EventImpl("Regular",
        LocalDateTime.of(2023, 5, 15, 10, 0),
        LocalDateTime.of(2023, 5, 15, 11, 0));
    EventImpl allDayEvent = new EventImpl("All Day", LocalDateTime.of(2023, 5, 15, 0, 0));
    assertTrue(regularEvent.conflictsWith(allDayEvent));
  }

  @Test
  public void testConflictsWith_BothAllDayEvents_DifferentDays() {
    EventImpl event1 = new EventImpl("Event 1", LocalDateTime.of(2023, 5, 15, 0, 0));
    EventImpl event2 = new EventImpl("Event 2", LocalDateTime.of(2023, 5, 16, 0, 0));
    assertFalse(event1.conflictsWith(event2));
  }

  @Test
  public void testConflictsWith_RegularEvents_Overlapping() {
    EventImpl event1 = new EventImpl("Event 1",
        LocalDateTime.of(2023, 5, 15, 10, 0),
        LocalDateTime.of(2023, 5, 15, 12, 0));
    EventImpl event2 = new EventImpl("Event 2",
        LocalDateTime.of(2023, 5, 15, 11, 0),
        LocalDateTime.of(2023, 5, 15, 13, 0));
    assertTrue(event1.conflictsWith(event2));
  }

  @Test
  public void testConflictsWith_RegularEvents_NonOverlapping() {
    EventImpl event1 = new EventImpl("Event 1",
        LocalDateTime.of(2023, 5, 15, 10, 0),
        LocalDateTime.of(2023, 5, 15, 11, 0));
    EventImpl event2 = new EventImpl("Event 2",
        LocalDateTime.of(2023, 5, 15, 12, 0),
        LocalDateTime.of(2023, 5, 15, 13, 0));
    assertFalse(event1.conflictsWith(event2));
  }

  @Test
  public void testConflictsWith_EventEndBeforeOtherStarts() {
    EventImpl event1 = new EventImpl("Event 1",
        LocalDateTime.of(2023, 5, 15, 9, 0),
        LocalDateTime.of(2023, 5, 15, 10, 0));
    EventImpl event2 = new EventImpl("Event 2",
        LocalDateTime.of(2023, 5, 15, 10, 0),
        LocalDateTime.of(2023, 5, 15, 11, 0));
    assertFalse(event1.conflictsWith(event2));
  }

  @Test
  public void testConflictsWith_EventStartsAfterOtherEnds() {
    EventImpl event1 = new EventImpl("Event 1",
        LocalDateTime.of(2023, 5, 15, 11, 0),
        LocalDateTime.of(2023, 5, 15, 12, 0));
    EventImpl event2 = new EventImpl("Event 2",
        LocalDateTime.of(2023, 5, 15, 9, 0),
        LocalDateTime.of(2023, 5, 15, 11, 0));
    assertFalse(event1.conflictsWith(event2));
  }

  @Test
  public void testEquals_SameInstance() {
    EventImpl event = new EventImpl("Event", LocalDateTime.of(2023, 5, 15, 10, 0));
    assertTrue(event.equals(event));
  }

  @Test
  public void testEquals_Null() {
    EventImpl event = new EventImpl("Event", LocalDateTime.of(2023, 5, 15, 10, 0));
    assertFalse(event == null);
  }

  @Test
  public void testEquals_DifferentClass() {
    EventImpl event = new EventImpl("Event", LocalDateTime.of(2023, 5, 15, 10, 0));
    assertFalse(event.equals("Not an event"));
  }

  @Test
  public void testEquals_SameProperties() {
    LocalDateTime dateTime = LocalDateTime.of(2023, 5, 15, 10, 0);
    EventImpl event1 = new EventImpl("Event", dateTime);
    EventImpl event2 = new EventImpl("Event", dateTime);
    assertTrue(event1.equals(event2));
  }

  @Test
  public void testEquals_DifferentSubject() {
    LocalDateTime dateTime = LocalDateTime.of(2023, 5, 15, 10, 0);
    EventImpl event1 = new EventImpl("Event 1", dateTime);
    EventImpl event2 = new EventImpl("Event 2", dateTime);
    assertFalse(event1.equals(event2));
  }

  @Test
  public void testEquals_DifferentDateTime() {
    EventImpl event1 = new EventImpl("Event", LocalDateTime.of(2023, 5, 15, 10, 0));
    EventImpl event2 = new EventImpl("Event", LocalDateTime.of(2023, 5, 16, 10, 0));
    assertFalse(event1.equals(event2));
  }

  @Test
  public void testHashCode() {
    String subject = "Event";
    LocalDateTime dateTime = LocalDateTime.of(2023, 5, 15, 10, 0);
    EventImpl event = new EventImpl(subject, dateTime);

    int expected = Objects.hash(subject, dateTime);
    assertEquals(expected, event.hashCode());
  }

  @Test
  public void testCalculateRecurrences_UntilDateBeforeMaxEndDate() {

    LocalDateTime baseDate = LocalDateTime.of(2023, 5, 15, 10, 0);
    LocalDateTime untilDate = baseDate.plusMonths(6);

    RecurrencePattern pattern = new RecurrencePattern("MWF", -1, untilDate);
    List<LocalDateTime> recurrences = pattern.calculateRecurrences(baseDate);

    for (LocalDateTime date : recurrences) {
      assertTrue(date.isEqual(untilDate) || date.isBefore(untilDate));
    }

    if (!recurrences.isEmpty()) {
      LocalDateTime lastDate = recurrences.get(recurrences.size() - 1);

      assertTrue(untilDate.minusWeeks(1).isBefore(lastDate));
    }
  }

  @Test
  public void testCalculateRecurrences_UntilDateAfterMaxEndDate() {

    LocalDateTime baseDate = LocalDateTime.of(2023, 5, 15, 10, 0);

    LocalDateTime untilDate = baseDate.plusYears(10);

    RecurrencePattern pattern = new RecurrencePattern("MWF", -1, untilDate);
    List<LocalDateTime> recurrences = pattern.calculateRecurrences(baseDate);

    LocalDateTime fiveYearLimit = baseDate.plusYears(5).plusWeeks(1);
    for (LocalDateTime date : recurrences) {
      assertTrue(date.isBefore(fiveYearLimit));
    }

    if (!recurrences.isEmpty()) {
      LocalDateTime lastDate = recurrences.get(recurrences.size() - 1);

      assertTrue(baseDate.plusYears(4).plusMonths(11).isBefore(lastDate));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseWeekdays_InvalidCharacter() {

    new RecurrencePattern("MX", 5, null);
  }
}