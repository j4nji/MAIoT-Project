<H3> Motorcycle Bank Angle & GPS Tracker </H3>

<p> MAIoT Project is an Android application providing real-time telemetry and ride logging. It uses the device's built-in sensors to calculate and display the motorcycle's bank angle in real time, while simultaneously tracking the ride path and logging key data.
Features </p>

- Real-time Bank Angle Display: Uses a combination of accelerometer and gyroscope data to calculate and display the current bank angle.

- Maximum Bank Angle Logging: Automatically logs and saves the maximum left and right bank angles achieved during a ride.

- GPS Path Tracking: Utilizes OpenStreetMap (OSM) to display the rider's current position and draw a line representing the path driven.

- Complementary Filter: A robust complementary filter combines sensor data for smooth and reliable angle calculations.

    Ride Management:

        Calibration: A simple calibration sequence at the start of each session ensures accurate sensor readings.

        Start/Stop Functionality: Users can easily start and stop a ride to begin and end a logging session.

    Journey Summary: After a ride is stopped, a dedicated summary activity provides a complete overview, including the full path driven on the map, the total distance, and the maximum bank angles achieved.

How to Use

    Initial Calibration: At the beginning of your ride, perform the calibration sequence as prompted by the application to ensure sensor accuracy.

    Start Your Journey: Press the Start button to begin receiving sensor data and logging your ride. The map will show your position, and the bank angle will be displayed below it.

    End Your Ride: Press the Stop button to end the logging session. You will be taken to a summary screen.

    View Summary: The summary screen displays the entire path of your ride on the map, the total distance, and your maximum recorded left and right bank angles.

Technology Stack

    Development Language: Java

    Platform: Android

    Sensors: Accelerometer and Gyroscope

    Mapping: OpenStreetMap (OSM)

    Data Processing: Custom-implemented Complementary Filter for sensor fusion.
