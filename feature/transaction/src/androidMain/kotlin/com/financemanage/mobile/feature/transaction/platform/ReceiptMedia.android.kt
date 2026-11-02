package com.financemanage.mobile.feature.transaction.platform

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

private const val MAX_DIMENSION = 2048
private const val START_QUALITY = 85
private const val QUALITY_STEP = 15
private const val MIN_QUALITY = 25

@Composable
actual fun rememberCameraLauncher(onResult: (PickedFile?) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = pendingUri
            if (!success || uri == null) {
                onResult(null)
            } else {
                scope.launch {
                    val bytes =
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }
                    onResult(bytes?.let { prepareImageForUpload(it, "receipt.jpg") })
                }
            }
        }
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) return null
    return {
        val directory = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File.createTempFile("receipt_", ".jpg", directory)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.receipts", file)
        pendingUri = uri
        launcher.launch(uri)
    }
}

actual suspend fun prepareImageForUpload(
    bytes: ByteArray,
    fileName: String,
): PickedFile =
    withContext(Dispatchers.Default) {
        val type = contentTypeOf(fileName)
        if (!type.startsWith("image/")) return@withContext PickedFile(bytes, fileName, type)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_DIMENSION) sample *= 2
        val bitmap =
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return@withContext PickedFile(bytes, fileName, type)
        var quality = START_QUALITY
        var output: ByteArray
        do {
            output =
                ByteArrayOutputStream().use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                    stream.toByteArray()
                }
            quality -= QUALITY_STEP
        } while (output.size > MAX_UPLOAD_BYTES && quality >= MIN_QUALITY)
        bitmap.recycle()
        PickedFile(output, fileName.substringBeforeLast('.') + ".jpg", "image/jpeg")
    }
