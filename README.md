🏍️ Motorcycle Lean Angle Sensor

This Android application, MaioTProject, uses a smartphone's internal sensors to accurately measure and log a motorcycle's lean angle (bank angle) in real-time. It provides a reliable and filtered angle that is robust against sensor noise and inaccuracies.

⚙️ How It Works

The app combines data from the gyroscope and accelerometer in a process called sensor fusion.

    Gyroscope: Tracks rapid changes in orientation, capturing the lean of a turn.

    Accelerometer: Provides a stable, long-term reference point using the force of gravity, which corrects the gyroscope's drift.

The application includes an adaptive filter that dynamically adjusts the influence of each sensor. This ensures the angle is highly responsive during active banking and quickly settles to 0∘ when the bike is upright.

🚀 Key Features

    Real-Time Display: See your current lean angle live.

    Adaptive Filtering: A smart algorithm that balances smoothness and accuracy.

    Automated Calibration: Simple routine to zero the angle to your bike's upright position.

    Max Angle Logging: Tracks the maximum positive (right-hand) and negative (left-hand) lean angles.

    GPS Tracking: Visualizes your ride path on a map.

🛠️ Usage

    Mount the Smartphone: Securely place the phone in an upright position on your bike.

    Calibrate: With the bike upright and stationary, press "Start Calibration."

    Track: Begin your ride and press "Start Tracking" to log your data. The app will display your lean angle and maximums.

For optimal performance and battery life, the sensors are set to SENSOR_DELAY_GAME by default, offering a great balance between responsiveness and power consumption.
