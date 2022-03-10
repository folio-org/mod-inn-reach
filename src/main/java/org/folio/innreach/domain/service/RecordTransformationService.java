package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.external.dto.BibItem;

public interface RecordTransformationService {

  BibInfo getBibInfo(UUID centralServerId, Instance instance);

  List<BibItem> getBibItems(UUID centralServerId, List<Item> items, BiConsumer<Item, Exception> errorHandler);
}
