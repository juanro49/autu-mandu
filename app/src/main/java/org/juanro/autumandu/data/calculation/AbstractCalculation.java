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

package org.juanro.autumandu.data.calculation;

import android.content.Context;

/**
 * Base class for all calculations.
 * Handles data lifecycle and provides a thread-safe entry point for calculations.
 */
public abstract class AbstractCalculation {

    protected final Context mContext;
    private volatile boolean mDataChanged = true;

    protected AbstractCalculation(Context context) {
        mContext = context.getApplicationContext();
    }

    public abstract String getName();

    public abstract String getInputUnit();

    public abstract String getOutputUnit();

    /**
     * Performs the calculation.
     * Note: This performs database operations, so it MUST be called from a background thread.
     * @param input The input value for the calculation.
     * @return An array of results.
     */
    public synchronized CalculationItem[] calculate(double input) {
        if (mDataChanged) {
            onLoadData();
            mDataChanged = false;
        }

        return onCalculate(input);
    }

    /**
     * Marks the data as changed, so it will be reloaded on the next calculation.
     */
    public void notifyDataChanged() {
        mDataChanged = true;
    }

    /**
     * Loads the required data from the database.
     * Implementations should optimize this to avoid N+1 query problems.
     */
    protected abstract void onLoadData();

    /**
     * Performs the actual mathematical calculation based on pre-loaded data.
     * @param input The user input.
     * @return Result items.
     */
    protected abstract CalculationItem[] onCalculate(double input);
}
