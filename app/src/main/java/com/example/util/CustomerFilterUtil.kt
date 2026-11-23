package com.example.util

import com.example.data.model.Customer
import com.example.data.model.OrderWithItems
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Filter categories for customers.
 */
enum class CustomerFilterType(
    val title: String,
    val shortLabel: String,
    val description: String
) {
    ALL("جميع العملاء", "الكل", "عرض جميع العملاء المسجلين في النظام"),
    TOP_ORDERING("الأكثر طلباً", "الأكثر طلباً ⭐", "العملاء الأكثر تسجيلاً للأوردرات"),
    INACTIVE("لم يطلب منذ فترة", "لم يطلب منذ فترة ⏳", "عملاء سابقين لم يطلبوا منذ 30 يوماً أو أكثر"),
    LOYAL_SPACED("العميل الدائم", "العميل الدائم 💎", "زبائن أوفياء يطلبون على فترات متباعدة لكنهم زبائن معتمدين"),
    HIGH_SPENDING("الأعلى إنفاقاً", "الأعلى إنفاقاً 💰", "العملاء الأكثر دفعاً وإيراداً"),
    NEW_CUSTOMERS("عملاء جدد", "عملاء جدد ✨", "عملاء مسجلون حديثاً أو طلبوا أوردر واحد")
}

/**
 * Sort options for customer list.
 */
enum class CustomerSortOption(val title: String) {
    ORDERS_COUNT_DESC("الأكثر طلباً (تنازلي)"),
    TOTAL_SPENT_DESC("الأعلى إنفاقاً (تنازلي)"),
    LAST_ORDER_RECENT("آخر طلب (الأحدث أولاً)"),
    LAST_ORDER_OLDEST("آخر طلب (الأقدم أولاً - الغائبين)"),
    NAME_ASC("الاسم (أ - ي)"),
    DATE_ADDED_DESC("تاريخ التسجيل (الأحدث)")
}

/**
 * Calculated analytical profile for a single customer.
 */
data class CustomerAnalysis(
    val customer: Customer,
    val matchedOrders: List<OrderWithItems>,
    val totalOrdersCount: Int,
    val totalSpent: Double,
    val firstOrderTime: Long?,
    val lastOrderTime: Long?,
    val daysSinceLastOrder: Int?, // null if never ordered
    val timeSpanDays: Int, // days between first order and last order
    val averageIntervalDays: Double?, // average spacing between orders in days
    val isTopOrdering: Boolean,
    val isInactive: Boolean,
    val isLoyalSpaced: Boolean,
    val isHighSpender: Boolean,
    val isNewCustomer: Boolean
) {
    /**
     * Formatted string representing how long ago the last order was placed.
     */
    fun getLastOrderRelativeText(): String {
        val days = daysSinceLastOrder ?: return "لا توجد أوردرات سابقة"
        return when {
            days == 0 -> "اليوم"
            days == 1 -> "أمس"
            days == 2 -> "منذ يومين"
            days in 3..10 -> "منذ $days أيام"
            days in 11..29 -> "منذ $days يوماً"
            days in 30..59 -> "منذ شهر تقريباً (${days} يوم)"
            days in 60..89 -> "منذ شهرين (${days} يوم)"
            days in 90..179 -> "منذ ${days / 30} أشهر (${days} يوم)"
            days in 180..364 -> "منذ نصف عام تقريباً"
            else -> "منذ أكثر من سنة (${days} يوم)"
        }
    }

    /**
     * Formatted date string for the last order.
     */
    fun getLastOrderDateFormatted(): String {
        val time = lastOrderTime ?: return "—"
        return SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(time))
    }

    /**
     * Formatted date string for the first order or registration.
     */
    fun getFirstOrderDateFormatted(): String {
        val time = firstOrderTime ?: customer.createdAt
        return SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(time))
    }

    /**
     * Text describing spacing between orders for loyal regulars.
     */
    fun getSpacedIntervalText(): String {
        val avg = averageIntervalDays ?: return ""
        val rounded = String.format(Locale.US, "%.0f", avg)
        return "معدل طلب كل $rounded يوم تقريباً"
    }
}

