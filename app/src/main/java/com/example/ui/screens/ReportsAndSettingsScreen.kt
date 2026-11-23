package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.settings.SettingsDeliveryAreasTab
import com.example.ui.screens.settings.SettingsInvoiceTemplateTab
import com.example.ui.screens.settings.SettingsMenuTab
import com.example.ui.screens.settings.SettingsPreparationsTab
import com.example.ui.screens.settings.SettingsPrintersTab
import com.example.ui.screens.settings.SettingsReportsAndBackupTab
import com.example.ui.theme.SeaTealPrimary
import com.example.viewmodel.MainViewModel

@Composable
fun ReportsAndSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "التقارير الشاملة والنسخ 📊",
        "القوائم والأصناف 🐟",
        "تجهيزات الفريش 🍳",
        "مناطق ورسوم التوصيل 🛵",
        "فورمة الفاتورة 🧾",
        "الطابعات 🖨️"
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 8.dp,
            containerColor = Color(0xFFF1F5F9),
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isSelected) SeaTealPrimary else Color(0xFF475569)
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> SettingsReportsAndBackupTab(viewModel = viewModel)
            1 -> SettingsMenuTab(viewModel = viewModel)
            2 -> SettingsPreparationsTab(viewModel = viewModel)
            3 -> SettingsDeliveryAreasTab(viewModel = viewModel)
            4 -> SettingsInvoiceTemplateTab(viewModel = viewModel)
            5 -> SettingsPrintersTab(viewModel = viewModel)
        }
    }
}

