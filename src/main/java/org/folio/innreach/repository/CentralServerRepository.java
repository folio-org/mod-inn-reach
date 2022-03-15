package org.folio.innreach.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.domain.entity.CentralServer;

@Repository
public interface CentralServerRepository extends JpaRepository<CentralServer, UUID> {

  @Query(name = CentralServer.FETCH_ALL_BY_ID_QUERY_NAME)
  List<CentralServer> fetchAllById(List<UUID> id);

  @Query(name = CentralServer.FETCH_ONE_BY_ID_QUERY_NAME)
  Optional<CentralServer> fetchOne(UUID id);

  @Query(name = CentralServer.FETCH_ONE_BY_CENTRAL_CODE_QUERY_NAME)
  Optional<CentralServer> fetchOneByCentralCode(String code);

  @Query(name = CentralServer.FETCH_CONNECTION_DETAILS_BY_ID_QUERY_NAME)
  Optional<CentralServerConnectionDetailsDTO> fetchConnectionDetails(UUID id);

  @Query(name = CentralServer.FETCH_CONNECTION_DETAILS_BY_CENTRAL_CODE_QUERY_NAME)
  Optional<CentralServerConnectionDetailsDTO> fetchConnectionDetailsByCentralCode(String code);

  @Query(name = CentralServer.GET_IDS_QUERY_NAME)
  Page<UUID> getIds(Pageable pageable);

  @Query(name = CentralServer.GET_ID_BY_CENTRAL_CODE_QUERY_NAME)
  Optional<UUID> getIdByCentralCode(String centralCode);

  @Query(name = CentralServer.FETCH_RECALL_USER_BY_ID_QUERY_NAME)
  Optional<CentralServer> fetchOneWithRecallUser(UUID id);

  @Query(name = CentralServer.VALIDATE_AGENCY_LIBRARIES_QUERY_NAME)
  List<UUID> validateAgencyLibraries(UUID id);
}
