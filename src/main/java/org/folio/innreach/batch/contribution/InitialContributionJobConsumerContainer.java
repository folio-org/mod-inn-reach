package org.folio.innreach.batch.contribution;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;

@Log4j2
@Setter
@RequiredArgsConstructor
public class InitialContributionJobConsumerContainer {

  public static final long POLL_TIMEOUT = 4200000L;
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

  private final ContributionJobContext context;

  public DefaultErrorHandler errorHandler() {
    log.info("interval :{} , maxAttempts:{}",interval,maxAttempts);
    BackOff fixedBackOff = new FixedBackOff(interval, maxAttempts);
    DefaultErrorHandler errorHandler = new DefaultErrorHandler((consumerRecord, exception) -> {
      // logic to execute when all the retry attempts are exhausted
      ConsumerRecord<String, InstanceIterationEvent> record = (ConsumerRecord<String, InstanceIterationEvent>) consumerRecord;
      contributionExceptionListener.logWriteError(exception, record.value().getInstanceId());
      contributionJobRunner.stopContribution(context.getTenantId());
      contributionJobRunner.cancelContributionIfRetryExhausted(context.getCentralServerId());
      log.info("Stopping consumer topic: {} after retry exhaustion", consumerRecord.topic());
      stopConsumer(consumerRecord.topic());
    }, fixedBackOff);
    errorHandler.addRetryableExceptions(ServiceSuspendedException.class);
    errorHandler.addRetryableExceptions(SocketTimeoutException.class);
    return errorHandler;
  }

  public void tryStartOrCreateConsumer(Object messageListner) {
    log.info("startOrCreateConsumer----");
    ConcurrentMessageListenerContainer<String, InstanceIterationEvent> container = consumersMap.get(topic);
    if (container != null) {
      log.info("container is not null");
      if (!container.isRunning()) {
        log.info("Consumer already created for topic {}, starting consumer!!", topic);
        container.start();
        log.info("Consumer for topic {} started!!!!", topic);
      }
      return;
    }

    log.info("container is null");

    ContainerProperties containerProps = new ContainerProperties(topic);

    //TODO decide poll timeout
    containerProps.setPollTimeout(POLL_TIMEOUT);
    Boolean enableAutoCommit = (Boolean) consumerProperties.get(ENABLE_AUTO_COMMIT_CONFIG);

    containerProps.setAckMode(ContainerProperties.AckMode.RECORD);

    ConsumerFactory<String, InstanceIterationEvent> factory = new DefaultKafkaConsumerFactory<>(consumerProperties,
      keyDeserializer,valueDeserializer);
    log.info("after DefaultKafkaConsumerFactory----");
    container = new ConcurrentMessageListenerContainer<>(factory, containerProps);


    container.setupMessageListener(messageListner);
    container.setCommonErrorHandler(errorHandler());

    container.setConcurrency(CONCURRENCY);

    container.start();

    consumersMap.put(topic, container);


    log.info("created and started kafka consumer for topic {}", topic);
  }

  public static void stopConsumer(final String topic) {
    log.info("Stopping consumer for topic {}", topic);
    ConcurrentMessageListenerContainer<String, InstanceIterationEvent> container = consumersMap.get(topic);
    if(container!=null)
    {
      container.stop();
    }
    else
      log.warn("container is already null and stopped");

    log.info("Consumer stopped for topic {}", topic);
  }


}
