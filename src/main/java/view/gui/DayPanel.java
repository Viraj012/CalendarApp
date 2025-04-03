package view.gui;

import model.Calendar;
import model.CalendarManager;
import model.Event;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel for displaying a day view of the calendar.
 */
public class DayPanel extends JPanel {
  private CalendarManager calendarManager;
  private CalendarGUI mainGUI;
  private LocalDate currentDate;
  private JPanel hoursPanel;
  private JPanel eventsPanel;
  private Map<Event, JPanel> eventPanelMap;

  // Constants for rendering
  private static final int HOURS_IN_DAY = 24;
  private static final int HOUR_HEIGHT = 60; // Pixels per hour
  private static final int TIMELINE_WIDTH = 60; // Width of the time labels column
  private static final Color EVENT_BACKGROUND = new Color(220, 240, 220);
  private static final Color EVENT_BORDER = new Color(200, 220, 200);
  private static final Color ALL_DAY_BACKGROUND = new Color(240, 240, 255);
  private static final Color CURRENT_TIME_LINE = new Color(255, 0, 0);

  /**
   * Constructor for the day view panel.
   *
   * @param calendarManager the calendar manager to use
   * @param mainGUI the main GUI frame
   */
  public DayPanel(CalendarManager calendarManager, CalendarGUI mainGUI) {
    this.calendarManager = calendarManager;
    this.mainGUI = mainGUI;
    this.currentDate = LocalDate.now();
    this.eventPanelMap = new HashMap<>();

    setLayout(new BorderLayout());

    // Create panel for all-day events
    JPanel allDayPanel = createAllDayPanel();
    add(allDayPanel, BorderLayout.NORTH);

    // Create scrollable panel for the day view
    JPanel dayViewPanel = new JPanel(new BorderLayout());

    // Create the hours timeline panel
    hoursPanel = createHoursPanel();
    dayViewPanel.add(hoursPanel, BorderLayout.WEST);

    // Create the events panel
    eventsPanel = createEventsPanel();
    dayViewPanel.add(eventsPanel, BorderLayout.CENTER);

    // Add the day view panel to a scroll pane
    JScrollPane scrollPane = new JScrollPane(dayViewPanel);
    scrollPane.getVerticalScrollBar().setUnitIncrement(HOUR_HEIGHT / 4);

    // Start scrolled to 8 AM by default
    SwingUtilities.invokeLater(() -> {
      scrollPane.getVerticalScrollBar().setValue(8 * HOUR_HEIGHT);
    });

    add(scrollPane, BorderLayout.CENTER);
  }

