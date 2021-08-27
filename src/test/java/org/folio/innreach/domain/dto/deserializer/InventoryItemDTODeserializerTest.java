package org.folio.innreach.domain.dto.deserializer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;

@JsonTest
class InventoryItemDTODeserializerTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @SneakyThrows
  void deserializeInventoryItemJson() {
    var resource = this.getClass().getResource("/json/inventory-item/item.json");

    var inventoryItemDTO = objectMapper.readValue(resource, InventoryItemDTO.class);

    assertNotNull(inventoryItemDTO);
  }
}
