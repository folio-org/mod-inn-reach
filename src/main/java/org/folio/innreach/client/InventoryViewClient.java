package org.folio.innreach.client;

import static java.util.Collections.emptyList;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.folio.innreach.client.cql.CqlQuery;
import org.folio.innreach.config.FolioRequestInterceptor;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@FeignClient(name = "inventory-view", configuration = FolioRequestInterceptor.class)
public interface InventoryViewClient {

  /**
   * Retrieves resources by ids from inventory service.
   *
   * @param cql - CQL query
   * @return {@link ResultList} with Instance objects inside.
   */
  @GetMapping(path = "/instances", consumes = APPLICATION_OCTET_STREAM_VALUE)
  ResultList<InstanceView> getInstances(@RequestParam("query") CqlQuery cql, @RequestParam("limit") int limit);

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  class InstanceView {
    private Instance instance;
    private List<Holding> holdingsRecords = emptyList();
    private List<Item> items = emptyList();

    public Instance toInstance() {
      return instance.holdings(holdingsRecords).items(items);
    }
  }
}
