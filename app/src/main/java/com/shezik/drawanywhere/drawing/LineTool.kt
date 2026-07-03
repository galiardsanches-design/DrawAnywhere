package com.shezik.drawanywhere.drawing

import androidx.compose.ui.geometry.Offset
import com.shezik.drawanywhere.model.DrawAction
import com.shezik.drawanywhere.model.Stroke
import com.shezik.drawanywhere.util.distance

/**
 * Straight two-point tool shared by Line / DoubleLine / DashedLine / Arrow.
 * The visual difference lives entirely in each pen type's [Renderer]; this tool
 * only maintains the two endpoints. Unlike [ShapeTool] it keeps the real
 * start/end points (direction matters for arrows), so it does NOT normalize to a
 * bounding box.
 */
class LineTool(private val ctx: ToolContext) : StrokeTool {

    override fun onStart(point: Offset) {
        ctx.strokes.add(Stroke(
            _points = mutableListOf(point, point),
            color = ctx.penConfig.color,
            width = ctx.penConfig.width,
            alpha = ctx.penConfig.alpha,
            penType = ctx.penConfig.penType,
        ))
    }

    override fun onMove(point: Offset) {
        ctx.strokes.lastOrNull()?.let { it._points[1] = point }
    }

    override fun onFinish() {
        val stroke = ctx.strokes.lastOrNull() ?: return
        val p0 = stroke._points[0]; val p1 = stroke._points[1]
        // Discard accidental taps that produced no real line.
        if (distance(p0, p1) < 4f) {
            ctx.strokes.removeAt(ctx.strokes.lastIndex)
            return
        }
        ctx.pushUndo(DrawAction.AddStroke(stroke))
        ctx.notifyChanged()
    }
}
