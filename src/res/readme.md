## How to Run

1. Compile all Java files:
   ```
   javac -d out *.java controller/*.java model/*.java view/*.java
   ```

2. Run the application:
   ```
   java -cp out CalendarApp --mode interactive
   ```
   or
   ```
   java -cp out CalendarApp --mode headless commands.txt
   ```

## Features and Functionality

### Event Creation
- **Regular Events**: Create events with specific start and end times
    - Automatically checks for conflicts with existing events
    - Supports adding description, location, and privacy settings
- **All-Day Events**: Create events that occupy an entire day
    - No specific start/end time needed
- **Recurring Events**: Create events that repeat on specific days of the week
    - Can specify number of occurrences or an end date
    - Checks for conflicts across all occurrences
    - Supports patterns like daily, specific weekdays (MTWRFSU format)

### Event Management
- **Edit Single Event**: Modify properties of a specific event instance
    - Can change name, description, location, start/end times, privacy
    - When editing date/time properties, the system does NOT check for conflicts
    - Time range validation ensures end time is always after start time
- **Edit Events From Date**: Modify all future occurrences of a recurring event
    - Preserves past occurrences with original properties
    - Updates only specified occurrences from the given date
- **Edit All Events**: Modify all events with the same name
    - Applies changes to both recurring and non-recurring events

### Event Viewing
- **Daily View**: Shows all events scheduled for a specific date
    - Includes one-time events and recurring event occurrences
- **Date Range View**: Shows all events within a specified time period
    - Sorted chronologically for easy reading

### Conflict Management
- **Auto-Decline**: Optional flag that rejects event creation if conflicts exist
- **Conflict Detection**: Prevents scheduling overlapping events when enabled
- **Availability Check**: Query whether you're busy or free at a specific time

### Other Functions
- **CSV Export**: Export entire calendar to standard CSV format
    - Compatible with other calendar applications
- **Interactive and Batch Modes**: Run commands interactively or from a file

### Command Processing
- **Input Validation**: Robust error handling for malformed commands
- **Error Handling**:
    - In interactive mode, errors are displayed and you can continue entering new commands
    - In headless mode, the program exits when encountering an error
- **Graceful Exit**: Properly closes resources when exiting

## Team Contributions

- **Vishal Rajpurohit**:
    - Implemented Calendar interface and CalendarImpl class
    - Developed Event interface and EventImpl class
    - Created RecurrencePattern class for handling repeating events
    - Implemented conflict detection and resolution for events

- **Sanskar Sharma**:
    - Developed command parsing and processing framework
    - Implemented all UI classes (InteractiveUI and HeadlessUI)
    - Created the DateTimeUtil class for date/time operations
    - Integrated components and implemented the main application

## Notes for Graders

- The application follows MVC architecture
- Dates use ISO format (YYYY-MM-DDThh:mm)
- Weekdays for recurrence use: M=Monday, T=Tuesday, W=Wednesday, R=Thursday, F=Friday, S=Saturday, U=Sunday# Calendar Application


## Command Examples

### Creating Events
```
# Regular event
create event <eventName> from <dateTimeString> to <dateTimeString>
create event --autoDecline <eventName> from <dateTimeString> to <dateTimeString>

# All-day event
create event <eventName> on <dateString>
create event --autoDecline <eventName> on <dateString>

# Recurring event with number of occurrences
create event <eventName> from <dateTimeString> to <dateTimeString> repeats <weekdays> for <number> times

# Recurring event with end date
create event <eventName> from <dateTimeString> to <dateTimeString> repeats <weekdays> until <dateString>

# Event with additional parameters
create event --autoDecline <eventName> from <dateTimeString> to <dateTimeString> --description "<description>" --location "<location>" --private
```

### Editing Events
```
# Edit a specific event
edit event <property> <eventName> from <dateTimeString> to <dateTimeString> with "<newValue>"

# Edit all future occurrences from a date
edit events <property> <eventName> from <dateTimeString> with "<newValue>"

# Edit all events with a name
edit events <property> "<eventName>" with "<newValue>"
```

### Viewing & Managing Events
```
# View events on a specific day
print events on <dateString>

# View events in a date range
print events from <dateString> to <dateString>

# Check availability
show status on <dateTimeString>

# Export calendar
export cal <fileName>

# Exit application
exit
```