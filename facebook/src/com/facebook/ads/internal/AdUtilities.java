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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

class AdUtilities {

    private static final String TAG = AdUtilities.class.getSimpleName();

    private static final Uri ATTRIBUTION_ID_CONTENT_URI =
            Uri.parse("content://com.facebook.katana.provider.AttributionIdProvider");
    private static final String ATTRIBUTION_ID_COLUMN_NAME = "aid";

    public static String readStreamToString(InputStream inputStream) throws IOException {
        BufferedInputStream bufferedInputStream = null;
        InputStreamReader reader = null;
        try {
            bufferedInputStream = new BufferedInputStream(inputStream);
            reader = new InputStreamReader(bufferedInputStream);
            StringBuilder stringBuilder = new StringBuilder();

            final int bufferSize = 1024 * 2;
            char[] buffer = new char[bufferSize];
            int n;
            while ((n = reader.read(buffer)) != -1) {
                stringBuilder.append(buffer, 0, n);
            }

            return stringBuilder.toString();
        } finally {
            AdUtilities.closeQuietly(bufferedInputStream);
            AdUtilities.closeQuietly(reader);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException ioe) {
            // ignore quietly
        }
    }

    public static String jsonEncode(Map<String, Object> analogInfo) {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, Object> entry : analogInfo.entrySet()) {
            try {
                jsonObject.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonObject.toString();
    }

    public static Object getStringPropertyAsJSON(JSONObject jsonObject, String key) throws JSONException {
        Object value = jsonObject.opt(key);
        if (value != null && value instanceof String) {
            JSONTokener tokener = new JSONTokener((String) value);
            value = tokener.nextValue();
        }

        if (value != null && !(value instanceof JSONObject || value instanceof JSONArray)) {
            throw new IllegalArgumentException(key);
        }

        return value;
    }

    /**
     * Acquire the current attribution id from the facebook app.
     * @return returns null if the facebook app is not present on the phone.
     */
    public static String getAttributionId(ContentResolver contentResolver) {
        try {
            String [] projection = {ATTRIBUTION_ID_COLUMN_NAME};
            Cursor c = contentResolver.query(ATTRIBUTION_ID_CONTENT_URI, projection, null, null, null);
            if (c == null || !c.moveToFirst()) {
                return null;
            }
            String attributionId = c.getString(c.getColumnIndex(ATTRIBUTION_ID_COLUMN_NAME));
            c.close();
            return attributionId;
        } catch (Exception e) {
            return null;
        }
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public static Method getMethod(String className, String methodName, Class<?>... parameterTypes) {
        try {
            Class<?> clazz = Class.forName(className);
            return getMethod(clazz, methodName, parameterTypes);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    public static Object invokeMethod(Object receiver, Method method, Object... args) {
        try {
            return method.invoke(receiver, args);
        } catch (IllegalAccessException ex) {
            return null;
        } catch (InvocationTargetException ex) {
            return null;
        }
    }
}
