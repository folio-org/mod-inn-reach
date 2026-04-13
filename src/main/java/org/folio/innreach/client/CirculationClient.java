package org.folio.innreach.client;

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

  @PostExchange("/requests")
  RequestDTO sendRequest(@RequestBody RequestDTO requestDTO);

  @PutExchange("/requests/{requestId}")
  void updateRequest(@PathVariable("requestId") UUID requestId, @RequestBody RequestDTO request);

  @PostExchange("/requests/{requestId}/move")
  RequestDTO moveRequest(@PathVariable("requestId") UUID requestId, @RequestBody MoveRequestDTO payload);

  @PostExchange("/check-in-by-barcode")
  CheckInResponseDTO checkInByBarcode(CheckInRequestDTO checkIn);

  @PostExchange("/check-out-by-barcode")
  LoanDTO checkOutByBarcode(CheckOutRequestDTO checkOut);

  @GetExchange("/loans?query=(itemId=={itemId})")
  ResultList<LoanDTO> queryLoansByItemId(@PathVariable("itemId") UUID itemId);

  @GetExchange("/loans?query=(itemId=={itemId}) and status=({status})")
  ResultList<LoanDTO> queryLoansByItemIdAndStatus(@PathVariable("itemId") UUID itemId, @PathVariable("status") String status);

  @GetExchange("/loans/{loanId}")
  Optional<LoanDTO> findLoan(@PathVariable("loanId") UUID loanId);

  @DeleteExchange("/loans/{loanId}")
  Optional<LoanDTO> deleteLoan(@PathVariable("loanId") UUID loanId);

  @PostExchange("/loans")
  LoanDTO createLoan(@RequestBody LoanDTO loan);

  @PutExchange("/loans/{loanId}")
  void updateLoan(@PathVariable("loanId") UUID loanId, @RequestBody LoanDTO loan);

  @PostExchange("/renew-by-id")
  LoanDTO renewLoan(@RequestBody RenewByIdDTO renewLoan);

  @PostExchange("/loans/{loanId}/claim-item-returned")
  void claimItemReturned(@PathVariable("loanId") UUID loanId, @RequestBody ClaimItemReturnedRequestDTO payload);

}
