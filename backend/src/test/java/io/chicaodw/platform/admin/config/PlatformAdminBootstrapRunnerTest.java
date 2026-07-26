package io.chicaodw.platform.admin.config;

import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Covers the full behavior table of DT-011A.7 §8, one row per test. */
@ExtendWith(MockitoExtension.class)
class PlatformAdminBootstrapRunnerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private PlatformAdminBootstrapProperties properties;
    private PlatformAdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        properties = new PlatformAdminBootstrapProperties();
        runner = new PlatformAdminBootstrapRunner(properties, userRepository, passwordEncoder);
    }

    @Test
    void neitherVariableSet_isNoOp() {
        properties.setEmail(null);
        properties.setPassword(null);

        runner.run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void onlyEmailSet_failsStartup() {
        properties.setEmail("admin@example.com");
        properties.setPassword(null);

        assertThatThrownBy(() -> runner.run(null)).isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void onlyPasswordSet_failsStartup() {
        properties.setEmail(null);
        properties.setPassword("SomeStrongPassword1");

        assertThatThrownBy(() -> runner.run(null)).isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void emailAlreadySuperAdmin_isNoOpAndNeverTouchesPassword() {
        properties.setEmail("admin@example.com");
        properties.setPassword("SomeStrongPassword1");

        User existing = new User();
        existing.setRole(UserRole.SUPER_ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));

        runner.run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void emailBelongsToOwner_failsStartup_neverPromotesSilently() {
        properties.setEmail("owner@example.com");
        properties.setPassword("SomeStrongPassword1");

        User existing = new User();
        existing.setRole(UserRole.OWNER);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> runner.run(null)).isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void weakPassword_failsStartup() {
        properties.setEmail("admin@example.com");
        properties.setPassword("short");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> runner.run(null)).isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void happyPath_createsActiveSuperAdminWithNoCompany() {
        properties.setEmail("admin@example.com");
        properties.setPassword("SomeStrongPassword1");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        runner.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(saved.getCompanyId()).isNull();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
    }
}
