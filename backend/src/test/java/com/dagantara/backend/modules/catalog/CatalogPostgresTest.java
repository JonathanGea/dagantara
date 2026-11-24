package com.dagantara.backend.modules.catalog;

import com.dagantara.backend.common.config.TransactionConfig;
import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.ChangeSkuRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.CreateProductRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.OptionRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.OptionValueRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.ProductDetailsRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.SkuMode;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Media;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.ProductDetail;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Variant;
import com.dagantara.backend.modules.catalog.repository.CatalogMediaRepository;
import com.dagantara.backend.modules.catalog.repository.CatalogRepository;
import com.dagantara.backend.modules.catalog.repository.CatalogVariantRepository;
import com.dagantara.backend.modules.catalog.repository.CatalogVariantRepository.VariantRow;
import com.dagantara.backend.modules.catalog.service.CatalogAccess;
import com.dagantara.backend.modules.catalog.service.CatalogMediaService;
import com.dagantara.backend.modules.catalog.service.CatalogService;
import com.dagantara.backend.modules.tenants.repository.TenantAccountRepository;
import com.dagantara.backend.modules.users.dto.AccountSession;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "PRODUCT01_TEST_DB_URL", matches = ".+")
class CatalogPostgresTest {
    private final String schema = "product01_test_" + UUID.randomUUID().toString().replace("-", "");
    private JdbcClient admin;
    private JdbcClient jdbc;
    private AnnotationConfigApplicationContext context;
    private TransactionTemplate transactions;
    private CatalogService catalog;
    private CatalogVariantRepository variants;
    private CatalogMediaService media;
    private AccountSession ownerA;
    private AccountSession ownerB;

