package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.InventoryItemFixture.createInventoryItemDTO;
import static org.folio.innreach.fixture.ItemContributionOptionsConfigurationFixture.createItmContribOptConfDTO;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.service.ItemContributionOptionsConfigurationService;

class ContributionServiceImplTest {

  @Mock
  private ItemContributionOptionsConfigurationService itemContributionOptionsConfigurationService;

  @Mock
  private InventoryClient inventoryClient;

  @InjectMocks
  private ContributionServiceImpl contributionService;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsAvailable() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.AVAILABLE,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = contributionService.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsInTransit() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.IN_TRANSIT,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = contributionService.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsNotListedInTheNotAvailableStatuses() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.AWAITING_PICKUP,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = contributionService.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnNotAvailableContributionStatusWhenItemStatusIsUnavailable() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.UNAVAILABLE,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = contributionService.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.NOT_AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnNotAvailableContributionStatusWhenItemStatusIsListedInTheNotAvailableStatuses() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.AGED_TO_LOST,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = contributionService.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.NOT_AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnOnLoanContributionStatusWhenItemStatusIsCheckedOut() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.CHECKED_OUT,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);

    var itemCirculationStatus = contributionService.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

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

    var itemCirculationStatus = contributionService.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

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

    var itemCirculationStatus = contributionService.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

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

    var itemCirculationStatus = contributionService.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.NON_LENDABLE, itemCirculationStatus);
  }

}
