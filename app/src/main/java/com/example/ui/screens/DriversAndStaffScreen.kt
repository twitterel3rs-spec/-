package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
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
import com.example.ui.components.mouseVerticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Unarchive
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.data.model.OrderWithItems
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeliveryDriver
import com.example.data.model.DriverSettlement
import com.example.data.model.Employee
import com.example.ui.components.AttendanceAndDepartureDialog
import com.example.ui.components.AttendanceBoardTab
import com.example.ui.components.DriverStatementDialog
import com.example.ui.components.EmployeeStatementDialog
import com.example.ui.components.PasswordPromptDialog
import com.example.util.Time12hUtil
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DriversAndStaffScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val drivers by viewModel.allDriversIncludingArchived.collectAsStateWithLifecycle()
    val settlements by viewModel.settlements.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployeesIncludingArchived.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val todayAttendance by viewModel.todayAttendance.collectAsStateWithLifecycle()
    val transactions by viewModel.employeeTransactions.collectAsStateWithLifecycle()
    val savedOwnerPhone by viewModel.ownerWhatsAppPhone.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: كشوف حسابات الموصلين, 1: إدارة الموظفين والرواتب, 2: لوحة الحضور والغياب والتأخير
    var attDialogInitialMode by remember { mutableStateOf("CHECK_IN") }

    // Driver States
    var showNewDriverDialog by remember { mutableStateOf(false) }
    var driverToEdit by remember { mutableStateOf<DeliveryDriver?>(null) }
    var driverToDelete by remember { mutableStateOf<DeliveryDriver?>(null) }
    var driverToSettle by remember { mutableStateOf<DeliveryDriver?>(null) }
    var driverForStatement by remember { mutableStateOf<DeliveryDriver?>(null) }

    // Employee States
    var showNewEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<Employee?>(null) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }
    var employeeForTx by remember { mutableStateOf<Employee?>(null) }
    var employeeForAtt by remember { mutableStateOf<Employee?>(null) }
    var employeeForStatement by remember { mutableStateOf<Employee?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("الموصلون (${drivers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("الموظفون (${employees.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        text = "لوحة الحضور والغياب 📋",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (selectedTab == 2) SeaTealPrimary else Color.Unspecified
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            // Drivers Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "كباتن الدليفري والتوصيل:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Button(
                    onClick = { showNewDriverDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة كابتن توصيل", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            MouseLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(drivers.distinctBy { it.id }, key = { "driver_${it.id}" }) { driver ->
                    val isCommission = driver.calculationType == "COMMISSION"

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (driver.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(SeaTealPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.DeliveryDining, contentDescription = null, tint = SeaTealPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = driver.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            if (driver.isArchived) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(shape = RoundedCornerShape(4.dp), color = Color.Gray.copy(alpha = 0.2f)) {
                                                    Text(text = "مؤرشف", fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Text(text = driver.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isCommission) AmberCoral.copy(alpha = 0.15f) else StatusInfo.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isCommission) "عمولة: ${driver.ratePerOrder} ج/أوردر" else "يومية ثابتة: ${driver.fixedDailySalary} ج/يوم",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isCommission) AmberCoral else StatusInfo,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { driverToSettle = driver },
                                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تسوية اليوم", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { driverForStatement = driver },
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("كشف حساب مفصل", fontSize = 11.sp)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { driverToEdit = driver }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("تعديل", fontSize = 11.sp)
                                }

                                TextButton(onClick = { viewModel.archiveDriver(driver.id, !driver.isArchived) }) {
                                    Icon(
                                        imageVector = if (driver.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(if (driver.isArchived) "استعادة" else "أرشفة", fontSize = 11.sp)
                                }

                                IconButton(
                                    onClick = { driverToDelete = driver },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                if (settlements.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "سجل التسويات المقفلة مؤخراً (${settlements.size}):",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)

                    items(settlements.distinctBy { it.id }, key = { "settlement_${it.id}" }) { st ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "تسوية: ${st.driverName}", fontWeight = FontWeight.Bold)
                                    Text(text = dateFormat.format(Date(st.date)), fontSize = 11.sp, color = Color.Gray)
                                }
                                Text(
                                    text = "عدد الأوردرات: ${st.ordersCount} | إجمالي الكاش المحصل: ${String.format(Locale.US, "%.1f", st.totalCashCollected)} ج.م",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "مستحقات السائق: ${st.driverEarnings} ج | بنزين ومصروفات: ${st.fuelAndExpenses} ج",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "الصافي المسلم للمطعم:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", st.netAmountToHandover)} ج.م",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SeaTealPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        } else if (selectedTab == 1) {
            // Staff & Payroll Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "فريق العمل والمطبخ:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Button(
                    onClick = { showNewEmployeeDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة موظف", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            MouseLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(employees.distinctBy { it.id }, key = { "employee_${it.id}" }) { emp ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (emp.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = emp.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        if (emp.isArchived) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color.Gray.copy(alpha = 0.2f)) {
                                                Text(text = "مؤرشف", fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Text(text = "الوظيفة: ${emp.role} | هاتف: ${emp.phone}", fontSize = 11.sp, color = SeaTealPrimary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${emp.basicSalary} ج.م",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (emp.salaryType == "MONTHLY") "شهرياً" else "يومياً",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            val empDeductions = remember(transactions, emp.id) {
                                transactions.filter { it.employeeId == emp.id && it.type == "DEDUCTION" }.sumOf { it.amount }
                            }
                            val empAdvances = remember(transactions, emp.id) {
                                transactions.filter { it.employeeId == emp.id && it.type == "ADVANCE" }.sumOf { it.amount }
                            }
                            val empBonuses = remember(transactions, emp.id) {
                                transactions.filter { it.employeeId == emp.id && it.type == "BONUS" }.sumOf { it.amount }
                            }

                            if (empDeductions > 0 || empAdvances > 0 || empBonuses > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (empDeductions > 0) {
                                        Surface(
                                            color = Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "⚠️ جزاءات: ${String.format(Locale.US, "%.1f", empDeductions)} ج",
                                                color = Color(0xFF991B1B),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (empAdvances > 0) {
                                        Surface(
                                            color = Color(0xFFFFEDD5),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "💰 سلف: ${String.format(Locale.US, "%.1f", empAdvances)} ج",
                                                color = Color(0xFF9A3412),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (empBonuses > 0) {
                                        Surface(
                                            color = Color(0xFFDCFCE7),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "🟢 مكافآت: ${String.format(Locale.US, "%.1f", empBonuses)} ج",
                                                color = Color(0xFF166534),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // Attendance and financial actions in 2 spacious rows to eliminate overlapping text
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        employeeForAtt = emp
                                        attDialogInitialMode = "CHECK_IN"
                                    },
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(15.dp), tint = SeaTealPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تسجيل حضور / انصراف ⏰", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        employeeForAtt = emp
                                        attDialogInitialMode = "ABSENCE"
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                                    modifier = Modifier.weight(0.8f)
                                ) {
                                    Icon(imageVector = Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusDanger)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("تسجيل غياب 🚫", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { employeeForTx = emp },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("سلفة / مكافأة 💰", fontSize = 11.sp, maxLines = 1)
                                }

                                Button(
                                    onClick = { employeeForStatement = emp },
                                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("كشف الحساب والراتب 📄", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { employeeToEdit = emp }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("تعديل", fontSize = 11.sp)
                                }

                                TextButton(onClick = { viewModel.archiveEmployee(emp.id, !emp.isArchived) }) {
                                    Icon(
                                        imageVector = if (emp.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(if (emp.isArchived) "استعادة" else "أرشفة", fontSize = 11.sp)
                                }

                                IconButton(
                                    onClick = { employeeToDelete = emp },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Recent Transactions log
                if (transactions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "سجل السلف والخصومات والمكافآت (${transactions.size}):",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                    items(transactions.take(10).distinctBy { it.id }, key = { "tx_${it.id}" }) { tx ->
                        val txLabel = when (tx.type) {
                            "ADVANCE" -> "سلفة / سحب"
                            "DEDUCTION" -> "خصم / جزاء"
                            "BONUS" -> "مكافأة"
                            "SALARY_PAYOUT" -> "صرف راتب"
                            else -> tx.type
                        }
                        val txColor = when (tx.type) {
                            "ADVANCE" -> AmberCoral
                            "DEDUCTION" -> StatusDanger
                            "BONUS" -> StatusSuccess
                            else -> SeaTealPrimary
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "$txLabel - ${tx.reason}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = dateFormat.format(Date(tx.date)), fontSize = 10.sp, color = Color.Gray)
                                }
                                Text(
                                    text = "${tx.amount} ج.م",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = txColor
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        } else if (selectedTab == 2) {
            // Attendance, Absence & Delay Board (لوحة الغياب والحضور والتأخير)
            AttendanceBoardTab(
                employees = employees,
                todayAttendance = todayAttendance,
                allAttendance = attendance,
                savedOwnerPhone = savedOwnerPhone,
                onRecordAttendance = { emp, mode ->
                    employeeForAtt = emp
                    attDialogInitialMode = mode
                },
                onViewStatement = { emp ->
                    employeeForStatement = emp
                },
                onSaveOwnerPhone = { phone ->
                    viewModel.saveOwnerWhatsAppPhone(phone)
                }
            )
        }
    }

    // Driver Statement Dialog
    driverForStatement?.let { drv ->
        DriverStatementDialog(
            driver = drv,
            viewModel = viewModel,
            onDismiss = {
                driverForStatement = null
                viewModel.closeStatements()
            }
        )
    }

    // Employee Statement Dialog
    employeeForStatement?.let { emp ->
        EmployeeStatementDialog(
            employee = emp,
            viewModel = viewModel,
            onDismiss = {
                employeeForStatement = null
                viewModel.closeStatements()
            }
        )
    }

    // Driver Settlement Dialog
    driverToSettle?.let { drv ->
        DriverSettlementDialog(
            driver = drv,
            viewModel = viewModel,
            onConfirm = { fuel, actualHandover, notes ->
                viewModel.settleDriver(
                    driverId = drv.id,
                    fuelExpenses = fuel,
                    actualHandover = actualHandover,
                    notes = notes
                )
                driverToSettle = null
            },
            onDismiss = { driverToSettle = null }
        )
    }

    // Driver Edit Dialog
    if (showNewDriverDialog || driverToEdit != null) {
        val existing = driverToEdit
        DriverEditDialog(
            driver = existing,
            onSave = { updated ->
                viewModel.saveDriver(updated)
                showNewDriverDialog = false
                driverToEdit = null
            },
            onDismiss = {
                showNewDriverDialog = false
                driverToEdit = null
            }
        )
    }

    // Delete Driver with Password Protection
    driverToDelete?.let { targetDriver ->
        PasswordPromptDialog(
            title = "تأكيد حذف كابتن التوصيل: ${targetDriver.name}",
            message = "حذف السائق نهائياً يتطلب كلمة سر الإدارة.",
            onConfirm = { pass ->
                scope.launch {
                    val ok = viewModel.verifyManagerPassword(pass)
                    if (ok) {
                        viewModel.deleteDriver(targetDriver.id)
                        driverToDelete = null
                    } else {
                        viewModel.userMessage.value = "كلمة مرور الإدارة غير صحيحة"
                    }
                }
            },
            onDismiss = { driverToDelete = null }
        )
    }

    // Employee Edit Dialog
    if (showNewEmployeeDialog || employeeToEdit != null) {
        val existing = employeeToEdit
        EmployeeEditDialog(
            employee = existing,
            onSave = { updated ->
                viewModel.saveEmployee(updated)
                showNewEmployeeDialog = false
                employeeToEdit = null
            },
            onDismiss = {
                showNewEmployeeDialog = false
                employeeToEdit = null
            }
        )
    }

    // Delete Employee with Password Protection
    employeeToDelete?.let { targetEmp ->
        PasswordPromptDialog(
            title = "تأكيد حذف الموظف: ${targetEmp.name}",
            message = "حذف الموظف وسجلاته نهائياً يتطلب كلمة سر الإدارة.",
            onConfirm = { pass ->
                scope.launch {
                    val ok = viewModel.verifyManagerPassword(pass)
                    if (ok) {
                        viewModel.deleteEmployee(targetEmp.id)
                        employeeToDelete = null
                    } else {
                        viewModel.userMessage.value = "كلمة مرور الإدارة غير صحيحة"
                    }
                }
            },
            onDismiss = { employeeToDelete = null }
        )
    }

    // Attendance & Departure Dialog (With exact 12-hour format, late detection, and owner WhatsApp notification)
    employeeForAtt?.let { emp ->
        AttendanceAndDepartureDialog(
            employee = emp,
            viewModel = viewModel,
            initialMode = attDialogInitialMode,
            onDismiss = { employeeForAtt = null }
        )
    }

    // Staff Transaction Dialog
    employeeForTx?.let { emp ->
        EmployeeTransactionDialog(
            employee = emp,
            onConfirm = { type, amount, reason, date, fromDrawer ->
                viewModel.addEmployeeTransaction(
                    employeeId = emp.id,
                    type = type,
                    amount = amount,
                    reason = reason,
                    date = date,
                    isDeductedFromDrawer = fromDrawer
                )
                employeeForTx = null
            },
            onDismiss = { employeeForTx = null }
        )
    }
}

@Composable
fun DriverEditDialog(
    driver: DeliveryDriver?,
    onSave: (DeliveryDriver) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(driver?.name ?: "") }
    var phone by remember { mutableStateOf(driver?.phone ?: "") }
    var calcType by remember { mutableStateOf(driver?.calculationType ?: "COMMISSION") }
    var rateStr by remember { mutableStateOf(if (driver != null) driver.ratePerOrder.toString() else "15.0") }
    var dailySalaryStr by remember { mutableStateOf(if (driver != null) driver.fixedDailySalary.toString() else "150.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (driver == null) "إضافة كابتن توصيل جديد" else "تعديل بيانات كابتن: ${driver.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .mouseVerticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "نظام الحساب والعمولة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = calcType == "COMMISSION",
                        onClick = { calcType = "COMMISSION" },
                        label = { Text("عمولة لكل أوردر") }
                    )
                    FilterChip(
                        selected = calcType == "FIXED_DAILY",
                        onClick = { calcType = "FIXED_DAILY" },
                        label = { Text("يومية ثابتة") }
                    )
                }

                if (calcType == "COMMISSION") {
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("قيمة العمولة للأوردر الواحد (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = dailySalaryStr,
                        onValueChange = { dailySalaryStr = it },
                        label = { Text("قيمة اليومية الثابتة (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val r = rateStr.toDoubleOrNull() ?: 15.0
                    val d = dailySalaryStr.toDoubleOrNull() ?: 150.0
                    if (name.isNotBlank()) {
                        onSave(
                            DeliveryDriver(
                                id = driver?.id ?: 0L,
                                name = name.trim(),
                                phone = phone.trim(),
                                calculationType = calcType,
                                ratePerOrder = r,
                                fixedDailySalary = d,
                                isActive = true,
                                isArchived = driver?.isArchived ?: false
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun EmployeeEditDialog(
    employee: Employee?,
    onSave: (Employee) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(employee?.name ?: "") }
    var role by remember { mutableStateOf(employee?.role ?: "شيف أسماك وشواية") }
    var phone by remember { mutableStateOf(employee?.phone ?: "") }
    var salaryType by remember { mutableStateOf(employee?.salaryType ?: "MONTHLY") }
    var salaryStr by remember { mutableStateOf(if (employee != null) employee.basicSalary.toString() else "3500.0") }
    var dailyHoursStr by remember { mutableStateOf((if (employee != null && employee.dailyWorkHours > 0) employee.dailyWorkHours else 12.0).toString()) }
    var shiftStartStr by remember { mutableStateOf(Time12hUtil.format12h(employee?.shiftStartTime ?: "09:00 ص")) }
    var shiftEndStr by remember { mutableStateOf(Time12hUtil.format12h(employee?.shiftEndTime ?: "09:00 م")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (employee == null) "إضافة موظف جديد" else "تعديل موظف: ${employee.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .mouseVerticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الموظف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("الوظيفة (شيف أسماك / كاشير / قلي / صالة)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "نوع الراتب:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = salaryType == "MONTHLY",
                        onClick = { salaryType = "MONTHLY" },
                        label = { Text("راتب شهري") }
                    )
                    FilterChip(
                        selected = salaryType == "DAILY",
                        onClick = { salaryType = "DAILY" },
                        label = { Text("يومية") }
                    )
                }

                OutlinedTextField(
                    value = salaryStr,
                    onValueChange = { salaryStr = it },
                    label = { Text(if (salaryType == "MONTHLY") "الراتب الأساسي الشهري (ج.م)" else "قيمة اليومية (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dailyHoursStr,
                    onValueChange = { dailyHoursStr = it },
                    label = { Text("ساعات العمل اليومية الرسمية (الافتراضي 12 ساعة)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "مواعيد الوردية المقررة (نظام 12 ساعة مع ص / م):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shiftStartStr,
                        onValueChange = { shiftStartStr = it },
                        label = { Text("موعد الحضور (مثلاً 09:00 ص)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = shiftEndStr,
                        onValueChange = { shiftEndStr = it },
                        label = { Text("موعد الانصراف (مثلاً 09:00 م)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = salaryStr.toDoubleOrNull() ?: 3000.0
                    val h = dailyHoursStr.toDoubleOrNull() ?: 12.0
                    if (name.isNotBlank()) {
                        onSave(
                            Employee(
                                id = employee?.id ?: 0L,
                                name = name.trim(),
                                role = role.trim(),
                                phone = phone.trim(),
                                basicSalary = s,
                                salaryType = salaryType,
                                dailyWorkHours = h,
                                shiftStartTime = Time12hUtil.format12h(shiftStartStr.trim().ifBlank { "09:00 ص" }),
                                shiftEndTime = Time12hUtil.format12h(shiftEndStr.trim().ifBlank { "09:00 م" }),
                                isActive = true,
                                isArchived = employee?.isArchived ?: false
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun DriverSettlementDialog(
    driver: DeliveryDriver,
    viewModel: MainViewModel,
    onConfirm: (fuelExpenses: Double, actualHandover: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var fuelStr by remember { mutableStateOf("0") }
    var actualHandoverStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var todayOrders by remember { mutableStateOf<List<OrderWithItems>>(emptyList()) }
    var isLoadingOrders by remember { mutableStateOf(true) }

    LaunchedEffect(driver.id) {
        isLoadingOrders = true
        todayOrders = viewModel.getTodayUnsettledOrdersForDriver(driver.id)
        isLoadingOrders = false
    }

    val totalCashOrdersAmount = todayOrders
        .filter { !it.order.isExcludedFromDriverSettlement && (it.order.paymentMethod.isBlank() || it.order.paymentMethod.equals("CASH", ignoreCase = true)) }
        .sumOf { it.order.totalAmount }

    val totalCommission = if (driver.calculationType == "COMMISSION") {
        todayOrders.filter { !it.order.isExcludedFromDriverSettlement }.sumOf { it.order.driverCommission }
    } else {
        driver.fixedDailySalary
    }

    val fuel = fuelStr.toDoubleOrNull() ?: 0.0
    val calculatedNetDue = (totalCashOrdersAmount - totalCommission - fuel).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("تسوية وتصفية حساب: ${driver.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "نظام الحساب: ${if (driver.calculationType == "COMMISSION") "عمولة ${driver.ratePerOrder} ج/أوردر" else "يومية ثابتة ${driver.fixedDailySalary} ج"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .mouseVerticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SeaNavyDeep.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("أوردرات اليوم قيد التسوية:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${todayOrders.size} أوردر", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SeaTealPrimary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي كاش مطلوب تحصيله:", fontSize = 12.sp)
                            Text("${String.format(java.util.Locale.US, "%.1f", totalCashOrdersAmount)} ج.م", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مستحقات الكابتن (عمولة/يومية):", fontSize = 12.sp)
                            Text("- ${String.format(java.util.Locale.US, "%.1f", totalCommission)} ج.م", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberCoral)
                        }
                        if (fuel > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مصروفات بنزين وصيانة:", fontSize = 12.sp)
                                Text("- ${String.format(java.util.Locale.US, "%.1f", fuel)} ج.م", fontSize = 12.sp, color = StatusDanger)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("صافي النقدية المتوقع توريدها للدرج:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SeaNavyDeep)
                            Text("${String.format(java.util.Locale.US, "%.1f", calculatedNetDue)} ج.م", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                        }
                    }
                }

                // Archiving Notice Banner
                Surface(
                    color = StatusSuccess.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📦 عند تأكيد التسوية: سيتم توريد النقدية للدرج وأرشفة جميع أوردرات اليوم المسواة (${todayOrders.size} أوردر) تلقائياً لاعتماد محاسبة الكابتن عليها.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SeaNavyDeep,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Today's orders list
                Text(
                    text = "أوردرات اليوم المطلوب تسويتها وتصفيتها (${todayOrders.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = SeaNavyDeep
                )

                if (isLoadingOrders) {
                    Text("جاري تحميل أوردرات اليوم...", fontSize = 11.sp, color = Color.Gray)
                } else if (todayOrders.isEmpty()) {
                    Text("تمت تسوية وأرشفة جميع أوردرات الكابتن اليوم بنجاح ولا توجد أوردرات معلقة للمحاسبة ✓", fontSize = 11.sp, color = StatusSuccess, fontWeight = FontWeight.Bold)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .mouseVerticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        todayOrders.forEach { o ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (o.order.isArchived || o.order.isSettledWithDriver) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "أوردر #${o.order.orderNumber} - ${o.order.customerName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "${o.order.deliveryAddressFull ?: ""}".ifBlank { "طلب دليفري" },
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${o.order.totalAmount} ج (${if (o.order.paymentMethod.equals("WALLET", ignoreCase = true)) "محفظة" else "كاش"})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = SeaNavyDeep
                                        )
                                        Text(
                                            text = if (o.order.isArchived || o.order.isSettledWithDriver) "مؤرشف ✓" else "عمولة: ${o.order.driverCommission} ج",
                                            fontSize = 9.sp,
                                            color = if (o.order.isArchived || o.order.isSettledWithDriver) StatusSuccess else SeaTealPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Input fields
                OutlinedTextField(
                    value = fuelStr,
                    onValueChange = { fuelStr = it },
                    label = { Text("مصروفات البنزين والصيانة لليوم (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = actualHandoverStr,
                    onValueChange = { actualHandoverStr = it },
                    label = { Text("المبلغ المسلم فعلياً للكاشير (ج.م)") },
                    placeholder = { Text("المحسوب: ${String.format(java.util.Locale.US, "%.1f", calculatedNetDue)} ج.م") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات التسوية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val f = fuelStr.toDoubleOrNull() ?: 0.0
                    val actual = actualHandoverStr.toDoubleOrNull() ?: calculatedNetDue
                    onConfirm(f, actual, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("تأكيد التسوية وأرشفة الأوردرات ✓")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun AttendanceDialog(
    employee: Employee,
    onConfirm: (status: String, hours: Double, overtimeHours: Double, notes: String, date: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    var dateStr by remember { mutableStateOf(dateFormat.format(Date())) }
    var status by remember { mutableStateOf("PRESENT") }
    val defaultHours = if (employee.dailyWorkHours > 0) employee.dailyWorkHours else 12.0
    var hoursStr by remember { mutableStateOf(defaultHours.toString()) }
    var overtimeStr by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل حضور: ${employee.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("تاريخ تسجيل الحضور (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "الحالة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
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
                OutlinedTextField(
                    value = hoursStr,
                    onValueChange = { hoursStr = it },
                    label = { Text("ساعات العمل الأساسية (الرسمية 12 ساعة)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = overtimeStr,
                    onValueChange = { overtimeStr = it },
                    label = { Text("ساعات العمل الإضافي (Overtime)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (وقت الحضور والانصراف)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hoursStr.toDoubleOrNull() ?: defaultHours
                    val ot = overtimeStr.toDoubleOrNull() ?: 0.0
                    val parsedDate = try { dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                    onConfirm(status, h, ot, notes, parsedDate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun EmployeeTransactionDialog(
    employee: Employee,
    onConfirm: (type: String, amount: Double, reason: String, date: Long, isDeductedFromDrawer: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    var dateStr by remember { mutableStateOf(dateFormat.format(Date())) }
    var type by remember { mutableStateOf("ADVANCE") }
    var amountStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var isDeductedFromDrawer by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حركة مالية لـ ${employee.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("تاريخ الحركة المالية (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "نوع العملية:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val types = listOf("ADVANCE" to "سلفة / سحب", "DEDUCTION" to "خصم / جزاء", "BONUS" to "مكافأة")
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

                if (type == "ADVANCE") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDeductedFromDrawer) SeaTealPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDeductedFromDrawer) SeaTealPrimary else Color.LightGray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDeductedFromDrawer = !isDeductedFromDrawer }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isDeductedFromDrawer,
                                onCheckedChange = { isDeductedFromDrawer = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "صرف السلفة نقداً من درج الكاشير",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (isDeductedFromDrawer) SeaTealPrimary else Color.DarkGray
                                )
                                Text(
                                    text = "تنقص تلقائياً من عهدة ونقدية الوردية الحالية بالدرج",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val parsedDate = try { dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                    if (amt > 0 && reason.isNotBlank()) {
                        onConfirm(type, amt, reason, parsedDate, if (type == "ADVANCE") isDeductedFromDrawer else false)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ الحركة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