object CustomerFilterUtil {

    /**
     * Helper to match orders belonging to a customer either by customerId or matching phone numbers.
     */
    fun getCustomerOrders(customer: Customer, allOrders: List<OrderWithItems>): List<OrderWithItems> {
        val p1 = customer.primaryPhone.trim()
        val p2 = customer.secondaryPhone?.trim() ?: ""

        return allOrders.filter { item ->
            val ord = item.order
            val matchId = ord.customerId != null && ord.customerId == customer.id
            val op1 = ord.customerPhone.trim()
            val op2 = ord.customerSecondaryPhone?.trim() ?: ""

            matchId ||
            (p1.isNotEmpty() && (op1 == p1 || op2 == p1)) ||
            (p2.isNotEmpty() && (op1 == p2 || op2 == p2))
        }.sortedByDescending { it.order.createdAt }
    }

    /**
     * Builds analytical profile for a customer based on orders.
     */
    fun analyzeCustomer(
        customer: Customer,
        allOrders: List<OrderWithItems>,
        currentTime: Long = System.currentTimeMillis(),
        inactiveThresholdDays: Int = 30
    ): CustomerAnalysis {
        val matchedOrders = getCustomerOrders(customer, allOrders)
        val validOrders = matchedOrders.filter { it.order.status != "CANCELLED" }

        val totalOrdersCount = maxOf(customer.totalOrdersCount, matchedOrders.size)
        val totalSpent = if (validOrders.isNotEmpty()) {
            validOrders.sumOf { it.order.totalAmount }
        } else {
            customer.totalSpent
        }

        val lastOrderTime = validOrders.maxOfOrNull { it.order.createdAt }
            ?: matchedOrders.maxOfOrNull { it.order.createdAt }

        val firstOrderTime = validOrders.minOfOrNull { it.order.createdAt }
            ?: matchedOrders.minOfOrNull { it.order.createdAt }

        val daysSinceLastOrder = if (lastOrderTime != null) {
            val diff = currentTime - lastOrderTime
            (diff / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        } else if (totalOrdersCount > 0) {
            val diff = currentTime - customer.createdAt
            (diff / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        } else {
            null
        }

        val timeSpanDays = if (firstOrderTime != null && lastOrderTime != null && lastOrderTime > firstOrderTime) {
            ((lastOrderTime - firstOrderTime) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        } else {
            0
        }

        val averageIntervalDays = if (validOrders.size >= 2 && timeSpanDays > 0) {
            timeSpanDays.toDouble() / (validOrders.size - 1)
        } else {
            null
        }

        // Logic for: الأكثر طلباً (Top Ordering)
        val isTopOrdering = totalOrdersCount >= 3

        // Logic for: لم يطلب منذ فترة (شهر مثلاً أو أكثر)
        val isInactive = totalOrdersCount > 0 && daysSinceLastOrder != null && daysSinceLastOrder >= inactiveThresholdDays

        // Logic for: العميل الدائم (اللي بيطلب على فترات متباعدة لكنه زبون)
        // زبون دائم: طلب عدة مرات (أوردرين على الأقل)، ممتدة على فترات زمنية متباعدة (أسبوعين أو أكثر أو متوسط فاصل 5 أيام فأكثر)
        val isLoyalSpaced = (totalOrdersCount >= 2 && timeSpanDays >= 14 && (averageIntervalDays == null || averageIntervalDays >= 4.0)) ||
                            (totalOrdersCount >= 3 && (currentTime - customer.createdAt) >= (20L * 24 * 60 * 60 * 1000L))

        // Logic for: الأعلى إنفاقاً (High Spender)
        val isHighSpender = totalSpent >= 800.0

        // Logic for: عملاء جدد (New Customers)
        val customerAgeDays = ((currentTime - customer.createdAt) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        val isNewCustomer = customerAgeDays <= 14 || (totalOrdersCount <= 1 && customerAgeDays <= 30)

        return CustomerAnalysis(
            customer = customer,
            matchedOrders = matchedOrders,
            totalOrdersCount = totalOrdersCount,
            totalSpent = totalSpent,
            firstOrderTime = firstOrderTime,
            lastOrderTime = lastOrderTime,
            daysSinceLastOrder = daysSinceLastOrder,
            timeSpanDays = timeSpanDays,
            averageIntervalDays = averageIntervalDays,
            isTopOrdering = isTopOrdering,
            isInactive = isInactive,
            isLoyalSpaced = isLoyalSpaced,
            isHighSpender = isHighSpender,
            isNewCustomer = isNewCustomer
        )
    }

    /**
     * Filters and sorts customers according to search query, selected filter tab, and sort option.
     */
    fun filterAndSortCustomers(
        customers: List<Customer>,
        allOrders: List<OrderWithItems>,
        searchQuery: String,
        selectedFilter: CustomerFilterType,
        sortOption: CustomerSortOption,
        inactiveThresholdDays: Int = 30,
        currentTime: Long = System.currentTimeMillis()
    ): List<CustomerAnalysis> {
        val query = searchQuery.trim().lowercase(Locale.getDefault())
        val arConverted = if (searchQuery.containsLatinLetters()) convertEnglishKeyboardToArabic(query).trim().lowercase(Locale.getDefault()) else ""

        val analyzedList = customers.map { cust ->
            analyzeCustomer(cust, allOrders, currentTime, inactiveThresholdDays)
        }

        // Apply text search
        val searchFiltered = if (query.isBlank()) {
            analyzedList
        } else {
            analyzedList.filter { analysis ->
                val c = analysis.customer
                c.name.lowercase(Locale.getDefault()).contains(query) ||
                c.primaryPhone.contains(query) ||
                (c.secondaryPhone?.contains(query) == true) ||
                c.notes.lowercase(Locale.getDefault()).contains(query) ||
                (arConverted.isNotBlank() && arConverted != query && (
                    c.name.lowercase(Locale.getDefault()).contains(arConverted) ||
                    c.notes.lowercase(Locale.getDefault()).contains(arConverted)
                ))
            }
        }

        // Apply category filter
        val categoryFiltered = when (selectedFilter) {
            CustomerFilterType.ALL -> searchFiltered
            CustomerFilterType.TOP_ORDERING -> searchFiltered.filter { it.isTopOrdering }
            CustomerFilterType.INACTIVE -> searchFiltered.filter { it.isInactive }
            CustomerFilterType.LOYAL_SPACED -> searchFiltered.filter { it.isLoyalSpaced }
            CustomerFilterType.HIGH_SPENDING -> searchFiltered.filter { it.isHighSpender }
            CustomerFilterType.NEW_CUSTOMERS -> searchFiltered.filter { it.isNewCustomer }
        }

        // Apply sorting
        return when (sortOption) {
            CustomerSortOption.ORDERS_COUNT_DESC -> categoryFiltered.sortedWith(
                compareByDescending<CustomerAnalysis> { it.totalOrdersCount }
                    .thenByDescending { it.totalSpent }
            )
            CustomerSortOption.TOTAL_SPENT_DESC -> categoryFiltered.sortedWith(
                compareByDescending<CustomerAnalysis> { it.totalSpent }
                    .thenByDescending { it.totalOrdersCount }
            )
            CustomerSortOption.LAST_ORDER_RECENT -> categoryFiltered.sortedWith(
                compareByDescending<CustomerAnalysis> { it.lastOrderTime ?: 0L }
                    .thenByDescending { it.totalOrdersCount }
            )
            CustomerSortOption.LAST_ORDER_OLDEST -> categoryFiltered.sortedWith(
                compareBy<CustomerAnalysis> { it.lastOrderTime ?: Long.MAX_VALUE }
                    .thenByDescending { it.totalOrdersCount }
            )
            CustomerSortOption.NAME_ASC -> categoryFiltered.sortedBy { it.customer.name }
            CustomerSortOption.DATE_ADDED_DESC -> categoryFiltered.sortedByDescending { it.customer.createdAt }
        }
    }
}
