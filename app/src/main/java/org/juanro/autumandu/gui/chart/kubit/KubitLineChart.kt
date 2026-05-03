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

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.juanro.autumandu.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kubit.charts.components.axis.AxisLabelHorizontalAlignment
import com.kubit.charts.components.axis.AxisLabelVerticalAlignment
import com.kubit.charts.components.axis.HorizontalAxisChart
import com.kubit.charts.components.axis.VerticalAxisChart
import com.kubit.charts.components.axis.model.AxisData
import com.kubit.charts.components.axis.model.AxisPadding
import com.kubit.charts.components.axis.model.AxisStep
import com.kubit.charts.components.axis.model.AxisStepStyle
import com.kubit.charts.components.chart.linechart.LineChart
import com.kubit.charts.components.chart.linechart.model.IntersectionPoint
import com.kubit.charts.components.chart.linechart.model.Line
import com.kubit.charts.components.chart.linechart.model.LineStyle
import com.kubit.charts.components.chart.linechart.model.Point
import com.kubit.charts.components.chart.linechart.model.SelectionHighlightPoint
import com.kubit.charts.components.chart.linechart.model.SelectionHighlightPopUp
import com.kubit.charts.components.scaffold.ChartScaffold
import com.kubit.charts.components.scaffold.ChartScaffoldContentData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.juanro.autumandu.data.report.AbstractReportChartData
import org.juanro.autumandu.data.report.AbstractReportChartLineData
import org.juanro.autumandu.data.report.OverallTrendReportChartData
import org.juanro.autumandu.data.report.ReportDateHelper
import org.juanro.autumandu.data.report.TrendReportChartData
import java.util.Calendar

@Composable
fun KubitLineChart(
    rawData: List<AbstractReportChartData>,
    yAxisLabel: (Float) -> String,
    xAxisLabel: (Float) -> String,
    config: LineChartConfig = LineChartConfig()
) {
    if (rawData.isEmpty() || rawData.all { it.dataPoints.isEmpty() }) return

    DisableSaveableState {
        val isDark = isSystemInDarkTheme()
        val textColor = if (isDark) Color.White else Color.Black

        val bounds = remember(rawData) { calculateBounds(rawData) }

        // Rango de X ajustado para que los puntos de los extremos sean visibles
        val rangeX = bounds.maxX - bounds.minX
        val safeRangeX = if (rangeX > 0f) rangeX else 1f
        val effectiveXMin = bounds.minX - safeRangeX * 0.02f
        val effectiveXMax = bounds.maxX + safeRangeX * 0.005f

        // Rango de Y optimizado
        val rangeY = bounds.maxY - bounds.minY
        val safeRangeY = if (rangeY > 0f) rangeY else 1f

        // 10% de margen abajo para no chocar con el eje X, 15% arriba para el popup
        val effectiveYMin = bounds.minY - safeRangeY * 0.10f
        val effectiveYMax = bounds.maxY + safeRangeY * 0.15f
        val effectiveRangeY = effectiveYMax - effectiveYMin

        val lines = remember(rawData, textColor, isDark) {
            createLines(rawData, textColor, isDark, yAxisLabel)
        }

        val labelStyle = remember(textColor) { TextStyle(color = textColor.copy(alpha = 0.5f), fontSize = 9.sp) }
        val gridStyle = remember(isDark) {
            AxisStepStyle.dashed(
                strokeWidth = 1.dp,
                strokeColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f),
                dashLength = 4.dp,
                gapLength = 4.dp,
                phase = 0.dp
            )
        }

        val ySteps = remember(effectiveYMin, effectiveRangeY) {
            createYSteps(effectiveYMin, effectiveRangeY, labelStyle, gridStyle, yAxisLabel)
        }

        val xSteps = remember(rangeX, bounds.minX, bounds.maxX, effectiveXMin, effectiveXMax) {
            createXSteps(rangeX, bounds.minX, bounds.maxX, effectiveXMin, effectiveXMax, labelStyle, gridStyle, xAxisLabel)
        }

        val yAxisData = remember(ySteps) { AxisData(axisSteps = ySteps) }
        val xAxisData = remember(xSteps) { AxisData(axisSteps = xSteps) }

        val baseStepSize = dimensionResource(id = R.dimen.chart_line_x_step_base)
        val xStepSize = remember(rangeX, config.isFullScreen, baseStepSize) {
            if (rangeX > 0 && !config.isFullScreen) {
                baseStepSize * 1.8f
            } else baseStepSize
        }
        val chartHeight = dimensionResource(id = R.dimen.chart_canvas_height)
        val yStepSize = chartHeight / effectiveRangeY
        val axisPadding = AxisPadding(
            start = dimensionResource(id = R.dimen.chart_axis_padding_start),
            end = 0.dp,
            top = 0.dp,
            bottom = dimensionResource(id = R.dimen.chart_axis_padding_bottom)
        )

        ChartContent(
            config = config,
            uiConfig = LineChartUiConfig(xAxisData, yAxisData, xStepSize, yStepSize, axisPadding),
            lines = lines
        )
    }
}

