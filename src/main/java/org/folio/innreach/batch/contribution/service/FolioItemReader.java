package org.folio.innreach.batch.contribution.service;

import static org.folio.innreach.batch.contribution.service.InstanceContributor.INSTANCE_CONTRIBUTED_ID_CONTEXT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.iterators.BoundedIterator;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@StepScope
@Component
@Getter(AccessLevel.PACKAGE)
@RequiredArgsConstructor
public class FolioItemReader extends AbstractItemStreamItemReader<Item> {

  private static final int FETCH_LIMIT = 100;

  public static final String INSTANCE_ITEM_OFFSET_CONTEXT = "contribution.instance.item.offset";
  public static final String INSTANCE_ITEM_TOTAL_CONTEXT = "contribution.instance.item.total";

  private final InventoryViewService inventoryService;

  private Map<UUID, Integer> instanceItemOffsets = Collections.emptyMap();
  private Map<UUID, Integer> instanceItemTotals = Collections.emptyMap();
  private List<UUID> contributedInstanceIds = Collections.emptyList();
  private Iterator<Item> itemsIterator = Collections.emptyIterator();

  @Override
  public Item read() {
    if (!itemsIterator.hasNext()) {
      fetchItems();
    }
    return itemsIterator.hasNext() ? itemsIterator.next() : null;
  }

  @Override
  public void open(ExecutionContext executionContext) {
    if (executionContext.containsKey(INSTANCE_CONTRIBUTED_ID_CONTEXT)) {
      contributedInstanceIds = new ArrayList<>((List<UUID>) executionContext.get(INSTANCE_CONTRIBUTED_ID_CONTEXT));
    }
    if (executionContext.containsKey(INSTANCE_ITEM_OFFSET_CONTEXT)) {
      instanceItemOffsets = new HashMap<>((Map<UUID, Integer>) executionContext.get(INSTANCE_ITEM_OFFSET_CONTEXT));
    }
    if (executionContext.containsKey(INSTANCE_ITEM_TOTAL_CONTEXT)) {
      instanceItemTotals = new HashMap<>((Map<UUID, Integer>) executionContext.get(INSTANCE_ITEM_TOTAL_CONTEXT));
    }
  }

  @Override
  public void update(ExecutionContext executionContext) {
    executionContext.put(INSTANCE_ITEM_OFFSET_CONTEXT, new HashMap<>(instanceItemOffsets));
    executionContext.put(INSTANCE_ITEM_TOTAL_CONTEXT, new HashMap<>(instanceItemTotals));
  }

  private void fetchItems() {
    for (var instanceId : contributedInstanceIds) {
      Integer offset = instanceItemOffsets.compute(instanceId, (k, v) -> (v == null) ? 0 : v + 1);
      Integer total = instanceItemTotals.get(instanceId);

      if (total != null && offset * FETCH_LIMIT >= total) {
        continue;
      }

      var instance = inventoryService.getInstance(instanceId);
      if (instance == null) {
        continue;
      }

      var items = instance.getItems();
      if (CollectionUtils.isNotEmpty(items)) {
        setInstanceRefs(instance, items);

        instanceItemTotals.putIfAbsent(instanceId, items.size());

        // inventory-view client doesn't support pagination of items
        itemsIterator = new BoundedIterator<>(items.iterator(), offset, FETCH_LIMIT);

        return;
      }
    }
  }

  private void setInstanceRefs(Instance instance, List<Item> items) {
    var holdingsByIds = instance.getHoldings()
      .stream()
      .collect(Collectors.toMap(Holding::getId, Function.identity()));

    for (var item : items) {
      item.setInstanceHrid(instance.getHrid());

      if (holdingsByIds.containsKey(item.getHoldingsRecordId())) {
        var holding = holdingsByIds.get(item.getHoldingsRecordId());
        item.setHoldingStatisticalCodeIds(holding.getStatisticalCodeIds());
      }
    }
  }
}
