package com.dagantara.backend.modules.storesettings.controller;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.modules.storesettings.dto.StoreSettingsRequest;
import com.dagantara.backend.modules.storesettings.dto.StoreSettingsResponse;
import com.dagantara.backend.modules.storesettings.service.StoreSettingsService;
import com.dagantara.backend.modules.users.dto.AccountSession;
import jakarta.servlet.http.Part;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import static com.dagantara.backend.common.api.ApiRoutes.STOREFRONT_SHOP;
import static com.dagantara.backend.common.api.ApiRoutes.STORE_SETTINGS_API;
import static com.dagantara.backend.common.security.AccountSessionFilter.ACCOUNT_ATTRIBUTE;

@RestController
@RequestMapping({STORE_SETTINGS_API, STOREFRONT_SHOP + STORE_SETTINGS_API})
public class StoreSettingsController {
    private final StoreSettingsService settings;

    public StoreSettingsController(StoreSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public ApiResponse<StoreSettingsResponse> read(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
            @PathVariable(required = false) String shopSlug) {
        return ApiResponse.success("Store settings", settings.read(actor, shopSlug));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StoreSettingsResponse> save(@RequestAttribute(ACCOUNT_ATTRIBUTE) AccountSession actor,
            @Valid @RequestPart("settings") StoreSettingsRequest request,
            @PathVariable(required = false) String shopSlug,
            @RequestPart(name = "logo", required = false) List<Part> logos) {
        return ApiResponse.success("Store settings saved", settings.save(actor, shopSlug, request,
                logos == null ? List.of() : logos));
    }
}
