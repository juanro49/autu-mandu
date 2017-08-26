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
package me.kuehle.carreport.util;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class Assets {
    private static final String TAG = "Assets";

    public static Spanned getHtml(Context context, String path) {
        String result;
        try {
            InputStream in = context.getAssets().open(path);
            byte[] buffer = new byte[in.available()];
            in.read(buffer);
            in.close();

            result = new String(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Error reading help html file.", e);
            result = "Error reading help html file.";
        }

        return Html.fromHtml(result);
    }
}
