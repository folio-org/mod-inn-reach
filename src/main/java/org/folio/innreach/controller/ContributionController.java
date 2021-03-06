package org.folio.innreach.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionsDTO;
import org.folio.innreach.rest.resource.ContributionsApi;


@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/inn-reach/central-servers/{centralServerId}/contributions")
public class ContributionController implements ContributionsApi {

  private final ContributionService service;

  @Override
  @GetMapping("/current")
  public ResponseEntity<ContributionDTO> getCurrentContributionByServerId(@PathVariable("centralServerId") UUID centralServerId) {
    var currContribution = service.getCurrent(centralServerId);
    return ResponseEntity.ok(currContribution);
  }

  @Override
  @DeleteMapping("/current")
  public ResponseEntity<Void> cancelCurrentContributionByServerId(@PathVariable("centralServerId") UUID centralServerId) {
    service.cancelCurrent(centralServerId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/history")
  public ResponseEntity<ContributionsDTO> getContributionHistoryByServerId(@PathVariable("centralServerId") UUID centralServerId,
                                                                           Integer offset, Integer limit) {
    var contributionHistory = service.getHistory(centralServerId, offset, limit);
    return ResponseEntity.ok(contributionHistory);
  }

  @Override
  @PostMapping
  public ResponseEntity<Void> startInitialContribution(@PathVariable UUID centralServerId) {
    service.startInitialContribution(centralServerId);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
