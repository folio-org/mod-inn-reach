package org.folio.innreach.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.entity.CentralPatronTypeMapping;
import org.folio.innreach.domain.entity.PatronTypeMapping;

@Repository
public interface PatronTypeMappingRepository extends JpaRepository<PatronTypeMapping, UUID> {

  Optional<CentralPatronTypeMapping> findOneByCentralServerIdAndPatronGroupId(UUID centralServerId, UUID patronGroupId);

}
