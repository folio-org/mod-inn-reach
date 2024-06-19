package org.folio.innreach.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class TenantDetailsCacheConfig {
  @Value("${contribution.tenant-cache.ttl}")
  private int tenantDetailsCacheTtl;

  @Bean("tenantDetailsCache")
  public Cache<String, List<String>> tenantDetailsCache() {
    return CacheBuilder.newBuilder()
      .expireAfterWrite(tenantDetailsCacheTtl, TimeUnit.SECONDS)
      .build();
  }
}
