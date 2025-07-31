package com.gg.maiotproject.sensors;

import android.content.Context;
import android.util.Log;

import com.gg.maiotproject.interfaces.IMyAccelerometer;
import com.gg.maiotproject.interfaces.IMyComplementaryFilter;
import com.gg.maiotproject.interfaces.IMyGyroscope;

public class MyComplementaryFilter implements IMyAccelerometer, IMyGyroscope {
    private final String TAG = "MyComplementaryFilter";

    private IMyComplementaryFilter iMyComplementaryFilter = null;

    private float filteredRoll = 0.0f;
    private long lastGyroTimestamp = 0; // Store timestamp of previous gyro event
    private static final float NS2S = 1.0f / 1000000000.0f;

    // Complementary filter constant:
    // ALPHA = 0.98f  -->  97% weight from previous filtered state (gyro integrated)
    //                     3% weight from accelerometer (for drift correction).
    private static final float ALPHA = 0.97f;

    private float maxPositiveRoll = 0.0f;
    private float maxNegativeRoll = 0.0f;

    // Calibration variables
    private boolean isCalibrating = false;
    private boolean isTrackingMode = false;

    private float avgGyroX_cal = 0.0f;
    // We only care about X for roll, but keeping Y and Z for completeness
    private float avgGyroY_cal = 0.0f;
    private float avgGyroZ_cal = 0.0f;
    private float initialAccRoll_cal = 0.0f;

    private int calibrationSampleCount = 0;
    private float tempGyroX = 0.0f, tempGyroY = 0.0f, tempGyroZ = 0.0f;
    private float tempAccRoll = 0.0f;

    // Store latest raw accelerometer data
    private float latestAccX = 0.0f;
    private float latestAccY = 0.0f;
    private float latestAccZ = 0.0f;

    public MyComplementaryFilter(Context context) {
        this.iMyComplementaryFilter = (IMyComplementaryFilter) context;
    }

    // Starts the calibration process. Resets accumulated calibration data
    public void startCalibration() {
        isCalibrating = true;
        isTrackingMode = false;
        calibrationSampleCount = 0;
        tempGyroX = 0.0f;
        tempGyroY = 0.0f;
        tempGyroZ = 0.0f;
        tempAccRoll = 0.0f;

        // Reset latest accelerometer data to ensure a clean start
        latestAccX = 0.0f;
        latestAccY = 0.0f;
        latestAccZ = 0.0f;
    }

    // Finalizes the calibration process by calculating averages and setting offsets.

    public void finalizeCalibration() {
        isCalibrating = false;

        // Conditional check ensures calibration proceeds if sensor data was acquired
        if (calibrationSampleCount > 0) {
            // Gyroscope average bias collection for each axis (then subtracted to gyro measures during tracking
            avgGyroX_cal = tempGyroX / calibrationSampleCount;
            avgGyroY_cal = tempGyroY / calibrationSampleCount;
            avgGyroZ_cal = tempGyroZ / calibrationSampleCount;

            // initialAccRoll_cal is the offset subtracted to from the live accelerometer roll
            initialAccRoll_cal = tempAccRoll / calibrationSampleCount;

        } else {
            avgGyroX_cal = 0.0f;
            avgGyroY_cal = 0.0f;
            avgGyroZ_cal = 0.0f;
            initialAccRoll_cal = 0.0f;
        }

        // Initialize filteredRoll to 0.0f to represent the calibrated "level" state.
        // This ensures the displayed angle starts at 0 relative to the calibrated position.
        filteredRoll = 0.0f;

        // Reset lastGyroTimestamp to ensure delta time is calculated correctly on first gyro reading after calibration
        lastGyroTimestamp = 0;
    }

    // Controls whether the filter should actively process data
    public void setTrackingMode(boolean trackingMode) {
        this.isTrackingMode = trackingMode;
        if (trackingMode) {
            // Reset max angles when starting a new journey
            maxPositiveRoll = 0.0f;
            maxNegativeRoll = 0.0f;
            // Also explicitly reset filteredRoll to 0 at the start of tracking.
            // This ensures that even if there was some drift between calibration and start button press,
            // the new journey starts from an assumed level/zero.
            filteredRoll = 0.0f;
            lastGyroTimestamp = 0; // Reset timestamp for fresh start
        } else {
            Log.d(TAG, "Tracking mode stopped.");
        }
    }

    // Two states: calibration and tracking
    @Override
    public void onNewAccelerometerDataAvailable(float AccX, float AccY, float AccZ) {
        if (isCalibrating) {
            // It accumulates raw accelerometer roll angle
            float rollAngle = (float) Math.toDegrees(Math.atan2(AccX, AccZ));
            tempAccRoll += rollAngle;
            calibrationSampleCount++;
        } else if (isTrackingMode) {
            // Store the latest accelerometer data
            latestAccX = AccX;
            latestAccY = AccY;
            latestAccZ = AccZ;
            // The actual filtering will happen in onNewGyroscopeDataAvailable
        }
    }

    @Override
    public void onNewGyroscopeDataAvailable(float RotX, float RotY, float RotZ, long timestamp) {
        if (isCalibrating) {
            tempGyroX += RotX;
            tempGyroY += RotY;
            tempGyroZ += RotZ;
        } else if (isTrackingMode) {
            // Perform complementary filter update here
            if (lastGyroTimestamp != 0) {
                float deltaTime = (timestamp - lastGyroTimestamp) * NS2S;

                // 1. Calculate accelerometer-derived roll angle using the LATEST stored accelerometer data
                float accRollAngle = (float) Math.toDegrees(Math.atan2(latestAccX, latestAccZ));
                float calibratedAccRoll = accRollAngle - initialAccRoll_cal;

                // 2. Apply gyroscope bias compensation to the current gyroscope reading
                float calibratedRotX = RotX - avgGyroX_cal;

                // 3. Integrate gyroscope data to predict the new angle
                float gyroIntegratedRoll = filteredRoll + (calibratedRotX * deltaTime);

                // 4. Combine gyroscope prediction with accelerometer correction using the complementary filter
                filteredRoll = ALPHA * gyroIntegratedRoll + (1 - ALPHA) * calibratedAccRoll;

                updateMaxRollAngles(filteredRoll);
                onNewFilteredAngleAvailable();
            }
            lastGyroTimestamp = timestamp; // Update timestamp AFTER calculation
        }
    }

    private void updateMaxRollAngles(float roll) {
        if (roll > maxPositiveRoll) {
            maxPositiveRoll = roll;
            iMyComplementaryFilter.onNewMaxPositiveRollAvailable(maxPositiveRoll);
        }
        // Use Math.abs for sending negative roll to UI to simplify display logic
        if (roll < maxNegativeRoll) {
            maxNegativeRoll = roll;
            iMyComplementaryFilter.onNewMaxNegativeRollAvailable(Math.abs(maxNegativeRoll));
        }
    }

    private void onNewFilteredAngleAvailable() {
        iMyComplementaryFilter.onNewFilteredAngleAvailable(filteredRoll);
    }
}