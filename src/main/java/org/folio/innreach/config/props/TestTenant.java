package org.folio.innreach.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties("test-tenant")
public class TestTenant {
  private String tenantName;
}
