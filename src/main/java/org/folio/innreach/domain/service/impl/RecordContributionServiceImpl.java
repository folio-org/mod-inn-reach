package org.folio.innreach.domain.service.impl;

import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.LocalAgency;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.repository.LocalAgencyRepository;

@Log4j2
@AllArgsConstructor
@Service
public class RecordContributionServiceImpl implements RecordContributionService {
  private final ContributionValidationService validationService;
  private final InnReachExternalService innReachExternalService;
  private final LocalAgencyRepository localAgencyRepository;
  private final FolioLocationService locationService;

  @Override
  public void decontributeInventoryItemEvents(Item item) {
    log.info("Decontributing item with id = " + item.getId());
    var centralServer = getCentralServer(item);

    Assert.isTrue(centralServer != null,
      String.format("Can't find central server for an item with id = %s and permanent location id = %s.", item.getId(), item.getEffectiveLocationId()));
    var centralCode = centralServer.getCentralServerCode();

    innReachExternalService.deleteInnReachApi(centralCode, "/contribution/item/" + item.getHrid());
  }

  @Override
  public void decontributeInventoryInstanceEvents(Instance instance) {
    log.info("Decontributing items from instance with id = " + instance.getId());
    instance.getItems().forEach(this::decontributeInventoryItemEvents);
  }

  @Override
  public void decontributeInventoryHoldingEvents(Holding holding) {
    log.info("Decontributing items from holding with id = " + holding.getId());
    holding.getHoldingsItems().forEach(this::decontributeInventoryItemEvents);
  }

  @Override
  public void updateInventoryItem(Item oldItem, Item newItem) {
    var centralServer = getCentralServer(newItem);
    if (centralServer == null){
      log.warn("Cannot find central server for item with id = {}, it is to be decontributed.", newItem.getHrid());
      decontributeInventoryItemEvents(oldItem);
      return;
    }
    var centralServerId = centralServer.getId();

    var valid = validationService.isEligibleForContribution(centralServerId, newItem);
    if (!valid) {
      log.warn("Item with id = " + newItem.getHrid() + " is not eligible for contribution, it is to be decontributed.");
      decontributeInventoryItemEvents(oldItem);
    }
  }

  @Override
  public void updateInventoryInstance(Instance oldInstance, Instance newInstance) {
    var centralServers = newInstance.getItems().stream().map(this::getCentralServer).collect(Collectors.toList());
    var centralServerIds = centralServers.stream().map(CentralServer::getId).distinct().collect(Collectors.toList());
    var valid = true;
    for (UUID centralServerId : centralServerIds) {
      valid = validationService.isEligibleForContribution(centralServerId, newInstance);
      if (!valid) {
        log.warn("Instance with id = {} is not eligible for contribution, it is to be decontributed.", newInstance.getHrid());
        decontributeInventoryInstanceEvents(oldInstance);
        break;
      }
    }
  }

  @Override
  public void updateInventoryHolding(Holding oldHolding, Holding newHolding) {
    var centralServer = getCentralServer(newHolding);
    if (centralServer == null){
      log.warn("Cannot find central server for holding with id = " + newHolding.getHrid() + ", it is to be decontributed.");
      decontributeInventoryHoldingEvents(oldHolding);
      return;
    }
    var centralServerId = centralServer.getId();
    var valid = validationService.isEligibleForContribution(centralServerId, newHolding);
    if (!valid) {
      log.warn("Holding with id = {} is not eligible for contribution, it is to be decontributed.", newHolding.getHrid());
      decontributeInventoryHoldingEvents(oldHolding);
    }
  }

  private CentralServer getCentralServer(Item item) {
    var locationId = item.getEffectiveLocationId();
    var libraryId = locationService.getLocationById(locationId).getLibraryId();
    var localAgency = localAgencyRepository.fetchOneByLibraryId(libraryId);
    return localAgency.map(LocalAgency::getCentralServer).orElse(null);
  }

  private CentralServer getCentralServer(Holding holding) {
    var locationId = holding.getPermanentLocationId();
    var libraryId = locationService.getLocationById(locationId).getLibraryId();
    var localAgency = localAgencyRepository.fetchOneByLibraryId(libraryId);
    return localAgency.map(LocalAgency::getCentralServer).orElse(null);
  }
}
