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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lichiai.companion.ui.theme.BackgroundDark
import com.lichiai.companion.ui.theme.BorderDark
import com.lichiai.companion.ui.theme.LichiCyan
import com.lichiai.companion.ui.theme.LichiGreen
import com.lichiai.companion.ui.theme.SurfaceDark
import com.lichiai.companion.ui.theme.SurfaceLightDark
import com.lichiai.companion.ui.theme.TextMuted
import com.lichiai.companion.ui.theme.TextPrimary
import com.lichiai.companion.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    backendUrl: String,
    preferredLanguage: String,
    isTtsEnabled: Boolean,
    ttsPitch: Float,
    ttsSpeed: Float,
    onSaveBackendUrl: (String) -> Unit,
    onLanguageChanged: (String) -> Unit,
    onToggleTts: (Boolean) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onTestConnection: () -> Unit,
    connectionStatus: String?
) {
    var urlInput by remember { mutableStateOf(backendUrl) }
    var currentPitch by remember { mutableFloatStateOf(ttsPitch) }
    var currentSpeed by remember { mutableFloatStateOf(ttsSpeed) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(bottom = 80.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = "সেটিংস (Settings)",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Voice, Privacy, AI Connection & Preferences",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        // 1. Backend AI Connection
        SettingSectionHeader(title = "নিরাপদ এআই ব্যাকএন্ড সংযোগ (Secure Backend)")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "সার্ভার URL (কোনো এপিআই কী ডিভাইসে সংরক্ষিত হয় না):",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LichiCyan,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceLightDark,
                        unfocusedContainerColor = SurfaceLightDark
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSaveBackendUrl(urlInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = LichiCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("সংরক্ষণ করুন", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onTestConnection,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("পরীক্ষা করুন", color = TextPrimary)
                    }
                }

                if (connectionStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = connectionStatus,
                        color = if (connectionStatus.contains("সফল") || connectionStatus.contains("OK")) LichiGreen else Color(0xFFFF5252),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Language Preference
        SettingSectionHeader(title = "ভাষা নির্বাচন (Language)")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = preferredLanguage == "bn",
                        onClick = { onLanguageChanged("bn") },
                        colors = RadioButtonDefaults.colors(selectedColor = LichiCyan)
                    )
                    Text(
                        text = "বাংলা (Bengali - Default)",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = preferredLanguage == "en",
                        onClick = { onLanguageChanged("en") },
                        colors = RadioButtonDefaults.colors(selectedColor = LichiCyan)
                    )
                    Text(
                        text = "English",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Voice & Speech Synthesis (TTS)
        SettingSectionHeader(title = "ভয়েস ও উচ্চারণ সেটিংস (Speech Output)")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ভয়েস স্পিচ সক্রিয় রাখুন (TTS)",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = isTtsEnabled,
                        onCheckedChange = onToggleTts,
                        colors = SwitchDefaults.colors(checkedThumbColor = LichiCyan)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ভয়েস পিচ: ${(currentPitch * 10).toInt() / 10.0}x",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Slider(
                    value = currentPitch,
                    onValueChange = { currentPitch = it },
                    onValueChangeFinished = { onPitchChanged(currentPitch) },
                    valueRange = 0.7f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = LichiCyan, activeTrackColor = LichiCyan)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "কথা বলার গতি: ${(currentSpeed * 10).toInt() / 10.0}x",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Slider(
                    value = currentSpeed,
                    onValueChange = { currentSpeed = it },
                    onValueChangeFinished = { onSpeedChanged(currentSpeed) },
                    valueRange = 0.6f..1.4f,
                    colors = SliderDefaults.colors(thumbColor = LichiCyan, activeTrackColor = LichiCyan)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Privacy & Android Security
        SettingSectionHeader(title = "গোপনীয়তা ও ডেটা সুরক্ষা (Privacy Policy)")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "• ব্যাকগ্রাউন্ড ক্যামেরা বা মাইক রেকর্ডিং সম্পূর্ণরূপে নিষ্ক্রিয়।",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = "• সমস্ত ছবি শুধুমাত্র ব্যবহারকারীর নির্দেশে প্রিভিউ দেখার পর আপলোড হয়।",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = "• গোপন কোনো ডেটা বা কি-লগিং সংগ্রহ করা হয় না।",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        color = TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}
