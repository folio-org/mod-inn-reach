package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.ItemTypeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ItemTypeMappingRepository extends JpaRepository<ItemTypeMapping, UUID> {
}
