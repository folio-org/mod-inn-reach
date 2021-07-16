package org.folio.innreach.fixture;

import static org.folio.innreach.fixture.TestUtil.randomInteger;

import java.util.UUID;

import org.folio.innreach.domain.entity.ItemContributionOptionsConfiguration;

public class ItemContributionOptionsConfigurationFixture {
  private static final String[] notAvailableItemStatuses = {"Aged to lost",
    "Claimed returned",
    "In process",
    "In process (non-requestable)",
    "Intellectual item",
    "Long missing",
    "Lost and paid",
    "Missing",
    "On order",
    "Order closed",
    "Restricted",
    "Unavailable",
    "Unknown"};

  public static ItemContributionOptionsConfiguration createItmContribOptConf() {
    var itmContribOptConf = new ItemContributionOptionsConfiguration();
    itmContribOptConf.getNonLendableLoanTypes().add(UUID.randomUUID());
    itmContribOptConf.getNonLendableLoanTypes().add(UUID.randomUUID());
    itmContribOptConf.getNonLendableLocations().add(UUID.randomUUID());
    itmContribOptConf.getNonLendableLocations().add(UUID.randomUUID());
    itmContribOptConf.getNonLendableMaterialTypes().add(UUID.randomUUID());
    itmContribOptConf.getNonLendableMaterialTypes().add(UUID.randomUUID());
    itmContribOptConf.getNotAvailableItemStatuses().add(notAvailableItemStatuses[randomInteger(notAvailableItemStatuses.length)]);
    return itmContribOptConf;
  }
}
