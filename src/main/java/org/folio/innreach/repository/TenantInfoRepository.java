package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.TenantInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
@Repository
public interface TenantInfoRepository extends JpaRepository<TenantInfo, UUID> {

  TenantInfo findByTenantId(String id);

  @Transactional
  void deleteByTenantId(String id);

}
