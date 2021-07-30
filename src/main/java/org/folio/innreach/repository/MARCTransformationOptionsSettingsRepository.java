package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.MARCTransformationOptionsSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MARCTransformationOptionsSettingsRepository extends JpaRepository<MARCTransformationOptionsSettings, UUID> {
  @Query(name = MARCTransformationOptionsSettings.FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY_NAME)
  Optional<MARCTransformationOptionsSettings> findOneByCentralServerId(UUID id);
}
