package org.folio.innreach.config;

import static com.github.benmanes.caffeine.cache.Caffeine.from;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguration {

  @Bean
  public CacheManager cacheManager(
    @Value("${inn-reach.cache.spec.users-by-id:maximumSize=100,expireAfterAccess=3h}") String usersByIdCacheSpec,
    @Value("${inn-reach.cache.spec.users-by-name:maximumSize=10,expireAfterAccess=3h}") String usersByNameCacheSpec,
    @Value("${inn-reach.cache.spec.system-user-cache:maximumSize=5}") String systemUserCacheSpec,
    @Value("${inn-reach.cache.spec.location-libraries:maximumSize=2000,expireAfterWrite=5m}") String locationLibrariesCacheSpec,
    @Value("${inn-reach.cache.spec.holding-source:maximumSize=500,expireAfterAccess=1h}") String holdingSourceCacheSpec,
    @Value("${inn-reach.cache.spec.domain-event-data-type:maximumSize=500,expireAfterAccess=1h}") String domainEventDataTypeCacheSpec) {
    var cacheManager = new CaffeineCacheManager();
    cacheManager.registerCustomCache("users-by-id", from(usersByIdCacheSpec).build());
    cacheManager.registerCustomCache("users-by-name", from(usersByNameCacheSpec).build());
    cacheManager.registerCustomCache("system-user-cache", from(systemUserCacheSpec).build());
    cacheManager.registerCustomCache("location-libraries", from(locationLibrariesCacheSpec).build());
    cacheManager.registerCustomCache("holding-source", from(holdingSourceCacheSpec).build());
    cacheManager.registerCustomCache("domain-event-data-type", from(domainEventDataTypeCacheSpec).build());
    return cacheManager;
  }
}
