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

  private final ItemContributionOptionsConfigurationRepository itemContributionOptionsConfigurationRepository;
  private final ItemContributionOptionsConfigurationMapper itemContributionOptionsConfigurationMapper;

  private static final String TEXT_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_WITH_ID
    = "Item Contribution Options Configuration with id: ";

  @Override
  @Transactional(readOnly = true)
  public ItemContributionOptionsConfigurationDTO getItemContributionOptionsConfiguration(UUID centralServerId) {
    var itemContributionOptionsConfiguration = itemContributionOptionsConfigurationRepository.findById(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException(TEXT_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_WITH_ID + centralServerId + " not found"));
    return itemContributionOptionsConfigurationMapper.toDto(itemContributionOptionsConfiguration);
  }

  @Override
  public ItemContributionOptionsConfigurationDTO createItemContributionOptionsConfiguration(ItemContributionOptionsConfigurationDTO itemContributionOptionsConfigurationDTO) {
    var centralServerId = itemContributionOptionsConfigurationDTO.getCentralServerId();
    itemContributionOptionsConfigurationRepository.findById(centralServerId).ifPresent(
      itemContributionOptionsConfiguration -> {
        throw new EntityExistsException(TEXT_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_WITH_ID
          + itemContributionOptionsConfiguration.getCentralServerId() + " already exists.");
      });
    var itemContributionOptionsConfiguration
      = itemContributionOptionsConfigurationMapper.toEntity(itemContributionOptionsConfigurationDTO);
    var createdItemContributionOptionsConfiguration =
      itemContributionOptionsConfigurationRepository.save(itemContributionOptionsConfiguration);
    return itemContributionOptionsConfigurationMapper.toDto(createdItemContributionOptionsConfiguration);
  }

  @Override
  public ItemContributionOptionsConfigurationDTO updateItemContributionOptionsConfiguration(ItemContributionOptionsConfigurationDTO itemContributionOptionsConfigurationDTO) {
    var itemContributionOptionsConfiguration =
      itemContributionOptionsConfigurationRepository.findById(itemContributionOptionsConfigurationDTO.getCentralServerId())
        .orElseThrow(() -> new EntityNotFoundException(TEXT_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_WITH_ID + itemContributionOptionsConfigurationDTO.getCentralServerId() + " not found"));
    var updatedItemContributionOptionsConfiguration =
      itemContributionOptionsConfigurationMapper.toEntity(itemContributionOptionsConfigurationDTO);
    updateItemContributionOptionsConfiguration(itemContributionOptionsConfiguration, updatedItemContributionOptionsConfiguration);

    itemContributionOptionsConfigurationRepository.save(itemContributionOptionsConfiguration);

    return itemContributionOptionsConfigurationMapper.toDto(itemContributionOptionsConfiguration);
  }

  private void updateItemContributionOptionsConfiguration(ItemContributionOptionsConfiguration itemContributionOptionsConfiguration, ItemContributionOptionsConfiguration updatedItemContributionOptionsConfiguration) {
    itemContributionOptionsConfiguration.setNotAvailableItemStatuses
      (updatedItemContributionOptionsConfiguration.getNotAvailableItemStatuses());
    itemContributionOptionsConfiguration.setNonLendableLoanTypes
      (updatedItemContributionOptionsConfiguration.getNonLendableLoanTypes());
    itemContributionOptionsConfiguration.setNonLendableLocations
      (updatedItemContributionOptionsConfiguration.getNonLendableLocations());
    itemContributionOptionsConfiguration.setNonLendableMaterialTypes
      (updatedItemContributionOptionsConfiguration.getNonLendableMaterialTypes());
  }
}
