package org.folio.innreach.controller.d2ir;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.CirculationService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.BaseCircRequestDTO;
import org.folio.innreach.dto.CancelRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.ItemReceivedDTO;
import org.folio.innreach.dto.ItemShippedDTO;
import org.folio.innreach.dto.LocalHoldDTO;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.RecallDTO;
import org.folio.innreach.dto.RenewLoanDTO;
import org.folio.innreach.dto.ReturnUncirculatedDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.TransferRequestDTO;
import org.folio.innreach.rest.resource.InnReachCirculationApi;

@RestController
@Validated
@RequestMapping("/inn-reach/d2ir/circ")
@RequiredArgsConstructor
public class InnReachCirculationController implements InnReachCirculationApi {

  private final CirculationService circulationService;
  private final RequestService requestService;

  @Override
  @PostMapping("/itemhold/{trackingId}/{centralCode}")
  public ResponseEntity<InnReachResponseDTO> createInnReachTransactionItemHold(@PathVariable String trackingId,
                                                                               @PathVariable String centralCode,
                                                                               TransactionHoldDTO dto) {
    var response = circulationService.createInnReachTransactionItemHold(trackingId, centralCode, dto);
    requestService.createItemHoldRequest(trackingId, centralCode);
    return ResponseEntity.ok(response);
  }

  @Override
  @PostMapping(value = "/patronhold/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> patronHold(@PathVariable String trackingId,
                                                        @PathVariable String centralCode, PatronHoldDTO patronHold) {
    var innReachResponse = circulationService.initiatePatronHold(trackingId, centralCode, patronHold);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/itemshipped/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> itemShipped(@PathVariable String trackingId,
                                                         @PathVariable String centralCode,
                                                         ItemShippedDTO itemShipped) {
    var innReachResponse = circulationService.trackPatronHoldShippedItem(trackingId, centralCode, itemShipped);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/cancelrequest/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> cancelPatronHold(@PathVariable String trackingId,
                                                              @PathVariable String centralCode,
                                                              CancelRequestDTO cancelRequest) {
    var innReachResponse = circulationService.cancelPatronHold(trackingId, centralCode, cancelRequest);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/localhold/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> createLocalHold(@PathVariable String trackingId,
                                                             @PathVariable String centralCode,
                                                             LocalHoldDTO localHold) {
    var innReachResponse = circulationService.initiateLocalHold(trackingId, centralCode, localHold);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/intransit/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> itemInTransit(@PathVariable String trackingId,
                                                           @PathVariable String centralCode,
                                                           BaseCircRequestDTO itemInTransitRequest) {
    var innReachResponse = circulationService.itemInTransit(trackingId, centralCode, itemInTransitRequest);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/transferrequest/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> transferRequest(@PathVariable String trackingId,
                                                             @PathVariable String centralCode,
                                                             TransferRequestDTO transferRequest) {
    var innReachResponse = circulationService.transferPatronHoldItem(trackingId, centralCode, transferRequest);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/cancelitemhold/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> cancelItemHold(@PathVariable String trackingId,
                                                            @PathVariable String centralCode,
                                                            BaseCircRequestDTO cancelItemDTO) {
    var innReachResponse = circulationService.cancelItemHold(trackingId, centralCode, cancelItemDTO);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/receiveunshipped/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> receiveUnshipped(@PathVariable String trackingId,
                                                              @PathVariable String centralCode,
                                                              BaseCircRequestDTO receiveUnshippedRequestDTO) {
    var innReachResponse = circulationService.receiveUnshipped(trackingId, centralCode, receiveUnshippedRequestDTO);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/returnuncirculated/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> returnUncirculated(@PathVariable String trackingId,
                                                                @PathVariable String centralCode,
                                                                ReturnUncirculatedDTO returnUncirculated) {
    var innReachResponse = circulationService.returnUncirculated(trackingId, centralCode, returnUncirculated);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/itemreceived/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> itemReceived(@PathVariable String trackingId,
                                                          @PathVariable String centralCode,
                                                          ItemReceivedDTO itemReceivedDTO) {
    var innReachResponse = circulationService.itemReceived(trackingId, centralCode, itemReceivedDTO);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/recall/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> recall(@PathVariable String trackingId,
                                                    @PathVariable String centralCode,
                                                    RecallDTO recallDTO) {
    var innReachResponse = circulationService.recall(trackingId, centralCode, recallDTO);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/borrowerrenew/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> borrowerRenew(@PathVariable String trackingId,
                                                           @PathVariable String centralCode, RenewLoanDTO renewLoan) {
    var innReachResponse = circulationService.borrowerRenewLoan(trackingId, centralCode, renewLoan);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping(value = "/ownerrenew/{trackingId}/{centralCode}", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InnReachResponseDTO> ownerRenew(@PathVariable String trackingId,
                                                        @PathVariable String centralCode, RenewLoanDTO renewLoan) {
    var innReachResponse = circulationService.ownerRenewLoan(trackingId, centralCode, renewLoan);

    return ResponseEntity.ok(innReachResponse);
  }

}
