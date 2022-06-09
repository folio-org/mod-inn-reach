package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.PagingSlipsDTO;

public interface PagingSlipService {

  PagingSlipsDTO getPagingSlipsByServicePoint(UUID servicePointId);
}
