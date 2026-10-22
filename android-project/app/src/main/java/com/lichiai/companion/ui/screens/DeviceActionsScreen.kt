package com.lichiai.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lichiai.companion.data.DeviceActionRequest
import com.lichiai.companion.data.DeviceActionType
import com.lichiai.companion.ui.theme.BackgroundDark
import com.lichiai.companion.ui.theme.BorderDark
import com.lichiai.companion.ui.theme.LichiAmber
import com.lichiai.companion.ui.theme.LichiCyan
import com.lichiai.companion.ui.theme.LichiGreen
import com.lichiai.companion.ui.theme.LichiPink
import com.lichiai.companion.ui.theme.SurfaceDark
import com.lichiai.companion.ui.theme.SurfaceLightDark
import com.lichiai.companion.ui.theme.TextMuted
import com.lichiai.companion.ui.theme.TextPrimary
import com.lichiai.companion.ui.theme.TextSecondary

@Composable
fun DeviceActionsScreen(
    onTriggerAction: (DeviceActionRequest) -> Unit,
    lastActionResultMessage: String?
) {
    var volumeLevel by remember { mutableFloatStateOf(60f) }
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
                text = "ডিভাইস কন্ট্রোল (Device Control)",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Approved, Permission-Aware Android APIs & Intents",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        // Security Notice Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, LichiGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = "Security", tint = LichiGreen)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "অ্যান্ড্রয়েড নিরাপত্তা নীতি বলবৎ",
                        color = LichiGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "গোপন নজরদারি বা অ্যাক্সেসিবিলিটি বাইপাস নিষিদ্ধ। সংবেদনশীল কাজের পূর্বে স্পষ্ট অনুমতি চাওয়া হয়।",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Last Execution Result
        if (lastActionResultMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceLightDark)
                    .padding(12.dp)
            ) {
                Text(
                    text = "ফলাফল: $lastActionResultMessage",
                    color = LichiCyan,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Quick App Launchers
        Text(
            text = "সমর্থিত অ্যাপ খুলুন (Open Apps)",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTile(
                title = "YouTube",
                icon = Icons.Default.Launch,
                tint = LichiPink,
                modifier = Modifier.weight(1f),
                onClick = {
                    onTriggerAction(
                        DeviceActionRequest(
                            type = DeviceActionType.OPEN_APP,
                            target = "com.google.android.youtube",
                            label = "ইউটিউব অ্যাপ ওপেন করুন",
                            explanation = "আপনার ডিভাইসে অফিসিয়াল YouTube অ্যাপ চালু করা হবে।",
                            requiresConfirmation = true
                        )
                    )
                }
            )

            ActionTile(
                title = "WhatsApp",
                icon = Icons.Default.Launch,
                tint = LichiGreen,
                modifier = Modifier.weight(1f),
                onClick = {
                    onTriggerAction(
                        DeviceActionRequest(
                            type = DeviceActionType.OPEN_APP,
                            target = "com.whatsapp",
                            label = "হোয়াটসঅ্যাপ খুলুন",
                            explanation = "আপনার মেসেজ দেখার জন্য WhatsApp খোলা হবে।",
                            requiresConfirmation = true
                        )
                    )
                }
            )

            ActionTile(
                title = "Chrome",
                icon = Icons.Default.Launch,
                tint = LichiCyan,
                modifier = Modifier.weight(1f),
                onClick = {
                    onTriggerAction(
                        DeviceActionRequest(
                            type = DeviceActionType.OPEN_APP,
                            target = "com.android.chrome",
                            label = "গুগল ক্রোম ব্রাউজার খুলুন",
                            explanation = "ইন্টারনেট ব্রাউজ করার জন্য Chrome চালু করা হবে।",
                            requiresConfirmation = true
                        )
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. System Settings Screens
        Text(
            text = "সিস্টেম সেটিংস (Android Settings)",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTile(
                title = "Wi-Fi",
                icon = Icons.Default.Wifi,
                tint = LichiCyan,
                modifier = Modifier.weight(1f),
                onClick = {
                    onTriggerAction(
                        DeviceActionRequest(
                            type = DeviceActionType.OPEN_SETTINGS,
                            target = "wifi",
                            label = "ওয়াই-ফাই সেটিংস খুলুন",
                            explanation = "নেটওয়ার্ক কনফিগার করতে Wi-Fi স্ক্রিন ওপেন হবে।",
                            requiresConfirmation = false
                        )
                    )
                }
            )

            ActionTile(
                title = "Bluetooth",
                icon = Icons.Default.Bluetooth,
                tint = LichiCyan,
                modifier = Modifier.weight(1f),
                onClick = {
                    onTriggerAction(
                        DeviceActionRequest(
                            type = DeviceActionType.OPEN_SETTINGS,
                            target = "bluetooth",
                            label = "ব্লুটুথ সেটিংস খুলুন",
                            explanation = "ডিভাইস পেয়ার করতে Bluetooth স্ক্রিন ওপেন হবে।",
                            requiresConfirmation = false
                        )
                    )
                }
            )

            ActionTile(
                title = "Display",
                icon = Icons.Default.Brightness6,
                tint = LichiAmber,
                modifier = Modifier.weight(1f),
                onClick = {
                    onTriggerAction(
                        DeviceActionRequest(
                            type = DeviceActionType.OPEN_SETTINGS,
                            target = "display",
                            label = "ডিসপ্লে সেটিংস খুলুন",
                            explanation = "ব্রাইটনেস ও স্ক্রিন টাইমআউট পরিবর্তনের জন্য সেটিংস ওপেন হবে।",
                            requiresConfirmation = false
                        )
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Hardware Toggles (Flashlight, Volume, Alarm)
        Text(
            text = "হার্ডওয়্যার ও সাউন্ড কন্ট্রোল",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashlightOn, contentDescription = "Flashlight", tint = LichiAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ফ্ল্যাশলাইট (Camera Torch)", color = TextPrimary, fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            onTriggerAction(
                                DeviceActionRequest(
                                    type = DeviceActionType.TOGGLE_FLASHLIGHT,
                                    label = "ফ্ল্যাশলাইট টগল করুন",
                                    explanation = "ক্যামেরা এলইডি ফ্ল্যাশ চালু বা বন্ধ করা হবে।",
                                    requiresConfirmation = false
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LichiAmber)
                    ) {
                        Text("টগল", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Volume slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = LichiCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("মিডিয়া ভলিউম: ${volumeLevel.toInt()}%", color = TextPrimary, fontSize = 14.sp)
                }

                Slider(
                    value = volumeLevel,
                    onValueChange = { volumeLevel = it },
                    onValueChangeFinished = {
                        onTriggerAction(
                            DeviceActionRequest(
                                type = DeviceActionType.SET_VOLUME,
                                label = "ভলিউম পরিবর্তন",
                                explanation = "মিডিয়া ভলিউম ${volumeLevel.toInt()}% এ সেট করা হবে।",
                                requiresConfirmation = false,
                                parameters = mapOf("level" to volumeLevel.toInt().toString())
                            )
                        )
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = LichiCyan,
                        activeTrackColor = LichiCyan,
                        inactiveTrackColor = SurfaceLightDark
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Alarm trigger
                Button(
                    onClick = {
                        onTriggerAction(
                            DeviceActionRequest(
                                type = DeviceActionType.SET_ALARM,
                                label = "সকাল ৮:০০ টায় অ্যালার্ম সেট করুন",
                                explanation = "অ্যান্ড্রয়েড ক্লক অ্যাপে সকাল ৮:০০ টায় অ্যালার্ম যোগ করা হবে।",
                                requiresConfirmation = true,
                                parameters = mapOf("hour" to "8", "minute" to "0", "message" to "লিচি এআই অ্যালার্ম")
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = "Alarm", tint = LichiCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সকাল ৮:০০ টায় অ্যালার্ম পরীক্ষা করুন", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
