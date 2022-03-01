package org.folio.innreach.domain.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.client.ItemStorageClient;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.dto.StorageLoanDTO;
import org.folio.innreach.repository.CentralServerRepository;

@ExtendWith(MockitoExtension.class)
class ContributionActionServiceImplTest {

  public static final UUID CENTRAL_SERVER_ID = UUID.randomUUID();
  @Mock
  private ContributionJobRunner contributionJobRunner;
  @Mock
  private CentralServerRepository centralServerRepository;
  @Mock
  private ItemStorageClient itemStorageClient;
  @Mock
  private InventoryViewService inventoryViewService;
  @Mock
  private InstanceStorageClient instanceStorageClient;
  @Mock
  private HoldingsService holdingsService;
  @Mock
  private ContributionValidationService validationService;

  @InjectMocks
  private ContributionActionServiceImpl service;

  @Test
  void handleInstanceCreation() {
    var instance = createInstance();

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(any())).thenReturn(VALID);

    service.handleInstanceCreation(instance);

    verify(contributionJobRunner).runInstanceContribution(eq(CENTRAL_SERVER_ID), eq(instance));
  }

  @Test
  void handleInstanceDelete() {
    var instance = createInstance();

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));

    service.handleInstanceDelete(instance);

    verify(contributionJobRunner).runInstanceDeContribution(eq(CENTRAL_SERVER_ID), eq(instance));
  }

  @Test
  void handleItemCreation() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(holdingsService.find(any())).thenReturn(Optional.of(holding));
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(any())).thenReturn(VALID);

    service.handleItemCreation(item);

    verify(contributionJobRunner).runItemContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item));
  }

  @Test
  void handleItemUpdate() {
    var instance = createInstance();
    var oldItem = createItem();
    var newItem = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(instanceStorageClient.getInstanceById(any())).thenReturn(instance);
    when(holdingsService.find(any())).thenReturn(Optional.of(holding));
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(any())).thenReturn(VALID);

    service.handleItemUpdate(newItem, oldItem);

    verify(contributionJobRunner).runItemContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(newItem));
  }

  @Test
  void handleItemUpdate_movedItem() {
    var newInstance = createInstance();
    var oldInstance = createInstance();
    var oldItem = createItem();
    var newItem = newInstance.getItems().get(0);
    var holding = newInstance.getHoldingsRecords().get(0);

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    when(inventoryViewService.getInstance(any())).thenReturn(newInstance);
    when(instanceStorageClient.getInstanceById(any())).thenReturn(oldInstance);
    when(holdingsService.find(any())).thenReturn(Optional.of(holding));
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(any())).thenReturn(VALID);

    service.handleItemUpdate(newItem, oldItem);

    verify(contributionJobRunner).runItemMove(eq(CENTRAL_SERVER_ID), eq(newInstance), eq(oldInstance), eq(newItem));
  }

  @Test
  void handleItemDelete() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(holdingsService.find(any())).thenReturn(Optional.of(holding));

    service.handleItemDelete(item);

    verify(contributionJobRunner).runItemDeContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item));
  }

  @Test
  void handleLoanCreation() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    var loan = new StorageLoanDTO().id(UUID.randomUUID()).itemId(UUID.randomUUID());

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    when(itemStorageClient.getItemById(any())).thenReturn(Optional.of(item));
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(holdingsService.find(any())).thenReturn(Optional.of(holding));
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(any())).thenReturn(VALID);

    service.handleLoanCreation(loan);

    verify(contributionJobRunner).runItemContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item));
  }

  @Test
  void handleLoanUpdate() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    var loan = new StorageLoanDTO().id(UUID.randomUUID()).itemId(UUID.randomUUID()).action("renewed");

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    when(itemStorageClient.getItemById(any())).thenReturn(Optional.of(item));
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(holdingsService.find(any())).thenReturn(Optional.of(holding));
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(any())).thenReturn(VALID);

    service.handleLoanUpdate(loan);

    verify(contributionJobRunner).runItemContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item));
  }

  @Test
  void handleRequestChange() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    var request = RequestDTO.builder().id(UUID.randomUUID()).itemId(item.getId()).build();

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    when(itemStorageClient.getItemById(any())).thenReturn(Optional.of(item));
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(holdingsService.find(any())).thenReturn(Optional.of(holding));
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(any())).thenReturn(VALID);

    service.handleRequestChange(request);

    verify(contributionJobRunner).runItemContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item));
  }

  @Test
  void handleHoldingUpdate() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(any())).thenReturn(VALID);

    service.handleHoldingUpdate(holding);

    verify(contributionJobRunner).runItemContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item));
  }

  @Test
  void handleHoldingDelete() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    when(inventoryViewService.getInstance(any())).thenReturn(instance);

    service.handleHoldingDelete(holding);

    verify(contributionJobRunner).runItemDeContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item));
  }
}
