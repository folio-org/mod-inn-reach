package org.folio.innreach.domain.service.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.folio.innreach.domain.entity.PagingSlipTemplate;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.PagingSlipTemplateService;
import org.folio.innreach.dto.PagingSlipTemplateDTO;
import org.folio.innreach.mapper.PagingSlipTemplateMapper;
import org.folio.innreach.repository.PagingSlipTemplateRepository;

@RequiredArgsConstructor
@Service
public class PagingSlipTemplateServiceImpl implements PagingSlipTemplateService {

  private final PagingSlipTemplateRepository repository;
  private final PagingSlipTemplateMapper mapper;

  @Override
  public PagingSlipTemplateDTO getByCentralServerId(UUID centralServerId) {
    var template = findTemplate(centralServerId);
    return mapper.toDTO(template);
  }

  @Override
  public PagingSlipTemplateDTO update(UUID centralServerId, PagingSlipTemplateDTO dto) {
    if (isNullOrEmpty(dto)) {
      return delete(centralServerId);
    }

    var incoming = mapper.toEntityWithRefs(dto, centralServerId);

    var updated = fetchOne(centralServerId)
      .map(mergeFunc(incoming))
      .orElse(incoming);

    repository.saveAndFlush(updated);

    return mapper.toDTO(updated);
  }

  private Optional<PagingSlipTemplate> fetchOne(UUID centralServerId) {
    return repository.fetchOneByCentralServerId(centralServerId);
  }

  private Function<PagingSlipTemplate, PagingSlipTemplate> mergeFunc(PagingSlipTemplate incoming) {
    return existing -> {
      existing.setDescription(incoming.getDescription());
      existing.setTemplate(incoming.getTemplate());

      return existing;
    };
  }

  private PagingSlipTemplateDTO delete(UUID centralServerId) {
    var template = findTemplate(centralServerId);
    repository.delete(template);
    return mapper.toDTO(template);
  }

  private PagingSlipTemplate findTemplate(UUID centralServerId) {
    return repository.fetchOneByCentralServerId(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException("Paging Slip Template not found: centralServerId = " + centralServerId));
  }

  private boolean isNullOrEmpty(PagingSlipTemplateDTO dto) {
    return dto == null ||
        (isBlank(dto.getDescription()) && isBlank(dto.getTemplate()));
  }
}
