/*
 * Copyright 2015 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.kuehle.carreport.data.report;

public class ReportChartOptions {
    private boolean mShowTrend = false;
    private boolean mShowOverallTrend = false;
    private int mChartOption = 0;

    public int getChartOption() {
        return mChartOption;
    }

    public boolean isShowTrend() {
        return mShowTrend;
    }

    public boolean isShowOverallTrend() {
        return mShowOverallTrend;
    }

    public void setChartOption(int chartOption) {
        mChartOption = chartOption;
    }

    public void setShowTrend(boolean showTrend) {
        mShowTrend = showTrend;
    }

    public void setShowOverallTrend(boolean showOverallTrend) {
        mShowOverallTrend = showOverallTrend;
    }
}
