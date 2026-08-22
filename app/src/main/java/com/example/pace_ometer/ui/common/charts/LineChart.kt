package com.example.pace_ometer.ui.common.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A minimal Canvas-based line chart: x = points[i].first (e.g. elapsed distance or time),
 * y = points[i].second (the metric value). Deliberately simple -- no zoom/pan/multi-series --
 * since that's all the analysis page's pace/HR/elevation/cadence-over-time views need.
 */
@Composable
fun LineChart(
    title: String,
    points: List<Pair<Float, Float>>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    valueFormatter: (Float) -> String = { "%.1f".format(it) },
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (points.size < 2) {
            Text("Not enough data", style = MaterialTheme.typography.bodySmall)
            return@Column
        }

        val minY = points.minOf { it.second }
        val maxY = points.maxOf { it.second }
        val minX = points.minOf { it.first }
        val maxX = points.maxOf { it.first }
        val yRange = (maxY - minY).takeIf { it > 0f } ?: 1f
        val xRange = (maxX - minX).takeIf { it > 0f } ?: 1f

        Text(
            "${valueFormatter(maxY)}  (max)",
            style = MaterialTheme.typography.labelSmall
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val path = androidx.compose.ui.graphics.Path()
            points.forEachIndexed { index, (x, y) ->
                val plotX = ((x - minX) / xRange) * size.width
                val plotY = size.height - ((y - minY) / yRange) * size.height
                if (index == 0) path.moveTo(plotX, plotY) else path.lineTo(plotX, plotY)
            }
            drawPath(path = path, color = lineColor, style = Stroke(width = 4f))
            drawLine(
                color = lineColor.copy(alpha = 0.2f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height)
            )
        }
        Text(
            "${valueFormatter(minY)}  (min)",
            style = MaterialTheme.typography.labelSmall
        )
    }
}
