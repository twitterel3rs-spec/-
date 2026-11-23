package com.example.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.platform.testTag
import com.example.util.CloudAuthManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ApkDownloadDialog
import com.example.util.ApkManager
import com.example.util.BackgroundNotificationHelper
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaNavyMedium
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@Composable
fun SettingsReportsAndBackupTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val menuItems by viewModel.menuItems.collectAsStateWithLifecycle()
    val printLogs by viewModel.printLogs.collectAsStateWithLifecycle()
    val deviceRole by viewModel.deviceRole.collectAsStateWithLifecycle()

    val lastBackupInfo by viewModel.lastBackupInfo.collectAsStateWithLifecycle()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsStateWithLifecycle()
    val isBackingUp by viewModel.isBackingUp.collectAsStateWithLifecycle()

    val cloudUser by viewModel.cloudUser.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val lastCloudSyncTime by viewModel.lastCloudSyncTime.collectAsStateWithLifecycle()
    val activeSellerName by viewModel.activeSellerName.collectAsStateWithLifecycle()
    val isRestoringBackupSyncingCloud by viewModel.isRestoringBackupSyncingCloud.collectAsStateWithLifecycle()
    val backgroundServiceEnabled by viewModel.backgroundServiceEnabled.collectAsStateWithLifecycle()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsStateWithLifecycle()
    val pendingSyncOrdersCount by viewModel.pendingSyncOrdersCount.collectAsStateWithLifecycle()
    var sellerNameDraft by remember(activeSellerName) { mutableStateOf(activeSellerName) }
    var showSellerNameBeforeLoginDialog by remember { mutableStateOf(false) }
    var isSigningInCloud by remember { mutableStateOf(false) }

    var showPasswordChangeDialog by remember { mutableStateOf(false) }
    var showBackupJsonDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var backupJsonText by remember { mutableStateOf("") }
    var showApkDownloadDialog by remember { mutableStateOf(false) }

    var pendingSellerRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCashierRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCashierRestoreJson by remember { mutableStateOf<String?>(null) }
    var isCashierSupremeRestoring by remember { mutableStateOf(false) }

    val restoreFromDriveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            if (deviceRole == "SELLER") {
                pendingSellerRestoreUri = uri
            } else {
                pendingCashierRestoreUri = uri
            }
        }
    }

    if (pendingSellerRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { pendingSellerRestoreUri = null },
            title = { Text("تنبيه استعادة محلية لجهاز البائع ⚠️", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text(
                    "سيتم استبدال قاعدة البيانات المحلية على هذا الجهاز فقط.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetUri = pendingSellerRestoreUri ?: return@Button
                        pendingSellerRestoreUri = null
                        viewModel.restoreFromUri(targetUri) { success, msg ->
                            if (success) {
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "خطأ في الاسترجاع: $msg", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberCoral)
                ) {
                    Text("استعادة محلياً لهذا الجهاز فقط", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingSellerRestoreUri = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (pendingCashierRestoreUri != null || pendingCashierRestoreJson != null) {
        val isCloudLinked = cloudUser != null
        AlertDialog(
            onDismissRequest = {
                if (!isCashierSupremeRestoring) {
                    pendingCashierRestoreUri = null
                    pendingCashierRestoreJson = null
                }
            },
            title = {
                Text(
                    "تأكيد الاستعادة بسلطة الكاشير العليا ⚠️",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isCloudLinked) {
                            "ستصبح هذه النسخة الاحتياطية هي المرجع الأعلى والوحيد:\n• سيتم استبدال كافة البيانات الحالية على جهاز الكاشير بالنسخة المختارة.\n• سيتم مسح السحابة المشتركة وإعادة رفع النسخة وتحديث جميع أجهزة البائعين المتصلة تلقائياً.\n• ستحتفظ قائمة العملاء بكامل سجل أوردراتهم التاريخي والأرشيفي من النسخة."
                        } else {
                            "ستصبح هذه النسخة الاحتياطية هي المرجع الأعلى:\n• سيتم استبدال كافة البيانات الحالية محلياً بالنسخة المختارة.\n• عند ربط السحابة لاحقاً، سيتم فرض هذه النسخة تلقائياً على السحابة المشتركة وجميع أجهزة البائعين."
                        },
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    if (isCashierSupremeRestoring) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ استعادة النسخة وفرضها على السحابة...", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetUri = pendingCashierRestoreUri
                        val targetJson = pendingCashierRestoreJson
                        isCashierSupremeRestoring = true
                        if (targetUri != null) {
                            viewModel.restoreFromUri(targetUri) { success, msg ->
                                isCashierSupremeRestoring = false
                                pendingCashierRestoreUri = null
                                pendingCashierRestoreJson = null
                                Toast.makeText(
                                    context,
                                    if (success) msg else "خطأ في الاسترجاع: $msg",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else if (targetJson != null) {
                            viewModel.restoreFromJson(targetJson) { success, msg ->
                                isCashierSupremeRestoring = false
                                pendingCashierRestoreUri = null
                                pendingCashierRestoreJson = null
                                if (success) {
                                    showRestoreDialog = false
                                    restoreJsonInput = ""
                                }
                                Toast.makeText(
                                    context,
                                    if (success) msg else "خطأ في الاسترجاع: $msg",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    enabled = !isCashierSupremeRestoring,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                    modifier = Modifier.testTag("confirm_cashier_supreme_restore_button")
                ) {
                    Text("تأكيد وفرض النسخة الآن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        pendingCashierRestoreUri = null
                        pendingCashierRestoreJson = null
                    },
                    enabled = !isCashierSupremeRestoring,
                    modifier = Modifier.testTag("cancel_cashier_supreme_restore_button")
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Advanced Sales Report States (Requirement 5)
    val advancedSalesReport by viewModel.advancedSalesReport.collectAsStateWithLifecycle()
    var selectedReportPeriod by remember { mutableIntStateOf(0) } // 0: اليوم, 1: آخر 7 أيام, 2: هذا الشهر, 3: كل الفترة

    fun calculateReportTimestamps(periodIndex: Int): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return when (periodIndex) {
            0 -> Pair(cal.timeInMillis, now)
            1 -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                Pair(cal.timeInMillis, now)
            }
            2 -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                Pair(cal.timeInMillis, now)
            }
            else -> Pair(0L, now)
        }
    }

    LaunchedEffect(selectedReportPeriod) {
        val (s, e) = calculateReportTimestamps(selectedReportPeriod)
        viewModel.loadAdvancedSalesReport(s, e)
    }

    // Analytics calculations
    val activeOrders = orders.filter { it.order.status != "CANCELLED" }
    val totalRevenue = activeOrders.sumOf { it.order.totalAmount }
    val totalDeliveryFees = activeOrders.sumOf { it.order.deliveryFee }
    val cancelledCount = orders.count { it.order.status == "CANCELLED" }
    val averageOrderValue = if (activeOrders.isNotEmpty()) totalRevenue / activeOrders.size else 0.0

    MouseLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            com.example.ui.components.TodaySalesChartComponent(viewModel = viewModel, showHeader = true)
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "تقارير العمليات والورديات السابقة:",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // Summary KPI Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SeaTealPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "إجمالي المبيعات", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", totalRevenue)} ج.م",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(text = "${activeOrders.size} أوردر ناجح", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = AmberCoral),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "متوسط قيمة الأوردر", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", averageOrderValue)} ج.م",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(text = "رسوم دليفري: $totalDeliveryFees ج", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                    }
                }
            }
        }

        // Orders breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "مؤشرات المطعم والنظام:", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "إجمالي الأوردرات المسجلة:", fontSize = 13.sp)
                        Text(text = "${orders.size}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "أوردرات ملغاة:", fontSize = 13.sp)
                        Text(text = "$cancelledCount", fontWeight = FontWeight.Bold, color = StatusDanger, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "عدد أصناف القائمة:", fontSize = 13.sp)
                        Text(text = "${menuItems.size} صنف", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "عمليات الطباعة المسجلة:", fontSize = 13.sp)
                        Text(text = "${printLogs.size} عملية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Advanced Sales and Items Report Section (Requirement 5)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تقرير مبيعات وحركة الأصناف (الأكثر والأقل مبيعاً):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        item {
            // Period selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val periods = listOf("اليوم", "آخر 7 أيام", "هذا الشهر", "كل الفترة")
                periods.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = selectedReportPeriod == idx,
                        onClick = { selectedReportPeriod = idx },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        advancedSalesReport?.let { rep ->
            // Financial KPI Highlights (No Profit Calculations)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = SeaTealPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("إجمالي المبيعات", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp)
                            Text(
                                "${String.format(Locale.US, "%.1f", rep.totalSalesRevenue)} ج",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = SeaNavyMedium),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("أوردرات الفترة", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp)
                            Text(
                                "${rep.totalOrdersCount} أوردر",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("إجمالي الخصومات", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                            Text(
                                "${String.format(Locale.US, "%.1f", rep.totalDiscounts)} ج",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Top-Selling Items
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "الأصناف الأكثر مبيعاً (الأعلى إقبالاً):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (rep.topSellingItems.isEmpty()) {
                            Text(text = "لا توجد مبيعات مسجلة في هذه الفترة بعد", fontSize = 11.sp, color = Color.Gray)
                        } else {
                            rep.topSellingItems.forEachIndexed { index, stat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (index == 0) AmberCoral else SeaTealPrimary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                color = if (index == 0) Color.White else SeaTealPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = stat.itemName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(text = "مباع: ${stat.totalQuantitySold} ${stat.unit}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "${String.format(Locale.US, "%.1f", stat.totalRevenue)} ج.م", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SeaTealPrimary)
                                    }
                                }
                                if (index < rep.topSellingItems.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }

            // Least-Selling Items
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.TrendingDown, contentDescription = null, tint = AmberCoral, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "الأصناف الأقل مبيعاً (بحاجة لتنشيط أو مراجعة):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (rep.leastSellingItems.isEmpty()) {
                            Text(text = "لا توجد أصناف راكدة", fontSize = 11.sp, color = Color.Gray)
                        } else {
                            rep.leastSellingItems.take(5).forEachIndexed { index, stat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = stat.itemName, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text(text = "مباع: ${stat.totalQuantitySold} ${stat.unit}", fontSize = 10.sp, color = Color.Gray)
                                    }

                                    Text(text = "${String.format(Locale.US, "%.1f", stat.totalRevenue)} ج.م", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = AmberCoral)
                                }
                                if (index < 4 && index < rep.leastSellingItems.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }

            // Out-of-Stock Alert Section
            if (rep.outOfStockItems.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StatusDanger.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "أصناف نفد رصيدها تماماً بالمطعم (${rep.outOfStockItems.size}):",
                                    color = StatusDanger,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            rep.outOfStockItems.forEach { m ->
                                Text(
                                    text = "• ${m.name} (${m.category}) - الرصيد الحالي: 0 ${m.unit}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // Manager Security & Password
        item {
            Text(
                text = "الأمان وكلمة سر الإدارة:",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "كلمة سر الإدارة (الافتراضية: 1234)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "تحمي تجاوز المخزون عند نفاده، وإلغاء الأوردرات، وتعديل وحذف الأصناف والأسعار.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showPasswordChangeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                    ) {
                        Text("تغيير", fontSize = 12.sp)
                    }
                }
            }
        }

        // Central Shared Cloud Database Section
        item {
            Text(
                text = "🌐 قاعدة البيانات السحابية المشتركة لكافة الأجهزة (Central Cloud DB):",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (cloudUser != null) StatusSuccess.copy(alpha = 0.15f) else AmberCoral.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (cloudUser != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (cloudUser != null) StatusSuccess else AmberCoral,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "قاعدة بيانات الويب السحابية المشتركة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                )
                                Text(
                                    text = if (cloudUser != null) "متصل بحساب المطعم: ${cloudUser?.email}" else "الجهاز يعمل محلياً فقط - غير مربوط بالسحابة",
                                    fontSize = 11.sp,
                                    color = if (cloudUser != null) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (cloudUser != null) StatusSuccess.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (cloudUser != null) StatusSuccess else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = if (cloudUser != null) "🟢 متصل" else "⚪ محلي",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (cloudUser != null) StatusSuccess else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Explanation of the shared database
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "💡 كيف تعمل قاعدة البيانات المشتركة؟",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SeaNavyDeep
                            )
                            Text(
                                text = "• عند ربط أجهزة المطعم بنفس الحساب، يكون التطبيق مجرد واجهة وقاعدة البيانات واحدة مركزية على الويب.\n" +
                                        "• أجهزة البائعين بالصالة ترسل الأوردرات فورياً للكاشير وطابعات المطبخ لحظة الحفظ.\n" +
                                        "• أي تعديل في أسعار الأسماك اليومية أو الأصناف يتحدث في ثانية واحدة في كل تليفونات المطعم.\n" +
                                        "• دعم كامل لـ Offline-First: لو النت فصل، يستمر البيع والطباعة ويرتفع تلقائياً عند عودة النت.",
                                fontSize = 10.5.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Terminal Role & Station Configuration
                    Text(
                        text = "وظيفة هذا الجهاز الحالية في المطعم:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = deviceRole == "CASHIER",
                            onClick = { viewModel.setDeviceRole("CASHIER") },
                            label = { Text("🖥️ كاشير رئيسي (يستقبل ويطبع)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = deviceRole == "SELLER",
                            onClick = { viewModel.setDeviceRole("SELLER") },
                            label = { Text("📱 بائع صالة (يرسل للسحابة)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Active Seller / Terminal Identity Input
                    Text(
                        text = "اسم البائع / الموظف الحالي على هذا الجهاز:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = sellerNameDraft,
                            onValueChange = { sellerNameDraft = it },
                            placeholder = { Text("مثال: أحمد / كاشير 1", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "اسم البائع",
                                    tint = SeaTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("seller_name_input")
                        )
                        Button(
                            onClick = {
                                viewModel.setActiveSellerName(sellerNameDraft)
                                Toast.makeText(context, "تم حفظ الاسم على هذا الجهاز ✓", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SeaNavyDeep),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("save_seller_name_button")
                        ) {
                            Text("حفظ الاسم ✓", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    val launchGoogleCloudSignIn: () -> Unit = {
                        val act = context as? Activity
                        if (act != null) {
                            if (sellerNameDraft.isNotBlank()) {
                                viewModel.setActiveSellerName(sellerNameDraft)
                            }
                            isSigningInCloud = true
                            CloudAuthManager.signInWithGoogle(
                                activity = act,
                                scope = scope,
                                onSuccess = { _ ->
                                    isSigningInCloud = false
                                    Toast.makeText(context, "تم ربط المطعم بقاعدة البيانات السحابية بنجاح ✓", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    isSigningInCloud = false
                                    Toast.makeText(context, "خطأ في الاتصال: $err", Toast.LENGTH_LONG).show()
                                },
                                onCancelled = {
                                    isSigningInCloud = false
                                }
                            )
                        }
                    }

                    if (showSellerNameBeforeLoginDialog) {
                        AlertDialog(
                            onDismissRequest = { showSellerNameBeforeLoginDialog = false },
                            title = {
                                Text(
                                    text = "تحديد اسم البائع قبل الاتصال 👤",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "اكتب اسم البائع ليظهر في شاشة الأوردرات (استلام بواسطة) عند إرسال الطلبات للكاشير الرئيسي:",
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                    OutlinedTextField(
                                        value = sellerNameDraft,
                                        onValueChange = { sellerNameDraft = it },
                                        label = { Text("اسم البائع") },
                                        placeholder = { Text("مثال: محمد / أحمد") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("pre_login_seller_name_input")
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (sellerNameDraft.isNotBlank()) {
                                            viewModel.setActiveSellerName(sellerNameDraft)
                                        }
                                        showSellerNameBeforeLoginDialog = false
                                        launchGoogleCloudSignIn()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                                ) {
                                    Text("حفظ ومتابعة للدخول ✓", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showSellerNameBeforeLoginDialog = false
                                        launchGoogleCloudSignIn()
                                    }
                                ) {
                                    Text("دخول بدون تعديل")
                                }
                            }
                        )
                    }

                    // Cloud Actions
                    if (cloudUser == null) {
                        Button(
                            onClick = {
                                if (deviceRole == "SELLER" && (sellerNameDraft.isBlank() || sellerNameDraft.trim() == "بائع صالة")) {
                                    showSellerNameBeforeLoginDialog = true
                                } else {
                                    launchGoogleCloudSignIn()
                                }
                            },
                            enabled = !isSigningInCloud,
                            colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("connect_cloud_database_button")
                        ) {
                            if (isSigningInCloud) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جارٍ التحقق وربط قاعدة البيانات المشتركة...", fontSize = 12.sp)
                            } else {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ربط هذا الجهاز بقاعدة البيانات السحابية المشتركة 🌐", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    } else {
                        // Connected actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "آخر مزامنة: ${lastCloudSyncTime ?: "لحظية مستمرة الآن ✓"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SeaTealPrimary
                            )
                        }

                        if (isRestoringBackupSyncingCloud) {
                            Surface(
                                color = Color(0xFFE0F2FE),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "جارٍ رفع النسخة المستعادة للسحابة المشتركة تلقائياً... ☁️",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SeaNavyDeep
                                    )
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = SeaTealPrimary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.syncAllLocalMenuToCloud() },
                                enabled = !isCloudSyncing && !isRestoringBackupSyncingCloud,
                                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isCloudSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("جارٍ الرفع...", fontSize = 11.sp)
                                } else {
                                    Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("رفع وتحديث الأصناف للسحابة 📤", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    CloudAuthManager.signOut(context, scope) {
                                        Toast.makeText(context, "تم تسجيل الخروج من السحابة والتحول للوضع المحلي", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.5f)
                            ) {
                                Text("خروج 🚪", fontSize = 11.sp, color = StatusDanger)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Always-On Background Service & Offline Sync Controls
                        val isBatteryExempt = remember(cloudUser, backgroundServiceEnabled) {
                            BackgroundNotificationHelper.isIgnoringBatteryOptimizations(context)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "التشغيل الدائم في الخلفية والإشعارات 🔔",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = SeaNavyDeep
                                        )
                                        Text(
                                            text = if (deviceRole == "CASHIER") {
                                                "يبقي الكاشير الرئيسي مستعداً لاستقبال وطباعة أوردرات البائعين فوراً حتى لو التطبيق مصغّر"
                                            } else {
                                                "يرسل الأوردرات المعلقة أوفلاين فور عودة النت (خلال 30 ثانية للطباعة التلقائية) ويعمل في الخلفية دائماً"
                                            },
                                            fontSize = 10.5.sp,
                                            lineHeight = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = backgroundServiceEnabled,
                                        onCheckedChange = { viewModel.setBackgroundServiceEnabled(it) },
                                        modifier = Modifier.testTag("background_service_switch")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isDeviceOnline) "🌐 الشبكة: متصل بالإنترنت" else "📴 الشبكة: أوفلاين حالياً",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDeviceOnline) StatusSuccess else Color(0xFFB45309)
                                    )
                                    if (pendingSyncOrdersCount > 0) {
                                        Text(
                                            text = "⏳ أوردرات معلقة: $pendingSyncOrdersCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AmberCoral
                                        )
                                    }
                                }

                                if (pendingSyncOrdersCount > 0) {
                                    Button(
                                        onClick = { viewModel.triggerManualOfflineFlush() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("manual_flush_offline_orders_button")
                                    ) {
                                        Text(
                                            text = "مزامنة وإرسال الأوردرات المعلقة الآن ($pendingSyncOrdersCount) 🔄",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { BackgroundNotificationHelper.openBatteryOptimizationSettings(context) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("battery_optimization_settings_button")
                                ) {
                                    Text(
                                        text = if (isBatteryExempt) {
                                            "🔋 استثناء البطارية: مفعّل ✓ (انقر للمراجعة)"
                                        } else {
                                            "🔋 فتح إعدادات البطارية (لضمان عدم إيقاف الخلفية)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SeaNavyDeep
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Automatic Daily Backup & Database Section
        item {
            Text(
                text = "النسخ الاحتياطي التلقائي اليومي وقاعدة البيانات:",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header with Backup status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SeaTealPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = SeaTealPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "النسخ الاحتياطي التلقائي اليومي",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "نسخ حقيقي كامل لكافة بيانات النظام وتصدير السجلات",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isAutoBackupEnabled,
                            onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Backup Information Box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("توقيت آخر نسخة احتياطية:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = lastBackupInfo["date"] ?: "اليوم تلقائياً",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SeaTealPrimary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("حجم البيانات وسجلات الأرشيف:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${lastBackupInfo["size"] ?: "0 ك.ب"} (${lastBackupInfo["records"] ?: "كامل السجلات"})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("حالة النسخ اليومي التلقائي:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (isAutoBackupEnabled) "نشط كل 24 ساعة تلقائياً ✓" else "متوقف مؤقتاً",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAutoBackupEnabled) StatusSuccess else StatusDanger
                                )
                            }
                        }
                    }

                    // Guaranteed 100% Comprehensive Coverage Assurance Box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SeaNavyDeep.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SeaTealPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🛡️ محتويات النسخة الشاملة (مؤكدة ومحمية 100%):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SeaTealPrimary
                                )
                            }
                            Text(
                                text = "• 🖨️ إعدادات الطابعات: طابعة الكاشير، مطابخ 1 و2، وطابعات الشبكة والبلوتوث.\n" +
                                        "• 💰 الحسابات العامة: الموردين والمستوردين وسندات الصرف والقبض وحركة الدرج.\n" +
                                        "• 👥 الأشخاص والموظفين: بيانات العاملين، الحضور والانصراف، والمسحوبات.\n" +
                                        "• 🛵 كباتن التوصيل: الموصلون، العهدة، اليوميات، وحسابات الدليفري.\n" +
                                        "• 📋 الأوردرات وفواتير المبيعات: كافة فواتير الأوردرات والأصناف وطرق الدفع.\n" +
                                        "• 🐟 القوائم والأصناف: الأقسام، الأسماك، أسعار الكيلو والقطعة، وطرق الطهي والتجهيز.\n" +
                                        "• 🕒 الورديات والمخزون: تاريخ الورديات وحركة الخزينة وأرصدة الخامات.",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.5.sp
                            )
                        }
                    }

                    // Main Backup Button
                    Button(
                        onClick = {
                            viewModel.triggerFullBackup { dbFile, _ ->
                                val uploadIntent = com.example.util.BackupManager.createBackupShareIntent(context, dbFile)
                                context.startActivity(uploadIntent)
                            }
                        },
                        enabled = !isBackingUp,
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ إنشاء وحفظ النسخة الاحتياطية...", fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ وتصدير نسخة احتياطية كاملة الآن 💾", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }

                    // Google Drive Backup Button
                    Button(
                        onClick = {
                            viewModel.triggerFullBackup { dbFile, _ ->
                                val driveIntent = com.example.util.BackupManager.createGoogleDriveUploadIntent(context, dbFile)
                                context.startActivity(driveIntent)
                            }
                        },
                        enabled = !isBackingUp,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير النسخة الاحتياطية إلى Google Drive أو الذاكرة 📁", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    // Google Drive Restore Button
                    Button(
                        onClick = {
                            restoreFromDriveLauncher.launch(
                                arrayOf(
                                    "*/*",
                                    "application/json",
                                    "application/octet-stream",
                                    "application/x-sqlite3"
                                )
                            )
                        },
                        enabled = !isBackingUp,
                        colors = ButtonDefaults.buttonColors(containerColor = SeaNavyDeep),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استرجاع نسخة احتياطية من ملف أو Google Drive 📂", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    // Secondary Backup Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.triggerFullBackup { _, jsonPath ->
                                    val jsonFile = java.io.File(jsonPath)
                                    if (jsonFile.exists()) {
                                        backupJsonText = jsonFile.readText()
                                        clipboardManager.setText(AnnotatedString(backupJsonText))
                                        showBackupJsonDialog = true
                                    }
                                }
                            },
                            enabled = !isBackingUp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير JSON كامل", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.triggerFullBackup { dbFile, _ ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/octet-stream"
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            dbFile
                                        )
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "نسخة قاعدة بيانات صنارة وشبكة - SQLite DB")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة أو حفظ ملف قاعدة البيانات .db"))
                                }
                            },
                            enabled = !isBackingUp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ ملف .DB خام", fontSize = 11.sp)
                        }
                    }

                    // Restore Button (Local Device)
                    if (deviceRole == "CASHIER") {
                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = AmberCoral, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استرجاع قاعدة البيانات (الكاشير الرئيسي ⚠️)", fontSize = 11.5.sp, color = AmberCoral, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("seller_local_restore_button")
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = AmberCoral, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استرجاع نسخة محلية لهذا الجهاز فقط 📱", fontSize = 11.5.sp, color = AmberCoral, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }


        // Native APK Download & Installation Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SeaTealPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تحميل وتثبيت تطبيق صنارة وشبكة (APK)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = "• التطبيق مبني بنظام أندرويد أصلي خالص بلغة Kotlin وJetpack Compose وقاعدة بيانات Room أوفلاين بالكامل.",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    // Requested Download Button
                    Button(
                        onClick = { showApkDownloadDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = SeaTealPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📲 تحميل وتثبيت التطبيق على موبايلي (APK)",
                            color = SeaTealPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Share Button
                    OutlinedButton(
                        onClick = {
                            val shareResult = ApkManager.shareRealApkFile(context)
                            shareResult.fold(
                                onSuccess = { shareIntent ->
                                    context.startActivity(shareIntent)
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "تعذر مشاركة ملف الـ APK: ${err.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة ملف الـ APK الفعلي عبر البلوتوث أو الواتساب", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Change Password Dialog
    if (showPasswordChangeDialog) {
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordChangeDialog = false },
            title = { Text("تغيير كلمة سر الإدارة", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "أدخل كلمة السر الجديدة لحماية العمليات الحساسة:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("كلمة السر الجديدة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPass.isNotBlank()) {
                            viewModel.saveSetting("managerPassword", newPass.trim())
                            showPasswordChangeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPasswordChangeDialog = false }) { Text("إلغاء") }
            }
        )
    }


    // Backup JSON Dialog
    if (showBackupJsonDialog) {
        AlertDialog(
            onDismissRequest = { showBackupJsonDialog = false },
            title = { Text("تم تصدير نسخة البيانات الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "تم نسخ كود النسخة الاحتياطية إلى الحافظة. يمكنك لصقه أو حفظه في ملف نصي على هاتفك لاسترجاعه عند الحاجة.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = backupJsonText,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBackupJsonDialog = false }) {
                    Text("تم")
                }
            }
        )
    }

    // Restore Database Dialog
    if (showRestoreDialog) {
        var restoreError by remember { mutableStateOf<String?>(null) }
        var isRestoring by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isRestoring) showRestoreDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = AmberCoral)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استرجاع قاعدة البيانات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showRestoreDialog = false
                            restoreFromDriveLauncher.launch(
                                arrayOf(
                                    "*/*",
                                    "application/json",
                                    "application/octet-stream",
                                    "application/x-sqlite3"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SeaNavyDeep),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📂 أو اختيار ملف النسخة من الذاكرة أو Google Drive مباشرة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "أو الصق محتوى كود JSON للنسخة في المربع التالي:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (deviceRole == "SELLER") {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ تنبيه: أنت تستخدم جهاز بائع. سيتم استبدال البيانات المحلية على هذا الجهاز فقط.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = {
                            restoreJsonInput = it
                            restoreError = null
                        },
                        placeholder = { Text("الصق كود النسخة الاحتياطية JSON هنا...", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 8
                    )

                    restoreError?.let { err ->
                        Text(
                            text = err,
                            color = StatusDanger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isRestoring) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ التحقق وتحديث الجداول وقاعدة البيانات...", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonInput.isBlank()) {
                            restoreError = "يرجى لصق كود النسخة الاحتياطية أولاً"
                            return@Button
                        }
                        if (deviceRole != "SELLER") {
                            pendingCashierRestoreJson = restoreJsonInput.trim()
                        } else {
                            isRestoring = true
                            viewModel.restoreFromJson(restoreJsonInput.trim()) { success, msg ->
                                isRestoring = false
                                if (success) {
                                    showRestoreDialog = false
                                    restoreJsonInput = ""
                                } else {
                                    restoreError = msg
                                }
                            }
                        }
                    },
                    enabled = !isRestoring && restoreJsonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                    modifier = Modifier.testTag("restore_json_submit_button")
                ) {
                    Text("تأكيد استرجاع الكود ⚠️", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRestoreDialog = false
                        restoreError = null
                    },
                    enabled = !isRestoring
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Direct APK Download & Installation Dialog
    if (showApkDownloadDialog) {
        ApkDownloadDialog(onDismiss = { showApkDownloadDialog = false })
    }
}
