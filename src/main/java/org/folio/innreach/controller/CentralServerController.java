package org.folio.innreach.controller;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

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

import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.CentralServersDTO;
import org.folio.innreach.rest.resource.CentralServersApi;

@RequiredArgsConstructor
@RestController
@RequestMapping("/inn-reach/central-servers")
@Validated
public class CentralServerController implements CentralServersApi {

  private final CentralServerService centralServerService;

  @Override
  @DeleteMapping("/{centralServerId}")
  public ResponseEntity<Void> deleteCentralServer(@PathVariable UUID centralServerId) {
    centralServerService.deleteCentralServer(centralServerId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/{centralServerId}")
  public ResponseEntity<CentralServerDTO> getCentralServerById(@PathVariable UUID centralServerId) {
    var centralServer = centralServerService.getCentralServer(centralServerId);
    return ResponseEntity.ok(centralServer);
  }

  @Override
  @GetMapping
  public ResponseEntity<CentralServersDTO> getCentralServers(@Min(0) @Max(2147483647) @Valid Integer offset,
      @Min(0) @Max(2147483647) @Valid Integer limit) {
    var allCentralServers = centralServerService.getAllCentralServers(offset, limit);
    return ResponseEntity.ok(allCentralServers);
  }

  @Override
  @PostMapping
  public ResponseEntity<CentralServerDTO> postCentralServer(@Valid CentralServerDTO centralServerDTO) {
    var createdCentralServer = centralServerService.createCentralServer(centralServerDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdCentralServer);
  }

  @Override
  @PutMapping("/{centralServerId}")
  public ResponseEntity<CentralServerDTO> updateCentralServer(@PathVariable UUID centralServerId,
      @Valid CentralServerDTO centralServerDTO) {
    var updatedCentralServer = centralServerService.updateCentralServer(centralServerId, centralServerDTO);
    return ResponseEntity.ok(updatedCentralServer);
  }
}
