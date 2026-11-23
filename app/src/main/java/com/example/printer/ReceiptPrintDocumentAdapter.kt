package com.example.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.FileOutputStream
import java.io.IOException

/**
 * Standard Android PrintDocumentAdapter for thermal receipt bitmaps.
 *
 * This allows direct printing through:
 * 1. Easy Thermal Print / RawBT (installed print services on the Android device).
 * 2. Android System Spooler / Default Print Service.
 * 3. Network or Wi-Fi printers recognized by Android.
 */
class ReceiptPrintDocumentAdapter(
    private val bitmap: Bitmap,
    private val jobTitle: String = "فاتورة مطعم صنارة وشبكة"
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("$jobTitle.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()

        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val pdfDocument = PdfDocument()

        try {
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))

            pdfDocument.finishPage(page)

            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            }

            FileOutputStream(destination.fileDescriptor).use { output ->
                pdfDocument.writeTo(output)
            }

            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.localizedMessage)
        } finally {
            pdfDocument.close()
        }
    }

    companion object {
        private var activeActivityRef: java.lang.ref.WeakReference<android.app.Activity>? = null

        var activeActivity: android.app.Activity?
            get() = activeActivityRef?.get()
            set(value) {
                activeActivityRef = if (value != null) java.lang.ref.WeakReference(value) else null
            }

        fun printBitmapViaSystemSpooler(context: Context, bitmap: Bitmap, jobName: String = "فاتورة مطعم صنارة وشبكة"): Boolean {
            return try {
                if (bitmap.isRecycled) return false
                val act = activeActivity ?: (context as? android.app.Activity)
                val targetContext = act ?: context
                val printManager = targetContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null && act != null && !act.isFinishing && !act.isDestroyed) {
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                        .build()

                    printManager.print(jobName, ReceiptPrintDocumentAdapter(bitmap, jobName), printAttributes)
                    true
                } else false
            } catch (_: Throwable) {
                false
            }
        }
    }
}
