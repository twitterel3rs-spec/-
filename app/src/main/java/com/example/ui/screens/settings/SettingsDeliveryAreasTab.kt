package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import java.util.Locale
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import com.example.ui.components.MouseLazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeliveryArea
import com.example.ui.components.PasswordPromptDialog
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsDeliveryAreasTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val areas by viewModel.allDeliveryAreas.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showNewAreaDialog by remember { mutableStateOf(false) }
    var areaToEdit by remember { mutableStateOf<DeliveryArea?>(null) }
    var areaToDelete by remember { mutableStateOf<DeliveryArea?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SeaTealPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مناطق ورسوم التوصيل 🛵",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "تحديد رسوم التوصيل الافتراضية وعمولة الموصل لكل حي ومنطقة لضبط الحسابات والأوردرات.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { showNewAreaDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة منطقة")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "المناطق المسجلة (${areas.size}):",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        MouseLazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(areas) { area ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (area.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = area.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (area.isActive) MaterialTheme.colorScheme.onSurface else Color.Gray
                                )
                                if (!area.isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "(معطلة)", color = StatusDanger, fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "رسوم التوصيل: ${String.format(Locale.US, "%.0f", area.deliveryFee)} ج.م",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SeaTealPrimary
                                )
                                Text(
                                    text = "عمولة الكابتن: ${String.format(Locale.US, "%.0f", area.defaultDriverCommission)} ج.م",
                                    fontSize = 12.sp,
                                    color = AmberCoral
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { areaToEdit = area }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = SeaTealPrimary)
                            }
                            IconButton(onClick = { areaToDelete = area }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StatusDanger)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add or Edit Dialog
    if (showNewAreaDialog || areaToEdit != null) {
        val currentArea = areaToEdit
        var name by remember { mutableStateOf(currentArea?.name ?: "") }
        var feeStr by remember { mutableStateOf(if (currentArea != null) String.format(Locale.US, "%.0f", currentArea.deliveryFee) else "20") }
        var commStr by remember { mutableStateOf(if (currentArea != null) String.format(Locale.US, "%.0f", currentArea.defaultDriverCommission) else "15") }
        var isActive by remember { mutableStateOf(currentArea?.isActive ?: true) }

        AlertDialog(
            onDismissRequest = {
                showNewAreaDialog = false
                areaToEdit = null
            },
            title = {
                Text(
                    text = if (currentArea == null) "إضافة منطقة توصيل جديدة" else "تعديل منطقة: ${currentArea.name}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المنطقة أو الحي") },
                        placeholder = { Text("مثال: حي الأربعين / حي السويس / بورفؤاد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = feeStr,
                        onValueChange = { feeStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("رسوم التوصيل الافتراضية (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = commStr,
                        onValueChange = { commStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("عمولة كابتن التوصيل الافتراضية (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "المنطقة مفعلة ومتاحة للأوردرات:")
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fee = feeStr.toDoubleOrNull() ?: 20.0
                        val comm = commStr.toDoubleOrNull() ?: 15.0
                        if (name.isNotBlank()) {
                            viewModel.saveDeliveryArea(
                                DeliveryArea(
                                    id = currentArea?.id ?: 0L,
                                    name = name.trim(),
                                    deliveryFee = fee,
                                    defaultDriverCommission = comm,
                                    isActive = isActive
                                )
                            )
                            showNewAreaDialog = false
                            areaToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showNewAreaDialog = false
                    areaToEdit = null
                }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Area Dialog with Manager Password Protection
    areaToDelete?.let { targetArea ->
        PasswordPromptDialog(
            title = "تأكيد حذف منطقة التوصيل: ${targetArea.name}",
            message = "حذف منطقة التوصيل يتطلب إدخال كلمة سر الإدارة.",
            onConfirm = { pass ->
                scope.launch {
                    val ok = viewModel.verifyManagerPassword(pass)
                    if (ok) {
                        viewModel.deleteDeliveryArea(targetArea.id)
                        areaToDelete = null
                    } else {
                        viewModel.userMessage.value = "كلمة مرور الإدارة غير صحيحة"
                    }
                }
            },
            onDismiss = { areaToDelete = null }
        )
    }
}
