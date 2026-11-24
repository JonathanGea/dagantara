package com.dagantara.backend.modules.inventory;

import com.dagantara.backend.common.config.TransactionConfig;
import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.ChangeSkuRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.CreateProductRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.CreateVariantRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.OptionRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.OptionValueRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.SkuMode;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Variant;
import com.dagantara.backend.modules.catalog.repository.CatalogMediaRepository;
import com.dagantara.backend.modules.catalog.repository.CatalogRepository;
import com.dagantara.backend.modules.catalog.repository.CatalogVariantRepository;
import com.dagantara.backend.modules.catalog.service.CatalogAccess;
import com.dagantara.backend.modules.catalog.service.CatalogService;
import com.dagantara.backend.modules.inventory.dto.CorrectInventoryRequest;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.CorrectionLine;
import com.dagantara.backend.modules.inventory.dto.InventoryResponses.ProductStock;
import com.dagantara.backend.modules.inventory.dto.ReceiveInventoryRequest;
import com.dagantara.backend.modules.inventory.dto.ReceiveInventoryRequest.Line;
import com.dagantara.backend.modules.inventory.repository.InventoryCorrectionRepository;
import com.dagantara.backend.modules.inventory.repository.InventoryReceiptRepository;
import com.dagantara.backend.modules.inventory.repository.InventoryStockRepository;
import com.dagantara.backend.modules.inventory.service.InventoryAvailabilityService;
import com.dagantara.backend.modules.inventory.service.InventoryService;
import com.dagantara.backend.modules.tenants.repository.TenantAccountRepository;
import com.dagantara.backend.modules.users.dto.AccountSession;
import com.dagantara.backend.modules.users.repository.RolePermissionRepository;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.dagantara.backend.modules.inventory.service.InventoryAvailabilityService.Availability.IN_STOCK;
import static com.dagantara.backend.modules.inventory.service.InventoryAvailabilityService.Availability.OUT_OF_STOCK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "INVENTORY01_TEST_DB_URL", matches = ".+")
class InventoryPostgresTest {
    private final String schema = "inventory01_test_" + UUID.randomUUID().toString().replace("-", "");
    private JdbcClient admin;
    private JdbcClient jdbc;
    private AnnotationConfigApplicationContext context;
    private TransactionTemplate transactions;
    private RolePermissionRepository roles;
    private CatalogService catalog;
    private InventoryService inventory;
    private InventoryAvailabilityService availability;
    private AccountSession existingOwner;

