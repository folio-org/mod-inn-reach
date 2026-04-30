package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Optional;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.circulation.CirculationSettingDTO;
import org.folio.innreach.domain.dto.folio.circulation.MoveRequestDTO;
import org.folio.innreach.domain.dto.folio.circulation.RenewByIdDTO;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.dto.CheckInRequestDTO;
import org.folio.innreach.dto.CheckInResponseDTO;
import org.folio.innreach.dto.CheckOutRequestDTO;
import org.folio.innreach.dto.ClaimItemReturnedRequestDTO;
import org.folio.innreach.dto.LoanDTO;

@HttpExchange("circulation")
public interface CirculationClient {

  @GetExchange("/settings?query=(name==other_settings)")
  ResultList<CirculationSettingDTO> getCheckoutSettings();

  @GetExchange("/requests?query=(itemId=={itemId})")
  ResultList<RequestDTO> queryRequestsByItemId(@PathVariable("itemId") UUID itemId);

  @GetExchange("/requests?query=(itemId=={itemId}) and " +
          "status==(\"Open - Awaiting pickup\" or \"Open - Not yet filled\" or \"Open - In transit\" or \"Open - Awaiting delivery\") " +
          "sortby requestDate desc")
  ResultList<RequestDTO> queryRequestsByItemIdAndStatus(@PathVariable("itemId") UUID itemId,@RequestParam("limit") int limit);


  @GetExchange("/requests?query=id=({requestIds}) and status==\"Open - Not yet filled\"")
  ResultList<RequestDTO> queryNotFilledRequestsByIds(@PathVariable("requestIds") String requestIds, @RequestParam("limit") int limit);

  @GetExchange("/requests/{requestId}")
  Optional<RequestDTO> findRequest(@PathVariable("requestId") UUID requestId);

  @DeleteExchange("/requests/{requestId}")
  void deleteRequest(@PathVariable("requestId") UUID requestId);

  @PostExchange(value = "/requests", contentType = APPLICATION_JSON_VALUE)
  RequestDTO sendRequest(@RequestBody RequestDTO requestDTO);

  @PutExchange(value = "/requests/{requestId}", contentType = APPLICATION_JSON_VALUE)
  void updateRequest(@PathVariable("requestId") UUID requestId, @RequestBody RequestDTO request);

  @PostExchange(value = "/requests/{requestId}/move", contentType = APPLICATION_JSON_VALUE)
  RequestDTO moveRequest(@PathVariable("requestId") UUID requestId, @RequestBody MoveRequestDTO payload);

  @PostExchange(value = "/check-in-by-barcode", contentType = APPLICATION_JSON_VALUE)
  CheckInResponseDTO checkInByBarcode(@RequestBody CheckInRequestDTO checkIn);

  @PostExchange(value = "/check-out-by-barcode", contentType = APPLICATION_JSON_VALUE)
  LoanDTO checkOutByBarcode(@RequestBody CheckOutRequestDTO checkOut);

  @GetExchange("/loans?query=(itemId=={itemId})")
  ResultList<LoanDTO> queryLoansByItemId(@PathVariable("itemId") UUID itemId);

  @GetExchange("/loans?query=(itemId=={itemId}) and status=({status})")
  ResultList<LoanDTO> queryLoansByItemIdAndStatus(@PathVariable("itemId") UUID itemId, @PathVariable("status") String status);

  @GetExchange("/loans/{loanId}")
  Optional<LoanDTO> findLoan(@PathVariable("loanId") UUID loanId);

  @DeleteExchange("/loans/{loanId}")
  Optional<LoanDTO> deleteLoan(@PathVariable("loanId") UUID loanId);

  @PostExchange(value = "/loans", contentType = APPLICATION_JSON_VALUE)
  LoanDTO createLoan(@RequestBody LoanDTO loan);

  @PutExchange(value = "/loans/{loanId}", contentType = APPLICATION_JSON_VALUE)
  void updateLoan(@PathVariable("loanId") UUID loanId, @RequestBody LoanDTO loan);

  @PostExchange(value = "/renew-by-id", contentType = APPLICATION_JSON_VALUE)
  LoanDTO renewLoan(@RequestBody RenewByIdDTO renewLoan);

  @PostExchange(value = "/loans/{loanId}/claim-item-returned", contentType = APPLICATION_JSON_VALUE)
  void claimItemReturned(@PathVariable("loanId") UUID loanId, @RequestBody ClaimItemReturnedRequestDTO payload);

}
