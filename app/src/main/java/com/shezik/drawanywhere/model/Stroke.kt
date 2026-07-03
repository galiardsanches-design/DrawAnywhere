package com.shezik.drawanywhere.model

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.shezik.drawanywhere.drawing.PenRenderer

enum class StrokeModifier {
    None, PrimaryButton, SecondaryButton, Both
}

data class Stroke(
    internal val _points: MutableList<Offset> = mutableListOf(),
    val color: Color,
    val width: Float,
    val alpha: Float,
    val penType: PenType = PenType.Pen,
    val createdAt: Long = System.currentTimeMillis(),
    var modifiedAt: Long = createdAt,
) {
    val points: List<Offset> get() = _points

    // Perf: cache the smoothed freehand Path so completed strokes aren't rebuilt
    // every frame. Rebuilt only when the point count changes (points are only
    // appended while drawing; erasers create fresh strokes).
    private var _cachedPath: Path? = null
    private var _cachedPointCount: Int = -1

    /** Smoothed path for freehand/laser strokes, cached by point count. */
    fun smoothPath(): Path {
        val cached = _cachedPath
        if (cached != null && _cachedPointCount == _points.size) return cached
        val built = PenRenderer.buildPath(_points)
        _cachedPath = built
        _cachedPointCount = _points.size
        return built
    }

    fun render(canvas: Canvas, paint: Paint) {
        if (_points.isEmpty()) return
        paint.strokeWidth = width
        // Reset any path effect a previous stroke's renderer may have left on the
        // shared paint (e.g. the dashed-line renderer), so styles never leak.
        paint.pathEffect = null
        val argb = color.toArgb()
        val combinedAlpha = (color.alpha * alpha * 255).toInt().coerceIn(0, 255)
        paint.color = (argb and 0x00FFFFFF) or (combinedAlpha shl 24)
        penType.renderer.render(this, canvas, paint, System.currentTimeMillis())
    }
}

sealed class DrawAction {
    data class AddStroke(val stroke: Stroke) : DrawAction()
    data class EraseStroke(val stroke: Stroke) : DrawAction()
    data class ClearStrokes(val strokes: List<Stroke>) : DrawAction()
    data class CanvasSnapshot(val before: List<Stroke>, val after: List<Stroke>) : DrawAction()

    /** Returns this action with ephemeral strokes removed, or null if nothing remains. */
    fun withoutEphemeral(): DrawAction? = when (this) {
        is AddStroke -> if (stroke.penType.isEphemeral) null else this
        is EraseStroke -> if (stroke.penType.isEphemeral) null else this
        is ClearStrokes -> {
            val f = strokes.filter { !it.penType.isEphemeral }
            if (f.isEmpty()) null else copy(strokes = f)
        }
        is CanvasSnapshot -> {
            val b = before.filter { !it.penType.isEphemeral }
            val a = after.filter { !it.penType.isEphemeral }
            if (b.isEmpty() && a.isEmpty()) null
            else if (b == before && a == after) this
            else copy(before = b, after = a)
        }
    }
}
