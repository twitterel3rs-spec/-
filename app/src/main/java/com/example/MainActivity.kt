package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.mouseVerticalScroll
import androidx.compose.material3.NavigationRailItem
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.ui.components.ApkDownloadDialog
import com.example.ui.components.IncomingCallerSheet
import com.example.ui.components.StockWarningOverrideDialog
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.CallsAndCustomersScreen
import com.example.ui.screens.DriversAndStaffScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.PricesAndStockScreen
import com.example.ui.screens.ReportsAndSettingsScreen
import com.example.service.SennaraBackgroundService
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.SennaraTheme
import com.example.ui.theme.StatusWarning
import com.example.util.BackgroundNotificationHelper
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.NavScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val arLocale = java.util.Locale("ar")
            java.util.Locale.setDefault(arLocale)
            val config = resources.configuration
            config.setLocale(arLocale)
            config.setLayoutDirection(arLocale)
        } catch (_: Throwable) {}
        com.example.printer.ReceiptPrintDocumentAdapter.activeActivity = this
        enableEdgeToEdge()
        setContent {
            SennaraTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.printer.ReceiptPrintDocumentAdapter.activeActivity = this
    }

    override fun onDestroy() {
        if (com.example.printer.ReceiptPrintDocumentAdapter.activeActivity == this) {
            com.example.printer.ReceiptPrintDocumentAdapter.activeActivity = null
        }
        super.onDestroy()
    }
}

