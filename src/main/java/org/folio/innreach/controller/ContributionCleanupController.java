package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.service.ContributionCleanupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/inn-reach/central-servers/contributions/cleanup")
public class ContributionCleanupController {

  private final ContributionCleanupService cleanupService;

  @PostMapping
  public ResponseEntity<Void> cleanup() {
    cleanupService.cleanup();
    return ResponseEntity.noContent().build();
  }
}
