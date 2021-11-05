package org.folio.innreach.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.entity.ItemTypeMapping;

@Repository
public interface ItemTypeMappingRepository extends JpaRepository<ItemTypeMapping, UUID> {
  Optional<ItemTypeMapping> findByCentralServerIdAndCentralItemType(UUID centralServerId, Integer centralItemType);
}
