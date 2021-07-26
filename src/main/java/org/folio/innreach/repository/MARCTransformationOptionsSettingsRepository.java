package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.MARCTransformationOptionsSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MARCTransformationOptionsSettingsRepository extends JpaRepository<MARCTransformationOptionsSettings, UUID> {
}