    @BeforeAll
    void migrateIsolatedSchema() {
        String url = System.getenv("INVENTORY01_TEST_DB_URL");
        String username = System.getenv("INVENTORY01_TEST_DB_USERNAME");
        String password = System.getenv("INVENTORY01_TEST_DB_PASSWORD");
        admin = JdbcClient.create(new DriverManagerDataSource(url, username, password));
        admin.sql("CREATE SCHEMA " + schema).update();
        var dataSource = new DriverManagerDataSource(url, username, password);
        var options = new Properties();
        options.setProperty("currentSchema", schema);
        dataSource.setConnectionProperties(options);
        jdbc = JdbcClient.create(dataSource);
        var transactionManager = new JdbcTransactionManager(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        roles = new RolePermissionRepository(jdbc);
        new ResourceDatabasePopulator(migration("01-platform-accounts"), migration("02-tenant-onboarding"),
                migration("03-platform-operators"), migration("04-tenant-accounts"),
                migration("05-help-center-recovery"), migration("06-product-catalog"),
                migration("07-product-media")).execute(dataSource);
        existingOwner = createOwner(false);
        new ResourceDatabasePopulator(migration("08-inventory-receipts"),
                migration("09-inventory-corrections")).execute(dataSource);

        context = new AnnotationConfigApplicationContext();
        context.registerBean(JdbcClient.class, () -> jdbc);
        context.registerBean(JdbcTransactionManager.class, () -> transactionManager);
        context.register(TransactionConfig.class, CatalogAccess.class, CatalogRepository.class,
                CatalogVariantRepository.class, CatalogMediaRepository.class, CatalogService.class,
                TenantAccountRepository.class,
                InventoryReceiptRepository.class, InventoryCorrectionRepository.class, InventoryStockRepository.class,
                InventoryService.class, InventoryAvailabilityService.class);
        context.refresh();
        inventory = context.getBean(InventoryService.class);
        availability = context.getBean(InventoryAvailabilityService.class);
        catalog = context.getBean(CatalogService.class);
    }

    @AfterAll
    void dropIsolatedSchema() {
        if (context != null) {
            context.close();
        }
        if (admin != null) {
            admin.sql("DROP SCHEMA IF EXISTS " + schema + " CASCADE").update();
        }
    }

    @Test
    void inventoryPermissionsReachExistingAndNewOwnersOnlyByDefault() {
        var accounts = context.getBean(TenantAccountRepository.class);
        Set<String> required = Set.of("inventory.read", "inventory.receive", "inventory.correct");
        assertTrue(accounts.findManager(existingOwner).orElseThrow().permissions().containsAll(required));
        assertTrue(accounts.findManager(createOwner(true)).orElseThrow().permissions().containsAll(required));
        assertEquals(0L, jdbc.sql("""
                SELECT count(*) FROM role_permissions rp JOIN permissions p ON p.id = rp.permission_id
                JOIN roles r ON r.id = rp.role_id
                WHERE p.code IN (:codes) AND (r.tenant_id IS NULL OR r.code <> 'TENANT_ADMIN')
                """).param("codes", required).query(Long.class).single());
    }

    @Test
    void imAt01AndIm05MergeReceiptLinesAndReadCommittedTenantAvailability() {
        AccountSession owner = createOwner(true);
        AccountSession other = createOwner(true);
        Product stockA = product(owner, "SHARED-SKU");
        Product stockB = product(owner, "EXTRA");
        Product sameSkuElsewhere = product(other, "SHARED-SKU");
        Product empty = product(owner, "EMPTY");
        UUID unknown = UUID.randomUUID();
        var ids = List.of(stockA.variant().id(), stockB.variant().id(), sameSkuElsewhere.variant().id(),
                empty.variant().id(), unknown);

        assertEquals(Map.of(stockA.variant().id(), OUT_OF_STOCK, stockB.variant().id(), OUT_OF_STOCK,
                empty.variant().id(), OUT_OF_STOCK), availability.availability(owner.tenantId(), ids));
        assertEquals(Map.of(), availability.availability(owner.tenantId(), List.of()));
        assertEquals(0L, warehouseCount(owner));

        var result = transactions.execute(status -> {
            var receipt = inventory.receive(owner, "shared-key", request(line(" shared-sku ", 3),
                    line("EXTRA", 5), line("SHARED-SKU", 2)));
            assertEquals(OUT_OF_STOCK, CompletableFuture.supplyAsync(() -> availability
                    .availability(owner.tenantId(), ids).get(stockA.variant().id())).join());
            return receipt;
        });
        assertEquals(owner.id(), result.receivedBy());
        assertEquals(1L, warehouseCount(owner));
        assertEquals(2, result.lines().size());
        assertTrue(result.lines().stream().allMatch(line -> line.quantity() == 5));
        assertEquals(Map.of(stockA.variant().id(), IN_STOCK, stockB.variant().id(), IN_STOCK,
                empty.variant().id(), OUT_OF_STOCK), availability.availability(owner.tenantId(), ids));
        assertEquals(5L, quantity(owner, stockA));

        var elsewhere = inventory.receive(other, "shared-key", request(line("SHARED-SKU", 7)));
        assertNotEquals(result.warehouseId(), elsewhere.warehouseId());
        assertEquals(Map.of(sameSkuElsewhere.variant().id(), IN_STOCK), availability.availability(other.tenantId(), ids));
        assertEquals(404, assertThrows(ApiException.class, () -> inventory.getReceipt(other, result.id()))
                .getStatus().value());
        assertEquals(List.of(elsewhere.id()), inventory.listReceipts(other, PageRequest.of(0, 1))
                .stream().map(receipt -> receipt.id()).toList());
    }

    @Test
    void imAt02RejectedSubmissionLeavesNoWarehouseLinesBalanceMarkerOrKey() {
        AccountSession owner = createOwner(true);
        Product current = product(owner, "VALID");
        product(createOwner(true), "FOREIGN");
        assertApiFailure("INVALID_SKU", () ->
                inventory.receive(owner, "reusable", request(line("VALID", 2), line("FOREIGN", 3))));
        assertApiFailure("INVALID_QUANTITY", () -> inventory.receive(owner, "reusable", request(line("VALID", 2),
                new Line(new TextNode("VALID"), new DecimalNode(new BigDecimal("1.5"))))));
        assertEquals(0L, countReceipts(owner));
        assertEquals(0L, warehouseCount(owner));
        assertEquals(0L, quantity(owner, current));
        assertNull(marker(current));
        inventory.receive(owner, "reusable", request(line("VALID", 3)));
        assertEquals(3L, quantity(owner, current));
        assertEquals(1L, countReceipts(owner));
    }

    @Test
    void imAt02BalanceOverflowRollsBackAllWritesAndLeavesTheKeyReusable() {
        AccountSession owner = createOwner(true);
        Product one = product(owner, "ONE");
        Product two = product(owner, "TWO");
        // Use PostgreSQL's UUID ordering so the fresh variant is written before the overflowing variant.
        boolean oneFirst = jdbc.sql("SELECT CAST(:one AS uuid) < CAST(:two AS uuid)")
                .param("one", one.variant().id()).param("two", two.variant().id()).query(Boolean.class).single();
        Product fresh = oneFirst ? one : two;
        Product full = oneFirst ? two : one;
        inventory.receive(owner, "fill", request(line(full.variant().sku(), Long.MAX_VALUE)));
        assertApiFailure("INVALID_QUANTITY", () -> inventory.receive(owner, "overflow",
                request(line(fresh.variant().sku(), 4), line(full.variant().sku(), 1))));
        assertEquals(1L, countReceipts(owner));
        assertEquals(Long.MAX_VALUE, quantity(owner, full));
        assertEquals(0L, quantity(owner, fresh));
        assertNull(marker(fresh));
        inventory.receive(owner, "overflow", request(line(fresh.variant().sku(), 4)));
        assertEquals(4L, quantity(owner, fresh));
    }

    @Test
    void imAt03ParallelIdenticalRetryWaitsAndReturnsTheOriginalReceipt() throws Exception {
        AccountSession owner = createOwner(true);
        Product product = product(owner, "RETRY");
        var payload = request(line("RETRY", 6));
        try (ExecutorService workers = Executors.newSingleThreadExecutor()) {
            var initial = transactions.execute(status -> {
                var first = inventory.receive(owner, "concurrent-key", payload);
                var retry = whileCurrentTransactionHoldsLocks(workers,
                        () -> inventory.receive(owner, "concurrent-key", payload));
                return Map.entry(first, retry);
            });
            assertEquals(initial.getKey(), initial.getValue().get(10, TimeUnit.SECONDS));
        }
        assertEquals(1L, countReceipts(owner));
        assertEquals(6L, quantity(owner, product));
        assertApiFailure("IDEMPOTENCY_CONFLICT", () ->
                inventory.receive(owner, "concurrent-key", request(line("RETRY", 7))));
        var secondAccount = transactions.execute(status -> {
            UUID role = jdbc.sql("SELECT role_id FROM users WHERE id = :id")
                    .param("id", owner.id()).query(UUID.class).single();
            return insertUser(owner.tenantId(), role);
        });
        var separate = inventory.receive(secondAccount, "concurrent-key", request(line("RETRY", 2)));
        assertEquals(secondAccount.id(), separate.receivedBy());
        assertEquals(2L, countReceipts(owner));
        assertEquals(8L, quantity(owner, product));
    }

    @Test
    void imAt09ParallelReceiptsAddBothChangesToTheSameWarehouse() throws Exception {
        AccountSession owner = createOwner(true);
        Product product = product(owner, "PARALLEL");
        try (ExecutorService workers = Executors.newSingleThreadExecutor()) {
            var receipts = transactions.execute(status -> {
                var first = inventory.receive(owner, "first", request(line("PARALLEL", 8)));
                var second = whileCurrentTransactionHoldsLocks(workers,
                        () -> inventory.receive(owner, "second", request(line("PARALLEL", 5))));
                return Map.entry(first, second);
            });
            var first = receipts.getKey();
            var second = receipts.getValue().get(10, TimeUnit.SECONDS);
            assertNotEquals(first.id(), second.id());
            assertEquals(first.warehouseId(), second.warehouseId());
        }
        assertEquals(2L, countReceipts(owner));
        assertEquals(1L, warehouseCount(owner));
        assertEquals(13L, quantity(owner, product));
    }

    @Test
    void parallelFirstReceiptsForDifferentVariantsCreateOneDefaultWarehouse() throws Exception {
        AccountSession owner = createOwner(true);
        Product firstProduct = product(owner, "FIRST-SKU");
        Product secondProduct = product(owner, "SECOND-SKU");
        try (ExecutorService workers = Executors.newSingleThreadExecutor()) {
            var receipts = transactions.execute(status -> {
                var first = inventory.receive(owner, "first", request(line("FIRST-SKU", 2)));
                var second = whileCurrentTransactionHoldsLocks(workers,
                        () -> inventory.receive(owner, "second", request(line("SECOND-SKU", 3))));
                return Map.entry(first, second);
            });
            assertEquals(receipts.getKey().warehouseId(), receipts.getValue().get(10, TimeUnit.SECONDS).warehouseId());
        }
        assertEquals(1L, warehouseCount(owner));
        assertEquals(2L, quantity(owner, firstProduct));
        assertEquals(3L, quantity(owner, secondProduct));
    }

    @Test
    void pmAt06ReceiptLocksSkuAndCompetingEditFailsWithoutChangingTheVariant() {
        AccountSession owner = createOwner(true);
        Product product = product(owner, "INITIAL");
        UUID id = product.variant().id();
        var fixed = catalog.changeSku(owner, product.id(), id, manual("FIXED"));
        assertEquals(id, fixed.id());
        try (ExecutorService workers = Executors.newSingleThreadExecutor()) {
            Future<Variant> editor = transactions.execute(status -> {
                var receipt = inventory.receive(owner, "record-fixed", request(line("FIXED", 4)));
                assertEquals(id, receipt.lines().getFirst().variantId());
                return whileCurrentTransactionHoldsLocks(workers,
                        () -> catalog.changeSku(owner, product.id(), id, manual("TOO-LATE")));
            });
            assertRejectedFuture("SKU_LOCKED", editor);
        }
        OffsetDateTime first = marker(product);
        assertNotNull(first);
        inventory.receive(owner, "second", request(line("FIXED", 2)));
        assertEquals(first, marker(product));
        assertEquals(6L, quantity(owner, product));
        assertEquals("FIXED", inventory.getProductStock(owner, product.id()).variants().getFirst().sku());
    }

    @Test
    void pmAt06ReceiptWaitingBehindSkuEditRejectsStaleSkuAndCanRetryWithTheNewCode() {
        AccountSession owner = createOwner(true);
        Product product = product(owner, "OLD");
        try (ExecutorService workers = Executors.newSingleThreadExecutor()) {
            var receipt = transactions.execute(status -> {
                catalog.changeSku(owner, product.id(), product.variant().id(), manual("NEW"));
                return whileCurrentTransactionHoldsLocks(workers,
                        () -> inventory.receive(owner, "after-edit", request(line("OLD", 8))));
            });
            assertRejectedFuture("INVALID_SKU", receipt);
        }
        assertEquals(0L, countReceipts(owner));
        assertEquals(0L, warehouseCount(owner));
        assertNull(marker(product));
        var result = inventory.receive(owner, "after-edit", request(line("NEW", 8)));
        assertEquals(product.variant().id(), result.lines().getFirst().variantId());
        assertEquals("NEW", result.lines().getFirst().sku());
        assertEquals(8L, quantity(owner, product));
    }

    @Test
    void imAt04AndImAt08CorrectionsAuditPreviousQuantitiesAndKeepOriginalReceipt() {
        var owner = createOwner(true);
        var item = product(owner, "CORRECTED");
        var receipt = inventory.receive(owner, "first", request(line("CORRECTED", 8)));
        var otherReceipt = inventory.receive(owner, "second", request(line("CORRECTED", 1)));
        UUID line = receipt.lines().getFirst().id();
        var toSix = correction("Counted six", line, 6);
        var first = inventory.correct(owner, receipt.id(), "first", toSix);
        var second = inventory.correct(owner, receipt.id(), "second", correction("Found one", line, 7));

        assertEquals(first, inventory.correct(owner, receipt.id(), "first", toSix));
        assertApiFailure("IDEMPOTENCY_CONFLICT", () ->
                inventory.correct(owner, receipt.id(), "first", correction("Changed reason", line, 6)));
        assertApiFailure("IDEMPOTENCY_CONFLICT", () ->
                inventory.correct(owner, otherReceipt.id(), "first", toSix));
        assertEquals(owner.id(), first.correctedBy());
        assertEquals(receipt.id(), first.receiptId());
        assertEquals("Counted six", first.reason());
        assertEquals(new CorrectionLine(line, 8, 6, -2), first.lines().getFirst());
        assertEquals(new CorrectionLine(line, 6, 7, 1), second.lines().getFirst());
        var detail = inventory.getReceipt(owner, receipt.id());
        assertEquals(List.of(first, second), detail.corrections());
        assertEquals(8L, detail.lines().getFirst().originalQuantity());
        assertEquals(7L, detail.lines().getFirst().effectiveQuantity());
        assertEquals(receipt, inventory.receive(owner, "first", request(line("CORRECTED", 8))));
        assertEquals(8L, quantity(owner, item));

        UUID variant = item.variant().id();
        var ids = List.of(variant, UUID.randomUUID());
        assertEquals(Map.of(variant, IN_STOCK), availability.availability(owner.tenantId(), ids));
        inventory.correct(owner, otherReceipt.id(), "zero-other",
                correction("Other units were not delivered", otherReceipt.lines().getFirst().id(), 0));
        var zero = transactions.execute(status -> {
            var result = inventory.correct(owner, receipt.id(), "zero", correction("Wrong SKU", line, 0));
            assertEquals(Map.of(variant, IN_STOCK), CompletableFuture.supplyAsync(
                    () -> availability.availability(owner.tenantId(), ids)).join());
            return result;
        });
        assertEquals(Map.of(variant, OUT_OF_STOCK), availability.availability(owner.tenantId(), ids));
        assertEquals(2L, countReceipts(owner));
        assertEquals(List.of(first, second, zero), inventory.getReceipt(owner, receipt.id()).corrections());
        assertNotNull(marker(item));
    }

    @Test
    void imAt05AndImAt07RejectInvalidCorrectionsWithoutPartialChangesAndReleaseFailedKey() {
        var owner = createOwner(true);
        var other = createOwner(true);
        var item = product(owner, "A");
        var secondItem = product(owner, "B");
        var receipt = inventory.receive(owner, "one", request(line("A", 8), line("B", 8)));
        var otherReceipt = inventory.receive(owner, "two", request(line("A", 1)));
        var first = receipt.lines().getFirst();
        var last = receipt.lines().getLast();
        var invalidLine = otherReceipt.lines().getFirst().id();
        var valid = correction("Valid recount", first.id(), 6);
        var reader = transactions.execute(status -> {
            UUID role = jdbc.sql("""
                    INSERT INTO roles (id, tenant_id, code, name)
                    VALUES (gen_random_uuid(), :tenant, 'READER', 'Reader') RETURNING id
                    """).param("tenant", owner.tenantId()).query(UUID.class).single();
            jdbc.sql("""
                    INSERT INTO role_permissions (role_id, permission_id)
                    SELECT :role, id FROM permissions WHERE code = 'inventory.read'
                    """).param("role", role).update();
            return insertUser(owner.tenantId(), role);
        });
        var before = inventory.getReceipt(reader, receipt.id());
        assertApiFailure("VALIDATION_ERROR", () ->
                inventory.correct(owner, receipt.id(), "empty", correction(" \t", first.id(), 6)));
        assertApiFailure("INVALID_RECEIPT_LINE", () ->
                inventory.correct(owner, receipt.id(), "wrong-line", correction("Wrong line", invalidLine, 0)));
        assertEquals(403, assertThrows(ApiException.class, () ->
                inventory.correct(reader, receipt.id(), "restricted", valid)).getStatus().value());
        assertEquals(404, assertThrows(ApiException.class, () ->
                inventory.correct(other, receipt.id(), "foreign", valid)).getStatus().value());
        assertEquals(before, inventory.getReceipt(reader, receipt.id()));
        assertEquals(9L, quantity(owner, item));
        assertEquals(8L, quantity(owner, secondItem));

        // Simulate a lower balance on the last variant to exercise guarded updates and rollback.
        jdbc.sql("UPDATE inventory_stock_balances SET quantity = 0 WHERE tenant_id = :tenant AND variant_id = :id")
                .param("tenant", owner.tenantId()).param("id", last.variantId()).update();
        var beforeQuantities = availabilityQuantities(owner, item, secondItem);
        var rejected = new CorrectInventoryRequest(new TextNode("Count both"), List.of(
                correctionLine(first.id(), 6), correctionLine(last.id(), 6)));
        assertApiFailure("STOCK_BALANCE_CONFLICT", () ->
                inventory.correct(owner, receipt.id(), "balance", rejected));
        assertEquals(before, inventory.getReceipt(reader, receipt.id()));
        assertEquals(beforeQuantities, availabilityQuantities(owner, item, secondItem));
        var accepted = inventory.correct(owner, receipt.id(), "balance", valid);
        assertEquals(List.of(accepted), inventory.getReceipt(reader, receipt.id()).corrections());
        assertEquals(8L, inventory.getReceipt(reader, receipt.id()).lines().getLast().effectiveQuantity());
    }

    @Test
    void imAt06AggregatesStockByProductAndValueIdentityAfterCorrection() {
        var owner = createOwner(true);
        UUID category = catalog.createCategory(owner, "Kaos").id();
        var product = catalog.createProduct(owner, new CreateProductRequest("Kaos Basic", null, null, Set.of(category),
                category, null, List.of(
                        new OptionRequest("Warna", List.of(new OptionValueRequest("Hitam", null),
                                new OptionValueRequest("Putih", null))),
                        new OptionRequest("Ukuran", List.of(new OptionValueRequest("M", null),
                                new OptionValueRequest("L", null)))), null, null));
        Map<String, UUID> values = product.options().stream().flatMap(option -> option.values().stream())
                .collect(Collectors.toMap(value -> value.label(), value -> value.id()));
        catalog.createVariant(owner, product.id(), new CreateVariantRequest(
                List.of(values.get("Hitam"), values.get("M")), SkuMode.MANUAL, "H-M"));
        catalog.createVariant(owner, product.id(), new CreateVariantRequest(
                List.of(values.get("Hitam"), values.get("L")), SkuMode.MANUAL, "H-L"));
        catalog.createVariant(owner, product.id(), new CreateVariantRequest(
                List.of(values.get("Putih"), values.get("L")), SkuMode.MANUAL, "P-L"));
        var defaultProduct = product(owner, "NO-OPTIONS");
        var receipt = inventory.receive(owner, "aggregate", request(line("H-M", 8), line("H-L", 2),
                line("P-L", 5), line("NO-OPTIONS", 3)));
        var before = inventory.getProductStock(owner, product.id());
        assertEquals(BigInteger.valueOf(15), before.totalQuantity());
        assertEquals(Map.of(values.get("Hitam"), 10L, values.get("Putih"), 5L,
                values.get("M"), 8L, values.get("L"), 7L), optionQuantities(before));
        UUID receiptLine = receipt.lines().stream().filter(line -> line.sku().equals("H-M")).findFirst().orElseThrow().id();
        inventory.correct(owner, receipt.id(), "recount", correction("Counted six", receiptLine, 6));
        var after = inventory.getProductStock(owner, product.id());
        assertEquals(BigInteger.valueOf(13), after.totalQuantity());
        assertEquals(Map.of(values.get("Hitam"), 8L, values.get("Putih"), 5L,
                values.get("M"), 6L, values.get("L"), 7L), optionQuantities(after));
        var defaultStock = inventory.getProductStock(owner, defaultProduct.id());
        assertEquals(BigInteger.valueOf(3), defaultStock.totalQuantity());
        assertEquals(List.of(), defaultStock.optionTotals());
    }

    @Test
    void imAt09ParallelCorrectionsUseLastEffectiveQuantityAndSerializeWithNewReceipts() throws Exception {
        var owner = createOwner(true);
        var item = product(owner, "PARALLEL-CORRECTION");
        var receipt = inventory.receive(owner, "start", request(line("PARALLEL-CORRECTION", 8)));
        UUID line = receipt.lines().getFirst().id();
        var payload = correction("First recount", line, 6);
        try (ExecutorService workers = Executors.newSingleThreadExecutor()) {
            var results = transactions.execute(status -> {
                var first = inventory.correct(owner, receipt.id(), "correct-first", payload);
                var second = whileCurrentTransactionHoldsLocks(workers, () ->
                        inventory.correct(owner, receipt.id(), "correct-second", correction("Next recount", line, 7)));
                return Map.entry(first, second);
            });
            var first = results.getKey();
            var second = results.getValue().get(10, TimeUnit.SECONDS);
            assertEquals(new CorrectionLine(line, 8, 6, -2), first.lines().getFirst());
            assertEquals(new CorrectionLine(line, 6, 7, 1), second.lines().getFirst());
            assertEquals(List.of(first, second), inventory.getReceipt(owner, receipt.id()).corrections());
            var received = transactions.execute(status -> {
                inventory.correct(owner, receipt.id(), "correct-third", correction("New count", line, 4));
                return whileCurrentTransactionHoldsLocks(workers, () ->
                        inventory.receive(owner, "new-receipt", request(line("PARALLEL-CORRECTION", 5))));
            });
            assertEquals(receipt.warehouseId(), received.get(10, TimeUnit.SECONDS).warehouseId());
            var retryPayload = correction("Retry count", line, 3);
            var retry = transactions.execute(status -> {
                var firstTry = inventory.correct(owner, receipt.id(), "retry", retryPayload);
                var duplicate = whileCurrentTransactionHoldsLocks(workers, () ->
                        inventory.correct(owner, receipt.id(), "retry", retryPayload));
                return Map.entry(firstTry, duplicate);
            });
            assertEquals(retry.getKey(), retry.getValue().get(10, TimeUnit.SECONDS));
        }
        assertEquals(8L, quantity(owner, item));
        assertEquals(4, inventory.getReceipt(owner, receipt.id()).corrections().size());
    }

    @Test
    void postgresRejectsCrossTenantStockAndKeysWithoutReceipts() {
        AccountSession owner = createOwner(true);
        Product item = product(owner, "OWNED");
        Product foreign = product(createOwner(true), "OTHER");
        var receipt = inventory.receive(owner, "valid", request(line(item.variant().sku(), 5)));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.sql("""
                INSERT INTO inventory_stock_balances (tenant_id, warehouse_id, variant_id, quantity)
                VALUES (:tenant, :warehouse, :variant, 1)
                """).param("tenant", owner.tenantId()).param("warehouse", receipt.warehouseId())
                .param("variant", foreign.variant().id()).update());
        assertThrows(DataIntegrityViolationException.class, () -> transactions.executeWithoutResult(status ->
                jdbc.sql("""
                        INSERT INTO inventory_receipt_keys
                            (tenant_id, account_id, idempotency_key, request_hash, receipt_id)
                        VALUES (:tenant, :account, 'orphan', :hash, gen_random_uuid())
                        """).param("tenant", owner.tenantId()).param("account", owner.id())
                        .param("hash", new byte[32]).update()));
    }

