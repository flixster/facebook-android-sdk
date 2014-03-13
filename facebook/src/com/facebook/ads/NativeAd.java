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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import com.facebook.ads.internal.*;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;

/**
 * NativeAd enables displaying ad creative in custom layout.
 * <p>
 * To use NativeAd, do the following:
 * <code>
 *     NativeAd nativeAd = new NativeAd(context, PLACEMENT_ID);
 *     nativeAd.setAdListener(new NativeAdListener() {
 *         public void onAdLoaded(Ad ad) {
 *             if (ad == this) {
 *               // Renders the ad
 *               String adTitle = getAdTitle();
 *         }
 *         // rest of AdListener implementation omitted
 *     });
 *     nativeAd.loadAd();
 * </code>
 */
public class NativeAd implements Ad {

    /**
     * Image creative.
     */
    public static class Image {

        private final String url;
        private final int width;
        private final int height;

        private Image(String url, int width, int height) {
            this.url = url;
            this.width = width;
            this.height = height;
        }

        /**
         * Gets the url of the image.
         * @return absolute url of the image
         */
        public String getUrl() {
            return url;
        }

        /**
         * Gets the width of the image.
         * @return width of the image in dp
         */
        public int getWidth() {
            return width;
        }

        /**
         * Gets the height of the image.
         * @return height of the image in dp
         */
        public int getHeight() {
            return height;
        }

        public static Image fromJSONObject(JSONObject dataObject) {
            if (dataObject == null) {
                return null;
            }

            String url = dataObject.optString("url");
            if (url == null) {
                return null;
            }
            int width = dataObject.optInt("width", 0);
            int height = dataObject.optInt("height", 0);
            return new Image(url, width, height);
        }
    }

    /**
     * Rating info.
     */
    public static class Rating {

        private final double value;
        private final double scale;

        private Rating(double value, double scale) {
            this.value = value;
            this.scale = scale;
        }

        /**
         * Gets the rating value.
         * @return the rating value
         */
        public double getValue() {
            return value;
        }

        /**
         * Gets the rating scale.
         * @return the rating scale
         */
        public double getScale() {
            return scale;
        }

        public static Rating fromJSONObject(JSONObject dataObject) {
            if (dataObject == null) {
                return null;
            }

            double value = dataObject.optDouble("width", 0.0);
            double scale = dataObject.optDouble("height", 0.0);
            return new Rating(value, scale);
        }
    }

    private static final String TAG = NativeAd.class.getSimpleName();

    private final Context context;

    private AdRequestController adRequestController;
    private AdListener adListener;

    private volatile boolean loadRequested;
    private boolean adLoaded;
    private NativeAdDataModel adDataModel;

    public NativeAd(Context context, String placementId) {
        this.context = context;

        adRequestController = new AdRequestController(this.context, placementId, AdSize.INTERSTITIAL,
                false, AdType.NATIVE, new AdRequest.Callback() {
            @Override
            public void onCompleted(AdResponse adResponse) {
                if (!(adResponse.getDataModel() instanceof NativeAdDataModel)) {
                    if (adListener != null) {
                        adListener.onError(NativeAd.this, AdError.INTERNAL_ERROR);
                    }
                }
                adDataModel = (NativeAdDataModel) adResponse.getDataModel();
                if (adDataModel != null && adDataModel.isValid()) {
                    adLoaded = true;
                    if (adListener != null) {
                        adListener.onAdLoaded(NativeAd.this);
                    }
                } else if (adDataModel == null) {
                    adLoaded = false;
                    if (adListener != null) {
                        adListener.onError(
                                NativeAd.this,
                                adResponse.getError() != null ? adResponse.getError() : AdError.INTERNAL_ERROR
                        );
                    }
                } else {
                    adLoaded = false;
                    adDataModel = null;
                    if (adListener != null) {
                        adListener.onError(NativeAd.this, AdError.MISSING_PROPERTIES);
                    }
                }
            }

            @Override
            public void onError(AdError error) {
                adLoaded = false;
                if (adListener != null) {
                    adListener.onError(NativeAd.this, error);
                }
            }
        });
    }

    private void ensureAdRequestController() {
        if (adRequestController == null) {
            throw new RuntimeException("No request controller available, has the NativeAd been destroyed?");
        }
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
        if (loadRequested) {
            throw new IllegalStateException("Ad already loaded");
        }
        loadRequested = true;
        ensureAdRequestController();
        adRequestController.loadAd(adTargetingOptions);
    }

    @Override
    public void destroy() {
        if (adRequestController != null) {
            adRequestController.destroy();
            adRequestController = null;
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
     * Logs the ad impression.
     */
    public void logImpression() {
        adDataModel.logImpression(context);
    }

    /**
     * Handles click on the ad.
     */
    public void handleClick() {
        if (adListener != null) {
            adListener.onAdClicked(this);
        }
        adDataModel.handleClick(context);
    }

    /**
     * Gets the icon creative.
     * @return the ad icon
     */
    public Image getAdIcon() {
        if (!adLoaded) {
            return null;
        }
        return adDataModel.getIcon();
    }

    /**
     * Gets the cover image creative.
     * @return the ad cover image
     */
    public Image getAdCoverImage() {
        if (!adLoaded) {
            return null;
        }
        return adDataModel.getImage();
    }

    /**
     * Gets the title.
     * @return the ad title
     */
    public String getAdTitle() {
        if (!adLoaded) {
            return null;
        }
        return adDataModel.getTitle();
    }

    /**
     * Gets the body, usually a longer description of the ad.
     * @return the ad body
     */
    public String getAdBody() {
        if (!adLoaded) {
            return null;
        }
        return adDataModel.getBody();
    }

    /**
     * Gets the call to action phrase.
     * @return the call to action phrase
     */
    public String getAdCallToAction() {
        if (!adLoaded) {
            return null;
        }
        return adDataModel.getCallToAction();
    }

    /**
     * Gets the social context.
     * @return the social content sentence
     */
    public String getAdSocialContext() {
        if (!adLoaded) {
            return null;
        }
        return adDataModel.getSocialContext();
    }

    /**
     * Gets the star rating.
     * @return the star rating
     */
    public Rating getAdStarRating() {
        if (!adLoaded) {
            return null;
        }
        return adDataModel.getStarRating();
    }

    /**
     * Helper AsyncTask to download image.
     */
    public static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        private final ImageView imageView;

        public DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap bitmap = null;
            try {
                InputStream input = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception ex) {
                Log.e(TAG, "Error downloading image: " + url, ex);
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }
}
