package com.example.pace_ometer.ui.common.charts

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
 * y = points[i].second (the metric value). Supports an optional second series sharing the same
 * axes (e.g. instantaneous vs. average pace) so two closely-related metrics can share one chart
 * instead of stacking separately -- deliberately simple beyond that, no zoom/pan/N-way series.
 */
@Composable
fun LineChart(
    title: String,
    points: List<Pair<Float, Float>>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    secondarySeries: List<Pair<Float, Float>>? = null,
    secondarySeriesLabel: String? = null,
    secondaryLineColor: Color = MaterialTheme.colorScheme.secondary,
    primarySeriesLabel: String? = null,
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
        if (secondarySeriesLabel != null) {
            Legend(primarySeriesLabel ?: title, lineColor, secondarySeriesLabel, secondaryLineColor)
        }

        val allPoints = if (secondarySeries != null) points + secondarySeries else points
        val minY = allPoints.minOf { it.second }
        val maxY = allPoints.maxOf { it.second }
        val minX = allPoints.minOf { it.first }
        val maxX = allPoints.maxOf { it.first }
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

            fun pathFor(series: List<Pair<Float, Float>>): androidx.compose.ui.graphics.Path {
                val path = androidx.compose.ui.graphics.Path()
                series.forEachIndexed { index, (x, y) ->
                    val plotX = toPlotX(x)
                    val plotY = toPlotY(y)
                    if (index == 0) path.moveTo(plotX, plotY) else path.lineTo(plotX, plotY)
                }
                return path
            }

            drawPath(path = pathFor(points), color = lineColor, style = Stroke(width = 4f))
            secondarySeries?.takeIf { it.size >= 2 }?.let {
                drawPath(path = pathFor(it), color = secondaryLineColor, style = Stroke(width = 4f))
            }
        }
    }
}

@Composable
private fun Legend(primaryLabel: String, primaryColor: Color, secondaryLabel: String, secondaryColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(primaryLabel, primaryColor)
        LegendDot(secondaryLabel, secondaryColor)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
