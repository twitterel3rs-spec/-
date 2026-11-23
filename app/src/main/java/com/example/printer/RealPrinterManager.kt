package com.example.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.app.PendingIntent
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import com.example.data.model.CancelledKitchenItem
import com.example.data.model.OrderWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

sealed class PrinterResult {
    data class Success(val message: String) : PrinterResult()
    data class Error(val errorMessage: String) : PrinterResult()
    data class PermissionRequired(val permissionName: String, val message: String) : PrinterResult()
}

data class UsbPrinterInfo(
    val deviceName: String,
    val displayName: String,
    val vendorId: Int,
    val productId: Int,
    val hasPermission: Boolean,
    val usbDevice: UsbDevice
)

data class PrinterConfig(
    val id: String, // "MAIN", "KITCHEN_1", "KITCHEN_2", or custom "PRINTER_3", etc.
    val displayName: String,
    val connectionType: String = "NETWORK_IP", // "NETWORK_IP", "BLUETOOTH", "SYSTEM_SPOOLER", "USB"
    val ipAddress: String = "192.168.1.50",
    val port: Int = 9100,
    val bluetoothMacOrName: String = "",
    val usbDeviceName: String = "", // Device name or VendorId:ProductId
    val paperWidthMm: Int = 80, // 80 or 58
    val codepageMode: String = "CP864_PAGE_22",
    val printMode: String = "IMAGE", // "IMAGE" (High-clarity Bitmap for 100% Arabic quality) or "ESC_POS_TEXT"
    val role: String = "CUSTOMER_INVOICE", // "CUSTOMER_INVOICE", "KITCHEN_ORDER", "BOTH"
    val isEnabled: Boolean = true,
    val isCashierDefault: Boolean = false,
    val customHeader: String = "",
    val formatTemplate: String = "STANDARD" // "STANDARD", "DETAILED", "COMPACT"
)

data class InvoiceTemplateConfig(
    val headerTitle: String = "صنارة وشبكة للمأكولات البحرية",
    val subtitle: String = "طازج من البحر لبيتك - أسماك وسواحل",
    val phone1: String = "01001234567",
    val phone2: String = "01123456789",
    val address: String = "كورنيش السويس - شارع الميناء القديم",
    val footerNote: String = "شكراً لزيارتكم ونتمنى لكم وجبة شهية!",
    val showPreparations: Boolean = true,
    val showOrderNotes: Boolean = true,
    val showDeliveryDetails: Boolean = true,
    val fontSize: String = "NORMAL", // "NORMAL", "LARGE", "HUGE"
    val logoPath: String? = null,
    val topPadding: Int = 20, // هوامش أعلى الفاتورة (بكسل: 10، 20، 30، 40)
    val headerLineSpacing: Int = 12, // تباعد سطور الترويسة والعنوان والوصف والهواتف (بكسل: 8، 12، 16، 20)
    val headerTitleFontSize: Int = 30, // حجم خط اسم المطعم الرئيسي (24، 28، 30، 34، 38)
    val headerDetailsFontSize: Int = 18, // حجم خط الوصف والعنوان والهواتف (14، 16، 18، 20)
    val deliveryManifestFontSize: String = "LARGE", // "NORMAL", "LARGE", "HUGE", "GIANT"
    // Paper-saving & Granular Toggles for Customer Invoice
    val compactHeaderMode: Boolean = true, // وضع الترويسة المضغوطة لتوفير الورق (condensed customer header)
    val showLogo: Boolean = true, // إظهار شعار/لوجو المطعم
    val showHeaderTitle: Boolean = true, // إظهار اسم المطعم الرئيسي
    val showSubtitle: Boolean = true, // إظهار الشعار والوصف الفرعي
    val showAddress: Boolean = true, // إظهار عنوان المحل
    val showPhones: Boolean = true, // إظهار هواتف المحل
    val showOrderBadge: Boolean = true, // إظهار شريط رقم الأوردر ونوعه
    val showDateTime: Boolean = true, // إظهار تاريخ ووقت الطلب
    val showSellerName: Boolean = false, // اسم البائع (افتراضياً معطل لتوفير الورق والمساحة)
    val showCustomerName: Boolean = true, // إظهار اسم العميل
    val showCustomerPhone: Boolean = true, // إظهار هاتف العميل
    val showDeliveryAddress: Boolean = true, // إظهار عنوان توصيل العميل
    val showDriverName: Boolean = true, // إظهار كابتن التوصيل
    val showSubtotal: Boolean = true, // إظهار المجموع الفرعي
    val showDeliveryFee: Boolean = true, // إظهار رسوم التوصيل والخصم
    val showPaymentStatusBox: Boolean = true, // إظهار صندوق الإجمالي وحالة الدفع البارز
    val showPaymentBreakdown: Boolean = true, // إظهار تفاصيل الدفع والمسدد والباقي
    val showFooterNote: Boolean = true, // إظهار الملاحظة الختامية بأسفل الفاتورة
    // Granular Font Sizes (in px)
    val metadataFontSizePx: Int = 16, // حجم خط بيانات العميل والتاريخ (13، 16، 19، 22)
    val tableHeaderFontSizePx: Int = 18, // حجم خط رأس جدول الأصناف (15، 18، 21)
    val itemFontSizePx: Int = 22, // حجم خط اسم الصنف وسعره (18، 22، 26، 30)
    val itemPrepFontSizePx: Int = 18, // حجم خط طريقة الطهي والتجهيز (14، 18، 22)
    val itemNotesFontSizePx: Int = 15, // حجم خط ملاحظات الأصناف (13، 15، 18)
    val totalBoxFontSizePx: Int = 24 // حجم خط صندوق الإجمالي النهائي (20، 24، 28، 32)
)

data class KitchenOrderTemplateConfig(
    val headerTitle: String = "أمر تشغيل المطبخ والاستاف",
    val kitchenDepartment: String = "",
    val showHeaderTitle: Boolean = true,
    val showDepartment: Boolean = true,
    val showCustomerName: Boolean = true,
    val showCustomerPhone: Boolean = true,
    val showDeliveryAddress: Boolean = true,
    val showSellerName: Boolean = false, // البائع (افتراضياً معطل لتوفير الورق والمساحة)
    val showOrderTime: Boolean = true, // وقت وتاريخ الطلب
    val showOrderTypeAndTable: Boolean = true,
    val compactHeaderMode: Boolean = true, // وضع الترويسة المضغوطة لتوفير الورق (condensed header mode)
    val showTableSectionTitle: Boolean = false, // جملة "جدول تجهيز وطهي الأصناف المطلوبة" (إخفاؤها يوفر مساحة)
    val showPreparations: Boolean = true,
    val showOrderNotes: Boolean = true,
    val showItemPrices: Boolean = false,
    val showCashierAndTime: Boolean = true, // للتوافق
    val showItemCountSummary: Boolean = true,
    val highlightItemNames: Boolean = true,
    val showFooterNote: Boolean = false, // الملاحظة الختامية بأسفل البون (إخفاؤها يوفر الورق)
    val fontSize: String = "LARGE", // "NORMAL", "LARGE", "HUGE"
    val footerNote: String = "برجاء سرعة التجهيز والالتزام بملاحظات التحضير",
    val topPadding: Int = 10, // هوامش أعلى أمر التشغيل (بكسل: 4، 10، 18، 26)
    val headerLineSpacing: Int = 6, // تباعد أسطر الترويسة (بكسل: 4، 6، 10، 14)
    val tableHeaderItemColumnTitle: String = "الصنف والتجهيز",
    val qtyColumnWidthPercent: Int = 28,
    // Granular font sizes
    val headerFontSizePx: Int = 24, // خط عنوان الترويسة (20، 24، 28، 32)
    val metadataFontSizePx: Int = 16, // خط بيانات وتفاصيل العميل والبائع والوقت (13، 16، 19، 22)
    val tableHeaderFontSizePx: Int = 18, // خط رأس جدول الأصناف (15، 18، 22)
    val itemFontSizePx: Int = 26, // خط اسم الصنف بالجدول (22، 26، 30، 34)
    val itemPrepFontSizePx: Int = 20, // خط طريقة الطهي والتجهيز (16، 20، 24)
    val itemNotesFontSizePx: Int = 17, // خط ملاحظات الأصناف (13، 17، 21)
    val qtyFontSizePx: Int = 28 // خط كمية الصنف في الجدول (22، 28، 32، 36)
)

