package org.folio.innreach.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  @GetMapping("/current")
  public ResponseEntity<ContributionDTO> getCurrentContributionByServerId(@PathVariable("centralServerId") UUID centralServerId) {
    var currContribution = service.getCurrent(centralServerId);
    return ResponseEntity.ok(currContribution);
  }

  @GetMapping("/history")
  public ResponseEntity<ContributionsDTO> getContributionHistoryByServerId(@PathVariable("centralServerId") UUID centralServerId,
                                                                           Integer offset, Integer limit) {
    var contributionHistory = service.getHistory(centralServerId, offset, limit);
    return ResponseEntity.ok(contributionHistory);
  }

}