data class NavItem(
    val screen: NavScreen,
    val icon: ImageVector,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val callerState by viewModel.callerState.collectAsStateWithLifecycle()
    val stockPrompt by viewModel.stockOverridePrompt.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val dailyPriceReminder by viewModel.dailyPriceReviewNeeded.collectAsStateWithLifecycle()
    val cloudUser by viewModel.cloudUser.collectAsStateWithLifecycle()
    val showRoleSwitcherDialog by viewModel.showRoleSwitcherDialog.collectAsStateWithLifecycle()
    val deviceRole by viewModel.deviceRole.collectAsStateWithLifecycle()
    val activeSellerName by viewModel.activeSellerName.collectAsStateWithLifecycle()
    val backgroundServiceEnabled by viewModel.backgroundServiceEnabled.collectAsStateWithLifecycle()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsStateWithLifecycle()
    val pendingSyncOrdersCount by viewModel.pendingSyncOrdersCount.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isLandscape) {
        viewModel.isLandscapeMode.value = isLandscape
    }

    // Handle back button
    BackHandler(enabled = currentScreen != NavScreen.POS) {
        viewModel.currentScreen.value = NavScreen.POS
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var hasAskedNotificationPermThisRun by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && cloudUser != null && backgroundServiceEnabled) {
            SennaraBackgroundService.syncServiceRunningState(context, true)
        }
    }

    LaunchedEffect(cloudUser, backgroundServiceEnabled) {
        val shouldRun = (cloudUser != null) && backgroundServiceEnabled
        if (shouldRun && Build.VERSION.SDK_INT >= 33 && !hasAskedNotificationPermThisRun) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                hasAskedNotificationPermThisRun = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        SennaraBackgroundService.syncServiceRunningState(context, shouldRun)
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("sennara_crash_log", android.content.Context.MODE_PRIVATE)
        val lastCrashTime = prefs.getLong("last_crash_timestamp", 0L)
        if (lastCrashTime > 0L && (System.currentTimeMillis() - lastCrashTime) < 15000L) {
            prefs.edit().putLong("last_crash_timestamp", 0L).apply()
            snackbarHostState.showSnackbar("تمت استعادة جلسة العمل بنجاح ونظام الكاشير مستمر بالعمل بكفاءة ✓")
        }
    }

    val snackbarChannel = remember {
        kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    }

    LaunchedEffect(Unit) {
        for (msg in snackbarChannel) {
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarChannel.trySend(msg)
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarChannel.trySend(msg)
            viewModel.clearUserMessage()
        }
    }

    val navItems = listOf(
        NavItem(NavScreen.CALLS_CUSTOMERS, Icons.Default.Person, "العملاء"),
        NavItem(NavScreen.POS, Icons.Default.Storefront, "البيع"),
        NavItem(NavScreen.ORDERS, Icons.Default.ReceiptLong, "الأوردرات"),
        NavItem(NavScreen.PRICES_STOCK, Icons.Default.Inventory, "المخزون"),
        NavItem(NavScreen.DRIVERS_STAFF, Icons.Default.AccountBalanceWallet, "الحسابات"),
        NavItem(NavScreen.REPORTS_SETTINGS, Icons.Default.Settings, "الإعدادات")
    )

    var showApkDownloadDialog by remember { mutableStateOf(false) }

    if (isLandscape) {
        // Landscape Orientation: Full edge-to-edge layout filling 100% of the screen with zero dead borders
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF1F5F9), // Soft slate background for high contrast card surfaces
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // NavigationRail with deep contrast container (Slate Navy 900) - Fixed slim width (68.dp)
                androidx.compose.material3.NavigationRail(
                    modifier = Modifier.width(68.dp),
                    containerColor = Color(0xFF0F172A),
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AmberCoral,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.RestaurantMenu,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "صنارة وشبكة",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "powerd py\ning elhassan\nhamdoon",
                                color = Color(0xFF94A3B8),
                                fontSize = 5.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 6.5.sp,
                                modifier = Modifier.padding(horizontal = 1.dp)
                            )
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .mouseVerticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        navItems.forEach { item ->
                            val isSelected = currentScreen == item.screen
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { viewModel.currentScreen.value = item.screen },
                                icon = {
                                    if (item.screen == NavScreen.POS && cartItems.isNotEmpty()) {
                                        BadgedBox(badge = {
                                            Badge(containerColor = AmberCoral) {
                                                Text("${cartItems.size}")
                                            }
                                        }) {
                                            Icon(imageVector = item.icon, contentDescription = item.label, modifier = Modifier.size(19.dp))
                                        }
                                    } else if (item.screen == NavScreen.PRICES_STOCK && dailyPriceReminder) {
                                        BadgedBox(badge = {
                                            Badge(containerColor = AmberCoral) { Text("!") }
                                        }) {
                                            Icon(imageVector = item.icon, contentDescription = item.label, modifier = Modifier.size(19.dp))
                                        }
                                    } else {
                                        Icon(imageVector = item.icon, contentDescription = item.label, modifier = Modifier.size(19.dp))
                                    }
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color(0xFF38BDF8),
                                    indicatorColor = SeaTealPrimary,
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                // Vertical Divider between NavigationRail and Content Area
                androidx.compose.material3.VerticalDivider(
                    thickness = 1.dp,
                    color = Color(0xFF1E293B)
                )

                // Landscape Main Content Column with Top Header Strip
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFF1F5F9))
                ) {
                    // Landscape Top Header Strip (Hidden in POS to maximize vertical space for items & orders)
                    if (currentScreen != NavScreen.POS) {
                        Surface(
                            color = Color(0xFF0F172A),
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val activeItem = navItems.find { it.screen == currentScreen }
                            val activeTitle = when (currentScreen) {
                                NavScreen.POS -> "🐟 شاشة البيع ونقطة الكاشير"
                                NavScreen.CALLS_CUSTOMERS -> "👥 العملاء"
                                NavScreen.ORDERS -> "📋 إدارة الأوردرات والتوصيل والموصلين"
                                NavScreen.PRICES_STOCK -> "📦 أسعار اليوم والمخزون والتنبيهات"
                                NavScreen.DRIVERS_STAFF -> "💼 الحسابات العامة وفريق العمل والورديات"
                                NavScreen.REPORTS_SETTINGS -> "⚙️ التقارير والإعدادات والنسخ الاحتياطي"
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SeaTealPrimary.copy(alpha = 0.25f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = activeItem?.icon ?: Icons.Default.RestaurantMenu,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color.White
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (dailyPriceReminder) {
                                    Surface(
                                        onClick = { viewModel.currentScreen.value = NavScreen.PRICES_STOCK },
                                        shape = RoundedCornerShape(12.dp),
                                        color = AmberCoral.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, AmberCoral)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = AmberCoral,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "تنبيه أسعار 7 ص",
                                                color = Color(0xFFFDBA74),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (cloudUser != null) {
                                    val isOfflineOrPending = !isDeviceOnline || pendingSyncOrdersCount > 0
                                    val badgeColor = when {
                                        !isDeviceOnline -> Color(0xFF9A3412).copy(alpha = 0.7f)
                                        pendingSyncOrdersCount > 0 -> Color(0xFF92400E).copy(alpha = 0.7f)
                                        else -> Color(0xFF065F46).copy(alpha = 0.4f)
                                    }
                                    val badgeBorder = when {
                                        !isDeviceOnline -> Color(0xFFFB923C)
                                        pendingSyncOrdersCount > 0 -> Color(0xFFFBBF24)
                                        else -> Color(0xFF34D399)
                                    }
                                    val badgeText = when {
                                        !isDeviceOnline && pendingSyncOrdersCount > 0 -> "📴 أوفلاين ($pendingSyncOrdersCount معلق)"
                                        !isDeviceOnline -> "📴 أوفلاين (بانتظار النت)"
                                        pendingSyncOrdersCount > 0 -> "⏳ مزامنة ($pendingSyncOrdersCount)"
                                        else -> "🟢 سحابي مشترك"
                                    }
                                    Surface(
                                        onClick = { viewModel.currentScreen.value = NavScreen.REPORTS_SETTINGS },
                                        shape = RoundedCornerShape(12.dp),
                                        color = badgeColor,
                                        border = BorderStroke(1.dp, badgeBorder),
                                        modifier = Modifier.testTag("top_cloud_status_badge")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isOfflineOrPending) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                                contentDescription = "حالة الاتصال السحابي",
                                                tint = badgeBorder,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = badgeText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        onClick = { viewModel.currentScreen.value = NavScreen.REPORTS_SETTINGS },
                                        shape = RoundedCornerShape(12.dp),
                                        color = SeaTealPrimary.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, SeaTealPrimary)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PointOfSale,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "🖥️ محلي (غير مربوط)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Text(
                                        text = "🖥️ وضع عريض",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                    }

                    // Content Screen Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .imePadding()
                    ) {
                        when (currentScreen) {
                            NavScreen.POS -> PosScreen(viewModel = viewModel)
                            NavScreen.CALLS_CUSTOMERS -> CallsAndCustomersScreen(viewModel = viewModel)
                            NavScreen.PRICES_STOCK -> PricesAndStockScreen(viewModel = viewModel)
                            NavScreen.ORDERS -> OrdersScreen(viewModel = viewModel)
                            NavScreen.DRIVERS_STAFF -> AccountsScreen(viewModel = viewModel)
                            NavScreen.REPORTS_SETTINGS -> ReportsAndSettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    } else {
        // Portrait Orientation: Standard Scaffold with TopAppBar & Bottom NavigationBar
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = AmberCoral,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.RestaurantMenu,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "صنارة وشبكة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "powerd py ing elhassan hamdoon",
                                    fontSize = 8.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = SeaTealPrimary,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        if (cloudUser != null) {
                            val isOfflineOrPending = !isDeviceOnline || pendingSyncOrdersCount > 0
                            val badgeColor = when {
                                !isDeviceOnline -> Color(0xFF9A3412).copy(alpha = 0.75f)
                                pendingSyncOrdersCount > 0 -> Color(0xFF92400E).copy(alpha = 0.75f)
                                else -> Color(0xFF065F46).copy(alpha = 0.5f)
                            }
                            val badgeBorder = when {
                                !isDeviceOnline -> Color(0xFFFB923C)
                                pendingSyncOrdersCount > 0 -> Color(0xFFFBBF24)
                                else -> Color(0xFF34D399)
                            }
                            val badgeText = when {
                                !isDeviceOnline && pendingSyncOrdersCount > 0 -> "📴 أوفلاين ($pendingSyncOrdersCount)"
                                !isDeviceOnline -> "📴 أوفلاين"
                                pendingSyncOrdersCount > 0 -> "⏳ معلق ($pendingSyncOrdersCount)"
                                else -> "🟢 سحابي مشترك"
                            }
                            Surface(
                                onClick = { viewModel.currentScreen.value = NavScreen.REPORTS_SETTINGS },
                                shape = RoundedCornerShape(12.dp),
                                color = badgeColor,
                                border = BorderStroke(1.dp, badgeBorder),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("top_cloud_status_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isOfflineOrPending) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                        contentDescription = "حالة الاتصال السحابي",
                                        tint = badgeBorder,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = badgeText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Surface(
                                onClick = { viewModel.currentScreen.value = NavScreen.REPORTS_SETTINGS },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PointOfSale,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "🖥️ محلي (غير مربوط)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (dailyPriceReminder) {
                            IconButton(onClick = { viewModel.currentScreen.value = NavScreen.PRICES_STOCK }) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "تنبيه أسعار 7 ص",
                                    tint = AmberCoral
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.currentScreen.value = item.screen },
                            icon = {
                                if (item.screen == NavScreen.POS && cartItems.isNotEmpty()) {
                                    BadgedBox(badge = {
                                        Badge(containerColor = AmberCoral) {
                                            Text("${cartItems.size}")
                                        }
                                    }) {
                                        Icon(imageVector = item.icon, contentDescription = item.label)
                                    }
                                } else if (item.screen == NavScreen.PRICES_STOCK && dailyPriceReminder) {
                                    BadgedBox(badge = {
                                        Badge(containerColor = AmberCoral) {
                                            Text("!")
                                        }
                                    }) {
                                        Icon(imageVector = item.icon, contentDescription = item.label)
                                    }
                                } else {
                                    Icon(imageVector = item.icon, contentDescription = item.label)
                                }
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = SeaTealPrimary,
                                indicatorColor = SeaTealPrimary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
            ) {
                when (currentScreen) {
                    NavScreen.POS -> PosScreen(viewModel = viewModel)
                    NavScreen.CALLS_CUSTOMERS -> CallsAndCustomersScreen(viewModel = viewModel)
                    NavScreen.PRICES_STOCK -> PricesAndStockScreen(viewModel = viewModel)
                    NavScreen.ORDERS -> OrdersScreen(viewModel = viewModel)
                    NavScreen.DRIVERS_STAFF -> AccountsScreen(viewModel = viewModel)
                    NavScreen.REPORTS_SETTINGS -> ReportsAndSettingsScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Global Incoming Call Alert Sheet
    callerState?.let { state ->
        if (state.isVisible) {
            IncomingCallerSheet(
                phoneNumber = state.phoneNumber,
                customer = state.customer,
                addresses = state.addresses,
                recentOrders = state.recentOrders,
                onStartOrder = { cust, phone, addr ->
                    viewModel.startNewOrderForCaller(cust, phone, addr)
                },
                onRegisterCustomer = { phone ->
                    viewModel.currentScreen.value = NavScreen.CALLS_CUSTOMERS
                    viewModel.dismissCallerDialog()
                },
                onDismiss = { viewModel.dismissCallerDialog() }
            )
        }
    }

    // Global Stock Warning Override Prompt
    stockPrompt?.let { prompt ->
        StockWarningOverrideDialog(
            itemName = prompt.itemName,
            requested = prompt.requested,
            available = prompt.available,
            onConfirmOverride = { pass ->
                viewModel.submitOrder(managerPasswordOverride = pass)
                viewModel.dismissStockPrompt()
            },
            onDismiss = { viewModel.dismissStockPrompt() }
        )
    }

    // Direct APK Download & Installation Modal
    if (showApkDownloadDialog) {
        ApkDownloadDialog(onDismiss = { showApkDownloadDialog = false })
    }

    // Role & Seller Name Switcher Dialog
    if (showRoleSwitcherDialog) {
        var selectedRole by remember(deviceRole) { mutableStateOf(deviceRole) }
        var editedSellerName by remember(activeSellerName) { mutableStateOf(activeSellerName) }

        AlertDialog(
            onDismissRequest = { viewModel.showRoleSwitcherDialog.value = false },
            title = {
                Text(
                    text = "تغيير وظيفة الجهاز واسم البائع",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedRole == "CASHIER",
                            onClick = { selectedRole = "CASHIER" },
                            label = { Text("🖥️ كاشير رئيسي", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedRole == "SELLER",
                            onClick = { selectedRole = "SELLER" },
                            label = { Text("📱 بائع صالة", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editedSellerName,
                        onValueChange = { editedSellerName = it },
                        label = { Text("اسم البائع / الموظف") },
                        placeholder = { Text("مثال: محمد / أحمد / كاشير 1") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("role_switcher_seller_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.applyDeviceRole(selectedRole, editedSellerName)
                        viewModel.showRoleSwitcherDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary),
                    modifier = Modifier.testTag("role_switcher_confirm_button")
                ) {
                    Text("تأكيد وحفظ ✓", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.showRoleSwitcherDialog.value = false }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}
