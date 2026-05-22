/*
 * Copyright 2026 Juanro49
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.juanro.autumandu.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.juanro.autumandu.R;

public class Carburoid {
    public static final String ACTION_SELECT_PRODUCT = "net.canvoki.carburoid.ACTION_SELECT_PRODUCT";
    public static final String EXTRA_PRODUCT = "net.canvoki.carburoid.EXTRA_PRODUCT";

    private Carburoid() {
        // Utility class
    }

    public static void launch(@NonNull Context context, @Nullable String product) {
        Log.i("Carburoid", "Launching Carburoid with product: " + (product != null ? product : "none"));
        Intent intent = new Intent(ACTION_SELECT_PRODUCT);
        if (!TextUtils.isEmpty(product)) {
            intent.putExtra(EXTRA_PRODUCT, product);
        }
        intent.setPackage(context.getString(R.string.carburoid_package));

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.toast_carburoid_not_installed, Toast.LENGTH_LONG).show();
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.carburoid_url))));
        }
    }
}
