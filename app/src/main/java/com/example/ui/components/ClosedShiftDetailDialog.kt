package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Shift
import com.example.data.repository.ShiftDrawerStatement
import com.example.data.repository.ShiftMovementItem
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClosedShiftDetailDialog(
    shift: Shift,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statement by remember { mutableStateOf<ShiftDrawerStatement?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: ملخص الداخل والخارج, 1: جدول الحركات التفصيلي, 2: الأوردرات والتسويات, 3: الموردين والموظفين

    val timeFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.US) }
    val hourFormat = remember { SimpleDateFormat("hh:mm a", Locale.US) }

    LaunchedEffect(shift.id) {
        scope.launch {
            isLoading = true
            statement = viewModel.getShiftDrawerStatement(shift.id)
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SeaTealPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = SeaTealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "بيان تقفيل الوردية #${shift.shiftNumber} (Z-Report)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "كاشير: ${shift.cashierName.ifBlank { "كاشير عام" }}",
                                fontSize = 11.5.sp,
                                color = SeaTealPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Shift duration info
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الفتح: ${timeFormat.format(Date(shift.openedAt))}",
                            fontSize = 10.5.sp,
                            color = Color.DarkGray
                        )
                        shift.closedAt?.let { closed ->
                            Text(
                                text = "الإغلاق: ${timeFormat.format(Date(closed))}",
                                fontSize = 10.5.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading || statement == null) {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        Text("جاري استخراج بيان حسابات الدرج والوردية...", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    val st = statement!!

                    // Navigation Tabs inside Dialog
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("ملخص الدرج", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("بيان الحركات (${st.movementsTimeline.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("المبيعات والتسويات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("الموردون والموظفون", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.weight(1f, fill = false).heightIn(max = 340.dp)) {
                        when (selectedTab) {
                            0 -> ShiftSummaryTabContent(st = st)
                            1 -> ShiftMovementsTimelineContent(movements = st.movementsTimeline, hourFormat = hourFormat)
                            2 -> ShiftSalesAndSettlementsContent(st = st)
                            3 -> ShiftSuppliersAndStaffContent(st = st)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action buttons (Print Z-Report & Share via WhatsApp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            statement?.let { st ->
                                val sb = StringBuilder()
                                sb.appendLine("🧾 *تقرير تقفيل الوردية #${st.shift.shiftNumber} (Z-Report)*")
                                sb.appendLine("👤 *كاشير:* ${st.shift.cashierName}")
                                sb.appendLine("⏰ *الفتح:* ${timeFormat.format(Date(st.shift.openedAt))}")
                                st.shift.closedAt?.let { sb.appendLine("🔒 *الإغلاق:* ${timeFormat.format(Date(it))}") }
                                sb.appendLine("━━━━━━━━━━━━━━━━━━━")
                                sb.appendLine("💵 *رصيد بداية الدرج (العهدة):* ${st.openingBalance} ج.م")
                                sb.appendLine("━━━━━━━━━━━━━━━━━━━")
                                sb.appendLine("🟢 *الوارد للدرج (المقبوضات):*")
                                sb.appendLine("• مبيعات كاش المحل (${st.storeCashOrders.size} أوردر): +${st.totalStoreCash} ج.م")
                                sb.appendLine("• توريدات تسويات الدليفري (${st.driverSettlements.size} تسوية): +${st.totalDriverSettlementsCash} ج.م")
                                if (st.totalAccountReceipts > 0) sb.appendLine("• مقبوضات عامة للدرج: +${st.totalAccountReceipts} ج.م")
                                sb.appendLine("➜ *إجمالي المقبوضات الداخلة:* +${st.totalIncomingCash - st.openingBalance} ج.م")
                                sb.appendLine("━━━━━━━━━━━━━━━━━━━")
                                sb.appendLine("🔴 *المنصرف من الدرج (المدفوعات):*")
                                sb.appendLine("• سلف ومسحوبات الموظفين (${st.staffWithdrawals.size}): -${st.totalStaffWithdrawals} ج.م")
                                sb.appendLine("• دفعات وفواتير الموردين (${st.supplierPayments.size}): -${st.totalSupplierPayments} ج.م")
                                if (st.otherExpenses > 0) sb.appendLine("• مصروفات ونثريات بالوردية: -${st.otherExpenses} ج.م")
                                sb.appendLine("➜ *إجمالي المدفوعات الخارجة:* -${st.totalOutgoingCash} ج.م")
                                sb.appendLine("━━━━━━━━━━━━━━━━━━━")
                                sb.appendLine("📱 *محافظ إلكترونية (فودافون كاش - خارج الدرج):* ${st.totalWalletSales} ج.م (${st.walletOrders.size} أوردر)")
                                sb.appendLine("━━━━━━━━━━━━━━━━━━━")
                                sb.appendLine("📊 *مطابقة الخزينة والدرج:*")
                                sb.appendLine("• المفروض بالدرج: *${st.expectedDrawerCash} ج.م*")
                                sb.appendLine("• الفعلي المستلم: *${st.actualDrawerCash} ج.م*")
                                val diff = st.difference
                                val diffStr = when {
                                    kotlin.math.abs(diff) < 0.01 -> "مطابق تماماً ✓"
                                    diff < 0 -> "عجز بالدرج: ${String.format(Locale.US, "%.1f", kotlin.math.abs(diff))} ج.م ⚠️"
                                    else -> "زيادة بالدرج: +${String.format(Locale.US, "%.1f", diff)} ج.م"
                                }
                                sb.appendLine("• الفارق المالي: *$diffStr*")
                                if (st.shift.notes.isNotBlank()) sb.appendLine("📝 *ملاحظات:* ${st.shift.notes}")

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير الوردية"))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة واتساب", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.printShiftStatement(shift.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طباعة تقرير Z-Report", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftSummaryTabContent(st: ShiftDrawerStatement) {
    MouseLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Opening Cash
        item {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SeaNavyDeep.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💵 رصيد بداية الدرج (العهدة):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${String.format(Locale.US, "%.1f", st.openingBalance)} ج.م", fontWeight = FontWeight.Bold, color = SeaNavyDeep, fontSize = 13.sp)
                }
            }
        }

        // Incoming to Drawer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SeaTealPrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🟢 الوارد للدرج (المقبوضات):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SeaTealPrimary)
                        Text("+${String.format(Locale.US, "%.1f", st.totalIncomingCash - st.openingBalance)} ج.م", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SeaTealPrimary)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• مبيعات كاش المحل (${st.storeCashOrders.size} أوردر):", fontSize = 11.sp, color = Color.DarkGray)
                        Text("+${String.format(Locale.US, "%.1f", st.totalStoreCash)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• توريدات تسويات الدليفري (${st.driverSettlements.size} تسوية):", fontSize = 11.sp, color = Color.DarkGray)
                        Text("+${String.format(Locale.US, "%.1f", st.totalDriverSettlementsCash)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (st.totalAccountReceipts > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• مقبوضات عامة للدرج (${st.accountReceipts.size}):", fontSize = 11.sp, color = Color.DarkGray)
                            Text("+${String.format(Locale.US, "%.1f", st.totalAccountReceipts)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Outgoing from Drawer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828).copy(alpha = 0.08f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🔴 المنصرف من الدرج (المدفوعات):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC62828))
                        Text("-${String.format(Locale.US, "%.1f", st.totalOutgoingCash)} ج.م", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC62828))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• سلف ومسحوبات موظفين (${st.staffWithdrawals.size}):", fontSize = 11.sp, color = Color.DarkGray)
                        Text("-${String.format(Locale.US, "%.1f", st.totalStaffWithdrawals)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• دفعات وفواتير موردين (${st.supplierPayments.size}):", fontSize = 11.sp, color = Color.DarkGray)
                        Text("-${String.format(Locale.US, "%.1f", st.totalSupplierPayments)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (st.otherExpenses > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• مصروفات ونثريات بالوردية:", fontSize = 11.sp, color = Color.DarkGray)
                            Text("-${String.format(Locale.US, "%.1f", st.otherExpenses)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Non-Drawer: Wallet Orders
        item {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AmberCoral.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("📱 مبيعات محافظ (فودافون كاش / خارج الدرج)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AmberCoral)
                        Text("${st.walletOrders.size} أوردرات تحويل إلكتروني لم تدخل الدرج", fontSize = 10.sp, color = Color.Gray)
                    }
                    Text("${String.format(Locale.US, "%.1f", st.totalWalletSales)} ج.م", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmberCoral)
                }
            }
        }

        // Drawer Reconciliation Summary
        item {
            val diff = st.difference
            val diffColor = if (kotlin.math.abs(diff) < 0.01) StatusSuccess else if (diff < 0) StatusDanger else Color(0xFF1565C0)
            val diffStatus = if (kotlin.math.abs(diff) < 0.01) "الدرج متطابق تماماً ✓" else if (diff < 0) "عجز بالدرج: ${String.format(Locale.US, "%.1f", kotlin.math.abs(diff))} ج.م ⚠️" else "زيادة بالدرج: +${String.format(Locale.US, "%.1f", diff)} ج.م +"

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = diffColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, diffColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "حالة رصيد الدرج والجرد الفعلي",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = diffStatus,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = diffColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("المفروض: ${String.format(Locale.US, "%.1f", st.expectedDrawerCash)} ج.م", fontSize = 11.sp, color = Color.DarkGray)
                        Text("الفعلي المسلم: ${String.format(Locale.US, "%.1f", st.actualDrawerCash)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftMovementsTimelineContent(
    movements: List<ShiftMovementItem>,
    hourFormat: SimpleDateFormat
) {
    if (movements.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            Text("لا توجد حركات مسجلة بالوردية", fontSize = 11.sp, color = Color.Gray)
        }
    } else {
        MouseLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(movements) { mov ->
                val isIn = mov.inAmount > 0
                val isOut = mov.outAmount > 0
                val amountText = if (isIn) "+${String.format(Locale.US, "%.1f", mov.inAmount)} ج.م"
                else if (isOut) "-${String.format(Locale.US, "%.1f", mov.outAmount)} ج.م"
                else "0.0 ج.م"

                val amountColor = if (isIn) StatusSuccess else if (isOut) StatusDanger else Color.Gray

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = hourFormat.format(Date(mov.time)),
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = mov.title,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (mov.subtitle.isNotBlank()) {
                                Text(
                                    text = mov.subtitle,
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                        Text(
                            text = amountText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftSalesAndSettlementsContent(st: ShiftDrawerStatement) {
    MouseLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Driver settlements section
        item {
            Text(
                text = "🛵 تسويات وتوريدات كباتن الدليفري (${st.driverSettlements.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = SeaTealPrimary
            )
        }
        if (st.driverSettlements.isEmpty()) {
            item { Text("لا توجد تسويات موصلين مسجلة في هذه الوردية", fontSize = 11.sp, color = Color.Gray) }
        } else {
            items(st.driverSettlements) { s ->
                val amt = if (s.actualHandoverAmount > 0.0) s.actualHandoverAmount else s.netAmountToHandover
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("كابتن: ${s.driverName} (${s.ordersCount} أوردر)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            Text("المحصل: ${s.totalCashCollected} ج | عمولة: ${s.driverEarnings} ج | بنزين: ${s.fuelAndExpenses} ج", fontSize = 10.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("+${String.format(Locale.US, "%.1f", amt)} ج.م", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusSuccess)
                            Text("توريد للدرج ✓", fontSize = 9.sp, color = SeaTealPrimary)
                        }
                    }
                }
            }
        }

        // Store Cash Orders
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🛍️ أوردرات كاش مباشرة بالمحل (${st.storeCashOrders.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = SeaNavyDeep
            )
        }
        if (st.storeCashOrders.isEmpty()) {
            item { Text("لا توجد أوردرات كاش محل في هذه الوردية", fontSize = 11.sp, color = Color.Gray) }
        } else {
            items(st.storeCashOrders) { ord ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${ord.order.orderNumber} - ${ord.order.customerName.ifBlank { "عميل صالة / تيك أواي" }}", fontSize = 11.sp)
                        Text("+${String.format(Locale.US, "%.1f", ord.order.totalAmount)} ج.م", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = StatusSuccess)
                    }
                }
            }
        }

        // Wallet Orders
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "📱 أوردرات محافظ إلكترونية (فودافون كاش - ${st.walletOrders.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = AmberCoral
            )
        }
        if (st.walletOrders.isEmpty()) {
            item { Text("لا توجد أوردرات محافظ في هذه الوردية", fontSize = 11.sp, color = Color.Gray) }
        } else {
            items(st.walletOrders) { ord ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AmberCoral.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${ord.order.orderNumber} - ${ord.order.customerName.ifBlank { "عميل محفظة" }}", fontSize = 11.sp)
                        Text("${String.format(Locale.US, "%.1f", ord.order.totalAmount)} ج.م", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = AmberCoral)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftSuppliersAndStaffContent(st: ShiftDrawerStatement) {
    MouseLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Staff withdrawals
        item {
            Text(
                text = "👥 سلف ومسحوبات الموظفين من الدرج (${st.staffWithdrawals.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFFC62828)
            )
        }
        if (st.staffWithdrawals.isEmpty()) {
            item { Text("لا توجد سلف أو مسحوبات موظفين مسجلة في هذه الوردية", fontSize = 11.sp, color = Color.Gray) }
        } else {
            items(st.staffWithdrawals) { (tx, empName) ->
                val typeName = if (tx.type == "SALARY_PAYOUT") "صرف راتب نقدي" else "سلفة نقدية"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("$empName ($typeName)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            Text(tx.reason.ifBlank { "سحب من الدرج" }, fontSize = 10.sp, color = Color.Gray)
                        }
                        Text("-${String.format(Locale.US, "%.1f", tx.amount)} ج.م", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusDanger)
                    }
                }
            }
        }

        // Supplier payments
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "📦 مدفوعات وفواتير الموردين من الدرج (${st.supplierPayments.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFFC62828)
            )
        }
        if (st.supplierPayments.isEmpty()) {
            item { Text("لا توجد مدفوعات موردين مسددة من الدرج في هذه الوردية", fontSize = 11.sp, color = Color.Gray) }
        } else {
            items(st.supplierPayments) { (tx, accName) ->
                val invStr = if (tx.invoiceNumber.isNotBlank()) " (فاتورة #${tx.invoiceNumber})" else ""
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("$accName$invStr", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            Text(tx.description.ifBlank { "سداد فاتورة/دفعة توريد" }, fontSize = 10.sp, color = Color.Gray)
                        }
                        Text("-${String.format(Locale.US, "%.1f", tx.amount)} ج.م", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusDanger)
                    }
                }
            }
        }
    }
}
