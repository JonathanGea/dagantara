package com.dagantara.backend.modules.catalog;

import com.dagantara.backend.DagantaraApplication;
import com.dagantara.backend.modules.catalog.dto.CatalogRequests.CreateProductRequest;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.ProductStatus;
import com.dagantara.backend.modules.catalog.service.CatalogService;
import com.dagantara.backend.modules.users.dto.AccountSession;
import com.dagantara.backend.modules.users.repository.RolePermissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "PRODUCT03_TEST_DB_URL", matches = ".+")
class StorefrontPostgresTest {
    private static final String PRODUCT_API = "/api/v1/storefront/products";
    private static final String SHOP = "/products";
    private static final String SCRIPT_LIKE_NAME = "</script><script>untrusted</script>";

    private final String schema = "product03_test_" + UUID.randomUUID().toString().replace("-", "");
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private JdbcClient admin;
    private ServletWebServerApplicationContext application;
    private int port;
    private UUID publicProduct;
    private UUID draftProduct;

    @BeforeAll
    void startOnIsolatedSchemaAndRandomPort() {
        String url = System.getenv("PRODUCT03_TEST_DB_URL");
        String user = System.getenv("PRODUCT03_TEST_DB_USERNAME");
        String password = System.getenv("PRODUCT03_TEST_DB_PASSWORD");
        admin = JdbcClient.create(new DriverManagerDataSource(url, user, password));
        admin.sql("CREATE SCHEMA " + schema).update();
        String scopedUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        new ResourceDatabasePopulator(migration("01-platform-accounts"), migration("02-tenant-onboarding"),
                migration("03-platform-operators"), migration("04-tenant-accounts"), migration("05-help-center-recovery"),
                migration("06-product-catalog"), migration("07-product-media"), migration("08-inventory-receipts"),
                migration("09-inventory-corrections"), migration("10-store-settings"))
                .execute(new DriverManagerDataSource(scopedUrl, user, password));

        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        application = (ServletWebServerApplicationContext) new SpringApplicationBuilder(DagantaraApplication.class)
                .run("--spring.datasource.url=" + scopedUrl, "--spring.datasource.username=" + user,
                        "--spring.datasource.password=" + password, "--server.port=0",
                        "--server.address=127.0.0.1", "--app.tenant.platform-host=localhost",
                        "--app.security.jwt.secret-base64=" + Base64.getEncoder().encodeToString(key));
        port = application.getWebServer().getPort();
        var owner = owner("localhost", SHOP);
        owner("127.0.0.1", "");
        var catalog = application.getBean(CatalogService.class);
        UUID category = catalog.createCategory(owner, "Fixture").id();
        publicProduct = catalog.createProduct(owner, product(category, SCRIPT_LIKE_NAME, "PUBLIC")).id();
        draftProduct = catalog.createProduct(owner, product(category, "Draft", "DRAFT")).id();
        catalog.updatePublication(owner, publicProduct, ProductStatus.PUBLISHED);
    }

    @AfterAll
    void releaseFixture() {
        if (application != null) {
            application.close();
        }
        if (admin != null) {
            admin.sql("DROP SCHEMA IF EXISTS " + schema + " CASCADE").update();
        }
    }

    @Test
    void pmAt08PrefixedPagesResolveEvenWhenShopSlugIsProductsAndEscapeDocumentData() throws Exception {
        var list = fetch("GET", "localhost", SHOP + "/products");
        assertEquals(200, list.statusCode());
        assertTrue(list.body().contains(publicProduct.toString()));
        assertFalse(list.body().contains(draftProduct.toString()));
        var detail = fetch("GET", "localhost", SHOP + "/products/" + publicProduct);
        assertEquals(200, detail.statusCode());
        assertTrue(detail.headers().firstValue("Content-Security-Policy").orElseThrow().contains("script-src 'nonce-"));
        assertTrue(detail.body().contains("\\u003c/script>\\u003cscript>untrusted\\u003c/script>"));
        assertFalse(detail.body().contains(SCRIPT_LIKE_NAME));
        assertTrue(detail.body().contains("\"priceIdr\":\"9223372036854775807\""));
        assertEquals(404, fetch("GET", "localhost", SHOP + "/products/" + draftProduct).statusCode());
    }

    @Test
    void pmAt08MissingForeignAndDraftAreEquivalentAndWritesStayProtected() throws Exception {
        var positive = fetch("GET", "localhost", SHOP + PRODUCT_API + "/" + publicProduct);
        assertEquals(200, positive.statusCode());
        var variant = json.readTree(positive.body()).at("/data/variants/0");
        assertEquals("OUT_OF_STOCK", variant.path("availability").asText());
        assertEquals(3, variant.size());

        for (var response : List.of(
                fetch("GET", "localhost", SHOP + PRODUCT_API + "/" + draftProduct),
                fetch("GET", "127.0.0.1", PRODUCT_API + "/" + publicProduct),
                fetch("GET", "localhost", "/unknown" + PRODUCT_API + "/" + publicProduct),
                fetch("GET", "localhost", PRODUCT_API + "/" + publicProduct))) {
            assertEquals(404, response.statusCode());
            var problem = json.readTree(response.body());
            assertEquals("NOT_FOUND", problem.path("errorCode").asText());
            assertEquals("Resource not found", problem.path("detail").asText());
        }
        assertEquals(401, fetch("PUT", "localhost", SHOP + PRODUCT_API + "/" + publicProduct).statusCode());
        assertEquals(401, fetch("HEAD", "localhost", SHOP + "/products").statusCode());
        assertEquals(401, fetch("GET", "localhost", SHOP + "/api/v1/catalog/products").statusCode());
        assertEquals(200, fetch("GET", "127.0.0.1", "/products").statusCode());
    }

    private AccountSession owner(String hostname, String shopPath) {
        var db = application.getBean(JdbcClient.class);
        var roles = application.getBean(RolePermissionRepository.class);
        var tx = new TransactionTemplate(application.getBean(PlatformTransactionManager.class));
        return tx.execute(status -> {
            UUID tenant = UUID.randomUUID();
            UUID account = UUID.randomUUID();
            db.sql("INSERT INTO tenants (id, name, owner_user_id) VALUES (:tenant, 'Fixture', :account)")
                    .param("tenant", tenant).param("account", account).update();
            UUID role = roles.createTenantAdminRole(tenant);
            db.sql("""
                    INSERT INTO users (id, tenant_id, role_id, username, password_hash)
                    VALUES (:account, :tenant, :role, 'owner', 'unused')
                    """).param("account", account).param("tenant", tenant).param("role", role).update();
            db.sql("INSERT INTO tenant_domains (hostname, shop_path, tenant_id) VALUES (:host, :path, :tenant)")
                    .param("host", hostname).param("path", shopPath).param("tenant", tenant).update();
            return new AccountSession(account, tenant, 0, false);
        });
    }

    private HttpResponse<String> fetch(String method, String host, String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://" + host + ":" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody()).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static ClassPathResource migration(String name) {
        return new ClassPathResource("db/" + name + ".sql");
    }

    private static CreateProductRequest product(UUID category, String name, String code) {
        return new CreateProductRequest(name, null, BigDecimal.valueOf(Long.MAX_VALUE), Set.of(category), category,
                code, List.of(), null, null);
    }
}
