package model;

import java.time.LocalDateTime;

/**
 * Interface representing a calendar event.
 */
public interface Event {
  /**
   * Get the subject/name of the event.
   * @return the event subject
   */
  String getSubject();

  /**
   * Get the start date and time of the event.
   * @return the start date and time
   */
  LocalDateTime getStartDateTime();

  /**
   * Get the end date and time of the event.
   * @return the end date and time
   */
  LocalDateTime getEndDateTime();

  /**
   * Get the description of the event.
   * @return the event description
   */
  String getDescription();

  /**
   * Get the location of the event.
   * @return the event location
   */
  String getLocation();

  /**
   * Check if the event is public.
   * @return true if the event is public, false if private
   */
  boolean isPublic();

  /**
   * Check if the event is an all-day event.
   * @return true if it's an all-day event
   */
  boolean isAllDay();

  /**
   * Check if the event is recurring.
   * @return true if the event is recurring
   */
  boolean isRecurring();

  /**
   * Check if this event conflicts with another event.
   * @param other the other event to check against
   * @return true if there is a conflict
   */
  boolean conflictsWith(Event other);
}