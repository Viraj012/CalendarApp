package view.gui;

import model.Calendar;
import model.CalendarManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

// And make sure you don't have this import
// import java.awt.List;

/**
 * Panel that represents a calendar, including its events and controls.
 * This panel contains both the month view and day view and manages switching between them.
 */
public class CalendarPanel extends JPanel {
  private CalendarManager calendarManager;
  private CalendarGUI mainGUI;
  private JComboBox<String> calendarSelector;
  private JPanel viewPanel;
  private JToggleButton monthViewButton;
  private JToggleButton dayViewButton;
  private JLabel currentCalendarLabel;
  private JButton exportButton;
  private JButton importButton;

  /**
   * Constructor for CalendarPanel.
   *
   * @param calendarManager the calendar manager
   * @param mainGUI the main GUI frame
   */
  public CalendarPanel(CalendarManager calendarManager, CalendarGUI mainGUI) {
    this.calendarManager = calendarManager;
    this.mainGUI = mainGUI;

    setLayout(new BorderLayout(5, 5));
    setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create control panel (top)
    JPanel controlPanel = createControlPanel();
    add(controlPanel, BorderLayout.NORTH);

    // Create view panel (center)
    viewPanel = new JPanel(new BorderLayout());

    add(viewPanel, BorderLayout.CENTER);

    // Set default view to month view
    switchToMonthView();

    // Create import/export panel (bottom)
    JPanel importExportPanel = createImportExportPanel();
    add(importExportPanel, BorderLayout.SOUTH);
  }

