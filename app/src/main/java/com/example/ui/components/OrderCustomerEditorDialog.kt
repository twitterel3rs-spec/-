package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.util.phoneDigitCount
import com.example.util.isCustomerPhoneIncomplete
import com.example.util.isCustomerPhoneTooLong
import com.example.util.isCustomerPhoneInvalid
import com.example.util.getCustomerPhoneWarning
import com.example.util.getCustomerPhoneToastWarning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.CustomerAddress
import com.example.data.model.OrderWithItems
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.viewmodel.MainViewModel

@Composable
fun OrderCustomerEditorDialog(
    orderWithItems: OrderWithItems,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val order = orderWithItems.order
    val deliveryAreas by viewModel.deliveryAreas.collectAsStateWithLifecycle()
    val allOrders by viewModel.orders.collectAsStateWithLifecycle()
    val drivers by viewModel.drivers.collectAsStateWithLifecycle()

    var orderType by remember { mutableStateOf(order.orderType) }
    var selectedDriverId by remember { mutableStateOf(order.driverId) }
    var selectedDriverName by remember { mutableStateOf(order.driverName) }

    var name by remember { mutableStateOf(order.customerName) }
    var phone by remember { mutableStateOf(order.customerPhone) }
    var secondaryPhone by remember { mutableStateOf(order.customerSecondaryPhone ?: "") }
    var area by remember { mutableStateOf(order.customerArea ?: "") }
    var address by remember { mutableStateOf(order.deliveryAddressFull ?: "") }
    var feeStr by remember { mutableStateOf(String.format(Locale.US, "%.0f", order.deliveryFee)) }
    var driverCommStr by remember { mutableStateOf(String.format(Locale.US, "%.0f", order.driverCommission)) }
    var notes by remember { mutableStateOf(order.notes) }

    var updatePermanentCustomer by remember { mutableStateOf(false) }
    var showPastOrdersDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }

    // Find past orders for this customer (by phone or customerId)
    val customerPastOrders = remember(order.customerPhone, order.customerId, allOrders) {
        allOrders.filter { o ->
            o.order.id != order.id && (
                (order.customerId != null && o.order.customerId == order.customerId) ||
                (order.customerPhone.isNotBlank() && o.order.customerPhone == order.customerPhone)
            )
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current

    val phoneDigits = phone.phoneDigitCount()
    val phoneWarning = phone.getCustomerPhoneWarning()
    val isPhoneInvalid = phone.isCustomerPhoneInvalid()
    val secPhoneDigits = secondaryPhone.phoneDigitCount()
    val secPhoneWarning = secondaryPhone.getCustomerPhoneWarning()
    val isSecPhoneInvalid = secondaryPhone.isCustomerPhoneInvalid()

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
                        text = "تعديل التوصيل والعميل لأوردر #${order.orderNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "تعديل نوع الطلب، كابتن التوصيل، العنوان، ورسوم التوصيل والعمولة.",
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
            MouseLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isLandscape) 180.dp else 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Order Type Selector
                item {
                    Text(text = "نوع الطلب:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = orderType == "DELIVERY",
                            onClick = { orderType = "DELIVERY" },
                            label = { Text("🛵 توصيل دليفري", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = orderType == "TAKEAWAY",
                            onClick = { orderType = "TAKEAWAY" },
                            label = { Text("🏪 استلام من المحل / حجز", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // If Delivery: Driver Selection
                if (orderType == "DELIVERY") {
                    item {
                        Text(text = "كابتن التوصيل (الطيار):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedDriverId == null,
                                    onClick = {
                                        selectedDriverId = null
                                        selectedDriverName = null
                                    },
                                    label = { Text("بدون طيار حالياً") }
                                )
                            }
                            items(drivers) { drv ->
                                val isSelected = selectedDriverId == drv.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedDriverId = drv.id
                                        selectedDriverName = drv.name
                                        if (drv.calculationType == "COMMISSION") {
                                            val currentFee = feeStr.toDoubleOrNull() ?: 0.0
                                            driverCommStr = if (currentFee > 0) currentFee.toString() else drv.ratePerOrder.toString()
                                        }
                                    },
                                    label = { Text("🛵 ${drv.name} (${if (drv.calculationType == "COMMISSION") "بالعمولة" else "يومية"})") }
                                )
                            }
                        }
                    }
                }

                // Past orders button if available
                if (customerPastOrders.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SeaNavyDeep,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPastOrdersDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = AmberCoral, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "سجل أوردرات العميل وتفضيلاته السابقة (${customerPastOrders.size} طلبات)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(text = "عرض 👁️", color = AmberCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم العميل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم هاتف العميل الأساسي *") },
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
                }

                item {
                    OutlinedTextField(
                        value = secondaryPhone,
                        onValueChange = { secondaryPhone = it },
                        label = { Text("رقم هاتف إضافي / بديل (اختياري)") },
                        placeholder = { Text("مثال: 011xxxxxxxx أو رقم العمل") },
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
                }

                // Delivery Area Selector (Only if DELIVERY)
                if (orderType == "DELIVERY") {
                    item {
                        Text(text = "منطقة التوصيل (انقر للتطبيق السريع):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        MouseLazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(deliveryAreas) { da ->
                                val isSelected = area == da.name
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        area = da.name
                                        feeStr = String.format(Locale.US, "%.0f", da.deliveryFee)
                                        driverCommStr = String.format(Locale.US, "%.0f", da.deliveryFee)
                                    },
                                    label = { Text("${da.name} (${String.format(Locale.US, "%.0f", da.deliveryFee)}ج)", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = area,
                            onValueChange = { area = it },
                            label = { Text("اسم المنطقة / الحي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("العنوان بالتفصيل (الشارع، العمارة، الشقة)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Fees Row
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = feeStr,
                                onValueChange = {
                                    val cleaned = it.filter { ch -> ch.isDigit() }
                                    feeStr = cleaned
                                    // Auto sync driver commission with delivery fee for commission driver
                                    val matchedDriver = drivers.firstOrNull { d -> d.id == selectedDriverId }
                                    if (matchedDriver == null || matchedDriver.calculationType == "COMMISSION") {
                                        driverCommStr = cleaned
                                    }
                                },
                                label = { Text("رسوم التوصيل (ج.م)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = driverCommStr,
                                onValueChange = { driverCommStr = it.filter { ch -> ch.isDigit() } },
                                label = { Text("عمولة الموصل (ج.م)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات الأوردر والتوصيل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Add another address button
                item {
                    OutlinedButton(
                        onClick = { showAddAddressDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة عنوان آخر للعميل وحفظه 📍", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    phone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                        Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                    }
                    secondaryPhone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                        Toast.makeText(context, toastMsg.replace("رقم هاتف العميل", "الرقم الإضافي"), Toast.LENGTH_LONG).show()
                    }
                    val f = feeStr.toDoubleOrNull() ?: order.deliveryFee
                    val comm = driverCommStr.toDoubleOrNull() ?: order.driverCommission

                    viewModel.updateOrderCustomerDetails(
                        orderId = order.id,
                        orderType = orderType,
                        driverId = if (orderType == "DELIVERY") selectedDriverId else null,
                        driverName = if (orderType == "DELIVERY") selectedDriverName else null,
                        name = name.trim(),
                        phone = phone.trim(),
                        secondaryPhone = secondaryPhone.trim().ifBlank { null },
                        area = if (orderType == "DELIVERY") area.trim().ifBlank { null } else null,
                        address = if (orderType == "DELIVERY") address.trim().ifBlank { null } else null,
                        fee = if (orderType == "DELIVERY") f else 0.0,
                        driverCommission = if (orderType == "DELIVERY") comm else 0.0,
                        notes = notes.trim()
                    )

                    if (phone.isNotBlank()) {
                        viewModel.updateCustomerPermanent(
                            Customer(
                                id = order.customerId ?: 0L,
                                name = name.trim().ifBlank { "عميل" },
                                primaryPhone = phone.trim(),
                                secondaryPhone = secondaryPhone.trim().ifBlank { null },
                                notes = notes.trim()
                            )
                        )
                    }

                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ وتحديث الأوردر")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        viewModel.startEditingOrder(orderWithItems)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberCoral),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberCoral)
                ) {
                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberCoral)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل كامل في شاشة البيع ✏️", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
                androidx.compose.material3.OutlinedButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )

    // Customer Past Orders Dialog (Preferences)
    if (showPastOrdersDialog) {
        AlertDialog(
            onDismissRequest = { showPastOrdersDialog = false },
            title = {
                Text(
                    text = "تفضيلات وأوردرات العميل: ${order.customerName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "إجمالي الطلبات السابقة: ${customerPastOrders.size} أوردر",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MouseLazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
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
                                    Text(
                                        text = "الأصناف والتجهيزات المفضلة:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    past.items.forEach { itm ->
                                        Text(
                                            text = "• ${itm.quantity} ${itm.unit} ${itm.itemName}" +
                                                    if (!itm.preparationName.isNullOrBlank()) " (${itm.preparationName})" else "" +
                                                    if (itm.itemNotes.isNotBlank()) " [${itm.itemNotes}]" else "",
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
                Button(onClick = { showPastOrdersDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Add Another Address Dialog
    if (showAddAddressDialog) {
        var addrTitle by remember { mutableStateOf("عنوان إضافي") }
        var addrArea by remember { mutableStateOf(area) }
        var addrStreet by remember { mutableStateOf("") }
        var addrFeeStr by remember { mutableStateOf(feeStr) }

        AlertDialog(
            onDismissRequest = { showAddAddressDialog = false },
            title = { Text("إضافة عنوان آخر للعميل", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = addrTitle,
                        onValueChange = { addrTitle = it },
                        label = { Text("تسمية العنوان (البيت / العمل / المصيف)") },
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
                        label = { Text("تفاصيل العنوان والشارع") },
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
                                    customerId = order.customerId ?: 0L,
                                    title = addrTitle.trim(),
                                    area = addrArea.trim(),
                                    streetDetails = addrStreet.trim(),
                                    deliveryFee = fee
                                )
                            )
                            // Set as current order address too
                            area = addrArea
                            address = "${addrArea.trim()} - ${addrStreet.trim()}"
                            feeStr = fee.toString()
                            showAddAddressDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("حفظ العنوان")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAddressDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
