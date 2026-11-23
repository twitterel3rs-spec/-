package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import com.example.ui.components.MouseLazyColumn
import com.example.ui.components.MouseLazyRow
import com.example.ui.components.MouseLazyVerticalGrid
import com.example.ui.components.mouseVerticalScroll
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Text
import com.example.util.parseArabicDoubleOrNull
import com.example.util.phoneDigitCount
import com.example.util.isCustomerPhoneIncomplete
import com.example.util.isCustomerPhoneTooLong
import com.example.util.isCustomerPhoneInvalid
import com.example.util.getCustomerPhoneWarning
import com.example.util.getCustomerPhoneToastWarning
import com.example.util.normalizePhoneNumber
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import com.example.util.convertEnglishKeyboardToArabic
import com.example.util.containsLatinLetters
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.model.Customer
import com.example.data.model.CustomerAddress
import com.example.data.model.DeliveryDriver
import com.example.data.model.MenuItem
import com.example.data.model.MenuItemIngredient
import com.example.data.model.PreparationType
import com.example.data.model.Shift
import com.example.ui.components.ClosedShiftDetailDialog
import com.example.ui.components.QuickDrawerTransactionDialog
import com.example.ui.screens.settings.MenuItemEditDialog
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.BabyBluePrimary
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaNavyMedium
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.util.SoundAlertUtil
import com.example.viewmodel.CartItem
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val menuItems by viewModel.menuItems.collectAsStateWithLifecycle()
    val preparations by viewModel.preparations.collectAsStateWithLifecycle()
    val allIngredients by viewModel.allIngredients.collectAsStateWithLifecycle()
    val allPrinters by viewModel.allPrinters.collectAsStateWithLifecycle()
    val activeShift by viewModel.activeShift.collectAsStateWithLifecycle()
    val liveDrawerInfo by viewModel.liveDrawerInfo.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val orderType by viewModel.orderType.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val allCustomers by viewModel.customers.collectAsStateWithLifecycle()
    val orderCustomerPhone by viewModel.orderCustomerPhone.collectAsStateWithLifecycle()
    val editingOrderId by viewModel.editingOrderId.collectAsStateWithLifecycle()
    val editingOrderNumber by viewModel.editingOrderNumber.collectAsStateWithLifecycle()
    val orderPaidAmount by viewModel.orderPaidAmount.collectAsStateWithLifecycle()
    val orderIsPaid by viewModel.orderIsPaid.collectAsStateWithLifecycle()
    val deviceRole by viewModel.deviceRole.collectAsStateWithLifecycle()
    val activeSellerName by viewModel.activeSellerName.collectAsStateWithLifecycle()

    var showCartBottomSheet by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showCartBottomSheet) {
        showCartBottomSheet = false
    }

    BackHandler(enabled = editingOrderId != null && !showCartBottomSheet) {
        viewModel.cancelEditingOrder()
    }

    val customerAddresses by viewModel.customerAddresses.collectAsStateWithLifecycle()
    val selectedAddress by viewModel.selectedAddress.collectAsStateWithLifecycle()
    val drivers by viewModel.drivers.collectAsStateWithLifecycle()
    val selectedDriver by viewModel.selectedDriver.collectAsStateWithLifecycle()
    val deliveryFee by viewModel.deliveryFee.collectAsStateWithLifecycle()
    val discount by viewModel.discount.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.paymentMethod.collectAsStateWithLifecycle()
    val orderNotes by viewModel.orderNotes.collectAsStateWithLifecycle()
    val printDiagnosticStatus by viewModel.printDiagnosticStatus.collectAsStateWithLifecycle()
    val isPrintingOrder by viewModel.isPrintingOrder.collectAsStateWithLifecycle()
    val isSubmittingOrderState by viewModel.isSubmittingOrderState.collectAsStateWithLifecycle()
    val lastSubmittedOrder by viewModel.lastSubmittedOrder.collectAsStateWithLifecycle()
    val categoryColors by viewModel.categoryColors.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedCategory by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state for item preparation
    var itemToConfigure by remember { mutableStateOf<MenuItem?>(null) }
    var itemToSpecifyQuantity by remember { mutableStateOf<MenuItem?>(null) }
    var itemToAddStock by remember { mutableStateOf<MenuItem?>(null) }
    var showOpenShiftDialog by remember { mutableStateOf(false) }
    var showCloseShiftDialog by remember { mutableStateOf(false) }
    var showQuickDrawerTransactionDialog by remember { mutableStateOf(false) }
    var showDrawerBalanceDetailsDialog by remember { mutableStateOf(false) }
    var showCustomItemDialog by remember { mutableStateOf(false) }
    var showExternalFishDialog by remember { mutableStateOf(false) }
    var showQuickAddMenuItemDialog by remember { mutableStateOf(false) }

    val dbCategories by viewModel.categories.collectAsStateWithLifecycle()
    val categories = remember(dbCategories) {
        listOf("الكل") + dbCategories.map { it.name }
    }

    LaunchedEffect(categories) {
        if (selectedCategory !in categories) {
            selectedCategory = "الكل"
        }
    }

    val filteredItems = remember(menuItems, selectedCategory, searchQuery) {
        val normQuery = normalizePosSearch(searchQuery)
        val arConverted = if (searchQuery.containsLatinLetters()) normalizePosSearch(convertEnglishKeyboardToArabic(searchQuery)) else ""
        val baseItems = if (selectedCategory == "الكل") menuItems else menuItems.filter { it.category == selectedCategory }
        if (normQuery.isBlank()) {
            baseItems
        } else {
            baseItems.filter { item ->
                val name = normalizePosSearch(item.name)
                val cat = normalizePosSearch(item.category)
                val kitchen = normalizePosSearch(item.kitchenName)
                name.contains(normQuery) || cat.contains(normQuery) || kitchen.contains(normQuery) ||
                (arConverted.isNotBlank() && arConverted != normQuery && (name.contains(arConverted) || cat.contains(arConverted) || kitchen.contains(arConverted)))
            }
        }
    }

    val totalCartAmount = cartItems.sumOf { it.total }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF1F5F9),
        bottomBar = {
            if (!isLandscape && cartItems.isNotEmpty()) {
                Surface(
                    color = SeaNavyDeep,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCartBottomSheet = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "السلة (${cartItems.size} أصناف): ${String.format(Locale.US, "%.1f", totalCartAmount)} ج.م",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (selectedCustomer != null) {
                                Text(
                                    text = "العميل: ${selectedCustomer!!.name}",
                                    color = AmberCoral,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { showCartBottomSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("open_cart_button").height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (editingOrderId != null) "مراجعة وحفظ التعديل ⚡" else "معاينة ودفع",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Edit Mode Active Banner - Ultra-slim compact bar
            if (editingOrderId != null) {
                Surface(
                    color = AmberCoral.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AmberCoral),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isLandscape) 4.dp else 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = AmberCoral,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "تعديل أوردر #${editingOrderNumber ?: ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isLandscape) 11.5.sp else 11.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isLandscape) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(فارق المطبخ فقط ⚡)",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cancelEditingOrder() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                                border = BorderStroke(0.8.dp, StatusDanger),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("إلغاء ✕", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }

                            val isSeller = deviceRole == "SELLER"
                            Button(
                                onClick = { viewModel.saveOrderEdits(autoPrint = true, navigateToOrders = false) },
                                enabled = !isSubmittingOrderState && !isPrintingOrder,
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSeller) Color(0xFF2563EB) else SeaTealPrimary),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSeller) Icons.AutoMirrored.Filled.Send else Icons.Default.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isSeller) "حفظ وطباعة بالكاشير 🖨️" else "حفظ وطباعة 🖨️",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Print Diagnostic Pill: Ultra-slim, essential info only to avoid eating screen space
            if (printDiagnosticStatus != null) {
                val rawStatus = printDiagnosticStatus!!
                val isSuccess = rawStatus.contains("طُبع") || rawStatus.contains("نجاح") || rawStatus.contains("✓")
                val isError = rawStatus.contains("فشل") || rawStatus.contains("خطأ") || rawStatus.contains("تعذر") || rawStatus.contains("عجز")

                // Extract concise essential message
                val displayMsg = remember(rawStatus) {
                    if (rawStatus.contains("ConnectException") || rawStatus.contains("timed out") || rawStatus.contains("ECONNREFUSED")) {
                        val p = rawStatus.substringBefore(":").replace("فشل طباعة أوردر", "").replace("نتيجة الإرسال", "").trim()
                        "تعذر الاتصال بالطابعة ($p)"
                    } else {
                        rawStatus.replace("نتيجة الإرسال: ", "")
                    }
                }

                Surface(
                    color = if (isSuccess) Color(0xFFF0FDF4)
                            else if (isError) Color(0xFFFFF1F2)
                            else Color(0xFFF0F9FF),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSuccess) Color(0xFFBBF7D0)
                        else if (isError) Color(0xFFFECDD3)
                        else Color(0xFFBAE6FD)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isLandscape) 4.dp else 8.dp, vertical = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPrintingOrder) {
                                CircularProgressIndicator(modifier = Modifier.size(11.dp), strokeWidth = 1.5.dp, color = BabyBluePrimary)
                            } else {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle
                                                  else if (isError) Icons.Default.Error
                                                  else Icons.Default.Print,
                                    contentDescription = null,
                                    tint = if (isSuccess) Color(0xFF16A34A)
                                           else if (isError) Color(0xFFE11D48)
                                           else BabyBluePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = displayMsg,
                                fontSize = if (isLandscape) 9.5.sp else 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSuccess) Color(0xFF14532D)
                                        else if (isError) Color(0xFF9F1239)
                                        else Color(0xFF0369A1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearPrintDiagnosticStatus() },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                modifier = Modifier.size(11.dp),
                                tint = if (isSuccess) Color(0xFF16A34A)
                                       else if (isError) Color(0xFFE11D48)
                                       else Color(0xFF0369A1)
                            )
                        }
                    }
                }
            }

            if (!isLandscape) {
                // Portrait Mode: Single column with all POS elements
                // 1. Compact Shift & Cash Drawer Bar
                PosShiftAndDrawerBar(
                    activeShift = activeShift,
                    liveDrawerInfo = liveDrawerInfo,
                    onOpenDrawerDetails = { showDrawerBalanceDetailsDialog = true },
                    onQuickTransaction = { showQuickDrawerTransactionDialog = true },
                    onCloseShift = { showCloseShiftDialog = true },
                    onOpenShift = { showOpenShiftDialog = true },
                    onAttendance = { viewModel.openAttendanceBoard() },
                    deviceRole = deviceRole,
                    sellerName = activeSellerName,
                    onOpenRoleSwitcher = { viewModel.toggleRoleSwitcherDialog() }
                )

                // 1.5 Quick Customer Phone Search Bar (سجل الرقم أثناء رنين الهاتف)
                PosCustomerQuickPhoneBar(
                    viewModel = viewModel,
                    allCustomers = allCustomers,
                    selectedCustomer = selectedCustomer,
                    orderCustomerPhone = orderCustomerPhone,
                    isLandscape = false
                )

                // 2. Search & Categories
                PosSearchAndCategoriesBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    categoryColors = categoryColors
                )

                // 3. Action Buttons Row
                PosActionButtonsRow(
                    filteredCount = filteredItems.size,
                    onExternalFishClick = { showExternalFishDialog = true },
                    onCustomItemClick = { showCustomItemDialog = true }
                )

                // 4. Catalog Grid & State Handling
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    val screenWidth = maxWidth
                    val columnsCount = when {
                        screenWidth < 600.dp -> 3
                        screenWidth < 840.dp -> 4
                        screenWidth < 1100.dp -> 6
                        else -> 8
                    }

                    if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (searchQuery.isNotBlank() || selectedCategory != "الكل") {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "لا توجد أصناف مطابقة للبحث",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    TextButton(onClick = { searchQuery = ""; selectedCategory = "الكل" }) {
                                        Text("عرض جميع الأصناف", fontSize = 11.sp, color = SeaTealPrimary)
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "لا توجد أصناف مضافة حالياً",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    } else {

                    MouseLazyVerticalGrid(
                        columns = GridCells.Fixed(columnsCount),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            bottom = if (cartItems.isNotEmpty()) 90.dp else 24.dp,
                            top = 4.dp
                        )
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            MenuItemGridTile(
                                item = item,
                                customThemeId = categoryColors[item.category],
                                onAddStockClick = { itemToAddStock = item },
                                onClick = {
                                    if (item.currentStock <= 0) {
                                        itemToAddStock = item
                                    } else if (item.supportsPreparation) {
                                        itemToConfigure = item
                                    } else {
                                        viewModel.addToCart(item, 1.0, null)
                                    }
                                },
                                onLongClick = {
                                    if (item.currentStock <= 0) {
                                        itemToAddStock = item
                                    } else {
                                        itemToSpecifyQuantity = item
                                    }
                                }
                            )
                        }

                        // Square '+' card after the last item in category
                        item(key = "quick_add_tile") {
                            QuickAddMenuItemTile(
                                onClick = { showQuickAddMenuItemDialog = true },
                                isLandscapeCompact = false,
                                currentCategory = selectedCategory
                            )
                        }
                    }
                    }
                }
            } else {
                // Landscape Mode: Split 50/50 - Right side is POS (Everything from portrait), Left side is Orders
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ==========================================
                        // RIGHT HALF (النصف الأيمن): نقطة البيع (POS)
                        // ==========================================
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // 1. Compact Shift & Cash Drawer Bar (Exact same features as portrait)
                                PosShiftAndDrawerBar(
                                    activeShift = activeShift,
                                    liveDrawerInfo = liveDrawerInfo,
                                    onOpenDrawerDetails = { showDrawerBalanceDetailsDialog = true },
                                    onQuickTransaction = { showQuickDrawerTransactionDialog = true },
                                    onCloseShift = { showCloseShiftDialog = true },
                                    onOpenShift = { showOpenShiftDialog = true },
                                    onAttendance = { viewModel.openAttendanceBoard() },
                                    isLandscape = true,
                                    deviceRole = deviceRole,
                                    sellerName = activeSellerName,
                                    onOpenRoleSwitcher = { viewModel.toggleRoleSwitcherDialog() }
                                )

                                // 1.5 Quick Customer Phone Search Bar (سجل الرقم أثناء رنين الهاتف)
                                PosCustomerQuickPhoneBar(
                                    viewModel = viewModel,
                                    allCustomers = allCustomers,
                                    selectedCustomer = selectedCustomer,
                                    orderCustomerPhone = orderCustomerPhone,
                                    isLandscape = true
                                )

                                // 2. Search & Categories
                                PosSearchAndCategoriesBar(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    onSelectCategory = { selectedCategory = it },
                                    isLandscape = true,
                                    categoryColors = categoryColors
                                )

                                // 3. Action Buttons Row
                                PosActionButtonsRow(
                                    filteredCount = filteredItems.size,
                                    onExternalFishClick = { showExternalFishDialog = true },
                                    onCustomItemClick = { showCustomItemDialog = true },
                                    isLandscape = true
                                )

                                // 4. Catalog Grid (3 columns in right half, compact cards)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                ) {
                                    if (filteredItems.isEmpty()) {
                                        if (searchQuery.isBlank()) {
                                            MouseLazyVerticalGrid(
                                                columns = GridCells.Adaptive(minSize = 92.dp),
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                contentPadding = PaddingValues(bottom = 4.dp, top = 1.dp)
                                            ) {
                                                item(key = "quick_add_tile") {
                                                    QuickAddMenuItemTile(
                                                        onClick = { showQuickAddMenuItemDialog = true },
                                                        isLandscapeCompact = true,
                                                        currentCategory = selectedCategory
                                                    )
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                                        tint = Color.LightGray,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(Modifier.height(3.dp))
                                                    Text(
                                                        text = "لا توجد أصناف مطابقة",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    if (searchQuery.isNotBlank() || selectedCategory != "الكل") {
                                                        Spacer(Modifier.height(3.dp))
                                                        TextButton(
                                                            onClick = { searchQuery = ""; selectedCategory = "الكل" },
                                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                            modifier = Modifier.height(24.dp)
                                                        ) {
                                                            Text("عرض الكل", fontSize = 10.sp, color = SeaTealPrimary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        MouseLazyVerticalGrid(
                                            columns = GridCells.Adaptive(minSize = 92.dp),
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(3.dp),
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            contentPadding = PaddingValues(bottom = 4.dp, top = 1.dp)
                                        ) {
                                            items(filteredItems, key = { it.id }) { item ->
                                                MenuItemGridTile(
                                                    item = item,
                                                    customThemeId = categoryColors[item.category],
                                                    onAddStockClick = { itemToAddStock = item },
                                                    onClick = {
                                                        if (item.currentStock <= 0) {
                                                            itemToAddStock = item
                                                        } else if (item.supportsPreparation) {
                                                            itemToConfigure = item
                                                        } else {
                                                            viewModel.addToCart(item, 1.0, null)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (item.currentStock <= 0) {
                                                            itemToAddStock = item
                                                        } else {
                                                            itemToSpecifyQuantity = item
                                                        }
                                                    },
                                                    isLandscapeCompact = true
                                                )
                                            }

                                            // Square '+' card after the last item in category
                                            item(key = "quick_add_tile") {
                                                QuickAddMenuItemTile(
                                                    onClick = { showQuickAddMenuItemDialog = true },
                                                    isLandscapeCompact = true,
                                                    currentCategory = selectedCategory
                                                )
                                            }
                                        }
                                    }
                                }

                                // 5. Docked Bottom Cart Bar in POS Half (When cart has items)
                                if (cartItems.isNotEmpty()) {
                                    Surface(
                                        color = SeaNavyDeep,
                                        shadowElevation = 8.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showCartBottomSheet = true }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "السلة (${cartItems.size} أصناف): ${String.format(Locale.US, "%.1f", totalCartAmount)} ج.م",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                if (selectedCustomer != null) {
                                                    Text(
                                                        text = "العميل: ${selectedCustomer!!.name}",
                                                        color = AmberCoral,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = { showCartBottomSheet = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.testTag("open_cart_button").height(36.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("معاينة ودفع", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // تفاصيل الأوردر والدفع في الوضع المائل - تأخذ شاشة البيع فقط لتبقى شاشة الأوردرات ظاهرة للكاشير
                            if (showCartBottomSheet) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 6.dp,
                                    shadowElevation = 8.dp
                                ) {
                                    CartCheckoutContent(
                                        viewModel = viewModel,
                                        cartItems = cartItems,
                                        orderType = orderType,
                                        selectedCustomer = selectedCustomer,
                                        customerAddresses = customerAddresses,
                                        selectedAddress = selectedAddress,
                                        drivers = drivers,
                                        selectedDriver = selectedDriver,
                                        deliveryFee = deliveryFee,
                                        discount = discount,
                                        paymentMethod = paymentMethod,
                                        orderNotes = orderNotes,
                                        isLandscape = true,
                                        onDismiss = { showCartBottomSheet = false }
                                    )
                                }
                            }
                        }

                        // ==========================================
                        // VERTICAL DIVIDER: خط فاصل بين النصفين
                        // ==========================================
                        VerticalDivider(
                            thickness = 1.dp,
                            color = Color(0xFFCBD5E1)
                        )

                        // ==========================================
                        // LEFT HALF (النصف الأيسر): الأوردرات (Orders)
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFFF8FAFC))
                        ) {
                            Surface(
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "📋 متابعة وإدارة الأوردرات والتوصيل",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SeaTealPrimary.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "مباشر للكاشير",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Live Orders Screen embedded directly!
                            OrdersScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // Configure Fresh Fish Item (Weight and Preparation)
    itemToConfigure?.let { item ->
        ItemPreparationDialog(
            item = item,
            preparations = preparations,
            allIngredients = allIngredients,
            menuItems = menuItems,
            onAddToCart = { qty, prep, notes ->
                viewModel.addToCart(item, qty, prep, notes)
                itemToConfigure = null
            },
            onDismiss = { itemToConfigure = null }
        )
    }

    // Quick Add Menu Item with custom typed quantity (e.g. from long-click or direct specify)
    itemToSpecifyQuantity?.let { item ->
        if (item.supportsPreparation) {
            ItemPreparationDialog(
                item = item,
                preparations = preparations,
                allIngredients = allIngredients,
                menuItems = menuItems,
                onAddToCart = { qty, prep, notes ->
                    viewModel.addToCart(item, qty, prep, notes)
                    itemToSpecifyQuantity = null
                },
                onDismiss = { itemToSpecifyQuantity = null }
            )
        } else {
            QuickAddMenuQuantityDialog(
                item = item,
                onAddToCart = { qty ->
                    viewModel.addToCart(item, qty, null)
                    itemToSpecifyQuantity = null
                },
                onDismiss = { itemToSpecifyQuantity = null }
            )
        }
    }

    // Quick Add Stock Dialog
    itemToAddStock?.let { item ->
        QuickAddStockDialog(
            item = item,
            onConfirm = { addedQty ->
                viewModel.receiveShipment(item.id, addedQty, "إضافة سريعة فورية من شاشة البيع")
                itemToAddStock = null
            },
            onDismiss = { itemToAddStock = null }
        )
    }

    // Quick Add Menu Item directly from POS Grid (Identical to Settings with full printer selection)
    if (showQuickAddMenuItemDialog) {
        val catList = remember(categories) {
            categories.filter { it != "الكل" && it.isNotBlank() }
        }
        val defaultCat = if (selectedCategory != "الكل" && selectedCategory.isNotBlank()) selectedCategory else (catList.firstOrNull() ?: "أسماك")
        MenuItemEditDialog(
            item = null,
            categories = catList,
            availableMenuItems = menuItems,
            existingIngredients = emptyList(),
            allPrinters = allPrinters,
            initialCategory = defaultCat,
            onSave = { newItem, linkedIngredients ->
                viewModel.saveMenuItemWithIngredients(newItem, linkedIngredients)
                showQuickAddMenuItemDialog = false
            },
            onDismiss = { showQuickAddMenuItemDialog = false }
        )
    }

    // Open Shift Dialog
    if (showOpenShiftDialog) {
        OpenShiftDialog(
            onConfirm = { cashierName, openingCash ->
                viewModel.openShift(cashierName, openingCash)
                showOpenShiftDialog = false
            },
            onDismiss = { showOpenShiftDialog = false }
        )
    }

    // Close Shift Dialog
    if (showCloseShiftDialog) {
        activeShift?.let { shift ->
            CloseShiftDialog(
                shift = shift,
                viewModel = viewModel,
                onConfirm = { actualCash, expenses, notes, pass ->
                    coroutineScope.launch {
                        if (viewModel.verifyManagerPassword(pass)) {
                            viewModel.closeShift(actualCash, expenses, notes)
                            showCloseShiftDialog = false
                        } else {
                            viewModel.userMessage.value = "كلمة سر الإدارة غير صحيحة!"
                        }
                    }
                },
                onDismiss = { showCloseShiftDialog = false }
            )
        }
    }

    // Quick Cash Drawer Transaction Dialog (In / Out)
    if (showQuickDrawerTransactionDialog) {
        QuickDrawerTransactionDialog(
            currentDrawerBalance = liveDrawerInfo.currentDrawerBalance,
            accounts = allAccounts,
            onConfirm = { type, amount, reason, accountId ->
                viewModel.recordQuickDrawerTransaction(type, amount, reason, accountId)
                showQuickDrawerTransactionDialog = false
            },
            onDismiss = { showQuickDrawerTransactionDialog = false }
        )
    }

    // Drawer Balance Details Breakdown Dialog (Requested: تفاصيل رصيد الخزينة بالكامل عند الضغط بدون أخذ مساحة من الشاشة)
    if (showDrawerBalanceDetailsDialog && activeShift != null) {
        val st = liveDrawerInfo
        AlertDialog(
            onDismissRequest = { showDrawerBalanceDetailsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = Color(0xFF1B5E20),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("كشف رصيد الخزينة والدرج", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("وردية #${activeShift!!.shiftNumber} • الكاشير: ${activeShift!!.cashierName}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    IconButton(onClick = { showDrawerBalanceDetailsDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .mouseVerticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Net Current Cash Drawer Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE8F5E9),
                        border = BorderStroke(1.2.dp, Color(0xFFA5D6A7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("رصيد الخزينة الفعلي المتواجد بالدرج حالياً", fontSize = 11.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format(Locale.US, "%,.1f", st.currentDrawerBalance)} ج.م",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }

                    // Detailed Breakdown Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("💵 عهدة البداية (رصيد افتتاحي):", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("${String.format(Locale.US, "%.1f", st.openingBalance)} ج.م", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            Text("🟢 المقبوضات والوارد للدرج:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SeaTealPrimary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("  • مبيعات كاش المحل (${st.ordersCount} أوردر):", fontSize = 11.sp, color = Color.DarkGray)
                                Text("+${String.format(Locale.US, "%.1f", st.storeCashSales)} ج.م", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 11.5.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("  • توريد تسويات الدليفري:", fontSize = 11.sp, color = Color.DarkGray)
                                Text("+${String.format(Locale.US, "%.1f", st.driverSettlementsCash)} ج.م", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 11.5.sp)
                            }
                            if (st.totalReceipts > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("  • مقبوضات عامة للدرج:", fontSize = 11.sp, color = Color.DarkGray)
                                    Text("+${String.format(Locale.US, "%.1f", st.totalReceipts)} ج.م", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 11.5.sp)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            Text("🔴 المدفوعات والمنصرف من الدرج:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = StatusDanger)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("  • سلف ومسحوبات الموظفين:", fontSize = 11.sp, color = Color.DarkGray)
                                Text("-${String.format(Locale.US, "%.1f", st.totalStaffWithdrawals)} ج.م", fontWeight = FontWeight.Bold, color = StatusDanger, fontSize = 11.5.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("  • مصروفات ونثريات ودفعات الموردين:", fontSize = 11.sp, color = Color.DarkGray)
                                Text("-${String.format(Locale.US, "%.1f", st.totalExpensesAndPayments)} ج.م", fontWeight = FontWeight.Bold, color = StatusDanger, fontSize = 11.5.sp)
                            }

                            if (st.walletSales > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("📱 مبيعات محافظ إلكترونية (خارج الدرج):", fontSize = 11.sp, color = AmberCoral)
                                    Text("${String.format(Locale.US, "%.1f", st.walletSales)} ج.م", fontSize = 11.sp, color = AmberCoral, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDrawerBalanceDetailsDialog = false
                        showQuickDrawerTransactionDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Icon(imageVector = Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معاملة سريعة بالخزينة ⚡")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDrawerBalanceDetailsDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }


    // Automatically display shift statement popup upon shift closing
    val recentlyClosedShift by viewModel.recentlyClosedShift.collectAsStateWithLifecycle()
    recentlyClosedShift?.let { closedShift ->
        ClosedShiftDetailDialog(
            shift = closedShift,
            viewModel = viewModel,
            onDismiss = { viewModel.clearRecentlyClosedShift() }
        )
    }

    // AdHoc Custom Item Dialog
    if (showCustomItemDialog) {
        val allPrintersForCustom by viewModel.allPrinters.collectAsStateWithLifecycle()
        CustomAdHocItemDialog(
            allPrinters = allPrintersForCustom,
            onAdd = { name, unit, qty, price, notes, targetPrinter ->
                viewModel.addAdHocCustomItem(name, unit, qty, price, notes, targetPrinter)
                showCustomItemDialog = false
            },
            onDismiss = { showCustomItemDialog = false }
        )
    }

    // External Fish Prep Dialog
    if (showExternalFishDialog) {
        ExternalFishPrepDialog(
            preparations = preparations,
            menuItems = menuItems,
            onAdd = { fishName, prepName, weight, prepPrice, notes ->
                viewModel.addExternalFishPreparationItem(fishName, prepName, weight, prepPrice, notes)
                showExternalFishDialog = false
            },
            onDismiss = { showExternalFishDialog = false }
        )
    }

    // Cart & Checkout Dialog (Preview and Payment - قايمة المعاينة والدفع في الوضع الرأسي فقط)
    if (showCartBottomSheet && !isLandscape) {
        Dialog(
            onDismissRequest = { /* dismissOnClickOutside is false so cashier work is never lost accidentally */ },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .fillMaxHeight(0.94f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp
                ) {
                    CartCheckoutContent(
                        viewModel = viewModel,
                        cartItems = cartItems,
                        orderType = orderType,
                        selectedCustomer = selectedCustomer,
                        customerAddresses = customerAddresses,
                        selectedAddress = selectedAddress,
                        drivers = drivers,
                        selectedDriver = selectedDriver,
                        deliveryFee = deliveryFee,
                        discount = discount,
                        paymentMethod = paymentMethod,
                        orderNotes = orderNotes,
                        isLandscape = false,
                        onDismiss = { showCartBottomSheet = false }
                    )
                }
            }
        }
    }

    // Order Completion & Invoice Sending Modal Dialog
    lastSubmittedOrder?.let { placedOrder ->
        AlertDialog(
            onDismissRequest = { viewModel.clearLastSubmittedOrder() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تم تسجيل الأوردر #${placedOrder.order.orderNumber} بنجاح ✓",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = StatusSuccess
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .mouseVerticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val custLabel = if (placedOrder.order.customerName.isNotBlank() && placedOrder.order.customerName != "زبون مباشر") {
                        if (placedOrder.order.customerPhone.isNotBlank()) "${placedOrder.order.customerName} (${placedOrder.order.customerPhone})" else placedOrder.order.customerName
                    } else {
                        if (placedOrder.order.customerPhone.isNotBlank()) "هاتف: ${placedOrder.order.customerPhone}" else "زبون مباشر"
                    }
                    Text(
                        text = "• العميل: $custLabel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "• نوع الطلب: ${if (placedOrder.order.orderType == "DELIVERY") "دليفري توصيل" else "استلام من المحل"}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• إجمالي الحساب: ${String.format(Locale.US, "%.1f", placedOrder.order.totalAmount)} ج.م",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SeaTealPrimary
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "خيارات إرسال الفاتورة والطباعة السريعة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    // 1. WhatsApp Button
                    Button(
                        onClick = {
                            if (placedOrder.order.customerPhone.isNotBlank()) {
                                viewModel.shareInvoiceViaWhatsApp(placedOrder, placedOrder.order.customerPhone, context)
                            } else {
                                android.widget.Toast.makeText(context, "لم يتم تسجيل هاتف للعميل", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال الفاتورة للعميل عبر واتساب 📲", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    // 2. Print Button
                    OutlinedButton(
                        onClick = {
                            viewModel.printReceipt(placedOrder.order.id, "MAIN")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة الفاتورة للكاشير 🖨️", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearLastSubmittedOrder() },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("أوردر جديد / إغلاق")
                }
            }
        )
    }
}

@Composable
private fun PosShiftAndDrawerBar(
    activeShift: Shift?,
    liveDrawerInfo: com.example.viewmodel.LiveDrawerInfo,
    onOpenDrawerDetails: () -> Unit,
    onQuickTransaction: () -> Unit,
    onCloseShift: () -> Unit,
    onOpenShift: () -> Unit,
    onAttendance: () -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false,
    deviceRole: String = "CASHIER",
    sellerName: String = "",
    onOpenRoleSwitcher: () -> Unit = {}
) {
    Surface(
        color = if (activeShift != null) Color(0xFFF1F8F5) else Color(0xFFFFF7ED),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isLandscape) 4.dp else 8.dp, vertical = if (isLandscape) 1.dp else 2.dp),
        shape = RoundedCornerShape(if (isLandscape) 8.dp else 14.dp),
        border = BorderStroke(
            1.dp,
            if (activeShift != null) Color(0xFFC8E6C9) else AmberCoral.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isLandscape) 6.dp else 8.dp, vertical = if (isLandscape) 1.5.dp else 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Role Switcher Pill
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(if (isLandscape) 6.dp else 12.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = if (isLandscape) 5.dp else 7.dp, vertical = if (isLandscape) 1.5.dp else 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(if (isLandscape) 11.dp else 13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "كاشير 🖥️",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isLandscape) 9.sp else 10.sp,
                            color = Color(0xFF334155)
                        )
                    }
                }

                // Cash Drawer Pill
            if (activeShift != null) {
                Surface(
                    onClick = onOpenDrawerDetails,
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(if (isLandscape) 6.dp else 12.dp),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = if (isLandscape) 5.dp else 7.dp, vertical = if (isLandscape) 1.5.dp else 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(if (isLandscape) 11.dp else 13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "الخزينة: ${String.format(Locale.US, "%,.0f", liveDrawerInfo.currentDrawerBalance)} ج",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isLandscape) 10.sp else 11.sp,
                            color = Color(0xFF14532D)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "#${activeShift.shiftNumber}",
                            fontSize = if (isLandscape) 8.5.sp else 9.5.sp,
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(if (isLandscape) 11.dp else 14.dp)
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AmberCoral,
                        modifier = Modifier.size(if (isLandscape) 11.dp else 13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "الوردية مغلقة",
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isLandscape) 10.sp else 11.sp,
                        color = Color(0xFF9A3412)
                    )
                }
            }
            }

            // Action Buttons in Shift Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (activeShift != null) {
                    Button(
                        onClick = onQuickTransaction,
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                        contentPadding = PaddingValues(horizontal = if (isLandscape) 4.dp else 6.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.height(if (isLandscape) 22.dp else 26.dp).testTag("quick_drawer_tx_button")
                    ) {
                        Text("⚡ سريعة", fontSize = if (isLandscape) 8.5.sp else 9.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onCloseShift,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                        contentPadding = PaddingValues(horizontal = if (isLandscape) 4.dp else 6.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.height(if (isLandscape) 22.dp else 26.dp).testTag("close_shift_button")
                    ) {
                        Text("تقفيل 🔒", fontSize = if (isLandscape) 8.5.sp else 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onOpenShift,
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.height(if (isLandscape) 22.dp else 26.dp)
                    ) {
                        Text("بدء وردية +", fontSize = if (isLandscape) 8.5.sp else 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onAttendance,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SeaNavyDeep),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    contentPadding = PaddingValues(horizontal = if (isLandscape) 4.dp else 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier.height(if (isLandscape) 22.dp else 26.dp)
                ) {
                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(if (isLandscape) 9.dp else 11.dp))
                    Spacer(modifier = Modifier.width(1.dp))
                    Text("حضور", fontSize = if (isLandscape) 8.5.sp else 9.5.sp, color = Color(0xFF475569))
                }
            }
        }
    }
}

@Composable
private fun PosCustomerQuickPhoneBar(
    viewModel: MainViewModel,
    allCustomers: List<Customer>,
    selectedCustomer: Customer?,
    orderCustomerPhone: String,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    var phoneInput by rememberSaveable { mutableStateOf("") }
    val cleanDigits = remember(phoneInput) {
        phoneInput.normalizePhoneNumber().filter { it.isDigit() }
    }
    val digitCount = cleanDigits.length
    val isIncomplete = phoneInput.isCustomerPhoneIncomplete()
    val isTooLong = phoneInput.isCustomerPhoneTooLong()
    val isComplete = digitCount == 11

    // Matching customers lookup (real-time as phone rings)
    val matchingCustomers = remember(cleanDigits, phoneInput, allCustomers) {
        if (cleanDigits.length < 2 && phoneInput.trim().length < 2) emptyList()
        else {
            val qNorm = phoneInput.trim().lowercase(Locale.getDefault())
            allCustomers.filter { c ->
                val p1 = c.primaryPhone.normalizePhoneNumber()
                val p2 = c.secondaryPhone?.normalizePhoneNumber() ?: ""
                (cleanDigits.isNotEmpty() && (p1.contains(cleanDigits) || p2.contains(cleanDigits))) ||
                (qNorm.isNotEmpty() && c.name.lowercase(Locale.getDefault()).contains(qNorm))
            }.take(5)
        }
    }

    // Has exact match
    val hasExactMatch = remember(cleanDigits, allCustomers) {
        if (cleanDigits.length < 8) false
        else allCustomers.any {
            it.primaryPhone.normalizePhoneNumber() == cleanDigits ||
            it.secondaryPhone?.normalizePhoneNumber() == cleanDigits
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isLandscape) 4.dp else 8.dp, vertical = 2.dp)
    ) {
        // Slim Input Bar (شريط رفيع جداً للبحث برقم الهاتف)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = BorderStroke(
                1.dp,
                when {
                    isIncomplete -> AmberCoral.copy(alpha = 0.8f)
                    isComplete -> StatusSuccess.copy(alpha = 0.8f)
                    isTooLong -> StatusDanger.copy(alpha = 0.8f)
                    else -> Color(0xFFCBD5E1)
                }
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = if (isLandscape) 4.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phone Icon
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = if (isComplete) StatusSuccess else if (isIncomplete) AmberCoral else SeaTealPrimary,
                    modifier = Modifier.size(if (isLandscape) 16.dp else 18.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Slim Input Box
                Box(modifier = Modifier.weight(1f)) {
                    if (phoneInput.isEmpty()) {
                        Text(
                            text = if (isLandscape) "📞 رقم العميل (سجل الرقم والموبايل بيرن)..." else "📞 بحث برقم العميل (سجل الرقم والموبايل بيرن)...",
                            fontSize = if (isLandscape) 11.sp else 12.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Search
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = if (isLandscape) 12.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SeaNavyDeep,
                            textDirection = TextDirection.Ltr
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_customer_phone_input")
                    )
                }

                // Incomplete / Complete Warning Pill Badge ("خاصية إخباري أن الرقم ناقص")
                if (phoneInput.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when {
                            isIncomplete -> Color(0xFFFEF3C7)
                            isComplete -> Color(0xFFDCFCE7)
                            isTooLong -> Color(0xFFFFE4E6)
                            else -> Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            0.5.dp,
                            when {
                                isIncomplete -> AmberCoral
                                isComplete -> StatusSuccess
                                isTooLong -> StatusDanger
                                else -> Color.Gray
                            }
                        )
                    ) {
                        Text(
                            text = when {
                                isIncomplete -> "⚠️ الرقم ناقص ($digitCount/11)"
                                isComplete -> "✓ رقم مكتمل (11)"
                                isTooLong -> "⚠️ الرقم زائد ($digitCount)"
                                else -> "$digitCount أرقام"
                            },
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isIncomplete -> Color(0xFF92400E)
                                isComplete -> Color(0xFF166534)
                                isTooLong -> Color(0xFF991B1B)
                                else -> Color.DarkGray
                            },
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // Clear button
                    IconButton(
                        onClick = { phoneInput = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "مسح",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else if (selectedCustomer != null || orderCustomerPhone.isNotBlank()) {
                    // Show current customer badge when input is empty
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SeaTealPrimary.copy(alpha = 0.1f),
                        border = BorderStroke(0.5.dp, SeaTealPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "👤 للأوردر: ${selectedCustomer?.name ?: orderCustomerPhone}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SeaTealPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إلغاء اختيار العميل",
                                tint = StatusDanger,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { viewModel.clearSelectedCustomer() }
                            )
                        }
                    }
                }
            }
        }

        // Matching Numbers Chips / Results Row (تظهر الأرقام والعملاء المتطابقين فورا)
        if (matchingCustomers.isNotEmpty() || (cleanDigits.length >= 7 && !hasExactMatch)) {
            Spacer(modifier = Modifier.height(3.dp))
            MouseLazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If this is a new number not in database, show 1-click quick assign button
                if (cleanDigits.length >= 7 && !hasExactMatch) {
                    item(key = "new_number_chip") {
                        Surface(
                            onClick = {
                                viewModel.clearSelectedCustomer()
                                viewModel.orderCustomerPhone.value = cleanDigits
                                viewModel.orderType.value = "DELIVERY"
                                viewModel.userMessage.value = "⚡ تم تجهيز أوردر دليفري للرقم الجديد: $cleanDigits — جاهز لاستقبال الطلب فوراً!"
                                phoneInput = ""
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = AmberCoral.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, AmberCoral)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = AmberCoral, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "⚡ أوردر جديد للرقم: $cleanDigits",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }

                // Matching Customers from database
                items(matchingCustomers, key = { it.id }) { cust ->
                    Surface(
                        onClick = {
                            viewModel.selectCustomer(cust)
                            viewModel.orderType.value = "DELIVERY"
                            viewModel.userMessage.value = "✓ تم اختيار العميل: ${cust.name} (${cust.primaryPhone})"
                            phoneInput = ""
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(13.dp))
                            Text(
                                text = cust.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14532D)
                            )
                            Text(
                                text = cust.primaryPhone,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF15803D),
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                            )
                            if (cust.totalOrdersCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = AmberCoral.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${cust.totalOrdersCount} طلب",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
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
private fun PosSearchAndCategoriesBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false,
    categoryColors: Map<String, String> = emptyMap()
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isLandscape) 4.dp else 8.dp, vertical = if (isLandscape) 1.dp else 3.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = if (isLandscape) "🔍 بحث في الأصناف (اسم الصنف، الطهي، الكود)..." else "🔍 ابحث في قائمة الأصناف (بلطي، جمبري، طواجن)...",
                    fontSize = if (isLandscape) 11.5.sp else 13.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث في الأصناف",
                    tint = SeaTealPrimary,
                    modifier = Modifier.size(if (isLandscape) 18.dp else 22.dp)
                )
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (searchQuery.containsLatinLetters()) {
                        val converted = convertEnglishKeyboardToArabic(searchQuery)
                        if (converted != searchQuery) {
                            Surface(
                                onClick = { onSearchQueryChange(converted) },
                                shape = RoundedCornerShape(4.dp),
                                color = SeaTealPrimary.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, SeaTealPrimary),
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .height(if (isLandscape) 24.dp else 28.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تحويل لعربي",
                                        tint = SeaTealPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "عربي",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SeaTealPrimary
                                    )
                                }
                            }
                        }
                    }
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(if (isLandscape) 24.dp else 28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح البحث",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(if (isLandscape) 15.dp else 18.dp)
                            )
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                imeAction = ImeAction.Search,
                autoCorrect = false
            ),
            shape = RoundedCornerShape(if (isLandscape) 10.dp else 12.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = if (isLandscape) 12.sp else 14.sp,
                fontWeight = FontWeight.Medium,
                textDirection = TextDirection.ContentOrRtl
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF8FAFC),
                focusedBorderColor = SeaTealPrimary,
                unfocusedBorderColor = Color(0xFFCBD5E1)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = if (isLandscape) 46.dp else 52.dp)
                .testTag("pos_search_input")
        )

        Spacer(modifier = Modifier.height(if (isLandscape) 1.dp else 3.dp))

        MouseLazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (isLandscape) 1.dp else 2.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 3.dp else 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                val catStyle = getCategoryStyle(cat, categoryColors[cat])
                val activeThemeColor = if (categoryColors[cat] != null) catStyle.priceBg else SeaTealPrimary
                Surface(
                    onClick = { onSelectCategory(cat) },
                    shape = RoundedCornerShape(if (isLandscape) 8.dp else 12.dp),
                    color = if (isSelected) activeThemeColor else catStyle.bg,
                    border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, if (isSelected) activeThemeColor else catStyle.border),
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Text(
                        text = cat,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (isLandscape) 9.5.sp else 11.5.sp,
                        color = if (isSelected) Color.White else catStyle.tagText,
                        modifier = Modifier.padding(horizontal = if (isLandscape) 6.dp else 9.dp, vertical = if (isLandscape) 2.dp else 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PosActionButtonsRow(
    filteredCount: Int,
    onExternalFishClick: () -> Unit,
    onCustomItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isLandscape) 4.dp else 10.dp, vertical = if (isLandscape) 1.dp else 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "الأصناف ($filteredCount):",
            fontWeight = FontWeight.Bold,
            style = if (isLandscape) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
            fontSize = if (isLandscape) 11.sp else 13.sp
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onExternalFishClick,
                colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                contentPadding = PaddingValues(horizontal = if (isLandscape) 5.dp else 8.dp, vertical = 1.dp),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .height(if (isLandscape) 24.dp else 30.dp)
                    .testTag("add_external_fish_button")
            ) {
                Icon(imageVector = Icons.Default.SetMeal, contentDescription = null, modifier = Modifier.size(if (isLandscape) 10.dp else 13.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("🐟 سمك خارجي", fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 9.sp else 10.5.sp, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
                onClick = onCustomItemClick,
                contentPadding = PaddingValues(horizontal = if (isLandscape) 5.dp else 8.dp, vertical = 1.dp),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .height(if (isLandscape) 24.dp else 30.dp)
                    .testTag("add_custom_item_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(if (isLandscape) 10.dp else 12.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("+ استثنائي", fontSize = if (isLandscape) 9.sp else 10.5.sp, maxLines = 1, softWrap = false)
            }
        }
    }
}

data class PosCategoryStyle(
    val bg: Color,
    val border: Color,
    val tagBg: Color,
    val tagText: Color,
    val priceBg: Color,
    val priceText: Color
)

fun getCategoryStyle(category: String, customThemeId: String? = null): PosCategoryStyle {
    val theme = com.example.util.CategoryColorUtil.resolveCategoryStyle(category, customThemeId)
    return PosCategoryStyle(
        bg = theme.bgColor,
        border = theme.borderColor,
        tagBg = theme.tagBgColor,
        tagText = theme.tagTextColor,
        priceBg = theme.priceBgColor,
        priceText = theme.priceTextColor
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuItemGridTile(
    item: MenuItem,
    onAddStockClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isLandscapeCompact: Boolean = false,
    customThemeId: String? = null
) {
    val isZeroStock = item.currentStock <= 0
    val isLowStock = item.currentStock <= item.minStockAlert
    val catStyle = getCategoryStyle(item.category, customThemeId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isLandscapeCompact) 102.dp else 136.dp)
            .wrapContentHeight()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("menu_item_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isZeroStock) Color(0xFFFFF1F2) else catStyle.bg
        ),
        border = if (isZeroStock) BorderStroke(1.2.dp, StatusDanger.copy(alpha = 0.6f))
                 else BorderStroke(1.2.dp, catStyle.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(if (isLandscapeCompact) 8.dp else 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (isLandscapeCompact) 102.dp else 136.dp)
                .padding(horizontal = 4.dp, vertical = if (isLandscapeCompact) 4.dp else 5.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Category tag, ID badge & Prep indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = catStyle.tagBg,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = item.category,
                        fontSize = if (isLandscapeCompact) 7.5.sp else 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = catStyle.tagText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (item.id > 0L) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF64748B).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "#${item.id}",
                                fontSize = if (isLandscapeCompact) 7.sp else 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                            )
                        }
                    }
                    if (item.supportsPreparation) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = AmberCoral.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "طهي 🔥",
                                fontSize = if (isLandscapeCompact) 7.5.sp else 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberCoral,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                            )
                        }
                    }
                }
            }

            // Name (formatted on 2 lines cleanly without clipping bottom price)
            val priceDisplayStr = if (item.basePrice % 1.0 == 0.0) {
                String.format(Locale.US, "%.0f", item.basePrice)
            } else {
                String.format(Locale.US, "%.1f", item.basePrice)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isLandscapeCompact) 10.sp else 11.5.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF0F172A),
                    maxLines = 2,
                    minLines = if (item.kitchenName.isNotBlank() && item.kitchenName.trim() != item.name.trim()) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = if (isLandscapeCompact) 13.sp else 14.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1.dp)
                )
                if (item.kitchenName.isNotBlank() && item.kitchenName.trim() != item.name.trim()) {
                    Text(
                        text = item.kitchenName,
                        fontSize = if (isLandscapeCompact) 7.5.sp else 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AmberCoral,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp)
                    )
                }
            }

            if (isLandscapeCompact) {
                // In landscape compact: price is prioritized and never squeezed/hidden, while stock shrinks cleanly if tight
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isZeroStock) Color(0xFFE2E8F0) else catStyle.priceBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$priceDisplayStr ج/${item.unit}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isZeroStock) Color.Gray else catStyle.priceText,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.testTag("menu_item_price_${item.id}")
                        )
                        Text(
                            text = if (isZeroStock) "نفد" else com.example.util.formatStockFraction(item.currentStock, item.unit),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isZeroStock || isLowStock) StatusDanger else Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Portrait Available Stock (showing halves, quarters, and exact fraction)
                    if (isZeroStock) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StatusDanger.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, StatusDanger.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onAddStockClick)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "نفد (توريد +)",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusDanger,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isLowStock) StatusDanger.copy(alpha = 0.10f) else Color(0xFFF1F5F9).copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "المتاح: ${com.example.util.formatStockFraction(item.currentStock, item.unit)}",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLowStock) StatusDanger else Color(0xFF334155),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    // Price at the Bottom (guaranteed visible regardless of item name length or display scaling)
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = if (isZeroStock) Color(0xFFE2E8F0) else catStyle.priceBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$priceDisplayStr ج/${item.unit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isZeroStock) Color.Gray else catStyle.priceText,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .padding(vertical = 2.5.dp, horizontal = 2.dp)
                                .testTag("menu_item_price_${item.id}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MenuItemRow(
    item: MenuItem,
    onAddStockClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) = MenuItemGridTile(item, onAddStockClick, onClick, onLongClick)

@Composable
fun LandscapeOrderTicketPanel(
    viewModel: MainViewModel,
    cartItems: List<CartItem>,
    orderType: String,
    selectedCustomer: Customer?,
    deliveryFee: Double,
    discount: Double,
    paymentMethod: String,
    totalCartAmount: Double,
    onOpenFullCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtotal = cartItems.sumOf { it.total }
    val effectiveDelivery = if (orderType == "DELIVERY") deliveryFee else 0.0
    val isRoundToFiveEnabled by viewModel.isRoundToFiveEnabled.collectAsStateWithLifecycle()
    val editingOrderId by viewModel.editingOrderId.collectAsStateWithLifecycle()
    val orderPaidAmount by viewModel.orderPaidAmount.collectAsStateWithLifecycle()
    val orderIsPaid by viewModel.orderIsPaid.collectAsStateWithLifecycle()
    val isPrintingOrder by viewModel.isPrintingOrder.collectAsStateWithLifecycle()
    val isSubmittingOrderState by viewModel.isSubmittingOrderState.collectAsStateWithLifecycle()
    val deviceRole by viewModel.deviceRole.collectAsStateWithLifecycle()
    val rawBeforeRounding = (subtotal + effectiveDelivery - discount).coerceAtLeast(0.0)
    val roundingDiscount = if (isRoundToFiveEnabled && rawBeforeRounding > 0.0) {
        val rounded = kotlin.math.floor(rawBeforeRounding / 5.0) * 5.0
        (rawBeforeRounding - rounded).coerceAtLeast(0.0)
    } else 0.0
    val finalTotal = (rawBeforeRounding - roundingDiscount).coerceAtLeast(0.0)
    var quickQuantityCartItem by remember { mutableStateOf<CartItem?>(null) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Dark Header Strip
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "طلب حالي (${cartItems.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearCart() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "مسح",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Order Type Toggle Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val types = listOf(
                    Triple("TAKEAWAY", "سفري 🛍️", Color(0xFF0284C7)),
                    Triple("DINE_IN", "صالة 🍽️", Color(0xFF16A34A)),
                    Triple("DELIVERY", "توصيل 🛵", AmberCoral)
                )
                types.forEach { (typeKey, label, color) ->
                    val isChosen = orderType == typeKey
                    Surface(
                        onClick = { viewModel.orderType.value = typeKey },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isChosen) color else Color(0xFFE2E8F0),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChosen) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // Active Customer Info Pill (if any)
            if (selectedCustomer != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE0F2FE),
                    border = BorderStroke(0.8.dp, Color(0xFF7DD3FC)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF0369A1), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${selectedCustomer.name} (${selectedCustomer.primaryPhone})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Scrollable Cart Items List
            MouseLazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(cartItems, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.0f", item.total)} ج",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = SeaTealPrimary
                                )
                            }
                            if (item.preparation != null) {
                                val prepPriceStr = if (item.prepPrice > 0) {
                                    val pStr = if (item.prepPrice % 1.0 == 0.0) item.prepPrice.toInt().toString() else String.format(Locale.US, "%.1f", item.prepPrice)
                                    "(+$pStr)للكيلو"
                                } else ""
                                Text(
                                    text = "${item.preparation.name}$prepPriceStr",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmberCoral
                                )
                            }
                            if (item.notes.isNotBlank()) {
                                Text(
                                    text = "ملاحظة: ${item.notes}",
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                            if (editingOrderId != null) {
                                val printedQty = item.printedKitchenQuantity
                                val qty = item.quantity
                                val unit = item.unit
                                if (printedQty > 0.0001) {
                                    if (qty > printedQty) {
                                        Surface(
                                            color = Color(0xFFDCFCE7),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = "⚡ زيادة تشغيل: +${com.example.printer.ReceiptBitmapRenderer.formatQuantity(qty - printedQty)} $unit (طُبع: ${com.example.printer.ReceiptBitmapRenderer.formatQuantity(printedQty)})",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    } else if (qty == printedQty) {
                                        Text(
                                            text = "✓ تم طباعته بالكامل للمطبخ",
                                            fontSize = 8.5.sp,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    } else {
                                        Surface(
                                            color = Color(0xFFFEF3C7),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = "⚠️ تقليل كمية (طُبع سابقاً: ${com.example.printer.ReceiptBitmapRenderer.formatQuantity(printedQty)} $unit)",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                } else {
                                    val isNewlyAddedInEdit = (item.orderItemId == null || item.orderItemId <= 0L)
                                    if (isNewlyAddedInEdit) {
                                        Surface(
                                            color = Color(0xFFE0F2FE),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = "✨ صنف جديد أضيف للأوردر (+${com.example.printer.ReceiptBitmapRenderer.formatQuantity(qty)} $unit)",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0369A1),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.0f", item.unitPrice + item.prepPrice)} ج/${item.unit}",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF64748B)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val step = if (item.unit == "كيلو") 0.25 else 1.0
                                    Surface(
                                        onClick = { viewModel.updateCartItemQuantity(item.id, item.quantity - step) },
                                        shape = CircleShape,
                                        color = Color(0xFFFEE2E2),
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.Remove, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                    Surface(
                                        onClick = { quickQuantityCartItem = item },
                                        shape = RoundedCornerShape(5.dp),
                                        color = SeaTealPrimary.copy(alpha = 0.12f),
                                        border = BorderStroke(0.8.dp, SeaTealPrimary.copy(alpha = 0.4f)),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${com.example.printer.ReceiptBitmapRenderer.formatQuantity(item.quantity)} ${item.unit}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SeaNavyDeep
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "كتابة الكمية",
                                                tint = SeaTealPrimary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = { viewModel.updateCartItemQuantity(item.id, item.quantity + step) },
                                        shape = CircleShape,
                                        color = Color(0xFFDCFCE7),
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.removeCartItem(item.id) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "حذف", tint = Color.LightGray, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Order Summary & Action
            Surface(
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Quick Payment Method
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "طريقة الدفع:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                onClick = { viewModel.paymentMethod.value = "CASH" },
                                shape = RoundedCornerShape(6.dp),
                                color = if (paymentMethod == "CASH") Color(0xFF16A34A) else Color.White,
                                border = BorderStroke(0.8.dp, if (paymentMethod == "CASH") Color(0xFF16A34A) else Color(0xFFCBD5E1)),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                                    Text("💵 كاش", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = if (paymentMethod == "CASH") Color.White else Color(0xFF334155))
                                }
                            }
                            Surface(
                                onClick = { viewModel.paymentMethod.value = "CARD" },
                                shape = RoundedCornerShape(6.dp),
                                color = if (paymentMethod == "CARD") SeaTealPrimary else Color.White,
                                border = BorderStroke(0.8.dp, if (paymentMethod == "CARD") SeaTealPrimary else Color(0xFFCBD5E1)),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                                    Text("💳 فيزا", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = if (paymentMethod == "CARD") Color.White else Color(0xFF334155))
                                }
                            }
                        }
                    }

                    // Rounding Option Row (تقريب لأقرب مبلغ صحيح بحيث الآحاد 0 أو 5)
                    Surface(
                        onClick = { viewModel.isRoundToFiveEnabled.value = !isRoundToFiveEnabled },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isRoundToFiveEnabled) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isRoundToFiveEnabled) Color(0xFF10B981) else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("landscape_round_to_five_toggle")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isRoundToFiveEnabled,
                                    onCheckedChange = { viewModel.isRoundToFiveEnabled.value = it },
                                    modifier = Modifier.size(20.dp),
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF059669),
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تقريب لأقرب مبلغ صحيح (0 أو 5)",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRoundToFiveEnabled) Color(0xFF065F46) else Color(0xFF334155)
                                )
                            }
                            if (isRoundToFiveEnabled && roundingDiscount > 0) {
                                Text(
                                    text = "-${String.format(Locale.US, "%.1f", roundingDiscount)} ج",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }

                    // Quick Payment Row (سداد الحساب والمدفوع والباقي)
                    Surface(
                        onClick = {
                            val newState = !orderIsPaid
                            viewModel.orderIsPaid.value = newState
                            if (newState && orderPaidAmount < finalTotal) {
                                viewModel.orderPaidAmount.value = finalTotal
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = if (orderIsPaid || (orderPaidAmount >= finalTotal && finalTotal > 0)) Color(0xFFDCFCE7) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (orderIsPaid || (orderPaidAmount >= finalTotal && finalTotal > 0)) Color(0xFF16A34A) else Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = orderIsPaid || (orderPaidAmount >= finalTotal && finalTotal > 0),
                                    onCheckedChange = { isChecked ->
                                        viewModel.orderIsPaid.value = isChecked
                                        if (isChecked && orderPaidAmount < finalTotal) {
                                            viewModel.orderPaidAmount.value = finalTotal
                                        }
                                    },
                                    modifier = Modifier.size(20.dp),
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF16A34A))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (orderIsPaid || (orderPaidAmount >= finalTotal && finalTotal > 0)) "الأوردر خالص ✓" else "حساب الزبون غير مسدد",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (orderIsPaid || (orderPaidAmount >= finalTotal && finalTotal > 0)) Color(0xFF15803D) else Color(0xFF475569)
                                )
                            }
                            if (orderPaidAmount > finalTotal) {
                                Text(
                                    text = "باقي: ${String.format(Locale.US, "%.1f", orderPaidAmount - finalTotal)} ج",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            } else if (orderPaidAmount > 0 && !orderIsPaid) {
                                Text(
                                    text = "مدفوع: ${String.format(Locale.US, "%.0f", orderPaidAmount)} ج (متبقي: ${String.format(Locale.US, "%.1f", finalTotal - orderPaidAmount)})",
                                    fontSize = 8.5.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Total Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "الإجمالي الصافي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", finalTotal)} ج.م",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SeaTealPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isSeller = deviceRole == "SELLER"
                        OutlinedButton(
                            onClick = onOpenFullCheckout,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(if (isSeller) 0.85f else 1f)
                                .height(34.dp)
                        ) {
                            Text(if (isSeller) "خيارات ⚙️" else "خيارات كاملة ⚙️", fontSize = 10.sp, maxLines = 1)
                        }

                        if (isSeller) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.submitOrder(
                                        autoPrint = false,
                                        navigateToOrders = false,
                                        highlightOrder = true
                                    )
                                },
                                enabled = !isSubmittingOrderState,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(0.95f)
                                    .height(34.dp)
                                    .testTag("landscape_quick_save_only_button")
                            ) {
                                Text(
                                    text = if (editingOrderId != null) "حفظ فقط 📋" else "تأكيد وحفظ 📋",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.submitOrder(
                                    autoPrint = true,
                                    navigateToOrders = false,
                                    highlightOrder = true
                                )
                            },
                            enabled = !isPrintingOrder && !isSubmittingOrderState,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (editingOrderId != null) AmberCoral else if (isSeller) Color(0xFF2563EB) else SeaTealPrimary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(if (isSeller) 1.2f else 1.3f)
                                .height(34.dp)
                                .testTag("landscape_quick_submit_order_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (editingOrderId != null) {
                                    if (isSeller) "تعديل وطباعة 🖨️" else "تأكيد وطباعة التعديل 🖨️"
                                } else {
                                    if (isSeller) "تأكيد وطباعة 🖨️" else "تأكيد وطباعة ⚡"
                                },
                                fontSize = if (editingOrderId != null || isSeller) 9.5.sp else 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    quickQuantityCartItem?.let { cItem ->
        QuickCartQuantityDialog(
            item = cItem,
            onConfirm = { newQty ->
                viewModel.updateCartItemQuantity(cItem.id, newQty)
                quickQuantityCartItem = null
            },
            onDismiss = { quickQuantityCartItem = null },
            onRemove = {
                viewModel.removeCartItem(cItem.id)
                quickQuantityCartItem = null
            }
        )
    }
}

@Composable
fun ItemPreparationDialog(
    item: MenuItem,
    preparations: List<PreparationType>,
    allIngredients: List<MenuItemIngredient> = emptyList(),
    menuItems: List<MenuItem> = emptyList(),
    onAddToCart: (quantity: Double, preparation: PreparationType?, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    val relevantPreps = remember(preparations, item.category) {
        preparations.filter {
            it.isActive && (it.applicableCategory == "ALL" || it.applicableCategory.trim().equals(item.category.trim(), ignoreCase = true))
        }
    }
    var selectedQuantity by remember { mutableDoubleStateOf(1.0) }
    var customQuantityStr by remember { mutableStateOf("1.0") }
    var selectedPrep by remember(relevantPreps) { mutableStateOf<PreparationType?>(null) }
    var customNotes by remember { mutableStateOf("") }

    val isPlateItem = item.unit == "طبق"
    val hasPlateWeight = isPlateItem && (item.isPlateWeightEnabled || item.plateWeightKg > 0.0)
    val plateWeight = if (hasPlateWeight && item.plateWeightKg > 0.0) item.plateWeightKg else 0.5

    val quickWeights = if (isPlateItem) listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0) else listOf(0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0)
    val parsedQty = customQuantityStr.parseArabicDoubleOrNull() ?: selectedQuantity
    val basePrepPricePerKg = selectedPrep?.additionalPricePerUnit ?: 0.0
    val prepPricePerUnit = if (hasPlateWeight) {
        basePrepPricePerKg * plateWeight
    } else {
        basePrepPricePerKg
    }
    val totalEstimated = (item.basePrice + prepPricePerUnit) * parsedQty

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isPlateItem) "تجهيز أطباق: ${item.name}" else "تجهيز ووزن: ${item.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (isPlateItem) {
                    val weightDetails = if (hasPlateWeight) " (وزن الطبق: ${plateWeight} كجم" + (if (item.pricePerKg > 0) " • سعر الكيلو: ${item.pricePerKg} ج.م)" else ")") else ""
                    Text(
                        text = "سعر الطبق: ${item.basePrice} ج.م$weightDetails",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "سعر الكيلو الأساسي: ${item.basePrice} ج.م",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            MouseLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "1. حدد الوزن أو الكمية المطلوبة:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Direct input with Minus and Plus buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val step = if (item.unit == "كيلو") 0.25 else 1.0
                        Surface(
                            onClick = {
                                val cur = customQuantityStr.parseArabicDoubleOrNull() ?: selectedQuantity
                                val next = (cur - step).coerceAtLeast(step)
                                selectedQuantity = next
                                customQuantityStr = if (next % 1.0 == 0.0) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                            },
                            shape = CircleShape,
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "إنقاص", tint = StatusDanger)
                            }
                        }

                        OutlinedTextField(
                            value = customQuantityStr,
                            onValueChange = {
                                customQuantityStr = it
                                it.parseArabicDoubleOrNull()?.let { num ->
                                    if (num > 0) selectedQuantity = num
                                }
                            },
                            label = { Text("اكتب الكمية / الوزن (${item.unit}) *") },
                            placeholder = { Text("اكتب الكمية...") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 56.dp)
                        )

                        Surface(
                            onClick = {
                                val cur = customQuantityStr.parseArabicDoubleOrNull() ?: selectedQuantity
                                val next = cur + step
                                selectedQuantity = next
                                customQuantityStr = if (next % 1.0 == 0.0) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                            },
                            shape = CircleShape,
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "زيادة", tint = Color(0xFF15803D))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    MouseLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quickWeights) { w ->
                            val label = if (isPlateItem) {
                                "${w.toInt()} طبق"
                            } else when (w) {
                                0.25 -> "¼ كجم"
                                0.5 -> "½ كجم"
                                0.75 -> "¾ كجم"
                                else -> if (w % 1.0 == 0.0) "${w.toInt()} كجم" else "$w كجم"
                            }
                            val isSelected = parsedQty == w
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedQuantity = w
                                    customQuantityStr = if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()
                                },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SeaTealPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Real-time Stock and Over-Quantity Warning
                    val isOverStock = item.currentStock >= 0 && parsedQty > item.currentStock
                    val isZeroStock = item.currentStock <= 0

                    LaunchedEffect(selectedQuantity) {
                        if (isOverStock || isZeroStock) {
                            SoundAlertUtil.playStockWarningBeep()
                        }
                    }

                    if (isZeroStock || isOverStock) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusDanger.copy(alpha = 0.12f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusDanger),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusDanger,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isZeroStock) "⚠️ تنبيه المخزون في الوقت الفعلي: الصنف نفد تماماً (المتاح: 0 ${item.unit})"
                                        else "⚠️ تنبيه فوري وصوتي: الكمية المحددة ($selectedQuantity ${item.unit}) تتجاوز المتاح حالياً (${item.currentStock} ${item.unit})!",
                                        color = StatusDanger,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "المتاح بالمطعم: ${item.currentStock} ${item.unit}. سيتطلب إتمام الأوردر تأكيد المدير أو استلام توريد.",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Check linked ingredients
                    val linkedIngs = allIngredients.filter { it.menuItemId == item.id }
                    val shortIngs = linkedIngs.filter { ing ->
                        val raw = menuItems.firstOrNull { it.id == ing.ingredientItemId }
                        raw != null && (ing.requiredQuantityPerUnit * selectedQuantity) > raw.currentStock
                    }
                    if (shortIngs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AmberCoral.copy(alpha = 0.12f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberCoral),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AmberCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "⚠️ تنبيه المكونات المرتبطة: نقص في خامات/مكونات الصنف!",
                                        color = AmberCoral,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    shortIngs.forEach { ing ->
                                        val raw = menuItems.firstOrNull { it.id == ing.ingredientItemId }
                                        Text(
                                            text = "• المكون (${ing.ingredientName}): مطلوب ${String.format(Locale.US, "%.2f", ing.requiredQuantityPerUnit * selectedQuantity)} ${ing.unit}، المتاح بالمخزن: ${raw?.currentStock ?: 0.0} ${ing.unit}",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "2. طريقة التجهيز والطهي (تُضاف تلقائياً للسعر):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // First choice at the top: ني
                item {
                    val isRaw = selectedPrep == null
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isRaw) SeaTealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPrep = null }
                            .border(
                                width = if (isRaw) 2.dp else 1.dp,
                                color = if (isRaw) SeaTealPrimary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ني (طازج بدون طهي / تجهيز)",
                                fontWeight = if (isRaw) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Text(text = "+0.0 ج.م", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                items(relevantPreps) { prep ->
                    val isChosen = selectedPrep?.id == prep.id
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isChosen) SeaTealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPrep = prep }
                            .border(
                                width = if (isChosen) 2.dp else 1.dp,
                                color = if (isChosen) SeaTealPrimary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = prep.name,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            val prepFeeText = if (hasPlateWeight) {
                                val platePrep = prep.additionalPricePerUnit * plateWeight
                                "+${String.format(Locale.US, "%.1f", platePrep)} ج.م للطبق (${prep.additionalPricePerUnit} ج/كجم × $plateWeight كجم)"
                            } else {
                                "+${prep.additionalPricePerUnit} ج.م / كجم"
                            }
                            Text(
                                text = prepFeeText,
                                color = AmberCoral,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = { customNotes = it },
                        label = { Text("ملاحظات خاصة (مثلاً: بدون شطة / ليمون زيادة)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "الإجمالي المحسوب:", fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", totalEstimated)} ج.م",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalQty = customQuantityStr.parseArabicDoubleOrNull() ?: selectedQuantity
                    if (finalQty > 0) {
                        onAddToCart(finalQty, selectedPrep, customNotes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("إضافة للسلة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun QuickAddStockDialog(
    item: MenuItem,
    onConfirm: (addedQty: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var qtyStr by remember { mutableStateOf("10") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مخزون فوري: ${item.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "الرصيد الحالي بالمخزن: ${item.currentStock} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.currentStock <= 0) StatusDanger else SeaTealPrimary,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("الكمية المضافة للمخزن (${item.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "سيتم تحديث الرصيد وتسجيل حركة توريد مخزنية فوراً ليصبح الصنف متاحاً للطلب دون مغادرة شاشة الكاشير.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qtyStr.toDoubleOrNull() ?: 0.0
                    if (q > 0) {
                        onConfirm(q)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("إضافة للمخزون الآن")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun OpenShiftDialog(
    onConfirm: (cashierName: String, openingCash: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var cashierName by remember { mutableStateOf("كاشير رئيسي") }
    var openingCashStr by remember { mutableStateOf("500") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بدء وردية كاشير جديدة", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "يتم فتح الوردية لعزل حساب اليوم والدرج عن الورديات السابقة مع الحفاظ على كافة أوردرات العملاء.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = cashierName,
                    onValueChange = { cashierName = it },
                    label = { Text("اسم الكاشير المسئول") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = openingCashStr,
                    onValueChange = { openingCashStr = it },
                    label = { Text("رصيد بداية الدرج (الفكة / العهدة النقدية)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val opening = openingCashStr.toDoubleOrNull() ?: 0.0
                    onConfirm(cashierName, opening)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("فتح الوردية الآن")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun CloseShiftDialog(
    shift: Shift,
    viewModel: MainViewModel,
    onConfirm: (actualCash: Double, expenses: Double, notes: String, pass: String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var statement by remember { mutableStateOf<com.example.data.repository.ShiftDrawerStatement?>(null) }
    var isLoadingStatement by remember { mutableStateOf(true) }

    var password by remember { mutableStateOf("") }
    var actualCashStr by remember { mutableStateOf("") }
    var extraExpensesStr by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(shift.id) {
        scope.launch {
            isLoadingStatement = true
            statement = viewModel.getShiftDrawerStatement(shift.id)
            isLoadingStatement = false
        }
    }

    val actualCash = actualCashStr.toDoubleOrNull() ?: 0.0
    val extraExpenses = extraExpensesStr.toDoubleOrNull() ?: 0.0

    val st = statement
    val expectedCash = if (st != null) {
        st.totalIncomingCash - (st.totalStaffWithdrawals + st.totalSupplierPayments + extraExpenses)
    } else {
        shift.openingBalance + shift.totalCashSales - extraExpenses
    }
    val diff = actualCash - expectedCash

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = AmberCoral, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("تقفيل الوردية #${shift.shiftNumber} (حسابات الدرج)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("كاشير: ${shift.cashierName}", fontSize = 11.5.sp, color = SeaTealPrimary)
                }
            }
        },
        text = {
            MouseLazyColumn(modifier = Modifier.fillMaxWidth().height(420.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isLoadingStatement || st == null) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("جاري احتساب ومطابقة رصيد الدرج...", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("💵 عهدة البداية (رصيد افتتاحي):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${String.format(Locale.US, "%.1f", st.openingBalance)} ج.م", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                // Incoming
                                Text("🟢 الوارد للدرج (المقبوضات):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SeaTealPrimary)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("  • مبيعات كاش المحل (${st.storeCashOrders.size} أوردر):", fontSize = 10.5.sp, color = Color.DarkGray)
                                    Text("+${String.format(Locale.US, "%.1f", st.totalStoreCash)} ج.م", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 11.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("  • توريد تسويات الدليفري (${st.driverSettlements.size} تسوية):", fontSize = 10.5.sp, color = Color.DarkGray)
                                    Text("+${String.format(Locale.US, "%.1f", st.totalDriverSettlementsCash)} ج.م", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 11.sp)
                                }
                                if (st.totalAccountReceipts > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("  • مقبوضات عامة للدرج:", fontSize = 10.5.sp, color = Color.DarkGray)
                                        Text("+${String.format(Locale.US, "%.1f", st.totalAccountReceipts)} ج.م", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 11.sp)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                // Outgoing
                                Text("🔴 المنصرف من الدرج (المدفوعات):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = StatusDanger)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("  • سلف ومسحوبات الموظفين (${st.staffWithdrawals.size}):", fontSize = 10.5.sp, color = Color.DarkGray)
                                    Text("-${String.format(Locale.US, "%.1f", st.totalStaffWithdrawals)} ج.م", fontWeight = FontWeight.Bold, color = StatusDanger, fontSize = 11.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("  • دفعات وفواتير الموردين (${st.supplierPayments.size}):", fontSize = 10.5.sp, color = Color.DarkGray)
                                    Text("-${String.format(Locale.US, "%.1f", st.totalSupplierPayments)} ج.م", fontWeight = FontWeight.Bold, color = StatusDanger, fontSize = 11.sp)
                                }
                                if (extraExpenses > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("  • نثريات ومصروفات إضافية الآن:", fontSize = 10.5.sp, color = Color.DarkGray)
                                        Text("-${String.format(Locale.US, "%.1f", extraExpenses)} ج.م", fontWeight = FontWeight.Bold, color = StatusDanger, fontSize = 11.sp)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                // Non-drawer wallet
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("📱 مبيعات فودافون كاش (خارج الدرج):", fontSize = 10.5.sp, color = AmberCoral)
                                    Text("${String.format(Locale.US, "%.1f", st.totalWalletSales)} ج.م", fontSize = 10.5.sp, color = AmberCoral, fontWeight = FontWeight.SemiBold)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                // Expected in drawer
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("💰 المفترض وجوده بالدرج الآن:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${String.format(Locale.US, "%.1f", expectedCash)} ج.م", color = SeaTealPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("العد الفعلي للكاش:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        androidx.compose.material3.TextButton(
                            onClick = {
                                actualCashStr = String.format(Locale.US, "%.1f", expectedCash)
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("نسخ المفترض بالدرج (${String.format(Locale.US, "%.0f", expectedCash)} ج)", fontSize = 11.sp, color = SeaTealPrimary)
                        }
                    }

                    OutlinedTextField(
                        value = actualCashStr,
                        onValueChange = { actualCashStr = it },
                        label = { Text("المبلغ الفعلي المعدود بالدرج (ج.م) *") },
                        placeholder = { Text("اكتب المبلغ الموجود في الدرج بالظبط") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = extraExpensesStr,
                        onValueChange = { extraExpensesStr = it },
                        label = { Text("نثريات ومصروفات إضافية عند التقفيل (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة سر الإدارة لتأكيد التقفيل (الافتراضية: 1234) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات تقفيل الوردية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )

                    if (actualCashStr.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val diffColor = if (kotlin.math.abs(diff) < 0.01) StatusSuccess else if (diff < 0) StatusDanger else Color(0xFF1565C0)
                        val diffLabel = when {
                            kotlin.math.abs(diff) < 0.01 -> "الدرج متطابق تماماً مع الحسابات الدفترية ✓"
                            diff < 0 -> "عجز بالدرج: ${String.format(Locale.US, "%.1f", kotlin.math.abs(diff))} ج.م ⚠️"
                            else -> "زيادة بالدرج: +${String.format(Locale.US, "%.1f", diff)} ج.م +"
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = diffColor.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, diffColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "حالة الدرج: $diffLabel",
                                color = diffColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    errorMessage?.let { err ->
                        Text(text = err, color = StatusDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isBlank()) {
                        errorMessage = "يرجى إدخال كلمة سر الإدارة"
                        return@Button
                    }
                    if (actualCashStr.isBlank()) {
                        errorMessage = "يرجى إدخال المبلغ الفعلي المعدود بالدرج"
                        return@Button
                    }
                    onConfirm(actualCash, extraExpenses, notes, password)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberCoral)
            ) {
                Text("تأكيد تقفيل الوردية")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun CustomAdHocItemDialog(
    allPrinters: List<com.example.printer.PrinterConfig> = emptyList(),
    onAdd: (name: String, unit: String, qty: Double, price: Double, notes: String, targetPrinter: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("قطعة") }
    var qtyStr by remember { mutableStateOf("1") }
    var priceStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedPrinterTarget by remember { mutableStateOf("MAIN") }

    val printerOptions = remember(allPrinters) {
        buildList {
            add("ALL" to "الكل (جميع الطابعات) 🖨️")
            if (allPrinters.isNotEmpty()) {
                for (p in allPrinters) {
                    val roleText = when (p.role) {
                        "CUSTOMER_INVOICE" -> "فاتورة"
                        "KITCHEN_ORDER" -> "مطبخ"
                        else -> "شامل"
                    }
                    add(p.id to "${p.displayName} ($roleText)")
                }
            } else {
                add("MAIN" to "كاشير رئيسي")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة صنف استثنائي / خاص للأوردر", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .mouseVerticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "هذا الصنف خاص بهذا الأوردر فقط ولا يغير قائمة الطعام الأصلية.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصنف (مثلاً: عزومة سمك سبيشيال)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("الكمية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("سعر الوحدة (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "توجيه الطابعة:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(printerOptions) { (pKey, pLabel) ->
                        FilterChip(
                            selected = selectedPrinterTarget == pKey,
                            onClick = { selectedPrinterTarget = pKey },
                            label = { Text(pLabel, fontSize = 10.5.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qtyStr.toDoubleOrNull() ?: 1.0
                    val p = priceStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && p > 0) {
                        onAdd(name, unit, q, p, notes, selectedPrinterTarget)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("إضافة للأوردر")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun ExternalFishPrepDialog(
    preparations: List<PreparationType>,
    menuItems: List<MenuItem> = emptyList(),
    onAdd: (fishName: String, prepName: String, weight: Double, prepPricePerKg: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    val activePreps = remember(preparations) {
        preparations.filter { it.isActive && it.name.isNotBlank() }
    }
    val realFishItems = remember(menuItems) {
        menuItems.filter { !it.isArchived && it.supportsPreparation }
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
    val defaultPrep = activePreps.firstOrNull()
    var fishName by remember { mutableStateOf(realFishItems.firstOrNull() ?: "") }
    var weightStr by remember { mutableStateOf("1.0") }
    var prepName by remember { mutableStateOf(defaultPrep?.name ?: "") }
    var prepPricePerKgStr by remember { mutableStateOf(defaultPrep?.additionalPricePerUnit?.toString() ?: "0.0") }
    var notes by remember { mutableStateOf("") }

    val weight = weightStr.toDoubleOrNull() ?: 1.0
    val prepPrice = prepPricePerKgStr.toDoubleOrNull() ?: 0.0
    val calculatedTotal = weight * prepPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.SetMeal, contentDescription = null, tint = AmberCoral)
                Spacer(modifier = Modifier.width(6.dp))
                Text("طلب سمك تجهيز خارجي (تجهيز فقط)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            MouseLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "يستخدم هذا الخيار عندما يجلب الزبون السمك من الخارج ويطلب طهيه وتجهيزه في المحل فقط.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Text("نوع / اسم السمك الخارجي:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    if (realFishItems.isNotEmpty()) {
                        MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            items(realFishItems) { f ->
                                FilterChip(
                                    selected = fishName == f,
                                    onClick = { fishName = f },
                                    label = { Text(f, fontSize = 11.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    OutlinedTextField(
                        value = fishName,
                        onValueChange = { fishName = it },
                        label = { Text("اسم أو نوع السمك الخارجي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = { weightStr = it },
                            label = { Text("الوزن (كجم)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = prepPricePerKgStr,
                            onValueChange = { prepPricePerKgStr = it },
                            label = { Text("سعر التجهيز للكيلو (ج)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("طريقة التجهيز والطهي المطلوبة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    if (activePreps.isNotEmpty()) {
                        MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            items(activePreps) { p ->
                                FilterChip(
                                    selected = prepName == p.name,
                                    onClick = {
                                        prepName = p.name
                                        prepPricePerKgStr = p.additionalPricePerUnit.toString()
                                    },
                                    label = { Text("${p.name} (+${p.additionalPricePerUnit}ج)", fontSize = 11.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    OutlinedTextField(
                        value = prepName,
                        onValueChange = { prepName = it },
                        label = { Text("طريقة التجهيز المحددة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SeaTealPrimary.copy(alpha = 0.1f)),
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
                            Text("إجمالي مصنعية التجهيز:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", calculatedTotal)} ج.م",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = SeaTealPrimary
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات خاصة بالتجهيز والفرن (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fishName.isNotBlank() && weight > 0) {
                        onAdd(fishName.trim(), prepName.trim(), weight, prepPrice, notes.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberCoral)
            ) {
                Text("إضافة للأوردر 🛒", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun CustomerAutoSuggestSection(
    selectedCustomer: Customer?,
    matchingCustomers: List<Customer>,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedCustomer != null) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "عميل مسجل: ${selectedCustomer.name}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14532D)
                        )
                        Text(
                            text = "الهاتف: ${selectedCustomer.primaryPhone}" + if (!selectedCustomer.secondaryPhone.isNullOrBlank()) " • ${selectedCustomer.secondaryPhone}" else "",
                            fontSize = 10.sp,
                            color = Color(0xFF15803D)
                        )
                    }
                }
                TextButton(
                    onClick = onClearCustomer,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("إلغاء الربط ✕", fontSize = 10.sp, color = StatusDanger)
                }
            }
        }
    } else if (matchingCustomers.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF0F9FF),
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "💡 عُثر على (${matchingCustomers.size}) عملاء مسجلين سابقاً يطابقون ما كتبته:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1)
                    )
                }
                matchingCustomers.forEach { cand ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCustomer(cand) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = cand.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (cand.totalOrdersCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE0F2FE)
                                        ) {
                                            Text(
                                                text = "${cand.totalOrdersCount} طلب سابق",
                                                fontSize = 9.sp,
                                                color = Color(0xFF0284C7),
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "📞 ${cand.primaryPhone}" + if (!cand.secondaryPhone.isNullOrBlank()) " • ${cand.secondaryPhone}" else "",
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                            Button(
                                onClick = { onSelectCustomer(cand) },
                                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("تعبئة تلقائية ⚡", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerPhoneSuggestionsRow(
    matchingCustomers: List<Customer>,
    typedPhone: String,
    selectedCustomer: Customer?,
    onSelectCustomer: (Customer) -> Unit
) {
    if (selectedCustomer == null && typedPhone.trim().length >= 2 && matchingCustomers.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Text(
                text = "أرقام مسجلة مطابقة (اضغط للاختيار والتعبئة الفورية):",
                fontSize = 9.5.sp,
                color = Color(0xFF0369A1),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            MouseLazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(matchingCustomers, key = { it.id }) { cand ->
                    Surface(
                        onClick = { onSelectCustomer(cand) },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE0F2FE),
                        border = BorderStroke(1.dp, Color(0xFF7DD3FC))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color(0xFF0284C7))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${cand.primaryPhone} (${cand.name})",
                                fontSize = 10.sp,
                                color = Color(0xFF0369A1),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartCheckoutContent(
    viewModel: MainViewModel,
    cartItems: List<CartItem>,
    orderType: String,
    selectedCustomer: Customer?,
    customerAddresses: List<CustomerAddress>,
    selectedAddress: CustomerAddress?,
    drivers: List<DeliveryDriver>,
    selectedDriver: DeliveryDriver?,
    deliveryFee: Double,
    discount: Double,
    paymentMethod: String,
    orderNotes: String,
    isLandscape: Boolean = false,
    onDismiss: () -> Unit
) {
    val deliveryAreas by viewModel.deliveryAreas.collectAsStateWithLifecycle()
    val allOrders by viewModel.orders.collectAsStateWithLifecycle()
    val savePermanently by viewModel.saveCustomerPermanently.collectAsStateWithLifecycle()
    val allRegisteredCustomers by viewModel.customers.collectAsStateWithLifecycle()
    val deviceRole by viewModel.deviceRole.collectAsStateWithLifecycle()

    var custName by remember(selectedCustomer) { mutableStateOf(viewModel.orderCustomerName.value.ifBlank { selectedCustomer?.name ?: "" }) }
    var custPhone by remember(selectedCustomer) { mutableStateOf(viewModel.orderCustomerPhone.value.ifBlank { selectedCustomer?.primaryPhone ?: "" }) }
    var custSecPhone by remember(selectedCustomer) { mutableStateOf(viewModel.orderCustomerSecondaryPhone.value.ifBlank { selectedCustomer?.secondaryPhone ?: "" }) }
    var custArea by remember(selectedAddress) { mutableStateOf(viewModel.orderCustomerArea.value.ifBlank { selectedAddress?.area ?: "" }) }
    var custStreet by remember(selectedAddress) { mutableStateOf(viewModel.orderCustomerStreet.value.ifBlank { selectedAddress?.streetDetails ?: "" }) }
    var feeInputStr by remember(deliveryFee) { mutableStateOf(if (deliveryFee > 0) String.format(Locale.US, "%.0f", deliveryFee) else "20") }

    val matchingCustomers = remember(custPhone, custName, allRegisteredCustomers) {
        val pTrim = custPhone.trim()
        val nTrim = custName.trim()
        if (pTrim.length >= 2 || nTrim.length >= 2) {
            allRegisteredCustomers.filter { c ->
                val matchPhone = pTrim.length >= 2 && (c.primaryPhone.contains(pTrim) || (c.secondaryPhone?.contains(pTrim) == true))
                val matchName = nTrim.length >= 2 && c.name.contains(nTrim, ignoreCase = true)
                matchPhone || matchName
            }.take(5)
        } else emptyList()
    }

    var discountInput by remember { mutableStateOf(if (discount > 0) discount.toString() else "") }
    var notesInput by remember { mutableStateOf(orderNotes) }
    var showAddAddressInCart by remember { mutableStateOf(false) }
    var showCustomerHistoryInCart by remember { mutableStateOf(false) }
    val allPreparations by viewModel.preparations.collectAsStateWithLifecycle()
    var editingCartItem by remember { mutableStateOf<CartItem?>(null) }
    var quickQuantityCartItem by remember { mutableStateOf<CartItem?>(null) }
    var showAddCustomItemInCart by remember { mutableStateOf(false) }

    val customerPastOrders = remember(custPhone, selectedCustomer, allOrders) {
        allOrders.filter { o ->
            (selectedCustomer != null && o.order.customerId == selectedCustomer.id) ||
            (custPhone.isNotBlank() && o.order.customerPhone == custPhone)
        }
    }

    val subtotal = cartItems.sumOf { it.total }
    val effectiveDeliveryFee = if (orderType == "DELIVERY") deliveryFee else 0.0
    val discValue = discountInput.toDoubleOrNull() ?: 0.0
    val context = androidx.compose.ui.platform.LocalContext.current

    val applyCustomerData: (Customer) -> Unit = { cust ->
        viewModel.selectCustomer(cust)
        custName = cust.name
        custPhone = cust.primaryPhone
        custSecPhone = cust.secondaryPhone ?: ""
        viewModel.orderCustomerName.value = cust.name
        viewModel.orderCustomerPhone.value = cust.primaryPhone
        viewModel.orderCustomerSecondaryPhone.value = cust.secondaryPhone ?: ""
        android.widget.Toast.makeText(context, "تم تعبئة بيانات العميل (${cust.name}) تلقائياً ✓", android.widget.Toast.LENGTH_SHORT).show()
    }

    val clearCustomerData: () -> Unit = {
        viewModel.clearSelectedCustomer()
        custName = ""
        custPhone = ""
        custSecPhone = ""
        custArea = ""
        custStreet = ""
    }

    LaunchedEffect(selectedCustomer) {
        if (selectedCustomer != null) {
            custName = selectedCustomer.name
            custPhone = selectedCustomer.primaryPhone
            custSecPhone = selectedCustomer.secondaryPhone ?: ""
        }
    }

    LaunchedEffect(selectedAddress) {
        if (selectedAddress != null) {
            custArea = selectedAddress.area
            custStreet = selectedAddress.streetDetails
            if (selectedAddress.deliveryFee > 0) {
                feeInputStr = String.format(Locale.US, "%.0f", selectedAddress.deliveryFee)
            }
        }
    }

    val isRoundToFiveEnabled by viewModel.isRoundToFiveEnabled.collectAsStateWithLifecycle()
    val editingOrderId by viewModel.editingOrderId.collectAsStateWithLifecycle()
    val editingOrderNumber by viewModel.editingOrderNumber.collectAsStateWithLifecycle()
    val orderPaidAmount by viewModel.orderPaidAmount.collectAsStateWithLifecycle()
    val orderIsPaid by viewModel.orderIsPaid.collectAsStateWithLifecycle()
    val isPrintingOrder by viewModel.isPrintingOrder.collectAsStateWithLifecycle()
    val isSubmittingOrderState by viewModel.isSubmittingOrderState.collectAsStateWithLifecycle()

    var paidInputStr by remember(orderPaidAmount) {
        mutableStateOf(if (orderPaidAmount > 0.0) String.format(Locale.US, "%.1f", orderPaidAmount) else "")
    }
    val paidAmountVal = paidInputStr.toDoubleOrNull() ?: 0.0

    val rawTotalBeforeRounding = (subtotal + effectiveDeliveryFee - discValue).coerceAtLeast(0.0)
    val roundingDiscount = if (isRoundToFiveEnabled && rawTotalBeforeRounding > 0.0) {
        val rounded = kotlin.math.floor(rawTotalBeforeRounding / 5.0) * 5.0
        (rawTotalBeforeRounding - rounded).coerceAtLeast(0.0)
    } else 0.0
    val finalTotal = (rawTotalBeforeRounding - roundingDiscount).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Pinned Top Bar: Always stays visible and clickable even when scrolling down
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق القائمة والنزول لأسفل",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "تفاصيل الأوردر والدفع",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.5.sp
                    )
                }

                TextButton(
                    onClick = { viewModel.clearCart() },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusDanger)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = StatusDanger
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مسح السلة", color = StatusDanger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        MouseLazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

        // Order Type Selector
        item {
            Text(text = "نوع الطلب:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf(
                    Triple("TAKEAWAY", "استلام من المحل 🏪", Icons.Default.Storefront),
                    Triple("DELIVERY", "دليفري توصيل 🛵", Icons.Default.DeliveryDining)
                )
                types.forEach { (typeKey, label, icon) ->
                    val isSelected = orderType == typeKey
                    Button(
                        onClick = { viewModel.orderType.value = typeKey },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) SeaTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = label, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Takeaway customer details (for pickup & reservations)
        if (orderType == "TAKEAWAY") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "بيانات العميل والحجز (استلام من المحل) 🏪",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "سجل بيانات العميل للتواصل ومتابعة الحجز وميعاد جاهزية الطلب.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CustomerAutoSuggestSection(
                            selectedCustomer = selectedCustomer,
                            matchingCustomers = matchingCustomers,
                            onSelectCustomer = applyCustomerData,
                            onClearCustomer = clearCustomerData
                        )

                        val takeawayPhoneDigits = custPhone.phoneDigitCount()
                        val takeawayPhoneWarning = custPhone.getCustomerPhoneWarning()
                        val isTakeawayPhoneInvalid = custPhone.isCustomerPhoneInvalid()

                        val takeawaySecPhoneDigits = custSecPhone.phoneDigitCount()
                        val takeawaySecPhoneWarning = custSecPhone.getCustomerPhoneWarning()
                        val isTakeawaySecPhoneInvalid = custSecPhone.isCustomerPhoneInvalid()

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = custName,
                                onValueChange = {
                                    custName = it
                                    viewModel.orderCustomerName.value = it
                                },
                                label = { Text("اسم العميل / الحجز") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = custPhone,
                                onValueChange = {
                                    custPhone = it
                                    viewModel.orderCustomerPhone.value = it
                                },
                                label = { Text("رقم الهاتف الأساسي") },
                                isError = isTakeawayPhoneInvalid,
                                supportingText = {
                                    if (takeawayPhoneWarning != null) {
                                        Text(
                                            text = takeawayPhoneWarning,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (takeawayPhoneDigits == 11) {
                                        Text(
                                            text = "✓ رقم مكتمل (11 رقم)",
                                            color = Color(0xFF16A34A),
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (isTakeawayPhoneInvalid) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = takeawayPhoneWarning ?: "تحذير: رقم غير صحيح",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (takeawayPhoneDigits == 11) {
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
                                modifier = Modifier.weight(1f)
                            )
                        }

                        CustomerPhoneSuggestionsRow(
                            matchingCustomers = matchingCustomers,
                            typedPhone = custPhone,
                            selectedCustomer = selectedCustomer,
                            onSelectCustomer = applyCustomerData
                        )
                        OutlinedTextField(
                            value = custSecPhone,
                            onValueChange = {
                                custSecPhone = it
                                viewModel.orderCustomerSecondaryPhone.value = it
                            },
                            label = { Text("هاتف إضافي / بديل (اختياري)") },
                            placeholder = { Text("مثال: 011xxxxxxxx") },
                            isError = isTakeawaySecPhoneInvalid,
                            supportingText = {
                                if (takeawaySecPhoneWarning != null) {
                                    Text(
                                        text = takeawaySecPhoneWarning,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (takeawaySecPhoneDigits == 11) {
                                    Text(
                                        text = "✓ رقم مكتمل (11 رقم)",
                                        color = Color(0xFF16A34A),
                                        fontSize = 10.sp
                                    )
                                }
                            },
                            trailingIcon = {
                                if (isTakeawaySecPhoneInvalid) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = takeawaySecPhoneWarning ?: "تحذير: رقم غير صحيح",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else if (takeawaySecPhoneDigits == 11) {
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
                    }
                }
            }
        }

        // Delivery details if DELIVERY
        if (orderType == "DELIVERY") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "بيانات العميل والتوصيل 🛵",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (customerPastOrders.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { showCustomerHistoryInCart = true },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("تفضيلات (${customerPastOrders.size})", fontSize = 10.sp)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showAddAddressInCart = true },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("عنوان آخر 📍", fontSize = 10.sp)
                                }
                            }
                        }

                        CustomerAutoSuggestSection(
                            selectedCustomer = selectedCustomer,
                            matchingCustomers = matchingCustomers,
                            onSelectCustomer = applyCustomerData,
                            onClearCustomer = clearCustomerData
                        )

                        val deliveryPhoneDigits = custPhone.phoneDigitCount()
                        val deliveryPhoneWarning = custPhone.getCustomerPhoneWarning()
                        val isDeliveryPhoneInvalid = custPhone.isCustomerPhoneInvalid()

                        val deliverySecPhoneDigits = custSecPhone.phoneDigitCount()
                        val deliverySecPhoneWarning = custSecPhone.getCustomerPhoneWarning()
                        val isDeliverySecPhoneInvalid = custSecPhone.isCustomerPhoneInvalid()

                        // Name & Phone fields
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = custName,
                                onValueChange = {
                                    custName = it
                                    viewModel.orderCustomerName.value = it
                                },
                                label = { Text("اسم العميل") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = custPhone,
                                onValueChange = {
                                    custPhone = it
                                    viewModel.orderCustomerPhone.value = it
                                },
                                label = { Text("رقم الهاتف الأساسي") },
                                isError = isDeliveryPhoneInvalid,
                                supportingText = {
                                    if (deliveryPhoneWarning != null) {
                                        Text(
                                            text = deliveryPhoneWarning,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (deliveryPhoneDigits == 11) {
                                        Text(
                                            text = "✓ رقم مكتمل (11 رقم)",
                                            color = Color(0xFF16A34A),
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (isDeliveryPhoneInvalid) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = deliveryPhoneWarning ?: "تحذير: رقم غير صحيح",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (deliveryPhoneDigits == 11) {
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
                                modifier = Modifier.weight(1f)
                            )
                        }

                        CustomerPhoneSuggestionsRow(
                            matchingCustomers = matchingCustomers,
                            typedPhone = custPhone,
                            selectedCustomer = selectedCustomer,
                            onSelectCustomer = applyCustomerData
                        )
                        OutlinedTextField(
                            value = custSecPhone,
                            onValueChange = {
                                custSecPhone = it
                                viewModel.orderCustomerSecondaryPhone.value = it
                            },
                            label = { Text("هاتف إضافي / بديل (اختياري)") },
                            placeholder = { Text("مثال: 011xxxxxxxx") },
                            isError = isDeliverySecPhoneInvalid,
                            supportingText = {
                                if (deliverySecPhoneWarning != null) {
                                    Text(
                                        text = deliverySecPhoneWarning,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (deliverySecPhoneDigits == 11) {
                                    Text(
                                        text = "✓ رقم مكتمل (11 رقم)",
                                        color = Color(0xFF16A34A),
                                        fontSize = 10.sp
                                    )
                                }
                            },
                            trailingIcon = {
                                if (isDeliverySecPhoneInvalid) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = deliverySecPhoneWarning ?: "تحذير: رقم غير صحيح",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else if (deliverySecPhoneDigits == 11) {
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

                        // Delivery Areas selector chips
                        Text(text = "اختر منطقة التوصيل السريعة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(deliveryAreas) { da ->
                                val isAreaSelected = custArea == da.name
                                FilterChip(
                                    selected = isAreaSelected,
                                    onClick = {
                                        custArea = da.name
                                        viewModel.orderCustomerArea.value = da.name
                                        feeInputStr = String.format(Locale.US, "%.0f", da.deliveryFee)
                                        viewModel.deliveryFee.value = da.deliveryFee
                                    },
                                    label = { Text("${da.name} (${String.format(Locale.US, "%.0f", da.deliveryFee)}ج)", fontSize = 11.sp) }
                                )
                            }
                        }

                        // Area & Street & Fee
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = custArea,
                                onValueChange = {
                                    custArea = it
                                    viewModel.orderCustomerArea.value = it
                                },
                                label = { Text("المنطقة / الحي") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = feeInputStr,
                                onValueChange = {
                                    val digitsOnly = it.filter { ch -> ch.isDigit() }
                                    feeInputStr = digitsOnly
                                    digitsOnly.toDoubleOrNull()?.let { f -> viewModel.deliveryFee.value = f }
                                },
                                label = { Text("رسوم التوصيل (ج.م)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = custStreet,
                            onValueChange = {
                                custStreet = it
                                viewModel.orderCustomerStreet.value = it
                            },
                            label = { Text("العنوان بالتفصيل (الشارع / العمارة / الشقة)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Saved addresses if customer exists
                        if (customerAddresses.isNotEmpty()) {
                            Text(text = "عناوين العميل المسجلة مسبقاً:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            customerAddresses.forEach { addr ->
                                val isAddrChosen = selectedAddress?.id == addr.id
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isAddrChosen) SeaTealPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectedAddress.value = addr
                                            custArea = addr.area
                                            custStreet = addr.streetDetails
                                            viewModel.orderCustomerArea.value = addr.area
                                            viewModel.orderCustomerStreet.value = addr.streetDetails
                                            feeInputStr = String.format(Locale.US, "%.0f", addr.deliveryFee)
                                            viewModel.deliveryFee.value = addr.deliveryFee
                                        }
                                        .border(1.dp, if (isAddrChosen) SeaTealPrimary else Color.LightGray, RoundedCornerShape(6.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "${addr.title}: ${addr.area} - ${addr.streetDetails}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                        Text(text = "${String.format(Locale.US, "%.0f", addr.deliveryFee)} ج.م", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Select Driver
                        Text(text = "كابتن التوصيل (الموصل):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(drivers.filter { it.isActive }) { drv ->
                                val isDrvSelected = selectedDriver?.id == drv.id
                                FilterChip(
                                    selected = isDrvSelected,
                                    onClick = { viewModel.selectedDriver.value = if (isDrvSelected) null else drv },
                                    label = { Text(drv.name) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cart items list
        item {
            Text(text = "الأصناف المطلوبة:", fontWeight = FontWeight.Bold)
        }

        items(cartItems) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (item.preparation != null) {
                            val prepPriceStr = if (item.prepPrice > 0) {
                                val pStr = if (item.prepPrice % 1.0 == 0.0) item.prepPrice.toInt().toString() else String.format(Locale.US, "%.1f", item.prepPrice)
                                "(+$pStr)للكيلو"
                            } else ""
                            Text(
                                text = "${item.preparation.name}$prepPriceStr",
                                color = AmberCoral,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (item.notes.isNotBlank()) {
                            Text(text = "ملاحظات: ${item.notes}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(
                            text = "${com.example.printer.ReceiptBitmapRenderer.formatQuantity(item.quantity)} ${item.unit} × ${item.unitPrice + item.prepPrice} = ${String.format(Locale.US, "%.1f", item.total)} ج.م",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        if (editingOrderId != null) {
                            val printedQty = item.printedKitchenQuantity
                            val qty = item.quantity
                            val unit = item.unit
                            if (printedQty > 0.0001) {
                                if (qty > printedQty) {
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "⚡ زيادة تشغيل للمطبخ: +${com.example.printer.ReceiptBitmapRenderer.formatQuantity(qty - printedQty)} $unit (طُبع سابقاً: ${com.example.printer.ReceiptBitmapRenderer.formatQuantity(printedQty)})",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (qty == printedQty) {
                                    Text(
                                        text = "✓ تم طباعته بالكامل للمطبخ",
                                        fontSize = 9.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                } else {
                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "⚠️ تم تقليل الكمية (طُبع سابقاً: ${com.example.printer.ReceiptBitmapRenderer.formatQuantity(printedQty)} $unit)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            } else {
                                val isNewlyAddedInEdit = (item.orderItemId == null || item.orderItemId <= 0L)
                                if (isNewlyAddedInEdit) {
                                    Surface(
                                        color = Color(0xFFE0F2FE),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "✨ صنف جديد أضيف للأوردر (+${com.example.printer.ReceiptBitmapRenderer.formatQuantity(qty)} $unit)",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0369A1),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val step = if (item.unit == "كيلو") 0.25 else 1.0
                                viewModel.updateCartItemQuantity(item.id, (item.quantity - step).coerceAtLeast(0.0))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        Surface(
                            onClick = { quickQuantityCartItem = item },
                            shape = RoundedCornerShape(6.dp),
                            color = SeaTealPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${com.example.printer.ReceiptBitmapRenderer.formatQuantity(item.quantity)} ${item.unit}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = SeaNavyDeep
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "كتابة الكمية مباشرة",
                                    tint = SeaTealPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val step = if (item.unit == "كيلو") 0.25 else 1.0
                                viewModel.updateCartItemQuantity(item.id, item.quantity + step)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = { editingCartItem = item },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل الصنف وتجهيزه", tint = SeaNavyDeep, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = { viewModel.removeCartItem(item.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { showAddCustomItemInCart = true },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("➕ إضافة صنف إضافي / مخصص في السلة والدفع", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (editingOrderId != null) {
            item {
                val kitchenDeltas = cartItems.mapNotNull { itm ->
                    val delta = itm.quantity - itm.printedKitchenQuantity
                    if (delta > 0.0001) itm to delta else null
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmberCoral.copy(alpha = 0.08f)),
                    border = BorderStroke(1.2.dp, AmberCoral),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Restaurant, contentDescription = null, tint = AmberCoral, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🍳 ملخص فارق أمر التشغيل للمطبخ (الزيادات فقط):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SeaNavyDeep
                            )
                        }
                        if (kitchenDeltas.isNotEmpty()) {
                            Text(
                                text = "الأصناف التالية هي فقط التي ستُطبع على طابعة المطبخ كتذكرة تشغيل إضافية لمنع تكرار ما تم تجهيزه:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            kitchenDeltas.forEach { (cItm, delta) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• ${cItm.name}${if (cItm.preparation != null) " (${cItm.preparation.name})" else ""}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "+${com.example.printer.ReceiptBitmapRenderer.formatQuantity(delta)} ${cItm.unit}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "✓ لا توجد كميات أو أصناف جديدة مضافة لأمر التشغيل. لن تُطبع ورقة مطبخ مكررة عند الحفظ.",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            }
        }

        // Discount & Payment
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = {
                        discountInput = it
                        it.toDoubleOrNull()?.let { d -> viewModel.discount.value = d }
                    },
                    label = { Text("خصم (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = {
                        notesInput = it
                        viewModel.orderNotes.value = it
                    },
                    label = { Text("ملاحظات عامة للأوردر") },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = "طريقة الدفع:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val methods = listOf("CASH" to "نقداً 💵", "WALLET" to "محفظة / فودافون كاش 📱")
                methods.forEach { (mKey, mLabel) ->
                    FilterChip(
                        selected = paymentMethod == mKey,
                        onClick = { viewModel.paymentMethod.value = mKey },
                        label = { Text(mLabel, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Rounding Option Card (خيار تقريب لأقرب مبلغ صحيح بحيث الآحاد 0 أو 5)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isRoundToFiveEnabled) Color(0xFF064E3B) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.2.dp,
                    if (isRoundToFiveEnabled) Color(0xFF10B981) else Color(0xFF334155)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.isRoundToFiveEnabled.value = !isRoundToFiveEnabled }
                    .testTag("round_to_five_toggle")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = isRoundToFiveEnabled,
                            onCheckedChange = { viewModel.isRoundToFiveEnabled.value = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF10B981),
                                uncheckedColor = Color(0xFF94A3B8),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "تقريب لأقرب مبلغ صحيح",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isRoundToFiveEnabled) Color(0xFFA7F3D0) else Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (isRoundToFiveEnabled) Color(0xFF047857) else Color(0xFF334155),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "الآحاد 0 أو 5",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRoundToFiveEnabled) Color(0xFFD1FAE5) else Color(0xFFCBD5E1),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isRoundToFiveEnabled && roundingDiscount > 0)
                                    "يتم خصم الفكة (-${String.format(Locale.US, "%.1f", roundingDiscount)} ج.م) لتصبح الآحاد إما 0 أو 5"
                                else
                                    "خصم الفكة من الأوردر بحيث خانة الآحاد في الحساب دائماً 0 أو 5",
                                fontSize = 11.sp,
                                color = if (isRoundToFiveEnabled) Color(0xFF6EE7B7) else Color(0xFF94A3B8)
                            )
                        }
                    }

                    if (isRoundToFiveEnabled) {
                        Surface(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (roundingDiscount > 0) "-${String.format(Locale.US, "%.1f", roundingDiscount)} ج" else "مضبوط ✓",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Customer Payment Section (حساب وسداد الزبون - دفع كام وباقي كام أو خالص)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, if (orderIsPaid || (paidAmountVal >= finalTotal && finalTotal > 0)) StatusSuccess else SeaTealPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pos_customer_payment_section")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "💵 حساب وسداد الزبون",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SeaNavyDeep
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (orderIsPaid || (paidAmountVal >= finalTotal && finalTotal > 0)) StatusSuccess else StatusWarning
                            ) {
                                Text(
                                    text = if (orderIsPaid || (paidAmountVal >= finalTotal && finalTotal > 0)) "✓ خالص بالكامل" else "معلق / غير خالص",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // One-tap checkbox "الأوردر خالص"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val newState = !orderIsPaid
                                viewModel.orderIsPaid.value = newState
                                if (newState && paidAmountVal < finalTotal) {
                                    viewModel.orderPaidAmount.value = finalTotal
                                    paidInputStr = String.format(Locale.US, "%.1f", finalTotal)
                                }
                            }
                        ) {
                            Checkbox(
                                checked = orderIsPaid || (paidAmountVal >= finalTotal && finalTotal > 0),
                                onCheckedChange = { isChecked ->
                                    viewModel.orderIsPaid.value = isChecked
                                    if (isChecked && paidAmountVal < finalTotal) {
                                        viewModel.orderPaidAmount.value = finalTotal
                                        paidInputStr = String.format(Locale.US, "%.1f", finalTotal)
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = StatusSuccess)
                            )
                            Text(
                                text = "الأوردر خالص ✓",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = if (orderIsPaid || (paidAmountVal >= finalTotal && finalTotal > 0)) StatusSuccess else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Quick ONE-TAP Full Payment Button
                    Button(
                        onClick = {
                            viewModel.orderPaidAmount.value = finalTotal
                            viewModel.orderIsPaid.value = true
                            paidInputStr = String.format(Locale.US, "%.1f", finalTotal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "سداد المبلغ المطلوب بالكامل (${String.format(Locale.US, "%.1f", finalTotal)} ج.م)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Paid Amount Input
                    OutlinedTextField(
                        value = paidInputStr,
                        onValueChange = { input ->
                            paidInputStr = input
                            val p = input.toDoubleOrNull() ?: 0.0
                            viewModel.orderPaidAmount.value = p
                            if (p >= finalTotal && finalTotal > 0) {
                                viewModel.orderIsPaid.value = true
                            }
                        },
                        label = { Text("المبلغ المدفوع نقدياً من العميل (ج.م)") },
                        placeholder = { Text("مثال: 500 أو 200 أو 1000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Cash Suggestion Chips
                    Text("اقتراحات نقدية سريعة:", fontSize = 11.sp, color = Color.Gray)
                    MouseLazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val suggestions = listOf(
                            finalTotal,
                            50.0, 100.0, 150.0, 200.0, 300.0, 400.0, 500.0, 1000.0
                        ).distinct().sorted()

                        items(suggestions) { amt ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (paidAmountVal == amt) SeaTealPrimary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (paidAmountVal == amt) SeaTealPrimary else Color.LightGray),
                                modifier = Modifier.clickable {
                                    paidInputStr = String.format(Locale.US, "%.1f", amt)
                                    viewModel.orderPaidAmount.value = amt
                                    if (amt >= finalTotal && finalTotal > 0) {
                                        viewModel.orderIsPaid.value = true
                                    }
                                }
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.0f", amt)} ج",
                                    color = if (paidAmountVal == amt) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Calculation Result Card (Change vs Remaining)
                    if (paidAmountVal > finalTotal) {
                        val change = paidAmountVal - finalTotal
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, Color(0xFF16A34A)),
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
                                    Text("الباقي المستحق للعميل:", fontSize = 12.sp, color = Color(0xFF166534))
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", change)} ج.م",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                                Text("يرجى إرجاع الباقي للزبون 💵", fontSize = 11.5.sp, color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (paidAmountVal < finalTotal && !orderIsPaid && paidAmountVal > 0) {
                        val rem = finalTotal - paidAmountVal
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
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
                                    Text("المتبقي على العميل:", fontSize = 12.sp, color = Color(0xFF92400E))
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", rem)} ج.م",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Text("مبلغ معلق على الحساب ⏳", fontSize = 11.5.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Summary & Submit (Harmonious Slate Navy & Baby Blue Palette - Resolves color clashing)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "المجموع الفرعي للأصناف:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text(text = "${String.format(Locale.US, "%.1f", subtotal)} ج.م", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    if (orderType == "DELIVERY") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "رسوم التوصيل:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text(text = "$effectiveDeliveryFee ج.م", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    if (discValue > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "الخصم المطبق:", color = Color(0xFFFDE68A), fontSize = 13.sp)
                            Text(text = "-$discValue ج.م", color = Color(0xFFFDE68A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    if (isRoundToFiveEnabled && roundingDiscount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "خصم تقريب الفكة (0 أو 5):", color = Color(0xFF86EFAC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "-${String.format(Locale.US, "%.1f", roundingDiscount)} ج.م", color = Color(0xFF86EFAC), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الإجمالي النهائي المطلوب:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (isRoundToFiveEnabled) {
                                Text(
                                    text = "خانة الآحاد: ${finalTotal.toLong() % 10} ✓",
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Text(
                            text = "${String.format(Locale.US, "%.1f", finalTotal)} ج.م",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Primary Action "تأكيد وطباعة" (Baby Blue Primary)
                    val isSeller = deviceRole == "SELLER"



                    Button(
                        onClick = {
                            custPhone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                                android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_LONG).show()
                            }
                            custSecPhone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                                android.widget.Toast.makeText(context, toastMsg.replace("رقم هاتف العميل", "الرقم الإضافي"), android.widget.Toast.LENGTH_LONG).show()
                            }
                            val p = paidInputStr.toDoubleOrNull() ?: 0.0
                            viewModel.orderPaidAmount.value = p
                            viewModel.orderIsPaid.value = orderIsPaid || (p >= finalTotal && finalTotal > 0.0)
                            viewModel.orderCustomerName.value = custName
                            viewModel.orderCustomerPhone.value = custPhone
                            viewModel.orderCustomerSecondaryPhone.value = custSecPhone
                            val isEditing = editingOrderId != null
                            viewModel.submitOrder(
                                autoPrint = true,
                                navigateToOrders = if (isEditing || isLandscape) false else true,
                                highlightOrder = true
                            )
                            onDismiss()
                        },
                        enabled = !isPrintingOrder && !isSubmittingOrderState,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (editingOrderId != null) AmberCoral else if (isSeller) Color(0xFF2563EB) else BabyBluePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("submit_and_print_order_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (editingOrderId != null) {
                                if (isSeller) "تأكيد وطباعة التعديل (من الكاشير) 🖨️" else "تأكيد وطباعة نسخة معدلة 🖨️"
                            } else {
                                if (isSeller) "تأكيد وطباعة (من الكاشير) 🖨️" else "تأكيد وطباعة 🖨️"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Second Option: "تأكيد وحفظ الأوردر بدون طباعة"
                    val shouldNavigateToOrders = !isLandscape && editingOrderId == null
                    val confirmBtnLabel = if (editingOrderId != null) {
                        "حفظ التعديل بدون طباعة 📋"
                    } else if (isSeller) {
                        "تأكيد وحفظ فقط (بدون طباعة) 📋"
                    } else if (isLandscape) {
                        "تأكيد وتحديد في الأوردرات 📋"
                    } else {
                        "تأكيد وانتقال للاوردر 📋"
                    }
                    Button(
                        onClick = {
                            custPhone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                                android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_LONG).show()
                            }
                            custSecPhone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                                android.widget.Toast.makeText(context, toastMsg.replace("رقم هاتف العميل", "الرقم الإضافي"), android.widget.Toast.LENGTH_LONG).show()
                            }
                            val p = paidInputStr.toDoubleOrNull() ?: 0.0
                            viewModel.orderPaidAmount.value = p
                            viewModel.orderIsPaid.value = orderIsPaid || (p >= finalTotal && finalTotal > 0.0)
                            viewModel.orderCustomerName.value = custName
                            viewModel.orderCustomerPhone.value = custPhone
                            viewModel.orderCustomerSecondaryPhone.value = custSecPhone
                            viewModel.submitOrder(
                                autoPrint = false,
                                navigateToOrders = shouldNavigateToOrders,
                                highlightOrder = true
                            )
                            onDismiss()
                        },
                        enabled = !isSubmittingOrderState,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("submit_and_navigate_order_button")
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = confirmBtnLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Optional 3rd helper: Confirm and share via WhatsApp
                    OutlinedButton(
                        onClick = {
                            custPhone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                                android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_LONG).show()
                            }
                            custSecPhone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                                android.widget.Toast.makeText(context, toastMsg.replace("رقم هاتف العميل", "الرقم الإضافي"), android.widget.Toast.LENGTH_LONG).show()
                            }
                            val p = paidInputStr.toDoubleOrNull() ?: 0.0
                            viewModel.orderPaidAmount.value = p
                            viewModel.orderIsPaid.value = orderIsPaid || (p >= finalTotal && finalTotal > 0.0)
                            viewModel.orderCustomerName.value = custName
                            viewModel.orderCustomerPhone.value = custPhone
                            viewModel.orderCustomerSecondaryPhone.value = custSecPhone
                            viewModel.submitOrder(
                                autoPrint = false,
                                navigateToOrders = shouldNavigateToOrders,
                                highlightOrder = true
                            )
                            onDismiss()
                        },
                        enabled = !isSubmittingOrderState,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFCBD5E1)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("submit_and_share_order_button")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (custPhone.isNotBlank()) "تأكيد ومشاركة الفاتورة واتساب 📲" else "تأكيد ومشاركة الفاتورة 📲",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

    // Customer Past Orders Dialog in Cart
    if (showCustomerHistoryInCart) {
        AlertDialog(
            onDismissRequest = { showCustomerHistoryInCart = false },
            title = {
                Text(
                    text = "تفضيلات وطلبات العميل السابقة: $custName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "عدد الأوردرات السابقة: ${customerPastOrders.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MouseLazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(customerPastOrders) { past ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "أوردر ${past.order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(text = "${past.order.totalAmount} ج.م", fontWeight = FontWeight.Bold, color = SeaTealPrimary, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    past.items.forEach { itm ->
                                        Text(
                                            text = "• ${itm.quantity} ${itm.unit} ${itm.itemName}" +
                                                    if (!itm.preparationName.isNullOrBlank()) " (${itm.preparationName})" else "",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCustomerHistoryInCart = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Add Another Address Dialog in Cart
    if (showAddAddressInCart) {
        var addrTitle by remember { mutableStateOf("عنوان إضافي") }
        var addrArea by remember { mutableStateOf(custArea) }
        var addrStreet by remember { mutableStateOf("") }
        var addrFeeStr by remember { mutableStateOf(feeInputStr) }

        AlertDialog(
            onDismissRequest = { showAddAddressInCart = false },
            title = { Text("إضافة عنوان آخر للعميل", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = addrTitle,
                        onValueChange = { addrTitle = it },
                        label = { Text("تسمية العنوان (البيت / العمل)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addrArea,
                        onValueChange = { addrArea = it },
                        label = { Text("المنطقة / الحي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addrStreet,
                        onValueChange = { addrStreet = it },
                        label = { Text("تفاصيل الشارع والعمارة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addrFeeStr,
                        onValueChange = { addrFeeStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("رسوم التوصيل لهذا العنوان (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fee = addrFeeStr.toDoubleOrNull() ?: 20.0
                        if (addrStreet.isNotBlank()) {
                            viewModel.saveCustomerAddress(
                                CustomerAddress(
                                    customerId = selectedCustomer?.id ?: 0L,
                                    title = addrTitle.trim(),
                                    area = addrArea.trim(),
                                    streetDetails = addrStreet.trim(),
                                    deliveryFee = fee
                                )
                            )
                            custArea = addrArea
                            custStreet = "${addrArea.trim()} - ${addrStreet.trim()}"
                            viewModel.orderCustomerArea.value = custArea
                            viewModel.orderCustomerStreet.value = custStreet
                            feeInputStr = String.format(Locale.US, "%.0f", fee)
                            viewModel.deliveryFee.value = fee
                            showAddAddressInCart = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("حفظ العنوان")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAddressInCart = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Edit Cart Item Dialog
    editingCartItem?.let { itemToEdit ->
        var editName by remember { mutableStateOf(itemToEdit.name) }
        var editQtyStr by remember { mutableStateOf(itemToEdit.quantity.toString()) }
        var editUnit by remember { mutableStateOf(itemToEdit.unit) }
        var editPriceStr by remember { mutableStateOf(itemToEdit.unitPrice.toString()) }
        var selectedPrep by remember {
            mutableStateOf(allPreparations.find { it.name == itemToEdit.preparation?.name })
        }
        var editPrepPriceStr by remember {
            mutableStateOf(itemToEdit.prepPrice.toString())
        }
        var editNotes by remember { mutableStateOf(itemToEdit.notes) }
        var editPrinterTarget by remember { mutableStateOf(itemToEdit.targetPrinter) }

        val q = editQtyStr.toDoubleOrNull() ?: itemToEdit.quantity
        val p = editPriceStr.toDoubleOrNull() ?: itemToEdit.unitPrice
        val pp = editPrepPriceStr.toDoubleOrNull() ?: (selectedPrep?.additionalPricePerUnit ?: 0.0)
        val liveItemTotal = (p + pp) * q

        AlertDialog(
            onDismissRequest = { editingCartItem = null },
            title = {
                Text("✏️ تعديل صنف: ${itemToEdit.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                MouseLazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("اسم الصنف") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = editQtyStr,
                                onValueChange = { editQtyStr = it },
                                label = { Text("الكمية / الوزن ($editUnit)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                            OutlinedTextField(
                                value = editPriceStr,
                                onValueChange = { editPriceStr = it },
                                label = { Text("سعر الوحدة") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Text("طريقة التجهيز:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedPrep == null && itemToEdit.preparation == null,
                                    onClick = {
                                        selectedPrep = null
                                        editPrepPriceStr = "0.0"
                                    },
                                    label = { Text("بدون تجهيز", fontSize = 10.sp) }
                                )
                            }
                            items(allPreparations) { prep ->
                                val isSel = selectedPrep?.id == prep.id || (selectedPrep == null && itemToEdit.preparation?.name == prep.name)
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        selectedPrep = prep
                                        editPrepPriceStr = prep.additionalPricePerUnit.toString()
                                    },
                                    label = { Text("${prep.name} (+${prep.additionalPricePerUnit} ج)", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = editPrepPriceStr,
                            onValueChange = { editPrepPriceStr = it },
                            label = { Text("سعر التجهيز المضاف للوحدة") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text("ملاحظات الصنف") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("توجيه الطابعة:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        val allPrintersInEdit by viewModel.allPrinters.collectAsStateWithLifecycle()
                        val printerOptions = remember(allPrintersInEdit) {
                            buildList {
                                add("ALL" to "الكل (جميع الطابعات) 🖨️")
                                if (allPrintersInEdit.isNotEmpty()) {
                                    for (p in allPrintersInEdit) {
                                        val roleText = when (p.role) {
                                            "CUSTOMER_INVOICE" -> "فاتورة"
                                            "KITCHEN_ORDER" -> "مطبخ"
                                            else -> "شامل"
                                        }
                                        add(p.id to "${p.displayName} ($roleText)")
                                    }
                                } else {
                                    add("MAIN" to "كاشير رئيسي")
                                }
                            }
                        }
                        MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(printerOptions) { (pKey, pLabel) ->
                                FilterChip(
                                    selected = editPrinterTarget == pKey,
                                    onClick = { editPrinterTarget = pKey },
                                    label = { Text(pLabel, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                    item {
                        Surface(
                            color = SeaTealPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الإجمالي:", fontSize = 12.sp)
                                Text("${String.format(Locale.US, "%.1f", liveItemTotal)} ج.م", fontWeight = FontWeight.Bold, color = SeaTealPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalQ = editQtyStr.toDoubleOrNull() ?: itemToEdit.quantity
                        val finalP = editPriceStr.toDoubleOrNull() ?: itemToEdit.unitPrice
                        val finalPP = editPrepPriceStr.toDoubleOrNull() ?: (selectedPrep?.additionalPricePerUnit ?: 0.0)
                        val effectivePrep = if (selectedPrep != null || finalPP > 0) {
                            val baseName = selectedPrep?.name ?: itemToEdit.preparation?.name ?: "تجهيز خاص"
                            PreparationType(
                                id = selectedPrep?.id ?: 0,
                                name = baseName,
                                additionalPricePerUnit = finalPP
                            )
                        } else null

                        viewModel.updateCartItemFull(
                            cartItemId = itemToEdit.id,
                            name = editName.trim(),
                            unitPrice = finalP,
                            quantity = finalQ,
                            unit = editUnit,
                            preparation = effectivePrep,
                            notes = editNotes.trim(),
                            targetPrinter = editPrinterTarget
                        )
                        editingCartItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingCartItem = null }) { Text("إلغاء") }
            }
        )
    }

    // Add Custom Ad-Hoc Item in Cart Dialog
    if (showAddCustomItemInCart) {
        var adName by remember { mutableStateOf("") }
        var adQtyStr by remember { mutableStateOf("1.0") }
        var adPriceStr by remember { mutableStateOf("") }
        var adUnit by remember { mutableStateOf("قطعة") }
        var adNotes by remember { mutableStateOf("") }
        var adPrinterTarget by remember { mutableStateOf("MAIN") }

        val allPrintersInCart by viewModel.allPrinters.collectAsStateWithLifecycle()
        val printerOptions = remember(allPrintersInCart) {
            buildList {
                add("ALL" to "الكل (جميع الطابعات) 🖨️")
                if (allPrintersInCart.isNotEmpty()) {
                    for (p in allPrintersInCart) {
                        val roleText = when (p.role) {
                            "CUSTOMER_INVOICE" -> "فاتورة"
                            "KITCHEN_ORDER" -> "مطبخ"
                            else -> "شامل"
                        }
                        add(p.id to "${p.displayName} ($roleText)")
                    }
                } else {
                    add("MAIN" to "كاشير رئيسي")
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddCustomItemInCart = false },
            title = { Text("إضافة صنف إضافي للسلة", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .mouseVerticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = adName,
                        onValueChange = { adName = it },
                        label = { Text("اسم الصنف أو الطلب الإضافي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = adQtyStr,
                            onValueChange = { adQtyStr = it },
                            label = { Text("الكمية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = adPriceStr,
                            onValueChange = { adPriceStr = it },
                            label = { Text("سعر الوحدة") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = adNotes,
                        onValueChange = { adNotes = it },
                        label = { Text("ملاحظات") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "توجيه الطابعة:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(printerOptions) { (pKey, pLabel) ->
                            FilterChip(
                                selected = adPrinterTarget == pKey,
                                onClick = { adPrinterTarget = pKey },
                                label = { Text(pLabel, fontSize = 10.5.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = adQtyStr.toDoubleOrNull() ?: 1.0
                        val p = adPriceStr.toDoubleOrNull() ?: 0.0
                        if (adName.isNotBlank() && p > 0) {
                            viewModel.addItemToCartDirect(
                                menuItem = null,
                                name = adName.trim(),
                                category = "إضافات",
                                unit = adUnit,
                                quantity = q,
                                unitPrice = p,
                                preparation = null,
                                notes = adNotes.trim(),
                                isCustomAdHoc = true,
                                targetPrinter = adPrinterTarget
                            )
                            showAddCustomItemInCart = false
                        }
                    },
                    enabled = adName.isNotBlank() && (adPriceStr.toDoubleOrNull() ?: 0.0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddCustomItemInCart = false }) { Text("إلغاء") }
            }
        )
    }

    quickQuantityCartItem?.let { cItem ->
        QuickCartQuantityDialog(
            item = cItem,
            onConfirm = { newQty ->
                viewModel.updateCartItemQuantity(cItem.id, newQty)
                quickQuantityCartItem = null
            },
            onDismiss = { quickQuantityCartItem = null },
            onRemove = {
                viewModel.removeCartItem(cItem.id)
                quickQuantityCartItem = null
            }
        )
    }
}

@Composable
fun QuickCartQuantityDialog(
    item: CartItem,
    onConfirm: (newQuantity: Double) -> Unit,
    onDismiss: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    var qtyText by remember {
        mutableStateOf(
            if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString()
            else item.quantity.toString()
        )
    }

    val parsedQty = qtyText.parseArabicDoubleOrNull() ?: item.quantity
    val step = if (item.unit == "كيلو") 0.25 else 1.0
    val totalPreview = (item.unitPrice + item.prepPrice) * parsedQty

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "تعديل كمية الصنف",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = item.name,
                    fontSize = 13.sp,
                    color = SeaTealPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.preparation != null) {
                    val prepPriceStr = if (item.prepPrice > 0) {
                        val pStr = if (item.prepPrice % 1.0 == 0.0) item.prepPrice.toInt().toString() else String.format(Locale.US, "%.1f", item.prepPrice)
                        "(+$pStr)للكيلو"
                    } else ""
                    Text(
                        text = "${item.preparation.name}$prepPriceStr",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AmberCoral
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اكتب الكمية أو الوزن المطلوب مباشرة:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Input row with Minus and Plus
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            val cur = qtyText.parseArabicDoubleOrNull() ?: item.quantity
                            val next = (cur - step).coerceAtLeast(step)
                            qtyText = if (next % 1.0 == 0.0) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                        },
                        shape = CircleShape,
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "إنقاص", tint = StatusDanger)
                        }
                    }

                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        placeholder = { Text("اكتب الكمية...") },
                        label = { Text("الكمية (${item.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 56.dp)
                    )

                    Surface(
                        onClick = {
                            val cur = qtyText.parseArabicDoubleOrNull() ?: item.quantity
                            val next = cur + step
                            qtyText = if (next % 1.0 == 0.0) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                        },
                        shape = CircleShape,
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "زيادة", tint = Color(0xFF15803D))
                        }
                    }
                }

                // Quick presets
                Text(text = "اختصارات سريعة:", fontSize = 11.sp, color = Color.Gray)
                MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presets = if (item.unit == "كيلو") {
                        listOf(0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0)
                    } else {
                        listOf(1.0, 2.0, 3.0, 4.0, 5.0, 10.0, 12.0, 20.0)
                    }
                    items(presets) { pVal ->
                        val pLabel = if (item.unit == "كيلو") {
                            when (pVal) {
                                0.25 -> "¼"
                                0.5 -> "½"
                                0.75 -> "¾"
                                else -> if (pVal % 1.0 == 0.0) "${pVal.toInt()}" else "$pVal"
                            }
                        } else {
                            "${pVal.toInt()}"
                        }
                        FilterChip(
                            selected = parsedQty == pVal,
                            onClick = {
                                qtyText = if (pVal % 1.0 == 0.0) pVal.toInt().toString() else pVal.toString()
                            },
                            label = { Text(pLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }
                }

                // Live total preview card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "الإجمالي الجديد:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", totalPreview)} ج.م",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalQty = qtyText.parseArabicDoubleOrNull() ?: item.quantity
                    if (finalQty > 0) {
                        onConfirm(finalQty)
                    } else if (onRemove != null) {
                        onRemove()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("تأكيد وحفظ")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onRemove != null) {
                    TextButton(onClick = onRemove) {
                        Text("حذف الصنف", color = StatusDanger, fontSize = 11.5.sp)
                    }
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}

@Composable
fun QuickAddMenuQuantityDialog(
    item: MenuItem,
    onAddToCart: (quantity: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var qtyText by remember { mutableStateOf(if (item.unit == "كيلو") "1.0" else "1") }
    val step = if (item.unit == "كيلو") 0.25 else 1.0
    val parsedQty = qtyText.parseArabicDoubleOrNull() ?: 1.0
    val totalPreview = item.basePrice * parsedQty

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("إضافة للسلة: ${item.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "السعر: ${String.format(Locale.US, "%.0f", item.basePrice)} ج.م / ${item.unit}",
                    fontSize = 12.sp,
                    color = SeaTealPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (item.currentStock >= 0) {
                    Text(
                        text = "المتاح بالمخزن: ${String.format(Locale.US, "%.1f", item.currentStock)} ${item.unit}",
                        fontSize = 10.5.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اكتب الكمية المطلوبة مباشرة:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            val cur = qtyText.parseArabicDoubleOrNull() ?: 1.0
                            val next = (cur - step).coerceAtLeast(step)
                            qtyText = if (next % 1.0 == 0.0) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                        },
                        shape = CircleShape,
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "إنقاص", tint = StatusDanger)
                        }
                    }

                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("الكمية (${item.unit}) *") },
                        placeholder = { Text("اكتب الكمية...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 56.dp)
                    )

                    Surface(
                        onClick = {
                            val cur = qtyText.parseArabicDoubleOrNull() ?: 1.0
                            val next = cur + step
                            qtyText = if (next % 1.0 == 0.0) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                        },
                        shape = CircleShape,
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "زيادة", tint = Color(0xFF15803D))
                        }
                    }
                }

                // Quick presets
                MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presets = if (item.unit == "كيلو") {
                        listOf(0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0)
                    } else {
                        listOf(1.0, 2.0, 3.0, 4.0, 5.0, 10.0, 12.0, 20.0)
                    }
                    items(presets) { pVal ->
                        val pLabel = if (item.unit == "كيلو") {
                            when (pVal) {
                                0.25 -> "¼"
                                0.5 -> "½"
                                0.75 -> "¾"
                                else -> if (pVal % 1.0 == 0.0) "${pVal.toInt()}" else "$pVal"
                            }
                        } else {
                            "${pVal.toInt()}"
                        }
                        FilterChip(
                            selected = parsedQty == pVal,
                            onClick = {
                                qtyText = if (pVal % 1.0 == 0.0) pVal.toInt().toString() else pVal.toString()
                            },
                            label = { Text(pLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }
                }

                // Summary
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "الإجمالي:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", totalPreview)} ج.م",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalQty = qtyText.parseArabicDoubleOrNull() ?: 1.0
                    if (finalQty > 0) {
                        onAddToCart(finalQty)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("إضافة للسلة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun QuickAddMenuItemTile(
    onClick: () -> Unit,
    isLandscapeCompact: Boolean = false,
    currentCategory: String = "الكل"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isLandscapeCompact) 102.dp else 136.dp)
            .clickable(onClick = onClick)
            .testTag("quick_add_menu_item_tile"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0FDF4)
        ),
        border = BorderStroke(1.5.dp, SeaTealPrimary.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(if (isLandscapeCompact) 8.dp else 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (isLandscapeCompact) 102.dp else 136.dp)
                .padding(horizontal = 4.dp, vertical = if (isLandscapeCompact) 4.dp else 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = SeaTealPrimary,
                modifier = Modifier.size(if (isLandscapeCompact) 26.dp else 34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة صنف جديد",
                        tint = Color.White,
                        modifier = Modifier.size(if (isLandscapeCompact) 18.dp else 22.dp)
                    )
                }
            }
            Spacer(Modifier.height(if (isLandscapeCompact) 2.dp else 4.dp))
            Text(
                text = "صنف جديد +",
                fontSize = if (isLandscapeCompact) 9.5.sp else 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = SeaTealPrimary,
                maxLines = 1,
                softWrap = false
            )
            val subText = if (currentCategory != "الكل" && currentCategory.isNotBlank()) currentCategory else "إضافة سريعة"
            Text(
                text = subText,
                fontSize = if (isLandscapeCompact) 7.5.sp else 9.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun QuickAddMenuItemDialog(
    categories: List<String>,
    initialCategory: String,
    allPrinters: List<com.example.printer.PrinterConfig> = emptyList(),
    availableMenuItems: List<MenuItem> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit
) {
    MenuItemEditDialog(
        item = null,
        categories = categories,
        availableMenuItems = availableMenuItems,
        existingIngredients = emptyList(),
        allPrinters = allPrinters,
        initialCategory = initialCategory,
        onSave = { savedItem, _ -> onSave(savedItem) },
        onDismiss = onDismiss
    )
}

private fun normalizePosSearch(text: String): String {
    return text.trim()
        .replace(Regex("[\\s]+"), " ")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ة', 'ه')
        .replace('ى', 'ي')
        .replace(Regex("[\\u064B-\\u065F]"), "")
        .lowercase()
}
