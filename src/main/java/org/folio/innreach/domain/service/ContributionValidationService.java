package org.folio.innreach.domain.service;

import java.util.Set;
import java.util.UUID;

import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.MappingValidationStatusDTO;

public interface ContributionValidationService {

  boolean isEligibleForContribution(UUID centralServerId, Instance instance);

  boolean isEligibleForContribution(UUID centralServerId, Item item);

  ContributionItemCirculationStatus getItemCirculationStatus(UUID centralServerId, Item item);

  Character getSuppressionStatus(UUID centralServerId, Set<UUID> statisticalCodeIds);

  MappingValidationStatusDTO getItemTypeMappingStatus(UUID centralServerId);

  /**
   * Returns the location mapping validation status for the given central server.
   * <p>
   * Note: unlike {@link #getItemTypeMappingStatus}, this method does NOT suppress exceptions.
   * Callers are expected to handle network/timeout errors (e.g. {@code InnReachTimeOutException})
   * and other runtime exceptions appropriately.
   */
  MappingValidationStatusDTO getLocationMappingStatus(UUID centralServerId);
}
