package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MenuItem
import com.example.data.model.OrderItem
import com.example.data.model.OrderWithItems
import com.example.data.model.PreparationType
import com.example.printer.ReceiptBitmapRenderer
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun OrderItemsEditorDialog(
    orderWithItems: OrderWithItems,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    // Observe live order state from database
    val liveOrderState by viewModel.getOrderByIdFlow(orderWithItems.order.id)
        .collectAsStateWithLifecycle(initialValue = orderWithItems)

    val currentOrder = liveOrderState?.order ?: orderWithItems.order
    val currentItems = liveOrderState?.items ?: orderWithItems.items

    val allMenuItems by viewModel.menuItems.collectAsStateWithLifecycle(emptyList())
    val allCategories by viewModel.categories.collectAsStateWithLifecycle(emptyList())
    val allPreparations by viewModel.preparations.collectAsStateWithLifecycle(emptyList())
    val drivers by viewModel.drivers.collectAsStateWithLifecycle(emptyList())
    val deliveryAreas by viewModel.deliveryAreas.collectAsStateWithLifecycle(emptyList())

    var selectedEditorTab by remember { mutableStateOf(0) } // 0: الأصناف والتجهيزات, 1: بيانات العميل والتوصيل, 2: سداد الحساب

    // Customer & delivery states initialized from currentOrder
    var editOrderType by remember(currentOrder.id, currentOrder.orderType) { mutableStateOf(currentOrder.orderType) }
    var editCustName by remember(currentOrder.id, currentOrder.customerName) { mutableStateOf(currentOrder.customerName) }
    var editCustPhone by remember(currentOrder.id, currentOrder.customerPhone) { mutableStateOf(currentOrder.customerPhone) }
    var editCustSecPhone by remember(currentOrder.id, currentOrder.customerSecondaryPhone) { mutableStateOf(currentOrder.customerSecondaryPhone ?: "") }
    var editArea by remember(currentOrder.id, currentOrder.customerArea) { mutableStateOf(currentOrder.customerArea ?: "") }
    var editAddress by remember(currentOrder.id, currentOrder.deliveryAddressFull) { mutableStateOf(currentOrder.deliveryAddressFull ?: "") }
    var editFeeStr by remember(currentOrder.id, currentOrder.deliveryFee) { mutableStateOf(String.format(Locale.US, "%.0f", currentOrder.deliveryFee)) }
    var editDriverId by remember(currentOrder.id, currentOrder.driverId) { mutableStateOf(currentOrder.driverId) }
    var editDriverName by remember(currentOrder.id, currentOrder.driverName) { mutableStateOf(currentOrder.driverName) }
    var editNotes by remember(currentOrder.id, currentOrder.notes) { mutableStateOf(currentOrder.notes) }

    // Payment states initialized from currentOrder
    var editPaidStr by remember(currentOrder.id, currentOrder.paidAmount) {
        mutableStateOf(if (currentOrder.paidAmount > 0.0) String.format(Locale.US, "%.1f", currentOrder.paidAmount) else "")
    }
    var editIsPaid by remember(currentOrder.id, currentOrder.isPaid) {
        mutableStateOf(currentOrder.isPaid || (currentOrder.paidAmount >= currentOrder.totalAmount && currentOrder.totalAmount > 0))
    }

    var editingItem by remember { mutableStateOf<OrderItem?>(null) }
    var itemForCustomQty by remember { mutableStateOf<OrderItem?>(null) }
    var showAddFromMenuDialog by remember { mutableStateOf(false) }
    var showAddAdHocDialog by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "✏️ تعديل كامل لأوردر #${currentOrder.orderNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SeaNavyDeep
                    )
                    Text(
                        text = "تعديل الأصناف، العميل، التوصيل وسداد الحساب كأنك تطلبه من جديد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                // Tab Selection Row
                TabRow(
                    selectedTabIndex = selectedEditorTab,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Tab(
                        selected = selectedEditorTab == 0,
                        onClick = { selectedEditorTab = 0 },
                        text = { Text("الأصناف (${currentItems.size}) 🍲", fontSize = 11.sp, fontWeight = if (selectedEditorTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedEditorTab == 1,
                        onClick = { selectedEditorTab = 1 },
                        text = { Text("العميل والتوصيل 🛵", fontSize = 11.sp, fontWeight = if (selectedEditorTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedEditorTab == 2,
                        onClick = { selectedEditorTab = 2 },
                        text = { Text("سداد الحساب 💵", fontSize = 11.sp, fontWeight = if (selectedEditorTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                if (selectedEditorTab == 0) {
                    // TAB 0: ORDER ITEMS
                    // Action Buttons to Add Items
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    Button(
                        onClick = { showAddFromMenuDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                        modifier = Modifier.weight(1.2f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("➕ إضافة من المنيو", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showAddAdHocDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SeaNavyDeep),
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("صنف استثنائي حر", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Items List
                if (currentItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد أصناف في هذا الأوردر حالياً.\nاضغط على 'إضافة من المنيو' لإضافة أصناف.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    MouseLazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (isLandscape) 150.dp else 290.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentItems) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                                            Text(
                                                text = item.itemName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            )
                                            val unitAndPrep = item.unitPrice + item.preparationPrice
                                            Text(
                                                text = "${ReceiptBitmapRenderer.formatQuantity(item.quantity)} ${item.unit} × ${String.format(Locale.US, "%.1f", unitAndPrep)} ج.م",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (!item.preparationName.isNullOrBlank()) {
                                                Text(
                                                    text = "• طريقة التجهيز: ${item.preparationName}" +
                                                            if (item.preparationPrice > 0) " (+${String.format(Locale.US, "%.1f", item.preparationPrice)} ج)" else "",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFB91C1C),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            if (item.itemNotes.isNotBlank()) {
                                                Text(
                                                    text = "• ملاحظات: ${item.itemNotes}",
                                                    fontSize = 10.5.sp,
                                                    color = AmberCoral
                                                )
                                            }
                                            if (item.quantity > item.printedKitchenQuantity) {
                                                val delta = item.quantity - item.printedKitchenQuantity
                                                val isNewlyAdded = (item.id <= 0L)
                                                val unprintedText = if (item.printedKitchenQuantity > 0.0001) {
                                                    "• زيادة تشغيل غير مطبوعة للمطبخ: +${ReceiptBitmapRenderer.formatQuantity(delta)} ${item.unit}"
                                                } else if (isNewlyAdded) {
                                                    "• صنف جديد لم يُرسل للمطبخ بعد (${ReceiptBitmapRenderer.formatQuantity(item.quantity)} ${item.unit})"
                                                } else null
                                                if (unprintedText != null) {
                                                    Text(
                                                        text = unprintedText,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFD97706)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", item.totalPrice)} ج.م",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = SeaNavyDeep
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Quick Quantity Step Buttons
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val step = if (item.unit == "كيلو") 0.25 else 1.0
                                            IconButton(
                                                onClick = {
                                                    val newQ = (item.quantity - step).coerceAtLeast(0.0)
                                                    if (newQ <= 0) {
                                                        viewModel.deleteOrderItemFromOrder(currentOrder.id, item.id)
                                                    } else {
                                                        val updatedTotal = (item.unitPrice + item.preparationPrice) * newQ
                                                        viewModel.updateOrderItem(currentOrder.id, item.copy(quantity = newQ, totalPrice = updatedTotal))
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Remove, contentDescription = "تقليل الكمية", modifier = Modifier.size(15.dp))
                                            }

                                            Surface(
                                                onClick = { itemForCustomQty = item },
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFF1F5F9),
                                                border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.5f)),
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = ReceiptBitmapRenderer.formatQuantity(item.quantity),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = SeaNavyDeep
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "تعديل الكمية بالكيبورد",
                                                        tint = SeaTealPrimary,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    val newQ = item.quantity + step
                                                    val updatedTotal = (item.unitPrice + item.preparationPrice) * newQ
                                                    viewModel.updateOrderItem(currentOrder.id, item.copy(quantity = newQ, totalPrice = updatedTotal))
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = "زيادة الكمية", modifier = Modifier.size(15.dp))
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Button(
                                                onClick = { editingItem = item },
                                                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("تعديل التجهيز والوزن", fontSize = 10.5.sp)
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteOrderItemFromOrder(currentOrder.id, item.id)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الصنف", tint = StatusDanger, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                } // End of if (selectedEditorTab == 0)
                else if (selectedEditorTab == 1) {
                    // TAB 1: CUSTOMER & DELIVERY DETAILS
                    MouseLazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (isLandscape) 180.dp else 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            // Order Type Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = editOrderType == "DELIVERY",
                                    onClick = { editOrderType = "DELIVERY" },
                                    label = { Text("🛵 دليفري توصيل", fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = editOrderType != "DELIVERY",
                                    onClick = { editOrderType = "TAKEAWAY" },
                                    label = { Text("🏪 حجز واستلام (تيك أواي)", fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = editCustName,
                                onValueChange = { editCustName = it },
                                label = { Text("اسم العميل") },
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
                                    value = editCustPhone,
                                    onValueChange = { editCustPhone = it },
                                    label = { Text("رقم الهاتف الأساسي") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = editCustSecPhone,
                                    onValueChange = { editCustSecPhone = it },
                                    label = { Text("هاتف إضافي (اختياري)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (editOrderType == "DELIVERY") {
                            item {
                                Text("منطقة التوصيل السريعة:", fontSize = 11.sp, color = Color.Gray)
                                MouseLazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(deliveryAreas) { areaItem ->
                                        FilterChip(
                                            selected = editArea == areaItem.name,
                                            onClick = {
                                                editArea = areaItem.name
                                                editFeeStr = String.format(Locale.US, "%.0f", areaItem.deliveryFee)
                                            },
                                            label = { Text("${areaItem.name} (${String.format(Locale.US, "%.0f", areaItem.deliveryFee)} ج)") }
                                        )
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editArea,
                                        onValueChange = { editArea = it },
                                        label = { Text("المنطقة / الحي") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    OutlinedTextField(
                                        value = editFeeStr,
                                        onValueChange = { editFeeStr = it },
                                        label = { Text("رسوم التوصيل (ج.م)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }
                            }

                            item {
                                OutlinedTextField(
                                    value = editAddress,
                                    onValueChange = { editAddress = it },
                                    label = { Text("تفاصيل العنوان (الشارع، العمارة، الشقة)") },
                                    singleLine = false,
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item {
                                Text("تعيين كابتن التوصيل (الطيار):", fontSize = 11.sp, color = Color.Gray)
                                MouseLazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    item {
                                        FilterChip(
                                            selected = editDriverId == null || editDriverId == 0L,
                                            onClick = {
                                                editDriverId = null
                                                editDriverName = null
                                            },
                                            label = { Text("بدون كابتن (معلق)") }
                                        )
                                    }
                                    items(drivers) { drv ->
                                        FilterChip(
                                            selected = editDriverId == drv.id,
                                            onClick = {
                                                editDriverId = drv.id
                                                editDriverName = drv.name
                                            },
                                            label = { Text(drv.name) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = editNotes,
                                onValueChange = { editNotes = it },
                                label = { Text("ملاحظات الأوردر") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Button(
                                onClick = {
                                    val f = editFeeStr.toDoubleOrNull() ?: currentOrder.deliveryFee
                                    viewModel.updateOrderCustomerDetails(
                                        orderId = currentOrder.id,
                                        orderType = editOrderType,
                                        driverId = if (editOrderType == "DELIVERY") editDriverId else null,
                                        driverName = if (editOrderType == "DELIVERY") editDriverName else null,
                                        name = editCustName.trim().ifBlank { currentOrder.customerName },
                                        phone = editCustPhone.trim().ifBlank { currentOrder.customerPhone },
                                        secondaryPhone = editCustSecPhone.trim().ifBlank { null },
                                        area = editArea.trim().ifBlank { null },
                                        address = editAddress.trim().ifBlank { null },
                                        fee = f,
                                        driverCommission = currentOrder.driverCommission,
                                        notes = editNotes.trim()
                                    )
                                    viewModel.userMessage.value = "تم تحديث بيانات العميل والتوصيل بنجاح ✓"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SeaNavyDeep),
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ بيانات العميل فورياً", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // TAB 2: PAYMENT & SETTLEMENT
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (isLandscape) 180.dp else 360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val paidVal = editPaidStr.toDoubleOrNull() ?: 0.0
                        val total = currentOrder.totalAmount
                        val change = if (paidVal > total) (paidVal - total) else 0.0
                        val remaining = if (paidVal < total && !editIsPaid) (total - paidVal) else 0.0

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SeaNavyDeep.copy(alpha = 0.07f),
                            border = BorderStroke(1.dp, SeaNavyDeep.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("الإجمالي المطلوب سداده:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${String.format(Locale.US, "%.1f", total)} ج.م", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SeaNavyDeep)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (editIsPaid || (paidVal >= total && total > 0)) StatusSuccess else StatusWarning
                                ) {
                                    Text(
                                        text = if (editIsPaid || (paidVal >= total && total > 0)) "✓ خالص بالكامل" else "معلق / غير خالص",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                editPaidStr = String.format(Locale.US, "%.1f", total)
                                editIsPaid = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✅ الأوردر خالص بالكامل (${String.format(Locale.US, "%.1f", total)} ج)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = editPaidStr,
                            onValueChange = { input ->
                                editPaidStr = input
                                val p = input.toDoubleOrNull() ?: 0.0
                                if (p >= total && total > 0) editIsPaid = true
                            },
                            label = { Text("المبلغ المدفوع من العميل (ج.م)") },
                            placeholder = { Text("مثال: 500 أو 200") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        MouseLazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val suggestions = listOf(total, 50.0, 100.0, 150.0, 200.0, 300.0, 400.0, 500.0, 1000.0).distinct().sorted()
                            items(suggestions) { amt ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (paidVal == amt) SeaTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (paidVal == amt) SeaTealPrimary else Color.LightGray),
                                    modifier = Modifier.clickable {
                                        editPaidStr = String.format(Locale.US, "%.1f", amt)
                                        if (amt >= total && total > 0) editIsPaid = true
                                    }
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.0f", amt)} ج",
                                        color = if (paidVal == amt) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (paidVal >= total && total > 0) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, if (paidVal >= total && total > 0) Color(0xFFBBF7D0) else Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (paidVal >= total && total > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("الباقي المستحق للعميل:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF166534))
                                        Text("${String.format(Locale.US, "%.1f", change)} ج.م", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF166534))
                                    }
                                } else if (paidVal > 0.0 && paidVal < total) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("المتبقي على العميل (دين/آجل):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB45309))
                                        Text("${String.format(Locale.US, "%.1f", remaining)} ج.م", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFFB45309))
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { editIsPaid = !editIsPaid }
                                ) {
                                    Checkbox(checked = editIsPaid, onCheckedChange = { editIsPaid = it })
                                    Text("تحديد الفاتورة كـ خالصة بالكامل", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val p = editPaidStr.toDoubleOrNull() ?: 0.0
                                val c = if (p > total) p - total else 0.0
                                viewModel.updateOrderPayment(currentOrder.id, p, c, editIsPaid)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SeaNavyDeep),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("حفظ بيانات الحساب والسداد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Recalculated Totals Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المجموع الفرعي:", fontSize = 11.sp)
                            Text("${String.format(Locale.US, "%.1f", currentOrder.subtotal)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (currentOrder.deliveryFee > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("رسوم التوصيل:", fontSize = 11.sp)
                                Text("+${String.format(Locale.US, "%.0f", currentOrder.deliveryFee)} ج.م", fontSize = 11.sp)
                            }
                        }
                        if (currentOrder.discount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الخصم:", fontSize = 11.sp, color = StatusDanger)
                                Text("-${currentOrder.discount} ج.م", fontSize = 11.sp, color = StatusDanger, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الإجمالي النهائي المطلوب:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${String.format(Locale.US, "%.1f", currentOrder.totalAmount)} ج.م", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SeaTealPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val hasUnprintedKitchenItems = currentItems.any { it.quantity > it.printedKitchenQuantity }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (hasUnprintedKitchenItems) {
                    Button(
                        onClick = {
                            viewModel.printKitchenAdditions(currentOrder.id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إرسال الزيادة للمطبخ 🍳", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.printReceipt(currentOrder.id, "MAIN", "طباعة الفاتورة بعد التعديل")
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SeaNavyDeep),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("طباعة الفاتورة المعدلة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        // Automatically save any updated customer / delivery or payment details
                        val f = editFeeStr.toDoubleOrNull() ?: currentOrder.deliveryFee
                        viewModel.updateOrderCustomerDetails(
                            orderId = currentOrder.id,
                            orderType = editOrderType,
                            driverId = if (editOrderType == "DELIVERY") editDriverId else null,
                            driverName = if (editOrderType == "DELIVERY") editDriverName else null,
                            name = editCustName.trim().ifBlank { currentOrder.customerName },
                            phone = editCustPhone.trim().ifBlank { currentOrder.customerPhone },
                            secondaryPhone = editCustSecPhone.trim().ifBlank { null },
                            area = editArea.trim().ifBlank { null },
                            address = editAddress.trim().ifBlank { null },
                            fee = f,
                            driverCommission = currentOrder.driverCommission,
                            notes = editNotes.trim()
                        )
                        val p = editPaidStr.toDoubleOrNull() ?: currentOrder.paidAmount
                        val c = if (p > currentOrder.totalAmount) p - currentOrder.totalAmount else 0.0
                        viewModel.updateOrderPayment(currentOrder.id, p, c, editIsPaid)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("تم وحفظ الكل")
                }
            }
        },
        dismissButton = {}
    )

    // 1. Edit Existing Item Dialog (Name, Weight/Qty, Unit Price, Cooking Preparation, Notes)
    editingItem?.let { targetItem ->
        var editName by remember { mutableStateOf(targetItem.itemName) }
        var editQtyStr by remember { mutableStateOf(targetItem.quantity.toString()) }
        var editUnit by remember { mutableStateOf(targetItem.unit) }
        var editPriceStr by remember { mutableStateOf(targetItem.unitPrice.toString()) }
        var selectedPrep by remember {
            mutableStateOf(allPreparations.find { it.name == targetItem.preparationName })
        }
        var editPrepPriceStr by remember {
            mutableStateOf(targetItem.preparationPrice.toString())
        }
        var editNotes by remember { mutableStateOf(targetItem.itemNotes) }

        val q = editQtyStr.toDoubleOrNull() ?: targetItem.quantity
        val p = editPriceStr.toDoubleOrNull() ?: targetItem.unitPrice
        val pp = editPrepPriceStr.toDoubleOrNull() ?: targetItem.preparationPrice
        val liveTotal = (p + pp) * q

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = {
                Text(
                    text = "تعديل الصنف والتجهيز والوزن",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                MouseLazyColumn(
                    modifier = Modifier.fillMaxWidth(),
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
                                modifier = Modifier.weight(1.3f)
                            )

                            OutlinedTextField(
                                value = editPriceStr,
                                onValueChange = { editPriceStr = it },
                                label = { Text("سعر الوحدة (ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Cooking Preparation Selection
                    item {
                        Text(
                            text = "طريقة التجهيز والطهي (اختيار أو تغيير):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SeaNavyDeep
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        MouseLazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedPrep == null && targetItem.preparationName == null,
                                    onClick = {
                                        selectedPrep = null
                                        editPrepPriceStr = "0.0"
                                    },
                                    label = { Text("بدون تجهيز إضافي", fontSize = 10.5.sp) }
                                )
                            }
                            items(allPreparations) { prep ->
                                val isSelected = selectedPrep?.id == prep.id || (selectedPrep == null && targetItem.preparationName == prep.name)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedPrep = prep
                                        editPrepPriceStr = prep.additionalPricePerUnit.toString()
                                    },
                                    label = {
                                        Text(
                                            "${prep.name} (+${prep.additionalPricePerUnit} ج)",
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = editPrepPriceStr,
                            onValueChange = { editPrepPriceStr = it },
                            label = { Text("سعر التجهيز المضاف للوحدة (ج.م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text("ملاحظات خاصة (توابل، تسوية...)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Live Total Preview
                    item {
                        Surface(
                            color = SeaTealPrimary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("إجمالي الصنف بعد التعديل:", fontSize = 12.sp)
                                Text("${String.format(Locale.US, "%.1f", liveTotal)} ج.م", fontWeight = FontWeight.Bold, color = SeaTealPrimary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalQ = editQtyStr.toDoubleOrNull() ?: targetItem.quantity
                        val finalP = editPriceStr.toDoubleOrNull() ?: targetItem.unitPrice
                        val finalPP = editPrepPriceStr.toDoubleOrNull() ?: (selectedPrep?.additionalPricePerUnit ?: 0.0)
                        val prepName = selectedPrep?.name ?: targetItem.preparationName

                        val updated = targetItem.copy(
                            itemName = editName.trim(),
                            unit = editUnit,
                            quantity = finalQ,
                            unitPrice = finalP,
                            preparationId = selectedPrep?.id ?: targetItem.preparationId,
                            preparationName = if (finalPP > 0 || selectedPrep != null) prepName else null,
                            preparationPrice = finalPP,
                            itemNotes = editNotes.trim(),
                            totalPrice = (finalP + finalPP) * finalQ
                        )
                        viewModel.updateOrderItem(currentOrder.id, updated)
                        editingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingItem = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 2. Add Item from Restaurant Menu Dialog
    if (showAddFromMenuDialog) {
        var selectedCategory by remember { mutableStateOf<String?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        var chosenMenuItem by remember { mutableStateOf<MenuItem?>(null) }
        var addQtyStr by remember { mutableStateOf("1.0") }
        var chosenPrep by remember { mutableStateOf<PreparationType?>(null) }
        var addNotes by remember { mutableStateOf("") }
        var customPriceStr by remember { mutableStateOf("") }

        val filteredMenuItems = allMenuItems.filter { item ->
            val matchCat = selectedCategory == null || item.category == selectedCategory
            val matchQuery = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }

        AlertDialog(
            onDismissRequest = { showAddFromMenuDialog = false },
            title = {
                Text("إضافة صنف من قائمة طعام المطعم", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                MouseLazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (chosenMenuItem == null) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("بحث عن صنف بالاسم...", fontSize = 12.5.sp, lineHeight = 20.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 20.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 56.dp)
                            )
                        }

                        // Categories filter
                        item {
                            MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                item {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { selectedCategory = null },
                                        label = { Text("الكل", fontSize = 10.5.sp) }
                                    )
                                }
                                items(allCategories) { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat.name,
                                        onClick = { selectedCategory = cat.name },
                                        label = { Text(cat.name, fontSize = 10.5.sp) }
                                    )
                                }
                            }
                        }

                        // Menu Items list to select
                        item {
                            Text("اختر الصنف المراد إضافته:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        items(filteredMenuItems) { mItem ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        chosenMenuItem = mItem
                                        customPriceStr = mItem.basePrice.toString()
                                        addQtyStr = if (mItem.unit == "كيلو") "1.0" else "1"
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(mItem.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                        Text("${mItem.basePrice} ج.م / ${mItem.unit} (${mItem.category})", fontSize = 10.5.sp, color = Color.Gray)
                                    }
                                    Button(
                                        onClick = {
                                            chosenMenuItem = mItem
                                            customPriceStr = mItem.basePrice.toString()
                                            addQtyStr = if (mItem.unit == "كيلو") "1.0" else "1"
                                        },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("اختيار", fontSize = 10.5.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // Configuration for selected menu item
                        val targetMItem = chosenMenuItem!!

                        item {
                            Surface(
                                color = SeaTealPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(targetMItem.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = SeaNavyDeep)
                                        Text("السعر الأساسي: ${targetMItem.basePrice} ج / ${targetMItem.unit}", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                    TextButton(onClick = { chosenMenuItem = null }) {
                                        Text("تغيير الصنف", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = addQtyStr,
                                    onValueChange = { addQtyStr = it },
                                    label = { Text("الكمية / الوزن (${targetMItem.unit})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                                OutlinedTextField(
                                    value = customPriceStr,
                                    onValueChange = { customPriceStr = it },
                                    label = { Text("سعر الوحدة") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Preparations for this item
                        item {
                            Text("طريقة التجهيز والطهي:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                item {
                                    FilterChip(
                                        selected = chosenPrep == null,
                                        onClick = { chosenPrep = null },
                                        label = { Text("بدون تجهيز إضافي", fontSize = 10.5.sp) }
                                    )
                                }
                                items(allPreparations) { prep ->
                                    val isSelected = chosenPrep?.id == prep.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { chosenPrep = prep },
                                        label = { Text("${prep.name} (+${prep.additionalPricePerUnit} ج)", fontSize = 10.5.sp) }
                                    )
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = addNotes,
                                onValueChange = { addNotes = it },
                                label = { Text("ملاحظات الصنف") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Calculated Total
                        val qVal = addQtyStr.toDoubleOrNull() ?: 1.0
                        val pVal = customPriceStr.toDoubleOrNull() ?: targetMItem.basePrice
                        val prepVal = chosenPrep?.additionalPricePerUnit ?: 0.0
                        val rowTotal = (pVal + prepVal) * qVal

                        item {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الإجمالي المحسوب للصنف:", fontSize = 12.sp)
                                    Text("${String.format(Locale.US, "%.1f", rowTotal)} ج.م", fontWeight = FontWeight.Bold, color = SeaTealPrimary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (chosenMenuItem != null) {
                    Button(
                        onClick = {
                            val item = chosenMenuItem!!
                            val qVal = addQtyStr.toDoubleOrNull() ?: 1.0
                            val pVal = customPriceStr.toDoubleOrNull() ?: item.basePrice
                            val prepVal = chosenPrep?.additionalPricePerUnit ?: 0.0
                            val rowTotal = (pVal + prepVal) * qVal

                            val newOrderItem = OrderItem(
                                orderId = currentOrder.id,
                                menuItemId = item.id,
                                itemName = item.name,
                                kitchenName = item.kitchenName,
                                category = item.category,
                                unit = item.unit,
                                quantity = qVal,
                                unitPrice = pVal,
                                preparationId = chosenPrep?.id,
                                preparationName = chosenPrep?.name,
                                preparationPrice = prepVal,
                                itemNotes = addNotes.trim(),
                                totalPrice = rowTotal,
                                isCustomAdHoc = false,
                                targetPrinter = item.targetPrinter
                            )
                            viewModel.addItemToOrder(currentOrder.id, newOrderItem)
                            showAddFromMenuDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                    ) {
                        Text("إضافة الصنف للأوردر ✓")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddFromMenuDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 3. Add Custom / Ad-Hoc Item Dialog
    if (showAddAdHocDialog) {
        var newName by remember { mutableStateOf("") }
        var newQtyStr by remember { mutableStateOf("1.0") }
        var newUnit by remember { mutableStateOf("كيلو") }
        var newPriceStr by remember { mutableStateOf("") }
        var newPrepName by remember { mutableStateOf("") }
        var newPrepPriceStr by remember { mutableStateOf("0.0") }
        var newNotes by remember { mutableStateOf("") }
        val allPrinters by viewModel.allPrinters.collectAsStateWithLifecycle()
        var selectedPrinterTarget by remember { mutableStateOf("MAIN") }

        AlertDialog(
            onDismissRequest = { showAddAdHocDialog = false },
            title = { Text("إضافة صنف استثنائي حر للفاتورة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                MouseLazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("اسم الصنف (مثلاً: عزومة سمك، صينية خاصة)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newQtyStr,
                                onValueChange = { newQtyStr = it },
                                label = { Text("الكمية") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newPriceStr,
                                onValueChange = { newPriceStr = it },
                                label = { Text("سعر الوحدة (ج.م)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newPrepName,
                                onValueChange = { newPrepName = it },
                                label = { Text("طريقة التجهيز (اختياري)") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                            OutlinedTextField(
                                value = newPrepPriceStr,
                                onValueChange = { newPrepPriceStr = it },
                                label = { Text("سعر التجهيز") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = newNotes,
                            onValueChange = { newNotes = it },
                            label = { Text("ملاحظات") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text(text = "توجيه الطابعة:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        val printerOptions = buildList {
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = newQtyStr.toDoubleOrNull() ?: 1.0
                        val p = newPriceStr.toDoubleOrNull() ?: 0.0
                        val pp = newPrepPriceStr.toDoubleOrNull() ?: 0.0
                        if (newName.isNotBlank() && p > 0) {
                            val newItem = OrderItem(
                                orderId = currentOrder.id,
                                itemName = newName.trim(),
                                category = "استثنائي",
                                unit = newUnit,
                                quantity = q,
                                unitPrice = p,
                                preparationName = newPrepName.ifBlank { null },
                                preparationPrice = pp,
                                totalPrice = (p + pp) * q,
                                itemNotes = newNotes.trim(),
                                isCustomAdHoc = true,
                                targetPrinter = selectedPrinterTarget
                            )
                            viewModel.addItemToOrder(currentOrder.id, newItem)
                            showAddAdHocDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("إضافة للفاتورة")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAdHocDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Quick Keyboard Quantity Dialog for OrderItem
    itemForCustomQty?.let { item ->
        var qtyInput by remember { mutableStateOf(ReceiptBitmapRenderer.formatQuantity(item.quantity)) }
        val parsedQty = qtyInput.toDoubleOrNull() ?: item.quantity
        val liveTotal = (item.unitPrice + item.preparationPrice) * parsedQty

        AlertDialog(
            onDismissRequest = { itemForCustomQty = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = SeaTealPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تعديل الكمية بالكيبورد: ${item.itemName}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SeaNavyDeep)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("سعر الوحدة: ${item.unitPrice + item.preparationPrice} ج.م / ${item.unit}", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { qtyInput = it },
                        label = { Text("الكمية / الوزن (${item.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الإجمالي الجديد المحسوب:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14532D))
                            Text("${String.format(Locale.US, "%.1f", liveTotal)} ج.م", fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalQ = qtyInput.toDoubleOrNull() ?: item.quantity
                        if (finalQ <= 0) {
                            viewModel.deleteOrderItemFromOrder(currentOrder.id, item.id)
                        } else {
                            val updatedTotal = (item.unitPrice + item.preparationPrice) * finalQ
                            viewModel.updateOrderItem(currentOrder.id, item.copy(quantity = finalQ, totalPrice = updatedTotal))
                        }
                        itemForCustomQty = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("تأكيد وحفظ ✓", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemForCustomQty = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}
