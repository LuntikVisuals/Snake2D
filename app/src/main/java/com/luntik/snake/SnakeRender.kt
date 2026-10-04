package com.luntik.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

internal enum class RenderDir { UP, DOWN, LEFT, RIGHT }
internal data class RenderCell(val x: Int, val y: Int)

@Composable
internal fun SmoothSnakeBoard(
    cols: Int, rows: Int, snake: List<RenderCell>, food: RenderCell, dir: RenderDir,
    headColor: Color, bodyColor: Color, foodColor: Color = Color(0xFFE53935),
    progress: Float = 1f, obstacles: Set<RenderCell> = emptySet(),
    extraSnakes: List<Pair<List<RenderCell>, Color>> = emptyList(), showGrid: Boolean = false
) {
    Canvas(Modifier.fillMaxSize()) {
        val cw = size.width / cols; val ch = size.height / rows; val cell = minOf(cw, ch)
        drawRect(Brush.radialGradient(listOf(Color(0xFF152028), Color(0xFF0A1014)), Offset(size.width / 2f, size.height / 2f), size.maxDimension * 0.75f))
        if (showGrid) {
            val g = Color.White.copy(alpha = 0.07f)
            for (x in 0..cols) drawLine(g, Offset(x * cw, 0f), Offset(x * cw, size.height), 1f)
            for (y in 0..rows) drawLine(g, Offset(0f, y * ch), Offset(size.width, y * ch), 1f)
        } else {
            for (x in 0 until cols) for (y in 0 until rows)
                drawCircle(Color.White.copy(alpha = 0.03f), 1.2f, Offset(x * cw + cw / 2f, y * ch + ch / 2f))
        }
        for (o in obstacles) {
            drawRoundRect(Color(0xFF3A4555), Offset(o.x * cw + cw * 0.15f, o.y * ch + ch * 0.15f), Size(cw * 0.7f, ch * 0.7f),
                androidx.compose.ui.geometry.CornerRadius(cell * 0.12f, cell * 0.12f))
        }
        val fx = food.x * cw + cw / 2f; val fy = food.y * ch + ch / 2f; val ar = cell * 0.32f
        drawCircle(foodColor.copy(alpha = 0.22f), ar * 1.55f, Offset(fx, fy))
        drawCircle(Brush.radialGradient(listOf(foodColor.copy(alpha = 0.95f), foodColor, foodColor.copy(alpha = 0.7f)), Offset(fx - ar * 0.25f, fy - ar * 0.3f), ar * 1.2f), ar, Offset(fx, fy + ar * 0.05f))
        drawCircle(Color.White.copy(alpha = 0.45f), ar * 0.22f, Offset(fx - ar * 0.28f, fy - ar * 0.22f))
        drawLine(Color(0xFF5D4037), Offset(fx, fy - ar * 0.85f), Offset(fx + ar * 0.12f, fy - ar * 1.25f), cell * 0.06f, cap = StrokeCap.Round)
        val leaf = Path().apply {
            moveTo(fx + ar * 0.05f, fy - ar * 0.95f)
            quadraticBezierTo(fx + ar * 0.55f, fy - ar * 1.35f, fx + ar * 0.35f, fy - ar * 0.7f)
            quadraticBezierTo(fx + ar * 0.15f, fy - ar * 0.85f, fx + ar * 0.05f, fy - ar * 0.95f)
        }
        drawPath(leaf, Color(0xFF66BB6A))
        for ((seg, col) in extraSnakes) {
            if (seg.isEmpty()) continue
            val pts = seg.map { Offset(it.x * cw + cw / 2f, it.y * ch + ch / 2f) }
            val ep = Path(); ep.moveTo(pts.last().x, pts.last().y)
            for (i in pts.lastIndex downTo 1) {
                val a = pts[i]; val b = pts[i - 1]
                ep.quadraticBezierTo(a.x, a.y, (a.x + b.x) / 2f, (a.y + b.y) / 2f)
            }
            ep.lineTo(pts[0].x, pts[0].y)
            val ew = cell * 0.72f
            drawPath(ep, Color.Black.copy(alpha = 0.3f), style = Stroke(width = ew * 1.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(ep, col.copy(alpha = 0.9f), style = Stroke(width = ew, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(col, cell * 0.38f, pts[0])
        }
        if (snake.isEmpty()) return@Canvas
        fun centerOf(c: RenderCell) = Offset(c.x * cw + cw / 2f, c.y * ch + ch / 2f)
        val points = ArrayList<Offset>(snake.size)
        val headBase = centerOf(snake.first()); val p = progress.coerceIn(0f, 1f)
        if (snake.size == 1) points.add(headBase) else {
            val neck = centerOf(snake[1])
            points.add(Offset(neck.x + (headBase.x - neck.x) * (0.55f + 0.45f * p), neck.y + (headBase.y - neck.y) * (0.55f + 0.45f * p)))
            for (i in 1 until snake.size) points.add(centerOf(snake[i]))
        }
        val bodyPath = Path()
        if (points.size == 1) { bodyPath.moveTo(points[0].x, points[0].y); bodyPath.lineTo(points[0].x + 0.1f, points[0].y) }
        else {
            bodyPath.moveTo(points.last().x, points.last().y)
            for (i in points.lastIndex downTo 1) {
                val a = points[i]; val b = points[i - 1]
                bodyPath.quadraticBezierTo(a.x, a.y, (a.x + b.x) / 2f, (a.y + b.y) / 2f)
            }
            bodyPath.lineTo(points[0].x, points[0].y)
        }
        val maxStroke = cell * 0.72f; val minStroke = cell * 0.28f
        drawPath(bodyPath, Color.Black.copy(alpha = 0.35f), style = Stroke(width = maxStroke * 1.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(bodyPath, brush = Brush.linearGradient(listOf(bodyColor.copy(alpha = 0.85f), bodyColor, headColor.copy(alpha = 0.9f))), style = Stroke(width = maxStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.forEachIndexed { i, pt ->
            if (i == 0) return@forEachIndexed
            drawCircle(headColor.copy(alpha = 0.35f), cell * 0.08f, Offset(pt.x, pt.y - cell * 0.12f))
            drawCircle(Color.Black.copy(alpha = 0.25f), cell * 0.05f, Offset(pt.x + cell * 0.1f, pt.y + cell * 0.05f))
        }
        val hp = points[0]
        val angleDeg = when (dir) { RenderDir.UP -> -90f; RenderDir.DOWN -> 90f; RenderDir.LEFT -> 180f; RenderDir.RIGHT -> 0f }
        val headR = cell * 0.42f
        rotate(angleDeg, hp) {
            drawOval(brush = Brush.radialGradient(listOf(headColor, bodyColor.copy(alpha = 0.95f)), Offset(hp.x - headR * 0.15f, hp.y - headR * 0.15f), headR * 1.2f),
                topLeft = Offset(hp.x - headR * 1.05f, hp.y - headR * 0.85f), size = Size(headR * 2.15f, headR * 1.7f))
            val eyeY = hp.y - headR * 0.12f; val eyeX = headR * 0.28f; val eyeR = cell * 0.11f
            drawCircle(Color.White, eyeR, Offset(hp.x + headR * 0.35f, eyeY - eyeX * 0.5f))
            drawCircle(Color.White, eyeR, Offset(hp.x + headR * 0.35f, eyeY + eyeX * 0.5f))
            drawCircle(Color(0xFF0A1410), eyeR * 0.55f, Offset(hp.x + headR * 0.42f, eyeY - eyeX * 0.5f))
            drawCircle(Color(0xFF0A1410), eyeR * 0.55f, Offset(hp.x + headR * 0.42f, eyeY + eyeX * 0.5f))
            drawLine(Color(0xFF1A1A1A), Offset(hp.x + headR * 0.15f, eyeY - eyeX * 0.85f), Offset(hp.x + headR * 0.55f, eyeY - eyeX * 0.35f), cell * 0.05f, cap = StrokeCap.Round)
            drawLine(Color(0xFF1A1A1A), Offset(hp.x + headR * 0.15f, eyeY + eyeX * 0.85f), Offset(hp.x + headR * 0.55f, eyeY + eyeX * 0.35f), cell * 0.05f, cap = StrokeCap.Round)
        }
        if (points.size >= 2) drawCircle(bodyColor.copy(alpha = 0.7f), minStroke * 0.55f, points.last())
    }
}

@Composable
internal fun SkinPreview(headColor: Color, bodyColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cell = size.minDimension / 5.5f
        val cy = size.height / 2f
        val pts = (0..5).map { i -> Offset(size.width * 0.12f + i * cell * 0.75f, cy) }
        val bodyPath = Path()
        bodyPath.moveTo(pts.first().x, pts.first().y)
        for (i in 1 until pts.size) {
            val a = pts[i - 1]; val b = pts[i]
            bodyPath.quadraticBezierTo(a.x, a.y, (a.x + b.x) / 2f, (a.y + b.y) / 2f)
        }
        bodyPath.lineTo(pts.last().x, pts.last().y)
        val stroke = cell * 0.7f
        drawPath(bodyPath, Color.Black.copy(alpha = 0.3f), style = Stroke(width = stroke * 1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(bodyPath, brush = Brush.linearGradient(listOf(bodyColor, bodyColor, headColor)), style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(bodyPath, Color.White.copy(alpha = 0.15f), style = Stroke(width = stroke * 0.35f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val hp = pts.last(); val headR = cell * 0.42f
        drawCircle(brush = Brush.radialGradient(listOf(headColor, bodyColor), Offset(hp.x - headR * 0.15f, hp.y - headR * 0.15f), headR * 1.2f), radius = headR, center = hp)
        drawCircle(Color.White, cell * 0.1f, Offset(hp.x + cell * 0.12f, hp.y - cell * 0.12f))
        drawCircle(Color.White, cell * 0.1f, Offset(hp.x + cell * 0.12f, hp.y + cell * 0.12f))
        drawCircle(Color(0xFF0A1410), cell * 0.055f, Offset(hp.x + cell * 0.16f, hp.y - cell * 0.12f))
        drawCircle(Color(0xFF0A1410), cell * 0.055f, Offset(hp.x + cell * 0.16f, hp.y + cell * 0.12f))
        val ax = size.width * 0.9f
        drawCircle(Color(0xFFE53935), cell * 0.32f, Offset(ax, cy))
        drawCircle(Color.White.copy(alpha = 0.4f), cell * 0.09f, Offset(ax - cell * 0.1f, cy - cell * 0.1f))
    }
}
