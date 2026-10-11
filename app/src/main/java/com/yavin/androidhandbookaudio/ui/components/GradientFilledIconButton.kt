package com.yavin.androidhandbookaudio.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yavin.androidhandbookaudio.R
import com.yavin.androidhandbookaudio.ui.theme.AndroidHandbookAudioTheme

@Composable
fun GradientFilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animateGradient: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val animatedFraction = if (enabled && animateGradient) {
        val transition = rememberInfiniteTransition(label = "Playback button gradient")
        transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "Cyan gradient fraction",
        )
    } else null
    val primary = scheme.primary
    val inversePrimary = scheme.inversePrimary
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.clip(CircleShape).drawBehind {
            val gradient = if (enabled) {
                Brush.radialGradient(
                    listOf(lerp(primary, Color.Magenta, animatedFraction?.value ?: 0.6f), primary),
                )
            } else {
                Brush.radialGradient(
                    listOf(
                        inversePrimary.copy(alpha = 0.08f),
                        inversePrimary.copy(alpha = 0.82f),
                    ),
                )
            }
            drawRect(gradient)
        },
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = scheme.onPrimary,
            disabledContentColor = scheme.onSurface.copy(alpha = 0.58f),
        ),
        content = content,
    )
}

@Preview(name = "Gradient buttons — light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Gradient buttons — dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GradientFilledIconButtonPreview() {
    AndroidHandbookAudioTheme(dynamicColor = false) {
        Surface {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GradientFilledIconButton(onClick = {}, modifier = Modifier.size(64.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow_24),
                        contentDescription = "Play",
                        modifier = Modifier.size(32.dp),
                    )
                }
                GradientFilledIconButton(onClick = {}, animateGradient = true) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pause_24),
                        contentDescription = "Pause",
                    )
                }
                GradientFilledIconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh_24),
                        contentDescription = "Retry",
                    )
                }
                GradientFilledIconButton(onClick = {}, enabled = false) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow_24),
                        contentDescription = "Disabled play",
                    )
                }
            }
        }
    }
}
