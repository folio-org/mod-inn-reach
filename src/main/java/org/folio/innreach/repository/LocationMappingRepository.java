package org.folio.innreach.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.entity.LocationMapping;

@Repository
public interface LocationMappingRepository extends JpaRepository<LocationMapping, UUID> {

  @Query(name = LocationMapping.FETCH_ALL_BY_CENTRAL_SERVER_QUERY_NAME)
  List<LocationMapping> fetchAllByCentralServer(UUID csId);
}
