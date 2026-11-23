package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SalesBucketStat
import com.example.data.model.SalesPeriodType
import com.example.data.model.TodayItemSalesStat
import com.example.data.model.TodaySalesSummary
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.ArabicLineHeightStyle
import com.example.ui.theme.ArabicPlatformTextStyle
import com.example.ui.theme.BabyBluePrimary
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaNavyMedium
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusSuccess
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
private fun SlimTodaySalesSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(34.dp)
            .background(Color.White, RoundedCornerShape(7.dp))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(7.dp)),
        singleLine = true,
        textStyle = TextStyle(
            color = Color(0xFF0F172A),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = CairoFontFamily,
            platformStyle = ArabicPlatformTextStyle,
            lineHeightStyle = ArabicLineHeightStyle,
            textDirection = TextDirection.Content
        ),
        cursorBrush = SolidColor(SeaTealPrimary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = CairoFontFamily,
                            style = TextStyle(
                                platformStyle = ArabicPlatformTextStyle,
                                lineHeightStyle = ArabicLineHeightStyle
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onValueChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "مسح",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun TodaySalesChartComponent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val todaySummary by viewModel.todaySalesSummary.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedSalesPeriod.collectAsStateWithLifecycle()
    val isLandscape by viewModel.isLandscapeMode.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var sortByQuantity by remember { mutableStateOf(true) } // true: by qty, false: by revenue

    // Custom period state
    var customStartDateText by remember { mutableStateOf(SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(System.currentTimeMillis() - 7 * 86400000L))) }
    var customEndDateText by remember { mutableStateOf(SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date())) }
    var customPeriodError by remember { mutableStateOf<String?>(null) }
    var isCustomPeriodExpanded by remember { mutableStateOf(false) }

    // Distinct categories in current sales
    val categories = remember(todaySummary) {
        val list = mutableListOf("الكل")
        val fromItems = todaySummary.topSellingItems.map { it.category.trim() }.filter { it.isNotBlank() }.distinct()
        list.addAll(fromItems)
        list
    }

    // Filter and sort items
    val filteredItems = remember(todaySummary, searchQuery, selectedCategory, sortByQuantity) {
        var items = todaySummary.topSellingItems

        if (selectedCategory != "الكل") {
            items = items.filter { it.category.trim().equals(selectedCategory.trim(), ignoreCase = true) }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            items = items.filter { it.itemName.lowercase().contains(q) || it.category.lowercase().contains(q) }
        }

        if (sortByQuantity) {
            items.sortedByDescending { it.totalQuantitySold }
        } else {
            items.sortedByDescending { it.totalRevenue }
        }
    }

    val maxQuantity = remember(filteredItems) {
        filteredItems.maxOfOrNull { it.totalQuantitySold } ?: 1.0
    }
    val maxRevenue = remember(filteredItems) {
        filteredItems.maxOfOrNull { it.totalRevenue } ?: 1.0
    }

    val topItem = remember(todaySummary) {
        todaySummary.topSellingItems.firstOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(if (isLandscape) 5.dp else 8.dp)
    ) {
        // 1. Time Period Selector Chips Row
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = SeaTealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "تحديد الفترة الزمنية للتقرير:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = SeaNavyDeep
                        )
                    }
                    Text(
                        text = "الفترة الحالية: ${todaySummary.periodTitle}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SeaTealPrimary
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Period Chips Scrollable Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .mouseHorizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SalesPeriodType.values().forEach { period ->
                        val isSelected = selectedPeriod == period
                        Surface(
                            color = if (isSelected) SeaTealPrimary else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) SeaTealPrimary else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.clickable {
                                if (period == SalesPeriodType.CUSTOM) {
                                    isCustomPeriodExpanded = true
                                    viewModel.setSalesPeriod(SalesPeriodType.CUSTOM)
                                } else {
                                    isCustomPeriodExpanded = false
                                    viewModel.setSalesPeriod(period)
                                }
                            }
                        ) {
                            Text(
                                text = period.title,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Custom Period Date Pickers & Validation
                if (selectedPeriod == SalesPeriodType.CUSTOM || isCustomPeriodExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "تحديد فترة مخصصة (تاريخ البداية والنهاية):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SeaNavyDeep
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Start Date Field
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("من تاريخ (سنة/شهر/يوم):", fontSize = 9.5.sp, color = Color(0xFF64748B))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    SlimTodaySalesSearchField(
                                        value = customStartDateText,
                                        onValueChange = {
                                            customStartDateText = it
                                            customPeriodError = null
                                        },
                                        placeholder = "2026/11/01",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // End Date Field
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("إلى تاريخ (سنة/شهر/يوم):", fontSize = 9.5.sp, color = Color(0xFF64748B))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    SlimTodaySalesSearchField(
                                        value = customEndDateText,
                                        onValueChange = {
                                            customEndDateText = it
                                            customPeriodError = null
                                        },
                                        placeholder = "2026/11/04",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Submit Button
                                Button(
                                    onClick = {
                                        try {
                                            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
                                            val startDate = sdf.parse(customStartDateText.trim())
                                            val endDate = sdf.parse(customEndDateText.trim())

                                            if (startDate == null || endDate == null) {
                                                customPeriodError = "يرجى إدخال التاريخ بالصيغة الصحيحة (سنة/شهر/يوم)"
                                                return@Button
                                            }

                                            val calEnd = Calendar.getInstance().apply {
                                                time = endDate
                                                set(Calendar.HOUR_OF_DAY, 23)
                                                set(Calendar.MINUTE, 59)
                                                set(Calendar.SECOND, 59)
                                                set(Calendar.MILLISECOND, 999)
                                            }

                                            if (calEnd.timeInMillis < startDate.time) {
                                                customPeriodError = "تاريخ النهاية يجب ألا يكون أسبق من تاريخ البداية"
                                                return@Button
                                            }

                                            customPeriodError = null
                                            viewModel.setCustomSalesPeriod(startDate.time, calEnd.timeInMillis)
                                            Toast.makeText(context, "تم تطبيق الفترة المخصصة بنجاح ✓", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            customPeriodError = "تنسيق التاريخ غير صحيح، استخدم سنة/شهر/يوم"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                                    shape = RoundedCornerShape(7.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .padding(top = 14.dp)
                                        .height(34.dp)
                                ) {
                                    Text("عرض النتائج 🔍", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (customPeriodError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚠️ $customPeriodError",
                                    color = Color(0xFFDC2626),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Top Header Banner with 4 KPI metrics
        if (showHeader) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SeaNavyDeep),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = AmberCoral,
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "مخطط مبيعات ${todaySummary.periodTitle} 📊",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                                val periodDatesFormatted = remember(todaySummary.startOfDayMillis, todaySummary.endOfDayMillis) {
                                    if (todaySummary.startOfDayMillis > 0L && todaySummary.endOfDayMillis < Long.MAX_VALUE) {
                                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
                                        val s = sdf.format(Date(todaySummary.startOfDayMillis))
                                        val e = sdf.format(Date(todaySummary.endOfDayMillis))
                                        if (s == e) s else "$s إلى $e"
                                    } else {
                                        "منذ بداية تسجيل البيانات"
                                    }
                                }
                                Text(
                                    text = periodDatesFormatted,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Live Local Badge
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "نظام محلي مباشر 🟢",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(5.dp))

                    // 4 Quick Metrics for the Selected Period
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Total Revenue
                        MetricHeaderChip(
                            title = "إجمالي المبيعات",
                            value = "${String.format(Locale.US, "%.1f", todaySummary.totalSalesRevenue)} ج.م",
                            iconColor = Color(0xFF34D399),
                            modifier = Modifier.weight(1f)
                        )
                        // Total Quantity
                        MetricHeaderChip(
                            title = "إجمالي المباع",
                            value = "${String.format(Locale.US, "%.1f", todaySummary.totalQuantitySold)} وحدة",
                            iconColor = AmberCoral,
                            modifier = Modifier.weight(1f)
                        )
                        // Total Orders
                        MetricHeaderChip(
                            title = "عدد الأوردرات",
                            value = "${todaySummary.totalOrdersCount} أوردر",
                            iconColor = BabyBluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        // Average Order Value
                        MetricHeaderChip(
                            title = "متوسط الأوردر",
                            value = "${String.format(Locale.US, "%.1f", todaySummary.averageOrderValue)} ج.م",
                            subValue = topItem?.let { "الأكثر: ${it.itemName}" } ?: "",
                            iconColor = Color(0xFFFBBF24),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // 3. Visual Time Distribution Bar Chart (Hourly or Daily Distribution)
        if (todaySummary.timeDistribution.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = SeaTealPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "مخطط توزيع المبيعات الزمني (${todaySummary.periodTitle}):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = SeaNavyDeep
                            )
                        }
                        Text(
                            text = "${todaySummary.timeDistribution.size} فترات زمنية",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val maxBucketRevenue = remember(todaySummary.timeDistribution) {
                        todaySummary.timeDistribution.maxOfOrNull { it.revenue }?.takeIf { it > 0.0 } ?: 1.0
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .mouseHorizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        todaySummary.timeDistribution.forEach { bucket ->
                            val heightFraction = (bucket.revenue / maxBucketRevenue).toFloat().coerceIn(0.08f, 1f)
                            val revLabel = when {
                                bucket.revenue >= 10000 -> "${(bucket.revenue / 1000).toInt()}k ج"
                                bucket.revenue >= 1000 -> "${String.format(Locale.US, "%.1f", bucket.revenue / 1000)}k ج"
                                else -> "${bucket.revenue.toInt()} ج"
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.width(48.dp)
                            ) {
                                Text(
                                    text = revLabel,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SeaNavyMedium,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((50 * heightFraction).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    SeaTealPrimary,
                                                    AmberCoral
                                                )
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = bucket.label,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${bucket.ordersCount} طلب",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // 4. Control & Search Row (Slim & Aligned at 34.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input with Clear High-Contrast Arabic Text
            SlimTodaySalesSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "ابحث عن أي صنف مباع في ${todaySummary.periodTitle}...",
                modifier = Modifier.weight(1f)
            )

            // Toggle Sort Button
            Button(
                onClick = { sortByQuantity = !sortByQuantity },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sortByQuantity) SeaTealPrimary else AmberCoral
                ),
                shape = RoundedCornerShape(7.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (sortByQuantity) "بالكمية ⚖️" else "بالإيراد 💰",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Share / Copy Report Button
            OutlinedButton(
                onClick = {
                    val reportText = viewModel.generateTodaySalesShareText()
                    clipboardManager.setText(AnnotatedString(reportText))
                    Toast.makeText(context, "تم نسخ تقرير مبيعات ${todaySummary.periodTitle} للحافظة ✓", Toast.LENGTH_SHORT).show()

                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, reportText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير ومخطط مبيعات ${todaySummary.periodTitle}"))
                    } catch (_: Exception) {}
                },
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, SeaTealPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SeaTealPrimary),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("مشاركة 📋", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Category Filter Chips (Slim horizontal scroll)
        if (categories.size > 2) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .mouseHorizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        color = if (isSelected) SeaTealPrimary else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) SeaTealPrimary else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else Color(0xFF334155),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5. The Visual Chart & Ranked Items List
        if (filteredItems.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة لبحثك" else "لا توجد مبيعات مسجلة في ${todaySummary.periodTitle}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "إجمالي المبيعات: 0.0 ج.م • عند تسجيل أي أوردرات في هذه الفترة ستظهر هنا تلقائياً ولحظياً 🐟",
                        fontSize = 10.5.sp,
                        color = Color.Gray.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "قائمة مبيعات الأصناف (${filteredItems.size} صنف مباع في ${todaySummary.periodTitle}):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = SeaNavyDeep
                            )
                        }

                        Text(
                            text = "محدث لحظياً محلياً ⚡",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    filteredItems.forEachIndexed { index, stat ->
                        val ratio = if (sortByQuantity) {
                            (stat.totalQuantitySold / maxQuantity).toFloat().coerceIn(0.04f, 1f)
                        } else {
                            (stat.totalRevenue / maxRevenue).toFloat().coerceIn(0.04f, 1f)
                        }

                        TodayItemChartRow(
                            rank = index + 1,
                            stat = stat,
                            barRatio = ratio,
                            sortByQuantity = sortByQuantity
                        )

                        if (index < filteredItems.lastIndex) {
                            HorizontalDivider(
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.padding(vertical = 2.5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricHeaderChip(
    title: String,
    value: String,
    subValue: String = "",
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 8.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                color = iconColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subValue.isNotBlank()) {
                Text(
                    text = subValue,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TodayItemChartRow(
    rank: Int,
    stat: TodayItemSalesStat,
    barRatio: Float,
    sortByQuantity: Boolean
) {
    val rankBadgeColor = when (rank) {
        1 -> Color(0xFFD97706) // Gold
        2 -> Color(0xFF64748B) // Silver
        3 -> Color(0xFFB45309) // Bronze
        else -> SeaTealPrimary.copy(alpha = 0.85f)
    }

    val rankIconText = when (rank) {
        1 -> "🥇 1"
        2 -> "🥈 2"
        3 -> "🥉 3"
        else -> "#$rank"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // Line 1: Header (Rank, Name, Category, Sold Qty, Total Revenue)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank Pill
                Surface(
                    color = rankBadgeColor.copy(alpha = if (rank <= 3) 0.18f else 0.10f),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, rankBadgeColor)
                ) {
                    Text(
                        text = rankIconText,
                        color = rankBadgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stat.itemName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = SeaNavyDeep,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stat.category,
                                fontSize = 8.5.sp,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                            )
                        }
                    }

                    Text(
                        text = "طُلب في ${stat.ordersCount} أوردر • متوسط: ${String.format(Locale.US, "%.1f", stat.averageUnitPrice)} ج",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }

            // Quantity & Revenue Badge
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.2f", stat.totalQuantitySold).replace(".00", ""),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        color = if (rank == 1) AmberCoral else SeaTealPrimary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stat.unit,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color(0xFF334155)
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%.1f", stat.totalRevenue)} ج.م",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF0F766E)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.5.dp))

        // Line 2: Visual Proportional Progress Bar (Slim: 5dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                val animatedRatio by animateFloatAsState(
                    targetValue = barRatio,
                    animationSpec = tween(500),
                    label = "barRatio"
                )

                val barBrush = if (rank == 1) {
                    Brush.horizontalGradient(listOf(AmberCoral, Color(0xFFF59E0B)))
                } else if (rank <= 3) {
                    Brush.horizontalGradient(listOf(SeaTealPrimary, Color(0xFF2DD4BF)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF60A5FA)))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedRatio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(barBrush)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Percentage label
            Text(
                text = "${String.format(Locale.US, "%.1f", if (sortByQuantity) stat.percentageOfTotalQuantity else stat.percentageOfTotalRevenue)}%",
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = Color(0xFF475569),
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun TodaySalesChartDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val isLandscape by viewModel.isLandscapeMode.collectAsStateWithLifecycle()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.94f else 0.96f)
                .fillMaxHeight(if (isLandscape) 0.94f else 0.92f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Dialog Action Bar (Slim)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SeaNavyDeep)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = AmberCoral, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تقرير ومخطط مبيعات اليوم 📊",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                }

                // Scrollable Content
                MouseLazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    item {
                        TodaySalesChartComponent(
                            viewModel = viewModel,
                            showHeader = true
                        )
                    }
                }

                // Bottom Close Bar (Slim)
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "محدث تلقائياً مع كل أوردر في الكاشير أو البائعين ✓",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B)
                        )

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                            shape = RoundedCornerShape(7.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("إغلاق النافذة ✕", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
