package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.PagingSlipTemplateDTO;
import org.folio.innreach.dto.PagingSlipTemplatesDTO;

public interface PagingSlipTemplateService {
  PagingSlipTemplateDTO getByCentralServerId(UUID centralServerId);

  PagingSlipTemplatesDTO getAllTemplates();

  PagingSlipTemplateDTO update(UUID centralServerId, PagingSlipTemplateDTO dto);
}
