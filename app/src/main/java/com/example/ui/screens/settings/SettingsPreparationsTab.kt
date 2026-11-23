package com.example.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.ui.components.MouseLazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.PreparationType
import com.example.ui.components.PasswordPromptDialog
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsPreparationsTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val preparations by viewModel.allPreparationsIncludingInactive.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle(emptyList())
    val activeCategoryNames = remember(categories) {
        categories.filter { !it.isArchived && it.name.isNotBlank() }.map { it.name.trim() }.distinct()
    }

    var prepToEdit by remember { mutableStateOf<PreparationType?>(null) }
    var showNewPrepDialog by remember { mutableStateOf(false) }
    var prepToDelete by remember { mutableStateOf<PreparationType?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "طرق تجهيز وطهي الفريش والأسماك:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "تشمل التنظيف والشوي والقلي والزيت والليمون والسنجاري مع أسعارها المضافة تلقائياً للكيلو.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showNewPrepDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_prep_type_button")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("إضافة طريقة تجهيز أو طهي جديدة", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        MouseLazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(preparations) { prep ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (prep.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    shape = RoundedCornerShape(10.dp),
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
                                    Icon(
                                        imageVector = Icons.Default.Restaurant,
                                        contentDescription = null,
                                        tint = if (prep.isActive) AmberCoral else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = prep.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    if (!prep.isActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color.Gray.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "معطلة",
                                                fontSize = 9.sp,
                                                color = Color.DarkGray,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "تنطبق على: ${when(prep.applicableCategory) {
                                        "ALL" -> "جميع الأسماك والمأكولات البحرية"
                                        "فريش" -> "الأسماك الفريش فقط"
                                        "جمبريات" -> "الجمبريات فقط"
                                        "ني" -> "الني فقط"
                                        else -> prep.applicableCategory
                                    }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+${String.format(Locale.US, "%.1f", prep.additionalPricePerUnit)} ج.م",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AmberCoral
                                )
                                Text(
                                    text = "لكل كيلو",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = prep.isActive,
                                    onCheckedChange = { active ->
                                        viewModel.savePreparation(prep.copy(isActive = active))
                                    }
                                )
                                Text(text = if (prep.isActive) "مفعلة في شاشة البيع" else "معطلة مؤقتاً", fontSize = 11.sp)
                            }

                            Row {
                                TextButton(onClick = { prepToEdit = prep }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("تعديل السعر والاسم", fontSize = 11.sp)
                                }
                                IconButton(onClick = { prepToDelete = prep }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Preparation Dialog
    if (showNewPrepDialog || prepToEdit != null) {
        val existing = prepToEdit
        PreparationEditDialog(
            prep = existing,
            activeCategories = activeCategoryNames,
            onSave = { updated ->
                viewModel.savePreparation(updated)
                showNewPrepDialog = false
                prepToEdit = null
            },
            onDismiss = {
                showNewPrepDialog = false
                prepToEdit = null
            }
        )
    }

    // Delete Preparation Dialog with Password
    prepToDelete?.let { targetPrep ->
        PasswordPromptDialog(
            title = "تأكيد حذف طريقة التجهيز: ${targetPrep.name}",
            message = "حذف طريقة التجهيز نهائياً يتطلب كلمة سر الإدارة.",
            onConfirm = { pass ->
                scope.launch {
                    val ok = viewModel.verifyManagerPassword(pass)
                    if (ok) {
                        viewModel.deletePreparation(targetPrep.id)
                        prepToDelete = null
                    } else {
                        viewModel.userMessage.value = "كلمة مرور الإدارة غير صحيحة"
                    }
                }
            },
            onDismiss = { prepToDelete = null }
        )
    }
}

@Composable
fun PreparationEditDialog(
    prep: PreparationType?,
    activeCategories: List<String> = emptyList(),
    onSave: (PreparationType) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(prep?.name ?: "") }
    var priceStr by remember { mutableStateOf(if (prep != null) prep.additionalPricePerUnit.toString() else "15.0") }
    var categoryScope by remember { mutableStateOf(prep?.applicableCategory ?: "ALL") }
    var isActive by remember { mutableStateOf(prep?.isActive ?: true) }

    val scopes = remember(activeCategories) {
        listOf("ALL" to "كل الأصناف") + activeCategories.map { it to "قسم $it" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (prep == null) "إضافة طريقة تجهيز وطهي" else "تعديل: ${prep.name}",
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
                    label = { Text("اسم التجهيز (مثلاً: شوي سنجاري إسكندراني)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("السعر المضاف لكل كيلو (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "ربط بالأصناف المناسبة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    scopes.forEach { (scKey, scLabel) ->
                        FilterChip(
                            selected = categoryScope == scKey,
                            onClick = { categoryScope = scKey },
                            label = { Text(scLabel, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isActive = !isActive },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text(text = "مفعلة وتظهر في شاشة البيع", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceStr.toDoubleOrNull() ?: 15.0
                    if (name.isNotBlank()) {
                        onSave(
                            PreparationType(
                                id = prep?.id ?: 0L,
                                name = name.trim(),
                                additionalPricePerUnit = p,
                                isActive = isActive,
                                applicableCategory = categoryScope
                            )
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
