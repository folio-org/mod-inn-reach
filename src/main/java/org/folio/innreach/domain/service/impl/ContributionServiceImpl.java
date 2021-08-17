package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.dto.folio.inventoryStorage.InstanceIterationRequest;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.external.client.feign.InventoryStorageClient;
import org.folio.innreach.repository.ContributionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ContributionServiceImpl implements ContributionService {

  private final ContributionRepository repository;
  private final InventoryStorageClient client;

  @Override
  public void startInitialContribution(UUID centralServerId) {
    var contribution = createEmptyContribution(centralServerId);
    repository.save(contribution);

    var request = createInstanceIterationRequest();
    client.startInitialContribution(request);
  }

  private InstanceIterationRequest createInstanceIterationRequest() {
    var request = new InstanceIterationRequest();
    request.setTopicName("inventory.instance-contribution");
    return request;
  }

  private Contribution createEmptyContribution(UUID centralServerId) {
    var contribution = new Contribution();
    contribution.setStatus(Contribution.Status.IN_PROGRESS);
    contribution.setRecordsTotal(0L);
    contribution.setCentralServer(centralServerRef(centralServerId));
    return contribution;
  }
}
