package com.yavin.androidhandbookaudio.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yavin.androidhandbookaudio.ui.theme.AndroidHandbookAudioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)?,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val thumbSize by animateDpAsState(
        targetValue = if (isDragged) 14.dp else 13.dp,
        label = "Audio seek thumb size",
    )
    val colorScheme = MaterialTheme.colorScheme
    val colors = SliderDefaults.colors(
        thumbColor = colorScheme.primary,
        activeTrackColor = colorScheme.primary,
        inactiveTrackColor = colorScheme.primary.copy(alpha = 0.18f),
        disabledThumbColor = colorScheme.onSurface.copy(alpha = 0.38f),
        disabledActiveTrackColor = colorScheme.onSurface.copy(alpha = 0.38f),
        disabledInactiveTrackColor = colorScheme.onSurface.copy(alpha = 0.12f),
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        thumb = {
            Box(
                // Material 3 Slider enforces a 16.dp minimum height for the horizontal thumb slot.
                // Matching that height keeps the smaller visual thumb centered on the track.
                modifier = Modifier.size(width = 14.dp, height = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Spacer(
                    modifier = Modifier
                        .size(thumbSize)
                        .background(
                            color = if (enabled) {
                                colorScheme.primary
                            } else {
                                colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                            shape = CircleShape,
                        ),
                )
            }
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(3.dp),
                enabled = enabled,
                colors = colors,
                drawStopIndicator = null,
                drawTick = { _, _ -> },
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 2.dp,
            )
        },
        modifier = modifier,
    )
}

@Preview(
    name = "Audio seek bar",
    showBackground = true,
    widthDp = 393,
    heightDp = 96,
)
@Composable
private fun AudioSeekBarPreview() {
    AndroidHandbookAudioTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                var value by remember { mutableFloatStateOf(12_000f) }
                AudioSeekBar(
                    value = value,
                    onValueChange = { value = it },
                    onValueChangeFinished = {},
                    valueRange = 0f..30_000f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
            }
        }
    }
}