class RealPrinterManager(private val context: Context) {

    companion object {
        // Standard SPP (Serial Port Profile) UUID for Bluetooth thermal printers
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // ESC/POS Commands
        private val ESC_INIT = byteArrayOf(0x1B, 0x40) // Initialize printer
        private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        private val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
        private val ESC_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x10)
        private val ESC_DOUBLE_WIDTH_HEIGHT = byteArrayOf(0x1B, 0x21, 0x30)
        private val ESC_NORMAL = byteArrayOf(0x1B, 0x21, 0x00)
        private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
        private val GS_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // Cut paper
        private val LF = byteArrayOf(0x0A) // Line Feed
        const val ACTION_USB_PERMISSION = "com.example.sennara.USB_PERMISSION"
        val ESC_DRAWER_KICK = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())

        // Process-wide serial print queue per physical printer endpoint so concurrent orders/edits never interleave bytes
        private val printerEndpointMutexes = ConcurrentHashMap<String, Mutex>()

        internal fun transportMutexKey(config: PrinterConfig): String {
            return when (config.connectionType) {
                "NETWORK_IP" -> "NET:${config.ipAddress.trim()}:${config.port}"
                "BLUETOOTH" -> "BT:${config.bluetoothMacOrName.trim().uppercase(Locale.US)}"
                "USB" -> "USB:${config.usbDeviceName.trim().ifBlank { "DEFAULT" }}"
                "SYSTEM_SPOOLER" -> "SPOOLER:${config.id}"
                else -> "OTHER:${config.id}"
            }
        }

        internal fun getEndpointMutex(config: PrinterConfig): Mutex {
            val key = transportMutexKey(config)
            return printerEndpointMutexes.getOrPut(key) { Mutex() }
        }

        fun parseCodepageMode(modeStr: String): ArabicEscPosHelper.CodepageMode {
            return when (modeStr) {
                "CP864_PAGE_37" -> ArabicEscPosHelper.CodepageMode.CP864_PAGE_37
                "CP1256_PAGE_50" -> ArabicEscPosHelper.CodepageMode.CP1256_PAGE_50
                "CP1256_PAGE_32" -> ArabicEscPosHelper.CodepageMode.CP1256_PAGE_32
                else -> ArabicEscPosHelper.CodepageMode.CP864_PAGE_22
            }
        }
    }

    // Get connected USB Thermal Printers list
    fun getConnectedUsbPrinters(): List<UsbPrinterInfo> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return emptyList()
        return try {
            usbManager.deviceList.values.mapNotNull { device ->
                if (isUsbPrinter(device)) {
                    val prodName = device.productName?.trim().takeUnless { it.isNullOrBlank() }
                    val mfgName = device.manufacturerName?.trim().takeUnless { it.isNullOrBlank() }
                    val label = when {
                        prodName != null && mfgName != null -> "$mfgName $prodName"
                        prodName != null -> prodName
                        mfgName != null -> mfgName
                        else -> "طابعة كاشير USB (VID:${Integer.toHexString(device.vendorId)}:PID:${Integer.toHexString(device.productId)})"
                    }
                    val hasPerm = usbManager.hasPermission(device)
                    UsbPrinterInfo(
                        deviceName = device.deviceName,
                        displayName = label,
                        vendorId = device.vendorId,
                        productId = device.productId,
                        hasPermission = hasPerm,
                        usbDevice = device
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e("RealPrinterManager", "Error querying USB devices", e)
            emptyList()
        }
    }

    fun isUsbPrinter(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) return true
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_PRINTER) return true
            // Class 0 (composite/per-interface) or 255 (vendor-specific ESC/POS chipsets like CH340, Prolific, PL2303, Silicon Labs)
            if (intf.interfaceClass == 0 || intf.interfaceClass == 255) {
                for (j in 0 until intf.endpointCount) {
                    val ep = intf.getEndpoint(j)
                    if (ep.direction == UsbConstants.USB_DIR_OUT && ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun requestUsbPermission(device: UsbDevice) {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
            usbManager.requestPermission(device, pendingIntent)
        } catch (e: Exception) {
            Log.e("RealPrinterManager", "Error requesting USB permission", e)
        }
    }

    suspend fun openCashDrawer(config: PrinterConfig): PrinterResult {
        return sendBytesToPrinter(config, ESC_DRAWER_KICK)
    }

    // Get paired Bluetooth devices list
    fun getPairedBluetoothDevices(): List<Pair<String, String>> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() ?: return emptyList()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return emptyList()
            }
        }

        return try {
            adapter.bondedDevices.map { Pair(it.name ?: "جهاز بدون اسم", it.address) }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    // Real Test Print function
    suspend fun testPrinterConnection(config: PrinterConfig): PrinterResult = withContext(Dispatchers.IO) {
        if (config.connectionType == "SYSTEM_SPOOLER") {
            val bmp = ReceiptBitmapRenderer.renderTestReceiptBitmap(config, config.paperWidthMm)
            withContext(Dispatchers.Main) {
                ReceiptPrintDocumentAdapter.printBitmapViaSystemSpooler(context, bmp, "اختبار طباعة أندرويد")
            }
            return@withContext PrinterResult.Success("تم فتح نافذة الطباعة عبر نظام أندرويد بنجاح ✓")
        }

        val testBytes = if (config.printMode == "IMAGE") {
            val bmp = ReceiptBitmapRenderer.renderTestReceiptBitmap(config, config.paperWidthMm)
            ReceiptBitmapRenderer.bitmapToEscPosRasterBytes(bmp)
        } else {
            buildTestReceiptBytes(config)
        }
        sendBytesToPrinter(config, testBytes)
    }

    // Real Order Print function supporting dynamic roles (Customer Invoice, Kitchen Order, or Both)
    suspend fun printOrder(
        config: PrinterConfig,
        orderWithItems: OrderWithItems,
        templateConfig: InvoiceTemplateConfig,
        isReprint: Boolean = false,
        reprintReason: String? = null,
        targetDocumentType: String? = null, // "CUSTOMER_INVOICE", "KITCHEN_ORDER", or null (use config.role)
        kitchenTemplate: KitchenOrderTemplateConfig = KitchenOrderTemplateConfig(),
        cancelledKitchenItems: List<CancelledKitchenItem> = emptyList()
    ): PrinterResult = withContext(Dispatchers.IO) {
        try {
            val docType = targetDocumentType ?: config.role

            if (docType == "BOTH") {
                // Print both Kitchen Order and Customer Invoice on the same printer
                val kitchenRes = printSingleDocument(config, orderWithItems, templateConfig, isReprint, reprintReason, "KITCHEN_ORDER", kitchenTemplate, cancelledKitchenItems)
                val invoiceRes = printSingleDocument(config, orderWithItems, templateConfig, isReprint, reprintReason, "CUSTOMER_INVOICE", kitchenTemplate, cancelledKitchenItems)
                return@withContext if (kitchenRes is PrinterResult.Success || invoiceRes is PrinterResult.Success) {
                    PrinterResult.Success("تمت طباعة أمر التشغيل وفاتورة العميل بنجاح على طابعة ${config.displayName} ✓")
                } else {
                    kitchenRes
                }
            }

            printSingleDocument(config, orderWithItems, templateConfig, isReprint, reprintReason, docType, kitchenTemplate, cancelledKitchenItems)
        } catch (e: Exception) {
            PrinterResult.Error("حدث خطأ أثناء تجهيز بيانات الأوردر للطباعة: ${e.localizedMessage ?: e.message}")
        }
    }

    private suspend fun printSingleDocument(
        config: PrinterConfig,
        orderWithItems: OrderWithItems,
        templateConfig: InvoiceTemplateConfig,
        isReprint: Boolean,
        reprintReason: String?,
        documentType: String, // "CUSTOMER_INVOICE" or "KITCHEN_ORDER"
        kitchenTemplate: KitchenOrderTemplateConfig = KitchenOrderTemplateConfig(),
        cancelledKitchenItems: List<CancelledKitchenItem> = emptyList()
    ): PrinterResult = withContext(Dispatchers.IO) {
        val isKitchen = documentType == "KITCHEN_ORDER"

        if (config.connectionType == "SYSTEM_SPOOLER") {
            val bitmap = if (isKitchen) {
                ReceiptBitmapRenderer.renderKitchenTicketBitmap(orderWithItems, config.displayName, config.paperWidthMm, isReprint, reprintReason, config.id, kitchenTemplate, cancelledKitchenItems)
            } else {
                ReceiptBitmapRenderer.renderCustomerInvoiceBitmap(orderWithItems, templateConfig, config.paperWidthMm, isReprint, reprintReason)
            }
            if (bitmap == null) {
                if (isKitchen) {
                    return@withContext PrinterResult.Success("جميع أصناف هذا الأوردر تم طباعتها للمطبخ مسبقاً (لا توجد كميات مضافة جديدة للتشغيل) ✓")
                }
                return@withContext PrinterResult.Error("لا توجد أصناف في هذا الأوردر موجهة لطابعة ${config.displayName}.")
            }
            withContext(Dispatchers.Main) {
                val printTitle = if (isKitchen) "أمر تشغيل #${orderWithItems.order.orderNumber}" else "فاتورة #${orderWithItems.order.orderNumber}"
                ReceiptPrintDocumentAdapter.printBitmapViaSystemSpooler(context, bitmap, printTitle)
            }
            return@withContext PrinterResult.Success("تم فتح نافذة الطباعة بنجاح عبر نظام أندرويد ✓")
        }

        val orderBytes = if (config.printMode == "IMAGE") {
            val bitmap = if (isKitchen) {
                ReceiptBitmapRenderer.renderKitchenTicketBitmap(orderWithItems, config.displayName, config.paperWidthMm, isReprint, reprintReason, config.id, kitchenTemplate, cancelledKitchenItems)
            } else {
                ReceiptBitmapRenderer.renderCustomerInvoiceBitmap(orderWithItems, templateConfig, config.paperWidthMm, isReprint, reprintReason)
            }
            if (bitmap == null) {
                if (isKitchen) {
                    return@withContext PrinterResult.Success("جميع أصناف هذا الأوردر تم طباعتها للمطبخ مسبقاً (لا توجد كميات مضافة جديدة للتشغيل) ✓")
                }
                return@withContext PrinterResult.Error("لا توجد أصناف في هذا الأوردر موجهة لطابعة ${config.displayName} (تم منع طباعة تذكرة فارغة).")
            }
            ReceiptBitmapRenderer.bitmapToEscPosRasterBytes(bitmap)
        } else {
            val bytes = if (isKitchen) {
                buildKitchenTicketBytes(config, orderWithItems, config.displayName, isReprint, reprintReason, kitchenTemplate, cancelledKitchenItems)
            } else {
                buildCustomerInvoiceBytes(config, orderWithItems, templateConfig, isReprint, reprintReason)
            }
            if (bytes.isEmpty()) {
                if (isKitchen) {
                    return@withContext PrinterResult.Success("جميع أصناف هذا الأوردر تم طباعتها للمطبخ مسبقاً (لا توجد كميات مضافة جديدة للتشغيل) ✓")
                }
                return@withContext PrinterResult.Error("لا توجد أصناف في هذا الأوردر موجهة لطابعة ${config.displayName} (تم منع طباعة تذكرة فارغة).")
            }
            bytes
        }
        sendBytesToPrinter(config, orderBytes)
    }

    // Real Delivery Manifest Print function (كشف تسليم أوردرات الدليفري)
    suspend fun printDeliveryManifest(
        config: PrinterConfig,
        ordersWithItems: List<OrderWithItems>,
        templateConfig: InvoiceTemplateConfig
    ): PrinterResult = withContext(Dispatchers.IO) {
        try {
            val bitmap = ReceiptBitmapRenderer.renderDeliveryBatchTicket(ordersWithItems, templateConfig, config.paperWidthMm)
            if (config.connectionType == "SYSTEM_SPOOLER") {
                withContext(Dispatchers.Main) {
                    ReceiptPrintDocumentAdapter.printBitmapViaSystemSpooler(
                        context,
                        bitmap,
                        "كشف تسليم دليفري (${ordersWithItems.size} أوردر)"
                    )
                }
                return@withContext PrinterResult.Success("تم فتح نافذة الطباعة بنجاح عبر نظام أندرويد ✓")
            }

            val manifestBytes = if (config.printMode == "IMAGE") {
                ReceiptBitmapRenderer.bitmapToEscPosRasterBytes(bitmap)
            } else {
                buildDeliveryManifestReceiptBytes(config, ordersWithItems, templateConfig)
            }
            sendBytesToPrinter(config, manifestBytes)
        } catch (e: Exception) {
            PrinterResult.Error("حدث خطأ أثناء طباعة كشف التسليم: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Direct print via Android System Spooler / Easy Thermal Print service.
     */
    fun printDirectViaSystemSpooler(
        orderWithItems: OrderWithItems,
        templateConfig: InvoiceTemplateConfig,
        targetId: String = "MAIN"
    ): Boolean {
        val bitmap = when (targetId) {
            "KITCHEN_1" -> ReceiptBitmapRenderer.renderKitchenTicketBitmap(orderWithItems, "مطبخ 1 (شواية وفرن وفريش)", 80, false, null, "KITCHEN_1")
            "KITCHEN_2" -> ReceiptBitmapRenderer.renderKitchenTicketBitmap(orderWithItems, "مطبخ 2 (قلي وساندوتشات ووجبات)", 80, false, null, "KITCHEN_2")
            else -> ReceiptBitmapRenderer.renderCustomerInvoiceBitmap(orderWithItems, templateConfig, 80, false, null)
        } ?: return false

        return ReceiptPrintDocumentAdapter.printBitmapViaSystemSpooler(
            context,
            bitmap,
            "فاتورة أوردر ${orderWithItems.order.orderNumber}"
        )
    }

    // Core Connection Dispatcher (Serialized per physical printer endpoint)
    suspend fun sendBytesToPrinterDirect(config: PrinterConfig, bytes: ByteArray): PrinterResult =
        sendBytesToPrinter(config, bytes)

    private suspend fun sendBytesToPrinter(config: PrinterConfig, bytes: ByteArray): PrinterResult {
        if (config.connectionType == "SYSTEM_SPOOLER") {
            return PrinterResult.Success("جاهز للطباعة عبر نظام أندرويد")
        }
        val mutex = getEndpointMutex(config)
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val result = when (config.connectionType) {
                    "NETWORK_IP" -> sendViaNetworkSocket(config.ipAddress, config.port, bytes)
                    "BLUETOOTH" -> sendViaBluetooth(config.bluetoothMacOrName, bytes)
                    "USB" -> sendViaUsb(config.usbDeviceName, bytes)
                    else -> PrinterResult.Error("نوع الاتصال المحدد غير مدعوم: ${config.connectionType}")
                }
                // Let the physical thermal printer finish cutting & draining before releasing mutex to the next queued job
                if (config.connectionType == "NETWORK_IP" || config.connectionType == "BLUETOOTH" || config.connectionType == "USB") {
                    delay(350)
                }
                result
            }
        }
    }

    // Real Network Socket Print with Chunked Flow Control
    private fun sendViaNetworkSocket(ip: String, port: Int, data: ByteArray): PrinterResult {
        if (ip.isBlank()) {
            return PrinterResult.Error("عنوان IP للطابعة فارغ. يرجى إدخال عنوان IP صالح (مثال: 192.168.1.50) أو البحث التلقائي.")
        }

        var socket: Socket? = null
        var outputStream: OutputStream? = null

        return try {
            socket = Socket()
            socket.tcpNoDelay = true
            socket.keepAlive = true
            // Connect with 4000ms timeout
            socket.connect(InetSocketAddress(ip.trim(), port), 4000)
            socket.soTimeout = 6000

            outputStream = socket.getOutputStream()

            // Stream in 512-byte chunks with 16ms pauses so thermal printer receive buffer never overflows during raster images
            val chunkSize = 512
            var offset = 0
            while (offset < data.size) {
                val len = minOf(chunkSize, data.size - offset)
                outputStream.write(data, offset, len)
                outputStream.flush()
                offset += len
                if (offset < data.size) {
                    try { Thread.sleep(16) } catch (_: InterruptedException) {}
                }
            }

            // Buffer drain pause
            try {
                Thread.sleep(700)
            } catch (_: InterruptedException) {}

            PrinterResult.Success("تم الاتصال بنجاح بطابعة الشبكة ($ip:$port) وتم إرسال أمر الطباعة بنجاح ✓")
        } catch (e: SocketTimeoutException) {
            PrinterResult.Error("فشل الاتصال: انتهت مهلة الاتصال بالطابعة ($ip:$port). تأكد من تشغيل الطابعة واتصالها بنفس شبكة الراوتر.")
        } catch (e: IOException) {
            PrinterResult.Error("خطأ في الاتصال بالشبكة ($ip:$port): ${e.localizedMessage ?: "تعذر الوصول للطابعة"}")
        } catch (e: Exception) {
            PrinterResult.Error("حدث خطأ غير متوقع أثناء الاتصال بالطابعة ($ip): ${e.localizedMessage}")
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    // Real Bluetooth RFCOMM Socket Print with Chunked Flow Control
    private fun sendViaBluetooth(macOrName: String, data: ByteArray): PrinterResult {
        if (macOrName.isBlank()) {
            return PrinterResult.Error("لم يتم تحديد طابعة بلوتوث. يرجى اختيار طابعة مقترنة من القائمة.")
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            ?: return PrinterResult.Error("البلوتوث غير مدعوم على هذا الجهاز.")

        if (!adapter.isEnabled) {
            return PrinterResult.Error("البلوتوث مغلق على هاتفك. يرجى تشغيل البلوتوث والاقتران بالطابعة.")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return PrinterResult.PermissionRequired(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    "صلاحية الاتصال بالبلوتوث غير ممنوحة. يرجى الموافقة على الصلاحية للاتصال بالطابعة."
                )
            }
        }

        return try {
            val pairedDevices = adapter.bondedDevices
            val targetDevice: BluetoothDevice? = pairedDevices.firstOrNull {
                it.address.equals(macOrName, ignoreCase = true) || it.name.equals(macOrName, ignoreCase = true)
            }

            if (targetDevice == null) {
                return PrinterResult.Error("تعذر العثور على الطابعة المقترنة '$macOrName'. تأكد من عمل اقتران مع الطابعة في إعدادات بلوتوث الهاتف.")
            }

            var btSocket: BluetoothSocket? = null
            var outStream: OutputStream? = null

            try {
                btSocket = targetDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                btSocket.connect() // blocking connect

                outStream = btSocket.outputStream

                // Stream in 512-byte chunks with 25ms pauses for Bluetooth RFCOMM buffer stability
                val chunkSize = 512
                var offset = 0
                while (offset < data.size) {
                    val len = minOf(chunkSize, data.size - offset)
                    outStream.write(data, offset, len)
                    outStream.flush()
                    offset += len
                    if (offset < data.size) {
                        try { Thread.sleep(25) } catch (_: InterruptedException) {}
                    }
                }

                // Buffer drain pause
                try {
                    Thread.sleep(800)
                } catch (_: InterruptedException) {}

                PrinterResult.Success("تم الاتصال بنجاح بطابعة البلوتوث '${targetDevice.name}' وتمت الطباعة.")
            } finally {
                try {
                    outStream?.close()
                    btSocket?.close()
                } catch (_: Exception) {}
            }
        } catch (e: SecurityException) {
            PrinterResult.Error("تم رفض إذن البلوتوث من قبل النظام: ${e.localizedMessage}")
        } catch (e: IOException) {
            PrinterResult.Error("فشل الاتصال بطابعة البلوتوث '$macOrName': ${e.localizedMessage ?: "تأكد من تشغيل الطابعة وقربها من الهاتف"}")
        } catch (e: Exception) {
            PrinterResult.Error("خطأ أثناء الاتصال بالبلوتوث: ${e.localizedMessage}")
        }
    }

    // Direct USB Printing with Bulk OUT Endpoint Flow
    private fun sendViaUsb(usbIdentifier: String, data: ByteArray): PrinterResult {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: return PrinterResult.Error("خدمة USB غير متوفرة على هذا الجهاز أو نظام التشغيل")

        val deviceList = try {
            usbManager.deviceList
        } catch (e: Exception) {
            return PrinterResult.Error("تعذر قراءة قائمة منافذ USB: ${e.localizedMessage}")
        }

        if (deviceList.isEmpty()) {
            return PrinterResult.Error("لم يتم العثور على أي طابعة USB متصلة بالجهاز. يرجى التأكد من توصيل كابل الطابعة وتشغيل زر الباور.")
        }

        // Find target printer matching identifier or first valid thermal printer
        val targetDevice = if (usbIdentifier.isNotBlank()) {
            deviceList.values.find {
                it.deviceName == usbIdentifier ||
                "${it.vendorId}:${it.productId}" == usbIdentifier ||
                (it.productName?.contains(usbIdentifier, ignoreCase = true) == true)
            } ?: deviceList.values.firstOrNull { isUsbPrinter(it) } ?: deviceList.values.first()
        } else {
            deviceList.values.firstOrNull { isUsbPrinter(it) } ?: deviceList.values.first()
        }

        if (!usbManager.hasPermission(targetDevice)) {
            requestUsbPermission(targetDevice)
            val devTitle = targetDevice.productName?.takeUnless { it.isBlank() } ?: "طابعة USB"
            return PrinterResult.PermissionRequired(
                "USB_PERMISSION",
                "طابعة الـ USB ($devTitle) تحتاج إذن وصول من نظام أندرويد. يرجى الضغط على موافق (Allow) في النافذة المنبثقة ثم إعادة المحاولة."
            )
        }

        var targetInterface: UsbInterface? = null
        var targetEndpoint: UsbEndpoint? = null

        for (i in 0 until targetDevice.interfaceCount) {
            val intf = targetDevice.getInterface(i)
            for (j in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(j)
                if (ep.direction == UsbConstants.USB_DIR_OUT && ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    targetInterface = intf
                    targetEndpoint = ep
                    break
                }
            }
            if (targetEndpoint != null) break
        }

        if (targetInterface == null || targetEndpoint == null) {
            return PrinterResult.Error("تعذر العثور على قناة إرسال البيانات (Bulk OUT) في طابعة الـ USB (${targetDevice.productName ?: "طابعة غير معروفة"}).")
        }

        var connection: UsbDeviceConnection? = null
        return try {
            connection = usbManager.openDevice(targetDevice)
                ?: return PrinterResult.Error("تعذر فتح منفذ الـ USB لطابعة الفواتير. يرجى فصل الكابل وإعادة توصيله.")

            if (!connection.claimInterface(targetInterface, true)) {
                return PrinterResult.Error("فشل حجز واجهة الـ USB لطابعة الفواتير. تأكد من عدم استخدامها بواسطة تطبيق آخر.")
            }

            val maxPacket = maxOf(targetEndpoint.maxPacketSize, 1024)
            var offset = 0
            while (offset < data.size) {
                val len = minOf(maxPacket, data.size - offset)
                val chunk = data.copyOfRange(offset, offset + len)
                val sent = connection.bulkTransfer(targetEndpoint, chunk, len, 5000)
                if (sent < 0) {
                    return PrinterResult.Error("فشل نقل البيانات لطابعة الـ USB.")
                }
                offset += len
                if (offset < data.size) {
                    try { Thread.sleep(8) } catch (_: InterruptedException) {}
                }
            }

            try { Thread.sleep(250) } catch (_: InterruptedException) {}

            val devTitle = targetDevice.productName?.takeUnless { it.isBlank() } ?: "طابعة الكاشير USB"
            PrinterResult.Success("تمت الطباعة بنجاح على طابعة الـ USB ($devTitle) ✓")
        } catch (e: Exception) {
            PrinterResult.Error("خطأ أثناء الطباعة عبر منفذ USB: ${e.localizedMessage ?: "استثناء غير معروف"}")
        } finally {
            try {
                if (targetInterface != null) connection?.releaseInterface(targetInterface)
            } catch (_: Exception) {}
            try {
                connection?.close()
            } catch (_: Exception) {}
        }
    }

    // Build Test Receipt Byte Array (Updated wording requested by user)
    internal fun buildTestReceiptBytes(config: PrinterConfig): ByteArray {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
        val mode = parseCodepageMode(config.codepageMode)
        val builder = ByteArrayBuilder(mode)

        builder.append(ESC_INIT)
        builder.append(ArabicEscPosHelper.CMD_CANCEL_KANJI)
        builder.append(ArabicEscPosHelper.getSelectCodepageCommand(mode))
        builder.append(ESC_ALIGN_CENTER)
        builder.append(ESC_DOUBLE_WIDTH_HEIGHT)
        builder.appendArabicText("تجربة اختبار الطابعة بنجاح\n")
        builder.append(ESC_NORMAL)
        builder.append(ESC_BOLD_ON)
        builder.appendArabicText("نظام إدارة وفواتير المأكولات البحرية\n")
        builder.append(ESC_BOLD_OFF)
        builder.appendArabicText("================================\n")
        builder.append(ESC_ALIGN_RIGHT)
        builder.append(ESC_BOLD_ON)
        builder.appendArabicText("تفاصيل فحص كفاءة الطابعة:\n")
        builder.appendArabicText("• خطوط الفاتورة العربية واضحة ومنظمة\n")
        builder.appendArabicText("• التنسيق يعمل بكفاءة مع جدول الحروف\n")
        builder.appendArabicText("• جاهز لاستقبال وطباعة الأوردرات والتذاكر\n")
        builder.append(ESC_BOLD_OFF)
        builder.appendArabicText("--------------------------------\n")
        builder.append(ESC_ALIGN_RIGHT)
        builder.appendArabicText("اسم الطابعة: ${config.displayName}\n")
        builder.appendArabicText("طريقة الربط: ${config.connectionType}\n")
        if (config.connectionType == "NETWORK_IP") {
            builder.appendArabicText("عنوان الشبكة: ${config.ipAddress}:${config.port}\n")
        } else if (config.connectionType == "BLUETOOTH") {
            builder.appendArabicText("البلوتوث: ${config.bluetoothMacOrName}\n")
        } else if (config.connectionType == "USB") {
            builder.appendArabicText("منفذ USB: ${config.usbDeviceName.ifBlank { "طابعة افتراضية" }}\n")
        }
        builder.appendArabicText("ترميز الحروف: ${mode.label}\n")
        builder.appendArabicText("تاريخ الفحص: ${dateFormat.format(Date())}\n")
        builder.append(ESC_ALIGN_CENTER)
        builder.appendArabicText("--------------------------------\n")
        builder.appendArabicText("مع تحيات إدارة المطعم\n")
        builder.appendArabicText("✓ فحص الاختبار سليم 100%\n")
        builder.append(GS_CUT_PAPER)

        return builder.toByteArray()
    }

    // Build Delivery Manifest in ESC/POS Text Mode
    internal fun buildDeliveryManifestReceiptBytes(
        config: PrinterConfig,
        ordersWithItems: List<OrderWithItems>,
        template: InvoiceTemplateConfig
    ): ByteArray {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
        val mode = parseCodepageMode(config.codepageMode)
        val builder = ByteArrayBuilder(mode)

        builder.append(ESC_INIT)
        builder.append(ArabicEscPosHelper.CMD_CANCEL_KANJI)
        builder.append(ArabicEscPosHelper.getSelectCodepageCommand(mode))

        // Center Title
        builder.append(ESC_ALIGN_CENTER)
        builder.append(ESC_DOUBLE_WIDTH_HEIGHT)
        builder.appendArabicText("${template.headerTitle}\n")
        builder.append(ESC_NORMAL)
        builder.append(ESC_BOLD_ON)
        builder.appendArabicText("كشف تسليم أوردرات الدليفري\n")
        builder.append(ESC_BOLD_OFF)
        builder.appendArabicText("التاريخ: ${dateFormat.format(Date())}\n")
        builder.append(byteArrayOf(0x0A))

        // Orders: Each order in a rectangle with address on right, price on left, mobile phone underneath
        builder.append(ESC_ALIGN_RIGHT)
        for (item in ordersWithItems) {
            val o = item.order
            val addr = (o.deliveryAddressFull ?: o.customerArea ?: "بدون عنوان").trim()
            val phone = if (o.customerPhone.isNotBlank()) o.customerPhone else "بدون هاتف"
            val secPhone = if (!o.customerSecondaryPhone.isNullOrBlank()) " / ${o.customerSecondaryPhone}" else ""

            builder.appendArabicText("┌────────────────────────────────┐\n")
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("أوردر #${o.orderNumber} - العنوان: $addr\n")
            builder.appendArabicText("السعر: ${o.totalAmount} ج.م\n")
            builder.appendArabicText("رقم الموبايل: $phone$secPhone\n")
            builder.append(ESC_BOLD_OFF)
            builder.appendArabicText("└────────────────────────────────┘\n")
        }

        builder.append(byteArrayOf(0x0A, 0x0A))
        builder.append(GS_CUT_PAPER)

        return builder.toByteArray()
    }

    // Build Full Customer Invoice Receipt
    internal fun buildCustomerInvoiceBytes(
        config: PrinterConfig,
        orderWithItems: OrderWithItems,
        template: InvoiceTemplateConfig,
        isReprint: Boolean,
        reprintReason: String?
    ): ByteArray {
        val order = orderWithItems.order
        val items = orderWithItems.items
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
        val mode = parseCodepageMode(config.codepageMode)
        val builder = ByteArrayBuilder(mode)

        builder.append(ESC_INIT)
        builder.append(ArabicEscPosHelper.CMD_CANCEL_KANJI)
        builder.append(ArabicEscPosHelper.getSelectCodepageCommand(mode))
        builder.append(ESC_ALIGN_CENTER)

        // Header Title
        builder.append(ESC_DOUBLE_WIDTH_HEIGHT)
        builder.appendArabicText("${template.headerTitle}\n")
        builder.append(ESC_NORMAL)

        if (template.subtitle.isNotBlank()) {
            builder.appendArabicText("${template.subtitle}\n")
        }
        if (template.address.isNotBlank()) {
            builder.appendArabicText("${template.address}\n")
        }
        if (template.phone1.isNotBlank()) {
            val phones = template.phone1 + if (template.phone2.isNotBlank()) " - ${template.phone2}" else ""
            builder.appendArabicText("هاتف: $phones\n")
        }

        builder.appendArabicText("--------------------------------\n")

        if (isReprint) {
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("*** إعادة طباعة فاتورة ***\n")
            if (!reprintReason.isNullOrBlank()) {
                builder.appendArabicText("السبب: $reprintReason\n")
            }
            builder.append(ESC_BOLD_OFF)
            builder.appendArabicText("--------------------------------\n")
        }

        // Order Meta
        builder.append(ESC_ALIGN_RIGHT)
        if (template.fontSize.uppercase() in listOf("LARGE", "HUGE")) {
            builder.append(ESC_DOUBLE_WIDTH_HEIGHT)
            builder.appendArabicText("أوردر رقم: ${order.orderNumber}\n")
            builder.append(ESC_NORMAL)
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("نوع الطلب: ${if (order.orderType == "DELIVERY") "دليفري توصيل" else "استلام وحجز"}\n")
            builder.append(ESC_BOLD_OFF)
        } else {
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("رقم الأوردر: ${order.orderNumber}\n")
            builder.append(ESC_BOLD_OFF)
            builder.appendArabicText("نوع الطلب: ${if (order.orderType == "DELIVERY") "دليفري توصيل" else "استلام من المحل"}\n")
        }
        builder.appendArabicText("التاريخ: ${dateFormat.format(Date(order.createdAt))}\n")

        if (order.customerName.isNotBlank() && order.customerName != "زبون مباشر") {
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("العميل: ${order.customerName}\n")
            builder.append(ESC_BOLD_OFF)
        }
        if (order.customerPhone.isNotBlank()) {
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("هاتف العميل: ${order.customerPhone}\n")
            builder.append(ESC_BOLD_OFF)
        }
        if (!order.customerSecondaryPhone.isNullOrBlank()) {
            builder.appendArabicText("هاتف إضافي: ${order.customerSecondaryPhone}\n")
        }

        if (template.showDeliveryDetails && !order.deliveryAddressFull.isNullOrBlank()) {
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("العنوان: ${order.deliveryAddressFull}\n")
            builder.append(ESC_BOLD_OFF)
        }
        if (template.showDeliveryDetails && order.orderType == "DELIVERY" && !order.driverName.isNullOrBlank()) {
            builder.appendArabicText("كابتن التوصيل: ${order.driverName}\n")
        }

        builder.append(ESC_ALIGN_CENTER)
        builder.appendArabicText("--------------------------------\n")

        // Table Header
        builder.append(ESC_ALIGN_RIGHT)
        builder.append(ESC_BOLD_ON)
        builder.appendArabicText("البيان              الكمية     السعر\n")
        builder.append(ESC_BOLD_OFF)
        builder.appendArabicText("--------------------------------\n")

        // Items
        for (item in items) {
            val totalStr = String.format(Locale.US, "%.1f", item.totalPrice)
            val unitAndPrep = item.unitPrice + item.preparationPrice
            val priceStr = String.format(Locale.US, "%.1f", unitAndPrep)
            
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("${item.itemName}\n")
            builder.append(ESC_BOLD_OFF)
            builder.appendArabicText("  ${ReceiptBitmapRenderer.formatQuantity(item.quantity)} ${item.unit} * $priceStr = $totalStr جنيه\n")

            if (template.showPreparations && !item.preparationName.isNullOrBlank()) {
                val prepPriceStr = if (item.preparationPrice > 0) {
                    val pStr = if (item.preparationPrice % 1.0 == 0.0) item.preparationPrice.toInt().toString() else String.format(Locale.US, "%.1f", item.preparationPrice)
                    "(+$pStr)للكيلو"
                } else ""
                builder.appendArabicText("  - ${item.preparationName}$prepPriceStr\n")
            }
            if (template.showOrderNotes && item.itemNotes.isNotBlank()) {
                builder.appendArabicText("  - ملاحظة: ${item.itemNotes}\n")
            }
        }

        builder.append(ESC_ALIGN_CENTER)
        builder.appendArabicText("--------------------------------\n")

        // Totals
        builder.append(ESC_ALIGN_RIGHT)
        builder.appendArabicText("المجموع الفرعي:   ${String.format(Locale.US, "%.1f", order.subtotal)} جنيه\n")

        if (order.deliveryFee > 0) {
            builder.appendArabicText("رسوم التوصيل:    ${String.format(Locale.US, "%.0f", order.deliveryFee)} جنيه\n")
        }
        if (order.discount > 0) {
            builder.appendArabicText("الخصم:          -${String.format(Locale.US, "%.1f", order.discount)} جنيه\n")
        }

        builder.append(ESC_ALIGN_CENTER)
        builder.appendArabicText("================================\n")
        builder.append(ESC_DOUBLE_WIDTH_HEIGHT)
        builder.append(ESC_BOLD_ON)
        val isOrderFullyPaid = order.isPaid || (order.paidAmount >= order.totalAmount && order.totalAmount > 0.0)
        val hasRemaining = !isOrderFullyPaid && (order.paidAmount < order.totalAmount) && (order.paidAmount > 0.0)
        val remainingAmt = (order.totalAmount - order.paidAmount).coerceAtLeast(0.0)

        if (isOrderFullyPaid) {
            builder.appendArabicText("*** الأوردر خالص بالكامل ***\n")
            builder.appendArabicText("المدفوع (خالص ✓): ${String.format(Locale.US, "%.1f", order.totalAmount)} جنيه\n")
        } else if (hasRemaining) {
            builder.appendArabicText("⚠️ متبقي على العميل: ${String.format(Locale.US, "%.1f", remainingAmt)} جنيه\n")
            builder.appendArabicText("المطلوب كاش: ${String.format(Locale.US, "%.1f", remainingAmt)} جنيه\n")
        } else {
            builder.appendArabicText("🔴 غير مدفوع - يُحصل كاش\n")
            builder.appendArabicText("المطلوب: ${String.format(Locale.US, "%.1f", order.totalAmount)} جنيه\n")
        }
        builder.append(ESC_BOLD_OFF)
        builder.append(ESC_NORMAL)
        builder.appendArabicText("================================\n")

        builder.append(ESC_ALIGN_RIGHT)
        builder.appendArabicText("طريقة الدفع: ${if (order.paymentMethod == "CASH") "نقداً" else "محفظة إلكترونية"}\n")

        if (hasRemaining) {
            builder.appendArabicText("إجمالي الأوردر: ${String.format(Locale.US, "%.1f", order.totalAmount)} جنيه\n")
            builder.appendArabicText("المسدد حتى الآن: ${String.format(Locale.US, "%.1f", order.paidAmount)} جنيه\n")
            builder.appendArabicText("المتبقي للتحصيل: ${String.format(Locale.US, "%.1f", remainingAmt)} جنيه\n")
        } else if (order.paidAmount > order.totalAmount && order.changeAmount > 0.0) {
            builder.appendArabicText("المستلم نقداً: ${String.format(Locale.US, "%.1f", order.paidAmount)} جنيه\n")
            builder.appendArabicText("الباقي للعميل: ${String.format(Locale.US, "%.1f", order.changeAmount)} جنيه\n")
        }

        if (order.notes.isNotBlank()) {
            builder.appendArabicText("ملاحظات الأوردر: ${order.notes}\n")
        }

        builder.append(ESC_ALIGN_CENTER)
        builder.appendArabicText("--------------------------------\n")
        if (template.footerNote.isNotBlank()) {
            builder.appendArabicText("${template.footerNote}\n")
        }
        builder.appendArabicText("\n")
        builder.append(GS_CUT_PAPER)

        return builder.toByteArray()
    }

    // Build Kitchen Prep/Cooking Ticket
    internal fun buildKitchenTicketBytes(
        config: PrinterConfig,
        orderWithItems: OrderWithItems,
        kitchenTitle: String,
        isReprint: Boolean,
        reprintReason: String?,
        kitchenTemplate: KitchenOrderTemplateConfig = KitchenOrderTemplateConfig(),
        cancelledKitchenItems: List<CancelledKitchenItem> = emptyList()
    ): ByteArray {
        val order = orderWithItems.order
        val items = orderWithItems.items
        val mode = parseCodepageMode(config.codepageMode)
        val builder = ByteArrayBuilder(mode)

        fun matchesKitchenTarget(rawTarget: String): Boolean {
            val target = rawTarget.uppercase()
            return if (config.id == "KITCHEN_1") {
                target.contains("KITCHEN_1") || target.contains("ALL") || target == "MAIN" || target.isBlank()
            } else if (config.id == "KITCHEN_2") {
                target.contains("KITCHEN_2") || target.contains("ALL")
            } else {
                target.contains(config.id.uppercase()) || target.contains("ALL") || target == "MAIN" || target.isBlank()
            }
        }

        // Filter items & cancellations appropriate for this kitchen target
        val targetMatched = items.filter { matchesKitchenTarget(it.targetPrinter) }
        val cancelledForTarget = cancelledKitchenItems.filter { matchesKitchenTarget(it.targetPrinter) }

        // Strict Kitchen Differential Check:
        // Any item already printed in full (quantity <= printedKitchenQuantity) MUST NOT print again for any reason!
        val hasPriorKitchenActivity = targetMatched.any { it.printedKitchenQuantity > 0.0001 } || cancelledForTarget.isNotEmpty()
        val filtered = targetMatched.mapNotNull { item ->
            val delta = item.quantity - item.printedKitchenQuantity
            if (delta > 0.0001) {
                val wasPreviouslyPrinted = item.printedKitchenQuantity > 0.0001
                val isNewItem = item.printedKitchenQuantity <= 0.0001 && hasPriorKitchenActivity

                val kitchenNameExtra = if (wasPreviouslyPrinted) {
                    val prevStr = ReceiptBitmapRenderer.formatQuantity(item.printedKitchenQuantity)
                    val newTotalStr = ReceiptBitmapRenderer.formatQuantity(item.quantity)
                    " (زيادة +${ReceiptBitmapRenderer.formatQuantity(delta)} | كان $prevStr وأصبح $newTotalStr)"
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
            return ByteArray(0)
        }

        builder.append(ESC_INIT)
        builder.append(ArabicEscPosHelper.CMD_CANCEL_KANJI)
        builder.append(ArabicEscPosHelper.getSelectCodepageCommand(mode))
        if (kitchenTemplate.showHeaderTitle) {
            builder.append(ESC_ALIGN_CENTER)
            builder.append(ESC_DOUBLE_WIDTH_HEIGHT)
            val header = kitchenTemplate.headerTitle.ifBlank { "أمر تشغيل المطبخ والاستاف" }
            builder.appendArabicText("*** $header ***\n")
            builder.append(ESC_NORMAL)
        }

        if (kitchenTemplate.showDepartment && (kitchenTemplate.kitchenDepartment.isNotBlank() || kitchenTitle.isNotBlank())) {
            val subDept = if (kitchenTemplate.kitchenDepartment.isNotBlank()) kitchenTemplate.kitchenDepartment else kitchenTitle
            builder.appendArabicText("[$subDept]\n")
        }

        if (isAdditionTicket && cancelledForTarget.isNotEmpty()) {
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("*** تعديل تشغيل (إضافات وإلغاءات) ***\n")
            builder.append(ESC_BOLD_OFF)
        } else if (cancelledForTarget.isNotEmpty()) {
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("!! تنبيه هام للمطبخ: إلغاء / تعديل أصناف !!\n")
            builder.append(ESC_BOLD_OFF)
        } else if (isAdditionTicket) {
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("*** تعديل / إضافة تشغيل (الزيادة فقط) ***\n")
            builder.append(ESC_BOLD_OFF)
        } else if (isReprint) {
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("!! إعادة تشغيل (بإذن الإدارة) !!\n")
            if (!reprintReason.isNullOrBlank()) {
                builder.appendArabicText("السبب: $reprintReason\n")
            }
            builder.append(ESC_BOLD_OFF)
        }

        if (kitchenTemplate.compactHeaderMode) {
            // Compact Header Mode: Dense layout saving paper
            builder.appendArabicText("--------------------------------\n")
            builder.append(ESC_BOLD_ON)
            val typeStr = when (order.orderType) {
                "DELIVERY" -> "دليفري"
                "DINE_IN" -> "صالة"
                else -> "تيك أواي"
            }
            val numAndType = if (kitchenTemplate.showOrderTypeAndTable) "أوردر #${order.orderNumber} ($typeStr)" else "أوردر #${order.orderNumber}"
            val timePart = if (kitchenTemplate.showOrderTime && kitchenTemplate.showCashierAndTime) {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = order.createdAt }
                val rawH = cal.get(java.util.Calendar.HOUR)
                val h12 = if (rawH == 0) 12 else rawH
                val m = cal.get(java.util.Calendar.MINUTE)
                val isAm = cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM
                String.format(Locale.US, " | %02d:%02d%s", h12, m, if (isAm) "ص" else "م")
            } else ""
            builder.appendArabicText("$numAndType$timePart\n")
            builder.append(ESC_BOLD_OFF)

            val metaParts = mutableListOf<String>()
            if (kitchenTemplate.showCustomerName && order.customerName.isNotBlank() && order.customerName != "زبون مباشر") {
                metaParts.add("عميل: ${order.customerName}")
            }
            if (kitchenTemplate.showCustomerPhone && order.customerPhone.isNotBlank()) {
                metaParts.add("هاتف: ${order.customerPhone}")
            }
            if (metaParts.isNotEmpty()) {
                builder.appendArabicText(metaParts.joinToString(" - ") + "\n")
            }
            if (kitchenTemplate.showDeliveryAddress && !order.deliveryAddressFull.isNullOrBlank()) {
                builder.appendArabicText("عنوان: ${order.deliveryAddressFull}\n")
            }
            if (order.orderType == "DELIVERY" && !order.driverName.isNullOrBlank()) {
                builder.appendArabicText("طيار: ${order.driverName}\n")
            }
        } else {
            // Standard spacious layout
            builder.appendArabicText("================================\n")
            if (kitchenTemplate.showOrderTypeAndTable) {
                builder.append(ESC_BOLD_ON)
                val typeStr = when (order.orderType) {
                    "DELIVERY" -> "دليفري توصيل"
                    "DINE_IN" -> "صالة داخلي"
                    else -> "تيك أواي استلام"
                }
                builder.appendArabicText("أوردر رقم: #${order.orderNumber} - $typeStr\n")
                builder.append(ESC_BOLD_OFF)
            } else {
                builder.append(ESC_BOLD_ON)
                builder.appendArabicText("أوردر رقم: #${order.orderNumber}\n")
                builder.append(ESC_BOLD_OFF)
            }

            if (kitchenTemplate.showOrderTime && kitchenTemplate.showCashierAndTime) {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = order.createdAt }
                val rawH = cal.get(java.util.Calendar.HOUR)
                val h12 = if (rawH == 0) 12 else rawH
                val m = cal.get(java.util.Calendar.MINUTE)
                val isAm = cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM
                val time12hStr = String.format(Locale.US, "%02d:%02d %s", h12, m, if (isAm) "ص" else "م")
                val dateDayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(order.createdAt))
                builder.appendArabicText("الوقت: $time12hStr | التاريخ: $dateDayStr\n")
            }

            // Customer & Delivery Information
            if (kitchenTemplate.showCustomerName && order.customerName.isNotBlank() && order.customerName != "زبون مباشر") {
                builder.append(ESC_BOLD_ON)
                builder.appendArabicText("العميل: ${order.customerName}\n")
                builder.append(ESC_BOLD_OFF)
            }
            if (kitchenTemplate.showCustomerPhone && order.customerPhone.isNotBlank()) {
                builder.append(ESC_BOLD_ON)
                builder.appendArabicText("الهاتف: ${order.customerPhone}\n")
                builder.append(ESC_BOLD_OFF)
            }
            if (!order.customerSecondaryPhone.isNullOrBlank()) {
                builder.appendArabicText("هاتف إضافي: ${order.customerSecondaryPhone}\n")
            }

            if (kitchenTemplate.showDeliveryAddress && !order.deliveryAddressFull.isNullOrBlank()) {
                builder.appendArabicText("العنوان: ${order.deliveryAddressFull}\n")
            }
            if (order.orderType == "DELIVERY" && !order.driverName.isNullOrBlank()) {
                builder.appendArabicText("كابتن التوصيل: ${order.driverName}\n")
            }
        }

        builder.append(ESC_ALIGN_CENTER)
        builder.appendArabicText("--------------------------------\n")

        if (cancelledForTarget.isNotEmpty()) {
            builder.append(ESC_ALIGN_CENTER)
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("== أصناف ملغاة / مقللة (أوقف التجهيز) ==\n")
            builder.append(ESC_BOLD_OFF)
            builder.append(ESC_ALIGN_RIGHT)
            for (cItem in cancelledForTarget) {
                val baseName = cItem.kitchenName.ifBlank { cItem.itemName }
                val prepSuffix = if (!cItem.preparationName.isNullOrBlank()) " (${cItem.preparationName})" else ""
                builder.append(ESC_DOUBLE_HEIGHT)
                if (cItem.newQty <= 0.0001) {
                    builder.appendArabicText("× ملغي: ${ReceiptBitmapRenderer.formatQuantity(cItem.cancelledQty)} ${cItem.unit} * $baseName$prepSuffix\n")
                    builder.append(ESC_NORMAL)
                } else {
                    builder.appendArabicText("↓ تقليل: $baseName$prepSuffix (-${ReceiptBitmapRenderer.formatQuantity(cItem.cancelledQty)})\n")
                    builder.append(ESC_NORMAL)
                    builder.appendArabicText("   كان ${ReceiptBitmapRenderer.formatQuantity(cItem.prevQty)} وأصبح ${ReceiptBitmapRenderer.formatQuantity(cItem.newQty)} ${cItem.unit}\n")
                }
                builder.appendArabicText("--------------------------------\n")
            }
        }

        if (filtered.isNotEmpty()) {
            if (kitchenTemplate.showTableSectionTitle) {
                builder.append(ESC_ALIGN_RIGHT)
                builder.append(ESC_BOLD_ON)
                builder.appendArabicText("قائمة أصناف التشغيل والتجهيز:\n")
                builder.append(ESC_BOLD_OFF)
                builder.appendArabicText("--------------------------------\n")
            }

            for (item in filtered) {
                if (kitchenTemplate.highlightItemNames) {
                    builder.append(ESC_DOUBLE_HEIGHT)
                } else {
                    builder.append(ESC_BOLD_ON)
                }
                val kitchenName = item.kitchenName.ifBlank { item.itemName }
                builder.appendArabicText("• ${ReceiptBitmapRenderer.formatQuantity(item.quantity)} ${item.unit} * $kitchenName\n")
                builder.append(ESC_NORMAL)

                if (kitchenTemplate.showItemPrices) {
                    builder.appendArabicText("  السعر: ${item.unitPrice} ج.م | الإجمالي: ${item.totalPrice} ج.م\n")
                }

                if (kitchenTemplate.showPreparations && !item.preparationName.isNullOrBlank()) {
                    val prepPriceStr = if (kitchenTemplate.showItemPrices && item.preparationPrice > 0) {
                        val pStr = if (item.preparationPrice % 1.0 == 0.0) item.preparationPrice.toInt().toString() else String.format(Locale.US, "%.1f", item.preparationPrice)
                        "(+$pStr)للكيلو"
                    } else ""
                    builder.appendArabicText("  ${item.preparationName}$prepPriceStr\n")
                }
                if (kitchenTemplate.showOrderNotes && item.itemNotes.isNotBlank()) {
                    builder.appendArabicText("  ملحوظة خاصة: ${item.itemNotes}\n")
                }
                builder.appendArabicText("--------------------------------\n")
            }
        }

        if (kitchenTemplate.showOrderNotes && order.notes.isNotBlank()) {
            builder.append(ESC_BOLD_ON)
            builder.appendArabicText("ملاحظة عامة للأوردر: ${order.notes}\n")
            builder.append(ESC_BOLD_OFF)
            builder.appendArabicText("--------------------------------\n")
        }

        if (kitchenTemplate.showItemCountSummary) {
            builder.append(ESC_ALIGN_CENTER)
            val summaryText = when {
                filtered.isNotEmpty() && cancelledForTarget.isNotEmpty() ->
                    "إضافات للتجهيز: ${filtered.size} | أصناف ملغاة: ${cancelledForTarget.size}"
                cancelledForTarget.isNotEmpty() ->
                    "تنبيه إلغاء/تعديل لعدد (${cancelledForTarget.size}) صنف"
                else ->
                    "إجمالي الأصناف المطلوبة: ${filtered.size} صنف"
            }
            builder.appendArabicText("$summaryText\n")
            builder.appendArabicText("--------------------------------\n")
        }

        if (kitchenTemplate.showFooterNote) {
            builder.append(ESC_ALIGN_CENTER)
            val footer = kitchenTemplate.footerNote.ifBlank { "--- تذكرة تشغيل وتحضير داخلية للمطبخ ---" }
            builder.appendArabicText("$footer\n")
        }
        builder.append(GS_CUT_PAPER)

        return builder.toByteArray()
    }
}

// Helper ByteArrayBuilder to assemble ESC/POS streams
private class ByteArrayBuilder(private val mode: ArabicEscPosHelper.CodepageMode = ArabicEscPosHelper.CodepageMode.CP864_PAGE_22) {
    private val buffer = mutableListOf<Byte>()

    fun append(bytes: ByteArray) {
        for (b in bytes) {
            buffer.add(b)
        }
    }

    fun appendArabicText(text: String) {
        val encoded = ArabicEscPosHelper.formatTextForPrinter(text, mode)
        append(encoded)
    }

    fun toByteArray(): ByteArray = buffer.toByteArray()
}
