package org.folio.innreach.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BibContributionRequest {

  /**
   * FOLIO Instance HRID
   */
  private String bibId;

  /**
   * Currently supports "ISO2709"
   */
  private String marc21BibFormat = "ISO2709";

  /**
   * Base 64 encoded MARC data
   * maximum 99999 bytes before base 64 encoding.
   */
  private String marc21BibData;

  /**
   * Number of title level holds - 0 (title level holds not currently supported in FOLIO)
   */
  private Integer titleHoldCount = 0;

  /**
   * Number of items linked to this bib.
   * (max 99, count of items associated with the FOLIO instance record that are contributed, initial value should be 0)
   */
  private Integer itemCount = 0;

  /**
   * ASCII y, n, or g; "g" means display the resource in the discovery layer as having been provided by the consortium, not by the individual site.
   * <p>
   * Default: n
   */
  private Character suppress;

}
