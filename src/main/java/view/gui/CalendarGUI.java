package view.gui;

import model.CalendarManager;
import model.Event;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Main GUI frame for the Calendar application.
 */
public class CalendarGUI extends JFrame {
  private CalendarManager calendarManager;
  private JPanel mainPanel;
  private JLabel monthYearLabel;
  private JLabel currentDateLabel;
  private MonthPanel monthView;
  private DayPanel dayView;
  private CalendarPanel calendarPanel;
  private JButton prevButton;
  private JButton nextButton;
  private JButton prevDayButton;
  private JButton nextDayButton;
  private JButton todayButton;
  private JButton newEventButton;
  private LocalDate currentDisplayDate;
  private SwingUI swingUI;
  private boolean isMonthView = true;

  /**
   * Constructor for the GUI.
   *
   * @param calendarManager the calendar manager to use
   */
  public CalendarGUI(CalendarManager calendarManager) {
    this.calendarManager = calendarManager;
    this.currentDisplayDate = LocalDate.now();

    // Set up the JFrame
    setTitle("Calendar Application");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setPreferredSize(new Dimension(1000, 700));

    createMenuBar();
    initializeComponents();

    // Make sure calendar panel is updated with existing calendars
    if (calendarPanel != null) {
      calendarPanel.refreshCalendars();
    }

    pack();
    setLocationRelativeTo(null); // Center on screen
  }

  /**
   * Sets the SwingUI instance for command execution.
   *
   * @param swingUI the SwingUI instance
   */
  public void setSwingUI(SwingUI swingUI) {
    this.swingUI = swingUI;
    if (calendarPanel != null) {
      calendarPanel.refreshCalendars();
      refreshView();
    }
  }

  /**
   * Creates the menu bar for the application.
   */
  private void createMenuBar() {
    JMenuBar menuBar = new JMenuBar();

    // File menu
    JMenu fileMenu = new JMenu("File");
    JMenuItem importItem = new JMenuItem("Import from CSV...");
    importItem.addActionListener(e -> importCalendar());
    fileMenu.add(importItem);

    JMenuItem exportItem = new JMenuItem("Export Calendar...");
    exportItem.addActionListener(e -> exportCalendar());
    fileMenu.add(exportItem);
    fileMenu.addSeparator();
    JMenuItem exitItem = new JMenuItem("Exit");
    exitItem.addActionListener(e -> System.exit(0));
    fileMenu.add(exitItem);

    // Calendar menu
    JMenu calendarMenu = new JMenu("Calendar");
    JMenuItem newCalItem = new JMenuItem("New Calendar...");
    newCalItem.addActionListener(e -> createNewCalendar());
    calendarMenu.add(newCalItem);
    JMenuItem editCalItem = new JMenuItem("Edit Calendar...");
    editCalItem.addActionListener(e -> editCalendar());
    calendarMenu.add(editCalItem);

    // Event menu
    JMenu eventMenu = new JMenu("Event");
    JMenuItem newEventItem = new JMenuItem("New Event...");
    newEventItem.addActionListener(e -> createNewEvent());
    eventMenu.add(newEventItem);

    // View menu
    JMenu viewMenu = new JMenu("View");
    JMenuItem todayItem = new JMenuItem("Go to Today");
    todayItem.addActionListener(e -> goToToday());
    viewMenu.add(todayItem);

    JMenuItem monthItem = new JMenuItem("Month View");
    monthItem.addActionListener(e -> switchToMonthView());
    viewMenu.add(monthItem);

    JMenuItem dayItem = new JMenuItem("Day View");
    dayItem.addActionListener(e -> switchToDayView());
    viewMenu.add(dayItem);

    // Help menu
    JMenu helpMenu = new JMenu("Help");
    JMenuItem aboutItem = new JMenuItem("About");
    aboutItem.addActionListener(e -> showAboutDialog());
    helpMenu.add(aboutItem);

    menuBar.add(fileMenu);
    menuBar.add(calendarMenu);
    menuBar.add(eventMenu);
    menuBar.add(viewMenu);
    menuBar.add(helpMenu);

    setJMenuBar(menuBar);
  }

