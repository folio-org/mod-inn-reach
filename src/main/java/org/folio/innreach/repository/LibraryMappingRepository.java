package org.folio.innreach.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.entity.LibraryMapping;

@Repository
public interface LibraryMappingRepository extends JpaRepository<LibraryMapping, UUID> {
}
