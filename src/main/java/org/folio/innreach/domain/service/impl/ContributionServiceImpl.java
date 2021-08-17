package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.dto.folio.inventory.MaterialTypeDTO;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionsDTO;
import org.folio.innreach.dto.MappingValidationStatusDTO;
import org.folio.innreach.mapper.ContributionMapper;
import org.folio.innreach.repository.ContributionRepository;

@Log4j2
@AllArgsConstructor
@Service
public class ContributionServiceImpl implements ContributionService {

  private static final String MATERIAL_TYPES_CQL = "cql.allRecords=1";
  private static final int MATERIAL_TYPES_LIMIT = 2000;

  private final ContributionRepository repository;
  private final ContributionMapper mapper;
  private final InventoryClient inventoryClient;
  private final MaterialTypeMappingService typeMappingService;

  @Override
  public ContributionDTO getCurrent(UUID centralServerId) {
    var entity = repository.fetchCurrentByCentralServerId(centralServerId)
      .orElse(emptyContribution(centralServerId));

    var contribution = mapper.toDTO(entity);

    contribution.setItemTypeMappingStatus(validateTypeMappings(centralServerId));

    return contribution;
  }

  private Contribution emptyContribution(UUID centralServerId) {
    var contribution = new Contribution();
    contribution.setCentralServer(ServiceUtils.centralServerRef(centralServerId));
    return contribution;
  }

  @Override
  public ContributionsDTO getHistory(UUID centralServerId, int offset, int limit) {
    var page = repository.fetchHistoryByCentralServerId(centralServerId, PageRequest.of(offset, limit));
    return mapper.toDTOCollection(page);
  }

  private MappingValidationStatusDTO validateTypeMappings(UUID centralServerId) {
    try {
      List<UUID> typeIds = inventoryClient.getMaterialTypes(MATERIAL_TYPES_CQL, MATERIAL_TYPES_LIMIT).getResult()
        .stream()
        .map(MaterialTypeDTO::getId)
        .collect(Collectors.toList());

      long mappedTypesCounter = typeMappingService.countByTypeIds(centralServerId, typeIds);

      return mappedTypesCounter == typeIds.size() ? MappingValidationStatusDTO.VALID : MappingValidationStatusDTO.INVALID;
    } catch (Exception e) {
      log.warn("Can't validate material type mappings", e);
      return MappingValidationStatusDTO.INVALID;
    }
  }

}
