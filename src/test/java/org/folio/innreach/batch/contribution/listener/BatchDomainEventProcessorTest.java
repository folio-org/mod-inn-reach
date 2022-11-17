package org.folio.innreach.batch.contribution.listener;

import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.listener.base.BaseKafkaApiTest;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


public class BatchDomainEventProcessorTest extends BaseKafkaApiTest {
  @MockBean
  private TenantScopedExecutionService executionService;

  @SpyBean
  private BatchDomainEventProcessor eventProcessor;

  private static final String TEST_TENANT_ID = "testing";

  @Test
  void shouldNotProcessEventIfModuleDisabled() {
    doThrow(new RuntimeException()).when(executionService).runTenantScoped(eq(TEST_TENANT_ID), any());

    assertThrows(RuntimeException.class, ()-> eventProcessor.process(any(), any()));
  }
}