    private AccountSession createOwner(boolean permissionsSeeded) {
        return transactions.execute(status -> {
            UUID tenant = UUID.randomUUID();
            UUID account = UUID.randomUUID();
            jdbc.sql("INSERT INTO tenants (id, name, owner_user_id) VALUES (:tenant, 'Fixture', :account)")
                    .param("tenant", tenant).param("account", account).update();
            UUID role;
            if (permissionsSeeded) {
                role = roles.createTenantAdminRole(tenant);
            } else {
                role = jdbc.sql("""
                        INSERT INTO roles (id, tenant_id, code, name)
                        VALUES (gen_random_uuid(), :tenant, 'TENANT_ADMIN', 'Admin') RETURNING id
                        """).param("tenant", tenant).query(UUID.class).single();
                jdbc.sql("""
                        INSERT INTO roles (id, tenant_id, code, name)
                        VALUES (gen_random_uuid(), :tenant, 'CUSTOM_READER', 'Reader')
                        """).param("tenant", tenant).update();
            }
            insertUser(tenant, role, account);
            return new AccountSession(account, tenant, 0, false);
        });
    }

    private AccountSession insertUser(UUID tenant, UUID role) {
        UUID account = UUID.randomUUID();
        insertUser(tenant, role, account);
        return new AccountSession(account, tenant, 0, false);
    }

