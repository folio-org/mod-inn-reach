package org.folio.innreach.domain.service.impl;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.equalsAny;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import static org.folio.innreach.external.dto.InnReachResponse.Error.ofMessage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.client.AutomatedPatronBlocksClient;
import org.folio.innreach.client.ManualPatronBlocksClient;
import org.folio.innreach.client.PatronClient;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.patron.PatronDTO;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.domain.service.PatronInfoService;
import org.folio.innreach.domain.service.PatronTypeMappingService;
import org.folio.innreach.domain.service.UserCustomFieldMappingService;
import org.folio.innreach.dto.LocalAgencyDTO;
import org.folio.innreach.dto.PatronInfo;
import org.folio.innreach.dto.PatronInfoResponseDTO;
import org.folio.innreach.external.dto.PatronInfoResponse;
import org.folio.innreach.external.mapper.InnReachResponseMapper;

@Log4j2
@Service
@RequiredArgsConstructor
public class PatronInfoServiceImpl implements PatronInfoService {

  public static final String ERROR_REASON = "Unable to verify patron";

  private final UserServiceImpl userService;
  private final PatronTypeMappingService patronTypeMappingService;
  private final CentralServerService centralServerService;
  private final InnReachTransactionService transactionService;
  private final UserCustomFieldMappingService customFieldMappingService;
  private final PatronClient patronClient;
  private final AutomatedPatronBlocksClient automatedPatronBlocksClient;
  private final ManualPatronBlocksClient manualPatronBlocksClient;
  private final InnReachResponseMapper mapper;

  @Override
  public PatronInfoResponseDTO verifyPatron(String centralServerCode, String visiblePatronId,
                                            String patronAgencyCode, String patronName) {
    PatronInfoResponse response;
    try {
      var centralServer = centralServerService.getCentralServerByCentralCode(centralServerCode);
      var centralServerId = centralServer.getId();
      var localAgencies = emptyIfNull(centralServer.getLocalAgencies());
      var user = findPatronUser(visiblePatronId, patronName);

      var patronInfo = getPatronInfo(centralServerId, localAgencies, user);

      var requestAllowed = requestAllowed(user);
      response = PatronInfoResponse.of(patronInfo, requestAllowed);
    } catch (Exception e) {
      log.warn(ERROR_REASON, e);
      response = PatronInfoResponse.error(ERROR_REASON, ofMessage(centralServerCode, e.getMessage()));
    }
    return mapper.toDto(response);
  }

  private PatronInfo getPatronInfo(UUID centralServerId, List<LocalAgencyDTO> agencies, User user) {
    var centralPatronType = getCentralPatronType(centralServerId, user);
    var patronId = getPatronId(user);
    var patron = getPatron(user);
    var patronName = getPatronName(user);
    var totalLoans = patron.getTotalLoans();
    var innReachLoans = countInnReachLoans(patronId, patron.getLoans());
    var expirationDate = ofNullable(user.getExpirationDate()).map(OffsetDateTime::toEpochSecond).orElse(null);
    var patronAgencyCode = getPatronAgencyCode(centralServerId, agencies, user);

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

  private User findPatronUser(String visiblePatronId, String patronName) {
    var user = userService.getUserByPublicId(visiblePatronId).orElse(null);

    Assert.isTrue(user != null, "Patron is not found by visiblePatronId: " + visiblePatronId);
    Assert.isTrue(user.isActive(), "Patron is not active");
    Assert.isTrue(matchName(user, patronName), "Patron is not found by name: " + patronName);

    return user;
  }

  private boolean requestAllowed(User user) {
    var blocks = automatedPatronBlocksClient.getPatronBlocks(user.getId()).getResult();
    if (hasAutomatedBlocks(blocks)) {
      return false;
    }

    var manualBlocks = manualPatronBlocksClient.getPatronBlocks(user.getId()).getResult();
    if (hasManualBlocks(manualBlocks)) {
      return false;
    }

    return true;
  }

  private boolean hasAutomatedBlocks(List<AutomatedPatronBlocksClient.AutomatedPatronBlock> blocks) {
    return CollectionUtils.emptyIfNull(blocks).stream()
      .filter(b -> TRUE.equals(b.getBlockBorrowing()) || TRUE.equals(b.getBlockRequests()))
      .findFirst()
      .isPresent();
  }

  private boolean hasManualBlocks(List<ManualPatronBlocksClient.ManualPatronBlock> blocks) {
    return CollectionUtils.emptyIfNull(blocks).stream()
      .filter(b -> TRUE.equals(b.getBorrowing()) || TRUE.equals(b.getRequests()))
      .findFirst()
      .isPresent();
  }

  private String getPatronId(User user) {
    return user.getId().replace("-", "");
  }

  private PatronDTO getPatron(User user) {
    return patronClient.getAccountDetails(user.getId());
  }

  private Integer countInnReachLoans(String patronId, List<PatronDTO.Loan> loans) {
    var loanIds = loans.stream().map(PatronDTO.Loan::getId).collect(Collectors.toList());
    return transactionService.countInnReachLoans(patronId, loanIds);
  }

  private Integer getCentralPatronType(UUID centralServerId, User user) {
    return patronTypeMappingService.getCentralPatronType(centralServerId, user.getPatronGroupId())
      .orElseThrow(() -> new IllegalStateException(
        "centralPatronType is not resolved for patron with public id: " + user.getBarcode()));
  }

  private String getPatronAgencyCode(UUID centralServerId, List<LocalAgencyDTO> agencies, User user) {
    String agencyCode = null;
    try {
      var patronAgencyMapping = customFieldMappingService.getMapping(centralServerId);

      var fieldRefId = patronAgencyMapping.getCustomFieldId();
      var libraryOptionId = user.getCustomFields().get(fieldRefId);
      Assert.isTrue(libraryOptionId != null, "User home library setting is not found by refId: " + fieldRefId);

      agencyCode = patronAgencyMapping.getConfiguredOptions().get(libraryOptionId);
    } catch (Exception e) {
      log.warn("Patron agency mapping for central server {} is not found", centralServerId, e);
    }

    // if no mapping found and only one agency code is hosted on the server, it should default to that
    if (agencyCode == null && agencies.size() == 1) {
      agencyCode = agencies.get(0).getCode();
    }

    Assert.isTrue(agencyCode != null, "Patron agency code is not resolved");

    return agencyCode;
  }

  private static boolean matchName(User user, String patronName) {
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
    return equalsAny(patronNameTokens[0], personal.getFirstName(), personal.getPreferredFirstName()) &&
      patronNameTokens[1].equals(personal.getMiddleName()) &&
      patronNameTokens[2].equals(personal.getLastName());
  }

  private static String getPatronName(User user) {
    var personal = user.getPersonal();

    var nameBuilder = new StringBuilder(personal.getLastName())
      .append(", ")
      .append(defaultIfEmpty(personal.getPreferredFirstName(), personal.getFirstName()));

    if (isNotEmpty(personal.getMiddleName())) {
      nameBuilder.append(" ").append(personal.getMiddleName());
    }

    return nameBuilder.toString();
  }

}
