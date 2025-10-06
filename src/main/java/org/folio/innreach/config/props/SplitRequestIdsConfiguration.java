package org.folio.innreach.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("inn-reach.request-ids")
public class SplitRequestIdsConfiguration {

  private Integer splitSize = 50;
}
