/*
 * Copyright 2026 Juanro49
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
package org.juanro.autumandu.gui.chart.kubit

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import org.juanro.autumandu.R
import org.juanro.autumandu.data.calculation.CalculationItem
import org.juanro.autumandu.data.report.AbstractReport
import org.juanro.autumandu.data.report.AbstractReportChartColumnData
import org.juanro.autumandu.data.report.AbstractReportChartData
import java.util.ArrayList
import java.util.Locale

object KubitChartBridge {
    @JvmStatic
    fun getColors(context: Context): IntArray {
        return intArrayOf(
            ContextCompat.getColor(context, R.color.blue),
            ContextCompat.getColor(context, R.color.purple),
            ContextCompat.getColor(context, R.color.green),
            ContextCompat.getColor(context, R.color.amber),
            ContextCompat.getColor(context, R.color.red)
        )
    }

    @JvmStatic
    @JvmOverloads
    fun createLineChart(
        context: Context,
        report: AbstractReport,
        rawData: List<AbstractReportChartData>,
        chartOption: Int = 0,
        showTrend: Boolean = false,
        showOverallTrend: Boolean = false,
        isFullScreen: Boolean = false
    ): ComposeView {
        val finalData = addTrendData(rawData, showTrend, showOverallTrend)
        return ComposeView(context).apply {
            id = R.id.kubit_chart_view
            isSaveEnabled = false
            setContent {
                DisableSaveableState {
                    KubitLineChart(
                        rawData = finalData,
                        yAxisLabel = { report.formatYValue(it, chartOption) },
                        xAxisLabel = { report.formatXValue(it, chartOption) },
                        isFullScreen = isFullScreen
                    )
                }
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun createColumnChart(
        context: Context,
        report: AbstractReport,
        rawData: List<AbstractReportChartData>,
        chartOption: Int = 0,
        showTrend: Boolean = false,
        showOverallTrend: Boolean = false,
        isFullScreen: Boolean = false
    ): ComposeView {
        val finalData = addTrendData(rawData, showTrend, showOverallTrend)
        return ComposeView(context).apply {
            id = R.id.kubit_chart_view
            isSaveEnabled = false
            setContent {
                DisableSaveableState {
                    KubitColumnChart(
                        rawData = finalData,
                        yAxisLabel = { report.formatYValue(it, chartOption) },
                        xAxisLabel = { report.formatXValue(it, chartOption) },
                        isFullScreen = isFullScreen
                    )
                }
            }
        }
    }

    private fun addTrendData(
        rawData: List<AbstractReportChartData>,
        showTrend: Boolean,
        showOverallTrend: Boolean
    ): List<AbstractReportChartData> {
        if (!showTrend && !showOverallTrend) return rawData

        val finalData = rawData.toMutableList()
        rawData.forEach { data ->
            if (showTrend) {
                val trend = data.createTrendData()
                if (!trend.isEmpty) finalData.add(trend)
            }
            if (showOverallTrend) {
                val overallTrend = data.createOverallTrendData()
                if (!overallTrend.isEmpty) finalData.add(overallTrend)
            }
        }
        return finalData
    }

    @JvmStatic
    fun createPieChart(
        context: Context,
        report: AbstractReport,
        rawData: List<AbstractReportChartData>,
        chartOption: Int = 0 // opción 0: Donut, 1: Tarta
    ): ComposeView {
        return ComposeView(context).apply {
            id = R.id.kubit_chart_view
            isSaveEnabled = false
            setContent {
                DisableSaveableState {
                    KubitPieChart(
                        rawData = rawData,
                        chartOption = chartOption,
                        yAxisLabel = { report.formatYValue(it, chartOption) }
                    )
                }
            }
        }
    }

    @JvmStatic
    fun createCalculatorChart(
        context: Context,
        items: Array<CalculationItem>,
        outputUnit: String
    ): ComposeView {
        val rawData: MutableList<AbstractReportChartData> = ArrayList()
        for (i in items.indices) {
            val item = items[i]
            val data: AbstractReportChartColumnData = object : AbstractReportChartColumnData(context, item.name(), item.color()) {}
            data.dataPoints.add(AbstractReportChartData.DataPoint(i.toFloat(), item.value().toFloat(), item.name()))
            rawData.add(data)
        }

        return ComposeView(context).apply {
            id = R.id.kubit_chart_view
            setContent {
                DisableSaveableState {
                    KubitColumnChart(
                        rawData = rawData,
                        yAxisLabel = { String.format(Locale.getDefault(), "%.2f %s", it, outputUnit) },
                        xAxisLabel = {
                            val index = it.toInt()
                            if (index >= 0 && index < items.size) items[index].name() else ""
                        },
                        xAxisLabelRotation = 0f,
                        isCalculator = true
                    )
                }
            }
        }
    }
}

@Composable
internal fun DisableSaveableState(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
        content()
    }
}
