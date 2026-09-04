package com.healthcare.auth.service;

import com.healthcare.auth.config.AuthProperties;
import com.healthcare.auth.dto.request.RegisterRequest;
import com.healthcare.auth.entity.Role;
import com.healthcare.auth.exception.WeakPasswordException;
import com.healthcare.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationServicePasswordPolicyTest {

    @Test
    void registerPublic_rejectsPasswordViolatingPolicy() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserEventPublisher events = mock(UserEventPublisher.class);
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        AuthProperties props = new AuthProperties();

        when(users.existsByEmail(anyString())).thenReturn(false);

        RegistrationService service = new RegistrationService(
                users, encoder, events, props, passwordPolicy);

        RegisterRequest req = new RegisterRequest(
                "u@example.com",
                "nodigitshere!",   // 12 chars but no digit -> violates policy
                "First",
                "Last",
                Role.PATIENT);

        assertThatThrownBy(() -> service.registerPublic(req, "corr-1"))
                .isInstanceOf(WeakPasswordException.class);

        verify(encoder, never()).encode(any());
        verify(users, never()).save(any());
    }
}
