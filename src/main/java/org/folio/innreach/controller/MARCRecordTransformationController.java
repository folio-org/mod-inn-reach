package org.folio.innreach.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.MARCRecordTransformationService;
import org.folio.innreach.rest.resource.MARCTransformationRecordsApi;

@RestController
@RequestMapping("/inn-reach/central-servers/{centralServerId}")
@RequiredArgsConstructor
public class MARCRecordTransformationController implements MARCTransformationRecordsApi {

  private final MARCRecordTransformationService marcRecordTransformationService;

  @Override
  @GetMapping("/marc-record-transformation/{inventoryInstanceId}")
  public ResponseEntity<Void> transformMARCRecord(@PathVariable UUID centralServerId, @PathVariable UUID inventoryInstanceId) {
      marcRecordTransformationService.transformRecord(centralServerId, inventoryInstanceId);
      return ResponseEntity.ok().build(); //todo - return formatted MARC
  }
}
