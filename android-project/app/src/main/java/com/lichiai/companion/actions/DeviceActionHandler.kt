package com.lichiai.companion.actions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import com.lichiai.companion.data.DeviceActionRequest
import com.lichiai.companion.data.DeviceActionType

sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Failure(val error: String) : ActionResult()
    data class RequiresConfirmation(val action: DeviceActionRequest) : ActionResult()
}

/**
 * DeviceActionHandler manages execution of system actions requested by Lichi AI.
 * Enforces Android security sandbox: never bypasses permission prompts, screen locks, or hidden services.
 */
class DeviceActionHandler(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var isTorchOn: Boolean = false

    fun executeAction(action: DeviceActionRequest, isUserConfirmed: Boolean = false): ActionResult {
        if (action.requiresConfirmation && !isUserConfirmed) {
            return ActionResult.RequiresConfirmation(action)
        }

        return when (action.type) {
            DeviceActionType.OPEN_APP -> openApplication(action.target)
            DeviceActionType.OPEN_SETTINGS -> openSettingsScreen(action.target)
            DeviceActionType.TOGGLE_FLASHLIGHT -> toggleFlashlight(action.parameters["state"]?.toBooleanStrictOrNull())
            DeviceActionType.SET_VOLUME -> setDeviceVolume(action.parameters["level"]?.toIntOrNull() ?: 50)
            DeviceActionType.SET_ALARM -> setAlarm(
                hour = action.parameters["hour"]?.toIntOrNull() ?: 8,
                minute = action.parameters["minute"]?.toIntOrNull() ?: 0,
                message = action.parameters["message"] ?: "Lichi AI Alarm"
            )
            DeviceActionType.SEARCH_WEB -> searchWeb(action.target)
            DeviceActionType.UNKNOWN -> ActionResult.Failure("Unrecognized device action request.")
        }
    }

    private fun openApplication(packageNameOrName: String): ActionResult {
        val targetPackage = when (packageNameOrName.lowercase()) {
            "youtube" -> "com.google.android.youtube"
            "chrome" -> "com.android.chrome"
            "whatsapp" -> "com.whatsapp"
            "maps" -> "com.google.android.apps.maps"
            "calculator" -> "com.google.android.calculator"
            "clock" -> "com.google.android.deskclock"
            else -> packageNameOrName
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ActionResult.Success("Successfully launched application: $packageNameOrName")
        } else {
            // Offer to open Play Store if app is not installed
            try {
                val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$targetPackage")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(storeIntent)
                ActionResult.Success("App not installed locally. Opened Google Play Store to install.")
            } catch (e: Exception) {
                ActionResult.Failure("Application $packageNameOrName is not installed on this device.")
            }
        }
    }

    private fun openSettingsScreen(settingType: String): ActionResult {
        val intentAction = when (settingType.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(intentAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Opened Android $settingType settings.")
        } catch (e: Exception) {
            ActionResult.Failure("Could not open settings: ${e.localizedMessage}")
        }
    }

    private fun toggleFlashlight(forceState: Boolean?): ActionResult {
        if (cameraManager == null) {
            return ActionResult.Failure("Flashlight hardware service unavailable.")
        }

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionResult.Failure("No rear camera with flash found on this device.")

            val newState = forceState ?: !isTorchOn
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, newState)
                isTorchOn = newState
                val statusText = if (newState) "turned ON" else "turned OFF"
                ActionResult.Success("Flashlight has been $statusText.")
            } else {
                ActionResult.Failure("Flashlight control requires Android 6.0+.")
            }
        } catch (e: CameraAccessException) {
            ActionResult.Failure("Flashlight access error: ${e.localizedMessage}")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to toggle flashlight: ${e.localizedMessage}")
        }
    }

    private fun setDeviceVolume(percent: Int): ActionResult {
        if (audioManager == null) return ActionResult.Failure("Audio manager service is unavailable.")

        return try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val clamped = percent.coerceIn(0, 100)
            val targetVolume = (maxVolume * (clamped / 100.0f)).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, AudioManager.FLAG_SHOW_UI)
            ActionResult.Success("Media volume adjusted to $clamped%.")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to adjust volume: ${e.localizedMessage}")
        }
    }

    private fun setAlarm(hour: Int, minute: Int, message: String): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeString = String.format("%02d:%02d", hour, minute)
            ActionResult.Success("Configured alarm for $timeString with label: $message.")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to schedule alarm: ${e.localizedMessage}")
        }
    }

    private fun searchWeb(query: String): ActionResult {
        return try {
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(searchIntent)
            ActionResult.Success("Searched web for: $query")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to execute web search: ${e.localizedMessage}")
        }
    }
}
