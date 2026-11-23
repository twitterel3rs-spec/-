package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GeneralAccount
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuickDrawerTransactionDialog(
    currentDrawerBalance: Double,
    accounts: List<GeneralAccount> = emptyList(),
    onConfirm: (type: String, amount: Double, reason: String, accountId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    // "IN" (داخل للدرج) or "OUT" (خارج من الدرج)
    var txType by remember { mutableStateOf("OUT") } // الافتراضي هو المصروف/الخارج لأنها الأكثر تكراراً
    var amountStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    val parsedAmount = amountStr.toDoubleOrNull() ?: 0.0
    val expectedBalanceAfter = if (txType == "IN") {
        currentDrawerBalance + parsedAmount
    } else {
        currentDrawerBalance - parsedAmount
    }

    val outPresets = listOf(
        "ثلج 🧊",
        "بيبسي ومشروبات 🥤",
        "أكياس وتغليف 🛍️",
        "بنزين دليفري 🛵",
        "صيانة طارئة 🔧",
        "شاي وضيافة ☕",
        "سلفة عامل 👤",
        "نثريات ومصروفات 🧾"
    )

    val inPresets = listOf(
        "إيداع عهدة إضافية 💵",
        "توريد نقدية 📥",
        "استرداد مصروف 🔄",
        "مقبوضات خارجية 💼",
        "سداد حساب عميل 🤝"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        tint = SeaTealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "تسجيل معاملة سريعة بالدرج",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "حركة نقدية فورية تؤثر في رصيد الدرج وتقفيل الوردية",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .mouseVerticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Current Balance Banner
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, StatusSuccess.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💵 رصيد الدرج الفعلي الآن:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                        Text(
                            text = "${String.format(Locale.US, "%,.1f", currentDrawerBalance)} ج.م",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }

                // IN / OUT Toggle Selection
                Text(
                    text = "اتجاه المعاملة (داخل أم خارج):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // OUT Button
                    val isOut = txType == "OUT"
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOut) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isOut) 2.dp else 1.dp,
                            color = if (isOut) StatusDanger else Color.LightGray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { txType = "OUT" }
                            .testTag("quick_drawer_tx_type_out")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.RemoveCircle,
                                contentDescription = null,
                                tint = if (isOut) StatusDanger else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🔴 خارج من الدرج",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isOut) StatusDanger else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مصروف / نثريات / سلفة",
                                fontSize = 9.5.sp,
                                color = if (isOut) StatusDanger.copy(alpha = 0.8f) else Color.Gray
                            )
                        }
                    }

                    // IN Button
                    val isIn = txType == "IN"
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isIn) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isIn) 2.dp else 1.dp,
                            color = if (isIn) StatusSuccess else Color.LightGray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { txType = "IN" }
                            .testTag("quick_drawer_tx_type_in")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = if (isIn) StatusSuccess else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🟢 داخل للدرج",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isIn) StatusSuccess else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "إيداع / مقبوضات / عهدة",
                                fontSize = 9.5.sp,
                                color = if (isIn) StatusSuccess.copy(alpha = 0.8f) else Color.Gray
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (txType == "IN") "المبلغ الوارد للدرج (ج.م) *" else "المبلغ المصروف من الدرج (ج.م) *") },
                    placeholder = { Text("مثال: 50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_drawer_tx_amount_input")
                )

                // Quick Amount Increments Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val addAmounts = listOf(10, 20, 50, 100, 200, 500)
                    addAmounts.forEach { amt ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val current = amountStr.toDoubleOrNull() ?: 0.0
                                    amountStr = String.format(Locale.US, "%.0f", current + amt)
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+$amt", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Reason / Description Field
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("البيان أو السبب *") },
                    placeholder = { Text(if (txType == "IN") "مثال: إيداع عهدة إضافية أو مقبوضات" else "مثال: شراء ثلج، بيبسي، صيانة، كروت شحن...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_drawer_tx_reason_input")
                )

                // Fast preset chips for reason
                Text(
                    text = "أسباب سريعة شائعة (اضغط للتعبئة):",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentPresets = if (txType == "OUT") outPresets else inPresets
                    currentPresets.forEach { tag ->
                        Surface(
                            color = if (reason == tag) SeaTealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (reason == tag) SeaTealPrimary else Color.LightGray.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.clickable { reason = tag }
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = if (reason == tag) SeaTealPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Optional Account Link (if there are general accounts)
                if (accounts.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = accountMenuExpanded,
                        onExpandedChange = { accountMenuExpanded = it }
                    ) {
                        val currentAcc = accounts.firstOrNull { it.id == selectedAccountId }
                        OutlinedTextField(
                            value = currentAcc?.name ?: "مصروفات ونثريات المحل العامة (افتراضي)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الحساب المرتبط (اختياري)", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("مصروفات ونثريات المحل العامة (افتراضي)") },
                                onClick = {
                                    selectedAccountId = null
                                    accountMenuExpanded = false
                                }
                            )
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${acc.type})") },
                                    onClick = {
                                        selectedAccountId = acc.id
                                        accountMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                // Forecast Balance Preview
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (expectedBalanceAfter < 0) Color(0xFFFFEBEE) else Color(0xFFF1F8E9)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (txType == "IN") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (txType == "IN") StatusSuccess else StatusDanger,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الرصيد بعد هذه الحركة:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${String.format(Locale.US, "%,.1f", expectedBalanceAfter)} ج.م",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = if (expectedBalanceAfter < 0) StatusDanger else Color(0xFF1B5E20)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedAmount > 0.0 && reason.isNotBlank()) {
                        onConfirm(txType, parsedAmount, reason.trim(), selectedAccountId)
                    }
                },
                enabled = parsedAmount > 0.0 && reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (txType == "IN") StatusSuccess else AmberCoral
                ),
                modifier = Modifier.testTag("confirm_quick_drawer_tx_button")
            ) {
                Text(
                    text = if (txType == "IN") "تسجيل توريد للدرج 🟢" else "تسجيل صرف من الدرج 🔴",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء", fontSize = 12.sp)
            }
        }
    )
}
