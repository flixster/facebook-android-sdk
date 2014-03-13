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

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import com.facebook.ads.internal.*;
import com.facebook.ads.internal.action.AdAction;
import com.facebook.ads.internal.action.AdActionFactory;

/**
 * View that displays advertisement by Facebook.
 * <p>
 * It can be declared and added to view:
 * <code>
 *     AdView adView = new AdView(context, "YOUR_PLACEMENT_ID", AdSize.BANNER_320_50);
 *     addView(adView);
 * </code>
 *
 * AdView requires android.permission.INTERNET permission to make network requests.
 * If android.permission.ACCESS_NETWORK_STATE permission is granted, AdView will only
 * make network requests when network is connected.
 */
public class AdView extends RelativeLayout implements Ad {

    private static final String TAG = AdView.class.getSimpleName();

    private static final String DEFAULT_MIME_TYPE = "text/html";
    private static final String DEFAULT_ENCODING = "utf-8";
    private static final String SUPER_STATE_KEY = "AdView_superState";
    private static final String LOADED_AD_DATA_MODEL_KEY = "AdView_loadedAdDataModel";
    private static final String AD_REQUEST_CONTROLLER_KEY = "AdView_adRequestController";

    private String placementId = null;
    private AdSize adSize = AdSize.BANNER_320_50;
    private AdListener adListener = null;

    private WebView adWebView;

    private HtmlAdDataModel loadedAdDataModel = null;

    private AdRequestController adRequestController;

    /**
     * Constructs an AdView using the given context, placement_id and size.
     * @param context Android context
     * @param placementId id of ad placement
     * @param adSize size of the ad control
     */
    public AdView(Context context, String placementId, AdSize adSize) {
        super(context);

        this.placementId = placementId;
        this.adSize = adSize;

        initializeView(context);
    }

    private void initializeView(Context context) {
        if (adSize == null) {
            throw new IllegalArgumentException("adSize");
        }

        RelativeLayout.LayoutParams layoutParams;
        if (adSize == AdSize.INTERSTITIAL) {
            layoutParams = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            float density = metrics.density;
            int screenWidth = (int) (metrics.widthPixels / density);
            layoutParams = new RelativeLayout.LayoutParams(
                    screenWidth >= adSize.getWidth() ?
                            metrics.widthPixels :
                            (int) Math.ceil(adSize.getWidth() * density),
                    (int) Math.ceil(adSize.getHeight() * density)
            );
        }
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        adWebView = new WebView(context);
        adWebView.setVisibility(GONE);
        adWebView.setLayoutParams(layoutParams);
        AdWebViewUtils.config(adWebView);
        adWebView.setWebViewClient(new AdWebViewClient());
        addView(adWebView);

        adRequestController = new AdRequestController(getContext(), placementId, adSize,
                true, AdType.HTML, createAdRequestCallback());
    }

    private AdRequest.Callback createAdRequestCallback() {
        return new AdRequest.Callback() {

            @Override
            public void onError(AdError error) {
                // Continue showing the previous ad in case of error.
                updateView();
                if (adListener != null) {
                    adListener.onError(AdView.this, error);
                }
            }

            @Override
            public void onCompleted(AdResponse adResponse) {
                AdDataModel dataModel = adResponse.getDataModel();
                if (dataModel != null && dataModel instanceof HtmlAdDataModel) {
                    // Received an ad
                    loadedAdDataModel = (HtmlAdDataModel) dataModel;
                    updateView();
                    if (adListener != null) {
                        adListener.onAdLoaded(AdView.this);
                    }
                } else if (dataModel == null) {
                    updateView();
                    if (adListener != null) {
                        adListener.onError(
                                AdView.this,
                                adResponse.getError() != null ? adResponse.getError() : AdError.INTERNAL_ERROR
                        );
                    }
                } else {
                    updateView();
                    if (adListener != null) {
                        adListener.onError(AdView.this, AdError.INTERNAL_ERROR);
                    }
                }
            }
        };
    }

    /**
     * Sets an AdListener to be notified on events happened in control lifecycle.
     * @param adListener the listener
     */
    public void setAdListener(AdListener adListener) {
        this.adListener = adListener;
    }

    @Override
    public void loadAd() {
        loadAd(null);
    }

    @Override
    public void loadAd(AdTargetingOptions adTargetingOptions) {
        ensureAdRequestController();
        adRequestController.loadAd(adTargetingOptions);
    }

    private void ensureAdRequestController() {
        if (adRequestController == null) {
            throw new RuntimeException("No request controller available, has the AdView been destroyed?");
        }
    }

    private void updateView() {
        if (loadedAdDataModel != null) {
            adWebView.loadUrl("about:blank");
            adWebView.setVisibility(GONE);
            adWebView.loadDataWithBaseURL(AdWebViewUtils.WEBVIEW_BASE_URL, loadedAdDataModel.getMarkup(), DEFAULT_MIME_TYPE, DEFAULT_ENCODING, null);
            adWebView.setVisibility(VISIBLE);
        } else {
            adWebView.setVisibility(GONE);
        }
    }

    @Override
    public void destroy() {
        if (adRequestController != null) {
            adRequestController.destroy();
            adRequestController = null;
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        Bundle instanceState = new Bundle();
        instanceState.putParcelable(SUPER_STATE_KEY, superState);
        instanceState.putParcelable(AD_REQUEST_CONTROLLER_KEY, adRequestController.onSaveInstanceState());
        if (loadedAdDataModel != null) {
            instanceState.putParcelable(LOADED_AD_DATA_MODEL_KEY, loadedAdDataModel.saveToBundle());
        }
        return instanceState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state.getClass() != Bundle.class) {
            super.onRestoreInstanceState(state);
        } else {
            Bundle instanceState = (Bundle)state;
            super.onRestoreInstanceState(instanceState.getParcelable(SUPER_STATE_KEY));

            Bundle adRequestControllerBundle = instanceState.getParcelable(AD_REQUEST_CONTROLLER_KEY);
            if (adRequestControllerBundle != null) {
                adRequestController.onRestoreInstanceState(adRequestControllerBundle);
            }

            Bundle loadedAdDataModelBundle = instanceState.getParcelable(LOADED_AD_DATA_MODEL_KEY);
            if (loadedAdDataModelBundle != null) {
                HtmlAdDataModel savedAdDataModel = null;
                try {
                    savedAdDataModel = HtmlAdDataModel.fromBundle(loadedAdDataModelBundle);
                } catch (Exception ex) {
                    Log.w(TAG, "Error restoring ad data model.", ex);
                }
                if (savedAdDataModel != null) {
                    loadedAdDataModel = savedAdDataModel;
                }
                updateView();
            }
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (adRequestController != null) {
            adRequestController.onWindowVisibilityChanged(visibility);
        }
    }

    private class AdWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (adListener != null) {
                adListener.onAdClicked(AdView.this);
            }

            Uri uri = Uri.parse(url);
            AdAction adAction = AdActionFactory.getAdAction(getContext(), uri);
            if (adAction != null) {
                try {
                    adAction.execute();
                } catch (Exception ex) {
                    Log.e(TAG, "Error executing action", ex);
                }
            }

            return true;
        }


    }
}
