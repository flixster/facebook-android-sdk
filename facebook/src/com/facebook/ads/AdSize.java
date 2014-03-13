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
 * AdSize is the dimension of the ad control in DP.
 */
public enum AdSize {

    BANNER_320_50(320, 50),
    INTERSTITIAL(0, 0);

    private final int width;
    private final int height;

    private AdSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Gets the width of the ad control.
     * @return width of the ad control in DP
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the height of the ad control.
     * @return height of the ad control in DP
     */
    public int getHeight() {
        return height;
    }
}
