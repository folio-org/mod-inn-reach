package org.folio.innreach.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.entity.CentralServer;

@Repository
public interface CentralServerRepository extends JpaRepository<CentralServer, UUID> {

  @Query(name = CentralServer.FETCH_ALL_QUERY_NAME)
  Page<CentralServer> fetchAll(Pageable pageable);

  @Query(name = CentralServer.FETCH_ONE_BY_ID_QUERY_NAME)
  Optional<CentralServer> fetchOne(UUID id);
}