  /**
   * Creates the control panel with calendar selection and view toggle.
   */
  private JPanel createControlPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(0, 0, 10, 0));

    // Calendar selection panel (left side)
    JPanel calendarSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    currentCalendarLabel = new JLabel("Calendar: ");
    calendarSelectionPanel.add(currentCalendarLabel);

    calendarSelector = new JComboBox<>();
    calendarSelector.setPreferredSize(new Dimension(150, 25));
    calendarSelector.addActionListener(e -> changeCalendar());
    calendarSelectionPanel.add(calendarSelector);

    JButton newCalendarButton = new JButton("New Calendar");
    newCalendarButton.addActionListener(e -> createNewCalendar());
    calendarSelectionPanel.add(newCalendarButton);

    JButton editCalendarButton = new JButton("Edit Calendar");
    editCalendarButton.addActionListener(e -> editCalendar());
    calendarSelectionPanel.add(editCalendarButton);

    panel.add(calendarSelectionPanel, BorderLayout.WEST);

    // View toggle panel (right side)
    JPanel viewTogglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    ButtonGroup viewGroup = new ButtonGroup();

    monthViewButton = new JToggleButton("Month View");
    monthViewButton.setSelected(true);
    monthViewButton.addActionListener(e -> switchToMonthView());
    viewGroup.add(monthViewButton);
    viewTogglePanel.add(monthViewButton);

    dayViewButton = new JToggleButton("Day View");
    dayViewButton.addActionListener(e -> switchToDayView());
    viewGroup.add(dayViewButton);
    viewTogglePanel.add(dayViewButton);

    panel.add(viewTogglePanel, BorderLayout.EAST);

    return panel;
  }

  /**
   * Creates the import/export panel.
   */
  private JPanel createImportExportPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.setBorder(new EmptyBorder(10, 0, 0, 0));

    importButton = new JButton("Import from CSV");
    importButton.addActionListener(e -> importFromCSV());
    panel.add(importButton);

    exportButton = new JButton("Export to CSV");
    exportButton.addActionListener(e -> exportToCSV());
    panel.add(exportButton);

    return panel;
  }

  /**
   * Switches the view to month view.
   */
  public void switchToMonthView() {
    viewPanel.removeAll();
    viewPanel.add(mainGUI.getMonthView(), BorderLayout.CENTER);
    monthViewButton.setSelected(true);
    updateView(mainGUI.getDisplayDate());
    mainGUI.setViewMode(true);  // Set month view mode
    viewPanel.revalidate();
    viewPanel.repaint();
  }

  /**
   * Switches the view to day view.
   */
  public void switchToDayView() {
    viewPanel.removeAll();
    viewPanel.add(mainGUI.getDayView(), BorderLayout.CENTER);
    dayViewButton.setSelected(true);
    updateView(mainGUI.getDisplayDate());
    mainGUI.setViewMode(false);  // Set day view mode
    viewPanel.revalidate();
    viewPanel.repaint();
  }

  /**
   * Updates the view to show the specified date.
   */
  public void updateView(LocalDate date) {
    if (monthViewButton.isSelected()) {
      mainGUI.getMonthView().updateView(date);
    } else {
      mainGUI.getDayView().updateView(date);
    }
  }

  /**
   * Refreshes the list of available calendars.
   */
  public void refreshCalendars() {
    // Save the currently selected calendar
    String selectedCalendar = (calendarSelector.getSelectedItem() != null) ?
        calendarSelector.getSelectedItem().toString() : null;

    calendarSelector.removeAllItems();

    // Add all calendars
    List<String> calendarNames = calendarManager.getCalendarNames();
    for (String calName : calendarNames) {
      calendarSelector.addItem(calName);
    }

    // Try to select the previously selected calendar or the current calendar
    Calendar currentCal = calendarManager.getCurrentCalendar();
    if (currentCal != null && calendarManager.calendarExists(currentCal.getName())) {
      calendarSelector.setSelectedItem(currentCal.getName());
    } else if (selectedCalendar != null && calendarManager.calendarExists(selectedCalendar)) {
      calendarSelector.setSelectedItem(selectedCalendar);
    } else if (calendarSelector.getItemCount() > 0) {
      calendarSelector.setSelectedIndex(0);
      // Use the first calendar
      calendarManager.useCalendar((String) calendarSelector.getSelectedItem());
    }

    updateCurrentCalendarLabel();
    updateView(mainGUI.getDisplayDate());
  }

  /**
   * Changes the current calendar.
   */
  private void changeCalendar() {
    if (calendarSelector.getSelectedItem() != null) {
      String calName = calendarSelector.getSelectedItem().toString();
      calendarManager.useCalendar(calName);
      updateCurrentCalendarLabel();
      updateView(mainGUI.getDisplayDate());
    }
  }

  /**
   * Updates the current calendar label.
   */
  private void updateCurrentCalendarLabel() {
    Calendar currentCal = calendarManager.getCurrentCalendar();
    if (currentCal != null) {
      String name = currentCal.getName();
      ZoneId timezone = currentCal.getTimezone();
      currentCalendarLabel.setText("Calendar: " + name + " (" + timezone + ")");
    } else {
      currentCalendarLabel.setText("Calendar: None");
    }
  }

  /**
   * Opens the create new calendar dialog.
   */
  public void createNewCalendar() {
    JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

    JTextField nameField = new JTextField(20);

    String[] timezoneIds = ZoneId.getAvailableZoneIds().toArray(new String[0]);
    JComboBox<String> timezoneCombo = new JComboBox<>(timezoneIds);
    timezoneCombo.setSelectedItem(ZoneId.systemDefault().toString());

    panel.add(new JLabel("Calendar Name:"));
    panel.add(nameField);
    panel.add(new JLabel("Timezone:"));
    panel.add(timezoneCombo);

    int result = JOptionPane.showConfirmDialog(
        this,
        panel,
        "Create New Calendar",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);

    if (result == JOptionPane.OK_OPTION) {
      String name = nameField.getText().trim();
      String timezoneId = (String) timezoneCombo.getSelectedItem();

      if (name.isEmpty()) {
        JOptionPane.showMessageDialog(
            this,
            "Calendar name cannot be empty",
            "Error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      ZoneId timezone = ZoneId.of(timezoneId);
      boolean created = calendarManager.createCalendar(name, timezone);

      if (created) {
        JOptionPane.showMessageDialog(
            this,
            "Calendar created successfully!",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
        refreshCalendars();
        calendarManager.useCalendar(name);
        updateCurrentCalendarLabel();
        updateView(mainGUI.getDisplayDate());
      } else {
        JOptionPane.showMessageDialog(
            this,
            "Failed to create calendar. Name may already exist.",
            "Error",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /**
   * Opens the edit calendar dialog.
   */
  public void editCalendar() {
    Calendar currentCal = calendarManager.getCurrentCalendar();
    if (currentCal == null) {
      JOptionPane.showMessageDialog(
          this,
          "No calendar selected",
          "Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    String currentName = currentCal.getName();

    JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

    String[] properties = {"name", "timezone"};
    JComboBox<String> propertyCombo = new JComboBox<>(properties);

    JTextField valueField = new JTextField(20);
    JComboBox<String> timezoneCombo = new JComboBox<>(ZoneId.getAvailableZoneIds().toArray(new String[0]));
    timezoneCombo.setSelectedItem(currentCal.getTimezone().toString());

    JPanel valuePanel = new JPanel(new CardLayout());
    valuePanel.add(valueField, "name");
    valuePanel.add(timezoneCombo, "timezone");

    propertyCombo.addActionListener(e -> {
      String property = (String) propertyCombo.getSelectedItem();
      CardLayout cl = (CardLayout) valuePanel.getLayout();
      cl.show(valuePanel, property);
    });

    panel.add(new JLabel("Property:"));
    panel.add(propertyCombo);
    panel.add(new JLabel("New Value:"));
    panel.add(valuePanel);

    int result = JOptionPane.showConfirmDialog(
        this,
        panel,
        "Edit Calendar: " + currentName,
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);

    if (result == JOptionPane.OK_OPTION) {
      String property = (String) propertyCombo.getSelectedItem();
      String newValue;

      if (property.equals("timezone")) {
        newValue = (String) timezoneCombo.getSelectedItem();
      } else {
        newValue = valueField.getText().trim();

        if (newValue.isEmpty()) {
          JOptionPane.showMessageDialog(
              this,
              "Value cannot be empty",
              "Error",
              JOptionPane.ERROR_MESSAGE);
          return;
        }
      }

      boolean success = calendarManager.editCalendar(currentName, property, newValue);

      if (success) {
        JOptionPane.showMessageDialog(
            this,
            "Calendar updated successfully!",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
        refreshCalendars();
      } else {
        JOptionPane.showMessageDialog(
            this,
            "Failed to update calendar",
            "Error",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /**
   * Exports the current calendar to a CSV file.
   */
  public void exportToCSV() {
    Calendar currentCal = calendarManager.getCurrentCalendar();
    if (currentCal == null) {
      JOptionPane.showMessageDialog(
          this,
          "No calendar selected",
          "Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Export Calendar to CSV");
    fileChooser.setSelectedFile(new File(currentCal.getName() + ".csv"));
    fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));

    int result = fileChooser.showSaveDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();
      String filePath = file.getAbsolutePath();

      // Ensure the file has .csv extension
      if (!filePath.toLowerCase().endsWith(".csv")) {
        filePath += ".csv";
      }

      // Create a command to export the calendar
      String command = "export cal " + filePath;
      mainGUI.executeCommand(command);
    }
  }

  /**
   * Imports events from a CSV file into the current calendar.
   */
  public void importFromCSV() {
    Calendar currentCal = calendarManager.getCurrentCalendar();
    if (currentCal == null) {
      JOptionPane.showMessageDialog(
          this,
          "No calendar selected",
          "Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Import Events from CSV");
    fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));

    int result = fileChooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();

      // Create a command to import the calendar
      String command = "import " + file.getAbsolutePath();
      mainGUI.executeCommand(command);

      // Refresh the view after import
      updateView(mainGUI.getDisplayDate());
    }
  }
}