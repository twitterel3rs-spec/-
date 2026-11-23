package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.example.ui.components.mouseHorizontalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeliveryDriver
import com.example.data.model.Employee
import com.example.data.model.GeneralAccount
import com.example.data.model.Shift
import com.example.ui.components.AttendanceAndDepartureDialog
import com.example.ui.components.AttendanceBoardTab
import com.example.ui.components.ClosedShiftDetailDialog
import com.example.ui.components.DriverStatementDialog
import com.example.ui.components.EmployeeStatementDialog
import com.example.ui.components.GeneralAccountEditDialog
import com.example.ui.components.GeneralAccountStatementDialog
import com.example.ui.components.GeneralAccountTransactionDialog
import com.example.ui.components.PasswordPromptDialog
import com.example.ui.components.getAccountTypeBadgeColor
import com.example.ui.components.getAccountTypeLabel
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
fun AccountsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Accounts State
    val accounts by viewModel.allAccountsIncludingArchived.collectAsStateWithLifecycle()
    val allShifts by viewModel.allShifts.collectAsStateWithLifecycle()
    val closedShifts = remember(allShifts) { allShifts.filter { it.isClosed } }
    val drivers by viewModel.allDriversIncludingArchived.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployeesIncludingArchived.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val todayAttendance by viewModel.todayAttendance.collectAsStateWithLifecycle()
    val savedOwnerPhone by viewModel.ownerWhatsAppPhone.collectAsStateWithLifecycle()
    val targetTab by viewModel.selectedAccountsTab.collectAsStateWithLifecycle()
    val transactions by viewModel.employeeTransactions.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: لوحة الحضور والغياب, 1: الموظفون والرواتب, 2: الموردون والأشخاص, 3: كباتن الدليفري, 4: الورديات المقفلة

    LaunchedEffect(targetTab) {
        selectedTab = targetTab
    }

    // Tab 0 States (General Accounts)
    var showNewAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<GeneralAccount?>(null) }
    var accountToDelete by remember { mutableStateOf<GeneralAccount?>(null) }
    var accountForTx by remember { mutableStateOf<GeneralAccount?>(null) }
    var accountForStatement by remember { mutableStateOf<GeneralAccount?>(null) }
    var accountSearchQuery by remember { mutableStateOf("") }
    var selectedAccountTypeFilter by remember { mutableStateOf("ALL") }
    var showArchivedAccounts by remember { mutableStateOf(false) }

    // Tab 1 States (Closed Shifts)
    var shiftSearchQuery by remember { mutableStateOf("") }
    var shiftForDetail by remember { mutableStateOf<Shift?>(null) }

    // Tab 2 & 3 States (Staff & Drivers)
    var showNewDriverDialog by remember { mutableStateOf(false) }
    var driverToEdit by remember { mutableStateOf<DeliveryDriver?>(null) }
    var driverToDelete by remember { mutableStateOf<DeliveryDriver?>(null) }
    var driverToSettle by remember { mutableStateOf<DeliveryDriver?>(null) }
    var driverForStatement by remember { mutableStateOf<DeliveryDriver?>(null) }

    var showNewEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<Employee?>(null) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }
    var employeeForTx by remember { mutableStateOf<Employee?>(null) }
    var employeeForAtt by remember { mutableStateOf<Pair<Employee, String>?>(null) }
    var employeeForStatement by remember { mutableStateOf<Employee?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Tab Header
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent
        ) {
            val nonArchivedAccountsCount = remember(accounts) { accounts.count { !it.isArchived } }
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    viewModel.selectedAccountsTab.value = 0
                },
                text = {
                    Text(
                        text = "لوحة الحضور والغياب 📋",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (selectedTab == 0) SeaTealPrimary else Color.Unspecified
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    viewModel.selectedAccountsTab.value = 1
                },
                text = { Text("الموظفون والرواتب (${employees.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = {
                    selectedTab = 2
                    viewModel.selectedAccountsTab.value = 2
                },
                text = { Text("الموردون والأشخاص ($nonArchivedAccountsCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = {
                    selectedTab = 3
                    viewModel.selectedAccountsTab.value = 3
                },
                text = { Text("كباتن الدليفري (${drivers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 4,
                onClick = {
                    selectedTab = 4
                    viewModel.selectedAccountsTab.value = 4
                },
                text = { Text("الورديات المقفلة (${closedShifts.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedTab) {
            0 -> {
                // ==================== TAB 0: لوحة الحضور والغياب والتأخير ====================
                AttendanceBoardTab(
                    employees = employees,
                    todayAttendance = todayAttendance,
                    allAttendance = attendance,
                    savedOwnerPhone = savedOwnerPhone,
                    onRecordAttendance = { emp, mode ->
                        employeeForAtt = Pair(emp, mode)
                    },
                    onViewStatement = { emp ->
                        employeeForStatement = emp
                    },
                    onSaveOwnerPhone = { phone ->
                        viewModel.saveOwnerWhatsAppPhone(phone)
                    }
                )
            }

            2 -> {
                // ==================== TAB 2: الموردون والأشخاص ====================
                val totalCredit = remember(accounts) { accounts.filter { !it.isArchived && it.currentBalance > 0 }.sumOf { it.currentBalance } }
                val totalDebit = remember(accounts) { accounts.filter { !it.isArchived && it.currentBalance < 0 }.sumOf { kotlin.math.abs(it.currentBalance) } }
                val activeAccounts = remember(accounts, showArchivedAccounts, accountSearchQuery, selectedAccountTypeFilter) {
                    accounts.filter { acc ->
                        (showArchivedAccounts || !acc.isArchived) &&
                        (selectedAccountTypeFilter == "ALL" || acc.type == selectedAccountTypeFilter) &&
                        (accountSearchQuery.isBlank() || acc.name.contains(accountSearchQuery, ignoreCase = true) || acc.phone.contains(accountSearchQuery))
                    }
                }

                // Top Balance Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberCoral.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberCoral.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("إجمالي مستحق للموردين (له)", fontSize = 10.sp, color = Color.Gray)
                            Text("${String.format(Locale.US, "%.1f", totalCredit)} ج.م", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberCoral)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SeaTealPrimary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("إجمالي مديونية للمحل (عليه)", fontSize = 10.sp, color = Color.Gray)
                            Text("${String.format(Locale.US, "%.1f", totalDebit)} ج.م", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SeaTealPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search and Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = accountSearchQuery,
                        onValueChange = { accountSearchQuery = it },
                        placeholder = { Text("بحث باسم الحساب أو الهاتف...", fontSize = 12.5.sp, lineHeight = 20.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (accountSearchQuery.isNotBlank()) {
                                IconButton(onClick = { accountSearchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 20.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 56.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showNewAccountDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة حساب", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .mouseHorizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedAccountTypeFilter == "ALL",
                        onClick = { selectedAccountTypeFilter = "ALL" },
                        label = { Text("الكل", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedAccountTypeFilter == "SUPPLIER",
                        onClick = { selectedAccountTypeFilter = "SUPPLIER" },
                        label = { Text("موردين", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedAccountTypeFilter == "IMPORTER",
                        onClick = { selectedAccountTypeFilter = "IMPORTER" },
                        label = { Text("مستوردين", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedAccountTypeFilter == "WHOLESALER",
                        onClick = { selectedAccountTypeFilter = "WHOLESALER" },
                        label = { Text("تجار جملة", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedAccountTypeFilter == "INDIVIDUAL",
                        onClick = { selectedAccountTypeFilter = "INDIVIDUAL" },
                        label = { Text("أشخاص عاديين", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedAccountTypeFilter == "MAINTENANCE",
                        onClick = { selectedAccountTypeFilter = "MAINTENANCE" },
                        label = { Text("صيانة وخدمات", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = showArchivedAccounts,
                        onClick = { showArchivedAccounts = !showArchivedAccounts },
                        label = { Text(if (showArchivedAccounts) "المؤرشفين ✓" else "إظهار المؤرشفين", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List of accounts
                if (activeAccounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (accountSearchQuery.isNotBlank()) "لا توجد نتائج مطابقة للبحث" else "لم يتم تسجيل حسابات موردين أو أشخاص بعد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "يمكنك إضافة حسابات للموردين، المستوردين، تجار الجملة، أو الأشخاص العاديين ومتابعة مسحوباتهم وسداداتهم.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showNewAccountDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة أول حساب")
                            }
                        }
                    }
                } else {
                    MouseLazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeAccounts.distinctBy { it.id }, key = { "acc_${it.id}" }) { acc ->
                            AccountCard(
                                account = acc,
                                onAddTransaction = { accountForTx = acc },
                                onViewStatement = { accountForStatement = acc },
                                onEdit = { accountToEdit = acc },
                                onArchive = { viewModel.archiveAccount(acc.id, !acc.isArchived) },
                                onDelete = { accountToDelete = acc }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(50.dp)) }
                    }
                }
            }

            4 -> {
                // ==================== TAB 4: الورديات المقفلة ====================
                val filteredShifts = remember(closedShifts, shiftSearchQuery) {
                    if (shiftSearchQuery.isBlank()) closedShifts
                    else closedShifts.filter {
                        it.shiftNumber.toString().contains(shiftSearchQuery) ||
                        it.cashierName.contains(shiftSearchQuery, ignoreCase = true)
                    }
                }

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SeaNavyDeep.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ورديات مقفلة", fontSize = 10.sp, color = Color.Gray)
                            Text("${closedShifts.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SeaNavyDeep)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SeaTealPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("مبيعات كاش", fontSize = 10.sp, color = Color.Gray)
                            Text("${String.format(Locale.US, "%.0f", closedShifts.sumOf { it.totalCashSales })} ج.م", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SeaTealPrimary)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AmberCoral.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("مبيعات محافظ", fontSize = 10.sp, color = Color.Gray)
                            Text("${String.format(Locale.US, "%.0f", closedShifts.sumOf { it.totalWalletSales })} ج.م", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberCoral)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFC62828).copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("مصروفات", fontSize = 10.sp, color = Color.Gray)
                            Text("${String.format(Locale.US, "%.0f", closedShifts.sumOf { it.totalExpenses })} ج.م", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = shiftSearchQuery,
                    onValueChange = { shiftSearchQuery = it },
                    placeholder = { Text("بحث برقم الوردية أو اسم الكاشير...", fontSize = 12.5.sp, lineHeight = 20.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (shiftSearchQuery.isNotBlank()) {
                            IconButton(onClick = { shiftSearchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 20.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredShifts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (shiftSearchQuery.isNotBlank()) "لا توجد نتائج مطابقة لبحثك" else "لا توجد ورديات مقفلة حالياً",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "عند تقفيل أي وردية كاشير من شاشة البيع (POS) عبر زر \"تقفيل الوردية والجرد\"، ستظهر تفاصيلها وحساباتها وإيصالاتها هنا آلياً.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    MouseLazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredShifts.distinctBy { it.id }, key = { "shift_${it.id}" }) { shift ->
                            ClosedShiftCard(
                                shift = shift,
                                onViewDetails = { shiftForDetail = shift },
                                onReprint = { viewModel.reprintShiftReport(shift) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(50.dp)) }
                    }
                }
            }

            1 -> {
                // ==================== TAB 1: الموظفون والرواتب ====================
                // Attendance Board Quick Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = SeaTealPrimary.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, SeaTealPrimary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedTab = 0
                            viewModel.selectedAccountsTab.value = 0
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SeaTealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "📋 فتح لوحة الغياب والحضور والتأخير اليومية",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = SeaNavyDeep
                                )
                                Text(
                                    text = "متابعة فورية للحضور والغياب والتأخير وإرسال تقارير الواتساب للأونر",
                                    fontSize = 10.5.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SeaTealPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "فريق العمل والموظفون:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { showNewEmployeeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة موظف جديد", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                MouseLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(employees.distinctBy { it.id }, key = { "acc_emp_${it.id}" }) { emp ->
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
                                        Text(
                                            text = "⏰ الوردية المقررة: ${com.example.util.Time12hUtil.format12h(emp.shiftStartTime)} إلى ${com.example.util.Time12hUtil.format12h(emp.shiftEndTime)}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
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

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { employeeForAtt = Pair(emp, "CHECK_IN") },
                                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(1.1f).height(34.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("حضور ⏰", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { employeeForAtt = Pair(emp, "CHECK_OUT") },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(1.1f).height(34.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("انصراف 🚪", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { employeeForAtt = Pair(emp, "ABSENCE") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(0.9f).height(34.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Block, contentDescription = null, modifier = Modifier.size(13.dp), tint = StatusDanger)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("غياب 🚫", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { employeeForTx = emp },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(1f).height(34.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("سلفة / جزاء", fontSize = 10.5.sp)
                                    }

                                    Button(
                                        onClick = { employeeForStatement = emp },
                                        colors = ButtonDefaults.buttonColors(containerColor = SeaNavyDeep),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(1.2f).height(34.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("كشف حساب 📋", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = { employeeToEdit = emp }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = SeaTealPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { viewModel.archiveEmployee(emp.id, !emp.isArchived) }) {
                                        Icon(
                                            imageVector = if (emp.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                            contentDescription = "أرشفة",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { employeeToDelete = emp }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = StatusDanger, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }

            3 -> {
                // ==================== TAB 3: كباتن الدليفري ====================
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
                    items(drivers.distinctBy { it.id }, key = { "acc_driver_${it.id}" }) { driver ->
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
                                            Text(text = "هاتف: ${driver.phone}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isCommission) "${driver.ratePerOrder} ج.م / أوردر" else "${driver.fixedDailySalary} ج.م / يوم",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = SeaNavyDeep
                                        )
                                        Text(
                                            text = if (isCommission) "نظام عمولة" else "نظام يومية ثابتة",
                                            fontSize = 10.sp,
                                            color = Color.Gray
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
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("تصفية الحساب", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { driverForStatement = driver },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("كشف حساب", fontSize = 11.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = { driverToEdit = driver }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = SeaTealPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { viewModel.archiveDriver(driver.id, !driver.isArchived) }) {
                                        Icon(
                                            imageVector = if (driver.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                            contentDescription = "أرشفة",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { driverToDelete = driver }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = StatusDanger, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }

    // ==================== DIALOGS ====================

    // General Account Dialogs
    if (showNewAccountDialog || accountToEdit != null) {
        GeneralAccountEditDialog(
            account = accountToEdit,
            onSave = { updated ->
                viewModel.saveAccount(updated)
                showNewAccountDialog = false
                accountToEdit = null
            },
            onDismiss = {
                showNewAccountDialog = false
                accountToEdit = null
            }
        )
    }

    accountForTx?.let { acc ->
        GeneralAccountTransactionDialog(
            account = acc,
            onConfirm = { type, amount, desc, invoiceNo, date, fromDrawer ->
                viewModel.recordAccountTransaction(
                    accountId = acc.id,
                    type = type,
                    amount = amount,
                    description = desc,
                    invoiceNumber = invoiceNo,
                    date = date,
                    isPaidFromDrawer = fromDrawer
                )
                accountForTx = null
            },
            onDismiss = { accountForTx = null }
        )
    }

    accountForStatement?.let { acc ->
        GeneralAccountStatementDialog(
            account = acc,
            viewModel = viewModel,
            onDismiss = { accountForStatement = null }
        )
    }

    accountToDelete?.let { targetAcc ->
        PasswordPromptDialog(
            title = "تأكيد حذف الحساب: ${targetAcc.name}",
            message = "حذف الحساب سيمسح جميع حركاته وسجلاته نهائياً. يتطلب كلمة سر الإدارة.",
            onConfirm = { pass ->
                scope.launch {
                    val ok = viewModel.verifyManagerPassword(pass)
                    if (ok) {
                        viewModel.deleteAccount(targetAcc.id)
                        accountToDelete = null
                    } else {
                        viewModel.userMessage.value = "كلمة مرور الإدارة غير صحيحة"
                    }
                }
            },
            onDismiss = { accountToDelete = null }
        )
    }

    // Closed Shift Detail Dialog
    shiftForDetail?.let { shift ->
        ClosedShiftDetailDialog(
            shift = shift,
            viewModel = viewModel,
            onDismiss = { shiftForDetail = null }
        )
    }

    // Driver Dialogs
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

    if (showNewDriverDialog || driverToEdit != null) {
        DriverEditDialog(
            driver = driverToEdit,
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

    // Employee Dialogs
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

    if (showNewEmployeeDialog || employeeToEdit != null) {
        EmployeeEditDialog(
            employee = employeeToEdit,
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

    employeeForAtt?.let { (emp, mode) ->
        AttendanceAndDepartureDialog(
            employee = emp,
            viewModel = viewModel,
            initialMode = mode,
            onDismiss = { employeeForAtt = null }
        )
    }

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
fun AccountCard(
    account: GeneralAccount,
    onAddTransaction: () -> Unit,
    onViewStatement: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val badgeColor = getAccountTypeBadgeColor(account.type)
    val isCredit = account.currentBalance >= 0.0
    val balanceColor = if (account.currentBalance == 0.0) StatusSuccess else if (isCredit) AmberCoral else Color(0xFF1565C0)
    val balanceLabel = if (account.currentBalance == 0.0) "الرصيد متزن (0 ج.م)" else if (isCredit) "له (دائن): ${String.format(Locale.US, "%.1f", account.currentBalance)} ج.م" else "عليه (مدين): ${String.format(Locale.US, "%.1f", kotlin.math.abs(account.currentBalance))} ج.م"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (account.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (account.type == "SUPPLIER" || account.type == "IMPORTER" || account.type == "WHOLESALER") Icons.Default.Business else Icons.Default.Person,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = account.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = getAccountTypeLabel(account.type),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (account.isArchived) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = Color.Gray.copy(alpha = 0.2f)) {
                                    Text(text = "مؤرشف", fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }

                        if (account.phone.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${account.phone}"))
                                        context.startActivity(intent)
                                    }
                                    .padding(top = 2.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = SeaTealPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = account.phone, fontSize = 11.sp, color = SeaTealPrimary)
                            }
                        }
                    }
                }

                // Balance Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = balanceColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, balanceColor)
                ) {
                    Text(
                        text = balanceLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (account.address.isNotBlank() || account.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    if (account.address.isNotBlank()) {
                        Text(text = "العنوان: ${account.address}", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (account.notes.isNotBlank()) {
                        Text(text = "ملاحظات: ${account.notes}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddTransaction,
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معاملة جديدة", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onViewStatement,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("كشف حساب", fontSize = 11.sp)
                }

                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = SeaTealPrimary, modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onArchive) {
                    Icon(
                        imageVector = if (account.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = "أرشفة",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = StatusDanger, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun ClosedShiftCard(
    shift: Shift,
    onViewDetails: () -> Unit,
    onReprint: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US) }
    val diff = shift.difference
    val diffColor = if (kotlin.math.abs(diff) < 0.01) StatusSuccess else if (diff < 0) StatusDanger else Color(0xFF1565C0)
    val diffStatus = if (kotlin.math.abs(diff) < 0.01) "متطابق ✓" else if (diff < 0) "عجز ${String.format(Locale.US, "%.1f", kotlin.math.abs(diff))} ج.م" else "زيادة ${String.format(Locale.US, "%.1f", diff)} ج.م"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
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
                        Icon(imageVector = Icons.Default.PointOfSale, contentDescription = null, tint = SeaTealPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "الوردية #${shift.shiftNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StatusSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "مقفلة ✓",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusSuccess,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "كاشير: ${shift.cashierName.ifBlank { "عام" }} | ${shift.ordersCount} أوردر",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = diffColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, diffColor)
                ) {
                    Text(
                        text = diffStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = diffColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Time Row
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "من: ${timeFormat.format(Date(shift.openedAt))}",
                    fontSize = 10.sp,
                    color = Color.DarkGray
                )
                shift.closedAt?.let { closed ->
                    Text(
                        text = " إلى: ${timeFormat.format(Date(closed))}",
                        fontSize = 10.sp,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Financial Summary Row
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("افتتاح درج", fontSize = 9.sp, color = Color.Gray)
                        Text("${String.format(Locale.US, "%.0f", shift.openingBalance)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("مبيعات كاش", fontSize = 9.sp, color = Color.Gray)
                        Text("${String.format(Locale.US, "%.0f", shift.totalCashSales)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SeaTealPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("محافظ", fontSize = 9.sp, color = Color.Gray)
                        Text("${String.format(Locale.US, "%.0f", shift.totalWalletSales)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberCoral)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("مصروفات", fontSize = 9.sp, color = Color.Gray)
                        Text("${String.format(Locale.US, "%.0f", shift.totalExpenses)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الفعلي بالجرد", fontSize = 9.sp, color = Color.Gray)
                        Text("${String.format(Locale.US, "%.0f", shift.actualDrawerCash)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SeaNavyDeep)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تقرير الوردية (Z-Report)", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onReprint,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعادة الطباعة", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
