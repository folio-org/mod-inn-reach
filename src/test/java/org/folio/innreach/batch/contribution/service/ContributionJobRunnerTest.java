package org.folio.innreach.batch.contribution.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createContributionJobContext;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.TestUtil.createNoRetryTemplate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.batch.KafkaItemReader;
import org.folio.innreach.batch.contribution.IterationEventReaderFactory;
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.batch.contribution.listener.ContributionJobStatsListener;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.config.props.FolioEnvironment;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.Instance;

@ExtendWith(MockitoExtension.class)
class ContributionJobRunnerTest {

  @Qualifier("instanceExceptionListener")
  @Mock
  private ContributionExceptionListener instanceExceptionListener;
  @Qualifier("itemExceptionListener")
  @Mock
  private ContributionExceptionListener itemExceptionListener;
  @Mock
  private ContributionJobStatsListener statsListener;
  @Mock
  private InstanceLoader instanceLoader;
  @Mock
  private InstanceContributor instanceContributor;
  @Mock
  private ItemContributor itemContributor;
  @Mock
  private ContributionJobProperties jobProperties;
  @Mock
  private KafkaProperties kafkaProperties;
  @Mock
  private FolioEnvironment folioEnv;
  @Mock
  private ContributionService contributionService;
  @Mock
  private IterationEventReaderFactory factory;
  @Mock
  private KafkaItemReader<String, InstanceIterationEvent> reader;
  @Spy
  private RetryTemplate retryTemplate = createNoRetryTemplate();

  @InjectMocks
  private ContributionJobRunner jobRunner;

  @Test
  void shouldRunJob() throws Exception {
    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read())
      .thenReturn(new InstanceIterationEvent())
      .thenReturn(null);
    when(instanceLoader.load(any())).thenReturn(createInstance());
    when(jobProperties.getChunkSize()).thenReturn(100);

    jobRunner.run(createContributionJobContext());

    verify(reader, times(2)).read();
    verify(instanceContributor).contributeInstance(any());
    verify(itemContributor).contributeItems(any(), any());
  }

  @Test
  void shouldRunJobWithNoInstanceItems() throws Exception {
    Instance instance = createInstance();
    instance.setItems(null);

    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read())
      .thenReturn(new InstanceIterationEvent())
      .thenReturn(null);
    when(instanceLoader.load(any())).thenReturn(instance);

    jobRunner.run(createContributionJobContext());

    verify(reader, times(2)).read();
    verify(instanceContributor).contributeInstance(any());
    verifyNoInteractions(itemContributor);
  }

  @Test
  void shouldRunJobWithNoInstances() {
    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read()).thenReturn(null);

    jobRunner.run(createContributionJobContext());

    verify(reader).read();
    verifyNoInteractions(instanceContributor);
    verifyNoInteractions(itemContributor);
  }

  @Test
  void shouldCancelJob() {
    jobRunner.cancelJobs();

    verify(contributionService).cancelAll();
  }

}
