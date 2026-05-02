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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
        val isDark = isSystemInDarkTheme()
        val textColor = if (isDark) Color.White else Color.Black
        val isDonut = chartOption == 1

        val groupedData = remember(rawData) {
            rawData.flatMap { it.dataPoints }
                .filter { it.y.isFinite() && it.y > 0 }
                .groupBy { it.tooltip ?: "" }
                .mapValues { entry -> entry.value.sumOf { it.y.toDouble() }.toFloat() }
        }

        val total = groupedData.values.sum()
        val categoryColors = getCategoryColors()
        val sections = createPieSections(groupedData, total, categoryColors)

        val chartDescription = stringResource(
            id = if (isDonut) R.string.chart_content_description_donut else R.string.chart_content_description_pie
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.chart_height))
                .padding(vertical = dimensionResource(id = R.dimen.report_card_padding) / 2)
                .semantics { contentDescription = chartDescription },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PieChartArea(isDonut, sections, total, textColor, yAxisLabel)
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.chart_label_padding)))
            PieLegend(groupedData, total, categoryColors, textColor)
        }
    }
}

@Composable
private fun getCategoryColors() = listOf(
    colorResource(id = R.color.blue),
    colorResource(id = R.color.green),
    colorResource(id = R.color.amber),
    colorResource(id = R.color.deep_orange),
    colorResource(id = R.color.purple),
    colorResource(id = R.color.blue_grey)
)

private fun createPieSections(
    groupedData: Map<String, Float>,
    total: Float,
    colors: List<Color>
): List<PieSectionData> {
    return groupedData.entries.mapIndexed { index, entry ->
        val value = entry.value
        val percentage = if (total > 0) (value / total * 100) else 0f
        val sectorColor = colors[index % colors.size]

        PieSectionData(
            value = value,
            label = if (percentage > 8f) String.format(Locale.getDefault(), "%.0f%%", percentage) else "",
            style = PieSectionStyle(
                sectorColor = sectorColor,
                selectedSectorColor = sectorColor,
                labelColor = Color.White
            )
        )
    }
}

@Composable
private fun PieChartArea(
    isDonut: Boolean,
    sections: List<PieSectionData>,
    total: Float,
    textColor: Color,
    yAxisLabel: (Float) -> String
) {
    Box(contentAlignment = Alignment.Center) {
        PieChart(
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.chart_pie_size))
                .background(Color.Transparent),
            pie = Pie(sections = sections),
            sectionWidth = if (isDonut) dimensionResource(id = R.dimen.chart_donut_thickness) else Dp.Hairline
        )

        if (isDonut) {
            DonutCenterText(total, textColor, yAxisLabel)
        }
    }
}

@Composable
private fun DonutCenterText(total: Float, textColor: Color, yAxisLabel: (Float) -> String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(id = R.string.chart_label_total),
            color = textColor.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Text(
            text = yAxisLabel(total),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PieLegend(
    groupedData: Map<String, Float>,
    total: Float,
    colors: List<Color>,
    textColor: Color
) {
    val entriesList = groupedData.entries.toList()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.fab_margin)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.chart_legend_row_spacing))
    ) {
        entriesList.chunked(2).forEach { rowItems ->
            LegendRow(rowItems, entriesList, total, colors, textColor)
        }
    }
}

@Composable
private fun LegendRow(
    rowItems: List<Map.Entry<String, Float>>,
    fullList: List<Map.Entry<String, Float>>,
    total: Float,
    colors: List<Color>,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.chart_legend_col_spacing))
    ) {
        rowItems.forEach { entry ->
            val index = fullList.indexOf(entry)
            val percentage = if (total > 0) (entry.value / total * 100) else 0f
            val color = colors[index % colors.size]

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(entry.key, percentage, color, textColor)
            }
        }
        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LegendItem(label: String, percentage: Float, color: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .size(dimensionResource(id = R.dimen.chart_legend_dot_size))
            .background(color, CircleShape)
    )
    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.chart_legend_dot_spacing)))
    Text(
        text = String.format(Locale.getDefault(), "%s (%.1f%%)", label, percentage),
        color = textColor.copy(alpha = 0.9f),
        fontSize = 11.sp,
        maxLines = 1
    )
}
