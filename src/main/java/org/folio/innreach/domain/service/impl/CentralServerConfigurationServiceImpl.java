package org.folio.innreach.domain.service.impl;

import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import static org.folio.innreach.external.dto.InnReachResponse.OK_STATUS;
import static org.folio.innreach.util.ListUtils.flatMapItems;
import static org.folio.innreach.util.ListUtils.toStream;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.service.CentralServerConfigurationService;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.dto.AgenciesPerCentralServerDTO;
import org.folio.innreach.dto.Agency;
import org.folio.innreach.dto.CentralItemTypesDTO;
import org.folio.innreach.dto.CentralPatronTypesDTO;
import org.folio.innreach.dto.CentralServerAgenciesDTO;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.CentralServerItemTypesDTO;
import org.folio.innreach.dto.CentralServerPatronTypesDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.ItemType;
import org.folio.innreach.dto.ItemTypesPerCentralServerDTO;
import org.folio.innreach.dto.LocalServer;
import org.folio.innreach.dto.LocalServerAgenciesDTO;
import org.folio.innreach.dto.PatronType;
import org.folio.innreach.dto.PatronTypesPerCentralServerDTO;
import org.folio.innreach.external.exception.InnReachException;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.util.JsonHelper;

@Log4j2
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class CentralServerConfigurationServiceImpl implements CentralServerConfigurationService {

  private static final String INN_REACH_LOCAL_SERVERS_URI = "/contribution/localservers";
  private static final String INN_REACH_ITEM_TYPES_URI = "/contribution/itemtypes";
  private static final String INN_REACH_PATRON_TYPES_URI = "/circ/patrontypes";

  private final CentralServerService centralServerService;
  private final InnReachExternalService innReachService;
  private final JsonHelper jsonHelper;


  @Override
  public CentralServerAgenciesDTO getAllAgencies() {
    var agencies = loadRecordsPerServer(INN_REACH_LOCAL_SERVERS_URI, LocalServerAgenciesDTO.class, this::toAgenciesOrNull);

    return new CentralServerAgenciesDTO()
        .centralServerAgencies(agencies)
        .totalRecords(agencies.size());
  }

  @Override
  public CentralServerItemTypesDTO getAllItemTypes() {
    var csItemTypes = loadRecordsPerServer(INN_REACH_ITEM_TYPES_URI, CentralItemTypesDTO.class, this::toItemTypesOrNull);

    return new CentralServerItemTypesDTO()
        .centralServerItemTypes(csItemTypes)
        .totalRecords(csItemTypes.size());
  }

  @Override
  public CentralServerPatronTypesDTO getAllPatronTypes() {
    var csPatronTypes = loadRecordsPerServer(INN_REACH_PATRON_TYPES_URI, CentralPatronTypesDTO.class,
                            this::toPatronTypesOrNull);

    return new CentralServerPatronTypesDTO()
        .centralServerPatronTypes(csPatronTypes)
        .totalRecords(csPatronTypes.size());
  }

  @Override
  public List<LocalServer> getLocalServers(UUID centralServerId) {
    return loadRecordPerServer(INN_REACH_LOCAL_SERVERS_URI, LocalServerAgenciesDTO.class,
      resp -> emptyIfNull(resp.getRight().getLocalServerList()), centralServerId);
  }

  private <Rec, CSResp extends InnReachResponseDTO> List<Rec> loadRecordsPerServer(String uri,
      Class<CSResp> centralServerRecordType, Function<Pair<CentralServerDTO, CSResp>, Rec> responseToRecordsMapper) {

    var servers = centralServerService.getAllCentralServers(0, Integer.MAX_VALUE).getCentralServers();

    return servers.stream()
        .map(retrieveAllConfigRecords(uri, centralServerRecordType))
        .filter(this::successfulResponse)
        .map(responseToRecordsMapper)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private <Rec, CSResp extends InnReachResponseDTO> Rec loadRecordPerServer(String uri,
    Class<CSResp> centralServerRecordType,
    Function<Pair<CentralServerDTO, CSResp>, Rec> responseToRecordsMapper,
    UUID centralServerId) {

    var server = centralServerService.getCentralServer(centralServerId);

    return Optional.of(server)
      .map(retrieveAllConfigRecords(uri, centralServerRecordType))
      .filter(this::successfulResponse)
      .map(responseToRecordsMapper)
      .filter(Objects::nonNull)
      .orElseThrow(() -> new IllegalStateException("Unable to load records for central server " + centralServerId));
  }

  private AgenciesPerCentralServerDTO toAgenciesOrNull(
      Pair<CentralServerDTO, LocalServerAgenciesDTO> centralServerWithResponse) {
    var agencies = flatMapItems(centralServerWithResponse.getRight().getLocalServerList(),
                                localServer -> toStream(localServer.getAgencyList()));
    var cs = centralServerWithResponse.getLeft();

    log.info("Number of agencies received: {}", agencies.size());

    return isNotEmpty(agencies) ? createAgencies(cs, agencies) : null;
  }

  private ItemTypesPerCentralServerDTO toItemTypesOrNull(
      Pair<CentralServerDTO, CentralItemTypesDTO> centralServerWithResponse) {
    var itList = emptyIfNull(centralServerWithResponse.getRight().getItemTypeList());
    var cs = centralServerWithResponse.getLeft();

    log.info("Number of item types received: {}", itList.size());

    return isNotEmpty(itList) ? createItemTypes(cs, itList) : null;
  }

  private PatronTypesPerCentralServerDTO toPatronTypesOrNull(
      Pair<CentralServerDTO, CentralPatronTypesDTO> centralServerWithResponse) {
    var ptList = emptyIfNull(centralServerWithResponse.getRight().getPatronTypeList());
    var cs = centralServerWithResponse.getLeft();

    log.info("Number of patron types received: {}", ptList.size());

    return isNotEmpty(ptList) ? createPatronTypes(cs, ptList) : null;
  }

  private <CSResp extends InnReachResponseDTO> Function<CentralServerDTO, Pair<CentralServerDTO, CSResp>> retrieveAllConfigRecords(
      String uri, Class<CSResp> recordType) {
    return centralServer -> {
      log.info("Retrieving {} from central server: code = {}", recordType.getSimpleName(),
          centralServer.getCentralServerCode());

      try {
        String response = innReachService.callInnReachApi(centralServer.getId(), uri);

        return Pair.of(centralServer, jsonHelper.fromJson(response, recordType));
      } catch (InnReachException | BadCredentialsException | IllegalStateException e) {
        log.warn("Failed to get {} from central server: code = {}", recordType.getSimpleName(),
            centralServer.getCentralServerCode(), e);

        return Pair.of(centralServer, null);
      }
    };
  }

  private <CSResp extends InnReachResponseDTO> boolean successfulResponse(
      Pair<CentralServerDTO, CSResp> centralServerWithResponse) {
    var cs = centralServerWithResponse.getLeft();
    var resp = centralServerWithResponse.getRight();

    if (resp != null && !isOk(resp)) {
      log.warn("Failed to get configuration records from central server: code = {}. Inn-reach response: {}",
          cs.getCentralServerCode(), resp);
    }

    return resp != null && isOk(resp);
  }

  private AgenciesPerCentralServerDTO createAgencies(CentralServerDTO cs, List<Agency> agencies) {
    return new AgenciesPerCentralServerDTO()
        .centralServerId(cs.getId())
        .centralServerCode(cs.getCentralServerCode())
        .agencies(agencies);
  }

  private ItemTypesPerCentralServerDTO createItemTypes(CentralServerDTO cs, List<ItemType> itemTypes) {
    return new ItemTypesPerCentralServerDTO()
        .centralServerCode(cs.getCentralServerCode())
        .centralServerId(cs.getId())
        .itemTypes(itemTypes);
  }

  private PatronTypesPerCentralServerDTO createPatronTypes(CentralServerDTO cs, List<PatronType> patronTypes) {
    return new PatronTypesPerCentralServerDTO()
        .centralServerCode(cs.getCentralServerCode())
        .centralServerId(cs.getId())
        .patronTypes(patronTypes);
  }

  private static boolean isOk(InnReachResponseDTO innReachResponse) {
    return OK_STATUS.equals(innReachResponse.getStatus()) && CollectionUtils.isEmpty(innReachResponse.getErrors());
  }

}
