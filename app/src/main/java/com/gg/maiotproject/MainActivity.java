package com.gg.maiotproject;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.views.MapView;

import com.gg.maiotproject.interfaces.IMyAccelerometer;
import com.gg.maiotproject.interfaces.IMyComplementaryFilter;
import com.gg.maiotproject.interfaces.IMyGyroscope;
import com.gg.maiotproject.sensors.MyAccelerometer;
import com.gg.maiotproject.sensors.MyComplementaryFilter;
import com.gg.maiotproject.sensors.MyGyroscope;
import com.gg.maiotproject.map.MapHandler;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements IMyAccelerometer, IMyGyroscope, IMyComplementaryFilter {

    private Button bttStartStop = null;
    private Button bttCalibrate = null;
    private TextView tvAngle = null;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private MapHandler mapHandler;

    private MyAccelerometer myAccelerometer = null;
    private MyGyroscope myGyroscope = null;
    private MyComplementaryFilter myComplementaryFilter = null;

    private boolean isTracking = false;
    private boolean isCalibrated = false;
    private boolean isCalibrating = false;

    private double maxPositiveRoll = 0;
    private double maxNegativeRoll = 0;

    private Handler calibrationHandler = new Handler();
    private static final int CALIBRATION_DURATION_MS = 3000; // 3 seconds for calibration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keeps the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialize UI elements
        bttStartStop = findViewById(R.id.bttStartStop);
        bttCalibrate = findViewById(R.id.bttCalibrate);
        tvAngle = findViewById(R.id.tvAngle);

        // Sensors and filter instantiation
        myAccelerometer = new MyAccelerometer(this);
        myGyroscope = new MyGyroscope(this);
        myComplementaryFilter = new MyComplementaryFilter(this);

        // Map initialization
        MapView mapView = findViewById(R.id.map);
        mapHandler = new MapHandler(this, mapView);

        // Handles runtime location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            mapHandler.displayCurrentLocation(); // Display current location on app start
        }

        bttCalibrate.setOnClickListener((v) -> {
            if (!isCalibrating) {
                startCalibrationSequence();
            }
        });

        bttStartStop.setOnClickListener((v) -> {
            if (!isCalibrated) {
                Toast.makeText(getApplicationContext(), "Please calibrate first!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isTracking) {
                // Start tracking
                myAccelerometer.start();
                myGyroscope.start();
                myComplementaryFilter.setTrackingMode(true); // Tell filter to process and send angles
                mapHandler.startTracking();
                Toast.makeText(getApplicationContext(), "Journey Started", Toast.LENGTH_SHORT).show();
                bttStartStop.setText("Stop");
                isTracking = true;
                tvAngle.setVisibility(TextView.VISIBLE);
            } else {
                // Stop tracking
                myAccelerometer.stop();
                myGyroscope.stop();
                myComplementaryFilter.setTrackingMode(false); // Tell filter to stop sending angles
                mapHandler.stopTracking();
                bttStartStop.setText("Start");
                isTracking = false;

                // Start SummaryActivity
                Intent intent = new Intent(MainActivity.this, SummaryActivity.class);
                intent.putParcelableArrayListExtra("geoPoints", new ArrayList<>(mapHandler.getGeoPoints()));
                intent.putExtra("distance", mapHandler.getDistance());
                intent.putExtra("maxPositiveAngle", maxPositiveRoll);
                intent.putExtra("maxNegativeAngle", maxNegativeRoll);
                startActivity(intent);
            }
        });
    }

    private void startCalibrationSequence() {
        isCalibrating = true;
        bttCalibrate.setEnabled(false);
        Toast.makeText(getApplicationContext(), "Calibrating... Keep device steady!", Toast.LENGTH_SHORT).show();

        myAccelerometer.start(); // Start sensors to collect calibration data
        myGyroscope.start();
        myComplementaryFilter.startCalibration(); // Tell filter to start collecting calibration data

        // Handler to delay the finalization of the calibration sequence
        calibrationHandler.postDelayed(() -> {
            finalizeCalibrationSequence();
        }, CALIBRATION_DURATION_MS);
    }

    private void finalizeCalibrationSequence() {
        isCalibrating = false;
        myComplementaryFilter.finalizeCalibration(); // Calculate and apply offsets
        myAccelerometer.stop(); // Stop sensors after calibration
        myGyroscope.stop();

        isCalibrated = true;
        bttCalibrate.setVisibility(Button.GONE); // Hide calibrate button
        bttStartStop.setVisibility(Button.VISIBLE); // Show start/stop button
        bttStartStop.setEnabled(true); // Enable start/stop button
        Toast.makeText(getApplicationContext(), "Calibration complete!", Toast.LENGTH_SHORT).show();
    }

    // Callback triggered by MyComplementaryFilter when a new filtered angle is calculated
    @Override
    public void onNewFilteredAngleAvailable(float filteredRoll) {
        // Only update UI if not calibrating and tracking
        if (isCalibrated && isTracking) {
            String filteredAngleRollStr = String.format("%.1f", filteredRoll);
            tvAngle.setText("Angle: " + filteredAngleRollStr);
        }
    }

    @Override
    public void onNewMaxPositiveRollAvailable(double maxPositiveRoll) {
        this.maxPositiveRoll = maxPositiveRoll;
    }

    @Override
    public void onNewMaxNegativeRollAvailable(double maxNegativeRoll) {
        this.maxNegativeRoll = maxNegativeRoll;
    }

    @Override
    public void onNewAccelerometerDataAvailable(float AccX, float AccY, float AccZ) {
        myComplementaryFilter.onNewAccelerometerDataAvailable(AccX, AccY, AccZ);
    }

    @Override
    public void onNewGyroscopeDataAvailable(float RotX, float RotY, float RotZ, long timestamp) {
        myComplementaryFilter.onNewGyroscopeDataAvailable(RotX, RotY, RotZ, timestamp);
    }

    // Method that handles the result of permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mapHandler.displayCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied. Map tracking will not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Ensures resources are release when the activity is in the foreground
    @Override
    protected void onPause() {
        super.onPause();
        if (isTracking) {
            myAccelerometer.stop();
            myGyroscope.stop();
            mapHandler.stopTracking();
            bttStartStop.setText("Start");
            isTracking = false;
        }
        // If calibration was in progress, stop it
        if (isCalibrating) {
            calibrationHandler.removeCallbacksAndMessages(null); // Remove any pending callbacks
            myAccelerometer.stop();
            myGyroscope.stop();
            bttCalibrate.setEnabled(true); // Re-enable calibrate button
            isCalibrating = false;
        }
    }
}