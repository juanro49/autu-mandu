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
package org.juanro.autumandu.data.report;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Encapsulates the configuration options for a report chart.
 */
public class ReportChartOptions {
    private boolean mShowTrend;
    private boolean mShowOverallTrend;
    private boolean mShowInvestment;
    private int mChartOption;

    public ReportChartOptions() {
        this(false, false, true, 0);
    }

    public ReportChartOptions(boolean showTrend, boolean showOverallTrend, boolean showInvestment, int chartOption) {
        mShowTrend = showTrend;
        mShowOverallTrend = showOverallTrend;
        mShowInvestment = showInvestment;
        mChartOption = chartOption;
    }

    public int getChartOption() {
        return mChartOption;
    }

    public boolean isShowTrend() {
        return mShowTrend;
    }

    public boolean isShowOverallTrend() {
        return mShowOverallTrend;
    }

    public boolean isShowInvestment() {
        return mShowInvestment;
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

    public void setShowInvestment(boolean showInvestment) {
        mShowInvestment = showInvestment;
    }

    public static ReportChartOptions load(Context context, String reportName) {
        var prefs = context.getSharedPreferences("org.juanro.autumandu.gui.fragment.ReportFragment", Context.MODE_PRIVATE);
        var options = new ReportChartOptions();
        options.setShowTrend(prefs.getBoolean(reportName + "_show_trend", false));
        options.setShowOverallTrend(prefs.getBoolean(reportName + "_show_overall_trend", false));
        options.setShowInvestment(prefs.getBoolean(reportName + "_show_investment", true));
        options.setChartOption(prefs.getInt(reportName + "_current_chart_option", 0));
        return options;
    }

    public void save(Context context, String reportName) {
        var prefsEdit = context.getSharedPreferences("org.juanro.autumandu.gui.fragment.ReportFragment", Context.MODE_PRIVATE).edit();
        prefsEdit.putBoolean(reportName + "_show_trend", isShowTrend());
        prefsEdit.putBoolean(reportName + "_show_overall_trend", isShowOverallTrend());
        prefsEdit.putBoolean(reportName + "_show_investment", isShowInvestment());
        prefsEdit.putInt(reportName + "_current_chart_option", getChartOption());
        prefsEdit.apply();
    }
}
