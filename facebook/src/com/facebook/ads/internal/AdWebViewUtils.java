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

import android.content.Context;
import android.webkit.WebView;

public class AdWebViewUtils {

	public static final String WEBVIEW_BASE_URL = "http://www.facebook.com/";

    public static final String FBAD_SCHEME = "fbad";

    private static String userAgentString = null;

    public static void config(WebView adWebView) {
        adWebView.getSettings().setJavaScriptEnabled(true);
        adWebView.getSettings().setSupportZoom(false);
        adWebView.setHorizontalScrollBarEnabled(false);
        adWebView.setHorizontalScrollbarOverlay(false);
        adWebView.setVerticalScrollBarEnabled(false);
        adWebView.setVerticalScrollbarOverlay(false);
        adWebView.addJavascriptInterface(new AdWebViewInterface(adWebView.getContext()), "AdControl");
    }

    public static String getUserAgentString(Context context) {
        if (userAgentString == null) {
            WebView webView = new WebView(context.getApplicationContext());
            userAgentString = webView.getSettings().getUserAgentString();
            webView.destroy();
        }
        return userAgentString;
    }
}
