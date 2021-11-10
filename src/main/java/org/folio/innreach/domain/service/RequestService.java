package org.folio.innreach.domain.service;

import java.util.UUID;

import org.springframework.scheduling.annotation.Async;

import org.folio.innreach.domain.entity.InnReachTransaction;

public interface RequestService {
  @Async
  void createItemRequest(String transactionTrackingId);

  void createItemRequest(InnReachTransaction transaction, UUID centralServerId, UUID servicePointId, UUID requesterId);

  void moveItemRequest(InnReachTransaction transaction);

  void cancelRequest(InnReachTransaction transaction, String reason);
}
