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
 * AdError contains the error information returned by Facebook.
 */
public class AdError {

    public static final AdError CLIENT_INVALIDATION = new AdError(2000, "Client Invalidation");
    public static final AdError INTERNAL_ERROR = new AdError(2001, "Internal Error");
    public static final AdError MISSING_PROPERTIES = new AdError(2002, "Native ad failed to load due to missing properties");

    public static final int INVALID_ERROR_CODE = -1;

    private final int errorCode;
    private final String errorMessage;

    /**
     * Constructs an AdError using the given error code and error message
     * @param errorCode the error code
     * @param errorMessage the error message
     */
    public AdError(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the error code
     * @return the error code
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the error message
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
