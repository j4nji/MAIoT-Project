package com.gg.maiotproject.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.gg.maiotproject.interfaces.IMyGyroscope;

public class MyGyroscope implements SensorEventListener {

    private final String TAG = "MyGyroscope";

    private SensorManager sensorManager = null;
    private Sensor gyroscope = null;
    private IMyGyroscope iMyGyroscope = null;

    public MyGyroscope(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        else
            Log.i(TAG, "Sensor GYROSCOPE not available");

        this.iMyGyroscope = (IMyGyroscope) context;
    }

    public void start() {
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "onGyroSensorChanged");

        float RotX = event.values[0];
        float RotY = event.values[1];
        float RotZ = event.values[2];
        long timestamp = event.timestamp;

        iMyGyroscope.onNewGyroscopeDataAvailable(RotX, RotY, RotZ, timestamp);

        Log.i(TAG, "Rotation --> X: " + RotX + " - Y: " + RotY + " - Z: " + RotZ);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "onAccuracyChanged - Accuracy: " + accuracy);
    }
}
