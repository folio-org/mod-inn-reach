package org.folio.innreach.controller.d2ir;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.PatronInfoService;
import org.folio.innreach.dto.PatronInfoRequestDTO;
import org.folio.innreach.dto.PatronInfoResponseDTO;
import org.folio.innreach.rest.resource.VerifyPatronD2irApi;

@Log4j2
@RequestMapping("/inn-reach/d2ir/")
@RestController
@RequiredArgsConstructor
public class PatronInfoController implements VerifyPatronD2irApi {

  private final PatronInfoService service;

  @Override
  @PostMapping(value = "/circ/verifypatron", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PatronInfoResponseDTO> verifyPatron(@RequestHeader(value = "x-to-code") String localCodeHeader,
                                                            @RequestHeader(value = "x-from-code") String centralCodeHeader,
                                                            @RequestBody(required = false) PatronInfoRequestDTO patronInfoRequest) {

    var visiblePatronId = patronInfoRequest.getVisiblePatronId();
    var patronAgencyCode = patronInfoRequest.getPatronAgencyCode();
    var patronName = patronInfoRequest.getPatronName();

    var info = service.verifyPatron(centralCodeHeader, visiblePatronId, patronAgencyCode, patronName);

    return new ResponseEntity<>(info, HttpStatus.OK);
  }

}
