package org.folio.innreach.domain.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.folio.innreach.dto.MappingValidationStatusDTO.INVALID;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import feign.FeignException;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
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

    verify(contributionJobRunner).runInstanceContribution(CENTRAL_SERVER_ID, instance);
  }

  @Test
  void handleInstanceCreation_skipUnsupportedSource() {
    var instance = createInstance();

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(INVALID);

    service.handleInstanceCreation(instance);

    verifyNoInteractions(contributionJobRunner);
  }

  @Test
  void handleInstanceCreation_skipInvalidMappings() {
    var instance = createInstance().source("test");

    service.handleInstanceCreation(instance);

    verifyNoInteractions(contributionJobRunner);
  }

  @Test
  void handleInstanceDelete() {
    var instance = createInstance();

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    service.handleInstanceDelete(instance);

    verify(contributionJobRunner).runInstanceDeContribution(CENTRAL_SERVER_ID, instance);

    doThrow(ServiceSuspendedException.class).when(contributionJobRunner).runInstanceDeContribution(CENTRAL_SERVER_ID, instance);
    assertThatThrownBy(() -> service.handleInstanceDelete(instance))
      .isInstanceOf(ServiceSuspendedException.class);

    doThrow(FeignException.class).when(contributionJobRunner).runInstanceDeContribution(CENTRAL_SERVER_ID, instance);
    assertThatThrownBy(() -> service.handleInstanceDelete(instance))
      .isInstanceOf(FeignException.class);

    doThrow(InnReachConnectionException.class).when(contributionJobRunner).runInstanceDeContribution(CENTRAL_SERVER_ID, instance);
    assertThatThrownBy(() -> service.handleInstanceDelete(instance))
      .isInstanceOf(InnReachConnectionException.class);
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
    verify(contributionJobRunner).runItemContribution(CENTRAL_SERVER_ID, instance, item);

    doThrow(RuntimeException.class).when(validationService).getItemTypeMappingStatus(any());
    service.handleItemCreation(item);
    verify(contributionJobRunner).runItemContribution(CENTRAL_SERVER_ID, instance, item);

    doThrow(ServiceSuspendedException.class).when(validationService).getItemTypeMappingStatus(any());
    assertThatThrownBy(() -> service.handleItemCreation(item))
      .isInstanceOf(ServiceSuspendedException.class);

    doThrow(FeignException.class).when(validationService).getItemTypeMappingStatus(any());
    assertThatThrownBy(() -> service.handleItemCreation(item))
      .isInstanceOf(FeignException.class);

    doThrow(InnReachConnectionException.class).when(validationService).getItemTypeMappingStatus(any());
    assertThatThrownBy(() -> service.handleItemCreation(item))
      .isInstanceOf(InnReachConnectionException.class);
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

    verify(contributionJobRunner).runItemContribution(CENTRAL_SERVER_ID, instance, newItem);
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

    verify(contributionJobRunner).runItemMove(CENTRAL_SERVER_ID, newInstance, oldInstance, newItem);
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

    verify(contributionJobRunner).runItemContribution(CENTRAL_SERVER_ID, instance, item);
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

    verify(contributionJobRunner).runItemContribution(CENTRAL_SERVER_ID, instance, item);
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

    verify(contributionJobRunner).runItemContribution(CENTRAL_SERVER_ID, instance, item);
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

    verify(contributionJobRunner).runItemContribution(CENTRAL_SERVER_ID, instance, item);
  }

  @Test
  void handleHoldingDelete() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);

    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(CENTRAL_SERVER_ID)));
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    service.handleHoldingDelete(holding);
    verify(contributionJobRunner).runItemDeContribution(CENTRAL_SERVER_ID, instance, item);

    doThrow(ServiceSuspendedException.class).when(contributionJobRunner).runItemDeContribution(CENTRAL_SERVER_ID, instance, item);
    assertThatThrownBy(() -> service.handleHoldingDelete(holding))
      .isInstanceOf(ServiceSuspendedException.class);

    doThrow(FeignException.class).when(contributionJobRunner).runItemDeContribution(CENTRAL_SERVER_ID, instance, item);
    assertThatThrownBy(() -> service.handleHoldingDelete(holding))
      .isInstanceOf(FeignException.class);

    doThrow(InnReachConnectionException.class).when(contributionJobRunner).runItemDeContribution(CENTRAL_SERVER_ID, instance, item);
    assertThatThrownBy(() -> service.handleHoldingDelete(holding))
      .isInstanceOf(InnReachConnectionException.class);
  }
}
