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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.facebook.*;
import com.facebook.ads.AdSize;

public class AdRequest {

    public interface AdRequestListener {
        void onLoading();
        void onCompleted(AdResponse adResponse);
        void onError(FacebookRequestError error);
    }

    private static final String TAG = AdRequest.class.getSimpleName();
    private static final String ADS_ENDPOINT = "ads";

    private static final String PLACEMENT_ID_PARAM = "placement_id";
    private static final String ATTRIBUTION_ID_PARAM = "attribution_id";
    private static final String WIDTH_PARAM = "width";
    private static final String HEIGHT_PARAM = "height";
    private static final String TEST_MODE_PARAM = "test_mode";

    private static final String DEVICE_ID_PARAM = "device_id";
    private static final String USER_AGENT_PARAM = "user_agent";
    private static final String OS_VERSION_PARAM = "os_version";

    private static final String PACKAGE_NAME_PARAM = "package_name";

    private final Context context;
    private final String placementId;
    private final AdSize adSize;
    private final String userAgentString;

    private final AdRequestListener listener;

    private final boolean testMode;

    public AdRequest(Context context, String placementId, AdSize adSize, String userAgentString,
            AdRequestListener listener, boolean testMode) {
        this.context = context;
        this.placementId = placementId;
        this.adSize = adSize;
        this.userAgentString = userAgentString;
        this.listener = listener;
        this.testMode = testMode;

        validate();
    }

    public RequestAsyncTask executeAsync() {
        Request.Callback adRequestCallback = new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                if (response.getError() != null) {
                    Log.e(TAG, response.getError().getErrorMessage(), response.getError().getException());
                    listener.onError(response.getError());
                } else {
                    AdResponse adResponse = AdResponse.fromJSONObject(response.getGraphObject().getInnerJSONObject());
                    listener.onCompleted(adResponse);
                }
            }
        };
        Request request = new Request(null, ADS_ENDPOINT, getRequestParameters(), HttpMethod.POST, adRequestCallback);
        listener.onLoading();
        return request.executeAsync();
    }

    private void validate() {
        if (placementId == null || placementId.length() < 1) {
            throw new IllegalArgumentException("placementId");
        }
        if (adSize == null) {
            throw new IllegalArgumentException("adSize");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener");
        }
    }

    private Bundle getRequestParameters() {
        Bundle bundle = new Bundle();
        bundle.putString(PLACEMENT_ID_PARAM, placementId);
        bundle.putString(ATTRIBUTION_ID_PARAM, Settings.getAttributionId(context.getContentResolver()));
        bundle.putInt(WIDTH_PARAM, adSize.getWidth());
        bundle.putInt(HEIGHT_PARAM, adSize.getHeight());
        bundle.putBoolean(TEST_MODE_PARAM, testMode);

        addDeviceInfoParams(bundle);
        addAppInfoParams(bundle);

        return bundle;
    }

    private void addDeviceInfoParams(Bundle bundle) {
        bundle.putString(DEVICE_ID_PARAM, android.provider.Settings.Secure.getString(context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID));
        bundle.putString(OS_VERSION_PARAM, Build.VERSION.SDK);
        bundle.putString(USER_AGENT_PARAM, userAgentString);
    }

    private void addAppInfoParams(Bundle bundle) {
        bundle.putString(PACKAGE_NAME_PARAM, context.getPackageName());
    }
}
