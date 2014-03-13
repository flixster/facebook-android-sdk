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

import android.content.Context;
import android.os.Looper;

import java.lang.reflect.Method;

public class AdvertisingIdInfo {

    // com.google.android.gms.common.ConnectionResult.SUCCESS
    private static final int CONNECTION_RESULT_SUCCESS = 0;

    private final String id;
    private final boolean limitAdTrackingEnabled;

    private AdvertisingIdInfo(String id, boolean limitAdTrackingEnabled) {
        this.id = id;
        this.limitAdTrackingEnabled = limitAdTrackingEnabled;
    }

    public String getId() {
        return id;
    }

    public boolean isLimitAdTrackingEnabled() {
        return limitAdTrackingEnabled;
    }

    /**
     * Note: This cannot be called on the UI thread, because AdvertisingIdClient.getAdvertisingIdInfo
     * throws on UI thread.
     *
     * @param context
     * @return
     */
    public static AdvertisingIdInfo getAdvertisingIdInfo(Context context) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Cannot get advertising info on main thread.");
        }

        Method isGooglePlayServicesAvailable = AdUtilities.getMethod(
                "com.google.android.gms.common.GooglePlayServicesUtil",
                "isGooglePlayServicesAvailable",
                Context.class
        );
        if (isGooglePlayServicesAvailable == null) {
            return null;
        }
        Object connectionResult = AdUtilities.invokeMethod(null, isGooglePlayServicesAvailable, context);
        if (connectionResult == null || (Integer) connectionResult != CONNECTION_RESULT_SUCCESS) {
            return null;
        }

        Method getAdvertisingIdInfo = AdUtilities.getMethod(
                "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                "getAdvertisingIdInfo",
                Context.class
        );
        if (getAdvertisingIdInfo == null) {
            return null;
        }
        Object advertisingInfo = AdUtilities.invokeMethod(null, getAdvertisingIdInfo, context);
        if (advertisingInfo == null) {
            return null;
        }

        Method getId = AdUtilities.getMethod(advertisingInfo.getClass(), "getId");
        Method isLimitAdTrackingEnabled = AdUtilities.getMethod(advertisingInfo.getClass(), "isLimitAdTrackingEnabled");
        if (getId == null || isLimitAdTrackingEnabled == null) {
            return null;
        }

        String id = (String) AdUtilities.invokeMethod(advertisingInfo, getId);
        Boolean limitAdTrackingEnabled = (Boolean) AdUtilities.invokeMethod(advertisingInfo, isLimitAdTrackingEnabled);
        return new AdvertisingIdInfo(id, limitAdTrackingEnabled);
    }
}
