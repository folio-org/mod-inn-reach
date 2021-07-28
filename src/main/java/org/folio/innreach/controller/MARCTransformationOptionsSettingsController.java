package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.service.MARCTransformationOptionsSettingsService;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;
import org.folio.innreach.rest.resource.MARCTransformationOptionsSettingsApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/inn-reach/central-servers/{centralServerId}/marc-transformation-options")
public class MARCTransformationOptionsSettingsController implements MARCTransformationOptionsSettingsApi {
  @Autowired
  private final MARCTransformationOptionsSettingsService service;

  @Override
  @GetMapping
  public ResponseEntity<MARCTransformationOptionsSettingsDTO> getMARCTransformationOptionsSettingsById(@PathVariable UUID centralServerId){
    var marcTransformOptSet = service.getMARCTransformOptSet(centralServerId);
    return ResponseEntity.ok(marcTransformOptSet);
  }

  @Override
  @PostMapping
  public ResponseEntity<MARCTransformationOptionsSettingsDTO> createMARCTransformationOptionsSettings(@PathVariable UUID centralServerId, @Valid MARCTransformationOptionsSettingsDTO dto){
    var createdMARCTransformOptSet = service.createMARCTransformOptSet(centralServerId, dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdMARCTransformOptSet);
  }

  @Override
  @PutMapping
  public ResponseEntity<Void> updateMARCTransformationOptionsSettings(@PathVariable UUID centralServerId, @Valid MARCTransformationOptionsSettingsDTO dto){
    service.updateMARCTransformOptSet(centralServerId, dto);
    return ResponseEntity.noContent().build();
  }
}
