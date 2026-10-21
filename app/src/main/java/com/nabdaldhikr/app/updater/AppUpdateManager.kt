package com.nabdaldhikr.app.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.nabdaldhikr.app.BuildConfig
import com.nabdaldhikr.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Data Model for Remote Update Information (version.json or GitHub Release).
 */
data class UpdateInfo(
    @SerializedName("versionCode")
    val versionCode: Int = 1,

    @SerializedName("versionName")
    val versionName: String = "1.0.0",

    @SerializedName("apkUrl")
    val apkUrl: String = "",

    @SerializedName("changelog")
    val changelog: String = "",

    @SerializedName("forceUpdate")
    val forceUpdate: Boolean = false
)

/**
 * Updater State Machine.
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    object UpToDate : UpdateState()
    data class Downloading(val progress: Float, val downloadedMb: Float, val totalMb: Float) : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/**
 * In-App Auto-Update Manager for Nabd Al-Dhikr.
 * Checks GitHub Releases / raw version.json, downloads APK with real-time progress,
 * and triggers FileProvider-based Package Installer.
 */
object AppUpdateManager {

    private const val GITHUB_OWNER = "omar15912007-hash"
    private const val GITHUB_REPO = "Nabd-Al-Dhikr"

    const val RAW_VERSION_URL =
        "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/main/version.json"
    const val GITHUB_RELEASES_API =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    /**
     * Checks if a newer version is available.
     * Tries raw version.json first, and falls back to GitHub Releases API.
     */
    suspend fun checkForUpdate(
        rawUrl: String = RAW_VERSION_URL,
        fallbackApiUrl: String = GITHUB_RELEASES_API
    ): UpdateState = withContext(Dispatchers.IO) {
        // Attempt 1: Fetch from raw version.json
        try {
            val url = URL(rawUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NabdAlDhikr-App")
            }

            if (conn.responseCode in 200..299) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val info = Gson().fromJson(json, UpdateInfo::class.java)
                if (info != null && info.versionCode > BuildConfig.VERSION_CODE) {
                    val resolvedApkUrl = if (info.apkUrl.isNotBlank() && !info.apkUrl.contains("/-/")) {
                        info.apkUrl
                    } else {
                        "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/download/v${info.versionName}/app-release.apk"
                    }
                    return@withContext UpdateState.UpdateAvailable(info.copy(apkUrl = resolvedApkUrl))
                } else if (info != null) {
                    return@withContext UpdateState.UpToDate
                }
            }
        } catch (_: Exception) {
            // Raw JSON failed or not pushed yet, proceed to GitHub API fallback
        }

        // Attempt 2: Fallback to GitHub Releases API
        try {
            val apiUrl = URL(fallbackApiUrl)
            val apiConn = (apiUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "NabdAlDhikr-App")
            }

            if (apiConn.responseCode in 200..299) {
                val jsonText = apiConn.inputStream.bufferedReader().use { it.readText() }
                val root = JsonParser.parseString(jsonText).asJsonObject

                val tagName = root.get("tag_name")?.asString ?: "v1.0.0"
                val body = root.get("body")?.asString ?: "تحسينات عامة وإصلاحات."

                // Look for APK download URL in release assets
                var apkDownloadUrl = ""
                if (root.has("assets") && root.get("assets").isJsonArray) {
                    val assets = root.getAsJsonArray("assets")
                    for (element in assets) {
                        val assetObj = element.asJsonObject
                        val name = assetObj.get("name")?.asString ?: ""
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = assetObj.get("browser_download_url")?.asString ?: ""
                            break
                        }
                    }
                }

                // If no asset found directly, build standard release URL
                if (apkDownloadUrl.isEmpty()) {
                    apkDownloadUrl =
                        "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/download/$tagName/app-release.apk"
                }

                val remoteVersionName = tagName.removePrefix("v").trim()
                val remoteVersionCode = parseVersionCode(remoteVersionName)

