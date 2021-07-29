package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.AgencyLocationMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgencyLocationMappingRepository extends JpaRepository<AgencyLocationMapping, UUID> {

  @Query(name = AgencyLocationMapping.FETCH_ONE_BY_CS_QUERY_NAME)
  Optional<AgencyLocationMapping> fetchOneByCsId(UUID id);

}
