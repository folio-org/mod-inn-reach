package org.folio.innreach.domain.dto.deserializer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.sourcerecord.SourceRecordDTO;

@JsonTest
class SourceRecordDTODeserializerTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @SneakyThrows
  public void shouldDeserializeSourceRecordDTO() {
    var resource = this.getClass().getResource("/json/source-record-storage/source-record-storage-example.json");
    var sourceRecordDTO = objectMapper.readValue(resource, SourceRecordDTO.class);

    assertNotNull(sourceRecordDTO);
  }

  @Test
  @SneakyThrows
  public void shouldDeserializeInventoryInstanceDTO() {
    var resource = this.getClass().getResource("/json/inventory-storage/american-bar-association.json");
    var inventoryInstanceDTO = objectMapper.readValue(resource, InventoryInstanceDTO.class);

    assertNotNull(inventoryInstanceDTO);
  }

}
