package com.shezik.drawanywhere.view.toolbar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.shezik.drawanywhere.DrawViewModel
import com.shezik.drawanywhere.R
import com.shezik.drawanywhere.ui.theme.DrawAnywhereTheme
import com.shezik.drawanywhere.ui.theme.Spacing

/**
 * Minimalist floating toolbar: a small draggable bubble (showing the current
 * tool) that taps open into a single compact icon ribbon — everything inside,
 * no text, no drawers. Reuses the existing draggable card, button definitions
 * and popup renderer so behaviour (tool popups, colour picker, drag-to-move,
 * drag-to-dismiss, inactivity dimming) is unchanged.
 */
@Composable
fun MiniToolbar(
    viewModel: DrawViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val canClearCanvas by viewModel.canClearCanvas.collectAsState()
    val lockMode by viewModel.lockMode.collectAsState()
    val haptics = LocalHapticFeedback.current

    var expanded by remember { mutableStateOf(true) }

    val buttons = createAllToolbarButtons(
        uiState = uiState,
        canUndo = canUndo,
        canRedo = canRedo,
        canClearCanvas = canClearCanvas,
        onCanvasVisibilityToggle = viewModel::toggleCanvasVisibility,
        onCanvasPassthroughToggle = viewModel::toggleCanvasPassthrough,
        onClearCanvas = viewModel::clearCanvas,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onPenTypeSwitch = viewModel::switchToPen,
        onColorChange = viewModel::setPenColor,
        onPresetColorChange = viewModel::setPresetColor,
        onStrokeWidthChange = viewModel::setStrokeWidth,
        onAlphaChange = viewModel::setStrokeAlpha,
        onChangeOrientation = viewModel::setToolbarOrientation,
        onChangeAutoClearCanvas = viewModel::setAutoClearCanvas,
        onChangeVisibleOnStart = viewModel::setVisibleOnStart,
        fingerDrawingEnabled = uiState.fingerDrawingEnabled,
        onChangeFingerDrawingEnabled = viewModel::setFingerDrawingEnabled,
        onCycleLockMode = viewModel::cycleLockMode,
        lockMode = lockMode,
        onQuitApplication = viewModel::quitApplication,
        onSave = viewModel::requestSave,
        onCycleBackground = viewModel::cycleCanvasBackground
    ).associateBy { it.id }

    // Curated single-row order (icon only). horizontalScroll keeps it on screen.
    val order = listOf(
        "tool_controls", "color_picker", "undo", "redo", "clear",
        "save", "background", "visibility", "passthrough", "zoom_lock", "settings"
    )

    DrawAnywhereTheme {
        BoxWithConstraints {
            val maxW = maxWidth
            DraggableToolbarCard(
                modifier = modifier
                    .wrapContentSize(unbounded = true)
                    .widthIn(max = maxW)
                    .padding(Spacing.xs),
                haptics = haptics,
                onPositionChange = viewModel::updateToolbarPosition,
                onPositionSaved = viewModel::saveToolbarPosition,
                onToolbarInteracted = viewModel::resetToolbarTimer,
                onDragStart = viewModel::onDismissDragStart,
                onDragPosition = viewModel::onDismissDragMove,
                onDragEnd = viewModel::onDismissDragEnd,
            ) {
                if (!expanded) {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier.padding(Spacing.xs)
                    ) {
                        Icon(
                            imageVector = uiState.currentPenType.icon,
                            contentDescription = stringResource(R.string.expand_toolbar),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .animateContentSize()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        IconButton(onClick = { expanded = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.collapse_toolbar),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        order.forEach { id ->
                            buttons[id]?.let { RenderButton(it, Alignment.TopCenter) }
                        }
                    }
                }
            }
        }
    }
}
