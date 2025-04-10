package view.gui;

import model.CalendarManager;
import model.Event;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Panel for displaying a month view of the calendar with a fresher design.
 */
public class MonthPanel extends JPanel {
  private CalendarManager calendarManager;
  private CalendarGUI mainGUI;
  private JPanel daysPanel;
  private JLabel[][] dayLabels;
  private JPanel[][] dayCells;
  private JScrollPane[] eventScrollPanes;
  private JPanel[] eventPanels;
  private LocalDate currentMonthDate;
  private LocalDate selectedDate;

  private Map<String, Color> calendarColors = new HashMap<>();

  private static final int ROWS = 6;
  private static final int COLS = 7;

  private static final Color PRIMARY_COLOR = new Color(66, 133, 244);
  private static final Color SECONDARY_COLOR = new Color(245, 247, 250);
  private static final Color ACCENT_COLOR = new Color(255, 148, 77);
  private static final Color TEXT_COLOR = new Color(50, 50, 50);
  private static final Color LIGHT_TEXT_COLOR = new Color(120, 120, 120);

  private static final Color TODAY_BACKGROUND = new Color(235, 245, 255);
  private static final Color TODAY_BORDER = new Color(120, 160, 215);
  private static final Color WEEKEND_BACKGROUND = new Color(250, 250, 250);
  private static final Color SELECTED_BACKGROUND = new Color(220, 230, 255);
  private static final Color NOT_MONTH_BACKGROUND = new Color(245, 245, 245);

