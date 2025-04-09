package view.gui;

import model.Calendar;
import model.CalendarImpl;
import model.CalendarManager;
import model.Event;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dialog for creating and editing calendar events.
 */
public class EventDialog extends JDialog {
  private CalendarManager calendarManager;
  private Event eventToEdit;
  private boolean eventCreated = false;

  // UI Components
  private JTextField subjectField;
  private JTextArea descriptionArea;
  private JTextField locationField;
  private JCheckBox allDayCheckBox;
  private JCheckBox privateCheckBox;
  private JSpinner dateSpinner;
  private JSpinner startTimeSpinner;
  private JSpinner endTimeSpinner;
  private JCheckBox recurringCheckBox;
  private JPanel recurringPanel;
  private JCheckBox[] weekdayCheckboxes;
  private JRadioButton occurrencesRadio;
  private JRadioButton untilDateRadio;
  private JSpinner occurrencesSpinner;
  private JSpinner untilDateSpinner;

  /**
   * Constructor for creating a new event.
   *
   * @param parent the parent frame
   * @param calendarManager the calendar manager
   * @param date the date for the new event
   */
  public EventDialog(Frame parent, CalendarManager calendarManager, LocalDate date) {
    super(parent, "Create New Event", true);
    this.calendarManager = calendarManager;

    LocalDateTime initialDateTime = date.atTime(LocalTime.now().withSecond(0).withNano(0));
    LocalDateTime endDateTime = initialDateTime.plusHours(1);

    initializeDialog();
    populateFields(null, initialDateTime, endDateTime, false);
    positionDialog(parent);
  }

  /**
   * Constructor for creating a new event with a specific start time.
   *
   * @param parent the parent frame
   * @param calendarManager the calendar manager
   * @param dateTime the date and time for the new event
   */
  public EventDialog(Frame parent, CalendarManager calendarManager, LocalDateTime dateTime) {
    super(parent, "Create New Event", true);
    this.calendarManager = calendarManager;

    LocalDateTime initialDateTime = dateTime.withSecond(0).withNano(0);
    LocalDateTime endDateTime = initialDateTime.plusHours(1);

    initializeDialog();
    populateFields(null, initialDateTime, endDateTime, false);
    positionDialog(parent);
  }

  /**
   * Constructor for editing an existing event.
   *
   * @param parent the parent frame
   * @param calendarManager the calendar manager
   * @param event the event to edit
   */
  public EventDialog(Frame parent, CalendarManager calendarManager, Event event) {
    super(parent, "Edit Event", true);
    this.calendarManager = calendarManager;
    this.eventToEdit = event;

    initializeDialog();

    LocalDateTime endDateTime = event.isAllDay() ?
        event.getStartDateTime().plusHours(1) : event.getEndDateTime();

    populateFields(event.getSubject(),
        event.getStartDateTime(),
        endDateTime,
        event.isAllDay());

    subjectField.setText(event.getSubject());
    descriptionArea.setText(event.getDescription());
    locationField.setText(event.getLocation());
    privateCheckBox.setSelected(!event.isPublic());

    if (event.isRecurring()) {
      recurringCheckBox.setSelected(true);
      toggleRecurringFields();

      // TODO: Set the recurring fields based on the event's recurrence pattern
      // This requires access to the recurrence pattern which might not be
      // directly accessible from the Event interface
    }

    positionDialog(parent);
  }

  /**
   * Initializes the dialog components.
   */
  private void initializeDialog() {
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
    contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create form panel
    JPanel formPanel = createFormPanel();
    contentPanel.add(formPanel, BorderLayout.CENTER);

    // Create button panel
    JPanel buttonPanel = createButtonPanel();
    contentPanel.add(buttonPanel, BorderLayout.SOUTH);

    setContentPane(contentPanel);
    setSize(500, 600);
    setResizable(true);
  }

  /**
   * Creates the form panel with all input fields.
   */
  private JPanel createFormPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    // Subject field
    JPanel subjectPanel = new JPanel(new BorderLayout());
    subjectPanel.add(new JLabel("Subject:"), BorderLayout.WEST);
    subjectField = new JTextField(20);
    subjectPanel.add(subjectField, BorderLayout.CENTER);
    panel.add(subjectPanel);
    panel.add(Box.createVerticalStrut(10));