    @BeforeAll
    void migrateFixtureSchema() {
        String url = System.getenv("PRODUCT01_TEST_DB_URL");
        String username = System.getenv("PRODUCT01_TEST_DB_USERNAME");
        String password = System.getenv("PRODUCT01_TEST_DB_PASSWORD");
        admin = JdbcClient.create(new DriverManagerDataSource(url, username, password));
        admin.sql("CREATE SCHEMA " + schema).update();

        var source = new DriverManagerDataSource(url, username, password);
        var properties = new Properties();
        properties.setProperty("currentSchema", schema);
        source.setConnectionProperties(properties);
        jdbc = JdbcClient.create(source);
        var manager = new JdbcTransactionManager(source);
        transactions = new TransactionTemplate(manager);
        new ResourceDatabasePopulator(
                migration("01-platform-accounts"), migration("02-tenant-onboarding"),
                migration("03-platform-operators"), migration("04-tenant-accounts"),
                migration("05-help-center-recovery")).execute(source);
        ownerA = createExistingOwner("A");
        ownerB = createExistingOwner("B");
        jdbc.sql("""
                INSERT INTO roles (id, tenant_id, code, name)
                VALUES (gen_random_uuid(), :tenant, 'CUSTOM_READER', 'Custom reader')
                """).param("tenant", ownerA.tenantId()).update();
        new ResourceDatabasePopulator(migration("06-product-catalog"), migration("07-product-media")).execute(source);

        context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> source);
        context.registerBean(JdbcTransactionManager.class, () -> manager);
        context.registerBean(JdbcClient.class, () -> jdbc);
        context.register(TransactionConfig.class, CatalogService.class, CatalogAccess.class, CatalogRepository.class,
                CatalogVariantRepository.class, CatalogMediaService.class, CatalogMediaRepository.class,
                TenantAccountRepository.class);
        context.refresh();
        catalog = context.getBean(CatalogService.class);
        variants = context.getBean(CatalogVariantRepository.class);
        media = context.getBean(CatalogMediaService.class);
    }

    @AfterAll
    void dropFixtureSchema() {
        if (context != null) {
            context.close();
        }
        if (admin != null) {
            admin.sql("DROP SCHEMA IF EXISTS " + schema + " CASCADE").update();
        }
    }

    @Test
    void migrationBackfillsExistingAdminButDoesNotGrantOtherRoles() {
        var accounts = context.getBean(TenantAccountRepository.class);
        assertTrue(accounts.findManager(ownerA).orElseThrow().permissions()
                .containsAll(Set.of("catalog.read", "catalog.manage")));
        assertEquals(0, jdbc.sql("""
                SELECT count(*) FROM role_permissions rp
                JOIN roles r ON r.id = rp.role_id JOIN permissions p ON p.id = rp.permission_id
                WHERE p.code IN ('catalog.read', 'catalog.manage')
                  AND (r.tenant_id IS NULL OR r.code <> 'TENANT_ADMIN')
                """).query(Integer.class).single());
    }

    @Test
    void pmAt02PrimaryCategoryIsDeferredAndBothRelationsEnforceTenant() {
        var ids = createDefaultProduct(ownerA, "CATEGORY-CONSTRAINT");
        UUID replacement = catalog.createCategory(ownerA, "Replacement").id();
        UUID foreign = catalog.createCategory(ownerB, "Foreign").id();

        transactions.executeWithoutResult(status -> {
            jdbc.sql("UPDATE products SET primary_category_id = :replacement WHERE id = :product")
                    .param("replacement", replacement).param("product", ids.product()).update();
            // The FK becomes valid only at commit, after both sides are written.
            linkCategory(ownerA.tenantId(), ids.product(), replacement);
        });
        assertEquals(replacement, catalog.getProduct(ownerA, ids.product()).primaryCategoryId());

        assertSqlState("23503", () -> transactions.executeWithoutResult(status -> {
            int removed = jdbc.sql("DELETE FROM product_categories WHERE product_id = :product AND category_id = :id")
                    .param("product", ids.product()).param("id", replacement).update();
            assertEquals(1, removed);
        }));
        assertSqlState("23503", () -> linkCategory(ownerA.tenantId(), ids.product(), foreign));
        assertSqlState("23503", () -> linkCategory(ownerB.tenantId(), ids.product(), foreign));
        var reread = catalog.getProduct(ownerA, ids.product());
        assertEquals(replacement, reread.primaryCategoryId());
        assertEquals(2, reread.categoryIds().size());
    }

    @Test
    void pmAt05PostgresEnforcesSkuAndCombinationUniquenessAndScopedLookup() {
        var first = createDefaultProduct(ownerA, "SKU-CONSTRAINT");
        var second = createDefaultProduct(ownerB, "SKU-CONSTRAINT");
        assertEquals(first.variant().id(), variants.findBySku(ownerA.tenantId(), " Sku-Constraint ").orElseThrow().id());
        assertEquals(second.variant().id(), variants.findBySku(ownerB.tenantId(), "sku-constraint").orElseThrow().id());
        assertTrue(variants.listForProduct(ownerB.tenantId(), first.product()).isEmpty());
        assertEquals(List.of(second.variant().id()), variants.findByIds(ownerB.tenantId(),
                List.of(first.variant().id(), second.variant().id())).stream().map(VariantRow::id).toList());

        assertSqlState("23505", () -> insertRawVariant(ownerA.tenantId(), first.product(),
                "SKU-CONSTRAINT", "{00000000-0000-0000-0000-000000000001}"));
        assertSqlState("23505", () -> insertRawVariant(ownerA.tenantId(), first.product(), "DIFFERENT-SKU", "{}"));
        assertSqlState("23514", () -> insertRawVariant(ownerA.tenantId(), first.product(), "lowercase", "{}"));
    }

    @Test
    void pmAt03MediaReferencesMustShareProductAndTenantAndBeUnique() {
        var product = createProductWithOptions(ownerA, "Media ownership");
        var other = createProductWithOptions(ownerA, "Other product");
        var foreign = createProductWithOptions(ownerB, "Other tenant");
        var photo = media.addMedia(ownerA, product.id(), "https://example.test/photo.png");
        UUID linked = valueId(product, 0);
        UUID extra = valueId(product, 1);
        var original = media.replaceOptionValues(ownerA, product.id(), photo.id(), Set.of(linked));

        assertSqlState("23503", () -> jdbc.sql("""
                INSERT INTO product_media (id, tenant_id, product_id, url)
                VALUES (gen_random_uuid(), :tenant, :product, 'https://example.test/foreign.png')
                """).param("tenant", ownerB.tenantId()).param("product", product.id()).update());
        assertSqlState("23503", () -> linkMedia(product.id(), photo.id(), valueId(other, 0)));
        assertSqlState("23503", () -> linkMedia(other.id(), photo.id(), valueId(other, 0)));
        assertSqlState("23503", () -> linkMedia(product.id(), photo.id(), valueId(foreign, 0)));
        assertSqlState("23505", () -> linkMedia(product.id(), photo.id(), linked));

        var rejected = assertThrows(ApiException.class, () -> media.replaceOptionValues(
                ownerA, product.id(), photo.id(), Set.of(extra, valueId(other, 0))));
        assertEquals(400, rejected.getStatus().value());
        assertEquals("INVALID_OPTION_VALUES", rejected.getErrorCode());
        assertEquals(List.of(original), catalog.getProduct(ownerA, product.id()).media());
        var repository = context.getBean(CatalogMediaRepository.class);
        assertTrue(repository.listForProduct(ownerB.tenantId(), product.id()).isEmpty());
    }

    @Test
    void pmAt04MediaOrderBreaksTimestampTiesUsingId() {
        var product = createProductWithOptions(ownerA, "Media order");
        UUID earlier = UUID.fromString("ffffffff-0000-0000-0000-000000000003");
        UUID firstTie = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondTie = UUID.fromString("00000000-0000-0000-0000-000000000002");
        jdbc.sql("""
                INSERT INTO product_media (id, tenant_id, product_id, url, created_at)
                VALUES (:secondTie, :tenant, :product, 'https://example.test/2', '2026-01-02T00:00:00Z'),
                       (:earlier, :tenant, :product, 'https://example.test/3', '2026-01-01T00:00:00Z'),
                       (:firstTie, :tenant, :product, 'https://example.test/1', '2026-01-02T00:00:00Z')
                """).param("tenant", ownerA.tenantId()).param("product", product.id())
                .param("secondTie", secondTie).param("earlier", earlier).param("firstTie", firstTie).update();

        assertEquals(List.of(earlier, firstTie, secondTie),
                catalog.getProduct(ownerA, product.id()).media().stream().map(Media::id).toList());
    }

    @Test
    void pmAt04ConcurrentMediaReplacementsLeaveOnlyTheFinalSet() throws Exception {
        var product = createProductWithOptions(ownerA, "Media replacement");
        var photo = media.addMedia(ownerA, product.id(), "https://example.test/replace.png");
        UUID first = valueId(product, 0);
        UUID last = valueId(product, 1);

        try (ExecutorService workers = Executors.newSingleThreadExecutor()) {
            var waiting = new CompletableFuture<Integer>();
            Future<Media> competing = transactions.execute(status -> {
                media.replaceOptionValues(ownerA, product.id(), photo.id(), Set.of(first));
                var replacement = workers.submit(() -> transactions.execute(other -> {
                    waiting.complete(prepareCompetingConnection());
                    return media.replaceOptionValues(ownerA, product.id(), photo.id(), Set.of(last));
                }));
                awaitBlockedByCurrentTransaction(waiting);
                return replacement;
            });
            assertEquals(List.of(last), competing.get(10, TimeUnit.SECONDS).optionValueIds());
        }
        assertEquals(List.of(last), catalog.getProduct(ownerA, product.id()).media().getFirst().optionValueIds());
    }

    @Test
    void publishedDetailsMustRetainPriceAndDraftAllowsNull() {
        var item = createDefaultProduct(ownerA, "PRICE-CHECK");
        var withPrice = new ProductDetailsRequest("Published", null, BigDecimal.ZERO, null);
        catalog.updateDetails(ownerA, item.product(), withPrice);
        jdbc.sql("UPDATE products SET status = 'PUBLISHED' WHERE id = :id").param("id", item.product()).update();

        var withoutPrice = new ProductDetailsRequest("Price removed", null, null, null);
        var rejected = assertThrows(ApiException.class, () -> catalog.updateDetails(ownerA, item.product(), withoutPrice));
        assertEquals(409, rejected.getStatus().value());
        assertEquals("PRODUCT_NOT_READY", rejected.getErrorCode());
        var stored = catalog.getProduct(ownerA, item.product());
        assertEquals(0L, stored.priceIdr());
        assertEquals("Published", stored.name());
        jdbc.sql("UPDATE products SET status = 'DRAFT' WHERE id = :id").param("id", item.product()).update();
        assertNull(catalog.updateDetails(ownerA, item.product(), withoutPrice).priceIdr());
    }

    @Test
    void pmAt06ReceiptLocksSkuChangeAndPreservesFirstRecordedMarker() throws Exception {
        var item = createDefaultProduct(ownerA, "BEFORE-RECEIPT");
        UUID tenant = ownerA.tenantId();
        UUID variant = item.variant().id();
        assertThrows(IllegalTransactionStateException.class, () -> variants.lockBySkus(tenant, List.of("BEFORE-RECEIPT")));
        var fixed = catalog.changeSku(ownerA, item.product(), variant, manual(" correct-receipt "));
        assertEquals(variant, fixed.id());

        transactions.executeWithoutResult(status -> {
            assertEquals(variant, variants.lockBySkus(tenant, List.of("correct-receipt")).getFirst().id());
            variants.markStockRecorded(tenant, List.of(variant));
            status.setRollbackOnly();
        });
        assertFalse(variants.findBySku(tenant, fixed.sku()).orElseThrow().stockRecorded());

        try (ExecutorService workers = Executors.newSingleThreadExecutor()) {
            var waiting = new CompletableFuture<Integer>();
            Future<Variant> competing = transactions.execute(status -> {
                var locked = variants.lockBySkus(tenant, List.of(" CORRECT-receipt ", "correct-receipt"));
                assertEquals(List.of(variant), locked.stream().map(VariantRow::id).toList());
                assertEquals(List.of(), variants.lockBySkus(ownerB.tenantId(), List.of(fixed.sku())));
                var correction = workers.submit(() -> transactions.execute(other -> {
                    waiting.complete(prepareCompetingConnection());
                    return catalog.changeSku(ownerA, item.product(), variant, manual("TOO-LATE"));
                }));
                awaitBlockedByCurrentTransaction(waiting);
                variants.markStockRecorded(tenant, List.of(variant));
                return correction;
            });
            var rejected = assertThrows(ExecutionException.class,
                    () -> competing.get(10, TimeUnit.SECONDS));
            var conflict = assertInstanceOf(ApiException.class, rejected.getCause());
            assertEquals(409, conflict.getStatus().value());
            assertEquals("SKU_LOCKED", conflict.getErrorCode());
        }

        OffsetDateTime firstRecorded = marker(variant);
        assertNotNull(firstRecorded);
        transactions.executeWithoutResult(status -> {
            var locked = variants.lockBySkus(tenant, List.of(fixed.sku())).getFirst();
            assertTrue(locked.stockRecorded());
            variants.markStockRecorded(tenant, List.of(locked.id()));
        });
        assertEquals(firstRecorded, marker(variant));
        assertEquals(variant, variants.findBySku(tenant, fixed.sku()).orElseThrow().id());
        assertTrue(variants.findBySku(tenant, "TOO-LATE").isEmpty());
    }

    @Test
    void pmAt06ReceiptWaitingForSkuEditCannotResolveTheStaleCode() throws Exception {
        var item = createDefaultProduct(ownerA, "OLD-RACING-SKU");
        UUID tenant = ownerA.tenantId();
        String newSku = "NEW-RACING-SKU";
        try (ExecutorService workers = Executors.newSingleThreadExecutor()) {
            var waiting = new CompletableFuture<Integer>();
            Future<List<VariantRow>> competing = transactions.execute(status -> {
                catalog.changeSku(ownerA, item.product(), item.variant().id(), manual(newSku));
                var receipt = workers.submit(() -> transactions.execute(other -> {
                    waiting.complete(prepareCompetingConnection());
                    return variants.lockBySkus(tenant, List.of(item.variant().sku()));
                }));
                awaitBlockedByCurrentTransaction(waiting);
                return receipt;
            });
            assertEquals(List.of(), competing.get(10, TimeUnit.SECONDS));
        }
        assertEquals(item.variant().id(), variants.findBySku(tenant, newSku).orElseThrow().id());
    }

    private int prepareCompetingConnection() {
        jdbc.sql("SET LOCAL lock_timeout = '10s'").update();
        return jdbc.sql("SELECT pg_backend_pid()").query(Integer.class).single();
    }

    private void awaitBlockedByCurrentTransaction(CompletableFuture<Integer> waiting) {
        try {
            int waiter = waiting.get(5, TimeUnit.SECONDS);
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (System.nanoTime() < deadline) {
                boolean blocked = jdbc.sql("SELECT pg_backend_pid() = ANY(pg_blocking_pids(:pid))")
                        .param("pid", waiter).query(Boolean.class).single();
                if (blocked) {
                    return;
                }
                Thread.sleep(20);
            }
            fail("The competing connection did not wait on the variant lock");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail("Interrupted while observing the lock", exception);
        } catch (Exception exception) {
            fail("Could not observe the competing connection", exception);
        }
    }

    private static ClassPathResource migration(String filename) {
        return new ClassPathResource("db/" + filename + ".sql");
    }

    private AccountSession createExistingOwner(String label) {
        UUID tenant = UUID.randomUUID();
        UUID account = UUID.randomUUID();
        UUID role = UUID.randomUUID();
        transactions.executeWithoutResult(status -> {
            jdbc.sql("INSERT INTO tenants (id, name, owner_user_id) VALUES (:tenant, :label, :account)")
                    .param("tenant", tenant).param("label", label).param("account", account).update();
            jdbc.sql("INSERT INTO roles (id, tenant_id, code, name) VALUES (:role, :tenant, 'TENANT_ADMIN', 'Admin')")
                    .param("role", role).param("tenant", tenant).update();
            jdbc.sql("""
                    INSERT INTO users (id, tenant_id, role_id, username, password_hash)
                    VALUES (:account, :tenant, :role, :label, 'unused')
                    """).param("account", account).param("tenant", tenant).param("role", role).param("label", label)
                    .update();
        });
        return new AccountSession(account, tenant, 0, false);
    }

    private DefaultProduct createDefaultProduct(AccountSession owner, String sku) {
        UUID category = catalog.createCategory(owner, sku).id();
        var draft = catalog.createProduct(owner, new CreateProductRequest(sku, null, null, Set.of(category), category,
                null, List.of(), SkuMode.MANUAL, sku));
        return new DefaultProduct(draft.id(), draft.variants().getFirst());
    }

    private ProductDetail createProductWithOptions(AccountSession owner, String name) {
        UUID category = catalog.createCategory(owner, name).id();
        var color = new OptionRequest("Color",
                List.of(new OptionValueRequest("Black", null), new OptionValueRequest("White", null)));
        return catalog.createProduct(owner, new CreateProductRequest(name, null, null, Set.of(category), category,
                null, List.of(color), null, null));
    }

    private static UUID valueId(ProductDetail product, int index) {
        return product.options().getFirst().values().get(index).id();
    }

    private void linkMedia(UUID product, UUID mediaId, UUID value) {
        jdbc.sql("""
                INSERT INTO media_option_values (product_id, media_id, option_value_id)
                VALUES (:product, :media, :value)
                """).param("product", product).param("media", mediaId).param("value", value).update();
    }

    private void linkCategory(UUID tenant, UUID product, UUID category) {
        jdbc.sql("""
                INSERT INTO product_categories (tenant_id, product_id, category_id) VALUES (:tenant, :product, :category)
                """).param("tenant", tenant).param("product", product).param("category", category).update();
    }

    private void insertRawVariant(UUID tenant, UUID product, String sku, String combination) {
        jdbc.sql("""
                INSERT INTO product_variants (id, tenant_id, product_id, sku, sku_mode, combination_key)
                VALUES (gen_random_uuid(), :tenant, :product, :sku, 'MANUAL', CAST(:combination AS uuid[]))
                """).param("tenant", tenant).param("product", product).param("sku", sku)
                .param("combination", combination).update();
    }

    private OffsetDateTime marker(UUID variant) {
        return jdbc.sql("SELECT first_stock_recorded_at FROM product_variants WHERE id = :variant")
                .param("variant", variant).query(OffsetDateTime.class).single();
    }

    private static ChangeSkuRequest manual(String sku) {
        return new ChangeSkuRequest(SkuMode.MANUAL, sku);
    }

    private static void assertSqlState(String expected, Runnable action) {
        Throwable failure = assertThrows(RuntimeException.class, action::run);
        while (failure != null && !(failure instanceof SQLException)) {
            failure = failure.getCause();
        }
        assertEquals(expected, assertInstanceOf(SQLException.class, failure).getSQLState());
    }

    private record DefaultProduct(UUID product, Variant variant) {
    }
}
