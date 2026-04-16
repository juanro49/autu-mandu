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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import org.juanro.autumandu.R
import androidx.compose.ui.semantics.semantics
import com.kubit.charts.components.chart.piechart.PieChart
import com.kubit.charts.components.chart.piechart.model.Pie
import com.kubit.charts.components.chart.piechart.model.PieSectionData
import com.kubit.charts.components.chart.piechart.model.PieSectionStyle
import org.juanro.autumandu.data.report.AbstractReportChartData
import java.util.Locale

@Composable
fun KubitPieChart(
    rawData: List<AbstractReportChartData>,
    chartOption: Int = 0,
    yAxisLabel: (Float) -> String = { it.toString() }
) {
    if (rawData.isEmpty()) return

    DisableSaveableState {
        // opción de gráfico 0: Donut (por defecto), 1: Tarta
    val isDonut = chartOption == 0

    // Agrupamos todos los puntos de datos de todas las series por su tooltip (categoría)
    val groupedData = rawData.flatMap { it.dataPoints }
        .filter { it.y.isFinite() && it.y > 0 }
        .groupBy { it.tooltip ?: "" }
        .mapValues { entry -> entry.value.sumOf { it.y.toDouble() }.toFloat() }

    // Calculamos el total global para los porcentajes y el centro del Donut
    val total = groupedData.values.sum()

    val categoryColors = listOf(
        colorResource(id = R.color.blue),        // Combustible
        colorResource(id = R.color.green),       // Facturas
        colorResource(id = R.color.amber),       // Neumáticos
        colorResource(id = R.color.deep_orange), // Inversión
        colorResource(id = R.color.purple),      // Ingresos
        colorResource(id = R.color.blue_grey)    // Otros
    )

    // Creamos las secciones de la tarta sin etiquetas de texto (solo porcentaje si cabe)
    val sections = groupedData.entries.mapIndexed { index, entry ->
        val value = entry.value
        val percentage = if (total > 0) (value / total * 100) else 0f
        val sectorColor = categoryColors[index % categoryColors.size]

        PieSectionData(
            value = value,
            // Solo mostramos el porcentaje en la tarta si es mayor al 8% para evitar amontonamientos
            label = if (percentage > 8f) String.format(Locale.getDefault(), "%.0f%%", percentage) else "",
            style = PieSectionStyle(
                sectorColor = sectorColor,
                selectedSectorColor = sectorColor,
                labelColor = Color.White
            )
        )
    }

    val chartDescription = if (isDonut) {
        stringResource(id = R.string.chart_content_description_donut)
    } else {
        stringResource(id = R.string.chart_content_description_pie)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.chart_height)) // Sincronizado con report.xml
            .padding(vertical = dimensionResource(id = R.dimen.report_card_padding) / 2)
            .semantics { contentDescription = chartDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Gráfico de Tarta / Donut - Ajustamos para maximizar espacio de leyenda
            PieChart(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.chart_pie_size))
                    .background(Color.Transparent),
                pie = Pie(sections = sections),
                sectionWidth = if (isDonut) dimensionResource(id = R.dimen.chart_donut_thickness) else Dp.Hairline
            )

            if (isDonut) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(id = R.string.chart_label_total),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = yAxisLabel(total),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.chart_label_padding)))

        // Leyenda optimizada en cuadrícula
        val entriesList = groupedData.entries.toList()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.fab_margin)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.chart_legend_row_spacing))
        ) {
            entriesList.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.chart_legend_col_spacing))
                ) {
                    rowItems.forEach { entry ->
                        val index = entriesList.indexOf(entry)
                        val percentage = if (total > 0) (entry.value / total * 100) else 0f
                        val color = categoryColors[index % categoryColors.size]

                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dimensionResource(id = R.dimen.chart_legend_dot_size))
                                    .background(color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.chart_legend_dot_spacing)))
                            Text(
                                text = String.format(Locale.getDefault(), "%s (%.1f%%)", entry.key, percentage),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
}
