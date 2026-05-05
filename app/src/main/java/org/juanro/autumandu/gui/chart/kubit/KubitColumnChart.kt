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

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import org.juanro.autumandu.R
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kubit.charts.components.axis.AxisLabelCenterAlignment
import com.kubit.charts.components.axis.AxisLabelHorizontalAlignment
import com.kubit.charts.components.axis.AxisLabelVerticalAlignment
import com.kubit.charts.components.axis.HorizontalAxisChart
import com.kubit.charts.components.axis.VerticalAxisChart
import com.kubit.charts.components.axis.model.AxisData
import com.kubit.charts.components.axis.model.AxisPadding
import com.kubit.charts.components.axis.model.AxisStep
import com.kubit.charts.components.axis.model.AxisStepStyle
import com.kubit.charts.components.chart.barchart.BarChart
import com.kubit.charts.components.chart.barchart.model.BarChartData
import com.kubit.charts.components.chart.barchart.model.BarChartOrientation
import com.kubit.charts.components.chart.barchart.model.BarChartSegmentData
import com.kubit.charts.components.chart.barchart.model.BarChartType
import com.kubit.charts.components.chart.linechart.LineChart
import com.kubit.charts.components.chart.linechart.model.Line
import com.kubit.charts.components.chart.linechart.model.LineStyle
import com.kubit.charts.components.chart.linechart.model.Point
import com.kubit.charts.components.scaffold.ChartScaffold
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.juanro.autumandu.data.report.AbstractReportChartData
import org.juanro.autumandu.data.report.OverallTrendReportChartData
import org.juanro.autumandu.data.report.TrendReportChartData

@Composable
fun KubitColumnChart(
    rawData: List<AbstractReportChartData>,
    yAxisLabel: (Float) -> String,
    xAxisLabel: (Float) -> String,
    config: ColumnChartConfig = ColumnChartConfig()
) {
    if (rawData.isEmpty() || rawData.all { it.dataPoints.isEmpty() }) return

    DisableSaveableState {
        val isDark = isSystemInDarkTheme()
        val textColor = if (isDark) Color.White else Color.Black

        var selectedSegment by remember { mutableStateOf<BarChartSegmentData?>(null) }

        val series = remember(rawData) { separateSeries(rawData) }
        val allXValues = remember(series.barSeries) { extractSortedXValues(series.barSeries) }
        val bounds = remember(series, allXValues) { calculateStackedBounds(series, allXValues) }

        val chartResources = loadChartResources(config)
        val yBounds = calculateEffectiveYBounds(bounds)

        val barDataList = remember(allXValues, rawData, selectedSegment, chartResources.segmentFormat, chartResources.barThickness) {
            createBarDataList(allXValues, rawData, selectedSegment, chartResources.segmentFormat, chartResources.barThickness, yAxisLabel)
        }

        val trendLines = remember(series.trendSeries, allXValues) {
            createTrendLines(series.trendSeries, allXValues)
        }

        val styles = createChartStyles(isDark, textColor)
        val ySteps = remember(yBounds.effectiveYMin, yBounds.effectiveRangeY) {
            createYSteps(yBounds.effectiveYMin, yBounds.effectiveRangeY, styles.labelStyle, styles.gridStyle, yAxisLabel)
        }
        val xSteps = remember(allXValues) {
            createXSteps(allXValues, styles.labelStyle, styles.gridStyle, xAxisLabel)
        }

        ChartContainer(
            config = config,
            uiConfig = ChartUiConfig(isDark, textColor, AxisData(xSteps), AxisData(ySteps), chartResources.xStepSize, chartResources.axisPadding, yBounds.effectiveRangeY, config.xAxisLabelRotation),
            dataConfig = ChartDataConfig(barDataList, trendLines, selectedSegment),
            yAxisLabel = yAxisLabel,
            onSelectionClear = { selectedSegment = null },
            onSegmentClick = { segment -> selectedSegment = if (selectedSegment == segment) null else segment }
        )
    }
}

private data class ChartResources(
    val segmentFormat: String,
    val barThickness: androidx.compose.ui.unit.Dp,
    val xStepSize: androidx.compose.ui.unit.Dp,
    val axisPadding: AxisPadding
)

