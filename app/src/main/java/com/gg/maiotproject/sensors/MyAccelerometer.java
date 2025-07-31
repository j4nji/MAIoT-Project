package com.gg.maiotproject.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.gg.maiotproject.interfaces.IMyAccelerometer;

public class MyAccelerometer implements SensorEventListener {

    private final String TAG = "MyAccelerometer";

    private SensorManager sensorManager = null;
    private Sensor accelerometer = null;
    private IMyAccelerometer iMyAccelerometer = null;

    public MyAccelerometer(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        else
            Log.i(TAG, "Sensor ACCELEROMETER not available");

        this.iMyAccelerometer = (IMyAccelerometer) context;
    }

    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "onAccelSensorChanged");

        float AccX = event.values[0];
        float AccY = event.values[1];
        float AccZ = event.values[2];

        iMyAccelerometer.onNewAccelerometerDataAvailable(AccX, AccY, AccZ);

        Log.i(TAG, "Acceleration --> X: " + AccX + " - Y: " + AccY + " - Z: " + AccZ);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "onAccuracyChanged - Accuracy: " + accuracy);
    }
}
