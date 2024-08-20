package org.folio.innreach.domain.dto.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CustomFieldsDeserializer extends JsonDeserializer<Map<String, String>> {
  @Override
  public Map<String, String> deserialize(JsonParser p, DeserializationContext ctxt)
    throws IOException {
    Map<String, String> customFields = new HashMap<>();
    JsonNode node = p.getCodec().readTree(p);

    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      String key = field.getKey();
      JsonNode valueNode = field.getValue();

      if (valueNode.isTextual()) {
        customFields.put(key, valueNode.asText());
      }
    }
    return customFields;
  }
}
