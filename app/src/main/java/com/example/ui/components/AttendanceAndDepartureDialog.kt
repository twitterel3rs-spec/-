package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Employee
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.util.Time12hUtil
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceAndDepartureDialog(
    employee: Employee,
    viewModel: MainViewModel,
    initialMode: String = "CHECK_IN", // "CHECK_IN", "CHECK_OUT", or "ABSENCE"
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val savedOwnerPhone by viewModel.ownerWhatsAppPhone.collectAsStateWithLifecycle()

    var activeMode by remember { mutableStateOf(initialMode) } // "CHECK_IN", "CHECK_OUT", "ABSENCE"

    // Standard 12-Hour Time calculations (AM/PM - ص / م)
    val now = Calendar.getInstance()
    val initialRawH = now.get(Calendar.HOUR)
    val initialH12 = if (initialRawH == 0) 12 else initialRawH
    val initialMinute = now.get(Calendar.MINUTE)
    val initialIsPm = now.get(Calendar.AM_PM) == Calendar.PM

    var hour12Str by remember { mutableStateOf(String.format(Locale.US, "%02d", initialH12)) }
    var minuteStr by remember { mutableStateOf(String.format(Locale.US, "%02d", initialMinute)) }
    var isPm by remember { mutableStateOf(initialIsPm) }

    // Scheduled Shift Start & End Times (12h format e.g. "09:00 ص", with live editing capability)
    var scheduledStartTime by remember { mutableStateOf(Time12hUtil.format12h(employee.shiftStartTime.ifBlank { "09:00 ص" })) }
    var scheduledEndTime by remember { mutableStateOf(Time12hUtil.format12h(employee.shiftEndTime.ifBlank { "09:00 م" })) }
    var isEditingSchedule by remember { mutableStateOf(false) }

    // 12-Hour Minutes from Midnight calculation
    val actualMinutes = remember(hour12Str, minuteStr, isPm) {
        val h = (hour12Str.toIntOrNull() ?: initialH12).coerceIn(1, 12)
        val m = (minuteStr.toIntOrNull() ?: initialMinute).coerceIn(0, 59)
        val h24 = if (isPm) {
            if (h == 12) 12 else h + 12
        } else {
            if (h == 12) 0 else h
        }
        h24 * 60 + m
    }

    val scheduledStartMinutes = remember(scheduledStartTime) {
        Time12hUtil.parseTimeToMinutes12h(scheduledStartTime)
    }

    val delayMinutes = remember(actualMinutes, scheduledStartMinutes, activeMode) {
        if (activeMode == "CHECK_IN") {
            (actualMinutes - scheduledStartMinutes).coerceAtLeast(0)
        } else {
            0
        }
    }

    // Late Action Selection: "PARDON", "PENALTY", "OWNER_NOTIFIED", or null
    var selectedLateAction by remember { mutableStateOf<String?>(null) }
    var penaltyAmountStr by remember { mutableStateOf("50") }

    // Absence State
    var absenceReasonType by remember { mutableStateOf("UNAUTHORIZED") } // "UNAUTHORIZED", "EXCUSED", "SICK", "OFFICIAL"
    var absencePenaltyType by remember { mutableStateOf("NONE") } // "NONE", "FULL_DAY", "CUSTOM"
    var absenceCustomPenaltyStr by remember { mutableStateOf("100") }
    var notifyOwnerForAbsence by remember { mutableStateOf(false) }

    // Owner phone handling
    var ownerPhoneInput by remember { mutableStateOf(savedOwnerPhone) }
    var showOwnerPhoneInput by remember { mutableStateOf(false) }

    // Departure hours & overtime calculation
    var hoursWorkedStr by remember { mutableStateOf(employee.dailyWorkHours.toString()) }
    var overtimeHoursStr by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(savedOwnerPhone) {
        if (ownerPhoneInput.isBlank()) {
            ownerPhoneInput = savedOwnerPhone
        }
    }

    // Auto set penalty amount based on employee delay hourly rate if configured
    LaunchedEffect(delayMinutes) {
        if (delayMinutes > 0 && selectedLateAction == null) {
            val autoPenalty = if (employee.delayHourlyDeductionRate > 0) {
                (employee.delayHourlyDeductionRate * (delayMinutes / 60.0)).coerceAtLeast(10.0)
            } else 50.0
            penaltyAmountStr = String.format(Locale.US, "%.0f", autoPenalty)
        }
    }

    val actualTimeString = remember(hour12Str, minuteStr, isPm) {
        val h = (hour12Str.toIntOrNull() ?: initialH12).coerceIn(1, 12)
        val m = (minuteStr.toIntOrNull() ?: initialMinute).coerceIn(0, 59)
        Time12hUtil.build12hString(h, m, isPm)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "تسجيل الحضور والغياب (نظام 12 ساعة)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "الموظف: ${employee.name} (${employee.role})",
                        style = MaterialTheme.typography.bodySmall,
                        color = SeaTealPrimary,
                        fontWeight = FontWeight.Bold
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
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mode Toggle: Check-In vs Check-Out vs Absence (3 Options)
                item {
                    TabRow(
                        selectedTabIndex = when (activeMode) {
                            "CHECK_OUT" -> 1
                            "ABSENCE" -> 2
                            else -> 0
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = activeMode == "CHECK_IN",
                            onClick = { activeMode = "CHECK_IN" },
                            text = {
                                Text("حضور ⏰", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        )
                        Tab(
                            selected = activeMode == "CHECK_OUT",
                            onClick = { activeMode = "CHECK_OUT" },
                            text = {
                                Text("انصراف 🚪", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        )
                        Tab(
                            selected = activeMode == "ABSENCE",
                            onClick = { activeMode = "ABSENCE" },
                            text = {
                                Text("غياب 🚫", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusDanger)
                            }
                        )
                    }
                }

                // ==========================================
                // MODE: ABSENCE (تسجيل غياب)
                // ==========================================
                if (activeMode == "ABSENCE") {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, StatusDanger),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "إثبات غياب الموظف لليوم:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = StatusDanger
                                    )
                                }

                                Text(
                                    text = "سبب الغياب:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = absenceReasonType == "UNAUTHORIZED",
                                        onClick = { absenceReasonType = "UNAUTHORIZED" },
                                        label = { Text("بدون إذن", fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = absenceReasonType == "EXCUSED",
                                        onClick = { absenceReasonType = "EXCUSED" },
                                        label = { Text("بعذر مسبق", fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = absenceReasonType == "SICK",
                                        onClick = { absenceReasonType = "SICK" },
                                        label = { Text("مرضي", fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider(color = StatusDanger.copy(alpha = 0.2f))

                                Text(
                                    text = "إجراء الخصم المالي للغياب:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )

                                val calculatedDaySalary = if (employee.salaryType == "DAILY") employee.basicSalary else (employee.basicSalary / 30.0)

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilterChip(
                                            selected = absencePenaltyType == "NONE",
                                            onClick = { absencePenaltyType = "NONE" },
                                            label = { Text("بدون أي خصم", fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = absencePenaltyType == "CUSTOM",
                                            onClick = { absencePenaltyType = "CUSTOM" },
                                            label = { Text("مبلغ خصم محدد", fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    FilterChip(
                                        selected = absencePenaltyType == "FULL_DAY",
                                        onClick = { absencePenaltyType = "FULL_DAY" },
                                        label = {
                                            Text(
                                                "خصم يومية كاملة (${String.format(Locale.US, "%.0f", calculatedDaySalary)} ج.م)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (absencePenaltyType == "CUSTOM") {
                                    OutlinedTextField(
                                        value = absenceCustomPenaltyStr,
                                        onValueChange = { absenceCustomPenaltyStr = it },
                                        label = { Text("قيمة الخصم / الجزاء (ج.م)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // WhatsApp Option for Absence
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray),
                                    modifier = Modifier.fillMaxWidth().clickable { notifyOwnerForAbsence = !notifyOwnerForAbsence }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.Checkbox(
                                            checked = notifyOwnerForAbsence,
                                            onCheckedChange = { notifyOwnerForAbsence = it }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(text = "إبلاغ الأونر بغياب الموظف عبر واتساب 🐦", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF15803D))
                                            Text(text = "إرسال إشعار فوري للأونر عبر واتساب", fontSize = 9.5.sp, color = Color.Gray)
                                        }
                                    }
                                }

                                if (notifyOwnerForAbsence) {
                                    OutlinedTextField(
                                        value = ownerPhoneInput,
                                        onValueChange = { ownerPhoneInput = it },
                                        label = { Text("رقم هاتف الأونر (واتساب)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // MODE: CHECK_IN & CHECK_OUT (12-HOUR SYSTEM)
                // ==========================================
                if (activeMode != "ABSENCE") {
                    // Scheduled Shift Time Banner (With instant 12h editing)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (activeMode == "CHECK_IN") "موعد الحضور المقرر (نظام 12 ساعة):" else "موعد الانصراف المقرر (12 ساعة):",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                    TextButton(
                                        onClick = { isEditingSchedule = !isEditingSchedule },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEditingSchedule) "تم" else "تعديل الموعد", fontSize = 11.sp)
                                    }
                                }

                                if (!isEditingSchedule) {
                                    Text(
                                        text = if (activeMode == "CHECK_IN") "⏰ يبدأ الوردية الساعة: ${Time12hUtil.format12h(scheduledStartTime)} (الرسمية)" else "🚪 ينتهي الساعة: ${Time12hUtil.format12h(scheduledEndTime)}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF14532D),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = scheduledStartTime,
                                            onValueChange = { scheduledStartTime = it },
                                            label = { Text("الحضور (12 ساعة)") },
                                            placeholder = { Text("مثلاً 09:00 ص") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = scheduledEndTime,
                                            onValueChange = { scheduledEndTime = it },
                                            label = { Text("الانصراف (12 ساعة)") },
                                            placeholder = { Text("مثلاً 09:00 م") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.saveEmployee(employee.copy(shiftStartTime = scheduledStartTime, shiftEndTime = scheduledEndTime))
                                            isEditingSchedule = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    ) {
                                        Text("حفظ الموعد للموظف في قاعدة البيانات ✓", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Exact 12-Hour & Minute Selection with AM/PM (Clean spacious non-overlapping layout)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (activeMode == "CHECK_IN") "وقت الحضور الفعلي الآن (نظام 12 ساعة):" else "وقت الانصراف الفعلي (نظام 12 ساعة):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = SeaNavyDeep
                                )

                                // Time Fields Row: Hour : Minute + AM/PM Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = hour12Str,
                                        onValueChange = { hour12Str = it },
                                        label = { Text("الساعة") },
                                        placeholder = { Text("1-12") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = ":",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        color = SeaNavyDeep
                                    )

                                    OutlinedTextField(
                                        value = minuteStr,
                                        onValueChange = { minuteStr = it },
                                        label = { Text("الدقيقة") },
                                        placeholder = { Text("00-59") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // AM / PM Segmented Controls
                                    Column(
                                        modifier = Modifier.weight(1.3f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            color = if (!isPm) SeaTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (!isPm) SeaTealPrimary else Color.LightGray
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isPm = false }
                                        ) {
                                            Text(
                                                text = "☀️ صباحاً (ص)",
                                                fontSize = 10.5.sp,
                                                fontWeight = if (!isPm) FontWeight.Bold else FontWeight.Normal,
                                                color = if (!isPm) Color.White else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }

                                        Surface(
                                            color = if (isPm) AmberCoral else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isPm) AmberCoral else Color.LightGray
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isPm = true }
                                        ) {
                                            Text(
                                                text = "🌙 مساءً (م)",
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isPm) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isPm) Color.White else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                // Quick Button: Set to Current Exact Time
                                OutlinedButton(
                                    onClick = {
                                        val c = Calendar.getInstance()
                                        val raw = c.get(Calendar.HOUR)
                                        hour12Str = String.format(Locale.US, "%02d", if (raw == 0) 12 else raw)
                                        minuteStr = String.format(Locale.US, "%02d", c.get(Calendar.MINUTE))
                                        isPm = c.get(Calendar.AM_PM) == Calendar.PM
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = SeaTealPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ضبط على الوقت الحالي الآن ⏰", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                // Result Badge
                                Surface(
                                    color = SeaTealPrimary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⏰ الوقت المسجل بنظام 12 ساعة: $actualTimeString",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SeaTealPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // LATE WARNING & 3 ACTION OPTIONS
                    if (activeMode == "CHECK_IN" && delayMinutes > 0) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, StatusDanger),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "الموظف ${employee.name} تأخر عن معاده بـ $delayMinutes دقائق!",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = StatusDanger
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "الموعد المقرر مسبقاً: $scheduledStartTime | وقت الحضور الفعلي: $actualTimeString",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF7F1D1D)
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = StatusDanger.copy(alpha = 0.3f))

                                    Text(
                                        text = "اختر الإجراء المطلوب للتأخير:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.Black
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Option 1: سماح (Pardon)
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedLateAction == "PARDON") Color(0xFFE0F2FE) else Color.White
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (selectedLateAction == "PARDON") 1.5.dp else 0.5.dp,
                                            if (selectedLateAction == "PARDON") SeaTealPrimary else Color.LightGray
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedLateAction = "PARDON" }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "🤝", fontSize = 18.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(text = "سماح (بدون تسجيل أي خصم أو جزاء)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(text = "تسجيل الحضور بتأخير مسموح به من الإدارة دون أي تأثير مالي.", fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Option 2: تسجيل جزا (Record Penalty)
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedLateAction == "PENALTY") Color(0xFFFFF1F2) else Color.White
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (selectedLateAction == "PENALTY") 1.5.dp else 0.5.dp,
                                            if (selectedLateAction == "PENALTY") StatusDanger else Color.LightGray
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedLateAction = "PENALTY" }
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "⚠️", fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(text = "تسجيل جزا مالي مباشر", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusDanger)
                                                    Text(text = "إضافة خصم جزاء آلياً في كشف حساب الموظف وراتبه.", fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }

                                            if (selectedLateAction == "PENALTY") {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedTextField(
                                                    value = penaltyAmountStr,
                                                    onValueChange = { penaltyAmountStr = it },
                                                    label = { Text("قيمة الجزاء المخصوم (ج.م)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Option 3: إبلاغ الأونر عبر الواتساب مع علامة عصفورة 🐦
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedLateAction == "OWNER_NOTIFIED") Color(0xFFF0FDF4) else Color.White
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (selectedLateAction == "OWNER_NOTIFIED") 1.5.dp else 0.5.dp,
                                            if (selectedLateAction == "OWNER_NOTIFIED") Color(0xFF16A34A) else Color.LightGray
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedLateAction = "OWNER_NOTIFIED"
                                                if (ownerPhoneInput.isBlank()) {
                                                    showOwnerPhoneInput = true
                                                }
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "🐦", fontSize = 20.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "إبلاغ الأونر عبر الواتساب 🐦",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.5.sp,
                                                        color = Color(0xFF15803D)
                                                    )
                                                    Text(
                                                        text = "إرسال تقرير فوري للأونر عبر واتساب يفيد بتأخير الموظف $delayMinutes دقيقة.",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }

                                            if (selectedLateAction == "OWNER_NOTIFIED" || showOwnerPhoneInput) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedTextField(
                                                    value = ownerPhoneInput,
                                                    onValueChange = {
                                                        ownerPhoneInput = it
                                                    },
                                                    label = { Text("رقم هاتف الأونر (واتساب)") },
                                                    placeholder = { Text("01xxxxxxxxx") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                    singleLine = true,
                                                    trailingIcon = {
                                                        if (ownerPhoneInput.isNotBlank()) {
                                                            IconButton(onClick = { viewModel.saveOwnerWhatsAppPhone(ownerPhoneInput) }) {
                                                                Icon(imageVector = Icons.Default.Save, contentDescription = "حفظ الرقم", tint = SeaTealPrimary)
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Text(
                                                    text = "يتم حفظ رقم الأونر في إعدادات التطبيق ليكون جاهزاً دائماً لكل الإشعارات.",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )

                                                Spacer(modifier = Modifier.height(6.dp))
                                                Button(
                                                    onClick = {
                                                        if (ownerPhoneInput.isNotBlank()) {
                                                            viewModel.saveOwnerWhatsAppPhone(ownerPhoneInput)
                                                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                                            viewModel.sendWhatsAppLateNotification(
                                                                ownerPhone = ownerPhoneInput,
                                                                employee = employee,
                                                                actualTime = actualTimeString,
                                                                delayMinutes = delayMinutes,
                                                                dateStr = dateStr,
                                                                context = context
                                                            )
                                                        }
                                                    },
                                                    enabled = ownerPhoneInput.isNotBlank(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("إرسال رسالة التأخير للأونر عبر واتساب 🐦", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // If Check-out: hours worked and overtime fields
                    if (activeMode == "CHECK_OUT") {
                        item {
                            OutlinedTextField(
                                value = hoursWorkedStr,
                                onValueChange = { hoursWorkedStr = it },
                                label = { Text("إجمالي ساعات العمل المنجزة اليوم") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = overtimeHoursStr,
                                onValueChange = { overtimeHoursStr = it },
                                label = { Text("ساعات العمل الإضافي (Overtime)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Notes input
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات إضافية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dateNow = System.currentTimeMillis()
                    when (activeMode) {
                        "CHECK_IN" -> {
                            val penalty = if (selectedLateAction == "PENALTY") (penaltyAmountStr.toDoubleOrNull() ?: 0.0) else 0.0
                            viewModel.recordEmployeeCheckIn(
                                employeeId = employee.id,
                                actualTimeString = actualTimeString,
                                delayMinutes = delayMinutes,
                                lateAction = selectedLateAction,
                                penaltyAmount = penalty,
                                notes = notes,
                                date = dateNow
                            )

                            if (selectedLateAction == "OWNER_NOTIFIED" && ownerPhoneInput.isNotBlank()) {
                                viewModel.saveOwnerWhatsAppPhone(ownerPhoneInput)
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                viewModel.sendWhatsAppLateNotification(
                                    ownerPhone = ownerPhoneInput,
                                    employee = employee,
                                    actualTime = actualTimeString,
                                    delayMinutes = delayMinutes,
                                    dateStr = dateStr,
                                    context = context
                                )
                            }
                        }
                        "CHECK_OUT" -> {
                            val hw = hoursWorkedStr.toDoubleOrNull() ?: employee.dailyWorkHours
                            val ot = overtimeHoursStr.toDoubleOrNull() ?: 0.0
                            viewModel.recordEmployeeCheckOut(
                                employeeId = employee.id,
                                departureTimeString = actualTimeString,
                                actualHoursWorked = hw,
                                overtimeHours = ot,
                                notes = notes,
                                date = dateNow
                            )
                        }
                        "ABSENCE" -> {
                            val reasonLabel = when (absenceReasonType) {
                                "UNAUTHORIZED" -> "غياب بدون إذن مسبق"
                                "EXCUSED" -> "غياب بعذر مقبول"
                                "SICK" -> "إجازة مرضية"
                                else -> "إجازة رسمية"
                            }
                            val finalNote = if (notes.isNotBlank()) "$reasonLabel - $notes" else reasonLabel
                            viewModel.recordEmployeeAbsence(employee.id, finalNote)

                            // Apply penalty deduction if requested
                            val calculatedDaySalary = if (employee.salaryType == "DAILY") employee.basicSalary else (employee.basicSalary / 30.0)
                            val penaltyAmount = when (absencePenaltyType) {
                                "FULL_DAY" -> calculatedDaySalary
                                "CUSTOM" -> absenceCustomPenaltyStr.toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            }
                            if (penaltyAmount > 0) {
                                viewModel.addEmployeeTransaction(
                                    employeeId = employee.id,
                                    type = "DEDUCTION",
                                    amount = penaltyAmount,
                                    reason = "خصم غياب: $reasonLabel",
                                    date = dateNow
                                )
                            }

                            // Notify Owner if requested
                            if (notifyOwnerForAbsence && ownerPhoneInput.isNotBlank()) {
                                viewModel.saveOwnerWhatsAppPhone(ownerPhoneInput)
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                try {
                                    val msg = "🚨 إشعار غياب موظف:\nالموظف: ${employee.name}\nالوظيفة: ${employee.role}\nالحالة: غائب اليوم ($reasonLabel)\nالتاريخ: $dateStr"
                                    val uri = android.net.Uri.parse("https://api.whatsapp.com/send?phone=${ownerPhoneInput.replace("[^0-9+]".toRegex(), "")}&text=${android.net.Uri.encode(msg)}")
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // ignore gracefully
                                }
                            }
                        }
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeMode == "ABSENCE") StatusDanger else SeaTealPrimary
                )
            ) {
                Text(
                    when (activeMode) {
                        "CHECK_IN" -> "حفظ الحضور (12 ساعة) ✓"
                        "CHECK_OUT" -> "حفظ الانصراف (12 ساعة) ✓"
                        else -> "تأكيد تسجيل الغياب لليوم ✓"
                    }
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
