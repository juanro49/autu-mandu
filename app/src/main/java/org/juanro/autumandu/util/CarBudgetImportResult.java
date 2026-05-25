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

public class CarBudgetImportResult {
    private int refuelingCount = 0;
    private int otherCostCount = 0;
    private int tireCount = 0;
    private boolean success = false;
    private String errorMessage = "";

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setRefuelingCount(int refuelingCount) {
        this.refuelingCount = refuelingCount;
    }

    public int getRefuelingCount() {
        return refuelingCount;
    }

    public void setOtherCostCount(int otherCostCount) {
        this.otherCostCount = otherCostCount;
    }

    public int getOtherCostCount() {
        return otherCostCount;
    }

    public void setTireCount(int tireCount) {
        this.tireCount = tireCount;
    }

    public int getTireCount() {
        return tireCount;
    }
}
