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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.juanro.autumandu.data.report.AbstractReportChartData
import org.juanro.autumandu.data.report.OverallTrendReportChartData
import org.juanro.autumandu.data.report.TrendReportChartData

@Composable
fun KubitColumnChart(
    rawData: List<AbstractReportChartData>,
    yAxisLabel: (Float) -> String,
    xAxisLabel: (Float) -> String,
    xAxisLabelRotation: Float = 0f,
    isCalculator: Boolean = false,
    isFullScreen: Boolean = false
) {
    if (rawData.isEmpty() || rawData.all { it.dataPoints.isEmpty() }) return

    DisableSaveableState {
        var selectedSegment by remember { mutableStateOf<BarChartSegmentData?>(null) }

    // Separar barras de tendencias
    val trendSeries = rawData.filter { it is TrendReportChartData || it is OverallTrendReportChartData }
    val barSeries = rawData.filter { it !in trendSeries }

    // Agrupar datos por valor X para manejar el apilado correctamente (usando solo series de barras)
    val allXValues = barSeries.flatMap { it.dataPoints.filter { p -> p.x.isFinite() && p.y.isFinite() } }
        .map { it.x }.distinct().sorted()

    // Calcular límites apilados
    var maxStackedY = 0f
    var minStackedY = 0f
    allXValues.forEach { x ->
        var posSumAtX = 0f
        var negSumAtX = 0f
        barSeries.forEach { series ->
            series.dataPoints.find { it.x == x }?.let { point ->
                if (point.y > 0) posSumAtX += point.y
                else negSumAtX += point.y
            }
        }
        maxStackedY = maxOf(maxStackedY, posSumAtX)
        minStackedY = minOf(minStackedY, negSumAtX)
    }

    // También considerar líneas de tendencia para los límites de Y
    trendSeries.forEach { series ->
        series.dataPoints.forEach { point ->
            if (point.y.isFinite()) {
                maxStackedY = maxOf(maxStackedY, point.y)
                minStackedY = minOf(minStackedY, point.y)
            }
        }
    }

    val yMinBound = minOf(0f, minStackedY)
    val yMaxBound = maxOf(0f, maxStackedY)
    val rangeY = yMaxBound - yMinBound
    val safeRangeY = if (rangeY > 0f) rangeY else 1f

    val effectiveYMin = yMinBound - safeRangeY * 0.10f
    val effectiveYMax = yMaxBound + safeRangeY * 0.15f
    val effectiveRangeY = effectiveYMax - effectiveYMin

    val segmentFormat = stringResource(id = R.string.chart_content_description_segment)
    val barThickness = dimensionResource(id = R.dimen.chart_bar_thickness)
    val barDataList = remember(allXValues, rawData, selectedSegment, segmentFormat, barThickness) {
        allXValues.mapIndexed { index, x ->
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

            // Secuencial: De lo más negativo a 0, luego de 0 a lo más positivo
            val finalSegments = (negSegments.asReversed() + posSegments).map { segment ->
                if (selectedSegment != null && selectedSegment != segment) {
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

    // Crear líneas de tendencia si las hay, remapeando X a índices
    val trendLines: ImmutableList<Line> = trendSeries.map { series ->
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

    val labelStyle = remember { TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp) }
    val gridStyle = remember {
        AxisStepStyle.dashed(
            strokeWidth = 1.dp,
            strokeColor = Color.White.copy(alpha = 0.1f),
            dashLength = 4.dp,
            gapLength = 4.dp,
            phase = 0.dp
        )
    }

    val ySteps = remember(effectiveYMin, effectiveRangeY) {
        (0..8).map { i ->
            val realValue = effectiveYMin + i.toFloat() * effectiveRangeY / 8.2f
            AxisStep(
                axisValue = realValue,
                axisLabel = if (i == 0) "" else yAxisLabel(realValue),
                labelStyle = labelStyle,
                stepStyle = gridStyle
            )
        }.toPersistentList()
    }

    val xSteps = remember(allXValues) {
        if (allXValues.isNotEmpty()) {
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
            steps.toPersistentList()
        } else {
            persistentListOf()
        }
    }

    val yAxisData = remember(ySteps) { AxisData(axisSteps = ySteps) }
    val xAxisData = remember(xSteps) { AxisData(axisSteps = xSteps) }

    val xStepSize = dimensionResource(id = if (isCalculator) R.dimen.chart_calculator_x_step else R.dimen.chart_column_x_step)
    val axisPadding = AxisPadding(
        start = dimensionResource(id = if (isCalculator) R.dimen.chart_calculator_axis_padding_start else R.dimen.chart_axis_padding_start),
        end = 20.dp,
        top = 0.dp,
        bottom = dimensionResource(id = R.dimen.chart_axis_padding_bottom)
    )

    var initialScrollDone by remember { mutableStateOf(false) }

    val chartDescription = stringResource(id = R.string.chart_content_description_column)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isCalculator || isFullScreen) Modifier.fillMaxHeight() else Modifier.height(dimensionResource(id = R.dimen.chart_height)))
            .semantics { contentDescription = chartDescription }
    ) {
        // Panel de información de selección
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.chart_selection_panel_height)),
            contentAlignment = Alignment.Center
        ) {
            val currentSegment = selectedSegment
            androidx.compose.animation.AnimatedVisibility(
                visible = currentSegment != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (currentSegment != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        val value = if (currentSegment.minValue < 0.0) {
                            currentSegment.minValue - currentSegment.maxValue
                        } else {
                            currentSegment.maxValue - currentSegment.minValue
                        }

                        Text(
                            text = "${currentSegment.label}: ${yAxisLabel(value.toFloat())}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            // Descartar selección
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { selectedSegment = null }
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val dynamicYStepSize = (maxHeight - axisPadding.bottom - axisPadding.top) / effectiveRangeY

                ChartScaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    xAxisData = xAxisData,
                    yAxisData = yAxisData,
                    xUnitSize = xStepSize,
                    yUnitSize = dynamicYStepSize,
                    axisPadding = axisPadding,
                    isPinchZoomEnabled = true,
                    horizontalAxis = { scroll, zoom, padding ->
                        HorizontalAxisChart(
                            data = xAxisData,
                            labelHeight = dimensionResource(id = if (isCalculator) R.dimen.chart_calculator_label_height else R.dimen.chart_axis_label_height),
                            horizontalScroll = scroll,
                            zoom = zoom,
                            fixedUnitSize = xStepSize,
                            padding = padding,
                            labelVerticalAlignment = AxisLabelVerticalAlignment.Top,
                            labelVerticalGap = 2.dp,
                            labelRotation = xAxisLabelRotation,
                            labelCenterAlignment = AxisLabelCenterAlignment.Center,
                            labelsBackgroundColor = Color.Transparent
                        )
                    },
                    verticalAxis = { scroll, zoom, padding ->
                        VerticalAxisChart(
                            data = yAxisData,
                            labelWidth = dimensionResource(id = if (isCalculator) R.dimen.chart_calculator_label_width else R.dimen.chart_axis_label_width),
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
                        LaunchedEffect(scaffoldData.onHorizontalScrollChangeRequest) {
                            if (!initialScrollDone) {
                                val scrollTarget = if (isCalculator) 0f else Float.MAX_VALUE
                                scaffoldData.onHorizontalScrollChangeRequest?.invoke(scrollTarget)
                                initialScrollDone = true
                            }
                        }

                        BarChart(
                            data = barDataList,
                            xAxisData = xAxisData,
                            yAxisData = yAxisData,
                            xAxisStepSize = xStepSize,
                            yAxisStepSize = dynamicYStepSize,
                            zoom = scaffoldData.zoom,
                            horizontalScroll = scaffoldData.horizontalScroll,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)
                                .clipToBounds(),
                            onBarClick = { clickedSegment ->
                                selectedSegment = if (selectedSegment == clickedSegment) null else clickedSegment
                            }
                        )

                        if (trendLines.isNotEmpty()) {
                            LineChart(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds(),
                                lines = trendLines,
                                xAxisData = xAxisData,
                                yAxisData = yAxisData,
                                xAxisStepSize = xStepSize,
                                yAxisStepSize = dynamicYStepSize,
                                horizontalScroll = scaffoldData.horizontalScroll,
                                zoom = scaffoldData.zoom,
                                backgroundColor = Color.Transparent
                            )
                        }
                    }
                )
            }
        }
    }
}
}
