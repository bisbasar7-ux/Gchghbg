package com.lichiai.companion.ui.screens

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lichiai.companion.ui.theme.BackgroundDark
import com.lichiai.companion.ui.theme.BorderDark
import com.lichiai.companion.ui.theme.LichiCyan
import com.lichiai.companion.ui.theme.LichiPink
import com.lichiai.companion.ui.theme.SurfaceDark
import com.lichiai.companion.ui.theme.SurfaceLightDark
import com.lichiai.companion.ui.theme.TextMuted
import com.lichiai.companion.ui.theme.TextPrimary
import com.lichiai.companion.ui.theme.TextSecondary

@Composable
fun CameraVisionScreen(
    hasCameraPermission: Boolean,
    isCameraActive: Boolean,
    capturedBitmap: Bitmap?,
    capturedBase64: String?,
    isAnalyzing: Boolean,
    analysisResult: String?,
    identifiedObjects: List<String>,
    onRequestPermission: () -> Unit,
    onStartCamera: (PreviewView) -> Unit,
    onStopCamera: () -> Unit,
    onCaptureFrame: () -> Unit,
    onDiscardCapture: () -> Unit,
    onAnalyzeImage: (prompt: String) -> Unit,
    onReplayAudio: (String) -> Unit
) {
    var customQuestion by remember { mutableStateOf("এই ছবিতে কী দেখা যাচ্ছে বাংলায় বিস্তারিত বলুন।") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(bottom = 80.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ক্যামেরা ভিশন (Camera Vision)",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "User-Authorized Gemini Multimodal Vision",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            if (isCameraActive) {
                IconButton(onClick = onStopCamera) {
                    Icon(Icons.Default.VideocamOff, contentDescription = "Stop Camera", tint = LichiPink)
                }
            }
        }

        // Camera Preview / Snapshot Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                // If permission is missing
                !hasCameraPermission -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "ক্যামেরা অনুমতি প্রয়োজন",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "লিচিকে কোনো বস্তু বা লেখা দেখাতে ক্যামেরার অনুমতি দিন। কোনো ছবি আপনার অনুমতি ছাড়া আপলোড হবে না।",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = LichiCyan)
                        ) {
                            Text("অনুমতি দিন (Grant Permission)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // If user captured a preview image (show preview before upload!)
                capturedBitmap != null -> {
                    Image(
                        bitmap = capturedBitmap.asImageBitmap(),
                        contentDescription = "Captured preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Discard button
                    IconButton(
                        onClick = onDiscardCapture,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(Color(0x99000000), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Discard snapshot", tint = Color.White)
                    }
                }

                // If Camera is Active
                isCameraActive -> {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { previewView ->
                                onStartCamera(previewView)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Camera is Idle (Explicit start required)
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Camera Ready",
                            tint = LichiCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ক্যামেরা বন্ধ রয়েছে",
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                // Trigger camera start
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LichiCyan)
                        ) {
                            Text("ক্যামেরা চালু করুন", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Camera Action Buttons
        if (hasCameraPermission) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (capturedBitmap == null && isCameraActive) {
                    Button(
                        onClick = onCaptureFrame,
                        colors = ButtonDefaults.buttonColors(containerColor = LichiCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = "Capture Frame", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ছবি তুলুন (Take Snapshot)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else if (capturedBitmap != null) {
                    Button(
                        onClick = onDiscardCapture,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retake", tint = TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("পুনরায় তুলুন (Retake)", color = TextSecondary)
                    }
                }
            }
        }

        // Question Input & Analysis Trigger (Only when image is ready for review)
        if (capturedBitmap != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = customQuestion,
                    onValueChange = { customQuestion = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("লিচিকে কী প্রশ্ন করতে চান?", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LichiCyan,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onAnalyzeImage(customQuestion) },
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LichiCyan)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("বিশ্লেষণ চলছে...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Analyze", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ছবি আপলোড ও বিশ্লেষণ করুন", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Analysis Output Card
        if (analysisResult != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, LichiCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "লিচির ভিশন রিপোর্ট",
                            color = LichiCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onReplayAudio(analysisResult) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Audio", tint = LichiCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = analysisResult,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    if (identifiedObjects.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "শনাক্তকৃত বিষয়বস্তু:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            identifiedObjects.forEach { obj ->
                                Box(
                                    modifier = Modifier
                                        .background(SurfaceLightDark, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = obj, color = LichiCyan, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
