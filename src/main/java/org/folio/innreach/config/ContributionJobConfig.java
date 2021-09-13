package org.folio.innreach.config;

import static java.util.List.of;

import java.time.Duration;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.apache.kafka.common.TopicPartition;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.retry.backoff.FixedBackOffPolicy;

import org.folio.innreach.batch.KafkaItemReader;
import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.listener.ContributionJobExceptionListener;
import org.folio.innreach.batch.contribution.listener.ContributionJobExecutionListener;
import org.folio.innreach.batch.contribution.listener.ContributionJobStatsListener;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.dto.Instance;

@Configuration
@EnableBatchProcessing
@EnableConfigurationProperties(ContributionJobProperties.class)
@ConditionalOnProperty(value = "batch.enabled", havingValue = "true", matchIfMissing = true)
public class ContributionJobConfig {

  public static final String CONTRIBUTION_JOB_NAME = "contributionJob";
  public static final String CONTRIBUTION_JOB_LAUNCHER_NAME = "contributionJobLauncher";

  public static final BiConsumer<String, InstanceIterationEvent> CONSUMER_REC_PROCESSOR =
    (String key, InstanceIterationEvent value) -> value.setInstanceId(UUID.fromString(key));

  @JobScope
  @Bean
  public ContributionJobContext jobContext() {
    return new ContributionJobContext();
  }

  @Bean
  public KafkaItemReader<String, InstanceIterationEvent> kafkaReader(KafkaProperties kafkaProperties,
                                                                     ContributionJobProperties jobProperties) {

    Properties props = new Properties();
    props.putAll(kafkaProperties.buildConsumerProperties());

    String topic = jobProperties.getReaderTopic();

    var reader = new KafkaItemReader<String, InstanceIterationEvent>(props, of(new TopicPartition(topic, 0)));
    reader.setName("contributionKafkaReader");
    reader.setPollTimeout(Duration.ofSeconds(jobProperties.getReaderPollTimeoutSec()));
    reader.setPartitionOffsets(new HashMap<>());
    reader.setRecordProcessor(CONSUMER_REC_PROCESSOR);
    return reader;
  }

  @Bean
  public Step instanceContributionStep(KafkaProperties kafkaProperties,
                                       ContributionJobProperties jobProperties,
                                       StepBuilderFactory stepBuilderFactory,
                                       ItemProcessor<InstanceIterationEvent, Instance> instanceLoader,
                                       ItemWriter<Instance> instanceContributor,
                                       ContributionJobExceptionListener failureListener,
                                       ContributionJobStatsListener countListener) {

    var backOffPolicy = new FixedBackOffPolicy();
    backOffPolicy.setBackOffPeriod(jobProperties.getRetryIntervalMs());

    return stepBuilderFactory.get("contributionStep")
      .<InstanceIterationEvent, Instance>chunk(jobProperties.getChunkSize())
      .processor(instanceLoader)
      .writer(instanceContributor)
      .reader(kafkaReader(kafkaProperties, jobProperties))
      .faultTolerant()
      .backOffPolicy(backOffPolicy)
      .retry(Exception.class)
      .retryLimit(jobProperties.getRetryAttempts())
      .skip(Exception.class)
      .skipLimit(Integer.MAX_VALUE)
      .listener((ItemReadListener) failureListener)
      .listener((ItemProcessListener) failureListener)
      .listener((ItemWriteListener) failureListener)
      .listener(countListener)
      .build();
  }

  @Bean(name = CONTRIBUTION_JOB_NAME)
  public Job job(KafkaProperties kafkaProperties,
                 ContributionJobProperties jobProperties,
                 JobBuilderFactory jobBuilderFactory,
                 StepBuilderFactory stepBuilderFactory,
                 ContributionJobExecutionListener jobExecutionListener,
                 ItemProcessor<InstanceIterationEvent, Instance> instanceLoader,
                 ItemWriter<Instance> instanceContributor,
                 ContributionJobExceptionListener failureListener,
                 ContributionJobStatsListener countListener) {

    return jobBuilderFactory.get(CONTRIBUTION_JOB_NAME)
      .incrementer(new RunIdIncrementer())
      .start(
        instanceContributionStep(
          kafkaProperties, jobProperties, stepBuilderFactory, instanceLoader,
          instanceContributor, failureListener, countListener))
      .listener(jobExecutionListener)
      .listener(jobContext())
      .build();
  }

  @Bean(CONTRIBUTION_JOB_LAUNCHER_NAME)
  public JobLauncher jobLauncher(@Lazy JobRepository jobRepository) throws Exception {
    SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
    jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
    jobLauncher.setJobRepository(jobRepository);
    jobLauncher.afterPropertiesSet();
    return jobLauncher;
  }

  @Bean
  public JobFactory jobFactory(@Lazy Job job) {
    return new ReferenceJobFactory(job);
  }

  @Bean
  public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(@Lazy JobRegistry jobRegistry) {
    JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
    postProcessor.setJobRegistry(jobRegistry);
    return postProcessor;
  }

}