    private void insertUser(UUID tenant, UUID role, UUID account) {
        jdbc.sql("""
                INSERT INTO users (id, tenant_id, role_id, username, password_hash)
                VALUES (:account, :tenant, :role, :name, 'unused')
                """).param("account", account).param("tenant", tenant).param("role", role)
                .param("name", account.toString()).update();
    }

    private Product product(AccountSession owner, String sku) {
        UUID category = catalog.createCategory(owner, sku).id();
        var draft = catalog.createProduct(owner, new CreateProductRequest(sku, null, null, Set.of(category),
                category, null, List.of(), SkuMode.MANUAL, sku));
        return new Product(draft.id(), draft.variants().getFirst());
    }

    private long warehouseCount(AccountSession account) {
        return jdbc.sql("SELECT count(*) FROM inventory_warehouses WHERE tenant_id = :tenant")
                .param("tenant", account.tenantId()).query(Long.class).single();
    }

    private long countReceipts(AccountSession account) {
        return inventory.listReceipts(account, PageRequest.of(0, 1)).getTotalElements();
    }

    private long quantity(AccountSession account, Product product) {
        return inventory.getProductStock(account, product.id()).variants().getFirst().quantity();
    }

    private List<Long> availabilityQuantities(AccountSession account, Product first, Product second) {
        return List.of(quantity(account, first), quantity(account, second));
    }

