package org.folio.innreach.domain.service;

import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsListDTO;

import java.util.UUID;

public interface MARCTransformationOptionsSettingsService {
  MARCTransformationOptionsSettingsDTO getMARCTransformOptSet(UUID centralServerId);

  MARCTransformationOptionsSettingsDTO createMARCTransformOptSet(UUID centralServerId, MARCTransformationOptionsSettingsDTO marcTransformOptSetDTO);

  MARCTransformationOptionsSettingsDTO updateMARCTransformOptSet(UUID centralServerId, MARCTransformationOptionsSettingsDTO marcTransformOptSetDTO);

  void deleteMARCTransformOptSet(UUID centralServerId);

  MARCTransformationOptionsSettingsListDTO getAllMARCTransformOptSet(int offset, int limit);
}
