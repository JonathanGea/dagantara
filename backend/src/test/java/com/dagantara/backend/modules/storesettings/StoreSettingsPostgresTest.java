package com.dagantara.backend.modules.storesettings;

import com.dagantara.backend.DagantaraApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "STORE_SETTINGS01_TEST_DB_URL", matches = ".+")
class StoreSettingsPostgresTest {
    private static final String API = "/ss01/api/v1/store-settings";
    private static final String SETTINGS = """
            {"displayName":"Kopi Pagi","publicWhatsapp":"+6281234567890","publicEmail":"kopi@example.test","logoAction":"KEEP"}
            """;
    private static final String BOUNDARY = "ss01-test-boundary";
    private final String schema = "ss01_test_" + UUID.randomUUID().toString().replace("-", "");
    private final String password = UUID.randomUUID().toString();
    private final UUID tenant = UUID.randomUUID();
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private JdbcClient admin;
    private JdbcClient db;
    private ServletWebServerApplicationContext app;
    private String origin;
    private String token;

    @BeforeAll
    void startOnIsolatedSchemaAndRandomPort() throws Exception {
        String url = System.getenv("STORE_SETTINGS01_TEST_DB_URL");
        String user = System.getenv("STORE_SETTINGS01_TEST_DB_USERNAME");
        String dbPassword = System.getenv("STORE_SETTINGS01_TEST_DB_PASSWORD");
        admin = JdbcClient.create(new DriverManagerDataSource(url, user, dbPassword));
        admin.sql("CREATE SCHEMA " + schema).update();
        String scopedUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        var source = new DriverManagerDataSource(scopedUrl, user, dbPassword);
        db = JdbcClient.create(source);
        new ResourceDatabasePopulator(migration("01-platform-accounts"), migration("02-tenant-onboarding"),
                migration("03-platform-operators"), migration("04-tenant-accounts"), migration("05-help-center-recovery"),
                migration("06-product-catalog"), migration("07-product-media"), migration("08-inventory-receipts"),
                migration("09-inventory-corrections")).execute(source);

        var tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        tx.executeWithoutResult(status -> insertExistingTenant());
        new ResourceDatabasePopulator(migration("10-store-settings")).execute(source);

        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        app = (ServletWebServerApplicationContext) new SpringApplicationBuilder(DagantaraApplication.class)
                .run("--spring.datasource.url=" + scopedUrl, "--spring.datasource.username=" + user,
                        "--spring.datasource.password=" + dbPassword, "--server.port=0", "--server.address=127.0.0.1",
                        "--app.tenant.platform-host=localhost", "--app.security.jwt.secret-base64="
                                + Base64.getEncoder().encodeToString(key));
        origin = "http://localhost:" + app.getWebServer().getPort();
        var login = http.send(HttpRequest.newBuilder(URI.create(origin + "/ss01/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(
                        Map.of("username", "owner", "password", password)))).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, login.statusCode());
        token = json.readTree(login.body()).at("/data/accessToken").asText();
        assertFalse(token.isBlank());
    }

    @BeforeEach
    void resetSettings() {
        db.sql("DELETE FROM store_settings WHERE tenant_id = :tenant").param("tenant", tenant).update();
    }

    @AfterAll
    void releaseFixture() {
        if (app != null) {
            app.close();
        }
        if (admin != null) {
            admin.sql("DROP SCHEMA IF EXISTS " + schema + " CASCADE").update();
        }
    }

    @Test
    void ssAt01MigrationGrantsLegacyRoleAccessAndMissingSettingsAreEmpty() throws Exception {
        JsonNode empty = read();
        assertEquals("Kopi Pagi Nusantara", empty.path("tenantName").asText());
        for (String field : List.of("displayName", "publicWhatsapp", "publicEmail", "logoUrl")) {
            assertTrue(empty.path(field).isNull(), field);
        }
        assertEquals(0, settingsRows());
        var storefront = get("/ss01/products", null);
        assertEquals(200, storefront.statusCode());
        assertTrue(storefront.body().contains("\"store\":{\"name\":\"Kopi Pagi Nusantara\""));
        assertTrue(storefront.headers().firstValue("Cache-Control").orElseThrow().contains("no-store"));
        assertEquals(200, get("/ss01/admin/store-settings", null).statusCode());
        assertEquals(404, get("/admin/store-settings", null).statusCode());
    }

    @Test
    void ssAt02SavesOneTenantRecordAndEscapesPublicDisplayName() throws Exception {
        String name = "</script><script>text only</script>";
        String input = SETTINGS.replace("Kopi Pagi", "  " + name + "  ").replace("kopi@example.test", " kopi@example.test ");
        JsonNode saved = data(put(multipart("settings", input), token), 200);
        assertEquals(name, saved.path("displayName").asText());
        assertEquals("kopi@example.test", saved.path("publicEmail").asText());
        assertEquals(saved, read());
        String html = get("/ss01/products", null).body();
        assertFalse(html.contains(name));
        assertTrue(html.contains("\\u003c/script>\\u003cscript>text only\\u003c/script>"));

        JsonNode cleared = data(put(multipart("settings", """
                {"displayName":" ","publicWhatsapp":" +6281234567890 ","publicEmail":"","logoAction":"KEEP"}
                """), token), 200);
        assertTrue(cleared.path("displayName").isNull());
        assertTrue(cleared.path("publicEmail").isNull());
        assertEquals("+6281234567890", cleared.path("publicWhatsapp").asText());
        assertEquals(1, settingsRows());
        assertEquals(cleared, read());
    }

    @Test
    void ssAt04RejectsInvalidContactAndMultipartWithoutChangingSettings() throws Exception {
        String limit = "🥤".repeat(255);
        String valid = SETTINGS.replace("Kopi Pagi", limit);
        JsonNode saved = data(put(multipart("settings", valid), token), 200);
        assertEquals(limit, saved.path("displayName").asText());
        for (String input : List.of(
                valid.replace(limit, limit + "🥤"),
                SETTINGS.replace("+6281234567890", "+628 1234567890"),
                SETTINGS.replace("kopi@example.test", "a@example.test,b@example.test"),
                SETTINGS.replace("\"KEEP\"", "\"REMOVE\""),
                SETTINGS.replace("\"KEEP\"", "0"))) {
            data(put(multipart("settings", input), token), 400);
            assertEquals(saved, read());
        }
        data(put(multipart("unexpected", SETTINGS), token), 400);
        String withLogo = multipart("settings", SETTINGS).replace("--" + BOUNDARY + "--\r\n",
                "--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"logo\"; filename=\"fake.txt\""
                        + "\r\nContent-Type: text/plain\r\n\r\nfake\r\n--" + BOUNDARY + "--\r\n");
        data(put(withLogo, token), 400);
        assertEquals(saved, read());
    }

    @Test
    void ssAt05RejectsMissingSessionAndTenantMismatchBeforeChangingData() throws Exception {
        assertEquals(401, get(API, null).statusCode());
        assertEquals(401, put(multipart("settings", SETTINGS), null).statusCode());
        assertEquals(403, get("/another/api/v1/store-settings", token).statusCode());
        assertEquals(0, settingsRows());
    }

    private void insertExistingTenant() {
        UUID account = UUID.randomUUID();
        UUID role = UUID.randomUUID();
        db.sql("INSERT INTO tenants (id, name, owner_user_id) VALUES (:tenant, 'Kopi Pagi Nusantara', :account)")
                .param("tenant", tenant).param("account", account).update();
        db.sql("INSERT INTO roles (id, tenant_id, code, name) VALUES (:role, :tenant, 'TENANT_ADMIN', 'Admin Tenant')")
                .param("role", role).param("tenant", tenant).update();
        db.sql("""
                INSERT INTO users (id, tenant_id, role_id, username, password_hash, phone_number)
                VALUES (:id, :tenant, :role, 'owner', :hash, '+6281100000000')
                """).param("id", account).param("tenant", tenant).param("role", role)
                .param("hash", new BCryptPasswordEncoder(4).encode(password)).update();
        db.sql("INSERT INTO tenant_domains (hostname, shop_path, tenant_id) VALUES ('localhost', '/ss01', :tenant)")
                .param("tenant", tenant).update();
    }

    private int settingsRows() {
        return db.sql("SELECT count(*) FROM store_settings WHERE tenant_id = :tenant")
                .param("tenant", tenant).query(Integer.class).single();
    }

    private JsonNode read() throws Exception {
        return data(get(API, token), 200);
    }

    private JsonNode data(HttpResponse<String> response, int expectedStatus) throws Exception {
        assertEquals(expectedStatus, response.statusCode());
        JsonNode body = json.readTree(response.body());
        if (expectedStatus >= 400) {
            assertEquals(expectedStatus, body.path("status").asInt());
        }
        return body.path("data");
    }

    private HttpResponse<String> get(String path, String bearer) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(origin + path)).GET(), bearer);
    }

    private HttpResponse<String> put(String payload, String bearer) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(origin + API))
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .PUT(HttpRequest.BodyPublishers.ofString(payload)), bearer);
    }

    private HttpResponse<String> send(HttpRequest.Builder builder, String bearer) throws Exception {
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String multipart(String part, String input) {
        return "--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"" + part + "\""
                + "\r\nContent-Type: application/json\r\n\r\n" + input + "\r\n--" + BOUNDARY + "--\r\n";
    }

    private static ClassPathResource migration(String name) {
        return new ClassPathResource("db/" + name + ".sql");
    }
}
