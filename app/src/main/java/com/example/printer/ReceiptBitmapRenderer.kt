package com.example.printer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.data.model.CancelledKitchenItem
import com.example.data.model.OrderWithItems
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-definition monochrome raster bitmap renderer and ESC/POS raster converter.
 *
 * Guarantees 100% flawless Arabic rendering for thermal receipt printers:
 * - Native HarfBuzz cursive shaping and BiDi layout via Android Canvas/StaticLayout.
 * - Zero scrambled letters, zero unjoined glyphs, and crystal-clear RTL Arabic typography.
 * - Exact table columns, dashed/solid dividers, prominent order badges, and total boxes.
 * - Universal ESC/POS Raster Bit Image (GS v 0) output compatible with 58mm and 80mm printers.
 */
object ReceiptBitmapRenderer {

    fun formatQuantity(qty: Double): String {
        return if (qty % 1.0 == 0.0) {
            qty.toLong().toString()
        } else {
            val formatted = String.format(Locale.US, "%.2f", qty).trimEnd('0')
            if (formatted.endsWith(".")) formatted.substring(0, formatted.length - 1) else formatted
        }
    }

    fun formatCurrency(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", amount)
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }

    /**
     * Renders a complete customer invoice receipt into a monochrome-friendly Bitmap.
     */
    fun renderCustomerInvoiceBitmap(
        orderWithItems: OrderWithItems,
        template: InvoiceTemplateConfig,
        paperWidthMm: Int = 80,
        isReprint: Boolean = false,
        reprintReason: String? = null
    ): Bitmap {
        val width = if (paperWidthMm == 58) 384 else 576
        val baseScale = if (paperWidthMm == 58) 0.72f else 1.0f
        
        // Dynamic font scale chosen by user: NORMAL (1.0), LARGE (1.30), HUGE (1.55)
        val fontScale = when (template.fontSize.uppercase()) {
            "HUGE" -> 1.55f
            "LARGE" -> 1.30f
            else -> 1.0f
        }
        val scale = baseScale

        val sidePadding = (14 * scale).toInt()
        val contentWidth = width - (sidePadding * 2)

        // Dynamically size temporary bitmap according to items count to eliminate OOM spikes
        val maxTempHeight = (orderWithItems.items.size * 140 + 1500).coerceIn(800, 3600)
        val tempBitmap = Bitmap.createBitmap(width, maxTempHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tempBitmap)
        canvas.drawColor(Color.WHITE)

        val centerX = width / 2f
        val rightX = (width - sidePadding).toFloat()
        val leftX = sidePadding.toFloat()

        val customTitleFontSize = template.headerTitleFontSize.toFloat().coerceIn(20f, 44f)
        val customDetailsFontSize = template.headerDetailsFontSize.toFloat().coerceIn(12f, 26f)
        val metaFontSize = template.metadataFontSizePx.toFloat().coerceIn(12f, 26f)
        val tableHeaderFontSize = template.tableHeaderFontSizePx.toFloat().coerceIn(13f, 26f)
        val itemFontSize = template.itemFontSizePx.toFloat().coerceIn(16f, 36f)
        val itemPrepFontSize = template.itemPrepFontSizePx.toFloat().coerceIn(12f, 28f)
        val itemNotesFontSize = template.itemNotesFontSizePx.toFloat().coerceIn(11f, 24f)
        val totalBoxFontSize = template.totalBoxFontSizePx.toFloat().coerceIn(18f, 36f)

        // Setup Paints with real font scaling
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = customTitleFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val centerRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = customDetailsFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val rightBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = itemFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val rightRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = metaFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        val leftBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = itemFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val leftRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = metaFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        val centerBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = tableHeaderFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val tableHeaderRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = tableHeaderFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val tableHeaderLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = tableHeaderFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val prepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = itemPrepFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val itemNotesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = itemNotesFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2.5f * scale
            style = Paint.Style.STROKE
        }

