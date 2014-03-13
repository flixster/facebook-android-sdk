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

package com.facebook.ads.internal.action;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import com.facebook.ads.internal.AdInvalidationUtils;
import com.facebook.ads.internal.AppSiteData;
import com.facebook.ads.internal.OpenUrlTask;
import com.facebook.ads.internal.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AppAdAction implements AdAction {

    private static final String TAG = AppAdAction.class.getSimpleName();

    private static final String MARKET_DETAIL_URI_FORMAT = "market://details?id=%s";

    private final Context context;
    private final Uri uri;

    public AppAdAction(Context context, Uri uri) {
        this.context = context;
        this.uri = uri;
    }

    private Uri getMarketUri() {
        String storeId = uri.getQueryParameter("store_id");
        return Uri.parse(String.format(MARKET_DETAIL_URI_FORMAT, storeId));
    }

    private List<AppSiteData> getAppsiteDatas() {
        String appsiteDataString = uri.getQueryParameter("appsite_data");
        if (StringUtils.isNullOrEmpty(appsiteDataString) || "[]".equals(appsiteDataString)) {
            return null;
        }

        List<AppSiteData> appSiteDatas = new ArrayList<AppSiteData>();
        try {
            JSONObject dataObject = new JSONObject(appsiteDataString);
            JSONArray appsiteDataArray = dataObject.optJSONArray("android");
            if (appsiteDataArray != null) {
                for (int i = 0; i < appsiteDataArray.length(); i++) {
                    AppSiteData appSiteData = AppSiteData.fromJSONObject(appsiteDataArray.optJSONObject(i));
                    if (appSiteData != null) {
                        appSiteDatas.add(appSiteData);
                    }
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Error parsing appsite_data", e);
        }
        return appSiteDatas;
    }

    private void logAdClick() {
        String clickReportUrl = uri.getQueryParameter("native_click_report_url");
        if (StringUtils.isNullOrEmpty(clickReportUrl)) {
            return;
        }
        new OpenUrlTask(context).execute(clickReportUrl);
    }

    private Intent getAppLinkIntentUnresolved(AppSiteData appSiteData) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (!StringUtils.isNullOrEmpty(appSiteData.getPackageName()) &&
                !StringUtils.isNullOrEmpty(appSiteData.getClassName())) {
            intent.setComponent(new ComponentName(appSiteData.getPackageName(), appSiteData.getClassName()));
        }

        if (!StringUtils.isNullOrEmpty(appSiteData.getAppLinkUri())) {
            intent.setData(Uri.parse(appSiteData.getAppLinkUri()));
        }

        return intent;
    }

    private Intent getAppLaunchIntent(AppSiteData appSiteData) {
        if (StringUtils.isNullOrEmpty(appSiteData.getPackageName())) {
            return null;
        }

        if (!AdInvalidationUtils.isNativePackageInstalled(context, appSiteData.getPackageName())) {
            return null;
        }

        // if this is click to call, we have a special case path to hit the registered dialer.
        String appLinkUri = appSiteData.getAppLinkUri();
        if (!StringUtils.isNullOrEmpty(appLinkUri) &&
                (appLinkUri.startsWith("tel:") || appLinkUri.startsWith("telprompt:"))) {
            return new Intent(Intent.ACTION_CALL, Uri.parse(appLinkUri));
        }

        PackageManager pm = context.getPackageManager();
        // if there is no class or deep link, this is a launch by itself.
        if (StringUtils.isNullOrEmpty(appSiteData.getClassName()) &&
                StringUtils.isNullOrEmpty(appLinkUri)) {
            return pm.getLaunchIntentForPackage(appSiteData.getPackageName());
        }

        Intent unresolvedAppIntent = getAppLinkIntentUnresolved(appSiteData);
        List<ResolveInfo> resolved = pm.queryIntentActivities(
                unresolvedAppIntent,
                PackageManager.MATCH_DEFAULT_ONLY
        );

        if (unresolvedAppIntent.getComponent() == null) {
            for (ResolveInfo ri : resolved) {
                if (ri.activityInfo.packageName.equals(appSiteData.getPackageName())) {
                    unresolvedAppIntent.setComponent(
                            new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
                    );
                    break;
                }
            }
        }

        if (resolved.isEmpty() || unresolvedAppIntent.getComponent() == null) {
            return null;
        }

        return unresolvedAppIntent;
    }

    private List<Intent> getAppLaunchIntents() {
        List<AppSiteData> appSiteDatas = getAppsiteDatas();
        List<Intent> intents = new ArrayList<Intent>();
        if (appSiteDatas != null) {
            for (AppSiteData appSiteData : appSiteDatas) {
                Intent intent = getAppLaunchIntent(appSiteData);
                if (intent != null) {
                    intents.add(intent);
                }
            }
        }
        return intents;
    }

    @Override
    public void execute() {
        logAdClick();

        List<Intent> appLaunchIntents = getAppLaunchIntents();
        if (appLaunchIntents != null) {
            for (Intent appLaunchIntent : appLaunchIntents) {
                try {
                    context.startActivity(appLaunchIntent);
                    return;
                } catch (Exception ex) {
                    Log.d(TAG, "Failed to open app intent, falling back", ex);
                }
            }
        }

        Uri marketUri = getMarketUri();
        Intent intent = new Intent(Intent.ACTION_VIEW, marketUri);
        try {
            context.startActivity(intent);
        } catch (Exception ex) {
            Log.d(TAG, "Failed to open market url: " + uri.toString(), ex);
            String fallbackUrl = uri.getQueryParameter("store_url_web_fallback");
            if (fallbackUrl != null && fallbackUrl.length() > 0) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl));
                try {
                    context.startActivity(intent);
                } catch (Exception ex2) {
                    Log.d(TAG, "Failed to open fallback url: " + fallbackUrl, ex2);
                }
            }
        }
    }
}
