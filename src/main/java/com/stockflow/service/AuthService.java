package com.stockflow.service;

import com.stockflow.domain.dto.auth.*;
import com.stockflow.domain.entity.Company;
import com.stockflow.domain.entity.User;
import com.stockflow.domain.enums.UserRole;
import com.stockflow.exception.BusinessException;
import com.stockflow.exception.DuplicateResourceException;
import com.stockflow.repository.CompanyRepository;
import com.stockflow.repository.UserRepository;
import com.stockflow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        UUID tenantId = UUID.randomUUID();

        Company company = Company.builder()
            .name(request.houseName())
            .tenantId(tenantId)
            .build();
        companyRepository.save(company);

        User user = User.builder()
            .name(request.name())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(UserRole.ADMIN)
            .tenantId(tenantId)
            .build();
        user = userRepository.save(user);

        log.info("New tenant registered: {} / user: {}", tenantId, user.getEmail());
        return buildLoginResponse(user);
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
            .orElseThrow(() -> new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }
        if (!user.isActive()) {
            throw new BusinessException("Account is inactive", HttpStatus.FORBIDDEN);
        }

        LoginResponseDTO response = buildLoginResponse(user);

        user.setRefreshToken(response.refreshToken());
        userRepository.save(user);

        return response;
    }

    @Transactional
    public LoginResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String token = request.refreshToken();
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }

        UUID userId = jwtTokenProvider.getUserId(token);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        if (!token.equals(user.getRefreshToken())) {
            throw new BusinessException("Refresh token mismatch", HttpStatus.UNAUTHORIZED);
        }

        LoginResponseDTO response = buildLoginResponse(user);
        user.setRefreshToken(response.refreshToken());
        userRepository.save(user);

        return response;
    }

    @Transactional
    public void logout(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setRefreshToken(null);
            userRepository.save(user);
        });
    }

    private LoginResponseDTO buildLoginResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name(), user.getTenantId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getTenantId());

        return new LoginResponseDTO(
            accessToken,
            refreshToken,
            jwtTokenProvider.getAccessTokenExpMs(),
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole().name()
        );
    }
}
