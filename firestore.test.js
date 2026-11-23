const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- Security Bounds & Validation Tests ---

test("Unauthenticated user: cannot read or write menu items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("menu_items").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("menu_items").doc("item_1").set({
      id: "item_1",
      name: "جمبري مقلي",
      category: "مأكولات بحرية",
      price: 150.0,
      createdAt: new Date(),
    })
  );
});

test("Cross-user isolation: Bob cannot read Alice's menu items or orders", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("menu_items").doc("item_1").set({
      id: "item_1",
      name: "جمبري مقلي جامبو",
      category: "جمبري",
      price: 220.0,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("menu_items").doc("item_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("menu_items").get());
});

test("Authenticated owner: can create and read menu items with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("menu_items").doc("item_jumbo").set({
      id: "item_jumbo",
      name: "جمبري مقلي جامبو",
      category: "جمبري",
      price: 240.0,
      costPrice: 180.0,
      stockQuantity: 25.5,
      unit: "كيلو",
      isActive: true,
      createdAt: now,
    })
  );

  // Similar item (different ID) - never merged or rejected
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("menu_items").doc("item_medium").set({
      id: "item_medium",
      name: "جمبري مقلي وسط",
      category: "جمبري",
      price: 170.0,
      costPrice: 130.0,
      stockQuantity: 40.0,
      unit: "كيلو",
      isActive: true,
      createdAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("menu_items").get());
});

test("Schema validation: rejects invalid menu items (negative price, bad ID)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Negative price
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("menu_items").doc("item_bad").set({
      id: "item_bad",
      name: "سمك بلطي",
      category: "أسماك",
      price: -10.0,
      createdAt: now,
    })
  );

  // ID mismatch with document path
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("menu_items").doc("item_path").set({
      id: "item_different",
      name: "سمك بلطي",
      category: "أسماك",
      price: 80.0,
      createdAt: now,
    })
  );
});

test("Cloud Orders: can create order with PENDING_PRINT and update to PRINTED", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloud_orders").doc("order_1001").set({
      id: "order_1001",
      orderNumber: 1,
      customerName: "أحمد علي",
      customerPhone: "01012345678",
      orderType: "TAKEAWAY",
      status: "PENDING",
      printStatus: "PENDING_PRINT",
      itemsJson: JSON.stringify([{ name: "جمبري مقلي جامبو", qty: 2, price: 240 }]),
      subtotal: 480.0,
      deliveryFee: 0.0,
      discount: 0.0,
      total: 480.0,
      createdByTerminal: "cashier_terminal_1",
      createdAt: now,
    })
  );

  // Master device updates printStatus to PRINTED
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloud_orders").doc("order_1001").update({
      printStatus: "PRINTED",
      printedByTerminal: "master_printer_terminal",
    })
  );
});

test("Menu Item: targetPrinter, kitchenName, isArchived, supportsPreparation allowed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("menu_items").doc("item_fish_1").set({
      id: "item_fish_1",
      name: "سمك دنيس مشوي",
      category: "أسماك فريش",
      price: 320.0,
      costPrice: 240.0,
      stockQuantity: 15.0,
      minStockAlert: 5.0,
      unit: "كيلو",
      targetPrinter: "KITCHEN_1,KITCHEN_2",
      kitchenName: "دنيس شوي زيت وليمون",
      supportsPreparation: true,
      isArchived: false,
      isActive: true,
      createdAt: now,
    })
  );
});

test("Cloud Orders: can create DINE_IN order from seller terminal", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloud_orders").doc("order_dinein_1").set({
      id: "order_dinein_1",
      orderNumber: 801,
      customerName: "زبون صالة",
      orderType: "DINE_IN",
      status: "PENDING",
      printStatus: "PENDING_PRINT",
      itemsJson: "[]",
      total: 350.0,
      createdByTerminal: "بائع صالة 1",
      createdAt: now,
    })
  );
});

test("Counters: can read and update shared order counter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("counters").doc("order_counter").set({
      lastOrderNumber: 850
    })
  );
  const snap = await aliceDb.collection("users").doc(ALICE_UID).collection("counters").doc("order_counter").get();
  if (snap.data().lastOrderNumber !== 850) {
    throw new Error("Counter value mismatch");
  }
});

test("Cloud Orders: supports SAVED_ONLY, PRINT_REQUESTED, PRINTING, isArchived, and status sync", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("cloud_orders").doc("order_saved_1");

  await assertSucceeds(
    ref.set({
      id: "order_saved_1",
      orderNumber: 855,
      customerName: "عميل صالة",
      orderType: "TAKEAWAY",
      status: "NEW",
      printStatus: "SAVED_ONLY",
      requestedPrinterTarget: "",
      isArchived: false,
      shiftNumber: 4,
      itemsJson: "[]",
      total: 180.0,
      createdByTerminal: "أحمد",
      createdAt: now,
    })
  );

  // Remote individual print request from seller
  await assertSucceeds(
    ref.update({
      printStatus: "PRINT_REQUESTED",
      requestedPrinterTarget: "KITCHEN_1",
      reprintReason: "نسخة ثانية",
    })
  );

  // Cashier claims printing lock and archives
  await assertSucceeds(
    ref.update({
      printStatus: "PRINTING",
    })
  );
  await assertSucceeds(
    ref.update({
      printStatus: "PRINTED",
      isArchived: true,
      status: "CANCELLED",
    })
  );
});

test("Shifts: authenticated owner can open and close shift; cross-user blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  const aliceShiftRef = aliceDb.collection("users").doc(ALICE_UID).collection("shifts").doc("shift_5");
  await assertSucceeds(
    aliceShiftRef.set({
      id: "shift_5",
      shiftNumber: 5,
      cashierName: "كاشير رئيسي",
      openedAtMs: 1700000000000,
      openingBalance: 500.0,
      isClosed: false,
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceShiftRef.update({
      isClosed: true,
      closedAtMs: 1700010000000,
      totalCashSales: 2500.0,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("shifts").doc("shift_5").get());
});


