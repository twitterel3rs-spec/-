package com.example.ui.components

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Enables click-and-drag scrolling with a mouse/non-touch pointer on Compose scrollables
 * (for Android-x86 / Bliss OS / Desktop mouse usage) while preserving:
 * - 100% native touch scrolling on mobile touchscreens (PointerType.Touch is untouched).
 * - Normal vertical mouse wheel scrolling up/down (horizontal rows never hijack plain vertical wheel).
 * - Optional Shift+Wheel or horizontal tilt-wheel scrolling on horizontal containers.
 * - Clean orthogonal axis separation (vertical drag passes through horizontal rows; horizontal text
 *   selection inside TextFields passes through vertical containers).
 * - Zero accidental child clicks when releasing a drag.
 */
fun Modifier.mouseDragVerticalScroll(
    state: ScrollableState,
    enabled: Boolean = true
): Modifier = mouseDragScrollInternal(
    state = state,
    orientation = Orientation.Vertical,
    enabled = enabled
)

fun Modifier.mouseDragHorizontalScroll(
    state: ScrollableState,
    enabled: Boolean = true
): Modifier = mouseDragScrollInternal(
    state = state,
    orientation = Orientation.Horizontal,
    enabled = enabled
)

fun Modifier.mouseVerticalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false
): Modifier = this
    .mouseDragVerticalScroll(state = state, enabled = enabled)
    .verticalScroll(
        state = state,
        enabled = enabled,
        flingBehavior = flingBehavior,
        reverseScrolling = reverseScrolling
    )

fun Modifier.mouseHorizontalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false
): Modifier = this
    .mouseDragHorizontalScroll(state = state, enabled = enabled)
    .horizontalScroll(
        state = state,
        enabled = enabled,
        flingBehavior = flingBehavior,
        reverseScrolling = reverseScrolling
    )

@Composable
fun MouseLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.mouseDragVerticalScroll(state = state, enabled = userScrollEnabled),
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}

@Composable
fun MouseLazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    LazyRow(
        modifier = modifier.mouseDragHorizontalScroll(state = state, enabled = userScrollEnabled),
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}

