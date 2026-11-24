package com.dagantara.backend.modules.storesettings.repository;

import com.dagantara.backend.modules.storesettings.dto.StoreSettingsRequest;
import com.dagantara.backend.modules.storesettings.dto.StoreLogo;
import java.sql.Types;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class StoreSettingsRepository {
    private final JdbcClient jdbc;

    public StoreSettingsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public StoredSettings find(UUID tenantId) {
        return jdbc.sql("""
                SELECT t.name, s.display_name, s.public_whatsapp, s.public_email, s.logo_id, s.logo_content_type
                FROM tenants t LEFT JOIN store_settings s ON s.tenant_id = t.id
                WHERE t.id = :tenantId
                """).param("tenantId", tenantId)
                .query((row, index) -> {
                    UUID logoId = row.getObject("logo_id", UUID.class);
                    return new StoredSettings(row.getString("name"), row.getString("display_name"),
                            row.getString("public_whatsapp"), row.getString("public_email"),
                            logoId == null ? null : new StoreLogo(logoId, row.getString("logo_content_type")));
                })
                .single();
    }

    public void lockTenant(UUID tenantId) {
        // The tenant row also exists before the first settings save.
        jdbc.sql("SELECT id FROM tenants WHERE id = :tenantId FOR NO KEY UPDATE")
                .param("tenantId", tenantId).query(UUID.class).single();
    }

    public Optional<StoreLogo> findActiveLogo(UUID tenantId, UUID logoId) {
        return jdbc.sql("""
                SELECT logo_content_type FROM store_settings WHERE tenant_id = :tenantId AND logo_id = :logoId
                """).param("tenantId", tenantId).param("logoId", logoId)
                .query((row, index) -> new StoreLogo(logoId, row.getString("logo_content_type"))).optional();
    }

    public void save(UUID tenantId, StoreSettingsRequest settings, StoreLogo logo) {
        jdbc.sql("""
                INSERT INTO store_settings (tenant_id, display_name, public_whatsapp, public_email,
                    logo_id, logo_content_type)
                VALUES (:tenantId, :displayName, :publicWhatsapp, :publicEmail, :logoId, :logoContentType)
                ON CONFLICT (tenant_id) DO UPDATE SET display_name = EXCLUDED.display_name,
                    public_whatsapp = EXCLUDED.public_whatsapp, public_email = EXCLUDED.public_email,
                    logo_id = EXCLUDED.logo_id, logo_content_type = EXCLUDED.logo_content_type
                """).param("tenantId", tenantId)
                .param("displayName", settings.displayName(), Types.VARCHAR)
                .param("publicWhatsapp", settings.publicWhatsapp(), Types.VARCHAR)
                .param("publicEmail", settings.publicEmail(), Types.VARCHAR)
                .param("logoId", logo == null ? null : logo.id(), Types.OTHER)
                .param("logoContentType", logo == null ? null : logo.contentType(), Types.VARCHAR).update();
    }

    public record StoredSettings(String tenantName, String displayName, String publicWhatsapp, String publicEmail,
                                 StoreLogo logo) {
    }
}
