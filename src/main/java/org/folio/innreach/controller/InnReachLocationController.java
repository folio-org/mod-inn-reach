package org.folio.innreach.controller;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

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
@RequestMapping("/locations")
public class InnReachLocationController implements LocationsApi {

	private final InnReachLocationService innReachLocationService;

  @Override
  @DeleteMapping("/{locationId}")
  public ResponseEntity<Void> deleteLocation(@PathVariable @Pattern(
      regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$") String locationId) {
    innReachLocationService.deleteInnReachLocation(UUID.fromString(locationId));
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/{locationId}")
  public ResponseEntity<InnReachLocationDTO> getLocationById(@PathVariable @Pattern(
      regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$") String locationId) {
    var innReachLocation = innReachLocationService.getInnReachLocation(UUID.fromString(locationId));
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
  @PutMapping("/{locationId}")
  public ResponseEntity<InnReachLocationDTO> updateLocation(@PathVariable @Pattern(
      regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$") String locationId,
      @Valid InnReachLocationDTO innReachLocationDTO) {
      var updatedInnReachLocation = innReachLocationService.updateInnReachLocation(UUID.fromString(locationId),
	      innReachLocationDTO);
		return ResponseEntity.ok(updatedInnReachLocation);
	}

}
