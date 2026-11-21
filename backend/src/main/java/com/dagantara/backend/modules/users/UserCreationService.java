package com.dagantara.backend.modules.users;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.common.security.UserIdentity;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCreationService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserCreationService(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreateUserResponse create(Jwt jwt, CreateUserRequest request) {
        UserAccount creator = UserIdentity.userId(jwt)
                .flatMap(users::findCreator)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                        "You do not have permission to create users"));

        if (!users.isAssignableOnCreate(request.roleId(), creator.getTenantId(), creator.getAccountType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE",
                    "The selected role is not valid for this account");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Password must be at most 72 UTF-8 bytes");
        }

        UserAccount user = new UserAccount(creator, request, passwordEncoder.encode(request.password()));
        return CreateUserResponse.from(users.saveAndFlush(user));
    }
}
