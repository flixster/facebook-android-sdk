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
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class LinkAdAction implements AdAction {

    private static final String TAG = LinkAdAction.class.getSimpleName();

    private final Context context;
    private final Uri uri;

    public LinkAdAction(Context context, Uri uri) {
        this.context = context;
        this.uri = uri;
    }

    @Override
    public void execute() {
        String link = uri.getQueryParameter("link");
        Uri linkUri = Uri.parse(link);
        Intent intent = new Intent(Intent.ACTION_VIEW, linkUri);
        try {
            context.startActivity(intent);
        } catch (Exception ex) {
            Log.d(TAG, "Failed to open market url: " + uri.toString(), ex);
        }
    }

}
