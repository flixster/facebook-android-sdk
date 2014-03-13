package com.facebook.ads.internal;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
public class AppSiteData {

    private final String packageName;
    private final String className;
    private final String appLinkUri;
    private final List<String> keyHashes;
    private final String marketUri;
    private final String fallbackUrl;

    private AppSiteData(String packageName, String className, String appLinkUri, List<String> keyHashes,
                       String marketUri, String fallbackUrl) {
        this.packageName = packageName;
        this.className = className;
        this.appLinkUri = appLinkUri;
        this.keyHashes = keyHashes;
        this.marketUri = marketUri;
        this.fallbackUrl = fallbackUrl;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getAppLinkUri() {
        return appLinkUri;
    }

    public List<String> getKeyHashes() {
        return keyHashes;
    }

    public String getMarketUri() {
        return marketUri;
    }

    public String getFallbackUrl() {
        return fallbackUrl;
    }

    public static AppSiteData fromJSONObject(JSONObject dataObject) {
        if (dataObject == null) {
            return null;
        }

        String packageName = dataObject.optString("package");
        String appsite = dataObject.optString("appsite");
        String appsiteUri = dataObject.optString("appsite_url");
        JSONArray keyHashesArray = dataObject.optJSONArray("key_hashes");
        List<String> keyHashes = new ArrayList<String>();
        if (keyHashesArray != null) {
            for (int i = 0; i < keyHashesArray.length(); i++) {
                keyHashes.add(keyHashesArray.optString(i));
            }
        }
        String marketUri = dataObject.optString("market_uri");
        String fallbackUrl = dataObject.optString("fallback_url");
        return new AppSiteData(packageName, appsite, appsiteUri, keyHashes, marketUri, fallbackUrl);
    }

}
