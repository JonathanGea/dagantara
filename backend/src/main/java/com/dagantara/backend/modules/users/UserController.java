package com.dagantara.backend.modules.users;

import com.dagantara.backend.common.api.ApiResponse;
import com.dagantara.backend.common.api.ApiRoutes;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.V1 + "/users")
public class UserController {
    private final UserCreationService userCreationService;

    public UserController(UserCreationService userCreationService) {
        this.userCreationService = userCreationService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateUserResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created", userCreationService.create(jwt, request));
    }
}