    private static Map<UUID, Long> optionQuantities(ProductStock stock) {
        return stock.optionTotals().stream().flatMap(option -> option.values().stream())
                .collect(Collectors.toMap(value -> value.valueId(), value -> value.totalQuantity().longValueExact()));
    }

    private static CorrectInventoryRequest correction(String reason, UUID line, long quantity) {
        return new CorrectInventoryRequest(new TextNode(reason), List.of(correctionLine(line, quantity)));
    }

    private static CorrectInventoryRequest.Line correctionLine(UUID line, long quantity) {
        return new CorrectInventoryRequest.Line(line, LongNode.valueOf(quantity));
    }

    private OffsetDateTime marker(Product product) {
        return jdbc.sql("""
                SELECT first_stock_recorded_at FROM product_variants
                WHERE id = :variant AND first_stock_recorded_at IS NOT NULL
                """).param("variant", product.variant().id()).query(OffsetDateTime.class).optional().orElse(null);
    }

    private <T> Future<T> whileCurrentTransactionHoldsLocks(ExecutorService worker, Supplier<T> competing) {
        var connection = new CompletableFuture<Integer>();
        var future = worker.submit(() -> transactions.execute(status -> {
            jdbc.sql("SET LOCAL lock_timeout = '10s'").update();
            connection.complete(jdbc.sql("SELECT pg_backend_pid()").query(Integer.class).single());
            return competing.get();
        }));
        try {
            int pid = connection.get(5, TimeUnit.SECONDS);
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (System.nanoTime() < deadline) {
                if (jdbc.sql("SELECT pg_backend_pid() = ANY(pg_blocking_pids(:pid))")
                        .param("pid", pid).query(Boolean.class).single()) {
                    return future;
                }
                Thread.sleep(20);
            }
            return fail("The competing PostgreSQL transaction did not wait for the current transaction");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return fail("Interrupted while observing PostgreSQL locking", exception);
        } catch (ExecutionException | java.util.concurrent.TimeoutException exception) {
            return fail("Could not observe the competing PostgreSQL connection", exception);
        }
    }

    private static void assertApiFailure(String code, Runnable request) {
        assertEquals(code, assertThrows(ApiException.class, request::run).getErrorCode());
    }

    private static void assertRejectedFuture(String code, Future<?> future) {
        var rejected = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
        assertEquals(code, assertInstanceOf(ApiException.class, rejected.getCause()).getErrorCode());
    }

    private static ReceiveInventoryRequest request(Line... lines) {
        return new ReceiveInventoryRequest(List.of(lines));
    }

    private static Line line(String sku, long quantity) {
        return new Line(new TextNode(sku), LongNode.valueOf(quantity));
    }

    private static ChangeSkuRequest manual(String sku) {
        return new ChangeSkuRequest(SkuMode.MANUAL, sku);
    }

    private static ClassPathResource migration(String filename) {
        return new ClassPathResource("db/" + filename + ".sql");
    }

    private record Product(UUID id, Variant variant) {
    }
}