@Composable
private fun loadChartResources(config: ColumnChartConfig): ChartResources {
    return ChartResources(
        segmentFormat = stringResource(id = R.string.chart_content_description_segment),
        barThickness = dimensionResource(id = R.dimen.chart_bar_thickness),
        xStepSize = dimensionResource(id = if (config.isCalculator) R.dimen.chart_calculator_x_step else R.dimen.chart_column_x_step),
        axisPadding = AxisPadding(
            start = dimensionResource(id = if (config.isCalculator) R.dimen.chart_calculator_axis_padding_start else R.dimen.chart_axis_padding_start),
            end = 20.dp,
            top = 0.dp,
            bottom = dimensionResource(id = R.dimen.chart_axis_padding_bottom)
        )
    )
}

private data class EffectiveYBounds(val effectiveYMin: Float, val effectiveYMax: Float, val effectiveRangeY: Float)

private fun calculateEffectiveYBounds(bounds: StackedBounds): EffectiveYBounds {
    val rangeY = bounds.maxY - bounds.minY
    val safeRangeY = if (rangeY > 0f) rangeY else 1f
    val effectiveYMin = bounds.minY - safeRangeY * 0.10f
    val effectiveYMax = bounds.maxY + safeRangeY * 0.15f
    return EffectiveYBounds(effectiveYMin, effectiveYMax, effectiveYMax - effectiveYMin)
}

private data class ChartStyles(val labelStyle: TextStyle, val gridStyle: AxisStepStyle)

private fun createChartStyles(isDark: Boolean, textColor: Color): ChartStyles {
    val labelStyle = TextStyle(color = textColor.copy(alpha = 0.5f), fontSize = 9.sp)
    val gridStyle = AxisStepStyle.dashed(
        strokeWidth = 1.dp,
        strokeColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f),
        dashLength = 4.dp,
        gapLength = 4.dp,
        phase = 0.dp
    )
    return ChartStyles(labelStyle, gridStyle)
}

data class ColumnChartConfig(
    val xAxisLabelRotation: Float = 0f,
    val isCalculator: Boolean = false,
    val isFullScreen: Boolean = false
)

private data class ChartUiConfig(
    val isDark: Boolean,
    val textColor: Color,
    val xAxisData: AxisData,
    val yAxisData: AxisData,
    val xStepSize: androidx.compose.ui.unit.Dp,
    val axisPadding: AxisPadding,
    val effectiveRangeY: Float,
    val xAxisLabelRotation: Float
)

private data class ChartDataConfig(
    val barDataList: PersistentList<BarChartData>,
    val trendLines: PersistentList<Line>,
    val selectedSegment: BarChartSegmentData?
)

private data class SplitSeries(val trendSeries: List<AbstractReportChartData>, val barSeries: List<AbstractReportChartData>)

private fun separateSeries(rawData: List<AbstractReportChartData>): SplitSeries {
    val trendSeries = rawData.filter { it is TrendReportChartData || it is OverallTrendReportChartData }
    val barSeries = rawData.filter { it !in trendSeries }
    return SplitSeries(trendSeries, barSeries)
}

private fun extractSortedXValues(barSeries: List<AbstractReportChartData>): List<Float> {
    return barSeries.flatMap { it.dataPoints.filter { p -> p.x.isFinite() && p.y.isFinite() } }
        .map { it.x }.distinct().sorted()
}

private data class StackedBounds(val minY: Float, val maxY: Float)

private fun calculateStackedBounds(series: SplitSeries, allXValues: List<Float>): StackedBounds {
    var maxStackedY = 0f
    var minStackedY = 0f
    allXValues.forEach { x ->
        var posSumAtX = 0f
        var negSumAtX = 0f
        series.barSeries.forEach { s ->
            s.dataPoints.find { it.x == x }?.let { point ->
                if (point.y > 0) posSumAtX += point.y
                else negSumAtX += point.y
            }
        }
        maxStackedY = maxOf(maxStackedY, posSumAtX)
        minStackedY = minOf(minStackedY, negSumAtX)
    }

    series.trendSeries.forEach { s ->
        s.dataPoints.forEach { point ->
            if (point.y.isFinite()) {
                maxStackedY = maxOf(maxStackedY, point.y)
                minStackedY = minOf(minStackedY, point.y)
            }
        }
    }
    return StackedBounds(minOf(0f, minStackedY), maxOf(0f, maxStackedY))
}