@Composable
fun MouseLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.() -> Unit
) {
    LazyVerticalGrid(
        columns = columns,
        modifier = modifier.mouseDragVerticalScroll(state = state, enabled = userScrollEnabled),
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}

private fun Modifier.mouseDragScrollInternal(
    state: ScrollableState,
    orientation: Orientation,
    enabled: Boolean
): Modifier = composed {
    if (!enabled) return@composed this

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val decaySpec = rememberSplineBasedDecay<Float>()
    val wheelStepPx = with(density) { 64.dp.toPx() }
    val currentState by rememberUpdatedState(state)

    this.pointerInput(state, orientation, isRtl) {
        var activeFlingJob: Job? = null

        awaitPointerEventScope {
            val touchSlop = viewConfiguration.touchSlop.coerceAtLeast(6f)

            while (true) {
                val firstEvent = awaitPointerEvent(PointerEventPass.Initial)

                // Support Shift+Wheel and native horizontal wheel on horizontal scrollables,
                // while leaving plain vertical mouse wheel completely free to scroll parent/vertical lists up & down.
                if (firstEvent.type == PointerEventType.Scroll && orientation == Orientation.Horizontal) {
                    val scrollChange = firstEvent.changes.firstOrNull()
                    if (scrollChange != null && !scrollChange.isConsumed) {
                        val sd = scrollChange.scrollDelta
                        val isShift = firstEvent.keyboardModifiers.isShiftPressed
                        val rawTicks = when {
                            abs(sd.x) > abs(sd.y) -> sd.x
                            isShift && abs(sd.y) > 0f -> sd.y
                            else -> 0f
                        }
                        if (rawTicks != 0f) {
                            // Positive wheel tick usually scrolls forward (left in RTL / right in LTR)
                            val scrollPx = rawTicks * wheelStepPx
                            val canMove = if (scrollPx > 0f) currentState.canScrollForward else currentState.canScrollBackward
                            if (canMove) {
                                activeFlingJob?.cancel()
                                currentState.dispatchRawDelta(scrollPx)
                                scrollChange.consume()
                            }
                        }
                    }
                    continue
                }

                if (firstEvent.type != PointerEventType.Press) continue
                val down = firstEvent.changes.firstOrNull() ?: continue

                // Leave touchscreens 100% native; handle mouse/non-touch primary button drags.
                if (down.type == PointerType.Touch) continue
                if (down.isConsumed || !down.pressed) continue
                if (firstEvent.buttons.isSecondaryPressed || firstEvent.buttons.isTertiaryPressed) continue
                if (!firstEvent.buttons.isPrimaryPressed && down.type == PointerType.Mouse) continue

                activeFlingJob?.cancel()

                val activeId = down.id
                val startPos = down.position
                var prevPos = down.position
                var dragging = false
                var abandoned = false
                val velocityTracker = VelocityTracker()
                velocityTracker.addPosition(down.uptimeMillis, down.position)

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == activeId } ?: break

                    if (!change.pressed) {
                        if (dragging) {
                            change.consume()
                            val velocity = velocityTracker.calculateVelocity()
                            val rawAxisVel = if (orientation == Orientation.Vertical) velocity.y else velocity.x
                            val flingVel = mapPointerDeltaToScrollDelta(
                                axisDelta = rawAxisVel,
                                orientation = orientation,
                                isRtl = isRtl
                            )
                            if (abs(flingVel) > 80f) {
                                val targetState = currentState
                                activeFlingJob?.cancel()
                                activeFlingJob = scope.launch {
                                    try {
                                        targetState.scroll {
                                            var lastValue = 0f
                                            AnimationState(
                                                initialValue = 0f,
                                                initialVelocity = flingVel
                                            ).animateDecay(decaySpec) {
                                                val step = value - lastValue
                                                lastValue = value
                                                val consumed = scrollBy(step)
                                                if (abs(step) > 0.5f && abs(consumed) < 0.1f) {
                                                    cancelAnimation()
                                                }
                                            }
                                        }
                                    } catch (_: CancellationException) {
                                    }
                                }
                            }
                        }
                        break
                    }

                    if (change.isConsumed || abandoned) {
                        prevPos = change.position
                        continue
                    }

                    val dxFromStart = change.position.x - startPos.x
                    val dyFromStart = change.position.y - startPos.y
                    val absDx = abs(dxFromStart)
                    val absDy = abs(dyFromStart)

                    if (!dragging) {
                        val primaryDist = if (orientation == Orientation.Vertical) absDy else absDx
                        val crossDist = if (orientation == Orientation.Vertical) absDx else absDy
                        if (primaryDist > touchSlop && primaryDist >= crossDist) {
                            dragging = true
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val stepDelta = if (orientation == Orientation.Vertical) {
                                change.position.y - prevPos.y
                            } else {
                                change.position.x - prevPos.x
                            }
                            val scrollDelta = mapPointerDeltaToScrollDelta(
                                axisDelta = stepDelta,
                                orientation = orientation,
                                isRtl = isRtl
                            )
                            if (scrollDelta != 0f) {
                                currentState.dispatchRawDelta(scrollDelta)
                            }
                        } else if (crossDist > touchSlop && crossDist > primaryDist * 1.15f) {
                            // User is clearly dragging orthogonally (e.g. horizontal row inside vertical sheet,
                            // or vertical drag over a horizontal row, or text selection in TextField).
                            abandoned = true
                        }
                    } else {
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val stepDelta = if (orientation == Orientation.Vertical) {
                            change.position.y - prevPos.y
                        } else {
                            change.position.x - prevPos.x
                        }
                        val scrollDelta = mapPointerDeltaToScrollDelta(
                            axisDelta = stepDelta,
                            orientation = orientation,
                            isRtl = isRtl
                        )
                        if (scrollDelta != 0f) {
                            currentState.dispatchRawDelta(scrollDelta)
                        }
                    }
                    prevPos = change.position
                }
            }
        }
    }
}

internal fun mapPointerDeltaToScrollDelta(
    axisDelta: Float,
    orientation: Orientation,
    isRtl: Boolean
): Float {
    return when (orientation) {
        Orientation.Vertical -> -axisDelta
        Orientation.Horizontal -> if (isRtl) axisDelta else -axisDelta
    }
}
