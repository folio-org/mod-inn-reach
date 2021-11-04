package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.AgencyLocationMappingDTO;

public interface AgencyMappingService {

  AgencyLocationMappingDTO getMapping(UUID centralServerId);

  AgencyLocationMappingDTO updateMapping(UUID centralServerId, AgencyLocationMappingDTO mapping);

  /**
   * <ul>
   *   <li>If a mapping is defined at the agency level, then it is possible to retrieve the corresponding FOLIO locationId using the agency code.</li>
   *   <li>If no mapping is defined at the agency level for a particular agency code, then the default location for the agency's parent local server is used.</li>
   *   <li>If no mapping is provided at the agency level and no default location is set for the local server for that agency, then the default location for the central server configuration is used.</li>
   * </ul>
   *
   * @param centralServerId central server id
   * @param agencyCode      agency code
   * @return FOLIO location ID mapped for the given agency code
   */
  UUID getLocationIdByAgencyCode(UUID centralServerId, String agencyCode);

}