  /**
   * Creates the panel for all-day events.
   */
  private JPanel createAllDayPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder("All Day"));
    panel.setPreferredSize(new Dimension(getWidth(), 80));

    JPanel eventsContainer = new JPanel();
    eventsContainer.setLayout(new BoxLayout(eventsContainer, BoxLayout.Y_AXIS));

    JScrollPane scrollPane = new JScrollPane(eventsContainer);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setPreferredSize(new Dimension(getWidth(), 70));

    panel.add(scrollPane, BorderLayout.CENTER);
    return panel;
  }

  /**
   * Creates the hours timeline panel.
   */
  private JPanel createHoursPanel() {
    JPanel panel = new JPanel(null); // Using null layout for absolute positioning
    panel.setPreferredSize(new Dimension(TIMELINE_WIDTH, HOURS_IN_DAY * HOUR_HEIGHT));

    for (int hour = 0; hour < HOURS_IN_DAY; hour++) {
      String time = String.format("%02d:00", hour);
      JLabel label = new JLabel(time);
      label.setBounds(5, hour * HOUR_HEIGHT - 8, TIMELINE_WIDTH - 10, 20);
      panel.add(label);

      // Add a horizontal line for each hour
      JSeparator separator = new JSeparator();
      separator.setBounds(0, hour * HOUR_HEIGHT, TIMELINE_WIDTH, 1);
      panel.add(separator);
    }

    return panel;
  }

  /**
   * Creates the events panel.
   */
  private JPanel createEventsPanel() {
    JPanel panel = new JPanel(null) { // Using null layout for absolute positioning
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawCurrentTimeLine(g);
        drawHourLines(g);
      }
    };
    panel.setPreferredSize(new Dimension(getWidth() - TIMELINE_WIDTH, HOURS_IN_DAY * HOUR_HEIGHT));

    // Add click listener to create events
    panel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          LocalTime time = pixelToTime(e.getY());
          LocalDateTime dateTime = LocalDateTime.of(currentDate, time);
          // Open the event dialog with the clicked time
          // This method would need to be added to CalendarGUI
          mainGUI.createNewEvent(dateTime);
        }
      }
    });

    return panel;
  }

  /**
   * Draws the hour lines on the events panel.
   */
  private void drawHourLines(Graphics g) {
    g.setColor(Color.LIGHT_GRAY);
    for (int hour = 0; hour < HOURS_IN_DAY; hour++) {
      g.drawLine(0, hour * HOUR_HEIGHT, getWidth(), hour * HOUR_HEIGHT);
    }
  }

  /**
   * Draws a line representing the current time.
   */
  private void drawCurrentTimeLine(Graphics g) {
    if (!currentDate.equals(LocalDate.now())) {
      return; // Only draw the line for today
    }

    LocalTime now = LocalTime.now();
    int minutes = now.getHour() * 60 + now.getMinute();
    int yPos = (minutes * HOUR_HEIGHT) / 60;

    g.setColor(CURRENT_TIME_LINE);
    g.drawLine(0, yPos, getWidth(), yPos);
  }

  /**
   * Converts a pixel position to a time.
   */
  private LocalTime pixelToTime(int yPos) {
    int totalMinutes = (yPos * 60) / HOUR_HEIGHT;
    int hour = totalMinutes / 60;
    int minute = totalMinutes % 60;

    // Ensure valid time
    hour = Math.max(0, Math.min(23, hour));
    minute = Math.max(0, Math.min(59, minute));

    return LocalTime.of(hour, minute);
  }

  /**
   * Updates the view to show the specified date.
   */
  public void updateView(LocalDate date) {
    this.currentDate = date;

    // Clear existing events
    eventsPanel.removeAll();
    eventPanelMap.clear();

    // Get the container for all-day events (first child of the NORTH component)
    JPanel allDayPanel = (JPanel) getComponent(0);
    JScrollPane scrollPane = (JScrollPane) allDayPanel.getComponent(0);
    JPanel allDayEventsContainer = (JPanel) scrollPane.getViewport().getView();
    allDayEventsContainer.removeAll();

    if (calendarManager.getCurrentCalendar() != null) {
      List<Event> events = calendarManager.getCurrentCalendar().getEventsOn(date.atStartOfDay());

      for (Event event : events) {
        if (event.isAllDay()) {
          addAllDayEvent(event, allDayEventsContainer);
        } else {
          addEvent(event);
        }
      }
    }

    // Refresh UI
    allDayEventsContainer.revalidate();
    allDayEventsContainer.repaint();
    eventsPanel.revalidate();
    eventsPanel.repaint();
    revalidate();
    repaint();
  }

  /**
   * Adds an all-day event to the view.
   */
  private void addAllDayEvent(Event event, JPanel container) {
    JPanel eventPanel = createEventPanel(event, true);
    container.add(eventPanel);
    eventPanelMap.put(event, eventPanel);
  }

  /**
   * Adds a timed event to the view.
   */
  private void addEvent(Event event) {
    LocalTime startTime = event.getStartDateTime().toLocalTime();
    LocalTime endTime = event.getEndDateTime().toLocalTime();

    // Calculate position
    int startMinutes = startTime.getHour() * 60 + startTime.getMinute();
    int endMinutes = endTime.getHour() * 60 + endTime.getMinute();

    int startY = (startMinutes * HOUR_HEIGHT) / 60;
    int height = ((endMinutes - startMinutes) * HOUR_HEIGHT) / 60;

    // Ensure minimum height
    height = Math.max(height, 30);

    JPanel eventPanel = createEventPanel(event, false);

    // Position the event panel
    eventPanel.setBounds(10, startY, eventsPanel.getWidth() - 20, height);

    eventsPanel.add(eventPanel);
    eventPanelMap.put(event, eventPanel);
  }

  /**
   * Creates a panel for displaying an event.
   */
  private JPanel createEventPanel(Event event, boolean isAllDay) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createLineBorder(EVENT_BORDER));
    panel.setBackground(isAllDay ? ALL_DAY_BACKGROUND : EVENT_BACKGROUND);

    // Format the time and subject
    String timeText = isAllDay ? "All day" :
        formatTime(event.getStartDateTime()) + " - " + formatTime(event.getEndDateTime());

    JLabel timeLabel = new JLabel(timeText);
    timeLabel.setFont(timeLabel.getFont().deriveFont(Font.BOLD));
    timeLabel.setBorder(new EmptyBorder(2, 5, 2, 5));

    JLabel subjectLabel = new JLabel(event.getSubject());
    subjectLabel.setBorder(new EmptyBorder(2, 5, 2, 5));

    JPanel infoPanel = new JPanel(new GridLayout(0, 1));
    infoPanel.setOpaque(false);
    infoPanel.add(timeLabel);
    infoPanel.add(subjectLabel);

    // Add location if available
    if (event.getLocation() != null && !event.getLocation().isEmpty()) {
      JLabel locationLabel = new JLabel("@ " + event.getLocation());
      locationLabel.setFont(locationLabel.getFont().deriveFont(Font.ITALIC));
      locationLabel.setBorder(new EmptyBorder(2, 5, 2, 5));
      infoPanel.add(locationLabel);
    }

    panel.add(infoPanel, BorderLayout.CENTER);

    // Add click listener to edit event
    panel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        mainGUI.editEvent(event);
      }
    });

    return panel;
  }

  /**
   * Formats time for display.
   */
  private String formatTime(LocalDateTime dateTime) {
    return DateTimeFormatter.ofPattern("HH:mm").format(dateTime);
  }
}