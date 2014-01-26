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

public class AdDataModel {

    private static final String MARKUP_KEY = "markup";
    private static final String STORE_ID_KEY = "storeId";
    private static final String STORE_TYPE_KEY = "storeType";

    private final String markup;
    private final String storeId;
    private final String storeType;

    public AdDataModel(String markup, String storeId, String storeType) {
        this.markup = markup;
        this.storeId = storeId;
        this.storeType = storeType;
    }

    public String getMarkup() {
        return markup;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getStoreType() {
        return storeType;
    }

    public Bundle saveToBundle() {
        Bundle instanceState = new Bundle();
        instanceState.putString(MARKUP_KEY, markup);
        instanceState.putString(STORE_ID_KEY, storeId);
        instanceState.putString(STORE_TYPE_KEY, storeType);
        return instanceState;
    }

    public void addToIntentExtra(Intent intent) {
        intent.putExtra(MARKUP_KEY, markup);
        intent.putExtra(STORE_ID_KEY, storeId);
        intent.putExtra(STORE_TYPE_KEY, storeType);
    }

    public static AdDataModel fromBundle(Bundle instanceState) {
        String markup = instanceState.getString(MARKUP_KEY);
        String storeId = instanceState.getString(STORE_ID_KEY);
        String storeType = instanceState.getString(STORE_TYPE_KEY);
        return new AdDataModel(markup, storeId, storeType);
    }

    public static AdDataModel fromIntentExtra(Intent intent) {
        String markup = intent.getStringExtra(MARKUP_KEY);
        String storeId = intent.getStringExtra(STORE_ID_KEY);
        String storeType = intent.getStringExtra(STORE_TYPE_KEY);
        return new AdDataModel(markup, storeId, storeType);
    }
}
