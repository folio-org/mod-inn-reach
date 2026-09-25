package org.folio.innreach.domain.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWER_RENEW;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.FINAL_CHECKIN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.LOCAL_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.OWNER_RENEW;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.PATRON_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.ITEM;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.LOCAL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.PATRON;
import static org.folio.innreach.util.DateHelper.toEpochSec;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionState;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.service.InnReachRecallUserService;
import org.folio.innreach.domain.service.InstanceService;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.LoanService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.domain.service.RetryableUpdateService;
import org.folio.innreach.domain.service.VirtualRecordService;
import org.folio.innreach.dto.StorageLoanDTO;
import org.folio.innreach.dto.StorageLoanDTOStatus;
import org.folio.innreach.fixture.InnReachTransactionFixture;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;

@ExtendWith(MockitoExtension.class)
class InnReachTransactionActionServiceImplTest {

  private static final UUID LOAN_ID = UUID.randomUUID();

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
  private RetryableUpdateService retryableUpdateService;
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

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(notifier, eventPublisher);
  }

  @Test
  void handleLoanUpdate_positive_itemTransactionRenewedSyncsDueDateWithoutStateChangeOrNotification() {
    var transaction = createTransaction(ITEM, ITEM_SHIPPED);
    var originalDueDate = transaction.getHold().getDueDateTime();
    var newDueDate = new Date();
    var loan = createLoan("renewed", null, newDueDate);

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getHold().getDueDateTime()).isEqualTo(toEpochSec(newDueDate)).isNotEqualTo(originalDueDate);
    assertThat(transaction.getState()).isEqualTo(ITEM_SHIPPED);
  }

  @Test
  void handleLoanUpdate_positive_itemTransactionDueDateChangedSyncsDueDateWithoutStateChangeOrNotification() {
    var transaction = createTransaction(ITEM, ITEM_SHIPPED);
    var newDueDate = new Date();
    var loan = createLoan("dueDateChanged", null, newDueDate);

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getHold().getDueDateTime()).isEqualTo(toEpochSec(newDueDate));
    assertThat(transaction.getState()).isEqualTo(ITEM_SHIPPED);
  }

  @Test
  void handleLoanUpdate_negative_patronTransactionDueDateChangedIsNoOp() {
    var transaction = createTransaction(PATRON, PATRON_HOLD);
    var originalDueDate = transaction.getHold().getDueDateTime();
    var loan = createLoan("dueDateChanged", null, new Date());

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getHold().getDueDateTime()).isEqualTo(originalDueDate);
    assertThat(transaction.getState()).isEqualTo(PATRON_HOLD);
  }

  @Test
  void handleLoanUpdate_negative_localTransactionRenewedIsNoOp() {
    var transaction = createTransaction(LOCAL, LOCAL_HOLD);
    var originalDueDate = transaction.getHold().getDueDateTime();
    var loan = createLoan("renewed", null, new Date());

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getHold().getDueDateTime()).isEqualTo(originalDueDate);
    assertThat(transaction.getState()).isEqualTo(LOCAL_HOLD);
  }

  @Test
  void handleLoanUpdate_negative_localTransactionDueDateChangedIsNoOp() {
    var transaction = createTransaction(LOCAL, LOCAL_HOLD);
    var originalDueDate = transaction.getHold().getDueDateTime();
    var loan = createLoan("dueDateChanged", null, new Date());

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getHold().getDueDateTime()).isEqualTo(originalDueDate);
    assertThat(transaction.getState()).isEqualTo(LOCAL_HOLD);
  }

  @Test
  void handleLoanUpdate_negative_patronTransactionRenewedOnOwnerRenewStateIsSkipped() {
    var transaction = createTransaction(PATRON, OWNER_RENEW);
    var originalDueDate = transaction.getHold().getDueDateTime();
    var loan = createLoan("renewed", null, new Date());

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getHold().getDueDateTime()).isEqualTo(originalDueDate);
    assertThat(transaction.getState()).isEqualTo(OWNER_RENEW);
  }

  @Test
  void handleLoanUpdate_positive_patronTransactionRenewedSetsBorrowerRenewAndNotifies() {
    var transaction = createTransaction(PATRON, PATRON_HOLD);
    var newDueDate = new Date();
    var loan = createLoan("renewed", null, newDueDate);
    var expectedDueDateSec = toEpochSec(newDueDate);

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getState()).isEqualTo(BORROWER_RENEW);
    assertThat(transaction.getHold().getDueDateTime()).isEqualTo(expectedDueDateSec);
    verify(notifier).reportBorrowerRenew(transaction, expectedDueDateSec);
  }

  @Test
  void handleLoanUpdate_negative_unmappedLoanActionIsSafeNoOp() {
    var transaction = createTransaction(ITEM, ITEM_SHIPPED);
    var originalDueDate = transaction.getHold().getDueDateTime();
    var loan = createLoan("checkedout", null, new Date());

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getHold().getDueDateTime()).isEqualTo(originalDueDate);
    assertThat(transaction.getState()).isEqualTo(ITEM_SHIPPED);
  }

  @Test
  void handleLoanUpdate_negative_checkedInActionWithOpenLoanStatusDoesNotCloseTransaction() {
    var transaction = createTransaction(ITEM, ITEM_SHIPPED);
    var originalDueDate = transaction.getHold().getDueDateTime();
    var loan = createLoan("checkedin", "Open", new Date());

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getState()).isEqualTo(ITEM_SHIPPED);
    assertThat(transaction.getHold().getDueDateTime()).isEqualTo(originalDueDate);
  }

  @Test
  void handleLoanUpdate_negative_checkedInActionWithNullLoanStatusDoesNotCloseTransaction() {
    var transaction = createTransaction(ITEM, ITEM_SHIPPED);
    var originalDueDate = transaction.getHold().getDueDateTime();
    var loan = createLoan("checkedin", null, new Date());

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getState()).isEqualTo(ITEM_SHIPPED);
    assertThat(transaction.getHold().getDueDateTime()).isEqualTo(originalDueDate);
  }

  @Test
  void handleLoanUpdate_positive_checkedInActionWithClosedStatusClosesItemTransaction() {
    var transaction = createTransaction(ITEM, ITEM_SHIPPED);
    var loan = createLoan("checkedin", "Closed", new Date());

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getState()).isEqualTo(FINAL_CHECKIN);
    assertThat(transaction.getHold().getDueDateTime()).isNull();
    verify(notifier).reportFinalCheckIn(transaction);
  }

  @Test
  void handleLoanUpdate_positive_checkedInActionWithClosedStatusClosesPatronTransaction() {
    var transaction = createTransaction(PATRON, ITEM_RECEIVED);
    var loan = createLoan("checkedin", "Closed", new Date());

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getState()).isEqualTo(ITEM_IN_TRANSIT);
    assertThat(transaction.getHold().getDueDateTime()).isNull();
    verify(notifier).reportItemInTransit(transaction);
  }

  @Test
  void handleLoanUpdate_negative_checkedInActionOnLocalTransactionIsSkippedRegardlessOfStatus() {
    var transaction = createTransaction(LOCAL, LOCAL_HOLD);
    var originalDueDate = transaction.getHold().getDueDateTime();
    var loan = createLoan("checkedin", "Closed", new Date());

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.of(transaction));

    service.handleLoanUpdate(loan);

    assertThat(transaction.getState()).isEqualTo(LOCAL_HOLD);
    assertThat(transaction.getHold().getDueDateTime()).isEqualTo(originalDueDate);
  }

  @Test
  void handleLoanUpdate_negative_noActiveTransactionFoundIsEarlyReturn() {
    var loan = createLoan("renewed", null, new Date());

    when(transactionRepository.fetchActiveByLoanId(LOAN_ID)).thenReturn(Optional.empty());

    service.handleLoanUpdate(loan);

    verifyNoInteractions(notifier, eventPublisher, loanService, requestService, itemService);
  }

  private InnReachTransaction createTransaction(TransactionType type, TransactionState state) {
    var transaction = InnReachTransactionFixture.createInnReachTransaction(type);
    transaction.setState(state);
    transaction.getHold().setFolioLoanId(LOAN_ID);
    transaction.getHold().setDueDateTime(toEpochSec(new Date(0)));
    return transaction;
  }

  private StorageLoanDTO createLoan(String action, String statusName, Date dueDate) {
    var loan = new StorageLoanDTO();
    loan.setId(LOAN_ID);
    loan.setAction(action);
    loan.setDueDate(dueDate);
    if (statusName != null) {
      loan.setStatus(new StorageLoanDTOStatus().name(statusName));
    }
    return loan;
  }
}
