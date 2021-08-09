package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.PatronTypeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PatronTypeMappingRepository extends JpaRepository<PatronTypeMapping, UUID> {
}
