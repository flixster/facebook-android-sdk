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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdTargetingOptions;

public class AdRequestController {

    private static final String TAG = AdRequestController.class.getSimpleName();
    private static final int MIN_REFRESH_INTERVAL_MILLIS = 30000;

    private static final String REFRESH_INTERVAL_KEY = "AdRequestController_refreshInterval";
    private static final String INITIAL_LOAD_FINISHED_KEY = "AdRequestController_initialLoadFinished";

    private static final String ANDROID_PERMISSION_ACCESS_NETWORK_STATE = "android.permission.ACCESS_NETWORK_STATE";

    private final Context context;
    private final String placementId;
    private final AdSize adSize;
    private final AdRequest.Callback adViewRequestCallback;
    private final AdType adType;
    private final boolean shouldRefresh;
    private AdTargetingOptions targetingOptions;
    private final ScreenStateReceiver screenStateReceiver;

    private int refreshInterval = MIN_REFRESH_INTERVAL_MILLIS;
    private boolean initialLoadFinished = false;

    private volatile boolean refreshScheduled = false;
    private final Handler handler;
    private final Runnable refreshRunnable;

    private AsyncTask lastRequest;

    private int currentVisibility = View.GONE;

    public AdRequestController(Context context, String placementId, AdSize adSize,
            boolean shouldRefresh, AdType adType, AdRequest.Callback adViewRequestCallback) {
        this.context = context;
        this.placementId = placementId;
        this.adSize = adSize;
        this.shouldRefresh = shouldRefresh;
        this.adType = adType;
        this.adViewRequestCallback = adViewRequestCallback;
        targetingOptions = null;
        screenStateReceiver = new ScreenStateReceiver();

        handler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshScheduled = false;
                loadAd(targetingOptions);
            }
        };

        registerScreenStateReceiver();
    }

    private void registerScreenStateReceiver() {
        if (!shouldRefresh) {
            return;
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(screenStateReceiver, filter);
    }

    private void unregisterScreenStateReceiver() {
        if (!shouldRefresh) {
            return;
        }

        context.unregisterReceiver(screenStateReceiver);
    }

    private synchronized void scheduleRefresh(String reason) {
        if (!shouldRefresh) {
            return;
        }

        if (refreshInterval > 0) {
            if (refreshScheduled) {
                return;
            }
            handler.postDelayed(refreshRunnable, refreshInterval);
            refreshScheduled = true;
        }
    }

    private synchronized void cancelRefresh(String reason) {
        if (!refreshScheduled) {
            return;
        }
        handler.removeCallbacks(refreshRunnable);
        refreshScheduled = false;
    }

    public void loadAd(AdTargetingOptions targetingOptions) {
        this.targetingOptions = targetingOptions;

        if (lastRequest != null && lastRequest.getStatus() != AsyncTask.Status.FINISHED) {
            lastRequest.cancel(true);
        }

        if (!isNetworkConnected()) {
            refreshInterval = MIN_REFRESH_INTERVAL_MILLIS;
            adViewRequestCallback.onError(new AdError(AdError.INVALID_ERROR_CODE, "network unavailable"));
            scheduleRefresh("no network connection");
            return;
        }

        AdRequest adRequest = new AdRequest(
                context,
                placementId,
                adSize,
                adType,
                AdSettings.isTestMode(context),
                new AdRequest.Callback() {

                    @Override
                    public void onError(AdError error) {
                        adViewRequestCallback.onError(error);
                        initialLoadFinished = true;
                        scheduleRefresh("onError");
                    }

                    @Override
                    public void onCompleted(AdResponse adResponse) {
                        refreshInterval = adResponse.getRefreshIntervalMillis();
                        adViewRequestCallback.onCompleted(adResponse);
                        initialLoadFinished = true;
                        scheduleRefresh("onCompleted");
                    }
                }
        );
        lastRequest = adRequest.executeAsync();
    }

    public void destroy() {
        unregisterScreenStateReceiver();
        cancelRefresh("destroy");
    }

    public Bundle onSaveInstanceState() {
        Bundle instanceState = new Bundle();
        instanceState.putInt(REFRESH_INTERVAL_KEY, refreshInterval);
        instanceState.putBoolean(INITIAL_LOAD_FINISHED_KEY, initialLoadFinished);
        return instanceState;
    }

    public void onRestoreInstanceState(Bundle instanceState) {
        refreshInterval = instanceState.getInt(REFRESH_INTERVAL_KEY, MIN_REFRESH_INTERVAL_MILLIS);
        initialLoadFinished = instanceState.getBoolean(INITIAL_LOAD_FINISHED_KEY, false);
    }

    public void onWindowVisibilityChanged(int visibility) {
        currentVisibility = visibility;
        if (visibility == View.VISIBLE) {
            // When the window first becomes visible (no ad has been requested), we don't want to schedule
            // a refresh at that time. Wait until the initial load has finished.
            // When returning to the app from play store, onRestoreInstanceState() is not called so we need
            // to schedule a refresh to keep new ads coming.
            if (initialLoadFinished) {
                scheduleRefresh("onWindowVisibilityChanged");
            }
        } else {
            cancelRefresh("onWindowVisibilityChanged");
        }
    }

    private boolean isNetworkConnected() {
        int result = context.checkCallingOrSelfPermission(ANDROID_PERMISSION_ACCESS_NETWORK_STATE);
        if (result != PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                cancelRefresh(intent.getAction());
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                if (currentVisibility == View.VISIBLE) {
                    scheduleRefresh(intent.getAction());
                }
            }
        }
    }
}