                if (remoteVersionCode > BuildConfig.VERSION_CODE) {
                    val info = UpdateInfo(
                        versionCode = remoteVersionCode,
                        versionName = remoteVersionName,
                        apkUrl = apkDownloadUrl,
                        changelog = body,
                        forceUpdate = false
                    )
                    return@withContext UpdateState.UpdateAvailable(info)
                } else {
                    return@withContext UpdateState.UpToDate
                }
            } else {
                return@withContext UpdateState.Error("فشل الاتصال بخادم التحديث (رمز: ${apiConn.responseCode})")
            }
        } catch (e: Exception) {
            return@withContext UpdateState.Error(e.localizedMessage ?: "حدث خطأ أثناء فحص التحديثات")
        }
    }

    /**
     * Converts a semantic version like 1.0.1 to integer code: 1 * 10000 + 0 * 100 + 1 = 10001
     */
    private fun parseVersionCode(versionName: String): Int {
        return try {
            val parts = versionName.split(".").map { it.filter { ch -> ch.isDigit() } }
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            major * 10000 + minor * 100 + patch
        } catch (_: Exception) {
            1
        }
    }

    /**
     * Downloads APK from remote URL with 0%-100% progress tracking.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (progress: Float, downloadedMb: Float, totalMb: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 40000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "NabdAlDhikr-App")
            }

            // Handle HTTP 302 redirects if needed
            var finalConnection = connection
            var redirectCount = 0
            while (finalConnection.responseCode in listOf(301, 302, 303, 307, 308) && redirectCount < 6) {
                val newUrl = finalConnection.getHeaderField("Location") ?: break
                finalConnection.disconnect()
                finalConnection = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 40000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "NabdAlDhikr-App")
                }
                redirectCount++
            }

            if (finalConnection.responseCode !in 200..299) {
                return@withContext Result.failure(
                    Exception("HTTP ${finalConnection.responseCode}: ${finalConnection.responseMessage}")
                )
            }

            val totalBytes = finalConnection.contentLengthLong
            val updateDir = context.externalCacheDir ?: context.cacheDir
            val targetFile = File(updateDir, "nabd_aldhikr_update.apk")
            if (targetFile.exists()) {
                targetFile.delete()
            }

            finalConnection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val progress = if (totalBytes > 0) (totalRead.toFloat() / totalBytes.toFloat()) else 0f
                        val downloadedMb = totalRead / (1024f * 1024f)
                        val totalMb = if (totalBytes > 0) (totalBytes / (1024f * 1024f)) else 0f

                        withContext(Dispatchers.Main) {
                            onProgress(progress, downloadedMb, totalMb)
                        }
                    }
                }
            }
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks unknown sources install permission and triggers package installation.
     */
    fun triggerInstall(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(manageIntent)
                return
            }
        }

        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}

/**
 * Elegant Islamic Emerald & Gold Auto-Update Dialog (Material 3).
 */
@Composable
fun AppUpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedMb by remember { mutableFloatStateOf(0f) }
    var totalMb by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    Dialog(
        onDismissRequest = {
            if (!isDownloading && !updateInfo.forceUpdate) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading && !updateInfo.forceUpdate,
            dismissOnClickOutside = !isDownloading && !updateInfo.forceUpdate
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(HeaderBg)
                        .border(1.dp, PrimaryGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✨",
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "تحديث جديد متوفر",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Version Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HeaderBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "الإصدار v${updateInfo.versionName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Changelog Box
                if (updateInfo.changelog.isNotBlank()) {
                    Text(
                        text = "ما الجديد في هذا التحديث:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ScreenBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                    ) {
                        Text(
                            text = updateInfo.changelog,
                            fontSize = 13.sp,
                            color = TextPrimary.copy(alpha = 0.9f),
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            lineHeight = 20.sp
                        )
                    }
                }

                // Error message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Progress Indicator
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PrimaryGold,
                        trackColor = ProgressTrack
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGold
                        )
                        Text(
                            text = String.format("%.1f / %.1f MB", downloadedMb, totalMb),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!updateInfo.forceUpdate && !isDownloading) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text(text = "لاحقاً", fontSize = 14.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (downloadedFile != null && downloadedFile!!.exists()) {
                                AppUpdateManager.triggerInstall(context, downloadedFile!!)
                            } else {
                                isDownloading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val result = AppUpdateManager.downloadApk(
                                        context = context,
                                        downloadUrl = updateInfo.apkUrl
                                    ) { progress, downloaded, total ->
                                        downloadProgress = progress
                                        downloadedMb = downloaded
                                        totalMb = total
                                    }

                                    isDownloading = false
                                    result.onSuccess { file ->
                                        downloadedFile = file
                                        AppUpdateManager.triggerInstall(context, file)
                                    }.onFailure { err ->
                                        errorMessage = err.localizedMessage ?: "فشل التحميل"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGold,
                            contentColor = Color.Black
                        ),
                        enabled = !isDownloading
                    ) {
                        Text(
                            text = if (downloadedFile != null) "تثبيت الآن" else if (isDownloading) "جاري التحميل..." else "تحديث الآن",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
