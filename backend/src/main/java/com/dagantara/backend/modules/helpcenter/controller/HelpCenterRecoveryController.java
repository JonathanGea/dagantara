package com.dagantara.backend.modules.helpcenter.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import com.dagantara.backend.modules.helpcenter.dto.RecoveryLookupRequest;
import com.dagantara.backend.modules.helpcenter.dto.RecoveryTarget;
import com.dagantara.backend.modules.helpcenter.dto.VerifiedResetRequest;
import com.dagantara.backend.modules.helpcenter.service.TenantAdminRecoveryService;
import com.dagantara.backend.modules.tenants.dto.TenantAccountResponse.PasswordReset;
import com.dagantara.backend.modules.users.dto.AccountSession;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.dagantara.backend.common.security.AccountSessionFilter.ACCOUNT_ATTRIBUTE;

@RestController
@RequestMapping(ApiRoutes.V1 + "/help-center")
public class HelpCenterRecoveryController {
    private final TenantAdminRecoveryService recovery;

    public HelpCenterRecoveryController(TenantAdminRecoveryService recovery) {
        this.recovery = recovery;
    }

    @PostMapping("/tenant-admins/lookup")
    public ResponseEntity<ApiResponse<RecoveryTarget>> lookup(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
            @Valid @RequestBody RecoveryLookupRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Recovery details retrieved", recovery.lookup(actor, request)));
    }

    @PostMapping("/tenants/{tenantId}/admins/{accountId}/reset-password")
    public ResponseEntity<ApiResponse<PasswordReset>> reset(
            @RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
            @PathVariable UUID tenantId,
            @PathVariable UUID accountId,
            @Valid @RequestBody VerifiedResetRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Tenant administrator password reset",
                        recovery.reset(actor, tenantId, accountId, request)));
    }
}
