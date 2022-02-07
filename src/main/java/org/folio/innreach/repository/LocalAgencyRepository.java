package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.LocalAgency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

import static org.folio.innreach.domain.entity.LocalAgency.FETCH_ONE_BY_LIBRARY_ID_NAME;

public interface LocalAgencyRepository extends JpaRepository<LocalAgency, UUID> {
    @Query(name = FETCH_ONE_BY_LIBRARY_ID_NAME)
    Optional<LocalAgency> fetchOneByLibraryId(UUID libraryId);
}
