package org.folio.innreach.domain.dto.deserializer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import tools.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import org.folio.innreach.domain.dto.folio.sourcerecord.SourceRecordDTO;
import org.folio.innreach.dto.Instance;

@JsonTest
class SourceRecordDTODeserializerTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @SneakyThrows
  public void shouldDeserializeSourceRecordDTO() {
    var resourceStream = this.getClass().getResourceAsStream("/json/source-record-storage/source-record-storage-example.json");
    var sourceRecordDTO = objectMapper.readValue(resourceStream, SourceRecordDTO.class);

    assertNotNull(sourceRecordDTO);
  }

  @Test
  @SneakyThrows
  public void shouldDeserializeInstance() {
    var resource = this.getClass().getResourceAsStream("/json/inventory-storage/american-bar-association.json");
    var inventoryInstanceDTO = objectMapper.readValue(resource, Instance.class);

    assertNotNull(inventoryInstanceDTO);
  }

}