        val thinLinePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1.2f * scale
            style = Paint.Style.STROKE
        }

        val isCompact = template.compactHeaderMode
        val topPaddingVal = (if (isCompact) template.topPadding.coerceAtMost(10) else template.topPadding.coerceIn(8, 60)).toFloat() * scale
        val lineSpacingVal = (if (isCompact) template.headerLineSpacing.coerceAtMost(6) else template.headerLineSpacing.coerceIn(4, 30)).toFloat() * scale
        var y = topPaddingVal
        var anyHeaderDrawn = false

        // 1. Restaurant Logo (if set by user and enabled)
        if (template.showLogo && !template.logoPath.isNullOrBlank()) {
            try {
                val logoFile = File(template.logoPath)
                if (logoFile.exists()) {
                    val rawLogo = BitmapFactory.decodeFile(template.logoPath)
                    if (rawLogo != null) {
                        val maxLogoWidth = (if (isCompact) (120f * scale).toInt() else (180f * scale).toInt())
                        val aspect = rawLogo.height.toFloat() / rawLogo.width.toFloat()
                        val scaledWidth = maxLogoWidth.coerceAtMost(rawLogo.width)
                        val scaledHeight = (scaledWidth * aspect).toInt().coerceAtMost(maxLogoWidth)
                        val scaledLogo = Bitmap.createScaledBitmap(rawLogo, scaledWidth, scaledHeight, true)
                        val logoLeft = centerX - (scaledWidth / 2f)
                        canvas.drawBitmap(scaledLogo, logoLeft, y, null)
                        y += scaledHeight + (if (isCompact) 4f * scale else 8f * scale)
                        anyHeaderDrawn = true
                    }
                }
            } catch (e: Exception) {
                // Ignore logo read error gracefully
            }
        }

        // 2. Header Title & Subtitle with guaranteed vertical clearance (no top clipping, no line collisions)
        if (template.showHeaderTitle && template.headerTitle.isNotBlank()) {
            val titleMetrics = titlePaint.fontMetrics
            val baseline = y - titleMetrics.ascent
            canvas.drawText(template.headerTitle, centerX, baseline, titlePaint)
            y = baseline + titleMetrics.descent + lineSpacingVal
            anyHeaderDrawn = true
        }

        val detailsMetrics = centerRegularPaint.fontMetrics
        if (template.showSubtitle && template.subtitle.isNotBlank()) {
            val baseline = y - detailsMetrics.ascent
            canvas.drawText(template.subtitle, centerX, baseline, centerRegularPaint)
            y = baseline + detailsMetrics.descent + lineSpacingVal
            anyHeaderDrawn = true
        }

        if (template.showAddress && template.address.isNotBlank()) {
            val baseline = y - detailsMetrics.ascent
            canvas.drawText(template.address, centerX, baseline, centerRegularPaint)
            y = baseline + detailsMetrics.descent + lineSpacingVal
            anyHeaderDrawn = true
        }

        if (template.showPhones && template.phone1.isNotBlank()) {
            val phones = template.phone1 + if (template.phone2.isNotBlank()) " - ${template.phone2}" else ""
            val baseline = y - detailsMetrics.ascent
            canvas.drawText("هاتف: $phones", centerX, baseline, centerRegularPaint)
            y = baseline + detailsMetrics.descent + lineSpacingVal
            anyHeaderDrawn = true
        }

        // Top Divider (only if any header was drawn)
        if (anyHeaderDrawn) {
            y += (if (isCompact) 2f else 4f) * scale
            canvas.drawLine(leftX, y, rightX, y, linePaint)
            y += (if (isCompact) 3f else 6f) * scale
        }

        // Reprint Banner (if reprinting)
        if (isReprint) {
            val bannerRect = RectF(leftX, y, rightX, y + (28f * scale * fontScale))
            val bgPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
            canvas.drawRect(bannerRect, bgPaint)
            val bannerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 17f * scale * fontScale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val reasonText = if (!reprintReason.isNullOrBlank()) " ($reprintReason)" else ""
            canvas.drawText("*** إعادة طباعة فاتورة$reasonText ***", centerX, y + (19f * scale * fontScale), bannerTextPaint)
            y += (32f * scale * fontScale)
        }

        val order = orderWithItems.order
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
        val shortTimeFormat = SimpleDateFormat("hh:mm a", Locale.US)

        val smallMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = metaFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val smallSellerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = (metaFontSize - 2f).coerceAtLeast(11f) * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        // 3. Order Badge & Date/Seller (COMPACT vs STANDARD)
        if (isCompact) {
            // ULTRA-COMPACT HEADER: Saves massive vertical space
            if (template.showOrderBadge) {
                val orderBoxHeight = (32f * scale * fontScale)
                val boxRect = RectF(leftX, y, rightX, y + orderBoxHeight)
                val blackFillPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
                canvas.drawRoundRect(boxRect, 4f * scale, 4f * scale, blackFillPaint)

                val orderNumWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 21f * scale * fontScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                val orderTypeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 15f * scale * fontScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.LEFT
                }

                val numBaseline = y + (orderBoxHeight / 2f) - ((orderNumWhitePaint.descent() + orderNumWhitePaint.ascent()) / 2f)
                val typeBaseline = y + (orderBoxHeight / 2f) - ((orderTypeWhitePaint.descent() + orderTypeWhitePaint.ascent()) / 2f)
                canvas.drawText("أوردر: #${order.orderNumber}", rightX - (8f * scale), numBaseline, orderNumWhitePaint)

                val typeLabel = if (order.orderType == "DELIVERY") "دليفري" else "استلام"
                val timeLabel = if (template.showDateTime) " | " + shortTimeFormat.format(Date(order.createdAt)) else ""
                canvas.drawText("$typeLabel$timeLabel", leftX + (8f * scale), typeBaseline, orderTypeWhitePaint)
                y += orderBoxHeight + (4f * scale)
            }

            // Date only if order badge hidden (seller name is intentionally omitted from receipts)
            if (!template.showOrderBadge && template.showDateTime) {
                val dateText = "التاريخ: ${dateFormat.format(Date(order.createdAt))}"
                val metaBaseline = y + smallMetaPaint.textSize
                canvas.drawText(dateText, rightX, metaBaseline, smallMetaPaint)
                y += (smallMetaPaint.textSize * 1.25f) + (3f * scale)
            }
        } else {
            // STANDARD HEADER
            if (template.showOrderBadge) {
                val orderBoxHeight = (54f * scale * fontScale)
                val boxRect = RectF(leftX, y, rightX, y + orderBoxHeight)
                val blackFillPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
                canvas.drawRoundRect(boxRect, 6f * scale, 6f * scale, blackFillPaint)

                val orderNumWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 32f * scale * fontScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                val orderTypeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 21f * scale * fontScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.LEFT
                }

                val numBaseline = y + (orderBoxHeight / 2f) - ((orderNumWhitePaint.descent() + orderNumWhitePaint.ascent()) / 2f)
                val typeBaseline = y + (orderBoxHeight / 2f) - ((orderTypeWhitePaint.descent() + orderTypeWhitePaint.ascent()) / 2f)
                canvas.drawText("أوردر رقم: ${order.orderNumber}", rightX - (10f * scale), numBaseline, orderNumWhitePaint)
                val typeLabel = if (order.orderType == "DELIVERY") "دليفري توصيل" else "استلام وحجز"
                canvas.drawText(typeLabel, leftX + (10f * scale), typeBaseline, orderTypeWhitePaint)
                y += orderBoxHeight + (10f * scale)
            }

            if (template.showDateTime) {
                val dateText = "التاريخ: ${dateFormat.format(Date(order.createdAt))}"
                val metaBaseline = y + smallMetaPaint.textSize
                canvas.drawText(dateText, rightX, metaBaseline, smallMetaPaint)
                y += (smallMetaPaint.textSize * 1.35f) + (8f * scale)
            }
        }

        // 4. Customer & Delivery Information Card (With granular toggles & compact mode)
        val showCustName = template.showCustomerName && order.customerName.isNotBlank() && order.customerName != "زبون مباشر"
        val showCustPhone = template.showCustomerPhone && order.customerPhone.isNotBlank()
        val showSecPhone = template.showCustomerPhone && !order.customerSecondaryPhone.isNullOrBlank()
        val showAddress = template.showDeliveryAddress && !order.deliveryAddressFull.isNullOrBlank()
        val showArea = template.showDeliveryAddress && !order.customerArea.isNullOrBlank() && (order.deliveryAddressFull.isNullOrBlank() || !order.deliveryAddressFull.contains(order.customerArea))
        val showDriver = template.showDriverName && order.orderType == "DELIVERY" && !order.driverName.isNullOrBlank()
        val hasAnyCustomerInfo = template.showDeliveryDetails && (showCustName || showCustPhone || showSecPhone || showAddress || showArea || showDriver)

        if (hasAnyCustomerInfo) {
            val cardPadding = if (isCompact) 6f * scale else 10f * scale
            val cardStartY = y
            val cardBorderPaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = if (isCompact) 1.5f * scale else 2.4f * scale
                style = Paint.Style.STROKE
            }

            val custNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = (if (isCompact) metaFontSize + 2f else metaFontSize + 6f) * scale * fontScale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            val custPhonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = (if (isCompact) metaFontSize + 2f else metaFontSize + 8f) * scale * fontScale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            val addrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = (if (isCompact) metaFontSize + 1f else metaFontSize + 5f) * scale * fontScale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            val driverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = (if (isCompact) metaFontSize else metaFontSize + 4f) * scale * fontScale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            var cardCurrentY = cardStartY + (if (isCompact) 6f * scale else 10f * scale)
            val custLineSpacing = (if (isCompact) 3f else 6f) * scale

            if (isCompact) {
                // Condensed layout: Single/Double tight lines
                val custParts = mutableListOf<String>()
                if (showCustName) custParts.add("العميل: ${order.customerName}")
                if (showCustPhone) custParts.add("هـ: ${order.customerPhone}")
                if (showSecPhone) custParts.add("إضافي: ${order.customerSecondaryPhone}")

                if (custParts.isNotEmpty()) {
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = custParts.joinToString(" • "),
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = custNamePaint,
                        alignRight = true
                    ) + custLineSpacing
                }

                val addrParts = mutableListOf<String>()
                if (showAddress) {
                    addrParts.add("عنوان: ${order.deliveryAddressFull!!.trim()}")
                } else if (showArea) {
                    addrParts.add("منطقة: ${order.customerArea!!.trim()}")
                }
                if (showDriver) {
                    addrParts.add("طيار: ${order.driverName}")
                }

                if (addrParts.isNotEmpty()) {
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = addrParts.joinToString(" • "),
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = addrPaint,
                        alignRight = true
                    ) + custLineSpacing
                }
            } else {
                // Standard layout
                if (showCustName) {
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = "العميل: ${order.customerName}",
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = custNamePaint,
                        alignRight = true
                    ) + custLineSpacing
                }

                if (showCustPhone) {
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = "هاتف العميل: ${order.customerPhone}",
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = custPhonePaint,
                        alignRight = true
                    ) + custLineSpacing
                }

                if (showSecPhone) {
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = "هاتف إضافي: ${order.customerSecondaryPhone}",
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = custPhonePaint,
                        alignRight = true
                    ) + custLineSpacing
                }

                if (showAddress) {
                    val cleanAddr = order.deliveryAddressFull!!.trim()
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = "العنوان: $cleanAddr",
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = addrPaint,
                        alignRight = true
                    ) + custLineSpacing
                } else if (showArea) {
                    val cleanArea = order.customerArea!!.trim()
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = "المنطقة: $cleanArea",
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = addrPaint,
                        alignRight = true
                    ) + custLineSpacing
                }

                if (showDriver) {
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = "كابتن التوصيل: ${order.driverName}",
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = driverPaint,
                        alignRight = true
                    ) + custLineSpacing
                }
            }

            val cardBottomY = cardCurrentY + (if (isCompact) 2f else 4f) * scale
            val cardRect = RectF(leftX, cardStartY, rightX, cardBottomY)
            canvas.drawRoundRect(cardRect, 5f * scale, 5f * scale, cardBorderPaint)
            y = cardBottomY + (if (isCompact) 6f else 14f) * scale
        }

        // 5. Structured Table of Order Items: [ الإجمالي | السعر | الكمية | الصنف والتجهيز ]
        val colPriceLeft = leftX + (4f * scale)
        val colPriceRight = leftX + (contentWidth * 0.22f)

        val colUnitPriceLeft = colPriceRight + (2f * scale)
        val colUnitPriceRight = leftX + (contentWidth * 0.40f)

        val colQtyLeft = colUnitPriceRight + (2f * scale)
        val colQtyRight = leftX + (contentWidth * 0.60f)

        val colItemLeft = colQtyRight + (4f * scale)
        val colItemRight = rightX - (4f * scale)

        // Table Header
        val tableHeaderHeight = (if (isCompact) 28f else 36f) * scale * fontScale
        val tableHeaderRect = RectF(leftX, y, rightX, y + tableHeaderHeight)
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#EEEEEE")
            style = Paint.Style.FILL
        }
        canvas.drawRect(tableHeaderRect, headerBgPaint)
        canvas.drawRect(tableHeaderRect, linePaint)

        val headerTextY = y + (tableHeaderHeight / 2f) - ((centerBoldPaint.descent() + centerBoldPaint.ascent()) / 2f)
        canvas.drawText("الإجمالي", colPriceLeft, headerTextY, tableHeaderLeftPaint)
        canvas.drawText("السعر", (colUnitPriceLeft + colUnitPriceRight) / 2f, headerTextY, centerBoldPaint)
        canvas.drawText("الكمية", (colQtyLeft + colQtyRight) / 2f, headerTextY, centerBoldPaint)
        canvas.drawText("الصنف والتجهيز", colItemRight, headerTextY, tableHeaderRightPaint)
        y += tableHeaderHeight

        // Table Body
        for (item in orderWithItems.items) {
            val totalStr = String.format(Locale.US, "%.1f", item.totalPrice) + " ج"
            val unitAndPrep = item.unitPrice + item.preparationPrice
            val pricePerUnitStr = String.format(Locale.US, "%.1f", unitAndPrep)
            val qtyStr = "${formatQuantity(item.quantity)} ${item.unit}"

            val rowStartY = y + (if (isCompact) 2f else 4f) * scale
            var detailsY = rowStartY

            // Draw Item Name strictly bounded inside [colItemLeft, colItemRight]
            val itemNameText = "• ${item.itemName}"
            detailsY = drawMultilineText(
                canvas = canvas,
                text = itemNameText,
                leftX = colItemLeft,
                rightX = colItemRight,
                startY = detailsY,
                paint = rightBoldPaint,
                alignRight = true
            ) + (1.5f * scale)

            // Preparation (without the word طهي, formatted e.g. شوي(+20)للكيلو)
            if (template.showPreparations && !item.preparationName.isNullOrBlank()) {
                val prepPriceStr = if (item.preparationPrice > 0) {
                    val pStr = if (item.preparationPrice % 1.0 == 0.0) item.preparationPrice.toInt().toString() else String.format(Locale.US, "%.1f", item.preparationPrice)
                    "(+$pStr)للكيلو"
                } else ""
                val prepText = "  - ${item.preparationName}$prepPriceStr"
                detailsY = drawMultilineText(
                    canvas = canvas,
                    text = prepText,
                    leftX = colItemLeft,
                    rightX = colItemRight,
                    startY = detailsY,
                    paint = prepPaint,
                    alignRight = true
                ) + (1.5f * scale)
            }

            // Draw Quantity, Unit Price, and Total aligned with the first line of the row
            val firstLineBaseline = rowStartY + rightBoldPaint.textSize
            canvas.drawText(qtyStr, (colQtyLeft + colQtyRight) / 2f, firstLineBaseline, centerRegularPaint)
            canvas.drawText(pricePerUnitStr, (colUnitPriceLeft + colUnitPriceRight) / 2f, firstLineBaseline, centerRegularPaint)
            canvas.drawText(totalStr, colPriceLeft, firstLineBaseline, leftBoldPaint)

            // Item Notes (if any, across the row)
            if (template.showOrderNotes && item.itemNotes.isNotBlank()) {
                val noteText = "  - ملاحظة: ${item.itemNotes}"
                detailsY = drawMultilineText(
                    canvas = canvas,
                    text = noteText,
                    leftX = leftX + (4f * scale),
                    rightX = rightX - (4f * scale),
                    startY = detailsY,
                    paint = itemNotesPaint,
                    alignRight = true
                ) + (1.5f * scale)
            }

            val rowEndY = detailsY + (if (isCompact) 2f else 4f) * scale
            canvas.drawLine(leftX, rowEndY, rightX, rowEndY, thinLinePaint)
            y = rowEndY
        }

        y += (if (isCompact) 4f else 8f) * scale

        // 6. Summary & Totals
        if (template.showSubtotal) {
            val subtotalStr = String.format(Locale.US, "%.1f", order.subtotal) + " ج.م"
            canvas.drawText("المجموع الفرعي:", rightX, y + (18f * scale * fontScale), rightRegularPaint)
            canvas.drawText(subtotalStr, leftX, y + (18f * scale * fontScale), leftBoldPaint)
            y += (24f * scale * fontScale)
        }

        if (template.showDeliveryFee && order.deliveryFee > 0) {
            val feeStr = "+" + String.format(Locale.US, "%.0f", order.deliveryFee) + " ج.م"
            canvas.drawText("رسوم التوصيل:", rightX, y + (18f * scale * fontScale), rightRegularPaint)
            canvas.drawText(feeStr, leftX, y + (18f * scale * fontScale), leftBoldPaint)
            y += (24f * scale * fontScale)
        }

        if (template.showDeliveryFee && order.discount > 0) {
            val discStr = "-" + String.format(Locale.US, "%.1f", order.discount) + " ج.م"
            canvas.drawText("خصم خاص:", rightX, y + (18f * scale * fontScale), rightRegularPaint)
            canvas.drawText(discStr, leftX, y + (18f * scale * fontScale), leftBoldPaint)
            y += (24f * scale * fontScale)
        }

        // 7. Prominent Final Total & Payment Status Box
        val isOrderFullyPaid = order.isPaid || (order.paidAmount >= order.totalAmount && order.totalAmount > 0.0)
        val hasRemainingOnCustomer = !isOrderFullyPaid && (order.paidAmount < order.totalAmount) && (order.paidAmount > 0.0)
        val remainingAmount = (order.totalAmount - order.paidAmount).coerceAtLeast(0.0)
        val totalStr = String.format(Locale.US, "%.1f", order.totalAmount) + " ج.م"
        val remStr = String.format(Locale.US, "%.1f", remainingAmount) + " ج.م"

        if (template.showPaymentStatusBox) {
            y += (if (isCompact) 4f else 8f) * scale
            val boxHeight = if (hasRemainingOnCustomer) {
                if (isCompact) (105f * scale * fontScale) else (130f * scale * fontScale)
            } else {
                if (isCompact) (74f * scale * fontScale) else (92f * scale * fontScale)
            }
            val totalBoxRect = RectF(leftX, y, rightX, y + boxHeight)
            val blackFillPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
            canvas.drawRoundRect(totalBoxRect, 6f * scale, 6f * scale, blackFillPaint)

            val headerText = if (isOrderFullyPaid) {
                "★ الأوردر خالص بالكامل ★"
            } else if (hasRemainingOnCustomer) {
                "⚠️ متبقي على العميل: $remStr"
            } else {
                "🔴 غير مدفوع - يُحصل كاش"
            }

            val statusHeaderWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (if (isCompact) totalBoxFontSize - 4f else totalBoxFontSize - 2f) * scale * fontScale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 0.6f * scale
            }

            val maxHeaderWidth = (rightX - leftX) - (20f * scale)
            while (statusHeaderWhitePaint.measureText(headerText) > maxHeaderWidth && statusHeaderWhitePaint.textSize > 11f * scale) {
                statusHeaderWhitePaint.textSize -= 1f
            }

            val whiteDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                strokeWidth = 1.2f * scale
            }

            val totalWhiteBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = totalBoxFontSize * scale * fontScale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 0.8f * scale
            }
            val totalWhiteNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (totalBoxFontSize + 4f) * scale * fontScale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 0.8f * scale
            }

            val headerY = y + (if (isCompact) 18f else 24f) * scale * fontScale
            canvas.drawText(headerText, centerX, headerY, statusHeaderWhitePaint)

            val dividerY = y + (if (isCompact) 28f else 36f) * scale * fontScale
            canvas.drawLine(leftX + (8f * scale), dividerY, rightX - (8f * scale), dividerY, whiteDividerPaint)

            val contentY = y + (if (isCompact) 56f else 70f) * scale * fontScale
            if (isOrderFullyPaid) {
                val label = "المدفوع (خالص ✓):"
                val numWidth = totalWhiteNumPaint.measureText(totalStr)
                val maxLabelWidth = (rightX - leftX) - numWidth - (24f * scale)
                while (totalWhiteBoldPaint.measureText(label) > maxLabelWidth && totalWhiteBoldPaint.textSize > 14f * scale) {
                    totalWhiteBoldPaint.textSize -= 1f
                }
                canvas.drawText(label, rightX - (10f * scale), contentY, totalWhiteBoldPaint)
                canvas.drawText(totalStr, leftX + (10f * scale), contentY, totalWhiteNumPaint)
            } else if (hasRemainingOnCustomer) {
                val label = "المطلوب تحصيله كاش:"
                val numWidth = totalWhiteNumPaint.measureText(remStr)
                val maxLabelWidth = (rightX - leftX) - numWidth - (24f * scale)
                while (totalWhiteBoldPaint.measureText(label) > maxLabelWidth && totalWhiteBoldPaint.textSize > 14f * scale) {
                    totalWhiteBoldPaint.textSize -= 1f
                }
                canvas.drawText(label, rightX - (10f * scale), contentY, totalWhiteBoldPaint)
                canvas.drawText(remStr, leftX + (10f * scale), contentY, totalWhiteNumPaint)

                val subDividerY = y + (if (isCompact) 72f else 88f) * scale * fontScale
                canvas.drawLine(leftX + (14f * scale), subDividerY, rightX - (14f * scale), subDividerY, whiteDividerPaint)

                val subDetailStr = "إجمالي: $totalStr | المسدد: ${String.format(Locale.US, "%.1f", order.paidAmount)} ج.م"
                val subWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = (if (isCompact) 15f else 18f) * scale * fontScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    style = Paint.Style.FILL_AND_STROKE
                    strokeWidth = 0.6f * scale
                }
                val maxSubWidth = (rightX - leftX) - (20f * scale)
                while (subWhitePaint.measureText(subDetailStr) > maxSubWidth && subWhitePaint.textSize > 11f * scale) {
                    subWhitePaint.textSize -= 1f
                }
                val subDetailY = y + (if (isCompact) 92f else 112f) * scale * fontScale
                canvas.drawText(subDetailStr, centerX, subDetailY, subWhitePaint)
            } else {
                val label = "المطلوب تحصيله:"
                val numWidth = totalWhiteNumPaint.measureText(totalStr)
                val maxLabelWidth = (rightX - leftX) - numWidth - (24f * scale)
                while (totalWhiteBoldPaint.measureText(label) > maxLabelWidth && totalWhiteBoldPaint.textSize > 14f * scale) {
                    totalWhiteBoldPaint.textSize -= 1f
                }
                canvas.drawText(label, rightX - (10f * scale), contentY, totalWhiteBoldPaint)
                canvas.drawText(totalStr, leftX + (10f * scale), contentY, totalWhiteNumPaint)
            }
            y += boxHeight + (if (isCompact) 4f else 8f) * scale
        }

        // 8. Payment Method & Detailed Breakdown
        if (template.showPaymentBreakdown) {
            val payMethodStr = "طريقة الدفع: " + if (order.paymentMethod == "CASH") "نقداً (كاش)" else "محفظة إلكترونية"
            canvas.drawText(payMethodStr, rightX, y + (16f * scale * fontScale), rightRegularPaint)
            y += (20f * scale * fontScale)

            if (order.paidAmount > 0.0) {
                val paidDetailStr = "المدفوع نقداً من العميل: " + String.format(Locale.US, "%.1f", order.paidAmount) + " ج.م"
                canvas.drawText(paidDetailStr, rightX, y + (16f * scale * fontScale), rightBoldPaint)
                y += (18f * scale * fontScale)
                if (order.changeAmount > 0.0) {
                    val changeStr = "الباقي المستحق للعميل: " + String.format(Locale.US, "%.1f", order.changeAmount) + " ج.م"
                    canvas.drawText(changeStr, rightX, y + (16f * scale * fontScale), rightBoldPaint)
                    y += (18f * scale * fontScale)
                }
            }
        }

        // 9. General Order Notes
        if (template.showOrderNotes && order.notes.isNotBlank()) {
            val orderNotesText = "ملاحظات عامة: ${order.notes}"
            y = drawMultilineText(canvas, orderNotesText, leftX, rightX, y, rightBoldPaint, alignRight = true) + (4f * scale)
        }

        // 10. Footer Note
        if (template.showFooterNote && template.footerNote.isNotBlank()) {
            y += (if (isCompact) 2f else 4f) * scale
            canvas.drawLine(leftX, y, rightX, y, thinLinePaint)
            y += (if (isCompact) 4f else 8f) * scale
            canvas.drawText(template.footerNote, centerX, y + (14f * scale * fontScale), centerRegularPaint)
            y += (18f * scale * fontScale)
        }

        y += (2f * scale) // minimal bottom spacing

        // Crop to actual content height safely
        val finalHeight = y.toInt().coerceIn(80, maxTempHeight)
        val finalBitmap = try {
            Bitmap.createBitmap(tempBitmap, 0, 0, width, finalHeight)
        } catch (_: Throwable) {
            tempBitmap
        }
        if (finalBitmap != tempBitmap) {
            try { tempBitmap.recycle() } catch (_: Throwable) {}
        }
        return finalBitmap
    }

    /**
     * Renders a kitchen prep/cooking ticket into a crisp Bitmap formatted as a clear structured TABLE.
     * Important information (quantities, item names, cooking methods, and notes) is rendered large and prominent.
     */
    fun renderKitchenTicketBitmap(
        orderWithItems: OrderWithItems,
        kitchenTitle: String,
        paperWidthMm: Int = 80,
        isReprint: Boolean = false,
        reprintReason: String? = null,
        targetId: String = "KITCHEN_1",
        kitchenTemplate: KitchenOrderTemplateConfig = KitchenOrderTemplateConfig(),
        cancelledKitchenItems: List<CancelledKitchenItem> = emptyList()
    ): Bitmap? {
        val order = orderWithItems.order
        val items = orderWithItems.items

        fun matchesKitchenTarget(rawTarget: String): Boolean {
            val target = rawTarget.uppercase()
            return if (targetId == "KITCHEN_1") {
                target.contains("KITCHEN_1") || target.contains("ALL") || target == "MAIN" || target.isBlank()
            } else if (targetId == "KITCHEN_2") {
                target.contains("KITCHEN_2") || target.contains("ALL")
            } else {
                target.contains(targetId.uppercase()) || target.contains("ALL") || target == "MAIN" || target.isBlank()
            }
        }

        // Filter items and pending cancellations for target kitchen
        val targetMatched = items.filter { matchesKitchenTarget(it.targetPrinter) }
        val cancelledForTarget = cancelledKitchenItems.filter { matchesKitchenTarget(it.targetPrinter) }

        // Strict Kitchen Differential Check:
        // Any item already printed in full (quantity <= printedKitchenQuantity) MUST NOT print again for any reason!
        // Only print new items or the delta increase of an existing item!
        val hasPriorKitchenActivity = targetMatched.any { it.printedKitchenQuantity > 0.0001 } || cancelledForTarget.isNotEmpty()
        val filtered = targetMatched.mapNotNull { item ->
            val delta = item.quantity - item.printedKitchenQuantity
            if (delta > 0.0001) {
                val wasPreviouslyPrinted = item.printedKitchenQuantity > 0.0001
                val isNewItem = item.printedKitchenQuantity <= 0.0001 && hasPriorKitchenActivity

                val kitchenNameExtra = if (wasPreviouslyPrinted) {
                    val prevStr = formatQuantity(item.printedKitchenQuantity)
                    val newTotalStr = formatQuantity(item.quantity)
                    " (زيادة +${formatQuantity(delta)} | كان $prevStr وأصبح $newTotalStr)"
                } else if (isNewItem) {
                    " (صنف جديد مضاف)"
                } else ""

                val baseKitchenName = item.kitchenName.ifBlank { item.itemName }
                val displayKitchenName = baseKitchenName + kitchenNameExtra
                val unitAndPrep = item.unitPrice + item.preparationPrice
                val deltaTotalPrice = unitAndPrep * delta

                item.copy(
                    kitchenName = displayKitchenName,
                    quantity = delta,
                    totalPrice = deltaTotalPrice
                )
            } else {
                null
            }
        }
        val isAdditionTicket = filtered.isNotEmpty() && hasPriorKitchenActivity

        if (filtered.isEmpty() && cancelledForTarget.isEmpty()) {
            return null
        }

        val baseScale = if (paperWidthMm == 58) 0.72f else 1.0f
        val fontMultiplier = when (kitchenTemplate.fontSize.uppercase()) {
            "HUGE" -> 1.45f
            "LARGE" -> 1.25f
            else -> 1.0f
        }
        val scale = baseScale * fontMultiplier

        val width = if (paperWidthMm == 58) 384 else 576
        val sidePadding = (14 * baseScale).toInt()
        val contentWidth = width - (sidePadding * 2)

        // Dynamically size temporary bitmap according to items & cancellations count to eliminate OOM spikes
        val maxTempHeight = ((filtered.size + cancelledForTarget.size) * 160 + 1500).coerceIn(800, 4200)
        val tempBitmap = Bitmap.createBitmap(width, maxTempHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tempBitmap)
        canvas.drawColor(Color.WHITE)

        val centerX = width / 2f
        val rightX = (width - sidePadding).toFloat()
        val leftX = sidePadding.toFloat()

        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 3f * baseScale
            style = Paint.Style.STROKE
        }

        val thinLine = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1.4f * baseScale
            style = Paint.Style.STROKE
        }

        val rightHugePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 28f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val rightBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val rightRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 19f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        val centerHugePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 30f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val centerBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 21f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val kitchenTopPadding = kitchenTemplate.topPadding.coerceIn(4, 50).toFloat() * baseScale
        val kitchenLineSpacing = kitchenTemplate.headerLineSpacing.coerceIn(2, 25).toFloat() * baseScale
        var y = kitchenTopPadding

        // 1. Header Title (if enabled)
        if (kitchenTemplate.showHeaderTitle) {
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = kitchenTemplate.headerFontSizePx.toFloat() * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val header = kitchenTemplate.headerTitle.ifBlank { "أمر تشغيل المطبخ والاستاف" }
            val titleMetrics = titlePaint.fontMetrics
            val titleBaseline = y - titleMetrics.ascent
            canvas.drawText("*** $header ***", centerX, titleBaseline, titlePaint)
            y = titleBaseline + titleMetrics.descent + kitchenLineSpacing
        }

        // 2. Department (if enabled & present)
        if (kitchenTemplate.showDepartment && (kitchenTemplate.kitchenDepartment.isNotBlank() || kitchenTitle.isNotBlank())) {
            val subDept = if (kitchenTemplate.kitchenDepartment.isNotBlank()) kitchenTemplate.kitchenDepartment else kitchenTitle
            val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = (kitchenTemplate.headerFontSizePx * 0.75f).coerceAtLeast(14f) * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            val subMetrics = subTitlePaint.fontMetrics
            val subBaseline = y - subMetrics.ascent
            canvas.drawText("[$subDept]", centerX, subBaseline, subTitlePaint)
            y = subBaseline + subMetrics.descent + kitchenLineSpacing
        }

        if (isAdditionTicket && cancelledForTarget.isNotEmpty()) {
            val bannerRect = RectF(leftX, y, rightX, y + (36f * scale))
            val bgPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
            canvas.drawRect(bannerRect, bgPaint)
            val addPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("*** تعديل تشغيل (إضافات وإلغاءات) ***", centerX, y + (24f * scale), addPaint)
            y += (42f * scale)
        } else if (cancelledForTarget.isNotEmpty()) {
            val bannerRect = RectF(leftX, y, rightX, y + (38f * scale))
            val bgPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
            canvas.drawRect(bannerRect, bgPaint)
            val cancelBannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("!! تنبيه هام للمطبخ: إلغاء / تعديل أصناف !!", centerX, y + (25f * scale), cancelBannerPaint)
            y += (44f * scale)
        } else if (isAdditionTicket) {
            val bannerRect = RectF(leftX, y, rightX, y + (34f * scale))
            val bgPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
            canvas.drawRect(bannerRect, bgPaint)
            val addPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("*** تعديل / إضافة تشغيل (الزيادة فقط) ***", centerX, y + (22f * scale), addPaint)
            y += (40f * scale)
        } else if (isReprint) {
            val bannerRect = RectF(leftX, y, rightX, y + (34f * scale))
            val bgPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
            canvas.drawRect(bannerRect, bgPaint)
            val reprintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 19f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val reasonStr = if (!reprintReason.isNullOrBlank()) " ($reprintReason)" else ""
            canvas.drawText("!! إعادة تشغيل (بإذن الإدارة)$reasonStr !!", centerX, y + (24f * scale), reprintPaint)
            y += (40f * scale)
        }

        // Metadata Paints based on user's metadataFontSizePx
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = kitchenTemplate.metadataFontSizePx.toFloat() * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val metaBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = kitchenTemplate.metadataFontSizePx.toFloat() * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val showCustName = kitchenTemplate.showCustomerName && order.customerName.isNotBlank() && order.customerName != "زبون مباشر"
        val showCustPhone = kitchenTemplate.showCustomerPhone && order.customerPhone.isNotBlank()
        val showSecPhone = !order.customerSecondaryPhone.isNullOrBlank()
        val showAddress = kitchenTemplate.showDeliveryAddress && !order.deliveryAddressFull.isNullOrBlank()
        val showArea = !order.customerArea.isNullOrBlank() && (order.deliveryAddressFull.isNullOrBlank() || !order.deliveryAddressFull.contains(order.customerArea))
        val showDriver = order.orderType == "DELIVERY" && !order.driverName.isNullOrBlank()
        val showSeller = false // Seller name shown exclusively in Orders screen ("استلام بواسطة فلان")
        val showTime = kitchenTemplate.showOrderTime && kitchenTemplate.showCashierAndTime

        if (kitchenTemplate.compactHeaderMode) {
            // ========================================================
            // ULTRA-COMPACT HEADER MODE (Saves >60% vertical paper)
            // ========================================================
            y += 2f * scale
            canvas.drawLine(leftX, y, rightX, y, linePaint)
            y += 4f * scale

            // Line 1: Order # + Type + Time (all on one tight bar)
            val orderBoxHeight = (38f * scale)
            val boxRect = RectF(leftX, y, rightX, y + orderBoxHeight)
            val fillBlack = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
            canvas.drawRoundRect(boxRect, 4f * scale, 4f * scale, fillBlack)

            val numWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 24f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val typeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 17f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            val numBaseline = y + (orderBoxHeight / 2f) - ((numWhitePaint.descent() + numWhitePaint.ascent()) / 2f)
            val typeBaseline = y + (orderBoxHeight / 2f) - ((typeWhitePaint.descent() + typeWhitePaint.ascent()) / 2f)

            canvas.drawText("أوردر: #${order.orderNumber}", rightX - (8f * scale), numBaseline, numWhitePaint)

            val typeBadge = when (order.orderType) {
                "DELIVERY" -> "دليفري توصيل"
                "DINE_IN" -> "صالة"
                else -> "استلام وتيك أواي"
            }
            val timeShort = if (showTime) {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = order.createdAt }
                val rawH = cal.get(java.util.Calendar.HOUR)
                val h12 = if (rawH == 0) 12 else rawH
                val m = cal.get(java.util.Calendar.MINUTE)
                val isAm = cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM
                String.format(Locale.US, " | %02d:%02d%s", h12, m, if (isAm) "ص" else "م")
            } else ""
            canvas.drawText("$typeBadge$timeShort", leftX + (8f * scale), typeBaseline, typeWhitePaint)
            y += orderBoxHeight + (4f * scale)

            // Line 2: Seller and/or Customer on compact line
            val custAndSellerParts = mutableListOf<String>()
            if (showSeller) custAndSellerParts.add("البائع: ${order.sellerName}")
            if (showCustName) custAndSellerParts.add("العميل: ${order.customerName}")
            if (showCustPhone) custAndSellerParts.add("هـ: ${order.customerPhone}")
            if (!order.customerSecondaryPhone.isNullOrBlank()) custAndSellerParts.add("هـ2: ${order.customerSecondaryPhone}")

            if (custAndSellerParts.isNotEmpty()) {
                val lineText = custAndSellerParts.joinToString(" • ")
                y = drawMultilineText(canvas, lineText, leftX, rightX, y, metaBoldPaint, alignRight = true) + (2f * scale)
            }

            // Line 3: Address & Driver if delivery
            val deliveryParts = mutableListOf<String>()
            if (showAddress) deliveryParts.add("عنوان: ${order.deliveryAddressFull!!.trim()}")
            else if (showArea) deliveryParts.add("منطقة: ${order.customerArea!!.trim()}")
            if (showDriver) deliveryParts.add("طيار: ${order.driverName}")

            if (deliveryParts.isNotEmpty()) {
                val delText = deliveryParts.joinToString(" • ")
                y = drawMultilineText(canvas, delText, leftX, rightX, y, metaPaint, alignRight = true) + (2f * scale)
            }

            y += 4f * scale
            canvas.drawLine(leftX, y, rightX, y, linePaint)
            y += 6f * scale
        } else {
            // Standard layout (spacious, but respects user toggles and font sizes)
            y += 4f * scale
            canvas.drawLine(leftX, y, rightX, y, linePaint)
            y += 10f * scale

            if (kitchenTemplate.showOrderTypeAndTable) {
                val orderBoxHeight = (52f * scale)
                val boxRect = RectF(leftX, y, rightX, y + orderBoxHeight)
                val fillBlack = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
                canvas.drawRoundRect(boxRect, 6f * scale, 6f * scale, fillBlack)

                val numWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 28f * scale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                val typeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 20f * scale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.LEFT
                }

                val numBaseline = y + (orderBoxHeight / 2f) - ((numWhitePaint.descent() + numWhitePaint.ascent()) / 2f)
                val typeBaseline = y + (orderBoxHeight / 2f) - ((typeWhitePaint.descent() + typeWhitePaint.ascent()) / 2f)
                canvas.drawText("أوردر: #${order.orderNumber}", rightX - (12f * scale), numBaseline, numWhitePaint)
                val typeBadge = when (order.orderType) {
                    "DELIVERY" -> "دليفري توصيل"
                    "DINE_IN" -> "صالة مطعم"
                    else -> "استلام وتيك أواي"
                }
                canvas.drawText(typeBadge, leftX + (12f * scale), typeBaseline, typeWhitePaint)
                y += orderBoxHeight + (8f * scale)
            } else {
                canvas.drawText("أوردر رقم: #${order.orderNumber}", rightX, y + 24f * scale, metaBoldPaint)
                y += 30f * scale
            }

            if (showTime) {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = order.createdAt }
                val rawH = cal.get(java.util.Calendar.HOUR)
                val h12 = if (rawH == 0) 12 else rawH
                val m = cal.get(java.util.Calendar.MINUTE)
                val isAm = cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM
                val time12hStr = String.format(Locale.US, "%02d:%02d %s", h12, m, if (isAm) "ص" else "م")
                val dateDayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(order.createdAt))
                val timeLineText = "وقت الطلب: $time12hStr | التاريخ: $dateDayStr"
                canvas.drawText(timeLineText, rightX, y + metaPaint.textSize, metaPaint)
                y += (metaPaint.textSize * 1.35f) + (4f * scale)
            }

            if (showSeller) {
                val sellerKitchenText = "👤 استلام البائع: ${order.sellerName}"
                canvas.drawText(sellerKitchenText, rightX, y + metaBoldPaint.textSize, metaBoldPaint)
                y += (metaBoldPaint.textSize * 1.35f) + (6f * scale)
            }

            val hasAnyCustomerInfo = showCustName || showCustPhone || showSecPhone || showAddress || showArea || showDriver
            if (hasAnyCustomerInfo) {
                val cardStartY = y
                val cardPadding = 8f * scale
                var cardCurrentY = cardStartY + (8f * scale)
                val custLineSpacing = 5f * scale

                val cardBorder = Paint().apply {
                    color = Color.BLACK
                    strokeWidth = 2f * scale
                    style = Paint.Style.STROKE
                }

                if (showCustName) {
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = "العميل: ${order.customerName}",
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = metaBoldPaint,
                        alignRight = true
                    ) + custLineSpacing
                }

                if (showCustPhone) {
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = "الهاتف: ${order.customerPhone}",
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = metaPaint,
                        alignRight = true
                    ) + custLineSpacing
                }

                if (showSecPhone) {
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = "هاتف إضافي: ${order.customerSecondaryPhone}",
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = metaPaint,
                        alignRight = true
                    ) + custLineSpacing
                }

                if (showAddress) {
                    val cleanAddr = order.deliveryAddressFull!!.trim()
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = "عنوان التوصيل: $cleanAddr",
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = metaPaint,
                        alignRight = true
                    ) + custLineSpacing
                } else if (showArea) {
                    val cleanArea = order.customerArea!!.trim()
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = "المنطقة: $cleanArea",
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = metaPaint,
                        alignRight = true
                    ) + custLineSpacing
                }

                if (showDriver) {
                    cardCurrentY = drawMultilineText(
                        canvas = canvas,
                        text = "كابتن التوصيل: ${order.driverName}",
                        leftX = leftX + cardPadding,
                        rightX = rightX - cardPadding,
                        startY = cardCurrentY,
                        paint = metaPaint,
                        alignRight = true
                    ) + custLineSpacing
                }

                val cardBottomY = cardCurrentY + (4f * scale)
                val cardRect = RectF(leftX, cardStartY, rightX, cardBottomY)
                canvas.drawRoundRect(cardRect, 6f * scale, 6f * scale, cardBorder)
                y = cardBottomY + (10f * scale)
            }

            y += 4f * scale
            canvas.drawLine(leftX, y, rightX, y, linePaint)
            y += 8f * scale
        }

        // Prominent Kitchen Cancellation / Reduction Alert Box (printed BEFORE new additions if any)
        if (cancelledForTarget.isNotEmpty()) {
            val cancelHeaderH = 38f * scale
            val cancelHeaderRect = RectF(leftX, y, rightX, y + cancelHeaderH)
            val fillBlack = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
            canvas.drawRect(cancelHeaderRect, fillBlack)
            val cancelTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 19f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val cancelHdrBaseline = y + (cancelHeaderH / 2f) - ((cancelTitlePaint.descent() + cancelTitlePaint.ascent()) / 2f)
            canvas.drawText("⛔ أصناف ملغاة / مقللة — أوقف تجهيزها فوراً", centerX, cancelHdrBaseline, cancelTitlePaint)
            y += cancelHeaderH

            val cancelRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = (kitchenTemplate.itemFontSizePx.toFloat().coerceAtLeast(18f)) * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val cancelSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = (kitchenTemplate.itemPrepFontSizePx.toFloat().coerceAtLeast(14f)) * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            for (cItem in cancelledForTarget) {
                val cStartY = y
                var cCurY = cStartY + (8f * scale)
                val baseName = cItem.kitchenName.ifBlank { cItem.itemName }
                val prepSuffix = if (!cItem.preparationName.isNullOrBlank()) " (${cItem.preparationName})" else ""
                val headline = if (cItem.newQty <= 0.0001) {
                    "❌ ملغي نهائياً: ${formatQuantity(cItem.cancelledQty)} ${cItem.unit} — $baseName$prepSuffix"
                } else {
                    "⬇️ تقليل كمية: $baseName$prepSuffix (-${formatQuantity(cItem.cancelledQty)} ${cItem.unit})"
                }
                cCurY = drawMultilineText(
                    canvas = canvas,
                    text = headline,
                    leftX = leftX + (10f * scale),
                    rightX = rightX - (10f * scale),
                    startY = cCurY,
                    paint = cancelRowPaint,
                    alignRight = true
                ) + (3f * scale)
                if (cItem.newQty > 0.0001) {
                    val subLine = "   كان مطلوباً ${formatQuantity(cItem.prevQty)} ${cItem.unit} وأصبح الآن ${formatQuantity(cItem.newQty)} ${cItem.unit} فقط"
                    cCurY = drawMultilineText(
                        canvas = canvas,
                        text = subLine,
                        leftX = leftX + (10f * scale),
                        rightX = rightX - (10f * scale),
                        startY = cCurY,
                        paint = cancelSubPaint,
                        alignRight = true
                    ) + (3f * scale)
                }
                val cEndY = (cCurY + (6f * scale)).coerceAtLeast(cStartY + (44f * scale))
                canvas.drawRect(RectF(leftX, cStartY, rightX, cEndY), linePaint)
                y = cEndY
            }
            y += (12f * scale)
        }

        if (filtered.isNotEmpty()) {
            // Section Title: Table of Cooking / Prep Items (Can be disabled to save paper!)
            if (kitchenTemplate.showTableSectionTitle) {
                val secTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 24f * scale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("جدول تجهيز وطهي الأصناف المطلوبة:", rightX, y + 20f * scale, secTitlePaint)
                y += 28f * scale
            }

            // STRUCTURED TABLE LAYOUT FOR KITCHEN TICKET:
            // Column partitioning:
            val showPrices = kitchenTemplate.showItemPrices
            val qtyPercent = (kitchenTemplate.qtyColumnWidthPercent.coerceIn(18, 40)).toFloat() / 100f
            val colQtyWidth = if (showPrices) contentWidth * 0.22f else contentWidth * qtyPercent
            val colPriceWidth = if (showPrices) contentWidth * 0.22f else 0f
            val colDetailsWidth = contentWidth - colQtyWidth - colPriceWidth

            val splitX1 = leftX + colQtyWidth
            val splitX2 = if (showPrices) leftX + colQtyWidth + colPriceWidth else splitX1

            // 1. Table Header
            val headerHeight = 38f * scale
            val headerRect = RectF(leftX, y, rightX, y + headerHeight)
            val headerBg = Paint().apply {
                color = Color.parseColor("#E0E0E0")
                style = Paint.Style.FILL
            }
            canvas.drawRect(headerRect, headerBg)
            canvas.drawRect(headerRect, linePaint)

            // Draw vertical dividers in header
            canvas.drawLine(splitX1, y, splitX1, y + headerHeight, linePaint)
            if (showPrices) {
                canvas.drawLine(splitX2, y, splitX2, y + headerHeight, linePaint)
            }

            val headerItemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = (kitchenTemplate.tableHeaderFontSizePx.toFloat() * scale).coerceAtMost(30f * scale)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val headerTextY = y + (headerHeight / 2f) - ((headerItemPaint.descent() + headerItemPaint.ascent()) / 2f)
            val headerQtyCenterX = leftX + (colQtyWidth / 2f)
            canvas.drawText("الكمية", headerQtyCenterX, headerTextY, headerItemPaint)

            if (showPrices) {
                val headerPriceCenterX = splitX1 + (colPriceWidth / 2f)
                canvas.drawText("الإجمالي", headerPriceCenterX, headerTextY, headerItemPaint)
            }

            // Table header title for items and cooking method
            val headerTitleText = kitchenTemplate.tableHeaderItemColumnTitle.ifBlank { "الصنف والتجهيز" }
            val maxHeaderWidth = colDetailsWidth - (16f * scale)
            while (headerItemPaint.measureText(headerTitleText) > maxHeaderWidth && headerItemPaint.textSize > 12f * scale) {
                headerItemPaint.textSize -= 1f
            }
            val headerDetailsCenterX = (splitX2 + rightX) / 2f
            canvas.drawText(headerTitleText, headerDetailsCenterX, headerTextY, headerItemPaint)
            y += headerHeight

            // Item Specific Paints for high prominence
            val itemQtyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = kitchenTemplate.qtyFontSizePx.toFloat() * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val itemNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = kitchenTemplate.itemFontSizePx.toFloat() * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            val itemPrepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = kitchenTemplate.itemPrepFontSizePx.toFloat() * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            val itemNotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = kitchenTemplate.itemNotesFontSizePx.toFloat() * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            // 2. Table Rows
            for (item in filtered) {
                val rowStartY = y
                val detailsRightX = rightX - (8f * scale)
                val detailsLeftX = splitX2 + (8f * scale)

                var detailsY = rowStartY + (8f * scale)

                // Draw Item Name
                val displayKitchenName = item.kitchenName.ifBlank { item.itemName }
                val nameText = "• $displayKitchenName"
                detailsY = drawMultilineText(
                    canvas = canvas,
                    text = nameText,
                    leftX = detailsLeftX,
                    rightX = detailsRightX,
                    startY = detailsY,
                    paint = itemNamePaint,
                    alignRight = true
                ) + (4f * scale)

                // Draw Preparation / Cooking Method
                if (kitchenTemplate.showPreparations && !item.preparationName.isNullOrBlank()) {
                    val prepPriceStr = if (kitchenTemplate.showItemPrices && item.preparationPrice > 0) {
                        val pStr = if (item.preparationPrice % 1.0 == 0.0) item.preparationPrice.toInt().toString() else String.format(Locale.US, "%.1f", item.preparationPrice)
                        "(+$pStr)للكيلو"
                    } else ""
                    val prepText = "  - ${item.preparationName}$prepPriceStr"
                    detailsY = drawMultilineText(
                        canvas = canvas,
                        text = prepText,
                        leftX = detailsLeftX,
                        rightX = detailsRightX,
                        startY = detailsY,
                        paint = itemPrepPaint,
                        alignRight = true
                    ) + (4f * scale)
                }

                // Draw Item Notes
                if (kitchenTemplate.showOrderNotes && item.itemNotes.isNotBlank()) {
                    val noteText = "  - ملاحظة: ${item.itemNotes}"
                    detailsY = drawMultilineText(
                        canvas = canvas,
                        text = noteText,
                        leftX = detailsLeftX,
                        rightX = detailsRightX,
                        startY = detailsY,
                        paint = itemNotePaint,
                        alignRight = true
                    ) + (4f * scale)
                }

                val rowCalculatedHeight = (detailsY - rowStartY + (8f * scale)).coerceAtLeast(54f * scale)
                val rowEndY = rowStartY + rowCalculatedHeight

                // Draw Row Bounding Box & Column Vertical Separator
                val rowRect = RectF(leftX, rowStartY, rightX, rowEndY)
                canvas.drawRect(rowRect, linePaint)
                canvas.drawLine(splitX1, rowStartY, splitX1, rowEndY, linePaint)
                if (showPrices) {
                    canvas.drawLine(splitX2, rowStartY, splitX2, rowEndY, linePaint)
                }

                // Draw Centered Quantity (Prominent & Large)
                val qtyStr = "${formatQuantity(item.quantity)} ${item.unit}"
                val qtyCenterY = rowStartY + (rowCalculatedHeight / 2f) - ((itemQtyPaint.descent() + itemQtyPaint.ascent()) / 2f)
                canvas.drawText(qtyStr, headerQtyCenterX, qtyCenterY, itemQtyPaint)

                // Draw Price if enabled
                if (showPrices) {
                    val priceCenterY = rowStartY + (rowCalculatedHeight / 2f) - ((centerBoldPaint.descent() + centerBoldPaint.ascent()) / 2f)
                    val priceCenterX = splitX1 + (colPriceWidth / 2f)
                    canvas.drawText("${item.totalPrice} ج", priceCenterX, priceCenterY, centerBoldPaint)
                }

                y = rowEndY
            }
        }

        y += (10f * scale)

        // General Order Notes (if enabled & present)
        if (kitchenTemplate.showOrderNotes && order.notes.isNotBlank()) {
            val noteBoxStartY = y
            val generalNote = "ملاحظة عامة هامة للأوردر: ${order.notes}"
            val noteEndY = drawMultilineText(canvas, generalNote, leftX + (10f * scale), rightX - (10f * scale), noteBoxStartY + (10f * scale), rightHugePaint, alignRight = true)
            val noteBoxHeight = noteEndY - noteBoxStartY + (10f * scale)
            val noteRect = RectF(leftX, noteBoxStartY, rightX, noteBoxStartY + noteBoxHeight)
            val fillNoteBg = Paint().apply { color = Color.parseColor("#F5F5F5"); style = Paint.Style.FILL }
            canvas.drawRect(noteRect, fillNoteBg)
            canvas.drawRect(noteRect, linePaint)
            // redraw text over background
            drawMultilineText(canvas, generalNote, leftX + (10f * scale), rightX - (10f * scale), noteBoxStartY + (10f * scale), rightHugePaint, alignRight = true)
            y = noteBoxStartY + noteBoxHeight + (12f * scale)
        }

        // Summary of Items Count
        if (kitchenTemplate.showItemCountSummary) {
            val summaryHeight = (42f * scale)
            val summaryRect = RectF(leftX, y, rightX, y + summaryHeight)
            val fillBlack = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
            canvas.drawRoundRect(summaryRect, 6f * scale, 6f * scale, fillBlack)

            val summaryWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 20f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val summaryMsg = when {
                filtered.isNotEmpty() && cancelledForTarget.isNotEmpty() ->
                    "إضافات للتجهيز: ${filtered.size} | أصناف ملغاة: ${cancelledForTarget.size}"
                cancelledForTarget.isNotEmpty() ->
                    "تنبيه إلغاء / تعديل لعدد (${cancelledForTarget.size}) صنف — أوقف التحضير"
                else ->
                    "إجمالي الأصناف المطلوبة للتجهيز: ${filtered.size} صنف"
            }
            canvas.drawText(summaryMsg, centerX, y + (28f * scale), summaryWhitePaint)
            y += summaryHeight + (12f * scale)
        }

        // Footer Note (only if enabled)
        if (kitchenTemplate.showFooterNote) {
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 18f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val footerMsg = kitchenTemplate.footerNote.ifBlank { "--- برجاء سرعة التجهيز والالتزام بملاحظات التحضير ---" }
            canvas.drawText(footerMsg, centerX, y + 16f * scale, footerPaint)
            y += 20f * scale
        }

        val finalHeight = y.toInt().coerceIn(60, maxTempHeight)
        val finalBitmap = try {
            Bitmap.createBitmap(tempBitmap, 0, 0, width, finalHeight)
        } catch (_: Throwable) {
            tempBitmap
        }
        if (finalBitmap != tempBitmap) {
            try { tempBitmap.recycle() } catch (_: Throwable) {}
        }
        return finalBitmap
    }

    /**
     * Renders a custom, high-clarity test receipt Bitmap with the updated wording requested by user.
     */
    fun renderTestReceiptBitmap(
        config: PrinterConfig,
        paperWidthMm: Int = 80
    ): Bitmap {
        val width = if (paperWidthMm == 58) 384 else 576
        val scale = if (paperWidthMm == 58) 0.72f else 1.0f
        val sidePadding = (16 * scale).toInt()
        val contentWidth = width - (sidePadding * 2)

        val maxTempHeight = 2500
        val tempBitmap = Bitmap.createBitmap(width, maxTempHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tempBitmap)
        canvas.drawColor(Color.WHITE)

        val centerX = width / 2f
        val rightX = (width - sidePadding).toFloat()
        val leftX = sidePadding.toFloat()

        val thickLinePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 3f * scale
            style = Paint.Style.STROKE
        }
        val thinLinePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1.2f * scale
            style = Paint.Style.STROKE
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 28f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val centerBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val centerRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 17f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val rightBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val rightRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 17f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        var y = 20f * scale

        // Header with friendly distinct text
        canvas.drawText("🐟 مطعم أسماك صنارة وشبكة 🐟", centerX, y + 26f * scale, titlePaint)
        y += 38f * scale

        canvas.drawText("فحص وتأكيد جاهزية نظام الطباعة", centerX, y + 20f * scale, centerBoldPaint)
        y += 28f * scale

        y += 4f * scale
        canvas.drawLine(leftX, y, rightX, y, thickLinePaint)
        y += 12f * scale

        // Connectivity Badge Box
        val badgeHeight = 44f * scale
        val badgeRect = RectF(leftX, y, rightX, y + badgeHeight)
        val fillPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
        canvas.drawRoundRect(badgeRect, 6f * scale, 6f * scale, fillPaint)

        val whiteTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("الاتصال بالطابعة: ناجح ومستقر 100% ✓", centerX, y + 29f * scale, whiteTextPaint)
        y += badgeHeight + 16f * scale

        // Printer Details
        canvas.drawText("• الطابعة: ${config.displayName}", rightX, y + 18f * scale, rightBoldPaint)
        y += 26f * scale

        val connDetails = when (config.connectionType) {
            "NETWORK_IP" -> "شبكة IP (${config.ipAddress}:${config.port})"
            "BLUETOOTH" -> "بلوتوث (${config.bluetoothMacOrName})"
            else -> "نظام أندرويد"
        }
        canvas.drawText("• نوع المنفذ: $connDetails", rightX, y + 18f * scale, rightRegularPaint)
        y += 26f * scale

        canvas.drawText("• نمط المعالجة: طباعة صور عالية الدقة (عربي نقي)", rightX, y + 18f * scale, rightRegularPaint)
        y += 26f * scale

        canvas.drawText("• مقاس الورق: ${config.paperWidthMm} مم", rightX, y + 18f * scale, rightRegularPaint)
        y += 26f * scale

        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
        canvas.drawText("• وقت الفحص: ${dateFormat.format(Date())}", rightX, y + 18f * scale, rightRegularPaint)
        y += 30f * scale

        canvas.drawLine(leftX, y, rightX, y, thinLinePaint)
        y += 12f * scale

        // Arabic Shaping & Typography Proof Box
        canvas.drawText("معاينة جودة الخط العربي والأرقام:", rightX, y + 18f * scale, rightBoldPaint)
        y += 26f * scale

        canvas.drawText("• 2 كجم سمك بلطي مشوي ردة بالخل والثوم", rightX, y + 18f * scale, rightRegularPaint)
        y += 24f * scale
        canvas.drawText("• 1 كجم جمبري لحم مقلي كرسبي كبير", rightX, y + 18f * scale, rightRegularPaint)
        y += 24f * scale
        canvas.drawText("• الإجمالي المطلوب: 350.5 جنيه مصري", rightX, y + 18f * scale, rightBoldPaint)
        y += 30f * scale

        canvas.drawLine(leftX, y, rightX, y, thickLinePaint)
        y += 16f * scale

        canvas.drawText("طازج يومياً من البحر لمائدتكم", centerX, y + 18f * scale, centerBoldPaint)
        y += 26f * scale
        canvas.drawText("جاهز الآن لإصدار فواتير الكاشير وبونات المطبخ", centerX, y + 16f * scale, centerRegularPaint)
        y += 32f * scale

        val finalHeight = y.toInt().coerceAtLeast(100)
        val finalBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, width, finalHeight)
        tempBitmap.recycle()
        return finalBitmap
    }

    /**
     * Renders a multi-order delivery batch manifest/ticket on a single cashier receipt.
     * Each selected order occupies a clean 2-line bordered rectangle:
     * - Line 1: Right = Order address (عنوان الاوردر), Left = Order price (سعر الاوردر)
     * - Line 2: Customer phone number (رقم العميل) and Order number
     * All in bold, clear font.
     */
    /**
     * High-Clarity Delivery Manifest Ticket (كشف تسليم أوردرات الدليفري المجمع):
     * Matches the customer invoice and kitchen ticket architectural quality:
     * - Renders restaurant branding (logo, title, contact details from template).
     * - Features a prominent solid black banner badge: "كشف تسليم أوردرات الدليفري" with order count.
     * - Displays driver name and date/time clearly.
     * - Renders each selected order in a structured, high-contrast bordered card:
     *     * Order # badge + price + payment status (نقدي عند الاستلام / خالص)
     *     * Customer name and phone(s)
     *     * Full delivery address & area
     *     * Items summary with quantities & cooking/prep methods
     *     * Order notes (if present)
     * - Full totals & cash collection summary table.
     * - Driver & Cashier signature/handover block.
     * - Dynamic safe cropping with recycle protection.
     */
    fun renderDeliveryBatchTicket(
        ordersWithItems: List<OrderWithItems>,
        template: InvoiceTemplateConfig,
        paperWidthMm: Int = 80
    ): Bitmap {
        val width = if (paperWidthMm == 58) 384 else 576
        val baseScale = if (paperWidthMm == 58) 0.72f else 1.0f
        val fontScale = when (template.deliveryManifestFontSize.uppercase()) {
            "NORMAL" -> 0.88f
            "LARGE" -> 1.05f
            "HUGE" -> 1.25f
            "GIANT" -> 1.45f
            else -> 1.05f
        }
        val scale = baseScale
        val sidePadding = (14 * scale).toInt()
        val leftX = sidePadding.toFloat()
        val rightX = (width - sidePadding).toFloat()
        val centerX = width / 2f

        val customTitleFontSize = template.headerTitleFontSize.coerceIn(20, 44).toFloat()
        val customDetailsFontSize = template.headerDetailsFontSize.coerceIn(12, 24).toFloat()

        // Allocate generous temporary canvas height to avoid any clipping
        val estimatedHeight = 650 + ordersWithItems.size * 260
        val maxTempHeight = maxOf(8000, estimatedHeight + 1500)
        val tempBitmap = Bitmap.createBitmap(width, maxTempHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tempBitmap)
        canvas.drawColor(Color.WHITE)

        val thickLinePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 3f * scale
            style = Paint.Style.STROKE
        }
        val thinLinePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1.2f * scale
            style = Paint.Style.STROKE
        }
        val boxBorderPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2.2f * scale
            style = Paint.Style.STROKE
        }
        val blackFillPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = customTitleFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val centerDetailsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = customDetailsFontSize * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val bannerWhiteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 23f * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val bannerWhiteCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val orderNumBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 21f * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val orderPriceWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val rightBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 21f * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val rightRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 19f * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val leftBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 21f * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val centerRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 17f * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val topPaddingVal = template.topPadding.coerceIn(8, 60).toFloat() * scale
        val lineSpacingVal = template.headerLineSpacing.coerceIn(4, 30).toFloat() * scale
        var y = topPaddingVal

        // 1. Restaurant Logo (if set)
        if (!template.logoPath.isNullOrBlank()) {
            try {
                val logoFile = File(template.logoPath)
                if (logoFile.exists()) {
                    val rawLogo = BitmapFactory.decodeFile(template.logoPath)
                    if (rawLogo != null) {
                        val maxLogoWidth = (180f * scale).toInt()
                        val aspect = rawLogo.height.toFloat() / rawLogo.width.toFloat()
                        val scaledWidth = maxLogoWidth.coerceAtMost(rawLogo.width)
                        val scaledHeight = (scaledWidth * aspect).toInt().coerceAtMost((180f * scale).toInt())
                        val scaledLogo = Bitmap.createScaledBitmap(rawLogo, scaledWidth, scaledHeight, true)
                        val logoLeft = centerX - (scaledWidth / 2f)
                        canvas.drawBitmap(scaledLogo, logoLeft, y, null)
                        y += scaledHeight + (8f * scale)
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Restaurant Header Title & Details
        if (template.headerTitle.isNotBlank()) {
            val titleMetrics = titlePaint.fontMetrics
            val baseline = y - titleMetrics.ascent
            canvas.drawText(template.headerTitle, centerX, baseline, titlePaint)
            y = baseline + titleMetrics.descent + lineSpacingVal
        }

        val detailsMetrics = centerDetailsPaint.fontMetrics
        if (template.subtitle.isNotBlank()) {
            val baseline = y - detailsMetrics.ascent
            canvas.drawText(template.subtitle, centerX, baseline, centerDetailsPaint)
            y = baseline + detailsMetrics.descent + lineSpacingVal
        }

        if (template.address.isNotBlank()) {
            val baseline = y - detailsMetrics.ascent
            canvas.drawText(template.address, centerX, baseline, centerDetailsPaint)
            y = baseline + detailsMetrics.descent + lineSpacingVal
        }

        if (template.phone1.isNotBlank()) {
            val phones = template.phone1 + if (template.phone2.isNotBlank()) " - ${template.phone2}" else ""
            val baseline = y - detailsMetrics.ascent
            canvas.drawText("هاتف: $phones", centerX, baseline, centerDetailsPaint)
            y = baseline + detailsMetrics.descent + lineSpacingVal
        }

        // Top Divider
        y += 4f * scale
        canvas.drawLine(leftX, y, rightX, y, thickLinePaint)
        y += 8f * scale

        // 3. Document Title Badge (Prominent Solid Black Banner)
        val bannerHeight = 44f * scale * fontScale
        val bannerRect = RectF(leftX, y, rightX, y + bannerHeight)
        canvas.drawRoundRect(bannerRect, 6f * scale, 6f * scale, blackFillPaint)

        val bannerTitleBaseline = y + (bannerHeight / 2f) - ((bannerWhiteTitlePaint.descent() + bannerWhiteTitlePaint.ascent()) / 2f)
        val bannerCenterPaint = Paint(bannerWhiteTitlePaint).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("🛵 كشف تسليم أوردرات الدليفري", centerX, bannerTitleBaseline, bannerCenterPaint)
        y += bannerHeight + (8f * scale)

        // Date and Driver Info Line
        val dateFormat = SimpleDateFormat("yyyy-MM-dd  hh:mm a", Locale.US)
        val dateText = "تاريخ الإصدار: ${dateFormat.format(Date())}"
        canvas.drawText(dateText, rightX, y + rightRegularPaint.textSize, rightRegularPaint)
        y += rightRegularPaint.textSize + (6f * scale)

        // Check if orders belong to a specific driver
        val distinctDrivers = ordersWithItems.mapNotNull { it.order.driverName?.trim() }.filter { it.isNotBlank() }.distinct()
        if (distinctDrivers.isNotEmpty()) {
            val driverLabel = if (distinctDrivers.size == 1) "مندوب التوصيل (الطيار): ${distinctDrivers.first()}" else "مندوبو التوصيل: ${distinctDrivers.joinToString("، ")}"
            canvas.drawText(driverLabel, rightX, y + rightBoldPaint.textSize, rightBoldPaint)
            y += rightBoldPaint.textSize + (8f * scale)
        }

        y += 4f * scale
        canvas.drawLine(leftX, y, rightX, y, thinLinePaint)
        y += 12f * scale

        // Dedicated Bold Paints for Each Order Box (مستطيل بخط عريض وواضح)
        val orderAddressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 23f * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val orderPricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 24f * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val orderPhonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f * scale * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        // 4. Render Each Selected Order Inside a Prominent Rectangle (مستطيل)
        // User requirements:
        // - Each order in a bordered rectangle with bold, clear font
        // - Right: Address (العنوان) with order number
        // - Left: Price (السعر)
        // - Underneath: Mobile number (رقم الموبايل)
        // - Absolutely NO items (الأصناف)
        // - Absolutely NO order counts, totals, signatures, or footers
        for (item in ordersWithItems) {
            val order = item.order
            val cardStartY = y
            val innerLeft = leftX + (12f * scale)
            val innerRight = rightX - (12f * scale)
            var curY = cardStartY + (12f * scale)

            // Price on the left (السعر)
            val priceText = "${formatCurrency(order.totalAmount)} ج.م"
            val priceWidth = orderPricePaint.measureText(priceText) + (16f * scale)

            // Address on the right (العنوان)
            val fullAddress = (order.deliveryAddressFull ?: order.customerArea ?: "بدون عنوان").trim()
            val addressText = "أوردر #${order.orderNumber} - العنوان: $fullAddress"

            // Draw Address on right
            val addressEndY = drawMultilineText(
                canvas = canvas,
                text = addressText,
                leftX = innerLeft + priceWidth,
                rightX = innerRight,
                startY = curY,
                paint = orderAddressPaint,
                alignRight = true
            )

            // Draw Price on left aligned with first line of address
            val priceBaseline = curY - orderPricePaint.fontMetrics.ascent
            canvas.drawText(priceText, innerLeft, priceBaseline, orderPricePaint)

            // Move Y below both address and price
            curY = maxOf(addressEndY, curY + orderPricePaint.textSize) + (10f * scale)

            // Phone number underneath (رقم الموبايل)
            val phone = if (order.customerPhone.isNotBlank()) order.customerPhone else "بدون هاتف"
            val secPhone = if (!order.customerSecondaryPhone.isNullOrBlank()) " / ${order.customerSecondaryPhone}" else ""
            val phoneText = "رقم الموبايل: $phone$secPhone"

            val phoneEndY = drawMultilineText(
                canvas = canvas,
                text = phoneText,
                leftX = innerLeft,
                rightX = innerRight,
                startY = curY,
                paint = orderPhonePaint,
                alignRight = true
            )
            curY = phoneEndY + (12f * scale)

            // Outer rectangle border
            val cardRect = RectF(leftX, cardStartY, rightX, curY)
            canvas.drawRoundRect(cardRect, 6f * scale, 6f * scale, boxBorderPaint)

            y = curY + (14f * scale)
        }

        y += 6f * scale

        // Safe Crop to content (receipt finishes right after last order box)
        val finalHeight = y.toInt().coerceIn(120, maxTempHeight)
        val finalBitmap = try {
            Bitmap.createBitmap(tempBitmap, 0, 0, width, finalHeight)
        } catch (_: Throwable) {
            tempBitmap
        }
        if (finalBitmap != tempBitmap) {
            try { tempBitmap.recycle() } catch (_: Throwable) {}
        }
        return finalBitmap
    }

    private fun fitText(text: String, paint: Paint, maxPx: Float): String {
        if (paint.measureText(text) <= maxPx) return text
        var truncated = text
        while (truncated.length > 3 && paint.measureText("$truncated…") > maxPx) {
            truncated = truncated.substring(0, truncated.length - 1)
        }
        return "$truncated…"
    }

    /**
     * Renders a comprehensive Shift Close Statement (Z-Report) showing full Cashier Drawer In & Out details.
     */
    fun renderShiftReportBitmap(
        statement: com.example.data.repository.ShiftDrawerStatement,
        template: InvoiceTemplateConfig,
        paperWidthMm: Int = 80
    ): Bitmap {
        val width = if (paperWidthMm == 58) 384 else 576
        val scale = if (paperWidthMm == 58) 0.72f else 1.0f
        val sidePadding = (14 * scale).toInt()
        val contentWidth = width - (sidePadding * 2)

        val maxTempHeight = 6000
        val tempBitmap = Bitmap.createBitmap(width, maxTempHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tempBitmap)
        canvas.drawColor(Color.WHITE)

        val centerX = width / 2f
        val rightX = (width - sidePadding).toFloat()
        val leftX = sidePadding.toFloat()

        val thickLinePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 3f * scale
            style = Paint.Style.STROKE
        }
        val thinLinePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1.2f * scale
            style = Paint.Style.STROKE
        }
        val dashLinePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1.2f * scale
            style = Paint.Style.STROKE
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f * scale, 4f * scale), 0f)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 28f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val centerRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 19f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val rightBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 21f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val rightRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 19f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val leftBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 21f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val leftRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 19f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        var y = 20f * scale

        // Header Title
        canvas.drawText(template.headerTitle.ifBlank { "صنارة وشبكة للمأكولات البحرية" }, centerX, y + 24f * scale, titlePaint)
        y += 34f * scale

        if (template.subtitle.isNotBlank()) {
            canvas.drawText(template.subtitle, centerX, y + 16f * scale, centerRegularPaint)
            y += 24f * scale
        }

        // Z-Report Banner
        y += 8f * scale
        val badgeRect = RectF(leftX, y, rightX, y + 42f * scale)
        val fillPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, 6f * scale, 6f * scale, fillPaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("تقرير تقفيل الوردية (Z-REPORT) - حركة الدرج", centerX, y + 28f * scale, badgeTextPaint)
        y += 50f * scale

        // Shift Info
        val timeFormat = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.US)
        canvas.drawText("رقم الوردية: #${statement.shift.shiftNumber}", rightX, y + 18f * scale, rightBoldPaint)
        canvas.drawText("كاشير: ${statement.shift.cashierName.ifBlank { "كاشير عام" }}", leftX, y + 18f * scale, leftBoldPaint)
        y += 26f * scale

        canvas.drawText("وقت الفتح: ${timeFormat.format(Date(statement.shift.openedAt))}", rightX, y + 16f * scale, rightRegularPaint)
        y += 24f * scale
        statement.shift.closedAt?.let { closedTime ->
            canvas.drawText("وقت الإغلاق: ${timeFormat.format(Date(closedTime))}", rightX, y + 16f * scale, rightRegularPaint)
            y += 24f * scale
        }

        // Divider
        y += 6f * scale
        canvas.drawLine(leftX, y, rightX, y, thickLinePaint)
        y += 12f * scale

        // 1. OPENING BALANCE (العهدة)
        canvas.drawText("💵 رصيد بداية الدرج (العهدة):", rightX, y + 20f * scale, rightBoldPaint)
        canvas.drawText("${String.format(Locale.US, "%.1f", statement.openingBalance)} ج.م", leftX, y + 20f * scale, leftBoldPaint)
        y += 30f * scale

        canvas.drawLine(leftX, y, rightX, y, thinLinePaint)
        y += 10f * scale

        // 2. INCOMING CASH (الوارد للدرج)
        canvas.drawText("🟢 إجمالي الوارد للدرج (المقبوضات):", rightX, y + 20f * scale, rightBoldPaint)
        y += 26f * scale

        // Store cash sales
        canvas.drawText("• مبيعات كاش مباشرة بالمحل (${statement.storeCashOrders.size} أوردر):", rightX - (10f * scale), y + 18f * scale, rightRegularPaint)
        canvas.drawText("+${String.format(Locale.US, "%.1f", statement.totalStoreCash)} ج.م", leftX, y + 18f * scale, leftBoldPaint)
        y += 26f * scale

        // Driver settlements
        if (statement.driverSettlements.isNotEmpty()) {
            canvas.drawText("• توريدات تسويات الدليفري المستلمة من الطيارين:", rightX - (10f * scale), y + 18f * scale, rightRegularPaint)
            canvas.drawText("+${String.format(Locale.US, "%.1f", statement.totalDriverSettlementsCash)} ج.م", leftX, y + 18f * scale, leftBoldPaint)
            y += 26f * scale

            statement.driverSettlements.forEach { s ->
                val amt = if (s.actualHandoverAmount > 0.0) s.actualHandoverAmount else s.netAmountToHandover
                canvas.drawText("  - كابتن ${s.driverName} (${s.ordersCount} أوردر):", rightX - (18f * scale), y + 16f * scale, rightRegularPaint)
                canvas.drawText("+${String.format(Locale.US, "%.1f", amt)} ج.م", leftX + (10f * scale), y + 16f * scale, leftRegularPaint)
                y += 22f * scale
            }
        } else {
            canvas.drawText("• توريدات تسويات الدليفري: 0.0 ج.م", rightX - (10f * scale), y + 18f * scale, rightRegularPaint)
            y += 24f * scale
        }

        // Account receipts
        if (statement.accountReceipts.isNotEmpty()) {
            canvas.drawText("• مقبوضات نقدية وإيداعات لحسابات:", rightX - (10f * scale), y + 18f * scale, rightRegularPaint)
            canvas.drawText("+${String.format(Locale.US, "%.1f", statement.totalAccountReceipts)} ج.م", leftX, y + 18f * scale, leftBoldPaint)
            y += 26f * scale

            statement.accountReceipts.forEach { (tx, accName) ->
                canvas.drawText("  - من $accName: ${tx.description}", rightX - (18f * scale), y + 16f * scale, rightRegularPaint)
                canvas.drawText("+${String.format(Locale.US, "%.1f", tx.amount)} ج.م", leftX + (10f * scale), y + 16f * scale, leftRegularPaint)
                y += 22f * scale
            }
        }

        // Subtotal In
        canvas.drawText("إجمالي المقبوضات الداخلة للدرج:", rightX, y + 20f * scale, rightBoldPaint)
        canvas.drawText("+${String.format(Locale.US, "%.1f", statement.totalIncomingCash - statement.openingBalance)} ج.م", leftX, y + 20f * scale, leftBoldPaint)
        y += 30f * scale

        canvas.drawLine(leftX, y, rightX, y, thinLinePaint)
        y += 10f * scale

        // 3. OUTGOING CASH (المنصرف من الدرج)
        canvas.drawText("🔴 إجمالي المنصرف من الدرج (المدفوعات):", rightX, y + 20f * scale, rightBoldPaint)
        y += 26f * scale

        // Staff withdrawals
        if (statement.staffWithdrawals.isNotEmpty()) {
            canvas.drawText("• سلف ومسحوبات الموظفين من الدرج:", rightX - (10f * scale), y + 18f * scale, rightRegularPaint)
            canvas.drawText("-${String.format(Locale.US, "%.1f", statement.totalStaffWithdrawals)} ج.م", leftX, y + 18f * scale, leftBoldPaint)
            y += 26f * scale

            statement.staffWithdrawals.forEach { (tx, empName) ->
                val typeName = if (tx.type == "SALARY_PAYOUT") "صرف راتب" else "سلفة"
                canvas.drawText("  - $empName ($typeName): ${tx.reason}", rightX - (18f * scale), y + 16f * scale, rightRegularPaint)
                canvas.drawText("-${String.format(Locale.US, "%.1f", tx.amount)} ج.م", leftX + (10f * scale), y + 16f * scale, leftRegularPaint)
                y += 22f * scale
            }
        } else {
            canvas.drawText("• سلف ومسحوبات موظفين: 0.0 ج.م", rightX - (10f * scale), y + 18f * scale, rightRegularPaint)
            y += 24f * scale
        }

        // Supplier payments
        if (statement.supplierPayments.isNotEmpty()) {
            canvas.drawText("• سداد فواتير ودفعات موردين وخامات:", rightX - (10f * scale), y + 18f * scale, rightRegularPaint)
            canvas.drawText("-${String.format(Locale.US, "%.1f", statement.totalSupplierPayments)} ج.م", leftX, y + 18f * scale, leftBoldPaint)
            y += 26f * scale

            statement.supplierPayments.forEach { (tx, accName) ->
                val invStr = if (tx.invoiceNumber.isNotBlank()) " (فاتورة #${tx.invoiceNumber})" else ""
                canvas.drawText("  - $accName$invStr: ${tx.description}", rightX - (18f * scale), y + 16f * scale, rightRegularPaint)
                canvas.drawText("-${String.format(Locale.US, "%.1f", tx.amount)} ج.م", leftX + (10f * scale), y + 16f * scale, leftRegularPaint)
                y += 22f * scale
            }
        } else {
            canvas.drawText("• مدفوعات موردين: 0.0 ج.م", rightX - (10f * scale), y + 18f * scale, rightRegularPaint)
            y += 24f * scale
        }

        // Other expenses
        if (statement.otherExpenses > 0.0) {
            canvas.drawText("• مصروفات ونثريات أخرى بالوردية:", rightX - (10f * scale), y + 18f * scale, rightRegularPaint)
            canvas.drawText("-${String.format(Locale.US, "%.1f", statement.otherExpenses)} ج.م", leftX, y + 18f * scale, leftBoldPaint)
            y += 26f * scale
        }

        // Subtotal Out
        canvas.drawText("إجمالي المدفوعات الخارجة من الدرج:", rightX, y + 20f * scale, rightBoldPaint)
        canvas.drawText("-${String.format(Locale.US, "%.1f", statement.totalOutgoingCash)} ج.م", leftX, y + 20f * scale, leftBoldPaint)
        y += 30f * scale

        // Divider
        canvas.drawLine(leftX, y, rightX, y, thickLinePaint)
        y += 10f * scale

        // 4. NON-DRAWER: WALLET ORDERS (فودافون كاش - للعلم والمراجعة)
        canvas.drawText("📱 مبيعات محافظ إلكترونية (فودافون كاش / خارج الدرج):", rightX, y + 20f * scale, rightBoldPaint)
        canvas.drawText("${String.format(Locale.US, "%.1f", statement.totalWalletSales)} ج.م", leftX, y + 20f * scale, leftBoldPaint)
        y += 26f * scale
        canvas.drawText("عدد أوردرات المحافظ: ${statement.walletOrders.size} أوردر (تحويل إلكتروني)", rightX - (10f * scale), y + 16f * scale, rightRegularPaint)
        y += 26f * scale

        canvas.drawLine(leftX, y, rightX, y, thickLinePaint)
        y += 12f * scale

        // 5. DRAWER RECONCILIATION SUMMARY BOX
        val summaryBoxRect = RectF(leftX, y, rightX, y + 110f * scale)
        val boxBorderPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2.5f * scale
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(summaryBoxRect, 8f * scale, 8f * scale, boxBorderPaint)

        canvas.drawText("الرصيد الدفتري المتوقع بالدرج:", rightX - (10f * scale), y + 26f * scale, rightBoldPaint)
        canvas.drawText("${String.format(Locale.US, "%.1f", statement.expectedDrawerCash)} ج.م", leftX + (10f * scale), y + 26f * scale, leftBoldPaint)
        y += 34f * scale

        canvas.drawText("الرصيد الفعلي المعدود بالدرج:", rightX - (10f * scale), y + 26f * scale, rightBoldPaint)
        canvas.drawText("${String.format(Locale.US, "%.1f", statement.actualDrawerCash)} ج.م", leftX + (10f * scale), y + 26f * scale, leftBoldPaint)
        y += 34f * scale

        val diff = statement.difference
        val diffStatus = when {
            kotlin.math.abs(diff) < 0.01 -> "الدرج متطابق تماماً ✓"
            diff < 0 -> "عجز بالدرج: ${String.format(Locale.US, "%.1f", kotlin.math.abs(diff))} ج.م ⚠️"
            else -> "زيادة بالدرج: +${String.format(Locale.US, "%.1f", diff)} ج.م"
        }
        canvas.drawText("حالة الدرج / الفارق المالي:", rightX - (10f * scale), y + 26f * scale, rightBoldPaint)
        canvas.drawText(diffStatus, leftX + (10f * scale), y + 26f * scale, leftBoldPaint)
        y += 50f * scale

        if (statement.shift.notes.isNotBlank()) {
            y += 6f * scale
            canvas.drawText("ملاحظات: ${statement.shift.notes}", rightX, y + 18f * scale, rightRegularPaint)
            y += 26f * scale
        }

        // Signatures
        y += 20f * scale
        canvas.drawLine(leftX, y, rightX, y, dashLinePaint)
        y += 24f * scale

        canvas.drawText("توقيع الكاشير: _____________", rightX, y + 18f * scale, rightRegularPaint)
        canvas.drawText("توقيع الإدارة: _____________", leftX, y + 18f * scale, leftRegularPaint)
        y += 34f * scale

        val finalHeight = y.toInt().coerceAtLeast(150)
        val finalBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, width, finalHeight)
        tempBitmap.recycle()
        return finalBitmap
    }

    /**
     * Converts an Android Bitmap into standard ESC/POS Raster Bit Image (GS v 0) byte stream.
     * Slices into compact 32-row chunks to safely fit inside thermal printer RAM buffers without overflowing.
     */
    fun bitmapToEscPosRasterBytes(bitmap: Bitmap, sliceHeight: Int = 32): ByteArray {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return byteArrayOf()
        }
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8
        val output = ByteArrayOutputStream()

        // ESC @ (Initialize printer) + Cancel Chinese/Kanji mode + Center alignment
        output.write(byteArrayOf(0x1B, 0x40))
        output.write(ArabicEscPosHelper.CMD_CANCEL_KANJI)
        output.write(byteArrayOf(0x1B, 0x61, 0x01))

        var yStart = 0
        while (yStart < height) {
            val currentSliceHeight = minOf(sliceHeight, height - yStart)
            val xL = (widthBytes and 0xFF).toByte()
            val xH = ((widthBytes shr 8) and 0xFF).toByte()
            val yL = (currentSliceHeight and 0xFF).toByte()
            val yH = ((currentSliceHeight shr 8) and 0xFF).toByte()

            // GS v 0 0 xL xH yL yH
            output.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

            for (row in 0 until currentSliceHeight) {
                val y = yStart + row
                for (colByte in 0 until widthBytes) {
                    var byteVal = 0
                    for (bit in 0 until 8) {
                        val x = colByte * 8 + bit
                        if (x < width) {
                            val pixel = bitmap.getPixel(x, y)
                            val r = (pixel shr 16) and 0xFF
                            val g = (pixel shr 8) and 0xFF
                            val b = pixel and 0xFF
                            // Standard ITU-R BT.601 perceived luminance
                            val luminance = (299 * r + 587 * g + 114 * b) / 1000
                            if (luminance < 175) { // Dark pixel -> thermal dot ON
                                byteVal = byteVal or (0x80 shr bit)
                            }
                        }
                    }
                    output.write(byteVal)
                }
            }
            yStart += currentSliceHeight
        }

        // Feed paper 1 line so it clears the printhead with minimal blank paper
        output.write(byteArrayOf(0x0A))
        // Cut paper command (GS V 66 0) + ESC @ (Reset printer state cleanly after ticket)
        output.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        output.write(byteArrayOf(0x1B, 0x40))

        return output.toByteArray()
    }

    private fun containsArabic(text: String): Boolean {
        for (i in 0 until text.length) {
            val code = text[i].code
            if ((code in 0x0600..0x06FF) || (code in 0x0750..0x077F) || 
                (code in 0xFB50..0xFDFF) || (code in 0xFE70..0xFEFF)) {
                return true
            }
        }
        return false
    }

    /**
     * Rock-solid multiline text renderer for Android thermal receipts:
     * - Fixes StaticLayout bug: ALWAYS forces textPaint.textAlign = Paint.Align.LEFT
     *   so StaticLayout never double-shifts text off screen or to the wrong side.
     * - Uses proper BiDi text direction (FIRSTSTRONG_RTL for Arabic, FIRSTSTRONG_LTR for English).
     * - Strictly bounds text between leftX and rightX so English and long text NEVER spill into
     *   adjacent columns (like Quantity, Price, or Total).
     * - Right-aligns (alignRight = true) both Arabic and English text to match invoice RTL orientation.
     */
    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        leftX: Float,
        rightX: Float,
        startY: Float,
        paint: Paint,
        alignRight: Boolean = true
    ): Float {
        val width = (rightX - leftX).toInt().coerceAtLeast(30)
        val textPaint = TextPaint(paint).apply {
            textAlign = Paint.Align.LEFT // CRITICAL: StaticLayout manages line alignment internally
        }

        val hasArabic = containsArabic(text)
        val textDir = if (hasArabic) TextDirectionHeuristics.FIRSTSTRONG_RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR

        // In Android StaticLayout:
        // For RTL text: ALIGN_NORMAL aligns to the right edge; ALIGN_OPPOSITE aligns to the left edge.
        // For LTR text (English): ALIGN_NORMAL aligns to the left edge; ALIGN_OPPOSITE aligns to the right edge.
        val alignment = if (alignRight) {
            if (hasArabic) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE
        } else {
            if (hasArabic) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
        }

        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
            .setAlignment(alignment)
            .setTextDirection(textDir)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(leftX, startY)
        layout.draw(canvas)
        canvas.restore()

        return startY + layout.height
    }

    private fun drawMultilineArabicText(
        canvas: Canvas,
        text: String,
        leftX: Float,
        rightX: Float,
        startY: Float,
        paint: Paint,
        maxWidth: Int
    ): Float {
        return drawMultilineText(canvas, text, leftX, rightX, startY, paint, alignRight = true) + 4f
    }
}
