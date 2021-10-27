package org.folio.innreach.domain.service;

import org.springframework.scheduling.annotation.Async;

public interface RequestService {
  @Async
  void createItemRequest(String transactionTrackingId);
}
