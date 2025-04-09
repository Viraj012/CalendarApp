package view.gui;

import model.Calendar;
import model.CalendarManager;
import model.Event;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel for displaying a day view of the calendar, updated for a more modern look.
 */
public class DayPanel extends JPanel {
  private CalendarManager calendarManager;
  private CalendarGUI mainGUI;
  private LocalDate currentDate;
  private JPanel hoursPanel;
  private JPanel eventsPanel;
  private Map<Event, JPanel> eventPanelMap;

  private static final int HOURS_IN_DAY = 24;
  private static final int HOUR_HEIGHT = 60;
  private static final int HALF_HOUR_HEIGHT = HOUR_HEIGHT / 2;
  private static final int TIMELINE_WIDTH = 70;

  // Colors
  private static final Color PRIMARY_COLOR = new Color(66, 133, 244);
  private static final Color SECONDARY_COLOR = new Color(245, 247, 250);
  private static final Color ACCENT_COLOR = new Color(255, 148, 77);
  private static final Color TEXT_COLOR = new Color(50, 50, 50);
  private static final Color LIGHT_TEXT_COLOR = new Color(120, 120, 120);
  private static final Color LIGHT_GRID_COLOR = new Color(230, 230, 230);
  private static final Color DARK_GRID_COLOR = new Color(200, 200, 200);

  private static final Color CURRENT_TIME_LINE = new Color(231, 76, 60);

  private static final Font TIME_FONT = new Font("Segoe UI", Font.PLAIN, 12);
  private static final Font EVENT_TITLE_FONT = new Font("Segoe UI", Font.BOLD, 13);
  private static final Font EVENT_DETAIL_FONT = new Font("Segoe UI", Font.PLAIN, 12);

  public DayPanel(CalendarManager calendarManager, CalendarGUI mainGUI) {
    this.calendarManager = calendarManager;
    this.mainGUI = mainGUI;
    this.currentDate = LocalDate.now();
    this.eventPanelMap = new HashMap<>();

    setLayout(new BorderLayout());
    setBackground(Color.WHITE);

    // Panel for all-day events (top)
    JPanel allDayPanel = createAllDayPanel();
    add(allDayPanel, BorderLayout.NORTH);

    // Scrollable panel for timeline
    JPanel dayViewPanel = new JPanel(new BorderLayout());
    dayViewPanel.setBackground(Color.WHITE);

    hoursPanel = createHoursPanel();
    dayViewPanel.add(hoursPanel, BorderLayout.WEST);

    eventsPanel = createEventsPanel();
    dayViewPanel.add(eventsPanel, BorderLayout.CENTER);

    JScrollPane scrollPane = new JScrollPane(dayViewPanel);
    scrollPane.setBorder(null);
    scrollPane.getVerticalScrollBar().setUnitIncrement(HOUR_HEIGHT / 4);
    scrollPane.setBackground(Color.WHITE);

    SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(8 * HOUR_HEIGHT));