    // Date/time panel
    JPanel dateTimePanel = createDateTimePanel();
    panel.add(dateTimePanel);
    panel.add(Box.createVerticalStrut(10));

    // All-day checkbox
    allDayCheckBox = new JCheckBox("All-day event");
    allDayCheckBox.addActionListener(e -> toggleTimeFields());
    panel.add(allDayCheckBox);
    panel.add(Box.createVerticalStrut(5));

    // Recurring checkbox and panel
    recurringCheckBox = new JCheckBox("Recurring event");
    recurringCheckBox.addActionListener(e -> toggleRecurringFields());
    panel.add(recurringCheckBox);
    panel.add(Box.createVerticalStrut(5));

    recurringPanel = createRecurringPanel();
    recurringPanel.setVisible(false);
    panel.add(recurringPanel);
    panel.add(Box.createVerticalStrut(10));

    // Location field
    JPanel locationPanel = new JPanel(new BorderLayout());
    locationPanel.add(new JLabel("Location:"), BorderLayout.WEST);
    locationField = new JTextField(20);
    locationPanel.add(locationField, BorderLayout.CENTER);
    panel.add(locationPanel);
    panel.add(Box.createVerticalStrut(10));

    // Description area
    JPanel descriptionPanel = new JPanel(new BorderLayout());
    descriptionPanel.add(new JLabel("Description:"), BorderLayout.NORTH);
    descriptionArea = new JTextArea(5, 20);
    descriptionArea.setLineWrap(true);
    descriptionArea.setWrapStyleWord(true);
    JScrollPane scrollPane = new JScrollPane(descriptionArea);
    descriptionPanel.add(scrollPane, BorderLayout.CENTER);
    panel.add(descriptionPanel);
    panel.add(Box.createVerticalStrut(10));

    // Private checkbox
    privateCheckBox = new JCheckBox("Private event");
    panel.add(privateCheckBox);

