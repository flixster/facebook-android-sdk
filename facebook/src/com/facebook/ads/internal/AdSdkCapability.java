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

import org.json.JSONArray;

enum AdSdkCapability {

    APP_AD(0),
    LINK_AD(1),
    APP_AD_V2(2),
    LINK_AD_V2(3),
    APP_ENGAGEMENT_AD(4),
    AD_CHOICES(5);

    private final int value;

    private AdSdkCapability(int value) {
        this.value = value;
    }

    int getValue() {
        return value;
    }

    private static final AdSdkCapability[] supportedCapabilities = new AdSdkCapability[] {
            APP_ENGAGEMENT_AD,
            LINK_AD_V2,
            AD_CHOICES
    };

    private static final String supportedCapabilitiesStr;

    static {
        JSONArray array = new JSONArray();
        for (AdSdkCapability supportedCapability : supportedCapabilities) {
            array.put(supportedCapability.getValue());
        }
        supportedCapabilitiesStr = array.toString();
    }

    public static String getSupportedCapabilities() {
        return supportedCapabilitiesStr;
    }
}
