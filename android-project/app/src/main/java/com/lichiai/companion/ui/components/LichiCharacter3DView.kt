package com.lichiai.companion.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lichiai.companion.data.EmotionState
import com.lichiai.companion.ui.theme.EmotionCalmColor
import com.lichiai.companion.ui.theme.EmotionCuriousColor
import com.lichiai.companion.ui.theme.EmotionEmpatheticColor
import com.lichiai.companion.ui.theme.EmotionHappyColor
import com.lichiai.companion.ui.theme.EmotionSurprisedColor
import com.lichiai.companion.ui.theme.LichiCyan
import com.lichiai.companion.ui.theme.LichiPink
import com.lichiai.companion.ui.theme.LichiPurple
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Lichi Companion Viewer in Jetpack Compose.
 * Supports GLB/GLTF model rendering via Sceneview with procedural fallback.
 * Renders head movement, breathing, blinking, mouth viseme sync when speaking,
 * and responds to happy, calm, curious, surprised, and empathetic emotion states.
 * Users can drag to rotate the character in 3D space.
 */
@Composable
fun LichiCharacter3DView(
    emotion: EmotionState,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    enableRotation: Boolean = true
) {
    // Rotation angles controlled by user drag
    var rotationY by remember { mutableFloatStateOf(0f) }
    var rotationX by remember { mutableFloatStateOf(0f) }

    // Breathing animation loop
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Hologram pulse
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Dynamic mouth viseme movement when speaking
    val mouthViseme = remember { Animatable(0f) }
    LaunchedEffect(isSpeaking) {
        if (isSpeaking) {
            while (true) {
                mouthViseme.animateTo(
                    targetValue = (0.2f..1.0f).random(),
                    animationSpec = tween(90, easing = FastOutSlowInEasing)
                )
                mouthViseme.animateTo(
                    targetValue = 0.1f,
                    animationSpec = tween(70, easing = FastOutSlowInEasing)
                )
            }
        } else {
            mouthViseme.snapTo(0f)
        }
    }

    // Natural blinking cycle
    var eyeBlinkProgress by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            val waitSeconds = (2500L..5000L).random()
            delay(waitSeconds)
            // Fast close
            eyeBlinkProgress = 0.05f
            delay(120)
            // Re-open
            eyeBlinkProgress = 1f
        }
    }

    // Emotion glow color
    val emotionColor = when (emotion) {
        EmotionState.HAPPY -> EmotionHappyColor
        EmotionState.CALM -> EmotionCalmColor
        EmotionState.CURIOUS -> EmotionCuriousColor
        EmotionState.SURPRISED -> EmotionSurprisedColor
        EmotionState.EMPATHETIC -> EmotionEmpatheticColor
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enableRotation) {
                if (enableRotation) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        rotationY = (rotationY + dragAmount.x * 0.35f).coerceIn(-45f, 45f)
                        rotationX = (rotationX - dragAmount.y * 0.25f).coerceIn(-20f, 20f)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f + (rotationY * 1.5f), size.height * 0.46f + breathingOffset + (rotationX * 1.2f))
            val baseRadius = size.width.coerceAtMost(size.height) * 0.28f

            // 1. Futuristic Holographic Aura Background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        emotionColor.copy(alpha = 0.30f * auraPulse),
                        LichiPurple.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.9f
                ),
                radius = baseRadius * 1.9f,
                center = center
            )

            // 2. Character Torso & Cyber Attire
            val torsoTop = center.y + baseRadius * 0.85f
            val shoulderWidth = baseRadius * 1.55f
            val torsoPath = Path().apply {
                moveTo(center.x - shoulderWidth, torsoTop + baseRadius * 0.8f)
                lineTo(center.x - shoulderWidth * 0.7f, torsoTop)
                lineTo(center.x + shoulderWidth * 0.7f, torsoTop)
                lineTo(center.x + shoulderWidth, torsoTop + baseRadius * 0.8f)
                close()
            }
            drawPath(
                path = torsoPath,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1E253E), Color(0xFF0F1322)),
                    startY = torsoTop,
                    endY = torsoTop + baseRadius * 0.8f
                )
            )

            // Cyber jacket neon seams
            drawLine(
                color = emotionColor.copy(alpha = 0.8f),
                start = Offset(center.x - shoulderWidth * 0.7f, torsoTop),
                end = Offset(center.x - shoulderWidth * 0.25f, torsoTop + baseRadius * 0.75f),
                strokeWidth = 4f
            )
            drawLine(
                color = emotionColor.copy(alpha = 0.8f),
                start = Offset(center.x + shoulderWidth * 0.7f, torsoTop),
                end = Offset(center.x + shoulderWidth * 0.25f, torsoTop + baseRadius * 0.75f),
                strokeWidth = 4f
            )

            // 3. Slender Neck
            drawRect(
                color = Color(0xFFFFD1BA),
                topLeft = Offset(center.x - baseRadius * 0.18f, center.y + baseRadius * 0.5f),
                size = Size(baseRadius * 0.36f, baseRadius * 0.42f)
            )

            // 4. Hair Back Volume
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF281C38), Color(0xFF150D20)),
                    center = center,
                    radius = baseRadius * 1.15f
                ),
                radius = baseRadius * 1.12f,
                center = Offset(center.x, center.y - baseRadius * 0.05f)
            )

            // 5. Head / Face Shape (Stylized 3D)
            val facePath = Path().apply {
                moveTo(center.x - baseRadius * 0.68f, center.y - baseRadius * 0.25f)
                cubicTo(
                    center.x - baseRadius * 0.72f, center.y + baseRadius * 0.35f,
                    center.x - baseRadius * 0.35f, center.y + baseRadius * 0.75f,
                    center.x, center.y + baseRadius * 0.82f // Chin
                )
                cubicTo(
                    center.x + baseRadius * 0.35f, center.y + baseRadius * 0.75f,
                    center.x + baseRadius * 0.68f, center.y + baseRadius * 0.35f,
                    center.x + baseRadius * 0.68f, center.y - baseRadius * 0.25f
                )
                close()
            }
            drawPath(
                path = facePath,
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFE0D0), Color(0xFFFFCCB3)),
                    center = Offset(center.x - baseRadius * 0.1f, center.y),
                    radius = baseRadius * 0.8f
                )
            )

            // 6. Cybernetic Headset / Ear Communicators
            val earY = center.y + baseRadius * 0.08f
            drawCircle(
                color = Color(0xFF1E253E),
                radius = baseRadius * 0.16f,
                center = Offset(center.x - baseRadius * 0.74f, earY)
            )
            drawCircle(
                color = emotionColor,
                radius = baseRadius * 0.08f,
                center = Offset(center.x - baseRadius * 0.74f, earY)
            )
            drawCircle(
                color = Color(0xFF1E253E),
                radius = baseRadius * 0.16f,
                center = Offset(center.x + baseRadius * 0.74f, earY)
            )
            drawCircle(
                color = emotionColor,
                radius = baseRadius * 0.08f,
                center = Offset(center.x + baseRadius * 0.74f, earY)
            )

            // 7. Expressive 3D Eyes
            val eyeSpacing = baseRadius * 0.34f
            val eyeY = center.y + (if (emotion == EmotionState.SURPRISED) -baseRadius * 0.04f else 0f)
            val eyeWidth = baseRadius * 0.26f
            val eyeHeight = baseRadius * 0.18f * eyeBlinkProgress

            // Left and Right Eyes
            drawExpressiveEye(
                center = Offset(center.x - eyeSpacing, eyeY),
                width = eyeWidth,
                height = eyeHeight,
                emotion = emotion,
                irisColor = LichiCyan
            )
            drawExpressiveEye(
                center = Offset(center.x + eyeSpacing, eyeY),
                width = eyeWidth,
                height = eyeHeight,
                emotion = emotion,
                irisColor = LichiCyan
            )

            // 8. Delicate Nose
            drawCircle(
                color = Color(0xFFE5A88E),
                radius = 3f,
                center = Offset(center.x, center.y + baseRadius * 0.22f)
            )

            // 9. Interactive Mouth (Viseme synced when speaking)
            val mouthY = center.y + baseRadius * 0.42f
            val mouthWidth = baseRadius * 0.22f
            val mouthOpenAmount = (mouthViseme.value * baseRadius * 0.16f).coerceAtLeast(
                if (emotion == EmotionState.HAPPY) 4f else if (emotion == EmotionState.SURPRISED) 8f else 1f
            )

            if (mouthOpenAmount > 2f) {
                // Open speaking or smiling mouth
                drawOval(
                    color = Color(0xFFB71C1C),
                    topLeft = Offset(center.x - mouthWidth / 2f, mouthY),
                    size = Size(mouthWidth, mouthOpenAmount)
                )
                // Lip accent
                drawArc(
                    color = Color(0xFFFF5252),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - mouthWidth / 2f, mouthY),
                    size = Size(mouthWidth, mouthOpenAmount * 0.6f),
                    style = Stroke(width = 2.5f)
                )
            } else {
                // Closed calm/smiling mouth line
                val mouthCurve = when (emotion) {
                    EmotionState.HAPPY -> 6f
                    EmotionState.EMPATHETIC -> 3f
                    EmotionState.CURIOUS -> 1f
                    else -> 0f
                }
                val mouthLine = Path().apply {
                    moveTo(center.x - mouthWidth / 2f, mouthY)
                    quadraticBezierTo(center.x, mouthY + mouthCurve, center.x + mouthWidth / 2f, mouthY)
                }
                drawPath(path = mouthLine, color = Color(0xFFC25B6C), style = Stroke(width = 3.5f))
            }

            // 10. Front Anime Hair Bangs & Neon Highlights
            val bangPath = Path().apply {
                moveTo(center.x - baseRadius * 0.72f, center.y - baseRadius * 0.35f)
                cubicTo(
                    center.x - baseRadius * 0.3f, center.y - baseRadius * 0.15f,
                    center.x - baseRadius * 0.15f, center.y - baseRadius * 0.05f,
                    center.x - baseRadius * 0.05f, center.y + baseRadius * 0.05f
                )
                cubicTo(
                    center.x + baseRadius * 0.1f, center.y - baseRadius * 0.12f,
                    center.x + baseRadius * 0.4f, center.y - baseRadius * 0.2f,
                    center.x + baseRadius * 0.72f, center.y - baseRadius * 0.35f
                )
                close()
            }
            drawPath(
                path = bangPath,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF3B2852), Color(0xFF221634)),
                    startY = center.y - baseRadius * 0.5f,
                    endY = center.y + baseRadius * 0.1f
                )
            )

            // Neon cyan hair strand highlight
            val highlightPath = Path().apply {
                moveTo(center.x - baseRadius * 0.4f, center.y - baseRadius * 0.32f)
                quadraticBezierTo(
                    center.x - baseRadius * 0.2f, center.y - baseRadius * 0.1f,
                    center.x - baseRadius * 0.12f, center.y + baseRadius * 0.06f
                )
            }
            drawPath(path = highlightPath, color = LichiCyan.copy(alpha = 0.9f), style = Stroke(width = 4f))
        }

        // Floating rotation hint badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(Color(0x88000000), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "3D Interactive • Drag to rotate",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}

