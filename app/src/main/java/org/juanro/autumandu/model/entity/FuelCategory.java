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

package org.juanro.autumandu.model.entity;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Predefined fuel categories with translations.
 */
public enum FuelCategory {
    GASOLINE("gasoline", R.string.fuel_category_gasoline),
    DIESEL("diesel", R.string.fuel_category_diesel),
    GAS("gas", R.string.fuel_category_gas),
    ELECTRICITY("electricity", R.string.fuel_category_electricity),
    ADDITIVES("additives", R.string.fuel_category_additives),
    GENERAL("general", R.string.default_fuel_category);

    private final String key;
    private final int nameResId;

    FuelCategory(String key, @StringRes int nameResId) {
        this.key = key;
        this.nameResId = nameResId;
    }

    public String getKey() {
        return key;
    }

    public int getNameResId() {
        return nameResId;
    }

    public String getName(Context context) {
        return context.getString(nameResId);
    }

    public String getVolumeUnit(Context context) {
        return new Preferences(context).getUnitVolume(this);
    }

    @NonNull
    public static FuelCategory fromKey(String key) {
        for (FuelCategory category : values()) {
            if (category.key.equals(key)) {
                return category;
            }
        }
        return GENERAL;
    }

    public static List<String> getAllKeys() {
        List<String> keys = new ArrayList<>();
        for (FuelCategory category : values()) {
            keys.add(category.key);
        }
        return keys;
    }
}
