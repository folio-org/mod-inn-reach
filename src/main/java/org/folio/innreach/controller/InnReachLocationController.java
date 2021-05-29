package org.folio.innreach.controller;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.InnReachLocationsDTO;
import org.folio.innreach.rest.resource.LocationsApi;

@RequiredArgsConstructor
@RestController
@RequestMapping("/inn-reach/locations")
public class InnReachLocationController implements LocationsApi {

	private final InnReachLocationService innReachLocationService;


  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteLocation(@PathVariable UUID id) {
    innReachLocationService.deleteInnReachLocation(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<InnReachLocationDTO> getLocationById(@PathVariable UUID id) {
    var innReachLocation = innReachLocationService.getInnReachLocation(id);
    return ResponseEntity.ok(innReachLocation);
  }

  @Override
  @GetMapping
  public ResponseEntity<InnReachLocationsDTO> getLocations(@Min(0) @Max(2147483647) @Valid Integer offset,
      @Min(0) @Max(2147483647) @Valid Integer limit) {
    var innReachLocations = innReachLocationService.getAllInnReachLocations(offset, limit);
    return ResponseEntity.ok(innReachLocations);
  }

  @Override
	@PostMapping
  public ResponseEntity<InnReachLocationDTO> postInnReachLocation(@Valid InnReachLocationDTO innReachLocationDTO) {
		var createdInnReachLocation = innReachLocationService.createInnReachLocation(innReachLocationDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdInnReachLocation);
	}

  @Override
  @PutMapping("/{id}")
  public ResponseEntity<InnReachLocationDTO> updateLocation(@PathVariable UUID id,
      @Valid InnReachLocationDTO innReachLocationDTO) {
    var updatedInnReachLocation = innReachLocationService.updateInnReachLocation(id, innReachLocationDTO);
    return ResponseEntity.ok(updatedInnReachLocation);
  }

}
