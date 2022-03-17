package org.folio.innreach.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration;

public interface VisiblePatronFieldConfigurationRepository extends JpaRepository<VisiblePatronFieldConfiguration, UUID> {
  @Query(name = VisiblePatronFieldConfiguration.FETCH_ONE_BY_CENTRAL_CODE_QUERY_NAME)
  Optional<VisiblePatronFieldConfiguration> findByCentralServerCode(String centralServerCode);
}
