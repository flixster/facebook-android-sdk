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

import android.content.Intent;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

public class HtmlAdDataModel implements AdDataModel {

    private static final String MARKUP_KEY = "markup";
    private static final String INVALIDATION_BEHAVIOR_KEY = "invalidation_behavior";
    private static final String DETECTION_STRINGS_KEY = "detection_strings";

    private final String markup;
    private final AdInvalidationBehavior invalidationBehavior;
    private final Collection<String> detectionStrings;

    private HtmlAdDataModel(String markup, AdInvalidationBehavior invalidationBehavior, Collection<String> detectionStrings) {
        this.markup = markup;
        this.invalidationBehavior = invalidationBehavior;
        this.detectionStrings = detectionStrings;
    }

    public String getMarkup() {
        return markup;
    }

    @Override
    public AdInvalidationBehavior getInvalidationBehavior() {
        return invalidationBehavior;
    }

    @Override
    public Collection<String> getDetectionStrings() {
        return detectionStrings;
    }

    public Bundle saveToBundle() {
        Bundle instanceState = new Bundle();
        instanceState.putString(MARKUP_KEY, markup);
        return instanceState;
    }

    public void addToIntentExtra(Intent intent) {
        intent.putExtra(MARKUP_KEY, markup);
    }

    public static HtmlAdDataModel fromBundle(Bundle instanceState) {
        String markup = instanceState.getString(MARKUP_KEY);
        return new HtmlAdDataModel(markup, AdInvalidationBehavior.NONE, null);
    }

    public static HtmlAdDataModel fromIntentExtra(Intent intent) {
        String markup = intent.getStringExtra(MARKUP_KEY);
        return new HtmlAdDataModel(markup, AdInvalidationBehavior.NONE, null);
    }

    public static HtmlAdDataModel fromJSONObject(JSONObject dataObject) {
        if (dataObject == null) {
            return null;
        }

        String markup = dataObject.optString(MARKUP_KEY);
        AdInvalidationBehavior invalidationBehavior =
                AdInvalidationBehavior.fromString(dataObject.optString(INVALIDATION_BEHAVIOR_KEY));
        JSONArray detectionStringsArray = null;
        try {
            detectionStringsArray = new JSONArray(dataObject.optString(DETECTION_STRINGS_KEY));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collection<String> detectionStrings =
                AdInvalidationUtils.parseDetectionStrings(detectionStringsArray);
        return new HtmlAdDataModel(markup, invalidationBehavior, detectionStrings);
    }
}
