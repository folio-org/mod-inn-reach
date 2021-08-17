package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.rest.resource.ContributionApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inn-reach/contribution")
public class ContributionController implements ContributionApi {

  private final ContributionService service;

  @Override
  @PostMapping("/start-initial-contribution")
  public ResponseEntity<Void> startInitialContribution() {
    service.startInitialContribution();
    return ResponseEntity.noContent().build();
  }
}
