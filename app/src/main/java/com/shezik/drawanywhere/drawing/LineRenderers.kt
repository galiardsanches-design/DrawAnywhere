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

/** Straight line with a solid arrowhead at the end point — стрелка направления. */
object ArrowRenderer : Renderer {
    private const val HEAD_SPREAD = 0.5f // radians, ~28.6°

    override fun render(stroke: Stroke, canvas: Canvas, paint: Paint, now: Long) {
        val pts = stroke.points
        if (pts.size < 2) return
        val a = pts[0]; val b = pts[1]
        canvas.drawLine(a.x, a.y, b.x, b.y, paint)

        val angle = atan2(b.y - a.y, b.x - a.x)
        val headLen = (stroke.width * 6f).coerceAtLeast(28f)
        val x1 = b.x - headLen * cos(angle - HEAD_SPREAD)
        val y1 = b.y - headLen * sin(angle - HEAD_SPREAD)
        val x2 = b.x - headLen * cos(angle + HEAD_SPREAD)
        val y2 = b.y - headLen * sin(angle + HEAD_SPREAD)
        canvas.drawLine(b.x, b.y, x1, y1, paint)
        canvas.drawLine(b.x, b.y, x2, y2, paint)
    }
}
