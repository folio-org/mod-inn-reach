package org.folio.innreach.batch.contribution.service;

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
import org.folio.innreach.mapper.OngoingContributionStatusMapper;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.ContributionRepository;
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
import static org.folio.innreach.fixture.ContributionFixture.createHolding;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createItem;
import static org.folio.innreach.util.InnReachConstants.INVALID_CENTRAL_SERVER_ID;
import static org.folio.innreach.util.InnReachConstants.MARC_ERROR_MSG;
import static org.folio.innreach.util.InnReachConstants.SKIPPING_INELIGIBLE_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OngoingContributionEventProcessorTest extends BaseControllerTest {
  private final UUID CENTRAL_SERVER_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);
  private static final String TENANT = "test_tenant";
  @Autowired
  OngoingContributionEventProcessor eventProcessor;
  @Autowired
  ContributionRepository contributionRepository;
  @SpyBean
  OngoingContributionStatusRepository ongoingContributionStatusRepository;
  @Autowired
  CentralServerRepository centralServerRepository;
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
  UUID itemId;
  DomainEvent<Item> itemCreate;
  DomainEvent<Item> itemDelete;
  Holding holdings;
  Instance instance;
  InventoryViewClient.InstanceView instanceView;

  @BeforeEach
  void runBefore() {
    itemId = UUID.randomUUID();
    itemCreate = createItemDomainEvent(itemId, DomainEventType.CREATED);
    itemDelete = createItemDomainEvent(itemId, DomainEventType.DELETED);
    holdings = createHolding();
    instance = createInstance();
    instanceView = new InventoryViewClient.InstanceView();
    instanceView.setInstance(instance);
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
    ongoingContributionStatus = ongoingContributionStatusRepository.findById(ongoingContributionStatus.getId()).get();
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
        verify(ongoingContributionStatusRepository, times(2)).save(any()));    ongoingContributionStatus = ongoingContributionStatusRepository.findById(ongoingContributionStatus.getId()).get();
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
        verify(ongoingContributionStatusRepository, times(2)).save(any()));    ongoingContributionStatus = ongoingContributionStatusRepository.findById(ongoingContributionStatus.getId()).get();
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
        verify(ongoingContributionStatusRepository, times(2)).save(any()));    ongoingContributionStatus = ongoingContributionStatusRepository.findById(ongoingContributionStatus.getId()).get();
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
        verify(ongoingContributionStatusRepository, times(2)).save(any()));    ongoingContributionStatus = ongoingContributionStatusRepository.findById(ongoingContributionStatus.getId()).get();
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
        verify(ongoingContributionStatusRepository, times(2)).save(any()));    ongoingContributionStatus = ongoingContributionStatusRepository.findById(ongoingContributionStatus.getId()).get();
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
        verify(ongoingContributionStatusRepository, times(2)).save(any()));    ongoingContributionStatus = ongoingContributionStatusRepository.findById(ongoingContributionStatus.getId()).get();
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
        verify(ongoingContributionStatusRepository, times(2)).save(any()));    ongoingContributionStatus = ongoingContributionStatusRepository.findById(ongoingContributionStatus.getId()).get();
    assertEquals(FAILED, ongoingContributionStatus.getStatus());
    assertEquals(SKIPPING_INELIGIBLE_MSG, ongoingContributionStatus.getError());
  }

  @Test
  void testItemDeletionEventWithContributedItemAndInvalidInstance() {
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
        verify(ongoingContributionStatusRepository, times(2)).save(any()));    ongoingContributionStatus = ongoingContributionStatusRepository.findById(ongoingContributionStatus.getId()).get();
    assertEquals(DE_CONTRIBUTED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
  }

  @Test
  void testItemDeletionEventWithContributedItemAndInstance() {
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
        verify(ongoingContributionStatusRepository, times(2)).save(any()));    ongoingContributionStatus = ongoingContributionStatusRepository.findById(ongoingContributionStatus.getId()).get();
    assertEquals(PROCESSED, ongoingContributionStatus.getStatus());
    assertNull(ongoingContributionStatus.getError());
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

  private OngoingContributionStatus saveOngoingContributionStatus(OngoingContributionStatus ongoingContributionStatus, UUID centralServerId) {
    ongoingContributionStatus.setTenant(TENANT);
    ongoingContributionStatus.setCentralServerId(centralServerId);
    return ongoingContributionStatusRepository.save(ongoingContributionStatus);
  }

}
