package org.folio.innreach.domain.service.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.external.service.InnReachExternalService;

@Log4j2
@AllArgsConstructor
@Service
public class RecordContributionServiceImpl implements RecordContributionService {
  private final ContributionValidationService validationService;
  private final InnReachExternalService innReachExternalService;

  @Override
  public Set<UUID> evaluateInventoryItemForContribution(Item item, Set<UUID> centralServerIds) {
    log.info("Evaluating inventory item.");
    Set<UUID> notValidCodes = new HashSet<>();
    var statisticalCodeIds = item.getStatisticalCodeIds();
    var holdingStatisticalCodes = item.getHoldingStatisticalCodeIds();

    if (statisticalCodeIds.size() != 1 || holdingStatisticalCodes.size() != 1) {
      log.warn("Multiple statistical codes defined, item with id = {} is to be decontributed.", item.getId());
      notValidCodes = centralServerIds;
    } else {
      for (UUID code : centralServerIds) {
        if (!validStatisticalCode(code, statisticalCodeIds)) {
          log.warn("Item with id = {}  has \"Do not contribute\" statistical code, it is to be decontributed" +
            "from central server with code = {}.", item.getId(), code);
          notValidCodes.add(code);
        } else if (!validStatisticalCode(code, item.getHoldingStatisticalCodeIds())) {
          log.warn("Parent holding of item with id = {} has \"Do not contribute\" statistical code, item is to be decontributed" +
            "from central server with code = {}.", item.getId(), code);
          notValidCodes.add(code);
        }
      }
    }
    log.info("Inventory item evaluation complete.");
    return notValidCodes;
  }

  private boolean validStatisticalCode(UUID centralServerCode, List<UUID> statisticalCodes) {
    var suppressionCode = validationService.getSuppressionStatus(centralServerCode, statisticalCodes);
    return suppressionCode == null || !suppressionCode.equals('n');
  }

  @Override
  public void decontributeInventoryItemEvents(Item item, Set<UUID> centralServerCodes) {
    log.info("Decontributing item with id = " + item.getId());

    centralServerCodes.forEach(code ->
      innReachExternalService.deleteInnReachApi(code.toString(), "/contribution/item/" + item.getId()));
  }

  @Override
  public Set<UUID> evaluateInventoryInstanceForContribution(Instance instance, Set<UUID> centralServerCodes) {
    log.info("Evaluating inventory instance.");
    Set<UUID> notValidCodes = Collections.emptySet();
    var statisticalCodeIds = instance.getStatisticalCodeIds();

    if (statisticalCodeIds.size() != 1) {
      log.warn("Multiple statistical codes defined, instance with id = {} is to be decontributed.", instance.getId());
      notValidCodes = centralServerCodes;
    }

    log.info("Inventory instance evaluation complete.");
    return notValidCodes;
  }

  @Override
  public void decontributeInventoryInstanceEvents(Instance instance, Set<UUID> centralServerCodes) {
    log.info("Decontributing instance with id = " + instance.getId());

    centralServerCodes.forEach(code ->
      innReachExternalService.deleteInnReachApi(code.toString(), "/contribution/bib/" + instance.getId()));
  }

  @Override
  public Set<UUID> evaluateInventoryHoldingForContribution(Holding holding, Set<UUID> centralServerCodes) {
    log.info("Evaluating inventory holding.");
    Set<UUID> result = new HashSet<>();
    var statisticalCodeIds = holding.getStatisticalCodeIds();
    if (statisticalCodeIds.size() != 1) {
      log.warn("Multiple statistical codes defined, instance with id = {} is to be decontributed.", holding.getId());
      result = centralServerCodes;
    } else {
      for (UUID code : centralServerCodes) {
        var suppressionCode = validationService.getSuppressionStatus(code, statisticalCodeIds);
        if (suppressionCode != null && suppressionCode.equals('n')) {
          log.warn("Holding with id = {} has \"Do not contribute\" statistical code, all associated items" +
            " is to be decontributed from central server with code = {}.", holding.getId(), code);
          result.add(code);
        }
      }
    }
    log.info("Evaluating associated items.");
    holding.getHoldingsItems().forEach(item -> evaluateInventoryItemForContribution(item, centralServerCodes));
    log.info("Inventory holding evaluation complete.");
    return result;
  }

  @Override
  public void decontributeInventoryHoldingEvents(Holding holding, Set<UUID> centralServerCodes) {
    log.info("Decontributing holding with id = " + holding.getId());

    centralServerCodes.forEach(code ->
      innReachExternalService.deleteInnReachApi(code.toString(), "/contribution/bib/" + holding.getId()));
  }

}