  /**
   * Initializes all UI components.
   */
  private void initializeComponents() {
    mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Top control panel
    JPanel controlPanel = new JPanel(new BorderLayout());
    JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    // Month navigation
    prevButton = new JButton("<<");
    prevButton.setToolTipText("Previous Month");
    prevButton.addActionListener(e -> navigateToPreviousMonth());

    monthYearLabel = new JLabel("", SwingConstants.CENTER);
    updateMonthYearLabel();

    nextButton = new JButton(">>");
    nextButton.setToolTipText("Next Month");
    nextButton.addActionListener(e -> navigateToNextMonth());

    // Day navigation buttons
    prevDayButton = new JButton("<<");
    prevDayButton.setToolTipText("Previous Day");
    prevDayButton.addActionListener(e -> navigateToPreviousDay());

    // Current date label - initially hidden
    currentDateLabel = new JLabel();
    currentDateLabel.setBorder(new EmptyBorder(0, 10, 0, 10)); // Add padding
    updateCurrentDateLabel(); // Initialize with current date
    currentDateLabel.setVisible(false); // Hidden initially

    nextDayButton = new JButton(">>");
    nextDayButton.setToolTipText("Next Day");
    nextDayButton.addActionListener(e -> navigateToNextDay());

    todayButton = new JButton("Today");
    todayButton.addActionListener(e -> goToToday());

    // Month navigation
    navigationPanel.add(prevButton);
    navigationPanel.add(monthYearLabel);
    navigationPanel.add(nextButton);

    // Add some space between month and day navigation
    navigationPanel.add(Box.createHorizontalStrut(20));

    // Day navigation
    navigationPanel.add(prevDayButton);
    navigationPanel.add(currentDateLabel);
    navigationPanel.add(nextDayButton);

    navigationPanel.add(Box.createHorizontalStrut(20));
    navigationPanel.add(todayButton);

    controlPanel.add(navigationPanel, BorderLayout.WEST);

    // Bottom button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    newEventButton = new JButton("New Event");
    newEventButton.addActionListener(e -> createNewEvent());
    buttonPanel.add(newEventButton);

    // Create the calendar views
    monthView = new MonthPanel(calendarManager, this);
    dayView = new DayPanel(calendarManager, this);

    // Create calendar panel
    calendarPanel = new CalendarPanel(calendarManager, this);

    // Add panels to main panel
    mainPanel.add(controlPanel, BorderLayout.NORTH);
    mainPanel.add(calendarPanel, BorderLayout.CENTER);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    // Add to frame
    setContentPane(mainPanel);

    // Set initial view (month view)
    setViewMode(true);
  }

  /**
   * Updates the month/year label based on the current display date.
   */
  private void updateMonthYearLabel() {
    String month = currentDisplayDate.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
    int year = currentDisplayDate.getYear();
    monthYearLabel.setText(month + " " + year);
  }

  /**
   * Updates the current date label based on the current display date.
   */
  private void updateCurrentDateLabel() {
    // Format the date as "Day, Month DD, YYYY" (e.g., "Monday, April 09, 2025")
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    currentDateLabel.setText(currentDisplayDate.format(formatter));
  }

  /**
   * Refreshes the current calendar view.
   */
  public void refreshView() {
    calendarPanel.updateView(currentDisplayDate);
  }

  /**
   * Navigates to the previous month.
   */
  private void navigateToPreviousMonth() {
    currentDisplayDate = currentDisplayDate.minusMonths(1);
    updateMonthYearLabel();
    updateCurrentDateLabel();
    refreshView();
  }

  /**
   * Navigates to the next month.
   */
  private void navigateToNextMonth() {
    currentDisplayDate = currentDisplayDate.plusMonths(1);
    updateMonthYearLabel();
    updateCurrentDateLabel();
    refreshView();
  }

  /**
   * Navigates to the previous day (for day view).
   */
  private void navigateToPreviousDay() {
    currentDisplayDate = currentDisplayDate.minusDays(1);
    updateMonthYearLabel();
    updateCurrentDateLabel();
    refreshView();
  }

  /**
   * Navigates to the next day (for day view).
   */
  private void navigateToNextDay() {
    currentDisplayDate = currentDisplayDate.plusDays(1);
    updateMonthYearLabel();
    updateCurrentDateLabel();
    refreshView();
  }

