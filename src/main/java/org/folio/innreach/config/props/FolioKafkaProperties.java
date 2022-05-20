package org.folio.innreach.config.props;

import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("kafka")
public class FolioKafkaProperties {

  private Map<String, KafkaListenerProperties> listener;

  @Data
  public static class KafkaListenerProperties {
    private String id;
    private String topicPattern;
    private String concurrency;
    private String groupId;
    private Class<?> dataType;
  }
}
