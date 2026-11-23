package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.data.model.EmployeeAttendance
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.util.Time12hUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceBoardTab(
    employees: List<Employee>,
    todayAttendance: List<EmployeeAttendance>,
    allAttendance: List<EmployeeAttendance>,
    savedOwnerPhone: String,
    onRecordAttendance: (Employee, mode: String) -> Unit,
    onViewStatement: (Employee) -> Unit,
    onSaveOwnerPhone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var boardFilter by remember { mutableStateOf("ALL") } // "ALL", "PRESENT", "LATE", "ABSENT", "PENDING"
    var showOwnerPhoneDialog by remember { mutableStateOf(false) }
    var ownerPhoneInput by remember { mutableStateOf(savedOwnerPhone) }

    val activeEmployees = remember(employees) {
        employees.filter { !it.isArchived }.distinctBy { it.id }
    }

    val effectiveTodayAttendance = remember(todayAttendance, allAttendance) {
        val calToday = java.util.Calendar.getInstance()
        val curYear = calToday.get(java.util.Calendar.YEAR)
        val curDay = calToday.get(java.util.Calendar.DAY_OF_YEAR)
        val calRec = java.util.Calendar.getInstance()

        (todayAttendance + allAttendance.filter {
            calRec.timeInMillis = it.date
            calRec.get(java.util.Calendar.YEAR) == curYear &&
            calRec.get(java.util.Calendar.DAY_OF_YEAR) == curDay
        }).distinctBy { it.id }
    }

    val presentList = remember(effectiveTodayAttendance) {
        effectiveTodayAttendance.filter { it.status == "PRESENT" && it.delayMinutes == 0 }
    }

    val lateList = remember(effectiveTodayAttendance) {
        effectiveTodayAttendance.filter { (it.status == "LATE" || it.status == "PRESENT") && it.delayMinutes > 0 }
    }

    val absentList = remember(effectiveTodayAttendance) {
        effectiveTodayAttendance.filter { it.status == "ABSENT" }
    }

    val pendingEmployees = remember(activeEmployees, effectiveTodayAttendance) {
        activeEmployees.filter { emp -> effectiveTodayAttendance.none { it.employeeId == emp.id } }
    }

    val totalDelayMinutes = remember(lateList) {
        lateList.sumOf { it.delayMinutes }
    }

    val dateFormattedArabic = remember {
        try {
            val sdf = SimpleDateFormat("EEEE d MMMM yyyy", Locale("ar"))
            sdf.format(Date())
        } catch (_: Exception) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
    }

    val filteredList = remember(boardFilter, activeEmployees, effectiveTodayAttendance) {
        when (boardFilter) {
            "PRESENT" -> activeEmployees.filter { emp ->
                effectiveTodayAttendance.any { it.employeeId == emp.id && it.status == "PRESENT" && it.delayMinutes == 0 }
            }
            "LATE" -> activeEmployees.filter { emp ->
                effectiveTodayAttendance.any { it.employeeId == emp.id && (it.status == "LATE" || it.status == "PRESENT") && it.delayMinutes > 0 }
            }
            "ABSENT" -> activeEmployees.filter { emp ->
                effectiveTodayAttendance.any { it.employeeId == emp.id && it.status == "ABSENT" }
            }
            "PENDING" -> pendingEmployees
            else -> activeEmployees
        }
    }

    MouseLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Dashboard Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📋 لوحة الغياب والحضور والتأخير اليومية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = SeaNavyDeep
                            )
                            Text(
                                text = "نظام 12 ساعة (ص / م) ومتابعة فورية للمطعم والمطبخ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = SeaTealPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "📅 $dateFormattedArabic",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SeaTealPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Owner WhatsApp Quick Report Button
                    Button(
                        onClick = {
                            val cleanOwner = savedOwnerPhone.ifBlank { ownerPhoneInput }
                            if (cleanOwner.isBlank()) {
                                showOwnerPhoneDialog = true
                            } else {
                                sendDailyAttendanceWhatsAppReport(
                                    context = context,
                                    ownerPhone = cleanOwner,
                                    dateStr = dateFormattedArabic,
                                    employees = activeEmployees,
                                    presentList = presentList,
                                    lateList = lateList,
                                    absentList = absentList,
                                    pendingList = pendingEmployees,
                                    totalDelayMinutes = totalDelayMinutes
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "🐦", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إرسال تقرير الحضور والغياب والتأخير للأونر عبر واتساب 🐦",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    if (savedOwnerPhone.isNotBlank()) {
                        Text(
                            text = "رقم هاتف الأونر المعتمد: $savedOwnerPhone",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 2. Summary KPI Metric Cards (4 Counters)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Present On-Time Card (Green)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { boardFilter = "PRESENT" }
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🟢 حاضر في الموعد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "${presentList.size}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF15803D))
                            Text(text = "موظف منتظم", fontSize = 9.sp, color = Color.Gray)
                        }
                    }

                    // Late Card (Amber)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { boardFilter = "LATE" }
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "⚠️ متأخرون اليوم", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "${lateList.size}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFB45309))
                            Text(text = "$totalDelayMinutes دقيقة تأخير", fontSize = 9.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Absent Card (Red)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { boardFilter = "ABSENT" }
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🔴 غائبون اليوم", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "${absentList.size}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                            Text(text = "غياب مثبت", fontSize = 9.sp, color = Color.Gray)
                        }
                    }

                    // Pending Card (Gray/Slate)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { boardFilter = "PENDING" }
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "⚪ لم يسجلوا بعد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "${pendingEmployees.size}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF334155))
                            Text(text = "بانتظار الحضور", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // 3. Filter Chips Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .mouseHorizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = boardFilter == "ALL",
                    onClick = { boardFilter = "ALL" },
                    label = { Text("الكل (${activeEmployees.size})", fontSize = 11.5.sp, fontWeight = if (boardFilter == "ALL") FontWeight.Bold else FontWeight.Normal) }
                )
                FilterChip(
                    selected = boardFilter == "PRESENT",
                    onClick = { boardFilter = "PRESENT" },
                    label = { Text("حاضر في الموعد 🟢 (${presentList.size})", fontSize = 11.5.sp, fontWeight = if (boardFilter == "PRESENT") FontWeight.Bold else FontWeight.Normal) }
                )
                FilterChip(
                    selected = boardFilter == "LATE",
                    onClick = { boardFilter = "LATE" },
                    label = { Text("متأخرون ⚠️ (${lateList.size})", fontSize = 11.5.sp, fontWeight = if (boardFilter == "LATE") FontWeight.Bold else FontWeight.Normal) }
                )
                FilterChip(
                    selected = boardFilter == "ABSENT",
                    onClick = { boardFilter = "ABSENT" },
                    label = { Text("غائبون 🔴 (${absentList.size})", fontSize = 11.5.sp, fontWeight = if (boardFilter == "ABSENT") FontWeight.Bold else FontWeight.Normal) }
                )
                FilterChip(
                    selected = boardFilter == "PENDING",
                    onClick = { boardFilter = "PENDING" },
                    label = { Text("لم يسجلوا بعد ⚪ (${pendingEmployees.size})", fontSize = 11.5.sp, fontWeight = if (boardFilter == "PENDING") FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // 4. Employee Attendance Cards List
        if (filteredList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "لا توجد سجلات تطابق هذا التصنيف حالياً",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(filteredList.distinctBy { it.id }, key = { "att_board_emp_${it.id}" }) { emp ->
                val att = effectiveTodayAttendance.filter { it.employeeId == emp.id }.maxByOrNull { it.date }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            att?.status == "ABSENT" -> StatusDanger.copy(alpha = 0.5f)
                            att != null && att.delayMinutes > 0 -> AmberCoral.copy(alpha = 0.5f)
                            att != null -> StatusSuccess.copy(alpha = 0.5f)
                            else -> Color.LightGray.copy(alpha = 0.4f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Employee Header & Shift
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = emp.name, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                Text(
                                    text = "الوظيفة: ${emp.role} | هاتف: ${emp.phone.ifBlank { "غير مسجل" }}",
                                    fontSize = 11.sp,
                                    color = SeaTealPrimary
                                )
                            }

                            // Scheduled shift 12h badge
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "الوردية: ${Time12hUtil.format12h(emp.shiftStartTime)} إلى ${Time12hUtil.format12h(emp.shiftEndTime)}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Live Status Banner
                        when {
                            att?.status == "ABSENT" -> {
                                Surface(
                                    color = Color(0xFFFEF2F2),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🔴", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(text = "غائب اليوم", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusDanger)
                                            Text(text = "السبب: ${att.notes.ifBlank { "غياب مسجل لليوم" }}", fontSize = 10.sp, color = Color(0xFF7F1D1D))
                                        }
                                    }
                                }
                            }

                            att != null && att.delayMinutes > 0 -> {
                                Surface(
                                    color = Color(0xFFFFFBEB),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "⚠️", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "متأخر بـ ${att.delayMinutes} دقيقة! (حضور: ${att.checkInTimeString})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF92400E)
                                            )
                                            val actionStr = when (att.lateActionTaken) {
                                                "PARDON" -> "🤝 سماح من الإدارة بدون خصم"
                                                "PENALTY" -> "⚠️ خصم جزا مالي: ${att.penaltyAmount} ج.م"
                                                "OWNER_NOTIFIED" -> "🐦 تم إبلاغ الأونر عبر واتساب"
                                                else -> "تم تسجيل التأخير"
                                            }
                                            Text(text = "الإجراء: $actionStr", fontSize = 10.sp, color = Color(0xFF78350F), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            att != null && att.status == "PRESENT" -> {
                                Surface(
                                    color = Color(0xFFF0FDF4),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🟢", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(text = "حاضر في الموعد تماماً", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF166534))
                                            Text(text = "وقت الحضور الفعلي: ${att.checkInTimeString} (نظام 12 ساعة)", fontSize = 10.sp, color = Color(0xFF14532D))
                                        }
                                    }
                                }
                            }

                            else -> {
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "⚪", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(text = "لم يسجل حضور حتى الآن", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))
                                            Text(text = "موعد الحضور المقرر: ${Time12hUtil.format12h(emp.shiftStartTime)}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }

                        // Departure Status if logged
                        if (att?.checkOutTimeString?.isNotBlank() == true) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🚪 تم تسجيل الانصراف: ${att.checkOutTimeString}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "ساعات العمل: ${att.hoursWorked} ساعة",
                                        fontSize = 10.5.sp,
                                        color = SeaNavyDeep
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        // Interactive Quick Action Buttons (12-Hour based in 2 spacious rows)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onRecordAttendance(emp, "CHECK_IN") },
                                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1.1f)
                            ) {
                                Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("حضور ⏰", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            Button(
                                onClick = { onRecordAttendance(emp, "CHECK_OUT") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1.1f)
                            ) {
                                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("انصراف 🚪", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { onRecordAttendance(emp, "ABSENCE") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Icon(imageVector = Icons.Default.Block, contentDescription = null, modifier = Modifier.size(13.dp), tint = StatusDanger)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("غياب 🚫", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        OutlinedButton(
                            onClick = { onViewStatement(emp) },
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp), tint = SeaTealPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("عرض كشف الحساب وسجل الحركات والراتب 📄", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 5. Today's Live Attendance Timeline Log
        if (todayAttendance.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "سجل الحركات المسجلة اليوم (${todayAttendance.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = SeaNavyDeep
                )
            }

            items(todayAttendance.distinctBy { it.id }, key = { "att_board_log_${it.id}_${it.date}" }) { record ->
                val empName = employees.find { it.id == record.employeeId }?.name ?: "موظف"
                val role = employees.find { it.id == record.employeeId }?.role ?: ""

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "$empName ($role)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            val statusLabel = when (record.status) {
                                "ABSENT" -> "🔴 غياب مثبت - ${record.notes}"
                                "LATE" -> "⚠️ تأخير ${record.delayMinutes} دقيقة - حضور: ${record.checkInTimeString}"
                                else -> "🟢 حضور: ${record.checkInTimeString}"
                            }
                            Text(text = statusLabel, fontSize = 10.5.sp, color = Color.DarkGray)
                        }

                        if (record.checkOutTimeString.isNotBlank()) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "انصراف: ${record.checkOutTimeString}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SeaTealPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
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

    // Owner Phone Prompt Dialog if blank
    if (showOwnerPhoneDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showOwnerPhoneDialog = false },
            title = { Text("تعيين رقم هاتف الأونر (واتساب)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل رقم هاتف الأونر ليتم إرسال التقارير التلقائية والتنبيهات إليه مباشرة:")
                    OutlinedTextField(
                        value = ownerPhoneInput,
                        onValueChange = { ownerPhoneInput = it },
                        label = { Text("رقم واتساب الأونر (مثلاً 010xxxxxxxx)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ownerPhoneInput.isNotBlank()) {
                            onSaveOwnerPhone(ownerPhoneInput)
                            showOwnerPhoneDialog = false
                            sendDailyAttendanceWhatsAppReport(
                                context = context,
                                ownerPhone = ownerPhoneInput,
                                dateStr = dateFormattedArabic,
                                employees = activeEmployees,
                                presentList = presentList,
                                lateList = lateList,
                                absentList = absentList,
                                pendingList = pendingEmployees,
                                totalDelayMinutes = totalDelayMinutes
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
                ) {
                    Text("حفظ وإرسال التقرير")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showOwnerPhoneDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

private fun sendDailyAttendanceWhatsAppReport(
    context: Context,
    ownerPhone: String,
    dateStr: String,
    employees: List<Employee>,
    presentList: List<EmployeeAttendance>,
    lateList: List<EmployeeAttendance>,
    absentList: List<EmployeeAttendance>,
    pendingList: List<Employee>,
    totalDelayMinutes: Int
) {
    try {
        val sb = StringBuilder()
        sb.append("🐟 *مطعم أسماك صنارة وشبكة*\n")
        sb.append("📋 *تقرير الحضور والغياب والتأخير اليومي*\n")
        sb.append("📅 التاريخ: $dateStr\n\n")

        sb.append("📊 *الملخص الإحصائي:*\n")
        sb.append("• إجمالي فريق العمل: ${employees.size} موظف\n")
        sb.append("• 🟢 الحاضرون في الموعد: ${presentList.size}\n")
        sb.append("• ⚠️ المتأخرون: ${lateList.size} (${totalDelayMinutes} دقيقة تأخير)\n")
        sb.append("• 🔴 الغائبون: ${absentList.size}\n")
        sb.append("• ⚪ لم يسجلوا حتى الآن: ${pendingList.size}\n\n")

        if (lateList.isNotEmpty()) {
            sb.append("⚠️ *المتأخرون عن الموعد:*\n")
            lateList.forEach { l ->
                val empName = employees.find { it.id == l.employeeId }?.name ?: "موظف"
                val role = employees.find { it.id == l.employeeId }?.role ?: ""
                val action = when (l.lateActionTaken) {
                    "PARDON" -> "سماح"
                    "PENALTY" -> "جزا ${l.penaltyAmount} ج"
                    "OWNER_NOTIFIED" -> "إشعار واتساب"
                    else -> "تأخير"
                }
                sb.append("• $empName ($role) - تأخير ${l.delayMinutes} دقيقة [حضور: ${l.checkInTimeString}] ($action)\n")
            }
            sb.append("\n")
        }

        if (absentList.isNotEmpty()) {
            sb.append("🔴 *الغائبون اليوم:*\n")
            absentList.forEach { a ->
                val empName = employees.find { it.id == a.employeeId }?.name ?: "موظف"
                val role = employees.find { it.id == a.employeeId }?.role ?: ""
                sb.append("• $empName ($role) - [${a.notes.ifBlank { "غياب مسجل" }}]\n")
            }
            sb.append("\n")
        }

        if (presentList.isNotEmpty()) {
            sb.append("🟢 *الحاضرون في الموعد:*\n")
            presentList.forEach { p ->
                val empName = employees.find { it.id == p.employeeId }?.name ?: "موظف"
                val role = employees.find { it.id == p.employeeId }?.role ?: ""
                sb.append("• $empName ($role) - حضور: ${p.checkInTimeString}\n")
            }
            sb.append("\n")
        }

        if (pendingList.isNotEmpty()) {
            sb.append("⚪ *لم يسجلوا بعد:*\n")
            pendingList.forEach { pending ->
                sb.append("• ${pending.name} (${pending.role}) - موعده المقرر: ${Time12hUtil.format12h(pending.shiftStartTime)}\n")
            }
        }

        var cleanPhone = ownerPhone.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.startsWith("01")) {
            cleanPhone = "2$cleanPhone"
        }
        val encodedMessage = Uri.encode(sb.toString())
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback generic share if WhatsApp direct fails
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "تقرير الحضور والغياب اليومي")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير الحضور"))
        } catch (_: Exception) {}
    }
}
