package org.folio.innreach.client;

import java.util.Optional;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.circulation.MoveRequestDTO;
import org.folio.innreach.domain.dto.folio.circulation.RenewByIdDTO;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.dto.CheckInRequestDTO;
import org.folio.innreach.dto.CheckInResponseDTO;
import org.folio.innreach.dto.CheckOutRequestDTO;
import org.folio.innreach.dto.ClaimItemReturnedRequestDTO;
import org.folio.innreach.dto.LoanDTO;

@FeignClient(name = "circulation", configuration = FolioFeignClientConfig.class, decode404 = true)
public interface CirculationClient {

  @GetMapping("/requests?query=(itemId=={itemId})")
  ResultList<RequestDTO> queryRequestsByItemId(@PathVariable("itemId") UUID itemId);

  @GetMapping("/requests?query=(itemId=={itemId}) and status==(\"Open - Awaiting pickup\" or \"Open - Not yet filled\" or \"Open - In transit\" or \"Open - Awaiting delivery\" )")
  ResultList<RequestDTO> queryRequestsByItemIdAndStatus(@PathVariable("itemId") UUID itemId);


  @GetMapping("/requests?query=id=({requestIds}) and status==\"Open - Not yet filled\"")
  ResultList<RequestDTO> queryNotFilledRequestsByIds(@PathVariable("requestIds") String requestIds, @RequestParam("limit") int limit);

  @GetMapping("/requests/{requestId}")
  Optional<RequestDTO> findRequest(@PathVariable("requestId") UUID requestId);

  @DeleteMapping("/requests/{requestId}")
  void deleteRequest(@PathVariable("requestId") UUID requestId);

  @PostMapping("/requests")
  RequestDTO sendRequest(@RequestBody RequestDTO requestDTO);

  @PutMapping("/requests/{requestId}")
  void updateRequest(@PathVariable("requestId") UUID requestId, @RequestBody RequestDTO request);

  @PostMapping("/requests/{requestId}/move")
  RequestDTO moveRequest(@PathVariable("requestId") UUID requestId, @RequestBody MoveRequestDTO payload);

  @PostMapping("/check-in-by-barcode")
  CheckInResponseDTO checkInByBarcode(CheckInRequestDTO checkIn);

  @PostMapping("/check-out-by-barcode")
  LoanDTO checkOutByBarcode(CheckOutRequestDTO checkOut);

  @GetMapping("/loans?query=(itemId=={itemId})")
  ResultList<LoanDTO> queryLoansByItemId(@PathVariable("itemId") UUID itemId);

  @GetMapping("/loans/{loanId}")
  Optional<LoanDTO> findLoan(@PathVariable("loanId") UUID loanId);

  @DeleteMapping("/loans/{loanId}")
  Optional<LoanDTO> deleteLoan(@PathVariable("loanId") UUID loanId);

  @PostMapping("/loans")
  LoanDTO createLoan(@RequestBody LoanDTO loan);

  @PutMapping("/loans/{loanId}")
  void updateLoan(@PathVariable("loanId") UUID loanId, @RequestBody LoanDTO loan);

  @PostMapping("/renew-by-id")
  LoanDTO renewLoan(@RequestBody RenewByIdDTO renewLoan);

  @PostMapping("/loans/{loanId}/claim-item-returned")
  void claimItemReturned(@PathVariable("loanId") UUID loanId, @RequestBody ClaimItemReturnedRequestDTO payload);

}
