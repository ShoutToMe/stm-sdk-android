package me.shoutto.sdk;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Date;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class ProximitySensorClient implements SensorEventListener {

    private static final String TAG = "ProximitySensorClient";
    private static final long PROXIMITY_WAVE_MIN_TIME_MS = 90;
    private static final long PROXIMITY_WAVE_MAX_TIME_MS = 400;
    private SensorManager sensorManager;
    private Sensor proximity;
    private StmService stmService;
    private boolean isListening = false;
    private float far = -1.0f;
    private long onTime = 0;

    public ProximitySensorClient(StmService stmService) {
        this.stmService = stmService;
        sensorManager = (SensorManager) this.stmService.getSystemService(Context.SENSOR_SERVICE);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];
        if (distance > far) {
            far = distance;
        }

        if (distance < far) {
            Date date = new Date();
            onTime = date.getTime();
        } else {
            Date date = new Date();
            long waveDuration = date.getTime() - onTime;
            if ((waveDuration >= PROXIMITY_WAVE_MIN_TIME_MS) && (waveDuration <= PROXIMITY_WAVE_MAX_TIME_MS)) {
                stmService.handleHandWaveGesture();
            }
        }
    }

    public void startListening() {
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
        isListening = true;
    }

    public void stopListening() {
        if (isListening) {
            sensorManager.unregisterListener(this);
            isListening = false;
        }
    }
}