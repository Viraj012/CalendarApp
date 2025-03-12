# Calendar Application

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

## Features

All features are working as specified:
- Creating regular, all-day, and recurring events
- Editing events (single, from date, all occurrences)
- Viewing events on specific dates or date ranges
- Checking availability at specific times
- Exporting calendar to CSV

## Team Contributions

- **Member 1**:
  - Implemented Calendar interface and CalendarImpl class
  - Developed Event interface and EventImpl class
  - Created RecurrencePattern class for handling repeating events
  - Implemented conflict detection and resolution for events

- **Member 2**:
  - Developed command parsing and processing framework
  - Implemented all UI classes (InteractiveUI and HeadlessUI)
  - Created the DateTimeUtil class for date/time operations
  - Integrated components and implemented the main application

## Notes for Graders

- The application follows MVC architecture
- Dates use ISO format (YYYY-MM-DDThh:mm)
- Weekdays for recurrence use: M=Monday, T=Tuesday, W=Wednesday, R=Thursday, F=Friday, S=Saturday, U=Sunday
