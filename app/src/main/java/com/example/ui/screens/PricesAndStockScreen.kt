package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import com.example.ui.components.MouseLazyColumn
import com.example.ui.components.MouseLazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.InventoryMovement
import com.example.data.model.MenuItem
import com.example.data.model.MenuItemIngredient
import com.example.ui.components.DailyPriceBatchEditorDialog
import com.example.ui.screens.settings.MenuItemEditDialog
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PricesAndStockScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val menuItems by viewModel.menuItems.collectAsStateWithLifecycle()
    val allIngredients by viewModel.allIngredients.collectAsStateWithLifecycle()
    val movements by viewModel.inventoryMovements.collectAsStateWithLifecycle()
    val dailyPriceReviewNeeded by viewModel.dailyPriceReviewNeeded.collectAsStateWithLifecycle()
    val activeCategories by viewModel.categories.collectAsStateWithLifecycle(emptyList())
    val categoryColors by viewModel.categoryColors.collectAsStateWithLifecycle()
    val deviceRole by viewModel.deviceRole.collectAsStateWithLifecycle()
    val allPrinters by viewModel.allPrinters.collectAsStateWithLifecycle()

    // 0: أرصدة المخزون والتوريد, 1: ربط الأصناف والمكونات (BOM), 2: سجل حركات المخزون
    var selectedTopTab by remember { mutableIntStateOf(0) }

    // Dialogs
    var showQuickAddStockDialog by remember { mutableStateOf(false) }
    var showLinkItemsDialog by remember { mutableStateOf(false) }
    var showBatchPriceDialog by remember { mutableStateOf(false) }
    var showAddNewItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MenuItem?>(null) }

    // Per-item action dialogs
    var itemForReceive by remember { mutableStateOf<MenuItem?>(null) }
    var itemForAdjust by remember { mutableStateOf<MenuItem?>(null) }
    var itemForLinkIngredients by remember { mutableStateOf<MenuItem?>(null) }
    var itemToArchive by remember { mutableStateOf<MenuItem?>(null) }

    // Search & Filter state for Stock Tab
    var stockSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    var showOnlyLowStock by remember { mutableStateOf(false) }

    val freshItems = menuItems.filter { it.supportsPreparation }
    val lowStockItems = menuItems.filter { it.currentStock <= it.minStockAlert }
    val categories = remember(activeCategories) {
        listOf("الكل") + activeCategories.filter { !it.isArchived && it.name.isNotBlank() }.map { it.name.trim() }.distinct()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // TabRow at the top
        TabRow(
            selectedTabIndex = selectedTopTab,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = selectedTopTab == 0,
                onClick = { selectedTopTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("أرصدة المخزون (${menuItems.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
            Tab(
                selected = selectedTopTab == 1,
                onClick = { selectedTopTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ربط صنف بصنف (${allIngredients.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
            Tab(
                selected = selectedTopTab == 2,
                onClick = { selectedTopTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("سجل حركات المخزون (${movements.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // TAB CONTENT
        when (selectedTopTab) {
            0 -> {
                // ---------------- TAB 0: Stock Balances & Quick Add ----------------
                val filteredItems = remember(menuItems, stockSearchQuery, selectedCategoryFilter, showOnlyLowStock) {
                    menuItems.filter { item ->
                        val matchesSearch = stockSearchQuery.isBlank() || item.name.contains(stockSearchQuery, ignoreCase = true)
                        val matchesCat = selectedCategoryFilter == "الكل" || item.category == selectedCategoryFilter
                        val matchesLow = !showOnlyLowStock || (item.currentStock <= item.minStockAlert)
                        matchesSearch && matchesCat && matchesLow
                    }
                }

                MouseLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Unified, Coordinated Header Card (Resolves: big space above & empty space below & fixed screen)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // Title & Stats Badges Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = SeaTealPrimary.copy(alpha = 0.15f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Inventory,
                                                    contentDescription = null,
                                                    tint = SeaTealPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "إدارة المخزون والتوريد والربط",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Live Badges
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "الأصناف: ${menuItems.size}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (lowStockItems.isNotEmpty()) StatusDanger.copy(alpha = 0.15f) else StatusSuccess.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (lowStockItems.isNotEmpty()) "⚠️ نواقص: ${lowStockItems.size}" else "المخزون كافٍ ✅",
                                                color = if (lowStockItems.isNotEmpty()) StatusDanger else StatusSuccess,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AmberCoral.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "🔗 روابط: ${allIngredients.size}",
                                                color = AmberCoral,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Daily 7:00 AM Alert Banner (compact, inside header card)
                                AnimatedVisibility(visible = dailyPriceReviewNeeded) {
                                    Surface(
                                        color = AmberCoral.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, AmberCoral.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Alarm,
                                                    contentDescription = null,
                                                    tint = AmberCoral,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "تنبيه 7:00 ص: يرجى تحديث أسعار الأسماك الفريش اليومية",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                TextButton(
                                                    onClick = { showBatchPriceDialog = true },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("تحديث الآن", color = AmberCoral, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                IconButton(
                                                    onClick = { viewModel.dismissDailyPriceReminder() },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Symmetrical 2x2 Grid of Quick Actions (no empty spaces)
                                // Row 1: Primary Stock Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showQuickAddStockDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("quick_add_stock_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إضافة رصيد مخزون (+)", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                    }

                                    Button(
                                        onClick = { selectedTopTab = 1 },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("open_link_items_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ربط صنف بصنف (BOM)", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Row 2: Secondary Quick Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showBatchPriceDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("open_batch_price_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("تعديل أسعار", fontSize = 11.sp, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = { showAddNewItemDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("add_new_menu_item_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("صنف جديد", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Search & Low Stock Filter Chip
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = stockSearchQuery,
                                onValueChange = { stockSearchQuery = it },
                                placeholder = { Text("بحث باسم الصنف...", fontSize = 12.5.sp, lineHeight = 20.sp) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (stockSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { stockSearchQuery = "" }) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = com.example.util.ArabicKeyboardUtil.search(),
                                shape = RoundedCornerShape(10.dp),
                                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 56.dp)
                            )

                            FilterChip(
                                selected = showOnlyLowStock,
                                onClick = { showOnlyLowStock = !showOnlyLowStock },
                                leadingIcon = {
                                    if (showOnlyLowStock) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    } else {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(14.dp))
                                    }
                                },
                                label = { Text("النواقص فقط (${lowStockItems.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StatusDanger.copy(alpha = 0.2f),
                                    selectedLabelColor = StatusDanger
                                ),
                                modifier = Modifier.height(42.dp)
                            )
                        }
                    }

                    // 3. Category Chips
                    item {
                        MouseLazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(categories) { cat ->
                                val isSelected = selectedCategoryFilter == cat
                                val catTheme = com.example.util.CategoryColorUtil.resolveCategoryStyle(cat, categoryColors[cat])
                                Surface(
                                    onClick = { selectedCategoryFilter = cat },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) (if (categoryColors[cat] != null) catTheme.priceBgColor else SeaTealPrimary) else catTheme.bgColor,
                                    border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, if (isSelected) (if (categoryColors[cat] != null) catTheme.priceBgColor else SeaTealPrimary) else catTheme.borderColor)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else catTheme.tagTextColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Items List or Empty State
                    if (filteredItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "لا توجد أصناف تطابق البحث أو الفلتر الحالي",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.5.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredItems, key = { it.id }) { item ->
                            val isLow = item.currentStock <= item.minStockAlert
                            val isDepleted = item.currentStock <= 0.0

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = if (isDepleted) BorderStroke(1.dp, StatusDanger.copy(alpha = 0.5f))
                                else if (isLow) BorderStroke(1.dp, StatusWarning.copy(alpha = 0.5f))
                                else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stock_item_${item.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                val catTheme = com.example.util.CategoryColorUtil.resolveCategoryStyle(item.category, categoryColors[item.category])
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = catTheme.bgColor,
                                                    border = BorderStroke(0.8.dp, catTheme.borderColor)
                                                ) {
                                                    Text(
                                                        text = item.category,
                                                        fontSize = 10.sp,
                                                        color = catTheme.priceBgColor,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (item.supportsPreparation) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = AmberCoral.copy(alpha = 0.12f)
                                                    ) {
                                                        Text(
                                                            text = "يقبل تجهيز",
                                                            fontSize = 9.sp,
                                                            color = AmberCoral,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (item.kitchenName.isNotBlank() && item.kitchenName.trim() != item.name.trim()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "وصف التشغيل: ${item.kitchenName}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AmberCoral
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = when {
                                                        isDepleted -> StatusDanger.copy(alpha = 0.15f)
                                                        isLow -> StatusWarning.copy(alpha = 0.2f)
                                                        else -> StatusSuccess.copy(alpha = 0.15f)
                                                    }
                                                ) {
                                                    Text(
                                                        text = "الرصيد بالمخزن: ${String.format(Locale.US, "%.2f", item.currentStock)} ${item.unit}",
                                                        color = when {
                                                            isDepleted -> StatusDanger
                                                            isLow -> StatusWarning
                                                            else -> StatusSuccess
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "حد التنبيه: ${item.minStockAlert} ${item.unit}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Check linked items / ingredients
                                            val linkedForItem = remember(item.id, allIngredients) {
                                                allIngredients.filter { it.menuItemId == item.id }
                                            }
                                            if (linkedForItem.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "🔗 يسحب تلقائياً مع البيع: " + linkedForItem.joinToString("، ") { "${it.ingredientName} (${it.requiredQuantityPerUnit} ${it.unit})" },
                                                    fontSize = 10.5.sp,
                                                    color = AmberCoral,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${String.format(Locale.US, "%.1f", item.basePrice)} ج/${item.unit}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                IconButton(
                                                    onClick = { itemToEdit = item },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "تعديل الصنف والطابعات",
                                                        tint = SeaTealPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { itemToArchive = item },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Archive,
                                                        contentDescription = "أرشفة الصنف وإخفاؤه",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "التكلفة: ${item.costPrice} ج.م",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    // Action Buttons Row on the Card
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Quick Receive / Add Stock
                                        Button(
                                            onClick = { itemForReceive = item },
                                            colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1.1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إضافة رصيد (+)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Adjust / Waste
                                        OutlinedButton(
                                            onClick = { itemForAdjust = item },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تسوية / هالك", fontSize = 11.5.sp)
                                        }

                                        // Link Ingredients
                                        val linkedCount = allIngredients.count { it.menuItemId == item.id }
                                        OutlinedButton(
                                            onClick = { itemForLinkIngredients = item },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (linkedCount > 0) AmberCoral else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1.1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (linkedCount > 0) "مرتبط ($linkedCount)" else "ربط بمكون",
                                                fontSize = 11.5.sp,
                                                fontWeight = if (linkedCount > 0) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(56.dp))
                        }
                    }
                }
            }

            1 -> {
                // ---------------- TAB 1: DEDICATED ORGANIZED ITEM LINKING (BOM) ----------------
                // Directly solves: "وموضوع الربط بالصنف يكون منظم اكتر مش يكون كأني بضيف صنف جديد من فوق يعني مثلا اختار من الاصناف الموجودة صنف وبعدين اختار صنف اخر من الاصناف الموجودة وبردر وطبعا مع طريقة ربطه"
                ItemLinkingOrganizedView(
                    allMenuItems = menuItems,
                    allIngredients = allIngredients,
                    onSaveIngredient = { ing -> viewModel.saveIngredient(ing) },
                    onDeleteIngredient = { id -> viewModel.deleteIngredient(id) }
                )
            }

            2 -> {
                // ---------------- TAB 2: Audit Movements Log View ----------------
                val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US) }

                MouseLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    if (movements.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "لا توجد حركات مخزون مسجلة بعد",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(movements, key = { it.id }) { mov ->
                            val isPositive = mov.quantityChange > 0
                            val typeLabel = when (mov.movementType) {
                                "RECEIVE" -> "استلام شحنة واردة (+)"
                                "SALE" -> "بيع أوردر (-)"
                                "ADJUSTMENT" -> "تسوية جردية"
                                "WASTE" -> "هالك وتالف (-)"
                                else -> mov.movementType
                            }
                            val typeColor = when (mov.movementType) {
                                "RECEIVE" -> StatusSuccess
                                "SALE" -> StatusInfo
                                "ADJUSTMENT" -> StatusWarning
                                "WASTE" -> StatusDanger
                                else -> Color.Gray
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = typeColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = typeLabel,
                                                    color = typeColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = mov.itemName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }

                                        if (mov.notes.isNotBlank()) {
                                            Text(
                                                text = mov.notes,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = dateFormat.format(Date(mov.timestamp)),
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isPositive) "+${String.format(Locale.US, "%.2f", mov.quantityChange)}" else String.format(Locale.US, "%.2f", mov.quantityChange),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isPositive) StatusSuccess else StatusDanger
                                        )
                                        Text(
                                            text = "الرصيد بعد: ${String.format(Locale.US, "%.2f", mov.balanceAfter)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }

    // ------------------- DIALOGS -------------------

    // 1. Quick Add Stock to Existing Item Dialog (Requested explicitly by user)
    if (showQuickAddStockDialog) {
        val catList = remember(activeCategories) {
            activeCategories.filter { !it.isArchived && it.name.isNotBlank() }.map { it.name.trim() }.distinct()
        }
        QuickAddStockToExistingItemDialog(
            allMenuItems = menuItems,
            activeCategories = catList,
            onConfirm = { itemId, qty, notes ->
                viewModel.receiveShipment(itemId, qty, notes)
                showQuickAddStockDialog = false
            },
            onDismiss = { showQuickAddStockDialog = false }
        )
    }

    // 2. Batch Price Dialog
    if (showBatchPriceDialog) {
        DailyPriceBatchEditorDialog(
            freshItems = menuItems,
            onSave = { updates ->
                viewModel.saveDailyPrices(updates)
                showBatchPriceDialog = false
            },
            onDismiss = { showBatchPriceDialog = false }
        )
    }

    // 3. Receive Shipment Dialog for specific item
    itemForReceive?.let { item ->
        ReceiveShipmentDialog(
            item = item,
            onConfirm = { qty, notes ->
                viewModel.receiveShipment(item.id, qty, notes)
                itemForReceive = null
            },
            onDismiss = { itemForReceive = null }
        )
    }

    // 4. Adjust Stock Dialog for specific item
    itemForAdjust?.let { item ->
        AdjustStockDialog(
            item = item,
            onConfirm = { newBal, reason ->
                viewModel.adjustStock(item.id, newBal, reason)
                itemForAdjust = null
            },
            onDismiss = { itemForAdjust = null }
        )
    }

    // 5. Link Ingredients Dialog for specific item
    itemForLinkIngredients?.let { item ->
        LinkIngredientsDialog(
            item = item,
            allMenuItems = menuItems,
            linkedIngredients = allIngredients.filter { it.menuItemId == item.id },
            onSaveIngredient = { ing -> viewModel.saveIngredient(ing) },
            onDeleteIngredient = { id -> viewModel.deleteIngredient(id) },
            onDismiss = { itemForLinkIngredients = null }
        )
    }

    // 6. Add New Item or Edit Item & Printers Dialog
    if (showAddNewItemDialog || itemToEdit != null) {
        val catList = remember(activeCategories) {
            activeCategories.filter { !it.isArchived && it.name.isNotBlank() }.map { it.name.trim() }.distinct()
        }
        val existingIngs = remember(itemToEdit, allIngredients) {
            if (itemToEdit != null) allIngredients.filter { it.menuItemId == itemToEdit?.id } else emptyList()
        }
        MenuItemEditDialog(
            item = itemToEdit,
            categories = catList,
            availableMenuItems = menuItems,
            existingIngredients = existingIngs,
            allPrinters = allPrinters,
            onSave = { savedItem, linkedIngredients ->
                val oldName = itemToEdit?.name
                viewModel.saveMenuItemWithIngredients(savedItem, linkedIngredients, oldName = oldName)
                showAddNewItemDialog = false
                itemToEdit = null
            },
            onDismiss = {
                showAddNewItemDialog = false
                itemToEdit = null
            }
        )
    }

    itemToArchive?.let { targetItem ->
        AlertDialog(
            onDismissRequest = { itemToArchive = null },
            title = { Text("أرشفة الصنف وإخفاؤه 📦", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text(
                    text = "هل تريد أرشفة صنف '${targetItem.name}'؟\nسيتم إخفاؤه فوراً من شاشة البيع وقائمة المخزون، ونقله إلى تبويب المؤرشف في الإعدادات مع إمكانية استعادته لاحقاً.",
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.archiveMenuItem(targetItem.id, true)
                        itemToArchive = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberCoral)
                ) {
                    Text("أرشفة الآن 📦")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToArchive = null }) { Text("إلغاء") }
            }
        )
    }
}

/**
 * Dedicated, highly-organized Component for linking one existing item to another existing item (BOM).
 * Solves:
 * "وموضوع الربط بالصنف يكون منظم اكتر مش يكون كأني بضيف صنف جديد من فوق يعني مثلا اختار من الاصناف الموجودة صنف وبعدين اختار صنف اخر من الاصناف الموجودة وبردر وطبعا مع طريقة ربطه"
 */
@Composable
fun ItemLinkingOrganizedView(
    allMenuItems: List<MenuItem>,
    allIngredients: List<MenuItemIngredient>,
    onSaveIngredient: (MenuItemIngredient) -> Unit,
    onDeleteIngredient: (Long) -> Unit
) {
    // Step 1: Parent Item (المباع / الوجبة)
    var selectedParentItem by remember { mutableStateOf<MenuItem?>(allMenuItems.firstOrNull()) }
    var parentSearchQuery by remember { mutableStateOf("") }
    var isParentDropdownOpen by remember { mutableStateOf(false) }

    // Step 2: Child Item (الخام / المكون المراد سحبه)
    val availableChildCandidates = remember(allMenuItems, selectedParentItem) {
        allMenuItems.filter { it.id != (selectedParentItem?.id ?: 0L) }
    }
    var selectedChildItem by remember { mutableStateOf<MenuItem?>(availableChildCandidates.firstOrNull()) }
    var childSearchQuery by remember { mutableStateOf("") }
    var isChildDropdownOpen by remember { mutableStateOf(false) }

    // Step 3: Quantity & Link Mode
    var deductQtyStr by remember { mutableStateOf("1.0") }
    var linkNotes by remember { mutableStateOf("") }
    var editingIngredient by remember { mutableStateOf<MenuItemIngredient?>(null) }

    MouseLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top instruction / Explanation banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = AmberCoral.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, AmberCoral.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AmberCoral),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ربط صنف بصنف لسحب المخزون آلياً (المكونات والوصفات BOM)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "اختر الصنف المباع من القائمة، ثم اختر الصنف المراد خصمه تلقائياً من المخزن عند كل بيع (مثل خصم أرز أو سمك خام أو خبز).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section: CREATE A NEW LINK (Step by Step Form)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🔗 إنشاء ربط جديد بين صنفين موجودين:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SeaTealPrimary
                    )

                    // ---------------- STEP 1: Select Main/Sold Item ----------------
                    Text(
                        text = "1. اختر الصنف الأساسي (الوجبة أو الطلب المباع):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isParentDropdownOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selectedParentItem != null) SeaTealPrimary else MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedParentItem?.let { "📌 ${it.name} (قسم: ${it.category}) - ${it.basePrice} ج.م" }
                                        ?: "اضغط لاختيار الصنف المباع...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1
                                )
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = isParentDropdownOpen,
                            onDismissRequest = { isParentDropdownOpen = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            OutlinedTextField(
                                value = parentSearchQuery,
                                onValueChange = { parentSearchQuery = it },
                                placeholder = { Text("ابحث بالاسم...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 54.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            val filteredParents = allMenuItems.filter {
                                parentSearchQuery.isBlank() || it.name.contains(parentSearchQuery, ignoreCase = true)
                            }
                            filteredParents.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("${item.category} | ${item.basePrice} ج.م", fontSize = 10.5.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        selectedParentItem = item
                                        isParentDropdownOpen = false
                                        // Update candidate for child
                                        if (selectedChildItem?.id == item.id) {
                                            selectedChildItem = allMenuItems.firstOrNull { it.id != item.id }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // ---------------- STEP 2: Select Child/Raw Item ----------------
                    Text(
                        text = "2. اختر الصنف المراد سحبه تلقائياً من المخزن (المكون الخام أو الطبق التابع):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isChildDropdownOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selectedChildItem != null) AmberCoral else MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedChildItem?.let { "📦 ${it.name} (رصيد حالي: ${it.currentStock} ${it.unit})" }
                                        ?: "اضغط لاختيار الصنف المسحوب...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = AmberCoral,
                                    maxLines = 1
                                )
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = isChildDropdownOpen,
                            onDismissRequest = { isChildDropdownOpen = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            OutlinedTextField(
                                value = childSearchQuery,
                                onValueChange = { childSearchQuery = it },
                                placeholder = { Text("ابحث بالاسم...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 54.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            val filteredChildren = availableChildCandidates.filter {
                                childSearchQuery.isBlank() || it.name.contains(childSearchQuery, ignoreCase = true)
                            }
                            filteredChildren.forEach { raw ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(raw.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("رصيد: ${raw.currentStock} ${raw.unit}", fontSize = 10.5.sp, color = SeaTealPrimary)
                                        }
                                    },
                                    onClick = {
                                        selectedChildItem = raw
                                        isChildDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // ---------------- STEP 3: Deduction Quantity & Rules ----------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val clean = deductQtyStr.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                                val cur = clean.toDoubleOrNull() ?: 1.0
                                val next = (cur - 0.25).coerceAtLeast(0.05)
                                deductQtyStr = if (next == next.toInt().toDouble()) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SeaTealPrimary)
                        }

                        OutlinedTextField(
                            value = deductQtyStr,
                            onValueChange = { deductQtyStr = it },
                            label = { Text("كمية السحب لكل 1 طلب") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        )

                        IconButton(
                            onClick = {
                                val clean = deductQtyStr.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                                val cur = clean.toDoubleOrNull() ?: 1.0
                                val next = cur + 0.25
                                deductQtyStr = if (next == next.toInt().toDouble()) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SeaTealPrimary)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = selectedChildItem?.unit ?: "وحدة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                            )
                        }
                    }

                    // Quick Quantity Presets for easy selection
                    val quickLinkPresets = listOf("0.25", "0.5", "0.75", "1.0", "1.5", "2.0")
                    MouseLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickLinkPresets) { pStr ->
                            FilterChip(
                                selected = deductQtyStr.trim() == pStr,
                                onClick = { deductQtyStr = pStr },
                                label = { Text("$pStr ${selectedChildItem?.unit ?: ""}", fontSize = 11.sp) }
                            )
                        }
                    }

                    // Dynamic Live Explanation
                    if (selectedParentItem != null && selectedChildItem != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SeaTealPrimary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = SeaTealPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "عند بيع (1) «${selectedParentItem?.name}»، سيتم تلقائياً سحب ($deductQtyStr ${selectedChildItem?.unit}) من رصيد «${selectedChildItem?.name}».",
                                    fontSize = 11.sp,
                                    color = SeaTealPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Confirm Link Button
                    Button(
                        onClick = {
                            val parent = selectedParentItem
                            val child = selectedChildItem
                            val clean = deductQtyStr.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                            val q = clean.toDoubleOrNull() ?: 0.0
                            if (parent != null && child != null && q > 0) {
                                onSaveIngredient(
                                    MenuItemIngredient(
                                        id = 0L,
                                        menuItemId = parent.id,
                                        ingredientItemId = child.id,
                                        ingredientName = child.name,
                                        requiredQuantityPerUnit = q,
                                        unit = child.unit,
                                        notes = linkNotes.ifBlank { "سحب تلقائي مع بيع ${parent.name}" }
                                    )
                                )
                                deductQtyStr = "1.0"
                                linkNotes = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🔗 حفظ ربط الصنفين الآن", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

        }

        // Section: CURRENTLY LINKED ITEMS IN THE RESTAURANT
        item {
            Text(
                text = "قائمة الأصناف المربوطة حالياً بالمطعم (${allIngredients.size} رابط):",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (allIngredients.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لا توجد روابط بين الأصناف حالياً.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "يمكنك استخدام النموذج بالأعلى لاختيار وجبة واختيار المكون الذي يتم سحبه منها تلقائياً.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            // Group ingredients by parent item
            val groupedByParent = allIngredients.groupBy { it.menuItemId }

            items(groupedByParent.keys.toList()) { parentId ->
                val parentItem = allMenuItems.firstOrNull { it.id == parentId }
                val ingredientsForThisParent = groupedByParent[parentId] ?: emptyList()

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, AmberCoral.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AmberCoral.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "الصنف المباع",
                                        color = AmberCoral,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = parentItem?.name ?: "صنف مجهول (#$parentId)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "${ingredientsForThisParent.size} مكونات مسحوبة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // List of linked components for this item
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ingredientsForThisParent.forEach { ing ->
                                val rawItem = allMenuItems.firstOrNull { it.id == ing.ingredientItemId }
                                val isDepleted = (rawItem?.currentStock ?: 0.0) <= 0.0
                                val isLow = (rawItem?.currentStock ?: 0.0) <= (rawItem?.minStockAlert ?: 3.0)

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDepleted) StatusDanger.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "↳ ${ing.ingredientName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = if (isDepleted) StatusDanger else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isDepleted) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = StatusDanger.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "⚠️ نفد من المخزن",
                                                            color = StatusDanger,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "كمية السحب: ${ing.requiredQuantityPerUnit} ${ing.unit} لكل 1 طلب | الرصيد الحالي للمكون: ${rawItem?.currentStock ?: 0.0} ${ing.unit}",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 1.dp)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { editingIngredient = ing },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "تعديل كمية السحب",
                                                    tint = SeaTealPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteIngredient(ing.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "فك الارتباط",
                                                    tint = StatusDanger,
                                                    modifier = Modifier.size(16.dp)
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
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Dialog for editing an existing linked ingredient's quantity directly
    editingIngredient?.let { ing ->
        var editQtyStr by remember(ing) { mutableStateOf(ing.requiredQuantityPerUnit.toString()) }
        AlertDialog(
            onDismissRequest = { editingIngredient = null },
            title = {
                Text("تعديل كمية السحب لـ ${ing.ingredientName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("حدد الكمية المسحوبة من رصيد المكون عند كل بيع لهذا الصنف:", fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val clean = editQtyStr.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                                val cur = clean.toDoubleOrNull() ?: 1.0
                                val next = (cur - 0.25).coerceAtLeast(0.05)
                                editQtyStr = if (next == next.toInt().toDouble()) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                            }
                        ) {
                            Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SeaTealPrimary)
                        }
                        OutlinedTextField(
                            value = editQtyStr,
                            onValueChange = { editQtyStr = it },
                            label = { Text("الكمية (${ing.unit})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val clean = editQtyStr.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                                val cur = clean.toDoubleOrNull() ?: 1.0
                                val next = cur + 0.25
                                editQtyStr = if (next == next.toInt().toDouble()) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                            }
                        ) {
                            Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SeaTealPrimary)
                        }
                    }
                    val presets = listOf("0.25", "0.5", "0.75", "1.0", "1.5", "2.0")
                    MouseLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets) { pStr ->
                            FilterChip(
                                selected = editQtyStr.trim() == pStr,
                                onClick = { editQtyStr = pStr },
                                label = { Text("$pStr ${ing.unit}", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = editQtyStr.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                        val q = clean.toDoubleOrNull() ?: 0.0
                        if (q > 0) {
                            onSaveIngredient(ing.copy(requiredQuantityPerUnit = q))
                            editingIngredient = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingIngredient = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Dedicated Dialog for adding stock / supply directly to an existing item.
 * Solves: "ويكون فيه زر اضافة مخزون لصنف موجود بالفعل"
 */
@Composable
fun QuickAddStockToExistingItemDialog(
    allMenuItems: List<MenuItem>,
    activeCategories: List<String> = emptyList(),
    onConfirm: (itemId: Long, quantity: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var selectedItem by remember { mutableStateOf<MenuItem?>(allMenuItems.firstOrNull()) }
    var quantityStr by remember { mutableStateOf("") }
    var invoiceNotes by remember { mutableStateOf("") }
    var isDropdownOpen by remember { mutableStateOf(false) }

    val categories = remember(activeCategories) {
        listOf("الكل") + activeCategories.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    val filteredList = remember(allMenuItems, searchQuery, selectedCategory) {
        allMenuItems.filter { item ->
            val matchSearch = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == "الكل" || item.category == selectedCategory
            matchSearch && matchCat
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = SeaTealPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = SeaTealPrimary)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("إضافة مخزون لصنف موجود بالفعل", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("توريد وزيادة رصيد صنف مسجل في القائمة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                // Step 1: Select Item
                Text(
                    text = "اختر الصنف المراد زيادة رصيده:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isDropdownOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, SeaTealPrimary)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedItem?.let { "${it.name} (${it.category}) - رصيد: ${it.currentStock} ${it.unit}" }
                                    ?: "اضغط لاختيار الصنف...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                maxLines = 1
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = isDropdownOpen,
                        onDismissRequest = { isDropdownOpen = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث باسم الصنف...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 54.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        filteredList.take(20).forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            "القسم: ${item.category} | الرصيد الحالي: ${item.currentStock} ${item.unit}",
                                            fontSize = 10.5.sp,
                                            color = if (item.currentStock <= item.minStockAlert) StatusDanger else SeaTealPrimary
                                        )
                                    }
                                },
                                onClick = {
                                    selectedItem = item
                                    isDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Selected Item Card Summary
                selectedItem?.let { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SeaTealPrimary.copy(alpha = 0.08f)),
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
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("الرصيد الحالي بالمخزن: ${item.currentStock} ${item.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SeaTealPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = item.unit,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = SeaTealPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Quantity to add
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("الكمية المضافة / المستلمة (${item.unit})") },
                        placeholder = { Text("مثال: 15.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Live preview of balance after addition
                    val addedQty = quantityStr.toDoubleOrNull() ?: 0.0
                    if (addedQty > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StatusSuccess.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✅ الرصيد بعد الإضافة سيصبح: ${String.format(Locale.US, "%.2f", item.currentStock + addedQty)} ${item.unit}",
                                color = StatusSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Invoice notes / Supplier
                    OutlinedTextField(
                        value = invoiceNotes,
                        onValueChange = { invoiceNotes = it },
                        label = { Text("رقم الفاتورة / اسم المورد أو ملاحظات") },
                        placeholder = { Text("توريد سوق اليوم...") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val item = selectedItem
                    val qty = quantityStr.toDoubleOrNull() ?: 0.0
                    if (item != null && qty > 0) {
                        onConfirm(item.id, qty, invoiceNotes.ifBlank { "توريد واستلام مخزني" })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("تأكيد إضافة المخزون (+)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun LinkIngredientsDialog(
    item: MenuItem,
    allMenuItems: List<MenuItem>,
    linkedIngredients: List<MenuItemIngredient>,
    onSaveIngredient: (MenuItemIngredient) -> Unit,
    onDeleteIngredient: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val availableIngredients = allMenuItems.filter { it.id != item.id }
    var selectedRawItem by remember { mutableStateOf<MenuItem?>(availableIngredients.firstOrNull()) }
    var requiredQtyStr by remember { mutableStateOf("0.25") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("ربط صنف «${item.name}» بمكونات أخرى", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "عند نفاد أي مكون مرتبط (مثل الأرز)، سيقوم النظام تلقائياً بتنبيه الكاشير وإيقاف بيع الوجبة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            MouseLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "المكونات المرتبطة حالياً (${linkedIngredients.size}):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (linkedIngredients.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "لا توجد مكونات مرتبطة بهذا الصنف حتى الآن.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                } else {
                    items(linkedIngredients) { ing ->
                        val rawItem = allMenuItems.firstOrNull { it.id == ing.ingredientItemId }
                        val isDepleted = (rawItem?.currentStock ?: 0.0) <= 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDepleted) StatusDanger.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "• ${ing.ingredientName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDepleted) StatusDanger else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "الكمية المطلوبة لكل وحدة: ${ing.requiredQuantityPerUnit} ${ing.unit} | الرصيد المتاح: ${rawItem?.currentStock ?: 0.0} ${ing.unit}",
                                        fontSize = 10.sp,
                                        color = if (isDepleted) StatusDanger else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isDepleted) {
                                        Text(
                                            text = "⚠️ نفد المخزون - سيتم إيقاف/تنبيه الوجبة بالـ POS",
                                            fontSize = 9.sp,
                                            color = StatusDanger,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteIngredient(ing.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "فك الارتباط", tint = StatusDanger, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "+ إضافة ربط بمكون جديد من القائمة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                item {
                    Text(text = "اختر الصنف المكون (الخام):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(availableIngredients) { raw ->
                            val isChosen = selectedRawItem?.id == raw.id
                            FilterChip(
                                selected = isChosen,
                                onClick = { selectedRawItem = raw },
                                label = { Text(raw.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    selectedRawItem?.let { raw ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = requiredQtyStr,
                                    onValueChange = { requiredQtyStr = it },
                                    label = { Text("الكمية المطلوبة (${raw.unit})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val clean = requiredQtyStr.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                                        val q = clean.toDoubleOrNull() ?: 0.0
                                        if (q > 0) {
                                            onSaveIngredient(
                                                MenuItemIngredient(
                                                    menuItemId = item.id,
                                                    ingredientItemId = raw.id,
                                                    ingredientName = raw.name,
                                                    requiredQuantityPerUnit = q,
                                                    unit = raw.unit,
                                                    notes = notes
                                                )
                                            )
                                            requiredQtyStr = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                                ) {
                                    Text("ربط الآن", fontSize = 11.sp)
                                }
                            }

                            val presets = listOf("0.25", "0.5", "0.75", "1.0", "1.5", "2.0")
                            MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(presets) { pStr ->
                                    FilterChip(
                                        selected = requiredQtyStr.trim() == pStr,
                                        onClick = { requiredQtyStr = pStr },
                                        label = { Text("$pStr ${raw.unit}", fontSize = 10.5.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("تم") }
        },
        dismissButton = {}
    )
}

@Composable
fun ReceiveShipmentDialog(
    item: MenuItem,
    onConfirm: (quantity: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var qtyStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("استلام توريد شحنة: ${item.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "الرصيد الحالي بالمخزن: ${item.currentStock} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("الكمية المستلمة (${item.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("اسم المورد / رقم الفاتورة أو ملاحظات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qtyStr.toDoubleOrNull() ?: 0.0
                    if (q > 0) {
                        onConfirm(q, if (notes.isBlank()) "توريد استلام مخزني" else notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("إضافة للمخزون")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun AdjustStockDialog(
    item: MenuItem,
    onConfirm: (newBalance: Double, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newBalStr by remember { mutableStateOf(item.currentStock.toString()) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسوية جردية / إتلاف: ${item.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "الرصيد المسجل حالياً: ${item.currentStock} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = newBalStr,
                    onValueChange = { newBalStr = it },
                    label = { Text("الرصيد الفعلي بعد الجرد (${item.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب التسوية (فرق ميزان، هالك تنظيف، تلف...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val b = newBalStr.toDoubleOrNull()
                    if (b != null && b >= 0) {
                        onConfirm(b, if (reason.isBlank()) "تسوية جردية يدوية" else reason)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberCoral)
            ) {
                Text("تطبيق التسوية")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
