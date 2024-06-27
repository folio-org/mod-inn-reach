package org.folio.innreach.batch.contribution.service;

import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.client.InventoryViewClient;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.event.EntityChangedData;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.MappingValidationStatusDTO;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.mapper.OngoingContributionStatusMapper;
import org.folio.innreach.repository.OngoingContributionStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.folio.innreach.domain.entity.ContributionStatus.DE_CONTRIBUTED;
import static org.folio.innreach.domain.entity.ContributionStatus.FAILED;
import static org.folio.innreach.domain.entity.ContributionStatus.PROCESSED;
import static org.folio.innreach.domain.entity.ContributionStatus.RETRY;
import static org.folio.innreach.fixture.ContributionFixture.createHolding;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createItem;
import static org.folio.innreach.util.InnReachConstants.INVALID_CENTRAL_SERVER_ID;
import static org.folio.innreach.util.InnReachConstants.MARC_ERROR_MSG;
import static org.folio.innreach.util.InnReachConstants.RETRY_LIMIT_MESSAGE;
import static org.folio.innreach.util.InnReachConstants.SKIPPING_INELIGIBLE_MSG;
import static org.folio.innreach.util.InnReachConstants.UNKNOWN_TYPE_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OngoingContributionEventProcessorTest extends BaseControllerTest {
  private final UUID CENTRAL_SERVER_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);
  private static final String TENANT = "test_tenant";
  @Autowired
  OngoingContributionEventProcessor eventProcessor;
  @SpyBean
  OngoingContributionStatusRepository ongoingContributionStatusRepository;
  @MockBean
  RecordContributionService recordContributionService;
  @MockBean
  ContributionValidationService validationService;
  @Autowired
  OngoingContributionStatusMapper ongoingContributionStatusMapper;
  @MockBean
  HoldingsService holdingsService;
  @MockBean
  InventoryViewClient inventoryViewClient;
  @MockBean
  InstanceStorageClient instanceStorageClient;
  UUID itemId;
  UUID holdingId;
  DomainEvent<Item> itemCreate;
  DomainEvent<Item> itemDelete;
  DomainEvent<Item> itemUpdate;
  DomainEvent<Holding> holdingUpdate;
  DomainEvent<Holding> holdingDelete;
  Holding holdings;
  Instance instance;
  Item item;
  InventoryViewClient.InstanceView instanceView;
  UUID instanceId;
  DomainEvent<Instance> instanceCreate;
  DomainEvent<Instance> instanceUpdate;
  DomainEvent<Instance> instanceDelete;
  @BeforeEach
  void runBefore() {
    itemId = UUID.randomUUID();
    holdingId = UUID.randomUUID();
    instanceId = UUID.randomUUID();
    itemCreate = createItemDomainEvent(itemId, DomainEventType.CREATED);
    itemDelete = createItemDomainEvent(itemId, DomainEventType.DELETED);
    itemUpdate = createItemDomainEvent(itemId, DomainEventType.UPDATED);
    holdingUpdate = createHoldingDomainEvent(holdingId, DomainEventType.UPDATED);
    holdingDelete = createHoldingDomainEvent(holdingId, DomainEventType.DELETED);
    instanceCreate = createInstanceDomainEvent(instanceId, DomainEventType.CREATED);
    instanceUpdate = createInstanceDomainEvent(instanceId, DomainEventType.UPDATED);
    instanceDelete = createInstanceDomainEvent(instanceId, DomainEventType.DELETED);
    holdings = createHolding();
    instance = createInstance();
    item = createItem();
    instanceView = new InventoryViewClient.InstanceView();
    instanceView.setInstance(instance);
    when(inventoryViewClient.getInstanceById(holdings.getInstanceId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(inventoryViewClient.getInstanceById(holdings.getInstanceId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(validationService.getItemTypeMappingStatus(CENTRAL_SERVER_ID))
      .thenReturn(MappingValidationStatusDTO.VALID);
    when(validationService.getLocationMappingStatus(CENTRAL_SERVER_ID))
      .thenReturn(MappingValidationStatusDTO.VALID);
  }

  @Test
  void testItemCreationEventWithNonMarcRecord() {
    instance.setSource("Non marc");
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemCreate), UUID.randomUUID());
    when(holdingsService.find(itemCreate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
        verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(MARC_ERROR_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testItemCreationEventWithInvalidCentralServerId() {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemCreate), UUID.randomUUID());
    when(holdingsService.find(itemCreate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
        verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(INVALID_CENTRAL_SERVER_ID, ongoingContributionStatus.getError());
  }

  @Test
  void testItemCreationEventWithInEligibleItem() {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemCreate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemCreate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(false);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(false);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
        verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(SKIPPING_INELIGIBLE_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testItemCreationEventWithInEligibleItemButAlreadyContributed() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemCreate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemCreate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(false);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    doNothing().when(recordContributionService).deContributeInstance(any(), any());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
        verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService).deContributeInstance(any(), any());
    verify(recordContributionService, never()).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItems(any(), any(), any());
    verify(recordContributionService, never()).deContributeItem(any(), any());
    assertEquals(DE_CONTRIBUTED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testItemCreationEventWithEligibleInstanceAndItem() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemCreate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemCreate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(UUID.class), any(Instance.class)))
      .thenReturn(true);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    doNothing().when(recordContributionService).contributeInstance(any(), any());
    when(recordContributionService.contributeItems(any(), any(), any())).thenReturn(1);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
        verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService, never()).deContributeInstance(any(), any());
    verify(recordContributionService).contributeInstance(any(), any());
    verify(recordContributionService).contributeItems(any(), any(), any());
    verify(recordContributionService, never()).deContributeItem(any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testItemCreationEventWithEligibleInstanceAndInEligibleItem() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemCreate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemCreate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(false);
    when(validationService.isEligibleForContribution(any(UUID.class), any(Instance.class)))
      .thenReturn(true);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    doNothing().when(recordContributionService).contributeInstance(any(), any());
    doNothing().when(recordContributionService).deContributeItem(any(), any());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
        verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService, never()).deContributeInstance(any(), any());
    verify(recordContributionService).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItems(any(), any(), any());
    verify(recordContributionService).deContributeItem(any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testItemDeletionEventWithNonMarcRecord() {
    instance.setSource("Non marc");
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemDelete), UUID.randomUUID());
    when(holdingsService.find(itemDelete.getData().getOldEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
        verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(MARC_ERROR_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testItemDeletionEventWithNonContributedItem() {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemDelete), UUID.randomUUID());
    when(holdingsService.find(itemDelete.getData().getOldEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    when(recordContributionService.isContributed(any(), any(), any()))
      .thenReturn(false);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
        verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(SKIPPING_INELIGIBLE_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testItemDeletionEventWithContributedItemAndInvalidInstance() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemDelete), UUID.randomUUID());
    when(holdingsService.find(itemDelete.getData().getOldEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    when(recordContributionService.isContributed(any(), any(), any()))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Instance.class)))
      .thenReturn(false);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService, never()).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItems(any(), any(), any());
    verify(recordContributionService, never()).deContributeItem(any(), any());
    verify(recordContributionService).deContributeInstance(any(), any());
    assertEquals(DE_CONTRIBUTED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testItemDeletionEventWithContributedItemAndInstance() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemDelete), UUID.randomUUID());
    when(holdingsService.find(itemDelete.getData().getOldEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    when(recordContributionService.isContributed(any(), any(), any()))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Instance.class)))
      .thenReturn(true);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
        verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService, never()).deContributeInstance(any(), any());
    verify(recordContributionService).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItems(any(), any(), any());
    verify(recordContributionService).deContributeItem(any(), any());
    verify(recordContributionService, never()).deContributeInstance(any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testItemUpdateEventWithNonMarcRecord() {
    instance.setSource("Non marc");
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemUpdate), UUID.randomUUID());
    when(holdingsService.find(itemUpdate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(MARC_ERROR_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testItemUpdateEventWithInvalidCentralId() {
    itemUpdate.getData().getOldEntity().setHoldingsRecordId(itemUpdate.getData().getNewEntity().getHoldingsRecordId());
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemUpdate), UUID.randomUUID());
    when(holdingsService.find(itemUpdate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(INVALID_CENTRAL_SERVER_ID, ongoingContributionStatus.getError());
  }

  @Test
  void testItemUpdateEventWithInEligibleItem() {
    itemUpdate.getData().getOldEntity().setHoldingsRecordId(itemUpdate.getData().getNewEntity().getHoldingsRecordId());
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemUpdate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemUpdate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(false);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(false);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(SKIPPING_INELIGIBLE_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testItemUpdateEventWithInEligibleItemButAlreadyContributed() throws SocketTimeoutException {
    itemUpdate.getData().getOldEntity().setHoldingsRecordId(itemUpdate.getData().getNewEntity().getHoldingsRecordId());
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemUpdate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemUpdate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(false);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    doNothing().when(recordContributionService).deContributeInstance(any(), any());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService).deContributeInstance(any(), any());
    verify(recordContributionService, never()).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItems(any(), any(), any());
    verify(recordContributionService, never()).deContributeItem(any(), any());
    assertEquals(DE_CONTRIBUTED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testItemUpdateEventWithEligibleInstanceAndItem() throws SocketTimeoutException {
    itemUpdate.getData().getOldEntity().setHoldingsRecordId(itemUpdate.getData().getNewEntity().getHoldingsRecordId());
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemUpdate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemUpdate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(UUID.class), any(Instance.class)))
      .thenReturn(true);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    doNothing().when(recordContributionService).contributeInstance(any(), any());
    when(recordContributionService.contributeItems(any(), any(), any())).thenReturn(1);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService, never()).deContributeInstance(any(), any());
    verify(recordContributionService).contributeInstance(any(), any());
    verify(recordContributionService).contributeItems(any(), any(), any());
    verify(recordContributionService, never()).deContributeItem(any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testItemUpdateEventWithEligibleInstanceAndInEligibleItem() throws SocketTimeoutException {
    itemUpdate.getData().getOldEntity().setHoldingsRecordId(itemUpdate.getData().getNewEntity().getHoldingsRecordId());
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemUpdate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemUpdate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(false);
    when(validationService.isEligibleForContribution(any(UUID.class), any(Instance.class)))
      .thenReturn(true);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    doNothing().when(recordContributionService).contributeInstance(any(), any());
    doNothing().when(recordContributionService).deContributeItem(any(), any());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService, never()).deContributeInstance(any(), any());
    verify(recordContributionService).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItems(any(), any(), any());
    verify(recordContributionService).deContributeItem(any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testItemUpdateEventWithInstanceUpdateAndInEligibleItem() {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemUpdate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemUpdate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    var newHoldings = createHolding();
    when(holdingsService.find(itemUpdate.getData().getOldEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(newHoldings));
    when(instanceStorageClient.getInstanceById(newHoldings.getInstanceId()))
      .thenReturn(createInstance());
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(false);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(false);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(SKIPPING_INELIGIBLE_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testItemUpdateEventWithInstanceUpdateAndContributedIneligibleItem() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemUpdate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemUpdate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    var newHoldings = createHolding();
    when(holdingsService.find(itemUpdate.getData().getOldEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(newHoldings));
    when(instanceStorageClient.getInstanceById(newHoldings.getInstanceId()))
      .thenReturn(createInstance());
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(false);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(UUID.class), any(Instance.class)))
      .thenReturn(true);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService).deContributeItem(any(), any());
    verify(recordContributionService, times(2)).contributeInstance(any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testItemUpdateEventWithInstanceUpdateAndContributedeligibleItem() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemUpdate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemUpdate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    var newHoldings = createHolding();
    when(holdingsService.find(itemUpdate.getData().getOldEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(newHoldings));
    when(instanceStorageClient.getInstanceById(newHoldings.getInstanceId()))
      .thenReturn(createInstance());
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(true);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(UUID.class), any(Instance.class)))
      .thenReturn(true);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService).deContributeItem(any(), any());
    verify(recordContributionService, times(2)).contributeInstance(any(), any());
    verify(recordContributionService, times(1)).contributeItems(any(), any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testItemUpdateEventWithInstanceUpdateAndIneligibleInstance() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemUpdate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemUpdate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    var newHoldings = createHolding();
    when(holdingsService.find(itemUpdate.getData().getOldEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(newHoldings));
    when(instanceStorageClient.getInstanceById(newHoldings.getInstanceId()))
      .thenReturn(createInstance());
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(true);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(UUID.class), any(Instance.class)))
      .thenReturn(false);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService).deContributeInstance(any(), any());
    verify(recordContributionService, never()).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItems(any(), any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testItemUpdateEventThrowInnReachConnectionException() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemUpdate), CENTRAL_SERVER_ID);
    when(holdingsService.find(itemUpdate.getData().getNewEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(holdings));
    var newHoldings = createHolding();
    when(holdingsService.find(itemUpdate.getData().getOldEntity().getHoldingsRecordId()))
      .thenReturn(Optional.of(newHoldings));
    when(instanceStorageClient.getInstanceById(newHoldings.getInstanceId()))
      .thenReturn(createInstance());
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(true);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(UUID.class), any(Instance.class)))
      .thenReturn(false);
    doThrow(InnReachConnectionException.class).when(recordContributionService).deContributeInstance(any(), any());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(RETRY, ongoingContributionStatus.getStatus());
    assertEquals(1, ongoingContributionStatus.getRetryAttempts());
    assertNull(ongoingContributionStatus.getError());
    doThrow(ServiceSuspendedException.class).when(recordContributionService).deContributeInstance(any(), any());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(3)).save(any()));
    assertEquals(RETRY, ongoingContributionStatus.getStatus());
    assertEquals(2, ongoingContributionStatus.getRetryAttempts());
    assertNull(ongoingContributionStatus.getError());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(4)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(RETRY_LIMIT_MESSAGE, ongoingContributionStatus.getError());
  }

  @Test
  void testItemEventWithUnknownEventType() {
    itemId = UUID.randomUUID();
    itemCreate = createItemDomainEvent(itemId, DomainEventType.ALL_DELETED);
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertItemToEntity(itemCreate), UUID.randomUUID());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(UNKNOWN_TYPE_MESSAGE, ongoingContributionStatus.getError());
  }

  @Test
  void testHoldingUpdateEventWithNonMarcRecord() {
    instance.setSource("Non marc");
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertHoldingToEntity(holdingUpdate), UUID.randomUUID());
    when(inventoryViewClient.getInstanceById(holdingUpdate.getData().getNewEntity().getInstanceId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(MARC_ERROR_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testHoldingUpdateEventWithInvalidCentralServerId() {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertHoldingToEntity(holdingUpdate), UUID.randomUUID());
    when(inventoryViewClient.getInstanceById(holdingUpdate.getData().getNewEntity().getInstanceId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(INVALID_CENTRAL_SERVER_ID, ongoingContributionStatus.getError());
  }

  @Test
  void testHoldingUpdateEventWithInEligibleItem() {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertHoldingToEntity(holdingUpdate), CENTRAL_SERVER_ID);
    holdings.setId(holdingId);
    instanceView.setHoldingsRecords(List.of(holdings));
    item.setHoldingsRecordId(holdings.getId());
    instanceView.setItems(List.of(item, createItem()));
    when(inventoryViewClient.getInstanceById(holdingUpdate.getData().getNewEntity().getInstanceId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(false);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(false);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(3)).save(any()));
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testHoldingUpdateEventWithInEligibleItemButAlreadyContributed() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertHoldingToEntity(holdingUpdate), CENTRAL_SERVER_ID);
    holdings.setId(holdingId);
    instanceView.setHoldingsRecords(List.of(holdings));
    item.setHoldingsRecordId(holdings.getId());
    instanceView.setItems(List.of(item, createItem()));
    when(inventoryViewClient.getInstanceById(holdingUpdate.getData().getNewEntity().getInstanceId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(false);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    doNothing().when(recordContributionService).deContributeInstance(any(), any());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(3)).save(any()));
    verify(recordContributionService).deContributeInstance(any(), any());
    verify(recordContributionService, never()).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItems(any(), any(), any());
    verify(recordContributionService, never()).deContributeItem(any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testHoldingUpdateEventWithEligibleInstanceAndItem() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertHoldingToEntity(holdingUpdate), CENTRAL_SERVER_ID);
    holdings.setId(holdingId);
    instanceView.setHoldingsRecords(List.of(holdings));
    var item1 = createItem();
    item1.setHoldingsRecordId(holdings.getId());
    item.setHoldingsRecordId(holdings.getId());
    instanceView.setItems(List.of(item, item1));
    when(inventoryViewClient.getInstanceById(holdingUpdate.getData().getNewEntity().getInstanceId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(UUID.class), any(Instance.class)))
      .thenReturn(true);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    doNothing().when(recordContributionService).contributeInstance(any(), any());
    when(recordContributionService.contributeItems(any(), any(), any())).thenReturn(1);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    /*
     In this test, there are 2 items under a holding so item contribution is called twice and for every
     method call there will be an entry added in DB to track the status of the item.
     Once the items are processed, the status of the holding is also updated.
     */
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(4)).save(any()));
    verify(recordContributionService, never()).deContributeInstance(any(), any());
    verify(recordContributionService, times(2)).contributeInstance(any(), any());
    verify(recordContributionService, times(2)).contributeItems(any(), any(), any());
    verify(recordContributionService, never()).deContributeItem(any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testHoldingUpdateEventWithEligibleInstanceAndInEligibleItem() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertHoldingToEntity(holdingUpdate), CENTRAL_SERVER_ID);
    holdings.setId(holdingId);
    instanceView.setHoldingsRecords(List.of(holdings));
    item.setHoldingsRecordId(holdings.getId());
    instanceView.setItems(List.of(item, createItem()));
    when(inventoryViewClient.getInstanceById(holdingUpdate.getData().getNewEntity().getInstanceId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(validationService.isEligibleForContribution(any(UUID.class), any(Item.class)))
      .thenReturn(false);
    when(validationService.isEligibleForContribution(any(UUID.class), any(Instance.class)))
      .thenReturn(true);
    when(recordContributionService.isContributed(any(UUID.class), any(Instance.class), any(Item.class)))
      .thenReturn(true);
    doNothing().when(recordContributionService).contributeInstance(any(), any());
    doNothing().when(recordContributionService).deContributeItem(any(), any());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(3)).save(any()));
    verify(recordContributionService, never()).deContributeInstance(any(), any());
    verify(recordContributionService).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItems(any(), any(), any());
    verify(recordContributionService).deContributeItem(any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testHoldingDeletionEventWithNonMarcRecord() {
    instance.setSource("Non marc");
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertHoldingToEntity(holdingDelete), UUID.randomUUID());
    when(inventoryViewClient.getInstanceById(holdingDelete.getData().getOldEntity().getInstanceId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(MARC_ERROR_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testHoldingDeletionEventWithNonContributedItem() {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertHoldingToEntity(holdingDelete), UUID.randomUUID());
    holdings.setId(holdingId);
    instanceView.setHoldingsRecords(List.of(holdings));
    item.setHoldingsRecordId(holdings.getId());
    instanceView.setItems(List.of(item, createItem()));
    when(inventoryViewClient.getInstanceById(holdingDelete.getData().getOldEntity().getInstanceId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(recordContributionService.isContributed(any(), any(), any()))
      .thenReturn(false);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(3)).save(any()));
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testHoldingDeletionEventWithContributedItemAndInvalidInstance() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertHoldingToEntity(holdingDelete), UUID.randomUUID());
    holdings.setId(holdingId);
    instanceView.setHoldingsRecords(List.of(holdings));
    item.setHoldingsRecordId(holdings.getId());
    instanceView.setItems(List.of(item, createItem()));
    when(inventoryViewClient.getInstanceById(holdingDelete.getData().getOldEntity().getInstanceId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(recordContributionService.isContributed(any(), any(), any()))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Instance.class)))
      .thenReturn(false);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(3)).save(any()));
    verify(recordContributionService, never()).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItems(any(), any(), any());
    verify(recordContributionService, never()).deContributeItem(any(), any());
    verify(recordContributionService).deContributeInstance(any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testHoldingDeletionEventWithContributedItemAndInstance() throws SocketTimeoutException {
    holdingDelete.setNewEntity(null);
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertHoldingToEntity(holdingDelete), UUID.randomUUID());
    holdings.setId(holdingId);
    var item1 = createItem();
    instanceView.setHoldingsRecords(List.of(holdings));
    item.setHoldingsRecordId(holdings.getId());
    item1.setHoldingsRecordId(holdings.getId());
    instanceView.setItems(List.of(item, item1));
    when(inventoryViewClient.getInstanceById(holdingDelete.getData().getOldEntity().getInstanceId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(recordContributionService.isContributed(any(), any(), any()))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Instance.class)))
      .thenReturn(true);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    /*
     In this test, there are 2 items under a holding so item contribution is called twice and for every
     method call there will be an entry added in DB to track the status of the item.
     Once the items are processed, the status of the holding is also updated.
     */
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(4)).save(any()));
    verify(recordContributionService, never()).deContributeInstance(any(), any());
    verify(recordContributionService, times(2)).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItems(any(), any(), any());
    verify(recordContributionService, times(2)).deContributeItem(any(), any());
    verify(recordContributionService, never()).deContributeInstance(any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testHoldingEventWithUnknownEventType() {
    holdingId = UUID.randomUUID();
    holdingUpdate = createHoldingDomainEvent(holdingId, DomainEventType.CREATED);
    holdingUpdate.setOldEntity(null);
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertHoldingToEntity(holdingUpdate), UUID.randomUUID());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(UNKNOWN_TYPE_MESSAGE, ongoingContributionStatus.getError());
  }

  @Test
  void testInstanceCreationEventWithNonMarcRecord() {
    instanceCreate.getData().getNewEntity().setSource("Non Marc");
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertInstanceToEntity(instanceCreate), UUID.randomUUID());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(MARC_ERROR_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testInstanceCreationEventWithInvalidCentralServerId() {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertInstanceToEntity(instanceCreate), UUID.randomUUID());
    when(inventoryViewClient.getInstanceById(instanceCreate.getData().getNewEntity().getId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(INVALID_CENTRAL_SERVER_ID, ongoingContributionStatus.getError());
  }

  @Test
  void testInstanceCreationEventWithInEligibleAndNonContributedInstance() {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertInstanceToEntity(instanceCreate), CENTRAL_SERVER_ID);
    when(inventoryViewClient.getInstanceById(instanceCreate.getData().getNewEntity().getId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(recordContributionService.isContributed(any(), any()))
      .thenReturn(false);
    when(validationService.isEligibleForContribution(any(), any(Instance.class)))
      .thenReturn(false);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(SKIPPING_INELIGIBLE_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testInstanceCreationEventWithEligibleInstanceAndNonContributedInstance() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertInstanceToEntity(instanceCreate), CENTRAL_SERVER_ID);
    instanceView.setItems(List.of(createItem()));
    when(inventoryViewClient.getInstanceById(instanceCreate.getData().getNewEntity().getId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(recordContributionService.isContributed(any(), any()))
      .thenReturn(false);
    when(validationService.isEligibleForContribution(any(), any(Instance.class)))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class)))
      .thenReturn(true);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService).contributeInstance(any(), any());
    verify(recordContributionService).contributeItemsWithoutRetry(any(), any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testInstanceCreationEventWithEligibleInstanceAndNonContributedInstanceWithoutItems() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertInstanceToEntity(instanceCreate), CENTRAL_SERVER_ID);
    instanceView.setItems(List.of(createItem()));
    when(inventoryViewClient.getInstanceById(instanceCreate.getData().getNewEntity().getId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(recordContributionService.isContributed(any(), any()))
      .thenReturn(false);
    when(validationService.isEligibleForContribution(any(), any(Instance.class)))
      .thenReturn(true);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItemsWithoutRetry(any(), any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testInstanceCreationEventWithEligibleInstanceAndContributedInstance() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertInstanceToEntity(instanceCreate), CENTRAL_SERVER_ID);
    instanceView.setItems(List.of(createItem()));
    when(inventoryViewClient.getInstanceById(instanceCreate.getData().getNewEntity().getId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(recordContributionService.isContributed(any(), any()))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Instance.class)))
      .thenReturn(true);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItemsWithoutRetry(any(), any(), any());
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testInstanceUpdateEventWithInEligibleInstanceAndContributedInstance() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertInstanceToEntity(instanceUpdate), CENTRAL_SERVER_ID);
    instanceView.setItems(List.of(createItem()));
    when(inventoryViewClient.getInstanceById(instanceCreate.getData().getNewEntity().getId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(recordContributionService.isContributed(any(), any()))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Instance.class)))
      .thenReturn(false);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService).deContributeInstance(any(), any());
    verify(recordContributionService, never()).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItemsWithoutRetry(any(), any(), any());
    assertEquals(DE_CONTRIBUTED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testInstanceUpdateEventWithSocketException() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertInstanceToEntity(instanceUpdate), CENTRAL_SERVER_ID);
    instanceView.setItems(List.of(createItem()));
    when(inventoryViewClient.getInstanceById(instanceCreate.getData().getNewEntity().getId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(recordContributionService.isContributed(any(), any()))
      .thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Instance.class)))
      .thenReturn(true);
    doThrow(SocketTimeoutException.class).when(recordContributionService)
      .contributeInstance(any(), any());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService, never()).contributeItemsWithoutRetry(any(), any(), any());
    assertEquals(RETRY, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testInstanceDeleteEventWithContributedInstance() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertInstanceToEntity(instanceUpdate), CENTRAL_SERVER_ID);
    instanceView.setItems(List.of(createItem()));
    when(inventoryViewClient.getInstanceById(instanceCreate.getData().getNewEntity().getId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(recordContributionService.isContributed(any(), any()))
      .thenReturn(true);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService).deContributeInstance(any(), any());
    verify(recordContributionService, never()).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItemsWithoutRetry(any(), any(), any());
    assertEquals(DE_CONTRIBUTED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testInstanceDeleteEventWithSocketException() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertInstanceToEntity(instanceUpdate), CENTRAL_SERVER_ID);
    instanceView.setItems(List.of(createItem()));
    when(inventoryViewClient.getInstanceById(instanceCreate.getData().getNewEntity().getId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(recordContributionService.isContributed(any(), any()))
      .thenReturn(true);
    doThrow(SocketTimeoutException.class).when(recordContributionService)
      .deContributeInstance(any(), any());
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService, never()).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItemsWithoutRetry(any(), any(), any());
    assertEquals(RETRY, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testInstanceDeleteEventWithNonContributedInstance() throws SocketTimeoutException {
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertInstanceToEntity(instanceUpdate), CENTRAL_SERVER_ID);
    instanceView.setItems(List.of(createItem()));
    when(inventoryViewClient.getInstanceById(instanceCreate.getData().getNewEntity().getId()))
      .thenReturn(ResultList.of(1, List.of(instanceView)));
    when(recordContributionService.isContributed(any(), any()))
      .thenReturn(false);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService, never()).deContributeInstance(any(), any());
    verify(recordContributionService, never()).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItemsWithoutRetry(any(), any(), any());
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(SKIPPING_INELIGIBLE_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testInstanceInvalidEvent() throws SocketTimeoutException {
    instanceUpdate.setType(DomainEventType.ALL_DELETED);
    var ongoingContributionStatus = saveOngoingContributionStatus(ongoingContributionStatusMapper
      .convertInstanceToEntity(instanceUpdate), CENTRAL_SERVER_ID);
    eventProcessor.processOngoingContribution(ongoingContributionStatus);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(ongoingContributionStatusRepository, times(2)).save(any()));
    verify(recordContributionService, never()).deContributeInstance(any(), any());
    verify(recordContributionService, never()).contributeInstance(any(), any());
    verify(recordContributionService, never()).contributeItemsWithoutRetry(any(), any(), any());
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(UNKNOWN_TYPE_MESSAGE, ongoingContributionStatus.getError());
  }

  private DomainEvent<Item> createItemDomainEvent(UUID itemId, DomainEventType eventType) {
    var oldItem = createItem().id(itemId);
    var newItem = createItem().id(itemId);

    return DomainEvent.<org.folio.innreach.dto.Item>builder()
      .tenant(TENANT)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(newItem, oldItem))
      .build();
  }

  private DomainEvent<Holding> createHoldingDomainEvent(UUID holdingId, DomainEventType eventType) {
    var oldHolding = createHolding().id(holdingId);
    var newHolding = createHolding().id(holdingId);

    return DomainEvent.<org.folio.innreach.dto.Holding>builder()
      .tenant(TENANT)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(oldHolding, newHolding))
      .build();
  }

  private DomainEvent<Instance> createInstanceDomainEvent(UUID instanceId, DomainEventType eventType) {
    var oldInstance = createInstance().id(instanceId);
    var newInstance = createInstance().id(instanceId);

    return DomainEvent.<Instance>builder()
      .tenant(TENANT)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(oldInstance, newInstance))
      .build();
  }

  private OngoingContributionStatus saveOngoingContributionStatus(OngoingContributionStatus ongoingContributionStatus, UUID centralServerId) {
    ongoingContributionStatus.setTenant(TENANT);
    ongoingContributionStatus.setCentralServerId(centralServerId);
    return ongoingContributionStatusRepository.save(ongoingContributionStatus);
  }

}
