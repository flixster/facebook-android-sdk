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
 * Ad interface is a common interface implemented by all Facebook ad controls.
 */
public interface Ad {

    /**
     * Loads an ad.
     * <p>
     * This method always returns immediately. The ad is loaded asynchronously. The control's
     * ad listener will be called when loading finishes or fails.
     */
    void loadAd();

    /**
     * Loads an ad with the given ad targeting option.
     * <p>
     * This method always returns immediately. The ad is loaded asynchronously. The control's
     * ad listener will be called when loading finishes or fails.
     *
     * @param adTargetingOptions the ad targeting option
     */
    void loadAd(AdTargetingOptions adTargetingOptions);

    /**
     * Destroys the ad control.
     * <p>
     * This method should be called when the hosting activity of the ad control is destroyed.
     */
    void destroy();
}
