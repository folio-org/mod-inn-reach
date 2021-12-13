package org.folio.innreach.controller.d2ir;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.CirculationService;
import org.folio.innreach.dto.BaseCircRequestDTO;
import org.folio.innreach.dto.CancelRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.ItemReceivedDTO;
import org.folio.innreach.dto.ItemShippedDTO;
import org.folio.innreach.dto.LocalHoldDTO;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.RecallDTO;
import org.folio.innreach.dto.ReturnUncirculatedDTO;
import org.folio.innreach.dto.TransferRequestDTO;
import org.folio.innreach.rest.resource.InnReachCirculationApi;

@RestController
@Validated
@RequestMapping("/inn-reach/d2ir/circ")
@RequiredArgsConstructor
public class InnReachCirculationController implements InnReachCirculationApi {

  private final CirculationService circulationService;

  @Override
  @PostMapping(value = "/patronhold/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> patronHold(@PathVariable String trackingId,
                                                        @PathVariable String centralCode,
                                                        @RequestHeader("X-To-Code") String xToCode,
                                                        @RequestHeader("X-From-Code") String xFromCode,
                                                        @RequestHeader("X-Request-Creation-Time") Integer requestTime,
                                                        PatronHoldDTO patronHold) {
    var innReachResponse = circulationService.initiatePatronHold(trackingId, centralCode, patronHold);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/itemshipped/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> itemShipped(@PathVariable String trackingId,
                                                         @PathVariable String centralCode,
                                                         @RequestHeader("X-To-Code") String xToCode,
                                                         @RequestHeader("X-From-Code") String xFromCode,
                                                         @RequestHeader("X-Request-Creation-Time") Integer requestTime,
                                                         ItemShippedDTO itemShipped) {
    var innReachResponse = circulationService.trackPatronHoldShippedItem(trackingId, centralCode, itemShipped);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/cancelrequest/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> cancelPatronHold(@PathVariable String trackingId,
                                                              @PathVariable String centralCode,
                                                              @RequestHeader("X-To-Code") String xToCode,
                                                              @RequestHeader("X-From-Code") String xFromCode,
                                                              @RequestHeader("X-Request-Creation-Time") Integer requestTime,
                                                              CancelRequestDTO cancelRequest) {
    var innReachResponse = circulationService.cancelPatronHold(trackingId, centralCode, cancelRequest);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/localhold/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> createLocalHold(@PathVariable String trackingId,
                                                             @PathVariable String centralCode,
                                                             @RequestHeader("X-To-Code") String xToCode,
                                                             @RequestHeader("X-From-Code") String xFromCode,
                                                             @RequestHeader("X-Request-Creation-Time") Integer requestTime,
                                                             LocalHoldDTO localHold) {
    var innReachResponse = circulationService.initiateLocalHold(trackingId, centralCode, localHold);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/intransit/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> itemInTransit(@PathVariable String trackingId,
                                                           @PathVariable String centralCode,
                                                           @RequestHeader("X-To-Code") String xToCode,
                                                           @RequestHeader("X-From-Code") String xFromCode,
                                                           @RequestHeader("X-Request-Creation-Time") Integer requestTime,
                                                           BaseCircRequestDTO itemInTransitRequest) {
    var innReachResponse = circulationService.itemInTransit(trackingId, centralCode, itemInTransitRequest);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/transferrequest/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> transferRequest(@PathVariable String trackingId,
                                                             @PathVariable String centralCode,
                                                             @RequestHeader("X-To-Code") String xToCode,
                                                             @RequestHeader("X-From-Code") String xFromCode,
                                                             @RequestHeader("X-Request-Creation-Time") Integer requestTime,
                                                             TransferRequestDTO transferRequest) {
    var innReachResponse = circulationService.transferPatronHoldItem(trackingId, centralCode, transferRequest);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/cancelitemhold/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> cancelItemHold(@PathVariable String trackingId,
                                                            @PathVariable String centralCode,
                                                            @RequestHeader("X-To-Code") String xToCode,
                                                            @RequestHeader("X-From-Code") String xFromCode,
                                                            @RequestHeader("X-Request-Creation-Time") Integer requestTime,
                                                            BaseCircRequestDTO cancelItemDTO) {
    var innReachResponse = circulationService.cancelItemHold(trackingId, centralCode, cancelItemDTO);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/receiveunshipped/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> receiveUnshipped(@PathVariable String trackingId,
                                                              @PathVariable String centralCode,
                                                              @RequestHeader("X-To-Code") String xToCode,
                                                              @RequestHeader("X-From-Code") String xFromCode,
                                                              @RequestHeader("X-Request-Creation-Time") Integer requestTime,
                                                              BaseCircRequestDTO receiveUnshippedRequestDTO) {
    var innReachResponse = circulationService.receiveUnshipped(trackingId, centralCode, receiveUnshippedRequestDTO);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/returnuncirculated/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> returnUncirculated(@PathVariable String trackingId,
                                                                @PathVariable String centralCode,
                                                                @RequestHeader("X-To-Code") String xToCode,
                                                                @RequestHeader("X-From-Code") String xFromCode,
                                                                @RequestHeader("X-Request-Creation-Time") Integer requestTime,
                                                                ReturnUncirculatedDTO returnUncirculated) {
    var innReachResponse = circulationService.returnUncirculated(trackingId, centralCode, returnUncirculated);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/itemreceived/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> itemReceived(@PathVariable String trackingId,
                                                          @PathVariable String centralCode,
                                                          @RequestHeader("X-To-Code") String xToCode,
                                                          @RequestHeader("X-From-Code") String xFromCode,
                                                          @RequestHeader("X-Request-Creation-Time") Integer requestTime,
                                                          ItemReceivedDTO itemReceivedDTO) {
    var innReachResponse = circulationService.itemReceived(trackingId, centralCode, itemReceivedDTO);

    return ResponseEntity.ok(innReachResponse);
  }
  @Override
  @PutMapping(value = "/recall/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> recall(String trackingId,
                                                    String centralCode,
                                                    @RequestHeader("X-To-Code") String xToCode,
                                                    @RequestHeader("X-From-Code") String xFromCode,
                                                    @RequestHeader("X-Request-Creation-Time") Integer requestTime,
                                                    RecallDTO recallDTO) {
    var innReachResponse = circulationService.recall(trackingId, centralCode, recallDTO);

    return ResponseEntity.ok(innReachResponse);
  }
}
