package com.healthcare.auth.mapper;

import com.healthcare.auth.dto.response.UserResponse;
import com.healthcare.auth.entity.User;
import org.springframework.stereotype.Component;

/**
 * Maps {@link User} to the safe {@link UserResponse} projection.
 * Never exposes {@code passwordHash}, {@code failedLoginCount},
 * or {@code lockedUntil}.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
