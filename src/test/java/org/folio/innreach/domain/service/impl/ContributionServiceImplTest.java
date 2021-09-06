package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.InventoryItemFixture.createInventoryItemDTO;
import static org.folio.innreach.fixture.ItemContributionOptionsConfigurationFixture.createItmContribOptConfDTO;
import static org.folio.innreach.fixture.JobResponseFixture.createJobResponse;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.RequestStorageClient;
import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestsDTO;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.ItemContributionOptionsConfigurationService;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.fixture.ContributionFixture.ContributionValidationServiceMock;
import org.folio.innreach.mapper.ContributionMapper;
import org.folio.innreach.mapper.ContributionMapperImpl;
import org.folio.innreach.mapper.MappingMethods;
import org.folio.innreach.repository.ContributionRepository;

class ContributionServiceImplTest {

  @Mock
  private ContributionRepository repository;

  @Spy
  private InstanceStorageClient client;

  @Mock
  private ItemContributionOptionsConfigurationService itemContributionOptionsConfigurationService;

  @Mock
  private InventoryClient inventoryClient;

  @Mock
  private RequestStorageClient requestStorageClient;

  @Mock
  private ContributionJobRunner jobRunner;

  @Spy
  private ContributionMapper mapper = new ContributionMapperImpl(new MappingMethods());

  @Spy
  private ContributionValidationService validationService = new ContributionValidationServiceMock();

  @InjectMocks
  private ContributionServiceImpl service;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void startInitialContributionProcess() {
    when(repository.save(any(Contribution.class))).thenReturn(new Contribution());
    when(client.startInitialContribution(any())).thenReturn(createJobResponse());

    service.startInitialContribution(UUID.randomUUID());

    verify(repository).save(any(Contribution.class));
    verify(client).startInitialContribution(any());
    verify(jobRunner).run(any(UUID.class), any(ContributionDTO.class));
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
    when(requestStorageClient.findRequests(any())).thenReturn(new RequestsDTO(0));

    var itemCirculationStatus = service.getItemCirculationStatus(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(ContributionItemCirculationStatus.AVAILABLE, itemCirculationStatus);
  }

  @Test
  void returnAvailableContributionStatusWhenItemStatusIsInTransitAndItemIsAlreadyRequested() {
    when(itemContributionOptionsConfigurationService.getItmContribOptConf(any())).thenReturn(createItmContribOptConfDTO());

    var inventoryItem = createInventoryItemDTO(InventoryItemStatus.IN_TRANSIT,
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(inventoryClient.getItemById(any())).thenReturn(inventoryItem);
    when(requestStorageClient.findRequests(any())).thenReturn(new RequestsDTO(1));

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

}
