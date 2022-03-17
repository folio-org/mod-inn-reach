package org.folio.innreach.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.folio.innreach.domain.entity.LocalAgency;

public interface LocalAgencyRepository extends JpaRepository<LocalAgency, UUID> {
  @Query(name = LocalAgency.FETCH_ONE_BY_CODE_QUERY_NAME)
  Optional<LocalAgency> fetchOneByCode(String code);

  @Query(name = LocalAgency.FIND_LIBRARIES_ASSIGNED_TO_MULTIPLE_AGENCIES_QUERY_NAME)
  List<UUID> findLibraryIdsAssignedToMultipleAgencies(UUID centralServerId);
}
