/*
 * Copyright 2017 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.juanro.autumandu.util;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for reading assets.
 */
public final class Assets {
    private static final String TAG = "Assets";

    private Assets() {
        // Utility class
    }

    /**
     * Reads an HTML file from assets and returns it as Spanned text.
     *
     * @param context The application context.
     * @param path    The path to the asset file.
     * @return Spanned text containing the HTML content.
     */
    public static Spanned getHtml(Context context, String path) {
        String result;
        try (InputStream in = context.getAssets().open(path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            result = out.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            Log.e(TAG, "Error reading help html file: " + path, e);
            result = "<i>Error reading help html file.</i>";
        }

        return Html.fromHtml(result, Html.FROM_HTML_MODE_LEGACY);
    }
}
