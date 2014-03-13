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

package com.facebook.ads;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import com.facebook.ads.internal.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

/**
 * Global settings for ad controls.
 */
public class AdSettings {

    public static final String TAG = AdSettings.class.getSimpleName();

    private static final String PREFS_NAME = "FBAdPrefs";
    private static final String DEVICE_ID_HASH_PREFS_KEY = "deviceIdHash";

    private static final Collection<String> testDevices;
    private static final Collection<String> emulatorProducts;

    private static String urlPrefix = null;
    private static boolean childDirected = false;
    private static String deviceIdHash = null;

    static {
        testDevices = new HashSet<String>();

        emulatorProducts = new HashSet<String>();
        emulatorProducts.add("sdk");
        emulatorProducts.add("google_sdk");
        emulatorProducts.add("vbox86p");
        emulatorProducts.add("vbox86tp");
    }

    /**
     * Adds a test device.
     * <p>
     * Copy the current device Id from debug log and add it as a test device to get test ads.
     * Apps running on emulator will automatically get test ads. Test devices should be added
     * before {@link Ad#loadAd()} is called.
     *
     * @param deviceIdHash id of the device to use test mode, can be obtained from debug log
     */
    public static void addTestDevice(String deviceIdHash) {
        testDevices.add(deviceIdHash);
    }

    /**
     * Adds a collection of test devices.
     * <p>
     * Copy the current device Id from debug log and add it as a test device to get test ads.
     * Apps running on emulator will automatically get test ads. Test devices should be added
     * before {@link Ad#loadAd()} is called.
     *
     * @param deviceIdHashes ids of the device to use test mode, can be obtained from debug log
     */
    public static void addTestDevices(Collection<String> deviceIdHashes) {
        testDevices.addAll(deviceIdHashes);
    }

    /**
     * Clears the collection of test devices.
     */
    public static void clearTestDevices() {
        testDevices.clear();
    }

    /**
     * Gets whether ad controls are working in test mode.
     * @param context android context
     * @return whether in test mode
     */
    public static boolean isTestMode(Context context) {
        if (emulatorProducts.contains(Build.PRODUCT)) {
            return true;
        }

        if (deviceIdHash == null) {
            SharedPreferences adPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            deviceIdHash = adPrefs.getString(DEVICE_ID_HASH_PREFS_KEY, null);
            if (StringUtils.isNullOrEmpty(deviceIdHash)) {
                deviceIdHash = StringUtils.md5(UUID.randomUUID().toString());
                adPrefs.edit()
                        .putString(DEVICE_ID_HASH_PREFS_KEY, deviceIdHash)
                        .commit();
            }
        }

        if (testDevices.contains(deviceIdHash)) {
            return true;
        }

        printTestDeviceNotice(deviceIdHash);
        return false;
    }

    static volatile boolean testDeviceNoticeDisplayed = false;
    private static void printTestDeviceNotice(String deviceIdHash) {
        if (testDeviceNoticeDisplayed) {
            return;
        }
        testDeviceNoticeDisplayed = true;
        Log.d(TAG, "Test mode device hash: " + deviceIdHash);
        Log.d(TAG, "When testing your app with Facebook's ad units you must specify the device hashed ID " +
                "to ensure the delivery of test ads, add the following code before loading an ad: " +
                "AdSettings.addTestDevice(\"" + deviceIdHash + "\");");
    }

    /**
     * Sets the url prefix to use when making requests. This method should never be used in production.
     * @param urlPrefix url prefix
     */
    public static void setUrlPrefix(String urlPrefix) {
        AdSettings.urlPrefix = urlPrefix;
    }

    /**
     * Gets the url prefix.
     * @return url prefix
     */
    public static String getUrlPrefix()  {
        return urlPrefix;
    }

    /**
     * Sets whether the ad control is targeting a child.
     * @param childDirected whether the ad control is targeting a child
     */
    public static void setIsChildDirected(boolean childDirected) {
        AdSettings.childDirected = childDirected;
    }

    /**
     * Gets whether the ad control is targeting a child.
     * @return whether the ad control is targeting a child
     */
    public static boolean isChildDirected() {
        return childDirected;
    }
}
