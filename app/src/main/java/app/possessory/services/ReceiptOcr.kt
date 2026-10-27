package app.possessory.services

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

object ReceiptOcr {
  fun bitmap(file: File, mime: String): Bitmap {
    if (mime == "application/pdf") {
      ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
        PdfRenderer(fd).use { renderer ->
          renderer.openPage(0).use { page ->
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

  fun recognise(context: Context, bitmap: Bitmap): String {
    val base = File(context.filesDir, "ocr")
    val data = File(base, "tessdata/eng.traineddata").apply { parentFile!!.mkdirs() }
    if (!data.exists())
      context.assets.open("tessdata/eng.traineddata").use { input ->
        data.outputStream().use { input.copyTo(it) }
      }
    val tess = TessBaseAPI()
    try {
      check(tess.init(base.path, "eng"))
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
  )

  fun extract(text: String): Fields {
    val lines = text.lines().filter { it.isNotBlank() }
    val total =
      lines
        .lastOrNull { Regex("(?i)\\b(total|amount due)\\b").containsMatchIn(it) }
        ?.let { Regex("\\d+[.,]\\d{2}").findAll(it).lastOrNull()?.value?.replace(',', '.') } ?: ""
    val date = Regex("\\b\\d{4}-\\d{2}-\\d{2}\\b").find(text)?.value ?: ""
    val currency =
      when {
        text.contains("£") -> "GBP"
        text.contains("€") -> "EUR"
        else -> ""
      }
    return Fields(lines.firstOrNull() ?: "", total, date, currency)
  }
}
