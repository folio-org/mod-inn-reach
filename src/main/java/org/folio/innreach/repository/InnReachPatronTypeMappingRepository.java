package org.folio.innreach.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import org.folio.innreach.domain.entity.InnReachPatronTypeMapping;

public interface InnReachPatronTypeMappingRepository extends JpaRepository<InnReachPatronTypeMapping, UUID> {
}
