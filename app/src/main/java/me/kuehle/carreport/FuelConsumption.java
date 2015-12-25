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

package me.kuehle.carreport;

import android.content.Context;

public class FuelConsumption {
    private Type consumptionType;
    private String unitVolume;
    private String unitDistance;
    private Preferences preferences;

    public FuelConsumption(Context context) {
        this.preferences = new Preferences(context);
        reload();
    }

    public static Type findConsumptionType(int id) {
        if (id == Type.DIST_FOR_VOL.id)
            return Type.DIST_FOR_VOL;
        return Type.VOL_FOR_DIST;
    }

    public void reload() {
        int id = preferences.getUnitFuelConsumption();
        this.setConsumptionType(id);
        this.unitVolume = preferences.getUnitVolume();
        this.unitDistance = preferences.getUnitDistance();
    }

    public void setConsumptionType(int id) {
        this.consumptionType = FuelConsumption.findConsumptionType(id);
    }

    public void setUnitVolume(String unitVolume) {
        this.unitVolume = unitVolume;
    }

    public void setUnitDistance(String unitDistance) {
        this.unitDistance = unitDistance;
    }

    public float computeFuelConsumption(Type consumptionType, float volume, float distance) {
        if (consumptionType == Type.DIST_FOR_VOL) {
            return distance / volume;
        } else {
            return (float) (100.0 * volume / distance);
        }
    }

    public float computeFuelConsumption(float volume, float distance) {
        return computeFuelConsumption(this.consumptionType, volume, distance);
    }

    public String getUnitLabel(Type consumptionType) {
        if (consumptionType == Type.DIST_FOR_VOL) {
            return String.format("%s/%s", this.unitDistance, this.unitVolume);
        } else {
            return String
                    .format("%s/100%s", this.unitVolume, this.unitDistance);
        }
    }

    public String getUnitLabel() {
        return this.getUnitLabel(this.consumptionType);
    }

    public String[] getUnitsEntries() {
        String[] list = new String[2];
        list[0] = this.getUnitLabel(Type.VOL_FOR_DIST);
        list[1] = this.getUnitLabel(Type.DIST_FOR_VOL);
        return list;
    }

    public String[] getUnitsEntryValues() {
        String[] list = new String[2];
        list[0] = String.valueOf(Type.VOL_FOR_DIST.id);
        list[1] = String.valueOf(Type.DIST_FOR_VOL.id);
        return list;
    }

    public enum Type {
        VOL_FOR_DIST(0), DIST_FOR_VOL(1);

        public final int id;

        Type(int id) {
            this.id = id;
        }
    }
}
