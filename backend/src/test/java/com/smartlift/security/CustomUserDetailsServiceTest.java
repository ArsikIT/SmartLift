package com.smartlift.security;

import com.smartlift.support.TestPasswords;
import com.smartlift.model.Role;
import com.smartlift.model.User;
import com.smartlift.model.enums.RoleName;
import com.smartlift.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_returnsUserDetailsWithCorrectAuthorities() {
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        Role serviceRole = new Role();
        serviceRole.setName(RoleName.SERVICE);

        User user = new User();
        user.setUsername("admin1");
        user.setPassword(TestPasswords.ENCODED);
        user.setEnabled(true);
        user.setRoles(Set.of(adminRole, serviceRole));

        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("admin1");

        assertThat(userDetails.getUsername()).isEqualTo("admin1");
        assertThat(userDetails.getPassword()).isEqualTo(TestPasswords.ENCODED);
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_SERVICE");
    }

    @Test
    void loadUserByUsername_disabledUser() {
        Role role = new Role();
        role.setName(RoleName.MANUFACTURER);

        User user = new User();
        user.setUsername("disabled");
        user.setPassword(TestPasswords.BASIC);
        user.setEnabled(false);
        user.setRoles(Set.of(role));

        when(userRepository.findByUsername("disabled")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("disabled");

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_throwsWhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: unknown");
    }
}
