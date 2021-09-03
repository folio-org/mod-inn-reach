package org.folio.innreach.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.entity.ContributionError;

@Repository
public interface ContributionErrorRepository extends JpaRepository<ContributionError, UUID> {
}
