package org.folio.innreach.domain.dto.folio.inventorystorage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePointsUsersDTO {
  private List<ServicePointUserDTO> servicePointsUsers;
}
