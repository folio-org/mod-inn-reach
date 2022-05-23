package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.folio.innreach.config.props.FolioKafkaProperties;
import org.folio.innreach.config.props.FolioKafkaProperties.KafkaListenerProperties;
import org.folio.innreach.dto.LoanDTO;

public class DomainEventTypeResolverTest {

  private static final String TOPIC_PATTERN = "(folio\\.)(.*\\.)circulation\\.loan";
  private static final String TOPIC = "folio.testing.circulation.loan";

  private static final Class<LoanDTO> DATA_TYPE = LoanDTO.class;

  private DomainEventTypeResolver service = new DomainEventTypeResolver(kafkaProperties());

  @Test
  void shouldResolveGenericDataBinding() {
    var javaType = service.resolveType(TOPIC, null, null);

    assertNotNull(javaType);
    assertEquals(DATA_TYPE, javaType.getBindings().getBoundType(0).getRawClass());
  }

  @Test
  void shouldReturnNull_whenUnknownTopic() {
    var javaType = service.resolveType("unknown.topic", null, null);

    assertNull(javaType);
  }

  private static FolioKafkaProperties kafkaProperties() {
    var listener = new KafkaListenerProperties();
    listener.setDataType(DATA_TYPE);
    listener.setTopicPattern(TOPIC_PATTERN);

    var props = new FolioKafkaProperties();
    props.setListener(Map.of("loan", listener));
    return props;
  }

}
