package org.folio.innreach.domain.service;

import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;

import java.util.UUID;

public interface ItemContributionOptionsConfigurationService {
  ItemContributionOptionsConfigurationDTO getItmContribOptConf(UUID centralServerId);

  ItemContributionOptionsConfigurationDTO createItmContribOptConf(UUID centralServerId, ItemContributionOptionsConfigurationDTO itmContribOptConfDTO);

  ItemContributionOptionsConfigurationDTO updateItmContribOptConf(UUID centralServerId, ItemContributionOptionsConfigurationDTO itmContribOptConfDTO);
}
