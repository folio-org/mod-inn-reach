package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ItemContributionOptionsConfigurationFixture.createItmContribOptConf;
import static org.folio.innreach.fixture.ItemContributionOptionsConfigurationFixture.createItmContribOptConfDTO;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import org.folio.innreach.domain.entity.ItemContributionOptionsConfiguration;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;
import org.folio.innreach.mapper.ItemContributionOptionsConfigurationMapper;
import org.folio.innreach.mapper.ItemContributionOptionsConfigurationMapperImpl;
import org.folio.innreach.mapper.MappingMethods;
import org.folio.innreach.repository.ItemContributionOptionsConfigurationRepository;

class ItemContributionOptionsConfigurationServiceImplTest {

  @Mock
  private ItemContributionOptionsConfigurationRepository repository;

  @Spy
  private final ItemContributionOptionsConfigurationMapper mapper = new ItemContributionOptionsConfigurationMapperImpl(new MappingMethods());

  @InjectMocks
  private ItemContributionOptionsConfigurationServiceImpl service;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void getItmContribOptConf_when_itmContribOptConfExists() {
    when(repository.findOne(any())).thenReturn(Optional.of(createItmContribOptConf()));

    var itmContribOptConf = service.getItmContribOptConf(UUID.randomUUID());

    verify(repository).findOne(any());

    assertNotNull(itmContribOptConf);
  }

  @Test
  void createItmContribOptConf_when_centralServerIsNew() {
    var itmContribOptConfDTO = createItmContribOptConfDTO();

    when(repository.save(any(ItemContributionOptionsConfiguration.class))).thenReturn(new ItemContributionOptionsConfiguration());

    var createdItmContribOptConf = service.createItmContribOptConf(UUID.randomUUID(), itmContribOptConfDTO);

    verify(mapper).toEntity(any(ItemContributionOptionsConfigurationDTO.class));
    verify(repository).save(any(ItemContributionOptionsConfiguration.class));

    assertNotNull(createdItmContribOptConf);
  }

  @Test
  void updateItmContribOptConf_when_itmContribOptConfExists() {
    when(repository.findOne(any())).thenReturn(Optional.of(createItmContribOptConf()));

    var updatedItmContribOptConf = createItmContribOptConfDTO();

    var updatedItmContribOptConfDTO = service.updateItmContribOptConf(UUID.randomUUID(), updatedItmContribOptConf);

    verify(repository).findOne(any());

    assertNotNull(updatedItmContribOptConfDTO);
    assertEquals(updatedItmContribOptConf.getNotAvailableItemStatuses(), updatedItmContribOptConfDTO.getNotAvailableItemStatuses());
    assertEquals(updatedItmContribOptConf.getNonLendableLoanTypes(), updatedItmContribOptConfDTO.getNonLendableLoanTypes());
    assertEquals(updatedItmContribOptConf.getNonLendableLocations(), updatedItmContribOptConfDTO.getNonLendableLocations());
    assertEquals(updatedItmContribOptConf.getNonLendableMaterialTypes(), updatedItmContribOptConfDTO.getNonLendableMaterialTypes());
  }

  @Test
  void throwException_when_itmContribOptConfDoesNotExist() {
    when(repository.findOne(any())).thenReturn(Optional.empty());

    UUID id = UUID.randomUUID();

    assertThrows(EntityNotFoundException.class, () -> service.getItmContribOptConf(id));

    verify(repository).findOne(any());
  }
}
