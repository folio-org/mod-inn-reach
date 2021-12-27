package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionState;
import org.folio.innreach.domain.entity.TransactionHold;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.InnReachTransactionsDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.TransactionStateEnum;
import org.folio.innreach.dto.TransactionTypeEnum;
import org.mapstruct.Builder;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWER_RENEW;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWING_SITE_CANCEL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CLAIMS_RETURNED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.FINAL_CHECKIN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.LOCAL_CHECKOUT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.LOCAL_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.PATRON_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECALL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECEIVE_UNANNOUNCED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RETURN_UNCIRCULATED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.ITEM;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.LOCAL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.PATRON;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public abstract class InnReachTransactionMapper {
  @Autowired
  InnReachTransactionHoldMapper holdMapper;
  @Autowired
  InnReachTransactionPickupLocationMapper innReachTransactionPickupLocationMapper;

  @Mapping(target = "hold", ignore = true)
  @AuditableMapping
  public abstract InnReachTransactionDTO toDTOWithoutHold(InnReachTransaction entity);

  public InnReachTransactionDTO toDTO(InnReachTransaction entity) {
    var dto = toDTOWithoutHold(entity);
    switch (entity.getType()) {
      case ITEM:
        dto.setHold(holdMapper.toItemHoldDTO((TransactionItemHold) entity.getHold()));
        break;
      case LOCAL:
        dto.setHold(holdMapper.toLocalHoldDTO((TransactionLocalHold) entity.getHold()));
        break;
      case PATRON:
        dto.setHold(holdMapper.toPatronHoldDTO((TransactionPatronHold) entity.getHold()));
        break;
      default:
        break;
    }
    return dto;
  }

  public List<InnReachTransactionDTO> toDTOs(Iterable<InnReachTransaction> entities) {
    List<InnReachTransactionDTO> dtos = new LinkedList<>();
    for (InnReachTransaction transaction : entities) {
      var dto = toDTO(transaction);
      dtos.add(dto);
    }
    return dtos;
  }

  public InnReachTransactionsDTO toDTOCollection(Page<InnReachTransaction> pageable) {
    List<InnReachTransactionDTO> dtos = emptyIfNull(toDTOs(pageable));

    return new InnReachTransactionsDTO().transactions(dtos).totalRecords((int) pageable.getTotalElements());
  }

  public InnReachTransaction toEntity(InnReachTransactionDTO transactionDTO) {
    var type = transactionDTO.getType();
    var transaction = new InnReachTransaction();

    transaction.setId( transactionDTO.getId());
    transaction.setTrackingId( transactionDTO.getTrackingId());
    transaction.setCentralServerCode( transactionDTO.getCentralServerCode());
    transaction.setType( getEntityTransactionType( type));
    transaction.setState( getEntityTransactionState( transactionDTO.getState()));
    transaction.setHold( getEntityTransactionHoldByType( transactionDTO.getHold(), type));

    return transaction;
  }

  private TransactionHold getEntityTransactionHoldByType(TransactionHoldDTO transactionHoldDTO, TransactionTypeEnum transactionTypeEnum) {
    TransactionHold hold = null;

    switch (transactionTypeEnum) {
      case LOCAL: hold = getEntityLocalHold(transactionHoldDTO);
      break;
      case PATRON: hold = getEntityPatronHold(transactionHoldDTO);
      break;
      case ITEM: hold = getEntityItemHold(transactionHoldDTO);
      break;
    }

    return hold;
  }

  private TransactionType getEntityTransactionType(TransactionTypeEnum transactionTypeEnum) {
    TransactionType type = null;

    switch (transactionTypeEnum) {
      case ITEM: type = ITEM;
      break;
      case PATRON: type = PATRON;
      break;
      case LOCAL: type = LOCAL;
      break;
    }

    return type;
  }

  private TransactionState getEntityTransactionState(TransactionStateEnum transactionStateEnum) {
    TransactionState state = null;

    switch (transactionStateEnum) {
      case RECEIVE_UNANNOUNCED: state = RECEIVE_UNANNOUNCED;
      break;
      case RECALL: state = RECALL;
      break;
      case TRANSFER: state = TRANSFER;
      break;
      case ITEM_HOLD: state = ITEM_HOLD;
      break;
      case LOCAL_HOLD: state = LOCAL_HOLD;
      break;
      case PATRON_HOLD: state = PATRON_HOLD;
      break;
      case ITEM_SHIPPED: state = ITEM_SHIPPED;
      break;
      case FINAL_CHECKIN: state = FINAL_CHECKIN;
      break;
      case ITEM_RECEIVED: state = ITEM_RECEIVED;
      break;
      case BORROWER_RENEW: state = BORROWER_RENEW;
      break;
      case CANCEL_REQUEST: state = CANCEL_REQUEST;
      break;
      case LOCAL_CHECKOUT: state = LOCAL_CHECKOUT;
      break;
      case CLAIMS_RETURNED: state = CLAIMS_RETURNED;
      break;
      case ITEM_IN_TRANSIT: state = ITEM_IN_TRANSIT;
      break;
      case RETURN_UNCIRCULATED: state = RETURN_UNCIRCULATED;
      break;
      case BORROWING_SITE_CANCEL: state = BORROWING_SITE_CANCEL;
      break;
    }

    return state;
  }

  private TransactionItemHold getEntityItemHold(TransactionHoldDTO transactionHoldDTO) {
    var hold = new TransactionItemHold();

    hold.setId(transactionHoldDTO.getId());
    hold.setTransactionTime(transactionHoldDTO.getTransactionTime());
    hold.setNeedBefore(transactionHoldDTO.getNeedBefore());
    hold.setItemId(transactionHoldDTO.getItemId());
    hold.setPatronId(transactionHoldDTO.getPatronId());
    hold.setItemAgencyCode(transactionHoldDTO.getItemAgencyCode());
    hold.setPatronAgencyCode(transactionHoldDTO.getPatronAgencyCode());
    hold.setCentralPatronType(transactionHoldDTO.getCentralPatronType());
    hold.setPatronName(transactionHoldDTO.getPatronName());
    hold.setPickupLocation(innReachTransactionPickupLocationMapper.fromString(transactionHoldDTO.getPickupLocation()));
    hold.setFolioHoldingId(transactionHoldDTO.getFolioHoldingId());
    hold.setFolioInstanceId(transactionHoldDTO.getFolioInstanceId());
    hold.setFolioItemId(transactionHoldDTO.getFolioItemId());
    hold.setFolioPatronId(transactionHoldDTO.getFolioPatronId());
    hold.setFolioLoanId(transactionHoldDTO.getFolioLoanId());
    hold.setFolioRequestId(transactionHoldDTO.getFolioRequestId());
    hold.setFolioItemBarcode(transactionHoldDTO.getFolioItemBarcode());
    hold.setFolioPatronBarcode(transactionHoldDTO.getFolioPatronBarcode());
    hold.setCentralItemType(transactionHoldDTO.getCentralItemType());

    return hold;
  }

  private TransactionPatronHold getEntityPatronHold(TransactionHoldDTO transactionHoldDTO) {
    var hold = new TransactionPatronHold();

    hold.setId(transactionHoldDTO.getId());
    hold.setAuthor(transactionHoldDTO.getAuthor());
    hold.setTransactionTime(transactionHoldDTO.getTransactionTime());
    hold.setNeedBefore(transactionHoldDTO.getNeedBefore());
    hold.setItemId(transactionHoldDTO.getItemId());
    hold.setPatronId(transactionHoldDTO.getPatronId());
    hold.setItemAgencyCode(transactionHoldDTO.getItemAgencyCode());
    hold.setPatronAgencyCode(transactionHoldDTO.getPatronAgencyCode());
    hold.setCallNumber(transactionHoldDTO.getCallNumber());
    hold.setPickupLocation(innReachTransactionPickupLocationMapper.fromString(transactionHoldDTO.getPickupLocation()));
    hold.setFolioHoldingId(transactionHoldDTO.getFolioHoldingId());
    hold.setFolioInstanceId(transactionHoldDTO.getFolioInstanceId());
    hold.setFolioItemId(transactionHoldDTO.getFolioItemId());
    hold.setFolioPatronId(transactionHoldDTO.getFolioPatronId());
    hold.setShippedItemBarcode(transactionHoldDTO.getShippedItemBarcode());
    hold.setFolioLoanId(transactionHoldDTO.getFolioLoanId());
    hold.setFolioRequestId(transactionHoldDTO.getFolioRequestId());
    hold.setFolioItemBarcode(transactionHoldDTO.getFolioItemBarcode());
    hold.setFolioPatronBarcode(transactionHoldDTO.getFolioPatronBarcode());
    hold.setTitle(transactionHoldDTO.getTitle());
    hold.setCentralItemType(transactionHoldDTO.getCentralItemType());

    return hold;
  }

  private TransactionLocalHold getEntityLocalHold(TransactionHoldDTO transactionHoldDTO) {
    var hold = new TransactionLocalHold();

    hold.setId(transactionHoldDTO.getId());
    hold.setAuthor(transactionHoldDTO.getAuthor());
    hold.setTransactionTime(transactionHoldDTO.getTransactionTime());
    hold.setNeedBefore(transactionHoldDTO.getNeedBefore());
    hold.setItemId(transactionHoldDTO.getItemId());
    hold.setPatronName(transactionHoldDTO.getPatronName());
    hold.setPatronId(transactionHoldDTO.getPatronId());
    hold.setItemAgencyCode(transactionHoldDTO.getItemAgencyCode());
    hold.setPatronAgencyCode(transactionHoldDTO.getPatronAgencyCode());
    hold.setCallNumber(transactionHoldDTO.getCallNumber());
    hold.setPickupLocation(innReachTransactionPickupLocationMapper.fromString(transactionHoldDTO.getPickupLocation()));
    hold.setPatronHomeLibrary(transactionHoldDTO.getPatronHomeLibrary());
    hold.setFolioHoldingId(transactionHoldDTO.getFolioHoldingId());
    hold.setFolioInstanceId(transactionHoldDTO.getFolioInstanceId());
    hold.setFolioItemId(transactionHoldDTO.getFolioItemId());
    hold.setFolioPatronId(transactionHoldDTO.getFolioPatronId());
    hold.setFolioLoanId(transactionHoldDTO.getFolioLoanId());
    hold.setPatronPhone(transactionHoldDTO.getPatronPhone());
    hold.setFolioRequestId(transactionHoldDTO.getFolioRequestId());
    hold.setFolioItemBarcode(transactionHoldDTO.getFolioItemBarcode());
    hold.setFolioPatronBarcode(transactionHoldDTO.getFolioPatronBarcode());
    hold.setTitle(transactionHoldDTO.getTitle());
    hold.setCentralItemType(transactionHoldDTO.getCentralItemType());

    return hold;
  }
}
