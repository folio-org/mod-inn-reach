package org.folio.innreach.domain.service;

import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;

import java.util.UUID;

public interface MARCTransformationOptionsSettingsService {
  MARCTransformationOptionsSettingsDTO getMARCTransformOptSet(UUID centralServerId);

  MARCTransformationOptionsSettingsDTO createMARCTransformOptSet(UUID centralServerId, MARCTransformationOptionsSettingsDTO marcTransformOptSetDTO);

  MARCTransformationOptionsSettingsDTO updateMARCTransformOptSet(UUID centralServerId, MARCTransformationOptionsSettingsDTO marcTransformOptSetDTO);
}
