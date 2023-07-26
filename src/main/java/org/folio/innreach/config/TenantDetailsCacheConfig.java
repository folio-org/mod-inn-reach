package org.folio.innreach.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@Log4j2
public class TenantDetailsCacheConfig {
  @Value("${initial-contribution.tenant-cache.ttl}")
  private int tenantDetailsCacheTtl;

  @Bean("tenantDetailsCache")
  public Cache<String, List<String>> tenantDetailsCache() {
    log.info("Tenant details cache {} ", tenantDetailsCacheTtl);
    return CacheBuilder.newBuilder()
      .expireAfterWrite(tenantDetailsCacheTtl, TimeUnit.SECONDS)
      .build();
  }
}
