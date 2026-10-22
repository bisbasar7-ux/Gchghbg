package com.lichiai.companion.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lichiai.companion.ui.theme.LichiCyan

@Composable
fun VoiceWaveformView(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 7,
    maxBarHeight: Dp = 32.dp,
    barWidth: Dp = 4.dp,
    color: Color = LichiCyan
) {
    val transition = rememberInfiniteTransition(label = "waveform")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val animHeight by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = if (isActive) (0.4f + (i % 3) * 0.3f) else 0.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 350 + i * 80,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            val currentHeight = if (isActive) maxBarHeight * animHeight else 4.dp

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(currentHeight)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}