    return panel;
  }

  /**
   * Creates the date and time input panel.
   */
  private JPanel createDateTimePanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(2, 2, 2, 5);

    // Date spinner
    panel.add(new JLabel("Date:"), gbc);
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    dateSpinner = new JSpinner(new SpinnerDateModel());
    JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
    dateSpinner.setEditor(dateEditor);
    panel.add(dateSpinner, gbc);

    // Start time
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 0;
    panel.add(new JLabel("Start Time:"), gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    startTimeSpinner = new JSpinner(new SpinnerDateModel());
    JSpinner.DateEditor startTimeEditor = new JSpinner.DateEditor(startTimeSpinner, "HH:mm");
    startTimeSpinner.setEditor(startTimeEditor);
    panel.add(startTimeSpinner, gbc);

    // End time
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 0;
    panel.add(new JLabel("End Time:"), gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    endTimeSpinner = new JSpinner(new SpinnerDateModel());
    JSpinner.DateEditor endTimeEditor = new JSpinner.DateEditor(endTimeSpinner, "HH:mm");
    endTimeSpinner.setEditor(endTimeEditor);
    panel.add(endTimeSpinner, gbc);

    return panel;
  }

  /**
   * Creates the recurring event configuration panel.
   */
  private JPanel createRecurringPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createTitledBorder("Recurrence"));

    // Weekdays checkboxes
    JPanel weekdaysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    weekdaysPanel.add(new JLabel("Repeat on:"));

    weekdayCheckboxes = new JCheckBox[7];
    String[] dayLabels = {"M", "T", "W", "R", "F", "S", "U"};

    for (int i = 0; i < 7; i++) {
      weekdayCheckboxes[i] = new JCheckBox(dayLabels[i]);
      weekdaysPanel.add(weekdayCheckboxes[i]);
    }

    panel.add(weekdaysPanel);

    // Recurrence end options
    JPanel endPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;

    ButtonGroup endGroup = new ButtonGroup();

    occurrencesRadio = new JRadioButton("End after:");
    endGroup.add(occurrencesRadio);
    endPanel.add(occurrencesRadio, gbc);

    gbc.gridx = 1;
    occurrencesSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 999, 1));
    endPanel.add(occurrencesSpinner, gbc);

    gbc.gridx = 2;
    endPanel.add(new JLabel("occurrences"), gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    untilDateRadio = new JRadioButton("End by:");
    endGroup.add(untilDateRadio);
    endPanel.add(untilDateRadio, gbc);

    gbc.gridx = 1;
    gbc.gridwidth = 2;
    untilDateSpinner = new JSpinner(new SpinnerDateModel());
    JSpinner.DateEditor untilDateEditor = new JSpinner.DateEditor(untilDateSpinner, "yyyy-MM-dd");
    untilDateSpinner.setEditor(untilDateEditor);
    endPanel.add(untilDateSpinner, gbc);

    // Set default selection
    occurrencesRadio.setSelected(true);

    panel.add(endPanel);

    return panel;
  }

  /**
   * Creates the button panel with Save and Cancel buttons.
   */
  private JPanel createButtonPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(e -> dispose());

    JButton saveButton = new JButton("Save");
    saveButton.addActionListener(e -> saveEvent());

    panel.add(cancelButton);
    panel.add(saveButton);

    return panel;
  }

  /**
   * Populates the form fields with data.
   */
  private void populateFields(String subject, LocalDateTime startDateTime,
      LocalDateTime endDateTime, boolean isAllDay) {
    if (subject != null) {
      subjectField.setText(subject);
    }

    // Set date
    java.util.Date date = java.util.Date.from(
        startDateTime.toLocalDate().atStartOfDay().atZone(
            java.time.ZoneId.systemDefault()).toInstant());
    dateSpinner.setValue(date);

    // Set time values
    java.util.Date startTime = java.util.Date.from(
        startDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
    startTimeSpinner.setValue(startTime);

    java.util.Date endTime = java.util.Date.from(
        endDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
    endTimeSpinner.setValue(endTime);

    // Set all-day checkbox
    allDayCheckBox.setSelected(isAllDay);
    toggleTimeFields();

    // Default values for recurring settings
    java.util.Date untilDate = java.util.Date.from(
        startDateTime.plusMonths(1).atZone(java.time.ZoneId.systemDefault()).toInstant());
    untilDateSpinner.setValue(untilDate);
  }

  /**
   * Shows or hides time fields based on the all-day checkbox.
   */
  private void toggleTimeFields() {
    boolean isAllDay = allDayCheckBox.isSelected();
    startTimeSpinner.setEnabled(!isAllDay);
    endTimeSpinner.setEnabled(!isAllDay);
  }

  /**
   * Shows or hides recurring fields based on the recurring checkbox.
   */
  private void toggleRecurringFields() {
    recurringPanel.setVisible(recurringCheckBox.isSelected());
    pack();
  }

  /**
   * Positions the dialog relative to the parent.
   */
  private void positionDialog(Window parent) {
    if (parent != null) {
      setLocationRelativeTo(parent);
    } else {
      setLocationByPlatform(true);
    }
  }

  /**
   * Checks if an event was created or updated.
   */
  public boolean isEventCreated() {
    return eventCreated;
  }

  /**
   * Parses the selected weekdays into a string format.
   */
  private String parseWeekdays() {
    StringBuilder sb = new StringBuilder();
    String[] codes = {"M", "T", "W", "R", "F", "S", "U"};

    for (int i = 0; i < weekdayCheckboxes.length; i++) {
      if (weekdayCheckboxes[i].isSelected()) {
        sb.append(codes[i]);
      }
    }

    return sb.toString();
  }

  /**
   * Creates or updates the event based on the form data.
   */
  private void saveEvent() {
    // Get subject
    String subject = subjectField.getText().trim();
    if (subject.isEmpty()) {
      JOptionPane.showMessageDialog(this,
              "Subject cannot be empty",
              "Error",
              JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Get calendar
    Calendar calendar = calendarManager.getCurrentCalendar();
    if (calendar == null) {
      JOptionPane.showMessageDialog(this,
              "No calendar selected",
              "Error",
              JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Get date and times
    java.util.Date dateValue = (java.util.Date) dateSpinner.getValue();
    LocalDate localDate = dateValue.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate();

    boolean isAllDay = allDayCheckBox.isSelected();
    boolean isPublic = !privateCheckBox.isSelected();
    String description = descriptionArea.getText().trim();
    String location = locationField.getText().trim();
    boolean isRecurring = recurringCheckBox.isSelected();

    LocalDateTime startDateTime;
    LocalDateTime endDateTime = null;

    if (isAllDay) {
      startDateTime = localDate.atStartOfDay();
    } else {
      java.util.Date startTimeValue = (java.util.Date) startTimeSpinner.getValue();
      LocalTime startTime = startTimeValue.toInstant()
              .atZone(java.time.ZoneId.systemDefault())
              .toLocalTime();

      java.util.Date endTimeValue = (java.util.Date) endTimeSpinner.getValue();
      LocalTime endTime = endTimeValue.toInstant()
              .atZone(java.time.ZoneId.systemDefault())
              .toLocalTime();

      startDateTime = LocalDateTime.of(localDate, startTime);
      endDateTime = LocalDateTime.of(localDate, endTime);

      // Validate end time is after start time
      if (endDateTime.isBefore(startDateTime) || endDateTime.equals(startDateTime)) {
        JOptionPane.showMessageDialog(this,
                "End time must be after start time",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        return;
      }
    }

    boolean success = false;

    if (eventToEdit != null) {
      // We're editing an existing event

      // For recurring events, editing becomes more complex
      // We'll use a different approach - delete and recreate
      if (eventToEdit.isRecurring() || isRecurring) {
        // Get all events from the calendar
        List<Event> calendarEvents = calendar.getAllEvents();

        // Find and remove our event
        for (int i = 0; i < calendarEvents.size(); i++) {
          Event event = calendarEvents.get(i);
          if (event == eventToEdit) {
            // Handle this through the CalendarImpl class directly
            ((CalendarImpl)calendar).getAllEvents().remove(i);
            break;
          }
        }

        // Now create a new event with the updated properties
        if (isRecurring) {
          String weekdays = parseWeekdays();
          if (weekdays.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select at least one day of the week for recurring events",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
          }

          int occurrences = -1;
          LocalDateTime untilDate = null;

          if (occurrencesRadio.isSelected()) {
            occurrences = (Integer) occurrencesSpinner.getValue();
          } else {
            java.util.Date untilDateValue = (java.util.Date) untilDateSpinner.getValue();
            untilDate = untilDateValue.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
          }

          if (isAllDay) {
            success = calendar.createRecurringAllDayEvent(
                    subject,
                    startDateTime,
                    weekdays,
                    occurrences,
                    untilDate,
                    true, // Auto decline is always true
                    description,
                    location,
                    isPublic
            );
          } else {
            success = calendar.createRecurringEvent(
                    subject,
                    startDateTime,
                    endDateTime,
                    weekdays,
                    occurrences,
                    untilDate,
                    true, // Auto decline is always true
                    description,
                    location,
                    isPublic
            );
          }
        } else {
          if (isAllDay) {
            success = calendar.createAllDayEvent(
                    subject,
                    startDateTime,
                    true, // Auto decline is always true
                    description,
                    location,
                    isPublic
            );
          } else {
            success = calendar.createEvent(
                    subject,
                    startDateTime,
                    endDateTime,
                    true, // Auto decline is always true
                    description,
                    location,
                    isPublic
            );
          }
        }

        // If creation failed, put back the original event
        if (!success) {
          ((CalendarImpl)calendar).addEvent(eventToEdit);
        }
      } else {
        // For non-recurring events, use the edit methods
        // Start with the subject
        boolean subjectUpdated = true;
        if (!eventToEdit.getSubject().equals(subject)) {
          subjectUpdated = calendar.editEvent(
                  "subject",
                  eventToEdit.getSubject(),
                  eventToEdit.getStartDateTime(),
                  eventToEdit.getEndDateTime(),
                  subject
          );

          if (!subjectUpdated) {
            JOptionPane.showMessageDialog(this,
                    "Failed to update subject.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
          }
        }

        // Update description
        boolean descUpdated = calendar.editEvent(
                "description",
                subjectUpdated ? subject : eventToEdit.getSubject(),
                eventToEdit.getStartDateTime(),
                eventToEdit.getEndDateTime(),
                description
        );

        // Update location
        boolean locUpdated = calendar.editEvent(
                "location",
                subjectUpdated ? subject : eventToEdit.getSubject(),
                eventToEdit.getStartDateTime(),
                eventToEdit.getEndDateTime(),
                location
        );

        // Update public/private status
        boolean publicUpdated = calendar.editEvent(
                "public",
                subjectUpdated ? subject : eventToEdit.getSubject(),
                eventToEdit.getStartDateTime(),
                eventToEdit.getEndDateTime(),
                String.valueOf(isPublic)
        );

        // Update start time
        boolean startTimeUpdated = true;
        if (!eventToEdit.getStartDateTime().equals(startDateTime)) {
          startTimeUpdated = calendar.editEvent(
                  "starttime",
                  subjectUpdated ? subject : eventToEdit.getSubject(),
                  eventToEdit.getStartDateTime(),
                  eventToEdit.getEndDateTime(),
                  startDateTime.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
          );

          if (!startTimeUpdated) {
            JOptionPane.showMessageDialog(this,
                    "Failed to update start time. There may be a conflict with another event.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
          }
        }

        // Update end time for non-all-day events
        boolean endTimeUpdated = true;
        if (!isAllDay && endDateTime != null &&
                (eventToEdit.getEndDateTime() == null ||
                        !endDateTime.equals(eventToEdit.getEndDateTime()))) {
          endTimeUpdated = calendar.editEvent(
                  "endtime",
                  subjectUpdated ? subject : eventToEdit.getSubject(),
                  startTimeUpdated ? startDateTime : eventToEdit.getStartDateTime(),
                  eventToEdit.getEndDateTime(),
                  endDateTime.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
          );

          if (!endTimeUpdated) {
            JOptionPane.showMessageDialog(this,
                    "Failed to update end time. There may be a conflict with another event.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
          }
        }

        success = descUpdated && locUpdated && publicUpdated && startTimeUpdated && endTimeUpdated;
      }
    } else {
      // Creating a new event
      if (isRecurring) {
        String weekdays = parseWeekdays();
        if (weekdays.isEmpty()) {
          JOptionPane.showMessageDialog(this,
                  "Please select at least one day of the week for recurring events",
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
          return;
        }

        int occurrences = -1;
        LocalDateTime untilDate = null;

        if (occurrencesRadio.isSelected()) {
          occurrences = (Integer) occurrencesSpinner.getValue();
        } else {
          java.util.Date untilDateValue = (java.util.Date) untilDateSpinner.getValue();
          untilDate = untilDateValue.toInstant()
                  .atZone(java.time.ZoneId.systemDefault())
                  .toLocalDateTime();
        }

        if (isAllDay) {
          success = calendar.createRecurringAllDayEvent(
                  subject,
                  startDateTime,
                  weekdays,
                  occurrences,
                  untilDate,
                  true, // Auto decline is always true
                  description,
                  location,
                  isPublic
          );
        } else {
          success = calendar.createRecurringEvent(
                  subject,
                  startDateTime,
                  endDateTime,
                  weekdays,
                  occurrences,
                  untilDate,
                  true, // Auto decline is always true
                  description,
                  location,
                  isPublic
          );
        }
      } else {
        if (isAllDay) {
          success = calendar.createAllDayEvent(
                  subject,
                  startDateTime,
                  true, // Auto decline is always true
                  description,
                  location,
                  isPublic
          );
        } else {
          success = calendar.createEvent(
                  subject,
                  startDateTime,
                  endDateTime,
                  true, // Auto decline is always true
                  description,
                  location,
                  isPublic
          );
        }
      }
    }

    if (success) {
      eventCreated = true;
      dispose();
    } else {
      JOptionPane.showMessageDialog(this,
              "Failed to create event. There may be a conflict with another event.",
              "Error",
              JOptionPane.ERROR_MESSAGE);
    }
  }
}