package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AccountTransaction
import com.example.data.model.GeneralAccount
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

fun getAccountTypeLabel(type: String): String {
    return when (type) {
        "SUPPLIER" -> "مورد أسماك وخامات"
        "IMPORTER" -> "مستورد"
        "WHOLESALER" -> "تاجر جملة"
        "INDIVIDUAL" -> "شخص عادي / عميل آجل"
        "PARTNER" -> "شريك / رأس مال"
        "MAINTENANCE" -> "صيانة وتجهيزات"
        else -> "أخرى"
    }
}

fun getAccountTypeBadgeColor(type: String): Color {
    return when (type) {
        "SUPPLIER" -> SeaTealPrimary
        "IMPORTER" -> Color(0xFF673AB7)
        "WHOLESALER" -> Color(0xFF00897B)
        "INDIVIDUAL" -> Color(0xFF2E7D32)
        "PARTNER" -> AmberCoral
        "MAINTENANCE" -> Color(0xFFE65100)
        else -> Color.Gray
    }
}

@Composable
fun GeneralAccountEditDialog(
    account: GeneralAccount?,
    onSave: (GeneralAccount) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var phone by remember { mutableStateOf(account?.phone ?: "") }
    var address by remember { mutableStateOf(account?.address ?: "") }
    var type by remember { mutableStateOf(account?.type ?: "SUPPLIER") }
    var openingBalanceText by remember { mutableStateOf(if (account != null && account.openingBalance != 0.0) String.format(Locale.US, "%.1f", kotlin.math.abs(account.openingBalance)) else "") }
    var isCredit by remember { mutableStateOf(account == null || account.openingBalance >= 0.0) } // true: له (دائن), false: عليه (مدين)
    var notes by remember { mutableStateOf(account?.notes ?: "") }
    var showTypeDropdown by remember { mutableStateOf(false) }

    val accountTypes = listOf(
        "SUPPLIER" to "مورد أسماك وخامات",
        "IMPORTER" to "مستورد",
        "WHOLESALER" to "تاجر جملة",
        "INDIVIDUAL" to "شخص عادي / عميل آجل",
        "PARTNER" to "شريك / رأس مال",
        "MAINTENANCE" to "صيانة وخدمات",
        "OTHER" to "حساب عام آخر"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (account == null) "إضافة حساب جديد (مورد / مستورد / شخص)" else "تعديل بيانات الحساب",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الشخص أو الشركة / المورد *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = getAccountTypeLabel(type),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نوع الحساب") },
                        trailingIcon = {
                            IconButton(onClick = { showTypeDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTypeDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false }
                    ) {
                        accountTypes.forEach { (typeCode, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    type = typeCode
                                    showTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف / الواتساب") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان أو المقر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (account == null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("الرصيد الافتتاحي عند بدء التعامل:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = openingBalanceText,
                                onValueChange = { openingBalanceText = it },
                                label = { Text("المبلغ (ج.م)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCredit) AmberCoral.copy(alpha = 0.2f) else Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isCredit) AmberCoral else Color.LightGray),
                                    modifier = Modifier
                                        .clickable { isCredit = true }
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = "له (دائن)",
                                        fontSize = 11.sp,
                                        color = if (isCredit) AmberCoral else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (!isCredit) SeaTealPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (!isCredit) SeaTealPrimary else Color.LightGray),
                                    modifier = Modifier
                                        .clickable { isCredit = false }
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = "عليه (مدين)",
                                        fontSize = 11.sp,
                                        color = if (!isCredit) SeaTealPrimary else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية (أصناف التوريد / شروط الدفع)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val rawBalance = openingBalanceText.toDoubleOrNull() ?: 0.0
                    val finalOpeningBalance = if (isCredit) rawBalance else -rawBalance
                    val updated = (account ?: GeneralAccount(name = name)).copy(
                        name = name.trim(),
                        type = type,
                        phone = phone.trim(),
                        address = address.trim(),
                        openingBalance = if (account == null) finalOpeningBalance else account.openingBalance,
                        notes = notes.trim()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("حفظ الحساب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun GeneralAccountTransactionDialog(
    account: GeneralAccount,
    onConfirm: (type: String, amount: Double, description: String, invoiceNo: String, date: Long, isPaidFromDrawer: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    var txDateStr by remember { mutableStateOf(dateFormat.format(Date())) }
    var selectedType by remember { mutableStateOf("PAYMENT") } // "SUPPLY", "PAYMENT", "RECEIPT", "RETURN", "DISCOUNT"
    var amountText by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPaidFromDrawer by remember { mutableStateOf(true) }

    val txTypes = listOf(
        "PAYMENT" to ("سداد دفعة نقدية" to "سداد مبلغ للمورد (ينقص حسابه)"),
        "SUPPLY" to ("توريد بضاعة / فاتورة" to "استلام بضاعة/أسماك (يزيد حسابه)"),
        "RECEIPT" to ("قبض نقدية مستلمة" to "استلام مبالغ منه (تقليل دينه)"),
        "RETURN" to ("مرتجع بضاعة" to "إرجاع بضاعة غير مطابقة (تخفيض حسابه)"),
        "DISCOUNT" to ("خصم / تسوية" to "خصم مالي أو تسوية بالاتفاق")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "تسجيل حركة مالية جديدة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "الحساب: ${account.name} (${getAccountTypeLabel(account.type)})",
                    fontSize = 12.sp,
                    color = SeaTealPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = txDateStr,
                    onValueChange = { txDateStr = it },
                    label = { Text("تاريخ الحركة (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("نوع الحركة المالية:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                MouseLazyColumn(modifier = Modifier.heightIn(max = 160.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(txTypes) { (typeCode, labels) ->
                        val (title, subtitle) = labels
                        val isSelected = selectedType == typeCode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) SeaTealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) SeaTealPrimary else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedType = typeCode }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) SeaTealPrimary else Color.LightGray)
                                    )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(text = subtitle, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ المالي (ج.م) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("رقم الفاتورة أو إيصال الصرف/القبض (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("البيان / تفاصيل العملية *") },
                    placeholder = { Text("مثال: توريد 30 كجم قاروص طازج، دفعة نقدية كاش...") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == "PAYMENT" || selectedType == "RECEIPT") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPaidFromDrawer) SeaTealPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPaidFromDrawer) SeaTealPrimary else Color.LightGray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPaidFromDrawer = !isPaidFromDrawer }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isPaidFromDrawer,
                                onCheckedChange = { isPaidFromDrawer = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = if (selectedType == "PAYMENT") "صرف من درج الكاشير (خصم من عهدة الوردية)" else "توريد لدرج الكاشير (إضافة لعهدة الوردية)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (isPaidFromDrawer) SeaTealPrimary else Color.DarkGray
                                )
                                Text(
                                    text = "يؤثر تلقائياً على الرصيد الدفتري والفعلي للدرج عند تقفيل الوردية",
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
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0.0) return@Button
                    val finalDesc = description.ifBlank {
                        when (selectedType) {
                            "PAYMENT" -> "سداد دفعة نقدية"
                            "SUPPLY" -> "توريد بضاعة"
                            "RECEIPT" -> "قبض نقدية"
                            "RETURN" -> "مرتجع بضاعة"
                            "DISCOUNT" -> "خصم وتسوية"
                            else -> "حركة حساب"
                        }
                    }
                    val parsedDate = try { dateFormat.parse(txDateStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                    onConfirm(selectedType, amount, finalDesc, invoiceNumber.trim(), parsedDate, isPaidFromDrawer)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("تأكيد وتسجيل الحركة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun GeneralAccountStatementDialog(
    account: GeneralAccount,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var transactions by remember { mutableStateOf<List<AccountTransaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var txToDelete by remember { mutableStateOf<AccountTransaction?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US) }

    fun refresh() {
        scope.launch {
            isLoading = true
            transactions = viewModel.getTransactionsForAccount(account.id)
            isLoading = false
        }
    }

    LaunchedEffect(account.id) {
        refresh()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "كشف حساب: ${account.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${getAccountTypeLabel(account.type)} | هاتف: ${account.phone.ifBlank { "غير مسجل" }}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Balance summary card
                val isCredit = account.currentBalance >= 0.0
                val balanceColor = if (account.currentBalance == 0.0) StatusSuccess else if (isCredit) AmberCoral else Color(0xFF1565C0)
                val balanceStatusText = if (account.currentBalance == 0.0) "الرصيد متزن (0 ج.م)" else if (isCredit) "مستحق له (دائن): ${String.format(Locale.US, "%.1f", account.currentBalance)} ج.م" else "مطلوب منه (مدين): ${String.format(Locale.US, "%.1f", kotlin.math.abs(account.currentBalance))} ج.م"

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = balanceColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, balanceColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "الرصيد الصافي الحالي",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = balanceStatusText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor
                        )
                        if (account.openingBalance != 0.0) {
                            Text(
                                text = "الرصيد الافتتاحي: ${String.format(Locale.US, "%.1f", account.openingBalance)} ج.م",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "سجل الحركات المالية (${transactions.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (isLoading) {
                    Text("جاري تحميل الحركات...", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد حركات مالية مسجلة بعد لهذا الحساب",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    MouseLazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(transactions, key = { it.id }) { tx ->
                            val (txLabel, txColor, sign) = when (tx.type) {
                                "SUPPLY" -> Triple("توريد بضاعة", AmberCoral, "+")
                                "PAYMENT" -> Triple("سداد دفعة نقدية", SeaTealPrimary, "-")
                                "RECEIPT" -> Triple("قبض نقدية", StatusSuccess, "-")
                                "RETURN" -> Triple("مرتجع بضاعة", Color(0xFFC2185B), "-")
                                "DISCOUNT" -> Triple("خصم / تسوية", Color(0xFF7B1FA2), "-")
                                else -> Triple("حركة", Color.Gray, "")
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = txColor.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = txLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = txColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (tx.invoiceNumber.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "فاتورة #${tx.invoiceNumber}",
                                                    fontSize = 10.sp,
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }
                                        Text(
                                            text = tx.description,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        Text(
                                            text = dateFormat.format(Date(tx.date)),
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$sign${String.format(Locale.US, "%.1f", tx.amount)} ج.م",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = txColor
                                        )
                                        IconButton(
                                            onClick = { txToDelete = tx },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف الحركة",
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val sb = StringBuilder()
                            sb.append("📋 كشف حساب: ${account.name} (${getAccountTypeLabel(account.type)})\n")
                            sb.append("📞 هاتف: ${account.phone}\n")
                            sb.append("-----------------------------\n")
                            sb.append("الرصيد الحالي: ${String.format(Locale.US, "%.1f", account.currentBalance)} ج.م (${if (account.currentBalance >= 0) "له دائن" else "عليه مدين"})\n")
                            sb.append("-----------------------------\n")
                            sb.append("سجل الحركات:\n")
                            transactions.forEach { t ->
                                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(t.date))
                                sb.append("• [$dateStr] ${t.description} : ${t.amount} ج.م\n")
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, sb.toString())
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الحساب"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة الكشف", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تم", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    txToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("تأكيد حذف الحركة المالية") },
            text = { Text("هل أنت متأكد من حذف حركة \"${tx.description}\" بمبلغ ${tx.amount} ج.م؟ سيتم إعادة احتساب رصيد الحساب آلياً.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccountTransaction(tx.id, account.id)
                        txToDelete = null
                        refresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text("نعم، حذف الحركة")
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
