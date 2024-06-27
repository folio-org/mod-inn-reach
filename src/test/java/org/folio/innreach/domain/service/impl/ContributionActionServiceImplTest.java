package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.entity.ContributionStatus.FAILED;
import static org.folio.innreach.domain.entity.ContributionStatus.PROCESSED;
import static org.folio.innreach.util.InnReachConstants.INVALID_CENTRAL_SERVER_ID;
import static org.folio.innreach.util.InnReachConstants.MARC_ERROR_MSG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.folio.innreach.dto.MappingValidationStatusDTO.INVALID;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.batch.contribution.service.OngoingContributionStatusServiceImpl;
import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.util.JsonHelper;
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
  @Mock
  private OngoingContributionStatusServiceImpl ongoingContributionStatusService;
  @Mock
  private JsonHelper jsonHelper;

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
  void handleNonMarcItemCreationForOngoingJob() {
    var instance = createInstance();
    instance.setSource("Non Marc");
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    var ongoingJob = new OngoingContributionStatus();
    when(holdingsService.find(item.getHoldingsRecordId())).thenReturn(Optional.of(holding));
    when(inventoryViewService.getInstance(holding.getInstanceId())).thenReturn(instance);

    service.handleItemCreation(item, ongoingJob);

    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, MARC_ERROR_MSG, FAILED);
  }

  @Test
  void handleInvalidCentralServerIdItemCreationForOngoingJob() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    var ongoingJob = new OngoingContributionStatus();
    var centralServerId = UUID.randomUUID();
    ongoingJob.setCentralServerId(centralServerId);
    when(holdingsService.find(item.getHoldingsRecordId())).thenReturn(Optional.of(holding));
    when(inventoryViewService.getInstance(holding.getInstanceId())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(centralServerId)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(centralServerId)).thenReturn(INVALID);

    service.handleItemCreation(item, ongoingJob);

    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, INVALID_CENTRAL_SERVER_ID, FAILED);
  }

  @Test
  void handleValidItemCreationForOngoingJob() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    var ongoingJob = new OngoingContributionStatus();
    var centralServerId = UUID.randomUUID();
    ongoingJob.setCentralServerId(centralServerId);
    when(holdingsService.find(item.getHoldingsRecordId())).thenReturn(Optional.of(holding));
    when(inventoryViewService.getInstance(holding.getInstanceId())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(centralServerId)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(centralServerId)).thenReturn(VALID);

    service.handleItemCreation(item, ongoingJob);

    verify(ongoingContributionStatusService, never()).updateOngoingContribution(any(), any());
    verify(ongoingContributionStatusService, never()).updateOngoingContribution(any(), any(), any());
    verify(contributionJobRunner).runItemContribution(centralServerId, instance, item, ongoingJob);
  }

  @Test
  void handleNonMarcItemUpdateForOngoingJob() {
    var instance = createInstance();
    instance.setSource("Non Marc");
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    var ongoingJob = new OngoingContributionStatus();
    when(holdingsService.find(item.getHoldingsRecordId())).thenReturn(Optional.of(holding));
    when(inventoryViewService.getInstance(holding.getInstanceId())).thenReturn(instance);

    service.handleItemUpdate(item, item, ongoingJob);

    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, MARC_ERROR_MSG, FAILED);
  }

  @Test
  void handleInvalidCentralServerIdItemUpdateForOngoingJob() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    var ongoingJob = new OngoingContributionStatus();
    var centralServerId = UUID.randomUUID();
    ongoingJob.setCentralServerId(centralServerId);
    when(holdingsService.find(item.getHoldingsRecordId())).thenReturn(Optional.of(holding));
    when(inventoryViewService.getInstance(holding.getInstanceId())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(centralServerId)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(centralServerId)).thenReturn(INVALID);

    service.handleItemUpdate(item, item, ongoingJob);

    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, INVALID_CENTRAL_SERVER_ID, FAILED);
  }

  @Test
  void handleValidItemUpdateWithSameInstanceForOngoingJob() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    var ongoingJob = new OngoingContributionStatus();
    var centralServerId = UUID.randomUUID();
    ongoingJob.setCentralServerId(centralServerId);
    when(holdingsService.find(item.getHoldingsRecordId())).thenReturn(Optional.of(holding));
    when(inventoryViewService.getInstance(holding.getInstanceId())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(centralServerId)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(centralServerId)).thenReturn(VALID);

    service.handleItemUpdate(item, item, ongoingJob);

    verify(ongoingContributionStatusService, never()).updateOngoingContribution(any(), any());
    verify(ongoingContributionStatusService, never()).updateOngoingContribution(any(), any(), any());
    verify(contributionJobRunner).runItemContribution(centralServerId, instance, item, ongoingJob);
  }

  @Test
  void handleValidItemUpdateWithDifferentInstanceForOngoingJob() {
    var instance = createInstance();
    instance.setSource("Non Marc");
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    var ongoingJob = new OngoingContributionStatus();
    var newInstance = createInstance();
    var newHolding = newInstance.getHoldingsRecords().get(0);
    var newItem = newInstance.getItems().get(0);
    var centralServerId = UUID.randomUUID();
    ongoingJob.setCentralServerId(centralServerId);
    when(holdingsService.find(item.getHoldingsRecordId())).thenReturn(Optional.of(holding));
    when(instanceStorageClient.getInstanceById(holding.getInstanceId())).thenReturn(instance);
    when(holdingsService.find(newItem.getHoldingsRecordId())).thenReturn(Optional.of(newHolding));
    when(inventoryViewService.getInstance(newHolding.getInstanceId())).thenReturn(newInstance);
    when(validationService.getItemTypeMappingStatus(centralServerId)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(centralServerId)).thenReturn(VALID);

    service.handleItemUpdate(newItem, item, ongoingJob);

    verify(ongoingContributionStatusService, never()).updateOngoingContribution(any(), any());
    verify(ongoingContributionStatusService, never()).updateOngoingContribution(any(), any(), any());
    verify(contributionJobRunner).runItemMove(centralServerId, newInstance, instance, newItem, ongoingJob);
  }

  @Test
  void handleNonMarcItemDeleteForOngoingJob() {
    var instance = createInstance();
    instance.setSource("Non Marc");
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    var ongoingJob = new OngoingContributionStatus();
    when(holdingsService.find(item.getHoldingsRecordId())).thenReturn(Optional.of(holding));
    when(inventoryViewService.getInstance(holding.getInstanceId())).thenReturn(instance);

    service.handleItemDelete(item, ongoingJob);

    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, MARC_ERROR_MSG, FAILED);
  }

  @Test
  void handleValidItemDeleteForOngoingJob() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    var ongoingJob = new OngoingContributionStatus();
    var centralServerId = UUID.randomUUID();
    ongoingJob.setCentralServerId(centralServerId);
    when(holdingsService.find(item.getHoldingsRecordId())).thenReturn(Optional.of(holding));
    when(inventoryViewService.getInstance(holding.getInstanceId())).thenReturn(instance);

    service.handleItemDelete(item, ongoingJob);

    verify(ongoingContributionStatusService, never()).updateOngoingContribution(any(), any());
    verify(ongoingContributionStatusService, never()).updateOngoingContribution(any(), any(), any());
    verify(contributionJobRunner).runItemDeContribution(centralServerId, instance, item, ongoingJob);
  }

  @Test
  void handleNonMarcHoldingUpdateForOngoingJob() {
    var instance = createInstance();
    instance.setSource("Non Marc");
    var holding = instance.getHoldingsRecords().get(0);
    instance.getItems().get(0).setHoldingsRecordId(instance.getHoldingsRecords().get(0).getId());
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);

    service.handleHoldingUpdate(holding, ongoingJob);

    verify(contributionJobRunner, never()).runItemContribution(any(), any(), any(), any());
    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, MARC_ERROR_MSG, FAILED);
  }

  @Test
  void handleInvalidCentralIdHoldingUpdateForOngoingJob() {
    var instance = createInstance();
    var holding = instance.getHoldingsRecords().get(0);
    instance.getItems().get(0).setHoldingsRecordId(instance.getHoldingsRecords().get(0).getId());
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(INVALID);

    service.handleHoldingUpdate(holding, ongoingJob);

    verify(contributionJobRunner, never()).runItemContribution(any(), any(), any(), any());
    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, INVALID_CENTRAL_SERVER_ID, FAILED);
  }


  @Test
  void handleHoldingUpdateForOngoingJob() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    instance.getItems().get(0).setHoldingsRecordId(instance.getHoldingsRecords().get(0).getId());
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(any())).thenReturn(VALID);

    service.handleHoldingUpdate(holding, ongoingJob);

    verify(contributionJobRunner).runItemContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item), any());
    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, PROCESSED);
  }

  @Test
  void handleNonMarcHoldingDeleteForOngoingJob() {
    var instance = createInstance();
    instance.setSource("Non Marc");
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    instance.getItems().get(0).setHoldingsRecordId(instance.getHoldingsRecords().get(0).getId());
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);

    service.handleHoldingDelete(holding, ongoingJob);

    verify(contributionJobRunner, never()).runItemContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item), any());
    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, MARC_ERROR_MSG, FAILED);
  }

  @Test
  void handleHoldingDeleteForOngoingJob() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var holding = instance.getHoldingsRecords().get(0);
    instance.getItems().get(0).setHoldingsRecordId(instance.getHoldingsRecords().get(0).getId());
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);

    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    service.handleHoldingDelete(holding, ongoingJob);
    verify(contributionJobRunner).runItemDeContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item), any());
    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, PROCESSED);
  }

  @Test
  void handleInstanceCreationForOngoingJob() {
    var instance = createInstance();
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);

    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(any())).thenReturn(VALID);

    service.handleInstanceCreation(instance, ongoingJob);

    verify(contributionJobRunner).runInstanceContribution(CENTRAL_SERVER_ID, instance, ongoingJob);
  }

  @Test
  void handleInstanceCreationWithInvalidCentralForOngoingJob() {
    var instance = createInstance();
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);

    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(INVALID);

    service.handleInstanceCreation(instance, ongoingJob);

    verifyNoInteractions(contributionJobRunner);
    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, INVALID_CENTRAL_SERVER_ID, FAILED);
  }

  @Test
  void handleInstanceCreationWithInvalidSource() {
    var instance = createInstance().source("test");
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);

    service.handleInstanceCreation(instance, ongoingJob);

    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, MARC_ERROR_MSG, FAILED);
    verifyNoInteractions(contributionJobRunner);
  }

  @Test
  void handleInstanceDeleteForOngoingJob() {
    var instance = createInstance();
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);

    service.handleInstanceDelete(instance, ongoingJob);

    verify(contributionJobRunner).runInstanceDeContribution(CENTRAL_SERVER_ID, instance, ongoingJob);
  }

}
