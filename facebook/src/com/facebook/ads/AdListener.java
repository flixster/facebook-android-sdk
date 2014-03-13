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

/**
 * AdListener interface is notified of events happened in ad control.
 */
public interface AdListener {

    /**
     * Called when an error happened while the ad control is attempting to load an ad.
     * @param ad the ad control
     * @param error the error
     */
    void onError(Ad ad, AdError error);

    /**
     * Called when the ad control has loaded an ad.
     * @param ad the ad control
     */
    void onAdLoaded(Ad ad);

    /**
     * Called when the ad control is clicked and user is redirected to the link in the ad.
     * @param ad the ad control
     */
    void onAdClicked(Ad ad);
}
