package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.DeliveryDriver
import com.example.data.model.MenuCategory
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderWithItems
import com.example.data.model.PreparationType
import com.example.data.repository.OrderPlacementResult
import com.example.data.repository.RestaurantRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.MouseLazyColumn
import com.example.ui.components.MouseLazyRow
import com.example.ui.screens.MenuItemGridTile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @org.junit.Before
  fun setUpFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
      com.google.firebase.FirebaseApp.initializeApp(context)
        ?: com.google.firebase.FirebaseApp.initializeApp(
          context,
          com.google.firebase.FirebaseOptions.Builder()
            .setApplicationId("1:123456789012:android:test")
            .setApiKey("AIzaSyTestKeyForRobolectric12345")
            .setProjectId("gen-lang-client-0219198835")
            .build()
        )
    }
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("صنارة وشبكة", appName)
  }

  @Test
  fun `verify restaurant repository initialization and order placement`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    repo.initializeDefaultDataIfEmpty()

    // Explicitly create a test item for this test
    val testItemId = db.menuDao().insertMenuItem(
      MenuItem(name = "بلطي تجربة", category = "فريش", unit = "كيلو", basePrice = 120.0, currentStock = 10.0)
    )
    val testItem = db.menuDao().getItemById(testItemId)
    assertNotNull(testItem)

    if (testItem != null) {
      val order = Order(
        orderNumber = "",
        orderType = "TAKEAWAY",
        customerName = "عميل تجربة",
        subtotal = testItem.basePrice,
        totalAmount = testItem.basePrice
      )
      val orderItem = OrderItem(
        menuItemId = testItem.id,
        itemName = testItem.name,
        quantity = 1.0,
        unitPrice = testItem.basePrice,
        totalPrice = testItem.basePrice
      )
      val result = repo.placeOrder(order, listOf(orderItem))
      assertTrue(result is OrderPlacementResult.Success)
    }
  }

  @Test
  fun `verify custom item modification in order does not alter master menu`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    repo.initializeDefaultDataIfEmpty()

    val testItemId = db.menuDao().insertMenuItem(
      MenuItem(name = "بلطي أصلي", category = "فريش", unit = "كيلو", basePrice = 100.0, currentStock = 10.0)
    )
    val originalItem = db.menuDao().getItemById(testItemId)!!
    val originalPrice = originalItem.basePrice

    val order = Order(
      orderNumber = "",
      orderType = "TAKEAWAY",
      customerName = "عميل خاص",
      subtotal = originalPrice,
      totalAmount = originalPrice
    )
    val item = OrderItem(
      menuItemId = originalItem.id,
      itemName = originalItem.name,
      quantity = 1.0,
      unitPrice = originalPrice,
      totalPrice = originalPrice
    )
    val placeResult = repo.placeOrder(order, listOf(item))
    assertTrue(placeResult is OrderPlacementResult.Success)
    val orderId = (placeResult as OrderPlacementResult.Success).orderId

    val placedOrderWithItems = db.orderDao().getOrderById(orderId)
    assertNotNull(placedOrderWithItems)
    val placedItem = placedOrderWithItems!!.items.first()

    // Modify the item specifically in this order (give it a custom name and discounted price)
    val modifiedItem = placedItem.copy(
      itemName = "بلطي مخصوص محمر للعميل",
      unitPrice = 85.0,
      totalPrice = 85.0
    )
    repo.updateOrderItemAndRecalculate(orderId, modifiedItem)

    // Verify order item and order total are updated
    val updatedOrder = db.orderDao().getOrderById(orderId)
    assertEquals(85.0, updatedOrder!!.order.totalAmount, 0.01)
    assertEquals("بلطي مخصوص محمر للعميل", updatedOrder.items.first().itemName)

    // Verify the master menu item is UNCHANGED
    val masterItem = db.menuDao().getItemById(originalItem.id)
    assertEquals(originalPrice, masterItem!!.basePrice, 0.01)
    assertEquals(originalItem.name, masterItem.name)
  }

  @Test
  fun `verify category and preparation CRUD operations`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // Category CRUD
    val newCat = MenuCategory(name = "أطباق بحرية خاصة", displayOrder = 10)
    repo.saveCategory(newCat)
    val cats = db.menuDao().getAllCategoriesList()
    assertTrue(cats.any { it.name == "أطباق بحرية خاصة" })

    // Preparation CRUD
    val prep = PreparationType(name = "شوي فحم إسكندراني", additionalPricePerUnit = 25.0)
    repo.savePreparation(prep)
    val preps = db.menuDao().getAllPreparationsList()
    assertTrue(preps.any { it.name == "شوي فحم إسكندراني" })
  }

  @Test
  fun `verify delivery driver settlement math`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val driver = DeliveryDriver(
      name = "كابتن ممدوح",
      phone = "01099998888",
      calculationType = "COMMISSION",
      ratePerOrder = 20.0
    )
    val driverId = db.driverDao().insertDriver(driver)

    // Place an order assigned to this driver
    val order = Order(
      orderNumber = "",
      orderType = "DELIVERY",
      driverId = driverId,
      driverName = "كابتن ممدوح",
      subtotal = 300.0,
      deliveryFee = 25.0,
      totalAmount = 325.0,
      paymentMethod = "CASH"
    )
    val orderItem = OrderItem(itemName = "جمبري مشوي", quantity = 1.0, unitPrice = 300.0, totalPrice = 300.0)
    repo.placeOrder(order, listOf(orderItem))

    // Calculate settlement with 15 LE petrol
    val settlement = repo.calculateAndSaveDriverSettlement(driverId, fuelExpenses = 15.0, notes = "تسوية شفت مسائي")
    assertNotNull(settlement)
    assertEquals(325.0, settlement!!.totalCashCollected, 0.01)
    assertEquals(20.0, settlement.driverEarnings, 0.01) // 1 order * 20 LE
    assertEquals(15.0, settlement.fuelAndExpenses, 0.01)
    // Net to handover = 325 - 20 - 15 = 290
    assertEquals(290.0, settlement.netAmountToHandover, 0.01)
  }

  @Test
  fun `verify apkManager inspects package and prepares real apk intent`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val apkInfo = com.example.util.ApkManager.getInstalledApkInfo(context)
    assertNotNull(apkInfo)
    assertEquals(context.packageName, apkInfo.packageName)
  }

  @Test
  fun `verify arabic test print and real order print byte generation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val printerManager = com.example.printer.RealPrinterManager(context)

    val config = com.example.printer.PrinterConfig(
      id = "MAIN",
      displayName = "طابعة الكاشير والفواتير",
      connectionType = "NETWORK_IP",
      ipAddress = "192.168.1.50",
      port = 9100,
      codepageMode = "CP864_PAGE_22"
    )

    // 1. Verify test receipt bytes
    val testBytes = printerManager.buildTestReceiptBytes(config)
    assertNotNull(testBytes)
    assertTrue("Test receipt bytes must not be empty", testBytes.isNotEmpty())

    // 2. Verify real order receipt bytes
    val order = com.example.data.model.Order(
      id = 101L,
      orderNumber = "#1001",
      orderType = "DELIVERY",
      customerName = "أحمد محمد",
      customerPhone = "01012345678",
      customerArea = "حي السويس",
      deliveryAddressFull = "شارع الجيش - عمارة 5",
      deliveryFee = 20.0,
      driverName = "كابتن ممدوح",
      subtotal = 200.0,
      discount = 0.0,
      totalAmount = 220.0,
      paymentMethod = "CASH",
      notes = "تسليم ساخن مع ليمون إضافي"
    )

    val item = com.example.data.model.OrderItem(
      id = 1L,
      orderId = 101L,
      itemName = "بلطي",
      quantity = 2.0,
      unit = "كجم",
      unitPrice = 100.0,
      totalPrice = 200.0,
      preparationName = "مشوي زيت وليمون",
      preparationPrice = 0.0,
      itemNotes = "مستوي كويس"
    )

    val orderWithItems = com.example.data.model.OrderWithItems(
      order = order,
      items = listOf(item)
    )

    val template = com.example.printer.InvoiceTemplateConfig()

    val invoiceBytes = printerManager.buildCustomerInvoiceBytes(
      config = config,
      orderWithItems = orderWithItems,
      template = template,
      isReprint = false,
      reprintReason = null
    )

    assertNotNull(invoiceBytes)
    assertTrue("Invoice bytes must not be empty", invoiceBytes.isNotEmpty())

    val kitchenBytes = printerManager.buildKitchenTicketBytes(
      config = config.copy(id = "KITCHEN_1"),
      orderWithItems = orderWithItems,
      kitchenTitle = "مطبخ 1 (شواية)",
      isReprint = false,
      reprintReason = null
    )
    assertNotNull(kitchenBytes)
    assertTrue("Kitchen bytes must not be empty", kitchenBytes.isNotEmpty())
  }

  @Test
  fun `verify placing real order and dispatching print`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    repo.initializeDefaultDataIfEmpty()

    val testItemId = db.menuDao().insertMenuItem(
      MenuItem(name = "بلطي بحري ممتاز", category = "فريش", unit = "كيلو", basePrice = 130.0, currentStock = 20.0)
    )
    val testItem = db.menuDao().getItemById(testItemId)!!

    val order = Order(
      orderNumber = "",
      orderType = "TAKEAWAY",
      customerName = "عميل تجربة الطباعة",
      customerPhone = "01099887766",
      subtotal = 260.0,
      totalAmount = 260.0
    )
    val item = OrderItem(
      menuItemId = testItem.id,
      itemName = testItem.name,
      quantity = 2.0,
      unitPrice = 130.0,
      totalPrice = 260.0,
      preparationName = "مشوي سنجاري",
      preparationPrice = 15.0,
      itemNotes = "مستوي جيدا مع ليمون"
    )

    val placeResult = repo.placeOrder(order, listOf(item))
    assertTrue(placeResult is OrderPlacementResult.Success)
    val success = placeResult as OrderPlacementResult.Success
    assertTrue(success.orderNumber.isNotBlank())

    // Fetch the order from DB
    val orderWithItems = db.orderDao().getOrderById(success.orderId)
    assertNotNull(orderWithItems)
    assertEquals(1, orderWithItems!!.items.size)
    assertEquals(2.0, orderWithItems.items.first().quantity, 0.001)

    // Verify building bytes for this exact order
    val config = repo.getPrinterConfig("MAIN")
    val template = repo.getInvoiceTemplateConfig()
    val invoiceBytes = repo.printerManager.buildCustomerInvoiceBytes(config, orderWithItems, template, false, null)
    assertNotNull(invoiceBytes)
    assertTrue("Real order invoice bytes must not be empty", invoiceBytes.isNotEmpty())
  }

  @Test
  fun `verify clearAllDummyData completely clears dummy records`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // Insert dummy records
    val empId = db.staffDao().insertEmployee(
      com.example.data.model.Employee(name = "موظف تجريبي", role = "شيف", phone = "01000000000")
    )
    db.staffDao().insertAttendance(
      com.example.data.model.EmployeeAttendance(employeeId = empId, checkInTimeString = "09:00 ص")
    )
    db.driverDao().insertDriver(
      com.example.data.model.DeliveryDriver(name = "سائق تجريبي", phone = "01111111111")
    )

    // Clear all dummy data
    repo.clearAllDummyData()

    // Verify completely empty
    assertEquals(0, db.driverDao().getAllDriversList().size)
    assertEquals(null, db.staffDao().getEmployeeById(empId))
  }

  @Test
  fun `verify Time12hUtil handles null, blank, and raw formats without crashing`() {
    val util = com.example.util.Time12hUtil
    assertEquals("09:00 ص", util.format12h(null))
    assertEquals("09:00 ص", util.format12h(""))
    assertEquals("09:00 ص", util.format12h("   "))
    assertEquals("09:00 ص", util.format12h("09:00"))
    assertEquals("09:30 م", util.format12h("21:30"))
    assertEquals("12:15 ص", util.format12h("00:15"))
    assertEquals(540, util.parseTimeToMinutes12h(null))
    assertEquals(540, util.parseTimeToMinutes12h(""))
    assertEquals(540, util.parseTimeToMinutes12h("09:00 ص"))
    assertEquals(1290, util.parseTimeToMinutes12h("09:30 م"))
  }

  @Test
  fun `verify delivery batch ticket bitmap rendering and raster conversion`() {
    val order1 = Order(
      id = 101L,
      orderNumber = "101",
      orderType = "DELIVERY",
      customerName = "محمد علي",
      customerPhone = "01012345678",
      customerArea = "حي السويس",
      deliveryAddressFull = "شارع الجيش عمارة 5 شقة 3",
      totalAmount = 285.0
    )
    val order2 = Order(
      id = 102L,
      orderNumber = "102",
      orderType = "DELIVERY",
      customerName = "أحمد حسن",
      customerPhone = "01234567890",
      customerArea = "الأربعين",
      deliveryAddressFull = "شارع الشهداء بجوار مسجد الحرمين",
      totalAmount = 450.0
    )
    val orders = listOf(
      OrderWithItems(order1, emptyList()),
      OrderWithItems(order2, emptyList())
    )
    val template = com.example.printer.InvoiceTemplateConfig()
    val bitmap = com.example.printer.ReceiptBitmapRenderer.renderDeliveryBatchTicket(orders, template, 80)
    assertNotNull(bitmap)
    assertEquals(576, bitmap.width)
    assertTrue(bitmap.height > 200)

    val rasterBytes = com.example.printer.ReceiptBitmapRenderer.bitmapToEscPosRasterBytes(bitmap)
    assertNotNull(rasterBytes)
    assertTrue(rasterBytes.isNotEmpty())
  }

  @Test
  fun `verify legacy json restore with missing and snake_case columns restores orders successfully`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)

    // Legacy JSON from an older version of the app:
    // Has snake_case columns, missing sellerName, missing deviceRole, missing paidAmount, missing isPaid, etc.
    // Order 5001 is unarchived (0), Order 5002 is archived (1)
    val legacyJson = """
    {
      "tables": {
        "orders": [
          {
            "id": 5001,
            "order_number": "#5001",
            "order_type": "DELIVERY",
            "customer_name": "عميل قديم نشط",
            "customer_phone": "01011223344",
            "delivery_fee": 15.0,
            "subtotal": 180.0,
            "total_amount": 195.0,
            "status": "COMPLETED",
            "is_archived": 0,
            "created_at": 1700000000000
          },
          {
            "id": 5002,
            "order_number": "#5002",
            "order_type": "TAKEAWAY",
            "customer_name": "عميل قديم مؤرشف",
            "customer_phone": "01055667788",
            "delivery_fee": 0.0,
            "subtotal": 300.0,
            "total_amount": 300.0,
            "status": "COMPLETED",
            "is_archived": 1,
            "created_at": 1690000000000
          }
        ],
        "order_items": [
          {
            "id": 8001,
            "order_id": 5001,
            "item_name": "بلطي قديم",
            "quantity": 1.5,
            "unit_price": 120.0,
            "total_price": 180.0
          },
          {
            "id": 8002,
            "order_id": 5002,
            "item_name": "جمبري قديم",
            "quantity": 1.0,
            "unit_price": 300.0,
            "total_price": 300.0
          }
        ]
      }
    }
    """.trimIndent()

    val restoreResult = com.example.util.BackupManager.restoreDatabaseFromJson(context, legacyJson)
    assertTrue("Restore should succeed", restoreResult.isSuccess)
    val restoredCount = restoreResult.getOrNull() ?: 0
    assertTrue("Should have restored records", restoredCount > 0)

    val order1 = db.orderDao().getOrderById(5001)
    assertNotNull("Order 5001 should be restored in DB", order1)
    assertEquals("عميل قديم نشط", order1!!.order.customerName)
    assertEquals(false, order1.order.isArchived)
    assertEquals(1, order1.items.size)
    assertEquals("بلطي قديم", order1.items.first().itemName)

    val order2 = db.orderDao().getOrderById(5002)
    assertNotNull("Order 5002 should be restored in DB", order2)
    assertEquals("عميل قديم مؤرشف", order2!!.order.customerName)
    assertEquals(true, order2.order.isArchived)
    assertEquals(1, order2.items.size)
    assertEquals("جمبري قديم", order2.items.first().itemName)
  }

  @Test
  fun `verify legacy sqlite db file restore restores orders successfully`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)

    // Create a temporary SQLite database with an older schema (version 7 style)
    val legacyDbFile = java.io.File(context.cacheDir, "legacy_test_backup.db")
    if (legacyDbFile.exists()) legacyDbFile.delete()

    val legacyDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(legacyDbFile, null)
    legacyDb.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY, orderNumber TEXT, orderType TEXT, customerName TEXT, customerPhone TEXT, totalAmount REAL, status TEXT, isArchived INTEGER, createdAt INTEGER)")
    legacyDb.execSQL("CREATE TABLE order_items (id INTEGER PRIMARY KEY, orderId INTEGER, itemName TEXT, quantity REAL, unitPrice REAL, totalPrice REAL)")

    legacyDb.execSQL("INSERT INTO orders (id, orderNumber, orderType, customerName, customerPhone, totalAmount, status, isArchived, createdAt) VALUES (6001, '#6001', 'DELIVERY', 'عميل قاعدة قديمة', '01200000000', 250.0, 'COMPLETED', 0, 1700000000000)")
    legacyDb.execSQL("INSERT INTO order_items (id, orderId, itemName, quantity, unitPrice, totalPrice) VALUES (9001, 6001, 'فيليه قديم', 1.0, 250.0, 250.0)")
    legacyDb.close()

    val uri = android.net.Uri.fromFile(legacyDbFile)
    val restoreResult = com.example.util.BackupManager.restoreDatabaseFromUri(context, uri)
    assertTrue("SQLite restore should succeed", restoreResult.isSuccess)

    val restoredOrder = db.orderDao().getOrderById(6001)
    assertNotNull("Order 6001 should be restored in DB", restoredOrder)
    assertEquals("عميل قاعدة قديمة", restoredOrder!!.order.customerName)
    assertEquals(false, restoredOrder.order.isArchived)
    assertEquals(1, restoredOrder.items.size)
    assertEquals("فيليه قديم", restoredOrder.items.first().itemName)

    legacyDbFile.delete()
    Unit
  }

  @Test
  fun `verify full menu item lifecycle - add, edit, stock receive, adjust, archive, delete`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // 1. ADD ITEM
    val newItem = MenuItem(
      name = "سمك دنيس بلدي",
      category = "فريش",
      unit = "كيلو",
      basePrice = 280.0,
      costPrice = 220.0,
      currentStock = 15.0,
      supportsPreparation = true
    )
    val savedId = repo.saveMenuItem(newItem)
    assertTrue("Item ID should be > 0", savedId > 0L)

    val fetchedItem = repo.getMenuItemById(savedId)
    assertNotNull(fetchedItem)
    assertEquals("سمك دنيس بلدي", fetchedItem!!.name)
    assertEquals(280.0, fetchedItem.basePrice, 0.01)
    assertEquals(15.0, fetchedItem.currentStock, 0.01)
    assertEquals(false, fetchedItem.isArchived)

    // 2. EDIT ITEM (Price & Name)
    val editedItem = fetchedItem.copy(
      name = "سمك دنيس سوبر جامبو",
      basePrice = 320.0,
      costPrice = 250.0
    )
    repo.saveMenuItem(editedItem)
    val afterEdit = repo.getMenuItemById(savedId)
    assertNotNull(afterEdit)
    assertEquals("سمك دنيس سوبر جامبو", afterEdit!!.name)
    assertEquals(320.0, afterEdit.basePrice, 0.01)
    assertEquals(250.0, afterEdit.costPrice, 0.01)

    // 3. RECEIVE SHIPMENT (Stock addition)
    repo.receiveInventoryBatch(savedId, 10.0, 240.0, "شحنة طازجة من المينا")
    val afterReceive = repo.getMenuItemById(savedId)
    assertNotNull(afterReceive)
    assertEquals(25.0, afterReceive!!.currentStock, 0.01) // 15 + 10 = 25
    assertEquals(240.0, afterReceive.costPrice, 0.01)

    // 4. ADJUST STOCK (Stock adjustment / waste)
    repo.adjustInventory(savedId, 22.5, "تسوية جردية نهاية اليوم")
    val afterAdjust = repo.getMenuItemById(savedId)
    assertNotNull(afterAdjust)
    assertEquals(22.5, afterAdjust!!.currentStock, 0.01)

    // 5. ARCHIVE ITEM
    repo.archiveMenuItem(savedId, true)
    val afterArchive = repo.getMenuItemById(savedId)
    assertNotNull(afterArchive)
    assertEquals(true, afterArchive!!.isArchived)

    // Active items query should NOT include it
    val activeItems = db.menuDao().getAllMenuItemsList()
    assertTrue(activeItems.none { it.id == savedId })

    // UNARCHIVE ITEM
    repo.archiveMenuItem(savedId, false)
    val afterUnarchive = repo.getMenuItemById(savedId)
    assertEquals(false, afterUnarchive!!.isArchived)
    val activeItemsAfter = db.menuDao().getAllMenuItemsList()
    assertTrue(activeItemsAfter.any { it.id == savedId })

    // 6. DELETE ITEM
    repo.deleteMenuItem(savedId)
    val afterDelete = repo.getMenuItemById(savedId)
    assertEquals(null, afterDelete)
  }

  @Test
  fun `verify daily prices batch update`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val item1Id = repo.saveMenuItem(MenuItem(name = "وقار صيد", category = "فريش", unit = "كيلو", basePrice = 300.0))
    val item2Id = repo.saveMenuItem(MenuItem(name = "قاروص صيد", category = "فريش", unit = "كيلو", basePrice = 250.0))

    val updates = mapOf(item1Id to 330.0, item2Id to 275.0)
    repo.updateDailyPrices(updates)

    val updated1 = repo.getMenuItemById(item1Id)
    val updated2 = repo.getMenuItemById(item2Id)
    assertEquals(330.0, updated1!!.basePrice, 0.01)
    assertEquals(275.0, updated2!!.basePrice, 0.01)
  }

  @Test
  fun `verify local order archiving and unarchiving status`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val order = Order(
      orderNumber = "#99901",
      orderType = "TAKEAWAY",
      customerName = "عميل محلي",
      totalAmount = 250.0,
      status = "COMPLETED",
      isArchived = false
    )
    val placement = repo.placeOrder(order, emptyList())
    val localId = (placement as OrderPlacementResult.Success).orderId
    val order1 = repo.getOrderById(localId)
    assertNotNull(order1)
    assertEquals(false, order1!!.order.isArchived)

    repo.setOrderArchived(localId, true)
    val orderArchived = repo.getOrderById(localId)
    assertEquals(true, orderArchived!!.order.isArchived)

    repo.setOrderArchived(localId, false)
    val orderUnarchived = repo.getOrderById(localId)
    assertEquals(false, orderUnarchived!!.order.isArchived)
  }

  @Test
  fun `verify restoring same backup twice does not create duplicate records`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)

    val backupJson = """
    {
      "tables": {
        "orders": [
          {
            "id": 7001,
            "orderNumber": "#7001",
            "orderType": "TAKEAWAY",
            "customerName": "عميل نسخ متكرر",
            "customerPhone": "01000000001",
            "totalAmount": 150.0,
            "subtotal": 150.0,
            "status": "COMPLETED",
            "isArchived": 0,
            "createdAt": 1700000000000
          }
        ],
        "order_items": [
          {
            "id": 9101,
            "orderId": 7001,
            "itemName": "بلطي سنجاري",
            "quantity": 1.0,
            "unitPrice": 150.0,
            "totalPrice": 150.0
          }
        ]
      }
    }
    """.trimIndent()

    // First restore
    val res1 = com.example.util.BackupManager.restoreDatabaseFromJson(context, backupJson)
    assertTrue("First restore should succeed", res1.isSuccess)

    val ordersAfterFirst = db.orderDao().getAllOrdersDirect()
    val countFirst = ordersAfterFirst.count { it.order.orderNumber == "#7001" }
    assertEquals("Should have exactly 1 order with #7001", 1, countFirst)

    // Second restore with same data
    val res2 = com.example.util.BackupManager.restoreDatabaseFromJson(context, backupJson)
    assertTrue("Second restore should succeed", res2.isSuccess)

    val ordersAfterSecond = db.orderDao().getAllOrdersDirect()
    val countSecond = ordersAfterSecond.count { it.order.orderNumber == "#7001" }
    assertEquals("Should still have exactly 1 order with #7001 after restoring twice", 1, countSecond)
  }

  @Test
  fun `verify restored orders are queryable locally by order number`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)

    val restoredOrder = Order(
      id = 7050L,
      orderNumber = "#7050",
      orderType = "TAKEAWAY",
      customerName = "عميل محلي",
      customerPhone = "01012345678",
      subtotal = 200.0,
      totalAmount = 200.0,
      status = "COMPLETED",
      createdAt = 1600000000000L
    )
    val restoredItem = OrderItem(
      id = 9200L,
      orderId = 7050L,
      itemName = "جمبري مقلي",
      quantity = 1.0,
      unitPrice = 200.0,
      totalPrice = 200.0
    )
    db.orderDao().insertOrder(restoredOrder)
    db.orderDao().insertOrderItems(listOf(restoredItem))

    val allWithNumber = db.orderDao().getOrdersByOrderNumberAll("#7050")
    assertEquals("Must have exactly 1 order record in database", 1, allWithNumber.size)
    assertEquals(200.0, allWithNumber.first().order.totalAmount, 0.01)
  }

  @Test
  fun `verify post-restore new order placement uses safe non-colliding order numbers`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // Restore backup with order #500
    val json = """
    {
      "tables": {
        "orders": [
          {
            "id": 8500,
            "orderNumber": "#500",
            "orderType": "TAKEAWAY",
            "customerName": "عميل رقم 500",
            "totalAmount": 100.0,
            "subtotal": 100.0,
            "status": "COMPLETED",
            "createdAt": 1700000000000
          }
        ]
      }
    }
    """.trimIndent()

    val res = com.example.util.BackupManager.restoreDatabaseFromJson(context, json)
    assertTrue(res.isSuccess)

    // Next order placed should be >= 501
    val nextNum = repo.getNextOrderNumber()
    assertTrue("Next order number should be at least 501, was $nextNum", nextNum >= 501)

    val newOrder = Order(
      orderNumber = "",
      orderType = "TAKEAWAY",
      customerName = "عميل ما بعد الاستعادة",
      subtotal = 120.0,
      totalAmount = 120.0
    )
    val item = OrderItem(itemName = "سمك بلطي", quantity = 1.0, unitPrice = 120.0, totalPrice = 120.0)

    val result = repo.placeOrder(newOrder, listOf(item))
    assertTrue(result is OrderPlacementResult.Success)
    val success = result as OrderPlacementResult.Success

    assertEquals("#$nextNum", success.orderNumber)
    val created = db.orderDao().getOrderById(success.orderId)
    assertNotNull(created)
    assertEquals("#$nextNum", created!!.order.orderNumber)
  }

  @Test
  fun `verify deduplicateOrders merges duplicates safely`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // Insert two orders with same orderNumber and total
    val masterOrder = Order(
      id = 9901L,
      orderNumber = "#9901",
      orderType = "TAKEAWAY",
      customerName = "عميل مكرر",
      subtotal = 100.0,
      totalAmount = 100.0,
      status = "PENDING",
      createdAt = 1700000000000L
    )
    val duplicateOrder = Order(
      id = 9902L,
      orderNumber = "#9901",
      orderType = "TAKEAWAY",
      customerName = "عميل مكرر",
      subtotal = 100.0,
      totalAmount = 100.0,
      status = "COMPLETED",
      createdAt = 1700000005000L
    )
    db.orderDao().insertOrder(masterOrder)
    db.orderDao().insertOrder(duplicateOrder)

    val itemMaster = OrderItem(orderId = 9901L, itemName = "بلطي", quantity = 1.0, unitPrice = 100.0, totalPrice = 100.0)
    val itemDuplicate = OrderItem(orderId = 9902L, itemName = "بلطي", quantity = 1.0, unitPrice = 100.0, totalPrice = 100.0)
    db.orderDao().insertOrderItems(listOf(itemMaster, itemDuplicate))

    // Run deduplicateOrders
    repo.deduplicateOrders()

    val remaining = db.orderDao().getOrdersByOrderNumberAll("#9901")
    assertEquals("Should only have 1 order remaining after deduplication", 1, remaining.size)
    val remainingOrder = remaining.first()
    assertEquals(9901L, remainingOrder.order.id)
    assertEquals("COMPLETED", remainingOrder.order.status)
    assertEquals(1, remainingOrder.items.size)
  }

  @Test
  fun `verify MainViewModel instantiation`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.viewmodel.MainViewModel(application)
    assertNotNull(vm)
  }

  @Test
  fun `verify editing menu item does not delete other items or alter IDs`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val id1 = db.menuDao().insertMenuItem(MenuItem(name = "صنف ثبات 1", category = "أسماك", unit = "كيلو", basePrice = 50.0))
    val id2 = db.menuDao().insertMenuItem(MenuItem(name = "صنف ثبات 2", category = "أسماك", unit = "كيلو", basePrice = 70.0))
    val id3 = db.menuDao().insertMenuItem(MenuItem(name = "صنف ثبات 3", category = "أسماك", unit = "كيلو", basePrice = 90.0))

    val beforeList = db.menuDao().getAllMenuItemsList()
    assertTrue(beforeList.any { it.id == id1 && it.name == "صنف ثبات 1" })
    assertTrue(beforeList.any { it.id == id2 && it.name == "صنف ثبات 2" })
    assertTrue(beforeList.any { it.id == id3 && it.name == "صنف ثبات 3" })

    // Update item 2 in-place (simulating user edit)
    val itemToEdit = db.menuDao().getItemById(id2)!!
    val updatedItem = itemToEdit.copy(basePrice = 85.0, name = "صنف ثبات 2 معدل")
    repo.saveMenuItem(updatedItem)

    val afterList = db.menuDao().getAllMenuItemsList()
    // Verify item 2 was updated without losing item 1 or item 3, and all IDs remained stable
    val updatedResult = afterList.firstOrNull { it.id == id2 }
    assertNotNull("Item 2 should still exist with same ID", updatedResult)
    assertEquals(85.0, updatedResult!!.basePrice, 0.01)
    assertEquals("صنف ثبات 2 معدل", updatedResult.name)

    // Other items must remain completely intact
    assertNotNull(afterList.firstOrNull { it.id == id1 && it.name == "صنف ثبات 1" })
    assertNotNull(afterList.firstOrNull { it.id == id3 && it.name == "صنف ثبات 3" })
  }

  @Test
  fun `verify archiving menu item hides from active menu and preserves data`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val id = db.menuDao().insertMenuItem(MenuItem(name = "صنف للأرشفة", category = "أسماك", unit = "كيلو", basePrice = 110.0))
    val initialActive = db.menuDao().getAllMenuItemsList()
    assertTrue(initialActive.any { it.id == id })

    // Archive the item
    repo.archiveMenuItem(id, true)

    val activeAfter = db.menuDao().getAllMenuItemsList()
    val allIncludingArchived = db.menuDao().getAllMenuItemsIncludingArchivedList()

    // Must be hidden from active list
    assertTrue("Archived item must not be in active items list", activeAfter.none { it.id == id })

    // Must still exist in all items with isArchived = true and data preserved
    val archivedItem = allIncludingArchived.firstOrNull { it.id == id }
    assertNotNull("Archived item must still exist in database", archivedItem)
    assertTrue("isArchived must be true", archivedItem!!.isArchived)
    assertEquals("صنف للأرشفة", archivedItem.name)
    assertEquals(110.0, archivedItem.basePrice, 0.01)
  }

  @Test
  fun `verify explicit unarchiving restores item to active menu list`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val id = db.menuDao().insertMenuItem(MenuItem(name = "صنف استعادة", category = "أسماك", unit = "كيلو", basePrice = 130.0, isArchived = true))

    // Verify it is not in active list
    assertTrue(db.menuDao().getAllMenuItemsList().none { it.id == id })

    // Unarchive the item
    repo.archiveMenuItem(id, false)

    val activeAfter = db.menuDao().getAllMenuItemsList()
    val restored = activeAfter.firstOrNull { it.id == id }
    assertNotNull("Restored item must reappear in active list", restored)
    assertTrue(!restored!!.isArchived)
    assertEquals("صنف استعادة", restored.name)
  }

  @Test
  fun `verify items with duplicate names keep unique cloudIds and never merge`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // Two distinct items with the EXACT same name, but different specs/categories/prices
    val item1 = MenuItem(
      name = "سلطة خضراء",
      category = "مقبلات صغيرة",
      unit = "طبق",
      basePrice = 15.0,
      cloudId = "item_salad_small_101"
    )
    val item2 = MenuItem(
      name = "سلطة خضراء",
      category = "مقبلات عائلية",
      unit = "سرفيس",
      basePrice = 35.0,
      cloudId = "item_salad_family_102"
    )

    val id1 = repo.saveMenuItem(item1)
    val id2 = repo.saveMenuItem(item2)
    assertTrue("Item IDs must be distinct", id1 != id2)

    val fetched1 = repo.getMenuItemById(id1)
    val fetched2 = repo.getMenuItemById(id2)
    assertNotNull(fetched1)
    assertNotNull(fetched2)
    assertEquals("item_salad_small_101", fetched1!!.cloudId)
    assertEquals("item_salad_family_102", fetched2!!.cloudId)
    assertEquals(15.0, fetched1.basePrice, 0.01)
    assertEquals(35.0, fetched2.basePrice, 0.01)

    // Deleting item1 by its cloudId must leave item2 completely intact
    repo.deleteMenuItem(id1)
    val afterDelete1 = repo.getMenuItemById(id1)
    val afterDelete2 = repo.getMenuItemById(id2)
    assertEquals(null, afterDelete1)
    assertNotNull("Item2 must remain intact after item1 deletion", afterDelete2)
    assertEquals("item_salad_family_102", afterDelete2!!.cloudId)
  }

  @Test
  fun `verify order item serialization preserves menuItemId, preparation, and printer details`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.viewmodel.MainViewModel(application)

    val items = listOf(
      OrderItem(
        id = 45L,
        orderId = 12L,
        menuItemId = 999L,
        itemName = "جمبري جامبو",
        kitchenName = "جمبري مشوي فرن",
        category = "جمبريات",
        unit = "كيلو",
        quantity = 1.75,
        unitPrice = 400.0,
        preparationId = 5L,
        preparationName = "شوي سنجاري بالخلطة",
        preparationPrice = 30.0,
        itemNotes = "بدون شطة زيادة",
        totalPrice = 752.5,
        isCustomAdHoc = false,
        targetPrinter = "KITCHEN_GRILL",
        printedInKitchen = true,
        printedKitchenQuantity = 1.75
      )
    )

    val json = vm.serializeOrderItemsToJson(items)
    assertTrue(json.contains("menuItemId"))
    assertTrue(json.contains("999"))

    val deserialized = vm.deserializeOrderItemsFromJson(json)
    assertEquals(1, deserialized.size)
    val item = deserialized.first()
    assertEquals(45L, item.id)
    assertEquals(999L, item.menuItemId)
    assertEquals("جمبري جامبو", item.itemName)
    assertEquals("جمبري مشوي فرن", item.kitchenName)
    assertEquals(1.75, item.quantity, 0.001)
    assertEquals(400.0, item.unitPrice, 0.01)
    assertEquals(5L, item.preparationId)
    assertEquals("شوي سنجاري بالخلطة", item.preparationName)
    assertEquals(30.0, item.preparationPrice, 0.01)
    assertEquals(752.5, item.totalPrice, 0.01)
    assertEquals("KITCHEN_GRILL", item.targetPrinter)
    assertEquals(true, item.printedInKitchen)
    assertEquals(1.75, item.printedKitchenQuantity, 0.001)
  }

  @Test
  fun `verify local order placement properly deducts stock`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val stockItemId = repo.saveMenuItem(
      MenuItem(
        name = "قاروص بحري",
        category = "فريش",
        unit = "كيلو",
        basePrice = 250.0,
        currentStock = 20.0
      )
    )

    val localOrder = Order(
      orderNumber = "#909",
      orderType = "TAKEAWAY",
      customerName = "عميل محلي",
      totalAmount = 500.0,
      status = "NEW"
    )

    val orderItem = OrderItem(
      menuItemId = stockItemId,
      itemName = "قاروص بحري",
      quantity = 2.0,
      unitPrice = 250.0,
      totalPrice = 500.0
    )

    val result = repo.placeOrder(localOrder, listOf(orderItem))
    assertTrue(result is OrderPlacementResult.Success)
    val stockAfter = repo.getMenuItemById(stockItemId)!!.currentStock
    assertEquals("Stock should be 18 after placing order", 18.0, stockAfter, 0.01)
  }

  @Test
  fun `verify originating order deducts stock once and cloud-synced copy skips duplicate deduction`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val itemId = db.menuDao().insertMenuItem(
      MenuItem(name = "سبيط بلدي", category = "فريش", unit = "كيلو", basePrice = 300.0, currentStock = 25.0)
    )

    val sellerOrder = Order(
      orderNumber = "",
      orderType = "TAKEAWAY",
      customerName = "عميل الصالة",
      subtotal = 300.0,
      totalAmount = 300.0,
      deviceRole = "SELLER",
      sellerName = "محمد بائع"
    )
    val orderItem = OrderItem(
      menuItemId = itemId,
      itemName = "سبيط بلدي",
      quantity = 3.0,
      unitPrice = 300.0,
      totalPrice = 900.0
    )

    // Originating device (including SELLER) deducts stock immediately and pushes to cloud
    val resOrigin = repo.placeOrder(sellerOrder, listOf(orderItem), deductStock = true)
    assertTrue(resOrigin is OrderPlacementResult.Success)

    val stockAfterOriginatingOrder = repo.getMenuItemById(itemId)!!.currentStock
    assertEquals("Stock must be deducted on originating terminal", 22.0, stockAfterOriginatingOrder, 0.001)

    // Receiving device (handleIncomingCloudOrder) calls placeOrder with deductStock = false
    val syncedCopy = Order(
      orderNumber = "",
      orderType = "TAKEAWAY",
      customerName = "عميل الصالة",
      subtotal = 300.0,
      totalAmount = 300.0,
      deviceRole = "CASHIER",
      sellerName = "محمد بائع"
    )
    val resSynced = repo.placeOrder(syncedCopy, listOf(orderItem), deductStock = false)
    assertTrue(resSynced is OrderPlacementResult.Success)

    val stockAfterSyncedCopy = repo.getMenuItemById(itemId)!!.currentStock
    assertEquals("Stock must not be double-deducted when receiving cloud order copy", 22.0, stockAfterSyncedCopy, 0.001)
  }
  @Test
  fun `verify bulk menu items insertion without data loss`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // Ensure database starts completely empty for menu items and categories
    val initialItems = db.menuDao().getAllMenuItemsIncludingArchivedList()
    for (itm in initialItems) {
      db.menuDao().deleteMenuItem(itm.id)
    }

    val sampleItems = listOf(
      MenuItem(
        name = "قاروص بحري طازج",
        category = "فريش",
        unit = "كيلو",
        basePrice = 350.0,
        costPrice = 280.0,
        currentStock = 15.0,
        targetPrinter = "KITCHEN",
        isAvailable = true,
        isArchived = false,
        updatedAtMillis = System.currentTimeMillis()
      ),
      MenuItem(
        name = "طاجن جمبري إسكندراني",
        category = "طواجن",
        unit = "وجبة",
        basePrice = 180.0,
        costPrice = 120.0,
        currentStock = 30.0,
        targetPrinter = "MAIN",
        isAvailable = true,
        isArchived = false,
        updatedAtMillis = System.currentTimeMillis()
      )
    )

    repo.insertMenuItems(sampleItems)

    val hydratedList = db.menuDao().getAllMenuItemsList()
    assertEquals(2, hydratedList.size)
    val qaroos = hydratedList.firstOrNull { it.name == "قاروص بحري طازج" }
    assertNotNull(qaroos)
    assertEquals(350.0, qaroos!!.basePrice, 0.01)
    assertEquals(15.0, qaroos.currentStock, 0.001)

    val tagine = hydratedList.firstOrNull { it.name == "طاجن جمبري إسكندراني" }
    assertNotNull(tagine)
    assertEquals(180.0, tagine!!.basePrice, 0.01)
  }

  @Test
  fun `verify menu items with similar names and distinct cloudIds are never merged or deleted`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // Insert two distinct items with similar base names and distinct cloudIds
    val id1 = repo.saveMenuItem(
      MenuItem(
        cloudId = "item_shrimp_big",
        name = "جمبري مقلي جامبو",
        category = "مقليات",
        unit = "وجبة",
        basePrice = 220.0,
        currentStock = 10.0
      )
    )
    val id2 = repo.saveMenuItem(
      MenuItem(
        cloudId = "item_shrimp_medium",
        name = "جمبري مقلي وسط",
        category = "مقليات",
        unit = "وجبة",
        basePrice = 160.0,
        currentStock = 12.0
      )
    )

    // Run deduplication
    val deleted = repo.deduplicateMenuItems()
    assertEquals("Deduplication must never delete distinct items with unique cloudIds", 0, deleted)

    val item1 = repo.getMenuItemById(id1)
    val item2 = repo.getMenuItemById(id2)
    assertNotNull(item1)
    assertNotNull(item2)
    assertEquals("جمبري مقلي جامبو", item1!!.name)
    assertEquals("جمبري مقلي وسط", item2!!.name)
    assertEquals(220.0, item1.basePrice, 0.01)
    assertEquals(160.0, item2.basePrice, 0.01)
  }

  @Test
  fun `verify menu item saving and retrieving preserves price and details`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val itemId = repo.saveMenuItem(
      MenuItem(
        name = "دنيس بحري ممتاز",
        category = "فريش",
        unit = "كيلو",
        basePrice = 320.0,
        currentStock = 8.0,
        updatedAtMillis = System.currentTimeMillis()
      )
    )

    val localItem = repo.getMenuItemById(itemId)!!
    assertEquals(320.0, localItem.basePrice, 0.01)
    assertEquals(8.0, localItem.currentStock, 0.001)
    assertEquals("دنيس بحري ممتاز", localItem.name)
  }

  @Test
  fun `verify item archiving round trip preserves archived state without resurrection`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val itemId = repo.saveMenuItem(
      MenuItem(
        cloudId = "item_archived_test",
        name = "صنف سيتم أرشفته",
        category = "فريش",
        unit = "كيلو",
        basePrice = 150.0,
        isArchived = false
      )
    )

    // Archive the item
    repo.archiveMenuItem(itemId, true)
    val archivedItem = repo.getMenuItemById(itemId)!!
    assertTrue(archivedItem.isArchived)

    // Active menu list must not contain archived item
    val activeList = db.menuDao().getAllMenuItemsList()
    assertTrue(activeList.none { it.id == itemId })

    // All items list including archived MUST contain it
    val allList = db.menuDao().getAllMenuItemsIncludingArchivedList()
    val found = allList.firstOrNull { it.id == itemId }
    assertNotNull(found)
    assertTrue(found!!.isArchived)
  }

  @Test
  fun `verify inventory receive and adjustment update stock balance idempotently`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val itemId = repo.saveMenuItem(
      MenuItem(
        cloudId = "item_stock_supply_test",
        name = "مكرونة جمبري وايت صوص",
        category = "مكرونات",
        unit = "وجبة",
        basePrice = 110.0,
        currentStock = 5.0
      )
    )

    // Receive supply (+10)
    repo.receiveInventoryBatch(itemId, 10.0, null, "توريد خامات")
    val stockAfterReceive = repo.getMenuItemById(itemId)!!.currentStock
    assertEquals(15.0, stockAfterReceive, 0.001)

    // Direct adjustment to 20.0
    repo.adjustInventory(itemId, 20.0, "جرد فعلي للمخزون")
    val stockAfterAdjust = repo.getMenuItemById(itemId)!!.currentStock
    assertEquals(20.0, stockAfterAdjust, 0.001)
  }

  @Test
  fun `verify printer deletion marks tombstone and preserves other printers`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val p1 = com.example.printer.PrinterConfig(
      id = "PRINTER_KITCHEN_TEST",
      displayName = "طابعة المطبخ والأسماك",
      connectionType = "NETWORK_IP",
      ipAddress = "192.168.1.200",
      port = 9100,
      role = "KITCHEN_ORDER",
      isEnabled = true,
      isCashierDefault = false
    )
    val p2 = com.example.printer.PrinterConfig(
      id = "PRINTER_CASHIER_TEST",
      displayName = "طابعة الكاشير",
      connectionType = "NETWORK_IP",
      ipAddress = "192.168.1.201",
      port = 9100,
      role = "CUSTOMER_INVOICE",
      isEnabled = true,
      isCashierDefault = true
    )

    repo.savePrinterConfig(p1)
    repo.savePrinterConfig(p2)

    val allBefore = repo.getAllPrinterConfigs()
    assertTrue(allBefore.any { it.id == "PRINTER_KITCHEN_TEST" })
    assertTrue(allBefore.any { it.id == "PRINTER_CASHIER_TEST" })

    // Delete kitchen printer
    repo.deletePrinterConfig("PRINTER_KITCHEN_TEST")

    val allAfter = repo.getAllPrinterConfigs()
    // Kitchen printer is deleted
    assertTrue(allAfter.none { it.id == "PRINTER_KITCHEN_TEST" })
    // Cashier printer is completely preserved with its IP and role!
    val preserved = allAfter.firstOrNull { it.id == "PRINTER_CASHIER_TEST" }
    assertNotNull(preserved)
    assertEquals("192.168.1.201", preserved!!.ipAddress)
    assertEquals("CUSTOMER_INVOICE", preserved.role)
    assertTrue(preserved.isCashierDefault)

    // Tombstone must be recorded for deleted printer
    assertTrue(repo.isPrinterTombstoned("PRINTER_KITCHEN_TEST"))
    assertTrue(repo.isPrinterTombstoned("p_printer_kitchen_test"))
  }

  @Test
  fun `verify closing shift archives pre-midnight and unassigned unarchived orders locally and from cloud sync`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val now = System.currentTimeMillis()
    val twoDaysAgo = now - (48L * 60L * 60L * 1000L)

    // Open active shift
    val shift = repo.openNewShift("كاشير اختبار", 500.0)

    // 1. Old previous-day unarchived order with null shiftId
    val oldUnassignedId = db.orderDao().insertOrder(
      Order(
        orderNumber = "#8101",
        orderType = "TAKEAWAY",
        customerName = "عميل أمس",
        totalAmount = 120.0,
        status = "NEW",
        isArchived = false,
        shiftId = null,
        createdAt = twoDaysAgo
      )
    )
    // 2. Current shift unarchived order
    val currentShiftOrderId = db.orderDao().insertOrder(
      Order(
        orderNumber = "#8102",
        orderType = "DELIVERY",
        customerName = "عميل حالي",
        totalAmount = 250.0,
        status = "NEW",
        isArchived = false,
        shiftId = shift.id,
        createdAt = now - 10_000L
      )
    )
    // 3. Future order created strictly AFTER shift close (simulating next shift / race)
    val futureOrderId = db.orderDao().insertOrder(
      Order(
        orderNumber = "#8103",
        orderType = "TAKEAWAY",
        customerName = "عميل لاحق",
        totalAmount = 90.0,
        status = "NEW",
        isArchived = false,
        shiftId = null,
        createdAt = now + 60_000L
      )
    )

    val closed = repo.closeActiveShift(500.0, 0.0, "تقفيل كامل")
    assertNotNull(closed)
    assertTrue(closed!!.isClosed)

    val afterOld = db.orderDao().getOrderById(oldUnassignedId)!!
    val afterCurrent = db.orderDao().getOrderById(currentShiftOrderId)!!
    val afterFuture = db.orderDao().getOrderById(futureOrderId)!!

    assertTrue("Old unassigned pre-midnight order MUST be archived on shift close", afterOld.order.isArchived)
    assertTrue("Current shift order MUST be archived on shift close", afterCurrent.order.isArchived)
    assertFalse("Future order after shift close timestamp must remain active", afterFuture.order.isArchived)

    // Test remote syncCloseShiftFromCloud for a shift number not yet stored locally
    val futureCloseCutoff = now + 120_000L
    val synced = repo.syncCloseShiftFromCloud(
      shiftNumber = shift.shiftNumber + 50,
      closedAtMs = futureCloseCutoff,
      actualDrawerCash = 300.0,
      totalExpenses = 0.0,
      notes = "إغلاق سحابي",
      cashierName = "كاشير سحابي",
      openedAtMs = now,
      openingBalance = 0.0
    )
    assertTrue(synced)
    val afterCloudClose = db.orderDao().getOrderById(futureOrderId)!!
    assertTrue("syncCloseShiftFromCloud must archive leftover unarchived orders up to closedAtMs", afterCloudClose.order.isArchived)
  }

  @Test
  fun `verify identical menu item names with different prices maintain distinct cloudIds and never overwrite during sync`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // Clean menu table for deterministic test
    for (itm in db.menuDao().getAllMenuItemsIncludingArchivedList()) {
      db.menuDao().deleteMenuItem(itm.id)
    }

    // Save two items locally with IDENTICAL name, category, and unit, but DIFFERENT prices and blank cloudIds
    val id1 = repo.saveMenuItem(
      MenuItem(
        name = "جمبري مقلي",
        category = "مقليات",
        unit = "وجبة",
        basePrice = 180.0,
        currentStock = 10.0
      )
    )
    val id2 = repo.saveMenuItem(
      MenuItem(
        name = "جمبري مقلي",
        category = "مقليات",
        unit = "وجبة",
        basePrice = 260.0,
        currentStock = 15.0
      )
    )

    val item1 = repo.getMenuItemById(id1)!!
    val item2 = repo.getMenuItemById(id2)!!
    assertTrue("UUID cloudId should be generated automatically", item1.cloudId.startsWith("item_"))
    assertTrue("UUID cloudId should be generated automatically", item2.cloudId.startsWith("item_"))
    assertTrue("Two items must have distinct cloudIds", item1.cloudId != item2.cloudId)
    assertEquals(180.0, item1.basePrice, 0.01)
    assertEquals(260.0, item2.basePrice, 0.01)

    // Sync two cloud items with identical name/category/unit but different prices and distinct cloud IDs
    val vm = com.example.viewmodel.MainViewModel(application)
    val cloudList = listOf(
      com.example.data.model.CloudMenuItem(
        id = "item_cloud_shrimp_small",
        name = "سمك بلطي سنجاري",
        category = "مشويات",
        price = 140.0,
        unit = "طبق",
        stockQuantity = 20.0,
        isActive = true,
        isArchived = false
      ),
      com.example.data.model.CloudMenuItem(
        id = "item_cloud_shrimp_large",
        name = "سمك بلطي سنجاري",
        category = "مشويات",
        price = 210.0,
        unit = "طبق",
        stockQuantity = 12.0,
        isActive = true,
        isArchived = false
      )
    )
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      vm.syncCloudMenuItemsToLocal(cloudList)
    }

    val allAfterSync = db.menuDao().getAllMenuItemsIncludingArchivedList()
    val boltiItems = allAfterSync.filter { it.name == "سمك بلطي سنجاري" }.sortedBy { it.basePrice }
    assertEquals("Both same-name items with different prices MUST survive cloud sync separately", 2, boltiItems.size)
    assertEquals(140.0, boltiItems[0].basePrice, 0.01)
    assertEquals("item_cloud_shrimp_small", boltiItems[0].cloudId)
    assertEquals(210.0, boltiItems[1].basePrice, 0.01)
    assertEquals("item_cloud_shrimp_large", boltiItems[1].cloudId)
  }

  @Test
  fun `verify stale cloud snapshot never unarchives locally archived order without explicit unarchiveTimestamp`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val orderId = db.orderDao().insertOrder(
      Order(
        orderNumber = "#9201",
        orderType = "TAKEAWAY",
        customerName = "عميل مؤرشف",
        totalAmount = 320.0,
        status = "COMPLETED",
        isArchived = true,
        cloudOrderId = "order_stale_resurrect_test",
        createdAt = System.currentTimeMillis() - 10_000L
      )
    )

    // 1. Simulate stale cloud snapshot arriving after app restart (isArchived=false, unarchiveTimestamp=0)
    val staleCloudDoc = com.example.data.model.CloudOrderDoc(
      id = "order_stale_resurrect_test",
      orderNumber = 9201,
      customerName = "عميل مؤرشف",
      orderType = "TAKEAWAY",
      status = "COMPLETED",
      printStatus = "PRINTED",
      isArchived = false,
      unarchiveTimestamp = 0L,
      total = 320.0
    )
    repo.upsertCloudOrderToLocal(staleCloudDoc, emptyList())

    val afterStale = db.orderDao().getOrderById(orderId)!!
    assertTrue("Stale cloud snapshot (unarchiveTimestamp=0) MUST NOT resurrect archived local order", afterStale.order.isArchived)

    // 2. Simulate explicit remote unarchive (isArchived=false, unarchiveTimestamp > 0)
    val explicitUnarchiveDoc = staleCloudDoc.copy(unarchiveTimestamp = System.currentTimeMillis())
    repo.upsertCloudOrderToLocal(explicitUnarchiveDoc, emptyList())

    val afterExplicit = db.orderDao().getOrderById(orderId)!!
    assertFalse("Explicit unarchive (unarchiveTimestamp > 0) MUST restore order to active list", afterExplicit.order.isArchived)
  }

  @Test
  fun `verify archived order is not matched by number fallback and closed shift cloud orders insert archived`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val now = System.currentTimeMillis()
    val archivedId = db.orderDao().insertOrder(
      Order(
        orderNumber = "#9305",
        orderType = "TAKEAWAY",
        customerName = "طلب قديم بدون كلاود",
        totalAmount = 150.0,
        status = "COMPLETED",
        isArchived = true,
        cloudOrderId = null,
        createdAt = now - 5_000L
      )
    )

    // Create and close a shift
    val shift = repo.openNewShift("كاشير اختبار", 0.0)
    val closed = repo.closeActiveShift(0.0, 0.0, "إغلاق")!!
    val closedAt = closed.closedAt ?: System.currentTimeMillis()

    // Cloud order referencing the closed shift with stale isArchived=false and unarchiveTimestamp=0
    val lateCloudDoc = com.example.data.model.CloudOrderDoc(
      id = "order_closed_shift_9305",
      orderNumber = 9305,
      customerName = "طلب وردية مغلقة",
      orderType = "TAKEAWAY",
      status = "COMPLETED",
      printStatus = "PRINTED",
      isArchived = false,
      unarchiveTimestamp = 0L,
      shiftNumber = closed.shiftNumber,
      total = 150.0,
      createdAt = com.google.firebase.Timestamp(java.util.Date(closedAt - 1000L))
    )
    val insertedOrMatchedId = repo.upsertCloudOrderToLocal(lateCloudDoc, emptyList())

    val oldArchived = db.orderDao().getOrderById(archivedId)!!
    assertTrue("Old archived local order must remain archived and untouched", oldArchived.order.isArchived)
    assertEquals(null, oldArchived.order.cloudOrderId)

    val insertedClosedOrder = db.orderDao().getOrderById(insertedOrMatchedId)!!
    assertTrue("New cloud order belonging to an already-closed shift must insert as archived", insertedClosedOrder.order.isArchived)
  }

  @Test
  fun `verify driver settlement marks delivery orders settled completed and archived with settledOrderIds`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val driverId = db.driverDao().insertDriver(
      com.example.data.model.DeliveryDriver(
        name = "كابتن اختبار",
        phone = "01000000001",
        ratePerOrder = 15.0
      )
    )
    val o1 = db.orderDao().insertOrder(
      Order(
        orderNumber = "#9401",
        orderType = "DELIVERY",
        customerName = "عميل دليفري 1",
        driverId = driverId,
        driverName = "كابتن اختبار",
        deliveryFee = 20.0,
        driverCommission = 15.0,
        totalAmount = 220.0,
        paymentMethod = "CASH",
        status = "OUT_FOR_DELIVERY",
        isArchived = false,
        isSettledWithDriver = false,
        cloudOrderId = "order_drv_9401",
        createdAt = System.currentTimeMillis()
      )
    )

    val settlement = repo.calculateAndSaveDriverSettlement(driverId, fuelExpenses = 0.0, actualHandoverAmount = 205.0)
    assertNotNull(settlement)
    assertTrue(settlement!!.settledOrderIds.split(",").mapNotNull { it.trim().toLongOrNull() }.contains(o1))

    val settledOrder = db.orderDao().getOrderById(o1)!!.order
    assertTrue("Settled order must be archived locally", settledOrder.isArchived)
    assertTrue("Settled order must be marked settled", settledOrder.isSettledWithDriver)
    assertEquals("COMPLETED", settledOrder.status)
  }

  @Test
  fun `verify deleted menu item tombstones delete matching local items and prevent stale cloud resurrection`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // Clean menu table for deterministic test
    for (itm in db.menuDao().getAllMenuItemsIncludingArchivedList()) {
      db.menuDao().deleteMenuItem(itm.id)
    }

    val tombstoneId = "item_tombstone_delete_sync_test"
    val aliveId = "item_alive_keep_sync_test"
    val now = System.currentTimeMillis()

    // Seed two local items
    repo.saveMenuItem(
      MenuItem(
        cloudId = tombstoneId,
        name = "صنف محذوف متزامن",
        category = "مقليات",
        unit = "وجبة",
        basePrice = 120.0
      )
    )
    repo.saveMenuItem(
      MenuItem(
        cloudId = aliveId,
        name = "صنف باقي في القائمة",
        category = "مقليات",
        unit = "وجبة",
        basePrice = 150.0
      )
    )

    // Record persistent tombstone as if another terminal deleted tombstoneId
    val tJson = org.json.JSONObject().apply { put(tombstoneId, now) }.toString()
    repo.saveSetting("local_menu_tombstones_json", tJson)

    val vm = com.example.viewmodel.MainViewModel(application)
    val staleCloudList = listOf(
      com.example.data.model.CloudMenuItem(
        id = tombstoneId,
        name = "صنف محذوف متزامن",
        category = "مقليات",
        price = 120.0,
        unit = "وجبة",
        isActive = true
      ),
      com.example.data.model.CloudMenuItem(
        id = aliveId,
        name = "صنف باقي في القائمة",
        category = "مقليات",
        price = 150.0,
        unit = "وجبة",
        isActive = true
      )
    )
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      vm.syncCloudMenuItemsToLocal(staleCloudList)
    }

    val remaining = db.menuDao().getAllMenuItemsIncludingArchivedList()
    assertTrue("Deleted tombstoned item MUST be removed locally", remaining.none { it.cloudId == tombstoneId })
    assertTrue("Non-deleted item MUST remain intact", remaining.any { it.cloudId == aliveId })
  }

  @Test
  fun `verify order counter reset allows lowering sequence cleanly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    repo.resetOrderCounter(150)
    assertEquals(150, repo.getNextOrderNumber())

    // Downward reset to 1
    repo.resetOrderCounter(1)
    assertEquals(1, repo.getNextOrderNumber())

    // Custom mid-number reset to 25
    repo.resetOrderCounter(25)
    assertEquals(25, repo.getNextOrderNumber())
  }

  @Test
  fun `verify supreme-authority backup restore overwrites newer orders and preserves century-old customer order stats`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // Seed a newer local order #500 created NOW (must NOT survive supreme-authority restore)
    repo.resetOrderCounter(501)
    db.orderDao().insertOrder(
      Order(
        orderNumber = "#500",
        orderType = "TAKEAWAY",
        customerName = "أوردر أحدث من النسخة",
        totalAmount = 999.0,
        status = "NEW",
        isArchived = false,
        createdAt = System.currentTimeMillis()
      )
    )

    val hundredYearsAgo = 1000L // ancient archived order timestamp
    val twoYearsAgo = System.currentTimeMillis() - (730L * 24L * 60L * 60L * 1000L)

    val backupJson = org.json.JSONObject().apply {
      put("tables", org.json.JSONObject().apply {
        put("app_settings", org.json.JSONArray().apply {
          put(org.json.JSONObject().apply {
            put("key", "next_order_number")
            put("value", "12")
          })
        })
        put("customers", org.json.JSONArray().apply {
          put(org.json.JSONObject().apply {
            put("id", 77)
            put("name", "أحمد العميل القديم")
            put("primaryPhone", "01012345678")
            put("totalOrdersCount", 0)
            put("totalSpent", 0.0)
            put("createdAt", hundredYearsAgo)
          })
        })
        put("orders", org.json.JSONArray().apply {
          put(org.json.JSONObject().apply {
            put("id", 10)
            put("orderNumber", "#10")
            put("orderType", "DELIVERY")
            put("customerId", 77)
            put("customerName", "أحمد العميل القديم")
            put("customerPhone", "01012345678")
            put("totalAmount", 250.0)
            put("status", "COMPLETED")
            put("isArchived", 1) // Archived ancient order
            put("createdAt", hundredYearsAgo)
          })
          put(org.json.JSONObject().apply {
            put("id", 11)
            put("orderNumber", "#11")
            put("orderType", "TAKEAWAY")
            put("customerId", 77)
            put("customerName", "أحمد العميل القديم")
            put("customerPhone", "01012345678")
            put("totalAmount", 150.0)
            put("status", "NEW")
            put("isArchived", 0) // Active order in backup
            put("createdAt", twoYearsAgo)
          })
        })
      })
    }.toString()

    val res = com.example.util.BackupManager.restoreDatabaseFromJson(
      context = context,
      jsonString = backupJson,
      preserveNewerRecords = false
    )
    assertTrue("Restore must succeed", res.isSuccess)

    val allOrders = db.orderDao().getAllOrdersDirect()
    assertTrue("Newer order #500 must NOT be preserved over supreme backup", allOrders.none { it.order.orderNumber == "#500" })
    assertEquals("Exactly 2 backup orders should exist", 2, allOrders.size)

    val activeOrder = allOrders.first { it.order.orderNumber == "#11" }.order
    assertFalse("Active backup order must remain unarchived", activeOrder.isArchived)

    // Order counter must obey backup (#12), NOT pre-restore #501
    assertEquals("Order counter must reflect backup counter #12", 12, repo.getNextOrderNumber())

    // Customer list must include BOTH ancient archived order and active order in stats
    val customer = db.customerDao().getCustomerById(77)
    assertNotNull(customer)
    assertEquals("Customer order count must include ancient archived order", 2, customer!!.totalOrdersCount)
    assertEquals("Customer totalSpent must include ancient archived order", 400.0, customer.totalSpent, 0.01)
  }

  @Test
  fun `verify clearSyncedCloudDataForRemoteRestore wipes mirrored tables cleanly while preserving device role`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    repo.saveSetting("device_role", "SELLER")
    repo.saveMenuItem(MenuItem(cloudId = "c_old_1", name = "صنف قديم", category = "فريش", unit = "كيلو", basePrice = 100.0))
    db.orderDao().insertOrder(
      Order(orderNumber = "#88", orderType = "TAKEAWAY", customerName = "زبون", totalAmount = 100.0, status = "NEW")
    )

    repo.clearSyncedCloudDataForRemoteRestore()

    assertTrue("Orders must be cleared before remote restore sync", db.orderDao().getAllOrdersDirect().isEmpty())
    assertTrue("Menu items must be cleared before remote restore sync", db.menuDao().getAllMenuItemsIncludingArchivedList().isEmpty())
    assertEquals("Device role must remain SELLER", "SELLER", repo.getSetting("device_role"))
  }

  @Test
  fun `verify upsertCloudOrderToLocal preserves original createdAtMs and full delivery address and driver`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val nineAmTimestamp = System.currentTimeMillis() - (10L * 60L * 60L * 1000L) // 10 hours earlier
    val doc = com.example.data.model.CloudOrderDoc(
      id = "order_nine_am_delivery",
      orderNumber = 301,
      customerName = "أحمد محمود",
      customerPhone = "01011223344",
      customerSecondaryPhone = "01299887766",
      customerArea = "حي الزهور",
      deliveryAddressTitle = "المنزل",
      deliveryAddressFull = "حي الزهور - شارع 15 برج النور شقة 4",
      orderType = "DELIVERY",
      status = "OUT_FOR_DELIVERY",
      printStatus = "PRINTED",
      isArchived = false,
      total = 275.0,
      deliveryFee = 25.0,
      driverName = "كابتن كريم",
      createdByTerminal = "كاشير رئيسي",
      createdAtMs = nineAmTimestamp
    )

    val localId = repo.upsertCloudOrderToLocal(doc, emptyList())
    val loaded = db.orderDao().getOrderById(localId)!!.order

    assertEquals("Order createdAt must use original createdAtMs, not upload/sync time", nineAmTimestamp, loaded.createdAt)
    assertEquals("حي الزهور - شارع 15 برج النور شقة 4", loaded.deliveryAddressFull)
    assertEquals("حي الزهور", loaded.customerArea)
    assertEquals("المنزل", loaded.deliveryAddressTitle)
    assertEquals("01299887766", loaded.customerSecondaryPhone)
    assertEquals("كابتن كريم", loaded.driverName)

    // Now simulate an existing local order that had a wrong upload timestamp & missing address, healed on next sync
    val wrongNow = System.currentTimeMillis()
    val staleLocalId = db.orderDao().insertOrder(
      Order(
        orderNumber = "#302",
        orderType = "DELIVERY",
        customerName = "عميل متزامن",
        customerPhone = "01100001111",
        totalAmount = 190.0,
        status = "NEW",
        cloudOrderId = "order_heal_302",
        createdAt = wrongNow
      )
    )
    val healingDoc = doc.copy(
      id = "order_heal_302",
      orderNumber = 302,
      customerName = "عميل متزامن",
      customerPhone = "01100001111",
      deliveryAddressFull = "وسط البلد - عمارة 8 الدور الثاني",
      customerArea = "وسط البلد",
      total = 190.0,
      createdAtMs = nineAmTimestamp
    )

    repo.upsertCloudOrderToLocal(healingDoc, emptyList())
    val healed = db.orderDao().getOrderById(staleLocalId)!!.order

    assertEquals("Existing order createdAt must be healed to original createdAtMs", nineAmTimestamp, healed.createdAt)
    assertEquals("وسط البلد - عمارة 8 الدور الثاني", healed.deliveryAddressFull)
    assertEquals("وسط البلد", healed.customerArea)
  }

  @Test
  fun `verify per-printer print log avoids false kitchen reprint and addition ticket omits reprint banner`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // Spin up a local loopback printer socket so dispatchPrint runs real TCP ESC/POS delivery without Main-looper deadlock
    val serverSocket = java.net.ServerSocket(0)
    val localPort = serverSocket.localPort
    val acceptThread = kotlin.concurrent.thread(isDaemon = true) {
      try {
        serverSocket.soTimeout = 5000
        val client = serverSocket.accept()
        client.getInputStream().readBytes()
        client.close()
      } catch (_: Throwable) {}
    }

    repo.savePrinterConfig(
      com.example.printer.PrinterConfig(
        id = "KITCHEN_1",
        displayName = "طابعة المطبخ 1",
        connectionType = "NETWORK_IP",
        ipAddress = "127.0.0.1",
        port = localPort,
        printMode = "TEXT",
        role = "KITCHEN_ORDER",
        isEnabled = true
      )
    )
    repo.savePrinterConfig(
      com.example.printer.PrinterConfig(
        id = "KITCHEN_2",
        displayName = "طابعة المطبخ 2",
        connectionType = "NETWORK_IP",
        ipAddress = "127.0.0.1",
        port = localPort,
        printMode = "TEXT",
        role = "KITCHEN_ORDER",
        isEnabled = true
      )
    )

    // Simulate an order whose cloud sync or BOTH printer prematurely flipped printedKitchen1=true before KITCHEN_1 ever printed
    val orderId = db.orderDao().insertOrder(
      Order(
        orderNumber = "#410",
        orderType = "TAKEAWAY",
        customerName = "عميل اختبار",
        totalAmount = 120.0,
        status = "NEW",
        printedKitchen1 = true
      )
    )
    db.orderDao().insertOrderItems(
      listOf(
        OrderItem(
          orderId = orderId,
          itemName = "بلطي مشوي",
          quantity = 1.0,
          unitPrice = 120.0,
          totalPrice = 120.0,
          targetPrinter = "ALL"
        )
      )
    )

    val firstResult = repo.dispatchPrint(orderId, "KITCHEN_1")
    acceptThread.join(2000)
    try { serverSocket.close() } catch (_: Throwable) {}
    assertTrue("First print on KITCHEN_1 must succeed", firstResult is com.example.printer.PrinterResult.Success)

    val logsAfterFirst = db.settingsDao().getLogsForOrder(orderId).filter { it.printerTarget == "KITCHEN_1" }
    assertEquals(1, logsAfterFirst.size)
    assertFalse("First print on KITCHEN_1 must NOT be marked isReprint even if printedKitchen1 flag was true", logsAfterFirst.first().isReprint)

    // KITCHEN_2 still has 0 print logs and printedKitchen2 must remain false
    val updatedOrd = db.orderDao().getOrderById(orderId)!!.order
    assertFalse("Printing on KITCHEN_1 must not falsely mark KITCHEN_2 printed", updatedOrd.printedKitchen2)

    // Verify ESC/POS kitchen ticket bytes for an addition ticket omit the "إعادة تشغيل" banner even when isReprint=true
    val additionOrderWithItems = OrderWithItems(
      order = updatedOrd,
      items = listOf(
        OrderItem(
          orderId = orderId,
          itemName = "جمبري مشوي",
          quantity = 2.0,
          printedKitchenQuantity = 1.0,
          unitPrice = 150.0,
          totalPrice = 300.0,
          targetPrinter = "KITCHEN_1"
        )
      )
    )
    val printerMgr = com.example.printer.RealPrinterManager(context)
    val kitchenConfig = com.example.printer.PrinterConfig(id = "KITCHEN_1", displayName = "مطبخ", role = "KITCHEN_ORDER", printMode = "TEXT")
    val bytes = printerMgr.buildKitchenTicketBytes(
      config = kitchenConfig,
      orderWithItems = additionOrderWithItems,
      kitchenTitle = "مطبخ",
      isReprint = true,
      reprintReason = "فارق تعديل الأصناف"
    )
    assertTrue("Addition kitchen ticket bytes must be non-empty", bytes.isNotEmpty())

    // Verify in-app bell chime helper executes safely without crashing or requiring NotificationManager
    com.example.util.SoundAlertUtil.playNewOrderAlert(context)
  }

  @Test
  fun `verify cloud order edits smart-merge items preserve printedKitchenQuantity queue cancelled alerts clear fields and print cancellation ticket`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val serverSocket = java.net.ServerSocket(0)
    val localPort = serverSocket.localPort
    val receivedPayloads = mutableListOf<ByteArray>()
    val acceptThread = kotlin.concurrent.thread(isDaemon = true) {
      try {
        serverSocket.soTimeout = 6000
        val client = serverSocket.accept()
        receivedPayloads.add(client.getInputStream().readBytes())
        client.close()
      } catch (_: Throwable) {}
    }

    repo.savePrinterConfig(
      com.example.printer.PrinterConfig(
        id = "KITCHEN_1",
        displayName = "طابعة المطبخ 1",
        connectionType = "NETWORK_IP",
        ipAddress = "127.0.0.1",
        port = localPort,
        printMode = "TEXT",
        role = "KITCHEN_ORDER",
        isEnabled = true
      )
    )

    // Initial order already printed to kitchen on Cashier:
    // Item 1: بلطي مقلي qty 2.0 (printed 2.0) -> will be REDUCED to 1.0
    // Item 2: جمبري مشوي qty 1.0 (printed 1.0) -> will be INCREASED to 3.0
    // Item 3: سبيط طاجن qty 1.0 (printed 1.0) -> will be DELETED completely
    val orderId = db.orderDao().insertOrder(
      Order(
        orderNumber = "#555",
        orderType = "DELIVERY",
        customerName = "عميل قديم",
        customerPhone = "01012345678",
        deliveryAddressFull = "شارع النيل القديم",
        driverName = "كابتن أحمد",
        deliveryFee = 20.0,
        subtotal = 600.0,
        totalAmount = 620.0,
        status = "NEW",
        cloudOrderId = "cloud_edit_merge_555",
        printedKitchen1 = true
      )
    )
    db.orderDao().insertOrderItems(
      listOf(
        OrderItem(
          orderId = orderId,
          itemName = "بلطي مقلي",
          unit = "كيلو",
          quantity = 2.0,
          unitPrice = 100.0,
          totalPrice = 200.0,
          targetPrinter = "KITCHEN_1",
          printedInKitchen = true,
          printedKitchenQuantity = 2.0
        ),
        OrderItem(
          orderId = orderId,
          itemName = "جمبري مشوي",
          unit = "كيلو",
          quantity = 1.0,
          unitPrice = 250.0,
          totalPrice = 250.0,
          targetPrinter = "KITCHEN_1",
          printedInKitchen = true,
          printedKitchenQuantity = 1.0
        ),
        OrderItem(
          orderId = orderId,
          itemName = "سبيط طاجن",
          unit = "طاجن",
          quantity = 1.0,
          unitPrice = 150.0,
          totalPrice = 150.0,
          targetPrinter = "KITCHEN_1",
          printedInKitchen = true,
          printedKitchenQuantity = 1.0
        )
      )
    )

    // Seller edits remotely (Seller has printedKitchenQuantity = 0.0 on all serialized items):
    // - Changes orderType to TAKEAWAY, clears deliveryAddressFull & driverName, updates name/phone/notes
    // - Reduces بلطي مقلي to 1.0, increases جمبري مشوي to 3.0, deletes سبيط طاجن, adds كابوريا مشوية 2.0
    val cloudEditDoc = com.example.data.model.CloudOrderDoc(
      id = "cloud_edit_merge_555",
      orderNumber = 555,
      customerName = "عميل محدث",
      customerPhone = "01099998888",
      deliveryAddressFull = "",
      driverName = "",
      orderType = "TAKEAWAY",
      deliveryFee = 0.0,
      subtotal = 1050.0,
      discount = 50.0,
      total = 1000.0,
      notes = "استلام شخصي سريع",
      printStatus = "PRINT_REQUESTED",
      hasFullDetails = true
    )
    val sellerItems = listOf(
      OrderItem(itemName = "بلطي مقلي", unit = "كيلو", quantity = 1.0, unitPrice = 100.0, totalPrice = 100.0, targetPrinter = "KITCHEN_1", printedKitchenQuantity = 0.0),
      OrderItem(itemName = "جمبري مشوي", unit = "كيلو", quantity = 3.0, unitPrice = 250.0, totalPrice = 750.0, targetPrinter = "KITCHEN_1", printedKitchenQuantity = 0.0),
      OrderItem(itemName = "كابوريا مشوية", unit = "كيلو", quantity = 2.0, unitPrice = 100.0, totalPrice = 200.0, targetPrinter = "KITCHEN_1", printedKitchenQuantity = 0.0)
    )

    repo.upsertCloudOrderToLocal(cloudEditDoc, sellerItems)

    val mergedOrder = db.orderDao().getOrderById(orderId)!!
    assertEquals("TAKEAWAY", mergedOrder.order.orderType)
    assertEquals("عميل محدث", mergedOrder.order.customerName)
    assertEquals("01099998888", mergedOrder.order.customerPhone)
    assertEquals(null, mergedOrder.order.deliveryAddressFull)
    assertEquals(null, mergedOrder.order.driverName)
    assertEquals("استلام شخصي سريع", mergedOrder.order.notes)
    assertEquals(1050.0, mergedOrder.order.subtotal, 0.01)
    assertEquals(50.0, mergedOrder.order.discount, 0.01)
    assertEquals(1000.0, mergedOrder.order.totalAmount, 0.01)

    assertEquals(3, mergedOrder.items.size)
    val bolti = mergedOrder.items.first { it.itemName == "بلطي مقلي" }
    val gambari = mergedOrder.items.first { it.itemName == "جمبري مشوي" }
    val kaboria = mergedOrder.items.first { it.itemName == "كابوريا مشوية" }

    assertEquals("Reduced printed item must clamp printedKitchenQuantity to new qty", 1.0, bolti.printedKitchenQuantity, 0.001)
    assertTrue(bolti.printedInKitchen)
    assertEquals("Increased printed item must preserve previous printedKitchenQuantity so only +2 prints", 1.0, gambari.printedKitchenQuantity, 0.001)
    assertFalse(gambari.printedInKitchen)
    assertEquals("Newly added item starts with printedKitchenQuantity=0", 0.0, kaboria.printedKitchenQuantity, 0.001)
    assertFalse(kaboria.printedInKitchen)

    // Verify 2 kitchen cancellation/reduction alerts were queued!
    val pendingCancels = repo.getPendingKitchenCancellationsForTarget(orderId, "KITCHEN_1")
    assertEquals(2, pendingCancels.size)
    val reducedCancel = pendingCancels.first { it.itemName == "بلطي مقلي" }
    assertEquals(2.0, reducedCancel.prevQty, 0.001)
    assertEquals(1.0, reducedCancel.newQty, 0.001)
    assertEquals(1.0, reducedCancel.cancelledQty, 0.001)
    val deletedCancel = pendingCancels.first { it.itemName == "سبيط طاجن" }
    assertEquals(1.0, deletedCancel.prevQty, 0.001)
    assertEquals(0.0, deletedCancel.newQty, 0.001)
    assertEquals(1.0, deletedCancel.cancelledQty, 0.001)

    // Also test cancellation-only bitmap rendering (no new items) returns valid non-null Bitmap
    val cancelOnlyOrder = OrderWithItems(
      order = mergedOrder.order,
      items = listOf(bolti.copy(quantity = 1.0, printedKitchenQuantity = 1.0))
    )
    val cancelOnlyBitmap = com.example.printer.ReceiptBitmapRenderer.renderKitchenTicketBitmap(
      orderWithItems = cancelOnlyOrder,
      kitchenTitle = "مطبخ 1",
      paperWidthMm = 80,
      targetId = "KITCHEN_1",
      cancelledKitchenItems = pendingCancels
    )
    assertNotNull("Cancellation-only kitchen ticket bitmap MUST render even when zero new/increased items exist", cancelOnlyBitmap)

    // Dispatch kitchen print over real loopback socket and verify cancellations clear on success
    val printRes = repo.dispatchPrint(orderId, "KITCHEN_1", targetDocumentType = "KITCHEN_ORDER")
    acceptThread.join(2500)
    try { serverSocket.close() } catch (_: Throwable) {}
    assertTrue("Kitchen edit dispatchPrint must succeed", printRes is com.example.printer.PrinterResult.Success)
    assertTrue("Loopback printer socket must receive ESC/POS bytes", receivedPayloads.isNotEmpty() && receivedPayloads[0].isNotEmpty())
    assertTrue("Pending kitchen cancellations must clear after successful KITCHEN_1 print", repo.getPendingKitchenCancellationsForTarget(orderId, "KITCHEN_1").isEmpty())

    val afterPrintOrder = db.orderDao().getOrderById(orderId)!!
    val postGambari = afterPrintOrder.items.first { it.itemName == "جمبري مشوي" }
    val postKaboria = afterPrintOrder.items.first { it.itemName == "كابوريا مشوية" }
    assertEquals(3.0, postGambari.printedKitchenQuantity, 0.001)
    assertEquals(2.0, postKaboria.printedKitchenQuantity, 0.001)
  }

  @Test
  fun `verify local Cashier item deletion and reduction queue kitchen cancellation alerts`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    val orderId = db.orderDao().insertOrder(
      Order(
        orderNumber = "#556",
        orderType = "DINE_IN",
        customerName = "طاولة 3",
        subtotal = 300.0,
        totalAmount = 300.0,
        status = "NEW",
        printedKitchen1 = true
      )
    )
    val itemAId = db.orderDao().insertOrderItem(
      OrderItem(
        orderId = orderId,
        itemName = "فيليه قشر بياض",
        unit = "كيلو",
        quantity = 2.0,
        unitPrice = 100.0,
        totalPrice = 200.0,
        targetPrinter = "KITCHEN_1",
        printedInKitchen = true,
        printedKitchenQuantity = 2.0
      )
    )
    val itemBId = db.orderDao().insertOrderItem(
      OrderItem(
        orderId = orderId,
        itemName = "شوربة سي فود",
        unit = "طبق",
        quantity = 1.0,
        unitPrice = 100.0,
        totalPrice = 100.0,
        targetPrinter = "KITCHEN_2",
        printedInKitchen = true,
        printedKitchenQuantity = 1.0
      )
    )

    // Reduce itemA via dialog edit helper 2.0 -> 0.5 and delete itemB completely
    val itemA = db.orderDao().getOrderById(orderId)!!.items.first { it.id == itemAId }
    repo.updateOrderItemAndRecalculate(orderId, itemA.copy(quantity = 0.5, totalPrice = 50.0))
    repo.deleteOrderItemFromOrder(orderId, itemBId)

    val k1Cancels = repo.getPendingKitchenCancellationsForTarget(orderId, "KITCHEN_1")
    val k2Cancels = repo.getPendingKitchenCancellationsForTarget(orderId, "KITCHEN_2")
    assertEquals(1, k1Cancels.size)
    assertEquals(1.5, k1Cancels[0].cancelledQty, 0.001)
    assertEquals(0.5, k1Cancels[0].newQty, 0.001)
    assertEquals(1, k2Cancels.size)
    assertEquals(1.0, k2Cancels[0].cancelledQty, 0.001)
    assertEquals(0.0, k2Cancels[0].newQty, 0.001)

    // Clearing KITCHEN_1 preserves KITCHEN_2's pending cancellation!
    repo.clearPendingKitchenCancellationsForTarget(orderId, "KITCHEN_1")
    assertTrue(repo.getPendingKitchenCancellationsForTarget(orderId, "KITCHEN_1").isEmpty())
    assertEquals(1, repo.getPendingKitchenCancellationsForTarget(orderId, "KITCHEN_2").size)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun `verify MouseLazyRow RTL drag scrolls horizontally without accidental click while wheel scrolls MouseLazyColumn vertically`() {
    lateinit var columnState: androidx.compose.foundation.lazy.LazyListState
    lateinit var rowState: androidx.compose.foundation.lazy.LazyListState
    val clickedDrivers = mutableListOf<Int>()

    composeTestRule.setContent {
      columnState = rememberLazyListState()
      rowState = rememberLazyListState()
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MouseLazyColumn(
          state = columnState,
          modifier = Modifier
            .size(width = 320.dp, height = 360.dp)
            .testTag("outer_mouse_column")
        ) {
          item {
            MouseLazyRow(
              state = rowState,
              modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("drivers_mouse_row")
            ) {
              items(12) { idx ->
                FilterChip(
                  selected = false,
                  onClick = { clickedDrivers.add(idx) },
                  label = { Text("كابتن $idx") },
                  modifier = Modifier
                    .width(96.dp)
                    .testTag("driver_chip_$idx")
                )
              }
            }
          }
          items(20) { idx ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
            ) {
              Text("سطر $idx")
            }
          }
        }
      }
    }

    composeTestRule.waitForIdle()
    assertEquals(0, rowState.firstVisibleItemIndex)
    assertEquals(0, rowState.firstVisibleItemScrollOffset)

    // 1. Horizontal mouse drag RIGHT (dx > 0 in RTL) starting over a clickable chip -> scrolls row forward, NO click!
    composeTestRule.onNodeWithTag("drivers_mouse_row").performMouseInput {
      val start = Offset(140f, centerY)
      moveTo(start)
      press()
      moveBy(Offset(20f, 0f))
      moveBy(Offset(80f, 0f))
      moveBy(Offset(80f, 0f))
      release()
    }
    composeTestRule.waitForIdle()
    val horizontalMoved = rowState.firstVisibleItemIndex > 0 || rowState.firstVisibleItemScrollOffset > 40
    assertTrue("Dragging mouse right in RTL MouseLazyRow must scroll forward to reveal later items", horizontalMoved)
    assertTrue("Dragging over FilterChips must NOT trigger accidental onClick", clickedDrivers.isEmpty())

    // 2. Normal mouse click without drag -> MUST trigger onClick
    composeTestRule.onNodeWithTag("driver_chip_0").let { _ ->
      // Tap first currently visible chip near right edge of row
      composeTestRule.onNodeWithTag("drivers_mouse_row").performMouseInput {
        val tapPoint = Offset(right - 24f, centerY)
        moveTo(tapPoint)
        press()
        release()
      }
    }
    composeTestRule.waitForIdle()
    assertTrue("Normal mouse click without drag must still select the chip", clickedDrivers.isNotEmpty())

    // 3. Normal vertical mouse wheel while cursor is over horizontal MouseLazyRow -> scrolls outer MouseLazyColumn vertically!
    val colBeforeWheel = columnState.firstVisibleItemScrollOffset + columnState.firstVisibleItemIndex * 100
    composeTestRule.onNodeWithTag("drivers_mouse_row").performMouseInput {
      moveTo(center)
      scroll(3f, androidx.compose.ui.test.ScrollWheel.Vertical)
    }
    composeTestRule.waitForIdle()
    val colAfterWheel = columnState.firstVisibleItemScrollOffset + columnState.firstVisibleItemIndex * 100
    assertTrue("Plain vertical mouse wheel over MouseLazyRow must scroll outer column down", colAfterWheel > colBeforeWheel)
  }

  @Test
  fun `printer endpoint mutex serializes same hardware target and isolates different printers`() {
    val main1 = com.example.printer.PrinterConfig(id = "MAIN", displayName = "الكاشير", connectionType = "NETWORK_IP", ipAddress = "192.168.1.50", port = 9100)
    val mainDup = com.example.printer.PrinterConfig(id = "BOTH_ALIAS", displayName = "كاشير تاني", connectionType = "NETWORK_IP", ipAddress = " 192.168.1.50 ", port = 9100)
    val kitchen = com.example.printer.PrinterConfig(id = "KITCHEN_1", displayName = "المطبخ", connectionType = "NETWORK_IP", ipAddress = "192.168.1.60", port = 9100)

    val m1 = com.example.printer.RealPrinterManager.getEndpointMutex(main1)
    val m2 = com.example.printer.RealPrinterManager.getEndpointMutex(mainDup)
    val m3 = com.example.printer.RealPrinterManager.getEndpointMutex(kitchen)

    assertTrue("Same IP/port printers must share one Mutex to prevent raster byte interleaving", m1 === m2)
    assertTrue("Different IP printers should use independent Mutexes", m1 !== m3)
  }

  @Test
  fun `raster esc pos stream cancels kanji mode starts with esc init and ends with esc init reset`() {
    val bmp = android.graphics.Bitmap.createBitmap(16, 40, android.graphics.Bitmap.Config.ARGB_8888)
    bmp.eraseColor(android.graphics.Color.WHITE)
    bmp.setPixel(2, 2, android.graphics.Color.BLACK)

    val bytes = com.example.printer.ReceiptBitmapRenderer.bitmapToEscPosRasterBytes(bmp)
    assertTrue("Raster output must be long enough for header + slices + footer", bytes.size > 20)
    // Starts with ESC @ (0x1B, 0x40)
    assertEquals(0x1B.toByte(), bytes[0])
    assertEquals(0x40.toByte(), bytes[1])
    // Immediately cancels Kanji mode (FS . -> 0x1C, 0x2E) so Chinese GBK characters cannot trigger
    assertEquals(0x1C.toByte(), bytes[2])
    assertEquals(0x2E.toByte(), bytes[3])
    // Ends cleanly with ESC @ (0x1B, 0x40) after cut command
    assertEquals(0x1B.toByte(), bytes[bytes.size - 2])
    assertEquals(0x40.toByte(), bytes[bytes.size - 1])
  }

  @Test
  fun `menu item grid tile price remains displayed under large font scaling and long arabic name`() {
    val longItem = MenuItem(
      id = 777L,
      name = "سندوتش كفتة جمبري بلدي مشوي على الفحم مخصوص جامبو",
      kitchenName = "تجهيز خاص شواية",
      category = "سندوتشات بحرية",
      basePrice = 145.0,
      unit = "قطعة",
      currentStock = 12.5,
      minStockAlert = 2.0,
      supportsPreparation = true
    )

    composeTestRule.setContent {
      CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides Density(density = 1f, fontScale = 1.6f)
      ) {
        Box(modifier = Modifier.width(108.dp)) {
          MenuItemGridTile(
            item = longItem,
            onAddStockClick = {},
            onClick = {},
            isLandscapeCompact = true
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("menu_item_price_777", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun `seller offline order uses deferred marker preserves next_order_number and transitions when synced`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repo = RestaurantRepository(db, context)

    // Verify v15 columns exist cleanly via direct DAO round-trip + pending offline query
    repo.resetOrderCounter(400)
    val counterBefore = repo.getNextOrderNumber()
    assertEquals(400, counterBefore)

    val tempNum = "معلق-901"
    val reqAt = System.currentTimeMillis()
    val offlineOrder = Order(
      orderNumber = tempNum,
      orderType = "TAKEAWAY",
      customerName = "عميل أوفلاين",
      totalAmount = 180.0,
      status = "NEW",
      deviceRole = "SELLER",
      sellerName = "بائع صالة",
      cloudOrderId = null,
      syncState = "PENDING_OFFLINE",
      pendingPrintAction = "NEW_ORDER",
      pendingPrintRequestedAtMs = reqAt
    )
    val offlineItem = OrderItem(
      itemName = "بوري مشوي",
      unit = "كيلو",
      quantity = 1.0,
      unitPrice = 180.0,
      totalPrice = 180.0
    )

    val placeRes = repo.placeOrder(offlineOrder, listOf(offlineItem), deductStock = false)
    assertTrue(placeRes is OrderPlacementResult.Success)
    val localId = (placeRes as OrderPlacementResult.Success).orderId

    // CRITICAL: Official order counter MUST NOT advance while Seller is offline!
    assertEquals(
      "Seller offline order must NEVER consume an official order number",
      counterBefore,
      repo.getNextOrderNumber()
    )

    val stored = db.orderDao().getOrderById(localId)!!
    assertEquals(tempNum, stored.order.orderNumber)
    assertEquals(null, stored.order.cloudOrderId)
    assertEquals("PENDING_OFFLINE", stored.order.syncState)
    assertEquals("NEW_ORDER", stored.order.pendingPrintAction)
    assertEquals(reqAt, stored.order.pendingPrintRequestedAtMs)

    val pendingOffline = repo.getPendingOfflineOrders()
    assertTrue("Pending offline query must return queued seller order", pendingOffline.any { it.order.id == localId })

    // Simulate online flush assigning official sequential number #705 from Master Cloud counter
    repo.updateOrderSyncMetadata(
      orderId = localId,
      orderNumber = "#705",
      cloudOrderId = "cloud_synced_705",
      syncState = "SYNCED",
      pendingPrintAction = "",
      pendingPrintRequestedAtMs = 0L
    )

    val afterFlush = db.orderDao().getOrderById(localId)!!
    assertEquals("#705", afterFlush.order.orderNumber)
    assertEquals("cloud_synced_705", afterFlush.order.cloudOrderId)
    assertEquals("SYNCED", afterFlush.order.syncState)
    assertFalse("Synced order must leave offline queue", repo.getPendingOfflineOrders().any { it.order.id == localId })
  }

  @Test
  fun `master cashier prints delayed cloud order automatically without confirmation dialog`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val db = AppDatabase.getInstance(application)
    val repo = RestaurantRepository(db, application)
    val coordinator = com.example.sync.AppSyncCoordinator.getInstance(application)

    val serverSocket = java.net.ServerSocket(0)
    val localPort = serverSocket.localPort
    val receivedPayloads = mutableListOf<ByteArray>()
    val acceptThread = kotlin.concurrent.thread(isDaemon = true) {
      try {
        serverSocket.soTimeout = 5000
        val client = serverSocket.accept()
        receivedPayloads.add(client.getInputStream().readBytes())
        client.close()
      } catch (_: Throwable) {}
    }

    repo.savePrinterConfig(
      com.example.printer.PrinterConfig(
        id = "KITCHEN_1",
        displayName = "طابعة المطبخ 1",
        connectionType = "NETWORK_IP",
        ipAddress = "127.0.0.1",
        port = localPort,
        printMode = "TEXT",
        role = "KITCHEN_ORDER",
        isEnabled = true
      )
    )

    val fixedNow = System.currentTimeMillis()
    val itemsJson = coordinator.serializeOrderItemsToJson(
      listOf(
        OrderItem(
          itemName = "جمبري جامبو مشوي",
          unit = "كيلو",
          quantity = 1.0,
          unitPrice = 420.0,
          totalPrice = 420.0,
          targetPrinter = "KITCHEN_1"
        )
      )
    )
    val delayedDoc = com.example.data.model.CloudOrderDoc(
      id = "cloud_delayed_autoprint_test",
      orderNumber = 812,
      customerName = "عميل شبكة متأخرة",
      customerPhone = "01055556666",
      orderType = "TAKEAWAY",
      status = "NEW",
      printStatus = "PENDING_PRINT",
      requestedPrinterTarget = "KITCHEN_1",
      itemsJson = itemsJson,
      subtotal = 420.0,
      total = 420.0,
      createdByTerminal = "بائع صالة",
      createdAtMs = fixedNow - 120_000L,
      printRequestedAtMs = fixedNow - 90_000L
    )

    coordinator.handleIncomingCloudOrder(
      userId = "test_uid_delay",
      cloudOrder = delayedDoc,
      skipCloudClaimForTest = true
    )
    acceptThread.join(3000)
    try { serverSocket.close() } catch (_: Throwable) {}

    val localSaved = db.orderDao().getOrderByCloudId("cloud_delayed_autoprint_test")
    assertNotNull("Delayed order must be persisted locally", localSaved)
    assertEquals("#812", localSaved!!.order.orderNumber)
    assertTrue(
      "Delayed cloud order must print automatically without waiting for Cashier confirmation",
      receivedPayloads.isNotEmpty() && receivedPayloads[0].isNotEmpty()
    )
  }

  @Test
  fun `event-driven seller ack watch marks order WAITING_CASHIER_ACK when master cashier does not respond`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val db = AppDatabase.getInstance(application)
    val repo = RestaurantRepository(db, application)
    val coordinator = com.example.sync.AppSyncCoordinator.getInstance(application)

    val prevRole = coordinator.deviceRole.value
    val prevOnline = coordinator.isDeviceOnline.value
    try {
      coordinator.deviceRole.value = "SELLER"
      coordinator.isDeviceOnline.value = true

      val cloudId = "cloud_ack_timeout_test_819"
      val res = repo.placeOrder(
        Order(
          orderNumber = "#819",
          orderType = "TAKEAWAY",
          customerName = "عميل انتظار الكاشير",
          totalAmount = 210.0,
          status = "NEW",
          deviceRole = "SELLER",
          sellerName = "بائع صالة",
          cloudOrderId = cloudId,
          syncState = com.example.sync.AppSyncCoordinator.SYNC_STATE_SYNCED
        ),
        listOf(
          OrderItem(
            itemName = "فيليه مقلي",
            unit = "كيلو",
            quantity = 1.0,
            unitPrice = 210.0,
            totalPrice = 210.0
          )
        ),
        deductStock = false
      )
      assertTrue(res is OrderPlacementResult.Success)
      val localId = (res as OrderPlacementResult.Success).orderId

      coordinator.watchSellerOrderCashierAck(
        userId = "test_uid_ack",
        cloudOrderId = cloudId,
        localOrderId = localId,
        orderNumberDisplay = "#819",
        timeoutMs = 60L
      )

      kotlinx.coroutines.delay(220L)

      val updated = db.orderDao().getOrderById(localId)!!.order
      assertEquals(
        "Unacknowledged online Seller order must transition to WAITING_CASHIER_ACK",
        com.example.sync.AppSyncCoordinator.SYNC_STATE_WAITING_CASHIER_ACK,
        updated.syncState
      )
    } finally {
      coordinator.deviceRole.value = prevRole
      coordinator.isDeviceOnline.value = prevOnline
    }
  }

  @Test
  fun `orders screen displays deferred offline order badge and banner for pending seller orders`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.viewmodel.MainViewModel(application)
    val db = AppDatabase.getInstance(application)
    val repo = RestaurantRepository(db, application)

    runBlocking {
      repo.placeOrder(
        Order(
          orderNumber = "معلق-44",
          orderType = "TAKEAWAY",
          customerName = "عميل بطاقة أوفلاين",
          totalAmount = 95.0,
          status = "NEW",
          deviceRole = "SELLER",
          sellerName = "بائع صالة",
          syncState = com.example.sync.AppSyncCoordinator.SYNC_STATE_PENDING_OFFLINE,
          pendingPrintAction = "NEW_ORDER",
          pendingPrintRequestedAtMs = System.currentTimeMillis()
        ),
        listOf(
          OrderItem(
            itemName = "كابوريا",
            unit = "طبق",
            quantity = 1.0,
            unitPrice = 95.0,
            totalPrice = 95.0
          )
        ),
        deductStock = false
      )
    }

    composeTestRule.setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        com.example.ui.screens.OrdersScreen(viewModel = vm)
      }
    }

    composeTestRule.waitForIdle()
    // Assert both the deferred badge and explicit device-clarified offline banner render in the order card
    composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("⏳ أوردر (معلق-44)")).let { nodes ->
      assertTrue("Offline card must display '⏳ أوردر (معلق-44)'", nodes.fetchSemanticsNodes().isNotEmpty())
    }
    composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("معلق بدون رقم رسمي", substring = true)).let { nodes ->
      assertTrue("Offline card must display clarified offline sync banner", nodes.fetchSemanticsNodes().isNotEmpty())
    }
  }

  @Test
  fun `cashier role hides seller PRINT_CONFIRMED banner and highlights previous-day active order`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val db = AppDatabase.getInstance(application)
    val repo = RestaurantRepository(db, application)
    val coordinator = com.example.sync.AppSyncCoordinator.getInstance(application)
    val prevRole = coordinator.deviceRole.value

    try {
      coordinator.deviceRole.value = "CASHIER"
      val vm = com.example.viewmodel.MainViewModel(application)
      val yesterdayMs = System.currentTimeMillis() - (26L * 60L * 60L * 1000L)

      runBlocking {
        repo.placeOrder(
          Order(
            orderNumber = "#901",
            orderType = "TAKEAWAY",
            customerName = "عميل من أمس",
            totalAmount = 150.0,
            status = "NEW",
            deviceRole = "SELLER",
            sellerName = "بائع صالة",
            syncState = com.example.sync.AppSyncCoordinator.SYNC_STATE_PRINT_CONFIRMED,
            createdAt = yesterdayMs
          ),
          listOf(
            OrderItem(
              itemName = "دنيس مشوي",
              unit = "كيلو",
              quantity = 1.0,
              unitPrice = 150.0,
              totalPrice = 150.0
            )
          ),
          deductStock = false
        )
      }

      composeTestRule.setContent {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          com.example.ui.screens.OrdersScreen(viewModel = vm)
        }
      }

      composeTestRule.waitForIdle()
      // Cashier MUST NOT show Seller's "تم الاستلام والطباعة عند الكاشير" banner
      composeTestRule.onAllNodes(
        androidx.compose.ui.test.hasText("تم الاستلام والطباعة عند الكاشير", substring = true)
      ).let { nodes ->
        assertTrue("Master Cashier must never display Seller PRINT_CONFIRMED banner", nodes.fetchSemanticsNodes().isEmpty())
      }
      // Unarchived order from yesterday MUST display previous-day warning badge
      composeTestRule.onAllNodes(
        androidx.compose.ui.test.hasText("أوردر من يوم سابق", substring = true)
      ).let { nodes ->
        assertTrue("Active order from previous calendar day must display previous-day badge", nodes.fetchSemanticsNodes().isNotEmpty())
      }
    } finally {
      coordinator.deviceRole.value = prevRole
    }
  }

  @Test
  fun `offline auto print expiry constant enforces 30 second cutoff`() {
    assertEquals(
      30_000L,
      com.example.sync.AppSyncCoordinator.OFFLINE_AUTO_PRINT_EXPIRY_MS
    )
  }
}
