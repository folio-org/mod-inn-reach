package org.folio.innreach.batch.contribution;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

import static org.folio.innreach.batch.contribution.ContributionJobContextManager.getContributionJobContext;

@Log4j2
@Setter
@RequiredArgsConstructor
public class InitialContributionJobConsumerContainer {

  public static final long POLL_TIMEOUT = 300000L;
  public static Map<String, ConcurrentMessageListenerContainer<String, InstanceIterationEvent>> consumersMap =
    new HashMap<>();

  private final Map<String,Object> consumerProperties;
  private final String topic;
  private final Deserializer<String> keyDeserializer;
  private final Deserializer<InstanceIterationEvent> valueDeserializer;

  private final Long interval;

  private final Long maxAttempts;

  private static final int CONCURRENCY = 2;

  private final ContributionExceptionListener contributionExceptionListener;

  private final ContributionJobRunner contributionJobRunner;

  public DefaultErrorHandler errorHandler() {
    log.info("Initializing initial contribution container error handler: interval: {}, maxAttempts: {}", interval, maxAttempts);
    BackOff fixedBackOff = new FixedBackOff(interval, maxAttempts);
    DefaultErrorHandler errorHandler = new DefaultErrorHandler((consumerRecord, exception) -> {
      // logic to execute when all the retry attempts are exhausted
      ConsumerRecord<String, InstanceIterationEvent> record = (ConsumerRecord<String, InstanceIterationEvent>) consumerRecord;
      contributionExceptionListener.logWriteError(exception, record.value().getInstanceId());
      contributionJobRunner.cancelContributionIfRetryExhausted(getContributionJobContext().getCentralServerId());
      contributionJobRunner.stopContribution(getContributionJobContext().getTenantId());
      log.info("Stopping consumer topic: {} after retry exhaustion", consumerRecord.topic());
      stopConsumer(consumerRecord.topic());
    }, fixedBackOff);
    errorHandler.addRetryableExceptions(ServiceSuspendedException.class);
    errorHandler.addRetryableExceptions(SocketTimeOutExceptionWrapper.class);
    errorHandler.addRetryableExceptions(FeignException.class);
    errorHandler.addRetryableExceptions(InnReachConnectionException.class);
    return errorHandler;
  }

  public void tryStartOrCreateConsumer(Object messageListner) {
    log.info("Init contribution container: start called");
    ConcurrentMessageListenerContainer<String, InstanceIterationEvent> container = consumersMap.get(topic);
    if (container != null) {
      log.info("Init contribution container: container is not null");
      if (!container.isRunning()) {
        log.info("Init contribution container: consumer already created for topic {}, starting consumer!!", topic);
        container.start();
        log.info("Init contribution container: consumer for topic {} started!!!!", topic);
      }
      return;
    }

    log.info("container is null");

    ContainerProperties containerProps = new ContainerProperties(topic);

    containerProps.setPollTimeout(POLL_TIMEOUT);

    containerProps.setAckMode(ContainerProperties.AckMode.RECORD);

    ConsumerFactory<String, InstanceIterationEvent> factory = new DefaultKafkaConsumerFactory<>(consumerProperties,
      keyDeserializer,valueDeserializer);
    log.info("Init contribution container: DefaultKafkaConsumerFactory created");
    container = new ConcurrentMessageListenerContainer<>(factory, containerProps);


    container.setupMessageListener(messageListner);
    container.setCommonErrorHandler(errorHandler());

    container.setConcurrency(CONCURRENCY);

    container.start();

    consumersMap.put(topic, container);


    log.info("Init contribution container: created and started kafka consumer for topic {}", topic);
  }

  public static void stopConsumer(final String topic) {
    log.info("Init contribution container: stopping consumer for topic {}", topic);
    ConcurrentMessageListenerContainer<String, InstanceIterationEvent> container = consumersMap.get(topic);
    if(container!=null)
    {
      container.stop();
    }
    else
      log.warn("Init contribution container: container is already null and stopped");

    log.info("Init contribution container: consumer stopped for topic {}", topic);
  }
}
