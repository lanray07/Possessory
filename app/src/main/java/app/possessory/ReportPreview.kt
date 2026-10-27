package app.possessory

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource as s
import androidx.compose.ui.unit.dp
import app.possessory.services.ReceiptOcr
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReportPreview(file: File, onDismiss: () -> Unit) {
    var page by rememberSaveable(file.name) { mutableStateOf(0) }
    var count by remember { mutableStateOf(1) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(file, page) {
        runCatching {
                withContext(Dispatchers.IO) {
                    val pages = ReceiptOcr.pages(file, "application/pdf")
                    pages to ReceiptOcr.bitmap(file, "application/pdf", page.coerceIn(0, pages - 1))
                }
            }
            .onSuccess {
                count = it.first
                bitmap = it.second
            }
            .onFailure { failed = true }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(R.string.report_preview)) },
        text = {
            Column {
                Choice(
                    s(R.string.preview_page),
                    page.toString(),
                    (0 until count).map { it.toString() to s(R.string.page_number, it + 1, count) },
                    { page = it.toInt() },
                )
                bitmap?.let {
                    Image(
                        it.asImageBitmap(),
                        s(R.string.report_preview),
                        Modifier.fillMaxWidth().heightIn(max = 500.dp),
                    )
                }
                if (failed) Text(s(R.string.operation_error))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s(R.string.close)) } },
    )
}