  /**
   * Navigates to today.
   */
  private void goToToday() {
    currentDisplayDate = LocalDate.now();
    updateMonthYearLabel();
    updateCurrentDateLabel();
    refreshView();
  }

  /**
   * Switches to the month view.
   */
  private void switchToMonthView() {
    if (calendarPanel != null) {
      calendarPanel.switchToMonthView();
    }
  }

  /**
   * Switches to the day view.
   */
  private void switchToDayView() {
    if (calendarPanel != null) {
      calendarPanel.switchToDayView();
    }
  }

  /**
   * Set the current view mode.
   * This method is called by CalendarPanel when the view is changed.
   *
   * @param isMonthView true if month view, false if day view
   */
  public void setViewMode(boolean isMonthView) {
    this.isMonthView = isMonthView;

    // In day view, show the day navigation buttons and currentDateLabel
    // In month view, hide them
    prevDayButton.setVisible(!isMonthView);
    nextDayButton.setVisible(!isMonthView);
    currentDateLabel.setVisible(!isMonthView);
  }

  /**
   * Opens the create new event dialog.
   */
  public void createNewEvent() {
    EventDialog dialog = new EventDialog(this, calendarManager, currentDisplayDate);
    dialog.setVisible(true);

    // After dialog closes, refresh the view
    if (dialog.isEventCreated()) {
      refreshView();
    }
  }

  /**
   * Opens the create new event dialog with a specific start time.
   *
   * @param dateTime the date and time for the new event
   */
  public void createNewEvent(LocalDateTime dateTime) {
    EventDialog dialog = new EventDialog(this, calendarManager, dateTime);
    dialog.setVisible(true);

    // After dialog closes, refresh the view
    if (dialog.isEventCreated()) {
      refreshView();
    }
  }

  /**
   * Opens the edit event dialog for an existing event.
   *
   * @param event the event to edit
   */
  public void editEvent(Event event) {
    EventDialog dialog = new EventDialog(this, calendarManager, event);
    dialog.setVisible(true);

    // After dialog closes, refresh the view
    if (dialog.isEventCreated()) {
      refreshView();
    }
  }

  /**
   * Opens the create new calendar dialog.
   */
  private void createNewCalendar() {
    if (calendarPanel != null) {
      calendarPanel.createNewCalendar();
    }
  }

  /**
   * Opens the edit calendar dialog.
   */
  private void editCalendar() {
    if (calendarPanel != null) {
      calendarPanel.editCalendar();
    }
  }

  /**
   * Opens the export calendar dialog.
   */
  private void exportCalendar() {
    if (calendarPanel != null) {
      calendarPanel.exportToCSV();
    }
  }

  /**
   * Opens the import calendar dialog.
   */
  private void importCalendar() {
    if (calendarPanel != null) {
      calendarPanel.importFromCSV();
    }
  }

  /**
   * Shows the about dialog.
   */
  private void showAboutDialog() {
    JOptionPane.showMessageDialog(this,
            "Calendar Application\nVersion 1.0\n\nA Java Swing calendar application.",
            "About",
            JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Changes the display date.
   *
   * @param date the new date to display
   */
  public void setDisplayDate(LocalDate date) {
    this.currentDisplayDate = date;
    updateMonthYearLabel();
    updateCurrentDateLabel();
    refreshView();
  }

  /**
   * Gets the current display date.
   *
   * @return the current display date
   */
  public LocalDate getDisplayDate() {
    return currentDisplayDate;
  }

  /**
   * Executes a command via the SwingUI.
   *
   * @param command the command to execute
   */
  public void executeCommand(String command) {
    if (swingUI != null) {
      swingUI.addCommand(command);
    } else {
      System.err.println("SwingUI not set. Cannot execute command: " + command);
    }
  }

  /**
   * Gets the month view panel.
   *
   * @return the month view panel
   */
  public MonthPanel getMonthView() {
    return monthView;
  }

  /**
   * Gets the day view panel.
   *
   * @return the day view panel
   */
  public DayPanel getDayView() {
    return dayView;
  }
}