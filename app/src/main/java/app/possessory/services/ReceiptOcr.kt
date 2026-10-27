package app.possessory.services

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

object ReceiptOcr {
    val languages =
        listOf(
            "eng",
            "spa",
            "fra",
            "deu",
            "ita",
            "por",
            "nld",
            "pol",
            "swe",
            "jpn",
            "hin",
            "ara",
            "tur",
            "ind",
        )

    fun pages(file: File, mime: String): Int =
        if (mime != "application/pdf") 1
        else
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { it.pageCount }
            }

    fun bitmap(file: File, mime: String, pageNumber: Int = 0): Bitmap {
        if (mime == "application/pdf") {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    require(pageNumber in 0 until renderer.pageCount)
                    renderer.openPage(pageNumber).use { page ->
                        val scale = 1800f / maxOf(page.width, page.height)
                        return Bitmap.createBitmap(
                                (page.width * scale).toInt(),
                                (page.height * scale).toInt(),
                                Bitmap.Config.ARGB_8888,
                            )
                            .also { b ->
                                b.eraseColor(Color.WHITE)
                                page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                    }
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "unsupported_file" }
        val options =
            BitmapFactory.Options().apply {
                inSampleSize = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 2000)
            }
        return BitmapFactory.decodeFile(file.path, options) ?: error("unsupported_file")
    }

    fun recognise(context: Context, bitmap: Bitmap, language: String = "eng"): String {
        require(language in languages)
        val base = File(context.filesDir, "ocr")
        val data = File(base, "tessdata/$language.traineddata").apply { parentFile!!.mkdirs() }
        if (!data.exists())
            context.assets.open("tessdata/$language.traineddata").use { input ->
                data.outputStream().use { input.copyTo(it) }
            }
        val tess = TessBaseAPI()
        try {
            check(tess.init(base.path, language))
            tess.setImage(bitmap)
            return tess.utF8Text ?: ""
        } finally {
            tess.recycle()
        }
    }

    data class Fields(
        val retailer: String = "",
        val total: String = "",
        val date: String = "",
        val currency: String = "",
        val description: String = "",
        val reference: String = "",
    )

    fun extract(text: String): Fields {
        val lines = text.lines().filter { it.isNotBlank() }
        val total =
            lines
                .lastOrNull { Regex("(?i)\\b(total|amount due)\\b").containsMatchIn(it) }
                ?.let { Regex("\\d+[.,]\\d{2}").findAll(it).lastOrNull()?.value?.replace(',', '.') }
                ?: ""
        val date =
            Regex("\\b\\d{4}-\\d{2}-\\d{2}\\b").find(text)?.value?.takeIf {
                runCatching { java.time.LocalDate.parse(it) }.isSuccess
            } ?: ""
        val currency =
            when {
                text.contains("£") -> "GBP"
                text.contains("€") -> "EUR"
                Regex("\\bUSD\\b").containsMatchIn(text) -> "USD"
                Regex("\\bCAD\\b").containsMatchIn(text) -> "CAD"
                Regex("\\bAUD\\b").containsMatchIn(text) -> "AUD"
                else -> ""
            }
        val reference =
            Regex("(?im)^(?:receipt|reference|invoice|order)[ #:.]*([A-Za-z0-9-]{3,40})\\s*$")
                .find(text)
                ?.groupValues
                ?.get(1) ?: ""
        // Only explicit labelled descriptions are suggested; arbitrary lines are not guessed.
        val description =
            Regex("(?im)^(?:item|product|description)\\s*:\\s*(.+)$")
                .find(text)
                ?.groupValues
                ?.get(1) ?: ""
        return Fields(lines.firstOrNull() ?: "", total, date, currency, description, reference)
    }
}
