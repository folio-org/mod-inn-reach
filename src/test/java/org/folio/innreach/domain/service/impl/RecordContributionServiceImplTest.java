package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.CentralServerFixture.createCentralServer;
import static org.folio.innreach.fixture.InventoryItemFixture.createInventoryItem;
import static org.folio.innreach.fixture.LocalAgencyFixture.createLocalAgency;
import static org.folio.innreach.fixture.TestUtil.randomFiveCharacterCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.repository.LocalAgencyRepository;

class RecordContributionServiceImplTest {
  private static final UUID PERMANENT_LOCATION_ID = UUID.fromString("7C244444-AE7C-11EB-8529-0242AC130004");

  @Mock
  private LocalAgencyRepository localAgencyRepository;
  @Mock
  private ContributionValidationService validationService;

  @InjectMocks
  private RecordContributionServiceImpl service;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void itemShouldPassValidation() {
    var localAgency = createLocalAgency();
    var centralServer = createCentralServer();
    centralServer.setCentralServerCode(randomFiveCharacterCode());
    localAgency.setCentralServer(centralServer);
    when(localAgencyRepository.fetchOneByLibraryId(any())).thenReturn(Optional.of(localAgency));

    var item = createInventoryItem();
    item.setPermanentLocationId(PERMANENT_LOCATION_ID);
    item.setStatisticalCodeIds(List.of(UUID.randomUUID()));
    item.setHoldingStatisticalCodeIds(List.of(UUID.randomUUID()));

    var valid = service.evaluateInventoryItemForContribution(item);
    assertTrue(valid);
  }

  @Test
  void itemShouldNotPassValidationWhenMoreThanOneStatisticalCodeDefined() {
    var localAgency = createLocalAgency();
    var centralServer = createCentralServer();
    centralServer.setCentralServerCode(randomFiveCharacterCode());
    localAgency.setCentralServer(centralServer);
    when(localAgencyRepository.fetchOneByLibraryId(any())).thenReturn(Optional.of(localAgency));

    var item = createInventoryItem();
    item.setPermanentLocationId(PERMANENT_LOCATION_ID);
    item.setStatisticalCodeIds(List.of(UUID.randomUUID(), UUID.randomUUID()));
    item.setHoldingStatisticalCodeIds(List.of(UUID.randomUUID()));

    var valid = service.evaluateInventoryItemForContribution(item);
    assertFalse(valid);
  }

  @Test
  void itemShouldNotPassValidationWhenCannotFindCentralServer() {
    when(localAgencyRepository.fetchOneByLibraryId(any())).thenReturn(Optional.empty());

    var item = createInventoryItem();
    item.setPermanentLocationId(PERMANENT_LOCATION_ID);
    item.setStatisticalCodeIds(List.of(UUID.randomUUID(), UUID.randomUUID()));
    item.setHoldingStatisticalCodeIds(List.of(UUID.randomUUID()));

    var valid = service.evaluateInventoryItemForContribution(item);
    assertFalse(valid);
  }

  @Test
  void itemShouldNotPassValidationWhenHasDoNotContributeHoldingStatisticalCode() {
    var holdingStatisticalCode = UUID.randomUUID();
    var doNotContribute = 'n';
    var localAgency = createLocalAgency();
    var centralServer = createCentralServer();
    centralServer.setCentralServerCode(randomFiveCharacterCode());
    localAgency.setCentralServer(centralServer);
    when(localAgencyRepository.fetchOneByLibraryId(any())).thenReturn(Optional.of(localAgency));
    when(validationService.getSuppressionStatus(any(), eq(List.of(holdingStatisticalCode)))).thenReturn(doNotContribute);

    var item = createInventoryItem();
    item.setPermanentLocationId(PERMANENT_LOCATION_ID);
    item.setStatisticalCodeIds(List.of(UUID.randomUUID()));
    item.setHoldingStatisticalCodeIds(List.of(holdingStatisticalCode));

    var valid = service.evaluateInventoryItemForContribution(item);
    assertFalse(valid);
  }
}
