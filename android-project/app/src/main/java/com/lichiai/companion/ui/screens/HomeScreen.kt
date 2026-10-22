package com.lichiai.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lichiai.companion.R
import com.lichiai.companion.data.EmotionState
import com.lichiai.companion.ui.components.EmotionIndicator
import com.lichiai.companion.ui.components.LichiCharacter3DView
import com.lichiai.companion.ui.components.VoiceWaveformView
import com.lichiai.companion.ui.theme.BackgroundDark
import com.lichiai.companion.ui.theme.BorderDark
import com.lichiai.companion.ui.theme.LichiCyan
import com.lichiai.companion.ui.theme.LichiPink
import com.lichiai.companion.ui.theme.LichiPurple
import com.lichiai.companion.ui.theme.SurfaceDark
import com.lichiai.companion.ui.theme.TextPrimary
import com.lichiai.companion.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    emotion: EmotionState,
    isSpeaking: Boolean,
    isListening: Boolean,
    isProcessing: Boolean,
    statusText: String,
    onMicClick: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToVision: () -> Unit,
    onNavigateToActions: () -> Unit,
    onQuickPrompt: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LICHI AI",
                        color = LichiCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "3D Companion • Gemini Powered",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                EmotionIndicator(emotion = emotion)
            }

            // Large 3D Interactive Lichi Character Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                LichiCharacter3DView(
                    emotion = emotion,
                    isSpeaking = isSpeaking,
                    modifier = Modifier.fillMaxSize()
                )

                // Status Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .background(SurfaceDark.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                        .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VoiceWaveformView(
                            isActive = isListening || isSpeaking,
                            barCount = 5,
                            maxBarHeight = 16.dp,
                            barWidth = 3.dp,
                            color = if (isListening) LichiPink else LichiCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bengali Welcome Speech Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(SurfaceDark, Color(0xFF181F36))
                        )
                    )
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "স্বাগতম! আমি লিচি, আপনার এআই সঙ্গী।",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "কথা বলুন, ক্যামেরা দিয়ে কিছু দেখান অথবা মোবাইল নিয়ন্ত্রণ করার নির্দেশ দিন।",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Quick Prompt Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val prompts = listOf(
                    "কেমন আছো লিচি?",
                    "ইউটিউব খোলো",
                    "ফ্ল্যাশলাইট জ্বালাও"
                )
                prompts.forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = prompt,
                            color = LichiCyan,
                            fontSize = 11.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Main Mic & Quick Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Camera Vision Quick Action
                IconButton(
                    onClick = onNavigateToVision,
                    modifier = Modifier
                        .size(50.dp)
                        .background(SurfaceDark, CircleShape)
                        .border(1.dp, BorderDark, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Vision",
                        tint = LichiCyan
                    )
                }

                // Central Voice Input Button
                Button(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(72.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) LichiPink else LichiCyan
                    )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Chat Quick Action
                IconButton(
                    onClick = onNavigateToChat,
                    modifier = Modifier
                        .size(50.dp)
                        .background(SurfaceDark, CircleShape)
                        .border(1.dp, BorderDark, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Open Chat",
                        tint = LichiCyan
                    )
                }
            }
        }
    }
}
