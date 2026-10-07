package com.stockflow.service;

import com.stockflow.domain.dto.membership.InviteCodeResponseDTO;
import com.stockflow.domain.dto.membership.MemberResponseDTO;
import com.stockflow.domain.entity.Company;
import com.stockflow.domain.entity.User;
import com.stockflow.domain.enums.UserRole;
import com.stockflow.exception.BusinessException;
import com.stockflow.exception.ResourceNotFoundException;
import com.stockflow.mapper.UserMapper;
import com.stockflow.repository.CompanyRepository;
import com.stockflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock UserRepository userRepository;
    @Mock CompanyRepository companyRepository;
    @Mock UserMapper userMapper;

    @InjectMocks MembershipService membershipService;

    private UUID tenantId;
    private UUID ownerId;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        memberId = UUID.randomUUID();
    }

    private User buildUser(UUID id, UserRole role) {
        User user = User.builder()
            .name("Test")
            .email("test@example.com")
            .passwordHash("$hashed$")
            .role(role)
            .tenantId(tenantId)
            .active(true)
            .build();
        try {
            var idField = user.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception ignored) {}
        return user;
    }

    @Test
    void listMembers_shouldMapAllUsersOfTenant() {
        User user = buildUser(memberId, UserRole.MEMBER);
        when(userRepository.findAllByTenantId(tenantId)).thenReturn(List.of(user));
        when(userMapper.toMemberResponse(user)).thenReturn(
            new MemberResponseDTO(memberId, "Test", "test@example.com", UserRole.MEMBER, null));

        List<MemberResponseDTO> result = membershipService.listMembers(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(memberId);
    }

    @Test
    void removeMember_shouldThrowWhenRequesterIsTarget() {
        assertThatThrownBy(() -> membershipService.removeMember(tenantId, ownerId, ownerId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("cannot remove themselves");
    }

    @Test
    void removeMember_shouldThrowWhenMemberNotFound() {
        when(userRepository.findByIdAndTenantIdAndDeletedAtIsNull(memberId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> membershipService.removeMember(tenantId, ownerId, memberId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeMember_shouldThrowWhenTargetIsOwner() {
        User owner = buildUser(memberId, UserRole.OWNER);
        when(userRepository.findByIdAndTenantIdAndDeletedAtIsNull(memberId, tenantId)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> membershipService.removeMember(tenantId, ownerId, memberId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("owner cannot be removed");
    }

    @Test
    void removeMember_shouldSoftDeleteRegularMember() {
        User member = buildUser(memberId, UserRole.MEMBER);
        when(userRepository.findByIdAndTenantIdAndDeletedAtIsNull(memberId, tenantId)).thenReturn(Optional.of(member));
        when(userRepository.save(any())).thenReturn(member);

        membershipService.removeMember(tenantId, ownerId, memberId);

        assertThat(member.isDeleted()).isTrue();
        assertThat(member.isActive()).isFalse();
        verify(userRepository).save(member);
    }

    @Test
    void rotateInviteCode_shouldGenerateAndPersistNewCode() {
        Company company = Company.builder().name("House").tenantId(tenantId).inviteCode("OLDCODE1").build();
        when(companyRepository.findByTenantIdAndDeletedAtIsNull(tenantId)).thenReturn(Optional.of(company));
        when(companyRepository.save(any())).thenReturn(company);

        InviteCodeResponseDTO response = membershipService.rotateInviteCode(tenantId);

        assertThat(response.inviteCode()).isNotEqualTo("OLDCODE1");
        assertThat(response.inviteCode()).hasSize(8);
    }
}
