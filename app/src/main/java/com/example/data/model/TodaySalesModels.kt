package com.example.data.model

enum class SalesPeriodType(val title: String) {
    TODAY("اليوم"),
    YESTERDAY("أمس"),
    THIS_WEEK("هذا الأسبوع"),
    LAST_WEEK("الأسبوع الماضي"),
    THIS_MONTH("هذا الشهر"),
    LAST_MONTH("الشهر الماضي"),
    ALL_TIME("كل الفترات"),
    CUSTOM("فترة مخصصة")
}

data class SalesBucketStat(
    val label: String,
    val timestamp: Long,
    val revenue: Double,
    val ordersCount: Int,
    val quantity: Double = 0.0
)

data class TodayItemSalesStat(
    val menuItemId: Long? = null,
    val itemName: String,
    val category: String,
    val unit: String,
    val totalQuantitySold: Double,
    val totalRevenue: Double,
    val ordersCount: Int,
    val averageUnitPrice: Double,
    val percentageOfTotalQuantity: Double,
    val percentageOfTotalRevenue: Double
)

data class TodaySalesSummary(
    val startOfDayMillis: Long = 0L,
    val endOfDayMillis: Long = 0L,
    val periodType: String = "TODAY",
    val periodTitle: String = "اليوم",
    val totalSalesRevenue: Double = 0.0,
    val totalQuantitySold: Double = 0.0,
    val totalOrdersCount: Int = 0,
    val averageOrderValue: Double = 0.0,
    val topSellingItems: List<TodayItemSalesStat> = emptyList(),
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val categoryQuantityBreakdown: Map<String, Double> = emptyMap(),
    val timeDistribution: List<SalesBucketStat> = emptyList(),
    val isRealTime: Boolean = true,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)
