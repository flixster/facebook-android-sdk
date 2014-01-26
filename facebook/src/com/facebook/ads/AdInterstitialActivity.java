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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import com.facebook.ads.internal.AdDataModel;
import com.facebook.ads.internal.AdResponse;
import com.facebook.ads.internal.AdWebViewUtils;
import com.facebook.android.R;

public class AdInterstitialActivity extends Activity {

    private static final String TAG = AdInterstitialActivity.class.getSimpleName();

    private WebView adWebView;
    private ImageButton closeButton;
    private AdInterstitialActivityBroadcastReceiver broadcastReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        broadcastReceiver = new AdInterstitialActivityBroadcastReceiver();
        broadcastReceiver.register();

        // Hide title and nav bar, must be done before setContentView.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.com_facebook_ad_interstitial_layout);

        adWebView = (WebView)findViewById(R.id.adWebView);
        closeButton = (ImageButton)findViewById(R.id.closeButton);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        AdWebViewUtils.config(adWebView);
        adWebView.setWebViewClient(new AdWebViewClient());

        Intent intent = getIntent();
        loadAdFromIntent(intent);

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(AdInterstitial.AD_INTERSTITIAL_DISPLAYED));
    }

    private void loadAdFromIntent(Intent intent) {
        AdDataModel dataModel = AdDataModel.fromIntentExtra(intent);
        if (dataModel != null) {
            AdResponse response = new AdResponse(0, dataModel, null);
            adWebView.loadDataWithBaseURL(null, response.getDataModel().getMarkup(), "text/html", "utf-8", null);
        }
    }

    @Override
    public void finish() {
        broadcastReceiver.unregister();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(AdInterstitial.AD_INTERSTITIAL_DISMISSED));
        super.finish();
    }

    private class AdWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            LocalBroadcastManager.getInstance(AdInterstitialActivity.this)
                    .sendBroadcast(new Intent(AdInterstitial.AD_INTERSTITIAL_CLICKED));
            // Open URL using default browser.
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                AdInterstitialActivity.this.startActivity(intent);
            } catch (Exception ex) {
                Log.d(TAG, "Failed to open ad url: " + url, ex);
            }
            return true;
        }
    }

    private class AdInterstitialActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadAdFromIntent(intent);
        }

        public void register() {
            IntentFilter adLoadedIntentFilter = new IntentFilter();
            adLoadedIntentFilter.addAction(AdInterstitial.AD_INTERSTITIAL_LOADED);
            LocalBroadcastManager.getInstance(AdInterstitialActivity.this).registerReceiver(this, adLoadedIntentFilter);

        }

        public void unregister() {
            LocalBroadcastManager.getInstance(AdInterstitialActivity.this).unregisterReceiver(this);
        }
    }
}
