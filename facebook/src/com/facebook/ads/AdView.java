/**
 * Copyright 2010-present Facebook.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.facebook.ads;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.facebook.FacebookRequestError;
import com.facebook.ads.internal.AdDataModel;
import com.facebook.ads.internal.AdRequest;
import com.facebook.ads.internal.AdRequestController;
import com.facebook.ads.internal.AdResponse;
import com.facebook.ads.internal.AdWebViewUtils;

/**
 * View that displays advertisement by Facebook.
 * 
 * It can be included in layout XML file: <code>
 *   <com.facebook.ads.AdView
 *         android:id="@+id/adView"
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         ad:placement_id="PLACEMENT_ID"
 *         ad:ad_size="BANNER_320_50" />
 * </code>
 * 
 * It can also be declared and added to view: <code>
 *     AdView adView = new AdView(context, "PLACEMENT_ID", AdSize.BANNER_320_50);
 *     addView(adView);
 * </code>
 * 
 * AdView requires android.permission.INTERNET permission to make network requests. If
 * android.permission.ACCESS_NETWORK_STATE permission is granted, AdView will only make network requests when network is
 * connected.
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

    private AdDataModel loadedAdDataModel = null;

    private final String userAgentString;
    private final AdRequestController adRequestController;

    public AdView(Context context) {
        this(context, null, AdSize.BANNER_320_50);
    }

    /*
    public AdView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.com_facebook_ad_view);
            try {
                placementId = a.getString(R.styleable.com_facebook_ad_view_placement_id);

                String adSizeString = a.getString(R.styleable.com_facebook_ad_view_ad_size);
                adSize = AdSize.valueOf(adSizeString.toUpperCase());
            } finally {
                a.recycle();
            }
        }

        initializeView(context);
        userAgentString = adWebView.getSettings().getUserAgentString();
        adRequestController = new AdRequestController(getContext(), placementId, adSize, userAgentString,
                createAdRequestListener());
    }
    */
    public AdView(Context context, String placementId, AdSize adSize) {
        super(context);

        this.placementId = placementId;
        this.adSize = adSize;

        initializeView(context);
        userAgentString = adWebView.getSettings().getUserAgentString();
        adRequestController = new AdRequestController(getContext(), placementId, adSize, userAgentString,
                createAdRequestListener());
    }

    private void initializeView(Context context) {
        if (adSize == null) {
            throw new IllegalArgumentException("adSize");
        }

        RelativeLayout.LayoutParams layoutParams;
        if (adSize == AdSize.INTERSTITIAL) {
            layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            float density = context.getResources().getDisplayMetrics().density;
            layoutParams = new RelativeLayout.LayoutParams((int) Math.ceil(adSize.getWidth() * density),
                    (int) Math.ceil(adSize.getHeight() * density));
        }
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        adWebView = new WebView(context);
        adWebView.setVisibility(GONE);
        adWebView.setLayoutParams(layoutParams);
        AdWebViewUtils.config(adWebView);
        adWebView.setWebViewClient(new AdWebViewClient());
        addView(adWebView);
    }

    private AdRequest.AdRequestListener createAdRequestListener() {
        return new AdRequest.AdRequestListener() {
            @Override
            public void onLoading() {
                // Do nothing.
            }

            @Override
            public void onError(FacebookRequestError error) {
                // Continue showing the previous ad in case of error.
                updateView();
                if (adListener != null) {
                    adListener.onError(AdView.this, new AdError(error.getErrorCode(), error.getErrorMessage()));
                }
            }

            @Override
            public void onCompleted(AdResponse adResponse) {
                AdDataModel adDataModel = adResponse.getDataModel();
                if (adDataModel != null) {
                    // Received an ad
                    loadedAdDataModel = adDataModel;
                    updateView();
                    if (adListener != null) {
                        adListener.onAdLoaded(AdView.this);
                    }
                } else {
                    Log.d(TAG, "Ad request succeeded but no ad at this time.");
                    updateView();
                    if (adListener != null) {
                        adListener.onError(AdView.this, adResponse.getError());
                    }
                }
            }
        };
    }

    @Override
    public void setAdListener(AdListener adListener) {
        this.adListener = adListener;
    }

    @Override
    public void setTestMode(boolean testMode) {
        adRequestController.setTestMode(testMode);
    }

    @Override
    public void loadAd() {
        loadAd(null);
    }

    @Override
    public void loadAd(AdTargetingOptions adTargetingOptions) {
        adRequestController.loadAd(adTargetingOptions);
    }

    private void updateView() {
        if (loadedAdDataModel != null) {
            adWebView.clearView();
            adWebView.setVisibility(GONE);
            adWebView.loadDataWithBaseURL(null, loadedAdDataModel.getMarkup(), DEFAULT_MIME_TYPE, DEFAULT_ENCODING,
                    null);
            adWebView.setVisibility(VISIBLE);
        } else {
            adWebView.setVisibility(GONE);
        }
    }

    @Override
    public void destroy() {
        adRequestController.destroy();
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
            Bundle instanceState = (Bundle) state;
            super.onRestoreInstanceState(instanceState.getParcelable(SUPER_STATE_KEY));

            Bundle adRequestControllerBundle = instanceState.getParcelable(AD_REQUEST_CONTROLLER_KEY);
            if (adRequestControllerBundle != null) {
                adRequestController.onRestoreInstanceState(adRequestControllerBundle);
            }

            Bundle loadedAdDataModelBundle = instanceState.getParcelable(LOADED_AD_DATA_MODEL_KEY);
            if (loadedAdDataModelBundle != null) {
                AdDataModel savedAdDataModel = null;
                try {
                    savedAdDataModel = AdDataModel.fromBundle(loadedAdDataModelBundle);
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
        adRequestController.onWindowVisibilityChanged(visibility);
    }

    private class AdWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (adListener != null) {
                adListener.onLeaveApplication(AdView.this);
            }
            // Open URL using default browser.
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                getContext().startActivity(intent);
            } catch (Exception ex) {
                Log.d(TAG, "Failed to open ad url: " + url, ex);
            }
            return true;
        }
    }
}
