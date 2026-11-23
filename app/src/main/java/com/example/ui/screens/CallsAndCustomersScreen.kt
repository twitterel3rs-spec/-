package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import com.example.ui.components.MouseLazyColumn
import com.example.ui.components.mouseHorizontalScroll
import com.example.ui.components.mouseVerticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import android.widget.Toast
import com.example.util.phoneDigitCount
import com.example.util.isCustomerPhoneIncomplete
import com.example.util.isCustomerPhoneTooLong
import com.example.util.isCustomerPhoneInvalid
import com.example.util.getCustomerPhoneWarning
import com.example.util.getCustomerPhoneToastWarning
import com.example.util.convertEnglishKeyboardToArabic
import com.example.util.containsLatinLetters
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.CustomerAddress
import com.example.data.model.OrderWithItems
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.util.CustomerFilterType
import com.example.util.CustomerSortOption
import com.example.util.CustomerAnalysis
import com.example.util.CustomerFilterUtil
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallsAndCustomersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val callerState by viewModel.callerState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var phoneInputForCall by remember { mutableStateOf("") }

    // Synchronize incoming caller phone if detected
    LaunchedEffect(callerState?.phoneNumber) {
        val incoming = callerState?.phoneNumber
        if (!incoming.isNullOrBlank() && phoneInputForCall.isBlank()) {
            phoneInputForCall = incoming
        }
    }

    val activeCallerPhone = phoneInputForCall.trim()
    val matchedCustomer = remember(activeCallerPhone, customers) {
        if (activeCallerPhone.length >= 7) {
            val clean = activeCallerPhone.replace("[^0-9]".toRegex(), "")
            customers.firstOrNull { cust ->
                val pClean = cust.primaryPhone.replace("[^0-9]".toRegex(), "")
                val sClean = cust.secondaryPhone?.replace("[^0-9]".toRegex(), "") ?: ""
                (clean.isNotEmpty() && (pClean.endsWith(clean) || clean.endsWith(pClean))) ||
                (clean.isNotEmpty() && sClean.isNotEmpty() && (sClean.endsWith(clean) || clean.endsWith(sClean)))
            }
        } else null
    }

    var selectedFilter by remember { mutableStateOf(CustomerFilterType.ALL) }
    var selectedSortOption by remember { mutableStateOf(CustomerSortOption.ORDERS_COUNT_DESC) }
    var inactiveThresholdDays by remember { mutableStateOf(30) }
    var showSortMenu by remember { mutableStateOf(false) }

    var expandedCustomerId by remember { mutableStateOf<Long?>(null) }
    var showNewCustomerDialog by remember { mutableStateOf(false) }
    var newCustomerPresetPhone by remember { mutableStateOf("") }
    var customerForNewAddress by remember { mutableStateOf<Customer?>(null) }

    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var addressToEdit by remember { mutableStateOf<CustomerAddress?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var addressToDelete by remember { mutableStateOf<CustomerAddress?>(null) }

    // Check system permission status
    var hasPhoneStatePerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPhoneStatePerm = perms[Manifest.permission.READ_PHONE_STATE] == true &&
                            perms[Manifest.permission.READ_CALL_LOG] == true
    }

    val customerAnalyses = remember(customers, orders, inactiveThresholdDays) {
        customers.map { cust ->
            CustomerFilterUtil.analyzeCustomer(cust, orders, inactiveThresholdDays = inactiveThresholdDays)
        }
    }

    val filteredAnalyses = remember(customers, orders, searchQuery, selectedFilter, selectedSortOption, inactiveThresholdDays) {
        CustomerFilterUtil.filterAndSortCustomers(
            customers = customers,
            allOrders = orders,
            searchQuery = searchQuery,
            selectedFilter = selectedFilter,
            sortOption = selectedSortOption,
            inactiveThresholdDays = inactiveThresholdDays
        )
    }

    // Counts for tabs
    val totalCount = customers.size
    val topOrderingCount = remember(customerAnalyses) { customerAnalyses.count { it.isTopOrdering } }
    val inactiveCount = remember(customerAnalyses) { customerAnalyses.count { it.isInactive } }
    val loyalSpacedCount = remember(customerAnalyses) { customerAnalyses.count { it.isLoyalSpaced } }
    val highSpendingCount = remember(customerAnalyses) { customerAnalyses.count { it.isHighSpender } }
    val newCustomersCount = remember(customerAnalyses) { customerAnalyses.count { it.isNewCustomer } }

    fun callCustomer(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الهاتف", Toast.LENGTH_SHORT).show()
        }
    }

    fun whatsAppCustomer(phone: String, customerName: String) {
        try {
            val cleanPhone = phone.replace("[^0-9+]".toRegex(), "")
            val intlPhone = if (cleanPhone.startsWith("01")) "2$cleanPhone" else cleanPhone
            val message = "أهلاً وسهلاً بحضرتك يا أستاذ/ة $customerName من مطعم «صنارة وشبكة» 🐟✨ بنتشرف دائماً بخدمتكم، ولأي استفسار أو طلب طازج نحن في خدمتكم!"
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$intlPhone&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // Landscape 2-Pane Layout: Left = Call Monitor & Actions, Right = Search & Customer List
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search, 2-Row Customer Filters & Full-Height Scrollable Customers List
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Search & Sort bar (compact height ~36dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "بحث بالاسم أو رقم الهاتف...",
                        modifier = Modifier.weight(1f)
                    )

                    // Sort button with dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .height(36.dp)
                                .clickable { showSortMenu = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "ترتيب", modifier = Modifier.size(15.dp), tint = SeaTealPrimary)
                                Text(
                                    text = when (selectedSortOption) {
                                        CustomerSortOption.ORDERS_COUNT_DESC -> "الأكثر طلباً"
                                        CustomerSortOption.TOTAL_SPENT_DESC -> "الأعلى إنفاقاً"
                                        CustomerSortOption.LAST_ORDER_RECENT -> "الأحدث طلباً"
                                        CustomerSortOption.LAST_ORDER_OLDEST -> "الأقدم طلباً"
                                        CustomerSortOption.NAME_ASC -> "الاسم"
                                        CustomerSortOption.DATE_ADDED_DESC -> "التسجيل"
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SeaTealPrimary
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            CustomerSortOption.values().forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = opt.title,
                                            fontWeight = if (selectedSortOption == opt) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedSortOption == opt) SeaTealPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.5.sp
                                        )
                                    },
                                    onClick = {
                                        selectedSortOption = opt
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Add Customer Button
                    Button(
                        onClick = {
                            newCustomerPresetPhone = phoneInputForCall.ifBlank { "" }
                            showNewCustomerDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp).testTag("add_customer_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("عميل جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Customer Filter Section in 2 Rows
                CustomerFilterSection(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    inactiveThresholdDays = inactiveThresholdDays,
                    onThresholdChanged = { inactiveThresholdDays = it },
                    totalCount = totalCount,
                    topOrderingCount = topOrderingCount,
                    inactiveCount = inactiveCount,
                    loyalSpacedCount = loyalSpacedCount,
                    highSpendingCount = highSpendingCount,
                    newCustomersCount = newCustomersCount
                )

                MouseLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredAnalyses.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("لا يوجد عملاء يطابقون هذا الفلتر أو البحث", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    items(filteredAnalyses, key = { it.customer.id }) { analysis ->
                        val cust = analysis.customer
                        val isExpanded = expandedCustomerId == cust.id

                        var customerAddresses by remember(cust.id) { mutableStateOf<List<CustomerAddress>>(emptyList()) }
                        LaunchedEffect(cust.id, isExpanded) {
                            if (isExpanded) {
                                customerAddresses = viewModel.getCustomerAddresses(cust.id)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isExpanded) SeaTealPrimary else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                                ),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            CustomerCardContent(
                                analysis = analysis,
                                isExpanded = isExpanded,
                                customerAddresses = customerAddresses,
                                onExpandToggle = { expandedCustomerId = if (isExpanded) null else cust.id },
                                onStartOrder = { addr ->
                                    viewModel.startNewOrderForCaller(cust, cust.primaryPhone, addr)
                                },
                                onAddNewAddress = { customerForNewAddress = cust },
                                onEditCustomer = { customerToEdit = cust },
                                onEditAddress = { addr -> addressToEdit = addr },
                                onDeleteCustomer = { customerToDelete = cust },
                                onDeleteAddress = { addr -> addressToDelete = addr },
                                onCallCustomer = { phone -> callCustomer(phone) },
                                onWhatsAppCustomer = { phone, name -> whatsAppCustomer(phone, name) }
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Portrait Layout
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Compact Search, Sort & New Customer in ONE slim row ("علي قد الوظيفه فقط")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactSearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "بحث بالاسم أو الهاتف...",
                    modifier = Modifier.weight(1f)
                )

                // Sort Button
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { showSortMenu = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "ترتيب", modifier = Modifier.size(15.dp), tint = SeaTealPrimary)
                            Text(
                                text = when (selectedSortOption) {
                                    CustomerSortOption.ORDERS_COUNT_DESC -> "الأكثر طلباً"
                                    CustomerSortOption.TOTAL_SPENT_DESC -> "الأعلى إنفاقاً"
                                    CustomerSortOption.LAST_ORDER_RECENT -> "الأحدث طلباً"
                                    CustomerSortOption.LAST_ORDER_OLDEST -> "الأقدم طلباً"
                                    CustomerSortOption.NAME_ASC -> "الاسم"
                                    CustomerSortOption.DATE_ADDED_DESC -> "التسجيل"
                                },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = SeaTealPrimary
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        }
                    }

                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        CustomerSortOption.values().forEach { opt ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt.title,
                                        fontWeight = if (selectedSortOption == opt) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedSortOption == opt) SeaTealPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.5.sp
                                    )
                                },
                                onClick = {
                                    selectedSortOption = opt
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                // Add Customer Button
                Button(
                    onClick = {
                        newCustomerPresetPhone = phoneInputForCall.ifBlank { "" }
                        showNewCustomerDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp).testTag("add_customer_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("عميل جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 3. Customer Filters in 2 Rows under each other ("والفلاتر مكتوبة في سطرين تحت بعض")
            CustomerFilterSection(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                inactiveThresholdDays = inactiveThresholdDays,
                onThresholdChanged = { inactiveThresholdDays = it },
                totalCount = totalCount,
                topOrderingCount = topOrderingCount,
                inactiveCount = inactiveCount,
                loyalSpacedCount = loyalSpacedCount,
                highSpendingCount = highSpendingCount,
                newCustomersCount = newCustomersCount
            )

            // Customer Cards List
            MouseLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filteredAnalyses.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "لا يوجد عملاء يطابقون هذا الفلتر أو البحث",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "جرّب تغيير الفلتر المختار أو تعديل كلمة البحث",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                items(filteredAnalyses, key = { it.customer.id }) { analysis ->
                    val cust = analysis.customer
                    val isExpanded = expandedCustomerId == cust.id

                    var customerAddresses by remember(cust.id) { mutableStateOf<List<CustomerAddress>>(emptyList()) }
                    LaunchedEffect(cust.id, isExpanded) {
                        if (isExpanded) {
                            customerAddresses = viewModel.getCustomerAddresses(cust.id)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isExpanded) SeaTealPrimary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(10.dp)
                            )
                            .testTag("customer_card_${cust.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        CustomerCardContent(
                            analysis = analysis,
                            isExpanded = isExpanded,
                            customerAddresses = customerAddresses,
                            onExpandToggle = { expandedCustomerId = if (isExpanded) null else cust.id },
                            onStartOrder = { addr ->
                                viewModel.startNewOrderForCaller(cust, cust.primaryPhone, addr)
                            },
                            onAddNewAddress = { customerForNewAddress = cust },
                            onEditCustomer = { customerToEdit = cust },
                            onEditAddress = { addr -> addressToEdit = addr },
                            onDeleteCustomer = { customerToDelete = cust },
                            onDeleteAddress = { addr -> addressToDelete = addr },
                            onCallCustomer = { phone -> callCustomer(phone) },
                            onWhatsAppCustomer = { phone, name -> whatsAppCustomer(phone, name) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // New Customer Dialog
    if (showNewCustomerDialog) {
        NewCustomerDialog(
            presetPhone = newCustomerPresetPhone,
            onSave = { name, phone, secPhone, notes, addrTitle, area, street, fee ->
                viewModel.saveCustomer(name, phone, secPhone, notes, addrTitle, area, street, fee)
                showNewCustomerDialog = false
            },
            onDismiss = { showNewCustomerDialog = false }
        )
    }

    // New Address Dialog
    customerForNewAddress?.let { cust ->
        NewAddressDialog(
            customerName = cust.name,
            onSave = { title, area, street, fee, isDefault ->
                viewModel.addCustomerAddress(cust.id, title, area, street, fee, isDefault)
                customerForNewAddress = null
            },
            onDismiss = { customerForNewAddress = null }
        )
    }

    // Edit Customer Profile Dialog
    customerToEdit?.let { cust ->
        EditCustomerProfileDialog(
            customer = cust,
            onSave = { updated ->
                viewModel.saveCustomer(updated)
                customerToEdit = null
            },
            onDismiss = { customerToEdit = null }
        )
    }

    // Edit Address Dialog
    addressToEdit?.let { addr ->
        EditAddressDialog(
            address = addr,
            onSave = { updated ->
                viewModel.saveCustomerAddress(updated)
                addressToEdit = null
            },
            onDismiss = { addressToEdit = null }
        )
    }

    // Delete Customer Confirmation
    customerToDelete?.let { cust ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("حذف العميل نهائياً", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف العميل «${cust.name}»؟ لن يتم مسح الأوردرات السابقة المرتبطة به ولكن سيتم إزالته من الدليل.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(cust.id)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text("نعم، حذف العميل")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { customerToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    // Delete Address Confirmation
    addressToDelete?.let { addr ->
        AlertDialog(
            onDismissRequest = { addressToDelete = null },
            title = { Text("مسح العنوان المسجل", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد مسح العنوان «${addr.title}: ${addr.streetDetails}» من سجل العميل؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomerAddress(addr.id)
                        addressToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text("نعم، مسح العنوان")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { addressToDelete = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun CustomerFilterSection(
    selectedFilter: CustomerFilterType,
    onFilterSelected: (CustomerFilterType) -> Unit,
    inactiveThresholdDays: Int,
    onThresholdChanged: (Int) -> Unit,
    totalCount: Int,
    topOrderingCount: Int,
    inactiveCount: Int,
    loyalSpacedCount: Int,
    highSpendingCount: Int,
    newCustomersCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Line 1: الكل | الأكثر طلباً ⭐ | لم يطلب منذ فترة ⏳
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CustomerFilterButton(
                title = "الكل",
                count = totalCount,
                isSelected = selectedFilter == CustomerFilterType.ALL,
                selectedColor = SeaTealPrimary,
                modifier = Modifier.weight(1f),
                onClick = { onFilterSelected(CustomerFilterType.ALL) }
            )
            CustomerFilterButton(
                title = "الأكثر طلباً ⭐",
                count = topOrderingCount,
                isSelected = selectedFilter == CustomerFilterType.TOP_ORDERING,
                selectedColor = AmberCoral,
                modifier = Modifier.weight(1.15f),
                onClick = { onFilterSelected(CustomerFilterType.TOP_ORDERING) }
            )
            CustomerFilterButton(
                title = "لم يطلب منذ فترة ⏳",
                count = inactiveCount,
                isSelected = selectedFilter == CustomerFilterType.INACTIVE,
                selectedColor = StatusDanger,
                modifier = Modifier.weight(1.25f),
                onClick = { onFilterSelected(CustomerFilterType.INACTIVE) }
            )
        }

        // Line 2: العميل الدائم 💎 | الأعلى إنفاقاً 💰 | عملاء جدد ✨
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CustomerFilterButton(
                title = "العميل الدائم 💎",
                count = loyalSpacedCount,
                isSelected = selectedFilter == CustomerFilterType.LOYAL_SPACED,
                selectedColor = Color(0xFF0284C7),
                modifier = Modifier.weight(1.25f),
                onClick = { onFilterSelected(CustomerFilterType.LOYAL_SPACED) }
            )
            CustomerFilterButton(
                title = "الأعلى إنفاقاً 💰",
                count = highSpendingCount,
                isSelected = selectedFilter == CustomerFilterType.HIGH_SPENDING,
                selectedColor = Color(0xFF7C3AED),
                modifier = Modifier.weight(1.15f),
                onClick = { onFilterSelected(CustomerFilterType.HIGH_SPENDING) }
            )
            CustomerFilterButton(
                title = "عملاء جدد ✨",
                count = newCustomersCount,
                isSelected = selectedFilter == CustomerFilterType.NEW_CUSTOMERS,
                selectedColor = Color(0xFF16A34A),
                modifier = Modifier.weight(1f),
                onClick = { onFilterSelected(CustomerFilterType.NEW_CUSTOMERS) }
            )
        }

        // Sub-filter bar when "لم يطلب منذ فترة" is chosen
        if (selectedFilter == CustomerFilterType.INACTIVE) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = AmberCoral.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, AmberCoral.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏳ فترة الانقطاع:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = SeaNavyDeep
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            30 to "شهر (30)",
                            45 to "45 يوم",
                            60 to "شهرين (60)",
                            90 to "3 شهور (90)"
                        ).forEach { (days, label) ->
                            val isChosen = inactiveThresholdDays == days
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isChosen) AmberCoral else Color.White,
                                border = BorderStroke(1.dp, if (isChosen) AmberCoral else Color.LightGray),
                                modifier = Modifier
                                    .clickable { onThresholdChanged(days) }
                                    .height(24.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChosen) Color.White else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerFilterButton(
    title: String,
    count: Int,
    isSelected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) selectedColor else Color.White,
        border = BorderStroke(
            1.dp,
            if (isSelected) selectedColor else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = modifier
            .height(32.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(3.dp))
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0xFFF1F5F9),
                modifier = Modifier.defaultMinSize(minWidth = 18.dp).height(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 3.dp)) {
                    Text(
                        text = "$count",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF475569)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "بحث بالاسم أو رقم الهاتف...",
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier.height(36.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.Gray,
                            fontSize = 11.5.sp,
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search,
                            autoCorrect = false
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDirection = TextDirection.ContentOrRtl
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (value.containsLatinLetters()) {
                    val converted = convertEnglishKeyboardToArabic(value)
                    if (converted != value) {
                        Surface(
                            onClick = { onValueChange(converted) },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .height(22.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تحويل لعربي",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "عربي",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "مسح",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SlimCallerBar(
    phoneInput: String,
    onPhoneInputChange: (String) -> Unit,
    hasPhoneStatePerm: Boolean,
    onRequestPermission: () -> Unit,
    onCheckAndStartOrder: () -> Unit,
    matchedCustomer: Customer?,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SeaTealPrimary),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            // Main slim line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Call status dot
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (hasPhoneStatePerm) StatusSuccess else StatusWarning)
                        .clickable(enabled = !hasPhoneStatePerm) { onRequestPermission() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneCallback,
                        contentDescription = "كاشف المكالمات",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Phone Input field
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (phoneInput.isEmpty()) {
                                Text(
                                    text = "رقم المتصل (مثال: 01012345678)...",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            BasicTextField(
                                value = phoneInput,
                                onValueChange = onPhoneInputChange,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 12.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("caller_phone_input")
                            )
                        }
                        if (phoneInput.isNotEmpty()) {
                            IconButton(
                                onClick = { onPhoneInputChange("") },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                // Action button: فحص وبدء أوردر
                Button(
                    onClick = onCheckAndStartOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("simulate_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "بدء أوردر 📞",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Caller recognition banner when a phone number is entered or detected
            if (phoneInput.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (matchedCustomer != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFFA5D6A7),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "العميل: ${matchedCustomer.name} (${matchedCustomer.totalOrdersCount} طلب)",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "مسجل بالنظام ✓",
                            color = Color(0xFFA5D6A7),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AmberCoral,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "رقم جديد: جاهز لبدء الأوردر وحفظ البيانات",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeChip(
    text: String,
    containerColor: Color,
    textColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
            Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun CustomerCardContent(
    analysis: CustomerAnalysis,
    isExpanded: Boolean,
    customerAddresses: List<CustomerAddress>,
    onExpandToggle: () -> Unit,
    onStartOrder: (CustomerAddress?) -> Unit,
    onAddNewAddress: () -> Unit,
    onEditCustomer: () -> Unit,
    onEditAddress: (CustomerAddress) -> Unit,
    onDeleteCustomer: () -> Unit,
    onDeleteAddress: (CustomerAddress) -> Unit,
    onCallCustomer: (String) -> Unit,
    onWhatsAppCustomer: (String, String) -> Unit
) {
    val cust = analysis.customer
    val displayOrderCount = analysis.totalOrdersCount
    val displaySpent = analysis.totalSpent
    val customerOrders = analysis.matchedOrders

    Column(modifier = Modifier.padding(10.dp)) {
        // Top Row: Avatar, Name & Phone, Orders Count & Spend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandToggle() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                analysis.isTopOrdering -> AmberCoral.copy(alpha = 0.2f)
                                analysis.isLoyalSpaced -> Color(0xFF0284C7).copy(alpha = 0.2f)
                                analysis.isInactive -> StatusDanger.copy(alpha = 0.15f)
                                else -> SeaTealPrimary.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            analysis.isTopOrdering -> Icons.Default.Star
                            analysis.isLoyalSpaced -> Icons.Default.Loyalty
                            analysis.isInactive -> Icons.Default.History
                            else -> Icons.Default.Person
                        },
                        contentDescription = null,
                        tint = when {
                            analysis.isTopOrdering -> AmberCoral
                            analysis.isLoyalSpaced -> Color(0xFF0284C7)
                            analysis.isInactive -> StatusDanger
                            else -> SeaTealPrimary
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cust.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (analysis.isTopOrdering) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("⭐", fontSize = 12.sp)
                        } else if (analysis.isLoyalSpaced) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("💎", fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = cust.primaryPhone + if (!cust.secondaryPhone.isNullOrBlank()) " / ${cust.secondaryPhone}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$displayOrderCount أوردر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "${String.format(Locale.US, "%.0f", displaySpent)} ج.م",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberCoral
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Insight Badges Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .mouseHorizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (analysis.isTopOrdering) {
                BadgeChip(
                    text = "الأكثر طلباً ($displayOrderCount أوردر)",
                    containerColor = AmberCoral.copy(alpha = 0.15f),
                    textColor = Color(0xFFB45309),
                    icon = Icons.Default.Star
                )
            }
            if (analysis.isLoyalSpaced) {
                BadgeChip(
                    text = "العميل الدائم (منتظم متباعد)",
                    containerColor = Color(0xFF0284C7).copy(alpha = 0.15f),
                    textColor = Color(0xFF0369A1),
                    icon = Icons.Default.Loyalty
                )
            }
            if (analysis.isInactive) {
                BadgeChip(
                    text = "لم يطلب منذ ${analysis.daysSinceLastOrder ?: 30} يوماً",
                    containerColor = StatusDanger.copy(alpha = 0.12f),
                    textColor = StatusDanger,
                    icon = Icons.Default.History
                )
            }
            if (analysis.isHighSpender) {
                BadgeChip(
                    text = "VIP مبيعات عالية",
                    containerColor = Color(0xFF7C3AED).copy(alpha = 0.12f),
                    textColor = Color(0xFF6D28D9),
                    icon = Icons.Default.Paid
                )
            }
            if (analysis.isNewCustomer) {
                BadgeChip(
                    text = "عميل جديد",
                    containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                    textColor = Color(0xFF047857),
                    icon = Icons.Default.PersonAdd
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Order Timing Summary Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = if (analysis.isInactive) StatusDanger else SeaTealPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "آخر طلب: ${analysis.getLastOrderRelativeText()}" +
                            if (analysis.lastOrderTime != null) " (${analysis.getLastOrderDateFormatted()})" else "",
                    fontSize = 11.sp,
                    fontWeight = if (analysis.isInactive) FontWeight.Bold else FontWeight.Normal,
                    color = if (analysis.isInactive) StatusDanger else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (analysis.averageIntervalDays != null) {
                Text(
                    text = analysis.getSpacedIntervalText(),
                    fontSize = 10.5.sp,
                    color = Color(0xFF0284C7),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Direct Action Bar (Call, WhatsApp, New Order, Expand)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick Call Button
            OutlinedButton(
                onClick = { onCallCustomer(cust.primaryPhone) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(imageVector = Icons.Default.Call, contentDescription = "اتصال", tint = SeaTealPrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("اتصال", fontSize = 11.sp, color = SeaTealPrimary)
            }

            // Quick WhatsApp Button
            OutlinedButton(
                onClick = { onWhatsAppCustomer(cust.primaryPhone, cust.name) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(imageVector = Icons.Default.Chat, contentDescription = "واتساب", tint = Color(0xFF25D366), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("واتساب", fontSize = 11.sp, color = Color(0xFF16A34A))
            }

            // Start Order Button
            Button(
                onClick = {
                    val defaultAddr = customerAddresses.firstOrNull { it.isDefault } ?: customerAddresses.firstOrNull()
                    onStartOrder(defaultAddr)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .testTag("quick_order_for_customer_${cust.id}")
            ) {
                Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("بدء أوردر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Toggle Expand Button
            IconButton(
                onClick = onExpandToggle,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "طي" else "تفاصيل",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expanded Details Section
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Loyalty and Order Stats Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "📊 تحليل نشاط وولاء العميل:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = SeaNavyDeep
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "• تاريخ أول تعامل:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = analysis.getFirstOrderDateFormatted(), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "• تاريخ آخر أوردر:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${analysis.getLastOrderDateFormatted()} (${analysis.getLastOrderRelativeText()})", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (analysis.timeSpanDays > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "• مدة العلاقة بالزبون:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "${analysis.timeSpanDays} يوماً", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (analysis.averageIntervalDays != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "• وتيرة الطلبات (المعدل):", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = analysis.getSpacedIntervalText(), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0284C7))
                            }
                        }
                    }
                }

                if (cust.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ملاحظات: ${cust.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Addresses Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عناوين التوصيل (${customerAddresses.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    TextButton(onClick = onAddNewAddress) {
                        Text("+ إضافة عنوان", fontSize = 11.sp)
                    }
                }

                if (customerAddresses.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        customerAddresses.forEach { addr ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = SeaTealPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${addr.title} (${addr.area}): ${addr.streetDetails}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (addr.isDefault) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = SeaTealPrimary.copy(alpha = 0.15f)
                                                ) {
                                                    Text("رئيسي", fontSize = 9.sp, color = SeaTealPrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            text = "رسوم التوصيل: ${String.format(Locale.US, "%.0f", addr.deliveryFee)} ج.م",
                                            fontSize = 10.sp,
                                            color = AmberCoral,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onEditAddress(addr) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل العنوان", tint = SeaTealPrimary, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(
                                            onClick = { onDeleteAddress(addr) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "مسح العنوان", tint = StatusDanger, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(text = "لا توجد عناوين مسجلة بعد", fontSize = 11.sp, color = Color.Gray)
                }

                // Recent orders
                if (customerOrders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "سجل الأوردرات السابقة (${customerOrders.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    customerOrders.take(4).forEach { o ->
                        val dateFormatted = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(o.order.createdAt))
                        Text(
                            text = "• ${o.order.orderNumber} ($dateFormatted - ${o.order.totalAmount} ج) - ${o.items.joinToString("، ") { it.itemName }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Profile action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditCustomer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل بيانات العميل", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onDeleteCustomer,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف العميل", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NewCustomerDialog(
    presetPhone: String,
    onSave: (name: String, phone: String, secPhone: String?, notes: String, addrTitle: String?, area: String?, street: String?, fee: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(presetPhone) }
    var secPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var addrTitle by remember { mutableStateOf("المنزل") }
    var area by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var feeStr by remember { mutableStateOf("20") }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val phoneDigits = phone.phoneDigitCount()
    val phoneWarning = phone.getCustomerPhoneWarning()
    val isPhoneInvalid = phone.isCustomerPhoneInvalid()
    val secPhoneDigits = secPhone.phoneDigitCount()
    val secPhoneWarning = secPhone.getCustomerPhoneWarning()
    val isSecPhoneInvalid = secPhone.isCustomerPhoneInvalid()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل عميل جديد وعنوانه", fontWeight = FontWeight.Bold) },
        text = {
            MouseLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isLandscape) 200.dp else 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(text = "البيانات الأساسية:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم العميل (مطلوب)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف الأساسي (مطلوب)") },
                        isError = isPhoneInvalid,
                        supportingText = {
                            if (phoneWarning != null) {
                                Text(
                                    text = phoneWarning,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (phoneDigits == 11) {
                                Text(
                                    text = "✓ رقم مكتمل (11 رقم)",
                                    color = Color(0xFF16A34A),
                                    fontSize = 10.sp
                                )
                            }
                        },
                        trailingIcon = {
                            if (isPhoneInvalid) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = phoneWarning ?: "تحذير: رقم غير صحيح",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (phoneDigits == 11) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "رقم مكتمل",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                    OutlinedTextField(
                        value = secPhone,
                        onValueChange = { secPhone = it },
                        label = { Text("رقم إضافي (اختياري)") },
                        isError = isSecPhoneInvalid,
                        supportingText = {
                            if (secPhoneWarning != null) {
                                Text(
                                    text = secPhoneWarning,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (secPhoneDigits == 11) {
                                Text(
                                    text = "✓ رقم مكتمل (11 رقم)",
                                    color = Color(0xFF16A34A),
                                    fontSize = 10.sp
                                )
                            }
                        },
                        trailingIcon = {
                            if (isSecPhoneInvalid) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = secPhoneWarning ?: "تحذير: رقم غير صحيح",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (secPhoneDigits == 11) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "رقم مكتمل",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات وتفضيلات (تتبيلة، ملح، إلخ)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "العنوان الأساسي الأول:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = addrTitle,
                        onValueChange = { addrTitle = it },
                        label = { Text("اسم العنوان (المنزل / العمل / المصيف)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = area,
                            onValueChange = { area = it },
                            label = { Text("المنطقة / الحي") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedTextField(
                            value = feeStr,
                            onValueChange = { feeStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("رسوم التوصيل (ج.م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = street,
                        onValueChange = { street = it },
                        label = { Text("الشارع والعمارة والشقة وعلامة مميزة") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    phone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                        Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                    }
                    secPhone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                        Toast.makeText(context, toastMsg.replace("رقم هاتف العميل", "الرقم الإضافي"), Toast.LENGTH_LONG).show()
                    }
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        val fee = feeStr.toDoubleOrNull() ?: 20.0
                        onSave(name, phone, secPhone.ifBlank { null }, notes, addrTitle, area, street, fee)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ العميل")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun NewAddressDialog(
    customerName: String,
    onSave: (title: String, area: String, street: String, fee: Double, isDefault: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("العمل") }
    var area by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var feeStr by remember { mutableStateOf("20") }
    var isDefault by remember { mutableStateOf(false) }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة عنوان جديد لـ $customerName", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isLandscape) 180.dp else 400.dp)
                    .mouseVerticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("مسمى العنوان (مثلاً: المكتب / المنزل الثاني)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        label = { Text("المنطقة / الحي") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = feeStr,
                        onValueChange = { feeStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("رسوم التوصيل (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("تفاصيل الشارع والعمارة والشقة") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعيين كعنوان افتراضي رئيسي لهذا العميل", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (street.isNotBlank()) {
                        val fee = feeStr.toDoubleOrNull() ?: 20.0
                        onSave(title, area, street, fee, isDefault)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("إضافة العنوان")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun EditCustomerProfileDialog(
    customer: Customer,
    onSave: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.primaryPhone) }
    var secPhone by remember { mutableStateOf(customer.secondaryPhone ?: "") }
    var notes by remember { mutableStateOf(customer.notes) }
    val context = LocalContext.current

    val phoneDigits = phone.phoneDigitCount()
    val phoneWarning = phone.getCustomerPhoneWarning()
    val isPhoneInvalid = phone.isCustomerPhoneInvalid()
    val secPhoneDigits = secPhone.phoneDigitCount()
    val secPhoneWarning = secPhone.getCustomerPhoneWarning()
    val isSecPhoneInvalid = secPhone.isCustomerPhoneInvalid()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات العميل: ${customer.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف الأساسي") },
                    isError = isPhoneInvalid,
                    supportingText = {
                        if (phoneWarning != null) {
                            Text(
                                text = phoneWarning,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (phoneDigits == 11) {
                            Text(
                                text = "✓ رقم مكتمل (11 رقم)",
                                color = Color(0xFF16A34A),
                                fontSize = 10.sp
                            )
                        }
                    },
                    trailingIcon = {
                        if (isPhoneInvalid) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = phoneWarning ?: "تحذير: رقم غير صحيح",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (phoneDigits == 11) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "رقم مكتمل",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secPhone,
                    onValueChange = { secPhone = it },
                    label = { Text("رقم هاتف إضافي (اختياري)") },
                    isError = isSecPhoneInvalid,
                    supportingText = {
                        if (secPhoneWarning != null) {
                            Text(
                                text = secPhoneWarning,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (secPhoneDigits == 11) {
                            Text(
                                text = "✓ رقم مكتمل (11 رقم)",
                                color = Color(0xFF16A34A),
                                fontSize = 10.sp
                            )
                        }
                    },
                    trailingIcon = {
                        if (isSecPhoneInvalid) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = secPhoneWarning ?: "تحذير: رقم غير صحيح",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (secPhoneDigits == 11) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "رقم مكتمل",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات وتفضيلات العميل") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    phone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                        Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                    }
                    secPhone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                        Toast.makeText(context, toastMsg.replace("رقم هاتف العميل", "الرقم الإضافي"), Toast.LENGTH_LONG).show()
                    }
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSave(
                            customer.copy(
                                name = name.trim(),
                                primaryPhone = phone.trim(),
                                secondaryPhone = secPhone.trim().ifBlank { null },
                                notes = notes.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ التعديلات")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun EditAddressDialog(
    address: CustomerAddress,
    onSave: (CustomerAddress) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(address.title) }
    var area by remember { mutableStateOf(address.area) }
    var street by remember { mutableStateOf(address.streetDetails) }
    var feeStr by remember { mutableStateOf(String.format(Locale.US, "%.0f", address.deliveryFee)) }
    var isDefault by remember { mutableStateOf(address.isDefault) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل عنوان التوصيل", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("مسمى العنوان (مثلاً: المنزل / العمل)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        label = { Text("المنطقة / الحي") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = feeStr,
                        onValueChange = { feeStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("رسوم التوصيل (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("تفاصيل الشارع والعمارة والشقة") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعيين كعنوان افتراضي رئيسي", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (street.isNotBlank()) {
                        val fee = feeStr.toDoubleOrNull() ?: address.deliveryFee
                        onSave(
                            address.copy(
                                title = title.trim().ifBlank { "الرئيسي" },
                                area = area.trim(),
                                streetDetails = street.trim(),
                                deliveryFee = fee,
                                isDefault = isDefault
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ العنوان")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
