package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.service.ItemContributionOptionsConfigurationService;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;
import org.folio.innreach.rest.resource.ItemContributionOptionsConfigurationApi;
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
@RequestMapping("/inn-reach/central-servers/{centralServerId}/item-contribution-options")
public class ItemContributionOptionsConfigurationController implements ItemContributionOptionsConfigurationApi {
  private final ItemContributionOptionsConfigurationService service;

  @Override
  @GetMapping
  public ResponseEntity<ItemContributionOptionsConfigurationDTO> getItemContributionOptionsConfigurationById(@PathVariable UUID centralServerId) {
    var itmContribOptConf = service.getItmContribOptConf(centralServerId);
    return ResponseEntity.ok(itmContribOptConf);
  }

  @Override
  @PostMapping
  public ResponseEntity<ItemContributionOptionsConfigurationDTO> createItemContributionOptionsConfiguration(@PathVariable UUID centralServerId, @Valid ItemContributionOptionsConfigurationDTO itmContribOptConfDTO) {
    var createdItmContribOptConf = service.createItmContribOptConf(centralServerId, itmContribOptConfDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdItmContribOptConf);
  }

  @Override
  @PutMapping
  public ResponseEntity<Void> updateItemContributionOptionsConfiguration(@PathVariable UUID centralServerId, @Valid ItemContributionOptionsConfigurationDTO itmContribOptConfDTO) {
    service.updateItmContribOptConf(centralServerId, itmContribOptConfDTO);
    return ResponseEntity.noContent().build();
  }
}
