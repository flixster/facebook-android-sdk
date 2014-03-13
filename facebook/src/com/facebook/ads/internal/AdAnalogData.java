/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.ads.internal;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AdAnalogData {

    private static AdSensorEventListener sensorEventListener = null;
    private static SensorManager sensorManager = null;
    private static Sensor accelerometer = null;
    private static volatile float[] sensorValues;

    public synchronized static void registerSensorListener(Context context) {
        if (sensorEventListener != null) {
            return;
        }
        if (sensorManager == null) {
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager == null) {
                return;
            }
        }
        if (accelerometer == null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer == null) {
                return;
            }
        }
        sensorEventListener = new AdSensorEventListener();
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public synchronized static void unregisterSensorListener() {
        if (sensorEventListener == null || sensorManager == null) {
            return;
        }
        sensorManager.unregisterListener(sensorEventListener);
        sensorEventListener = null;
    }

    public static Map<String, Object> getAnalogInfo(Context context) {
        Map<String, Object> analogInfo = new HashMap<String, Object>();
        putMemoryInfo(context, analogInfo);
        putDiskInfo(context, analogInfo);
        putBatteryData(context, analogInfo);
        putSensorData(analogInfo);

        return analogInfo;
    }

    private static void putMemoryInfo(Context context, Map<String, Object> analogInfo) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        analogInfo.put("available_memory", String.valueOf(mi.availMem));
    }

    private static void putDiskInfo(Context context, Map<String, Object> analogInfo) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        analogInfo.put("free_space", Formatter.formatFileSize(context, availableBlocks * blockSize));
    }

    private static void putBatteryData(Context context, Map<String, Object> analogInfo) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent == null) {
            return;
        }

        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryLevel = 0;
        if (scale > 0) {
            batteryLevel = ((float) level / (float) scale) * 100;
        }

        analogInfo.put("battery", batteryLevel);
    }

    private static void putSensorData(Map<String, Object> analogInfo) {
        float[] currentSensorValues = sensorValues;
        if (currentSensorValues == null || currentSensorValues.length == 0) {
            return;
        }

        for (int i = 0; i < currentSensorValues.length; i++) {
            analogInfo.put("sensor_" + i, currentSensorValues[i]);
        }
    }

    public static class AdSensorEventListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            sensorValues = event.values;
            AdAnalogData.unregisterSensorListener();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // ignore
        }
    }
}