    add(scrollPane, BorderLayout.CENTER);
  }

  private JPanel createAllDayPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(new Color(235, 242, 255));
    // Reduce the border from (10,10,10,10) to (5,5,5,5)
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, DARK_GRID_COLOR),
        new EmptyBorder(5, 5, 5, 5)
    ));

    JLabel titleLabel = new JLabel("All Day Events");
    titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
    titleLabel.setForeground(PRIMARY_COLOR);
    // Reduce the gap under the title label from 10px to 5px
    titleLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
    panel.add(titleLabel, BorderLayout.NORTH);

    JPanel eventsContainer = new JPanel();
    eventsContainer.setBackground(new Color(235, 242, 255));
    eventsContainer.setLayout(new BoxLayout(eventsContainer, BoxLayout.Y_AXIS));

    JScrollPane scrollPane = new JScrollPane(eventsContainer);
    scrollPane.setBorder(null);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    // Decrease preferred height from 70 to 40, for instance
    scrollPane.setPreferredSize(new Dimension(getWidth(), 40));
    scrollPane.getViewport().setBackground(new Color(235, 242, 255));

    panel.add(scrollPane, BorderLayout.CENTER);
    return panel;
  }


  private JPanel createHoursPanel() {
    JPanel panel = new JPanel(null);
    panel.setPreferredSize(new Dimension(TIMELINE_WIDTH, HOURS_IN_DAY * HOUR_HEIGHT));
    panel.setBackground(SECONDARY_COLOR);
    panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DARK_GRID_COLOR));

    for (int hour = 0; hour < HOURS_IN_DAY; hour++) {
      String time = String.format("%02d:00", hour);
      JLabel label = new JLabel(time);
      label.setFont(TIME_FONT);
      label.setForeground(LIGHT_TEXT_COLOR);
      label.setBounds(10, hour * HOUR_HEIGHT, TIMELINE_WIDTH - 15, 20);
      panel.add(label);
    }
    return panel;
  }

  private JPanel createEventsPanel() {
    JPanel panel = new JPanel(null) {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawHourLines(g);
        drawHalfHourLines(g);
        drawCurrentTimeLine(g);
      }
    };
    panel.setBackground(Color.WHITE);
    panel.setPreferredSize(new Dimension(800, HOURS_IN_DAY * HOUR_HEIGHT));

    panel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          LocalTime time = pixelToTime(e.getY());
          LocalDateTime dateTime = LocalDateTime.of(currentDate, time);
          mainGUI.createNewEvent(dateTime);
        }
      }
    });
    return panel;
  }

  private void drawHourLines(Graphics g) {
    g.setColor(LIGHT_GRID_COLOR);
    for (int hour = 0; hour < HOURS_IN_DAY; hour++) {
      g.drawLine(0, hour * HOUR_HEIGHT, getWidth(), hour * HOUR_HEIGHT);
    }
  }

  private void drawHalfHourLines(Graphics g) {
    Graphics2D g2d = (Graphics2D) g.create();
    g2d.setColor(LIGHT_GRID_COLOR);

    float[] dash = {2f, 4f};
    g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
    for (int hour = 0; hour < HOURS_IN_DAY; hour++) {
      int yPos = hour * HOUR_HEIGHT + HALF_HOUR_HEIGHT;
      g2d.drawLine(0, yPos, getWidth(), yPos);
    }
    g2d.dispose();
  }

  private void drawCurrentTimeLine(Graphics g) {
    if (!currentDate.equals(LocalDate.now())) return;
    LocalTime now = LocalTime.now();
    int minutes = now.getHour() * 60 + now.getMinute();
    int yPos = (minutes * HOUR_HEIGHT) / 60;

    Graphics2D g2d = (Graphics2D) g.create();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g2d.setColor(CURRENT_TIME_LINE);
    g2d.fillOval(4, yPos - 4, 8, 8);

    g2d.setStroke(new BasicStroke(2f));
    g2d.drawLine(12, yPos, getWidth(), yPos);

    g2d.dispose();
  }

  private LocalTime pixelToTime(int yPos) {
    int totalMinutes = (yPos * 60) / HOUR_HEIGHT;
    int hour = totalMinutes / 60;
    int minute = totalMinutes % 60;
    minute = (minute / 15) * 15; // Round to 15-minute increments
    hour = Math.max(0, Math.min(23, hour));
    minute = Math.max(0, Math.min(59, minute));
    return LocalTime.of(hour, minute);
  }

  public void updateView(LocalDate date) {
    this.currentDate = date;
    eventsPanel.removeAll();
    eventPanelMap.clear();

    // All-day container
    JPanel allDayPanel = (JPanel) getComponent(0);
    JScrollPane scrollPane = (JScrollPane) allDayPanel.getComponent(1);
    JPanel allDayContainer = (JPanel) scrollPane.getViewport().getView();
    allDayContainer.removeAll();

    if (calendarManager.getCurrentCalendar() != null) {
      List<Event> events = calendarManager.getCurrentCalendar().getEventsOn(date.atStartOfDay());
      for (Event event : events) {
        if (event.isAllDay()) {
          addAllDayEvent(event, allDayContainer);
        } else {
          addTimedEvent(event);
        }
      }
    }
    allDayContainer.revalidate();
    allDayContainer.repaint();
    eventsPanel.revalidate();
    eventsPanel.repaint();
    revalidate();
    repaint();
  }

  private void addAllDayEvent(Event event, JPanel container) {
    JPanel eventPanel = createEventPanel(event, true);
    container.add(eventPanel);
    container.add(Box.createVerticalStrut(5));
    eventPanelMap.put(event, eventPanel);
  }

  private void addTimedEvent(Event event) {
    LocalTime startTime = event.getStartDateTime().toLocalTime();
    LocalTime endTime = event.getEndDateTime().toLocalTime();
    int startMinutes = startTime.getHour() * 60 + startTime.getMinute();
    int endMinutes = endTime.getHour() * 60 + endTime.getMinute();
    int startY = (startMinutes * HOUR_HEIGHT) / 60;
    int height = ((endMinutes - startMinutes) * HOUR_HEIGHT) / 60;
    height = Math.max(height, 30);

    JPanel eventPanel = createEventPanel(event, false);
    eventPanel.setBounds(10, startY, eventsPanel.getWidth() - 20, height);
    eventsPanel.add(eventPanel);
    eventPanelMap.put(event, eventPanel);
  }

  private JPanel createEventPanel(Event event, boolean isAllDay) {
    Color eventColor = getEventTypeColor(event);
    JPanel panel = new JPanel(new BorderLayout()) {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(eventColor.getRed(), eventColor.getGreen(), eventColor.getBlue(), isAllDay ? 40 : 60));
        g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));

        g2d.setColor(eventColor);
        g2d.fill(new RoundRectangle2D.Float(0, 0, 4, getHeight(), 2, 2));
        g2d.dispose();
        super.paintComponent(g);
      }
    };
    panel.setOpaque(false);
    panel.setBorder(new EmptyBorder(6, 8, 6, 8));

    String timeText = isAllDay
        ? "All day"
        : formatTime(event.getStartDateTime()) + " - " + formatTime(event.getEndDateTime());

    JLabel timeLabel = new JLabel(timeText);
    timeLabel.setFont(EVENT_DETAIL_FONT);
    timeLabel.setForeground(LIGHT_TEXT_COLOR);

    JLabel subjectLabel = new JLabel(event.getSubject());
    subjectLabel.setFont(EVENT_TITLE_FONT);
    subjectLabel.setForeground(TEXT_COLOR);

    JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
    infoPanel.setOpaque(false);
    infoPanel.add(subjectLabel);
    infoPanel.add(Box.createVerticalStrut(2));
    infoPanel.add(timeLabel);

    if (event.getLocation() != null && !event.getLocation().isEmpty()) {
      JLabel locationLabel = new JLabel("@ " + event.getLocation());
      locationLabel.setFont(EVENT_DETAIL_FONT);
      locationLabel.setForeground(LIGHT_TEXT_COLOR);
      infoPanel.add(Box.createVerticalStrut(3));
      infoPanel.add(locationLabel);
    }

    panel.add(infoPanel, BorderLayout.CENTER);
    panel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        mainGUI.editEvent(event);
      }
    });
    return panel;
  }

  private Color getEventTypeColor(Event event) {
    boolean isAllDay = event.isAllDay();
    boolean isRecurring = event.isRecurring();
    if (isAllDay && isRecurring) {
      return new Color(138, 43, 226); // Purple
    } else if (isAllDay) {
      return new Color(76, 175, 80);  // Green
    } else if (isRecurring) {
      return new Color(255, 152, 0);  // Orange
    } else {
      return new Color(33, 150, 243); // Blue
    }
  }

  private String formatTime(LocalDateTime dateTime) {
    return DateTimeFormatter.ofPattern("HH:mm").format(dateTime);
  }
}
