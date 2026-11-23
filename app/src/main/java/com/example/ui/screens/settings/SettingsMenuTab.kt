package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MenuCategory
import com.example.data.model.MenuItem
import com.example.data.model.MenuItemIngredient
import com.example.ui.components.PasswordPromptDialog
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.util.CategoryColorUtil
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsMenuTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val categories by viewModel.allCategoriesIncludingArchived.collectAsStateWithLifecycle()
    val menuItems by viewModel.allMenuItemsIncludingArchived.collectAsStateWithLifecycle()
    val allIngredients by viewModel.allIngredients.collectAsStateWithLifecycle()
    val allPrinters by viewModel.allPrinters.collectAsStateWithLifecycle()
    val categoryColors by viewModel.categoryColors.collectAsStateWithLifecycle()
    val deviceRole by viewModel.deviceRole.collectAsStateWithLifecycle()

    var subTab by remember { mutableIntStateOf(0) } // 0: الأصناف والأسعار, 1: أقسام القائمة

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    var selectedStatusFilter by remember { mutableStateOf("ACTIVE") } // "ACTIVE", "ARCHIVED", "ALL"

    val activeCount = remember(menuItems) { menuItems.count { !it.isArchived } }
    val archivedCount = remember(menuItems) { menuItems.count { it.isArchived } }

    val distinctCategories = remember(categories, menuItems) {
        val catNames = categories.map { it.name.trim() }.filter { it.isNotBlank() }
        val itemCatNames = menuItems.map { it.category.trim() }.filter { it.isNotBlank() }
        listOf("الكل") + (catNames + itemCatNames).distinct()
    }

    val filteredMenuItems = remember(menuItems, searchQuery, selectedCategoryFilter, selectedStatusFilter) {
        menuItems.filter { item ->
            val q = searchQuery.trim()
            val matchesQuery = q.isBlank() ||
                item.name.contains(q, ignoreCase = true) ||
                item.category.contains(q, ignoreCase = true) ||
                item.kitchenName.contains(q, ignoreCase = true) ||
                item.unit.contains(q, ignoreCase = true) ||
                item.basePrice.toString().contains(q)

            val matchesCategory = selectedCategoryFilter == "الكل" || item.category.trim() == selectedCategoryFilter.trim()

            val matchesStatus = when (selectedStatusFilter) {
                "ACTIVE" -> !item.isArchived
                "ARCHIVED" -> item.isArchived
                else -> true
            }

            matchesQuery && matchesCategory && matchesStatus
        }
    }

    var itemToEdit by remember { mutableStateOf<MenuItem?>(null) }
    var showNewItemDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<MenuItem?>(null) }
    var categoryToEdit by remember { mutableStateOf<MenuCategory?>(null) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<MenuCategory?>(null) }
    var showBatchCategoryDialog by remember { mutableStateOf(false) }
    var categoryForBatchEdit by remember { mutableStateOf<MenuCategory?>(null) }
    var showBatchPriceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = Color.Transparent
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("الأصناف والأسعار (${menuItems.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("أقسام القائمة (${categories.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (subTab == 0) {
            // Menu Items List & CRUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تعديل الأصناف وأسعار البيع:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                    OutlinedButton(
                        onClick = { showBatchPriceDialog = true },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("⚡ تعديل الأسعار", fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = { showNewItemDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_new_menu_item_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة صنف جديد", fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search and Filter Card for Menu Items
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("🔍 بحث عن صنف بالاسم، أو القسم، أو اسم المطبخ...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = SeaTealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح البحث",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = SeaTealPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("menu_items_search_input")
                    )

                    // Categories Filter Chips Row
                    MouseLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(distinctCategories) { catName ->
                            val isSelected = selectedCategoryFilter == catName
                            val count = if (catName == "الكل") menuItems.size else menuItems.count { it.category.trim() == catName }
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryFilter = catName },
                                label = {
                                    Text(
                                        text = "$catName ($count)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SeaTealPrimary.copy(alpha = 0.18f),
                                    selectedLabelColor = SeaTealPrimary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }

                    // Status and Count Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                "ACTIVE" to "الأصناف النشطة (${activeCount})",
                                "ARCHIVED" to "📦 المؤرشفة (${archivedCount})",
                                "ALL" to "الكل (${menuItems.size})"
                            ).forEach { (key, label) ->
                                val isSel = selectedStatusFilter == key
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) SeaTealPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSel) SeaTealPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.clickable { selectedStatusFilter = key }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) SeaTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "المطابق: ${filteredMenuItems.size} من ${menuItems.size} صنف",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SeaTealPrimary
                        )
                    }
                }
            }

            if (selectedStatusFilter == "ARCHIVED") {
                var showPurgeConfirm by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📦 الأصناف المؤرشفة مخفية من البيع والمخزون",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (archivedCount > 0) {
                            TextButton(onClick = { showPurgeConfirm = true }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("حذف كافة المؤرشف نهائياً", fontSize = 10.5.sp, color = StatusDanger)
                            }
                        }
                    }
                }
                if (showPurgeConfirm) {
                    AlertDialog(
                        onDismissRequest = { showPurgeConfirm = false },
                        title = { Text("حذف نهائي للأصناف المؤرشفة", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                        text = { Text("هل تريد حذف كافة الأصناف المؤرشفة ($archivedCount صنف) نهائياً من قاعدة البيانات؟ لن يمكن استعادتها.", fontSize = 12.5.sp) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.purgeAllArchivedMenuItems()
                                    showPurgeConfirm = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                            ) {
                                Text("تأكيد الحذف النهائي")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showPurgeConfirm = false }) { Text("إلغاء") }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredMenuItems.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "لا توجد أصناف تطابق معايير البحث أو التصفية",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                selectedCategoryFilter = "الكل"
                                selectedStatusFilter = "ALL"
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مسح البحث وعرض كل الأصناف", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                MouseLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMenuItems, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
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
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (item.id > 0L) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF64748B).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "#${item.id}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF334155),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SeaTealPrimary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = item.category,
                                                fontSize = 10.sp,
                                                color = SeaTealPrimary,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (item.isArchived) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color.Gray.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "مؤرشف",
                                                    fontSize = 9.sp,
                                                    color = Color.DarkGray,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (item.kitchenName.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 1.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.RestaurantMenu, contentDescription = null, tint = AmberCoral, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "أمر التشغيل: ${item.kitchenName}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AmberCoral
                                            )
                                        }
                                    }
                                    Text(
                                        text = "الوحدة: ${item.unit} | التكلفة: ${item.costPrice} ج | المخزون: ${item.currentStock}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val printerSummary = remember(item.targetPrinter, allPrinters) {
                                        val parts = item.targetPrinter.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                                        if (parts.contains("ALL") || parts.isEmpty()) {
                                            "كافة الطابعات 🖨️"
                                        } else {
                                            val names = allPrinters.filter { parts.contains(it.id.uppercase()) }.map { it.displayName }
                                            if (names.isNotEmpty()) names.joinToString(" + ")
                                            else parts.joinToString(" + ")
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = null,
                                            tint = SeaTealPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "الطابعات: $printerSummary",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SeaTealPrimary
                                        )
                                    }
                                    if (item.supportsPreparation) {
                                        Text(
                                            text = "• يدعم طرق التجهيز (شوي/قلي/سنجاري)",
                                            fontSize = 10.sp,
                                            color = AmberCoral
                                        )
                                    }
                                    val linkedForItem = remember(item.id, allIngredients) {
                                        allIngredients.filter { it.menuItemId == item.id }
                                    }
                                    if (linkedForItem.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 1.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = null,
                                                tint = AmberCoral,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "سحب تلقائي: " + linkedForItem.joinToString("، ") { "${it.ingredientName} (${it.requiredQuantityPerUnit} ${it.unit})" },
                                                fontSize = 10.sp,
                                                color = AmberCoral,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "${String.format(Locale.US, "%.1f", item.basePrice)} ج/${item.unit}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { itemToEdit = item }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("تعديل الاسم والسعر", fontSize = 11.sp)
                                }

                                TextButton(onClick = { viewModel.archiveMenuItem(item.id, !item.isArchived) }) {
                                    Icon(
                                        imageVector = if (item.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(if (item.isArchived) "استعادة للنشط 🔄" else "أرشفة الصنف 📦", fontSize = 11.sp)
                                }

                                IconButton(
                                    onClick = { itemToDelete = item },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Categories List & CRUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أقسام وقوائم الطعام (${categories.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { showBatchCategoryDialog = true },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("⚡ تعديل أصناف الأقسام دفعة واحدة", fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = { showNewCategoryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ إضافة قسم", fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            MouseLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val itemsInCat = remember(cat.name, menuItems) {
                        menuItems.filter { it.category == cat.name }
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (cat.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = cat.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (!cat.isArchived) StatusSuccess.copy(alpha = 0.12f) else Color.Gray.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (!cat.isArchived) "مفعل ✓" else "معطل / غير نشط",
                                                fontSize = 10.sp,
                                                color = if (!cat.isArchived) StatusSuccess else Color.DarkGray,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "ترتيب العرض: ${cat.displayOrder} | عدد الأصناف التابعة: ${itemsInCat.size} صنف",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    val currentThemeId = categoryColors[cat.name]
                                    val catTheme = CategoryColorUtil.resolveCategoryStyle(cat.name, currentThemeId)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        Text("🎨 لون القسم بالكاشير:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = catTheme.bgColor,
                                            border = BorderStroke(1.dp, catTheme.borderColor)
                                        ) {
                                            Text(
                                                text = catTheme.nameAr,
                                                color = catTheme.priceBgColor,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    MouseLazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        items(CategoryColorUtil.PRESET_THEMES) { themeItem ->
                                            val isChosen = (currentThemeId ?: CategoryColorUtil.resolveCategoryStyle(cat.name, null).id) == themeItem.id
                                            Surface(
                                                onClick = { viewModel.setCategoryColor(cat.name, themeItem.id) },
                                                shape = CircleShape,
                                                color = themeItem.primaryColor,
                                                border = BorderStroke(if (isChosen) 2.5.dp else 1.dp, if (isChosen) Color.Black else Color(0x33000000)),
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                if (isChosen) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { categoryToEdit = cat }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = SeaTealPrimary)
                                    }
                                    IconButton(onClick = { categoryToDelete = cat }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StatusDanger)
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (itemsInCat.isNotEmpty()) "الأصناف: " + itemsInCat.take(4).joinToString("، ") { it.name } + if (itemsInCat.size > 4) "..." else "" else "لا توجد أصناف تابعة لهذا القسم حالياً",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { categoryForBatchEdit = cat }
                                ) {
                                    Text("⚡ تعديل أصناف القسم (${itemsInCat.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add or Edit Menu Item Dialog
    if (showNewItemDialog || itemToEdit != null) {
        val existing = itemToEdit
        val existingIngs = remember(existing, allIngredients) {
            if (existing != null) allIngredients.filter { it.menuItemId == existing.id } else emptyList()
        }
        val activeCategoriesList = categories.filter { !it.isArchived && it.name.isNotBlank() }.map { it.name.trim() }.distinct()
        MenuItemEditDialog(
            item = existing,
            categories = activeCategoriesList,
            availableMenuItems = menuItems,
            existingIngredients = existingIngs,
            allPrinters = allPrinters,
            onSave = { updatedItem, linkedIngredients ->
                val oldName = existing?.name
                viewModel.saveMenuItemWithIngredients(updatedItem, linkedIngredients, oldName = oldName)
                showNewItemDialog = false
                itemToEdit = null
            },
            onDismiss = {
                showNewItemDialog = false
                itemToEdit = null
            }
        )
    }

    // Delete Menu Item with Password Protection
    itemToDelete?.let { targetItem ->
        PasswordPromptDialog(
            title = "تأكيد حذف صنف: ${targetItem.name}",
            message = "حذف الصنف نهائياً يتطلب كلمة سر الإدارة لمنع الحذف بالخطأ. يمكنك أيضاً أرشفته بدلاً من حذفه.",
            onConfirm = { pass ->
                scope.launch {
                    val ok = viewModel.verifyManagerPassword(pass)
                    if (ok) {
                        viewModel.deleteMenuItem(targetItem.id)
                        itemToDelete = null
                    } else {
                        viewModel.userMessage.value = "كلمة مرور الإدارة غير صحيحة"
                    }
                }
            },
            onDismiss = { itemToDelete = null }
        )
    }

    // Add or Edit Category Dialog
    if (showNewCategoryDialog || categoryToEdit != null) {
        val existingCat = categoryToEdit
        val initialTheme = existingCat?.let { categoryColors[it.name] }
        CategoryEditDialog(
            category = existingCat,
            availableMenuItems = menuItems,
            initialThemeId = initialTheme,
            onSave = { updatedCat, assignedItemIds, chosenThemeId ->
                viewModel.saveCategory(updatedCat)
                viewModel.setCategoryColor(updatedCat.name, chosenThemeId)
                if (assignedItemIds.isNotEmpty()) {
                    viewModel.batchUpdateItemsCategory(assignedItemIds, updatedCat.name)
                }
                showNewCategoryDialog = false
                categoryToEdit = null
            },
            onDismiss = {
                showNewCategoryDialog = false
                categoryToEdit = null
            }
        )
    }

    // Batch Category Items Dialog (Requirement 8 & Batch Price Edit)
    if (showBatchCategoryDialog || categoryForBatchEdit != null) {
        val initialCategory = categoryForBatchEdit ?: categories.firstOrNull()
        BatchCategoryItemsDialog(
            categories = categories,
            initialCategory = initialCategory,
            menuItems = menuItems,
            onApply = { targetCat, itemIds, changedPrices ->
                viewModel.batchUpdateItemsCategory(itemIds, targetCat.name)
                if (changedPrices.isNotEmpty()) {
                    viewModel.batchUpdateItemPrices(changedPrices)
                }
                showBatchCategoryDialog = false
                categoryForBatchEdit = null
            },
            onDismiss = {
                showBatchCategoryDialog = false
                categoryForBatchEdit = null
            }
        )
    }

    if (showBatchPriceDialog) {
        com.example.ui.components.DailyPriceBatchEditorDialog(
            freshItems = menuItems,
            onSave = { updates ->
                viewModel.batchUpdateItemPrices(updates)
                showBatchPriceDialog = false
            },
            onDismiss = { showBatchPriceDialog = false }
        )
    }

    // Delete Category Dialog with Password Protection
    categoryToDelete?.let { targetCat ->
        PasswordPromptDialog(
            title = "تأكيد حذف قسم: ${targetCat.name}",
            message = "حذف قسم كامل من القائمة يتطلب كلمة سر الإدارة.",
            onConfirm = { pass ->
                scope.launch {
                    val ok = viewModel.verifyManagerPassword(pass)
                    if (ok) {
                        viewModel.deleteCategory(targetCat.id)
                        categoryToDelete = null
                    } else {
                        viewModel.userMessage.value = "كلمة مرور الإدارة غير صحيحة"
                    }
                }
            },
            onDismiss = { categoryToDelete = null }
        )
    }
}

@Composable
fun MenuItemEditDialog(
    item: MenuItem?,
    categories: List<String>,
    availableMenuItems: List<MenuItem> = emptyList(),
    existingIngredients: List<MenuItemIngredient> = emptyList(),
    allPrinters: List<com.example.printer.PrinterConfig> = emptyList(),
    initialCategory: String? = null,
    onSave: (MenuItem, List<MenuItemIngredient>) -> Unit,
    onDismiss: () -> Unit
) {
    val cleanCategories = remember(categories, item) {
        val base = categories.map { it.trim() }.filter { it.isNotBlank() }
        if (item != null && item.category.isNotBlank() && item.category.trim() !in base) {
            (listOf(item.category.trim()) + base).distinct()
        } else {
            base.distinct()
        }
    }
    var name by remember { mutableStateOf(item?.name ?: "") }
    var kitchenName by remember { mutableStateOf(item?.kitchenName ?: "") }
    var category by remember {
        mutableStateOf(
            item?.category?.trim()?.ifBlank { null }
                ?: initialCategory?.trim()?.ifBlank { null }
                ?: cleanCategories.firstOrNull()
                ?: "عام"
        )
    }
    var unit by remember { mutableStateOf(item?.unit ?: "كيلو") }
    var priceStr by remember { mutableStateOf(if (item != null) item.basePrice.toString() else "") }
    var costStr by remember { mutableStateOf(if (item != null) item.costPrice.toString() else "0") }
    var stockStr by remember { mutableStateOf(if (item != null) item.currentStock.toString() else "10") }
    var minStockStr by remember { mutableStateOf(if (item != null) item.minStockAlert.toString() else "3") }
    var supportsPrep by remember { mutableStateOf(item?.supportsPreparation ?: true) }
    var isPlateWeightEnabled by remember { mutableStateOf(item?.isPlateWeightEnabled ?: (item?.unit == "طبق")) }
    var plateWeightKgStr by remember { mutableStateOf(if (item != null && item.plateWeightKg > 0.0) item.plateWeightKg.toString() else "0.5") }
    var pricePerKgStr by remember { mutableStateOf(if (item != null && item.pricePerKg > 0.0) item.pricePerKg.toString() else "") }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Multi-printer linking state based on dynamically configured allPrinters
    val selectedPrinterIds = remember(item, allPrinters) {
        val raw = item?.targetPrinter ?: ""
        val parsed = raw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        mutableStateListOf<String>().apply {
            if (parsed.isNotEmpty()) {
                addAll(parsed)
            } else if (item == null && allPrinters.isNotEmpty()) {
                addAll(allPrinters.map { it.id.uppercase() })
            }
        }
    }

    // Linked items / deductions state (e.g. pulling a rice dish with each Rizo meal):
    val linkedItems = remember(existingIngredients) {
        mutableStateListOf<MenuItemIngredient>().apply {
            addAll(existingIngredients)
        }
    }
    var isAddingLinkedItem by remember { mutableStateOf(false) }
    var selectedLinkedCandidate by remember { mutableStateOf<MenuItem?>(null) }
    var linkedQtyStr by remember { mutableStateOf("1.0") }
    var isCandidateDropdownOpen by remember { mutableStateOf(false) }
    var editingIngredientIndex by remember { mutableStateOf<Int?>(null) }
    var editingIngredientQty by remember { mutableStateOf("1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "إضافة صنف جديد" else "تعديل الصنف: ${item.name}", fontWeight = FontWeight.Bold) },
        text = {
            MouseLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isLandscape) 220.dp else 450.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الصنف في الفاتورة (مثلاً: ساندوتش جمبري)") },
                        placeholder = { Text("يظهر في فاتورة الكاشير وحساب العميل") },
                        singleLine = true,
                        keyboardOptions = com.example.util.ArabicKeyboardUtil.text(),
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                            textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val trimmedItemName = name.trim()
                    val duplicateNameItems = remember(trimmedItemName, availableMenuItems, item?.id) {
                        if (trimmedItemName.isBlank()) emptyList()
                        else availableMenuItems.filter {
                            it.id != (item?.id ?: -1L) &&
                                it.name.trim().equals(trimmedItemName, ignoreCase = true)
                        }
                    }
                    if (duplicateNameItems.isNotEmpty()) {
                        val existingPricesDesc = duplicateNameItems.joinToString("، ") { dup ->
                            val pStr = if (dup.basePrice % 1.0 == 0.0) String.format(Locale.US, "%.0f", dup.basePrice)
                            else String.format(Locale.US, "%.1f", dup.basePrice)
                            "$pStr ج/${dup.unit}"
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SeaTealPrimary.copy(alpha = 0.10f),
                            border = BorderStroke(0.8.dp, SeaTealPrimary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text(
                                text = "ℹ️ يوجد صنف آخر بنفس الاسم ($existingPricesDesc). سيتم حفظه كصنف مستقل بسعره الخاص دون استبدال الآخر، ويمكنك تمييزه بوصف في اسم المطبخ أدناه.",
                                fontSize = 10.5.sp,
                                color = SeaNavyDeep,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = kitchenName,
                        onValueChange = { kitchenName = it },
                        label = { Text("اسم الصنف لأمر التشغيل / المطبخ (اختياري)") },
                        placeholder = { Text("مثلاً: سندوتش ٤٠ جرام جمبري مقلي") },
                        supportingText = {
                            Text(
                                text = "يُطبع في تذكرة أمر تشغيل المطبخ (إن تُرِك فارغاً يُطبع اسم الفاتورة تلقائياً)",
                                fontSize = 10.5.sp,
                                color = SeaTealPrimary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = com.example.util.ArabicKeyboardUtil.text(),
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                            textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(text = "القسم / القائمة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    if (cleanCategories.isNotEmpty()) {
                        MouseLazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(cleanCategories) { catName ->
                                FilterChip(
                                    selected = category == catName,
                                    onClick = { category = catName },
                                    label = { Text(catName, fontSize = 11.5.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(if (cleanCategories.isNotEmpty()) "القسم المختار (يمكن تعديله أو كتابة قسم جديد)" else "اسم القسم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("سعر البيع (ج.م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = costStr,
                            onValueChange = { costStr = it },
                            label = { Text("سعر التكلفة (ج.م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = stockStr,
                            onValueChange = { stockStr = it },
                            label = { Text("الرصيد الافتتاحي") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minStockStr,
                            onValueChange = { minStockStr = it },
                            label = { Text("حد تنبيه النفاد") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text(text = "وحدة القياس:", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("وجبة", "طبق", "قطعة", "كيلو", "ساندوتش").forEach { u ->
                            FilterChip(
                                selected = unit == u,
                                onClick = { unit = u },
                                label = { Text(u) }
                            )
                        }
                    }
                }

                if (unit == "طبق") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SeaTealPrimary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isPlateWeightEnabled = !isPlateWeightEnabled },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isPlateWeightEnabled,
                                        onCheckedChange = { isPlateWeightEnabled = it }
                                    )
                                    Text(
                                        text = "تحديد وزن الطبق وسعر الكيلو (لحساب تكلفة التجهيز لوزن الطبق)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SeaNavyDeep
                                    )
                                }

                                if (isPlateWeightEnabled) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = plateWeightKgStr,
                                            onValueChange = {
                                                plateWeightKgStr = it
                                                val w = it.toDoubleOrNull() ?: 0.0
                                                val kgP = pricePerKgStr.toDoubleOrNull() ?: 0.0
                                                if (w > 0 && kgP > 0 && priceStr.isBlank()) {
                                                    priceStr = String.format(Locale.US, "%.1f", w * kgP)
                                                }
                                            },
                                            label = { Text("وزن الطبق (كجم) *") },
                                            placeholder = { Text("مثال: 0.5") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = pricePerKgStr,
                                            onValueChange = {
                                                pricePerKgStr = it
                                                val kgP = it.toDoubleOrNull() ?: 0.0
                                                val w = plateWeightKgStr.toDoubleOrNull() ?: 0.0
                                                if (w > 0 && kgP > 0) {
                                                    priceStr = String.format(Locale.US, "%.1f", w * kgP)
                                                }
                                            },
                                            label = { Text("سعر الكيلو (ج.م) *") },
                                            placeholder = { Text("مثال: 200") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Text(
                                        text = "💡 سيتم احتساب سعر التجهيز والطهي بناءً على وزن الطبق (${plateWeightKgStr} كجم) بدلاً من احتسابه ككيلو كامل.",
                                        fontSize = 11.sp,
                                        color = SeaTealPrimary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { supportsPrep = !supportsPrep },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = supportsPrep, onCheckedChange = { supportsPrep = it })
                        Text(text = "يدعم خيارات وتكلفة التجهيز (شوي/قلي/سنجاري)", fontSize = 12.sp)
                    }
                }

                // 1. Dedicated Multi-Printer Linking Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    tint = SeaTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "ربط وتوجيه الطابعات لهذا الصنف:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = SeaTealPrimary
                                )
                            }


                            Text(
                                text = "توجيه أمر التشغيل للطابعات المحددة:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (allPrinters.isEmpty()) {
                                Text(
                                    text = "لم تقم بإضافة طابعات بعد في إدارة الطابعات. عند إضافة طابعاتك ستظهر هنا لتوجيه الأصناف إليها.",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            } else {
                                allPrinters.forEach { p ->
                                    val isChecked = selectedPrinterIds.contains(p.id.uppercase()) || selectedPrinterIds.contains("ALL")
                                    Surface(
                                        color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isChecked) SeaTealPrimary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) selectedPrinterIds.remove(p.id.uppercase())
                                                else selectedPrinterIds.add(p.id.uppercase())
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { chk ->
                                                    if (chk) selectedPrinterIds.add(p.id.uppercase())
                                                    else selectedPrinterIds.remove(p.id.uppercase())
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = SeaTealPrimary)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = p.displayName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp
                                                )
                                                Text(
                                                    text = "الغرض: ${when (p.role) { "CUSTOMER_INVOICE" -> "فاتورة زبون 👤"; "KITCHEN_ORDER" -> "أمر تشغيل مطبخ 👨‍🍳"; else -> "فاتورة وتشغيل 🔄" }} (${p.connectionType})",
                                                    fontSize = 9.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedPrinterIds.size == allPrinters.size,
                                        onClick = {
                                            selectedPrinterIds.clear()
                                            selectedPrinterIds.addAll(allPrinters.map { it.id.uppercase() })
                                        },
                                        label = { Text("تحديد كل الطابعات (${allPrinters.size})", fontSize = 10.sp) }
                                    )
                                    FilterChip(
                                        selected = selectedPrinterIds.isEmpty(),
                                        onClick = { selectedPrinterIds.clear() },
                                        label = { Text("إلغاء التحديد", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Dedicated Linked Items Section (Automatic stock pull on order e.g. Rizo pulls Rice)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = AmberCoral.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AmberCoral.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = AmberCoral,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "ربط صنف بصنف لسحب المخزون تلقائياً:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = AmberCoral
                                )
                            }

                            Text(
                                text = "مثال: كل أوردر (ريزو) يتم تلقائياً سحب (طبق أرز) أو خامات من المخزون مع كل طلب.",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Display current linked items
                            if (linkedItems.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "لا توجد أصناف مرتبطة حالياً بهذا الصنف (اختياري).",
                                        fontSize = 10.5.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for ((index, ing) in linkedItems.withIndex()) {
                                        val targetItem = availableMenuItems.firstOrNull { it.id == ing.ingredientItemId }
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, AmberCoral.copy(alpha = 0.35f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "• ${ing.ingredientName}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.5.sp
                                                    )
                                                    Text(
                                                        text = "سحب: ${ing.requiredQuantityPerUnit} ${ing.unit} لكل 1 طلب | الرصيد المتاح: ${targetItem?.currentStock ?: 0.0} ${ing.unit}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = {
                                                            editingIngredientIndex = index
                                                            editingIngredientQty = ing.requiredQuantityPerUnit.toString()
                                                        },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "تعديل الكمية",
                                                            tint = SeaTealPrimary,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { linkedItems.removeAt(index) },
                                                        modifier = Modifier.size(26.dp)
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

                            // Add new linked item UI
                            val candidateItems = remember(availableMenuItems, linkedItems.toList(), item) {
                                availableMenuItems.filter {
                                    it.id != (item?.id ?: 0L) && !linkedItems.any { l -> l.ingredientItemId == it.id }
                                }
                            }

                            if (candidateItems.isNotEmpty()) {
                                if (!isAddingLinkedItem) {
                                    OutlinedButton(
                                        onClick = {
                                            isAddingLinkedItem = true
                                            if (selectedLinkedCandidate == null || !candidateItems.contains(selectedLinkedCandidate)) {
                                                selectedLinkedCandidate = candidateItems.firstOrNull()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberCoral)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ ربط صنف مسحوب (مثل طبق أرز)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "اختر الصنف المطلوب سحبه تلقائياً:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            // Dropdown for selecting candidate
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                OutlinedButton(
                                                    onClick = { isCandidateDropdownOpen = true },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = selectedLinkedCandidate?.let { "${it.name} (${it.category}) - رصيد: ${it.currentStock} ${it.unit}" }
                                                                ?: "اضغط لاختيار الصنف...",
                                                            fontSize = 11.sp,
                                                            maxLines = 1
                                                        )
                                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                                    }
                                                }

                                                DropdownMenu(
                                                    expanded = isCandidateDropdownOpen,
                                                    onDismissRequest = { isCandidateDropdownOpen = false }
                                                ) {
                                                    candidateItems.forEach { cand ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Column {
                                                                    Text(cand.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                                    Text(
                                                                        "القسم: ${cand.category} | الرصيد: ${cand.currentStock} ${cand.unit}",
                                                                        fontSize = 10.sp,
                                                                        color = Color.Gray
                                                                    )
                                                                }
                                                            },
                                                            onClick = {
                                                                selectedLinkedCandidate = cand
                                                                isCandidateDropdownOpen = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            // Quantity with stepper buttons
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        val clean = linkedQtyStr.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                                                        val cur = clean.toDoubleOrNull() ?: 1.0
                                                        val next = (cur - 0.25).coerceAtLeast(0.05)
                                                        linkedQtyStr = if (next == next.toInt().toDouble()) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AmberCoral)
                                                }

                                                OutlinedTextField(
                                                    value = linkedQtyStr,
                                                    onValueChange = { linkedQtyStr = it },
                                                    label = { Text("الكمية المسحوبة لكل 1 طلب") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1.2f)
                                                )

                                                IconButton(
                                                    onClick = {
                                                        val clean = linkedQtyStr.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                                                        val cur = clean.toDoubleOrNull() ?: 1.0
                                                        val next = cur + 0.25
                                                        linkedQtyStr = if (next == next.toInt().toDouble()) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AmberCoral)
                                                }

                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    Text(
                                                        text = selectedLinkedCandidate?.unit ?: "وحدة",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                                                    )
                                                }
                                            }

                                            // Quick preset chips
                                            val presets = listOf("0.25", "0.5", "0.75", "1.0", "1.5", "2.0")
                                            MouseLazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(presets) { pStr ->
                                                    FilterChip(
                                                        selected = linkedQtyStr.trim() == pStr,
                                                        onClick = { linkedQtyStr = pStr },
                                                        label = { Text("$pStr ${selectedLinkedCandidate?.unit ?: ""}", fontSize = 10.5.sp) }
                                                    )
                                                }
                                            }

                                            // Action buttons
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(onClick = { isAddingLinkedItem = false }) {
                                                    Text("إلغاء", fontSize = 11.sp)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Button(
                                                    onClick = {
                                                        val cand = selectedLinkedCandidate
                                                        val clean = linkedQtyStr.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                                                        val q = clean.toDoubleOrNull() ?: 1.0
                                                        if (cand != null && q > 0) {
                                                            linkedItems.add(
                                                                MenuItemIngredient(
                                                                    id = 0L,
                                                                    menuItemId = item?.id ?: 0L,
                                                                    ingredientItemId = cand.id,
                                                                    ingredientName = cand.name,
                                                                    requiredQuantityPerUnit = q,
                                                                    unit = cand.unit,
                                                                    notes = "سحب تلقائي مع بيع ${name.ifBlank { "الصنف" }}"
                                                                )
                                                            )
                                                            isAddingLinkedItem = false
                                                            linkedQtyStr = "1.0"
                                                            selectedLinkedCandidate = null
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("تأكيد الربط", fontSize = 11.sp)
                                                }
                                            }

                                        }
                                    }
                                }
                            } else if (availableMenuItems.isEmpty()) {
                                Text(
                                    text = "لا توجد أصناف أخرى مسجلة بالقائمة لربطها حالياً.",
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
                    val p = priceStr.toDoubleOrNull() ?: 0.0
                    val c = costStr.toDoubleOrNull() ?: 0.0
                    val st = stockStr.toDoubleOrNull() ?: 0.0
                    val mst = minStockStr.toDoubleOrNull() ?: 3.0
                    if (name.isNotBlank() && p > 0) {
                        val finalPrinterString = if (selectedPrinterIds.isEmpty()) "ALL" else selectedPrinterIds.distinct().joinToString(",")

                        val savedItem = MenuItem(
                            id = item?.id ?: 0L,
                            cloudId = item?.cloudId ?: "",
                            name = name.trim(),
                            kitchenName = kitchenName.trim(),
                            category = category.trim().ifBlank { item?.category?.trim() ?: "عام" },
                            unit = unit,
                            basePrice = p,
                            costPrice = c,
                            supportsPreparation = supportsPrep,
                            currentStock = st,
                            minStockAlert = mst,
                            targetPrinter = finalPrinterString,
                            isAvailable = true,
                            isArchived = item?.isArchived ?: false,
                            displayOrder = item?.displayOrder ?: 0,
                            isPlateWeightEnabled = if (unit == "طبق") isPlateWeightEnabled else false,
                            plateWeightKg = plateWeightKgStr.toDoubleOrNull() ?: 0.5,
                            pricePerKg = pricePerKgStr.toDoubleOrNull() ?: 0.0,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                        onSave(savedItem, linkedItems.toList())
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

    editingIngredientIndex?.let { idx ->
        if (idx in linkedItems.indices) {
            val ing = linkedItems[idx]
            AlertDialog(
                onDismissRequest = { editingIngredientIndex = null },
                title = { Text("تعديل كمية السحب لـ ${ing.ingredientName}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("حدد كمية السحب لهذا المكون:", fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val clean = editingIngredientQty.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                                    val cur = clean.toDoubleOrNull() ?: 1.0
                                    val next = (cur - 0.25).coerceAtLeast(0.05)
                                    editingIngredientQty = if (next == next.toInt().toDouble()) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                                }
                            ) {
                                Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AmberCoral)
                            }
                            OutlinedTextField(
                                value = editingIngredientQty,
                                onValueChange = { editingIngredientQty = it },
                                label = { Text("الكمية (${ing.unit})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clean = editingIngredientQty.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                                    val cur = clean.toDoubleOrNull() ?: 1.0
                                    val next = cur + 0.25
                                    editingIngredientQty = if (next == next.toInt().toDouble()) next.toInt().toString() else String.format(Locale.US, "%.2f", next)
                                }
                            ) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AmberCoral)
                            }
                        }
                        val presets = listOf("0.25", "0.5", "0.75", "1.0", "1.5", "2.0")
                        MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(presets) { pStr ->
                                FilterChip(
                                    selected = editingIngredientQty.trim() == pStr,
                                    onClick = { editingIngredientQty = pStr },
                                    label = { Text("$pStr ${ing.unit}", fontSize = 10.5.sp) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clean = editingIngredientQty.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                            val q = clean.toDoubleOrNull() ?: 1.0
                            if (q > 0) {
                                linkedItems[idx] = ing.copy(requiredQuantityPerUnit = q)
                                editingIngredientIndex = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberCoral)
                    ) {
                        Text("حفظ التعديل")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingIngredientIndex = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun CategoryEditDialog(
    category: MenuCategory?,
    availableMenuItems: List<MenuItem> = emptyList(),
    initialThemeId: String? = null,
    onSave: (MenuCategory, List<Long>, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var orderStr by remember { mutableStateOf(if (category != null) category.displayOrder.toString() else "1") }
    var isActive by remember { mutableStateOf(category?.isArchived != true) }
    var selectedThemeId by remember {
        mutableStateOf(
            initialThemeId ?: (category?.let { CategoryColorUtil.resolveCategoryStyle(it.name, null).id } ?: "TEAL")
        )
    }

    val currentCatName = category?.name ?: ""
    val initialAssigned = remember(category, availableMenuItems) {
        if (category != null) {
            availableMenuItems.filter { it.category == currentCatName }.map { it.id }.toSet()
        } else emptySet()
    }
    val selectedItemIds = remember { mutableStateListOf<Long>().apply { addAll(initialAssigned) } }
    var itemSearchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(itemSearchQuery, availableMenuItems) {
        if (itemSearchQuery.isBlank()) availableMenuItems
        else availableMenuItems.filter { it.name.contains(itemSearchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (category == null) "إضافة قسم جديد للقائمة" else "تعديل قسم: ${category.name}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            MouseLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم القسم (مثلاً: جمبريات)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = orderStr,
                        onValueChange = { orderStr = it },
                        label = { Text("ترتيب الظهور في القائمة (رقم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Color Theme selection
                item {
                    val activeTheme = CategoryColorUtil.PRESET_THEMES.find { it.id == selectedThemeId } ?: CategoryColorUtil.PRESET_THEMES[0]
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎨 لون القسم وأصنافه في الكاشير:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = activeTheme.bgColor,
                                border = BorderStroke(1.dp, activeTheme.borderColor)
                            ) {
                                Text(
                                    text = activeTheme.nameAr,
                                    color = activeTheme.priceBgColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        MouseLazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            items(CategoryColorUtil.PRESET_THEMES) { t ->
                                val isCurrent = selectedThemeId == t.id
                                Surface(
                                    onClick = { selectedThemeId = t.id },
                                    shape = CircleShape,
                                    color = t.primaryColor,
                                    modifier = Modifier.size(34.dp),
                                    border = BorderStroke(if (isCurrent) 2.5.dp else 1.dp, if (isCurrent) Color.Black else Color(0x33000000))
                                ) {
                                    if (isCurrent) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Active / Inactive switch (Requirement: تفعيل القسم عند الإضافة نعم ولا)
                item {
                    Text(text = "تفعيل هذا القسم في القائمة ونقطة البيع:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isActive,
                            onClick = { isActive = true },
                            label = { Text("مفعل نشط ✓ (يظهر بالكاشير)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isActive,
                            onClick = { isActive = false },
                            label = { Text("معطل ⏸ (مخفي مؤقتاً)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Batch assign items section
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "تحديد أصناف تابعة لهذا القسم دفعة واحدة (${selectedItemIds.size} صنف محدد):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "اختر الأصناف التي ترغب بضمها ونقلها لهذا القسم مباشرة:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = itemSearchQuery,
                        onValueChange = { itemSearchQuery = it },
                        placeholder = { Text("بحث عن صنف لتحديده...", fontSize = 11.5.sp, lineHeight = 18.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, lineHeight = 18.sp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }

                items(filteredItems) { item ->
                    val isChecked = selectedItemIds.contains(item.id)
                    Surface(
                        color = if (isChecked) SeaTealPrimary.copy(alpha = 0.1f) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedItemIds.remove(item.id)
                                else selectedItemIds.add(item.id)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { chk ->
                                    if (chk == true) selectedItemIds.add(item.id)
                                    else selectedItemIds.remove(item.id)
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(text = "القسم الحالي: ${item.category} | السعر: ${item.basePrice} ج", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ord = orderStr.toIntOrNull() ?: 1
                    if (name.isNotBlank()) {
                        onSave(
                            MenuCategory(
                                id = category?.id ?: 0L,
                                name = name.trim(),
                                displayOrder = ord,
                                isArchived = !isActive
                            ),
                            selectedItemIds.toList(),
                            selectedThemeId
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
fun BatchCategoryItemsDialog(
    categories: List<MenuCategory>,
    initialCategory: MenuCategory?,
    menuItems: List<MenuItem>,
    onApply: (targetCategory: MenuCategory, itemIds: List<Long>, changedPrices: Map<Long, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCat by remember { mutableStateOf(initialCategory ?: categories.firstOrNull()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableIntStateOf(0) } // 0: أصناف هذا القسم, 1: كل الأصناف
    var batchPriceInput by remember { mutableStateOf("") }

    val currentCatName = selectedCat?.name ?: ""
    val selectedItemIds = remember(selectedCat) {
        mutableStateListOf<Long>().apply {
            addAll(menuItems.filter { it.category == currentCatName }.map { it.id })
        }
    }

    val priceMap = remember(menuItems) {
        mutableStateMapOf<Long, String>().apply {
            menuItems.forEach { put(it.id, it.basePrice.toString()) }
        }
    }

    val displayedItems = remember(menuItems, searchQuery, filterMode, currentCatName) {
        menuItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterMode) {
                0 -> item.category == currentCatName || selectedItemIds.contains(item.id)
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = SeaTealPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("تعديل وتوزيع الأصناف والأسعار دفعة واحدة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Text(
                    text = "حدد القسم المطلوب، والأصناف التابعة له، ويمكنك تعديل أسعارها دفعة واحدة أو بشكل فردي:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text("القسم المستهدف للتعديل:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                MouseLazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCat?.id == cat.id,
                            onClick = { selectedCat = cat },
                            label = { Text(cat.name, fontSize = 11.sp, fontWeight = if (selectedCat?.id == cat.id) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Unified Batch Price Box (Solves Requirement 1: مربع أعدل منه السعر دفعة واحدة)
                Surface(
                    color = SeaTealPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "⚡ تعديل سعر الأصناف المحددة (${selectedItemIds.size} صنف):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = SeaTealPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = batchPriceInput,
                                onValueChange = { batchPriceInput = it },
                                label = { Text("السعر الجديد (ج.م)") },
                                placeholder = { Text("مثال: 150") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val clean = batchPriceInput.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                                    val p = clean.toDoubleOrNull()
                                    if (p != null && p >= 0) {
                                        selectedItemIds.forEach { id ->
                                            priceMap[id] = clean
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                shape = RoundedCornerShape(6.dp),
                                enabled = selectedItemIds.isNotEmpty() && batchPriceInput.isNotBlank()
                            ) {
                                Text("تطبيق السعر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث عن صنف...", fontSize = 12.sp, lineHeight = 20.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 20.sp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 52.dp)
                    )
                    FilterChip(
                        selected = filterMode == 1,
                        onClick = { filterMode = if (filterMode == 0) 1 else 0 },
                        label = { Text(if (filterMode == 1) "كل الأصناف" else "أصناف القسم", fontSize = 10.5.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الأصناف المحددة: ${selectedItemIds.size} صنف",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SeaTealPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                displayedItems.forEach {
                                    if (!selectedItemIds.contains(it.id)) selectedItemIds.add(it.id)
                                }
                            }
                        ) {
                            Text("تحديد الكل", fontSize = 10.5.sp)
                        }
                        TextButton(
                            onClick = {
                                displayedItems.forEach { selectedItemIds.remove(it.id) }
                            }
                        ) {
                            Text("إلغاء التحديد", fontSize = 10.5.sp)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                MouseLazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(displayedItems, key = { it.id }) { item ->
                        val isChecked = selectedItemIds.contains(item.id)
                        Surface(
                            color = if (isChecked) SeaTealPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isChecked) SeaTealPrimary.copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedItemIds.remove(item.id)
                                    else selectedItemIds.add(item.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { chk ->
                                        if (chk == true) selectedItemIds.add(item.id)
                                        else selectedItemIds.remove(item.id)
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        text = "القسم: ${item.category} | الرصيد: ${item.currentStock} ${item.unit}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                OutlinedTextField(
                                    value = priceMap[item.id] ?: "",
                                    onValueChange = { priceMap[item.id] = it },
                                    label = { Text("السعر (ج)", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.width(90.dp)
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
                    val target = selectedCat
                    if (target != null && selectedItemIds.isNotEmpty()) {
                        val changedPrices = mutableMapOf<Long, Double>()
                        for ((id, str) in priceMap) {
                            val clean = str.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                            clean.toDoubleOrNull()?.let { p ->
                                val orig = menuItems.firstOrNull { it.id == id }?.basePrice ?: -1.0
                                if (p != orig) {
                                    changedPrices[id] = p
                                }
                            }
                        }
                        onApply(target, selectedItemIds.toList(), changedPrices)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("تطبيق وحفظ التعديلات (تم)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
