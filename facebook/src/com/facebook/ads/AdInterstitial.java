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

package com.facebook.ads;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.webkit.WebView;
import com.facebook.FacebookRequestError;
import com.facebook.ads.internal.AdRequest;
import com.facebook.ads.internal.AdRequestController;
import com.facebook.ads.internal.AdResponse;

public class AdInterstitial implements Ad {

    public static final String AD_INTERSTITIAL_DISPLAYED = "com.facebook.ads.interstitial.displayed";
    public static final String AD_INTERSTITIAL_DISMISSED = "com.facebook.ads.interstitial.dismissed";
    public static final String AD_INTERSTITIAL_CLICKED = "com.facebook.ads.interstitial.clicked";
    public static final String AD_INTERSTITIAL_LOADED = "com.facebook.ads.interstitial.loaded";

    private final Context context;
    private final String placementId;
    private final AdRequestController adRequestController;
    private final AdInterstitialBroadcastReceiver broadcastReceiver;

    private AdListener adListener;
    private boolean loadRequested = false;
    private boolean adLoaded = false;
    private AdResponse adResponse;

    public AdInterstitial(Context context, String placementId) {
        this.context = context;
        this.placementId = placementId;

        WebView webView = new WebView(context);
        String userAgentString = webView.getSettings().getUserAgentString();
        webView.destroy();

        adRequestController = new AdRequestController(this.context, this.placementId, AdSize.INTERSTITIAL,
                userAgentString, new AdRequest.AdRequestListener() {
            @Override
            public void onLoading() {
                // Do nothing.
            }

            @Override
            public void onCompleted(AdResponse adResponse) {
                AdInterstitial.this.adResponse = adResponse;
                if (adResponse.getDataModel() != null) {
                    adLoaded = true;
                    if (adListener != null) {
                        adListener.onAdLoaded(AdInterstitial.this);
                    }

                    Intent intent = new Intent(AdInterstitial.AD_INTERSTITIAL_LOADED);
                    adResponse.getDataModel().addToIntentExtra(intent);
                    LocalBroadcastManager.getInstance(AdInterstitial.this.context).sendBroadcast(intent);
                } else {
                    if (adListener != null) {
                        adListener.onError(AdInterstitial.this, adResponse.getError());
                    }
                }
            }

            @Override
            public void onError(FacebookRequestError error) {
                adLoaded = false;
                if (adListener != null) {
                    adListener.onError(AdInterstitial.this, new AdError(error.getErrorCode(), error.getErrorMessage()));
                }
            }
        });
        adRequestController.setShouldRefresh(false);

        broadcastReceiver = new AdInterstitialBroadcastReceiver();
        broadcastReceiver.register();
    }

    @Override
    public void setAdListener(AdListener adListener) {
        this.adListener = adListener;
    }

    @Override
    public void setTestMode(boolean testMode) {
        adRequestController.setTestMode(true);
    }

    @Override
    public void loadAd() {
        loadAd(null);
    }

    @Override
    public void loadAd(AdTargetingOptions targetingOptions) {
        loadRequested = true;
        adLoaded = false;
        adRequestController.loadAd(targetingOptions);
    }

    @Override
    public void destroy() {
        broadcastReceiver.unregister();
    }

    public boolean isAdLoaded() {
        return adLoaded;
    }

    public void show() {
        broadcastReceiver.register();

        Intent intent = new Intent(context, AdInterstitialActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (adLoaded) {
            adResponse.getDataModel().addToIntentExtra(intent);
        }
        context.startActivity(intent);

        // Load the ad if not already requested.
        if (!loadRequested) {
            loadAd();
        }
    }

    private class AdInterstitialBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (adListener == null) {
                return;
            }

            String action = intent.getAction();
            if (action.equals(AD_INTERSTITIAL_CLICKED)) {
                adListener.onLeaveApplication(AdInterstitial.this);
            } else if (action.equals(AD_INTERSTITIAL_DISMISSED)) {
                adListener.onInterstitialDismissed(AdInterstitial.this);
            } else if (action.equals(AD_INTERSTITIAL_DISPLAYED)) {
                adListener.onInterstitialDisplayed(AdInterstitial.this);
            }
        }

        public void register() {
            IntentFilter interstitialIntentFilter = new IntentFilter();
            interstitialIntentFilter.addAction(AD_INTERSTITIAL_DISPLAYED);
            interstitialIntentFilter.addAction(AD_INTERSTITIAL_DISMISSED);
            interstitialIntentFilter.addAction(AD_INTERSTITIAL_CLICKED);
            LocalBroadcastManager.getInstance(context).registerReceiver(this, interstitialIntentFilter);
        }

        public void unregister() {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        }
    }
}
