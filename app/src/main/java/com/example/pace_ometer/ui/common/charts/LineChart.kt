package com.example.pace_ometer.ui.common.charts

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val Y_GRID_LINES = 4
private const val X_GRID_LINES = 4

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
    xValueFormatter: (Float) -> String = { "%.1f".format(it) },
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

        val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
        val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        val labelSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { 11.sp.toPx() }
        val axisPaint = remember(labelColor, labelSizePx) {
            Paint().apply {
                color = labelColor.toArgb()
                textSize = labelSizePx
                isAntiAlias = true
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val yLabelWidth = axisPaint.measureText(valueFormatter(maxY))
                .coerceAtLeast(axisPaint.measureText(valueFormatter(minY)))
            val leftMargin = yLabelWidth + 8.dp.toPx()
            val bottomMargin = axisPaint.textSize + 8.dp.toPx()
            val plotWidth = (size.width - leftMargin).coerceAtLeast(0f)
            val plotHeight = (size.height - bottomMargin).coerceAtLeast(0f)

            fun toPlotX(x: Float) = leftMargin + ((x - minX) / xRange) * plotWidth
            fun toPlotY(y: Float) = plotHeight - ((y - minY) / yRange) * plotHeight

            // Horizontal gridlines + y-axis value labels.
            for (i in 0..Y_GRID_LINES) {
                val value = minY + (yRange * i / Y_GRID_LINES)
                val y = toPlotY(value)
                drawLine(
                    color = gridColor,
                    start = Offset(leftMargin, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                drawContext.canvas.nativeCanvas.drawText(
                    valueFormatter(value),
                    0f,
                    y + axisPaint.textSize / 3f,
                    axisPaint
                )
            }

            // Vertical gridlines + x-axis value labels.
            for (i in 0..X_GRID_LINES) {
                val value = minX + (xRange * i / X_GRID_LINES)
                val x = toPlotX(value)
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, plotHeight),
                    strokeWidth = 1.dp.toPx()
                )
                val label = xValueFormatter(value)
                val labelWidth = axisPaint.measureText(label)
                val labelX = (x - labelWidth / 2f).coerceIn(leftMargin, size.width - labelWidth)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    labelX,
                    size.height,
                    axisPaint
                )
            }

            // Axis border (left + bottom of the plot area).
            drawLine(
                color = gridColor,
                start = Offset(leftMargin, 0f),
                end = Offset(leftMargin, plotHeight),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = gridColor,
                start = Offset(leftMargin, plotHeight),
                end = Offset(size.width, plotHeight),
                strokeWidth = 1.dp.toPx()
            )

            val path = androidx.compose.ui.graphics.Path()
            points.forEachIndexed { index, (x, y) ->
                val plotX = toPlotX(x)
                val plotY = toPlotY(y)
                if (index == 0) path.moveTo(plotX, plotY) else path.lineTo(plotX, plotY)
            }
            drawPath(path = path, color = lineColor, style = Stroke(width = 4f))
        }
    }
}
