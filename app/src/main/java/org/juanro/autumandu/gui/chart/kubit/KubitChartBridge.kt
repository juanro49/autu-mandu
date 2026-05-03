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
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.core.content.ContextCompat
import org.juanro.autumandu.Preferences
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
                AutuManduChartTheme {
                    KubitLineChart(
                        rawData = finalData,
                        yAxisLabel = { report.formatYValue(it, chartOption) },
                        xAxisLabel = { report.formatXValue(it, chartOption) },
                        config = LineChartConfig(isFullScreen = isFullScreen)
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
                AutuManduChartTheme {
                    KubitColumnChart(
                        rawData = finalData,
                        yAxisLabel = { report.formatYValue(it, chartOption) },
                        xAxisLabel = { report.formatXValue(it, chartOption) },
                        config = ColumnChartConfig(isFullScreen = isFullScreen)
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
                AutuManduChartTheme {
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
                AutuManduChartTheme {
                    KubitColumnChart(
                        rawData = rawData,
                        yAxisLabel = { String.format(Locale.getDefault(), "%.2f %s", it, outputUnit) },
                        xAxisLabel = {
                            val index = it.toInt()
                            if (index >= 0 && index < items.size) items[index].name() else ""
                        },
                        config = ColumnChartConfig(isCalculator = true)
                    )
                }
            }
        }
    }
}

@Composable
fun AutuManduChartTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { Preferences(context) }
    val isDark = isSystemInDarkTheme()
    val dynamicColor = preferences.isDynamicColorEnabled

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme(
            primary = colorResource(R.color.primary_dark),
            onPrimary = colorResource(R.color.onPrimary_dark),
            primaryContainer = colorResource(R.color.primaryContainer_dark),
            onPrimaryContainer = colorResource(R.color.onPrimaryContainer_dark),
            secondary = colorResource(R.color.secondary_dark),
            onSecondary = colorResource(R.color.onSecondary_dark),
            secondaryContainer = colorResource(R.color.secondaryContainer_dark),
            onSecondaryContainer = colorResource(R.color.onSecondaryContainer_dark),
            tertiary = colorResource(R.color.tertiary_dark),
            onTertiary = colorResource(R.color.onTertiary_dark),
            tertiaryContainer = colorResource(R.color.tertiaryContainer_dark),
            onTertiaryContainer = colorResource(R.color.onTertiaryContainer_dark),
            error = colorResource(R.color.error_dark),
            onError = colorResource(R.color.onError_dark),
            errorContainer = colorResource(R.color.errorContainer_dark),
            onErrorContainer = colorResource(R.color.onErrorContainer_dark),
            background = colorResource(R.color.background_dark),
            onBackground = colorResource(R.color.onBackground_dark),
            surface = colorResource(R.color.surface_dark),
            onSurface = colorResource(R.color.onSurface_dark),
            surfaceVariant = colorResource(R.color.surfaceVariant_dark),
            onSurfaceVariant = colorResource(R.color.onSurfaceVariant_dark),
            outline = colorResource(R.color.outline_dark)
        )
        else -> lightColorScheme(
            primary = colorResource(R.color.primary_light),
            onPrimary = colorResource(R.color.onPrimary_light),
            primaryContainer = colorResource(R.color.primaryContainer_light),
            onPrimaryContainer = colorResource(R.color.onPrimaryContainer_light),
            secondary = colorResource(R.color.secondary_light),
            onSecondary = colorResource(R.color.onSecondary_light),
            secondaryContainer = colorResource(R.color.secondaryContainer_light),
            onSecondaryContainer = colorResource(R.color.onSecondaryContainer_light),
            tertiary = colorResource(R.color.tertiary_light),
            onTertiary = colorResource(R.color.onTertiary_light),
            tertiaryContainer = colorResource(R.color.tertiaryContainer_light),
            onTertiaryContainer = colorResource(R.color.onTertiaryContainer_light),
            error = colorResource(R.color.error_light),
            onError = colorResource(R.color.onError_light),
            errorContainer = colorResource(R.color.errorContainer_light),
            onErrorContainer = colorResource(R.color.onErrorContainer_light),
            background = colorResource(R.color.background_light),
            onBackground = colorResource(R.color.onBackground_light),
            surface = colorResource(R.color.surface_light),
            onSurface = colorResource(R.color.onSurface_light),
            surfaceVariant = colorResource(R.color.surfaceVariant_light),
            onSurfaceVariant = colorResource(R.color.onSurfaceVariant_light),
            outline = colorResource(R.color.outline_light)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes(
            small = RoundedCornerShape(dimensionResource(R.dimen.corner_small)),
            medium = RoundedCornerShape(dimensionResource(R.dimen.corner_medium)),
            large = RoundedCornerShape(dimensionResource(R.dimen.corner_large)),
            extraLarge = RoundedCornerShape(dimensionResource(R.dimen.corner_extra_large))
        ),
        content = content
    )
}

@Composable
internal fun DisableSaveableState(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
        content()
    }
}
