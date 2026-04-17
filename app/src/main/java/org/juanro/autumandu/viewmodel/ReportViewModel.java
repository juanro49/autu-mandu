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

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.data.report.AbstractReport;
import org.juanro.autumandu.model.AutuManduDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportViewModel extends AndroidViewModel {
    private final MediatorLiveData<List<AbstractReport>> mReports = new MediatorLiveData<>();
    private final List<AbstractReport> mCachedReports = new ArrayList<>();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Preferences mPrefs;

    public ReportViewModel(@NonNull Application application) {
        super(application);
        mPrefs = new Preferences(application);

        AutuManduDatabase db = AutuManduDatabase.getInstance(application);
        // Observamos todas las fuentes de datos que afectan a los informes
        mReports.addSource(db.getCarDao().getAllLiveData(), cars -> invalidateAndRefresh());
        mReports.addSource(db.getRefuelingDao().getWithDetailsForCarLiveData(-1), refueling -> invalidateAndRefresh());
        mReports.addSource(db.getOtherCostDao().getAllLiveData(), otherCosts -> invalidateAndRefresh());
        mReports.addSource(db.getTireDao().getAllTireListsLiveData(), tires -> invalidateAndRefresh());

        refreshReports();
    }

    private void invalidateAndRefresh() {
        for (AbstractReport report : mCachedReports) {
            report.invalidate();
        }
        refreshReports();
    }

    public LiveData<List<AbstractReport>> getReports() {
        return mReports;
    }

    public void refreshReports() {
        mExecutor.execute(() -> {
            List<Class<? extends AbstractReport>> reportClasses = mPrefs.getReportOrder();

            // Si el orden o la cantidad ha cambiado, reconstruimos la lista base
            if (mCachedReports.size() != reportClasses.size()) {
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
    protected void onCleared() {
        super.onCleared();
        mExecutor.shutdown();
    }
}