private fun createBarDataList(
    allXValues: List<Float>,
    rawData: List<AbstractReportChartData>,
    selectedSegment: BarChartSegmentData?,
    segmentFormat: String,
    barThickness: androidx.compose.ui.unit.Dp,
    yAxisLabel: (Float) -> String
): PersistentList<BarChartData> {
    return allXValues.mapIndexed { index, x ->
        var positiveAccumulator = 0.0
        var negativeAccumulator = 0.0

        val posSegments = mutableListOf<BarChartSegmentData>()
        val negSegments = mutableListOf<BarChartSegmentData>()

        rawData.forEach { series ->
            series.dataPoints.find { it.x == x }?.let { point ->
                val valY = point.y.toDouble()
                val segmentDescription = String.format(segmentFormat, series.name, yAxisLabel(point.y))
                if (valY >= 0) {
                    posSegments.add(BarChartSegmentData(
                        maxValue = positiveAccumulator + valY,
                        minValue = positiveAccumulator,
                        color = Color(series.color),
                        contentDescription = segmentDescription,
                        label = series.name
                    ))
                    positiveAccumulator += valY
                } else {
                    negSegments.add(BarChartSegmentData(
                        maxValue = negativeAccumulator,
                        minValue = negativeAccumulator + valY,
                        color = Color(series.color),
                        contentDescription = segmentDescription,
                        label = series.name
                    ))
                    negativeAccumulator += valY
                }
            }
        }

        val finalSegments = (negSegments.asReversed() + posSegments).map { segment ->
            val isSelected = selectedSegment?.let { sel ->
                sel.label == segment.label &&
                        sel.minValue == segment.minValue &&
                        sel.maxValue == segment.maxValue
            } ?: false

            if (selectedSegment != null && !isSelected) {
                segment.copy(color = segment.color.copy(alpha = 0.4f))
            } else {
                segment
            }
        }.toPersistentList()

        BarChartData(
            type = BarChartType.Stacked,
            stepPosition = index.toFloat(),
            segments = finalSegments,
            barThickness = barThickness,
            orientation = BarChartOrientation.Vertical
        )
    }.toPersistentList()
}

private fun createTrendLines(trendSeries: List<AbstractReportChartData>, allXValues: List<Float>): PersistentList<Line> {
    return trendSeries.map { series ->
        val lineColor = Color(series.color)
        Line(
            dataPoints = series.dataPoints.mapNotNull { dp ->
                val xIndex = allXValues.indexOf(dp.x)
                if (xIndex != -1) {
                    Point(xIndex.toFloat(), dp.y)
                } else null
            },
            lineStyle = LineStyle(color = lineColor.copy(alpha = 0.7f), width = 3.5f)
        )
    }.filter { it.dataPoints.isNotEmpty() }.toPersistentList()
}

private fun createYSteps(
    min: Float,
    range: Float,
    labelStyle: TextStyle,
    gridStyle: AxisStepStyle,
    yAxisLabel: (Float) -> String
): PersistentList<AxisStep> {
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
    allXValues: List<Float>,
    labelStyle: TextStyle,
    gridStyle: AxisStepStyle,
    xAxisLabel: (Float) -> String
): PersistentList<AxisStep> {
    val steps = mutableListOf<AxisStep>()
    steps.add(AxisStep(-0.5f, "", labelStyle, stepStyle = null))
    for (i in allXValues.indices) {
        steps.add(AxisStep(
            axisValue = i.toFloat(),
            axisLabel = xAxisLabel(allXValues[i]),
            labelStyle = labelStyle,
            stepStyle = gridStyle
        ))
    }
    steps.add(AxisStep(allXValues.size - 0.5f, "", labelStyle, stepStyle = null))
    return steps.toPersistentList()
}

