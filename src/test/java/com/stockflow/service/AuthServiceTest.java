package com.stockflow.service;

import com.stockflow.domain.dto.auth.JoinRequestDTO;
import com.stockflow.domain.dto.auth.LoginRequestDTO;
import com.stockflow.domain.dto.auth.LoginResponseDTO;
import com.stockflow.domain.dto.auth.RegisterRequestDTO;
import com.stockflow.domain.entity.Company;
import com.stockflow.domain.entity.User;
import com.stockflow.domain.enums.UserRole;
import com.stockflow.exception.BusinessException;
import com.stockflow.exception.DuplicateResourceException;
import com.stockflow.repository.CompanyRepository;
import com.stockflow.repository.UserRepository;
import com.stockflow.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock CompanyRepository companyRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;

    @InjectMocks AuthService authService;

    private User testUser;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        testUser = User.builder()
            .name("Test User")
            .email("test@example.com")
            .passwordHash("$hashed$")
            .role(UserRole.OWNER)
            .tenantId(tenantId)
            .active(true)
            .build();
        // Set id via reflection since it's in BaseEntity
        try {
            var idField = testUser.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, UUID.randomUUID());
        } catch (Exception ignored) {}
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {
        when(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).thenReturn(true);

        RegisterRequestDTO request = new RegisterRequestDTO(
            "Name", "test@example.com", "Pass@1234", "House");

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void join_shouldThrowWhenEmailAlreadyExists() {
        when(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).thenReturn(true);

        JoinRequestDTO request = new JoinRequestDTO("Name", "test@example.com", "Pass@1234", "ABCD1234");

        assertThatThrownBy(() -> authService.join(request))
            .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void join_shouldThrowWhenInviteCodeInvalid() {
        when(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(companyRepository.findByInviteCodeAndDeletedAtIsNull("BADCODE")).thenReturn(Optional.empty());

        JoinRequestDTO request = new JoinRequestDTO("Name", "new@example.com", "Pass@1234", "BADCODE");

        assertThatThrownBy(() -> authService.join(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Invalid invite code");
    }

    @Test
    void join_shouldCreateMemberWhenInviteCodeValid() {
        Company company = Company.builder()
            .name("Test House")
            .tenantId(tenantId)
            .inviteCode("ABCD1234")
            .build();

        when(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(companyRepository.findByInviteCodeAndDeletedAtIsNull("ABCD1234")).thenReturn(Optional.of(company));
        when(passwordEncoder.encode(anyString())).thenReturn("$hashed$");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), eq("MEMBER"), any())).thenReturn("access.token.here");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh.token.here");
        when(jwtTokenProvider.getAccessTokenExpMs()).thenReturn(900000L);

        JoinRequestDTO request = new JoinRequestDTO("New Member", "new@example.com", "Pass@1234", "ABCD1234");
        LoginResponseDTO response = authService.join(request);

        assertThat(response.role()).isEqualTo("MEMBER");
        assertThat(response.email()).isEqualTo("new@example.com");
    }

    @Test
    void login_shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmailAndDeletedAtIsNull(anyString())).thenReturn(Optional.empty());

        LoginRequestDTO request = new LoginRequestDTO("notfound@example.com", "password");

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_shouldThrowWhenPasswordWrong() {
        when(userRepository.findByEmailAndDeletedAtIsNull(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        LoginRequestDTO request = new LoginRequestDTO("test@example.com", "wrongpassword");

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_shouldReturnTokensWhenCredentialsCorrect() {
        when(userRepository.findByEmailAndDeletedAtIsNull(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString(), any())).thenReturn("access.token.here");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh.token.here");
        when(jwtTokenProvider.getAccessTokenExpMs()).thenReturn(900000L);
        when(userRepository.save(any())).thenReturn(testUser);

        LoginRequestDTO request = new LoginRequestDTO("test@example.com", "correctpassword");
        LoginResponseDTO response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access.token.here");
        assertThat(response.refreshToken()).isEqualTo("refresh.token.here");
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    void login_shouldThrowWhenUserInactive() {
        testUser.setActive(false);
        when(userRepository.findByEmailAndDeletedAtIsNull(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("test@example.com", "pass")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("inactive");
    }
}
