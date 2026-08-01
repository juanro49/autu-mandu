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

package org.juanro.autumandu;

import android.content.Context;
import androidx.annotation.NonNull;
import java.util.Locale;

/**
 * Utility class for cost per distance calculations.
 */
public class CostPerDistance {
    private Type costPerDistanceType;
    private final String unitCurrency;
    private final String unitDistance;
    private final Preferences preferences;

    public CostPerDistance(Context context) {
        this.preferences = new Preferences(context);
        this.unitCurrency = preferences.getUnitCurrency();
        this.unitDistance = preferences.getUnitDistance();
        reload();
    }

    public void reload() {
        int id = preferences.getUnitCostPerDistance();
        this.costPerDistanceType = Type.fromId(id);
    }

    public void setCostPerDistanceType(int id) {
        this.costPerDistanceType = Type.fromId(id);
    }

    public double computeCostPerDistance(double cost, double distance) {
        if (distance <= 0) return 0;
        return switch (costPerDistanceType) {
            case PRICE_PER_DISTANCE -> cost / distance;
            case PRICE_PER_100_DISTANCE -> 100.0 * cost / distance;
        };
    }

    public String getUnitLabel(Type type) {
        return switch (type) {
            case PRICE_PER_DISTANCE -> String.format(Locale.getDefault(), "%s/%s", unitCurrency, unitDistance);
            case PRICE_PER_100_DISTANCE -> String.format(Locale.getDefault(), "%s/100 %s", unitCurrency, unitDistance);
        };
    }

    public String getUnitLabel() {
        return getUnitLabel(this.costPerDistanceType);
    }

    public String getDistanceUnitLabel() {
        return switch (costPerDistanceType) {
            case PRICE_PER_DISTANCE -> unitDistance;
            case PRICE_PER_100_DISTANCE -> "100 " + unitDistance;
        };
    }

    public String[] getUnitsEntries() {
        return new String[]{
                getUnitLabel(Type.PRICE_PER_100_DISTANCE),
                getUnitLabel(Type.PRICE_PER_DISTANCE)
        };
    }

    public String[] getUnitsEntryValues() {
        return new String[]{
                String.valueOf(Type.PRICE_PER_100_DISTANCE.id),
                String.valueOf(Type.PRICE_PER_DISTANCE.id)
        };
    }

    public enum Type {
        PRICE_PER_100_DISTANCE(0), PRICE_PER_DISTANCE(1);

        public final int id;

        Type(int id) {
            this.id = id;
        }

        @NonNull
        public static Type fromId(int id) {
            return id == 1 ? PRICE_PER_DISTANCE : PRICE_PER_100_DISTANCE;
        }
    }
}
