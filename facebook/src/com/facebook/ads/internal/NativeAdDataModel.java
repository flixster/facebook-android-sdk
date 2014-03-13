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
import android.net.Uri;
import android.util.Log;
import com.facebook.ads.NativeAd;
import com.facebook.ads.internal.action.AdAction;
import com.facebook.ads.internal.action.AdActionFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

/**
 * NativeAdDataModel contains creative info about an ad.
 */
public class NativeAdDataModel implements AdDataModel {

    private static final String TAG = NativeAdDataModel.class.getSimpleName();

    private final Uri adCommand;
    private final String title;
    private final String body;
    private final String callToAction;
    private final String socialContext;
    private final NativeAd.Image icon;
    private final NativeAd.Image image;
    private final NativeAd.Rating starRating;
    private final String impressionReportUrl;
    private final String clickReportUrl;
    private final AdInvalidationBehavior invalidationBehavior;
    private final Collection<String> detectionStrings;

    private NativeAdDataModel(Uri adCommand, String title, String body, String callToAction, String socialContext,
            NativeAd.Image icon, NativeAd.Image image, NativeAd.Rating starRating, String impressionReportUrl,
            String clickReportUrl, AdInvalidationBehavior invalidationBehavior, Collection<String> detectionStrings) {
        this.adCommand = adCommand;
        this.title = title;
        this.body = body;
        this.callToAction = callToAction;
        this.socialContext = socialContext;
        this.icon = icon;
        this.image = image;
        this.starRating = starRating;
        this.impressionReportUrl = impressionReportUrl;
        this.clickReportUrl = clickReportUrl;
        this.invalidationBehavior = invalidationBehavior;
        this.detectionStrings = detectionStrings;
    }

    @Override
    public AdInvalidationBehavior getInvalidationBehavior() {
        return invalidationBehavior;
    }

    @Override
    public Collection<String> getDetectionStrings() {
        return detectionStrings;
    }

    public NativeAd.Image getIcon() {
        return icon;
    }

    public NativeAd.Image getImage() {
        return image;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public String getSocialContext() {
        return socialContext;
    }

    public NativeAd.Rating getStarRating() {
        return starRating;
    }

    public void logImpression(Context context) {
        new OpenUrlTask(context).execute(impressionReportUrl);
    }

    /**
     * Handles click on the ad.
     * @param context activity context
     */
    public void handleClick(Context context) {
        new OpenUrlTask(context).execute(clickReportUrl);
        AdAction adAction = AdActionFactory.getAdAction(context, adCommand);
        if (adAction != null) {
            try {
                adAction.execute();
            } catch (Exception ex) {
                Log.e(TAG, "Error executing action", ex);
            }
        }
    }

    public boolean isValid() {
        return title != null && title.length() > 0 && callToAction != null && callToAction.length() > 0 &&
                icon != null && image != null;
    }

    public static NativeAdDataModel fromJSONObject(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        String fbadCommand = jsonObject.optString("fbad_command");
        Uri adCommand = Uri.parse(fbadCommand);
        String title = jsonObject.optString("title");
        String body = jsonObject.optString("body");
        String callToAction = jsonObject.optString("call_to_action");
        String socialContext = jsonObject.optString("social_context");
        NativeAd.Image icon = NativeAd.Image.fromJSONObject(jsonObject.optJSONObject("icon"));
        NativeAd.Image image = NativeAd.Image.fromJSONObject(jsonObject.optJSONObject("image"));
        NativeAd.Rating starRating = NativeAd.Rating.fromJSONObject(jsonObject.optJSONObject("star_rating"));
        String impressionReportUrl = jsonObject.optString("impression_report_url");
        String clickReportUrl = jsonObject.optString("click_report_url");

        AdInvalidationBehavior invalidationBehavior =
                AdInvalidationBehavior.fromString(jsonObject.optString("invalidation_behavior"));
        JSONArray detectionStringsArray = null;
        try {
            detectionStringsArray = new JSONArray(jsonObject.optString("detection_strings"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collection<String> detectionStrings =
                AdInvalidationUtils.parseDetectionStrings(detectionStringsArray);
        return new NativeAdDataModel(adCommand, title, body, callToAction, socialContext, icon, image, starRating,
                impressionReportUrl, clickReportUrl, invalidationBehavior, detectionStrings);
    }

}
