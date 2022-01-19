package org.folio.innreach.repository;

import static org.folio.innreach.domain.entity.LocalServerCredentials.FIND_BY_LOCAL_SERVER_KEY_QUERY_NAME;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.entity.LocalServerCredentials;

@Repository
public interface LocalServerCredentialsRepository extends JpaRepository<LocalServerCredentials, UUID> {

  @Query(name = FIND_BY_LOCAL_SERVER_KEY_QUERY_NAME)
  Optional<LocalServerCredentials> findByLocalServerKey(String localServerKey);
}
