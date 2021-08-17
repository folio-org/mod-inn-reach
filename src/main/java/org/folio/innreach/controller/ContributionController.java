package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.rest.resource.ContributionApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inn-reach/central-servers/{centralServerId}/contributions")
public class ContributionController implements ContributionApi {

  private final ContributionService service;

  @Override
  @PostMapping()
  public ResponseEntity<Void> startInitialContribution(@PathVariable UUID centralServerId) {
    service.startInitialContribution(centralServerId);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
