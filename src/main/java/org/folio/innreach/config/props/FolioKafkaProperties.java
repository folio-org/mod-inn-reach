package org.folio.innreach.config.props;

import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("kafka")
public class FolioKafkaProperties {

  private int retryIntervalMs = 1000;
  private int retryAttempts = 5;

  private Map<String, KafkaListenerProperties> listener;

  @Data
  public static class KafkaListenerProperties {

    private String topicPattern;
    private String groupId;
    private Integer concurrency = 5;
  }
}
