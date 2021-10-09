package org.folio.innreach.batch.contribution;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.config.props.FolioEnvironment;

@ExtendWith(MockitoExtension.class)
class IterationEventReaderFactoryTest {

  @Mock
  private KafkaProperties kafkaProperties;
  @Mock
  private FolioEnvironment folioEnv;
  @Mock
  private ContributionJobProperties jobProperties;

  @InjectMocks
  private IterationEventReaderFactory factory;

  @Test
  void createReader() {
    var reader = factory.createReader("test");

    assertNotNull(reader);

    verify(kafkaProperties).buildConsumerProperties();
    verify(folioEnv).getEnvironment();
    verify(jobProperties).getReaderTopic();
    verify(jobProperties).getReaderPollTimeoutSec();
  }

}
