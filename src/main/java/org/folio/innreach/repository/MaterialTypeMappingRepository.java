package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.MaterialTypeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialTypeMappingRepository extends JpaRepository<MaterialTypeMapping, UUID> {

  long countByCentralServerIdAndMaterialTypeIdIn(UUID centralServerId, List<UUID> ids);

  Optional<MaterialTypeMapping> findOneByCentralServerIdAndMaterialTypeId(UUID centralServerId, UUID materialTypeId);

}
