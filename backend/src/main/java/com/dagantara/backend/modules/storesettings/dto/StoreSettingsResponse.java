package com.dagantara.backend.modules.storesettings.dto;

public record StoreSettingsResponse(String tenantName, String displayName, String publicWhatsapp, String publicEmail,
                                    String logoUrl) {
}