  private static final Font DAY_NUMBER_FONT = new Font("Segoe UI", Font.BOLD, 14);
  private static final Font EVENT_FONT = new Font("Segoe UI", Font.PLAIN, 11);
  private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 13);

  private static final Color ALL_DAY_EVENT_COLOR = new Color(75, 175, 80);
  private static final Color SINGLE_EVENT_COLOR = new Color(33, 150, 243);
  private static final Color RECURRING_EVENT_COLOR = new Color(255, 152, 0);
  private static final Color ALL_DAY_RECURRING_COLOR = new Color(138, 43, 226);

  private static final Color ALL_DAY_EVENT_BG = new Color(76, 175, 80, 40);
  private static final Color SINGLE_EVENT_BG = new Color(33, 150, 243, 40);
  private static final Color RECURRING_EVENT_BG = new Color(255, 152, 0, 40);
  private static final Color ALL_DAY_RECURRING_BG = new Color(138, 43, 226, 40);

  private static final Color[] CALENDAR_COLORS = {
      new Color(41, 128, 185),
      new Color(39, 174, 96),
      new Color(192, 57, 43),
      new Color(142, 68, 173),
      new Color(22, 160, 133),
      new Color(243, 156, 18),
      new Color(127, 140, 141),
      new Color(211, 84, 0),
      new Color(44, 62, 80),
      new Color(241, 196, 15)
  };

  /**
   * Creates a new month view panel displaying a grid of days with events.
   * Initializes the calendar grid structure, sets up day cells, and prepares event display areas.
   * Sets the current month and selected date to today by default.
   *
   * @param calendarManager The manager providing access to calendar data
   * @param mainGUI The parent GUI for handling user interactions
   */
  public MonthPanel(CalendarManager calendarManager, CalendarGUI mainGUI) {
    this.calendarManager = calendarManager;
    this.mainGUI = mainGUI;
    this.currentMonthDate = LocalDate.now();
    this.selectedDate = LocalDate.now();

    setLayout(new BorderLayout());
    setBackground(Color.WHITE);

    dayLabels = new JLabel[ROWS][COLS];
    dayCells = new JPanel[ROWS][COLS];
    eventScrollPanes = new JScrollPane[ROWS * COLS];
    eventPanels = new JPanel[ROWS * COLS];

    JPanel headerPanel = createHeaderPanel();
    add(headerPanel, BorderLayout.NORTH);

    daysPanel = new JPanel(new GridLayout(ROWS, COLS, 2, 2));
    daysPanel.setBackground(new Color(230, 230, 230));
    daysPanel.setBorder(new EmptyBorder(2, 0, 0, 0));
    initializeDayCells();
    add(daysPanel, BorderLayout.CENTER);

    initializeCalendarColors();
  }

  private void initializeCalendarColors() {
    List<String> calendarNames = calendarManager.getCalendarNames();
    for (int i = 0; i < calendarNames.size(); i++) {
      calendarColors.put(calendarNames.get(i),
          CALENDAR_COLORS[i % CALENDAR_COLORS.length]);
    }
  }

  /**
   * Refreshes the calendar color mapping when calendars are added or removed.
   * Assigns a unique color to each calendar from the predefined color palette,
   *     cycling through colors if there are more calendars than colors.
   */
  public void updateCalendarColors() {
    calendarColors.clear();
    List<String> calendarNames = calendarManager.getCalendarNames();
    for (int i = 0; i < calendarNames.size(); i++) {
      calendarColors.put(calendarNames.get(i),
          CALENDAR_COLORS[i % CALENDAR_COLORS.length]);
    }
  }

  private JPanel createHeaderPanel() {
    JPanel headerPanel = new JPanel(new GridLayout(1, 7, 2, 0));
    headerPanel.setBackground(Color.WHITE);
    headerPanel.setBorder(new EmptyBorder(5, 0, 10, 0));

    // Days in order: Sunday...Saturday
    for (int i = 0; i < 7; i++) {
      String dayName = java.time.DayOfWeek.of(i == 0 ? 7 : i)
          .getDisplayName(TextStyle.SHORT, Locale.getDefault());

      JLabel dayLabel = new JLabel(dayName, SwingConstants.CENTER);
      dayLabel.setFont(HEADER_FONT);
      dayLabel.setBorder(new EmptyBorder(8, 0, 8, 0));
      if (i == 0 || i == 6) {
        dayLabel.setForeground(new Color(192, 57, 43));
      } else {
        dayLabel.setForeground(TEXT_COLOR);
      }
      headerPanel.add(dayLabel);
    }

    return headerPanel;
  }

  private void initializeDayCells() {
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        final int r = row;
        final int c = col;

        // Create the day cell container.
        dayCells[row][col] = new JPanel(new BorderLayout());
        dayCells[row][col].setBackground(Color.WHITE);
        dayCells[row][col].setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));

        // Create header panel to display the day number.
        JPanel dayHeaderPanel = new JPanel(new BorderLayout());
        dayHeaderPanel.setOpaque(false);
        dayHeaderPanel.setBorder(new EmptyBorder(6, 8, 2, 8));

        dayLabels[row][col] = new JLabel();
        dayLabels[row][col].setFont(DAY_NUMBER_FONT);
        dayHeaderPanel.add(dayLabels[row][col], BorderLayout.WEST);

        dayCells[row][col].add(dayHeaderPanel, BorderLayout.NORTH);

        // Create the event panel and add it inside a scroll pane.
        int index = row * COLS + col;
        eventPanels[index] = new JPanel();
        eventPanels[index].setLayout(new BoxLayout(eventPanels[index], BoxLayout.Y_AXIS));
        eventPanels[index].setOpaque(false);

        eventScrollPanes[index] = new JScrollPane(eventPanels[index]);
        eventScrollPanes[index].setBorder(null);
        eventScrollPanes[index].setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        eventScrollPanes[index].getViewport().setOpaque(false);
        eventScrollPanes[index].setOpaque(false);

        dayCells[row][col].add(eventScrollPanes[index], BorderLayout.CENTER);

        // Add a mouse listener directly to the day cell.
        dayCells[row][col].addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            handleDayCellClick(r, c);
            if (e.getClickCount() == 2) {
              handleDayCellDoubleClick(r, c);
            }
          }
        });

        // Add a mouse listener to the eventScrollPane to forward its events to the day cell.
        eventScrollPanes[index].addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            MouseEvent newEvent = SwingUtilities.convertMouseEvent(
                    eventScrollPanes[index], e, dayCells[r][c]);
            dayCells[r][c].dispatchEvent(newEvent);
          }
        });

        // Also add a listener to the viewport inside the scroll pane.
        eventScrollPanes[index].getViewport().addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            MouseEvent newEvent = SwingUtilities.convertMouseEvent(
                    eventScrollPanes[index].getViewport(), e, dayCells[r][c]);
            dayCells[r][c].dispatchEvent(newEvent);
          }
        });

        daysPanel.add(dayCells[row][col]);
      }
    }
  }

  /**
   * Updates the month view display based on the provided date.
   * Renders the calendar grid showing days of the current month,
   *     adjacent months' days where needed, and all events for each day.
   * Applies special styling for today, weekends, and the selected date.
   *
   * @param date The date to center the month view around
   */
  public void updateView(LocalDate date) {
    this.currentMonthDate = date.withDayOfMonth(1);

    YearMonth yearMonth = YearMonth.from(currentMonthDate);
    LocalDate firstDayOfMonth = yearMonth.atDay(1);
    int daysInMonth = yearMonth.lengthOfMonth();
    int firstDayOfWeekIndex = firstDayOfMonth.getDayOfWeek().getValue() % 7;

    YearMonth prevYearMonth = yearMonth.minusMonths(1);
    int daysInPrevMonth = prevYearMonth.lengthOfMonth();

    for (int i = 0; i < ROWS * COLS; i++) {
      eventPanels[i].removeAll();
    }

    int dayCounter = 1;
    int nextMonthDay = 1;

    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        JPanel cell = dayCells[row][col];
        JLabel label = dayLabels[row][col];
        int index = row * COLS + col;

        int cellIndex = row * 7 + col;
        LocalDate cellDate;
        boolean isCurrentMonth;

        if (cellIndex < firstDayOfWeekIndex) {
          int prevMonthDay = daysInPrevMonth - firstDayOfWeekIndex + cellIndex + 1;
          cellDate = prevYearMonth.atDay(prevMonthDay);
          isCurrentMonth = false;
        } else if (cellIndex < firstDayOfWeekIndex + daysInMonth) {
          dayCounter = cellIndex - firstDayOfWeekIndex + 1;
          cellDate = currentMonthDate.withDayOfMonth(dayCounter);
          isCurrentMonth = true;
        } else {
          cellDate = yearMonth.plusMonths(1).atDay(nextMonthDay);
          nextMonthDay++;
          isCurrentMonth = false;
        }

        label.setText(String.valueOf(cellDate.getDayOfMonth()));
        boolean isWeekend = (col == 0 || col == 6);
        boolean isToday = cellDate.equals(LocalDate.now());
        boolean isSelected = cellDate.equals(selectedDate);

        cell.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));

        if (isCurrentMonth) {
          if (isToday) {
            cell.setBackground(TODAY_BACKGROUND);
            cell.setBorder(BorderFactory.createLineBorder(TODAY_BORDER, 2));
            label.setForeground(PRIMARY_COLOR.darker());
          } else if (isSelected) {
            cell.setBackground(SELECTED_BACKGROUND);
            cell.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 2));
            label.setForeground(isWeekend ? new Color(192, 57, 43) : TEXT_COLOR);
          } else if (isWeekend) {
            cell.setBackground(WEEKEND_BACKGROUND);
            label.setForeground(new Color(192, 57, 43));
          } else {
            cell.setBackground(Color.WHITE);
            label.setForeground(TEXT_COLOR);
          }
        } else {
          cell.setBackground(NOT_MONTH_BACKGROUND);
          label.setForeground(isWeekend
              ? new Color(192, 57, 43, 150)
              : LIGHT_TEXT_COLOR);
        }

        loadEvents(cellDate, eventPanels[index]);
        eventPanels[index].revalidate();
        eventPanels[index].repaint();
      }
    }

    revalidate();
    repaint();
  }

  private void loadEvents(LocalDate date, JPanel eventPanel) {
    if (calendarManager.getCurrentCalendar() == null) {
      return;
    }

    LocalDateTime startOfDay = date.atStartOfDay();
    List<Event> events = calendarManager.getCurrentCalendar().getEventsOn(startOfDay);
    events.sort((e1, e2) -> e1.getStartDateTime().compareTo(e2.getStartDateTime()));

    int displayLimit = 3;
    int visibleCount = Math.min(events.size(), displayLimit);

    for (int i = 0; i < visibleCount; i++) {
      Event event = events.get(i);
      String timeLabel = event.isAllDay()
          ? "All day:"
          : formatTime(event.getStartDateTime()) + ":";

      String eventText = timeLabel + " " + event.getSubject();
      JPanel eventContainer = createRoundedEventPanel(event, eventText);

      final Event eventFinal = event;
      eventContainer.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          mainGUI.editEvent(eventFinal);
        }
      });

      eventPanel.add(eventContainer);
      eventPanel.add(Box.createVerticalStrut(2));
    }

    if (events.size() > displayLimit) {
      int moreCount = events.size() - displayLimit;
      JPanel morePanel = new JPanel(new BorderLayout());
      morePanel.setOpaque(false);

      JLabel moreLabel = new JLabel("+" + moreCount + " more");
      moreLabel.setFont(new Font("Segoe UI", Font.ITALIC, 10));
      moreLabel.setForeground(LIGHT_TEXT_COLOR);
      moreLabel.setHorizontalAlignment(SwingConstants.RIGHT);
      moreLabel.setBorder(new EmptyBorder(1, 1, 1, 5));

      morePanel.add(moreLabel, BorderLayout.EAST);
      eventPanel.add(morePanel);
    }
  }

  private JPanel createRoundedEventPanel(Event event, String eventText) {
    final Color eventColor = getEventTypeColor(event);
    final Color bgColor = getEventBackgroundColor(event);

    JPanel eventContainer = new JPanel(new BorderLayout()) {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(bgColor);
        g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));

        g2d.setColor(eventColor);
        g2d.fill(new RoundRectangle2D.Float(0, 0, 4, getHeight(), 2, 2));
        g2d.dispose();
        super.paintComponent(g);
      }
    };
    eventContainer.setOpaque(false);
    eventContainer.setBorder(new EmptyBorder(3, 6, 3, 3));

    JLabel eventLabel = new JLabel(eventText);
    eventLabel.setFont(EVENT_FONT);
    eventLabel.setForeground(TEXT_COLOR);

    eventContainer.add(eventLabel, BorderLayout.CENTER);
    return eventContainer;
  }

  private Color getEventTypeColor(Event event) {
    boolean isAllDay = event.isAllDay();
    boolean isRecurring = event.isRecurring();

    if (isAllDay && isRecurring) {
      return ALL_DAY_RECURRING_COLOR;
    } else if (isAllDay) {
      return ALL_DAY_EVENT_COLOR;
    } else if (isRecurring) {
      return RECURRING_EVENT_COLOR;
    } else {
      return SINGLE_EVENT_COLOR;
    }
  }

  private Color getEventBackgroundColor(Event event) {
    boolean isAllDay = event.isAllDay();
    boolean isRecurring = event.isRecurring();

    if (isAllDay && isRecurring) {
      return ALL_DAY_RECURRING_BG;
    } else if (isAllDay) {
      return ALL_DAY_EVENT_BG;
    } else if (isRecurring) {
      return RECURRING_EVENT_BG;
    } else {
      return SINGLE_EVENT_BG;
    }
  }

  private String formatTime(LocalDateTime dateTime) {
    return String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
  }

  private void handleDayCellClick(int row, int col) {
    YearMonth yearMonth = YearMonth.from(currentMonthDate);
    LocalDate firstDayOfMonth = yearMonth.atDay(1);
    int firstDayOfWeekIndex = firstDayOfMonth.getDayOfWeek().getValue() % 7;
    int cellIndex = row * 7 + col;
    LocalDate clickedDate;

    int daysInMonth = yearMonth.lengthOfMonth();
    YearMonth prevYearMonth = yearMonth.minusMonths(1);
    int daysInPrevMonth = prevYearMonth.lengthOfMonth();

    if (cellIndex < firstDayOfWeekIndex) {
      int prevMonthDay = daysInPrevMonth - firstDayOfWeekIndex + cellIndex + 1;
      clickedDate = prevYearMonth.atDay(prevMonthDay);
    } else if (cellIndex < firstDayOfWeekIndex + daysInMonth) {
      int dayOfMonth = cellIndex - firstDayOfWeekIndex + 1;
      clickedDate = currentMonthDate.withDayOfMonth(dayOfMonth);
    } else {
      int nextMonthDay = cellIndex - (firstDayOfWeekIndex + daysInMonth) + 1;
      clickedDate = yearMonth.plusMonths(1).atDay(nextMonthDay);
    }

    selectedDate = clickedDate;
    mainGUI.setDisplayDate(clickedDate);
  }

  private void handleDayCellDoubleClick(int row, int col) {
    handleDayCellClick(row, col);
    mainGUI.createNewEvent();
  }
}
