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

package org.juanro.autumandu.viewmodel;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.data.query.ReminderQueries;
import org.juanro.autumandu.data.report.AbstractReport;
import org.juanro.autumandu.model.dao.CarDao;
import org.juanro.autumandu.model.dao.OtherCostDao;
import org.juanro.autumandu.model.dao.RefuelingDao;
import org.juanro.autumandu.model.dao.ReminderDao;
import org.juanro.autumandu.model.dao.TireDao;
import org.juanro.autumandu.model.dao.TripDao;
import org.juanro.autumandu.model.dto.ReminderWithCar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ReportViewModel extends AndroidViewModel implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final MediatorLiveData<List<AbstractReport>> mReports = new MediatorLiveData<>();
    private final MediatorLiveData<Boolean> mHasDueReminders = new MediatorLiveData<>();
    private final List<AbstractReport> mCachedReports = new ArrayList<>();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Preferences mPrefs;

    @Inject
    public ReportViewModel(
            Application application,
            CarDao carDao,
            RefuelingDao refuelingDao,
            OtherCostDao otherCostDao,
            TireDao tireDao,
            TripDao tripDao,
            ReminderDao reminderDao
    ) {
        super(application);
        mPrefs = new Preferences(application);
        getDefaultSharedPreferences(application)
                .registerOnSharedPreferenceChangeListener(this);

        // Observamos todas las fuentes de datos que afectan a los informes
        mReports.addSource(carDao.getAllLiveData(), cars -> invalidateAndRefresh());
        mReports.addSource(refuelingDao.getWithDetailsForCarLiveData(-1), refueling -> invalidateAndRefresh());
        mReports.addSource(otherCostDao.getAllLiveData(), otherCosts -> invalidateAndRefresh());
        mReports.addSource(tireDao.getAllTireListsLiveData(), tires -> invalidateAndRefresh());
        mReports.addSource(tripDao.getTripsWithDetailsForCarLive(-1), trips -> invalidateAndRefresh());

        mHasDueReminders.addSource(reminderDao.getAllWithCarLiveData(), list -> {
            if (list == null) {
                mHasDueReminders.setValue(false);
                return;
            }
            mExecutor.execute(() -> {
                boolean due = false;
                for (ReminderWithCar item : list) {
                    ReminderQueries queries = new ReminderQueries(getApplication(), item);
                    if (queries.isDue() && !item.reminder().isNotificationDismissed() && !queries.isSnoozed()) {
                        due = true;
                        break;
                    }
                }
                mHasDueReminders.postValue(due);
            });
        });

        refreshReports();
    }

    public void invalidateAndRefresh() {
        for (AbstractReport report : mCachedReports) {
            report.invalidate();
        }
        refreshReports();
    }

    public LiveData<List<AbstractReport>> getReports() {
        return mReports;
    }

    public LiveData<Boolean> getHasDueReminders() {
        return mHasDueReminders;
    }

    public void refreshReports() {
        mExecutor.execute(() -> {
            List<Class<? extends AbstractReport>> reportClasses = mPrefs.getReportOrder();

            // Check if the order or count has changed
            boolean listChanged = mCachedReports.size() != reportClasses.size();
            if (!listChanged) {
                for (int i = 0; i < reportClasses.size(); i++) {
                    if (!mCachedReports.get(i).getClass().equals(reportClasses.get(i))) {
                        listChanged = true;
                        break;
                    }
                }
            }

            if (listChanged) {
                mCachedReports.clear();
                for (Class<? extends AbstractReport> reportClass : reportClasses) {
                    AbstractReport report = AbstractReport.newInstance(reportClass, getApplication());
                    if (report != null) {
                        mCachedReports.add(report);
                    }
                }
            }

            // Publicamos la lista de instancias persistentes
            mReports.postValue(new ArrayList<>(mCachedReports));
        });
    }

    @Override
    public void onSharedPreferenceChanged(android.content.SharedPreferences sharedPreferences, String key) {
        // Al cambiar cualquier preferencia, invalidamos los informes para forzar su recarga
        // Esto cubre cambios en moneda, unidades, orden, etc.
        invalidateAndRefresh();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        getDefaultSharedPreferences(getApplication())
                .unregisterOnSharedPreferenceChangeListener(this);
        mExecutor.shutdown();
    }
}
