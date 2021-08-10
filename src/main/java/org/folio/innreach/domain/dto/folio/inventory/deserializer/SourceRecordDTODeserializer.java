package org.folio.innreach.domain.dto.folio.inventory.deserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.folio.innreach.domain.dto.folio.inventory.SourceRecordDTO;

public class SourceRecordDTODeserializer extends StdDeserializer<SourceRecordDTO> {

  public static final String ID_JSON_NODE_NAME = "id";
  public static final String RECORD_TYPE_JSON_NODE_NAME = "recordType";
  public static final String STATE_JSON_NODE_NAME = "state";
  public static final String DELETED_JSON_NODE_NAME = "deleted";

  public static final String PARSED_RECORD_JSON_NODE_NAME = "parsedRecord";
  public static final String CONTENT_JSON_NODE_NAME = "content";
  public static final String FIELDS_JSON_NODE_NAME = "fields";
  public static final String SUB_FIELDS_JSON_NODE_NAME = "subfields";


  protected SourceRecordDTODeserializer(Class<?> vc) {
    super(vc);
  }

  public SourceRecordDTODeserializer() {
    this(null);
  }

  @Override
  public SourceRecordDTO deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);

    return SourceRecordDTO.builder()
      .id(UUID.fromString(node.get(ID_JSON_NODE_NAME).asText()))
      .recordType(node.get(RECORD_TYPE_JSON_NODE_NAME).asText())
      .state(node.get(STATE_JSON_NODE_NAME).asText())
      .deleted(node.get(DELETED_JSON_NODE_NAME).asBoolean())
      .fields(collectFields(node.get(PARSED_RECORD_JSON_NODE_NAME).get(CONTENT_JSON_NODE_NAME).get(FIELDS_JSON_NODE_NAME)))
      .build();
  }

  private List<SourceRecordDTO.RecordField> collectFields(JsonNode fieldsNode) {
    var fields = new ArrayList<SourceRecordDTO.RecordField>();

    fieldsNode.forEach(fieldNode -> fieldNode.fields()
      .forEachRemaining(fieldKeyValue -> fields.add(buildField(fieldKeyValue)))
    );

    return fields;
  }

  private SourceRecordDTO.RecordField buildField(Map.Entry<String, JsonNode> fieldKeyValue) {
    var key = fieldKeyValue.getKey();
    var value = fieldKeyValue.getValue();

    if (value.isContainerNode()) {
      return new SourceRecordDTO.RecordField(key, null, collectSubFields(value));
    }

    return new SourceRecordDTO.RecordField(key, value.asText(), new ArrayList<>());
  }

  private List<SourceRecordDTO.RecordField> collectSubFields(JsonNode fieldWithSubfieldsNode) {
    var subFields = new ArrayList<SourceRecordDTO.RecordField>();

    fieldWithSubfieldsNode.get(SUB_FIELDS_JSON_NODE_NAME)
      .forEach(subFieldNode -> subFieldNode.fields()
        .forEachRemaining(subfieldKeyValue -> subFields.add(
          new SourceRecordDTO.RecordField(subfieldKeyValue.getKey(), subfieldKeyValue.getValue().asText(), new ArrayList<>()))
        ));

    return subFields;
  }

}
