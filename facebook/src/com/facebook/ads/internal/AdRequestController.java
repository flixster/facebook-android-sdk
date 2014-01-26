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
import android.util.Log;
import android.view.View;
import com.facebook.FacebookRequestError;
import com.facebook.RequestAsyncTask;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdTargetingOptions;

public class AdRequestController {

    private static final String TAG = AdRequestController.class.getSimpleName();
    private static final int REFRESH_INTERVAL_MIN_SECONDS = 30;

    private static final String REFRESH_INTERVAL_KEY = "AdRequestController_refreshInterval";
    private static final String INITIAL_LOAD_REQUESTED_KEY = "AdRequestController_initialLoadRequested";

    private final Context context;
    private final String placementId;
    private final AdSize adSize;
    private final String userAgentString;
    private final AdRequest.AdRequestListener adViewRequestListener;
    private boolean testMode;
    private boolean shouldRefresh = true;
    private AdTargetingOptions targetingOptions;
    private final ScreenStateReceiver screenStateReceiver = new ScreenStateReceiver();

    private int refreshInterval = REFRESH_INTERVAL_MIN_SECONDS;
    private boolean initialLoadRequested = false;

    private volatile boolean refreshScheduled = false;
    private Handler handler;
    private Runnable refreshRunnable;

    private RequestAsyncTask lastRequest;

    private int currentVisibility = View.GONE;

    public AdRequestController(Context context, String placementId, AdSize adSize, String userAgentString,
            AdRequest.AdRequestListener adViewRequestListener) {
        this.context = context;
        this.placementId = placementId;
        this.adSize = adSize;
        this.userAgentString = userAgentString;
        this.adViewRequestListener = adViewRequestListener;
        targetingOptions = null;

        handler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Refreshing ad");
                refreshScheduled = false;
                loadAd(targetingOptions);
            }
        };

        registerScreenStateReceiver();
    }

    private void registerScreenStateReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(screenStateReceiver, filter);
    }

    private void unregisterScreenStateReceiver() {
        context.unregisterReceiver(screenStateReceiver);
    }

    private synchronized void scheduleRefresh(String reason) {
        if (!shouldRefresh) {
            Log.d(TAG, "should not schedule refresh");
            return;
        }

        Log.d(TAG, "schedule refresh " + reason);
        if (refreshInterval > 0) {
            if (refreshScheduled) {
                return;
            }
            handler.postDelayed(refreshRunnable, refreshInterval * 1000);
            refreshScheduled = true;
        }
    }

    private synchronized void cancelRefresh(String reason) {
        Log.d(TAG, "cancel refresh " + reason);
        if (!refreshScheduled) {
            return;
        }
        handler.removeCallbacks(refreshRunnable);
        refreshScheduled = false;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public void setShouldRefresh(boolean shouldRefresh) {
        this.shouldRefresh = shouldRefresh;
        if (shouldRefresh) {
            scheduleRefresh("should start refresh");
        } else {
            cancelRefresh("should stop refresh");
        }
    }

    public void loadAd(AdTargetingOptions targetingOptions) {
        this.targetingOptions = targetingOptions;

        initialLoadRequested = true;

        if (lastRequest != null && lastRequest.getStatus() != AsyncTask.Status.FINISHED) {
            lastRequest.cancel(true);
        }

        if (!isNetworkConnected()) {
            refreshInterval = REFRESH_INTERVAL_MIN_SECONDS;
            adViewRequestListener.onError(new FacebookRequestError(
                    FacebookRequestError.INVALID_ERROR_CODE, "network unavailable", "network unavailable"));
            scheduleRefresh("no network connection");
            return;
        }

        AdRequest adRequest = new AdRequest(
                context,
                placementId,
                adSize,
                userAgentString,
                new AdRequest.AdRequestListener() {
                    @Override
                    public void onLoading() {
                        adViewRequestListener.onLoading();
                    }

                    @Override
                    public void onError(FacebookRequestError error) {
                        adViewRequestListener.onError(error);
                        scheduleRefresh("onError");
                    }

                    @Override
                    public void onCompleted(AdResponse adResponse) {
                        refreshInterval = adResponse.getRefreshInterval();
                        adViewRequestListener.onCompleted(adResponse);
                        scheduleRefresh("onCompleted");
                    }
                },
                testMode
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
        instanceState.putBoolean(INITIAL_LOAD_REQUESTED_KEY, initialLoadRequested);
        return instanceState;
    }

    public void onRestoreInstanceState(Bundle instanceState) {
        refreshInterval = instanceState.getInt(REFRESH_INTERVAL_KEY, REFRESH_INTERVAL_MIN_SECONDS);
        initialLoadRequested = instanceState.getBoolean(INITIAL_LOAD_REQUESTED_KEY, false);
    }

    public void onWindowVisibilityChanged(int visibility) {
        currentVisibility = visibility;
        if (visibility == View.VISIBLE) {
            // When the window first becomes visible (no ad has been requested), we don't want to schedule
            // a refresh at that time.
            // When returning to the app from play store, onRestoreInstanceState() is not called so we need
            // to schedule a refresh to keep new ads coming.
            if (initialLoadRequested) {
                scheduleRefresh("onWindowVisibilityChanged");
            }
        } else {
            cancelRefresh("onWindowVisibilityChanged");
        }
    }

    private boolean isNetworkConnected() {
        String permission = "android.permission.ACCESS_NETWORK_STATE";
        int result = context.checkCallingOrSelfPermission(permission);
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
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                cancelRefresh(intent.getAction());
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                if (currentVisibility == View.VISIBLE) {
                    scheduleRefresh(intent.getAction());
                }
            }
        }
    }
}
