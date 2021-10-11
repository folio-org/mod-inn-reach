package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.MappingValidationStatusDTO;

public interface ContributionValidationService {

  ContributionItemCirculationStatus getItemCirculationStatus(UUID centralServerId, Item item);

  Character getSuppressionStatus(UUID centralServerId, List<UUID> statisticalCodeIds);

  MappingValidationStatusDTO getItemTypeMappingStatus(UUID centralServerId);

  MappingValidationStatusDTO getLocationMappingStatus(UUID centralServerId);
}
