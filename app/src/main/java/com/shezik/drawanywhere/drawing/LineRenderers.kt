package com.shezik.drawanywhere.drawing

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import com.shezik.drawanywhere.model.Stroke
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Road-marking renderers. All operate on a two-point [Stroke] laid down by
 * [LineTool]. [Stroke.render] clears [Paint.setPathEffect] to null before every
 * call, so a renderer that sets a dash effect never leaks it onto later strokes.
 */

/** Single solid line — центральная/краевая сплошная. */
object LineRenderer : Renderer {
    override fun render(stroke: Stroke, canvas: Canvas, paint: Paint, now: Long) {
        val pts = stroke.points
        if (pts.size < 2) return
        val a = pts[0]; val b = pts[1]
        canvas.drawLine(a.x, a.y, b.x, b.y, paint)
    }
}

/** Two parallel solid lines — двойная сплошная. */
object DoubleLineRenderer : Renderer {
    override fun render(stroke: Stroke, canvas: Canvas, paint: Paint, now: Long) {
        val pts = stroke.points
        if (pts.size < 2) return
        val a = pts[0]; val b = pts[1]
        val dx = b.x - a.x; val dy = b.y - a.y
        val len = hypot(dx, dy)
        if (len < 1e-3f) return
        // Perpendicular unit vector scaled by the stroke width = half the gap.
        val off = stroke.width
        val nx = -dy / len * off; val ny = dx / len * off
        canvas.drawLine(a.x + nx, a.y + ny, b.x + nx, b.y + ny, paint)
        canvas.drawLine(a.x - nx, a.y - ny, b.x - nx, b.y - ny, paint)
    }
}

/** Dashed line — прерывистая разметка. Dash length scales with stroke width. */
object DashedLineRenderer : Renderer {
    override fun render(stroke: Stroke, canvas: Canvas, paint: Paint, now: Long) {
        val pts = stroke.points
        if (pts.size < 2) return
        val a = pts[0]; val b = pts[1]
        val on = (stroke.width * 5f).coerceAtLeast(12f)
        val off = (stroke.width * 4f).coerceAtLeast(10f)
        paint.pathEffect = DashPathEffect(floatArrayOf(on, off), 0f)
        canvas.drawLine(a.x, a.y, b.x, b.y, paint)
    }
}

/**
 * Straight line with a solid arrowhead at the START point (pts[0]) — стрелка
 * рисуется "обратно": наконечник там, где касание НАЧАЛОСЬ, хвост — там где
 * палец поднят.
 */
object ArrowRenderer : Renderer {
    private const val HEAD_SPREAD = 0.5f // radians, ~28.6°

    override fun render(stroke: Stroke, canvas: Canvas, paint: Paint, now: Long) {
        val pts = stroke.points
        if (pts.size < 2) return
        // head end = a (start of the gesture); tail end = b (where finger lifted)
        val head = pts[0]; val tail = pts[1]
        canvas.drawLine(tail.x, tail.y, head.x, head.y, paint)

        // Arrowhead points away from the tail, i.e. in the head→(away from tail) direction.
        val angle = atan2(head.y - tail.y, head.x - tail.x)
        val headLen = (stroke.width * 6f).coerceAtLeast(28f)
        val x1 = head.x - headLen * cos(angle - HEAD_SPREAD)
        val y1 = head.y - headLen * sin(angle - HEAD_SPREAD)
        val x2 = head.x - headLen * cos(angle + HEAD_SPREAD)
        val y2 = head.y - headLen * sin(angle + HEAD_SPREAD)
        canvas.drawLine(head.x, head.y, x1, y1, paint)
        canvas.drawLine(head.x, head.y, x2, y2, paint)
    }
}

/**
 * A wide grey road band — инструмент "дорога". Draw grey strips and compose any
 * intersection (cross, T, roundabout) from them, then add white markings on top.
 * Ignores the current drawing colour (always grey) and renders much thicker than
 * the nominal width so it reads as a road surface.
 */
object RoadRenderer : Renderer {
    private const val ROAD_COLOR = 0xFF5A5A5A.toInt()
    private const val WIDTH_MULTIPLIER = 10f
    private const val MIN_WIDTH = 60f

    override fun render(stroke: Stroke, canvas: Canvas, paint: Paint, now: Long) {
        val pts = stroke.points
        if (pts.size < 2) return
        val a = pts[0]; val b = pts[1]
        val savedColor = paint.color
        val savedWidth = paint.strokeWidth
        paint.color = ROAD_COLOR
        paint.strokeWidth = (stroke.width * WIDTH_MULTIPLIER).coerceAtLeast(MIN_WIDTH)
        canvas.drawLine(a.x, a.y, b.x, b.y, paint)
        paint.color = savedColor
        paint.strokeWidth = savedWidth
    }
}
