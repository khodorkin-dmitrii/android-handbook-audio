package com.yavin.androidhandbookaudio.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yavin.androidhandbookaudio.ui.theme.AndroidHandbookAudioTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun PlayerAmbientBackground(
    progress: Float?,
    progressKey: Any? = null,
    isProgressPreview: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
    ) {
        AmbientBlobBackground(Modifier.fillMaxSize())
        if (progress != null || progressKey != null) {
            PerimeterPlaybackProgress(
                progress = progress,
                progressKey = progressKey,
                isProgressPreview = isProgressPreview,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun AmbientBlobBackground(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val blobAlpha = if (isDark) 0.28f else 0.23f
    val transition = rememberInfiniteTransition(label = "ambient blobs")
    val firstPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = FULL_CIRCLE_RADIANS,
        animationSpec = infiniteRepeatable(
            animation = tween(23_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "primary blob phase",
    )
    val secondPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = FULL_CIRCLE_RADIANS,
        animationSpec = infiniteRepeatable(
            animation = tween(29_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "secondary blob phase",
    )
    val thirdPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = FULL_CIRCLE_RADIANS,
        animationSpec = infiniteRepeatable(
            animation = tween(35_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tertiary blob phase",
    )

    Box(
        modifier = modifier.drawWithCache {
            val baseRadius = min(size.width, size.height) * 0.58f
            val primaryBrush = ambientBlobBrush(colorScheme.primary, blobAlpha, baseRadius)
            val secondaryBrush = ambientBlobBrush(colorScheme.secondary, blobAlpha * 0.9f, baseRadius)
            val tertiaryBrush = ambientBlobBrush(colorScheme.tertiary, blobAlpha * 0.85f, baseRadius)

            onDrawBehind {
                drawAmbientBlob(
                    brush = primaryBrush,
                    radius = baseRadius,
                    center = Offset(
                        x = size.width * (
                            0.24f + 0.13f * cos(firstPhase) +
                                0.04f * cos(firstPhase * 2f)
                            ),
                        y = size.height * (
                            0.25f + 0.10f * sin(firstPhase) +
                                0.035f * sin(firstPhase * 3f)
                            ),
                    ),
                    scaleX = 1.15f,
                    scaleY = 0.78f,
                )
                drawAmbientBlob(
                    brush = secondaryBrush,
                    radius = baseRadius * 0.92f,
                    center = Offset(
                        x = size.width * (
                            0.77f + 0.11f * cos(secondPhase + 1.9f) +
                                0.035f * sin(secondPhase * 2f)
                            ),
                        y = size.height * (
                            0.48f + 0.15f * sin(secondPhase + 1.9f) +
                                0.03f * cos(secondPhase * 3f)
                            ),
                    ),
                    scaleX = 0.88f,
                    scaleY = 1.12f,
                )
                drawAmbientBlob(
                    brush = tertiaryBrush,
                    radius = baseRadius * 0.86f,
                    center = Offset(
                        x = size.width * (
                            0.42f + 0.15f * cos(thirdPhase + 3.7f) +
                                0.025f * sin(thirdPhase * 3f)
                            ),
                        y = size.height * (
                            0.78f + 0.09f * sin(thirdPhase + 3.7f) +
                                0.04f * cos(thirdPhase * 2f)
                            ),
                    ),
                    scaleX = 1.05f,
                    scaleY = 0.82f,
                )
            }
        },
    )
}

@Composable
private fun PerimeterPlaybackProgress(
    progress: Float?,
    progressKey: Any?,
    isProgressPreview: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetProgress = progress?.coerceIn(0f, 1f)
    val animatedProgress = remember(progressKey) { Animatable(targetProgress ?: 0f) }
    LaunchedEffect(targetProgress, isProgressPreview) {
        val target = targetProgress ?: return@LaunchedEffect
        val delta = abs(target - animatedProgress.value)
        val durationMillis = when {
            isProgressPreview -> PREVIEW_PROGRESS_ANIMATION_DURATION_MS
            delta > MAX_SMOOTH_PROGRESS_STEP -> SEEK_PROGRESS_ANIMATION_DURATION_MS
            else -> PROGRESS_ANIMATION_DURATION_MS
        }
        animatedProgress.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearEasing,
            ),
        )
    }
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val trailColor = colorScheme.primary.copy(alpha = if (isDark) 0.62f else 0.46f)
    val trailGlowColor = colorScheme.tertiary.copy(alpha = if (isDark) 0.20f else 0.14f)
    val headGlowColor = colorScheme.tertiary.copy(alpha = if (isDark) 0.36f else 0.26f)
    val headColor = colorScheme.primary.copy(alpha = if (isDark) 0.96f else 0.84f)

    Box(
        modifier = modifier.drawWithCache {
            val inset = 12.dp.toPx()
            val cornerRadius = 28.dp.toPx()
            val trailWidth = 2.5.dp.toPx()
            val glowWidth = 7.dp.toPx()
            val headRadius = 3.dp.toPx()
            val headGlowRadius = 15.dp.toPx()
            val bounds = Rect(
                left = inset,
                top = inset,
                right = size.width - inset,
                bottom = size.height - inset,
            )

            if (bounds.width <= 0f || bounds.height <= 0f) {
                onDrawBehind { }
            } else {
                val radius = min(cornerRadius, min(bounds.width, bounds.height) / 2f)
                val perimeterPath = roundedPerimeterPath(bounds, radius)
                val pathMeasure = PathMeasure().apply {
                    setPath(perimeterPath, forceClosed = true)
                }
                val travelledPath = Path()
                val headGlowBrush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to headGlowColor,
                        0.38f to headGlowColor.copy(alpha = headGlowColor.alpha * 0.5f),
                        1f to Color.Transparent,
                    ),
                    center = Offset.Zero,
                    radius = headGlowRadius,
                )
                val glowStroke = Stroke(
                    width = glowWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
                val trailStroke = Stroke(
                    width = trailWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )

                onDrawBehind {
                    val fraction = animatedProgress.value
                    if (fraction <= 0f) return@onDrawBehind

                    val travelledDistance = pathMeasure.length * fraction
                    travelledPath.rewind()
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = travelledDistance,
                        destination = travelledPath,
                        startWithMoveTo = true,
                    )
                    drawPath(travelledPath, color = trailGlowColor, style = glowStroke)
                    drawPath(travelledPath, color = trailColor, style = trailStroke)

                    val head = pathMeasure.getPosition(travelledDistance)
                    withTransform({ translate(head.x, head.y) }) {
                        drawCircle(
                            brush = headGlowBrush,
                            radius = headGlowRadius,
                            center = Offset.Zero,
                        )
                        drawCircle(
                            color = headColor,
                            radius = headRadius,
                            center = Offset.Zero,
                        )
                    }
                }
            }
        },
    )
}

private fun roundedPerimeterPath(bounds: Rect, radius: Float) = Path().apply {
    val diameter = radius * 2f

    moveTo(bounds.center.x, bounds.top)
    lineTo(bounds.right - radius, bounds.top)
    arcTo(
        rect = Rect(
            left = bounds.right - diameter,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.top + diameter,
        ),
        startAngleDegrees = -90f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
    )
    lineTo(bounds.right, bounds.bottom - radius)
    arcTo(
        rect = Rect(
            left = bounds.right - diameter,
            top = bounds.bottom - diameter,
            right = bounds.right,
            bottom = bounds.bottom,
        ),
        startAngleDegrees = 0f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
    )
    lineTo(bounds.left + radius, bounds.bottom)
    arcTo(
        rect = Rect(
            left = bounds.left,
            top = bounds.bottom - diameter,
            right = bounds.left + diameter,
            bottom = bounds.bottom,
        ),
        startAngleDegrees = 90f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
    )
    lineTo(bounds.left, bounds.top + radius)
    arcTo(
        rect = Rect(
            left = bounds.left,
            top = bounds.top,
            right = bounds.left + diameter,
            bottom = bounds.top + diameter,
        ),
        startAngleDegrees = 180f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
    )
    lineTo(bounds.center.x, bounds.top)
    close()
}

private fun ambientBlobBrush(
    color: Color,
    alpha: Float,
    radius: Float,
) = Brush.radialGradient(
    colorStops = arrayOf(
        0f to color.copy(alpha = alpha),
        0.46f to color.copy(alpha = alpha * 0.62f),
        0.78f to color.copy(alpha = alpha * 0.18f),
        1f to Color.Transparent,
    ),
    center = Offset.Zero,
    radius = radius,
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAmbientBlob(
    brush: Brush,
    radius: Float,
    center: Offset,
    scaleX: Float,
    scaleY: Float,
) {
    withTransform({
        translate(center.x, center.y)
        scale(scaleX, scaleY, pivot = Offset.Zero)
    }) {
        drawCircle(
            brush = brush,
            radius = radius,
            center = Offset.Zero,
        )
    }
}

private const val FULL_CIRCLE_RADIANS = (PI * 2).toFloat()
private const val PROGRESS_ANIMATION_DURATION_MS = 500
private const val SEEK_PROGRESS_ANIMATION_DURATION_MS = 180
private const val PREVIEW_PROGRESS_ANIMATION_DURATION_MS = 80
private const val MAX_SMOOTH_PROGRESS_STEP = 0.05f

@Preview(
    name = "Player ambient background — light",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun PlayerAmbientBackgroundLightPreview() {
    AndroidHandbookAudioTheme(darkTheme = false, dynamicColor = false) {
        PlayerAmbientBackground(
            progress = 0.6f,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(
    name = "Player ambient background — dark",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PlayerAmbientBackgroundDarkPreview() {
    AndroidHandbookAudioTheme(darkTheme = true, dynamicColor = false) {
        PlayerAmbientBackground(
            progress = 0.6f,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(
    name = "Player ambient background — progress hidden",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
)
@Composable
private fun PlayerAmbientBackgroundWithoutProgressPreview() {
    AndroidHandbookAudioTheme(dynamicColor = false) {
        PlayerAmbientBackground(
            progress = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
