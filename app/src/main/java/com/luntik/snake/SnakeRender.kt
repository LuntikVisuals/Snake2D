package com.luntik.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal enum class RenderDir { UP, DOWN, LEFT, RIGHT }

internal data class RenderCell(val x: Int, val y: Int)

@Composable
internal fun SmoothSnakeBoard(
    cols: Int,
    rows: Int,
    snake: List<RenderCell>,
    food: RenderCell,
    dir: RenderDir,
    headColor: Color,
    bodyColor: Color,
    foodColor: Color = Color(0xFFFF5252)
) {
    Canvas(Modifier.fillMaxSize()) {
        val cw = size.width / cols
        val ch = size.height / rows
        val cell = minOf(cw, ch)

        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFF152028), Color(0xFF0A1014)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.75f
            )
        )

        for (x in 0 until cols) {
            for (y in 0 until rows) {
                drawCircle(
                    Color.White.copy(alpha = 0.035f),
                    radius = 1.3f,
                    center = Offset(x * cw + cw / 2, y * ch + ch / 2)
                )
            }
        }

        val fx = food.x * cw + cw / 2
        val fy = food.y * ch + ch / 2
        drawCircle(foodColor.copy(alpha = 0.18f), cell * 0.58f, Offset(fx, fy))
        drawCircle(foodColor.copy(alpha = 0.4f), cell * 0.4f, Offset(fx, fy))
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFFF8A80), foodColor, Color(0xFFB71C1C)),
                center = Offset(fx - cell * 0.1f, fy - cell * 0.1f),
                radius = cell * 0.35f
            ),
            radius = cell * 0.3f,
            center = Offset(fx, fy)
        )
        drawCircle(Color.White.copy(alpha = 0.55f), cell * 0.08f, Offset(fx - cell * 0.09f, fy - cell * 0.11f))

        for (i in snake.lastIndex downTo 0) {
            val c = snake[i]
            val cx = c.x * cw + cw / 2
            val cy = c.y * ch + ch / 2
            val t = i.toFloat() / snake.size.coerceAtLeast(1)
            val r = if (i == 0) cell * 0.44f else cell * (0.38f - t * 0.07f).coerceAtLeast(0.2f)
            val col = if (i == 0) headColor else bodyColor.copy(alpha = (0.95f - t * 0.35f).coerceAtLeast(0.4f))

            drawCircle(Color.Black.copy(alpha = 0.28f), r * 1.08f, Offset(cx + 1.8f, cy + 2.2f))
            drawCircle(col, r, Offset(cx, cy))
            drawCircle(
                Color.White.copy(alpha = if (i == 0) 0.25f else 0.1f),
                r * 0.32f,
                Offset(cx - r * 0.28f, cy - r * 0.3f)
            )

            if (i == 0) {
                val eyeR = cell * 0.075f
                val ox = when (dir) {
                    RenderDir.LEFT -> -cell * 0.14f
                    RenderDir.RIGHT -> cell * 0.14f
                    else -> 0f
                }
                val oy = when (dir) {
                    RenderDir.UP -> -cell * 0.14f
                    RenderDir.DOWN -> cell * 0.14f
                    else -> 0f
                }
                val side = cell * 0.12f
                val px = when (dir) {
                    RenderDir.UP, RenderDir.DOWN -> side
                    else -> 0f
                }
                val py = when (dir) {
                    RenderDir.LEFT, RenderDir.RIGHT -> side
                    else -> 0f
                }
                val ex = cx + ox
                val ey = cy + oy
                drawCircle(Color.White, eyeR * 1.35f, Offset(ex - px, ey - py))
                drawCircle(Color.White, eyeR * 1.35f, Offset(ex + px, ey + py))
                drawCircle(Color(0xFF0A1410), eyeR * 0.7f, Offset(ex - px + ox * 0.2f, ey - py + oy * 0.2f))
                drawCircle(Color(0xFF0A1410), eyeR * 0.7f, Offset(ex + px + ox * 0.2f, ey + py + oy * 0.2f))
            }
        }
    }
}
