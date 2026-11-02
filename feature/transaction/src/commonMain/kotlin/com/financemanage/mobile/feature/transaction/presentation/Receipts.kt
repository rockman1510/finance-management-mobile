package com.financemanage.mobile.feature.transaction.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.transaction.platform.PickedFile
import com.financemanage.mobile.feature.transaction.platform.prepareImageForUpload
import com.financemanage.mobile.feature.transaction.platform.rememberCameraLauncher
import com.financemanage.mobile.feature.transaction.resources.Res
import com.financemanage.mobile.feature.transaction.resources.add_receipt
import com.financemanage.mobile.feature.transaction.resources.choose_image
import com.financemanage.mobile.feature.transaction.resources.choose_pdf
import com.financemanage.mobile.feature.transaction.resources.take_photo
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource

private const val MAX_ZOOM = 5f

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun rememberImageBitmap(bytes: ByteArray?): ImageBitmap? =
    remember(bytes) { bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }

/** "Add receipt" button: Take photo / Choose image / Choose PDF (F10). */
@Composable
internal fun AddReceiptButton(
    enabled: Boolean,
    onPicked: (PickedFile) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val camera = rememberCameraLauncher { file -> file?.let(onPicked) }
    val imagePicker =
        rememberFilePickerLauncher(type = PickerType.Image) { file ->
            if (file != null) scope.launch { onPicked(prepareImageForUpload(file.readBytes(), file.name)) }
        }
    val pdfPicker =
        rememberFilePickerLauncher(type = PickerType.File(listOf("pdf"))) { file ->
            if (file != null) scope.launch { onPicked(PickedFile(file.readBytes(), file.name, "application/pdf")) }
        }
    Box {
        OutlinedCard(
            modifier = Modifier.size(72.dp).clickable(enabled = enabled) { menu = true },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AddAPhoto, contentDescription = stringResource(Res.string.add_receipt))
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (camera != null) {
                DropdownMenuItem(text = {
                    androidx.compose.material3.Text(
                        stringResource(Res.string.take_photo),
                    )
                }, onClick = {
                    menu = false
                    camera()
                })
            }
            DropdownMenuItem(text = {
                androidx.compose.material3.Text(
                    stringResource(Res.string.choose_image),
                )
            }, onClick = {
                menu = false
                imagePicker.launch()
            })
            DropdownMenuItem(text = {
                androidx.compose.material3.Text(
                    stringResource(Res.string.choose_pdf),
                )
            }, onClick = {
                menu = false
                pdfPicker.launch()
            })
        }
    }
}

/** Thumbnail of an image or a PDF icon, with an optional remove button. */
@Composable
internal fun ReceiptThumbnail(
    image: ImageBitmap?,
    isPdf: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    Box(Modifier.size(72.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(
                    RoundedCornerShape(8.dp),
                ).background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            when {
                image != null ->
                    Image(
                        image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                isPdf -> Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
            }
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp).align(Alignment.TopEnd)) {
                Icon(Icons.Outlined.Close, contentDescription = null)
            }
        }
    }
}

/** Receipts picked in Quick add before the transaction exists. */
@Composable
internal fun PendingReceiptsRow(
    files: List<PickedFile>,
    canAdd: Boolean,
    onAdd: (PickedFile) -> Unit,
    onRemove: (PickedFile) -> Unit,
    onView: (ImageBitmap) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(files) { file ->
            val bitmap = rememberImageBitmap(if (file.contentType.startsWith("image/")) file.bytes else null)
            ReceiptThumbnail(bitmap, file.contentType == "application/pdf", onClick = {
                bitmap?.let(onView)
            }, onRemove = { onRemove(file) })
        }
        item { AddReceiptButton(enabled = canAdd, onPicked = onAdd) }
    }
}

/** Full-screen viewer with pinch to zoom. */
@Composable
internal fun ImageViewer(
    image: ImageBitmap,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState =
        rememberTransformableState { zoom, pan, _ ->
            scale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
            offset += pan
        }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .transformable(transformState)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        ),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Outlined.Close, contentDescription = null, tint = Color.White)
            }
        }
    }
}
