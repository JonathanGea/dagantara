package com.dagantara.backend.modules.users.dto;

import java.util.UUID;

public record AccountSession(UUID id, UUID tenantId, int sessionVersion,
                             boolean passwordChangeRequired) {
}
