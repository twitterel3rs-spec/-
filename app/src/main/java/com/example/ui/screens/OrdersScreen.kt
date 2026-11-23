package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import com.example.ui.components.MouseLazyColumn
import com.example.ui.components.MouseLazyRow
import com.example.ui.components.mouseVerticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Update
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import com.example.printer.PrinterConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.CompositionLocalProvider
import com.example.util.normalizeArabicDigits
import com.example.util.convertEnglishKeyboardToArabic
import com.example.util.containsLatinLetters
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeliveryDriver
import com.example.data.model.OrderWithItems
import com.example.printer.ReceiptBitmapRenderer
import com.example.ui.components.OrderCustomerEditorDialog
import com.example.ui.components.OrderInvoiceAndPrintingDialog
import com.example.ui.components.OrderItemsEditorDialog
import com.example.ui.components.PasswordPromptDialog
import com.example.ui.components.PaymentSettlementDialog
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.ArabicPlatformTextStyle
import androidx.compose.ui.text.PlatformTextStyle
import com.example.ui.theme.ArabicLineHeightStyle
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
private val CompactPlatformTextStyle = PlatformTextStyle(includeFontPadding = false)

@Composable
private fun OrderScreenSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 38.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    prefixText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }
    val isCompactNumberBox = !prefixText.isNullOrBlank()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = modifier
                .height(height)
                .testTag(testTag),
            shape = shape,
            color = if (isFocused) Color.White else Color(0xFFF8FAFC),
            border = BorderStroke(
                width = if (isFocused || value.isNotBlank()) 1.5.dp else 1.dp,
                color = if (isFocused || value.isNotBlank()) SeaTealPrimary else Color(0xFFCBD5E1)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isCompactNumberBox) 4.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (!prefixText.isNullOrBlank()) {
                    Text(
                        text = prefixText,
                        fontSize = if (height > 40.dp) 14.sp else 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SeaTealPrimary,
                        style = TextStyle(
                            fontFamily = CairoFontFamily,
                            textDirection = TextDirection.Rtl,
                            platformStyle = CompactPlatformTextStyle
                        )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder.isNotBlank()) {
                        Text(
                            text = placeholder,
                            fontSize = if (height > 40.dp) 13.sp else 12.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                fontFamily = CairoFontFamily,
                                textDirection = TextDirection.Rtl,
                                platformStyle = CompactPlatformTextStyle
                            )
                        )
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = ImeAction.Search,
                            autoCorrect = false
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { /* done */ }
                        ),
                        cursorBrush = SolidColor(SeaTealPrimary),
                        textStyle = TextStyle(
                            fontFamily = CairoFontFamily,
                            fontSize = if (height > 40.dp) 14.5.sp else 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SeaNavyDeep,
                            textDirection = if (isCompactNumberBox) TextDirection.Ltr else TextDirection.ContentOrRtl,
                            platformStyle = CompactPlatformTextStyle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                    )
                }

                if (value.isNotBlank() && !isCompactNumberBox) {
                    if (value.containsLatinLetters()) {
                        val converted = convertEnglishKeyboardToArabic(value)
                        if (converted != value) {
                            Surface(
                                onClick = { onValueChange(converted) },
                                shape = RoundedCornerShape(4.dp),
                                color = SeaTealPrimary.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, SeaTealPrimary),
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .height(if (height > 40.dp) 24.dp else 20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تحويل لعربي",
                                        tint = SeaTealPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "عربي",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SeaTealPrimary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(if (height > 40.dp) 24.dp else 20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "مسح",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(if (height > 40.dp) 16.dp else 13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrdersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val drivers by viewModel.drivers.collectAsStateWithLifecycle()
    val allPrinters by viewModel.allPrinters.collectAsStateWithLifecycle()
    val remoteSyncedPrinters by viewModel.remoteSyncedPrinters.collectAsStateWithLifecycle()
    val highlightedOrderId by viewModel.highlightedOrderId.collectAsStateWithLifecycle()
    val orderVisibilityPulse by viewModel.orderVisibilityPulse.collectAsStateWithLifecycle()
    val deviceRole by viewModel.deviceRole.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var orderNumberSearchQuery by remember { mutableStateOf("") }
    var isDeliveryBatchMode by remember { mutableStateOf(false) }
    val selectedDeliveryOrderIds = remember { mutableStateListOf<Long>() }
    var showDeliveryPrinterDialog by remember { mutableStateOf(false) }
    var selectedDeliveryPrinterId by remember { mutableStateOf<String?>(null) }
    var selectedManifestFontSize by remember { mutableStateOf("LARGE") }

    // Multi-dimensional filter states:
    // 1. Preset / Quick Category
    var selectedPresetFilter by remember { mutableStateOf("ALL") }
    // 2. Order Type filter: "ALL", "DELIVERY", "TAKEAWAY"
    var selectedOrderTypeFilter by remember { mutableStateOf("ALL") }
    // 3. Dispatch / Store Presence: "ALL", "IN_STORE", "OUT_STORE", "COMPLETED"
    var selectedDispatchFilter by remember { mutableStateOf("ALL") }
    // 4. Delivery Driver filter: null = all drivers, -1L = unassigned delivery, > 0 = specific driverId
    var selectedDriverId by remember { mutableStateOf<Long?>(null) }
    // 5. Detailed status filter
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    // Handle incoming navigation to order from POS screen ("تأكيد وانتقال للاوردر")
    LaunchedEffect(highlightedOrderId, orderVisibilityPulse) {
        if (highlightedOrderId != null || orderVisibilityPulse > 0L) {
            selectedPresetFilter = "ALL"
            selectedOrderTypeFilter = "ALL"
            selectedDispatchFilter = "ALL"
            selectedDriverId = null
            selectedStatusFilter = "ALL"
            searchQuery = ""
            orderNumberSearchQuery = ""
        }
    }



    // UI Dialog & Sheet states
    val requestedInvoiceOrder by viewModel.requestedInvoiceOrder.collectAsStateWithLifecycle()
    var orderForInvoice by remember { mutableStateOf<OrderWithItems?>(null) }
    var orderForCustomerEdit by remember { mutableStateOf<OrderWithItems?>(null) }
    var orderForItemsEdit by remember { mutableStateOf<OrderWithItems?>(null) }
    var orderForPaymentSettlement by remember { mutableStateOf<OrderWithItems?>(null) }
    var orderToCancel by remember { mutableStateOf<OrderWithItems?>(null) }
    var orderForDriverAssign by remember { mutableStateOf<OrderWithItems?>(null) }
    var isDriverFilterDropdownOpen by remember { mutableStateOf(false) }

    LaunchedEffect(requestedInvoiceOrder) {
        requestedInvoiceOrder?.let {
            orderForInvoice = it
            viewModel.clearRequestedInvoiceOrder()
        }
    }

    // Summary counts
    val archivedCount = remember(orders) {
        orders.count { it.order.isArchived }
    }
    val inStoreCount = remember(orders) {
        orders.count { !it.order.isArchived && it.order.status in listOf("NEW", "PREPARING") }
    }
    val outForDeliveryCount = remember(orders) {
        orders.count { 
            !it.order.isArchived && (
                (it.order.orderType == "DELIVERY" && (it.order.status == "OUT_FOR_DELIVERY" || it.order.status == "COMPLETED")) ||
                (it.order.orderType != "DELIVERY" && it.order.status == "COMPLETED")
            )
        }
    }
    val deliveryCount = remember(orders) {
        orders.count { !it.order.isArchived && it.order.orderType == "DELIVERY" && it.order.status != "CANCELLED" }
    }
    val takeawayCount = remember(orders) {
        orders.count { !it.order.isArchived && it.order.orderType != "DELIVERY" && it.order.status != "CANCELLED" }
    }
    val completedCount = remember(orders) {
        orders.count { !it.order.isArchived && it.order.status == "COMPLETED" }
    }

    val allOrdersTotalCount = remember(orders) {
        orders.count { !it.order.isArchived }
    }

    // Quick preset options
    val presetFilters = listOf(
        "ALL" to "جميع الأوردرات ($allOrdersTotalCount)",
        "IN_STORE" to "🏠 لسه في المحل ($inStoreCount)",
        "OUT_STORE" to "🚀 خرج من المحل ($outForDeliveryCount)",
        "DELIVERY" to "🛵 دليفري ($deliveryCount)",
        "TAKEAWAY" to "🏪 حجز واستلام ($takeawayCount)",
        "COMPLETED" to "✅ تم التسليم ($completedCount)",
        "CANCELLED" to "❌ ملغي",
        "ARCHIVED" to "📦 الأوردرات المؤرشفة ($archivedCount)"
    )

    // Active Driver Selected Object
    val selectedDriver = remember(drivers, selectedDriverId) {
        if (selectedDriverId != null && selectedDriverId != -1L) {
            drivers.firstOrNull { it.id == selectedDriverId }
        } else null
    }

    val isSearching = searchQuery.isNotBlank() || orderNumberSearchQuery.isNotBlank()

    // Multi-criteria filtering
    val filteredOrders = orders.filter { o ->
        val ord = o.order

        // Archive Filtering:
        // - In ARCHIVED tab: show only archived orders.
        // - In active tabs: show only active (unarchived) orders.
        if (selectedPresetFilter == "ARCHIVED") {
            if (!ord.isArchived) return@filter false
        } else {
            if (ord.isArchived) return@filter false
        }

        // 0. Dedicated Order Number Search Box Filter
        val numQ = orderNumberSearchQuery.normalizeArabicDigits().trim().removePrefix("#").trim()
        val numQClean = numQ.trimStart('0')
        val cleanOrdNum = ord.orderNumber.normalizeArabicDigits().trim().removePrefix("#").trim()
        val cleanOrdId = ord.id.toString()

        val isOrderNumSearchActive = numQ.isNotBlank()
        val matchesOrderNumber = !isOrderNumSearchActive ||
            cleanOrdNum == numQ ||
            (numQClean.isNotBlank() && cleanOrdNum.trimStart('0') == numQClean) ||
            cleanOrdNum.contains(numQ) ||
            cleanOrdId == numQ ||
            cleanOrdId == numQClean

        // If the user explicitly searched for a specific order number in the dedicated box:
        // Prioritize finding this order across all workflow statuses, matching general text query if also entered
        if (isOrderNumSearchActive) {
            val q = searchQuery.normalizeArabicDigits().trim()
            val qAr = convertEnglishKeyboardToArabic(q).trim()
            val matchesSearch = q.isBlank() ||
                ord.customerName.contains(searchQuery.trim(), ignoreCase = true) ||
                ord.customerPhone.contains(searchQuery.trim(), ignoreCase = true) ||
                (ord.deliveryAddressFull?.contains(searchQuery.trim(), ignoreCase = true) == true) ||
                ord.orderNumber.contains(searchQuery.trim(), ignoreCase = true) ||
                cleanOrdNum.contains(q) ||
                cleanOrdId == q ||
                (qAr.isNotBlank() && qAr != q && (
                    ord.customerName.contains(qAr, ignoreCase = true) ||
                    (ord.deliveryAddressFull?.contains(qAr, ignoreCase = true) == true)
                ))

            return@filter matchesOrderNumber && matchesSearch
        }

        // 1. General Text Search Filter
        val q = searchQuery.normalizeArabicDigits().trim()
        val qClean = q.removePrefix("#").trimStart('0')
        val qAr = convertEnglishKeyboardToArabic(q).trim()
        val qArClean = qAr.removePrefix("#").trimStart('0')
        val matchesSearch = q.isBlank() ||
            ord.customerName.contains(searchQuery.trim(), ignoreCase = true) ||
            ord.customerPhone.contains(searchQuery.trim(), ignoreCase = true) ||
            (ord.deliveryAddressFull?.contains(searchQuery.trim(), ignoreCase = true) == true) ||
            (ord.customerArea?.contains(searchQuery.trim(), ignoreCase = true) == true) ||
            ord.orderNumber.contains(searchQuery.trim(), ignoreCase = true) ||
            cleanOrdNum.contains(q) ||
            (qClean.isNotBlank() && cleanOrdNum.trimStart('0') == qClean) ||
            cleanOrdId == q ||
            cleanOrdId == qClean ||
            // Dual search if typed with English layout on LDPlayer emulator
            (qAr.isNotBlank() && qAr != q && (
                ord.customerName.contains(qAr, ignoreCase = true) ||
                (ord.deliveryAddressFull?.contains(qAr, ignoreCase = true) == true) ||
                (ord.customerArea?.contains(qAr, ignoreCase = true) == true) ||
                ord.orderNumber.contains(qAr, ignoreCase = true) ||
                cleanOrdNum.contains(qAr) ||
                (qArClean.isNotBlank() && cleanOrdNum.trimStart('0') == qArClean)
            ))

        // 2. Preset Filter (Solves: قايمة خرج من المحل مش للدليفري بس للاستلام كمان بس لما يتأكد الاستلام)
        val matchesPreset = when (selectedPresetFilter) {
            "ALL" -> true
            "ARCHIVED" -> true
            "IN_STORE" -> ord.status in listOf("NEW", "PREPARING")
            "OUT_STORE" -> {
                if (ord.orderType == "DELIVERY") {
                    ord.status in listOf("OUT_FOR_DELIVERY", "COMPLETED")
                } else {
                    ord.status == "COMPLETED"
                }
            }
            "DELIVERY" -> ord.orderType == "DELIVERY"
            "TAKEAWAY" -> ord.orderType != "DELIVERY"
            "COMPLETED" -> ord.status == "COMPLETED"
            "CANCELLED" -> ord.status == "CANCELLED"
            else -> true
        }

        // 3. Order Type Filter (Delivery vs Takeaway / Reservation)
        val matchesOrderType = when (selectedOrderTypeFilter) {
            "ALL" -> true
            "DELIVERY" -> ord.orderType == "DELIVERY"
            "TAKEAWAY" -> ord.orderType != "DELIVERY"
            else -> true
        }

        // 4. Dispatch / Store Presence Filter (In store vs Out of store)
        val matchesDispatch = when (selectedDispatchFilter) {
            "ALL" -> true
            "IN_STORE" -> ord.status in listOf("NEW", "PREPARING")
            "OUT_STORE" -> {
                if (ord.orderType == "DELIVERY") {
                    ord.status in listOf("OUT_FOR_DELIVERY", "COMPLETED")
                } else {
                    ord.status == "COMPLETED"
                }
            }
            "COMPLETED" -> ord.status == "COMPLETED"
            else -> true
        }

        // 5. Driver Filter
        val matchesDriver = when {
            selectedDriverId == null -> true
            selectedDriverId == -1L -> ord.orderType == "DELIVERY" && (ord.driverId == null || ord.driverId == 0L)
            else -> ord.driverId == selectedDriverId
        }

        // 6. Status Filter
        val matchesStatus = selectedStatusFilter == "ALL" || ord.status == selectedStatusFilter

        matchesSearch && matchesPreset && matchesOrderType && matchesDispatch && matchesDriver && matchesStatus
    }

    val hasActiveFilter = selectedPresetFilter != "ALL" ||
        selectedOrderTypeFilter != "ALL" ||
        selectedDispatchFilter != "ALL" ||
        selectedDriverId != null ||
        selectedStatusFilter != "ALL" ||
        searchQuery.isNotBlank() ||
        orderNumberSearchQuery.isNotBlank()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
    val prevDayOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val startOfTodayMs = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (isLandscape) 4.dp else 12.dp)
    ) {
        if (isLandscape) {
            // Landscape Ultra-Compact Filter Bar - Row 1: Search & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. General search field (takes all available width!)
                OrderScreenSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "بحث بالاسم، الهاتف، العنوان...",
                    height = 38.dp,
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = SeaTealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "orders_search_input"
                )

                // 2. Dedicated compact order number search box (#) - slim 48dp width
                OrderScreenSearchField(
                    value = orderNumberSearchQuery,
                    onValueChange = { orderNumberSearchQuery = it },
                    placeholder = "",
                    prefixText = "#",
                    height = 38.dp,
                    shape = RoundedCornerShape(8.dp),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.width(48.dp),
                    testTag = "order_number_search_input"
                )

                // 3. Small Delivery Batch Print Button
                Surface(
                    onClick = {
                        if (selectedDeliveryOrderIds.isNotEmpty()) {
                            val defaultCashier = allPrinters.firstOrNull { it.isCashierDefault }
                                ?: allPrinters.firstOrNull { it.role == "CUSTOMER_INVOICE" || it.role == "BOTH" }
                                ?: allPrinters.firstOrNull()
                            selectedDeliveryPrinterId = defaultCashier?.id
                            showDeliveryPrinterDialog = true
                        } else {
                            isDeliveryBatchMode = !isDeliveryBatchMode
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDeliveryBatchMode || selectedDeliveryOrderIds.isNotEmpty()) AmberCoral else SeaTealPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isDeliveryBatchMode || selectedDeliveryOrderIds.isNotEmpty()) AmberCoral else SeaTealPrimary.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("delivery_batch_mode_btn_landscape")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedDeliveryOrderIds.isNotEmpty()) Icons.Default.Print else Icons.Default.DeliveryDining,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isDeliveryBatchMode || selectedDeliveryOrderIds.isNotEmpty()) Color.White else SeaTealPrimary
                        )
                        Text(
                            text = if (selectedDeliveryOrderIds.isNotEmpty()) {
                                "طباعة (${selectedDeliveryOrderIds.size})"
                            } else {
                                "دليفري"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeliveryBatchMode || selectedDeliveryOrderIds.isNotEmpty()) Color.White else SeaTealPrimary,
                            style = TextStyle(
                                fontFamily = CairoFontFamily,
                                platformStyle = CompactPlatformTextStyle
                            )
                        )
                    }
                }

                

                if (hasActiveFilter) {
                    Surface(
                        onClick = {
                            selectedPresetFilter = "ALL"
                            selectedOrderTypeFilter = "ALL"
                            selectedDispatchFilter = "ALL"
                            selectedDriverId = null
                            selectedStatusFilter = "ALL"
                            searchQuery = ""
                            orderNumberSearchQuery = ""
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = StatusDanger.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .height(38.dp)
                            .width(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "إعادة ضبط الفلاتر",
                                tint = StatusDanger,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Scrollable count badge, driver dropdown, and quick preset filter chips
            MouseLazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // (A) Orders Count Badge
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SeaTealPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = "${filteredOrders.size} طلب",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SeaTealPrimary,
                                style = TextStyle(
                                    fontFamily = CairoFontFamily,
                                    platformStyle = ArabicPlatformTextStyle,
                                    lineHeightStyle = ArabicLineHeightStyle
                                )
                            )
                        }
                    }
                }

                // (B) Driver Filter Dropdown Button
                item {
                    Box {
                        Surface(
                            onClick = { isDriverFilterDropdownOpen = true },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDriverId != null) SeaTealPrimary.copy(alpha = 0.15f) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (selectedDriverId != null) SeaTealPrimary else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = SeaTealPrimary
                                )
                                Text(
                                    text = when {
                                        selectedDriverId == null -> "الموصلين ▾"
                                        selectedDriverId == -1L -> "بدون كابتن ✕"
                                        else -> "${selectedDriver?.name ?: ""} ✕"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedDriverId != null) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedDriverId != null) SeaTealPrimary else MaterialTheme.colorScheme.onSurface,
                                    style = TextStyle(
                                        fontFamily = CairoFontFamily,
                                        platformStyle = ArabicPlatformTextStyle,
                                        lineHeightStyle = ArabicLineHeightStyle
                                    )
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isDriverFilterDropdownOpen,
                            onDismissRequest = { isDriverFilterDropdownOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🛵 جميع الموصلين", fontSize = 12.sp) },
                                onClick = {
                                    selectedDriverId = null
                                    isDriverFilterDropdownOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⚠️ دليفري بدون كابتن (معلق)", fontSize = 12.sp, color = AmberCoral, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    selectedDriverId = -1L
                                    isDriverFilterDropdownOpen = false
                                }
                            )
                            HorizontalDivider()
                            drivers.forEach { drv ->
                                val driverActiveOrdersCount = orders.count {
                                    it.order.driverId == drv.id && it.order.status in listOf("NEW", "PREPARING", "OUT_FOR_DELIVERY")
                                }
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(drv.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            if (driverActiveOrdersCount > 0) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = SeaTealPrimary.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "$driverActiveOrdersCount طلب",
                                                        fontSize = 10.sp,
                                                        color = SeaTealPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedDriverId = drv.id
                                        isDriverFilterDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // (C) Quick Preset Filter Chips Bar
                items(presetFilters) { (presetKey, label) ->
                    val countLabel = when (presetKey) {
                        "IN_STORE" -> " ($inStoreCount)"
                        "OUT_STORE" -> " ($outForDeliveryCount)"
                        "DELIVERY" -> " ($deliveryCount)"
                        "TAKEAWAY" -> " ($takeawayCount)"
                        "COMPLETED" -> " ($completedCount)"
                        else -> ""
                    }
                    FilterChip(
                        selected = selectedPresetFilter == presetKey,
                        onClick = {
                            selectedPresetFilter = presetKey
                            selectedDriverId = null
                            when (presetKey) {
                                "IN_STORE" -> {
                                    selectedOrderTypeFilter = "ALL"
                                    selectedDispatchFilter = "IN_STORE"
                                    selectedStatusFilter = "ALL"
                                }
                                "OUT_STORE" -> {
                                    selectedOrderTypeFilter = "ALL"
                                    selectedDispatchFilter = "OUT_STORE"
                                    selectedStatusFilter = "ALL"
                                }
                                "DELIVERY" -> {
                                    selectedOrderTypeFilter = "DELIVERY"
                                    selectedDispatchFilter = "ALL"
                                    selectedStatusFilter = "ALL"
                                }
                                "TAKEAWAY" -> {
                                    selectedOrderTypeFilter = "TAKEAWAY"
                                    selectedDispatchFilter = "ALL"
                                    selectedStatusFilter = "ALL"
                                }
                                "COMPLETED" -> {
                                    selectedOrderTypeFilter = "ALL"
                                    selectedDispatchFilter = "ALL"
                                    selectedStatusFilter = "COMPLETED"
                                }
                                else -> {
                                    selectedOrderTypeFilter = "ALL"
                                    selectedDispatchFilter = "ALL"
                                    selectedStatusFilter = "ALL"
                                }
                            }
                        },
                        label = { Text("$label$countLabel", fontSize = 9.5.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        } else {
            // Portrait Search in Orders Bar - Aligned to height 46.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. General search field
                OrderScreenSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "بحث (اسم، هاتف، عنوان)...",
                    height = 46.dp,
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = SeaTealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "orders_search_input"
                )

                // 2. Dedicated order number search box (#)
                OrderScreenSearchField(
                    value = orderNumberSearchQuery,
                    onValueChange = { orderNumberSearchQuery = it },
                    placeholder = "",
                    prefixText = "#",
                    height = 46.dp,
                    shape = RoundedCornerShape(10.dp),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.width(54.dp),
                    testTag = "order_number_search_input"
                )

                if (hasActiveFilter) {
                    Surface(
                        onClick = {
                            selectedPresetFilter = "ALL"
                            selectedOrderTypeFilter = "ALL"
                            selectedDispatchFilter = "ALL"
                            selectedDriverId = null
                            selectedStatusFilter = "ALL"
                            searchQuery = ""
                            orderNumberSearchQuery = ""
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = StatusDanger.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .height(46.dp)
                            .width(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "إعادة ضبط الفلاتر",
                                tint = StatusDanger,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Preset Filter Chips Bar
            MouseLazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(presetFilters) { (presetKey, label) ->
                    val countLabel = when (presetKey) {
                        "IN_STORE" -> " ($inStoreCount)"
                        "OUT_STORE" -> " ($outForDeliveryCount)"
                        "DELIVERY" -> " ($deliveryCount)"
                        "TAKEAWAY" -> " ($takeawayCount)"
                        "COMPLETED" -> " ($completedCount)"
                        else -> ""
                    }
                    FilterChip(
                        selected = selectedPresetFilter == presetKey,
                        onClick = {
                            selectedPresetFilter = presetKey
                            selectedDriverId = null
                            when (presetKey) {
                                "IN_STORE" -> {
                                    selectedOrderTypeFilter = "ALL"
                                    selectedDispatchFilter = "IN_STORE"
                                    selectedStatusFilter = "ALL"
                                }
                                "OUT_STORE" -> {
                                    selectedOrderTypeFilter = "ALL"
                                    selectedDispatchFilter = "OUT_STORE"
                                    selectedStatusFilter = "ALL"
                                }
                                "DELIVERY" -> {
                                    selectedOrderTypeFilter = "DELIVERY"
                                    selectedDispatchFilter = "ALL"
                                    selectedStatusFilter = "ALL"
                                }
                                "TAKEAWAY" -> {
                                    selectedOrderTypeFilter = "TAKEAWAY"
                                    selectedDispatchFilter = "ALL"
                                    selectedStatusFilter = "ALL"
                                }
                                "COMPLETED" -> {
                                    selectedOrderTypeFilter = "ALL"
                                    selectedDispatchFilter = "ALL"
                                    selectedStatusFilter = "COMPLETED"
                                }
                                else -> {
                                    selectedOrderTypeFilter = "ALL"
                                    selectedDispatchFilter = "ALL"
                                    selectedStatusFilter = "ALL"
                                }
                            }
                        },
                        label = { Text("$label$countLabel", fontSize = 11.5.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Advanced Multi-dimensional Filter Bar (Type, Dispatch, Driver)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Driver Filter Dropdown Button
                    Box {
                        OutlinedButton(
                            onClick = { isDriverFilterDropdownOpen = true },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedDriverId != null) SeaTealPrimary.copy(alpha = 0.15f) else Color.Transparent
                            )
                        ) {
                            Icon(imageVector = Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(14.dp), tint = SeaTealPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    selectedDriverId == null -> "كل الموصلين ▾"
                                    selectedDriverId == -1L -> "بدون كابتن ✕"
                                    else -> "كابتن: ${selectedDriver?.name ?: ""} ✕"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (selectedDriverId != null) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedDriverId != null) SeaTealPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = isDriverFilterDropdownOpen,
                            onDismissRequest = { isDriverFilterDropdownOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🛵 جميع الموصلين", fontSize = 12.sp) },
                                onClick = {
                                    selectedDriverId = null
                                    isDriverFilterDropdownOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⚠️ دليفري بدون كابتن (معلق)", fontSize = 12.sp, color = AmberCoral, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    selectedDriverId = -1L
                                    isDriverFilterDropdownOpen = false
                                }
                            )
                            HorizontalDivider()
                            if (drivers.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("لا يوجد موصلين مسجلين", fontSize = 11.sp, color = Color.Gray) },
                                    onClick = { isDriverFilterDropdownOpen = false }
                                )
                            } else {
                                drivers.forEach { drv ->
                                    val driverActiveOrdersCount = orders.count {
                                        it.order.driverId == drv.id && it.order.status in listOf("NEW", "PREPARING", "OUT_FOR_DELIVERY")
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(drv.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                if (driverActiveOrdersCount > 0) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = SeaTealPrimary.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "$driverActiveOrdersCount طلب",
                                                            fontSize = 10.sp,
                                                            color = SeaTealPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedDriverId = drv.id
                                            isDriverFilterDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Type Quick Segmented Chip (Delivery vs Takeaway)
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedOrderTypeFilter == "DELIVERY",
                            onClick = {
                                selectedOrderTypeFilter = if (selectedOrderTypeFilter == "DELIVERY") "ALL" else "DELIVERY"
                            },
                            label = { Text("دليفري", fontSize = 10.5.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                        FilterChip(
                            selected = selectedOrderTypeFilter == "TAKEAWAY",
                            onClick = {
                                selectedOrderTypeFilter = if (selectedOrderTypeFilter == "TAKEAWAY") "ALL" else "TAKEAWAY"
                            },
                            label = { Text("حجز/استلام", fontSize = 10.5.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                    }

                    // Reset Filters Button if any active
                    if (hasActiveFilter) {
                        IconButton(
                            onClick = {
                                selectedPresetFilter = "ALL"
                                selectedOrderTypeFilter = "ALL"
                                selectedDispatchFilter = "ALL"
                                selectedDriverId = null
                                selectedStatusFilter = "ALL"
                                searchQuery = ""
                                orderNumberSearchQuery = ""
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "إعادة ضبط الفلاتر", tint = StatusDanger, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Header Title with Count and Active Filter Indicator & Delivery Print Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "قائمة الأوردرات (${filteredOrders.size}):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                // Current filter description summary
                if (hasActiveFilter) {
                    Text(
                        text = buildString {
                            if (selectedPresetFilter != "ALL") append(presetFilters.firstOrNull { it.first == selectedPresetFilter }?.second ?: "")
                            if (selectedDriver != null) append(" • مع: ${selectedDriver.name}")
                            if (selectedDriverId == -1L) append(" • بدون كابتن")
                        },
                        fontSize = 11.sp,
                        color = SeaTealPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                // Small "طباعة للدليفري" Button in Header
                Surface(
                    onClick = {
                        isDeliveryBatchMode = !isDeliveryBatchMode
                        if (!isDeliveryBatchMode) {
                            selectedDeliveryOrderIds.clear()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDeliveryBatchMode) AmberCoral else SeaTealPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isDeliveryBatchMode) AmberCoral else SeaTealPrimary.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("delivery_batch_mode_btn_header")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isDeliveryBatchMode) Color.White else SeaTealPrimary
                        )
                        Text(
                            text = if (isDeliveryBatchMode && selectedDeliveryOrderIds.isNotEmpty()) {
                                "طباعة (${selectedDeliveryOrderIds.size}) للدليفري"
                            } else {
                                "طباعة للدليفري"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeliveryBatchMode) Color.White else SeaTealPrimary,
                            style = TextStyle(
                                fontFamily = CairoFontFamily,
                                platformStyle = ArabicPlatformTextStyle,
                                lineHeightStyle = ArabicLineHeightStyle
                            )
                        )
                    }
                }
            }
        }

        // Delivery Multi-Selection Action Bar (appears when isDeliveryBatchMode is true)
        if (isDeliveryBatchMode) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SeaTealPrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, SeaTealPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("delivery_batch_action_panel")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeliveryDining,
                                contentDescription = null,
                                tint = SeaTealPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "وضع تحديد أوردرات الدليفري للطباعة:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SeaNavyDeep,
                                style = TextStyle(fontFamily = CairoFontFamily)
                            )
                        }

                        // Prominent Count Display: "تم تحديد (X) أوردر"
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedDeliveryOrderIds.isNotEmpty()) AmberCoral else Color(0xFF64748B)
                        ) {
                            Text(
                                text = "تم تحديد (${selectedDeliveryOrderIds.size}) أوردر",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = TextStyle(fontFamily = CairoFontFamily)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Direct Print Delivery Manifest on default Cashier printer
                        Button(
                            onClick = {
                                val toPrint = orders.filter { selectedDeliveryOrderIds.contains(it.order.id) }
                                viewModel.printDeliveryBatch(toPrint, null)
                            },
                            enabled = selectedDeliveryOrderIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SeaTealPrimary,
                                disabledContainerColor = Color(0xFFCBD5E1)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(38.dp)
                                .testTag("print_delivery_batch_confirm_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "طباعة كشف تسليم الدليفري (${selectedDeliveryOrderIds.size}) 🛵",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(fontFamily = CairoFontFamily)
                            )
                        }

                        // 2. Select any registered printer dialog button
                        OutlinedButton(
                            onClick = {
                                val defaultCashier = allPrinters.firstOrNull { it.isCashierDefault }
                                    ?: allPrinters.firstOrNull { it.role == "CUSTOMER_INVOICE" || it.role == "BOTH" }
                                    ?: allPrinters.firstOrNull()
                                selectedDeliveryPrinterId = defaultCashier?.id
                                showDeliveryPrinterDialog = true
                            },
                            enabled = selectedDeliveryOrderIds.isNotEmpty(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SeaTealPrimary),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(38.dp)
                                .testTag("select_delivery_printer_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "تحديد طابعة ▾",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(fontFamily = CairoFontFamily)
                            )
                        }

                        // 3. Select All / Deselect All Button
                        OutlinedButton(
                            onClick = {
                                val displayedIds = filteredOrders.map { it.order.id }
                                val allSelected = displayedIds.isNotEmpty() && displayedIds.all { selectedDeliveryOrderIds.contains(it) }
                                if (allSelected) {
                                    selectedDeliveryOrderIds.removeAll(displayedIds)
                                } else {
                                    for (id in displayedIds) {
                                        if (!selectedDeliveryOrderIds.contains(id)) {
                                            selectedDeliveryOrderIds.add(id)
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            val allDisplayedSelected = filteredOrders.isNotEmpty() && filteredOrders.all { selectedDeliveryOrderIds.contains(it.order.id) }
                            Text(
                                text = if (allDisplayedSelected) "إلغاء الكل" else "تحديد الكل (${filteredOrders.size})",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = TextStyle(fontFamily = CairoFontFamily)
                            )
                        }

                        // 4. Exit Mode Button
                        OutlinedButton(
                            onClick = {
                                isDeliveryBatchMode = false
                                selectedDeliveryOrderIds.clear()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                            border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.5f)),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(
                                text = "إلغاء ✕",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusDanger,
                                style = TextStyle(fontFamily = CairoFontFamily)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LaunchedEffect(highlightedOrderId, orderVisibilityPulse) {
            val hId = highlightedOrderId
            if (hId != null || orderVisibilityPulse > 0L) {
                // Reset any filters that might hide the newly placed order so it is immediately visible
                if (selectedPresetFilter != "ALL") selectedPresetFilter = "ALL"
                if (selectedOrderTypeFilter != "ALL") selectedOrderTypeFilter = "ALL"
                if (selectedDispatchFilter != "ALL") selectedDispatchFilter = "ALL"
                if (selectedDriverId != null) selectedDriverId = null
                if (selectedStatusFilter != "ALL") selectedStatusFilter = "ALL"
                if (searchQuery.isNotBlank()) searchQuery = ""
                if (orderNumberSearchQuery.isNotBlank()) orderNumberSearchQuery = ""
            }
        }

        LaunchedEffect(highlightedOrderId, orderVisibilityPulse, filteredOrders) {
            val hId = highlightedOrderId
            if (hId != null) {
                val targetIdx = filteredOrders.indexOfFirst { it.order.id == hId }
                if (targetIdx >= 0) {
                    lazyListState.animateScrollToItem(targetIdx)
                } else if (filteredOrders.isNotEmpty()) {
                    lazyListState.animateScrollToItem(0)
                }
            } else if (orderVisibilityPulse > 0L && filteredOrders.isNotEmpty()) {
                lazyListState.animateScrollToItem(0)
            }
        }

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isLandscape) 10.dp else 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(if (isLandscape) 28.dp else 44.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = if (orders.isEmpty()) "لا توجد أوردرات مسجلة في قاعدة البيانات بعد" else "لا توجد أوردرات مطابقة للفلتر المحدد",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = if (isLandscape) 12.sp else 14.sp
                    )
                    if (orders.isNotEmpty()) {
                        Text(
                            text = "إجمالي الأوردرات المسجلة في النظام: ${orders.size} أوردر (منها $archivedCount طلب في الأرشيف)",
                            fontSize = 11.5.sp,
                            color = SeaNavyDeep,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                selectedPresetFilter = "ALL"
                                selectedOrderTypeFilter = "ALL"
                                selectedDispatchFilter = "ALL"
                                selectedDriverId = null
                                selectedStatusFilter = "ALL"
                                searchQuery = ""
                                orderNumberSearchQuery = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                        ) {
                            Text("عرض كافة الأوردرات النشطة ($allOrdersTotalCount)")
                        }
                    }
                }
            }
        } else {
            MouseLazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (isLandscape) 5.dp else 10.dp)
            ) {
                if (selectedPresetFilter == "ARCHIVED") {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SeaNavyDeep,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("📦", fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "قائمة الأوردرات المؤرشفة ($archivedCount أوردر)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SeaNavyDeep
                                    )
                                    Text(
                                        text = "تم عزل وأرشفة هذه الأوردرات محلياً وتلقائياً عند تقفيل الوردية أو يدوياً حتى لا تظهر في قائمة الأوردرات النشطة. يمكنك الاطلاع عليها وإعادة طباعتها أو إلغاء أرشفتها في أي وقت.",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF475569),
                                        lineHeight = 15.sp
                                    )
                                }
                                if (archivedCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.unarchiveAllOrders() },
                                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("استعادة الكل من الأرشيف ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedPresetFilter == "COMPLETED") {
                    val completedUnarchivedIds = filteredOrders.filter { it.order.status == "COMPLETED" && !it.order.isArchived }.map { it.order.id }
                    if (completedUnarchivedIds.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "يوجد ${completedUnarchivedIds.size} أوردر مكتمل في القائمة النشطة",
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = { viewModel.archiveAllOrders(completedUnarchivedIds) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("أرشفة المكتمل للأرشيف 📦", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                itemsIndexed(filteredOrders, key = { index, it -> "order_${it.order.id}_$index" }) { index, orderWithItems ->
                    val order = orderWithItems.order
                    val items = orderWithItems.items
                    val isEven = index % 2 == 0

                    val isInStore = order.status in listOf("NEW", "PREPARING")
                    val isOutForDelivery = order.status == "OUT_FOR_DELIVERY"
                    val isCompleted = order.status == "COMPLETED"
                    val isCancelled = order.status == "CANCELLED"
                    val isHighlighted = highlightedOrderId == order.id

                    val statusColor = when (order.status) {
                        "NEW" -> StatusWarning
                        "PREPARING" -> SeaTealPrimary
                        "OUT_FOR_DELIVERY" -> StatusInfo
                        "COMPLETED" -> StatusSuccess
                        "CANCELLED" -> StatusDanger
                        else -> Color.Gray
                    }

                    val statusLabel = when (order.status) {
                        "NEW" -> if (order.orderType == "DELIVERY") "جديد (في المحل)" else "جديد (في المحل - غير محسوب بالخزينة)"
                        "PREPARING" -> if (order.orderType == "DELIVERY") "قيد التجهيز (في المحل)" else "قيد التجهيز (في المحل - غير محسوب بالخزينة)"
                        "OUT_FOR_DELIVERY" -> "خارج للتوصيل (خرج من المحل)"
                        "COMPLETED" -> if (order.orderType == "DELIVERY") "تم التسليم بنجاح (خرج من المحل)" else "تم استلام العميل (خرج من المحل ومحسوب بالخزينة ✓)"
                        "CANCELLED" -> "ملغي"
                        else -> order.status
                    }

                    val isSelectedForDelivery = isDeliveryBatchMode && selectedDeliveryOrderIds.contains(order.id)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isSelectedForDelivery -> SeaTealPrimary.copy(alpha = 0.14f)
                                isHighlighted -> AmberCoral.copy(alpha = 0.08f)
                                isEven -> Color(0xFFFFFFFF)
                                else -> Color(0xFFE2E8F0)
                            }
                        ),
                        border = when {
                            isSelectedForDelivery -> BorderStroke(2.5.dp, SeaTealPrimary)
                            isHighlighted -> BorderStroke(2.dp, AmberCoral)
                            isEven -> BorderStroke(1.2.dp, Color(0xFFCBD5E1))
                            else -> BorderStroke(1.8.dp, Color(0xFF64748B))
                        },
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelectedForDelivery || isHighlighted) 6.dp else if (isEven) 2.dp else 4.dp),
                        shape = RoundedCornerShape(if (isLandscape) 7.dp else 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isDeliveryBatchMode) {
                                    if (selectedDeliveryOrderIds.contains(order.id)) {
                                        selectedDeliveryOrderIds.remove(order.id)
                                    } else {
                                        selectedDeliveryOrderIds.add(order.id)
                                    }
                                } else {
                                    viewModel.startEditingOrder(orderWithItems)
                                }
                            }
                            .testTag("order_card_${order.id}")
                    ) {
                        if (isDeliveryBatchMode) {
                            Surface(
                                color = if (isSelectedForDelivery) SeaTealPrimary.copy(alpha = 0.22f) else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedDeliveryOrderIds.contains(order.id)) {
                                            selectedDeliveryOrderIds.remove(order.id)
                                        } else {
                                            selectedDeliveryOrderIds.add(order.id)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isSelectedForDelivery,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    if (!selectedDeliveryOrderIds.contains(order.id)) {
                                                        selectedDeliveryOrderIds.add(order.id)
                                                    }
                                                } else {
                                                    selectedDeliveryOrderIds.remove(order.id)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isSelectedForDelivery) "✓ تم تحديد هذا الأوردر للطباعة" else "اضغط للتحديد للطباعة",
                                            fontSize = if (isLandscape) 11.5.sp else 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelectedForDelivery) SeaTealPrimary else Color(0xFF475569),
                                            style = TextStyle(fontFamily = CairoFontFamily)
                                        )
                                    }
                                    Text(
                                        text = "${ReceiptBitmapRenderer.formatCurrency(order.totalAmount)} ج",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = if (isLandscape) 12.sp else 13.5.sp,
                                        color = SeaNavyDeep,
                                        style = TextStyle(fontFamily = CairoFontFamily)
                                    )
                                }
                            }
                        }
                        if (isHighlighted) {
                            Surface(
                                color = AmberCoral,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = if (isLandscape) 4.dp else 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Stars,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(if (isLandscape) 14.dp else 16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "🎯 الأوردر المنقول حالياً من شاشة البيع (#${order.orderNumber})",
                                            color = Color.White,
                                            fontSize = if (isLandscape) 11.sp else 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.clearHighlightedOrder() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إلغاء التمييز",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(if (isLandscape) 6.dp else 10.dp)) {
                            // =========================================================================
                            // AUTHENTIC THERMAL RECEIPT SLIP (LOOKS EXACTLY LIKE PRINTED PAPER INVOICE)
                            // ALTERNATING LIGHT / DARK BACKGROUND FOR CRYSTAL-CLEAR DISTINCTION
                            // =========================================================================
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isEven) Color(0xFFFFFFFF) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isEven) 1.2.dp else 1.6.dp,
                                    if (isEven) Color(0xFFCBD5E1) else Color(0xFF64748B)
                                ),
                                shadowElevation = if (isEven) 1.dp else 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(if (isLandscape) 8.dp else 12.dp)
                                ) {
                                    // 1. Restaurant Header & Visual Zebra Contrast Indicator
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isEven) Color(0xFFF1F5F9) else Color(0xFF334155),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isEven) Color(0xFFCBD5E1) else Color(0xFF0F172A))
                                            ) {
                                                Text(
                                                    text = if (isEven) "⚪ طلب #${index + 1} (فاتح)" else "🔘 طلب #${index + 1} (داكن)",
                                                    fontSize = if (isLandscape) 8.5.sp else 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isEven) Color(0xFF475569) else Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "🐟 مطعم أسماك صنارة وشبكة",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = if (isLandscape) 12.5.sp else 14.5.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                        Text(
                                            text = "فاتورة حساب ومبيعات",
                                            fontSize = if (isLandscape) 10.sp else 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isEven) Color(0xFF64748B) else Color(0xFF334155)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 🌟 Prominent Seller Identity Strip (اسم البائع يظهر فوق الأوردر مباشرة)
                                    val sellerDisplayName = if (order.sellerName.isNotBlank()) order.sellerName else "كاشير رئيسي"
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF0F172A),
                                        border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = AmberCoral,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "استلام بواسطة:",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = if (isLandscape) 10.sp else 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = AmberCoral.copy(alpha = 0.25f),
                                                    border = BorderStroke(1.dp, AmberCoral)
                                                ) {
                                                    Text(
                                                        text = "👤 $sellerDisplayName",
                                                        color = Color.White,
                                                        fontSize = if (isLandscape) 11.sp else 12.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 2. Inverted Solid Dark Badge (Order Number & Type - Authentic Receipt Banner)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val displayOrderHeader = when {
                                                    order.orderNumber.startsWith("#") -> "أوردر ${order.orderNumber}"
                                                    order.orderNumber.startsWith("معلق") -> "⏳ أوردر (${order.orderNumber})"
                                                    else -> "أوردر #${order.orderNumber}"
                                                }
                                                Text(
                                                    text = displayOrderHeader,
                                                    color = if (order.syncState == "PENDING_OFFLINE") Color(0xFFFDE68A) else Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = if (isLandscape) 14.sp else 16.sp
                                                )
                                                if (order.isArchived) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF475569)
                                                    ) {
                                                        Text(
                                                            text = "📦 مؤرشف",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                if (order.overridePasswordUsed) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = AmberCoral
                                                    ) {
                                                        Text(
                                                            text = "تجاوز إداري",
                                                            color = Color.White,
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Order Type & Compact Status Dropdown Pill & Quick Action
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (order.orderType == "DELIVERY") Color(0xFF0284C7) else Color(0xFF059669)
                                                ) {
                                                    Text(
                                                        text = if (order.orderType == "DELIVERY") "🛵 دليفري" else "🏪 استلام",
                                                        color = Color.White,
                                                        fontSize = if (isLandscape) 9.sp else 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))

                                                // Compact Clickable Status Dropdown Pill
                                                var isCardStatusMenuOpen by remember { mutableStateOf(false) }
                                                Box {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = statusColor.copy(alpha = 0.25f),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor),
                                                        modifier = Modifier.clickable { isCardStatusMenuOpen = true }
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = when (order.status) {
                                                                    "NEW" -> "جديد ▾"
                                                                    "PREPARING" -> "تجهيز ▾"
                                                                    "OUT_FOR_DELIVERY" -> "خرج للتوصيل ▾"
                                                                    "COMPLETED" -> "تم التسليم ✓ ▾"
                                                                    "CANCELLED" -> "ملغي ▾"
                                                                    else -> "${order.status} ▾"
                                                                },
                                                                color = statusColor,
                                                                fontSize = if (isLandscape) 9.sp else 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }

                                                    DropdownMenu(
                                                        expanded = isCardStatusMenuOpen,
                                                        onDismissRequest = { isCardStatusMenuOpen = false }
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("🟡 جديد (في المحل)", fontSize = 12.sp) },
                                                            onClick = {
                                                                viewModel.updateOrderStatus(order.id, "NEW")
                                                                isCardStatusMenuOpen = false
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("🔵 قيد التجهيز (في المحل)", fontSize = 12.sp) },
                                                            onClick = {
                                                                viewModel.updateOrderStatus(order.id, "PREPARING")
                                                                isCardStatusMenuOpen = false
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("🚀 خارج للتوصيل (خرج من المحل)", fontSize = 12.sp) },
                                                            onClick = {
                                                                if (order.driverId != null && order.driverId != 0L) {
                                                                    viewModel.updateOrderStatus(order.id, "OUT_FOR_DELIVERY")
                                                                } else if (order.orderType == "DELIVERY") {
                                                                    orderForDriverAssign = orderWithItems
                                                                } else {
                                                                    viewModel.updateOrderStatus(order.id, "OUT_FOR_DELIVERY")
                                                                }
                                                                isCardStatusMenuOpen = false
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text(if (order.orderType == "DELIVERY") "🟢 تم التسليم بنجاح (خالص)" else "🟢 تم استلام العميل (خزينة ✓)", fontSize = 12.sp) },
                                                            onClick = {
                                                                viewModel.updateOrderStatus(order.id, "COMPLETED")
                                                                isCardStatusMenuOpen = false
                                                            }
                                                        )
                                                    }
                                                }

                                                // Quick One-Tap Next Status Action Button (super compact)
                                                if (isInStore) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (order.orderType == "DELIVERY") SeaTealPrimary else StatusSuccess,
                                                        modifier = Modifier.clickable {
                                                            if (order.orderType == "DELIVERY") {
                                                                if (order.driverId != null && order.driverId != 0L) {
                                                                    viewModel.updateOrderStatus(order.id, "OUT_FOR_DELIVERY")
                                                                } else {
                                                                    orderForDriverAssign = orderWithItems
                                                                }
                                                            } else {
                                                                viewModel.updateOrderStatus(order.id, "COMPLETED")
                                                            }
                                                        }
                                                    ) {
                                                        Text(
                                                            text = if (order.orderType == "DELIVERY") "🚀 خروج" else "🛍️ تسليم",
                                                            color = Color.White,
                                                            fontSize = if (isLandscape) 8.5.sp else 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else if (isOutForDelivery) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = StatusSuccess,
                                                        modifier = Modifier.clickable {
                                                            viewModel.updateOrderStatus(order.id, "COMPLETED")
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "✅ تسليم",
                                                            color = Color.White,
                                                            fontSize = if (isLandscape) 8.5.sp else 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 2b. Previous-Day Unarchived Order Visual Warning
                                    if (!order.isArchived && order.createdAt > 0L && order.createdAt < startOfTodayMs) {
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFFFBEB),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("order_prev_day_badge_${order.id}")
                                        ) {
                                            Text(
                                                text = "📅 أوردر من يوم سابق (${prevDayOnlyFormat.format(Date(order.createdAt))})",
                                                color = Color(0xFF92400E),
                                                fontSize = if (isLandscape) 10.sp else 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // 2c. Offline / Cashier ACK / Print Status Banner (Seller tracking banners shown ONLY on Seller!)
                                    val showSyncBanner = when {
                                        order.syncState == "PENDING_OFFLINE" -> true
                                        deviceRole == "SELLER" && order.syncState in listOf("WAITING_CASHIER_ACK", "PRINT_ERROR", "PRINT_CONFIRMED") -> true
                                        else -> false
                                    }
                                    if (showSyncBanner) {
                                        Spacer(modifier = Modifier.height(5.dp))
                                        val (syncBg, syncBorder, syncTextColor, syncText) = when (order.syncState) {
                                            "PENDING_OFFLINE" -> listOf(
                                                Color(0xFFFFF7ED),
                                                Color(0xFFFB923C),
                                                Color(0xFF9A3412),
                                                "📴 تليفونك (البائع) أوفلاين — معلق بدون رقم رسمي (لو مرّ >30 ثانية يُحفظ بدون طباعة تلقائية) ⏳"
                                            )
                                            "WAITING_CASHIER_ACK" -> listOf(
                                                Color(0xFFFEFCE8),
                                                Color(0xFFEAB308),
                                                Color(0xFF854D0E),
                                                "⏳ تليفونك متصل، لكن جهاز الكاشير الرئيسي لم يستجب الآن — الأوردر معلق لحين اتصال الكاشير"
                                            )
                                            "PRINT_ERROR" -> listOf(
                                                Color(0xFFFEF2F2),
                                                Color(0xFFEF4444),
                                                Color(0xFF991B1B),
                                                "⚠️ تعذر الطباعة على جهاز الكاشير الرئيسي — يرجى مراجعة الطابعات"
                                            )
                                            "PRINT_CONFIRMED" -> listOf(
                                                Color(0xFFECFDF5),
                                                Color(0xFF10B981),
                                                Color(0xFF065F46),
                                                "🖨️ تم الاستلام والطباعة عند الكاشير ✓"
                                            )
                                            else -> listOf(Color.Transparent, Color.Transparent, Color.Transparent, "")
                                        }
                                        val bannerText = syncText as String
                                        if (bannerText.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = syncBg as Color,
                                                border = BorderStroke(1.dp, syncBorder as Color),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("order_sync_state_badge_${order.id}")
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                    Text(
                                                        text = bannerText,
                                                        color = syncTextColor as Color,
                                                        fontSize = if (isLandscape) 10.sp else 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (order.syncState == "PENDING_OFFLINE" && deviceRole == "SELLER") {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        OutlinedButton(
                                                            onClick = { viewModel.printOrderToAllLinkedPrinters(order.id) },
                                                            shape = RoundedCornerShape(6.dp),
                                                            border = BorderStroke(1.dp, Color(0xFFEA580C)),
                                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                            modifier = Modifier
                                                                .height(28.dp)
                                                                .testTag("flush_and_print_offline_order_${order.id}")
                                                        ) {
                                                            Text(
                                                                text = "🔄 إرسال وطباعة الآن",
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color(0xFF9A3412)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 3. Receipt Date & Payment Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "التاريخ والوقت: ${dateFormat.format(Date(order.createdAt))}",
                                            fontSize = if (isLandscape) 9.5.sp else 11.sp,
                                            color = Color(0xFF475569),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "الدفع: ${if (order.paymentMethod == "CASH") "نقداً" else "محفظة إلكترونية"}",
                                            fontSize = if (isLandscape) 9.5.sp else 11.sp,
                                            color = Color(0xFF475569),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // 4. Customer & Delivery Card (Receipt Style)
                                    val showCustName = order.customerName.isNotBlank() && order.customerName != "زبون مباشر"
                                    val showCustPhone = order.customerPhone.isNotBlank()
                                    val showSecPhone = !order.customerSecondaryPhone.isNullOrBlank()
                                    val showAddress = !order.deliveryAddressFull.isNullOrBlank()
                                    val showArea = !order.customerArea.isNullOrBlank() && (order.deliveryAddressFull.isNullOrBlank() || order.deliveryAddressFull?.contains(order.customerArea ?: "") == false)
                                    val showDriver = !order.driverName.isNullOrBlank() || order.orderType == "DELIVERY"
                                    val hasAnyCustInfo = showCustName || showCustPhone || showAddress || showArea || showDriver

                                    if (hasAnyCustInfo) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                if (showCustName) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(text = "👤 العميل: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                                                        Text(text = order.customerName, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                                    }
                                                }
                                                if (showCustPhone || showSecPhone) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(text = "📞 الهاتف: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                                                        val phoneText = buildString {
                                                            if (showCustPhone) append(order.customerPhone)
                                                            if (showSecPhone) append(" | إضافي: ${order.customerSecondaryPhone}")
                                                        }
                                                        Text(text = phoneText, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF0F172A))
                                                    }
                                                }
                                                if (showAddress) {
                                                    Row(verticalAlignment = Alignment.Top) {
                                                        Text(text = "📍 العنوان: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                                                        Text(text = order.deliveryAddressFull!!.trim(), fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                                    }
                                                } else if (showArea) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(text = "📍 المنطقة: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                                                        Text(text = order.customerArea!!.trim(), fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                                    }
                                                }
                                                if (order.orderType == "DELIVERY") {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        if (order.driverName != null) {
                                                            Text(
                                                                text = "🛵 كابتن التوصيل: ${order.driverName} (توصيل: ${String.format(Locale.US, "%.0f", order.deliveryFee)} ج)",
                                                                fontSize = 11.sp,
                                                                color = SeaTealPrimary,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        } else {
                                                            Text(
                                                                text = "⚠️ كابتن التوصيل: لم يتم التحديد بعد",
                                                                fontSize = 11.sp,
                                                                color = AmberCoral,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        if (!isCompleted && !isCancelled) {
                                                            TextButton(
                                                                onClick = { orderForDriverAssign = orderWithItems },
                                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                                                modifier = Modifier.height(24.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (order.driverName != null) "تغيير ▾" else "إسناد +",
                                                                    fontSize = 10.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = SeaTealPrimary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                if (!order.notes.isNullOrBlank()) {
                                                    Row(verticalAlignment = Alignment.Top) {
                                                        Text(text = "📝 ملاحظات: ", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = AmberCoral)
                                                        Text(text = order.notes, fontSize = 10.5.sp, color = Color(0xFF334155))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    ReceiptDashedDivider()
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 5. Thermal Receipt Table Header
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isEven) Color(0xFFE2E8F0) else Color(0xFFCBD5E1),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "الصنف والتجهيز",
                                                modifier = Modifier.weight(1.8f),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "الكمية",
                                                modifier = Modifier.weight(0.9f),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "السعر",
                                                modifier = Modifier.weight(0.9f),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "الإجمالي",
                                                modifier = Modifier.weight(1.0f),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 6. Thermal Receipt Table Items
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        items.forEachIndexed { index, item ->
                                            val unitAndPrep = item.unitPrice + item.preparationPrice
                                            Column {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    // Item name & preparation
                                                    Column(modifier = Modifier.weight(1.8f)) {
                                                        Text(
                                                            text = "• ${item.itemName}",
                                                            fontSize = if (isLandscape) 11.sp else 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                        if (!item.preparationName.isNullOrBlank()) {
                                                            val prepPriceStr = if (item.preparationPrice > 0) {
                                                                val pStr = if (item.preparationPrice % 1.0 == 0.0) item.preparationPrice.toInt().toString() else String.format(Locale.US, "%.1f", item.preparationPrice)
                                                                "(+$pStr)للكيلو"
                                                            } else ""
                                                            Text(
                                                                text = "  ${item.preparationName}$prepPriceStr",
                                                                fontSize = if (isLandscape) 9.5.sp else 10.5.sp,
                                                                color = AmberCoral,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                        if (item.itemNotes.isNotBlank()) {
                                                            Text(
                                                                text = "  ملاحظة: ${item.itemNotes}",
                                                                fontSize = if (isLandscape) 9.sp else 10.sp,
                                                                color = Color(0xFF475569)
                                                            )
                                                        }
                                                    }

                                                    // Quantity
                                                    Text(
                                                        text = "${ReceiptBitmapRenderer.formatQuantity(item.quantity)} ${item.unit}",
                                                        modifier = Modifier.weight(0.9f),
                                                        fontSize = if (isLandscape) 10.5.sp else 11.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        color = Color(0xFF1E293B)
                                                    )

                                                    // Unit Price
                                                    Text(
                                                        text = String.format(Locale.US, "%.1f", unitAndPrep),
                                                        modifier = Modifier.weight(0.9f),
                                                        fontSize = if (isLandscape) 10.5.sp else 11.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        color = Color(0xFF1E293B)
                                                    )

                                                    // Total Item Price
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.1f", item.totalPrice)} ج",
                                                        modifier = Modifier.weight(1.0f),
                                                        fontSize = if (isLandscape) 11.sp else 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                }
                                                if (index < items.size - 1) {
                                                    ReceiptDashedDivider(
                                                        color = Color(0xFFE2E8F0),
                                                        modifier = Modifier.padding(vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    ReceiptDashedDivider()
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 7. Totals & Grand Total Box
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "المجموع الفرعي:", fontSize = 11.5.sp, color = Color(0xFF475569))
                                            Text(text = "${String.format(Locale.US, "%.1f", order.subtotal)} ج.م", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                        }

                                        if (order.deliveryFee > 0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "رسوم التوصيل:", fontSize = 11.5.sp, color = Color(0xFF475569))
                                                Text(text = "+${String.format(Locale.US, "%.0f", order.deliveryFee)} ج.م", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SeaTealPrimary)
                                            }
                                        }

                                        if (order.discount > 0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "خصم خاص:", fontSize = 11.5.sp, color = Color(0xFF475569))
                                                Text(text = "-${String.format(Locale.US, "%.1f", order.discount)} ج.م", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = StatusDanger)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Grand Total Box (Identical to receipt total box)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0F172A)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "الإجمالي المطلوب سداده:",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = if (isLandscape) 12.sp else 13.5.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "${String.format(Locale.US, "%.1f", order.totalAmount)} ج.م",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = if (isLandscape) 15.sp else 17.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Payment Settlement Status Box on Receipt Slip
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (order.isPaid) StatusSuccess.copy(alpha = 0.12f) else if (order.paidAmount > 0) SeaTealPrimary.copy(alpha = 0.1f) else Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (order.isPaid) StatusSuccess else if (order.paidAmount > 0) SeaTealPrimary else Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { orderForPaymentSettlement = orderWithItems }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (order.isPaid) Icons.Default.CheckCircle else Icons.Default.ReceiptLong,
                                                    contentDescription = null,
                                                    tint = if (order.isPaid) StatusSuccess else if (order.paidAmount > 0) SeaTealPrimary else Color(0xFF475569),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Column {
                                                    if (order.isPaid) {
                                                        Text("✓ الفاتورة خالصة بالكامل", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = StatusSuccess)
                                                    } else if (order.paidAmount > 0) {
                                                        Text("المدفوع: ${String.format(Locale.US, "%.1f", order.paidAmount)} ج  |  الباقي: ${String.format(Locale.US, "%.1f", order.changeAmount)} ج", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SeaTealPrimary)
                                                    } else {
                                                        Text("حساب الفاتورة: غير محدد (اضغط لتسجيل المدفوع)", fontSize = 10.5.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "تحديد الحساب 💵",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SeaTealPrimary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    val activeReceiptPrinters = remember(allPrinters, remoteSyncedPrinters, deviceRole) {
                                        val remote = remoteSyncedPrinters.distinctBy { it.id.removePrefix("p_").trim() }
                                        val localEnabled = allPrinters.filter { it.isEnabled }.distinctBy { it.id.removePrefix("p_").trim() }
                                        if (deviceRole == "SELLER" && remote.isNotEmpty()) {
                                            remote
                                        } else if (localEnabled.isNotEmpty()) {
                                            localEnabled
                                        } else {
                                            remote
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Print,
                                                    contentDescription = null,
                                                    tint = SeaTealPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "أزرار الطباعة الفردية:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SeaNavyDeep
                                                )
                                            }
                                            Text(
                                                text = if (deviceRole == "SELLER") "يرسل أمر الطباعة مباشرة للكاشير الرئيسي 🖨️" else "كل زر يطبع لطابعة منفصلة 🖨️",
                                                fontSize = 9.5.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        MouseLazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 1. Fast Print All / Send All Button
                                            item {
                                                Button(
                                                    onClick = {
                                                        viewModel.printOrderToAllLinkedPrinters(order.id)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SeaNavyDeep),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier
                                                        .height(if (isLandscape) 28.dp else 32.dp)
                                                        .testTag("print_all_order_${order.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Print,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "طباعة للكل ⚡",
                                                        fontSize = if (isLandscape) 9.5.sp else 10.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            // 2. Individual printer buttons
                                            if (activeReceiptPrinters.isNotEmpty()) {
                                                itemsIndexed(activeReceiptPrinters, key = { pIdx, p -> "btn_p_${order.id}_${p.id}_$pIdx" }) { _, p ->
                                                    val isPrinted = when (p.role) {
                                                        "KITCHEN_1" -> order.printedKitchen1
                                                        "KITCHEN_2" -> order.printedKitchen2
                                                        else -> order.printedMain
                                                    }
                                                    val isKitchen = p.role.startsWith("KITCHEN")
                                                    val explicitDocType = if (isKitchen) "KITCHEN_ORDER" else if (p.role == "CUSTOMER_INVOICE") "CUSTOMER_INVOICE" else null
                                                    val btnColor = if (isKitchen) AmberCoral else SeaTealPrimary

                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.printReceipt(order.id, p.id, reprintReason = if (isPrinted) "نسخة ثانية" else null, targetDocumentType = explicitDocType)
                                                        },
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, if (isPrinted) StatusSuccess else btnColor),
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            containerColor = if (isPrinted) StatusSuccess.copy(alpha = 0.12f) else btnColor.copy(alpha = 0.08f),
                                                            contentColor = if (isPrinted) StatusSuccess else btnColor
                                                        ),
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                                        modifier = Modifier
                                                            .height(if (isLandscape) 28.dp else 32.dp)
                                                            .testTag("print_printer_${p.id}_order_${order.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isPrinted) Icons.Default.CheckCircle else if (isKitchen) Icons.Default.RestaurantMenu else Icons.Default.Print,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(12.dp),
                                                            tint = if (isPrinted) StatusSuccess else btnColor
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "${p.displayName}${if (isPrinted) " (نسخة أخرى) 🖨️" else ""}",
                                                            fontSize = if (isLandscape) 9.5.sp else 10.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            } else {
                                                // Default individual buttons if no printers configured yet
                                                item {
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.printReceipt(order.id, "MAIN", reprintReason = if (order.printedMain) "نسخة ثانية" else null, targetDocumentType = "CUSTOMER_INVOICE")
                                                        },
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, if (order.printedMain) StatusSuccess else SeaTealPrimary),
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            containerColor = if (order.printedMain) StatusSuccess.copy(alpha = 0.12f) else SeaTealPrimary.copy(alpha = 0.08f),
                                                            contentColor = if (order.printedMain) StatusSuccess else SeaTealPrimary
                                                        ),
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                                        modifier = Modifier
                                                            .height(if (isLandscape) 28.dp else 32.dp)
                                                            .testTag("print_main_order_${order.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = if (order.printedMain) Icons.Default.CheckCircle else Icons.Default.Print,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(12.dp),
                                                            tint = if (order.printedMain) StatusSuccess else SeaTealPrimary
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "طابعة الكاشير${if (order.printedMain) " (نسخة أخرى) 🖨️" else ""}",
                                                            fontSize = if (isLandscape) 9.5.sp else 10.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                item {
                                                    val isKitchenPrinted = order.printedKitchen1 || order.printedKitchen2 || (items.isNotEmpty() && items.all { it.printedInKitchen })
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.printReceipt(order.id, "KITCHEN_1", reprintReason = if (isKitchenPrinted) "نسخة ثانية" else null, targetDocumentType = "KITCHEN_ORDER")
                                                        },
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, if (isKitchenPrinted) StatusSuccess else AmberCoral),
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            containerColor = if (isKitchenPrinted) StatusSuccess.copy(alpha = 0.12f) else AmberCoral.copy(alpha = 0.08f),
                                                            contentColor = if (isKitchenPrinted) StatusSuccess else AmberCoral
                                                        ),
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                                        modifier = Modifier
                                                            .height(if (isLandscape) 28.dp else 32.dp)
                                                            .testTag("print_kitchen_order_${order.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isKitchenPrinted) Icons.Default.CheckCircle else Icons.Default.RestaurantMenu,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(12.dp),
                                                            tint = if (isKitchenPrinted) StatusSuccess else AmberCoral
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "طابعة المطبخ${if (isKitchenPrinted) " (نسخة أخرى) 🖨️" else ""}",
                                                            fontSize = if (isLandscape) 9.5.sp else 10.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                }
                        Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 6.dp))

                            // =========================================================================
                            // COMPACT, TIDY ORDER ACTION BAR (NO CONFUSION BETWEEN ADJACENT ORDERS)
                            // =========================================================================
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // 1. Direct Edit Order in POS Button
                                OutlinedButton(
                                    onClick = { viewModel.startEditingOrder(orderWithItems) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, AmberCoral),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = AmberCoral.copy(alpha = 0.09f),
                                        contentColor = AmberCoral
                                    ),
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .height(if (isLandscape) 30.dp else 34.dp)
                                        .testTag("edit_order_btn_${order.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = AmberCoral)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "تعديل ✏️",
                                        fontSize = if (isLandscape) 10.sp else 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberCoral
                                    )
                                }

                                // 2. Fast Payment Settlement Button
                                OutlinedButton(
                                    onClick = { orderForPaymentSettlement = orderWithItems },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1.1f).height(if (isLandscape) 30.dp else 34.dp)
                                ) {
                                    Icon(imageVector = if (order.isPaid) Icons.Default.CheckCircle else Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (order.isPaid) StatusSuccess else SeaTealPrimary)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (order.isPaid) "خالص ✓" else "الحساب",
                                        fontSize = if (isLandscape) 10.sp else 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (order.isPaid) StatusSuccess else SeaNavyDeep
                                    )
                                }

                                // 3. Invoice Preview Button
                                OutlinedButton(
                                    onClick = { orderForInvoice = orderWithItems },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(0.9f).height(if (isLandscape) 30.dp else 34.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF475569))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("معاينة", fontSize = if (isLandscape) 9.5.sp else 10.5.sp, color = Color(0xFF475569))
                                }

                                // 4. Archive / Unarchive Button
                                if (order.isArchived) {
                                    OutlinedButton(
                                        onClick = { viewModel.unarchiveOrder(order.id) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SeaTealPrimary),
                                        modifier = Modifier.weight(0.85f).height(if (isLandscape) 30.dp else 34.dp)
                                    ) {
                                        Text("استعادة", fontSize = if (isLandscape) 9.5.sp else 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.archiveOrder(order.id) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(0.8f).height(if (isLandscape) 30.dp else 34.dp)
                                    ) {
                                        Text("أرشفة", fontSize = if (isLandscape) 9.5.sp else 10.sp, color = Color(0xFF64748B))
                                    }
                                }

                                // 5. Cancel Button (if active)
                                if (order.status != "CANCELLED" && order.status != "COMPLETED") {
                                    IconButton(
                                        onClick = { orderToCancel = orderWithItems },
                                        modifier = Modifier.size(if (isLandscape) 30.dp else 34.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Cancel, contentDescription = "إلغاء الأوردر", tint = StatusDanger, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Quick Driver Assignment Dialog
    orderForDriverAssign?.let { oi ->
        val currentOrder = orders.firstOrNull { it.order.id == oi.order.id } ?: oi
        var autoDispatch by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { orderForDriverAssign = null },
            title = {
                Column {
                    Text("إسناد موصل دليفري", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "أوردر: ${currentOrder.order.orderNumber} - ${currentOrder.order.customerName}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { autoDispatch = !autoDispatch }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = autoDispatch,
                            onCheckedChange = { autoDispatch = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تسجيل خروج الأوردر للتوصيل فوراً (خرج من المحل)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider()

                    Text("اختر كابتن التوصيل:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    if (drivers.isEmpty()) {
                        Text(
                            text = "لا يوجد موصلين دليفري مسجلين في النظام. يمكنك إضافتهم من شاشة الموظفين والموصلين.",
                            fontSize = 11.sp,
                            color = AmberCoral
                        )
                    } else {
                        MouseLazyColumn(modifier = Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(drivers) { drv ->
                                val isCurrent = currentOrder.order.driverId == drv.id
                                val activeOrdersCount = orders.count {
                                    it.order.driverId == drv.id && it.order.status in listOf("NEW", "PREPARING", "OUT_FOR_DELIVERY")
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) SeaTealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isCurrent) SeaTealPrimary else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (autoDispatch) {
                                                viewModel.dispatchOrderWithDriver(currentOrder.order.id, drv)
                                            } else {
                                                viewModel.assignDriverToOrder(
                                                    orderId = currentOrder.order.id,
                                                    driverId = drv.id,
                                                    driverName = drv.name,
                                                    commission = drv.ratePerOrder,
                                                    reason = "إسناد من قائمة الأوردرات"
                                                )
                                            }
                                            orderForDriverAssign = null
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.TwoWheeler, contentDescription = null, tint = SeaTealPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(drv.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(drv.phone, fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (activeOrdersCount > 0) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = AmberCoral.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "$activeOrdersCount طلبات معه",
                                                        fontSize = 10.sp,
                                                        color = AmberCoral,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }

                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = if (isCurrent) SeaTealPrimary else Color.LightGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { orderForDriverAssign = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Invoice & Print Dialog
    orderForInvoice?.let { oi ->
        val currentOrder = orders.firstOrNull { it.order.id == oi.order.id } ?: oi
        OrderInvoiceAndPrintingDialog(
            orderWithItems = currentOrder,
            onPrintToTarget = { target, reprintReason ->
                val docType = if (target != "MAIN" && (target.contains("KITCHEN") || target == "KITCHEN_1" || target == "KITCHEN_2")) "KITCHEN_ORDER" else "CUSTOMER_INVOICE"
                viewModel.printReceipt(oi.order.id, target, reprintReason, targetDocumentType = docType)
            },
            viewModel = viewModel,
            onDismiss = { orderForInvoice = null }
        )
    }


    // Payment Settlement Dialog
    orderForPaymentSettlement?.let { ops ->
        PaymentSettlementDialog(
            orderWithItems = ops,
            viewModel = viewModel,
            onDismiss = { orderForPaymentSettlement = null }
        )
    }

    // Edit Order Customer Details Dialog
    orderForCustomerEdit?.let { oce ->
        val currentOrder = orders.firstOrNull { it.order.id == oce.order.id } ?: oce
        OrderCustomerEditorDialog(
            orderWithItems = currentOrder,
            viewModel = viewModel,
            onDismiss = { orderForCustomerEdit = null }
        )
    }

    // Edit Order Items (Redirects to POS Screen for seamless, unified editing)
    LaunchedEffect(orderForItemsEdit) {
        orderForItemsEdit?.let { oie ->
            val currentOrder = orders.firstOrNull { it.order.id == oie.order.id } ?: oie
            viewModel.startEditingOrder(currentOrder)
            orderForItemsEdit = null
        }
    }

    // Cancel Order Password Dialog
    orderToCancel?.let { otc ->
        PasswordPromptDialog(
            title = "تأكيد إلغاء الأوردر ${otc.order.orderNumber}",
            message = "إلغاء الأوردر سيعيد الكميات المباعة فوراً إلى المخزون المتاح، ويتطلب إدخال كلمة سر الإدارة لمنع التلاعب.",
            onConfirm = { pass ->
                viewModel.cancelOrder(otc.order.id, pass) { success ->
                    if (success) {
                        orderToCancel = null
                    }
                }
            },
            onDismiss = { orderToCancel = null }
        )
    }

    // Delivery Batch Printer Selection Dialog
    if (showDeliveryPrinterDialog) {
        val enabledPrinters = remember(allPrinters, remoteSyncedPrinters) {
            allPrinters.filter { it.isEnabled }.ifEmpty { remoteSyncedPrinters }
        }
        val toPrint = orders.filter { selectedDeliveryOrderIds.contains(it.order.id) }

        AlertDialog(
            onDismissRequest = { showDeliveryPrinterDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    tint = SeaTealPrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = SeaTealPrimary, modifier = Modifier.size(24.dp))
                    Text(
                        text = "طباعة كشف تسليم الدليفري (${toPrint.size} أوردر)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = SeaNavyDeep,
                        style = TextStyle(fontFamily = CairoFontFamily)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .mouseVerticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Font Size Controller for Delivery Manifest (التحكم في حجم خط كشف التسليم)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = SeaTealPrimary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "حجم الخط في كشف التسليم المطبوع:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SeaNavyDeep,
                                    style = TextStyle(fontFamily = CairoFontFamily)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    "NORMAL" to "عادي (100%)",
                                    "LARGE" to "كبير (120%) ✓",
                                    "HUGE" to "ضخم (145%)",
                                    "GIANT" to "عملاق (170%)"
                                ).forEach { (sz, label) ->
                                    FilterChip(
                                        selected = selectedManifestFontSize == sz,
                                        onClick = {
                                            selectedManifestFontSize = sz
                                            viewModel.updateDeliveryManifestFontSize(sz)
                                        },
                                        label = { Text(label, fontSize = 10.5.sp, fontWeight = if (selectedManifestFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "حدد الطابعة التي ترغب في إرسال كشف التسليم المجمع إليها بصورة عالية الدقة:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        style = TextStyle(fontFamily = CairoFontFamily)
                    )

                    // Select Target Printer
                    if (enabledPrinters.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberCoral.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, AmberCoral.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لا توجد طابعات مخصصة مفعلة في خانة الطابعات. يمكنك الطباعة عبر نظام أندرويد (PDF / طابعة النظام).",
                                fontSize = 11.5.sp,
                                color = AmberCoral,
                                modifier = Modifier.padding(10.dp),
                                style = TextStyle(fontFamily = CairoFontFamily)
                            )
                        }
                    } else {
                        enabledPrinters.forEach { p ->
                            val isSelected = selectedDeliveryPrinterId == p.id
                            Surface(
                                onClick = { selectedDeliveryPrinterId = p.id },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) SeaTealPrimary.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) SeaTealPrimary else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedDeliveryPrinterId = p.id }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = p.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = SeaNavyDeep,
                                                style = TextStyle(fontFamily = CairoFontFamily)
                                            )
                                            if (p.isCashierDefault) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = AmberCoral.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "كاشير افتراضي ⭐",
                                                        fontSize = 9.sp,
                                                        color = AmberCoral,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                        style = TextStyle(fontFamily = CairoFontFamily)
                                                    )
                                                }
                                            }
                                        }
                                        val connDetail = when (p.connectionType) {
                                            "NETWORK_IP" -> "شبكة LAN (${p.ipAddress}:${p.port})"
                                            "BLUETOOTH" -> "بلوتوث (${if (p.bluetoothMacOrName.isNotBlank()) p.bluetoothMacOrName else "جهاز مقترن"})"
                                            "SYSTEM_SPOOLER" -> "نظام أندرويد"
                                            else -> p.connectionType
                                        }
                                        Text(
                                            text = "$connDetail • ${p.paperWidthMm}مم • ${if (p.role == "KITCHEN_ORDER") "مطبخ" else if (p.role == "CUSTOMER_INVOICE") "كاشير" else "شامل"}",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B),
                                            style = TextStyle(fontFamily = CairoFontFamily)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Android System Spooler option
                    val isSpoolerSelected = selectedDeliveryPrinterId == "SYSTEM_SPOOLER_OPT"
                    Surface(
                        onClick = { selectedDeliveryPrinterId = "SYSTEM_SPOOLER_OPT" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSpoolerSelected) SeaTealPrimary.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            if (isSpoolerSelected) 2.dp else 1.dp,
                            if (isSpoolerSelected) SeaTealPrimary else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSpoolerSelected,
                                onClick = { selectedDeliveryPrinterId = "SYSTEM_SPOOLER_OPT" }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🖨️ طباعة عبر نظام أندرويد (PDF / طابعة النظام)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SeaNavyDeep,
                                    style = TextStyle(fontFamily = CairoFontFamily)
                                )
                                Text(
                                    text = "يتيح الحفظ كملف PDF أو الإرسال لأي طابعة متصلة",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    style = TextStyle(fontFamily = CairoFontFamily)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeliveryPrinterDialog = false
                        val targetPrinter = if (selectedDeliveryPrinterId == "SYSTEM_SPOOLER_OPT") {
                            PrinterConfig(
                                id = "SYSTEM_SPOOLER_OPT",
                                displayName = "نظام أندرويد",
                                connectionType = "SYSTEM_SPOOLER",
                                paperWidthMm = 80
                            )
                        } else {
                            enabledPrinters.firstOrNull { it.id == selectedDeliveryPrinterId }
                        }
                        viewModel.printDeliveryBatch(toPrint, targetPrinter)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "طباعة كشف التسليم الآن",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        style = TextStyle(fontFamily = CairoFontFamily)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeliveryPrinterDialog = false }
                ) {
                    Text(
                        text = "إلغاء",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        style = TextStyle(fontFamily = CairoFontFamily)
                    )
                }
            }
        )
    }
    }
}

@Composable
fun PrinterStatusChip(label: String, isPrinted: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isPrinted) StatusSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ${if (isPrinted) "طُبعت ✓" else "لم تُطبع"}",
                fontSize = 10.sp,
                color = if (isPrinted) StatusSuccess else Color.Gray,
                fontWeight = if (isPrinted) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ReceiptDashedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF64748B).copy(alpha = 0.5f),
    dashWidth: androidx.compose.ui.unit.Dp = 4.dp,
    dashGap: androidx.compose.ui.unit.Dp = 3.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 1.dp
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(strokeWidth)
    ) {
        val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
            floatArrayOf(dashWidth.toPx(), dashGap.toPx()),
            0f
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = strokeWidth.toPx(),
            pathEffect = pathEffect
        )
    }
}

