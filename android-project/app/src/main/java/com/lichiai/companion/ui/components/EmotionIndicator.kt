package com.lichiai.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lichiai.companion.data.EmotionState
import com.lichiai.companion.ui.theme.EmotionCalmColor
import com.lichiai.companion.ui.theme.EmotionCuriousColor
import com.lichiai.companion.ui.theme.EmotionEmpatheticColor
import com.lichiai.companion.ui.theme.EmotionHappyColor
import com.lichiai.companion.ui.theme.EmotionSurprisedColor
import com.lichiai.companion.ui.theme.TextPrimary

@Composable
fun EmotionIndicator(
    emotion: EmotionState,
    modifier: Modifier = Modifier
) {
    val (targetColor, labelBengali, labelEnglish) = when (emotion) {
        EmotionState.HAPPY -> Triple(EmotionHappyColor, "আনন্দিত", "Happy")
        EmotionState.CALM -> Triple(EmotionCalmColor, "শান্ত", "Calm")
        EmotionState.CURIOUS -> Triple(EmotionCuriousColor, "কৌতূহলী", "Curious")
        EmotionState.SURPRISED -> Triple(EmotionSurprisedColor, "বিস্মিত", "Surprised")
        EmotionState.EMPATHETIC -> Triple(EmotionEmpatheticColor, "সহানুভূতিশীল", "Empathetic")
    }

    val animatedColor by animateColorAsState(targetValue = targetColor, label = "emotionColor")

    Row(
        modifier = modifier
            .background(Color(0xFF14192B), RoundedCornerShape(20.dp))
            .border(1.dp, animatedColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(animatedColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$labelBengali ($labelEnglish)",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
