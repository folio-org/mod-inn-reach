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
import org.folio.innreach.fixture.ContributionFixture;

import org.folio.innreach.batch.contribution.service.OngoingContributionStatusServiceImpl;
import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.util.JsonHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.InventoryViewService;

@ExtendWith(MockitoExtension.class)
class ContributionActionServiceImplTest {

  public static final UUID CENTRAL_SERVER_ID = UUID.randomUUID();
  @Mock
  private ContributionJobRunner contributionJobRunner;
  @Mock
  private InventoryViewService inventoryViewService;
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
    when(inventoryViewService.getInstance(holding.getInstanceId())).thenReturn(instance);
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

    service.handleHoldingUpdate(holding, holding, ongoingJob);

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

    service.handleHoldingUpdate(holding, holding, ongoingJob);

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

    service.handleHoldingUpdate(holding, holding, ongoingJob);

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

    verify(contributionJobRunner).runOngoingInstanceContribution(CENTRAL_SERVER_ID, instance, ongoingJob);
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

    verify(contributionJobRunner).runOngoingInstanceDeContribution(CENTRAL_SERVER_ID, instance, ongoingJob);
  }

  @Test
  void handleHoldingUpdateWithNullCentralServerId() {
    var instance = createInstance();
    var holding = instance.getHoldingsRecords().get(0);
    instance.getItems().get(0).setHoldingsRecordId(holding.getId());
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(null);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);

    service.handleHoldingUpdate(holding, holding, ongoingJob);

    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, INVALID_CENTRAL_SERVER_ID, FAILED);
    verifyNoInteractions(contributionJobRunner);
  }

  @Test
  void handleHoldingUpdateWithInvalidLocationMapping() {
    var instance = createInstance();
    var holding = instance.getHoldingsRecords().get(0);
    instance.getItems().get(0).setHoldingsRecordId(holding.getId());
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(CENTRAL_SERVER_ID)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(CENTRAL_SERVER_ID)).thenReturn(INVALID);

    service.handleHoldingUpdate(holding, holding, ongoingJob);

    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, INVALID_CENTRAL_SERVER_ID, FAILED);
    verifyNoInteractions(contributionJobRunner);
  }

  @Test
  void handleHoldingUpdateWithNoMatchingItems() {
    var instance = createInstance();
    var holding = instance.getHoldingsRecords().get(0);
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(CENTRAL_SERVER_ID)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(CENTRAL_SERVER_ID)).thenReturn(VALID);

    service.handleHoldingUpdate(holding, holding, ongoingJob);

    verifyNoInteractions(contributionJobRunner);
    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, PROCESSED);
  }

  @Test
  void handleHoldingUpdateWithItemMoveToDifferentInstance() {
    var newInstance = createInstance();
    var oldInstance = createInstance();
    var newHolding = newInstance.getHoldingsRecords().get(0);
    var oldHolding = oldInstance.getHoldingsRecords().get(0);
    newHolding.setInstanceId(newInstance.getId());
    oldHolding.setInstanceId(oldInstance.getId());
    var item = newInstance.getItems().get(0);
    item.setHoldingsRecordId(newHolding.getId());
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);
    when(inventoryViewService.getInstance(newHolding.getInstanceId())).thenReturn(newInstance);
    when(inventoryViewService.getInstance(oldHolding.getInstanceId())).thenReturn(oldInstance);
    when(validationService.getItemTypeMappingStatus(CENTRAL_SERVER_ID)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(CENTRAL_SERVER_ID)).thenReturn(VALID);

    service.handleHoldingUpdate(newHolding, oldHolding, ongoingJob);

    verify(contributionJobRunner).runItemMove(eq(CENTRAL_SERVER_ID), eq(newInstance), eq(oldInstance), eq(item), any());
    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, PROCESSED);
  }

  @Test
  void handleHoldingUpdateWithMultipleMatchingItems() {
    var instance = createInstance();
    var holding = instance.getHoldingsRecords().get(0);
    var item1 = instance.getItems().get(0);
    item1.setHoldingsRecordId(holding.getId());
    var item2 = ContributionFixture.createItem();
    item2.setHoldingsRecordId(holding.getId());
    instance.setItems(List.of(item1, item2));
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(CENTRAL_SERVER_ID)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(CENTRAL_SERVER_ID)).thenReturn(VALID);

    service.handleHoldingUpdate(holding, holding, ongoingJob);

    verify(contributionJobRunner).runItemContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item1), any());
    verify(contributionJobRunner).runItemContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item2), any());
    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, PROCESSED);
  }

  @Test
  void handleHoldingUpdateWithMultipleItemsMovedToDifferentInstance() {
    var newInstance = createInstance();
    var oldInstance = createInstance();
    var newHolding = newInstance.getHoldingsRecords().get(0);
    var oldHolding = oldInstance.getHoldingsRecords().get(0);
    newHolding.setInstanceId(newInstance.getId());
    oldHolding.setInstanceId(oldInstance.getId());
    var item1 = newInstance.getItems().get(0);
    item1.setHoldingsRecordId(newHolding.getId());
    var item2 = ContributionFixture.createItem();
    item2.setHoldingsRecordId(newHolding.getId());
    newInstance.setItems(List.of(item1, item2));
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);
    when(inventoryViewService.getInstance(newHolding.getInstanceId())).thenReturn(newInstance);
    when(inventoryViewService.getInstance(oldHolding.getInstanceId())).thenReturn(oldInstance);
    when(validationService.getItemTypeMappingStatus(CENTRAL_SERVER_ID)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(CENTRAL_SERVER_ID)).thenReturn(VALID);

    service.handleHoldingUpdate(newHolding, oldHolding, ongoingJob);

    verify(contributionJobRunner).runItemMove(eq(CENTRAL_SERVER_ID), eq(newInstance), eq(oldInstance), eq(item1), any());
    verify(contributionJobRunner).runItemMove(eq(CENTRAL_SERVER_ID), eq(newInstance), eq(oldInstance), eq(item2), any());
    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, PROCESSED);
  }

  @Test
  void handleHoldingUpdateWithSameInstanceId() {
    var instance = createInstance();
    var holding = instance.getHoldingsRecords().get(0);
    var item = instance.getItems().get(0);
    item.setHoldingsRecordId(holding.getId());
    var oldHolding = ContributionFixture.createHolding();
    oldHolding.setInstanceId(holding.getInstanceId());
    var ongoingJob = new OngoingContributionStatus();
    ongoingJob.setCentralServerId(CENTRAL_SERVER_ID);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(validationService.getItemTypeMappingStatus(CENTRAL_SERVER_ID)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(CENTRAL_SERVER_ID)).thenReturn(VALID);

    service.handleHoldingUpdate(holding, oldHolding, ongoingJob);

    verify(contributionJobRunner).runItemContribution(eq(CENTRAL_SERVER_ID), eq(instance), eq(item), any());
    verify(contributionJobRunner, never()).runItemMove(any(), any(), any(), any(), any());
    verify(ongoingContributionStatusService).updateOngoingContribution(ongoingJob, PROCESSED);
  }

}
