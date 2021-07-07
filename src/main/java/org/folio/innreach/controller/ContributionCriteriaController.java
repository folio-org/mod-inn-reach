package org.folio.innreach.controller;

import java.util.UUID;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.ContributionCriteriaConfigurationService;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.rest.resource.ContributionCriteriaApi;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/inn-reach/central-servers/")
public class ContributionCriteriaController implements ContributionCriteriaApi {

  private final ContributionCriteriaConfigurationService criteriaConfigurationService;


  @Override
  @PostMapping("/contribution-criteria")
  public ResponseEntity<ContributionCriteriaDTO> postContributionCriteria(
    @Valid @RequestBody ContributionCriteriaDTO contributionCriteriaDTO) {

    return ResponseEntity.status(HttpStatus.CREATED).body(criteriaConfigurationService
      .createCriteria(contributionCriteriaDTO));
  }

  @Override
  @DeleteMapping("/{centralServerId}/contribution-criteria")
  public ResponseEntity<Void> deleteCriteria(@PathVariable UUID centralServerId) {
    criteriaConfigurationService.deleteCriteria(centralServerId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/{centralServerId}/contribution-criteria")
  public ResponseEntity<ContributionCriteriaDTO> getCriteriaById(@PathVariable UUID centralServerId) {
    return ResponseEntity.ok(
      criteriaConfigurationService.getCriteria(centralServerId)
    );
  }

  @Override
  @PutMapping("/{centralServerId}/contribution-criteria")
  public ResponseEntity<Void> updateCriteria(@PathVariable UUID centralServerId,
                                                                @Valid ContributionCriteriaDTO contributionCriteriaDTO) {
    criteriaConfigurationService.updateCriteria(contributionCriteriaDTO);
    return ResponseEntity.noContent().build();
  }

}
