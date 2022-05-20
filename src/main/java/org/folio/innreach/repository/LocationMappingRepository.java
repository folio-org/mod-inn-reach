package org.folio.innreach.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.entity.LocationMapping;

@Repository
public interface LocationMappingRepository extends JpaRepository<LocationMapping, UUID> {
  List<LocationMapping> findByCentralServerIdAndLibraryId(UUID centralServerId, UUID libraryId);
  List<LocationMapping> findByCentralServerId(UUID centralServerId);
}
