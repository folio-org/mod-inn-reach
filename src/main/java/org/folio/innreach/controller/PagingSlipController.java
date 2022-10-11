package org.folio.innreach.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.PagingSlipService;
import org.folio.innreach.dto.PagingSlipsDTO;
import org.folio.innreach.rest.resource.PagingSlipApi;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/inn-reach")
public class PagingSlipController implements PagingSlipApi {

  private final PagingSlipService pagingSlipService;

  @Override
  @GetMapping("/paging-slips/{servicePointId}")
  public ResponseEntity<PagingSlipsDTO> getPagingSlips(@PathVariable UUID servicePointId) {
    var pagingSlips = pagingSlipService.getPagingSlipsByServicePoint(servicePointId);
    return ResponseEntity.ok(pagingSlips);
  }

}