data class LineChartConfig(val isFullScreen: Boolean = false)

private data class LineChartUiConfig(
    val xAxisData: AxisData,
    val yAxisData: AxisData,
    val xStepSize: Dp,
    val yStepSize: Dp,
    val axisPadding: AxisPadding
)

private data class ChartBounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float)

private fun calculateBounds(rawData: List<AbstractReportChartData>): ChartBounds {
    val validPoints = rawData.flatMap { it.dataPoints }
        .filter { it.x.isFinite() && it.y.isFinite() }

    if (validPoints.isEmpty()) {
        return ChartBounds(0f, 1f, 0f, 1f)
    }

    val minX = validPoints.minOf { it.x }
    var maxX = validPoints.maxOf { it.x }
    val minY = validPoints.minOf { it.y }
    val maxY = validPoints.maxOf { it.y }

    if (maxX == minX) maxX = minX + 1f
    return ChartBounds(minX, maxX, minY, maxY)
}

private fun createLines(
    rawData: List<AbstractReportChartData>,
    textColor: Color,
    isDark: Boolean,
    yAxisLabel: (Float) -> String
): ImmutableList<Line> {
    return rawData.mapIndexed { index, data ->
        val lineColor = Color(data.color)
        val isTrend = data is TrendReportChartData || data is OverallTrendReportChartData
        Line(
            dataPoints = data.dataPoints.filter { it.x.isFinite() && it.y.isFinite() }.map { dp ->
                val isMarked = data is AbstractReportChartLineData && data.isMarked(dp.x)
                val xOffset = index * 0.0001f
                Point(
                    dp.x + xOffset,
                    dp.y,
                    intersectionNode = if (isTrend) null else IntersectionPoint(
                        radius = 4.dp, // Assuming radius fix
                        color = if (isMarked) textColor else lineColor
                    )
                )
            },
            lineStyle = if (isTrend) {
                LineStyle(color = lineColor.copy(alpha = 0.7f), width = 3.5f)
            } else {
                LineStyle(color = lineColor, width = 5.5f)
            },
            selectionHighlightPoint = SelectionHighlightPoint(color = textColor),
            selectionHighlightPopUp = SelectionHighlightPopUp(
                backgroundColor = (if (isDark) Color.Black else Color.White).copy(alpha = 0.8f),
                labelColor = textColor,
                labelSize = 10.sp,
                paddingBetweenPopUpAndPoint = 8.dp,
                popUpLabel = { _, y -> yAxisLabel(y) }
            )
        )
    }.filter { it.dataPoints.isNotEmpty() }.toPersistentList()
}

private fun createYSteps(
    min: Float,
    range: Float,
    labelStyle: TextStyle,
    gridStyle: AxisStepStyle,
    yAxisLabel: (Float) -> String
): ImmutableList<AxisStep> {
    return (0..8).map { i ->
        val realValue = min + i.toFloat() * range / 8.2f
        AxisStep(
            axisValue = realValue,
            axisLabel = if (i == 0) "" else yAxisLabel(realValue),
            labelStyle = labelStyle,
            stepStyle = gridStyle
        )
    }.toPersistentList()
}

