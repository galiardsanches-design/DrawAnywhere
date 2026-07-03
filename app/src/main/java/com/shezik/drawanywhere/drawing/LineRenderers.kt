package com.shezik.drawanywhere.drawing

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
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

/**
 * Pedestrian crossing (зебра). The drag a→b spans the crossing across the road;
 * parallel white bars are laid perpendicular to it (along the travel direction).
 */
object ZebraRenderer : Renderer {
    override fun render(stroke: Stroke, canvas: Canvas, paint: Paint, now: Long) {
        val pts = stroke.points
        if (pts.size < 2) return
        val a = pts[0]; val b = pts[1]
        val dx = b.x - a.x; val dy = b.y - a.y
        val len = hypot(dx, dy)
        if (len < 1f) return
        val ux = dx / len; val uy = dy / len          // along a→b (across the road)
        val px = -uy; val py = ux                      // perpendicular (travel dir)
        val barHalf = (stroke.width * 6f).coerceAtLeast(40f)
        val barW = (stroke.width * 2f).coerceAtLeast(10f)
        paint.strokeWidth = barW
        val period = barW * 2f
        var d = period / 2f
        while (d < len) {
            val cx = a.x + ux * d; val cy = a.y + uy * d
            canvas.drawLine(cx - px * barHalf, cy - py * barHalf, cx + px * barHalf, cy + py * barHalf, paint)
            d += period
        }
    }
}

/** Stop line — толстая поперечная сплошная. */
object StopLineRenderer : Renderer {
    override fun render(stroke: Stroke, canvas: Canvas, paint: Paint, now: Long) {
        val pts = stroke.points
        if (pts.size < 2) return
        val a = pts[0]; val b = pts[1]
        paint.strokeWidth = (stroke.width * 4f).coerceAtLeast(24f)
        canvas.drawLine(a.x, a.y, b.x, b.y, paint)
    }
}

/** Curved turn arrow — стрелка поворота (дуга + наконечник в конце). */
object TurnArrowRenderer : Renderer {
    private const val HEAD_SPREAD = 0.5f

    override fun render(stroke: Stroke, canvas: Canvas, paint: Paint, now: Long) {
        val pts = stroke.points
        if (pts.size < 2) return
        val a = pts[0]; val b = pts[1]
        val dx = b.x - a.x; val dy = b.y - a.y
        val len = hypot(dx, dy)
        if (len < 1f) return
        val mx = (a.x + b.x) / 2f; val my = (a.y + b.y) / 2f
        val px = -dy / len; val py = dx / len
        val bend = len * 0.3f
        val cx = mx + px * bend; val cy = my + py * bend   // bezier control point

        val path = Path()
        path.moveTo(a.x, a.y)
        path.quadTo(cx, cy, b.x, b.y)
        canvas.drawPath(path, paint)

        // Arrowhead points along the tangent at the end (control→end direction).
        val angle = atan2(b.y - cy, b.x - cx)
        val headLen = (stroke.width * 6f).coerceAtLeast(28f)
        val x1 = b.x - headLen * cos(angle - HEAD_SPREAD)
        val y1 = b.y - headLen * sin(angle - HEAD_SPREAD)
        val x2 = b.x - headLen * cos(angle + HEAD_SPREAD)
        val y2 = b.y - headLen * sin(angle + HEAD_SPREAD)
        canvas.drawLine(b.x, b.y, x1, y1, paint)
        canvas.drawLine(b.x, b.y, x2, y2, paint)
    }
}
