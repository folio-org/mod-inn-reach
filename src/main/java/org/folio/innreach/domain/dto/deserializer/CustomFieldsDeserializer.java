package org.folio.innreach.domain.dto.deserializer;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

public class CustomFieldsDeserializer extends ValueDeserializer<Map<String, String>> {
  @Override
  public Map<String, String> deserialize(JsonParser parser, DeserializationContext context) {
    Map<String, String> customFields = new HashMap<>();
    JsonNode node = parser.readValueAsTree();

    for (Map.Entry<String, JsonNode> field : node.properties()) {
      String key = field.getKey();
      JsonNode valueNode = field.getValue();

      if (valueNode.isString()) {
        customFields.put(key, valueNode.asString());
      }
    }
    return customFields;
  }
}
