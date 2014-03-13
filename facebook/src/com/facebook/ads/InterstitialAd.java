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
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import com.facebook.ads.internal.*;

import java.util.UUID;

/**
 * InterstitialAd displays advertisement by Facebook in an interstitial.
 * <p>
 * To use the interstitial, do the following:
 * <code>
 *     InterstitialAd interstitial = new InterstitialAd(context, PLACEMENT_ID);
 *     interstitial.setAdListener(new InterstitialAdListener() {
 *         public void onAdLoaded(Ad ad) {
 *             if (ad == interstitial) {
 *                 interstitial.show();
 *             }
 *         }
 *         // rest of InterstitialAdListener implementation omitted
 *     });
 *     interstitial.loadAd();
 * </code>
 */
public class InterstitialAd implements Ad {

    public static final String INTERSTITIAL_DISPLAYED = "com.facebook.ads.interstitial.displayed";
    public static final String INTERSTITIAL_DISMISSED = "com.facebook.ads.interstitial.dismissed";
    public static final String INTERSTITIAL_CLICKED = "com.facebook.ads.interstitial.clicked";

    public static final String INTERSTITIAL_UNIQUE_ID_EXTRA = "adInterstitialUniqueId";
    public static final String DISPLAY_ROTATION_INTENT_EXTRA = "displayRotation";
    public static final String DISPLAY_WIDTH_INTENT_EXTRA = "displayWidth";
    public static final String DISPLAY_HEIGHT_INTENT_EXTRA = "displayHeight";

    private final Context context;
    private final String uniqueId;
    private AdRequestController adRequestController;
    private final AdInterstitialBroadcastReceiver broadcastReceiver;

    private InterstitialAdListener adListener;
    private boolean adLoaded = false;
    private AdResponse adResponse;

    /**
     * Constructs an InterstitialAd using the given context and placement id.
     * @param context android context
     * @param placementId id of ad placement
     */
    public InterstitialAd(Context context, String placementId) {
        this.context = context;
        uniqueId = UUID.randomUUID().toString();

        adRequestController = new AdRequestController(this.context, placementId, AdSize.INTERSTITIAL,
                false, AdType.HTML, new AdRequest.Callback() {

            @Override
            public void onCompleted(AdResponse adResponse) {
                InterstitialAd.this.adResponse = adResponse;
                if (adResponse.getDataModel() != null && adResponse.getDataModel() instanceof HtmlAdDataModel) {
                    adLoaded = true;
                    if (adListener != null) {
                        adListener.onAdLoaded(InterstitialAd.this);
                    }
                } else if (adResponse.getDataModel() == null) {
                    adLoaded = false;
                    if (adListener != null) {
                        adListener.onError(
                                InterstitialAd.this,
                                adResponse.getError() != null ? adResponse.getError() : AdError.INTERNAL_ERROR
                        );
                    }
                } else {
                    adLoaded = false;
                    if (adListener != null) {
                        adListener.onError(InterstitialAd.this, AdError.INTERNAL_ERROR);
                    }
                }
            }

            @Override
            public void onError(AdError error) {
                adLoaded = false;
                if (adListener != null) {
                    adListener.onError(InterstitialAd.this, error);
                }
            }
        });

        broadcastReceiver = new AdInterstitialBroadcastReceiver();
        broadcastReceiver.register();
    }

    private void ensureAdRequestController() {
        if (adRequestController == null) {
            throw new RuntimeException("No request controller available, has the InterstitialAd been destroyed?");
        }
    }

    /**
     * Sets an InterstitialAdListener to be notified on events happened in control lifecycle.
     * @param adListener the listener
     */
    public void setAdListener(InterstitialAdListener adListener) {
        this.adListener = adListener;
    }

    @Override
    public void loadAd() {
        loadAd(null);
    }

    @Override
    public void loadAd(AdTargetingOptions targetingOptions) {
        ensureAdRequestController();
        adLoaded = false;
        adRequestController.loadAd(targetingOptions);
    }

    @Override
    public void destroy() {
        if (adRequestController != null) {
            adRequestController.destroy();
            adRequestController = null;
            broadcastReceiver.unregister();
        }
    }

    /**
     * Gets whether an ad is loaded and ready to show.
     * @return whether an ad is loaded
     */
    public boolean isAdLoaded() {
        return adLoaded;
    }

    /**
     * Shows the interstitial ad. If no ad is loaded, returns false.
     */
    public boolean show() {
        if (!adLoaded) {
            if (adListener != null) {
                adListener.onError(this, new AdError(2001,
                        "Interstitial ad units must be loaded with loadAd() before calling show()"));
            }
            return false;
        }

        Intent intent = new Intent(context, InterstitialAdActivity.class);
        if (adLoaded) {
            ((HtmlAdDataModel) adResponse.getDataModel()).addToIntentExtra(intent);
        }

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        intent.putExtra(DISPLAY_ROTATION_INTENT_EXTRA, display.getRotation());
        intent.putExtra(DISPLAY_WIDTH_INTENT_EXTRA, displayMetrics.widthPixels);
        intent.putExtra(DISPLAY_HEIGHT_INTENT_EXTRA, displayMetrics.heightPixels);
        intent.putExtra(INTERSTITIAL_UNIQUE_ID_EXTRA, uniqueId);
        context.startActivity(intent);

        return true;
    }

    private class AdInterstitialBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (adListener == null) {
                return;
            }

            String uniqueId = intent.getStringExtra(INTERSTITIAL_UNIQUE_ID_EXTRA);
            if (!InterstitialAd.this.uniqueId.equals(uniqueId)) {
                return;
            }

            String action = intent.getAction();
            if (INTERSTITIAL_CLICKED.equals(action)) {
                adListener.onAdClicked(InterstitialAd.this);
            } else if (INTERSTITIAL_DISMISSED.equals(action)) {
                adListener.onInterstitialDismissed(InterstitialAd.this);
            } else if (INTERSTITIAL_DISPLAYED.equals(action)) {
                adListener.onInterstitialDisplayed(InterstitialAd.this);
            }
        }

        public void register() {
            IntentFilter interstitialIntentFilter = new IntentFilter();
            interstitialIntentFilter.addAction(INTERSTITIAL_DISPLAYED);
            interstitialIntentFilter.addAction(INTERSTITIAL_DISMISSED);
            interstitialIntentFilter.addAction(INTERSTITIAL_CLICKED);
            LocalBroadcastManager.getInstance(context).registerReceiver(this, interstitialIntentFilter);
        }

        public void unregister() {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        }
    }
}
