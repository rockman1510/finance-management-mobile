package com.financemanage.mobile.feature.transaction.platform

import androidx.compose.runtime.Composable

/** A receipt ready to upload (F10). */
class PickedFile(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String,
)

/** The backend limit is 5 MB per file (API §5.6). */
const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024

/** Opens the camera; `null` when the device has no camera. The result is already compressed. */
@Composable
expect fun rememberCameraLauncher(onResult: (PickedFile?) -> Unit): (() -> Unit)?

/** Re-encodes an image as JPEG below [MAX_UPLOAD_BYTES] (PDFs and undecodable files are returned unchanged). */
expect suspend fun prepareImageForUpload(
    bytes: ByteArray,
    fileName: String,
): PickedFile

fun contentTypeOf(fileName: String): String =
    when (fileName.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        "pdf" -> "application/pdf"
        else -> "application/octet-stream"
    }
