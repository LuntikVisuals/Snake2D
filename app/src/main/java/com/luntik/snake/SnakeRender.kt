package com.luntik.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
    extraSnakes: List<Pair<List<RenderCell>, Color>> = emptyList(), showGrid: Boolean = false,
    showHitboxes: Boolean = false, foodShape: String = "apple",
    appearance: String = "gliist", gridColor: Long = 0x22FFFFFF, fieldBg: Long = 0xFF0B1420,
    fieldPhoto: ImageBitmap? = null
) {
    Canvas(Modifier.fillMaxSize()) {
        val cw = size.width / cols; val ch = size.height / rows; val cell = minOf(cw, ch)
        if (fieldPhoto != null) drawImage(fieldPhoto, dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())) else drawRect(Color(fieldBg))
        if (showGrid) {
            val g = Color(gridColor)
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
        when (foodShape) {
            "berry" -> {
                drawCircle(foodColor, ar * 0.55f, Offset(fx - ar * 0.35f, fy))
                drawCircle(foodColor, ar * 0.55f, Offset(fx + ar * 0.28f, fy + ar * 0.1f))
                drawCircle(foodColor, ar * 0.5f, Offset(fx, fy - ar * 0.35f))
            }
            "mushroom" -> {
                drawCircle(foodColor, ar * 0.7f, Offset(fx, fy - ar * 0.15f))
                drawRect(Color(0xFFFFE0B2), Offset(fx - ar * 0.22f, fy), Size(ar * 0.44f, ar * 0.7f))
            }
            "grape" -> {
                repeat(5) { i -> drawCircle(foodColor, ar * 0.32f, Offset(fx + ((i % 3) - 1) * ar * 0.35f, fy + (i / 3) * ar * 0.35f)) }
            }
            "juice" -> {
                drawRoundRect(foodColor, Offset(fx - ar * 0.35f, fy - ar * 0.55f), Size(ar * 0.7f, ar * 1.2f), androidx.compose.ui.geometry.CornerRadius(ar * 0.15f, ar * 0.15f))
                drawRect(Color.White.copy(alpha = 0.35f), Offset(fx - ar * 0.2f, fy - ar * 0.2f), Size(ar * 0.15f, ar * 0.45f))
            }
            "citrus" -> {
                drawCircle(foodColor, ar, Offset(fx, fy))
                drawLine(Color.White.copy(alpha = 0.5f), Offset(fx - ar * 0.4f, fy), Offset(fx + ar * 0.4f, fy), 2f)
                drawLine(Color.White.copy(alpha = 0.5f), Offset(fx, fy - ar * 0.4f), Offset(fx, fy + ar * 0.4f), 2f)
            }
            "burger" -> {
                drawOval(Color(0xFFFFE082), Offset(fx - ar, fy - ar * 0.35f), Size(ar * 2f, ar * 0.45f))
                drawOval(Color(0xFF6D4C41), Offset(fx - ar * 0.8f, fy - ar * 0.05f), Size(ar * 1.6f, ar * 0.35f))
                drawOval(Color(0xFFFFE082), Offset(fx - ar, fy + ar * 0.15f), Size(ar * 2f, ar * 0.4f))
            }
            else -> drawCircle(Brush.radialGradient(listOf(foodColor.copy(alpha = 0.95f), foodColor, foodColor.copy(alpha = 0.7f)), Offset(fx - ar * 0.25f, fy - ar * 0.3f), ar * 1.2f), ar, Offset(fx, fy + ar * 0.05f))
        }
        if (showHitboxes) drawCircle(Color.Cyan.copy(alpha = 0.45f), ar, Offset(fx, fy), style = Stroke(width = 2f))
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
        if (appearance == "retro") {
            snake.forEachIndexed { i, c ->
                drawRect(if (i == 0) headColor else bodyColor, Offset(c.x * cw + cw * 0.12f, c.y * ch + ch * 0.12f), Size(cw * 0.76f, ch * 0.76f))
            }
        } else if (appearance == "retro2") {
            points.forEachIndexed { i, pt -> drawCircle(if (i == 0) headColor else bodyColor, cell * 0.38f, pt) }
        } else {
        val bodyPath = Path()
        if (points.size == 1) { bodyPath.moveTo(points[0].x, points[0].y); bodyPath.lineTo(points[0].x + 0.1f, points[0].y) }
        else {
            bodyPath.moveTo(points.last().x, points.last().y)
            for (i in points.lastIndex downTo 1) {
                val a = points[i]; val b = points[i - 1]
                val far = kotlin.math.abs(a.x - b.x) > cell * 1.6f || kotlin.math.abs(a.y - b.y) > cell * 1.6f
                if (far) { bodyPath.moveTo(b.x, b.y); continue }
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
        if (showHitboxes) points.forEach { drawCircle(Color.Yellow.copy(alpha = 0.35f), cell * 0.36f, it, style = Stroke(width = 2f)) }
        }
        val face = points.first()
        when (appearance) {
            "spongebob" -> {
                drawRoundRect(Color(0xFFFFEB3B), Offset(face.x - cell * 0.42f, face.y - cell * 0.42f), Size(cell * 0.84f, cell * 0.84f), androidx.compose.ui.geometry.CornerRadius(8f, 8f))
                listOf(-0.18f to -0.16f, 0.12f to -0.2f, -0.05f to 0.12f, 0.2f to 0.08f).forEach { (ox, oy) ->
                    drawCircle(Color(0xFFE6C200), cell * 0.07f, Offset(face.x + cell * ox, face.y + cell * oy))
                }
                drawCircle(Color.White, cell * 0.1f, Offset(face.x - cell * 0.12f, face.y - cell * 0.08f))
                drawCircle(Color.White, cell * 0.1f, Offset(face.x + cell * 0.14f, face.y - cell * 0.08f))
                drawCircle(Color(0xFF1565C0), cell * 0.05f, Offset(face.x - cell * 0.1f, face.y - cell * 0.08f))
                drawCircle(Color(0xFF1565C0), cell * 0.05f, Offset(face.x + cell * 0.16f, face.y - cell * 0.08f))
                drawArc(Color(0xFF5D4037), cell * 0.16f, cell * 0.1f, 20f, 140f, false, Offset(face.x - cell * 0.16f, face.y + cell * 0.05f), Size(cell * 0.36f, cell * 0.22f), style = Stroke(width = 3f))
            }
            "patrick" -> {
                val star = Path().apply {
                    moveTo(face.x, face.y - cell * 0.42f)
                    lineTo(face.x + cell * 0.14f, face.y - cell * 0.1f)
                    lineTo(face.x + cell * 0.42f, face.y - cell * 0.08f)
                    lineTo(face.x + cell * 0.18f, face.y + cell * 0.12f)
                    lineTo(face.x + cell * 0.26f, face.y + cell * 0.42f)
                    lineTo(face.x, face.y + cell * 0.22f)
                    lineTo(face.x - cell * 0.26f, face.y + cell * 0.42f)
                    lineTo(face.x - cell * 0.18f, face.y + cell * 0.12f)
                    lineTo(face.x - cell * 0.42f, face.y - cell * 0.08f)
                    lineTo(face.x - cell * 0.14f, face.y - cell * 0.1f)
                    close()
                }
                drawPath(star, Color(0xFFF48FB1))
                drawCircle(Color.White, cell * 0.07f, Offset(face.x - cell * 0.08f, face.y))
                drawCircle(Color.White, cell * 0.07f, Offset(face.x + cell * 0.1f, face.y))
            }
            "squidward" -> {
                drawOval(Color(0xFFC6B48A), Offset(face.x - cell * 0.22f, face.y - cell * 0.48f), Size(cell * 0.44f, cell * 0.7f))
                drawOval(Color(0xFFB09A78), Offset(face.x + cell * 0.05f, face.y - cell * 0.08f), Size(cell * 0.28f, cell * 0.16f))
                drawLine(Color(0xFF6D5A45), Offset(face.x - cell * 0.08f, face.y - cell * 0.18f), Offset(face.x + cell * 0.02f, face.y - cell * 0.12f), 3f)
                drawLine(Color(0xFF6D5A45), Offset(face.x + cell * 0.06f, face.y - cell * 0.18f), Offset(face.x + cell * 0.14f, face.y - cell * 0.12f), 3f)
            }
            "gary" -> {
                drawCircle(Color(0xFF81D4FA), cell * 0.34f, face)
                drawCircle(Color(0xFF4FC3F7), cell * 0.2f, Offset(face.x + cell * 0.05f, face.y), style = Stroke(width = 4f))
                drawCircle(Color(0xFF0288D1), cell * 0.08f, Offset(face.x + cell * 0.08f, face.y))
                drawCircle(Color(0xFFFFCC80), cell * 0.1f, Offset(face.x + cell * 0.28f, face.y + cell * 0.05f))
            }
            "krabs" -> {
                drawOval(Color(0xFFE53935), Offset(face.x - cell * 0.2f, face.y - cell * 0.28f), Size(cell * 0.4f, cell * 0.5f))
                drawOval(Color(0xFFC62828), Offset(face.x - cell * 0.48f, face.y - cell * 0.12f), Size(cell * 0.28f, cell * 0.22f))
                drawOval(Color(0xFFC62828), Offset(face.x + cell * 0.22f, face.y - cell * 0.12f), Size(cell * 0.28f, cell * 0.22f))
                drawLine(Color(0xFFFFD54F), Offset(face.x - cell * 0.08f, face.y + cell * 0.02f), Offset(face.x + cell * 0.08f, face.y + cell * 0.02f), 3f)
            }
            "drawn" -> {
                drawRoundRect(Color.White.copy(alpha = 0.15f), Offset(face.x - cell * 0.4f, face.y - cell * 0.4f), Size(cell * 0.8f, cell * 0.8f), androidx.compose.ui.geometry.CornerRadius(6f, 6f), style = Stroke(width = 3f))
                drawLine(Color.Black, Offset(face.x - cell * 0.2f, face.y - cell * 0.1f), Offset(face.x - cell * 0.05f, face.y - cell * 0.1f), 3f)
                drawLine(Color.Black, Offset(face.x + cell * 0.05f, face.y - cell * 0.1f), Offset(face.x + cell * 0.2f, face.y - cell * 0.1f), 3f)
                drawLine(Color.Black, Offset(face.x - cell * 0.12f, face.y + cell * 0.12f), Offset(face.x + cell * 0.14f, face.y + cell * 0.12f), 3f)
            }
            "plankton" -> {
                drawOval(Color(0xFF66BB6A), Offset(face.x - cell * 0.22f, face.y - cell * 0.28f), Size(cell * 0.44f, cell * 0.5f))
                drawCircle(Color.White, cell * 0.12f, Offset(face.x, face.y - cell * 0.05f))
                drawCircle(Color(0xFF1B5E20), cell * 0.06f, Offset(face.x + cell * 0.02f, face.y - cell * 0.05f))
                drawLine(Color(0xFF1B5E20), Offset(face.x - cell * 0.06f, face.y - cell * 0.32f), Offset(face.x - cell * 0.1f, face.y - cell * 0.48f), 3f)
                drawLine(Color(0xFF1B5E20), Offset(face.x + cell * 0.06f, face.y - cell * 0.32f), Offset(face.x + cell * 0.1f, face.y - cell * 0.48f), 3f)
                drawArc(Color(0xFF1B5E20), cell * 0.08f, cell * 0.05f, 10f, 160f, false, Offset(face.x - cell * 0.08f, face.y + cell * 0.08f), Size(cell * 0.18f, cell * 0.1f), style = Stroke(width = 2f))
            }
        }
        points.forEachIndexed { i, pt ->
            when (appearance) {
                "patrick" -> {
                    drawLine(Color(0xFFFFF59D), Offset(pt.x, pt.y - cell * 0.45f), Offset(pt.x + cell * 0.08f, pt.y - cell * 0.55f), 2f)
                    drawLine(Color(0xFFFFF59D), Offset(pt.x - cell * 0.08f, pt.y - cell * 0.4f), Offset(pt.x + cell * 0.08f, pt.y - cell * 0.4f), 2f)
                }
                "squidward" -> drawRect(Color(0xFF9E9E9E), Offset(pt.x - cell * 0.12f, pt.y - cell * 0.28f), Size(cell * 0.24f, cell * 0.16f))
                "gary" -> drawCircle(Color(0xFFFFAB91), cell * 0.06f, Offset(pt.x + cell * 0.22f, pt.y - cell * 0.1f))
                "krabs" -> {
                    drawCircle(Color(0xFFFFD54F), cell * 0.08f, Offset(pt.x, pt.y - cell * 0.32f))
                    drawRect(Color(0xFFF9A825), Offset(pt.x - cell * 0.03f, pt.y - cell * 0.4f), Size(cell * 0.06f, cell * 0.08f))
                }
                "spongebob" -> drawCircle(Color(0xFFFFF59D).copy(alpha = 0.35f), cell * 0.22f, pt)
                "drawn" -> drawLine(Color.Black.copy(alpha = 0.65f), Offset(pt.x - cell * 0.2f, pt.y), Offset(pt.x + cell * 0.18f, pt.y - cell * 0.12f), 2f)
                "plankton" -> drawArc(Color.White, cell * 0.05f, cell * 0.04f, 200f, 140f, false, Offset(pt.x - cell * 0.05f, pt.y - cell * 0.4f), Size(cell * 0.12f, cell * 0.08f), style = Stroke(width = 2f))
            }
        }
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
