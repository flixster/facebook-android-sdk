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
import com.facebook.ads.AdError;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdResponse {

    private static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 0;

    private final int refreshIntervalMillis;
    private final List<AdDataModel> dataModels;
    private final AdError error;

    private AdResponse(int refreshIntervalMillis, List<AdDataModel> dataModels, AdError error) {
        this.refreshIntervalMillis = refreshIntervalMillis;
        this.dataModels = dataModels;
        this.error = error;
    }

    public int getRefreshIntervalMillis() {
        return refreshIntervalMillis;
    }

    public AdDataModel getDataModel() {
        if (dataModels == null || dataModels.isEmpty()) {
            return null;
        }
        return dataModels.get(0);
    }

    public AdError getError() {
        return error;
    }

    public static AdResponse parseResponse(Context context, JSONObject jsonObject) {
        // Server returns refresh interval in seconds
        int refreshIntervalInMilli = jsonObject.optInt("refresh", DEFAULT_REFRESH_INTERVAL_SECONDS) * 1000;

        AdError error = null;
        JSONObject errorObject = jsonObject.optJSONObject("reason");
        if (errorObject != null) {
            error = new AdError(errorObject.optInt("code"), errorObject.optString("message"));
        }

        int adType = jsonObject.optInt("ad_type");
        List<AdDataModel> dataModels =  new ArrayList<AdDataModel>();
        JSONArray adsArray = jsonObject.optJSONArray("ads");
        if (adsArray != null && adsArray.length() > 0) {
            for (int i = 0; i < adsArray.length(); i++) {
                AdDataModel dataModel = null;
                if (adType == AdType.HTML.getValue()) {
                    JSONObject dataObject = adsArray.optJSONObject(i).optJSONObject("data");
                    dataModel = HtmlAdDataModel.fromJSONObject(dataObject);
                } else if (adType == AdType.NATIVE.getValue()) {
                    JSONObject dataObject = adsArray.optJSONObject(i).optJSONObject("metadata");
                    dataModel = NativeAdDataModel.fromJSONObject(dataObject);
                }
                if (dataModel != null && !AdInvalidationUtils.shouldInvalidate(context, dataModel)) {
                    dataModels.add(dataModel);
                }
            }
            if (dataModels.isEmpty()) {
                // All ads have been invalidated
                error = AdError.CLIENT_INVALIDATION;
            }
        }

        return new AdResponse(refreshIntervalInMilli, dataModels, error);
    }
}
