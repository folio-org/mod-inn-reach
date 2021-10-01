package org.folio.innreach.domain.service.impl;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.InventoryItemFixture.createInventoryItemDTO;
import static org.folio.innreach.fixture.ItemContributionOptionsConfigurationFixture.createItmContribOptConfDTO;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.MaterialTypesClient;
import org.folio.innreach.client.RequestStorageClient;
import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestsDTO;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.ContributionCriteriaConfigurationService;
import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.domain.service.ItemContributionOptionsConfigurationService;
import org.folio.innreach.domain.service.LibraryMappingService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.external.service.InnReachLocationExternalService;

class ContributionValidationServiceImplTest {

  @Mock
  private MaterialTypesClient materialTypesClient;
  @Mock
  private MaterialTypeMappingService typeMappingService;
  @Mock
  private LibraryMappingService libraryMappingService;
  @Mock
  private CentralServerService centralServerService;
  @Mock
  private ContributionCriteriaConfigurationService contributionConfigService;
  @Mock
  private InnReachLocationService innReachLocationService;
  @Mock
  private InnReachLocationExternalService innReachLocationExternalService;
  @Mock
  private ItemContributionOptionsConfigurationService itemContributionOptionsConfigurationService;
  @Mock
  private InventoryClient inventoryClient;
  @Mock
  private RequestStorageClient requestStorageClient;

  @InjectMocks
  private ContributionValidationServiceImpl service;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsAvailable() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.AVAILABLE,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsInTransitAndItemIsNotRequested() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.IN_TRANSIT,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);
    when(requestStorageClient.findRequests(any())).thenReturn(new RequestsDTO(null, 0));

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsInTransitAndItemIsAlreadyRequested() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.IN_TRANSIT,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);
    when(requestStorageClient.findRequests(any())).thenReturn(new RequestsDTO(List.of(new RequestDTO()), 1));

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.NOT_AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsNotListedInTheNotAvailableStatuses() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.AWAITING_PICKUP,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnNotAvailableContributionStatusWhenItemStatusIsUnavailable() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.UNAVAILABLE,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.NOT_AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnNotAvailableContributionStatusWhenItemStatusIsListedInTheNotAvailableStatuses() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.AGED_TO_LOST,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.NOT_AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnOnLoanContributionStatusWhenItemStatusIsCheckedOut() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.CHECKED_OUT,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.ON_LOAN, itemCirculationStatus);
  }

  @Test
  void returnNonLendableContributionStatusWhenItemIsItemNonLendableByLoanTypes() {
    var itmContribOptConfDTO = createItmContribOptConfDTO();
    var nonLendableLoanTypes = itmContribOptConfDTO.getNonLendableLoanTypes();

    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(itmContribOptConfDTO);

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.AVAILABLE,
      UUID.randomUUID(), nonLendableLoanTypes.get(0), nonLendableLoanTypes.get(1), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.NON_LENDABLE, itemCirculationStatus);
  }

  @Test
  void returnNonLendableContributionStatusWhenItemIsItemNonLendableByLocations() {
    var itmContribOptConfDTO = createItmContribOptConfDTO();
    var nonLendableLocations = itmContribOptConfDTO.getNonLendableLocations();

    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(itmContribOptConfDTO);

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.AVAILABLE,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), nonLendableLocations.get(0));

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.NON_LENDABLE, itemCirculationStatus);
  }

  @Test
  void returnNonLendableContributionStatusWhenItemIsItemNonLendableByMaterialTypes() {
    var itmContribOptConfDTO = createItmContribOptConfDTO();
    var nonLendableMaterialTypes = itmContribOptConfDTO.getNonLendableMaterialTypes();

    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(itmContribOptConfDTO);

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.AVAILABLE,
      nonLendableMaterialTypes.get(0), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.NON_LENDABLE, itemCirculationStatus);
  }

  @Test
  void returnNullSuppressionStatus() {
    when(contributionConfigService.getCriteria(any())).thenReturn(new ContributionCriteriaDTO());

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), singletonList(UUID.randomUUID()));

    assertNull(suppress);
  }

  @Test
  void returnSuppressionStatus_y() {
    var statisticalCodeId = UUID.randomUUID();
    var config = new ContributionCriteriaDTO();
    config.setContributeButSuppressId(statisticalCodeId);

    when(contributionConfigService.getCriteria(any())).thenReturn(config);

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), singletonList(statisticalCodeId));

    assertNotNull(suppress);

    assertEquals('y', suppress);
  }

  @Test
  void returnSuppressionStatus_n() {
    var statisticalCodeId = UUID.randomUUID();
    var config = new ContributionCriteriaDTO();
    config.setDoNotContributeId(statisticalCodeId);

    when(contributionConfigService.getCriteria(any())).thenReturn(config);

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), singletonList(statisticalCodeId));

    assertNotNull(suppress);

    assertEquals('n', suppress);
  }

  @Test
  void returnSuppressionStatus_g() {
    var statisticalCodeId = UUID.randomUUID();
    var config = new ContributionCriteriaDTO();
    config.setContributeAsSystemOwnedId(statisticalCodeId);

    when(contributionConfigService.getCriteria(any())).thenReturn(config);

    var suppress = service.getSuppressionStatus(UUID.randomUUID(), singletonList(statisticalCodeId));

    assertNotNull(suppress);

    assertEquals('g', suppress);
  }

  @Test
  void throwWhenMultipleStatisticalCodes() {
    when(contributionConfigService.getCriteria(any())).thenReturn(new ContributionCriteriaDTO());

    assertThatThrownBy(() -> service.getSuppressionStatus(UUID.randomUUID(), Collections.nCopies(3, UUID.randomUUID())))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Multiple statistical codes defined");
  }

}
