package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CustomerRequest
import com.example.data.model.SalesReportSummary
import com.example.data.model.ShopSettings
import java.io.File
import java.io.FileOutputStream

object ReceiptAndReportExporter {

    private val navyColor = Color.rgb(10, 22, 51)
    private val goldColor = Color.rgb(184, 134, 11)
    private val darkInk = Color.rgb(24, 28, 38)
    private val mutedInk = Color.rgb(95, 104, 120)

    fun printOrSaveReceiptPdf(
        context: Context,
        request: CustomerRequest,
        shopSettings: ShopSettings
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            shareReceiptPdf(context, request, shopSettings)
            return
        }
        val orderNum = request.orderNumber.ifBlank { request.id.take(10) }
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("MARUTI_DHAM_$orderNum.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val doc = buildReceiptPdfDocument(request, shopSettings)
                try {
                    FileOutputStream(destination?.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.localizedMessage)
                } finally {
                    doc.close()
                }
            }
        }
        printManager.print("MARUTI DHAM Receipt $orderNum", adapter, PrintAttributes.Builder().build())
    }

    fun shareReceiptPdf(
        context: Context,
        request: CustomerRequest,
        shopSettings: ShopSettings
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val orderNum = request.orderNumber.ifBlank { request.id.take(10) }
            val pdfFile = File(exportDir, "receipt_$orderNum.pdf")
            val doc = buildReceiptPdfDocument(request, shopSettings)
            FileOutputStream(pdfFile).use { fos -> doc.writeTo(fos) }
            doc.close()
            shareFileUri(context, pdfFile, "application/pdf", "Share Digital Bill PDF")
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to generate PDF receipt", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareReceiptImage(
        context: Context,
        request: CustomerRequest,
        shopSettings: ShopSettings
    ) {
        try {
            val width = 820
            val itemCount = request.items.size.coerceAtLeast(1)
            val height = 760 + (itemCount * 54)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawReceiptOnCanvas(canvas, width.toFloat(), height.toFloat(), request, shopSettings)

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val orderNum = request.orderNumber.ifBlank { request.id.take(10) }
            val imgFile = File(exportDir, "receipt_$orderNum.png")
            FileOutputStream(imgFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            bitmap.recycle()
            shareFileUri(context, imgFile, "image/png", "Share Receipt Image")
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to create receipt image", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportSalesReportCsv(
        context: Context,
        report: SalesReportSummary
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val csvFile = File(exportDir, "maruti_dham_${report.period.name.lowercase()}_report.csv")
            val content = buildString {
                appendLine("MARUTI DHAM - ${report.period.name} SALES REPORT")
                if (report.dateRangeLabel.isNotBlank()) {
                    appendLine("Date Range,${report.dateRangeLabel}")
                }
                appendLine("Generated At IST,${DomainCalculators.formatIstDateTime(System.currentTimeMillis())}")
                appendLine("Total Sales (INR),${report.totalSales}")
                appendLine("Total Orders,${report.totalOrders}")
                appendLine("Completed Orders,${report.completedOrders}")
                appendLine("Cancelled Orders,${report.cancelledOrders}")
                appendLine("Pickup Orders,${report.pickupOrders}")
                appendLine("Delivery Orders,${report.deliveryOrders}")
                appendLine("Room Bookings,${report.roomBookings}")
                appendLine("Cash Sales (INR),${report.cashSales}")
                appendLine("UPI Sales (INR),${report.upiSales}")
                appendLine("Verified Paid Total (INR),${report.verifiedPaidTotal}")
                appendLine("Unpaid or Verification Pending (INR),${report.unpaidOrPendingTotal}")
                appendLine()
                appendLine("Product Name,Quantity Sold,Revenue (INR)")
                report.productStats.forEach { stat ->
                    val safeName = stat.productName.replace(",", " ")
                    appendLine("$safeName,${stat.quantitySold},${stat.totalRevenue}")
                }
            }
            csvFile.writeText(content)
            shareFileUri(context, csvFile, "text/csv", "Export ${report.period.name} Report CSV")
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to export CSV report", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildReceiptPdfDocument(
        request: CustomerRequest,
        shopSettings: ShopSettings
    ): PdfDocument {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842.coerceAtLeast(560 + request.items.size * 34)
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        drawReceiptOnCanvas(page.canvas, pageWidth.toFloat(), pageHeight.toFloat(), request, shopSettings)
        doc.finishPage(page)
        return doc
    }

    private fun drawReceiptOnCanvas(
        canvas: Canvas,
        width: Float,
        height: Float,
        request: CustomerRequest,
        shopSettings: ShopSettings
    ) {
        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint().apply { color = navyColor }
        canvas.drawRect(0f, 0f, width, 130f, headerPaint)

        val borderPaint = Paint().apply {
            color = goldColor
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(12f, 12f, width - 12f, height - 12f, borderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(245, 200, 75)
            textSize = 30f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(shopSettings.shopName, width / 2f, 54f, titlePaint)
        canvas.drawText("FOOD ORDER RECEIPT", width / 2f, 84f, subtitlePaint)
        canvas.drawText("Owner: ${shopSettings.ownerName} • Khatu Shyam Ji", width / 2f, 110f, subtitlePaint)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkInk
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val boldPaint = Paint(bodyPaint).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val mutedPaint = Paint(bodyPaint).apply {
            color = mutedInk
            textSize = 15f
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }

        val left = 36f
        val right = width - 36f
        var y = 172f

        val createdMillis = request.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        val orderNum = request.orderNumber.ifBlank { request.id.take(12) }

        canvas.drawText("Order ID : $orderNum", left, y, boldPaint); y += 28f
        canvas.drawText("Date     : ${DomainCalculators.formatIstDate(createdMillis)}", left, y, bodyPaint); y += 26f
        canvas.drawText("Time     : ${DomainCalculators.formatIstTime(createdMillis)}", left, y, bodyPaint); y += 26f
        canvas.drawText("Customer : ${request.customerName}", left, y, bodyPaint); y += 26f
        canvas.drawText("Mobile   : ${request.contactPhone}", left, y, bodyPaint); y += 26f
        canvas.drawText("Mode     : ${request.fulfillmentType} (${request.expectedTime})", left, y, bodyPaint); y += 26f
        canvas.drawText("Payment  : ${request.paymentMethod} [${request.paymentStatus}]", left, y, bodyPaint); y += 26f

        if (!request.deliveryAddress.isNullOrBlank()) {
            canvas.drawText("Address  : ${request.deliveryAddress.take(42)}", left, y, mutedPaint); y += 26f
        }

        canvas.drawLine(left, y, right, y, linePaint); y += 28f
        canvas.drawText("ITEM", left, y, boldPaint)
        canvas.drawText("QTY", width * 0.52f, y, boldPaint)
        canvas.drawText("RATE", width * 0.66f, y, boldPaint)
        val priceHeaderPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("TOTAL", right, y, priceHeaderPaint); y += 18f
        canvas.drawLine(left, y, right, y, linePaint); y += 28f

        val rightAlignBody = Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT }
        request.items.forEach { item ->
            val label = if (item.variantLabel.isNotBlank()) "${item.name} (${item.variantLabel})" else item.name
            canvas.drawText(label.take(22), left, y, bodyPaint)
            canvas.drawText(item.quantity.toString(), width * 0.53f, y, bodyPaint)
            canvas.drawText("₹${item.unitPrice}", width * 0.66f, y, bodyPaint)
            canvas.drawText("₹${item.lineTotal}", right, y, rightAlignBody)
            y += 30f
        }

        y += 6f
        canvas.drawLine(left, y, right, y, linePaint); y += 30f
        val subtotal = DomainCalculators.calculateRequestSubtotal(request)
        canvas.drawText("Subtotal", left, y, bodyPaint)
        canvas.drawText("₹$subtotal", right, y, rightAlignBody); y += 28f
        if (request.deliveryCharge > 0L) {
            canvas.drawText("Delivery Charge", left, y, bodyPaint)
            canvas.drawText("₹${request.deliveryCharge}", right, y, rightAlignBody); y += 28f
        }
        if (request.packagingCharge > 0L) {
            canvas.drawText("Packaging Charge", left, y, bodyPaint)
            canvas.drawText("₹${request.packagingCharge}", right, y, rightAlignBody); y += 28f
        }

        val totalPaint = Paint(boldPaint).apply {
            textSize = 22f
            color = navyColor
        }
        val rightTotalPaint = Paint(totalPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Grand Total", left, y, totalPaint)
        canvas.drawText("₹${request.grandTotal}", right, y, rightTotalPaint); y += 44f

        canvas.drawLine(left, y, right, y, linePaint); y += 34f
        val footerPaint = Paint(boldPaint).apply {
            textAlign = Paint.Align.CENTER
            color = navyColor
            textSize = 18f
        }
        canvas.drawText("THANK YOU FOR VISITING", width / 2f, y, footerPaint); y += 26f
        canvas.drawText("MARUTI DHAM", width / 2f, y, footerPaint)
    }

    private fun shareFileUri(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
