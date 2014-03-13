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
import android.content.pm.PackageManager;
import org.json.JSONArray;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AdInvalidationUtils {

    public static boolean shouldInvalidate(Context context, AdDataModel dataModel) {
        AdInvalidationBehavior invalidationBehavior = dataModel.getInvalidationBehavior();
        if (invalidationBehavior == null || invalidationBehavior == AdInvalidationBehavior.NONE) {
            return false;
        }

        boolean packageInstalled = false;
        Collection<String> detectionStrings = dataModel.getDetectionStrings();
        if (detectionStrings == null || detectionStrings.isEmpty()) {
            return false;
        }

        for (String packageName : detectionStrings) {
            if (isNativePackageInstalled(context, packageName)) {
                packageInstalled = true;
                break;
            }
        }
        if (invalidationBehavior == AdInvalidationBehavior.INSTALLED) {
            return packageInstalled;
        } else if (invalidationBehavior == AdInvalidationBehavior.NOT_INSTALLED) {
            return !packageInstalled;
        }
        return false;
    }

    public static boolean isNativePackageInstalled(Context context, String packageName) {
        if (StringUtils.isNullOrEmpty(packageName)) {
            return false;
        }

        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            // Expected when package is not installed.
            return false;
        }
    }

    public static Collection<String> parseDetectionStrings(JSONArray detectionStrings) {
        if (detectionStrings == null || detectionStrings.length() == 0) {
            return null;
        }

        Set<String> results = new HashSet<String>();
        for (int i = 0; i < detectionStrings.length(); i++) {
            results.add(detectionStrings.optString(i));
        }
        return results;
    }
}
