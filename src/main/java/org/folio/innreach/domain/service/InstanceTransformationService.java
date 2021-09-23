package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.dto.Instance;

public interface InstanceTransformationService {

  BibInfo getBibInfo(UUID centralServerId, Instance instance);

}
