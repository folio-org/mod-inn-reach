package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OngoingContributionStatusRepository extends JpaRepository<OngoingContributionStatus, UUID> {
}
