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
import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.net.URLEncoder;
import java.util.Map;

public class OpenUrlTask extends AsyncTask<String, Void, Void> {

    private static final String TAG = OpenUrlTask.class.getSimpleName();
    private static final String INVALID_ADDRESS = "#";

    private final Context context;

    public OpenUrlTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(String... urls) {
        String url = urls[0];
        if (StringUtils.isNullOrEmpty(url) || url.equals(INVALID_ADDRESS)) {
            return null;
        }
        url = addAnalogInfo(url);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        try {
            client.execute(get);
        } catch (Exception ex) {
            Log.e(TAG, "Error opening url: " + url, ex);
        }
        return null;
    }

    private String addAnalogInfo(String url) {
        if (StringUtils.isNullOrEmpty(url)) {
            return url;
        }

        Map<String, Object> analogData = AdAnalogData.getAnalogInfo(context);
        String analogDataQuery = URLEncoder.encode(AdUtilities.jsonEncode(analogData));
        if (url.contains("?")) {
            return url + "&analog=" + analogDataQuery;
        } else {
            return url + "?analog=" + analogDataQuery;
        }
    }
}
