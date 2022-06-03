package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.PagingSlipTemplateDTO;

public interface PagingSlipTemplateService {
    PagingSlipTemplateDTO getByCentralServerId(UUID centralServerId);

    PagingSlipTemplateDTO create(UUID centralServerId, PagingSlipTemplateDTO dto);

    PagingSlipTemplateDTO update(UUID centralServerId, PagingSlipTemplateDTO dto);

    void delete(UUID centralServerId);
}
