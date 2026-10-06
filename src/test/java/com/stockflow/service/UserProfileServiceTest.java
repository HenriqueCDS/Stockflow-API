package com.stockflow.service;

import com.stockflow.domain.dto.user.UserProfileRequestDTO;
import com.stockflow.domain.dto.user.UserProfileResponseDTO;
import com.stockflow.domain.entity.User;
import com.stockflow.exception.ResourceNotFoundException;
import com.stockflow.mapper.UserMapper;
import com.stockflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;

    @InjectMocks UserProfileService userProfileService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void getProfile_shouldThrowWhenUserNotInTenant() {
        when(userRepository.findByIdAndTenantIdAndDeletedAtIsNull(userId, tenantId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getProfile(tenantId, userId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_shouldChangeNameAndReturnProfile() {
        User user = User.builder().name("Antigo").email("a@b.com").passwordHash("x").tenantId(tenantId).build();
        UserProfileResponseDTO response = new UserProfileResponseDTO(userId, "Novo", "a@b.com", "ADMIN");

        when(userRepository.findByIdAndTenantIdAndDeletedAtIsNull(userId, tenantId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toProfileResponse(user)).thenReturn(response);

        UserProfileResponseDTO result = userProfileService.updateProfile(
            tenantId, userId, new UserProfileRequestDTO("Novo"));

        assertThat(user.getName()).isEqualTo("Novo");
        assertThat(result.name()).isEqualTo("Novo");
        verify(userRepository).save(user);
    }
}
