package org.folio.innreach.batch.contribution.service;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createContributionJobContext;
import static org.folio.innreach.fixture.ContributionFixture.createItem;
import static org.folio.innreach.fixture.TestUtil.createNoRetryTemplate;

import java.util.ArrayList;
import java.util.List;

import lombok.SneakyThrows;
import org.folio.innreach.external.dto.InnReachResponse;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.InnReachRetryException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.ContributionJobContextManager;
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.domain.service.RecordTransformationService;
import org.folio.innreach.domain.service.impl.RecordContributionServiceImpl;
import org.folio.innreach.external.dto.BibItem;
import org.folio.innreach.external.service.InnReachContributionService;
import org.springframework.core.retry.RetryTemplate;

@ExtendWith(MockitoExtension.class)
class ItemContributorTest {

  private static final ContributionJobContext JOB_CONTEXT = createContributionJobContext();

  @Spy
  private RetryTemplate retryTemplate = createNoRetryTemplate();
  @Mock
  private InnReachContributionService irContributionService;
  @Mock
  private RecordTransformationService recordTransformationService;
  @Mock
  private ContributionExceptionListener exceptionListener;

  @Mock
  private InnReachResponse response;

  @InjectMocks
  private RecordContributionServiceImpl service;

  @BeforeEach
  public void init() {
    ContributionJobContextManager.beginContributionJobContext(JOB_CONTEXT);
  }

  @AfterEach
  public void clear() {
    ContributionJobContextManager.endContributionJobContext();
  }

  @SneakyThrows
  @Test
  void shouldContributeItems() {
    when(irContributionService.contributeBibItems(any(), any(), any())).thenReturn(response);
    when(recordTransformationService.getBibItems(any(), any(), any())).thenReturn(List.of(new BibItem()));
    when(response.getErrors()).thenReturn(new ArrayList<>());
    when(response.isOk()).thenReturn(true);

    service.contributeItems(JOB_CONTEXT.getCentralServerId(), "test", of(createItem()));

    verify(irContributionService).contributeBibItems(eq(JOB_CONTEXT.getCentralServerId()), any(), any());

    when(response.getErrors()).thenReturn(null);
    service.contributeItems(JOB_CONTEXT.getCentralServerId(), "test", of(createItem()));
    verify(irContributionService,times(2)).contributeBibItems(eq(JOB_CONTEXT.getCentralServerId()), any(), any());


    when(response.getErrors()).thenReturn(List.of());
    service.contributeItems(JOB_CONTEXT.getCentralServerId(), "test", of(createItem()));
    verify(irContributionService,times(3)).contributeBibItems(eq(JOB_CONTEXT.getCentralServerId()), any(), any());

    response = null;
    service.contributeItems(JOB_CONTEXT.getCentralServerId(), "test", of(createItem()));
    verify(irContributionService,times(4)).contributeBibItems(eq(JOB_CONTEXT.getCentralServerId()), any(), any());
  }

  @Test
  void testContributeItemsWithoutRetry() {
    when(irContributionService.contributeBibItems(any(), any(), any())).thenReturn(response);
    when(recordTransformationService.getBibItems(any(), any(), any())).thenReturn(List.of(new BibItem()));
    when(response.getErrors()).thenReturn(new ArrayList<>());
    when(response.isOk()).thenReturn(true);

    service.contributeItemsWithoutRetry(JOB_CONTEXT.getCentralServerId(), "test", of(createItem()));

    verify(irContributionService).contributeBibItems(eq(JOB_CONTEXT.getCentralServerId()), any(), any());

  }

  @SneakyThrows
  @Test
  void shouldContributeItems_throwException() {
    InnReachResponse.Error errorResp1 = InnReachResponse.Error.builder().reason("Contribution to d2irm is not currently suspended").build();
    InnReachResponse.Error errorResp2 = InnReachResponse.Error.builder().reason("Contribution to d2irm is currently suspended").build();
    InnReachResponse.Error errorResp3 = InnReachResponse.Error.builder().reason("Central Error").central("d2irm")
                        .messages(of("connections allowed from this server")).build();

    when(irContributionService.contributeBibItems(any(), any(), any())).thenReturn(response);
    when(recordTransformationService.getBibItems(any(), any(), any())).thenReturn(List.of(new BibItem(),new BibItem()));
    when(response.getErrors()).thenReturn(of(errorResp1));
    when(response.isOk()).thenReturn(true);
    var item = createItem();
    var centralServerId = JOB_CONTEXT.getCentralServerId();

    service.contributeItems(centralServerId, "test", of(createItem()));
    verify(irContributionService).contributeBibItems(eq(centralServerId), any(), any());

    when(response.getErrors()).thenReturn(of(errorResp2));
    assertThatThrownBy(() -> service.contributeItems(centralServerId, "test", of(item)))
      .isInstanceOf(InnReachRetryException.class)
      .hasMessageContaining("Contributing items for bib test has failed")
      .cause().isInstanceOf(ServiceSuspendedException.class)
      .hasMessageContaining("is currently suspended");

    when(response.getErrors()).thenReturn(of(errorResp3));
    assertThatThrownBy(() -> service.contributeItems(centralServerId, "test", of(item)))
      .isInstanceOf(InnReachRetryException.class)
      .hasMessageContaining("Contributing items for bib test has failed")
      .cause().isInstanceOf(InnReachConnectionException.class)
      .hasMessageContaining("Only 5 connections allowed from this server");


    when(recordTransformationService.getBibItems(any(), any(), any())).thenReturn(List.of());
    assertThrows(IllegalArgumentException.class,
      () -> service.contributeItems(centralServerId, "test", of(item)),
       "Failed to convert items for contribution");
  }

  @Test
  void testIsContributed() {
    InnReachResponse response = InnReachResponse.builder().errors(new ArrayList<>()).status("ok").build();
    when(irContributionService.lookUpBibItem(any(), any(), any())).thenReturn(response);

    boolean resp = service.isContributed(JOB_CONTEXT.getCentralServerId(), createInstance(),createItem());
    assertTrue(resp);

    response.setStatus("nok");
    resp = service.isContributed(JOB_CONTEXT.getCentralServerId(), createInstance(),createItem());
    assertFalse(resp);
  }

  @SneakyThrows
  @Test
  void testMoveItem() {
    when(irContributionService.deContributeBibItem(any(), any())).thenReturn(response);
    when(irContributionService.contributeBibItems(any(), any(), any())).thenReturn(response);
    when(recordTransformationService.getBibItems(any(), any(), any())).thenReturn(List.of(new BibItem()));
    when(response.getErrors()).thenReturn(new ArrayList<>());
    when(response.isOk()).thenReturn(true);

    service.moveItem(JOB_CONTEXT.getCentralServerId(), "test", createItem());

    verify(irContributionService).deContributeBibItem(any(),any());
    verify(irContributionService).contributeBibItems(eq(JOB_CONTEXT.getCentralServerId()), any(), any());
  }
}
