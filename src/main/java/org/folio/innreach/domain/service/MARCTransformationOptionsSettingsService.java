package org.folio.innreach.domain.service;

import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsListDTO;

import java.util.UUID;

public interface MARCTransformationOptionsSettingsService {
  MARCTransformationOptionsSettingsDTO get(UUID centralServerId);

  MARCTransformationOptionsSettingsDTO create(UUID centralServerId, MARCTransformationOptionsSettingsDTO marcTransformOptSetDTO);

  MARCTransformationOptionsSettingsDTO update(UUID centralServerId, MARCTransformationOptionsSettingsDTO marcTransformOptSetDTO);

  void delete(UUID centralServerId);

  MARCTransformationOptionsSettingsListDTO getAll(int offset, int limit);
}
