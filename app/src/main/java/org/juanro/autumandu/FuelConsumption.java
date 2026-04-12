/*
 * Copyright 2013 Mihai Ibanescu
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
 * Utility class for fuel consumption calculations.
 */
public class FuelConsumption {
    private Type consumptionType;
    private String unitVolume;
    private String unitDistance;
    private final Preferences preferences;

    public FuelConsumption(Context context) {
        this.preferences = new Preferences(context);
        reload();
    }

    public void reload() {
        int id = preferences.getUnitFuelConsumption();
        this.consumptionType = Type.fromId(id);
        this.unitVolume = preferences.getUnitVolume();
        this.unitDistance = preferences.getUnitDistance();
    }

    public void setConsumptionType(int id) {
        this.consumptionType = Type.fromId(id);
    }

    public void setUnitVolume(String unitVolume) {
        this.unitVolume = unitVolume;
    }

    public void setUnitDistance(String unitDistance) {
        this.unitDistance = unitDistance;
    }

    public float computeFuelConsumption(Type consumptionType, float volume, float distance) {
        return switch (consumptionType) {
            case DIST_FOR_VOL -> distance / volume;
            case VOL_FOR_DIST -> (float) (100.0 * volume / distance);
        };
    }

    public float computeFuelConsumption(float volume, float distance) {
        return computeFuelConsumption(this.consumptionType, volume, distance);
    }

    public String getUnitLabel(Type consumptionType) {
        return switch (consumptionType) {
            case DIST_FOR_VOL -> String.format(Locale.US, "%s/%s", unitDistance, unitVolume);
            case VOL_FOR_DIST -> String.format(Locale.US, "%s/100%s", unitVolume, unitDistance);
        };
    }

    public String getUnitLabel() {
        return getUnitLabel(this.consumptionType);
    }

    public String[] getUnitsEntries() {
        return new String[]{
                getUnitLabel(Type.VOL_FOR_DIST),
                getUnitLabel(Type.DIST_FOR_VOL)
        };
    }

    public String[] getUnitsEntryValues() {
        return new String[]{
                String.valueOf(Type.VOL_FOR_DIST.id),
                String.valueOf(Type.DIST_FOR_VOL.id)
        };
    }

    public enum Type {
        VOL_FOR_DIST(0), DIST_FOR_VOL(1);

        public final int id;

        Type(int id) {
            this.id = id;
        }

        @NonNull
        public static Type fromId(int id) {
            return id == 1 ? DIST_FOR_VOL : VOL_FOR_DIST;
        }
    }
}
