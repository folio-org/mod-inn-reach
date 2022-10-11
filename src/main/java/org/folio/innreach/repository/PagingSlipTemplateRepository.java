package org.folio.innreach.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.folio.innreach.domain.entity.PagingSlipTemplate;

public interface PagingSlipTemplateRepository extends JpaRepository<PagingSlipTemplate, UUID> {
  @Query(name = PagingSlipTemplate.FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY_NAME)
  Optional<PagingSlipTemplate> fetchOneByCentralServerId(UUID id);
}