private fun createXSteps(
    rangeX: Float,
    minX: Float,
    maxX: Float,
    effectiveXMin: Float,
    effectiveXMax: Float,
    labelStyle: TextStyle,
    gridStyle: AxisStepStyle,
    xAxisLabel: (Float) -> String
): ImmutableList<AxisStep> {
    if (rangeX <= 0) {
        return persistentListOf(
            AxisStep(minX, xAxisLabel(minX), labelStyle, stepStyle = gridStyle)
        )
    }

    val steps = mutableListOf<AxisStep>()
    steps.add(AxisStep(effectiveXMin, "", labelStyle, stepStyle = null))

    val startCal = Calendar.getInstance().apply {
        time = ReportDateHelper.toDate(minX)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    val endCal = Calendar.getInstance().apply {
        time = ReportDateHelper.toDate(maxX)
    }

    while (startCal.before(endCal) || startCal == endCal) {
        val currentFloat = ReportDateHelper.toFloat(startCal.time)
        if (currentFloat >= minX) {
            steps.add(AxisStep(
                axisValue = currentFloat,
                axisLabel = xAxisLabel(currentFloat),
                labelStyle = labelStyle,
                stepStyle = gridStyle
            ))
        }
        startCal.add(Calendar.MONTH, 1)
    }

    if (steps.size <= 2) {
        steps.add(AxisStep(minX, xAxisLabel(minX), labelStyle, stepStyle = gridStyle))
        steps.add(AxisStep(maxX, xAxisLabel(maxX), labelStyle, stepStyle = gridStyle))
    }
    steps.add(AxisStep(effectiveXMax, "", labelStyle, stepStyle = null))
    return steps.toPersistentList()
}

@Composable
private fun ChartContent(
    config: LineChartConfig,
    uiConfig: LineChartUiConfig,
    lines: ImmutableList<Line>
) {
    var initialScrollDone by remember { mutableStateOf(false) }
    val chartDescription = stringResource(id = R.string.chart_content_description_line)

    Box(
        modifier = if (config.isFullScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ChartScaffold(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.chart_height))
                .background(Color.Transparent)
                .semantics { contentDescription = chartDescription },
            xAxisData = uiConfig.xAxisData,
            yAxisData = uiConfig.yAxisData,
            xUnitSize = uiConfig.xStepSize,
            yUnitSize = uiConfig.yStepSize,
            axisPadding = uiConfig.axisPadding,
            isPinchZoomEnabled = true,
            horizontalAxis = { scroll, zoom, padding ->
                HorizontalAxisChart(
                    data = uiConfig.xAxisData,
                    labelHeight = 25.dp,
                    horizontalScroll = scroll,
                    zoom = zoom,
                    fixedUnitSize = uiConfig.xStepSize,
                    padding = padding,
                    labelVerticalAlignment = AxisLabelVerticalAlignment.Top,
                    labelVerticalGap = dimensionResource(id = R.dimen.chart_label_padding),
                    labelsBackgroundColor = Color.Transparent
                )
            },
            verticalAxis = { scroll, zoom, padding ->
                VerticalAxisChart(
                    data = uiConfig.yAxisData,
                    labelWidth = 25.dp,
                    verticalScroll = scroll,
                    zoom = zoom,
                    fixedUnitSize = uiConfig.yStepSize,
                    padding = padding,
                    labelHorizontalAlignment = AxisLabelHorizontalAlignment.End,
                    labelHorizontalGap = 1.dp,
                    labelsBackgroundColor = Color.Transparent
                )
            },
            content = { scaffoldData ->
                LaunchedEffect(scaffoldData.onHorizontalScrollChangeRequest) {
                    if (!initialScrollDone) {
                        scaffoldData.onHorizontalScrollChangeRequest?.invoke(Float.MAX_VALUE)
                        initialScrollDone = true
                    }
                }

                LineChart(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                    lines = lines,
                    xAxisData = uiConfig.xAxisData,
                    yAxisData = uiConfig.yAxisData,
                    xAxisStepSize = uiConfig.xStepSize,
                    yAxisStepSize = uiConfig.yStepSize,
                    horizontalScroll = scaffoldData.horizontalScroll,
                    zoom = scaffoldData.zoom,
                    backgroundColor = Color.Transparent,
                    onPointSelect = { _, _ -> }
                )
            }
        )
    }
}
