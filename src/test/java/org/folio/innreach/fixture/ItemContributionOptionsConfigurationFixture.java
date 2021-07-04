package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.ItemContributionOptionsConfiguration;

import java.util.UUID;

import static org.folio.innreach.fixture.InnReachLocationFixture.createInnReachLocation;
import static org.folio.innreach.fixture.TestUtil.randomInteger;

public class ItemContributionOptionsConfigurationFixture {
  public static final UUID CENTRAL_SERVER_UUID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
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
    itmContribOptConf.getLoanTypes().add(UUID.randomUUID());
    itmContribOptConf.getLoanTypes().add(UUID.randomUUID());
    itmContribOptConf.getLocations().add(createInnReachLocation());
    itmContribOptConf.getLocations().add(createInnReachLocation());
    itmContribOptConf.getMaterialTypes().add(UUID.randomUUID());
    itmContribOptConf.getMaterialTypes().add(UUID.randomUUID());
    itmContribOptConf.getStatuses().add(notAvailableItemStatuses[randomInteger(notAvailableItemStatuses.length)]);
    return itmContribOptConf;
  }
}
