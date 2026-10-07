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
import com.stockflow.utils.InviteCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<MemberResponseDTO> listMembers(UUID tenantId) {
        return userRepository.findAllByTenantId(tenantId).stream()
            .map(userMapper::toMemberResponse)
            .toList();
    }

    @Transactional
    public void removeMember(UUID tenantId, UUID requesterId, UUID memberId) {
        if (requesterId.equals(memberId)) {
            throw new BusinessException("The owner cannot remove themselves", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        User member = userRepository.findByIdAndTenantIdAndDeletedAtIsNull(memberId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("User", memberId));

        if (member.getRole() == UserRole.OWNER) {
            throw new BusinessException("The house owner cannot be removed", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        member.softDelete();
        member.setActive(false);
        userRepository.save(member);
    }

    @Transactional
    public InviteCodeResponseDTO rotateInviteCode(UUID tenantId) {
        Company company = companyRepository.findByTenantIdAndDeletedAtIsNull(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found for tenant"));

        company.setInviteCode(InviteCodeGenerator.generate());
        companyRepository.save(company);

        return new InviteCodeResponseDTO(company.getInviteCode());
    }
}
