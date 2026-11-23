package com.example.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import com.example.ui.components.MouseLazyColumn
import com.example.ui.components.MouseLazyRow
import com.example.ui.components.mouseVerticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.printer.PrinterConfig
import com.example.printer.PrinterResult
import com.example.ui.components.PasswordPromptDialog
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsPrintersTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allPrinters by viewModel.allPrinters.collectAsStateWithLifecycle()
    val deviceRole by viewModel.deviceRole.collectAsStateWithLifecycle()
    val discoveredPrinters by viewModel.discoveredNetworkPrinters.collectAsStateWithLifecycle()
    val isScanningPrinters by viewModel.isScanningNetworkPrinters.collectAsStateWithLifecycle()

    var selectedPrinterId by remember { mutableStateOf("") }
    var currentConfig by remember { mutableStateOf<PrinterConfig?>(null) }

    var showAddPrinterDialog by remember { mutableStateOf(false) }
    var showClearAllPasswordPrompt by remember { mutableStateOf(false) }
    var printerToDelete by remember { mutableStateOf<PrinterConfig?>(null) }
    var showPasswordPromptForCashierUncheck by remember { mutableStateOf(false) }
    var pendingUncheckAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    var pairedBluetoothDevices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var connectedUsbPrinters by remember { mutableStateOf<List<com.example.printer.UsbPrinterInfo>>(emptyList()) }
    var testResult by remember { mutableStateOf<PrinterResult?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isDrawerTesting by remember { mutableStateOf(false) }

    // Bluetooth permission status
    var hasBtPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBtPerm = granted
        if (granted) {
            pairedBluetoothDevices = viewModel.getPairedBluetoothPrinters()
        }
    }

    // Sync selected printer configuration
    LaunchedEffect(allPrinters, selectedPrinterId) {
        if (allPrinters.isNotEmpty()) {
            val matched = allPrinters.firstOrNull { it.id == selectedPrinterId }
                ?: allPrinters.first()
            selectedPrinterId = matched.id
            currentConfig = matched
            testResult = null
        } else {
            selectedPrinterId = ""
            currentConfig = null
            testResult = null
        }
        if (hasBtPerm) {
            pairedBluetoothDevices = viewModel.getPairedBluetoothPrinters()
        }
    }

    MouseLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (deviceRole == "SELLER") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "👤 وضع جهاز البائع",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1E40AF)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "الطباعة والاتصال بالطابعات يتم إدارتها وتهيئتها على جهاز الكاشير.",
                                fontSize = 12.sp,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }
                }
            }
        }

        // Clean, uncluttered header with title and action buttons
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = SeaTealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "الطابعات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SeaNavyDeep
                        )
                        if (allPrinters.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = SeaTealPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${allPrinters.size}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SeaTealPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (allPrinters.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showClearAllPasswordPrompt = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                                border = BorderStroke(1.dp, StatusDanger),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = StatusDanger
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "مسح الكل",
                                    fontSize = 11.5.sp,
                                    color = StatusDanger,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { showAddPrinterDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إضافة طابعة جديدة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Empty State: clean and direct
        if (allPrinters.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "لا توجد طابعات مضافة حالياً",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SeaNavyDeep
                        )
                        Button(
                            onClick = { showAddPrinterDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة طابعة جديدة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Printer Selector Tabs
            item {
                val safePrinters = remember(allPrinters) { allPrinters.distinctBy { it.id.removePrefix("p_").trim() } }
                MouseLazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(safePrinters, key = { idx, p -> "settings_printer_${p.id}_$idx" }) { _, p ->
                        val isSelected = selectedPrinterId == p.id
                        val roleLabel = when (p.role) {
                            "CUSTOMER_INVOICE" -> "زبون 👤"
                            "KITCHEN_ORDER" -> "تشغيل 👨‍🍳"
                            "BOTH" -> "الاثنين 🔄"
                            else -> "زبون"
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) SeaTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) SeaTealPrimary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.clickable {
                                selectedPrinterId = p.id
                                currentConfig = p
                                testResult = null
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = p.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = roleLabel,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    if (p.isCashierDefault) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else AmberCoral.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "الكاشير ⭐",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else AmberCoral,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Printer Settings Card
            currentConfig?.let { config ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header: Name & Delete Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "إعدادات: ${config.displayName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { printerToDelete = config },
                                    colors = ButtonDefaults.textButtonColors(contentColor = StatusDanger)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حذف هذه الطابعة", fontSize = 11.5.sp)
                                }
                            }

                            // 1. Printer Name
                            OutlinedTextField(
                                value = config.displayName,
                                onValueChange = { currentConfig = config.copy(displayName = it) },
                                label = { Text("اسم الطابعة") },
                                placeholder = { Text("مثال: طابعة المشويات") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 2. Printer Role
                            Text(
                                text = "الغرض من الطابعة:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val roles = listOf(
                                    Pair("CUSTOMER_INVOICE", "فاتورة زبون 👤"),
                                    Pair("KITCHEN_ORDER", "أمر تشغيل مطبخ 👨‍🍳"),
                                    Pair("BOTH", "كلاهما معاً 🔄")
                                )
                                roles.forEach { (roleKey, roleLabel) ->
                                    val isSelected = config.role == roleKey
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (config.isCashierDefault && roleKey == "KITCHEN_ORDER") {
                                                pendingUncheckAction = {
                                                    currentConfig = config.copy(role = roleKey)
                                                }
                                                showPasswordPromptForCashierUncheck = true
                                            } else {
                                                currentConfig = config.copy(role = roleKey)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = roleLabel,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // 3. Status & Cashier Defaults
                            Surface(
                                color = if (config.isCashierDefault) AmberCoral.copy(alpha = 0.08f) else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (config.isCashierDefault) AmberCoral.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = AmberCoral,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "طابعة الكاشير الرئيسية (افتراضية الفواتير)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Checkbox(
                                            checked = config.isCashierDefault,
                                            onCheckedChange = { willCheck ->
                                                if (!willCheck) {
                                                    pendingUncheckAction = {
                                                        currentConfig = config.copy(isCashierDefault = false)
                                                    }
                                                    showPasswordPromptForCashierUncheck = true
                                                } else {
                                                    currentConfig = config.copy(isCashierDefault = true)
                                                }
                                            }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "تفعيل الطابعة في النظام",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Switch(
                                            checked = config.isEnabled,
                                            onCheckedChange = { willEnable ->
                                                if (!willEnable && config.isCashierDefault) {
                                                    pendingUncheckAction = {
                                                        currentConfig = config.copy(isEnabled = false)
                                                    }
                                                    showPasswordPromptForCashierUncheck = true
                                                } else {
                                                    currentConfig = config.copy(isEnabled = willEnable)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            // 4. Connection Type
                            Text(
                                text = "طريقة التوصيل:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val types = listOf(
                                    Triple("USB", "طابعة USB 🔌", Icons.Default.Usb),
                                    Triple("NETWORK_IP", "شبكة IP 🌐", Icons.Default.Lan),
                                    Triple("BLUETOOTH", "بلوتوث 🖨️", Icons.Default.Bluetooth),
                                    Triple("SYSTEM_SPOOLER", "نظام أندرويد 📄", Icons.Default.Share)
                                )
                                types.forEach { (typeKey, typeLabel, _) ->
                                    val isChosen = config.connectionType == typeKey
                                    FilterChip(
                                        selected = isChosen,
                                        onClick = { currentConfig = config.copy(connectionType = typeKey) },
                                        label = { Text(typeLabel, fontSize = 10.5.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // USB Direct Printing
                            if (config.connectionType == "USB") {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            connectedUsbPrinters = viewModel.getConnectedUsbPrinters()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("فحص طابعات الـ USB المتصلة 🔍", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (connectedUsbPrinters.isEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "🔌 توصيل طابعة الكاشير بالـ USB:",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = SeaNavyDeep
                                                )
                                                Text(
                                                    text = "• تأكد من توصيل كابل الـ USB الخاص بالطابعة وتشغيل زر الباور.\n• اضغط زر 'فحص طابعات الـ USB المتصلة' لاكتشافها تلقائياً.\n• عند ظهور رسالة إذن أندرويد، اضغط (موافق / Allow).",
                                                    fontSize = 10.5.sp,
                                                    lineHeight = 15.sp,
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "طابعات USB المكتشفة (${connectedUsbPrinters.size}):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        connectedUsbPrinters.forEach { usbInfo ->
                                            val isSel = config.usbDeviceName == usbInfo.deviceName ||
                                                    config.usbDeviceName == "${usbInfo.vendorId}:${usbInfo.productId}"
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSel) SeaTealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        currentConfig = config.copy(usbDeviceName = usbInfo.deviceName)
                                                        if (!usbInfo.hasPermission) {
                                                            viewModel.requestUsbPermission(usbInfo.usbDevice)
                                                        }
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = "🖨️ ${usbInfo.displayName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text(
                                                            text = "منفذ: ${usbInfo.deviceName} | VID:${Integer.toHexString(usbInfo.vendorId)}:PID:${Integer.toHexString(usbInfo.productId)}",
                                                            fontSize = 10.sp,
                                                            color = Color.Gray
                                                        )
                                                        if (usbInfo.hasPermission) {
                                                            Text(text = "صلاحية الوصول ممنوحة ✓", fontSize = 10.sp, color = StatusSuccess, fontWeight = FontWeight.Bold)
                                                        } else {
                                                            Text(text = "اضغط لطلب إذن الوصول 🔑", fontSize = 10.sp, color = StatusWarning)
                                                        }
                                                    }
                                                    if (isSel) {
                                                        Text(text = "مختارة ✓", color = SeaTealPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = config.usbDeviceName,
                                        onValueChange = { currentConfig = config.copy(usbDeviceName = it) },
                                        label = { Text("معرف منفذ USB (اختياري)") },
                                        placeholder = { Text("اتركه فارغاً للاكتشاف التلقائي") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Network IP
                            if (config.connectionType == "NETWORK_IP") {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = config.ipAddress,
                                            onValueChange = { currentConfig = config.copy(ipAddress = it) },
                                            label = { Text("عنوان IP") },
                                            placeholder = { Text("192.168.1.100") },
                                            singleLine = true,
                                            modifier = Modifier.weight(2f)
                                        )
                                        OutlinedTextField(
                                            value = config.port.toString(),
                                            onValueChange = { it.toIntOrNull()?.let { p -> currentConfig = config.copy(port = p) } },
                                            label = { Text("Port") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.scanNetworkPrinters() },
                                        enabled = !isScanningPrinters,
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isScanningPrinters) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("جارٍ فحص شبكة الراوتر...", fontSize = 11.sp)
                                        } else {
                                            Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("اكتشاف طابعات الشبكة تلقائياً", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (discoveredPrinters.isNotEmpty()) {
                                        Text(
                                            text = "الطابعات المكتشفة (${discoveredPrinters.size}):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        discoveredPrinters.forEach { p ->
                                            val isSel = config.ipAddress == p.ip && config.port == p.port
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSel) SeaTealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { currentConfig = config.copy(ipAddress = p.ip, port = p.port) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(text = "🖨️ ${p.displayName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text(text = "${p.ip}:${p.port}", fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                    if (isSel) {
                                                        Text(text = "مختارة ✓", color = SeaTealPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (config.connectionType == "BLUETOOTH") {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (pairedBluetoothDevices.isEmpty()) {
                                        Text(
                                            text = "لا توجد أجهزة بلوتوث مقترنة. يرجى التوجه لإعدادات الهاتف وعمل اقتران (Pair).",
                                            fontSize = 11.sp,
                                            color = StatusWarning
                                        )
                                    } else {
                                        pairedBluetoothDevices.forEach { (devName, devMac) ->
                                            val isSel = config.bluetoothMacOrName == devMac || config.bluetoothMacOrName == devName
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSel) SeaTealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { currentConfig = config.copy(bluetoothMacOrName = devMac) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(text = devName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text(text = devMac, fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                    if (isSel) {
                                                        Text(text = "مختارة ✓", color = SeaTealPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = config.bluetoothMacOrName,
                                        onValueChange = { currentConfig = config.copy(bluetoothMacOrName = it) },
                                        label = { Text("عنوان MAC للبلوتوث") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // 5. Paper Width & Print Mode
                            Text(
                                text = "مقاس الورق ونمط الطباعة:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = config.paperWidthMm == 80,
                                    onClick = { currentConfig = config.copy(paperWidthMm = 80) },
                                    label = { Text("80 مم (عريض)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = config.paperWidthMm == 58,
                                    onClick = { currentConfig = config.copy(paperWidthMm = 58) },
                                    label = { Text("58 مم (صغير)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = config.printMode == "IMAGE",
                                    onClick = { currentConfig = config.copy(printMode = "IMAGE") },
                                    label = { Text("🖼️ صورة عالية الدقة (عربي 100%)", fontSize = 10.5.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = config.printMode == "ESC_POS_TEXT",
                                    onClick = { currentConfig = config.copy(printMode = "ESC_POS_TEXT") },
                                    label = { Text("📝 نص حراري مباشر (ESC/POS)", fontSize = 10.5.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = config.customHeader,
                                onValueChange = { currentConfig = config.copy(customHeader = it) },
                                label = { Text("ترويسة مخصصة (اختياري)") },
                                placeholder = { Text("مثال: تذكرة تشغيل مشويات") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 6. Action Buttons: Save & Test
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.savePrinterConfig(config)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حفظ الإعدادات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.savePrinterConfig(config)
                                        isTesting = true
                                        testResult = null
                                        viewModel.testPrinter(config.id) { res ->
                                            isTesting = false
                                            testResult = res
                                        }
                                    },
                                    enabled = !isTesting,
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberCoral),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isTesting) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("جارٍ الاختبار...", fontSize = 11.sp)
                                    } else {
                                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("اختبار الطباعة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    viewModel.savePrinterConfig(config)
                                    isDrawerTesting = true
                                    testResult = null
                                    viewModel.openCashDrawer(config.id) { res ->
                                        isDrawerTesting = false
                                        testResult = res
                                    }
                                },
                                enabled = !isDrawerTesting,
                                colors = ButtonDefaults.buttonColors(containerColor = SeaNavyDeep),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isDrawerTesting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("جارٍ إرسال أمر فتح الدرج...", fontSize = 11.sp)
                                } else {
                                    Text("💵 تجربة فتح درج النقدية (Kick Drawer)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Test Result Banner
        testResult?.let { res ->
            item {
                when (res) {
                    is PrinterResult.Success -> {
                        Surface(
                            color = StatusSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "تم الاتصال والطباعة بنجاح ✓", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 13.sp)
                                    Text(text = res.message, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    is PrinterResult.Error -> {
                        Surface(
                            color = StatusDanger.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = StatusDanger)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "فشل الاتصال بالطابعة ✗", fontWeight = FontWeight.Bold, color = StatusDanger, fontSize = 13.sp)
                                    Text(text = res.errorMessage, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    is PrinterResult.PermissionRequired -> {
                        Surface(
                            color = StatusWarning.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "مطلوب صلاحية بلوتوث:", fontWeight = FontWeight.Bold, color = StatusWarning, fontSize = 13.sp)
                                Text(text = res.message, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(onClick = { btPermissionLauncher.launch(res.permissionName) }) {
                                    Text("منح الصلاحية الآن")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Add New Printer Dialog
    if (showAddPrinterDialog) {
        var newName by remember { mutableStateOf("طابعة ${allPrinters.size + 1}") }
        var newRole by remember { mutableStateOf(if (allPrinters.isEmpty()) "BOTH" else "CUSTOMER_INVOICE") }
        var newConnType by remember { mutableStateOf("NETWORK_IP") }
        var newIp by remember { mutableStateOf("192.168.1.100") }
        var newPortStr by remember { mutableStateOf("9100") }
        var newWidthMm by remember { mutableStateOf(80) }

        AlertDialog(
            onDismissRequest = { showAddPrinterDialog = false },
            title = { Text("إضافة طابعة جديدة", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .mouseVerticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("اسم الطابعة *") },
                        placeholder = { Text("مثال: طابعة المشويات") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "الغرض من الطابعة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = newRole == "CUSTOMER_INVOICE",
                            onClick = { newRole = "CUSTOMER_INVOICE" },
                            label = { Text("فاتورة زبون", fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = newRole == "KITCHEN_ORDER",
                            onClick = { newRole = "KITCHEN_ORDER" },
                            label = { Text("أمر تشغيل", fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = newRole == "BOTH",
                            onClick = { newRole = "BOTH" },
                            label = { Text("كلاهما معاً 🔄", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1.1f)
                        )
                    }

                    Text(text = "نوع الاتصال:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = newConnType == "USB",
                            onClick = { newConnType = "USB" },
                            label = { Text("طابعة USB", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = newConnType == "NETWORK_IP",
                            onClick = { newConnType = "NETWORK_IP" },
                            label = { Text("شبكة IP", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = newConnType == "BLUETOOTH",
                            onClick = { newConnType = "BLUETOOTH" },
                            label = { Text("بلوتوث", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = newConnType == "SYSTEM_SPOOLER",
                            onClick = { newConnType = "SYSTEM_SPOOLER" },
                            label = { Text("أندرويد", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (newConnType == "NETWORK_IP") {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newIp,
                                onValueChange = { newIp = it },
                                label = { Text("IP الطابعة") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = newPortStr,
                                onValueChange = { newPortStr = it },
                                label = { Text("المنفذ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(text = "عرض الورق:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = newWidthMm == 80, onClick = { newWidthMm = 80 }, label = { Text("80 مم (عريض)") })
                        FilterChip(selected = newWidthMm == 58, onClick = { newWidthMm = 58 }, label = { Text("58 مم (صغير)") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            val newId = "PRINTER_${System.currentTimeMillis()}"
                            val isFirstPrinter = allPrinters.isEmpty()
                            val newConfig = PrinterConfig(
                                id = newId,
                                displayName = newName.trim(),
                                role = newRole,
                                connectionType = newConnType,
                                ipAddress = newIp.trim(),
                                port = newPortStr.toIntOrNull() ?: 9100,
                                paperWidthMm = newWidthMm,
                                printMode = "IMAGE",
                                isEnabled = true,
                                isCashierDefault = isFirstPrinter
                            )
                            viewModel.savePrinterConfig(newConfig)
                            selectedPrinterId = newId
                            currentConfig = newConfig
                            showAddPrinterDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("إضافة وحفظ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddPrinterDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Password Prompt Dialog when attempting to UNCHECK Cashier Printer
    if (showPasswordPromptForCashierUncheck) {
        PasswordPromptDialog(
            title = "تأكيد تعديل طابعة الكاشير 🔒",
            message = "يرجى إدخال كلمة سر الإدارة لتأكيد تعديل طابعة الكاشير.",
            onConfirm = { pass ->
                scope.launch {
                    val ok = viewModel.verifyManagerPassword(pass)
                    if (ok) {
                        pendingUncheckAction?.invoke()
                        currentConfig?.let { viewModel.savePrinterConfig(it) }
                        showPasswordPromptForCashierUncheck = false
                        pendingUncheckAction = null
                        viewModel.userMessage.value = "تم تأكيد كلمة السر بنجاح ✓"
                    } else {
                        viewModel.userMessage.value = "كلمة مرور الإدارة غير صحيحة ✗"
                    }
                }
            },
            onDismiss = {
                showPasswordPromptForCashierUncheck = false
                pendingUncheckAction = null
            }
        )
    }

    // Delete Printer Dialog
    printerToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { printerToDelete = null },
            title = { Text("حذف الطابعة", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف ${target.displayName}؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePrinter(target.id)
                        printerToDelete = null
                        selectedPrinterId = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { printerToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    // Password-Protected Clear All Printers Dialog
    if (showClearAllPasswordPrompt) {
        PasswordPromptDialog(
            title = "تأكيد مسح كافة الطابعات 🔒",
            message = "يرجى إدخال كلمة مرور الإدارة لمسح جميع الطابعات للبدء من الصفر.",
            onConfirm = { pass ->
                scope.launch {
                    val ok = viewModel.verifyManagerPassword(pass)
                    if (ok) {
                        viewModel.clearAllPrinters()
                        showClearAllPasswordPrompt = false
                        selectedPrinterId = ""
                        currentConfig = null
                        viewModel.userMessage.value = "تم مسح كافة الطابعات بنجاح ✓"
                    } else {
                        viewModel.userMessage.value = "كلمة مرور الإدارة غير صحيحة ✗"
                    }
                }
            },
            onDismiss = { showClearAllPasswordPrompt = false }
        )
    }
}
