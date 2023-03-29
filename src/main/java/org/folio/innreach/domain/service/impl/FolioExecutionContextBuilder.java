package org.folio.innreach.domain.service.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.dto.folio.SystemUser;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;

@Component
@RequiredArgsConstructor
public class FolioExecutionContextBuilder {
  private final FolioModuleMetadata moduleMetadata;

  public Builder builder() {
    return new Builder(moduleMetadata);
  }

  public FolioExecutionContext forSystemUser(SystemUser systemUser) {
    return builder()
      .withTenantId(systemUser.getTenantId())
      .withOkapiUrl(systemUser.getOkapiUrl())
      .withToken(systemUser.getToken())
      .withUserId(systemUser.getUserId())
      .build();
  }

  public FolioExecutionContext withUserId(FolioExecutionContext folioContext, UUID userID) {
    return builder()
      .withTenantId(folioContext.getTenantId())
      .withOkapiUrl(folioContext.getOkapiUrl())
      .withUserId(userID)
      .withToken(folioContext.getToken())
      .withAllHeaders(folioContext.getAllHeaders())
      .withModuleMetadata(folioContext.getFolioModuleMetadata())
      .withOkapiUrl(folioContext.getOkapiUrl())
      .withRequestId(folioContext.getRequestId())
      .build();
  }

  public FolioExecutionContext dbOnlyContext(String tenantId) {
    return builder().withTenantId(tenantId).build();
  }

  @With
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Builder {
    private final FolioModuleMetadata moduleMetadata;
    private String tenantId;
    private String okapiUrl;
    private String token;
    private UUID userId;
    private final Map<String, Collection<String>> allHeaders;
    private final Map<String, Collection<String>> okapiHeaders;
    private String requestId;

    public Builder(FolioModuleMetadata moduleMetadata) {
      this.moduleMetadata = moduleMetadata;
      this.allHeaders = new HashMap<>();
      this.okapiHeaders = new HashMap<>();
    }

    public FolioExecutionContext build() {
      return new FolioExecutionContext() {
        @Override
        public String getTenantId() {
          return tenantId;
        }

        @Override
        public String getOkapiUrl() {
          return okapiUrl;
        }

        @Override
        public String getToken() {
          return token;
        }

        @Override
        public UUID getUserId() {
          return userId;
        }

        @Override
        public Map<String, Collection<String>> getAllHeaders() {
          return allHeaders;
        }

        @Override
        public Map<String, Collection<String>> getOkapiHeaders() {
          return okapiHeaders;
        }

        @Override
        public FolioModuleMetadata getFolioModuleMetadata() {
          return moduleMetadata;
        }
        @Override
        public String getRequestId() {
          return requestId;
        }
      };
    }
  }

}
