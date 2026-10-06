package com.stockflow.service;

import com.stockflow.domain.dto.user.UserProfileRequestDTO;
import com.stockflow.domain.dto.user.UserProfileResponseDTO;
import com.stockflow.domain.entity.User;
import com.stockflow.exception.ResourceNotFoundException;
import com.stockflow.mapper.UserMapper;
import com.stockflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserProfileResponseDTO getProfile(UUID tenantId, UUID userId) {
        return userMapper.toProfileResponse(findActiveUser(tenantId, userId));
    }

    @Transactional
    public UserProfileResponseDTO updateProfile(UUID tenantId, UUID userId, UserProfileRequestDTO request) {
        User user = findActiveUser(tenantId, userId);
        user.setName(request.name());
        return userMapper.toProfileResponse(userRepository.save(user));
    }

    private User findActiveUser(UUID tenantId, UUID userId) {
        return userRepository.findByIdAndTenantIdAndDeletedAtIsNull(userId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
