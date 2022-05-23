package org.folio.innreach.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.InnReachRecallUserService;
import org.folio.innreach.dto.InnReachRecallUserDTO;
import org.folio.innreach.rest.resource.InnReachRecallUserApi;

@RequiredArgsConstructor
@RestController
@RequestMapping("/inn-reach/central-servers")
public class InnReachRecallUserController implements InnReachRecallUserApi {

  private final InnReachRecallUserService recallUserService;

  @Override
  @GetMapping("/{centralServerId}/inn-reach-recall-user")
  public ResponseEntity<InnReachRecallUserDTO> getCentralServerRecallUser(@PathVariable UUID centralServerId) {
    var recallUser = recallUserService.getInnReachRecallUser(centralServerId);
    return ResponseEntity.ok(recallUser);
  }

  @Override
  @PostMapping("/{centralServerId}/inn-reach-recall-user")
  public ResponseEntity<InnReachRecallUserDTO> saveInnReachRecallUser(@PathVariable UUID centralServerId, @RequestBody InnReachRecallUserDTO innReachRecallUserDTO) {
    var recallUser = recallUserService.saveInnReachRecallUser(centralServerId, innReachRecallUserDTO);
    return ResponseEntity.ok(recallUser);
  }

  @Override
  @PutMapping("/{centralServerId}/inn-reach-recall-user")
  public ResponseEntity<Void> updateCentralServerRecallUser(@PathVariable UUID centralServerId, @RequestBody InnReachRecallUserDTO innReachRecallUserDTO) {
    recallUserService.updateInnReachRecallUser(centralServerId, innReachRecallUserDTO);
    return ResponseEntity.noContent().build();
  }

}
