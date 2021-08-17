package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.repository.ContributionRepository;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ContributionServiceImpl implements ContributionService {

  private final ContributionRepository repository;
  private final InventoryClient client;

  @Override
  public void startInitialContribution() {
    var contribution = emptyContribution();
    repository.save(contribution);
    client.startInitialContribution();
  }

  private Contribution emptyContribution() {
    var contribution = new Contribution();
    contribution.setStatus(Contribution.Status.IN_PROGRESS);
    contribution.setRecordsTotal(0L);
    contribution.setRecordsProcessed(0L);
    contribution.setRecordsContributed(0L);
    contribution.setRecordsUpdated(0L);
    contribution.setRecordsDecontributed(0L);
    return contribution;
  }
}
