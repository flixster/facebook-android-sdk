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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AdSize;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class AdRequest {

    public interface Callback {
        void onCompleted(AdResponse adResponse);
        void onError(AdError error);
    }

    private static final String TAG = AdRequest.class.getSimpleName();

    private static final String GRAPH_URL_BASE = "https://graph.facebook.com";
    private static final String GRAPH_URL_BASE_PREFIX_FORMAT = "http://graph.%s.facebook.com";
    private static final String ADS_ENDPOINT = "network_ads";
    private static final String NATIVE_ADS_ENDPOINT = "network_ads_native";

    private static final String DEFAULT_ENCODING = "utf-8";
    private static final String OS = "Android";

    private static final String AD_TYPE_PARAM = "ad_type";
    private static final String SDK_CAPABILITIES_PARAM = "sdk_capabilities";

    private static final String PLACEMENT_ID_PARAM = "placement_id";
    private static final String ATTRIBUTION_ID_PARAM = "attribution_id";
    private static final String WIDTH_PARAM = "width";
    private static final String HEIGHT_PARAM = "height";
    private static final String TEST_MODE_PARAM = "test_mode";

    private static final String DEVICE_ID_PARAM = "device_id";
    private static final String DEVICE_ID_TRACKING_ENABLED_PARAM = "tracking_enabled";
    private static final String OS_PARAM = "os";
    private static final String OS_VERSION_PARAM = "os_version";
    private static final String SCREEN_WIDTH_PARAM = "screen_width";
    private static final String SCREEN_HEIGHT_PARAM = "screen_height";
    private static final String APP_BUILD_PARAM = "app_build";
    private static final String APP_VERSION_PARAM = "app_version";

    private static final String CHILD_DIRECTED_PARAM = "child_directed";

    private static final String PACKAGE_NAME_PARAM = "package_name";

    private final Context context;
    private final String placementId;
    private final AdSize adSize;
    private final String userAgentString;

    private final Callback callback;

    private final AdType adType;
    private final boolean testMode;

    public AdRequest(Context context, String placementId, AdSize adSize,
            AdType adType, boolean testMode, Callback callback) {
        this.context = context;
        this.placementId = placementId;
        this.adSize = adSize;
        this.userAgentString = AdWebViewUtils.getUserAgentString(context);
        this.adType = adType;
        this.testMode = testMode;
        this.callback = callback;

        validate();
    }

    private String getAdsEndpoint() {
        switch (adType) {
            case NATIVE:
                return NATIVE_ADS_ENDPOINT;
            case HTML:
            default:
                return ADS_ENDPOINT;
        }
    }

    public AsyncTask executeAsync() {
        AdAnalogData.registerSensorListener(context);
        AsyncTask<Void, Void, AdRequestResponse> asyncTask = new AsyncTask<Void, Void, AdRequestResponse>() {
            @Override
            protected AdRequestResponse doInBackground(Void... params) {
                return executeConnectionAndWait();
            }

            @Override
            protected void onPostExecute(AdRequestResponse response) {
                if (response == null) {
                    callback.onError(AdError.INTERNAL_ERROR);
                } else if (response.error != null) {
                    callback.onError(response.error);
                } else {
                    AdResponse adResponse = AdResponse.parseResponse(context, response.body);
                    callback.onCompleted(adResponse);
                }
            }
        };
        return asyncTask.execute();
    }

    private void validate() {
        if (placementId == null || placementId.length() < 1) {
            throw new IllegalArgumentException("placementId");
        }
        if (adSize == null) {
            throw new IllegalArgumentException("adSize");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }
    }

    private Map<String, Object> getRequestParameters() {
        Map<String, Object> params = new HashMap<String, Object>();

        params.put(AD_TYPE_PARAM, adType.getValue());
        params.put(SDK_CAPABILITIES_PARAM, AdSdkCapability.getSupportedCapabilities());
        params.put(PLACEMENT_ID_PARAM, placementId);
        params.put(ATTRIBUTION_ID_PARAM, AdUtilities.getAttributionId(context.getContentResolver()));
        params.put(WIDTH_PARAM, adSize.getWidth());
        params.put(HEIGHT_PARAM, adSize.getHeight());
        params.put(TEST_MODE_PARAM, testMode);
        params.put(CHILD_DIRECTED_PARAM, AdSettings.isChildDirected());

        addDeviceInfoParams(params);
        addAppInfoParams(params);

        addAdvertisingInfoParams(params);

        return params;
    }

    private void addDeviceInfoParams(Map<String, Object> params) {
        params.put(OS_PARAM, OS);
        params.put(OS_VERSION_PARAM, Build.VERSION.RELEASE);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        float density = metrics.density;
        int screenWidth = (int) (metrics.widthPixels / density);
        int screenHeight = (int) (metrics.heightPixels / density);
        params.put(SCREEN_WIDTH_PARAM, screenWidth);
        params.put(SCREEN_HEIGHT_PARAM, screenHeight);

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            params.put(APP_BUILD_PARAM, pInfo.versionCode);
            params.put(APP_VERSION_PARAM, pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            params.put(APP_VERSION_PARAM, 0);
        }
    }

    private void addAdvertisingInfoParams(Map<String, Object> params) {
        AdvertisingIdInfo advertisingIdInfo = AdvertisingIdInfo.getAdvertisingIdInfo(context);
        if (advertisingIdInfo == null) {
            params.put(DEVICE_ID_TRACKING_ENABLED_PARAM, true);
            return;
        }

        params.put(DEVICE_ID_TRACKING_ENABLED_PARAM, !advertisingIdInfo.isLimitAdTrackingEnabled());
        if (!advertisingIdInfo.isLimitAdTrackingEnabled()) {
            params.put(DEVICE_ID_PARAM, advertisingIdInfo.getId());
        }
    }

    private void addAppInfoParams(Map<String, Object> params) {
        params.put(PACKAGE_NAME_PARAM, context.getPackageName());
    }

    private URL getUrlForRequest() throws MalformedURLException {
        String urlBase;
        String urlPrefix = AdSettings.getUrlPrefix();
        if (StringUtils.isNullOrEmpty(urlPrefix)) {
            urlBase = GRAPH_URL_BASE;
        } else {
            urlBase = String.format(GRAPH_URL_BASE_PREFIX_FORMAT, urlPrefix);
        }
        return new URL(String.format("%s/%s", urlBase, getAdsEndpoint()));
    }

    private String getQueryString(Map<String, Object> params) throws UnsupportedEncodingException
    {
        StringBuilder sb = new StringBuilder(512);
        boolean first = true;

        for (Map.Entry<String, Object> entry : params.entrySet())
        {
            if (first) {
                first = false;
            } else {
                sb.append("&");
            }

            sb.append(URLEncoder.encode(entry.getKey(), DEFAULT_ENCODING))
              .append("=")
              .append(URLEncoder.encode(String.valueOf(entry.getValue()), DEFAULT_ENCODING));
        }

        return sb.toString();
    }

    private HttpURLConnection makeRequest() throws IOException {
        URL url = getUrlForRequest();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", userAgentString);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(0);
        connection.setReadTimeout(0);

        Map<String, Object> params = getRequestParameters();
        String queryString = getQueryString(params);

        OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, DEFAULT_ENCODING));
        writer.write(queryString);
        writer.flush();
        writer.close();
        outputStream.close();

        connection.connect();
        return connection;
    }

    public AdRequestResponse executeConnectionAndWait() {
        InputStream stream = null;
        HttpURLConnection connection = null;
        try {
            connection = makeRequest();
            if (connection.getResponseCode() >= 400) {
                stream = connection.getErrorStream();
            } else {
                stream = connection.getInputStream();
            }

            return createResponsesFromStream(stream);
        } catch (IOException ex) {
            AdRequestResponse response = new AdRequestResponse();
            response.error = new AdError(AdError.INVALID_ERROR_CODE, ex.getMessage());
            return response;
        } catch (Exception ex) {
            Log.e(TAG, "Unexpected error", ex);
            AdRequestResponse response = new AdRequestResponse();
            response.error = AdError.INTERNAL_ERROR;
            return response;
        } finally {
            AdUtilities.closeQuietly(stream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private AdRequestResponse createResponsesFromStream(InputStream stream) {
        AdRequestResponse response = new AdRequestResponse();

        try {
            String responseString = AdUtilities.readStreamToString(stream);
            JSONTokener tokener = new JSONTokener(responseString);
            Object resultObject = tokener.nextValue();
            if (resultObject instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) resultObject;
                if (jsonObject.has("error")) {
                    JSONObject error = (JSONObject) AdUtilities.getStringPropertyAsJSON(jsonObject, "error");
                    int errorCode = error.optInt("code", AdError.INVALID_ERROR_CODE);
                    String errorMessage = error.optString("message", null);
                    response.error = new AdError(errorCode, errorMessage);
                } else {
                    response.body = jsonObject;
                    response.error = null;
                }
            }
        } catch (Exception ex) {
            response.error = new AdError(AdError.INVALID_ERROR_CODE, ex.getMessage());
        }
        return response;
    }

    private static class AdRequestResponse {
        JSONObject body = null;
        AdError error = null;
    }
}
