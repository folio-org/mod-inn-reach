package org.folio.innreach.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.entity.CentralServerSettings;

import static org.folio.innreach.domain.entity.CentralServerSettings.FIND_BY_CENTRAL_SERVER_ID_QUERY_NAME;

@Repository
public interface CentralServerSettingsRepository extends JpaRepository<CentralServerSettings, UUID> {

  @Query(name = FIND_BY_CENTRAL_SERVER_ID_QUERY_NAME)
  Optional<CentralServerSettings> findByCentralServerId(UUID centralServerId);
}
