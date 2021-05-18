package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.dto.CentralServerDTO;
import org.folio.innreach.domain.service.CentralServerService;
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

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/central-servers")
public class CentralServerController {

  private final CentralServerService centralServerService;

  @PostMapping
  public ResponseEntity<CentralServerDTO> createCentralServer(@Valid @RequestBody CentralServerDTO centralServerDTO) {
    var createdCentralServer = centralServerService.createCentralServer(centralServerDTO);

    return ResponseEntity.status(HttpStatus.CREATED).body(createdCentralServer);
  }

  @GetMapping
  public ResponseEntity<List<CentralServerDTO>> getAllCentralServers() {
    var allCentralServers = centralServerService.getAllCentralServers();

    return ResponseEntity.ok(allCentralServers);
  }

  @GetMapping("/{centralServerId}")
  public ResponseEntity<CentralServerDTO> getCentralServer(@PathVariable UUID centralServerId) {
    var centralServer = centralServerService.getCentralServer(centralServerId);

    return ResponseEntity.ok(centralServer);
  }

  @PutMapping("/{centralServerId}")
  public ResponseEntity<CentralServerDTO> updateCentralServer(@PathVariable UUID centralServerId,
                                                              @RequestBody @Valid CentralServerDTO centralServerDTO) {
    var updatedCentralServer = centralServerService.updateCentralServer(centralServerId, centralServerDTO);

    return ResponseEntity.ok(updatedCentralServer);
  }

  @DeleteMapping("/{centralServerId}")
  public ResponseEntity<CentralServerDTO> deleteCentralServer(@PathVariable UUID centralServerId) {
    centralServerService.deleteCentralServer(centralServerId);

    return ResponseEntity.noContent().build();
  }
}
