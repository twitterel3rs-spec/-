package com.example.ui.components

import android.graphics.BitmapFactory
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.printer.InvoiceTemplateConfig
import com.example.printer.ReceiptBitmapRenderer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CheckCircle
import android.widget.Toast
import com.example.util.phoneDigitCount
import com.example.util.isCustomerPhoneIncomplete
import com.example.util.isCustomerPhoneTooLong
import com.example.util.isCustomerPhoneInvalid
import com.example.util.getCustomerPhoneWarning
import com.example.util.getCustomerPhoneToastWarning
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.CustomerAddress
import com.example.data.model.DeliveryDriver
import com.example.data.model.MenuItem
import com.example.data.model.OrderWithItems
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaNavyMedium
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PasswordPromptDialog(
    title: String = "مطلوب كلمة سر الإدارة",
    message: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = AmberCoral,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        hasError = false
                    },
                    label = { Text("كلمة السر (الافتراضية: 1234)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = hasError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (hasError) {
                    Text(
                        text = "كلمة السر غير صحيحة",
                        color = StatusDanger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isNotBlank()) {
                        onConfirm(password)
                    } else {
                        hasError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("تأكيد")
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
fun StockWarningOverrideDialog(
    itemName: String,
    requested: Double,
    available: Double,
    onConfirmOverride: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = StatusDanger,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "الكمية تتجاوز المخزون المتاح!",
                fontWeight = FontWeight.Bold,
                color = StatusDanger
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "الصنف: $itemName\nالكمية المطلوبة: $requested\nالمتاح في المخزن: $available",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "للسماح بالبيع وتجاوز الرصيد المتاح، يجب إدخال كلمة سر الإدارة:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة سر الإدارة (1234)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmOverride(password) },
                colors = ButtonDefaults.buttonColors(containerColor = AmberCoral)
            ) {
                Text("تجاوز وإتمام الأوردر")
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
fun DailyPriceBatchEditorDialog(
    freshItems: List<MenuItem>,
    onSave: (Map<Long, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    val priceMap = remember(freshItems) {
        mutableStateMapOf<Long, String>().apply {
            freshItems.forEach { put(it.id, it.basePrice.toString()) }
        }
    }
    val selectedItemIds = remember(freshItems) {
        androidx.compose.runtime.mutableStateListOf<Long>().apply {
            addAll(freshItems.map { it.id })
        }
    }
    var batchPriceInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val displayedItems = remember(freshItems, searchQuery) {
        if (searchQuery.isBlank()) freshItems
        else freshItems.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = AmberCoral,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تعديل أسعار الأصناف دفعة واحدة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Text(
                    text = "حدد الأصناف وأدخل السعر الجديد لتطبيقه على الأصناف المحددة دفعة واحدة، أو عدل سعر كل صنف مباشرة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Unified Batch Price Box (Solves Requirement 1: مربع أعدل منه السعر دفعة واحدة)
                Surface(
                    color = SeaTealPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "⚡ تطبيق سعر موحد على الأصناف المحددة (${selectedItemIds.size} صنف):",
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
                                placeholder = { Text("مثال: 180") },
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
                                Text("تطبيق السعر", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Search & Select Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث عن صنف...", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(
                        onClick = {
                            selectedItemIds.clear()
                            selectedItemIds.addAll(displayedItems.map { it.id })
                        }
                    ) {
                        Text("تحديد الكل", fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = { selectedItemIds.clear() }
                    ) {
                        Text("إلغاء", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                MouseLazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(displayedItems, key = { it.id }) { item ->
                        val isChecked = selectedItemIds.contains(item.id)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) SeaTealPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (isChecked) BorderStroke(1.dp, SeaTealPrimary.copy(alpha = 0.5f)) else null,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { chk ->
                                        if (chk == true) selectedItemIds.add(item.id)
                                        else selectedItemIds.remove(item.id)
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                    Text(
                                        text = "${item.category} | رصيد: ${item.currentStock} ${item.unit}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedTextField(
                                    value = priceMap[item.id] ?: "",
                                    onValueChange = { priceMap[item.id] = it },
                                    label = { Text("السعر") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.width(95.dp)
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
                    val finalMap = mutableMapOf<Long, Double>()
                    for ((id, str) in priceMap) {
                        val clean = str.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9').replace('٫','.').replace(',','.').trim()
                        clean.toDoubleOrNull()?.let { finalMap[id] = it }
                    }
                    onSave(finalMap)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ وتطبيق الأسعار (تم)", fontWeight = FontWeight.Bold)
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
fun IncomingCallerSheet(
    phoneNumber: String,
    customer: Customer?,
    addresses: List<CustomerAddress>,
    recentOrders: List<OrderWithItems>,
    onStartOrder: (Customer?, String, CustomerAddress?) -> Unit,
    onRegisterCustomer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (customer != null) StatusSuccess else AmberCoral),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (customer != null) "مكالمة واردة: ${customer.name}" else "مكالمة واردة من رقم جديد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (customer != null) {
                    // Registered Customer
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "بيانات العميل المسجل",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (customer.notes.isNotBlank()) {
                                Text(
                                    text = "ملاحظات وتفضيلات: ${customer.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "عدد الأوردرات السابقة: ${customer.totalOrdersCount}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "إجمالي التعامل: ${String.format(Locale.US, "%.1f", customer.totalSpent)} ج.م",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "العناوين المسجلة (${addresses.size}):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    var selectedAddr by remember { mutableStateOf(addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()) }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        addresses.forEach { addr ->
                            val isChosen = selectedAddr?.id == addr.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChosen) SeaTealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isChosen) 2.dp else 1.dp,
                                        color = if (isChosen) SeaTealPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${addr.title}: ${addr.area} - ${addr.streetDetails}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "رسوم التوصيل: ${String.format(Locale.US, "%.0f", addr.deliveryFee)} ج.م",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { selectedAddr = addr },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(if (isChosen) "المحدد ✓" else "اختيار")
                                    }
                                }
                            }
                        }
                    }

                    if (recentOrders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "آخر الأوردرات السابقة:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        recentOrders.take(2).forEach { ro ->
                            Text(
                                text = "• أوردر ${ro.order.orderNumber} (${ro.order.totalAmount} ج.م) - ${ro.items.joinToString("، ") { it.itemName }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onStartOrder(customer, phoneNumber, selectedAddr) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة أوردر جديد لهذا العميل", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Unregistered New Number
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "رقم هاتف جديد غير مسجل",
                                fontWeight = FontWeight.Bold,
                                color = StatusWarning
                            )
                            Text(
                                text = "يمكنك بدء الأوردر مباشرة وسيتم حفظ بيانات وعنوان العميل تلقائياً مع الطلب.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onStartOrder(null, phoneNumber, null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بدء أوردر جديد لهذا الرقم", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun OrderInvoiceAndPrintingDialog(
    orderWithItems: OrderWithItems,
    onPrintToTarget: (printerTarget: String, reprintReason: String?) -> Unit,
    onDismiss: () -> Unit,
    viewModel: MainViewModel? = null
) {
    val liveOrderState = viewModel?.getOrderByIdFlow(orderWithItems.order.id)?.collectAsStateWithLifecycle(initialValue = orderWithItems)?.value ?: orderWithItems
    val context = LocalContext.current
    val order = liveOrderState.order
    val items = liveOrderState.items
    var selectedTab by remember { mutableStateOf("MAIN") }
    var reprintReason by remember { mutableStateOf("") }
    var showReprintInput by remember { mutableStateOf(false) }

    var showEditOrderItemsDialog by remember { mutableStateOf(false) }
    var showEditOrderCustomerDialog by remember { mutableStateOf(false) }
    var showPaymentSettlementDialog by remember { mutableStateOf(false) }
    var templateConfig by remember { mutableStateOf(InvoiceTemplateConfig()) }
    val scope = rememberCoroutineScope()
    var showKitchenReprintPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel != null) {
            templateConfig = viewModel.getInvoiceTemplateConfig()
        }
    }

    val logoBitmap = remember(templateConfig.logoPath) {
        templateConfig.logoPath?.let { path ->
            try {
                val f = File(path)
                if (f.exists()) BitmapFactory.decodeFile(path) else null
            } catch (e: Exception) {
                null
            }
        }
    }

    val previewScale = when (templateConfig.fontSize.uppercase()) {
        "HUGE" -> 1.45f
        "LARGE" -> 1.25f
        else -> 1.0f
    }

    // WhatsApp Sharing States
    var showCustomWhatsAppDialog by remember { mutableStateOf(false) }
    var customWhatsAppPhone by remember { mutableStateOf("") }

    // Driver Assignment States
    val drivers = viewModel?.drivers?.collectAsStateWithLifecycle()?.value ?: emptyList()
    var showDriverSelectionDialog by remember { mutableStateOf(false) }
    var showQuickAddDriverDialog by remember { mutableStateOf(false) }
    var newDriverName by remember { mutableStateOf("") }
    var newDriverPhone by remember { mutableStateOf("") }
    var newDriverCommissionStr by remember { mutableStateOf("15") }
    val configuredPrinters = viewModel?.allPrinters?.collectAsStateWithLifecycle()?.value ?: emptyList()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
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
                        text = "فاتورة أوردر ${order.orderNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "نوع الطلب: ${if (order.orderType == "DELIVERY") "دليفري توصيل" else "استلام وحجز (تيك أواي)"}",
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
                    .heightIn(max = if (isLandscape) 220.dp else 520.dp)
                    .mouseVerticalScroll(rememberScrollState())
            ) {
                val tabs = if (configuredPrinters.isNotEmpty()) {
                    configuredPrinters.filter { it.isEnabled }.map { p ->
                        val isPrinted = when {
                            p.isCashierDefault || p.id == "MAIN" -> order.printedMain
                            p.id == "KITCHEN_1" -> order.printedKitchen1
                            p.id == "KITCHEN_2" -> order.printedKitchen2
                            p.role == "KITCHEN_ORDER" -> order.printedKitchen1 || order.printedKitchen2
                            else -> order.printedMain
                        }
                        Triple(p.id, p.displayName, isPrinted)
                    }
                } else {
                    listOf(
                        Triple("MAIN", "الفاتورة الرسمية", order.printedMain)
                    )
                }
                MouseLazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(tabs) { (key, label, isPrinted) ->
                        val isSelected = selectedTab == key
                        Button(
                            onClick = {
                                selectedTab = key
                                showReprintInput = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) SeaTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = label, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                if (isPrinted) {
                                    Text(text = "طُبعت ✓", fontSize = 9.sp, maxLines = 1, softWrap = false, color = if (isSelected) Color.White else StatusSuccess)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Prominent Edit Order Buttons (Items & Customer/Type)
                if (viewModel != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { showEditOrderItemsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "✏️ تعديل الأصناف",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = { showEditOrderCustomerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                            modifier = Modifier.weight(1.3f).height(38.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeliveryDining, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "🛵 تعديل العميل (دليفري/استلام)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Printable Preview Box (Organized Table Design)
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (isLandscape) 120.dp else 160.dp, max = if (isLandscape) 180.dp else 320.dp)
                        .border(1.dp, Color(0xFFD0D7DE), RoundedCornerShape(8.dp))
                ) {
                    MouseLazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        item {
                            // Header with Logo
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (logoBitmap != null) {
                                    Image(
                                        bitmap = logoBitmap.asImageBitmap(),
                                        contentDescription = "لوجو المطعم",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.height(44.dp).padding(bottom = 4.dp)
                                    )
                                }

                                Text(
                                    text = templateConfig.headerTitle.ifBlank { "« صنارة وشبكة للمأكولات البحرية »" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (15f * previewScale).sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = if (selectedTab == "MAIN") "فاتورة حساب تفصيلية للعميل" else if (selectedTab == "KITCHEN_1") "تذكرة تشغيل: مطبخ 1 (شواية وفرن)" else "تذكرة تشغيل: مطبخ 2 (قلي وتجهيز)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = (11.5f * previewScale).sp,
                                    color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Prominent Order Number & Type Badge
                                Surface(
                                    color = Color.Black,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "أوردر رقم: ${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = (14f * previewScale).sp, color = Color.White)
                                        Text(text = if (order.orderType == "DELIVERY") "🛵 دليفري توصيل" else "🏪 استلام وحجز", fontWeight = FontWeight.Bold, fontSize = (12f * previewScale).sp, color = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "التاريخ: ${dateFormat.format(Date(order.createdAt))}",
                                        fontSize = (10f * previewScale).sp,
                                        color = Color.Gray
                                    )
                                }

                                // Prominent Customer & Delivery Information (Independent fields)
                                val showCustName = order.customerName.isNotBlank() && order.customerName != "زبون مباشر"
                                val showCustPhone = order.customerPhone.isNotBlank()
                                val showSecPhone = !order.customerSecondaryPhone.isNullOrBlank()
                                val showAddress = !order.deliveryAddressFull.isNullOrBlank()
                                val showDriver = !order.driverName.isNullOrBlank()
                                val hasAnyCustomerInfo = showCustName || showCustPhone || showSecPhone || showAddress || showDriver

                                if (hasAnyCustomerInfo) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp)) {
                                            if (showCustName) {
                                                Text(
                                                    text = "👤 العميل: ${order.customerName}",
                                                    fontSize = (11.5f * previewScale).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            }
                                            if (showCustPhone) {
                                                Text(
                                                    text = "📞 هاتف: ${order.customerPhone}",
                                                    fontSize = (12f * previewScale).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            }
                                            if (showSecPhone) {
                                                Text(
                                                    text = "📱 هاتف إضافي: ${order.customerSecondaryPhone}",
                                                    fontSize = (11f * previewScale).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            }
                                            if (showAddress) {
                                                Text(
                                                    text = "📍 العنوان: ${order.deliveryAddressFull}",
                                                    fontSize = (11f * previewScale).sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.Black
                                                )
                                            }
                                            if (showDriver) {
                                                Text(
                                                    text = "🛵 كابتن التوصيل: ${order.driverName}",
                                                    fontSize = (11f * previewScale).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SeaTealPrimary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        // Filter items based on selected target printer
                        val currentPrinterConfig = configuredPrinters.firstOrNull { it.id == selectedTab }
                        val isCustomerInvoicePrinter = currentPrinterConfig?.role == "CUSTOMER_INVOICE" || selectedTab == "MAIN"
                        val filteredItems = items.filter { item ->
                            if (isCustomerInvoicePrinter) {
                                true
                            } else {
                                item.targetPrinter.contains(selectedTab) || item.targetPrinter.contains("ALL") || item.targetPrinter.isBlank() || item.targetPrinter == "MAIN"
                            }
                        }

                        // Table Header
                        item {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "الصنف والتجهيز", modifier = Modifier.weight(3.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                    Text(text = "الكمية", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Black)
                                    if (selectedTab == "MAIN") {
                                        Text(text = "السعر", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Black)
                                        Text(text = "الإجمالي", modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, color = Color.Black)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Table Rows
                        items(filteredItems) { item ->
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(3.5f)) {
                                        Text(
                                            text = if (selectedTab == "MAIN") item.itemName else item.kitchenName.ifBlank { item.itemName },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.Black
                                        )
                                        if (!item.preparationName.isNullOrBlank()) {
                                            Text(
                                                text = "• طريقة التجهيز: ${item.preparationName} (+${item.preparationPrice} ج)",
                                                fontSize = 9.sp,
                                                color = Color(0xFFB91C1C),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (item.itemNotes.isNotBlank()) {
                                            Text(
                                                text = "• ملحوظة: ${item.itemNotes}",
                                                fontSize = 9.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${ReceiptBitmapRenderer.formatQuantity(item.quantity)} ${item.unit}",
                                        modifier = Modifier.weight(1.5f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        color = Color.Black
                                    )
                                    if (selectedTab == "MAIN") {
                                        val unitPrice = item.totalPrice / if (item.quantity > 0) item.quantity else 1.0
                                        Text(
                                            text = String.format(Locale.US, "%.1f", unitPrice),
                                            modifier = Modifier.weight(1.5f),
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", item.totalPrice)} ج",
                                            modifier = Modifier.weight(1.8f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.End,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }

                        // Financial Summary Section (Detailed Breakdown)
                        if (selectedTab == "MAIN") {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = Color.Black, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("المجموع الفرعي للأصناف:", fontSize = 11.sp, color = Color.Black)
                                    Text("${String.format(Locale.US, "%.1f", order.subtotal)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                }
                                if (order.deliveryFee > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("رسوم التوصيل (الدليفري):", fontSize = 11.sp, color = Color.Black)
                                        Text("+${String.format(Locale.US, "%.0f", order.deliveryFee)} ج.م", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                    }
                                }
                                if (order.discount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("خصم خاص للعميل:", fontSize = 11.sp, color = Color(0xFFDC2626))
                                        Text("-${order.discount} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                    }
                                }
                                HorizontalDivider(color = Color.Black, thickness = 1.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                val isOrderFullyPaid = order.isPaid || (order.paidAmount >= order.totalAmount && order.totalAmount > 0)
                                val hasRemainingOnCustomer = !isOrderFullyPaid && (order.paidAmount < order.totalAmount) && (order.paidAmount > 0)
                                val remainingAmt = (order.totalAmount - order.paidAmount).coerceAtLeast(0.0)

                                // Prominent Solid Black Payment & Total Box (matching order number box style)
                                Surface(
                                    color = Color.Black,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isOrderFullyPaid) "★ الأوردر خالص بالكامل ★"
                                                       else if (hasRemainingOnCustomer) "⚠️ متبقي على العميل: ${String.format(Locale.US, "%.1f", remainingAmt)} ج"
                                                       else "🔴 الأوردر غير مدفوع - كاش",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (12f * previewScale).sp,
                                                color = Color.White,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (viewModel != null) {
                                                OutlinedButton(
                                                    onClick = { showPaymentSettlementDialog = true },
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                                    modifier = Modifier.height(26.dp),
                                                    shape = RoundedCornerShape(4.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = Color.White,
                                                        contentColor = Color.Black
                                                    ),
                                                    border = BorderStroke(1.dp, Color.White)
                                                ) {
                                                    Text("💵 تعديل", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = Color.DarkGray, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isOrderFullyPaid) "المدفوع (خالص ✓):" else if (hasRemainingOnCustomer) "المطلوب كاش:" else "المطلوب تحصيله:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (13f * previewScale).sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (isOrderFullyPaid) "${String.format(Locale.US, "%.1f", order.totalAmount)} ج.م"
                                                       else if (hasRemainingOnCustomer) "${String.format(Locale.US, "%.1f", remainingAmt)} ج.م"
                                                       else "${String.format(Locale.US, "%.1f", order.totalAmount)} ج.م",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = (15f * previewScale).sp,
                                                color = Color.White
                                            )
                                        }

                                        if (order.paidAmount > order.totalAmount && order.changeAmount > 0) {
                                            Text(
                                                text = "المدفوع نقداً: ${String.format(Locale.US, "%.1f", order.paidAmount)} ج | الباقي للعميل: ${String.format(Locale.US, "%.1f", order.changeAmount)} ج",
                                                fontSize = (9.5f * previewScale).sp,
                                                color = Color(0xFFE2E8F0),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        } else if (hasRemainingOnCustomer) {
                                            Text(
                                                text = "إجمالي: ${String.format(Locale.US, "%.1f", order.totalAmount)} ج | المسدد: ${String.format(Locale.US, "%.1f", order.paidAmount)} ج",
                                                fontSize = (9.5f * previewScale).sp,
                                                color = Color(0xFFFDE68A),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "طريقة الدفع: ${if (order.paymentMethod == "CASH") "نقداً كاش" else "محفظة إلكترونية"}",
                                        fontSize = 10.sp,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "شكراً لتعاملكم معنا",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Delivery Driver Quick Assignment Row (Requirement 9)
                if (order.orderType == "DELIVERY") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(imageVector = Icons.Default.DeliveryDining, contentDescription = null, tint = SeaTealPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (order.driverName != null) "الطيار: ${order.driverName}" else "لم يُحدد طيار بعد",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "تحديد أو استبدال كابتن التوصيل لهذا الأوردر",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showDriverSelectionDialog = true },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(if (order.driverName != null) "تغيير الطيار" else "تعيين طيار", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // WhatsApp Sharing Controls (Requirement 6)
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إرسال الفاتورة عبر واتساب:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF166534)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (order.customerPhone.isNotBlank()) {
                                Button(
                                    onClick = {
                                        viewModel?.shareInvoiceViaWhatsApp(orderWithItems, order.customerPhone, context)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("واتساب العميل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { showCustomWhatsAppDialog = true },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رقم واتساب آخر", fontSize = 11.sp)
                            }
                        }
                    }
                }

                val activeConfig = configuredPrinters.firstOrNull { it.id == selectedTab }
                val isKitchenTicket = selectedTab != "MAIN" || activeConfig?.role == "STAFF_KITCHEN" || activeConfig?.role == "BOTH"
                val isAlreadyPrinted = when (selectedTab) {
                    "MAIN" -> order.printedMain
                    "KITCHEN_1" -> order.printedKitchen1
                    "KITCHEN_2" -> order.printedKitchen2
                    else -> if (activeConfig?.role == "CUSTOMER_INVOICE") order.printedMain else (order.printedKitchen1 || order.printedKitchen2)
                }

                if (isAlreadyPrinted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isKitchenTicket) "⚠️ تم طباعة أمر التشغيل مسبقاً. إعادة الطباعة تتطلب كلمة سر الإدارة لمنع التكرار." else "⚠️ تم طباعة هذه التذكرة مسبقاً لمنع التكرار والازدواجية.",
                        color = StatusWarning,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (showReprintInput) {
                        OutlinedTextField(
                            value = reprintReason,
                            onValueChange = { reprintReason = it },
                            label = { Text("سبب إعادة الطباعة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            val activeConfig = configuredPrinters.firstOrNull { it.id == selectedTab }
            val isKitchenTicket = selectedTab != "MAIN" || activeConfig?.role == "STAFF_KITCHEN" || activeConfig?.role == "BOTH"
            val isAlreadyPrinted = when (selectedTab) {
                "MAIN" -> order.printedMain
                "KITCHEN_1" -> order.printedKitchen1
                "KITCHEN_2" -> order.printedKitchen2
                else -> if (activeConfig?.role == "CUSTOMER_INVOICE") order.printedMain else (order.printedKitchen1 || order.printedKitchen2)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Send invoice to customer button (WhatsApp / Share)
                Button(
                    onClick = {
                        if (order.customerPhone.isNotBlank()) {
                            viewModel?.shareInvoiceViaWhatsApp(orderWithItems, order.customerPhone, context)
                        } else {
                            showCustomWhatsAppDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "إرسال للعميل (واتساب)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        if (isAlreadyPrinted && isKitchenTicket) {
                            showKitchenReprintPassword = true
                        } else {
                            onPrintToTarget(selectedTab, if (isAlreadyPrinted) (reprintReason.ifBlank { "إعادة طباعة يدوية" }) else null)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isAlreadyPrinted && isKitchenTicket) AmberCoral else SeaTealPrimary)
                ) {
                        Icon(imageVector = if (isAlreadyPrinted && isKitchenTicket) Icons.Default.Lock else Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAlreadyPrinted) {
                                if (isKitchenTicket) "إعادة طبع أمر التشغيل 🔒" else "إعادة الطباعة"
                            } else "إرسال للطابعة",
                            maxLines = 1,
                            softWrap = false,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )

    // Edit Order Items & Details (Redirects directly to POS Screen)
    if (showEditOrderItemsDialog && viewModel != null) {
        LaunchedEffect(Unit) {
            viewModel.startEditingOrder(liveOrderState)
            showEditOrderItemsDialog = false
            onDismiss()
        }
    }

    // Edit Order Customer & Type (Delivery / Takeaway) Dialog
    if (showEditOrderCustomerDialog && viewModel != null) {
        OrderCustomerEditorDialog(
            orderWithItems = liveOrderState,
            viewModel = viewModel,
            onDismiss = { showEditOrderCustomerDialog = false }
        )
    }

    // Payment & Settle Dialog
    if (showPaymentSettlementDialog && viewModel != null) {
        PaymentSettlementDialog(
            orderWithItems = liveOrderState,
            viewModel = viewModel,
            onDismiss = { showPaymentSettlementDialog = false }
        )
    }

    // Send to Custom WhatsApp Phone Dialog
    if (showCustomWhatsAppDialog) {
        val whatsAppPhoneDigits = customWhatsAppPhone.phoneDigitCount()
        val whatsAppPhoneWarning = customWhatsAppPhone.getCustomerPhoneWarning()
        val isWhatsAppPhoneInvalid = customWhatsAppPhone.isCustomerPhoneInvalid()

        AlertDialog(
            onDismissRequest = { showCustomWhatsAppDialog = false },
            title = { Text("إرسال الفاتورة لرقم واتساب آخر", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "أدخل رقم الهاتف الذي ترغب بإرسال الفاتورة المنظمة إليه عبر تطبيق واتساب:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customWhatsAppPhone,
                        onValueChange = { customWhatsAppPhone = it },
                        label = { Text("رقم الواتساب (مثال: 01012345678)") },
                        isError = isWhatsAppPhoneInvalid,
                        supportingText = {
                            if (whatsAppPhoneWarning != null) {
                                Text(
                                    text = whatsAppPhoneWarning,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (whatsAppPhoneDigits == 11) {
                                Text(
                                    text = "✓ رقم مكتمل (11 رقم)",
                                    color = Color(0xFF16A34A),
                                    fontSize = 10.sp
                                )
                            }
                        },
                        trailingIcon = {
                            if (isWhatsAppPhoneInvalid) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = whatsAppPhoneWarning ?: "تحذير: رقم غير صحيح",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (whatsAppPhoneDigits == 11) {
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        customWhatsAppPhone.getCustomerPhoneToastWarning()?.let { toastMsg ->
                            Toast.makeText(
                                context,
                                toastMsg.replace("رقم هاتف العميل", "رقم الواتساب"),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        if (customWhatsAppPhone.isNotBlank()) {
                            viewModel?.shareInvoiceViaWhatsApp(orderWithItems, customWhatsAppPhone.trim(), context)
                            showCustomWhatsAppDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إرسال عبر واتساب")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomWhatsAppDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Driver Selection Dialog (Requirement 9)
    if (showDriverSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showDriverSelectionDialog = false },
            title = { Text("اختيار أو تغيير كابتن التوصيل", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اختر الطيار المسؤول عن توصيل أوردر #${order.orderNumber}:", fontSize = 12.sp)

                    if (drivers.isEmpty()) {
                        Text("لا يوجد موصلين مسجلين بالنظام حالياً.", fontSize = 12.sp, color = StatusDanger)
                    } else {
                        MouseLazyColumn(modifier = Modifier.fillMaxWidth().height(180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(drivers) { d ->
                                Surface(
                                    color = if (order.driverId == d.id) SeaTealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().border(
                                        1.dp,
                                        if (order.driverId == d.id) SeaTealPrimary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = d.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = "هاتف: ${d.phone} | عمولة: ${d.ratePerOrder} ج", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Button(
                                            onClick = {
                                                viewModel?.assignDriverToOrder(order.id, d.id, d.name, d.ratePerOrder)
                                                showDriverSelectionDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("تعيين", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showDriverSelectionDialog = false
                            showQuickAddDriverDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة موصل (طيار) جديد فوراً")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showDriverSelectionDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Quick Add Driver Dialog
    if (showQuickAddDriverDialog) {
        AlertDialog(
            onDismissRequest = { showQuickAddDriverDialog = false },
            title = { Text("إضافة طيار جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newDriverName,
                        onValueChange = { newDriverName = it },
                        label = { Text("اسم الطيار *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDriverPhone,
                        onValueChange = { newDriverPhone = it },
                        label = { Text("رقم الهاتف *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDriverCommissionStr,
                        onValueChange = { newDriverCommissionStr = it },
                        label = { Text("العمولة لكل أوردر (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDriverName.isNotBlank() && newDriverPhone.isNotBlank()) {
                            val rate = newDriverCommissionStr.toDoubleOrNull() ?: 15.0
                            viewModel?.saveDriver(
                                DeliveryDriver(
                                    name = newDriverName.trim(),
                                    phone = newDriverPhone.trim(),
                                    calculationType = "COMMISSION",
                                    ratePerOrder = rate,
                                    fixedDailySalary = 0.0
                                )
                            )
                            showQuickAddDriverDialog = false
                            showDriverSelectionDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("حفظ الطيار")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showQuickAddDriverDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showKitchenReprintPassword) {
        PasswordPromptDialog(
            title = "إعادة طباعة أمر التشغيل 🔒",
            message = "طُبع أمر تشغيل المطبخ والاستاف مسبقاً لهذا الطلب (رقم #${order.orderNumber}). لإعادة طباعته مرة أخرى لمنع تكرار تحضير الأصناف بالخطأ، يجب إدخال كلمة سر الإدارة.",
            onConfirm = { pass ->
                scope.launch {
                    val ok = viewModel?.verifyManagerPassword(pass) ?: (pass == "1234")
                    if (ok) {
                        showKitchenReprintPassword = false
                        onPrintToTarget(selectedTab, reprintReason.ifBlank { "إعادة طباعة أمر التشغيل بتصريح الإدارة" })
                    } else {
                        viewModel?.userMessage?.value = "كلمة مرور الإدارة غير صحيحة ✗"
                    }
                }
            },
            onDismiss = { showKitchenReprintPassword = false }
        )
    }
}

@Composable
fun PaymentSettlementDialog(
    orderWithItems: OrderWithItems,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val liveOrderState by viewModel.getOrderByIdFlow(orderWithItems.order.id)
        .collectAsStateWithLifecycle(initialValue = orderWithItems)
    val currentOrder = liveOrderState?.order ?: orderWithItems.order

    var paidAmountStr by remember(currentOrder.paidAmount) {
        mutableStateOf(if (currentOrder.paidAmount > 0.0) String.format(Locale.US, "%.1f", currentOrder.paidAmount) else "")
    }
    var isPaid by remember(currentOrder.isPaid) {
        mutableStateOf(currentOrder.isPaid || (currentOrder.paidAmount >= currentOrder.totalAmount && currentOrder.totalAmount > 0))
    }

    val paidVal = paidAmountStr.toDoubleOrNull() ?: 0.0
    val total = currentOrder.totalAmount
    val change = if (paidVal > total) (paidVal - total) else 0.0
    val remaining = if (paidVal < total && !isPaid) (total - paidVal) else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💵 سداد وحساب أوردر #${currentOrder.orderNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = SeaNavyDeep
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
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
                // Total Summary Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SeaNavyDeep.copy(alpha = 0.07f),
                    border = BorderStroke(1.dp, SeaNavyDeep.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الإجمالي النهائي المطلوب:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", total)} ج.م",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SeaNavyDeep
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isPaid || (paidVal >= total && total > 0)) StatusSuccess else StatusWarning
                        ) {
                            Text(
                                text = if (isPaid || (paidVal >= total && total > 0)) "✓ خالص بالكامل" else "معلق / غير خالص",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Quick ONE-TAP Full Payment Button
                Button(
                    onClick = {
                        paidAmountStr = String.format(Locale.US, "%.1f", total)
                        isPaid = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "✅ الأوردر خالص بالكامل (${String.format(Locale.US, "%.1f", total)} ج)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Paid Amount Input
                OutlinedTextField(
                    value = paidAmountStr,
                    onValueChange = { input ->
                        paidAmountStr = input
                        val p = input.toDoubleOrNull() ?: 0.0
                        if (p >= total && total > 0) {
                            isPaid = true
                        }
                    },
                    label = { Text("المبلغ المدفوع من العميل (ج.م)") },
                    placeholder = { Text("مثال: 500 أو 200") },
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
                        total,
                        50.0, 100.0, 150.0, 200.0, 300.0, 400.0, 500.0, 1000.0
                    ).distinct().sorted()

                    items(suggestions) { amt ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (paidVal == amt) SeaTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (paidVal == amt) SeaTealPrimary else Color.LightGray),
                            modifier = Modifier.clickable {
                                paidAmountStr = String.format(Locale.US, "%.1f", amt)
                                if (amt >= total && total > 0) {
                                    isPaid = true
                                }
                            }
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.0f", amt)} ج",
                                color = if (paidVal == amt) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Calculation Result Card (Change vs Remaining)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (paidVal >= total && total > 0) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, if (paidVal >= total && total > 0) Color(0xFFBBF7D0) else Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (paidVal >= total && total > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الباقي المستحق للعميل:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", change)} ج.م",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        } else if (paidVal > 0.0 && paidVal < total) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("المتبقي على العميل (دين/آجل):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFB45309))
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", remaining)} ج.م",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        } else {
                            Text("أدخل المبلغ المدفوع أو اضغط 'الأوردر خالص بالكامل'", fontSize = 11.sp, color = Color.Gray)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isPaid = !isPaid }
                        ) {
                            Checkbox(checked = isPaid, onCheckedChange = { isPaid = it })
                            Text(
                                text = "تحديد الأوردر كـ خالص بالكامل",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPaid = paidVal
                    val finalChange = change
                    viewModel.updateOrderPayment(currentOrder.id, finalPaid, finalChange, isPaid)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ الحساب", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

