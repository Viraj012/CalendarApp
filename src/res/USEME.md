# Calendar Application User Guide

This document provides detailed instructions on how to use the Calendar Application GUI.

## Getting Started

1. Launch the application by double-clicking the JAR file or running:
   ```
   java -jar CalendarApp.jar
   ```

2. The application will open in GUI mode showing the current month with a default calendar.

## Navigation

- **Month Navigation**: Use the `<<` and `>>` buttons to move between months
- **Day Navigation**: In day view, use the `<<` and `>>` buttons to move between days
- **Today Button**: Click the `Today` button to jump to the current date
- **View Toggle**: Switch between Month and Day view using the buttons in the top-right corner

## Calendar Management

### Creating a New Calendar

1. Click the `New Calendar` button in the toolbar
2. Enter a name for the calendar
3. Select a timezone from the dropdown menu
4. Click `OK` to create the calendar

### Switching Between Calendars

- Use the calendar dropdown in the toolbar to switch between different calendars

### Editing a Calendar

1. Select the calendar you want to edit
2. Click the `Edit Calendar` button
3. Choose the property to edit (name or timezone)
4. Enter the new value
5. Click `OK` to save changes

## Working with Events

### Creating a New Event

1. Double-click on a day in the calendar or click the `New Event` button
2. Fill in the event details:
   - **Subject**: The name of the event (required)
   - **Date**: Select the date for the event
   - **Start Time**: Set the start time (disabled for all-day events)
   - **End Time**: Set the end time (disabled for all-day events)
   - **All-day event**: Check this for events that take the entire day
   - **Recurring event**: Check this for events that repeat
   - **Location**: Optional location information
   - **Description**: Optional details about the event
   - **Private event**: Check this for private events
3. For recurring events, select:
   - Which days of the week the event repeats on
   - Whether it ends after a number of occurrences or on a specific date
4. Click `Save` to create the event

### Editing an Event

1. Click on any event in the calendar view
2. Modify the event details as needed
3. Click `Save` to update the event

### Event Color Coding

Events in the month view are color-coded:
- **Blue**: Regular single events
- **Green**: All-day events
- **Orange**: Recurring events
- **Purple**: All-day recurring events

## Import and Export

### Exporting Calendar Events

1. Click the `Export to CSV` button in the calendar panel
2. Choose a location and filename for the CSV file
3. Click `Save` to export the events

### Importing Events from CSV

1. Click the `Import from CSV` button in the calendar panel
2. Select a CSV file to import (must follow Google Calendar CSV format)
3. Click `Open` to import the events

## Additional Features

### Day View

1. Click the `Day View` button in the calendar panel
2. The view will change to show a detailed timeline for the selected day
3. Events are displayed at their scheduled times
4. Double-click on the timeline to create a new event at that time

### Month View

1. Click the `Month View` button in the calendar panel
2. The view will change to show the entire month
3. Events are listed in each day cell
4. A "+" indicator is shown if there are more events than can be displayed

### Checking Availability

To check if you're available at a specific time:
1. Switch to Day View
2. A red horizontal line indicates the current time
3. Free time slots appear empty on the timeline

## Exiting the Application

Close the application window to exit.
