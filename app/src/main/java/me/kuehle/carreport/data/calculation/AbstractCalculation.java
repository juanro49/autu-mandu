/*
 * Copyright 2015 Jan Kühle
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

package me.kuehle.carreport.data.calculation;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCalculation {
    public final class ForceLoadContentObserver extends ContentObserver {
        public ForceLoadContentObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            contentChanged();
        }
    }

    protected Context mContext;
    private ForceLoadContentObserver mInternalObserver;
    private List<ContentObserver> mPublicObservers;
    private boolean mDataChanged;

    public AbstractCalculation(Context context) {
        mContext = context;
        mInternalObserver = new ForceLoadContentObserver();
        mPublicObservers = new ArrayList<>();
        mDataChanged = true;
    }

    public void registerContentObserver(ContentObserver observer) {
        mPublicObservers.add(observer);
    }

    public void unregisterContentObserver(ContentObserver observer) {
        mPublicObservers.remove(observer);
    }

    public abstract String getName();

    public abstract String getInputUnit();

    public abstract String getOutputUnit();

    public abstract boolean hasColors();

    public CalculationItem[] calculate(double input) {
        if (mDataChanged) {
            onLoadData(mInternalObserver);
        }

        return onCalculate(input);
    }

    protected abstract void onLoadData(ContentObserver observer);

    protected abstract CalculationItem[] onCalculate(double input);

    private void contentChanged() {
        mDataChanged = true;

        for (ContentObserver observer : mPublicObservers) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                observer.dispatchChange(false, null);
            }else {
                observer.dispatchChange(false);
            }
        }
    }
}
