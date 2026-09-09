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

package org.juanro.autumandu.di;

import android.content.Context;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dao.CarDao;
import org.juanro.autumandu.model.dao.FuelTypeDao;
import org.juanro.autumandu.model.dao.OtherCostDao;
import org.juanro.autumandu.model.dao.RefuelingDao;
import org.juanro.autumandu.model.dao.ReminderDao;
import org.juanro.autumandu.model.dao.StationDao;
import org.juanro.autumandu.model.dao.TankDao;
import org.juanro.autumandu.model.dao.TireDao;
import org.juanro.autumandu.model.dao.TripDao;
import org.juanro.autumandu.model.dao.TripPrefabDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public AutuManduDatabase provideDatabase(@ApplicationContext Context context) {
        return AutuManduDatabase.getInstance(context);
    }

    @Provides
    public CarDao provideCarDao(AutuManduDatabase db) {
        return db.getCarDao();
    }

    @Provides
    public FuelTypeDao provideFuelTypeDao(AutuManduDatabase db) {
        return db.getFuelTypeDao();
    }

    @Provides
    public OtherCostDao provideOtherCostDao(AutuManduDatabase db) {
        return db.getOtherCostDao();
    }

    @Provides
    public RefuelingDao provideRefuelingDao(AutuManduDatabase db) {
        return db.getRefuelingDao();
    }

    @Provides
    public ReminderDao provideReminderDao(AutuManduDatabase db) {
        return db.getReminderDao();
    }

    @Provides
    public StationDao provideStationDao(AutuManduDatabase db) {
        return db.getStationDao();
    }

    @Provides
    public TankDao provideTankDao(AutuManduDatabase db) {
        return db.getTankDao();
    }

    @Provides
    public TireDao provideTireDao(AutuManduDatabase db) {
        return db.getTireDao();
    }

    @Provides
    public TripDao provideTripDao(AutuManduDatabase db) {
        return db.getTripDao();
    }

    @Provides
    public TripPrefabDao provideTripPrefabDao(AutuManduDatabase db) {
        return db.getTripPrefabDao();
    }
}
