package org.folio.innreach.batch.contribution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.service.InnReachRecallUserService;
import org.folio.innreach.domain.service.InstanceService;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.LoanService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.domain.service.VirtualRecordService;
import org.folio.innreach.domain.service.impl.InnReachTransactionActionNotifier;
import org.folio.innreach.domain.service.impl.InnReachTransactionActionServiceImpl;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.dto.LoanStatus;
import org.folio.innreach.dto.StorageLoanDTO;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InnReachTransactionActionServiceTest {

  @Mock
  private InnReachTransactionRepository transactionRepository;
  @Mock
  private InnReachTransactionMapper transactionMapper;
  @Mock
  private RequestService requestService;
  @Mock
  private LoanService loanService;
  @Mock
  private PatronHoldService patronHoldService;
  @Mock
  private ItemService itemService;
  @Mock
  private InstanceService instanceService;
  @Mock
  private InnReachTransactionActionNotifier notifier;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private InnReachRecallUserService recallUserService;
  @Mock
  private VirtualRecordService virtualRecordService;

  @InjectMocks
  private InnReachTransactionActionServiceImpl service;

  @Test
  void whenItemCheckedOut_onLocalHoldTransaction_shouldSetLocalCheckoutStatusAndNotifyCentral() {
    // Arrange
    var loanId = UUID.randomUUID();
    var itemId = UUID.randomUUID();
    var patronId = UUID.randomUUID();
    var dueDate = new Date();

    var loan = createLoanDTO(loanId, itemId, patronId, dueDate);
    var transaction = createLocalTransaction();
    var itemDto = InventoryItemDTO.builder().hrid("it0001").barcode("item001").build();

    when(transactionRepository.fetchActiveByFolioItemIdAndPatronId(itemId, patronId))
      .thenReturn(Optional.of(transaction));
    when(itemService.find(itemId)).thenReturn(Optional.of(itemDto));

    // Act
    service.associateNewLoanWithTransaction(loan);

    // Assert
    assertEquals(InnReachTransaction.TransactionState.LOCAL_CHECKOUT, transaction.getState());
    verify(transactionRepository).fetchActiveByFolioItemIdAndPatronId(itemId, patronId);
    verify(notifier).reportCheckOut(transaction, itemDto.getHrid(), itemDto.getBarcode());
  }

  @Test
  void whenRequestIsUpdatedAndClosedByOtherMeans_onLocalHoldTransaction_shouldCheckOutItem() {
    // Arrange
    var requestId = UUID.randomUUID();
    var itemId = UUID.randomUUID();
    var servicePointId = UUID.randomUUID();

    var oldRequest = createRequestDTO(requestId, itemId, RequestDTO.RequestStatus.OPEN_NOT_YET_FILLED, servicePointId);
    var newRequest = createRequestDTO(requestId, itemId, RequestDTO.RequestStatus.CLOSED_CANCELLED, servicePointId);

    var transaction = createLocalTransaction();
    transaction.getHold().setFolioRequestId(requestId);
    transaction.getHold().setFolioItemId(itemId);

    when(transactionRepository.fetchActiveByRequestId(requestId)).thenReturn(Optional.of(transaction));
    when(loanService.findByItemId(itemId)).thenReturn(Optional.empty());

    // Act
    service.handleRequestUpdate(oldRequest, newRequest);

    // Assert
    verify(loanService).checkOutItem(transaction, servicePointId);
  }

  @Test
  void whenRequestIsUpdatedAndClosedByOtherMeans_onLocalHoldTransactionWithOpenLoan_shouldUpdateStatusAndNotifyCentral() {
    // Arrange
    var requestId = UUID.randomUUID();
    var itemId = UUID.randomUUID();
    var servicePointId = UUID.randomUUID();

    var oldRequest = createRequestDTO(requestId, itemId, RequestDTO.RequestStatus.OPEN_NOT_YET_FILLED, servicePointId);
    var newRequest = createRequestDTO(requestId, itemId, RequestDTO.RequestStatus.CLOSED_CANCELLED, servicePointId);
    var itemDto = InventoryItemDTO.builder().hrid("it0001").barcode("item001").build();
    var loanDto = new LoanDTO();
    loanDto.setStatus(new LoanStatus());

    var transaction = createLocalTransaction();
    transaction.getHold().setFolioRequestId(requestId);
    transaction.getHold().setFolioItemId(itemId);

    when(transactionRepository.fetchActiveByRequestId(requestId)).thenReturn(Optional.of(transaction));
    when(loanService.findByItemId(itemId)).thenReturn(Optional.of(loanDto));
    when(itemService.find(itemId)).thenReturn(Optional.of(itemDto));
    when(loanService.isOpen(loanDto)).thenReturn(true);

    // Act
    service.handleRequestUpdate(oldRequest, newRequest);

    // Assert
    assertEquals(InnReachTransaction.TransactionState.LOCAL_CHECKOUT, transaction.getState());
    verify(notifier).reportCheckOut(transaction, itemDto.getHrid(), itemDto.getBarcode());
  }

  private InnReachTransaction createLocalTransaction() {
    var transaction = new InnReachTransaction();
    transaction.setId(UUID.randomUUID());
    transaction.setType(InnReachTransaction.TransactionType.LOCAL);
    transaction.setState(InnReachTransaction.TransactionState.LOCAL_HOLD);

    var hold = new TransactionLocalHold();
    transaction.setHold(hold);

    return transaction;
  }

  private StorageLoanDTO createLoanDTO(UUID loanId, UUID itemId, UUID patronId, Date dueDate) {
    var loan = new StorageLoanDTO();
    loan.setId(loanId);
    loan.setItemId(itemId);
    loan.setUserId(patronId);
    loan.setDueDate(dueDate);

    return loan;
  }

  private RequestDTO createRequestDTO(UUID requestId, UUID itemId, RequestDTO.RequestStatus status, UUID servicePointId) {
    return RequestDTO.builder()
      .id(requestId)
      .itemId(itemId)
      .status(status)
      .pickupServicePointId(servicePointId)
      .build();
  }
}
