package app.possessory.services

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import app.possessory.R
import app.possessory.data.*
import app.possessory.domain.InventoryRules
import java.io.File
import java.io.OutputStream

/** Paginated native text layout supports shaping and bidirectional paragraphs. */
object ReportPdf {
    fun write(
        context: Context,
        items: List<InventoryItem>,
        rooms: List<RoomRecord>,
        attachments: List<Attachment>,
        serials: Boolean,
        prices: Boolean,
        photos: Boolean,
        references: Boolean,
        output: OutputStream,
    ) {
        val document = PdfDocument()
        var page: PdfDocument.Page? = null
        var number = 0
        var y = 0f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(36, 52, 74) }
        fun layout(text: String, size: Float, width: Int = 515): StaticLayout {
            paint.textSize = size
            return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()
        }
        fun draw(block: StaticLayout, x: Float, top: Float) {
            page!!.canvas.save()
            page!!.canvas.translate(x, top)
            block.draw(page!!.canvas)
            page!!.canvas.restore()
        }
        fun nextPage() {
            page?.let { document.finishPage(it) }
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, ++number).create())
            draw(layout("POSSESSORY", 24f), 40f, 30f)
            draw(layout(context.getString(R.string.inventory_report), 14f), 40f, 66f)
            draw(
                layout(
                    app.possessory.date(java.time.LocalDate.now().toString()) + " · " + number,
                    10f,
                ),
                40f,
                93f,
            )
            draw(layout(context.getString(R.string.report_disclaimer), 8f), 40f, 805f)
            y = 130f
        }
        fun paragraph(text: String, size: Float = 10f) {
            if (text.isBlank()) return
            val block = layout(text, size)
            for (index in 0 until block.lineCount) {
                val line =
                    text.substring(block.getLineStart(index), block.getLineEnd(index)).trimEnd()
                val part = layout(line, size)
                if (y + part.height > 780f) nextPage()
                draw(part, 40f, y)
                y += part.height + 5f
            }
        }
        try {
            nextPage()
            for (item in items) {
                if (y > 650f) nextPage()
                paragraph(item.name, 15f)
                paragraph(rooms.find { it.id == item.roomId }?.name ?: "")
                if (prices)
                    paragraph(
                        context.getString(R.string.purchase_price) +
                            ": " +
                            (item.priceMinor?.let { InventoryRules.money(it, item.currency) }
                                ?: "—")
                    )
                if (item.purchaseDate.isNotBlank())
                    paragraph(
                        context.getString(R.string.purchase_date) +
                            ": " +
                            app.possessory.date(item.purchaseDate)
                    )
                if (serials && item.serial.isNotBlank())
                    paragraph(context.getString(R.string.serial) + ": " + item.serial)
                if (item.warrantyEnd.isNotBlank())
                    paragraph(
                        context.getString(R.string.warranty_end) +
                            ": " +
                            app.possessory.date(item.warrantyEnd)
                    )
                if (references)
                    attachments
                        .filter { it.itemId == item.id && it.kind != "Photo" }
                        .forEach { paragraph(it.name + " · " + it.id, 9f) }
                if (photos)
                    attachments
                        .firstOrNull {
                            it.itemId == item.id &&
                                it.kind == "Photo" &&
                                it.mime.startsWith("image/")
                        }
                        ?.let { attachment ->
                            runCatching {
                                    ReceiptOcr.bitmap(
                                        File(context.filesDir, "attachments/${attachment.file}"),
                                        attachment.mime,
                                    )
                                }
                                .getOrNull()
                                ?.let { bitmap ->
                                    try {
                                        if (y + 130 > 780) nextPage()
                                        val scale = minOf(150f / bitmap.width, 120f / bitmap.height)
                                        page!!
                                            .canvas
                                            .drawBitmap(
                                                bitmap,
                                                null,
                                                RectF(
                                                    40f,
                                                    y,
                                                    40f + bitmap.width * scale,
                                                    y + bitmap.height * scale,
                                                ),
                                                null,
                                            )
                                        y += bitmap.height * scale + 10f
                                    } finally {
                                        bitmap.recycle()
                                    }
                                }
                        }
                y += 14f
            }
            document.finishPage(page!!)
            page = null
            document.writeTo(output)
        } finally {
            document.close()
        }
    }
}
