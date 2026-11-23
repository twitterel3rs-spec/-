package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CloudMenuItem
import com.example.data.model.CloudOrderDoc
import com.example.data.model.CloudShiftDoc
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirestoreCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertMenuItem_similarNames_keepsBothDistinctWithoutMerging() = runBlocking {
        val aliceUid: String = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreCloudRepository(firestore, auth)

        val item1 = CloudMenuItem(
            id = "item_jumbo_fried",
            name = "جمبري مقلي جامبو",
            category = "جمبري",
            price = 260.0,
            costPrice = 190.0,
            stockQuantity = 15.0,
            unit = "كيلو"
        )
        val item2 = CloudMenuItem(
            id = "item_medium_fried",
            name = "جمبري مقلي وسط",
            category = "جمبري",
            price = 180.0,
            costPrice = 130.0,
            stockQuantity = 30.0,
            unit = "كيلو"
        )

        val res1 = withTimeout(DEFAULT_TIMEOUT_MS) { repository.upsertMenuItem(aliceUid, item1) }
        val res2 = withTimeout(DEFAULT_TIMEOUT_MS) { repository.upsertMenuItem(aliceUid, item2) }

        assertTrue(res1.isSuccess)
        assertTrue(res2.isSuccess)

        val items = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeMenuItems(aliceUid).first { list: List<CloudMenuItem> -> list.size >= 2 }
        }
        val ids = items.map { it.id }
        assertTrue(ids.contains("item_jumbo_fried"))
        assertTrue(ids.contains("item_medium_fried"))
    }

    @Test
    fun createOrder_sellerTerminal_createsOrderWithPendingPrint() = runBlocking {
        val aliceUid: String = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreCloudRepository(firestore, auth)

        val order = CloudOrderDoc(
            id = "order_9901",
            orderNumber = 1,
            customerName = "عميل تجريبي",
            customerPhone = "01122334455",
            orderType = "TAKEAWAY",
            status = "PENDING",
            printStatus = "PENDING_PRINT",
            itemsJson = "[{\"name\":\"سمك بوري سنجاري\",\"qty\":1,\"price\":120}]",
            subtotal = 120.0,
            total = 120.0,
            createdByTerminal = "cashier_2"
        )

        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createOrder(aliceUid, order) }
        if (!createRes.isSuccess) {
            System.err.println("DEBUG CREATE_ORDER FAILED: ${createRes.exceptionOrNull()?.message}")
            createRes.exceptionOrNull()?.printStackTrace()
        }
        assertTrue("Create order failed: ${createRes.exceptionOrNull()?.message}", createRes.isSuccess)

        // Master terminal marks order as printed
        val printRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.markOrderPrinted(aliceUid, "order_9901", "master_device")
        }
        assertTrue(printRes.isSuccess)

        val orders = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeCloudOrders(aliceUid).first { list: List<CloudOrderDoc> ->
                list.any { it.id == "order_9901" && it.printStatus == "PRINTED" }
            }
        }
        val printedOrder = orders.first { it.id == "order_9901" }
        assertEquals("master_device", printedOrder.printedByTerminal)
    }

    @Test
    fun crossUser_bobCannotAccessAliceItems_fails() = runBlocking {
        val aliceUid: String = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreCloudRepository(firestore, auth)
        val item = CloudMenuItem(
            id = "item_alice_special",
            name = "طاجن بطارخ",
            category = "طواجن",
            price = 300.0
        )
        aliceRepo.upsertMenuItem(aliceUid, item)

        val bobUid: String = signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreCloudRepository(firestore, auth)

        var failed = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.observeMenuItems(aliceUid).first()
            }
        } catch (e: Exception) {
            failed = true
        }
        assertTrue("Expected cross-user access to fail", failed)
    }

    @Test
    fun unauthenticated_signOut_failsWithPermissionDenied() = runBlocking {
        val aliceUid: String = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreCloudRepository(firestore, auth)

        auth.signOut()

        var caughtException: Throwable? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.observeMenuItems(aliceUid).first()
            }
        } catch (e: Exception) {
            caughtException = e
        }
        assertNotNull("Expected unauthenticated query to throw exception", caughtException)
        val firestoreException = caughtException?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun getAndIncrementOrderNumber_allocatesSequentialNumbers() = runBlocking {
        val aliceUid: String = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreCloudRepository(firestore, auth)

        val num1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getAndIncrementOrderNumber(aliceUid, 800)
        }
        val num2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getAndIncrementOrderNumber(aliceUid, 800)
        }
        assertEquals(800, num1)
        assertEquals(801, num2)
    }

    @Test
    fun upsertMenuItem_archivedTrue_persistsAndObservesAsArchived() = runBlocking {
        val aliceUid: String = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreCloudRepository(firestore, auth)

        val archivedItem = CloudMenuItem(
            id = "item_archived_test",
            name = "جمبري مشوي مؤرشف",
            category = "جمبري",
            price = 310.0,
            targetPrinter = "KITCHEN_1",
            kitchenName = "جمبري جريل",
            supportsPreparation = true,
            isArchived = true,
            isActive = true
        )

        val res = withTimeout(DEFAULT_TIMEOUT_MS) { repository.upsertMenuItem(aliceUid, archivedItem) }
        assertTrue(res.isSuccess)

        val observed = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeMenuItems(aliceUid).first { list ->
                list.any { it.id == "item_archived_test" }
            }
        }
        val item = observed.first { it.id == "item_archived_test" }
        assertTrue("Expected isArchived=true to survive Firestore round-trip", item.isArchived)
        assertEquals("KITCHEN_1", item.targetPrinter)
        assertEquals("جمبري جريل", item.kitchenName)
        assertTrue(item.supportsPreparation)
    }

    @Test
    fun tryClaimOrderForPrinting_allowsOnlyFirstClaim_preventsDoublePrint() = runBlocking {
        val aliceUid: String = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreCloudRepository(firestore, auth)

        val order = CloudOrderDoc(
            id = "order_atomic_claim_1",
            orderNumber = 888,
            customerName = "عميل صالة",
            orderType = "DINE_IN",
            status = "NEW",
            printStatus = "PENDING_PRINT",
            itemsJson = "[]",
            total = 250.0,
            createdByTerminal = "محمود"
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.createOrder(aliceUid, order) }.isSuccess)

        val firstClaim = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.tryClaimOrderForPrinting(aliceUid, "order_atomic_claim_1")
        }
        val secondClaim = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.tryClaimOrderForPrinting(aliceUid, "order_atomic_claim_1")
        }

        assertTrue("First claim must succeed", firstClaim)
        assertFalse("Second concurrent claim must fail to prevent double print", secondClaim)

        // Seller can still trigger explicit remote individual print later
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.requestRemotePrint(aliceUid, "order_atomic_claim_1", "KITCHEN_2", "نسخة ثانية")
            }.isSuccess
        )
        val thirdClaim = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.tryClaimOrderForPrinting(aliceUid, "order_atomic_claim_1")
        }
        assertTrue("Explicit PRINT_REQUESTED should be claimable once", thirdClaim)
    }

    @Test
    fun upsertShift_andObserveShifts_syncsOpenAndClosedAcrossDevices() = runBlocking {
        val aliceUid: String = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreCloudRepository(firestore, auth)

        val openShift = CloudShiftDoc(
            id = "shift_12",
            shiftNumber = 12,
            cashierName = "أحمد كاشير",
            openedAtMs = 1700000000000L,
            openingBalance = 1000.0,
            isClosed = false
        )
        val openRes = withTimeout(DEFAULT_TIMEOUT_MS) { repository.upsertShift(aliceUid, openShift) }
        assertTrue("Open shift failed: ${openRes.exceptionOrNull()?.message}", openRes.isSuccess)

        val closedShift = openShift.copy(
            closedAtMs = 1700020000000L,
            totalCashSales = 4500.0,
            isClosed = true,
            notes = "إغلاق مسائي"
        )
        val closeRes = withTimeout(DEFAULT_TIMEOUT_MS) { repository.upsertShift(aliceUid, closedShift) }
        assertTrue("Close shift failed: ${closeRes.exceptionOrNull()?.message}", closeRes.isSuccess)

        val observed = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeShifts(aliceUid).first { list ->
                list.any { it.id == "shift_12" && it.isClosed }
            }
        }
        assertEquals(4500.0, observed.first { it.id == "shift_12" }.totalCashSales, 0.01)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_pos@test.com"
        const val BOB_EMAIL = "bob_pos@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
