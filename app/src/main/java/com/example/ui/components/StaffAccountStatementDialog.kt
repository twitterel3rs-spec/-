package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.data.model.OrderWithItems
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeliveryDriver
import com.example.data.model.Employee
import com.example.data.repository.DriverAccountStatement
import com.example.data.repository.EmployeeStatement
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.screens.DriverSettlementDialog
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DriverStatementDialog(
    driver: DeliveryDriver,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val statement by viewModel.activeDriverStatement.collectAsStateWithLifecycle()

    var selectedPeriodIndex by remember { mutableIntStateOf(0) } // 0: اليوم, 1: آخر 7 أيام, 2: هذا الشهر, 3: الكل
    var orderToExclude by remember { mutableStateOf<OrderWithItems?>(null) }
    var excludeReason by remember { mutableStateOf("استلم العميل بنفسه من المحل") }
    var showAddForgottenOrderDialog by remember { mutableStateOf(false) }
    var candidateOrders by remember { mutableStateOf<List<OrderWithItems>>(emptyList()) }
    var showSettlementDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun calculateTimestamps(index: Int): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return when (index) {
            0 -> Pair(cal.timeInMillis, now) // اليوم
            1 -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                Pair(cal.timeInMillis, now) // آخر 7 أيام
            }
            2 -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                Pair(cal.timeInMillis, now) // هذا الشهر
            }
            else -> Pair(0L, now) // الكل
        }
    }

    LaunchedEffect(selectedPeriodIndex) {
        val (start, end) = calculateTimestamps(selectedPeriodIndex)
        viewModel.loadDriverStatement(driver.id, start, end)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "كشف حساب كابتن: ${driver.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "نظام المحاسبة: ${if (driver.calculationType == "COMMISSION") "عمولة ${driver.ratePerOrder} ج/أوردر" else "يومية ${driver.fixedDailySalary} ج"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                // Period Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val periods = listOf("اليوم", "آخر 7 أيام", "هذا الشهر", "الكل")
                    periods.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedPeriodIndex == idx,
                            onClick = { selectedPeriodIndex = idx },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                statement?.let { st ->
                    MouseLazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            // Summary Cards
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SeaTealPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "ملخص الكشف المالي:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "إجمالي الأوردرات المسلمة:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                        Text(text = "${st.totalDeliveredOrders} أوردر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "إجمالي الكاش المحصل:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                        Text(text = "${String.format(Locale.US, "%.1f", st.totalCashCollected)} ج.م", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "مستحقات السائق (عمولة/يومية):", color = AmberCoral, fontSize = 11.sp)
                                        Text(text = "-${String.format(Locale.US, "%.1f", st.totalCommissionOrEarnings)} ج.م", color = AmberCoral, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    if (st.totalFuelAndExpenses > 0) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "مصروفات البنزين والعهد:", color = AmberCoral, fontSize = 11.sp)
                                            Text(text = "-${String.format(Locale.US, "%.1f", st.totalFuelAndExpenses)} ج.م", color = AmberCoral, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "الصافي المسلّم لخزينة المحل:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "${String.format(Locale.US, "%.1f", st.netHandoverToRestaurant)} ج.م", color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        item {
                            val unsettledOrders = st.orders.filter { !it.order.isSettledWithDriver && !it.order.isArchived && !it.order.isExcludedFromDriverSettlement }
                            val settledOrders = st.orders.filter { it.order.isSettledWithDriver || it.order.isArchived }

                            if (unsettledOrders.isNotEmpty()) {
                                Surface(
                                    color = AmberCoral.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, AmberCoral),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "🛵 يوجد ${unsettledOrders.size} أوردر جاري مع الكابتن بانتظار التسوية",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFFB45309)
                                            )
                                            Text(
                                                text = "المبلغ المطلوب تحصيله وتوريده: ${String.format(Locale.US, "%.1f", unsettledOrders.filter { it.order.paymentMethod == "CASH" }.sumOf { it.order.totalAmount })} ج.م",
                                                fontSize = 10.sp,
                                                color = Color(0xFF92400E)
                                            )
                                        }
                                        Button(
                                            onClick = { showSettlementDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("تسوية الآن", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else if (settledOrders.isNotEmpty()) {
                                Surface(
                                    color = StatusSuccess.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✓ جميع أوردرات الكابتن في هذه الفترة (${settledOrders.size} أوردر) تمت تسويتها ومؤرشفة بالكامل.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "سجل الأوردرات في هذه الفترة (${st.orders.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            candidateOrders = viewModel.getUnsettledDeliveryOrders().filter { it.order.driverId != driver.id }
                                            showAddForgottenOrderDialog = true
                                        }
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("➕ إضافة أوردر منسي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (st.orders.isEmpty()) {
                            item {
                                Text(
                                    text = "لا توجد أوردرات مسندة لهذا السائق خلال الفترة المحددة",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }

                        items(st.orders) { o ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (o.order.isExcludedFromDriverSettlement) Color(0xFFFFF1F2) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "أوردر #${o.order.orderNumber} - ${o.order.customerName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            if (o.order.isExcludedFromDriverSettlement) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = StatusDanger.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(text = "مستبعد من الحساب", color = StatusDanger, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            } else if (o.order.isSettledWithDriver || o.order.isArchived) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = StatusSuccess.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(text = "مؤرشف ومحاسب عليه ✓", color = StatusSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = AmberCoral.copy(alpha = 0.18f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(text = "مع الكابتن (معلق للتسوية) 🛵", color = Color(0xFFB45309), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            }
                                        }
                                        Text(text = "توصيل: ${o.order.deliveryAddressFull ?: ""}", fontSize = 10.sp, color = Color.Gray)
                                        if (o.order.isExcludedFromDriverSettlement && !o.order.driverSettlementAdjustmentReason.isNullOrBlank()) {
                                            Text(text = "سبب الاستبعاد: ${o.order.driverSettlementAdjustmentReason}", fontSize = 9.sp, color = StatusDanger)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${o.order.totalAmount} ج (${if (o.order.paymentMethod == "CASH") "كاش" else "محفظة"})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "عمولة: ${o.order.driverCommission} ج",
                                                fontSize = 9.sp,
                                                color = SeaTealPrimary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        if (o.order.isExcludedFromDriverSettlement) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.excludeOrderFromDriver(o.order.id, false, null)
                                                    val (start, end) = calculateTimestamps(selectedPeriodIndex)
                                                    viewModel.loadDriverStatement(driver.id, start, end)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = "استعادة للحساب", tint = StatusSuccess, modifier = Modifier.size(16.dp))
                                            }
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    orderToExclude = o
                                                    excludeReason = "استلم العميل بنفسه من المحل"
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "استبعاد من الحساب", tint = StatusDanger, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showSettlementDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تسوية وأرشفة الأوردرات", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                OutlinedButton(
                    onClick = {
                        statement?.let { st ->
                            val text = """
                                ===============================
                                كشف حساب كابتن توصيل: ${driver.name}
                                الفترة: ${when(selectedPeriodIndex) { 0 -> "اليوم"; 1 -> "آخر 7 أيام"; 2 -> "هذا الشهر"; else -> "الكل" }}
                                عدد الأوردرات المسلمة: ${st.totalDeliveredOrders}
                                إجمالي الكاش المحصل: ${st.totalCashCollected} ج.م
                                مستحقات السائق: ${st.totalCommissionOrEarnings} ج.م
                                بنزين ومصروفات: ${st.totalFuelAndExpenses} ج.م
                                الصافي المسلم للمحل: ${st.netHandoverToRestaurant} ج.م
                                ===============================
                            """.trimIndent()
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "تم نسخ كشف الحساب للحافظة للمشاركة والطباعة", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ / طباعة الكشف", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                Button(onClick = onDismiss) {
                    Text("إغلاق")
                }
            }
        },
        dismissButton = {}
    )

    if (showSettlementDialog) {
        DriverSettlementDialog(
            driver = driver,
            viewModel = viewModel,
            onConfirm = { fuel, actual, notes ->
                viewModel.settleDriver(
                    driverId = driver.id,
                    fuelExpenses = fuel,
                    actualHandover = actual,
                    notes = notes
                )
                showSettlementDialog = false
                val (start, end) = calculateTimestamps(selectedPeriodIndex)
                viewModel.loadDriverStatement(driver.id, start, end)
            },
            onDismiss = { showSettlementDialog = false }
        )
    }

    // Exclude Order Dialog (e.g., customer took it directly or cancelled)
    orderToExclude?.let { targetOrder ->
        AlertDialog(
            onDismissRequest = { orderToExclude = null },
            title = { Text("استبعاد أوردر من حساب الطيار", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "سيتم استبعاد أوردر #${targetOrder.order.orderNumber} (مبلغ: ${targetOrder.order.totalAmount} ج) من عمولة وتحصيل هذا الطيار دون مسحه من سجل المطعم.",
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = excludeReason,
                        onValueChange = { excludeReason = it },
                        label = { Text("سبب الاستبعاد (مثال: استلم العميل بنفسه من المحل)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.excludeOrderFromDriver(targetOrder.order.id, true, excludeReason.trim())
                        orderToExclude = null
                        val (start, end) = calculateTimestamps(selectedPeriodIndex)
                        viewModel.loadDriverStatement(driver.id, start, end)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text("تأكيد الاستبعاد")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { orderToExclude = null }) { Text("إلغاء") }
            }
        )
    }

    // Add Forgotten Order Dialog
    if (showAddForgottenOrderDialog) {
        AlertDialog(
            onDismissRequest = { showAddForgottenOrderDialog = false },
            title = { Text("إضافة أوردر منسي لحساب الطيار", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "اختر الأوردر المنسي لضمه مباشرة لحساب الكابتن «${driver.name}» واحتساب عمولته (${driver.ratePerOrder} ج):",
                        fontSize = 12.sp
                    )

                    if (candidateOrders.isEmpty()) {
                        Text(
                            text = "لا توجد أوردرات دليفري غير مسندة أو معلقة حالياً.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        MouseLazyColumn(modifier = Modifier.fillMaxWidth().height(220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(candidateOrders) { candidate ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "أوردر #${candidate.order.orderNumber} - ${candidate.order.customerName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(text = "العنوان: ${candidate.order.deliveryAddressFull ?: ""}", fontSize = 10.sp, color = Color.Gray)
                                            Text(text = "المبلغ: ${candidate.order.totalAmount} ج.م", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SeaTealPrimary)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.assignDriverToOrder(
                                                    candidate.order.id,
                                                    driver.id,
                                                    driver.name,
                                                    driver.ratePerOrder,
                                                    "إضافة أوردر منسي أثناء تقفيل الحساب"
                                                )
                                                showAddForgottenOrderDialog = false
                                                val (start, end) = calculateTimestamps(selectedPeriodIndex)
                                                viewModel.loadDriverStatement(driver.id, start, end)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("ضم للكشف", fontSize = 10.sp)
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
                OutlinedButton(onClick = { showAddForgottenOrderDialog = false }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
fun EmployeeStatementDialog(
    employee: Employee,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val statement by viewModel.activeEmployeeStatement.collectAsStateWithLifecycle()

    var selectedPeriodIndex by remember { mutableIntStateOf(2) } // 0: اليوم, 1: آخر 7 أيام, 2: هذا الشهر, 3: الكل, 4: فترة مخصصة
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: الملخص, 1: العمليات المالية, 2: سجل الحضور
    var txTypeFilter by remember { mutableStateOf("ALL") } // ALL, DEDUCTION, ADVANCE, BONUS, SALARY_PAYOUT

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val timeFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)

    var customStartDateStr by remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        mutableStateOf(dateFormat.format(cal.time))
    }
    var customEndDateStr by remember {
        mutableStateOf(dateFormat.format(Date()))
    }

    // Record editing states
    var transactionToEdit by remember { mutableStateOf<com.example.data.model.EmployeeTransaction?>(null) }
    var attendanceToEdit by remember { mutableStateOf<com.example.data.model.EmployeeAttendance?>(null) }
    var transactionToDelete by remember { mutableStateOf<com.example.data.model.EmployeeTransaction?>(null) }
    var attendanceToDelete by remember { mutableStateOf<com.example.data.model.EmployeeAttendance?>(null) }
    var showSettleDialog by remember { mutableStateOf(false) }

    fun calculateTimestamps(index: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val endOfDayCal = Calendar.getInstance()
        endOfDayCal.set(Calendar.HOUR_OF_DAY, 23)
        endOfDayCal.set(Calendar.MINUTE, 59)
        endOfDayCal.set(Calendar.SECOND, 59)
        endOfDayCal.set(Calendar.MILLISECOND, 999)

        return when (index) {
            0 -> Pair(cal.timeInMillis, endOfDayCal.timeInMillis) // اليوم
            1 -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                Pair(cal.timeInMillis, endOfDayCal.timeInMillis) // آخر 7 أيام
            }
            2 -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                Pair(cal.timeInMillis, endOfDayCal.timeInMillis) // هذا الشهر
            }
            3 -> Pair(0L, endOfDayCal.timeInMillis + 86400000L * 365) // الكل
            4 -> {
                val s = try { dateFormat.parse(customStartDateStr)?.time ?: 0L } catch (e: Exception) { 0L }
                val eCal = Calendar.getInstance()
                val e = try {
                    val parsed = dateFormat.parse(customEndDateStr)
                    if (parsed != null) {
                        eCal.time = parsed
                        eCal.set(Calendar.HOUR_OF_DAY, 23)
                        eCal.set(Calendar.MINUTE, 59)
                        eCal.set(Calendar.SECOND, 59)
                        eCal.timeInMillis
                    } else endOfDayCal.timeInMillis
                } catch (ex: Exception) { endOfDayCal.timeInMillis }
                Pair(s, e)
            }
            else -> Pair(0L, endOfDayCal.timeInMillis)
        }
    }

    LaunchedEffect(selectedPeriodIndex, customStartDateStr, customEndDateStr) {
        val (start, end) = calculateTimestamps(selectedPeriodIndex)
        viewModel.loadEmployeeStatement(employee.id, start, end)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "دفتر حسابات الموظف: ${employee.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "الوظيفة: ${employee.role} | الراتب: ${employee.basicSalary} ج (${if (employee.salaryType == "MONTHLY") "شهري" else "يومي"})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                // Period Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val periods = listOf("اليوم", "7 أيام", "هذا الشهر", "الكل", "مخصص 📅")
                    periods.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedPeriodIndex == idx,
                            onClick = { selectedPeriodIndex = idx },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }

                // Custom Date Range Inputs
                if (selectedPeriodIndex == 4) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customStartDateStr,
                            onValueChange = { customStartDateStr = it },
                            label = { Text("من تاريخ (yyyy-MM-dd)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = customEndDateStr,
                            onValueChange = { customEndDateStr = it },
                            label = { Text("إلى تاريخ (yyyy-MM-dd)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        Button(
                            onClick = {
                                val (s, e) = calculateTimestamps(4)
                                viewModel.loadEmployeeStatement(employee.id, s, e)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("تطبيق", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Inner Tabs for Ledger Sections
                TabRow(
                    selectedTabIndex = selectedSubTab,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedSubTab == 0,
                        onClick = { selectedSubTab = 0 },
                        text = { Text("المستحقات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSubTab == 1,
                        onClick = { selectedSubTab = 1 },
                        text = { Text("الماليات (${statement?.transactions?.size ?: 0})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSubTab == 2,
                        onClick = { selectedSubTab = 2 },
                        text = { Text("الحضور والغياب (${statement?.attendanceRecords?.size ?: 0})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                statement?.let { st ->
                    val filteredTxList = when (txTypeFilter) {
                        "DEDUCTION" -> st.transactions.filter { it.type == "DEDUCTION" }
                        "ADVANCE" -> st.transactions.filter { it.type == "ADVANCE" }
                        "BONUS" -> st.transactions.filter { it.type == "BONUS" }
                        "SALARY_PAYOUT" -> st.transactions.filter { it.type == "SALARY_PAYOUT" }
                        else -> st.transactions
                    }

                    MouseLazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedSubTab == 0) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SeaTealPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(text = "كشف تصفية الراتب والمستحقات:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "أيام الحضور والغياب:", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                                            Text(text = "${st.presentDaysCount} حضور | ${st.absentDaysCount} غياب | ${st.lateDaysCount} تأخير", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "ساعات العمل الأساسية:", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                                            Text(text = "${st.totalHoursWorked} ساعة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        if (st.totalOvertimeHours > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(text = "ساعات إضافي (+):", color = Color(0xFFA7F3D0), fontSize = 11.sp)
                                                Text(text = "${st.totalOvertimeHours} ساعة (+${String.format(Locale.US, "%.1f", st.overtimePay)} ج)", color = Color(0xFFA7F3D0), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            val workedDaysCount = st.presentDaysCount + st.lateDaysCount
                                            Text(text = "الراتب المستحق عن ($workedDaysCount يوم عمل فعلي):", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                                            Text(text = "${String.format(Locale.US, "%.1f", st.totalEarnedSalary)} ج.م", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        if (st.totalBonuses > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(text = "المكافآت المضافة (+):", color = Color(0xFFA7F3D0), fontSize = 11.sp)
                                                Text(text = "+${String.format(Locale.US, "%.1f", st.totalBonuses)} ج.م", color = Color(0xFFA7F3D0), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "إجمالي السلف والسحوبات (-):", color = Color(0xFFFFEDD5), fontSize = 11.sp)
                                            Text(text = "-${String.format(Locale.US, "%.1f", st.totalAdvances)} ج.م", color = Color(0xFFFFEDD5), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "إجمالي الخصومات والجزاءات والتأخير (-):", color = Color(0xFFFECACA), fontSize = 11.sp)
                                            val allDeductions = st.totalDeductions + st.delayDeductions
                                            Text(text = "-${String.format(Locale.US, "%.1f", allDeductions)} ج.م", color = Color(0xFFFECACA), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "صافي المستحق للصرف والتصفية:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = "${String.format(Locale.US, "%.1f", st.netEntitlement)} ج.م", color = Color(0xFFA7F3D0), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { showSettleDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تقفيل وتسوية الحساب النهائي للموظف", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (selectedSubTab == 1) {
                            // Filterable Transactions list (Penalties/Deductions, Advances, Bonuses, Payouts)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .mouseHorizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterChip(
                                        selected = txTypeFilter == "ALL",
                                        onClick = { txTypeFilter = "ALL" },
                                        label = { Text("الكل (${st.transactions.size})", fontSize = 10.sp) }
                                    )
                                    FilterChip(
                                        selected = txTypeFilter == "DEDUCTION",
                                        onClick = { txTypeFilter = "DEDUCTION" },
                                        label = { Text("⚠️ جزاءات وخصومات (${st.transactions.count { it.type == "DEDUCTION" }})", fontSize = 10.sp) }
                                    )
                                    FilterChip(
                                        selected = txTypeFilter == "ADVANCE",
                                        onClick = { txTypeFilter = "ADVANCE" },
                                        label = { Text("💰 سلف (${st.transactions.count { it.type == "ADVANCE" }})", fontSize = 10.sp) }
                                    )
                                    FilterChip(
                                        selected = txTypeFilter == "BONUS",
                                        onClick = { txTypeFilter = "BONUS" },
                                        label = { Text("🟢 مكافآت (${st.transactions.count { it.type == "BONUS" }})", fontSize = 10.sp) }
                                    )
                                    FilterChip(
                                        selected = txTypeFilter == "SALARY_PAYOUT",
                                        onClick = { txTypeFilter = "SALARY_PAYOUT" },
                                        label = { Text("📄 تسويات (${st.transactions.count { it.type == "SALARY_PAYOUT" }})", fontSize = 10.sp) }
                                    )
                                }
                            }

                            if (filteredTxList.isEmpty()) {
                                item {
                                    Text(
                                        text = if (txTypeFilter == "DEDUCTION") "لا توجد جزاءات أو خصومات مسجلة في هذه الفترة" else "لا توجد عمليات مسجلة في هذا التصنيف",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                            items(filteredTxList) { tx ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${when(tx.type) { "ADVANCE" -> "سلفة / سحب"; "DEDUCTION" -> "خصم / جزاء"; "BONUS" -> "مكافأة"; "SALARY_PAYOUT" -> "تقفيل وتسوية راتب"; else -> tx.type }} - ${tx.reason}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(text = timeFormat.format(Date(tx.date)), fontSize = 10.sp, color = Color.Gray)
                                            Text(
                                                text = "${tx.amount} ج.م",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (tx.type == "BONUS") StatusSuccess else if (tx.type == "SALARY_PAYOUT") SeaTealPrimary else StatusDanger
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { transactionToEdit = tx }, modifier = Modifier.size(32.dp)) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = SeaTealPrimary, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { transactionToDelete = tx }, modifier = Modifier.size(32.dp)) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = StatusDanger, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Attendance list with Edit and Delete
                            if (st.attendanceRecords.isEmpty()) {
                                item {
                                    Text(text = "لا توجد سجلات حضور مسجلة خلال هذه الفترة", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                                }
                            }
                            items(st.attendanceRecords) { att ->
                                val statusLabel = when(att.status) {
                                    "PRESENT" -> "حضور ✓"
                                    "ABSENT" -> "غياب ✗"
                                    "LATE" -> "تأخير ⏰ (${att.delayMinutes} دقيقة)"
                                    "LEAVE" -> "إجازة 🏖️"
                                    else -> att.status
                                }
                                val statusColor = when(att.status) {
                                    "PRESENT" -> StatusSuccess
                                    "ABSENT" -> StatusDanger
                                    "LATE" -> AmberCoral
                                    else -> Color.Gray
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = dateFormat.format(Date(att.date)), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = statusColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(text = statusLabel, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                                if (att.delayMinutes > 0) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = StatusDanger.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(text = "تأخير ${att.delayMinutes} د", color = StatusDanger, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                    }
                                                }
                                                if (!att.lateActionTaken.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    val actionLabel = when (att.lateActionTaken) {
                                                        "PARDON" -> "سماح ✓"
                                                        "PENALTY" -> "جزا ${att.penaltyAmount}ج ⚠️"
                                                        "OWNER_NOTIFIED" -> "إبلاغ الأونر 🐦"
                                                        else -> att.lateActionTaken
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFFEF3C7)
                                                    ) {
                                                        Text(text = actionLabel, color = Color(0xFF92400E), fontWeight = FontWeight.Bold, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            val timeDetails = buildString {
                                                if (att.checkInTimeString.isNotBlank()) append("🕒 حضور: ${com.example.util.Time12hUtil.format12h(att.checkInTimeString)} ")
                                                if (att.checkOutTimeString.isNotBlank()) append("🚪 انصراف: ${com.example.util.Time12hUtil.format12h(att.checkOutTimeString)} ")
                                                append("| الساعات: ${att.hoursWorked}س ")
                                                if (att.overtimeHours > 0) append("(+${att.overtimeHours}س إضافي) ")
                                                if (att.notes.isNotBlank()) append("| ${att.notes}")
                                            }
                                            Text(
                                                text = timeDetails,
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { attendanceToEdit = att }, modifier = Modifier.size(32.dp)) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = SeaTealPrimary, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { attendanceToDelete = att }, modifier = Modifier.size(32.dp)) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = StatusDanger, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        statement?.let { st ->
                            val text = """
                                ===============================
                                كشف حساب موظف: ${employee.name}
                                الوظيفة: ${employee.role}
                                الفترة: ${dateFormat.format(Date(st.startDate))} إلى ${dateFormat.format(Date(st.endDate))}
                                أيام الحضور: ${st.presentDaysCount} | أيام الغياب: ${st.absentDaysCount}
                                ساعات العمل: ${st.totalHoursWorked}
                                الراتب المستحق: ${st.totalEarnedSalary} ج.م
                                المكافآت: ${st.totalBonuses} ج.م
                                السلف والسحوبات: ${st.totalAdvances} ج.م
                                الخصومات والغياب: ${st.totalDeductions + st.absenceDeductions} ج.م
                                صافي المستحق للصرف: ${st.netEntitlement} ج.م
                                ===============================
                            """.trimIndent()
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "تم نسخ كشف الحساب للحافظة للمشاركة والطباعة", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ الكشف", fontSize = 11.sp)
                }

                Button(onClick = onDismiss) {
                    Text("إغلاق")
                }
            }
        },
        dismissButton = {}
    )

    // Edit Transaction Dialog
    transactionToEdit?.let { tx ->
        EditTransactionRecordDialog(
            transaction = tx,
            onSave = { updated ->
                viewModel.updateEmployeeTransaction(updated)
                transactionToEdit = null
            },
            onDismiss = { transactionToEdit = null }
        )
    }

    // Delete Transaction Confirmation
    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("حذف العملية المالية", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد بالتأكيد حذف هذه الحركة المالية (${tx.amount} ج.م - ${tx.reason})؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmployeeTransaction(tx.id, employee.id)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text("نعم، حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { transactionToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    // Edit Attendance Dialog
    attendanceToEdit?.let { att ->
        EditAttendanceRecordDialog(
            attendance = att,
            onSave = { updated ->
                viewModel.updateAttendance(updated)
                attendanceToEdit = null
            },
            onDismiss = { attendanceToEdit = null }
        )
    }

    // Delete Attendance Confirmation
    attendanceToDelete?.let { att ->
        AlertDialog(
            onDismissRequest = { attendanceToDelete = null },
            title = { Text("حذف سجل الحضور", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد حذف سجل حضور يوم ${dateFormat.format(Date(att.date))} لهذا الموظف؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAttendance(att.id, employee.id)
                        attendanceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text("نعم، حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { attendanceToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    // Settle Salary Payout Dialog
    if (showSettleDialog && statement != null) {
        val st = statement!!
        var payoutDateStr by remember { mutableStateOf(dateFormat.format(Date())) }
        var payoutAmountStr by remember(st.netEntitlement) { mutableStateOf(String.format(Locale.US, "%.1f", st.netEntitlement)) }
        var payoutNote by remember { mutableStateOf("تقفيل وتسوية راتب الفترة") }

        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            title = { Text("تقفيل وتصفية حساب: ${employee.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .mouseVerticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val days = st.presentDaysCount + st.lateDaysCount
                    Text(text = "صافي الراتب المستحق عن ($days يوم عمل مسجل): ${String.format(Locale.US, "%.1f", st.netEntitlement)} ج.م", fontWeight = FontWeight.Bold, color = SeaTealPrimary)
                    OutlinedTextField(
                        value = payoutDateStr,
                        onValueChange = { payoutDateStr = it },
                        label = { Text("تاريخ الصرف والتقفيل (yyyy-MM-dd)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = payoutAmountStr,
                        onValueChange = { payoutAmountStr = it },
                        label = { Text("المبلغ المنصرف فعلياً (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = payoutNote,
                        onValueChange = { payoutNote = it },
                        label = { Text("ملاحظات تقفيل الحساب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payoutAmountStr.toDoubleOrNull() ?: st.netEntitlement
                        val parsedDate = try { dateFormat.parse(payoutDateStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                        viewModel.settleEmployeeSalary(employee.id, amount, payoutNote, parsedDate)
                        showSettleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("تأكيد الصرف وتقفيل الحساب")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSettleDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun EditTransactionRecordDialog(
    transaction: com.example.data.model.EmployeeTransaction,
    onSave: (com.example.data.model.EmployeeTransaction) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var txDateStr by remember { mutableStateOf(dateFormat.format(Date(transaction.date))) }
    var type by remember { mutableStateOf(transaction.type) }
    var amountStr by remember { mutableStateOf(transaction.amount.toString()) }
    var reason by remember { mutableStateOf(transaction.reason) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل حركة مالية", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .mouseVerticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = txDateStr,
                    onValueChange = { txDateStr = it },
                    label = { Text("تاريخ الحركة (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val types = listOf("ADVANCE" to "سلفة", "DEDUCTION" to "خصم", "BONUS" to "مكافأة")
                    types.forEach { (tKey, tLabel) ->
                        FilterChip(
                            selected = type == tKey,
                            onClick = { type = tKey },
                            label = { Text(tLabel, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("السبب أو البيان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val a = amountStr.toDoubleOrNull() ?: transaction.amount
                    val parsedDate = try { dateFormat.parse(txDateStr)?.time ?: transaction.date } catch (e: Exception) { transaction.date }
                    onSave(transaction.copy(type = type, amount = a, reason = reason.trim(), date = parsedDate))
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ التعديل")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun EditAttendanceRecordDialog(
    attendance: com.example.data.model.EmployeeAttendance,
    onSave: (com.example.data.model.EmployeeAttendance) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var attDateStr by remember { mutableStateOf(dateFormat.format(Date(attendance.date))) }
    var status by remember { mutableStateOf(attendance.status) }
    var hoursStr by remember { mutableStateOf(attendance.hoursWorked.toString()) }
    var overtimeStr by remember { mutableStateOf(attendance.overtimeHours.toString()) }
    var delayMinutesStr by remember { mutableStateOf(attendance.delayMinutes.toString()) }
    var notes by remember { mutableStateOf(attendance.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل سجل الحضور والغياب", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .mouseVerticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = attDateStr,
                    onValueChange = { attDateStr = it },
                    label = { Text("تاريخ الحضور والغياب (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val statuses = listOf("PRESENT" to "حضور ✓", "ABSENT" to "غياب ✗", "LATE" to "تأخير ⏰", "LEAVE" to "إجازة 🏖️")
                    statuses.forEach { (sKey, sLabel) ->
                        FilterChip(
                            selected = status == sKey,
                            onClick = { status = sKey },
                            label = { Text(sLabel, fontSize = 11.sp) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = hoursStr,
                        onValueChange = { hoursStr = it },
                        label = { Text("ساعات العمل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = overtimeStr,
                        onValueChange = { overtimeStr = it },
                        label = { Text("إضافي (ساعة)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = delayMinutesStr,
                    onValueChange = { delayMinutesStr = it },
                    label = { Text("دقائق التأخير إن وجدت") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hoursStr.toDoubleOrNull() ?: attendance.hoursWorked
                    val ot = overtimeStr.toDoubleOrNull() ?: attendance.overtimeHours
                    val d = delayMinutesStr.toIntOrNull() ?: attendance.delayMinutes
                    val parsedDate = try { dateFormat.parse(attDateStr)?.time ?: attendance.date } catch (e: Exception) { attendance.date }
                    onSave(
                        attendance.copy(
                            date = parsedDate,
                            status = status,
                            hoursWorked = h,
                            overtimeHours = ot,
                            delayMinutes = d,
                            notes = notes.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ التعديل")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
