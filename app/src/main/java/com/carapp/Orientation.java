
package com.carapp;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.Nullable;

import android.view.Surface;
import android.view.WindowManager;

public class Orientation implements SensorEventListener {

    public interface Listener {
        void onOrientationChanged(float pitch, float roll);
        void onGyroChanged(float [] values);
    }

    private static final int SENSOR_DELAY_MICROS = 16 * 1000; // 16ms

    private final WindowManager windowManager;

    private final SensorManager sensorManager;


    private Sensor rotationSensor;
    private Sensor gyroSensor;

    private int lastAccuracy;
    private Listener listener;

    public Orientation(ComActivity activity) {
        windowManager = activity.getWindow().getWindowManager();
        sensorManager = (SensorManager) activity.getSystemService(Activity.SENSOR_SERVICE);

        // Can be null if the sensor hardware is not available
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public void startListening(Listener listener) {
        if (this.listener == listener) {
            return;
        }

        this.listener = listener;
        if (rotationSensor == null) {
            LogUtil.w("Rotation vector sensor not available; will not provide orientation data.");
            return;
        }
        sensorManager.registerListener(this, rotationSensor, SENSOR_DELAY_MICROS);
        sensorManager.registerListener(this, gyroSensor, SENSOR_DELAY_MICROS);
    }

    public void stopListening() {
        sensorManager.unregisterListener(this);
        listener = null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (lastAccuracy != accuracy) {
            lastAccuracy = accuracy;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (listener == null) {
            return;
        }

        if (lastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }

        if (event.sensor == rotationSensor) {
            updateOrientation(event.values);
        }else if (event.sensor == gyroSensor) {
            updateGyro(event.values);
        }
    }

    private void updateGyro(float [] values) {
        listener.onGyroChanged( values );
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void updateOrientation(float[] rotationVector) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);

        final int worldAxisForDeviceAxisX;
        final int worldAxisForDeviceAxisY;

        // Remap the axes as if the device screen was the instrument panel,
        // and adjust the rotation matrix for the device orientation.
        switch (windowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
            default:
                worldAxisForDeviceAxisX = SensorManager.AXIS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
                break;
            case Surface.ROTATION_90:
                worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
                break;
            case Surface.ROTATION_270:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_X;
                break;
        }

        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX, worldAxisForDeviceAxisY, adjustedRotationMatrix);

        // Transform rotation matrix into azimuth/pitch/roll
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);

        // Convert radians to degrees
        float pitch = orientation[1] * -57;
        float roll = orientation[2] * -57;

        listener.onOrientationChanged(pitch, roll);
    }
}
