package org.folio.innreach.client;

import java.util.Optional;
import java.util.UUID;

import feign.codec.ErrorDecoder;
import feign.error.AnnotationErrorDecoder;
import feign.error.ErrorCodes;
import feign.error.ErrorHandling;
import org.apache.http.HttpStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.exception.ResourceVersionConflictException;

@FeignClient(name = "inventory", configuration = InventoryClient.Config.class, decode404 = true)
public interface InventoryClient {

  @GetMapping("/items?query=hrid=={hrId}")
  ResultList<InventoryItemDTO> getItemsByHrId(@PathVariable("hrId") String hrId);

  @PostMapping("/instances")
  void createInstance(@RequestBody InventoryInstanceDTO instance);

  @GetMapping("/instances?query=(hrid=={hrid})")
  ResultList<InventoryInstanceDTO> queryInstanceByHrid(@PathVariable("hrid") String hrid);

  @PostMapping("/items")
  InventoryItemDTO createItem(@RequestBody InventoryItemDTO item);

  @ErrorHandling(codeSpecific = {
      @ErrorCodes(codes = {HttpStatus.SC_CONFLICT}, generate = ResourceVersionConflictException.class)
  })
  @PutMapping("/items/{itemId}")
  InventoryItemDTO updateItem(@PathVariable("itemId") UUID itemId, @RequestBody InventoryItemDTO item);

  @GetMapping("/items/{itemId}")
  Optional<InventoryItemDTO> findItem(@PathVariable("itemId") UUID itemId);

  @GetMapping("/instances/{instanceId}")
  Optional<InventoryInstanceDTO> findInstance(@PathVariable("instanceId") UUID instanceId);

  @GetMapping("/items?query=barcode=={barcode}")
  ResultList<InventoryItemDTO> getItemByBarcode(@PathVariable("barcode") String barcode);

  class Config extends FolioFeignClientConfig {

    @Bean
    public ErrorDecoder inventoryClientErrorDecoder() {
      return AnnotationErrorDecoder.builderFor(InventoryClient.class).build();
    }

  }

}
