package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;

import java.util.UUID;

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
  public PagingSlipTemplateDTO create(UUID centralServerId, PagingSlipTemplateDTO dto) {
    var entity = mapper.toEntity(dto);
    entity.setCentralServer(centralServerRef(centralServerId));

    var saved = repository.save(entity);

    return mapper.toDTO(saved);
  }

  @Override
  public PagingSlipTemplateDTO update(UUID centralServerId, PagingSlipTemplateDTO dto) {
    var template = findTemplate(centralServerId);

    template.setDescription(dto.getDescription());
    template.setTemplate(dto.getTemplate());

    repository.save(template);

    return mapper.toDTO(template);
  }

  @Override
  public void delete(UUID centralServerId) {
    var template = findTemplate(centralServerId);
    repository.delete(template);
  }

  private PagingSlipTemplate findTemplate(UUID centralServerId) {
    return repository.fetchOneByCentralServerId(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException("Paging Slip Template not found: centralServerId = " + centralServerId));
  }
}
