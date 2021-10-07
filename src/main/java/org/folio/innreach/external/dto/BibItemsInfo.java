package org.folio.innreach.external.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class BibItemsInfo {
  private List<BibItem> itemInfo;
}
