package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.service.MARCTransformationOptionsSettingsService;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsListDTO;
import org.folio.innreach.rest.resource.MARCTransformationOptionsSettingsApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/inn-reach/central-servers")
public class MARCTransformationOptionsSettingsController implements MARCTransformationOptionsSettingsApi {
  @Autowired
  private final MARCTransformationOptionsSettingsService service;

  @Override
  @GetMapping("/marc-transformation-options")
  public ResponseEntity<MARCTransformationOptionsSettingsListDTO> getAllMARCTransformationOptionsSettings(@Min(0) @Max(2147483647) @Valid Integer offset,
                                                                                                          @Min(0) @Max(2147483647) @Valid Integer limit){
    var marcTransformOptSetList = service.getAllMARCTransformOptSet(offset, limit);
    return ResponseEntity.ok(marcTransformOptSetList);
  }

  @Override
  @GetMapping("/{centralServerId}/marc-transformation-options")
  public ResponseEntity<MARCTransformationOptionsSettingsDTO> getMARCTransformationOptionsSettingsById(@PathVariable UUID centralServerId){
    var marcTransformOptSet = service.getMARCTransformOptSet(centralServerId);
    return ResponseEntity.ok(marcTransformOptSet);
  }

  @Override
  @PostMapping("/{centralServerId}/marc-transformation-options")
  public ResponseEntity<MARCTransformationOptionsSettingsDTO> createMARCTransformationOptionsSettings(@PathVariable UUID centralServerId, @Valid MARCTransformationOptionsSettingsDTO dto){
    var createdMARCTransformOptSet = service.createMARCTransformOptSet(centralServerId, dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdMARCTransformOptSet);
  }

  @Override
  @PutMapping("/{centralServerId}/marc-transformation-options")
  public ResponseEntity<Void> updateMARCTransformationOptionsSettings(@PathVariable UUID centralServerId, @Valid MARCTransformationOptionsSettingsDTO dto){
    service.updateMARCTransformOptSet(centralServerId, dto);
    return ResponseEntity.noContent().build();
  }

  @Override
  @DeleteMapping("/{centralServerId}/marc-transformation-options")
  public ResponseEntity<Void> deleteMARCTransformationOptionsSettings (@PathVariable UUID centralServerId) {
    service.deleteMARCTransformOptSet(centralServerId);
    return ResponseEntity.noContent().build();
  }
}
