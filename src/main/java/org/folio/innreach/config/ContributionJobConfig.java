package org.folio.innreach.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
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

@EnableBatchProcessing
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ContributionJobProperties.class)
@ConditionalOnProperty(value = "batch.enabled", havingValue = "true", matchIfMissing = true)
public class ContributionJobConfig {

  public static final String CONTRIBUTION_JOB_NAME = "contributionJob";
  public static final String CONTRIBUTION_JOB_LAUNCHER_NAME = "contributionJobLauncher";
  public static final String CONTRIBUTION_JOB_RUNNER_NAME = "contributionJobRunner";

  private final KafkaProperties kafkaProperties;
  private final ContributionJobProperties jobProperties;
  private final ContributionJobStatsListener countListener;
  private final ContributionJobExecutionListener jobExecutionListener;
  private final ContributionJobExceptionListener failureListener;

  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;

  private final ItemProcessor<InstanceIterationEvent, Instance> instanceLoader;
  private final ItemWriter<Instance> instanceContributor;

  @Bean
  public KafkaItemReader<String, InstanceIterationEvent> kafkaReader() {
    Properties props = new Properties();
    props.putAll(kafkaProperties.buildConsumerProperties());

    String topic = jobProperties.getReaderTopic();

    var reader = new KafkaItemReader<String, InstanceIterationEvent>(props, topic, 0);
    reader.setName("contributionKafkaReader");
    reader.setPollTimeout(Duration.ofSeconds(jobProperties.getReaderPollTimeoutSec()));
    reader.setPartitionOffsets(new HashMap<>());
    reader.peek((String key, InstanceIterationEvent value) -> value.setInstanceId(UUID.fromString(key)));

    return reader;
  }

  @Bean
  public Step instanceContributionStep() {
    var backOffPolicy = new FixedBackOffPolicy();
    backOffPolicy.setBackOffPeriod(jobProperties.getRetryIntervalMs());

    return stepBuilderFactory.get("contributionStep")
      .<InstanceIterationEvent, Instance>chunk(jobProperties.getChunkSize())
      .processor(instanceLoader)
      .writer(instanceContributor)
      .reader(kafkaReader())
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
  public Job job(ContributionJobContext jobContext) {
    return jobBuilderFactory.get(CONTRIBUTION_JOB_NAME)
      .incrementer(new RunIdIncrementer())
      .start(instanceContributionStep())
      .listener(jobExecutionListener)
      .listener(jobContext)
      .build();
  }

  @Bean(CONTRIBUTION_JOB_LAUNCHER_NAME)
  public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
    SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
    jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
    jobLauncher.setJobRepository(jobRepository);
    jobLauncher.afterPropertiesSet();
    return jobLauncher;
  }

  @Bean
  public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
    JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
    postProcessor.setJobRegistry(jobRegistry);
    return postProcessor;
  }

}
