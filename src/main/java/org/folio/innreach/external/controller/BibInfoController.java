package org.folio.innreach.external.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.dto.BibInfoResponseDTO;
import org.folio.innreach.external.service.BibInfoService;
import org.folio.innreach.rest.resource.BibInfoD2irApi;

@Log4j2
@RequestMapping("/innreach/v2")
@RestController
@RequiredArgsConstructor
public class BibInfoController implements BibInfoD2irApi {

  private final BibInfoService bibInfoService;

  @GetMapping(value = "/getbibrecord/{bibId}/{centralCode}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BibInfoResponseDTO> getBibRecord(@PathVariable("bibId") String bibId,
                                                         @PathVariable("centralCode") String centralCode) {
    var info = bibInfoService.getBibInfo(bibId, centralCode);

    return new ResponseEntity<>(info, HttpStatus.OK);
  }

}
