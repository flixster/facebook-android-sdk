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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import com.facebook.ads.internal.AdWebViewUtils;
import com.facebook.ads.internal.HtmlAdDataModel;
import com.facebook.ads.internal.action.AdAction;
import com.facebook.ads.internal.action.AdActionFactory;

/**
 * InterstitialAdActivity is the Android activity used by InterstitialAd to display the ad.
 */
public class InterstitialAdActivity extends Activity {

    private static final String TAG = InterstitialAdActivity.class.getSimpleName();

    private static final String DATA_MODEL_KEY = "dataModel";
    private static final String LAST_REQUESTED_ORIENTATION_KEY = "lastRequestedOrientation";

    private static final int AD_WEBVIEW_ID = 100001;

    // Reverse portrait/landscape was added in api level 9 in ActivityInfo, redefine to compile for api level 8.
    private static final int ORIENTATION_REVERSE_PORTRAIT = 9;
    private static final int ORIENTATION_REVERSE_LANDSCAPE = 8;

    private WebView adWebView;

    private int lastRequestedOrientation;
    private int displayWidth;
    private int displayHeight;

    private boolean isRestart = false;

    private HtmlAdDataModel dataModel;

    private String placementId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide title and nav bar, must be done before setContentView.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        RelativeLayout relativelayout = new RelativeLayout(this);

        adWebView = new WebView(this);
        adWebView.setId(AD_WEBVIEW_ID);
        adWebView.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        ));
        AdWebViewUtils.config(adWebView);
        adWebView.setWebViewClient(new AdWebViewClient());
        relativelayout.addView(adWebView);

        setContentView(relativelayout, new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        ));

        Intent intent = getIntent();
        loadAdFromIntentOrSavedState(intent, savedInstanceState);

        sendBroadcastForEvent(InterstitialAd.INTERSTITIAL_DISPLAYED);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!isRestart) {
            setScreenOrientation(displayWidth, displayHeight);
        } else if (lastRequestedOrientation >= 0) {
            setRequestedOrientation(lastRequestedOrientation);
            lastRequestedOrientation = -1;
        }
        isRestart = false;
    }

    public void setRequestedOrientation(int requestedOrientation) {
        lastRequestedOrientation = requestedOrientation;
        super.setRequestedOrientation(requestedOrientation);
    }

    private void setScreenOrientation(int displayWidth, int displayHeight) {
        boolean defaultInPortrait = displayHeight >= displayWidth;
        int currentOrientation = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        // Must call setRequestedOrientation once to lock the orientation of current activity.
        if (defaultInPortrait) {
            switch (currentOrientation) {
                case Surface.ROTATION_90:
                case Surface.ROTATION_180:
                    setRequestedOrientation(ORIENTATION_REVERSE_PORTRAIT);
                    break;
                case Surface.ROTATION_270:
                default :
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            switch (currentOrientation) {
                case Surface.ROTATION_180:
                case Surface.ROTATION_270:
                    setRequestedOrientation(ORIENTATION_REVERSE_LANDSCAPE);
                    break;
                case Surface.ROTATION_90:
                default :
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (dataModel != null) {
            savedInstanceState.putBundle(DATA_MODEL_KEY, dataModel.saveToBundle());
        }
        savedInstanceState.putInt(LAST_REQUESTED_ORIENTATION_KEY, lastRequestedOrientation);
        savedInstanceState.putString(InterstitialAd.INTERSTITIAL_UNIQUE_ID_EXTRA, placementId);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        isRestart = true;
    }

    private void loadAdFromIntentOrSavedState(Intent intent, Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(DATA_MODEL_KEY)) {
            dataModel = HtmlAdDataModel.fromBundle(savedInstanceState.getBundle(DATA_MODEL_KEY));
            if (dataModel != null) {
                adWebView.loadDataWithBaseURL(AdWebViewUtils.WEBVIEW_BASE_URL, dataModel.getMarkup(), "text/html", "utf-8", null);
            }
            lastRequestedOrientation = savedInstanceState.getInt(LAST_REQUESTED_ORIENTATION_KEY, -1);
            placementId = savedInstanceState.getString(InterstitialAd.INTERSTITIAL_UNIQUE_ID_EXTRA);
            isRestart = true;
            return;
        }

        displayWidth = intent.getIntExtra(InterstitialAd.DISPLAY_WIDTH_INTENT_EXTRA, 0);
        displayHeight = intent.getIntExtra(InterstitialAd.DISPLAY_HEIGHT_INTENT_EXTRA, 0);
        placementId = intent.getStringExtra(InterstitialAd.INTERSTITIAL_UNIQUE_ID_EXTRA);
        dataModel = HtmlAdDataModel.fromIntentExtra(intent);
        if (dataModel != null) {
            adWebView.loadDataWithBaseURL(AdWebViewUtils.WEBVIEW_BASE_URL, dataModel.getMarkup(), "text/html", "utf-8", null);
        }
    }

    @Override
    public void finish() {
        sendBroadcastForEvent(InterstitialAd.INTERSTITIAL_DISMISSED);
        super.finish();
    }

    private void sendBroadcastForEvent(String event) {
        Intent intent = new Intent(event);
        intent.putExtra(InterstitialAd.INTERSTITIAL_UNIQUE_ID_EXTRA, placementId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private class AdWebViewClient extends WebViewClient {

        private static final String FBAD_CLOSE = "close";

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if (AdWebViewUtils.FBAD_SCHEME.equals(uri.getScheme()) && FBAD_CLOSE.equals(uri.getAuthority())) {
                InterstitialAdActivity.this.finish();
                return true;
            }

            sendBroadcastForEvent(InterstitialAd.INTERSTITIAL_CLICKED);

            AdAction adAction = AdActionFactory.getAdAction(InterstitialAdActivity.this, uri);
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
