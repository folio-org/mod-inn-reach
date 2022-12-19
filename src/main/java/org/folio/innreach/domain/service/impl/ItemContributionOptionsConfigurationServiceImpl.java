package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.ItemContributionOptionsConfiguration;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.ItemContributionOptionsConfigurationService;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;
import org.folio.innreach.mapper.ItemContributionOptionsConfigurationMapper;
import org.folio.innreach.repository.ItemContributionOptionsConfigurationRepository;

@Log4j2
@RequiredArgsConstructor
@Service
public class ItemContributionOptionsConfigurationServiceImpl implements ItemContributionOptionsConfigurationService {

  private final ItemContributionOptionsConfigurationRepository repository;
  private final ItemContributionOptionsConfigurationMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public ItemContributionOptionsConfigurationDTO getItmContribOptConf(UUID centralServerId) {
    log.debug("getItmContribOptConf:: parameters centralServerId: {}", centralServerId);
    var itmContribOptConf = findItmContribOptConf(centralServerId);
    log.info("getItmContribOptConf:: result: {}", mapper.toDto(itmContribOptConf));
    return mapper.toDto(itmContribOptConf);
  }

  @Override
  public ItemContributionOptionsConfigurationDTO createItmContribOptConf(UUID centralServerId, ItemContributionOptionsConfigurationDTO itmContribOptConfDTO) {
    log.debug("createItmContribOptConf:: parameters centralServerId: {}, itmContribOptConfDTO: {}", centralServerId, itmContribOptConfDTO);
    var itmContribOptConf = mapper.toEntity(itmContribOptConfDTO);
    itmContribOptConf.setCentralServer(centralServerRef(centralServerId));
    var createdItmContribOptConf = repository.save(itmContribOptConf);
    log.info("createItmContribOptConf:: result: {}", mapper.toDto(createdItmContribOptConf));
    return mapper.toDto(createdItmContribOptConf);
  }

  @Override
  public ItemContributionOptionsConfigurationDTO updateItmContribOptConf(UUID centralServerId, ItemContributionOptionsConfigurationDTO itmContribOptConfDTO) {
    log.debug("updateItmContribOptConf:: parameters centralServerId: {}, itmContribOptConfDTO: {}", centralServerId, itmContribOptConfDTO);
    var itmContribOptConf = findItmContribOptConf(centralServerId);
    var updatedItmContribOptConf = mapper.toEntity(itmContribOptConfDTO);
    updateItmContribOptConf(itmContribOptConf, updatedItmContribOptConf);

    repository.save(itmContribOptConf);

    log.info("updateItmContribOptConf:: result: {}", mapper.toDto(itmContribOptConf));
    return mapper.toDto(itmContribOptConf);
  }

  private void updateItmContribOptConf(ItemContributionOptionsConfiguration itmContribOptConf, ItemContributionOptionsConfiguration updatedItmContribOptConf) {
    itmContribOptConf.setNotAvailableItemStatuses(updatedItmContribOptConf.getNotAvailableItemStatuses());
    itmContribOptConf.setNonLendableLoanTypes(updatedItmContribOptConf.getNonLendableLoanTypes());
    itmContribOptConf.setNonLendableLocations(updatedItmContribOptConf.getNonLendableLocations());
    itmContribOptConf.setNonLendableMaterialTypes(updatedItmContribOptConf.getNonLendableMaterialTypes());
  }

  private ItemContributionOptionsConfiguration findItmContribOptConf(UUID centralServerId) {
    log.debug("findItmContribOptConf:: parameters centralServerId: {}", centralServerId);
    return repository.findOne(exampleWithServerId(centralServerId))
      .orElseThrow(() -> new EntityNotFoundException("Item Contribution Options Configuration not found: " +
        "centralServerId = " + centralServerId));
  }

  private static Example<ItemContributionOptionsConfiguration> exampleWithServerId(UUID centralServerId) {
    var toFind = new ItemContributionOptionsConfiguration();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }
}
