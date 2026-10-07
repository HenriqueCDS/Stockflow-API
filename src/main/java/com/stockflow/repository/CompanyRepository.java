package com.stockflow.repository;

import com.stockflow.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    Optional<Company> findByInviteCodeAndDeletedAtIsNull(String inviteCode);
}
