package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.CentralServerSettingsService;
import org.folio.innreach.dto.CentralServerSettingsDTO;
import org.folio.innreach.rest.resource.CentralServerSettingsApi;

import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/inn-reach/central-servers/settings")
public class CentralServerSettingsController implements CentralServerSettingsApi {

  private final CentralServerSettingsService centralServerSettings;

  @Override
  @PostMapping
  public ResponseEntity<CentralServerSettingsDTO> createCentralServerSettings(CentralServerSettingsDTO centralServerSettingsDTO) {
    var createdCentralServerSettings = centralServerSettings.createCentralServerSettings(centralServerSettingsDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdCentralServerSettings);
  }

  @Override
  @GetMapping("/{centralServerId}")
  public ResponseEntity<CentralServerSettingsDTO> getCentralServerSettingsByCentralServerId(@PathVariable UUID centralServerId) {
    var centralServersSettings = centralServerSettings.getCentralServerSettingsByCentralServerId(centralServerId);
    return ResponseEntity.ok(centralServersSettings);
  }

  @Override
  @PutMapping
  public ResponseEntity<Void> updateCentralServerSettingsByCentralServerId(CentralServerSettingsDTO centralServerSettingsDTO) {
    centralServerSettings.updateCentralServerSettingsByCentralServerId(centralServerSettingsDTO);
    return ResponseEntity.noContent().build();
  }

}
