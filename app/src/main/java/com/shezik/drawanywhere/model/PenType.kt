package com.shezik.drawanywhere.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.shezik.drawanywhere.R
import com.shezik.drawanywhere.drawing.ArrowRenderer
import com.shezik.drawanywhere.drawing.DashedLineRenderer
import com.shezik.drawanywhere.drawing.DoubleLineRenderer
import com.shezik.drawanywhere.drawing.EdgeHitTester
import com.shezik.drawanywhere.drawing.RoadRenderer
import com.shezik.drawanywhere.drawing.StopLineRenderer
import com.shezik.drawanywhere.drawing.TurnArrowRenderer
import com.shezik.drawanywhere.drawing.ZebraRenderer
import com.shezik.drawanywhere.drawing.FreehandTool
import com.shezik.drawanywhere.drawing.HitTester
import com.shezik.drawanywhere.drawing.LaserRenderer
import com.shezik.drawanywhere.drawing.LineRenderer
import com.shezik.drawanywhere.drawing.LineTool
import com.shezik.drawanywhere.drawing.OvalRenderer
import com.shezik.drawanywhere.drawing.PenRenderer
import com.shezik.drawanywhere.drawing.PixelEraserTool
import com.shezik.drawanywhere.drawing.RectRenderer
import com.shezik.drawanywhere.drawing.Renderer
import com.shezik.drawanywhere.drawing.SegmentHitTester
import com.shezik.drawanywhere.drawing.ShapeTool
import com.shezik.drawanywhere.drawing.StrokeEraserTool
import com.shezik.drawanywhere.drawing.StrokeTool
import com.shezik.drawanywhere.drawing.ToolContext
import com.shezik.drawanywhere.view.toolbar.InkEraser24Px

enum class PenType(
    val labelResId: Int,
    val icon: ImageVector,
    val renderer: Renderer,
    val hitTester: HitTester,
    val ttlMs: Long? = null,
    val isEraser: Boolean = false,
) {
    Pen(R.string.pen, Icons.Default.Edit, PenRenderer, SegmentHitTester),
    Laser(R.string.laser, Icons.Default.FlashOn, LaserRenderer, SegmentHitTester, ttlMs = 3_000L),
    Road(R.string.road, Icons.Default.AddRoad, RoadRenderer, SegmentHitTester),
    Line(R.string.line, Icons.Default.HorizontalRule, LineRenderer, SegmentHitTester),
    DoubleLine(R.string.double_line, Icons.Default.DragHandle, DoubleLineRenderer, SegmentHitTester),
    DashedLine(R.string.dashed_line, Icons.Default.MoreHoriz, DashedLineRenderer, SegmentHitTester),
    Arrow(R.string.arrow, Icons.Default.ArrowForward, ArrowRenderer, SegmentHitTester),
    TurnArrow(R.string.turn_arrow, Icons.Default.TurnRight, TurnArrowRenderer, SegmentHitTester),
    StopLine(R.string.stop_line, Icons.Default.Minimize, StopLineRenderer, SegmentHitTester),
    Zebra(R.string.zebra, Icons.Default.DirectionsWalk, ZebraRenderer, SegmentHitTester),
    Rectangle(R.string.rectangle, Icons.Default.CropSquare, RectRenderer, EdgeHitTester),
    Ellipse(R.string.ellipse, Icons.Default.RadioButtonUnchecked, OvalRenderer, EdgeHitTester),
    StrokeEraser(R.string.stroke_eraser, InkEraser24Px, PenRenderer, SegmentHitTester, isEraser = true),
    PixelEraser(R.string.pixel_eraser, Icons.Default.BlurOn, PenRenderer, SegmentHitTester, isEraser = true);

    val isEphemeral: Boolean get() = ttlMs != null

    fun createTool(ctx: ToolContext): StrokeTool = when (this) {
        Pen, Laser -> FreehandTool(ctx)
        Road, Line, DoubleLine, DashedLine, Arrow, TurnArrow, StopLine, Zebra -> LineTool(ctx)
        Rectangle, Ellipse -> ShapeTool(ctx)
        StrokeEraser -> StrokeEraserTool(ctx)
        PixelEraser -> PixelEraserTool(ctx)
    }
}

data class PenConfig(
    val penType: PenType = PenType.Pen,
    // Road markings are white — default the drawing color to white.
    val color: Color = Color.White,
    val width: Float = 5f,
    val alpha: Float = 1f
)
