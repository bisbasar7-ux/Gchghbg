package com.lichiai.companion.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lichiai.companion.data.EmotionState
import com.lichiai.companion.ui.components.EmotionIndicator
import com.lichiai.companion.ui.components.LichiCharacter3DView
import com.lichiai.companion.ui.components.VoiceWaveformView
import com.lichiai.companion.ui.theme.BackgroundDark
import com.lichiai.companion.ui.theme.BorderDark
import com.lichiai.companion.ui.theme.LichiCyan
import com.lichiai.companion.ui.theme.LichiPink
import com.lichiai.companion.ui.theme.SurfaceDark
import com.lichiai.companion.ui.theme.SurfaceLightDark
import com.lichiai.companion.ui.theme.TextPrimary
import com.lichiai.companion.ui.theme.TextSecondary
import com.lichiai.companion.voice.CallState

@Composable
fun CallScreen(
    callState: CallState,
    emotion: EmotionState,
    callDurationSeconds: Long,
    isMuted: Boolean,
    onStartCall: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenApiSettings: () -> Unit,
    voiceSpeed: Float,
    voicePitch: Float,
    onVoiceSpeedChange: (Float) -> Unit,
    onVoicePitchChange: (Float) -> Unit
) {
    var showVoiceSettings by remember { mutableStateOf(false) }

    val formattedDuration = remember(callDurationSeconds) {
        val minutes = callDurationSeconds / 60
        val seconds = callDurationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    val (statusLabel, statusColor) = when (callState) {
        CallState.DISCONNECTED -> Pair("কল প্রস্তুত (Ready)", Color(0xFF64748B))
        CallState.CONNECTING -> Pair("সংযুক্ত হচ্ছে... (Connecting)", LichiCyan)
        CallState.CONNECTED -> Pair("সংযুক্ত (Connected)", Color(0xFF00E676))
        CallState.LISTENING -> Pair("শুনছি... বলো (Listening)", LichiPink)
        CallState.THINKING -> Pair("ভাবছি... (Thinking)", Color(0xFFFFB300))
        CallState.SPEAKING -> Pair("কথা বলছি... (Speaking)", LichiCyan)
    }

    val isCallOngoing = callState != CallState.DISCONNECTED

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B0D19),
                        Color(0xFF070912),
                        Color(0xFF05060C)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Call Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "লিচির সঙ্গে কথা বলো",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isCallOngoing) "লাইভ রিয়েল-টাইম কল • $formattedDuration" else "Real-time AI Calling Companion",
                        color = if (isCallOngoing) LichiCyan else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isCallOngoing) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmotionIndicator(emotion = emotion)
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onOpenApiSettings,
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceDark, CircleShape)
                            .border(1.dp, LichiCyan.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Gemini API Settings",
                            tint = LichiCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { showVoiceSettings = !showVoiceSettings },
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceDark, CircleShape)
                            .border(1.dp, BorderDark, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Voice settings",
                            tint = if (showVoiceSettings) LichiCyan else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Quick Voice Adjustments Overlay
            AnimatedVisibility(
                visible = showVoiceSettings,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark.copy(alpha = 0.95f))
                        .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "লিচির মিষ্টি কণ্ঠস্বর কাস্টমাইজ করুন",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "কথা বলার গতি (Speed): ${(voiceSpeed * 10).toInt() / 10.0}x",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Slider(
                            value = voiceSpeed,
                            onValueChange = onVoiceSpeedChange,
                            valueRange = 0.8f..1.3f,
                            colors = SliderDefaults.colors(thumbColor = LichiCyan, activeTrackColor = LichiCyan)
                        )

                        Text(
                            text = "কণ্ঠের সুর (Pitch): ${(voicePitch * 10).toInt() / 10.0}x",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Slider(
                            value = voicePitch,
                            onValueChange = onVoicePitchChange,
                            valueRange = 0.9f..1.4f,
                            colors = SliderDefaults.colors(thumbColor = LichiCyan, activeTrackColor = LichiCyan)
                        )
                    }
                }
            }

            // Center: 3D Character Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LichiCharacter3DView(
                    emotion = emotion,
                    isSpeaking = callState == CallState.SPEAKING,
                    modifier = Modifier.fillMaxSize()
                )

                // Live Calling Status & Waveform Indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceDark.copy(alpha = 0.9f))
                        .border(1.dp, BorderDark, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VoiceWaveformView(
                            isActive = callState == CallState.LISTENING || callState == CallState.SPEAKING,
                            barCount = 6,
                            maxBarHeight = 18.dp,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = statusLabel,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom Call Action Controls
            if (!isCallOngoing) {
                // One Prominent Start Call Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Button(
                        onClick = onStartCall,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(62.dp),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LichiCyan
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = "Start Call",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "কল শুরু করুন (Start Call)",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "এক ট্যাপেই বাংলায় সার্বক্ষণিক হ্যান্ডস-ফ্রি কথোপকথন",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            } else {
                // Active Call Controls: End Call, Mute
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 30.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute / Unmute Button
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(56.dp)
                            .background(if (isMuted) Color(0xFFFF5252) else SurfaceDark, CircleShape)
                            .border(1.dp, BorderDark, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color.White else LichiCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Obvious End Call Button (Red Circle)
                    Button(
                        onClick = onEndCall,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }
    }
}
