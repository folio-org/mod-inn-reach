package org.folio.innreach.domain.service.impl;

import org.folio.innreach.domain.entity.ItemContributionOptionsConfiguration;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;
import org.folio.innreach.mapper.ItemContributionOptionsConfigurationMapper;
import org.folio.innreach.repository.ItemContributionOptionsConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Optional;
import java.util.UUID;

import static org.folio.innreach.fixture.ItemContributionOptionsConfigurationFixture.createItmContribOptConf;
import static org.folio.innreach.fixture.ItemContributionOptionsConfigurationFixture.createItmContribOptConfDTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemContributionOptionsConfigurationServiceImplTest {

  @Mock
  private ItemContributionOptionsConfigurationRepository itemContributionOptionsConfigurationRepository;

  @Spy
  private final ItemContributionOptionsConfigurationMapper itemContributionOptionsConfigurationMapper = Mappers.getMapper(ItemContributionOptionsConfigurationMapper.class);

  @InjectMocks
  private ItemContributionOptionsConfigurationServiceImpl itemContributionOptionsConfigurationService;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void getItemContributionOptionsConfiguration_when_itemContributionOptionsConfigurationExists() {
    when(itemContributionOptionsConfigurationRepository.findById(any())).thenReturn(Optional.of(createItmContribOptConf()));

    var itemContributionOptionsConfiguration = itemContributionOptionsConfigurationService.getItemContributionOptionsConfiguration(UUID.randomUUID());

    verify(itemContributionOptionsConfigurationRepository).findById(any());

    assertNotNull(itemContributionOptionsConfiguration);
  }

  @Test
  void createItemContributionOptionsConfiguration_when_centralServerIsNew() {
    var itemContributionOptionsConfigurationDTO = createItmContribOptConfDTO();

    when(itemContributionOptionsConfigurationRepository.save(any(ItemContributionOptionsConfiguration.class))).thenReturn(new ItemContributionOptionsConfiguration());

    var createdItemContributionOptionsConfiguration = itemContributionOptionsConfigurationService.createItemContributionOptionsConfiguration(itemContributionOptionsConfigurationDTO);

    verify(itemContributionOptionsConfigurationMapper).toEntity(any(ItemContributionOptionsConfigurationDTO.class));
    verify(itemContributionOptionsConfigurationRepository).save(any(ItemContributionOptionsConfiguration.class));

    assertNotNull(createdItemContributionOptionsConfiguration);
  }

  @Test
  void updateItemContributionOptionsConfiguration_when_itemContributionOptionsConfigurationExists() {
    when(itemContributionOptionsConfigurationRepository.findById(any())).thenReturn(Optional.of(createItmContribOptConf()));

    var updatedItemContributionOptionsConfiguration = createItmContribOptConfDTO();

    var updatedItemContributionOptionsConfigurationDTO = itemContributionOptionsConfigurationService.updateItemContributionOptionsConfiguration(
      updatedItemContributionOptionsConfiguration);

    verify(itemContributionOptionsConfigurationRepository).findById(any());

    assertNotNull(updatedItemContributionOptionsConfigurationDTO);
    assertEquals(updatedItemContributionOptionsConfiguration.getNotAvailableItemStatuses(), updatedItemContributionOptionsConfigurationDTO.getNotAvailableItemStatuses());
    assertEquals(updatedItemContributionOptionsConfiguration.getNonLendableLoanTypes(), updatedItemContributionOptionsConfigurationDTO.getNonLendableLoanTypes());
    assertEquals(updatedItemContributionOptionsConfiguration.getNonLendableLocations(), updatedItemContributionOptionsConfigurationDTO.getNonLendableLocations());
    assertEquals(updatedItemContributionOptionsConfiguration.getNonLendableMaterialTypes(), updatedItemContributionOptionsConfigurationDTO.getNonLendableMaterialTypes());
  }

  @Test
  void throwException_when_itemContributionOptionsConfigurationDoesNotExist() {
    when(itemContributionOptionsConfigurationRepository.findById(any())).thenReturn(Optional.empty());

    UUID id = UUID.randomUUID();

    assertThrows(EntityNotFoundException.class, () -> itemContributionOptionsConfigurationService.getItemContributionOptionsConfiguration(id));

    verify(itemContributionOptionsConfigurationRepository).findById(any());
  }
}
