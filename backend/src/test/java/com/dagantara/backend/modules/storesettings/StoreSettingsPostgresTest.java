package com.dagantara.backend.modules.storesettings;

import com.dagantara.backend.DagantaraApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    @TempDir
    static Path fixtureDirectory;
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
        new ResourceDatabasePopulator(migration("10-store-settings"), migration("11-store-settings-logo"))
                .execute(source);

        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        app = (ServletWebServerApplicationContext) new SpringApplicationBuilder(DagantaraApplication.class)
                .run("--spring.datasource.url=" + scopedUrl, "--spring.datasource.username=" + user,
                        "--spring.datasource.password=" + dbPassword, "--server.port=0", "--server.address=127.0.0.1",
                        "--app.store-settings.logo-directory=" + logoDirectory(),
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
    void resetSettings() throws Exception {
        db.sql("DELETE FROM store_settings WHERE tenant_id = :tenant").param("tenant", tenant).update();
        if (Files.isDirectory(logoDirectory())) {
            try (var files = Files.list(logoDirectory())) {
                for (Path file : files.toList()) {
                    Files.delete(file);
                }
            }
        }
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
                SETTINGS.replace("\"KEEP\"", "\"UNKNOWN\""),
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

    @Test
    void ssAt03ServesPngJpegAndWebpByContentAndRetiresPreviousIds() throws Exception {
        String previousUrl = null;
        for (var image : Map.of("png", image("png", 4), "jpeg", image("jpeg", 4), "webp",
                Base64.getDecoder().decode("UklGRhwAAABXRUJQVlA4TA8AAAAvA8AAAAcQUcD+ByKi/wEA")).entrySet()) {
            JsonNode saved = data(upload("REPLACE", SETTINGS, image.getValue()), 200);
            String url = saved.path("logoUrl").asText();
            assertTrue(url.matches("/ss01/api/v1/storefront/logo/[0-9a-f-]{36}"));
            assertEquals(saved, read());
            assertLogo(url, "image/" + image.getKey(), image.getValue());
            assertTrue(get("/ss01/products", null).body().contains(url));
            if (previousUrl != null) {
                assertEquals(404, get(previousUrl, null).statusCode());
            }
            assertEquals(List.of(logoFile(url)), storedFiles());
            previousUrl = url;
        }
        JsonNode kept = data(upload("KEEP", SETTINGS.replace("Kopi Pagi", "Kopi Baru")), 200);
        assertEquals(previousUrl, kept.path("logoUrl").asText());
        assertEquals(200, get(previousUrl, null).statusCode());
        Files.delete(logoFile(previousUrl));
        assertEquals(404, get(previousUrl, null).statusCode());
        assertTrue(get("/ss01/products", null).body().contains("Kopi Baru"));

        JsonNode removed = data(upload("REMOVE", SETTINGS), 200);
        assertTrue(removed.path("logoUrl").isNull());
        assertEquals(removed, read());
        assertEquals(404, get(previousUrl, null).statusCode());
        assertFalse(get("/ss01/products", null).body().contains(previousUrl));
    }

    @Test
    void ssAt04RejectsUnreadableAndOversizeImagesAndInvalidContact() throws Exception {
        byte[] png = image("png", 4);
        JsonNode saved = data(upload("REPLACE", SETTINGS, png), 200);
        String url = saved.path("logoUrl").asText();
        String edited = SETTINGS.replace("Kopi Pagi", "Do not persist");
        data(upload("REPLACE", edited, new byte[]{1, 2, 3}), 400);
        data(upload("REPLACE", edited, java.util.Arrays.copyOf(png, 33)), 400);
        byte[] webpWithoutFrames = HexFormat.of().parseHex("524946461600000057454250565038580a00000002000000000000000000");
        data(upload("REPLACE", edited, webpWithoutFrames), 400);
        byte[] oversized = image("png", 900);
        assertTrue(oversized.length > 2 * 1024 * 1024);
        data(upload("REPLACE", edited, oversized), 413);
        data(upload("REPLACE", edited.replace("kopi@example.test", "invalid-email"), png), 400);
        assertEquals(saved, read());
        assertLogo(url, "image/png", png);
        assertEquals(List.of(logoFile(url)), storedFiles());
    }

    @Test
    void ssAt04FileAndDatabaseFailurePreserveSettingsAndCleanUpTheCandidate() throws Exception {
        byte[] png = image("png", 4);
        JsonNode saved = data(upload("REPLACE", SETTINGS, png), 200);
        String url = saved.path("logoUrl").asText();
        Path parked = fixtureDirectory.resolve("parked");
        Files.move(logoDirectory(), parked);
        Files.createFile(logoDirectory());
        try {
            var failed = upload("REPLACE", SETTINGS.replace("Kopi Pagi", "Write failure"), png);
            data(failed, 500);
            assertEquals("LOGO_STORAGE_ERROR", json.readTree(failed.body()).path("errorCode").asText());
            assertFalse(failed.body().contains(logoDirectory().getFileName().toString()));
            assertEquals(saved, read());
        } finally {
            Files.delete(logoDirectory());
            Files.move(parked, logoDirectory());
        }
        assertLogo(url, "image/png", png);

        db.sql("ALTER TABLE store_settings ADD CHECK (display_name <> 'Database failure')").update();
        try {
            data(upload("REPLACE", SETTINGS.replace("Kopi Pagi", "Database failure"), png), 409);
            assertEquals(saved, read());
            assertLogo(url, "image/png", png);
            assertTrue(get("/ss01/products", null).body().contains(url));
            assertEquals(List.of(logoFile(url)), storedFiles());
        } finally {
            db.sql("ALTER TABLE store_settings DROP CONSTRAINT store_settings_display_name_check").update();
        }
    }

    @Test
    void logoActionRequiresExactlyOneFileForReplaceOnly() throws Exception {
        byte[] png = image("png", 4);
        JsonNode before = read();
        for (String action : List.of("KEEP", "REMOVE")) {
            data(upload(action, SETTINGS, png), 400);
        }
        data(upload("REPLACE", SETTINGS), 400);
        data(upload("REPLACE", SETTINGS, png, png), 400);
        assertEquals(before, read());
        assertEquals(0, settingsRows());
    }

    @Test
    void concurrentFirstReplacementsAndKeepLeaveOnlyTheActiveFile() throws Exception {
        byte[] png = image("png", 4);
        byte[] jpeg = image("jpeg", 4);
        var first = http.sendAsync(uploadRequest("REPLACE", SETTINGS, png), HttpResponse.BodyHandlers.ofString());
        var second = http.sendAsync(uploadRequest("REPLACE", SETTINGS, jpeg), HttpResponse.BodyHandlers.ofString());
        var keep = http.sendAsync(uploadRequest("KEEP", SETTINGS), HttpResponse.BodyHandlers.ofString());
        var firstUrl = data(first.get(), 200).path("logoUrl").asText();
        var secondUrl = data(second.get(), 200).path("logoUrl").asText();
        data(keep.get(), 200);
        String active = read().path("logoUrl").asText();
        assertFalse(firstUrl.equals(secondUrl));
        assertTrue(List.of(firstUrl, secondUrl).contains(active));
        assertEquals(1, settingsRows());
        assertEquals(List.of(logoFile(active)), storedFiles());
        for (String url : List.of(firstUrl, secondUrl)) {
            assertEquals(url.equals(active) ? 200 : 404, get(url, null).statusCode());
        }
        data(upload("REMOVE", SETTINGS), 200);
        assertEquals(404, get(active, null).statusCode());
        assertTrue(storedFiles().isEmpty());
    }

    private void assertLogo(String url, String type, byte[] bytes) throws Exception {
        var result = http.send(HttpRequest.newBuilder(URI.create(origin + url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, result.statusCode());
        assertEquals(type, result.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(result.headers().firstValue("Cache-Control").orElseThrow().contains("no-store"));
        assertArrayEquals(bytes, result.body());
    }

    private Path logoDirectory() {
        return fixtureDirectory.resolve("logos");
    }

    private Path logoFile(String url) {
        return logoDirectory().resolve(url.substring(url.lastIndexOf('/') + 1));
    }

    private List<Path> storedFiles() throws Exception {
        try (var files = Files.list(logoDirectory())) {
            return files.toList();
        }
    }

    private HttpResponse<String> upload(String action, String input, byte[]... files) throws Exception {
        return http.send(uploadRequest(action, input, files), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest uploadRequest(String action, String input, byte[]... files) {
        var body = new ByteArrayOutputStream();
        String jsonPart = multipart("settings", input.replace("\"KEEP\"", "\"" + action + "\""));
        body.writeBytes(jsonPart.substring(0, jsonPart.lastIndexOf("--" + BOUNDARY)).getBytes(StandardCharsets.UTF_8));
        for (byte[] file : files) {
            body.writeBytes(("--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"logo\"; filename=\"fake.txt\""
                    + "\r\nContent-Type: text/plain\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.writeBytes(file);
            body.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.writeBytes(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.newBuilder(URI.create(origin + API))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
    }

    private static byte[] image(String type, int size) throws Exception {
        var pixels = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        var random = new Random(42);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                pixels.setRGB(x, y, random.nextInt(1 << 24));
            }
        }
        var bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(pixels, type, bytes));
        return bytes.toByteArray();
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
