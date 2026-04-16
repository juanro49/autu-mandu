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
    isFullScreen: Boolean = false
) {
    if (rawData.isEmpty() || rawData.all { it.dataPoints.isEmpty() }) return

    DisableSaveableState {
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        rawData.forEach { series ->
            series.dataPoints.forEach { point ->
                if (point.x.isFinite() && point.y.isFinite()) {
                    if (point.x < minX) minX = point.x
                    if (point.x > maxX) maxX = point.x
                    if (point.y < minY) minY = point.y
                    if (point.y > maxY) maxY = point.y
                }
            }
        }

        if (minX == Float.MAX_VALUE) {
            minX = 0f; maxX = 1f; minY = 0f; maxY = 1f
        }
        if (maxX == minX) maxX = minX + 1f

        // Rango de X ajustado para que los puntos de los extremos sean visibles
        val rangeX = maxX - minX
        val safeRangeX = if (rangeX > 0f) rangeX else 1f
        val effectiveXMin = minX - safeRangeX * 0.02f
        val effectiveXMax = maxX + safeRangeX * 0.005f

        // Rango de Y optimizado: sin margen arriba para pegar el gráfico al título
        val yMinBound = minY
        val yMaxBound = maxY
        val rangeY = yMaxBound - yMinBound
        val safeRangeY = if (rangeY > 0f) rangeY else 1f

        // 10% de margen abajo para no chocar con el eje X, 15% arriba para el popup
        val effectiveYMin = yMinBound - safeRangeY * 0.10f
        val effectiveYMax = yMaxBound + safeRangeY * 0.15f
        val effectiveRangeY = effectiveYMax - effectiveYMin

        // Usamos los valores reales para Y y calculamos yUnitSize para que se ajuste al alto
        val lines: ImmutableList<Line> = rawData.mapIndexed { index, data ->
            val lineColor = Color(data.color)
            val isTrend = data is TrendReportChartData || data is OverallTrendReportChartData
            Line(
                dataPoints = data.dataPoints.filter { it.x.isFinite() && it.y.isFinite() }.map { dp ->
                    val isMarked = data is AbstractReportChartLineData && data.isMarked(dp.x)
                    // Añadimos un pequeño offset para evitar puntos superpuestos en la misma X (que crashean LineChart)
                    val xOffset = index * 0.0001f
                    Point(
                        dp.x + xOffset,
                        dp.y,
                        intersectionNode = if (isTrend) null else IntersectionPoint(
                            radius = dimensionResource(id = R.dimen.chart_line_point_radius),
                            color = if (isMarked) Color.White else lineColor
                        )
                    )
                },
                lineStyle = if (isTrend) {
                    LineStyle(color = lineColor.copy(alpha = 0.7f), width = 3.5f)
                } else {
                    LineStyle(color = lineColor, width = 5.5f)
                },
                selectionHighlightPoint = SelectionHighlightPoint(color = Color.White),
                selectionHighlightPopUp = SelectionHighlightPopUp(
                    backgroundColor = Color.Black.copy(alpha = 0.5f),
                    labelColor = Color.White,
                    labelSize = 10.sp,
                    paddingBetweenPopUpAndPoint = dimensionResource(id = R.dimen.chart_line_popup_padding),
                    popUpLabel = { _, y -> yAxisLabel(y) }
                )
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

        // 8 pasos para una granularidad mayor y cubrir todo el alto
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

        // etiquetas del eje X: una por cada mes
        val xSteps = remember(rangeX, minX, maxX, effectiveXMin, effectiveXMax) {
            if (rangeX > 0) {
                val steps = mutableListOf<AxisStep>()
                // Paso base invisible para el alineamiento
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

                while (startCal.before(endCal) || startCal.equals(endCal)) {
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

                // Si no hay pasos (rango menor a un mes), ponemos el inicio y el fin
                if (steps.size <= 2) {
                    steps.add(AxisStep(minX, xAxisLabel(minX), labelStyle, stepStyle = gridStyle))
                    steps.add(AxisStep(maxX, xAxisLabel(maxX), labelStyle, stepStyle = gridStyle))
                }
                // Aseguramos que el eje X se extienda hasta effectiveXMax para dar espacio lateral a los popups
                steps.add(AxisStep(effectiveXMax, "", labelStyle, stepStyle = null))
                steps.toPersistentList()
            } else {
                persistentListOf(
                    AxisStep(
                        axisValue = minX,
                        axisLabel = xAxisLabel(minX),
                        labelStyle = labelStyle,
                        stepStyle = gridStyle
                    )
                )
            }
        }

        val yAxisData = remember(ySteps) { AxisData(axisSteps = ySteps) }
        val xAxisData = remember(xSteps) { AxisData(axisSteps = xSteps) }

        // Cálculo dinámico de la escala para mostrar aproximadamente los últimos meses/puntos
        // similar a applyViewport de la versión anterior.
        val baseStepSize = dimensionResource(id = R.dimen.chart_line_x_step_base)
        val xStepSize = remember(rangeX, isFullScreen, baseStepSize) {
            if (rangeX > 0 && !isFullScreen) {
                baseStepSize * 1.8f
            } else baseStepSize
        }
        // Altura total 220dp - 15dp (bottom padding) = 205dp para aprovechar el espacio superior
        val chartHeight = dimensionResource(id = R.dimen.chart_canvas_height)
        val yStepSize = chartHeight / effectiveRangeY
        val axisPadding = AxisPadding(
            start = dimensionResource(id = R.dimen.chart_axis_padding_start),
            end = 0.dp,
            top = 0.dp,
            bottom = dimensionResource(id = R.dimen.chart_axis_padding_bottom)
        )

        var initialScrollDone by remember { mutableStateOf(false) }
        val chartDescription = stringResource(id = R.string.chart_content_description_line)

        Box(
            modifier = if (isFullScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ChartScaffold(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(id = R.dimen.chart_height))
                    .background(Color.Transparent)
                    .semantics { contentDescription = chartDescription },
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                xUnitSize = xStepSize,
                yUnitSize = yStepSize,
                axisPadding = axisPadding,
                isPinchZoomEnabled = true,
                horizontalAxis = { scroll: Dp, zoom: Float, padding: AxisPadding ->
                    HorizontalAxisChart(
                        data = xAxisData,
                        labelHeight = 25.dp,
                        horizontalScroll = scroll,
                        zoom = zoom,
                        fixedUnitSize = xStepSize,
                        padding = padding,
                        labelVerticalAlignment = AxisLabelVerticalAlignment.Top,
                        labelVerticalGap = dimensionResource(id = R.dimen.chart_label_padding),
                        labelsBackgroundColor = Color.Transparent
                    )
                },
                verticalAxis = { scroll: Dp, zoom: Float, padding: AxisPadding ->
                    VerticalAxisChart(
                        data = yAxisData,
                        labelWidth = 25.dp,
                        verticalScroll = scroll,
                        zoom = zoom,
                        fixedUnitSize = yStepSize,
                        padding = padding,
                        labelHorizontalAlignment = AxisLabelHorizontalAlignment.End,
                        labelHorizontalGap = 1.dp,
                        labelsBackgroundColor = Color.Transparent
                    )
                },
                content = { scaffoldData: ChartScaffoldContentData ->
                    // Scroll inicial al final
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
                        xAxisData = xAxisData,
                        yAxisData = yAxisData,
                        xAxisStepSize = xStepSize,
                        yAxisStepSize = yStepSize,
                        horizontalScroll = scaffoldData.horizontalScroll,
                        zoom = scaffoldData.zoom,
                        backgroundColor = Color.Transparent,
                        onPointSelect = { _: Point, _: Offset -> /* Kubit maneja internamente el popup si está configurado en la Line */ }
                    )
                }
            )
        }
    }
}
