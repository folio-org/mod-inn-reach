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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.ContributionCriteriaConfigurationService;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.rest.resource.ContributionCriteriaApi;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/inn-reach/central-servers/")
public class ContributionCriteriaController implements ContributionCriteriaApi {

  private final ContributionCriteriaConfigurationService service;


  @Override
  @GetMapping("/{centralServerId}/contribution-criteria")
  public ResponseEntity<ContributionCriteriaDTO> getCriteriaByServerId(@PathVariable UUID centralServerId) {
    var criteria = service.getCriteria(centralServerId);

    return ResponseEntity.ok(criteria);
  }

  @Override
  @PostMapping("/{centralServerId}/contribution-criteria")
  public ResponseEntity<ContributionCriteriaDTO> postContributionCriteria(@PathVariable UUID centralServerId,
      ContributionCriteriaDTO dto) {
    var criteria = service.createCriteria(centralServerId, dto);
    
    return ResponseEntity.status(HttpStatus.CREATED).body(criteria);
  }

  @Override
  @PutMapping("/{centralServerId}/contribution-criteria")
  public ResponseEntity<Void> updateCriteria(@PathVariable UUID centralServerId, ContributionCriteriaDTO dto) {
    service.updateCriteria(centralServerId, dto);

    return ResponseEntity.noContent().build();
  }

  @Override
  @DeleteMapping("/{centralServerId}/contribution-criteria")
  public ResponseEntity<Void> deleteCriteria(@PathVariable UUID centralServerId) {
    service.deleteCriteria(centralServerId);
    
    return ResponseEntity.noContent().build();
  }

}
