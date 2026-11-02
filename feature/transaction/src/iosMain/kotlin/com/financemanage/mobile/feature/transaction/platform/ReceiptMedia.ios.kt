package com.financemanage.mobile.feature.transaction.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.memcpy

private const val START_QUALITY = 0.85
private const val QUALITY_STEP = 0.15
private const val MIN_QUALITY = 0.25

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
    }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val result = ByteArray(size)
    if (size > 0) result.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    return result
}

private fun jpegBelowLimit(image: UIImage): ByteArray? {
    var quality = START_QUALITY
    var data = UIImageJPEGRepresentation(image, quality)
    while (data != null && data.length.toLong() > MAX_UPLOAD_BYTES && quality - QUALITY_STEP >= MIN_QUALITY) {
        quality -= QUALITY_STEP
        data = UIImageJPEGRepresentation(image, quality)
    }
    return data?.toByteArray()
}

actual suspend fun prepareImageForUpload(
    bytes: ByteArray,
    fileName: String,
): PickedFile {
    val type = contentTypeOf(fileName)
    if (!type.startsWith("image/")) return PickedFile(bytes, fileName, type)
    val image = UIImage.imageWithData(bytes.toNSData()) ?: return PickedFile(bytes, fileName, type)
    val jpeg = jpegBelowLimit(image) ?: return PickedFile(bytes, fileName, type)
    return PickedFile(jpeg, fileName.substringBeforeLast('.') + ".jpg", "image/jpeg")
}

private class CameraDelegate(
    private val onImage: (UIImage?) -> Unit,
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, completion = null)
        onImage(image)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onImage(null)
    }
}

private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) controller = controller.presentedViewController
    return controller
}

@Composable
actual fun rememberCameraLauncher(onResult: (PickedFile?) -> Unit): (() -> Unit)? {
    val scope = rememberCoroutineScope()
    val delegate =
        remember {
            CameraDelegate { image ->
                scope.launch {
                    onResult(image?.let { jpegBelowLimit(it) }?.let { PickedFile(it, "receipt.jpg", "image/jpeg") })
                }
            }
        }
    val available =
        UIImagePickerController.isSourceTypeAvailable(
            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
        )
    if (!available) return null
    return {
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}
