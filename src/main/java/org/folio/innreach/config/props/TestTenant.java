package org.folio.innreach.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties("test-tenant")
@Component
public class TestTenant {
  private String tenantName;
}
