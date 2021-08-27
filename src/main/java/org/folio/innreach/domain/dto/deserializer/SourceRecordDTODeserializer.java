package org.folio.innreach.domain.dto.deserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.folio.innreach.domain.dto.folio.sourcerecord.ParsedRecordDTO;
import org.folio.innreach.domain.dto.folio.sourcerecord.RecordFieldDTO;
import org.folio.innreach.domain.dto.folio.sourcerecord.SourceRecordDTO;

public class SourceRecordDTODeserializer extends StdDeserializer<SourceRecordDTO> {

  /* Source record */
  public static final String ID_JSON_NODE_NAME = "id";
  public static final String RECORD_TYPE_JSON_NODE_NAME = "recordType";
  public static final String STATE_JSON_NODE_NAME = "state";
  public static final String DELETED_JSON_NODE_NAME = "deleted";

  /* Parsed records */
  public static final String PARSED_RECORD_JSON_NODE_NAME = "parsedRecord";
  public static final String CONTENT_JSON_NODE_NAME = "content";
  public static final String LEADER_JSON_NODE_NAME = "leader";
  public static final String FIELDS_JSON_NODE_NAME = "fields";
  public static final String SUB_FIELDS_JSON_NODE_NAME = "subfields";
  public static final String IND1_JSON_NODE_NAME = "ind1";
  public static final String IND2_JSON_NODE_NAME = "ind2";

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
      .parsedRecord(deserializeParsedRecord(node.get(PARSED_RECORD_JSON_NODE_NAME)))
      .build();
  }

  private ParsedRecordDTO deserializeParsedRecord(JsonNode parsedRecordJsonNode) {
    var contentJsonNode = parsedRecordJsonNode.get(CONTENT_JSON_NODE_NAME);

    return ParsedRecordDTO.builder()
      .id(UUID.fromString(parsedRecordJsonNode.get(ID_JSON_NODE_NAME).asText()))
      .leader(contentJsonNode.get(LEADER_JSON_NODE_NAME).asText())
      .fields(deserializeRecordFields(contentJsonNode.get(FIELDS_JSON_NODE_NAME)))
      .build();
  }

  private List<RecordFieldDTO> deserializeRecordFields(JsonNode recordFieldsJsonNode) {
    var fields = new ArrayList<RecordFieldDTO>();

    recordFieldsJsonNode.forEach(recordField -> recordField.fields()
      .forEachRemaining(fieldJsonNode -> fields.add(deserializeRecordField(fieldJsonNode)))
    );

    return fields;
  }

  private RecordFieldDTO deserializeRecordField(Map.Entry<String, JsonNode> fieldJsonNode) {
    var key = fieldJsonNode.getKey();
    var value = fieldJsonNode.getValue();

    if (value.isContainerNode()) {
      return RecordFieldDTO.builder()
        .code(key)
        .value(null)
        .subFields(deserializeSubFields(value))
        .ind1(value.get(IND1_JSON_NODE_NAME).asText().charAt(0))
        .ind2(value.get(IND2_JSON_NODE_NAME).asText().charAt(0))
        .build();
    }

    return RecordFieldDTO.builder()
      .code(key)
      .value(value.asText())
      .build();
  }

  private List<RecordFieldDTO.SubFieldDTO> deserializeSubFields(JsonNode subFieldJsonNode) {
    var subFields = new ArrayList<RecordFieldDTO.SubFieldDTO>();

    subFieldJsonNode.get(SUB_FIELDS_JSON_NODE_NAME)
      .forEach(subFieldNode -> subFieldNode.fields()
        .forEachRemaining(subfieldKeyValue -> subFields.add(
          new RecordFieldDTO.SubFieldDTO(subfieldKeyValue.getKey().charAt(0), subfieldKeyValue.getValue().asText()))
        ));

    return subFields;
  }

}
