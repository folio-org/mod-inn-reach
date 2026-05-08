package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.function.Consumer;

import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.InnReachTimeOutException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BatchDomainEventProcessorTest {

  private static final String TENANT_ID = "test-tenant";

  @Mock
  private TenantScopedExecutionService executionService;

  @Mock
  private Consumer<DomainEvent<String>> recordProcessor;

  private BatchDomainEventProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new BatchDomainEventProcessor(executionService);
    setInnReachTenants(TENANT_ID);
  }

  @Test
  void shouldProcessAllEventsForKnownTenant() {
    var event1 = createEvent(TENANT_ID);
    var event2 = createEvent(TENANT_ID, DomainEventType.CREATED);
    mockExecutionService();

    processor.process(List.of(event1, event2), recordProcessor);

    verify(recordProcessor).accept(event1);
    verify(recordProcessor).accept(event2);
  }

  @Test
  void shouldIgnoreEventsForUnknownTenant() {
    var event = createEvent("unknown-tenant");

    processor.process(List.of(event), recordProcessor);

    verifyNoInteractions(executionService);
    verifyNoInteractions(recordProcessor);
  }

  @Test
  void shouldRethrowServiceSuspendedException() {
    var event = createEvent(TENANT_ID);
    var batch = List.of(event);
    mockExecutionServiceThrowing(new ServiceSuspendedException("suspended"));

    assertThrows(ServiceSuspendedException.class,
      () -> processor.process(batch, recordProcessor));
  }

  @Test
  void shouldRethrowHttpClientErrorException() {
    var event = createEvent(TENANT_ID);
    var batch = List.of(event);
    mockExecutionServiceThrowing(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad", null, null, null));

    assertThrows(HttpClientErrorException.class,
      () -> processor.process(batch, recordProcessor));
  }

  @Test
  void shouldRethrowHttpServerErrorException() {
    var event = createEvent(TENANT_ID);
    var batch = List.of(event);
    mockExecutionServiceThrowing(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "error", null, null, null));

    assertThrows(HttpServerErrorException.class,
      () -> processor.process(batch, recordProcessor));
  }

  @Test
  void shouldRethrowInnReachConnectionException() {
    var event = createEvent(TENANT_ID);
    var batch = List.of(event);
    mockExecutionServiceThrowing(new InnReachConnectionException("connection error"));

    assertThrows(InnReachConnectionException.class,
      () -> processor.process(batch, recordProcessor));
  }

  @Test
  void shouldRethrowInnReachTimeOutException() {
    var event = createEvent(TENANT_ID);
    var batch = List.of(event);
    mockExecutionServiceThrowing(new InnReachTimeOutException("timeout"));

    assertThrows(InnReachTimeOutException.class,
      () -> processor.process(batch, recordProcessor));
  }

  @Test
  void shouldSwallowListenerExecutionFailedException() {
    var event = createEvent(TENANT_ID);
    var batch = List.of(event);
    mockExecutionServiceThrowing(new ListenerExecutionFailedException("not permitted", new RuntimeException()));

    processor.process(batch, recordProcessor);
  }

  @Test
  void shouldContinueProcessingOtherEventsWhenOneFailsWithGenericException() {
    var event1 = createEvent(TENANT_ID);
    var event2 = createEvent(TENANT_ID, DomainEventType.CREATED);
    mockExecutionService();
    org.mockito.Mockito.doThrow(new RuntimeException("generic error")).doNothing().when(recordProcessor).accept(any());

    processor.process(List.of(event1, event2), recordProcessor);

    verify(recordProcessor).accept(event1);
    verify(recordProcessor).accept(event2);
  }

  @Test
  void shouldRethrowServiceSuspendedExceptionFromRecordProcessor() {
    var event = createEvent(TENANT_ID);
    var batch = List.of(event);
    mockExecutionService();
    org.mockito.Mockito.doThrow(new ServiceSuspendedException("suspended")).when(recordProcessor).accept(any());

    assertThrows(ServiceSuspendedException.class,
      () -> processor.process(batch, recordProcessor));
  }

  @Test
  void shouldProcessEventsGroupedByTenant() {
    var tenant2 = "tenant2";
    setInnReachTenants(TENANT_ID + "," + tenant2);
    var event1 = createEvent(TENANT_ID);
    var event2 = createEvent(tenant2);
    mockExecutionService();

    processor.process(List.of(event1, event2), recordProcessor);

    verify(executionService).runTenantScoped(eq(TENANT_ID), any());
    verify(executionService).runTenantScoped(eq(tenant2), any());
  }

  @Test
  void shouldProcessEmptyBatchWithoutError() {
    processor.process(List.of(), recordProcessor);

    verifyNoInteractions(executionService);
    verifyNoInteractions(recordProcessor);
  }

  private DomainEvent<String> createEvent(String tenant) {
    return createEvent(tenant, DomainEventType.UPDATED);
  }

  private DomainEvent<String> createEvent(String tenant, DomainEventType type) {
    return DomainEvent.<String>builder()
      .tenant(tenant)
      .type(type)
      .build();
  }

  private void mockExecutionService() {
    doAnswer(invocation -> {
      Runnable job = invocation.getArgument(1);
      job.run();
      return null;
    }).when(executionService).runTenantScoped(any(), any());
  }

  private void mockExecutionServiceThrowing(RuntimeException ex) {
    doAnswer(invocation -> { throw ex; })
      .when(executionService).runTenantScoped(any(), any());
  }

  private void setInnReachTenants(String tenants) {
    try {
      var field = BatchDomainEventProcessor.class.getDeclaredField("innReachTenants");
      field.setAccessible(true);
      field.set(processor, tenants);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

