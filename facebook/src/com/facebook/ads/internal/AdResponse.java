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

import com.facebook.ads.AdError;
import org.json.JSONObject;

public class AdResponse {

    private static final String TAG = AdResponse.class.getSimpleName();
    private static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 0;

    private final int refreshInterval;
    private final AdDataModel dataModel;
    private final AdError error;

    public AdResponse(int refreshInterval, AdDataModel dataModel, AdError error) {
        this.refreshInterval = refreshInterval;
        this.dataModel = dataModel;
        this.error = error;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public AdDataModel getDataModel() {
        return dataModel;
    }

    public AdError getError() {
        return error;
    }

    public static AdResponse fromJSONObject(JSONObject jsonObject) {
        int refreshInterval = jsonObject.optInt("refresh", DEFAULT_REFRESH_INTERVAL_SECONDS);

        AdDataModel data = null;
        JSONObject dataObject = jsonObject.optJSONObject("data");
        if (dataObject != null) {
            String markup = dataObject.optString("markup");
            String storeId = dataObject.optString("store_id");
            String storeType = dataObject.optString("store_type");
            data = new AdDataModel(markup, storeId, storeType);
        }

        AdError error = null;
        JSONObject errorObject = jsonObject.optJSONObject("reason");
        if (errorObject != null) {
            error = new AdError(errorObject.optInt("code"), errorObject.optString("message"));
        }

        return new AdResponse(refreshInterval, data, error);
    }
}
