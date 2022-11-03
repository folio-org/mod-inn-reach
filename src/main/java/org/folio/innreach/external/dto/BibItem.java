package org.folio.innreach.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BibItem {
  /**
   * Lowercase alphanumeric string, maximum 32 characters
   */
  private String itemId;

  /**
   * 5 character lowercase alphanumeric
   */
  private String agencyCode;

  /**
   * Valid values provided by central (central-specific)
   */
  private Integer centralItemType;

  /**
   * Lowercase alphanumeric, maximum 5 characters.
   * <p>
   * Validated against keys sent via Post Locations endpoints
   */
  private String locationKey;

  /**
   * Circulation status
   */
  private String itemCircStatus;

  /**
   * (required if non-zero)
   */
  private Integer copyNumber;

  /**
   * Volume is intended for monographs when a multipart monograph (optional)
   */
  private String volumeDesignation;

  /**
   * Item effective call number, max 128 characters (not required)
   */
  private String callNumber;

  /**
   * If there is an online access URL on the item record that
   * is different from the URL of the associated MARC bib record, include it here
   * <p>
   * DO NOT include if there is a marc856URI in the contributed bib
   * record that applies to this Item. That is, include only if there is an
   * 856 URI that applies specifically to this item
   */
  private String marc856URI;

  /**
   * Maximum 64 characters, include if sending marc856URI
   */
  private String marc856PublicNote;

  /**
   * ASCII 'y' or 'n' based on statistical code or item suppress from discovery flag (default: 'n')
   */
  private Character suppress;

  /**
   * Item hold count
   */
  private Long holdCount;

  /**
   * Epoch UNIX time stamp
   */
  private Integer dueDateTime;

}