@Composable
private fun ChartContainer(
    config: ColumnChartConfig,
    uiConfig: ChartUiConfig,
    dataConfig: ChartDataConfig,
    yAxisLabel: (Float) -> String,
    onSelectionClear: () -> Unit,
    onSegmentClick: (BarChartSegmentData) -> Unit
) {
    val chartDescription = stringResource(id = R.string.chart_content_description_column)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (config.isCalculator || config.isFullScreen) Modifier.fillMaxHeight() else Modifier.height(dimensionResource(id = R.dimen.chart_height)))
            .semantics { contentDescription = chartDescription }
    ) {
        SelectionPanel(dataConfig.selectedSegment, uiConfig.isDark, uiConfig.textColor, yAxisLabel)

        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSelectionClear
                    )
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val dynamicYStepSize = (maxHeight - uiConfig.axisPadding.bottom - uiConfig.axisPadding.top) / uiConfig.effectiveRangeY
                ChartScaffold(
                    modifier = Modifier.fillMaxSize().background(Color.Transparent),
                    xAxisData = uiConfig.xAxisData,
                    yAxisData = uiConfig.yAxisData,
                    xUnitSize = uiConfig.xStepSize,
                    yUnitSize = dynamicYStepSize,
                    axisPadding = uiConfig.axisPadding,
                    isPinchZoomEnabled = true,
                    horizontalAxis = { scroll, zoom, padding ->
                        HorizontalAxisChart(
                            data = uiConfig.xAxisData,
                            labelHeight = dimensionResource(id = if (config.isCalculator) R.dimen.chart_calculator_label_height else R.dimen.chart_axis_label_height),
                            horizontalScroll = scroll,
                            zoom = zoom,
                            fixedUnitSize = uiConfig.xStepSize,
                            padding = padding,
                            labelVerticalAlignment = AxisLabelVerticalAlignment.Top,
                            labelVerticalGap = 2.dp,
                            labelRotation = uiConfig.xAxisLabelRotation,
                            labelCenterAlignment = AxisLabelCenterAlignment.Center,
                            labelsBackgroundColor = Color.Transparent
                        )
                    },
                    verticalAxis = { scroll, zoom, padding ->
                        VerticalAxisChart(
                            data = uiConfig.yAxisData,
                            labelWidth = dimensionResource(id = if (config.isCalculator) R.dimen.chart_calculator_label_width else R.dimen.chart_axis_label_width),
                            verticalScroll = scroll,
                            zoom = zoom,
                            fixedUnitSize = dynamicYStepSize,
                            padding = padding,
                            labelHorizontalAlignment = AxisLabelHorizontalAlignment.End,
                            labelHorizontalGap = 1.dp,
                            labelsBackgroundColor = Color.Transparent
                        )
                    },
                    content = { scaffoldData ->
                        ChartMainContent(
                            isCalculator = config.isCalculator,
                            dataConfig = dataConfig,
                            uiConfig = uiConfig,
                            yStepSize = dynamicYStepSize,
                            stateParams = ChartStateParams(
                                horizontalScroll = scaffoldData.horizontalScroll,
                                zoom = scaffoldData.zoom,
                                onScrollRequest = scaffoldData.onHorizontalScrollChangeRequest
                            ),
                            onSegmentClick = onSegmentClick
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectionPanel(selectedSegment: BarChartSegmentData?, isDark: Boolean, textColor: Color, yAxisLabel: (Float) -> String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(dimensionResource(id = R.dimen.chart_selection_panel_height)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = selectedSegment != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (selectedSegment != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background((if (isDark) Color.Black else Color.White).copy(alpha = 0.8f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    val value = if (selectedSegment.minValue < 0.0) {
                        selectedSegment.minValue - selectedSegment.maxValue
                    } else {
                        selectedSegment.maxValue - selectedSegment.minValue
                    }

                    Text(
                        text = "${selectedSegment.label}: ${yAxisLabel(value.toFloat())}",
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class ChartStateParams(
    val horizontalScroll: androidx.compose.ui.unit.Dp,
    val zoom: Float,
    val onScrollRequest: ((Float) -> Unit)?
)

@Composable
private fun ChartMainContent(
    isCalculator: Boolean,
    dataConfig: ChartDataConfig,
    uiConfig: ChartUiConfig,
    yStepSize: androidx.compose.ui.unit.Dp,
    stateParams: ChartStateParams,
    onSegmentClick: (BarChartSegmentData) -> Unit
) {
    var initialScrollDone by remember { mutableStateOf(false) }
    LaunchedEffect(stateParams.onScrollRequest) {
        if (!initialScrollDone) {
            val scrollTarget = if (isCalculator) 0f else Float.MAX_VALUE
            stateParams.onScrollRequest?.invoke(scrollTarget)
            initialScrollDone = true
        }
    }

    BarChart(
        data = dataConfig.barDataList,
        xAxisData = uiConfig.xAxisData,
        yAxisData = uiConfig.yAxisData,
        xAxisStepSize = uiConfig.xStepSize,
        yAxisStepSize = yStepSize,
        zoom = stateParams.zoom,
        horizontalScroll = stateParams.horizontalScroll,
        modifier = Modifier.fillMaxSize().background(Color.Transparent).clipToBounds(),
        onBarClick = onSegmentClick
    )

    if (dataConfig.trendLines.isNotEmpty()) {
        LineChart(
            modifier = Modifier.fillMaxSize().clipToBounds(),
            lines = dataConfig.trendLines,
            xAxisData = uiConfig.xAxisData,
            yAxisData = uiConfig.yAxisData,
            xAxisStepSize = uiConfig.xStepSize,
            yAxisStepSize = yStepSize,
            horizontalScroll = stateParams.horizontalScroll,
            zoom = stateParams.zoom,
            backgroundColor = Color.Transparent
        )
    }
}
