package com.classmanager.app.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.classmanager.app.data.Payment
import com.classmanager.app.data.Student
import com.classmanager.app.data.fmtINR
import java.io.File
import java.io.FileOutputStream

/** One-page fee receipt PDF, written to the app cache for sharing/printing. */
object ReceiptPdf {
    fun create(context: Context, student: Student, payment: Payment): File? {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val title = Paint().apply { textSize = 26f; isFakeBoldText = true }
            val body = Paint().apply { textSize = 16f }
            val small = Paint().apply { textSize = 13f }
            var y = 80f
            canvas.drawText("Fee Receipt", 40f, y, title)
            y += 40f
            canvas.drawText("Receipt: ${payment.receiptNo}", 40f, y, body)
            y += 30f
            canvas.drawText("Student: ${student.name}", 40f, y, body)
            y += 30f
            canvas.drawText("Batch: ${student.batch}", 40f, y, body)
            y += 30f
            canvas.drawText("Amount: ${fmtINR(payment.amount)}", 40f, y, body)
            y += 30f
            canvas.drawText("Method: ${payment.method}", 40f, y, body)
            y += 30f
            canvas.drawText("Date: ${payment.date}", 40f, y, body)
            y += 50f
            canvas.drawText("Thank you for your payment.", 40f, y, small)
            document.finishPage(page)
            val file = File(context.cacheDir, "${payment.receiptNo}.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()
            file
        } catch (e: Exception) {
            null
        }
    }
}
