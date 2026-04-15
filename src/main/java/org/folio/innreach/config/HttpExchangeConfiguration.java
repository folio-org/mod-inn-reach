package org.folio.innreach.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.client.AutomatedPatronBlocksClient;
import org.folio.innreach.client.CancellationReasonClient;
import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.ConfigurationClient;
import org.folio.innreach.client.HoldingSourcesClient;
import org.folio.innreach.client.HoldingsStorageClient;
import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.InventoryViewClient;
import org.folio.innreach.client.ItemStorageClient;
import org.folio.innreach.client.LocationsClient;
import org.folio.innreach.client.ManualPatronBlocksClient;
import org.folio.innreach.client.MaterialTypesClient;
import org.folio.innreach.client.PatronClient;
import org.folio.innreach.client.RequestPreferenceStorageClient;
import org.folio.innreach.client.ServicePointsClient;
import org.folio.innreach.client.ServicePointsUsersClient;
import org.folio.innreach.client.SourceRecordStorageClient;
import org.folio.innreach.client.UsersClient;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.client.InnReachClient;
import org.folio.innreach.external.client.InnReachContributionClient;
import org.folio.innreach.external.client.InnReachLocationClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class HttpExchangeConfiguration {

  private final @Qualifier("httpServiceProxyFactory") HttpServiceProxyFactory factory;

  /**
   * Creates a {@link InventoryClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean
   * @return the {@link InventoryClient} instance
   */
  @Bean
  public InventoryClient inventoryClient(@Qualifier("inventoryHttpServiceProxyFactory") HttpServiceProxyFactory factory) {
    return factory.createClient(InventoryClient.class);
  }

  /**
   * Creates an {@link HoldingsStorageClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean for inventory service
   * @return the {@link HoldingsStorageClient} instance
   */
  @Bean
  public HoldingsStorageClient holdingsStorageClient(@Qualifier("inventoryHttpServiceProxyFactory") HttpServiceProxyFactory factory) {
    return factory.createClient(HoldingsStorageClient.class);
  }

  /**
   * Creates an {@link InnReachAuthClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean for Inn Reach External Service
   * @return the {@link InnReachAuthClient} instance
   */
  @Bean
  public InnReachAuthClient innReachAuthClient(@Qualifier("innReachHttpServiceProxyFactory") HttpServiceProxyFactory factory) {
    return factory.createClient(InnReachAuthClient.class);
  }

  /**
   * Creates an {@link InnReachClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean for Inn Reach External Service
   * @return the {@link InnReachClient} instance
   */
  @Bean
  public InnReachClient innReachClient(@Qualifier("innReachHttpServiceProxyFactory") HttpServiceProxyFactory factory) {
    return factory.createClient(InnReachClient.class);
  }

  /**
   * Creates an {@link InnReachContributionClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean for Inn Reach External Service
   * @return the {@link InnReachContributionClient} instance
   */
  @Bean
  public InnReachContributionClient innReachContributionClient(@Qualifier("innReachHttpServiceProxyFactory") HttpServiceProxyFactory factory) {
    return factory.createClient(InnReachContributionClient.class);
  }

  /**
   * Creates an {@link InnReachLocationClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} bean for Inn Reach External Service
   * @return the {@link InnReachLocationClient} instance
   */
  @Bean
  public InnReachLocationClient innReachLocationClient(@Qualifier("innReachHttpServiceProxyFactory") HttpServiceProxyFactory factory) {
    return factory.createClient(InnReachLocationClient.class);
  }

  /**
   * Creates an {@link AutomatedPatronBlocksClient} bean.
   *
   * @return the {@link AutomatedPatronBlocksClient} instance
   */
  @Bean
  public AutomatedPatronBlocksClient automatedPatronBlocksClient() {
    return factory.createClient(AutomatedPatronBlocksClient.class);
  }

  /**
   * Creates an {@link CancellationReasonClient} bean.
   *
   * @return the {@link CancellationReasonClient} instance
   */
  @Bean
  public CancellationReasonClient cancellationReasonClient() {
    return factory.createClient(CancellationReasonClient.class);
  }

  /**
   * Creates an {@link CirculationClient} bean.
   *
   * @return the {@link CirculationClient} instance
   */
  @Bean
  public CirculationClient circulationClient() {
    return factory.createClient(CirculationClient.class);
  }

  /**
   * Creates an {@link ConfigurationClient} bean.
   *
   * @return the {@link ConfigurationClient} instance
   */
  @Bean
  public ConfigurationClient configurationClient() {
    return factory.createClient(ConfigurationClient.class);
  }

  /**
   * Creates an {@link HoldingSourcesClient} bean.
   *
   * @return the {@link HoldingSourcesClient} instance
   */
  @Bean
  public HoldingSourcesClient holdingSourcesClient() {
    return factory.createClient(HoldingSourcesClient.class);
  }

  /**
   * Creates an {@link HridSettingsClient} bean.
   *
   * @return the {@link HridSettingsClient} instance
   */
  @Bean
  public HridSettingsClient hridSettingsClient() {
    return factory.createClient(HridSettingsClient.class);
  }

  /**
   * Creates an {@link InstanceContributorTypeClient} bean.
   *
   * @return the {@link InstanceContributorTypeClient} instance
   */
  @Bean
  public InstanceContributorTypeClient instanceContributorTypeClient() {
    return factory.createClient(InstanceContributorTypeClient.class);
  }

  /**
   * Creates an {@link InstanceStorageClient} bean.
   *
   * @return the {@link InstanceStorageClient} instance
   */
  @Bean
  public InstanceStorageClient instanceStorageClient() {
    return factory.createClient(InstanceStorageClient.class);
  }

  /**
   * Creates an {@link InstanceTypeClient} bean.
   *
   * @return the {@link InstanceTypeClient} instance
   */
  @Bean
  public InstanceTypeClient instanceTypeClient() {
    return factory.createClient(InstanceTypeClient.class);
  }

  /**
   * Creates an {@link InventoryViewClient} bean.
   *
   * @return the {@link InventoryViewClient} instance
   */
  @Bean
  public InventoryViewClient inventoryViewClient() {
    return factory.createClient(InventoryViewClient.class);
  }

  /**
   * Creates an {@link ItemStorageClient} bean.
   *
   * @return the {@link ItemStorageClient} instance
   */
  @Bean
  public ItemStorageClient itemStorageClient() {
    return factory.createClient(ItemStorageClient.class);
  }

  /**
   * Creates an {@link LocationsClient} bean.
   *
   * @return the {@link LocationsClient} instance
   */
  @Bean
  public LocationsClient locationsClient() {
    return factory.createClient(LocationsClient.class);
  }

  /**
   * Creates an {@link ManualPatronBlocksClient} bean.
   *
   * @return the {@link ManualPatronBlocksClient} instance
   */
  @Bean
  public ManualPatronBlocksClient manualPatronBlocksClient() {
    return factory.createClient(ManualPatronBlocksClient.class);
  }

  /**
   * Creates an {@link MaterialTypesClient} bean.
   *
   * @return the {@link MaterialTypesClient} instance
   */
  @Bean
  public MaterialTypesClient materialTypeClient() {
    return factory.createClient(MaterialTypesClient.class);
  }

  /**
   * Creates an {@link PatronClient} bean.
   *
   * @return the {@link PatronClient} instance
   */
  @Bean
  public PatronClient patronClient() {
    return factory.createClient(PatronClient.class);
  }

  /**
   * Creates an {@link RequestPreferenceStorageClient} bean.
   *
   * @return the {@link RequestPreferenceStorageClient} instance
   */
  @Bean
  public RequestPreferenceStorageClient requestPreferenceStorageClient() {
    return factory.createClient(RequestPreferenceStorageClient.class);
  }

  /**
   * Creates an {@link ServicePointsClient} bean.
   *
   * @return the {@link ServicePointsClient} instance
   */
  @Bean
  public ServicePointsClient servicePointsClient() {
    return factory.createClient(ServicePointsClient.class);
  }

  /**
   * Creates an {@link ServicePointsUsersClient} bean.
   *
   * @return the {@link ServicePointsUsersClient} instance
   */
  @Bean
  public ServicePointsUsersClient servicePointsUsersClient() {
    return factory.createClient(ServicePointsUsersClient.class);
  }

  /**
   * Creates an {@link SourceRecordStorageClient} bean.
   *
   * @return the {@link SourceRecordStorageClient} instance
   */
  @Bean
  public SourceRecordStorageClient sourceRecordStorageClient() {
    return factory.createClient(SourceRecordStorageClient.class);
  }

  /**
   * Creates an {@link UsersClient} bean.
   *
   * @return the {@link UsersClient} instance
   */
  @Bean
  public UsersClient innReachUsersClient() {
    return factory.createClient(UsersClient.class);
  }
}
