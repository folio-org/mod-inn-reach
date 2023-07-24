package org.folio.innreach.domain.service;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.UUID;

import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.external.dto.InnReachResponse;

public interface RecordContributionService {

  boolean isContributed(UUID centralServerId, Instance instance);

  boolean isContributed(UUID centralServerId, Instance instance, Item item);

  void contributeInstance(UUID centralServerId, Instance instance) throws SocketTimeoutException;

  void deContributeInstance(UUID centralServerId, Instance instance) throws SocketTimeoutException;

  int contributeItems(UUID centralServerId, String bibId, List<Item> items) throws SocketTimeoutException;

  void moveItem(UUID centralServerId, String newBibId, Item item) throws SocketTimeoutException;

  void deContributeItem(UUID centralServerId, Item item);

  void contributeInstanceWithoutRetry(UUID centralServerId, Instance instance);

  void contributeItemsWithoutRetry(UUID centralServerId, String bibId, List<Item> items);

  InnReachResponse verifyBibContribution(UUID centralServerId, String bibId);

}