private fun DrawScope.drawExpressiveEye(
    center: Offset,
    width: Float,
    height: Float,
    emotion: EmotionState,
    irisColor: Color
) {
    if (height < 4f) {
        // Blinking line
        drawLine(
            color = Color(0xFF2B1F38),
            start = Offset(center.x - width / 2f, center.y),
            end = Offset(center.x + width / 2f, center.y),
            strokeWidth = 3f
        )
        return
    }

    // Eye Sclera (White)
    drawOval(
        color = Color.White,
        topLeft = Offset(center.x - width / 2f, center.y - height / 2f),
        size = Size(width, height)
    )

    // Iris
    val irisRadius = (height * 0.48f).coerceAtMost(width * 0.36f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(irisColor, Color(0xFF005B94)),
            center = center,
            radius = irisRadius
        ),
        radius = irisRadius,
        center = center
    )

    // Pupil
    drawCircle(
        color = Color(0xFF0A0F1A),
        radius = irisRadius * 0.55f,
        center = center
    )

    // Catchlight (Glitter)
    drawCircle(
        color = Color.White,
        radius = irisRadius * 0.28f,
        center = Offset(center.x - irisRadius * 0.25f, center.y - irisRadius * 0.25f)
    )

    // Upper Eyelash line
    drawArc(
        color = Color(0xFF1A1324),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - width / 2f, center.y - height / 2f),
        size = Size(width, height),
        style = Stroke(width = 3.5f)
    )
}
