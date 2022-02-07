package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.LocalAgency;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.repository.LocalAgencyRepository;
import org.springframework.util.Assert;

@Log4j2
@AllArgsConstructor
@Service
public class RecordContributionServiceImpl implements RecordContributionService {
  private final ContributionValidationService validationService;
  private final InnReachExternalService innReachExternalService;
  private final LocalAgencyRepository localAgencyRepository;

  @Override
  public boolean evaluateInventoryItemForContribution(Item item) {
    log.info("Evaluating inventory item.");
    var result = true;
    var statisticalCodeIds = item.getStatisticalCodeIds();
    var holdingStatisticalCodes = item.getHoldingStatisticalCodeIds();

    if (statisticalCodeIds.size() != 1 || holdingStatisticalCodes.size() != 1) {
      log.warn("Multiple statistical codes defined, item with id = {} is to be decontributed.", item.getId());
      result = false;
    }

    var centralServer = getCentralServer(item);
    if (centralServer == null) {
      log.warn("Can't find central server for an item with id = {} and permanent location id = {}.",
        item.getId(), item.getPermanentLocationId());
      result = false;
    } else {

      var centralServerId = centralServer.getId();
      if (!validStatisticalCode(centralServerId, statisticalCodeIds)) {
        log.warn("Item with id = {}  has \"Do not contribute\" statistical code, it is to be decontributed" +
          "from central server with id = {}.", item.getId(), centralServerId);
        result = false;
      } else if (!validStatisticalCode(centralServerId, holdingStatisticalCodes)) {
        log.warn("Parent holding of item with id = {} has \"Do not contribute\" statistical code, item is to be decontributed" +
          "from central server with id = {}.", item.getId(), centralServerId);
        result = false;
      }
    }

    log.info("Inventory item evaluation complete.");
    return result;
  }

  private boolean validStatisticalCode(UUID centralServerCode, List<UUID> statisticalCodes) {
    var suppressionCode = validationService.getSuppressionStatus(centralServerCode, statisticalCodes);
    return suppressionCode == null || !suppressionCode.equals('n');
  }

  @Override
  public void decontributeInventoryItemEvents(Item item) {
    log.info("Decontributing item with id = " + item.getId());
    var centralServer = getCentralServer(item);
    Assert.isTrue(centralServer != null,
      String.format("Can't find central server for an item with id = %s and permanent location id = %s.", item.getId(), item.getPermanentLocationId()));
    var centralCode = centralServer.getCentralServerCode();

    innReachExternalService.deleteInnReachApi(centralCode, "/contribution/item/" + item.getHrid());
  }

  private CentralServer getCentralServer(Item item) {
    var libraryId = item.getPermanentLocationId();
    var localAgency = localAgencyRepository.fetchOneByLibraryId(libraryId);
    return localAgency.map(LocalAgency::getCentralServer).orElse(null);
  }

  @Override
  public boolean evaluateInventoryInstanceForContribution(Instance instance) {
    log.info("Evaluating inventory instance.");
    var result = true;

    var statisticalCodeIds = instance.getStatisticalCodeIds();

    if (statisticalCodeIds.size() != 1) {
      log.warn("Multiple statistical codes defined, instance with id = {} is to be decontributed.", instance.getId());
      result = false;
    } else {

      var instanceItems = instance.getItems();
      var eligibleItems = false;
      for (Item item : instanceItems) {
        eligibleItems = evaluateInstanceItem(item);
        if (eligibleItems) {
          log.warn("Instance with id = {} has item eligible for contribution, the instance is not to be decontributed.", instance.getId());
          break;
        }
      }

      if (!eligibleItems) {
        log.warn("All items associated with instance with id = {} are not eligible for contribution, the instance is to be decontributed.", instance.getId());
        result = false;
      }
    }

    log.info("Inventory instance evaluation complete.");
    return result;
  }

  private boolean evaluateInstanceItem(Item item) {
    var eligibleItems = false;
    var statisticalCodeIds = item.getStatisticalCodeIds();
    var centralServer = getCentralServer(item);
    if (centralServer == null) {
      log.warn("Can't find central server for an item with id = {} and permanent location id = {}. Skipping evaluating this item.",
        item.getId(), item.getPermanentLocationId());
    } else {
      var centralServerId = centralServer.getId();
      var suppressionCode = validationService.getSuppressionStatus(centralServerId, statisticalCodeIds);
      if (suppressionCode != null && !suppressionCode.equals('n')
        || validationService.getItemCirculationStatus(centralServerId, item).equals(ContributionItemCirculationStatus.AVAILABLE)) {
        eligibleItems = true;
      }
    }
    return eligibleItems;
  }

  @Override
  public void decontributeInventoryInstanceEvents(Instance instance) {
    log.info("Decontributing items from instance with id = " + instance.getId());
    instance.getItems().forEach(this::decontributeInventoryItemEvents);
  }

  @Override
  public boolean evaluateInventoryHoldingForContribution(Holding holding) {
    log.info("Evaluating inventory holding.");
    var result = true;
    var statisticalCodeIds = holding.getStatisticalCodeIds();
    if (statisticalCodeIds.size() != 1) {
      log.warn("Multiple statistical codes defined, instance with id = {} is to be decontributed.", holding.getId());
      result = false;
    } else {
      var centralServer = getCentralServer(holding);
      if (centralServer == null) {
        log.warn("Can't find central server for an item with id = {} and permanent location id = {}.",
          holding.getId(), holding.getPermanentLocationId());
        result = false;
      } else {

        var centralServerId = centralServer.getId();
        var suppressionCode = validationService.getSuppressionStatus(centralServerId, statisticalCodeIds);
        if (suppressionCode != null && suppressionCode.equals('n')) {
          log.warn("Holding with id = {} has \"Do not contribute\" statistical code, all associated items" +
            " is to be decontributed from central server with id = {}.", holding.getId(), centralServerId);
          result = false;
        }
      }
    }
    log.info("Inventory holding evaluation complete.");
    return result;
  }

  @Override
  public void decontributeInventoryHoldingEvents(Holding holding) {
    log.info("Decontributing holding with id = " + holding.getId());
    var centralServer = getCentralServer(holding);
    Assert.isTrue(centralServer != null,
      String.format("Can't find central server for a holding with id = %s and permanent location id = %s.", holding.getId(), holding.getPermanentLocationId()));
    var centralCode = centralServer.getCentralServerCode();

    innReachExternalService.deleteInnReachApi(centralCode, "/contribution/bib/" + holding.getHrid());
  }

  private CentralServer getCentralServer(Holding holding) {
    var libraryId = holding.getPermanentLocationId();
    var localAgency = localAgencyRepository.fetchOneByLibraryId(libraryId);
    return localAgency.map(LocalAgency::getCentralServer).orElse(null);
  }

}
