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

package com.facebook.ads.internal.action;

import android.content.Context;
import android.net.Uri;

public class AdActionFactory {

    private static final String AD_ACTION_APP_AD = "store";
    private static final String AD_ACTION_LINK_AD = "open_link";

    public static AdAction getAdAction(Context context, Uri uri) {
        String action = uri.getAuthority();
        if (AD_ACTION_APP_AD.equals(action)) {
            return new AppAdAction(context, uri);
        }
        if (AD_ACTION_LINK_AD.equals(action)) {
            return new LinkAdAction(context, uri);
        }
        return null;
    }
}
