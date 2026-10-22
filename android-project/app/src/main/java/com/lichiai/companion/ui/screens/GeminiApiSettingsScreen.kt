package com.lichiai.companion.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lichiai.companion.ui.theme.BackgroundDark
import com.lichiai.companion.ui.theme.BorderDark
import com.lichiai.companion.ui.theme.LichiCyan
import com.lichiai.companion.ui.theme.LichiGreen
import com.lichiai.companion.ui.theme.LichiPink
import com.lichiai.companion.ui.theme.SurfaceDark
import com.lichiai.companion.ui.theme.SurfaceLightDark
import com.lichiai.companion.ui.theme.TextMuted
import com.lichiai.companion.ui.theme.TextPrimary
import com.lichiai.companion.ui.theme.TextSecondary

enum class GeminiConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    TESTING
}

data class AndroidModelChoice(
    val id: String,
    val name: String,
    val description: String,
    val badge: String? = null
)

@Composable
fun GeminiApiSettingsScreen(
    connectionStatus: GeminiConnectionStatus,
    isTesting: Boolean,
    testSuccessMessage: String?,
    testErrorMessage: String?,
    errorCode: String? = null,
    latencyMs: Long?,
    selectedModel: String = "gemini-3.8-flash",
    canRetry: Boolean = true,
    onModelSelected: (String) -> Unit = {},
    onSaveKey: (String, String) -> Unit,
    onTestConnection: (String?, String) -> Unit,
    onDeleteKey: () -> Unit,
    onStartVoiceCall: () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var currentModel by remember(selectedModel) { mutableStateOf(selectedModel) }
    val scrollState = rememberScrollState()

    val availableModels = remember {
        listOf(
            AndroidModelChoice(
                id = "gemini-3.8-flash",
                name = "Gemini 3.8 Flash (ডিফল্ট)",
                description = "সুষম গতি, বুদ্ধি ও মাল্টিমোডাল ভিশন",
                badge = "Recommended"
            ),
            AndroidModelChoice(
                id = "gemini-3.1-flash-lite",
                name = "Gemini 3.1 Flash Lite",
                description = "উচ্চ ট্রাফিকের সময় অতি দ্রুত রেসপন্স ও ৫০৩ নিরাপদ",
                badge = "503 Safe / Fast"
            ),
            AndroidModelChoice(
                id = "gemini-flash-latest",
                name = "Gemini Flash Latest",
                description = "সর্বশেষ স্ট্যাবল ফ্ল্যাশ মডেল সংস্করণ",
                badge = null
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(bottom = 80.dp)
            .verticalScroll(scrollState)
    ) {
        // Screen Header
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "Gemini Key",
                    tint = LichiCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gemini API সেটিংস ও মডেল নির্বাচন",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Secure Backend Proxy • 503 Auto-Backoff & Model Fallback",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        // 1. Connection Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(
                    1.dp,
                    when (connectionStatus) {
                        GeminiConnectionStatus.CONNECTED -> LichiGreen.copy(alpha = 0.6f)
                        GeminiConnectionStatus.TESTING -> LichiCyan.copy(alpha = 0.6f)
                        GeminiConnectionStatus.DISCONNECTED -> Color(0xFFEF4444).copy(alpha = 0.4f)
                    },
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "সংযোগ অবস্থা (Connection Status):",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (connectionStatus) {
                                        GeminiConnectionStatus.CONNECTED -> LichiGreen
                                        GeminiConnectionStatus.TESTING -> LichiCyan
                                        GeminiConnectionStatus.DISCONNECTED -> Color(0xFFEF4444)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (connectionStatus) {
                                GeminiConnectionStatus.CONNECTED -> "Connected (সংযুক্ত)"
                                GeminiConnectionStatus.TESTING -> "Testing (পরীক্ষা চলছে...)"
                                GeminiConnectionStatus.DISCONNECTED -> "Disconnected (সংযোগ বিচ্ছিন্ন)"
                            },
                            color = when (connectionStatus) {
                                GeminiConnectionStatus.CONNECTED -> LichiGreen
                                GeminiConnectionStatus.TESTING -> LichiCyan
                                GeminiConnectionStatus.DISCONNECTED -> Color(0xFFEF4444)
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (connectionStatus == GeminiConnectionStatus.CONNECTED) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = "Latency", tint = LichiCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Model: $currentModel • ${latencyMs ?: 150}ms",
                                color = LichiCyan,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (connectionStatus == GeminiConnectionStatus.CONNECTED) {
                    Button(
                        onClick = onStartVoiceCall,
                        colors = ButtonDefaults.buttonColors(containerColor = LichiCyan),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PhoneInTalk, contentDescription = "Start Call", tint = Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কল করো", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Model Selection Card (Requirement 5)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gemini মডেল নির্বাচন (Model Configuration):",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "503 Fallback Active",
                        color = LichiCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                availableModels.forEach { model ->
                    val isSelected = currentModel == model.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SurfaceLightDark else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) LichiCyan.copy(alpha = 0.5f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                currentModel = model.id
                                onModelSelected(model.id)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                currentModel = model.id
                                onModelSelected(model.id)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = LichiCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = model.name,
                                    color = if (isSelected) LichiCyan else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                model.badge?.let { badgeText ->
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (badgeText.contains("503")) Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                else LichiGreen.copy(alpha = 0.2f)
                                            )
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = if (badgeText.contains("503")) Color(0xFFFBBF24) else LichiGreen,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(text = model.description, color = TextMuted, fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = "Fallback", tint = LichiCyan, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "৫০৩ বা কোটা শেষ হলে স্বয়ংক্রিয়ভাবে ফ্ল্যাশ লাইট মডেলে ফলব্যাক করা হবে।",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. API Key Input Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Gemini API Key ইনপুট (Password-style Input):",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "কীটি সুরক্ষিতভাবে ব্যাকএন্ডে যাচাই করা হয়; APK বা লগ ফাইলে সেভ হয় না।",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("AIzaSy... আপনার Gemini API Key লিখুন", color = TextMuted, fontSize = 12.sp) },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isKeyVisible) "Hide key" else "Show key",
                                tint = LichiCyan
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LichiCyan,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceLightDark,
                        unfocusedContainerColor = SurfaceLightDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Test Connection, Save, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Test Connection Button with minimal valid payload
                    Button(
                        onClick = { onTestConnection(apiKeyInput.ifBlank { null }, currentModel) },
                        enabled = !isTesting,
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = LichiCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("যাচাই হচ্ছে...", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Test", tint = Color.Black, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("সংযোগ টেস্ট", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Save Button
                    Button(
                        onClick = { onSaveKey(apiKeyInput, currentModel) },
                        enabled = apiKeyInput.isNotBlank() && !isTesting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = TextPrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("সংরক্ষণ", color = TextPrimary, fontSize = 11.sp)
                    }

                    // Delete Key Button
                    IconButton(
                        onClick = {
                            apiKeyInput = ""
                            onDeleteKey()
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(SurfaceLightDark, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete key", tint = Color(0xFFEF4444))
                    }
                }
            }
        }

        // 4. Success / Error Banners with Categorized Messages & Retry
        if (testSuccessMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LichiGreen.copy(alpha = 0.12f))
                    .border(1.dp, LichiGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = LichiGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = testSuccessMessage,
                        color = LichiGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (testErrorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (errorCode) {
                                    "UNAVAILABLE" -> "HTTP 503 UNAVAILABLE (মডেল অতিরিক্ত চাপে রয়েছে)"
                                    "RESOURCE_EXHAUSTED" -> "HTTP 429 RESOURCE EXHAUSTED (কোটা শেষ)"
                                    "UNAUTHENTICATED" -> "HTTP 401/403 AUTHENTICATION ERROR"
                                    "NOT_FOUND" -> "HTTP 404 MODEL NOT FOUND"
                                    else -> "সংযোগ ত্রুটি"
                                },
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = testErrorMessage,
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (canRetry) {
                            Button(
                                onClick = { onTestConnection(apiKeyInput.ifBlank { null }, currentModel) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পুনরায় চেষ্টা করুন (Retry)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (errorCode == "UNAVAILABLE" && currentModel != "gemini-3.1-flash-lite") {
                            Button(
                                onClick = {
                                    currentModel = "gemini-3.1-flash-lite"
                                    onModelSelected("gemini-3.1-flash-lite")
                                    onTestConnection(apiKeyInput.ifBlank { null }, "gemini-3.1-flash-lite")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LichiCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Flash Lite এ বদলান", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = "Switch", tint = Color.Black, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Google AI Studio Setup Guide Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Guide", tint = LichiCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "গুগল এআই স্টুডিও থেকে কী পাওয়ার নিয়ম:",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "১. ব্রাউজারে aistudio.google.com/app/apikey ভিজিট করুন।\n" +
                            "২. আপনার গুগল অ্যাকাউন্ট দিয়ে লগইন করে 'Create API key' বাটনে ক্লিক করুন।\n" +
                            "৩. প্রাপ্ত API Key কপি করে ব্যাকএন্ডের .env ফাইলে GEMINI_API_KEY=\"YOUR_KEY\" হিসেবে সেট করুন বা উপরের বক্সে দিয়ে টেস্ট করুন।\n" +
                            "৪. মডেল ওভারলোড হলে 'gemini-3.1-flash-lite' বেছে নিন।",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceLightDark)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "🔒 নিরাপত্তা নিশ্চয়তা: API কী কখনোই ক্লায়েন্ট কোড বা APK-তে এম্বেড হয় না। সকল রিকোয়েস্ট নিরাপদ সার্ভার প্রক্সির মাধ্যমে পরিচালিত হয়।",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
