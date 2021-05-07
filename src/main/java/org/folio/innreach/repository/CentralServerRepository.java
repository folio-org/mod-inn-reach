package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.CentralServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CentralServerRepository extends JpaRepository<CentralServer, UUID> {

  @Query(name = CentralServer.FETCH_ALL_QUERY_NAME)
  List<CentralServer> fetchAll();

  @Query(name = CentralServer.FETCH_ONE_BY_ID_QUERY_NAME)
  Optional<CentralServer> fetchOne(UUID id);
}
