package org.folio.innreach.domain.service.impl;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.equalsAny;

import static org.folio.innreach.external.dto.InnReachResponse.Error.ofMessage;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.client.PatronBlocksClient;
import org.folio.innreach.client.PatronClient;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.patron.PatronBlock;
import org.folio.innreach.domain.service.CentralPatronTypeMappingService;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.domain.service.PatronInfoService;
import org.folio.innreach.domain.service.PatronTypeMappingService;
import org.folio.innreach.dto.PatronInfo;
import org.folio.innreach.dto.PatronInfoResponseDTO;
import org.folio.innreach.external.dto.PatronInfoResponse;
import org.folio.innreach.external.mapper.InnReachResponseMapper;

@Log4j2
@Service
@RequiredArgsConstructor
public class PatronInfoServiceImpl implements PatronInfoService {

  private static final String ERROR_REASON = "Unable to verify patron";

  private final UserServiceImpl userService;
  private final CentralPatronTypeMappingService centralPatronTypeMappingService;
  private final PatronTypeMappingService patronTypeMappingService;
  private final CentralServerService centralServerService;
  private final InnReachTransactionService transactionService;
  private final PatronClient patronClient;
  private final PatronBlocksClient patronBlocksClient;
  private final InnReachResponseMapper mapper;

  @Override
  public PatronInfoResponseDTO verifyPatron(String centralServerCode, String visiblePatronId,
                                            String patronAgencyCode, String patronName) {
    PatronInfoResponse response;
    try {
      var centralServerId = centralServerService.getCentralServerByCentralCode(centralServerCode).getId();

      var user = userService.getUserByPublicId(visiblePatronId).orElse(null);

      Assert.isTrue(user != null, "Patron is not found");
      Assert.isTrue(user.isActive(), "Patron is not active");
      Assert.isTrue(matchName(user, patronName), "Patron is not found by name: " + patronName);
      verifyPatronBlocks(user);

      var patronInfo = getPatronInfo(centralServerId, user, patronAgencyCode, patronName);

      response = PatronInfoResponse.of(patronInfo);
    } catch (Exception e) {
      log.warn(ERROR_REASON, e);
      response = PatronInfoResponse.error(ERROR_REASON, ofMessage(centralServerCode, e.getMessage()));
    }
    return mapper.toDto(response);
  }

  private PatronInfo getPatronInfo(UUID centralServerId, User user, String patronAgencyCode, String patronName) {
    var patronId = getPatronId(user);
    var centralPatronType = getCentralPatronType(centralServerId, user);
    var innReachLoans = countInnReachLoans(patronId);
    var totalLoans = countLoans(user);
    var expirationDate = ofNullable(user.getExpirationDate()).map(OffsetDateTime::toEpochSecond).orElse(null);

    var patronInfo = new PatronInfo();
    patronInfo.setPatronId(patronId);
    patronInfo.setCentralPatronType(centralPatronType);
    patronInfo.setPatronAgencyCode(patronAgencyCode);
    patronInfo.setNonLocalLoans(innReachLoans);
    patronInfo.setLocalLoans(totalLoans - innReachLoans);
    patronInfo.setPatronExpireDate(expirationDate);
    patronInfo.setPatronName(patronName);
    return patronInfo;
  }

  private void verifyPatronBlocks(User user) {
    var blocks = patronBlocksClient.getPatronBlocks(user.getId()).getResult();

    if (CollectionUtils.isNotEmpty(blocks)) {
      var block = blocks.stream()
        .filter(PatronBlock::getBlockBorrowing)
        .filter(PatronBlock::getBlockRequests)
        .findFirst()
        .orElse(null);

      Assert.isTrue(block == null, "Patron block found: " + block.getMessage());
    }
  }

  private String getPatronId(User user) {
    return user.getId().replaceAll("-", "");
  }

  private Integer countLoans(User user) {
    return patronClient.getAccountDetails(user.getId())
      .getTotalLoans();
  }

  private Integer countInnReachLoans(String visiblePatronId) {
    return transactionService.countLoansByPatronId(visiblePatronId);
  }

  private Integer getCentralPatronType(UUID centralServerId, User user) {
    var userPatronType = centralPatronTypeMappingService.getCentralPatronType(centralServerId, user.getBarcode());

    return userPatronType != null ? userPatronType :
      patronTypeMappingService.getCentralPatronType(centralServerId, user.getPatronGroupId());
  }

  private boolean matchName(User user, String patronName) {
    var personal = user.getPersonal();
    String[] patronNameTokens = patronName.split("\\s");

    if (patronNameTokens.length < 2) {
      return false;
    } else if (patronNameTokens.length == 2) {
      // "First Last" or "Middle Last" format
      return equalsAny(patronNameTokens[0], personal.getFirstName(), personal.getPreferredFirstName(), personal.getMiddleName()) &&
        patronNameTokens[1].equals(personal.getLastName());
    }

    // "First Middle Last" format
    return equalsAny(patronNameTokens[0], personal.getFirstName(), personal.getPreferredFirstName())  &&
      patronNameTokens[1].equals(personal.getMiddleName()) &&
      patronNameTokens[2].equals(personal.getLastName());
  }

}
