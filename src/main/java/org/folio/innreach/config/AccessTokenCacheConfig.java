package org.folio.innreach.config;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.folio.innreach.external.dto.AccessTokenDTO;

@Configuration
public class AccessTokenCacheConfig {

  @Value("${inn-reach.jwt-access-token.cache.ttl}")
  private int accessTokenCacheTTL;

  @Value("${inn-reach.jwt-access-token.cache.max-size}")
  private int accessTokenCacheMaxSize;

  @Bean
  public Cache<String, AccessTokenDTO> accessTokenCache() {
    return CacheBuilder.newBuilder()
      .maximumSize(accessTokenCacheMaxSize)
      .expireAfterWrite(accessTokenCacheTTL, TimeUnit.SECONDS)
      .build();
  }
}
