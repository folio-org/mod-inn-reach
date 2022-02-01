package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.EvaluateService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.external.service.InnReachExternalService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@Service
public class EvaluateServiceImpl implements EvaluateService {

  private final ContributionValidationService contributionValidationService;
  private final InnReachExternalService innReachExternalService;

  @Override
  public void handleItemEvent(Item item, List<String> centralServerCodes) {
    var statisticalCodeIds = item.getStatisticalCodeIds();
    var holdingStatisticalCodes = item.getHoldingStatisticalCodeIds();

    if (statisticalCodeIds.size() > 1 || holdingStatisticalCodes.size() > 1) {
      log.warn("Multiple statistical codes defined, item with id = {} is to be decontributed.", item.getHrid());
    } else {
      for (String code : centralServerCodes) {
        var check = contributionValidationService.getSuppressionStatus(UUID.fromString(code), statisticalCodeIds);
        var check2 = contributionValidationService.getSuppressionStatus(UUID.fromString(code), holdingStatisticalCodes);
        if (check != null && !check.equals('n')) {
          innReachExternalService.postInnReachApi(code, "/contribution/item/" + item.getHrid());
        } else {
          log.warn("Item with id = {}  has \"Do not contribute\" statistical code, it is to be decontributed" +
            "from central server with code = {}.", item.getHrid(), code);
        }
        if (check2 != null && !check2.equals('n')) {
          innReachExternalService.postInnReachApi(code, "/contribution/item/" + item.getHrid());
        } else {
          log.warn("Parent holding of item with id = {} has \"Do not contribute\" statistical code, item is to be decontributed" +
            "from central server with code = {}.", item.getHrid(), code);
        }
      }
    }
  }

  @Override
  public void handleHoldingEvent(Holding holding, List<String> centralServerCodes) {
    var statisticalCodeIds = holding.getStatisticalCodeIds();

    if (statisticalCodeIds.size() > 1) {
      log.warn("Multiple statistical codes defined, holding with id = {} is to be decontributed.", holding.getId());
    } else {
      for (String code : centralServerCodes) {
        var check = contributionValidationService.getSuppressionStatus(UUID.fromString(code), statisticalCodeIds);
        if (check != null && !check.equals('n')) {
          innReachExternalService.postInnReachApi(code, "/contribution/bib/" + holding.getId());
        } else {
          log.warn("Holding with id = {}  has \"Do not contribute\" statistical code, it is to be decontributed" +
            "from central server with code = {}.", holding.getId(), code);
        }
      }
    }
  }

  @Override
  public void handleInstanceEvent(Instance instance, List<String> centralServerCodes) {
    var statisticalCodeIds = instance.getStatisticalCodeIds();

    if (statisticalCodeIds.size() > 1) {
      log.warn("Multiple statistical codes defined, instance with id = {} is to be decontributed.", instance.getHrid());
    } else {
      for (String code : centralServerCodes) {
        var check = contributionValidationService.getSuppressionStatus(UUID.fromString(code), statisticalCodeIds);
        if (check != null && !check.equals('n')) {
          innReachExternalService.postInnReachApi(code, "/contribution/bib/" + instance.getHrid());
        } else {
          log.warn("Instance with id = {}  has \"Do not contribute\" statistical code, it is to be decontributed" +
            "from central server with code = {}.", instance.getHrid(), code);
        }
      }
    }
  }
}
