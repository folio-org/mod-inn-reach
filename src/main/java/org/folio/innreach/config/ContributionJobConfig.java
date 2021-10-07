package org.folio.innreach.config;

import static java.util.List.of;

import static org.folio.innreach.batch.contribution.service.InstanceContributor.INSTANCE_CONTRIBUTED_ID_CONTEXT;

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
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;

import org.folio.innreach.batch.KafkaItemReader;
import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.listener.ContributionJobExecutionListener;
import org.folio.innreach.batch.contribution.listener.ContributionJobStatsListener;
import org.folio.innreach.batch.contribution.listener.InstanceExceptionListener;
import org.folio.innreach.batch.contribution.listener.ItemExceptionListener;
import org.folio.innreach.batch.contribution.service.FolioItemReader;
import org.folio.innreach.batch.contribution.service.ItemContributor;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.config.props.FolioEnvironment;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@Configuration
@EnableBatchProcessing
@EnableConfigurationProperties(ContributionJobProperties.class)
@ConditionalOnProperty(value = "batch.enabled", havingValue = "true", matchIfMissing = true)
public class ContributionJobConfig {

  public static final String CONTRIBUTION_JOB_NAME = "contributionJob";
  public static final String CONTRIBUTION_JOB_LAUNCHER_NAME = "contributionJobLauncher";
  public static final String CONTRIBUTION_JOB_REPOSITORY_NAME = "contributionJobRepo";
  public static final String INSTANCE_CONTRIBUTION_STEP = "instanceContributionStep";
  public static final String ITEM_CONTRIBUTION_STEP = "itemContributionStep";

  public static final BiConsumer<String, InstanceIterationEvent> CONSUMER_REC_PROCESSOR =
    (String key, InstanceIterationEvent value) -> value.setInstanceId(UUID.fromString(key));

  @JobScope
  @Bean
  public ContributionJobContext jobContext() {
    return new ContributionJobContext();
  }

  @StepScope
  @Bean
  public KafkaItemReader<String, InstanceIterationEvent> kafkaReader(KafkaProperties kafkaProperties,
                                                                     ContributionJobProperties jobProperties,
                                                                     ContributionJobContext jobContext,
                                                                     FolioEnvironment folioEnv) {

    Properties props = new Properties();
    props.putAll(kafkaProperties.buildConsumerProperties());

    var topic = String.format("%s.%s.%s",
      folioEnv.getEnvironment(), jobContext.getTenantId(), jobProperties.getReaderTopic());

    var reader = new KafkaItemReader<String, InstanceIterationEvent>(props, of(new TopicPartition(topic, 0)));
    reader.setName("contributionKafkaReader");
    reader.setPollTimeout(Duration.ofSeconds(jobProperties.getReaderPollTimeoutSec()));
    reader.setPartitionOffsets(new HashMap<>());
    reader.setRecordProcessor(CONSUMER_REC_PROCESSOR);

    return reader;
  }

  @Bean(name = INSTANCE_CONTRIBUTION_STEP)
  public Step instanceContributionStep(KafkaItemReader<String, InstanceIterationEvent> kafkaReader,
                                       ContributionJobProperties jobProperties,
                                       StepBuilderFactory stepBuilderFactory,
                                       ItemProcessor<InstanceIterationEvent, Instance> instanceLoader,
                                       ItemWriter<Instance> instanceContributor,
                                       ContributionJobStatsListener countListener,
                                       BackOffPolicy backOffPolicy,
                                       InstanceExceptionListener failureListener) {

    return stepBuilderFactory.get(INSTANCE_CONTRIBUTION_STEP)
      .<InstanceIterationEvent, Instance>chunk(1)
      .processor(instanceLoader)
      .writer(instanceContributor)
      .reader(kafkaReader)
      .faultTolerant()
      .backOffPolicy(backOffPolicy)
      .retry(Exception.class)
      .retryLimit(jobProperties.getRetryAttempts())
      .skip(Exception.class)
      .skipLimit(Integer.MAX_VALUE)
      .listener(contextPromotionListener())
      .listener((ItemReadListener) failureListener)
      .listener((ItemProcessListener) failureListener)
      .listener((ItemWriteListener) failureListener)
      .listener(countListener)
      .build();
  }

  @Bean(name = ITEM_CONTRIBUTION_STEP)
  public Step itemContributionStep(FolioItemReader itemReader,
                                   ContributionJobProperties jobProperties,
                                   StepBuilderFactory stepBuilderFactory,
                                   ItemContributor itemContributor,
                                   ContributionJobStatsListener countListener,
                                   ContributionService contributionService,
                                   BackOffPolicy backOffPolicy,
                                   ItemExceptionListener failureListener) {

    return stepBuilderFactory.get(ITEM_CONTRIBUTION_STEP)
      .<Item, Item>chunk(jobProperties.getChunkSize())
      .reader(itemReader)
      .writer(itemContributor)
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
  public Job job(@Qualifier(INSTANCE_CONTRIBUTION_STEP) Step instanceContributionStep,
                 @Qualifier(ITEM_CONTRIBUTION_STEP) Step itemContributionStep,
                 ContributionJobExecutionListener jobExecutionListener,
                 ContributionJobContext jobContext,
                 JobBuilderFactory jobBuilderFactory) {

    return jobBuilderFactory.get(CONTRIBUTION_JOB_NAME)
      .incrementer(new RunIdIncrementer())
      .start(instanceContributionStep)
      .next(itemContributionStep)
      .listener(jobExecutionListener)
      .listener(jobContext)
      .build();
  }

  @Bean
  public FixedBackOffPolicy backOffPolicy(ContributionJobProperties jobProperties) {
    var backOffPolicy = new FixedBackOffPolicy();
    backOffPolicy.setBackOffPeriod(jobProperties.getRetryIntervalMs());
    return backOffPolicy;
  }

  @Bean(CONTRIBUTION_JOB_LAUNCHER_NAME)
  public JobLauncher jobLauncher(@Lazy JobRepository jobRepository) throws Exception {
    var jobLauncher = new SimpleJobLauncher();
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
  public ExecutionContextPromotionListener contextPromotionListener() {
    ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
    listener.setKeys(new String[] {INSTANCE_CONTRIBUTED_ID_CONTEXT});
    return listener;
  }

  @Bean(CONTRIBUTION_JOB_REPOSITORY_NAME)
  public TransactionProxyFactoryBean baseProxy(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    TransactionProxyFactoryBean transactionProxyFactoryBean = new TransactionProxyFactoryBean();
    Properties transactionAttributes = new Properties();
    transactionAttributes.setProperty("*", "PROPAGATION_REQUIRED");
    transactionProxyFactoryBean.setTransactionAttributes(transactionAttributes);
    transactionProxyFactoryBean.setTarget(jobRepository);
    transactionProxyFactoryBean.setTransactionManager(transactionManager);
    return transactionProxyFactoryBean;
  }

  @Bean
  public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(@Lazy JobRegistry jobRegistry) {
    var postProcessor = new JobRegistryBeanPostProcessor();
    postProcessor.setJobRegistry(jobRegistry);
    return postProcessor;
  }

}
