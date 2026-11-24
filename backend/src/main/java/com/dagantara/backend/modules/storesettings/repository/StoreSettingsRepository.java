package com.dagantara.backend.modules.storesettings.repository;

import com.dagantara.backend.modules.storesettings.dto.StoreSettingsRequest;
import com.dagantara.backend.modules.storesettings.dto.StoreSettingsResponse;
import java.sql.Types;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class StoreSettingsRepository {
    private final JdbcClient jdbc;

    public StoreSettingsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public StoreSettingsResponse find(UUID tenantId) {
        return jdbc.sql("""
                SELECT t.name, s.display_name, s.public_whatsapp, s.public_email
                FROM tenants t LEFT JOIN store_settings s ON s.tenant_id = t.id
                WHERE t.id = :tenantId
                """).param("tenantId", tenantId)
                .query((row, index) -> new StoreSettingsResponse(row.getString("name"), row.getString("display_name"),
                        row.getString("public_whatsapp"), row.getString("public_email"), null))
                .single();
    }

    public void save(UUID tenantId, StoreSettingsRequest settings) {
        jdbc.sql("""
                INSERT INTO store_settings (tenant_id, display_name, public_whatsapp, public_email)
                VALUES (:tenantId, :displayName, :publicWhatsapp, :publicEmail)
                ON CONFLICT (tenant_id) DO UPDATE SET display_name = EXCLUDED.display_name,
                    public_whatsapp = EXCLUDED.public_whatsapp, public_email = EXCLUDED.public_email
                """).param("tenantId", tenantId)
                .param("displayName", settings.displayName(), Types.VARCHAR)
                .param("publicWhatsapp", settings.publicWhatsapp(), Types.VARCHAR)
                .param("publicEmail", settings.publicEmail(), Types.VARCHAR).update();
    }
}
