package view.gui;

import model.Calendar;
import model.CalendarManager;
import model.Event;
import model.EventImpl;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Panel for displaying a month view of the calendar.
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

  // Calendar color mapping
  private Map<String, Color> calendarColors = new HashMap<>();

  // Constants for rendering
  private static final int ROWS = 6; // Max 6 weeks in a month view
  private static final int COLS = 7; // 7 days in a week
  private static final Color TODAY_BACKGROUND = new Color(230, 240, 255);
  private static final Color WEEKEND_BACKGROUND = new Color(245, 245, 245);
  private static final Color SELECTED_BACKGROUND = new Color(220, 230, 255);
  private static final Color NOT_MONTH_BACKGROUND = new Color(240, 240, 240);

  // Event type colors
  private static final Color ALL_DAY_EVENT_COLOR = new Color(76, 175, 80);   // Green
  private static final Color SINGLE_EVENT_COLOR = new Color(33, 150, 243);   // Blue
  private static final Color RECURRING_EVENT_COLOR = new Color(255, 152, 0); // Orange
  private static final Color ALL_DAY_RECURRING_COLOR = new Color(138, 43, 226); // Purple


  // Event background colors (lighter versions)
  private static final Color ALL_DAY_EVENT_BG = new Color(76, 175, 80, 50);   // Light Green
  private static final Color SINGLE_EVENT_BG = new Color(33, 150, 243, 50);   // Light Blue
  private static final Color RECURRING_EVENT_BG = new Color(255, 152, 0, 50); // Light Orange
  private static final Color ALL_DAY_RECURRING_BG = new Color(138, 43, 226, 50); // Light purple

  // Predefined colors for different calendars
  private static final Color[] CALENDAR_COLORS = {
      new Color(30, 144, 255),   // DodgerBlue
      new Color(50, 205, 50),    // LimeGreen
      new Color(255, 69, 0),     // OrangeRed
      new Color(148, 0, 211),    // DarkViolet
      new Color(0, 191, 255),    // DeepSkyBlue
      new Color(255, 140, 0),    // DarkOrange
      new Color(220, 20, 60),    // Crimson
      new Color(0, 128, 128),    // Teal
      new Color(255, 215, 0),    // Gold
      new Color(75, 0, 130)      // Indigo
  };

  /**
   * Constructor for the month view panel.
   *
   * @param calendarManager the calendar manager to use
   * @param mainGUI the main GUI frame
   */
  public MonthPanel(CalendarManager calendarManager, CalendarGUI mainGUI) {
    this.calendarManager = calendarManager;
    this.mainGUI = mainGUI;
    this.currentMonthDate = LocalDate.now();

    setLayout(new BorderLayout());

    // Initialize arrays
    dayLabels = new JLabel[ROWS][COLS];
    dayCells = new JPanel[ROWS][COLS];
    eventScrollPanes = new JScrollPane[ROWS * COLS];
    eventPanels = new JPanel[ROWS * COLS];

    // Create header panel with day names
    JPanel headerPanel = createHeaderPanel();
    add(headerPanel, BorderLayout.NORTH);

    // Create panel for calendar days
    daysPanel = new JPanel(new GridLayout(ROWS, COLS));
    initializeDayCells();
    add(daysPanel, BorderLayout.CENTER);

    // Initialize calendar colors
    initializeCalendarColors();
  }

  /**
   * Initializes the color mapping for calendars.
   */
  private void initializeCalendarColors() {
    List<String> calendarNames = calendarManager.getCalendarNames();
    for (int i = 0; i < calendarNames.size(); i++) {
      calendarColors.put(calendarNames.get(i),
          CALENDAR_COLORS[i % CALENDAR_COLORS.length]);
    }
  }

  /**
   * Updates the color mapping when calendars are added or removed.
   */
  public void updateCalendarColors() {
    // Clear existing mappings
    calendarColors.clear();

    // Recreate mappings
    List<String> calendarNames = calendarManager.getCalendarNames();
    for (int i = 0; i < calendarNames.size(); i++) {
      calendarColors.put(calendarNames.get(i),
          CALENDAR_COLORS[i % CALENDAR_COLORS.length]);
    }
  }

  /**
   * Creates the header panel with day names.
   */
  private JPanel createHeaderPanel() {
    JPanel headerPanel = new JPanel(new GridLayout(1, 7));
    headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

    // Days in order: Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
    String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    boolean[] isWeekendDay = {true, false, false, false, false, false, true};

    for (int i = 0; i < 7; i++) {
      JLabel dayLabel = new JLabel(dayNames[i]);
      dayLabel.setHorizontalAlignment(SwingConstants.CENTER);

      // Set weekend days to red (Sunday and Saturday)
      if (isWeekendDay[i]) {
        dayLabel.setForeground(Color.RED);
      } else {
        dayLabel.setForeground(Color.BLACK);
      }

      dayLabel.setFont(dayLabel.getFont().deriveFont(Font.BOLD));
      headerPanel.add(dayLabel);
    }

    return headerPanel;
  }

  /**
   * Initializes the day cells in the calendar grid.
   */
  private void initializeDayCells() {
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        final int r = row;
        final int c = col;

        dayCells[row][col] = new JPanel(new BorderLayout());
        dayCells[row][col].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        // Day number label at top
        dayLabels[row][col] = new JLabel();
        dayLabels[row][col].setBorder(new EmptyBorder(2, 5, 2, 5));
        dayLabels[row][col].setFont(dayLabels[row][col].getFont().deriveFont(Font.BOLD));
        dayCells[row][col].add(dayLabels[row][col], BorderLayout.NORTH);

        // Panel for events
        int index = row * COLS + col;
        eventPanels[index] = new JPanel();
        eventPanels[index].setLayout(new BoxLayout(eventPanels[index], BoxLayout.Y_AXIS));

        eventScrollPanes[index] = new JScrollPane(eventPanels[index]);
        eventScrollPanes[index].setBorder(null);
        eventScrollPanes[index].setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        dayCells[row][col].add(eventScrollPanes[index], BorderLayout.CENTER);

        // Add click listener to select day
        dayCells[row][col].addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            handleDayCellClick(r, c);
            if (e.getClickCount() == 2) {
              handleDayCellDoubleClick(r, c);
            }
          }
        });

        daysPanel.add(dayCells[row][col]);
      }
    }
  }

  /**
   * Updates the view to show the specified month.
   */
  public void updateView(LocalDate date) {
    this.currentMonthDate = date.withDayOfMonth(1); // First day of month

    YearMonth yearMonth = YearMonth.from(currentMonthDate);
    LocalDate firstDayOfMonth = yearMonth.atDay(1);
    int daysInMonth = yearMonth.lengthOfMonth();

    // Find what day of the week the 1st falls on (0=Sunday, 1=Monday, ..., 6=Saturday)
    // Java's DayOfWeek: 1=Monday, ..., 7=Sunday, so we need to convert
    int firstDayOfWeekIndex = firstDayOfMonth.getDayOfWeek().getValue() % 7;

    // Find days from previous month to display
    YearMonth prevYearMonth = yearMonth.minusMonths(1);
    int daysInPrevMonth = prevYearMonth.lengthOfMonth();

    // Clear all day cells
    for (int i = 0; i < ROWS * COLS; i++) {
      eventPanels[i].removeAll();
    }

    // Update day cells
    int dayCounter = 1;
    int nextMonthDay = 1;

    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        JPanel cell = dayCells[row][col];
        JLabel label = dayLabels[row][col];
        int index = row * COLS + col;

        // Calculate what date this cell represents
        LocalDate cellDate;
        boolean isCurrentMonth;

        int cellIndex = row * 7 + col;

        if (cellIndex < firstDayOfWeekIndex) {
          // Previous month
          int prevMonthDay = daysInPrevMonth - firstDayOfWeekIndex + cellIndex + 1;
          cellDate = prevYearMonth.atDay(prevMonthDay);
          isCurrentMonth = false;
        } else if (cellIndex < firstDayOfWeekIndex + daysInMonth) {
          // Current month
          dayCounter = cellIndex - firstDayOfWeekIndex + 1;
          cellDate = currentMonthDate.withDayOfMonth(dayCounter);
          isCurrentMonth = true;
        } else {
          // Next month
          cellDate = yearMonth.plusMonths(1).atDay(nextMonthDay);
          nextMonthDay++;
          isCurrentMonth = false;
        }

        // Set the day number
        label.setText(String.valueOf(cellDate.getDayOfMonth()));

        // Set colors based on whether day is in current month and if it's a weekend
        boolean isWeekend = col == 0 || col == 6; // Sunday or Saturday

        if (isCurrentMonth) {
          boolean isToday = cellDate.equals(LocalDate.now());

          if (isToday) {
            cell.setBackground(TODAY_BACKGROUND);
          } else if (isWeekend) {
            cell.setBackground(WEEKEND_BACKGROUND);
          } else {
            cell.setBackground(Color.WHITE);
          }

          // Only color weekend days red
          if (isWeekend) {
            label.setForeground(Color.RED);
          } else {
            label.setForeground(Color.BLACK);
          }
        } else {
          // Days not in current month
          cell.setBackground(NOT_MONTH_BACKGROUND);

          // Only color weekend days in light red, others in gray
          if (isWeekend) {
            label.setForeground(new Color(255, 150, 150)); // Light red
          } else {
            label.setForeground(Color.GRAY);
          }
        }

        // Load events for this day
        loadEvents(cellDate, eventPanels[index]);

        // Update UI
        eventPanels[index].revalidate();
        eventPanels[index].repaint();
      }
    }

    // Update our parent panel
    revalidate();
    repaint();
  }

  /**
   * Loads events for a specific day into the provided panel.
   */
  private void loadEvents(LocalDate date, JPanel eventPanel) {
    if (calendarManager.getCurrentCalendar() == null) {
      return;
    }

    LocalDateTime startOfDay = date.atStartOfDay();
    List<Event> events = calendarManager.getCurrentCalendar().getEventsOn(startOfDay);

    // Sort events by start time
    events.sort((e1, e2) -> e1.getStartDateTime().compareTo(e2.getStartDateTime()));

    // Limit to 3 visible events to avoid overcrowding
    int displayLimit = 3;
    int visibleCount = Math.min(events.size(), displayLimit);

    for (int i = 0; i < visibleCount; i++) {
      Event event = events.get(i);
      String timeLabel = event.isAllDay() ? "All day:" :
          formatTime(event.getStartDateTime()) + ":";

      String eventText = timeLabel + " " + event.getSubject();
      JLabel eventLabel = new JLabel(eventText);

      // Get the appropriate event color based on type
      Color eventColor = getEventTypeColor(event);
      Color bgColor = getEventBackgroundColor(event);

      eventLabel.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createMatteBorder(0, 3, 0, 0, eventColor),
          BorderFactory.createEmptyBorder(2, 2, 2, 2)
      ));
      eventLabel.setFont(eventLabel.getFont().deriveFont(10.0f));

      JPanel eventContainer = new JPanel(new BorderLayout());
      eventContainer.add(eventLabel, BorderLayout.CENTER);

      // Set background based on event type
      eventContainer.setBackground(bgColor);
      eventContainer.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

      // Add click listener to open event
      final Event eventFinal = event;
      eventContainer.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          mainGUI.editEvent(eventFinal);
        }
      });

      eventPanel.add(eventContainer);
    }

    // If there are more events than we're showing, add a "more" indicator
    if (events.size() > displayLimit) {
      int moreCount = events.size() - displayLimit;
      JLabel moreLabel = new JLabel("+" + moreCount + " more");
      moreLabel.setFont(moreLabel.getFont().deriveFont(9.0f));
      moreLabel.setForeground(Color.GRAY);
      moreLabel.setHorizontalAlignment(SwingConstants.RIGHT);
      moreLabel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 5));
      eventPanel.add(moreLabel);
    }
  }

  /**
   * Determines the color for an event based on its type.
   */
  private Color getEventTypeColor(Event event) {
    // Check for combination of properties
    boolean isAllDay = event.isAllDay();
    boolean isRecurring = event.isRecurring();

    if (isAllDay && isRecurring) {
      // All-day recurring events (purple)
      return ALL_DAY_RECURRING_COLOR;
    } else if (isAllDay) {
      // All-day non-recurring (green)
      return ALL_DAY_EVENT_COLOR;
    } else if (isRecurring) {
      // Recurring non-all-day (orange)
      return RECURRING_EVENT_COLOR;
    } else {
      // Regular single event (blue)
      return SINGLE_EVENT_COLOR;
    }
  }

  /**
   * Gets a background color for an event based on its type.
   */
  private Color getEventBackgroundColor(Event event) {
    // Check for combination of properties
    boolean isAllDay = event.isAllDay();
    boolean isRecurring = event.isRecurring();

    if (isAllDay && isRecurring) {
      // All-day recurring events (light purple)
      return ALL_DAY_RECURRING_BG;
    } else if (isAllDay) {
      // All-day non-recurring (light green)
      return ALL_DAY_EVENT_BG;
    } else if (isRecurring) {
      // Recurring non-all-day (light orange)
      return RECURRING_EVENT_BG;
    } else {
      // Regular single event (light blue)
      return SINGLE_EVENT_BG;
    }
  }

  /**
   * Formats time for display.
   */
  private String formatTime(LocalDateTime dateTime) {
    return String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
  }

  /**
   * Handles clicking on a day cell.
   */
  private void handleDayCellClick(int row, int col) {
    // Calculate the date of the clicked cell
    YearMonth yearMonth = YearMonth.from(currentMonthDate);
    LocalDate firstDayOfMonth = yearMonth.atDay(1);

    // Find what day of the week the 1st falls on (0=Sunday, 1=Monday, ..., 6=Saturday)
    // Adjusted for Sunday as first day of week
    int firstDayOfWeekIndex = firstDayOfMonth.getDayOfWeek().getValue() % 7;

    int cellIndex = row * 7 + col;
    LocalDate selectedDate;

    if (cellIndex < firstDayOfWeekIndex) {
      // Previous month
      YearMonth prevYearMonth = yearMonth.minusMonths(1);
      int daysInPrevMonth = prevYearMonth.lengthOfMonth();
      int prevMonthDay = daysInPrevMonth - firstDayOfWeekIndex + cellIndex + 1;
      selectedDate = prevYearMonth.atDay(prevMonthDay);
    } else {
      int daysInMonth = yearMonth.lengthOfMonth();
      if (cellIndex < firstDayOfWeekIndex + daysInMonth) {
        // Current month
        int dayOfMonth = cellIndex - firstDayOfWeekIndex + 1;
        selectedDate = currentMonthDate.withDayOfMonth(dayOfMonth);
      } else {
        // Next month
        int nextMonthDay = cellIndex - (firstDayOfWeekIndex + daysInMonth) + 1;
        selectedDate = yearMonth.plusMonths(1).atDay(nextMonthDay);
      }
    }

    mainGUI.setDisplayDate(selectedDate);
  }

  /**
   * Handles double-clicking on a day cell.
   */
  private void handleDayCellDoubleClick(int row, int col) {
    // Double-click should create a new event on this day
    // First, set the current date
    handleDayCellClick(row, col);

    // Then open the new event dialog
    mainGUI.createNewEvent();
  }
}