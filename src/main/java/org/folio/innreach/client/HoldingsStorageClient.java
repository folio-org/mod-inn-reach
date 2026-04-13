package org.folio.innreach.client;

import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.folio.innreach.dto.Holding;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange("holdings-storage")
public interface HoldingsStorageClient {

  @GetExchange("/holdings/{holdingId}")
  Optional<Holding> findHolding(@PathVariable("holdingId") UUID holdingId);

  @DeleteExchange("/holdings/{holdingsRecordId}")
  void deleteHolding(@PathVariable("holdingsRecordId") UUID holdingId);

  @PostExchange("/holdings")
  Holding createHolding(@RequestBody Holding holding);

  @PutExchange("/holdings/{holdingId}")
  void updateHolding(@PathVariable UUID holdingId, @RequestBody Holding holding);

}
