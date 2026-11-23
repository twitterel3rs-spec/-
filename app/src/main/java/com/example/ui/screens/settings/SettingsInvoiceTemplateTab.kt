package com.example.ui.screens.settings

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import com.example.ui.components.MouseLazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printer.InvoiceTemplateConfig
import com.example.printer.KitchenOrderTemplateConfig
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.viewmodel.MainViewModel
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun SettingsInvoiceTemplateTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var activeSubTab by remember { mutableIntStateOf(0) } // 0: فاتورة العميل, 1: أمر تشغيل المطبخ

    // --- Customer Invoice State ---
    var headerTitle by remember { mutableStateOf("صنارة وشبكة للمأكولات البحرية") }
    var subtitle by remember { mutableStateOf("طازج من البحر لبيتك - أسماك وسواحل") }
    var phone1 by remember { mutableStateOf("01001234567") }
    var phone2 by remember { mutableStateOf("01123456789") }
    var address by remember { mutableStateOf("كورنيش السويس - شارع الميناء القديم") }
    var footerNote by remember { mutableStateOf("شكراً لزيارتكم ونتمنى لكم وجبة شهية!") }
    var showPreparations by remember { mutableStateOf(true) }
    var showOrderNotes by remember { mutableStateOf(true) }
    var showDeliveryDetails by remember { mutableStateOf(true) }
    var fontSize by remember { mutableStateOf("NORMAL") }
    var logoPath by remember { mutableStateOf<String?>(null) }
    var reloadLogoKey by remember { mutableStateOf(0) }
    var topPadding by remember { mutableIntStateOf(20) }
    var headerLineSpacing by remember { mutableIntStateOf(12) }
    var headerTitleFontSize by remember { mutableIntStateOf(30) }
    var headerDetailsFontSize by remember { mutableIntStateOf(18) }
    var deliveryManifestFontSize by remember { mutableStateOf("LARGE") }
    // Customer invoice paper-saving & granular toggles
    var invCompactHeader by remember { mutableStateOf(true) }
    var invShowLogo by remember { mutableStateOf(true) }
    var invShowHeaderTitle by remember { mutableStateOf(true) }
    var invShowSubtitle by remember { mutableStateOf(true) }
    var invShowAddress by remember { mutableStateOf(true) }
    var invShowPhones by remember { mutableStateOf(true) }
    var invShowOrderBadge by remember { mutableStateOf(true) }
    var invShowDateTime by remember { mutableStateOf(true) }
    var invShowSellerName by remember { mutableStateOf(false) }
    var invShowCustName by remember { mutableStateOf(true) }
    var invShowCustPhone by remember { mutableStateOf(true) }
    var invShowDeliveryAddr by remember { mutableStateOf(true) }
    var invShowDriverName by remember { mutableStateOf(true) }
    var invShowSubtotal by remember { mutableStateOf(true) }
    var invShowDeliveryFee by remember { mutableStateOf(true) }
    var invShowPaymentStatusBox by remember { mutableStateOf(true) }
    var invShowPaymentBreakdown by remember { mutableStateOf(true) }
    var invShowFooterNote by remember { mutableStateOf(true) }
    var invMetaFontSize by remember { mutableIntStateOf(16) }
    var invTableHeaderFontSize by remember { mutableIntStateOf(18) }
    var invItemFontSize by remember { mutableIntStateOf(22) }
    var invPrepFontSize by remember { mutableIntStateOf(18) }
    var invNotesFontSize by remember { mutableIntStateOf(15) }
    var invTotalBoxFontSize by remember { mutableIntStateOf(24) }

    // --- Kitchen Order (أمر التشغيل) State ---
    var kitchenHeaderTitle by remember { mutableStateOf("أمر تشغيل المطبخ والاستاف") }
    var kitchenDepartment by remember { mutableStateOf("") }
    var kitchenShowHeaderTitle by remember { mutableStateOf(true) }
    var kitchenShowDept by remember { mutableStateOf(true) }
    var kitchenFontSize by remember { mutableStateOf("LARGE") }
    var kitchenShowCustName by remember { mutableStateOf(true) }
    var kitchenShowCustPhone by remember { mutableStateOf(true) }
    var kitchenShowDeliveryAddr by remember { mutableStateOf(true) }
    var kitchenShowSellerName by remember { mutableStateOf(false) }
    var kitchenShowOrderTime by remember { mutableStateOf(true) }
    var kitchenCompactHeader by remember { mutableStateOf(true) }
    var kitchenShowTableSectionTitle by remember { mutableStateOf(false) }
    var kitchenShowPreps by remember { mutableStateOf(true) }
    var kitchenShowNotes by remember { mutableStateOf(true) }
    var kitchenShowPrices by remember { mutableStateOf(false) }
    var kitchenShowOrderTypeAndTable by remember { mutableStateOf(true) }
    var kitchenShowCashierAndTime by remember { mutableStateOf(true) }
    var kitchenShowItemCountSummary by remember { mutableStateOf(true) }
    var kitchenHighlightItems by remember { mutableStateOf(true) }
    var kitchenShowFooterNote by remember { mutableStateOf(false) }
    var kitchenFooterNote by remember { mutableStateOf("برجاء سرعة التجهيز والالتزام بملاحظات التحضير") }
    var kitchenTopPadding by remember { mutableIntStateOf(10) }
    var kitchenLineSpacing by remember { mutableIntStateOf(6) }
    var kitchenTableItemTitle by remember { mutableStateOf("الصنف والتجهيز") }
    var kitchenQtyWidthPct by remember { mutableIntStateOf(28) }
    var kitchenHeaderFontSize by remember { mutableIntStateOf(24) }
    var kitchenMetaFontSize by remember { mutableIntStateOf(16) }
    var kitchenTableHeaderFontSize by remember { mutableIntStateOf(18) }
    var kitchenItemFontSize by remember { mutableIntStateOf(26) }
    var kitchenPrepFontSize by remember { mutableIntStateOf(20) }
    var kitchenNotesFontSize by remember { mutableIntStateOf(17) }
    var kitchenQtyFontSize by remember { mutableIntStateOf(28) }

    LaunchedEffect(reloadLogoKey) {
        val currentInvoice = viewModel.getInvoiceTemplateConfig()
        headerTitle = currentInvoice.headerTitle
        subtitle = currentInvoice.subtitle
        phone1 = currentInvoice.phone1
        phone2 = currentInvoice.phone2
        address = currentInvoice.address
        footerNote = currentInvoice.footerNote
        showPreparations = currentInvoice.showPreparations
        showOrderNotes = currentInvoice.showOrderNotes
        showDeliveryDetails = currentInvoice.showDeliveryDetails
        fontSize = currentInvoice.fontSize
        logoPath = currentInvoice.logoPath
        topPadding = currentInvoice.topPadding
        headerLineSpacing = currentInvoice.headerLineSpacing
        headerTitleFontSize = currentInvoice.headerTitleFontSize
        headerDetailsFontSize = currentInvoice.headerDetailsFontSize
        deliveryManifestFontSize = currentInvoice.deliveryManifestFontSize
        invCompactHeader = currentInvoice.compactHeaderMode
        invShowLogo = currentInvoice.showLogo
        invShowHeaderTitle = currentInvoice.showHeaderTitle
        invShowSubtitle = currentInvoice.showSubtitle
        invShowAddress = currentInvoice.showAddress
        invShowPhones = currentInvoice.showPhones
        invShowOrderBadge = currentInvoice.showOrderBadge
        invShowDateTime = currentInvoice.showDateTime
        invShowSellerName = currentInvoice.showSellerName
        invShowCustName = currentInvoice.showCustomerName
        invShowCustPhone = currentInvoice.showCustomerPhone
        invShowDeliveryAddr = currentInvoice.showDeliveryAddress
        invShowDriverName = currentInvoice.showDriverName
        invShowSubtotal = currentInvoice.showSubtotal
        invShowDeliveryFee = currentInvoice.showDeliveryFee
        invShowPaymentStatusBox = currentInvoice.showPaymentStatusBox
        invShowPaymentBreakdown = currentInvoice.showPaymentBreakdown
        invShowFooterNote = currentInvoice.showFooterNote
        invMetaFontSize = currentInvoice.metadataFontSizePx
        invTableHeaderFontSize = currentInvoice.tableHeaderFontSizePx
        invItemFontSize = currentInvoice.itemFontSizePx
        invPrepFontSize = currentInvoice.itemPrepFontSizePx
        invNotesFontSize = currentInvoice.itemNotesFontSizePx
        invTotalBoxFontSize = currentInvoice.totalBoxFontSizePx

        val currentKitchen = viewModel.getKitchenOrderTemplateConfig()
        kitchenHeaderTitle = currentKitchen.headerTitle
        kitchenDepartment = currentKitchen.kitchenDepartment
        kitchenShowHeaderTitle = currentKitchen.showHeaderTitle
        kitchenShowDept = currentKitchen.showDepartment
        kitchenFontSize = currentKitchen.fontSize
        kitchenShowCustName = currentKitchen.showCustomerName
        kitchenShowCustPhone = currentKitchen.showCustomerPhone
        kitchenShowDeliveryAddr = currentKitchen.showDeliveryAddress
        kitchenShowSellerName = currentKitchen.showSellerName
        kitchenShowOrderTime = currentKitchen.showOrderTime
        kitchenCompactHeader = currentKitchen.compactHeaderMode
        kitchenShowTableSectionTitle = currentKitchen.showTableSectionTitle
        kitchenShowPreps = currentKitchen.showPreparations
        kitchenShowNotes = currentKitchen.showOrderNotes
        kitchenShowPrices = currentKitchen.showItemPrices
        kitchenShowOrderTypeAndTable = currentKitchen.showOrderTypeAndTable
        kitchenShowCashierAndTime = currentKitchen.showCashierAndTime
        kitchenShowItemCountSummary = currentKitchen.showItemCountSummary
        kitchenHighlightItems = currentKitchen.highlightItemNames
        kitchenShowFooterNote = currentKitchen.showFooterNote
        kitchenFooterNote = currentKitchen.footerNote
        kitchenTopPadding = currentKitchen.topPadding
        kitchenLineSpacing = currentKitchen.headerLineSpacing
        kitchenTableItemTitle = currentKitchen.tableHeaderItemColumnTitle
        kitchenQtyWidthPct = currentKitchen.qtyColumnWidthPercent
        kitchenHeaderFontSize = currentKitchen.headerFontSizePx
        kitchenMetaFontSize = currentKitchen.metadataFontSizePx
        kitchenTableHeaderFontSize = currentKitchen.tableHeaderFontSizePx
        kitchenItemFontSize = currentKitchen.itemFontSizePx
        kitchenPrepFontSize = currentKitchen.itemPrepFontSizePx
        kitchenNotesFontSize = currentKitchen.itemNotesFontSizePx
        kitchenQtyFontSize = currentKitchen.qtyFontSizePx
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.saveRestaurantLogo(uri)
            reloadLogoKey++
        }
    }

    val logoBitmap = remember(logoPath, reloadLogoKey) {
        logoPath?.let { path ->
            try {
                val f = File(path)
                if (f.exists()) BitmapFactory.decodeFile(path) else null
            } catch (e: Exception) {
                null
            }
        }
    }

    MouseLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Main Tab Selection between Customer Invoice and Kitchen Order Ticket
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "تصميم وفورمات الطباعة ونقطة البيع:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = SeaNavyDeep,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "اختر الفورمة التي تريد تخصيصها وتعديل جدولها ومظهرها:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    TabRow(
                        selectedTabIndex = activeSubTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = activeSubTab == 0,
                            onClick = { activeSubTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("فاتورة العميل 🧾", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        )
                        Tab(
                            selected = activeSubTab == 1,
                            onClick = { activeSubTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("أمر تشغيل المطبخ 🍳", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        )
                    }
                }
            }
        }

        // =================================================================
        // SUB-TAB 0: CUSTOMER INVOICE
        // =================================================================
        if (activeSubTab == 0) {
            // Restaurant Logo Upload Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "🖼️ لوجو وشعار المطعم المطبوع في أعلى الفاتورة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = SeaNavyDeep
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (logoBitmap != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Image(
                                        bitmap = logoBitmap.asImageBitmap(),
                                        contentDescription = "لوجو المطعم",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().padding(4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (logoBitmap != null) "تغيير اللوجو" else "من الجهاز", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.setDefaultRestaurantLogo()
                                            reloadLogoKey++
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SeaNavyDeep),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("🐟 لوجو صنارة وشبكة", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (logoBitmap != null) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.removeRestaurantLogo()
                                            reloadLogoKey++
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.fillMaxWidth().height(34.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إزالة اللوجو", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Live Thermal Customer Receipt Preview (Updates INSTANTANEOUSLY)
            item {
                val previewScale = when (fontSize) {
                    "HUGE" -> 1.55f
                    "LARGE" -> 1.30f
                    else -> 1.0f
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "--- معاينة حية فورية لفاتورة العميل المطبوعة ---", fontSize = 10.5.sp, color = Color.Gray)
                            if (invCompactHeader) {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF2E7D32))
                                ) {
                                    Text(
                                        text = "🌿 توفير الورق: الترويسة مضغوطة",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height((if (invCompactHeader) (topPadding / 4).coerceAtMost(3) else (topPadding / 2)).dp))

                        // Logo
                        if (invShowLogo && logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .height(if (invCompactHeader) 34.dp else 45.dp)
                                    .padding(bottom = (headerLineSpacing / 3).dp)
                            )
                        }

                        var anyHeaderShown = false

                        // Header Title
                        if (invShowHeaderTitle && headerTitle.isNotBlank()) {
                            Text(
                                text = headerTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = (headerTitleFontSize * 0.52f * previewScale).sp,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height((headerLineSpacing / (if (invCompactHeader) 3.5f else 2.5f)).dp))
                            anyHeaderShown = true
                        }

                        // Subtitle
                        if (invShowSubtitle && subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                fontSize = (headerDetailsFontSize * 0.58f * previewScale).sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height((headerLineSpacing / (if (invCompactHeader) 3.5f else 2.5f)).dp))
                            anyHeaderShown = true
                        }

                        // Address
                        if (invShowAddress && address.isNotBlank()) {
                            Text(
                                text = address,
                                fontSize = (headerDetailsFontSize * 0.58f * previewScale).sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height((headerLineSpacing / (if (invCompactHeader) 3.5f else 2.5f)).dp))
                            anyHeaderShown = true
                        }

                        // Phones
                        if (invShowPhones && phone1.isNotBlank()) {
                            val phones = phone1 + if (phone2.isNotBlank()) " - $phone2" else ""
                            Text(
                                text = "هاتف: $phones",
                                fontSize = (headerDetailsFontSize * 0.58f * previewScale).sp,
                                color = Color.Black
                            )
                            anyHeaderShown = true
                        }

                        if (anyHeaderShown || (invShowLogo && logoBitmap != null)) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = (if (invCompactHeader) 3.dp else 6.dp)),
                                color = Color.Gray
                            )
                        }

                        // Order Badge & Metadata (COMPACT vs STANDARD)
                        if (invCompactHeader) {
                            // ULTRA-COMPACT HEADER
                            if (invShowOrderBadge) {
                                Surface(
                                    color = Color.Black,
                                    shape = RoundedCornerShape(3.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "أوردر: #1",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (13f * previewScale).sp,
                                            color = Color.White
                                        )
                                        val timeStr = if (invShowDateTime) " | 02:30 م" else ""
                                        Text(
                                            text = "دليفري$timeStr",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (11f * previewScale).sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else if (invShowDateTime) {
                                Text(
                                    text = "التاريخ والوقت: 2026-10-18 02:30 م",
                                    fontSize = (invMetaFontSize * 0.6f * previewScale).sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                            }

                            if (invShowSellerName) {
                                Text(
                                    text = "(بائع: كاشير 1)",
                                    fontSize = ((invMetaFontSize - 2) * 0.58f * previewScale).sp,
                                    color = Color.Gray,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Left
                                )
                            }

                            // Compact Customer Info
                            if (showDeliveryDetails) {
                                val custParts = mutableListOf<String>()
                                if (invShowCustName) custParts.add("العميل: أحمد السيد")
                                if (invShowCustPhone) custParts.add("هـ: 01012345678")
                                if (custParts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = custParts.joinToString(" • "),
                                        fontSize = (invMetaFontSize * 0.62f * previewScale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )
                                }

                                val addrParts = mutableListOf<String>()
                                if (invShowDeliveryAddr) addrParts.add("العنوان: حي الأربعين - شارع الجيش")
                                if (invShowDriverName) addrParts.add("طيار: كابتن محمد")
                                if (addrParts.isNotEmpty()) {
                                    Text(
                                        text = addrParts.joinToString(" • "),
                                        fontSize = ((invMetaFontSize - 1) * 0.60f * previewScale).sp,
                                        color = Color.DarkGray,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp), color = Color.Gray)
                        } else {
                            // STANDARD SPACIOUS HEADER
                            if (invShowOrderBadge) {
                                Surface(
                                    color = Color.Black,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "أوردر رقم: #1", fontWeight = FontWeight.Bold, fontSize = (14f * previewScale).sp, color = Color.White)
                                        Text(text = "دليفري توصيل", fontWeight = FontWeight.Bold, fontSize = (12f * previewScale).sp, color = Color.White)
                                    }
                                }
                            }

                            if (invShowDateTime || invShowSellerName) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (invShowDateTime) {
                                        Text(text = "التاريخ: 2026-10-18 02:30 م", fontSize = (invMetaFontSize * 0.6f * previewScale).sp, color = Color.DarkGray)
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }
                                    if (invShowSellerName) {
                                        Text(text = "(بائع: كاشير 1)", fontSize = ((invMetaFontSize - 2) * 0.6f * previewScale).sp, color = Color.Gray)
                                    }
                                }
                            }

                            if (showDeliveryDetails && (invShowCustName || invShowCustPhone || invShowDeliveryAddr || invShowDriverName)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        if (invShowCustName || invShowCustPhone) {
                                            val namePart = if (invShowCustName) "العميل: أحمد السيد" else ""
                                            val phonePart = if (invShowCustPhone) " | هاتف: 01012345678" else ""
                                            Text(
                                                text = "$namePart$phonePart".removePrefix(" | "),
                                                fontSize = (invMetaFontSize * 0.65f * previewScale).sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                        if (invShowDeliveryAddr) {
                                            Text(
                                                text = "العنوان: حي الأربعين - شارع الجيش - عمارة 5 شقة 3",
                                                fontSize = (invMetaFontSize * 0.60f * previewScale).sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.Black
                                            )
                                        }
                                        if (invShowDriverName) {
                                            Text(
                                                text = "كابتن التوصيل: كابتن محمد طيار",
                                                fontSize = (invMetaFontSize * 0.58f * previewScale).sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Structured Table Header Preview
                        Surface(
                            color = Color(0xFFEEEEEE),
                            shape = RoundedCornerShape(2.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = (if (invCompactHeader) 2.5f else 4f).dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "الصنف والتجهيز", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold, fontSize = (invTableHeaderFontSize * 0.58f * previewScale).sp, color = Color.Black)
                                Text(text = "الكمية", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = (invTableHeaderFontSize * 0.58f * previewScale).sp, textAlign = TextAlign.Center, color = Color.Black)
                                Text(text = "السعر", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = (invTableHeaderFontSize * 0.58f * previewScale).sp, textAlign = TextAlign.Center, color = Color.Black)
                                Text(text = "الإجمالي", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, fontSize = (invTableHeaderFontSize * 0.58f * previewScale).sp, textAlign = TextAlign.End, color = Color.Black)
                            }
                        }

                        // Table Row Sample
                        Surface(
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFCCCCCC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = (if (invCompactHeader) 2.dp else 4.dp))) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "• سمك بوري دمياطي", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold, fontSize = (invItemFontSize * 0.55f * previewScale).sp, color = Color.Black)
                                    Text(text = "1.5 كجم", modifier = Modifier.weight(1.2f), fontSize = (invItemFontSize * 0.50f * previewScale).sp, textAlign = TextAlign.Center, color = Color.Black)
                                    Text(text = "185.0", modifier = Modifier.weight(1.2f), fontSize = (invItemFontSize * 0.50f * previewScale).sp, textAlign = TextAlign.Center, color = Color.Black)
                                    Text(text = "277.5 ج", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, fontSize = (invItemFontSize * 0.55f * previewScale).sp, textAlign = TextAlign.End, color = Color.Black)
                                }
                                if (showPreparations) {
                                    Text(text = "  - سنجاري بالخلطة (+20)للكيلو", fontSize = (invPrepFontSize * 0.52f * previewScale).sp, color = Color(0xFFB03A2E), fontWeight = FontWeight.SemiBold)
                                }
                                if (showOrderNotes) {
                                    Text(text = "  - ملاحظة: ليمون زيادة وسلطات حارة", fontSize = (invNotesFontSize * 0.52f * previewScale).sp, color = Color.Gray)
                                }
                            }
                        }

                        // Summary & Totals
                        if (invShowSubtotal || invShowDeliveryFee) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = (if (invCompactHeader) 2.dp else 4.dp)), color = Color.Gray)
                        }

                        if (invShowSubtotal) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "المجموع الفرعي:", fontSize = (10.5f * previewScale).sp, color = Color.Black)
                                Text(text = "277.5 ج.م", fontSize = (10.5f * previewScale).sp, color = Color.Black)
                            }
                        }

                        if (invShowDeliveryFee) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "رسوم التوصيل:", fontSize = (10.5f * previewScale).sp, color = Color.Black)
                                Text(text = "+20.0 ج.م", fontSize = (10.5f * previewScale).sp, color = Color.Black)
                            }
                        }

                        // Grand Total Box
                        if (invShowPaymentStatusBox) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color.Black,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = (if (invCompactHeader) 4.dp else 6.dp)),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "★ الأوردر خالص بالكامل ★",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = ((invTotalBoxFontSize - 4) * 0.55f * previewScale).sp,
                                        color = Color.White
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 3.dp),
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "المدفوع (خالص ✓):", fontWeight = FontWeight.Bold, fontSize = (invTotalBoxFontSize * 0.55f * previewScale).sp, color = Color.White)
                                        Text(text = "297.5 ج.م", fontWeight = FontWeight.Bold, fontSize = ((invTotalBoxFontSize + 3) * 0.55f * previewScale).sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Payment Details Breakdown
                        if (invShowPaymentBreakdown) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "طريقة الدفع: نقداً (كاش) | المسدد: 300.0 ج.م | الباقي: 2.5 ج.م",
                                fontSize = (9.5f * previewScale).sp,
                                color = Color.DarkGray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                        }

                        // Footer Note
                        if (invShowFooterNote && footerNote.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Gray)
                            Text(text = footerNote, fontSize = (9.5f * previewScale).sp, color = Color.DarkGray, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Paper-saving Mode Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (invCompactHeader) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = if (invCompactHeader) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784)) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🌿 وضع الترويسة المضغوطة لتوفير الورق (Paper-Saving Mode):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (invCompactHeader) Color(0xFF1B5E20) else SeaNavyDeep
                                )
                                Text(
                                    text = "يدمج ويوفر المساحات الفارغة الرأسية، يقلص ارتفاع البانرات والهوامش، ويدمج تفاصيل العميل والهاتف والأوردر بدون إهدار سنتيمترات من بكرة الورق الحراري.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = invCompactHeader,
                                onCheckedChange = { invCompactHeader = it }
                            )
                        }
                    }
                }
            }

            // Granular Show/Hide Toggles Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "👁️ التحكم في إظهار وإخفاء تفاصيل الفاتورة بدقة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = SeaNavyDeep
                        )
                        Text(
                            text = "حدد بالضبط ما يظهر وما يُخفى في الفاتورة المطبوعة للزبون، لتفصيل الفاتورة كما يناسب عملك وتقليل الهدر:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(text = "🔹 ترويسة وهوية المطعم:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SeaTealPrimary)

                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowLogo = !invShowLogo }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowLogo, onCheckedChange = { invShowLogo = it })
                            Text("إظهار لوجو وشعار المطعم المطبوع", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowHeaderTitle = !invShowHeaderTitle }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowHeaderTitle, onCheckedChange = { invShowHeaderTitle = it })
                            Text("إظهار اسم المطعم الرئيسي (عنوان الترويسة)", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowSubtitle = !invShowSubtitle }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowSubtitle, onCheckedChange = { invShowSubtitle = it })
                            Text("إظهار الشعار والوصف الفرعي", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowAddress = !invShowAddress }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowAddress, onCheckedChange = { invShowAddress = it })
                            Text("إظهار عنوان فرع المطعم المطبوع", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowPhones = !invShowPhones }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowPhones, onCheckedChange = { invShowPhones = it })
                            Text("إظهار أرقام هواتف الدليفري والفرع", fontSize = 12.sp)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(text = "🔹 بيانات الأوردر والتوقيت والبائع:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SeaTealPrimary)

                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowOrderBadge = !invShowOrderBadge }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowOrderBadge, onCheckedChange = { invShowOrderBadge = it })
                            Text("إظهار شريط رقم الأوردر ونوعه (دليفري / استلام)", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowDateTime = !invShowDateTime }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowDateTime, onCheckedChange = { invShowDateTime = it })
                            Text("إظهار تاريخ ووقت إصدار الفاتورة", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowSellerName = !invShowSellerName }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowSellerName, onCheckedChange = { invShowSellerName = it })
                            Text("إظهار اسم البائع / الكاشير (معطل افتراضياً لتوفير المساحة)", fontSize = 12.sp)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(text = "🔹 بيانات العميل والتوصيل:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SeaTealPrimary)

                        Row(modifier = Modifier.fillMaxWidth().clickable { showDeliveryDetails = !showDeliveryDetails }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showDeliveryDetails, onCheckedChange = { showDeliveryDetails = it })
                            Text("تفعيل قسم بيانات العميل والتوصيل (المفتاح العام)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (showDeliveryDetails) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp).clickable { invShowCustName = !invShowCustName }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = invShowCustName, onCheckedChange = { invShowCustName = it })
                                Text("إظهار اسم العميل", fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp).clickable { invShowCustPhone = !invShowCustPhone }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = invShowCustPhone, onCheckedChange = { invShowCustPhone = it })
                                Text("إظهار رقم هاتف العميل والهاتف الإضافي", fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp).clickable { invShowDeliveryAddr = !invShowDeliveryAddr }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = invShowDeliveryAddr, onCheckedChange = { invShowDeliveryAddr = it })
                                Text("إظهار عنوان توصيل العميل بالتفصيل", fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp).clickable { invShowDriverName = !invShowDriverName }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = invShowDriverName, onCheckedChange = { invShowDriverName = it })
                                Text("إظهار اسم كابتن التوصيل (الطيار)", fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(text = "🔹 تفاصيل الأصناف والطهي:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SeaTealPrimary)

                        Row(modifier = Modifier.fillMaxWidth().clickable { showPreparations = !showPreparations }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showPreparations, onCheckedChange = { showPreparations = it })
                            Text("إظهار طرق التجهيز والطهي (شوي/قلي/سنجاري/طواجن)", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clickable { showOrderNotes = !showOrderNotes }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showOrderNotes, onCheckedChange = { showOrderNotes = it })
                            Text("إظهار ملاحظات الأصناف والملاحظات العامة", fontSize = 12.sp)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(text = "🔹 الحسابات وطريقة الدفع والختام:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SeaTealPrimary)

                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowSubtotal = !invShowSubtotal }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowSubtotal, onCheckedChange = { invShowSubtotal = it })
                            Text("إظهار سطر المجموع الفرعي", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowDeliveryFee = !invShowDeliveryFee }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowDeliveryFee, onCheckedChange = { invShowDeliveryFee = it })
                            Text("إظهار رسوم التوصيل والخصومات", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowPaymentStatusBox = !invShowPaymentStatusBox }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowPaymentStatusBox, onCheckedChange = { invShowPaymentStatusBox = it })
                            Text("إظهار صندوق حالة الدفع والإجمالي البارز (خالص / متبقي / غير مدفوع)", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowPaymentBreakdown = !invShowPaymentBreakdown }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowPaymentBreakdown, onCheckedChange = { invShowPaymentBreakdown = it })
                            Text("إظهار تفاصيل الدفع (كاش/محفظة، المسدد، الباقي المستحق)", fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clickable { invShowFooterNote = !invShowFooterNote }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = invShowFooterNote, onCheckedChange = { invShowFooterNote = it })
                            Text("إظهار جملة الشكر والختام بأسفل الفاتورة", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Granular Font Size Controls Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔤 التحكم الدقيق في أحجام خطوط فاتورة العميل:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = SeaNavyDeep
                        )
                        Text(
                            text = "تعديل كل حجم خط يظهر فوراً في المعاينة أعلاه، مما يتيح لك رؤية النتيجة لحظياً قبل الحفظ والطباعة:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط اسم المطعم الرئيسي المطبوع:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(26 to "26 (وسط)", 30 to "30 (قياسي) ✓", 34 to "34 (بارز)", 38 to "38 (عريض)").forEach { (sz, label) ->
                                FilterChip(
                                    selected = headerTitleFontSize == sz,
                                    onClick = { headerTitleFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (headerTitleFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط بيانات الترويسة (العنوان والهواتف والوصف):", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(16 to "16 (ناعم)", 18 to "18 (قياسي) ✓", 20 to "20 (واضح)", 22 to "22 (كبير)").forEach { (sz, label) ->
                                FilterChip(
                                    selected = headerDetailsFontSize == sz,
                                    onClick = { headerDetailsFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (headerDetailsFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط بيانات العميل والتاريخ والبائع:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(13 to "13 (مضغوط)", 16 to "16 (قياسي) ✓", 19 to "19 (واضح)", 22 to "22 (كبير)").forEach { (sz, label) ->
                                FilterChip(
                                    selected = invMetaFontSize == sz,
                                    onClick = { invMetaFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (invMetaFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط رأس جدول الأصناف (الإجمالي، السعر، الكمية، الصنف):", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15 to "15 (ناعم)", 18 to "18 (قياسي) ✓", 21 to "21 (بارز)").forEach { (sz, label) ->
                                FilterChip(
                                    selected = invTableHeaderFontSize == sz,
                                    onClick = { invTableHeaderFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (invTableHeaderFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط اسم الصنف وسعره بالجدول:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(18 to "18 (هادئ)", 22 to "22 (قياسي) ✓", 26 to "26 (بارز)", 30 to "30 (عريض)").forEach { (sz, label) ->
                                FilterChip(
                                    selected = invItemFontSize == sz,
                                    onClick = { invItemFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (invItemFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط طريقة الطهي والتجهيز (شوي/سنجاري):", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(14 to "14 (ناعم)", 18 to "18 (قياسي) ✓", 22 to "22 (بارز)").forEach { (sz, label) ->
                                FilterChip(
                                    selected = invPrepFontSize == sz,
                                    onClick = { invPrepFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (invPrepFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط ملاحظات الأصناف والملاحظات العامة:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(13 to "13 (مضغوط)", 15 to "15 (قياسي) ✓", 18 to "18 (واضح)").forEach { (sz, label) ->
                                FilterChip(
                                    selected = invNotesFontSize == sz,
                                    onClick = { invNotesFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (invNotesFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط صندوق الإجمالي النهائي وحالة التحصيل:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(20 to "20 (متوسط)", 24 to "24 (قياسي) ✓", 28 to "28 (كبير)", 32 to "32 (عملاق)").forEach { (sz, label) ->
                                FilterChip(
                                    selected = invTotalBoxFontSize == sz,
                                    onClick = { invTotalBoxFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (invTotalBoxFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "نسبة التكبير العامة للفاتورة المطبوعة (Scale):", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("NORMAL" to "عادي (100%)", "LARGE" to "كبير (130%)", "HUGE" to "ضخم جداً (155%)").forEach { (sz, label) ->
                                FilterChip(
                                    selected = fontSize == sz,
                                    onClick = { fontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (fontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم الخط في كشف تسليم الدليفري (كشف الطيار المجمع):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "NORMAL" to "عادي (100%)",
                                "LARGE" to "كبير (120%) ✓",
                                "HUGE" to "ضخم (145%)",
                                "GIANT" to "عملاق (170%)"
                            ).forEach { (sz, label) ->
                                FilterChip(
                                    selected = deliveryManifestFontSize == sz,
                                    onClick = { deliveryManifestFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (deliveryManifestFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }
            }

            // Customer Receipt Margins, Line Spacing and Font Size Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📐 نصوص الترويسة وضبط الهوامش والتباعدات:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = SeaNavyDeep
                        )

                        OutlinedTextField(
                            value = headerTitle,
                            onValueChange = { headerTitle = it },
                            label = { Text("عنوان الفاتورة الرئيسي (اسم المطعم)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = subtitle,
                            onValueChange = { subtitle = it },
                            label = { Text("الوصف أو الشعار الفرعي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = phone1,
                                onValueChange = { phone1 = it },
                                label = { Text("رقم الهاتف 1") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = phone2,
                                onValueChange = { phone2 = it },
                                label = { Text("رقم الهاتف 2") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("عنوان المحل / الفرع المطبوع") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = footerNote,
                            onValueChange = { footerNote = it },
                            label = { Text("جملة الختام أسفل الفاتورة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "هامش أعلى الفاتورة (نزول النص من حافة الورقة):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                10 to "قصير (10px)",
                                20 to "متوازن (20px) ✓",
                                30 to "مريح (30px)",
                                40 to "فسيح (40px)"
                            ).forEach { (paddingVal, label) ->
                                FilterChip(
                                    selected = topPadding == paddingVal,
                                    onClick = { topPadding = paddingVal },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (topPadding == paddingVal) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "المسافة بين سطور الترويسة (حل تداخل الأسطر):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                8 to "متقارب (8px)",
                                12 to "قياسي (12px) ✓",
                                16 to "مريح (16px)",
                                20 to "متباعد (20px)"
                            ).forEach { (spacingVal, label) ->
                                FilterChip(
                                    selected = headerLineSpacing == spacingVal,
                                    onClick = { headerLineSpacing = spacingVal },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (headerLineSpacing == spacingVal) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط اسم المحل الرئيسي المطبوع:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                26 to "26 (وسط)",
                                30 to "30 (قياسي) ✓",
                                34 to "34 (بارز)",
                                38 to "38 (عريض)"
                            ).forEach { (szVal, label) ->
                                FilterChip(
                                    selected = headerTitleFontSize == szVal,
                                    onClick = { headerTitleFontSize = szVal },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (headerTitleFontSize == szVal) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط بيانات الترويسة (العنوان والهواتف):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                16 to "16 (ناعم)",
                                18 to "18 (قياسي) ✓",
                                20 to "20 (واضح)",
                                22 to "22 (كبير)"
                            ).forEach { (szVal, label) ->
                                FilterChip(
                                    selected = headerDetailsFontSize == szVal,
                                    onClick = { headerDetailsFontSize = szVal },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (headerDetailsFontSize == szVal) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }
            }

            // Order Numbering Starting at 1
            item {
                val nextOrderNum by viewModel.nextOrderNumber.collectAsStateWithLifecycle()
                val deviceRole by viewModel.deviceRole.collectAsStateWithLifecycle()
                val isCloudConnected by viewModel.isCloudConnected.collectAsStateWithLifecycle()
                val canModifyOrderNumber = !isCloudConnected || deviceRole == "CASHIER"
                var customNextNumStr by remember { mutableStateOf("") }
                var pendingOrderNumConfirm by remember { mutableStateOf<Int?>(null) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔢 ترقيم وتسلسل الأوردرات (تبدأ من رقم 1):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = SeaNavyDeep
                        )
                        Text(
                            text = "الترقيم يبدأ من رقم 1 (بدلاً من 100). يمكنك تصفير العداد ليعود لرقم 1 في أي وقت أو تعيين رقم بداية للأوردر القادم.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!canModifyOrderNumber) {
                            Surface(
                                color = StatusDanger.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ تعديل أو تصفير ترقيم الفواتير متاح من جهاز الكاشير الرئيسي فقط أثناء الاتصال بالقاعدة المشتركة.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StatusDanger,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = SeaTealPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "رقم الأوردر التالي: #$nextOrderNum",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SeaTealPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    pendingOrderNumConfirm = 1
                                },
                                enabled = canModifyOrderNumber,
                                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("reset_order_counter_button")
                            ) {
                                Text("🔄 تصفير والبدء من رقم 1", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customNextNumStr,
                                onValueChange = { customNextNumStr = it.filter { ch -> ch.isDigit() } },
                                label = { Text("تعيين رقم أوردر محدد للبدء منه") },
                                placeholder = { Text("مثلاً: 1") },
                                enabled = canModifyOrderNumber,
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_order_number_input")
                            )
                            OutlinedButton(
                                onClick = {
                                    val num = customNextNumStr.toIntOrNull()
                                    if (num != null && num > 0) {
                                        pendingOrderNumConfirm = num
                                    }
                                },
                                enabled = canModifyOrderNumber && customNextNumStr.isNotBlank(),
                                modifier = Modifier.testTag("save_custom_order_number_button")
                            ) {
                                Text("حفظ الرقم", fontSize = 11.sp)
                            }
                        }
                    }
                }

                pendingOrderNumConfirm?.let { targetNum ->
                    AlertDialog(
                        onDismissRequest = { pendingOrderNumConfirm = null },
                        title = {
                            Text(
                                text = "تأكيد تغيير ترقيم الفواتير",
                                fontWeight = FontWeight.Bold,
                                color = SeaNavyDeep
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "سيتم تغيير رقم الأوردر التالي من #$nextOrderNum إلى #$targetNum."
                                )
                                if (isCloudConnected) {
                                    Text(
                                        text = "سيُطبَّق هذا الترقيم مباشرة على القاعدة المشتركة وجميع الأجهزة المتصلة فوراً.",
                                        fontWeight = FontWeight.SemiBold,
                                        color = SeaTealPrimary
                                    )
                                }
                                if (targetNum < nextOrderNum) {
                                    Text(
                                        text = "تنبيه: اختيار رقم أصغر من الترقيم الحالي قد يؤدي لتكرار أرقام فواتير قديمة في السجل.",
                                        fontSize = 12.sp,
                                        color = StatusDanger
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.setNextOrderNumber(targetNum)
                                    customNextNumStr = ""
                                    pendingOrderNumConfirm = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                modifier = Modifier.testTag("confirm_order_number_change_button")
                            ) {
                                Text("تأكيد وتطبيق")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { pendingOrderNumConfirm = null },
                                modifier = Modifier.testTag("cancel_order_number_change_button")
                            ) {
                                Text("إلغاء")
                            }
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val updated = InvoiceTemplateConfig(
                            headerTitle = headerTitle.trim(),
                            subtitle = subtitle.trim(),
                            phone1 = phone1.trim(),
                            phone2 = phone2.trim(),
                            address = address.trim(),
                            footerNote = footerNote.trim(),
                            showPreparations = showPreparations,
                            showOrderNotes = showOrderNotes,
                            showDeliveryDetails = showDeliveryDetails,
                            fontSize = fontSize,
                            logoPath = logoPath,
                            topPadding = topPadding,
                            headerLineSpacing = headerLineSpacing,
                            headerTitleFontSize = headerTitleFontSize,
                            headerDetailsFontSize = headerDetailsFontSize,
                            deliveryManifestFontSize = deliveryManifestFontSize,
                            compactHeaderMode = invCompactHeader,
                            showLogo = invShowLogo,
                            showHeaderTitle = invShowHeaderTitle,
                            showSubtitle = invShowSubtitle,
                            showAddress = invShowAddress,
                            showPhones = invShowPhones,
                            showOrderBadge = invShowOrderBadge,
                            showDateTime = invShowDateTime,
                            showSellerName = invShowSellerName,
                            showCustomerName = invShowCustName,
                            showCustomerPhone = invShowCustPhone,
                            showDeliveryAddress = invShowDeliveryAddr,
                            showDriverName = invShowDriverName,
                            showSubtotal = invShowSubtotal,
                            showDeliveryFee = invShowDeliveryFee,
                            showPaymentStatusBox = invShowPaymentStatusBox,
                            showPaymentBreakdown = invShowPaymentBreakdown,
                            showFooterNote = invShowFooterNote,
                            metadataFontSizePx = invMetaFontSize,
                            tableHeaderFontSizePx = invTableHeaderFontSize,
                            itemFontSizePx = invItemFontSize,
                            itemPrepFontSizePx = invPrepFontSize,
                            itemNotesFontSizePx = invNotesFontSize,
                            totalBoxFontSizePx = invTotalBoxFontSize
                        )
                        viewModel.saveInvoiceTemplateConfig(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ إعدادات وتنسيق فاتورة العميل", fontWeight = FontWeight.Bold)
                }
            }
        }

        // =================================================================
        // SUB-TAB 1: KITCHEN ORDER TICKET (أمر التشغيل وبون المطبخ)
        // =================================================================
        if (activeSubTab == 1) {
            item {
                Text(
                    text = "🍳 تخصيص فورمة وجدول أمر التشغيل وبون المطبخ:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SeaNavyDeep
                )
                Text(
                    text = "تحكم في كل تفصيلة وحجمها في أمر التشغيل مع توفير ورق الطباعة الحرارية، وتتغير المعاينة الحية أمامك لحظياً فور اختيارك.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Live Thermal Kitchen Ticket Table Preview (Updates INSTANTANEOUSLY)
            item {
                val previewFactor = 0.58f

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "--- معاينة حية فورية لأمر التشغيل المطبوع ---", fontSize = 10.5.sp, color = Color.Gray)
                            if (kitchenCompactHeader) {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF2E7D32))
                                ) {
                                    Text(
                                        text = "🌿 توفير الورق: الترويسة مضغوطة",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height((kitchenTopPadding / 2).dp.coerceAtLeast(2.dp)))

                        // 1. Kitchen Header Title
                        if (kitchenShowHeaderTitle) {
                            Text(
                                text = "*** ${kitchenHeaderTitle.ifBlank { "أمر تشغيل المطبخ والاستاف" }} ***",
                                fontWeight = FontWeight.Bold,
                                fontSize = (kitchenHeaderFontSize * previewFactor).sp,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }

                        // 2. Department
                        if (kitchenShowDept && kitchenDepartment.isNotBlank()) {
                            Text(
                                text = "[$kitchenDepartment]",
                                fontSize = (kitchenHeaderFontSize * 0.75f * previewFactor).sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (kitchenShowHeaderTitle || (kitchenShowDept && kitchenDepartment.isNotBlank())) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = (kitchenLineSpacing / 2).dp.coerceAtLeast(2.dp)),
                                color = Color.Black
                            )
                        }

                        // Header Layout: COMPACT MODE vs STANDARD MODE
                        if (kitchenCompactHeader) {
                            // ULTRA-COMPACT HEADER: Tight single bar for Order # + Type + Time
                            Surface(
                                color = Color.Black,
                                shape = RoundedCornerShape(3.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "أوردر: #1",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (22 * previewFactor).sp,
                                        color = Color.White
                                    )

                                    val typePart = if (kitchenShowOrderTypeAndTable) "دليفري توصيل" else ""
                                    val timePart = if (kitchenShowOrderTime && kitchenShowCashierAndTime) " | 02:30 م" else ""
                                    Text(
                                        text = "$typePart$timePart".trim(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (15 * previewFactor).sp,
                                        color = Color.White
                                    )
                                }
                            }

                            // Compact metadata line (Seller, Customer, Phone)
                            val compactMetaParts = mutableListOf<String>()
                            if (kitchenShowSellerName) compactMetaParts.add("بائع: كاشير 1")
                            if (kitchenShowCustName) compactMetaParts.add("عميل: أحمد السيد")
                            if (kitchenShowCustPhone) compactMetaParts.add("هـ: 01012345678")

                            if (compactMetaParts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = compactMetaParts.joinToString(" • "),
                                    fontSize = (kitchenMetaFontSize * previewFactor).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                            }

                            // Compact delivery address & driver
                            if (kitchenShowDeliveryAddr) {
                                Text(
                                    text = "عنوان: السويس - حي الأربعين • طيار: كابتن محمد",
                                    fontSize = ((kitchenMetaFontSize - 1) * previewFactor).sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Black)
                        } else {
                            // STANDARD SPACIOUS HEADER
                            if (kitchenShowOrderTypeAndTable) {
                                Surface(
                                    color = Color.Black,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "أوردر: #1", fontWeight = FontWeight.Bold, fontSize = (26 * previewFactor).sp, color = Color.White)
                                        Text(text = "دليفري توصيل", fontWeight = FontWeight.Bold, fontSize = (18 * previewFactor).sp, color = Color.White)
                                    }
                                }
                            }

                            if (kitchenShowOrderTime && kitchenShowCashierAndTime) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "وقت الطلب: 02:30 م | التاريخ: 2026-10-18",
                                    fontSize = (kitchenMetaFontSize * previewFactor).sp,
                                    color = Color.Black,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (kitchenShowSellerName) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "👤 استلام البائع: كاشير رئيسي",
                                    fontSize = (kitchenMetaFontSize * previewFactor).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            val showCustCard = kitchenShowCustName || kitchenShowCustPhone || kitchenShowDeliveryAddr
                            if (showCustCard) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color.Black),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        if (kitchenShowCustName) {
                                            Text(
                                                text = "العميل: أحمد السيد" + if (kitchenShowCustPhone) " | هاتف: 01012345678" else "",
                                                fontSize = (kitchenMetaFontSize * previewFactor).sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                        if (kitchenShowDeliveryAddr) {
                                            Text(
                                                text = "عنوان التوصيل: السويس - حي الأربعين - شارع الجيش عمارة 5",
                                                fontSize = (kitchenMetaFontSize * 0.9f * previewFactor).sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = "🛵 كابتن التوصيل: كابتن محمد طيار",
                                                fontSize = (kitchenMetaFontSize * 0.85f * previewFactor).sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Black)
                        }

                        // Optional Table Section Title
                        if (kitchenShowTableSectionTitle) {
                            Text(
                                text = "📋 جدول تجهيز وطهي الأصناف المطلوبة:",
                                fontWeight = FontWeight.Bold,
                                fontSize = (18 * previewFactor).sp,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                        }

                        // STRUCTURED TABLE PREVIEW (THE KITCHEN ORDER TABLE)
                        val qtyWeight = (kitchenQtyWidthPct / 25f).coerceIn(0.85f, 1.55f)
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Table Header Row
                                Surface(
                                    color = Color(0xFFE0E0E0),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Black),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "الكمية",
                                            modifier = Modifier.weight(qtyWeight),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (kitchenTableHeaderFontSize * previewFactor).sp,
                                            textAlign = TextAlign.Center,
                                            color = Color.Black
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(18.dp)
                                                .background(Color.Black)
                                        )
                                        if (kitchenShowPrices) {
                                            Text(
                                                text = "الإجمالي",
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = ((kitchenTableHeaderFontSize - 1) * previewFactor).sp,
                                                textAlign = TextAlign.Center,
                                                color = Color.Black
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(18.dp)
                                                    .background(Color.Black)
                                            )
                                        }
                                        Text(
                                            text = kitchenTableItemTitle.ifBlank { "الصنف والتجهيز" },
                                            modifier = Modifier.weight(2.6f).padding(end = 4.dp),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (kitchenTableHeaderFontSize * previewFactor).sp,
                                            textAlign = TextAlign.Right,
                                            color = Color.Black
                                        )
                                    }
                                }

                                // Table Row 1
                                Surface(
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Black),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.weight(qtyWeight).padding(2.dp)
                                        ) {
                                            Text(
                                                text = "2 كجم",
                                                fontWeight = FontWeight.Black,
                                                fontSize = (kitchenQtyFontSize * previewFactor).sp,
                                                textAlign = TextAlign.Center,
                                                color = Color.Black,
                                                modifier = Modifier.padding(vertical = 3.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(42.dp)
                                                .background(Color.Black)
                                        )

                                        if (kitchenShowPrices) {
                                            Text(
                                                text = "370 ج",
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (15 * previewFactor).sp,
                                                textAlign = TextAlign.Center,
                                                color = Color.Black
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(42.dp)
                                                    .background(Color.Black)
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(2.6f).padding(start = 6.dp)
                                        ) {
                                            Text(
                                                text = "• سمك بوري دمياطي كبير",
                                                fontWeight = if (kitchenHighlightItems) FontWeight.ExtraBold else FontWeight.Bold,
                                                fontSize = (kitchenItemFontSize * previewFactor).sp,
                                                color = Color.Black
                                            )
                                            if (kitchenShowPreps) {
                                                Text(
                                                    text = "🔥 سنجاري بالخلطة والليمون (+20)للكيلو",
                                                    fontSize = (kitchenPrepFontSize * previewFactor).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB03A2E)
                                                )
                                            }
                                            if (kitchenShowNotes) {
                                                Text(
                                                    text = "⚠️ ملاحظة: تسوية حارة بدون بصل",
                                                    fontSize = (kitchenNotesFontSize * previewFactor).sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }
                                    }
                                }

                                // Table Row 2
                                Surface(
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Black),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.weight(qtyWeight).padding(2.dp)
                                        ) {
                                            Text(
                                                text = "1 كجم",
                                                fontWeight = FontWeight.Black,
                                                fontSize = (kitchenQtyFontSize * previewFactor).sp,
                                                textAlign = TextAlign.Center,
                                                color = Color.Black,
                                                modifier = Modifier.padding(vertical = 3.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(42.dp)
                                                .background(Color.Black)
                                        )

                                        if (kitchenShowPrices) {
                                            Text(
                                                text = "320 ج",
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (15 * previewFactor).sp,
                                                textAlign = TextAlign.Center,
                                                color = Color.Black
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(42.dp)
                                                    .background(Color.Black)
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(2.6f).padding(start = 6.dp)
                                        ) {
                                            Text(
                                                text = "• جمبري لحم مقلي كرسبي جامبو",
                                                fontWeight = if (kitchenHighlightItems) FontWeight.ExtraBold else FontWeight.Bold,
                                                fontSize = (kitchenItemFontSize * previewFactor).sp,
                                                color = Color.Black
                                            )
                                            if (kitchenShowPreps) {
                                                Text(
                                                    text = "🔥 مقلي مقرمش بالخلطة الخاصة (+15)للكيلو",
                                                    fontSize = (kitchenPrepFontSize * previewFactor).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB03A2E)
                                                )
                                            }
                                            if (kitchenShowNotes) {
                                                Text(
                                                    text = "⚠️ ملاحظة: ليمون زيادة وكاتشب خارجي",
                                                    fontSize = (kitchenNotesFontSize * previewFactor).sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Order General Note Box
                        if (kitchenShowNotes) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFFF8F9FA),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📝 ملاحظة عامة هامة للأوردر: تسليم طازج وسريع مع علبة مخلل هدية",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (kitchenNotesFontSize * previewFactor).sp,
                                    color = Color.Black,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }

                        // Summary Count Badge
                        if (kitchenShowItemCountSummary) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color.Black,
                                shape = RoundedCornerShape(3.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "إجمالي الأصناف المطلوبة للتجهيز: 2 صنف",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (18 * previewFactor).sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )
                            }
                        }

                        if (kitchenShowFooterNote && kitchenFooterNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "--- $kitchenFooterNote ---",
                                fontSize = (15 * previewFactor).sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // =================================================================
            // CARD 1: SMART PAPER-SAVING MODE (وضع توفير الورق الذكي)
            // =================================================================
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🌿 وضع توفير ورق الطباعة والترويسة المضغوطة:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = SeaNavyDeep
                            )
                        }
                        Text(
                            text = "حل مشكلة المساحة الكبيرة في رأس البون: يضغط تفاصيل الأوردر في سطر واحد ويوفر أكثر من 60% من طول الورقة قبل جدول الأصناف.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            color = if (kitchenCompactHeader) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenCompactHeader = !kitchenCompactHeader }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = kitchenCompactHeader,
                                    onCheckedChange = { kitchenCompactHeader = it }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "تفعيل وضع الترويسة المضغوطة (يوفر 65% من الورق) ⭐",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (kitchenCompactHeader) Color(0xFF1B5E20) else Color.Black
                                    )
                                    Text(
                                        text = "دمج رقم الأوردر ونوعه وتاريخه وبيانات العميل في أسطر مضغوطة دون كروت كبيرة مفرغة.",
                                        fontSize = 10.5.sp,
                                        color = if (kitchenCompactHeader) Color(0xFF2E7D32) else Color.Gray
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowTableSectionTitle = !kitchenShowTableSectionTitle },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowTableSectionTitle, onCheckedChange = { kitchenShowTableSectionTitle = it })
                            Column {
                                Text(text = "إظهار سطر عنوان جدول الأصناف ('جدول تجهيز وطهي...')", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(text = "إلغاء التفعيل يبدأ جدول الأصناف مباشرة ويوفر 35 بكسل رأسية إضافية.", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowFooterNote = !kitchenShowFooterNote },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowFooterNote, onCheckedChange = { kitchenShowFooterNote = it })
                            Column {
                                Text(text = "إظهار الملاحظة الختامية بأسفل البون", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(text = "إلغاء التفعيل يوفر المساحة بأسفل الورقة.", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // =================================================================
            // CARD 2: GRANULAR TOGGLES FOR EVERY DETAIL (إظهار وإخفاء كل تفصيلة)
            // =================================================================
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "👁️ التحكم الدقيق في إظهار وإخفاء كل تفصيلة في البون:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = SeaNavyDeep
                        )
                        Text(
                            text = "يمكنك إخفاء أي معلومة لا تحتاجها في المطبخ (مثل اسم البائع أو العميل) لتركيز البون على الأصناف فقط:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowHeaderTitle = !kitchenShowHeaderTitle },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowHeaderTitle, onCheckedChange = { kitchenShowHeaderTitle = it })
                            Text(text = "إظهار العنوان الرئيسي للترويسة (أمر تشغيل المطبخ)", fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowDept = !kitchenShowDept },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowDept, onCheckedChange = { kitchenShowDept = it })
                            Text(text = "إظهار اسم القسم / المطبخ الفرعي (إن وُجد)", fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowOrderTypeAndTable = !kitchenShowOrderTypeAndTable },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowOrderTypeAndTable, onCheckedChange = { kitchenShowOrderTypeAndTable = it })
                            Text(text = "إظهار نوع الأوردر (دليفري توصيل / صالة / استلام)", fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowSellerName = !kitchenShowSellerName },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowSellerName, onCheckedChange = { kitchenShowSellerName = it })
                            Column {
                                Text(text = "إظهار اسم البائع / الكاشير 👤", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "يمكنك إخفاؤه لتوفير مساحة الورقة إذا كان المطبخ لا يحتاج اسم البائع.", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowOrderTime = !kitchenShowOrderTime },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowOrderTime, onCheckedChange = { kitchenShowOrderTime = it })
                            Text(text = "إظهار وقت وتاريخ الطلب (نظام 12 ساعة ص / م) ⏱️", fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowCustName = !kitchenShowCustName },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowCustName, onCheckedChange = { kitchenShowCustName = it })
                            Text(text = "إظهار اسم العميل", fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowCustPhone = !kitchenShowCustPhone },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowCustPhone, onCheckedChange = { kitchenShowCustPhone = it })
                            Text(text = "إظهار هاتف العميل", fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowDeliveryAddr = !kitchenShowDeliveryAddr },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowDeliveryAddr, onCheckedChange = { kitchenShowDeliveryAddr = it })
                            Text(text = "إظهار عنوان التوصيل واسم كابتن الدليفري", fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowPreps = !kitchenShowPreps },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowPreps, onCheckedChange = { kitchenShowPreps = it })
                            Text(text = "إظهار طرق الطهي والتجهيز البارزة (شوي / قلي / سنجاري)", fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowNotes = !kitchenShowNotes },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowNotes, onCheckedChange = { kitchenShowNotes = it })
                            Text(text = "إظهار ملاحظات الأصناف والملاحظات العامة للأوردر", fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowPrices = !kitchenShowPrices },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowPrices, onCheckedChange = { kitchenShowPrices = it })
                            Text(text = "إظهار أسعار الأصناف في بون المطبخ (افتراضياً مخفية لتسريع القراءة)", fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenShowItemCountSummary = !kitchenShowItemCountSummary },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenShowItemCountSummary, onCheckedChange = { kitchenShowItemCountSummary = it })
                            Text(text = "إظهار شريط إجمالي عدد الأصناف المطلوبة للتجهيز", fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { kitchenHighlightItems = !kitchenHighlightItems },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = kitchenHighlightItems, onCheckedChange = { kitchenHighlightItems = it })
                            Text(text = "تضخيم وإبراز أسماء الأصناف في الجدول", fontSize = 12.sp)
                        }
                    }
                }
            }

            // =================================================================
            // CARD 3: GRANULAR FONT SIZES (التحكم في أحجام الخطوط لكل عنصر)
            // =================================================================
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔤 التحكم في أحجام الخطوط لكل عنصر على حدة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = SeaNavyDeep
                        )
                        Text(
                            text = "اضبط حجم كل عنصر بدقة، لتكبير الأصناف والكميات وتصغير الترويسة وبيانات العميل:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 1. Header Title Font Size
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط عنوان الترويسة الرئيسي:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                20 to "20 (صغير موفر)",
                                24 to "24 (قياسي) ✓",
                                28 to "28 (كبير)",
                                32 to "32 (ضخم)"
                            ).forEach { (sz, label) ->
                                FilterChip(
                                    selected = kitchenHeaderFontSize == sz,
                                    onClick = { kitchenHeaderFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (kitchenHeaderFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        // 2. Metadata Font Size (Customer, Seller, Date/Time)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط بيانات العميل والبائع والوقت (الترويسة):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                13 to "13 (ناعم وموفر)",
                                16 to "16 (قياسي) ✓",
                                19 to "19 (واضح)",
                                22 to "22 (كبير)"
                            ).forEach { (sz, label) ->
                                FilterChip(
                                    selected = kitchenMetaFontSize == sz,
                                    onClick = { kitchenMetaFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (kitchenMetaFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        // 3. Item Name Font Size
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط أسماء الأصناف في جدول المطبخ:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                22 to "22 (عادي)",
                                26 to "26 (بارز) ✓",
                                30 to "30 (كبير للمطبخ)",
                                34 to "34 (عملاق)"
                            ).forEach { (sz, label) ->
                                FilterChip(
                                    selected = kitchenItemFontSize == sz,
                                    onClick = { kitchenItemFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (kitchenItemFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        // 4. Quantity Font Size
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط الكمية بالجدول (2 كجم، 1 صنف):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                22 to "22 (وسط)",
                                28 to "28 (كبير) ✓",
                                32 to "32 (بارز)",
                                36 to "36 (ضخم)"
                            ).forEach { (sz, label) ->
                                FilterChip(
                                    selected = kitchenQtyFontSize == sz,
                                    onClick = { kitchenQtyFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (kitchenQtyFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        // 5. Preparation & Cooking Method Font Size
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط طريقة الطهي والتجهيز (شوي/سنجاري/قلي):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                16 to "16 (ناعم)",
                                20 to "20 (قياسي) ✓",
                                24 to "24 (بارز)"
                            ).forEach { (sz, label) ->
                                FilterChip(
                                    selected = kitchenPrepFontSize == sz,
                                    onClick = { kitchenPrepFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (kitchenPrepFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        // 6. Item Notes Font Size
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط ملاحظات الأصناف:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                13 to "13 (صغير)",
                                17 to "17 (قياسي) ✓",
                                21 to "21 (واضح)"
                            ).forEach { (sz, label) ->
                                FilterChip(
                                    selected = kitchenNotesFontSize == sz,
                                    onClick = { kitchenNotesFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (kitchenNotesFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        // 7. Table Header Font Size
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "حجم خط رأس جدول الأصناف (الكمية / الصنف والتجهيز):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                15 to "15 (صغير)",
                                18 to "18 (قياسي) ✓",
                                22 to "22 (بارز)"
                            ).forEach { (sz, label) ->
                                FilterChip(
                                    selected = kitchenTableHeaderFontSize == sz,
                                    onClick = { kitchenTableHeaderFontSize = sz },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (kitchenTableHeaderFontSize == sz) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // CARD 4: TEXT FIELDS, PADDINGS & DIMENSIONS (النصوص والهوامش والأبعاد)
            // =================================================================
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📐 النصوص والهوامش وأبعاد جدول أمر التشغيل:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = SeaNavyDeep
                        )

                        OutlinedTextField(
                            value = kitchenHeaderTitle,
                            onValueChange = { kitchenHeaderTitle = it },
                            label = { Text("عنوان أمر التشغيل الرئيسي") },
                            placeholder = { Text("أمر تشغيل المطبخ والاستاف") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = kitchenDepartment,
                            onValueChange = { kitchenDepartment = it },
                            label = { Text("اسم القسم / المطبخ الفرعي (اختياري)") },
                            placeholder = { Text("مثلاً: قسم الشواية والفرن / قسم المقلي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = kitchenTableItemTitle,
                            onValueChange = { kitchenTableItemTitle = it },
                            label = { Text("عنوان عمود الصنف والتجهيز في رأس الجدول") },
                            placeholder = { Text("الصنف والتجهيز") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = kitchenFooterNote,
                            onValueChange = { kitchenFooterNote = it },
                            label = { Text("جملة الملاحظة الختامية أسفل البون") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "عرض عمود الكمية بالجدول (نسبة العرض المئوية):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                22 to "مضغوط (22%)",
                                28 to "متوازن (28%) ✓",
                                34 to "عريض وبارز (34%)"
                            ).forEach { (pctVal, label) ->
                                FilterChip(
                                    selected = kitchenQtyWidthPct == pctVal,
                                    onClick = { kitchenQtyWidthPct = pctVal },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (kitchenQtyWidthPct == pctVal) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "هامش أعلى أمر التشغيل (نزول النص من حافة الورقة):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                4 to "أقصى توفير (4px) ⚡",
                                10 to "قصير (10px) ✓",
                                18 to "متوازن (18px)",
                                26 to "مريح (26px)"
                            ).forEach { (paddingVal, label) ->
                                FilterChip(
                                    selected = kitchenTopPadding == paddingVal,
                                    onClick = { kitchenTopPadding = paddingVal },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (kitchenTopPadding == paddingVal) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "المسافة بين سطور الترويسة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                3 to "مضغوط وموفر (3px)",
                                6 to "قياسي (6px) ✓",
                                10 to "متباعد (10px)"
                            ).forEach { (spacingVal, label) ->
                                FilterChip(
                                    selected = kitchenLineSpacing == spacingVal,
                                    onClick = { kitchenLineSpacing = spacingVal },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (kitchenLineSpacing == spacingVal) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // SAVE BUTTON
            // =================================================================
            item {
                Button(
                    onClick = {
                        val updated = KitchenOrderTemplateConfig(
                            headerTitle = kitchenHeaderTitle.trim(),
                            kitchenDepartment = kitchenDepartment.trim(),
                            showHeaderTitle = kitchenShowHeaderTitle,
                            showDepartment = kitchenShowDept,
                            showCustomerName = kitchenShowCustName,
                            showCustomerPhone = kitchenShowCustPhone,
                            showDeliveryAddress = kitchenShowDeliveryAddr,
                            showSellerName = kitchenShowSellerName,
                            showOrderTime = kitchenShowOrderTime,
                            showOrderTypeAndTable = kitchenShowOrderTypeAndTable,
                            compactHeaderMode = kitchenCompactHeader,
                            showTableSectionTitle = kitchenShowTableSectionTitle,
                            showPreparations = kitchenShowPreps,
                            showOrderNotes = kitchenShowNotes,
                            showItemPrices = kitchenShowPrices,
                            showCashierAndTime = kitchenShowCashierAndTime,
                            showItemCountSummary = kitchenShowItemCountSummary,
                            highlightItemNames = kitchenHighlightItems,
                            showFooterNote = kitchenShowFooterNote,
                            fontSize = kitchenFontSize,
                            footerNote = kitchenFooterNote.trim(),
                            topPadding = kitchenTopPadding,
                            headerLineSpacing = kitchenLineSpacing,
                            tableHeaderItemColumnTitle = kitchenTableItemTitle.trim().ifBlank { "الصنف والتجهيز" },
                            qtyColumnWidthPercent = kitchenQtyWidthPct,
                            headerFontSizePx = kitchenHeaderFontSize,
                            metadataFontSizePx = kitchenMetaFontSize,
                            tableHeaderFontSizePx = kitchenTableHeaderFontSize,
                            itemFontSizePx = kitchenItemFontSize,
                            itemPrepFontSizePx = kitchenPrepFontSize,
                            itemNotesFontSizePx = kitchenNotesFontSize,
                            qtyFontSizePx = kitchenQtyFontSize
                        )
                        viewModel.saveKitchenOrderTemplateConfig(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ إعدادات وتنسيق أمر التشغيل للمطبخ ✓", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
