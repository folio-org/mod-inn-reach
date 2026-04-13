package org.folio.innreach.client;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@HttpExchange("inventory-view")
public interface InventoryViewClient {

  @GetExchange(url = "/instances?query=(id={instanceId})&limit=1")
  ResultList<InstanceView> getInstanceById(@PathVariable("instanceId") UUID instanceId);

  @GetExchange(url = "/instances?query=(instance.hrid={hrid})&limit=1")
  ResultList<InstanceView> getInstanceByHrid(@PathVariable("hrid") String instanceHrid);

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  class InstanceView {
    private Instance instance;
    private List<Holding> holdingsRecords = emptyList();
    private List<Item> items = emptyList();

    public Instance toInstance() {
      return instance.holdingsRecords(holdingsRecords).items(items);
    }
  }
}
