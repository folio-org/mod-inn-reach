package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

public interface RecordContributionService {

  boolean isContributed(UUID centralServerId, Instance instance);

  boolean isContributed(UUID centralServerId, Instance instance, Item item);

  void contributeInstance(UUID centralServerId, Instance instance);

  void deContributeInstance(UUID centralServerId, Instance instance);

  int contributeItems(UUID centralServerId, String bibId, List<Item> items);

  void deContributeItem(UUID centralServerId, Item item);

  void contributeInstanceWithoutRetry(UUID centralServerId, Instance instance);

  void contributeItemsWithoutRetry(UUID centralServerId, String bibId, List<Item> items);

}
