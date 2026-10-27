package app.possessory

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource as s
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun CropReceiptDialog(original: Bitmap, onDismiss: () -> Unit, onSave: (Bitmap) -> Unit) {
    var left by remember { mutableStateOf(0f) }
    var top by remember { mutableStateOf(0f) }
    var right by remember { mutableStateOf(0f) }
    var bottom by remember { mutableStateOf(0f) }
    val bounds =
        remember(original, left, top, right, bottom) {
            val x = (original.width * left / 100).toInt()
            val y = (original.height * top / 100).toInt()
            android.graphics.Rect(
                x,
                y,
                (original.width * (100 - right) / 100).toInt().coerceIn(x + 1, original.width),
                (original.height * (100 - bottom) / 100).toInt().coerceIn(y + 1, original.height),
            )
        }
    val paint = remember { android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG) }
    val description = s(R.string.receipt_preview)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(R.string.crop_receipt)) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Canvas(
                    Modifier.fillMaxWidth().height(180.dp).semantics {
                        contentDescription = description
                    }
                ) {
                    val scale = minOf(size.width / bounds.width(), size.height / bounds.height())
                    val width = bounds.width() * scale
                    val height = bounds.height() * scale
                    val target =
                        android.graphics.RectF(
                            (size.width - width) / 2,
                            (size.height - height) / 2,
                            (size.width + width) / 2,
                            (size.height + height) / 2,
                        )
                    drawIntoCanvas { it.nativeCanvas.drawBitmap(original, bounds, target, paint) }
                }
                Text(s(R.string.crop_left) + " ${left.toInt()}")
                Slider(left, { left = it }, valueRange = 0f..(95f - right))
                Text(s(R.string.crop_right) + " ${right.toInt()}")
                Slider(right, { right = it }, valueRange = 0f..(95f - left))
                Text(s(R.string.crop_top) + " ${top.toInt()}")
                Slider(top, { top = it }, valueRange = 0f..(95f - bottom))
                Text(s(R.string.crop_bottom) + " ${bottom.toInt()}")
                Slider(bottom, { bottom = it }, valueRange = 0f..(95f - top))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        Bitmap.createBitmap(
                            original,
                            bounds.left,
                            bounds.top,
                            bounds.width(),
                            bounds.height(),
                        )
                    )
                }
            ) {
                Text(s(R.string.apply_details))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
    )
}
