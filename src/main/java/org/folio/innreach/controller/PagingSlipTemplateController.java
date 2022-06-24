package org.folio.innreach.controller;

import java.util.UUID;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.PagingSlipTemplateService;
import org.folio.innreach.dto.PagingSlipTemplateDTO;
import org.folio.innreach.dto.PagingSlipTemplatesDTO;
import org.folio.innreach.rest.resource.PagingSlipTemplateApi;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inn-reach/central-servers")
public class PagingSlipTemplateController implements PagingSlipTemplateApi {

  private final PagingSlipTemplateService service;

  @Override
  @GetMapping("/paging-slip-template")
  public ResponseEntity<PagingSlipTemplatesDTO> getAllPagingSlipTemplates() {
    var templates = service.getAllTemplates();
    return ResponseEntity.ok(templates);
  }

  @Override
  @GetMapping("/{centralServerId}/paging-slip-template")
  public ResponseEntity<PagingSlipTemplateDTO> getPagingSlipTemplate(@PathVariable UUID centralServerId) {
    var template = service.getByCentralServerId(centralServerId);
    return ResponseEntity.ok(template);
  }

  @Override
  @PutMapping("/{centralServerId}/paging-slip-template")
  public ResponseEntity<Void> updatePagingSlipTemplate(@PathVariable UUID centralServerId,
                                                       @Valid PagingSlipTemplateDTO dto) {
    service.update(centralServerId, dto);
    return ResponseEntity.noContent().build();
  }

}
