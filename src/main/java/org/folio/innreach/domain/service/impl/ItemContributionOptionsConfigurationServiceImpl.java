package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.entity.ItemContributionOptionsConfiguration;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.ItemContributionOptionsConfigurationService;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;
import org.folio.innreach.mapper.ItemContributionOptionsConfigurationMapper;
import org.folio.innreach.repository.ItemContributionOptionsConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityExistsException;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ItemContributionOptionsConfigurationServiceImpl implements ItemContributionOptionsConfigurationService {

  private final ItemContributionOptionsConfigurationRepository repository;
  private final ItemContributionOptionsConfigurationMapper mapper;

  private static final String TEXT_ITM_CONTRIB_OPT_CONFIG_WITH_ID = "Item Contribution Options Configuration with id: ";

  @Override
  @Transactional(readOnly = true)
  public ItemContributionOptionsConfigurationDTO getItmContribOptConf(UUID centralServerId) {
    var itmContribOptConf = repository.findById(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException(TEXT_ITM_CONTRIB_OPT_CONFIG_WITH_ID + centralServerId + " not found"));
    return mapper.toDto(itmContribOptConf);
  }

  @Override
  public ItemContributionOptionsConfigurationDTO createItmContribOptConf(ItemContributionOptionsConfigurationDTO itmContribOptConfDTO) {
    var centralServerId = itmContribOptConfDTO.getCentralServerId();
    repository.findById(centralServerId).ifPresent(itmContribOptConf -> {
      throw new EntityExistsException(TEXT_ITM_CONTRIB_OPT_CONFIG_WITH_ID
        + itmContribOptConf.getCentralServerId() + " already exists.");
    });
    var itmContribOptConf = mapper.toEntity(itmContribOptConfDTO);
    var createdItmContribOptConf = repository.save(itmContribOptConf);
    return mapper.toDto(createdItmContribOptConf);
  }

  @Override
  public ItemContributionOptionsConfigurationDTO updateItmContribOptConf(ItemContributionOptionsConfigurationDTO itmContribOptConfDTO) {
    var itmContribOptConf = repository.findById(itmContribOptConfDTO.getCentralServerId())
      .orElseThrow(() -> new EntityNotFoundException(TEXT_ITM_CONTRIB_OPT_CONFIG_WITH_ID + itmContribOptConfDTO.getCentralServerId() + " not found"));
    var updatedItmContribOptConf = mapper.toEntity(itmContribOptConfDTO);
    updateItmContribOptConf(itmContribOptConf, updatedItmContribOptConf);

    repository.save(itmContribOptConf);

    return mapper.toDto(itmContribOptConf);
  }

  private void updateItmContribOptConf(ItemContributionOptionsConfiguration itmContribOptConf, ItemContributionOptionsConfiguration updatedItmContribOptConf) {
    itmContribOptConf.setNotAvailableItemStatuses(updatedItmContribOptConf.getNotAvailableItemStatuses());
    itmContribOptConf.setNonLendableLoanTypes(updatedItmContribOptConf.getNonLendableLoanTypes());
    itmContribOptConf.setNonLendableLocations(updatedItmContribOptConf.getNonLendableLocations());
    itmContribOptConf.setNonLendableMaterialTypes(updatedItmContribOptConf.getNonLendableMaterialTypes());
  }
}
