package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.printer.ArabicEscPosHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

  @Test
  fun testArabicShapingAndCP864Mapping() {
    val sample = "مطعم أسماك - بلطي 2 كجم - الإجمالي 200 جنيه"

    // Test CP864
    val cp864Bytes = ArabicEscPosHelper.formatTextForPrinter(sample, ArabicEscPosHelper.CodepageMode.CP864_PAGE_22)
    assertNotNull(cp864Bytes)
    assertTrue(cp864Bytes.isNotEmpty())

    val hex864 = cp864Bytes.joinToString(" ") { String.format("%02X", it) }
    println("Original: $sample")
    println("CP864 Bytes: $hex864")

    // Test CP1256
    val cp1256Bytes = ArabicEscPosHelper.formatTextForPrinter(sample, ArabicEscPosHelper.CodepageMode.CP1256_PAGE_50)
    assertNotNull(cp1256Bytes)
    assertTrue(cp1256Bytes.isNotEmpty())

    val hex1256 = cp1256Bytes.joinToString(" ") { String.format("%02X", it) }
    println("CP1256 Bytes: $hex1256")

    // Test receipt line formatting
    val testLines = listOf(
        "مطعم أسماك - بلطي 2 كجم - الإجمالي 200 جنيه",
        "الطابعة: طابعة 1 (كاشير)",
        "العنوان: 192.168.1.50:9100",
        "التاريخ والوقت: 2026-10-15 10:30 AM",
        "  1.5 كيلو × 120.0 = 180.0 ج.م",
        "المجموع الفرعي:   180.0 ج.م",
        "الإجمالي المطلوب: 200.0 ج.م",
        "كابتن التوصيل: كابتن ممدوح (01099998888)",
        "طريقة الدفع: نقداً"
    )

    for (line in testLines) {
      val bytes864 = ArabicEscPosHelper.formatTextForPrinter(line, ArabicEscPosHelper.CodepageMode.CP864_PAGE_22)
      assertNotNull(bytes864)
      assertTrue(bytes864.isNotEmpty())
      println("Line: $line")
      println("  -> Bytes: ${bytes864.size} bytes")
    }
  }

  @Test
  fun testRealCustomerInvoiceBytes() {
    val order = com.example.data.model.Order(
      id = 1L,
      orderNumber = "#1001",
      orderType = "DELIVERY",
      customerName = "أحمد محمد علي",
      customerPhone = "01012345678",
      customerArea = "حي السويس",
      deliveryAddressTitle = "المنزل",
      deliveryAddressFull = "شارع الجيش - برج السلام - شقة 4",
      deliveryFee = 25.0,
      driverId = 1L,
      driverName = "كابتن ممدوح",
      subtotal = 350.0,
      discount = 20.0,
      totalAmount = 355.0,
      paymentMethod = "CASH",
      notes = "تسليم ساخن مع زيادة ليمون وسلطات"
    )

    val items = listOf(
      com.example.data.model.OrderItem(
        id = 1L,
        orderId = 1L,
        itemName = "بلطي ممتاز",
        category = "فريش",
        unit = "كيلو",
        quantity = 2.0,
        unitPrice = 120.0,
        preparationName = "مشوي ردة بالخلطة",
        preparationPrice = 15.0,
        itemNotes = "مستوي كويس على الفحم",
        totalPrice = 270.0
      ),
      com.example.data.model.OrderItem(
        id = 2L,
        orderId = 1L,
        itemName = "جمبري مقاس كبير",
        category = "جمبريات",
        unit = "كيلو",
        quantity = 0.5,
        unitPrice = 400.0,
        preparationName = "مقلي مقرمش",
        preparationPrice = 20.0,
        itemNotes = "بدون شطة",
        totalPrice = 210.0
      )
    )

    val orderWithItems = com.example.data.model.OrderWithItems(order, items)
    val template = com.example.printer.InvoiceTemplateConfig()
    val config = com.example.printer.PrinterConfig(
      id = "MAIN",
      displayName = "طابعة الكاشير والفواتير",
      connectionType = "NETWORK_IP",
      ipAddress = "192.168.1.50",
      port = 9100,
      codepageMode = "CP864_PAGE_22"
    )

    // Verify building bytes
    val manager = com.example.printer.RealPrinterManager(ApplicationProvider.getApplicationContext<Context>())
    val bytes = manager.buildCustomerInvoiceBytes(config, orderWithItems, template, isReprint = false, reprintReason = null)
    assertNotNull(bytes)
    assertTrue("Bytes should not be empty", bytes.isNotEmpty())
    println("Generated invoice bytes size: ${bytes.size}")
  }

  @Test
  fun testCustomerFiltersAndLoyalty() {
    val now = 1770000000000L // arbitrary fixed time
    val dayMs = 24L * 60 * 60 * 1000L

    // Customer 1: Regular loyal customer (orders spaced out across 50 days)
    val cust1 = com.example.data.model.Customer(
      id = 101L,
      name = "عميل دائم وفي",
      primaryPhone = "01011111111",
      createdAt = now - (60 * dayMs)
    )
    val ordersCust1 = listOf(
      com.example.data.model.OrderWithItems(
        order = com.example.data.model.Order(
          id = 1L,
          orderNumber = "#101",
          orderType = "DELIVERY",
          customerId = 101L,
          createdAt = now - (50 * dayMs),
          totalAmount = 250.0,
          status = "COMPLETED"
        ),
        items = emptyList()
      ),
      com.example.data.model.OrderWithItems(
        order = com.example.data.model.Order(
          id = 2L,
          orderNumber = "#102",
          orderType = "DELIVERY",
          customerId = 101L,
          createdAt = now - (25 * dayMs),
          totalAmount = 300.0,
          status = "COMPLETED"
        ),
        items = emptyList()
      ),
      com.example.data.model.OrderWithItems(
        order = com.example.data.model.Order(
          id = 3L,
          orderNumber = "#103",
          orderType = "DELIVERY",
          customerId = 101L,
          createdAt = now - (5 * dayMs),
          totalAmount = 350.0,
          status = "COMPLETED"
        ),
        items = emptyList()
      )
    )

    // Customer 2: Inactive customer (hasn't ordered for 40 days)
    val cust2 = com.example.data.model.Customer(
      id = 102L,
      name = "عميل لم يطلب منذ فترة",
      primaryPhone = "01022222222",
      createdAt = now - (90 * dayMs)
    )
    val ordersCust2 = listOf(
      com.example.data.model.OrderWithItems(
        order = com.example.data.model.Order(
          id = 4L,
          orderNumber = "#104",
          orderType = "DELIVERY",
          customerId = 102L,
          createdAt = now - (40 * dayMs),
          totalAmount = 200.0,
          status = "COMPLETED"
        ),
        items = emptyList()
      )
    )

    // Customer 3: Top frequent customer (5 orders)
    val cust3 = com.example.data.model.Customer(
      id = 103L,
      name = "العميل الأكثر طلباً",
      primaryPhone = "01033333333",
      createdAt = now - (15 * dayMs)
    )
    val ordersCust3 = (1..5).map { i ->
      com.example.data.model.OrderWithItems(
        order = com.example.data.model.Order(
          id = (10 + i).toLong(),
          orderNumber = "#20$i",
          orderType = "DELIVERY",
          customerId = 103L,
          createdAt = now - ((6 - i) * dayMs),
          totalAmount = 150.0,
          status = "COMPLETED"
        ),
        items = emptyList()
      )
    }

    val allCustomers = listOf(cust1, cust2, cust3)
    val allOrders = ordersCust1 + ordersCust2 + ordersCust3

    // 1. Analyze Cust1 (Loyal Spaced)
    val analysis1 = com.example.util.CustomerFilterUtil.analyzeCustomer(cust1, allOrders, currentTime = now)
    assertTrue("Customer 1 should be recognized as loyal spaced (العميل الدائم)", analysis1.isLoyalSpaced)
    assertEquals(3, analysis1.totalOrdersCount)
    assertEquals(900.0, analysis1.totalSpent, 0.01)
    assertEquals(5, analysis1.daysSinceLastOrder)
    assertFalse("Customer 1 is active, should not be inactive", analysis1.isInactive)

    // 2. Analyze Cust2 (Inactive > 30 days)
    val analysis2 = com.example.util.CustomerFilterUtil.analyzeCustomer(cust2, allOrders, currentTime = now, inactiveThresholdDays = 30)
    assertTrue("Customer 2 should be recognized as inactive (لم يطلب منذ شهر)", analysis2.isInactive)
    assertEquals(40, analysis2.daysSinceLastOrder)

    // 3. Analyze Cust3 (Top Ordering)
    val analysis3 = com.example.util.CustomerFilterUtil.analyzeCustomer(cust3, allOrders, currentTime = now)
    assertTrue("Customer 3 should be recognized as top ordering", analysis3.isTopOrdering)
    assertEquals(5, analysis3.totalOrdersCount)

    // 4. Test Filtering
    val inactiveFilterResult = com.example.util.CustomerFilterUtil.filterAndSortCustomers(
      customers = allCustomers,
      allOrders = allOrders,
      searchQuery = "",
      selectedFilter = com.example.util.CustomerFilterType.INACTIVE,
      sortOption = com.example.util.CustomerSortOption.LAST_ORDER_OLDEST,
      currentTime = now
    )
    assertEquals(1, inactiveFilterResult.size)
    assertEquals(102L, inactiveFilterResult.first().customer.id)

    val loyalFilterResult = com.example.util.CustomerFilterUtil.filterAndSortCustomers(
      customers = allCustomers,
      allOrders = allOrders,
      searchQuery = "",
      selectedFilter = com.example.util.CustomerFilterType.LOYAL_SPACED,
      sortOption = com.example.util.CustomerSortOption.ORDERS_COUNT_DESC,
      currentTime = now
    )
    assertTrue(loyalFilterResult.any { it.customer.id == 101L })

    val topFilterResult = com.example.util.CustomerFilterUtil.filterAndSortCustomers(
      customers = allCustomers,
      allOrders = allOrders,
      searchQuery = "",
      selectedFilter = com.example.util.CustomerFilterType.TOP_ORDERING,
      sortOption = com.example.util.CustomerSortOption.ORDERS_COUNT_DESC,
      currentTime = now
    )
    assertTrue(topFilterResult.any { it.customer.id == 103L })
    assertEquals(103L, topFilterResult.first().customer.id)
  }
}

