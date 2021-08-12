package org.folio.innreach.fixture;

import lombok.experimental.UtilityClass;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.Contribution;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import java.time.OffsetDateTime;
import java.util.UUID;

import static java.util.UUID.fromString;
import static org.jeasy.random.FieldPredicates.named;

@UtilityClass
public class ContributionFixture {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_UUID = fromString("cfae4887-f8fb-4e4c-a5cc-34f74e353cf8");

  private static final EasyRandom contributionRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .overrideDefaultInitialization(true)
      .randomize(named("centralServer"), ContributionFixture::refCentralServer)
      .randomize(named("createdBy"), () -> "admin")
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("id"))
      .excludeField(named("contribution"))
      .excludeField(named("lastModifiedBy"))
      .excludeField(named("lastModifiedDate"))
      .excludeField(named("metadata"));

    contributionRandom = new EasyRandom(params);
  }

  public static Contribution createContribution() {
    var contribution = contributionRandom.nextObject(Contribution.class);

    contribution.getErrors().forEach(e -> e.setContribution(contribution));

    return contribution;
  }

  public static CentralServer refCentralServer() {
    return TestUtil.refCentralServer(PRE_POPULATED_CENTRAL_SERVER_UUID);
  }

}
