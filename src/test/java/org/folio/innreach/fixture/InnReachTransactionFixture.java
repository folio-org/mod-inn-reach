package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.entity.TransactionHold;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.entity.base.AuditableUser;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import java.time.OffsetDateTime;
import java.util.UUID;

import static java.util.UUID.fromString;
import static org.folio.innreach.fixture.TestUtil.randomInteger;
import static org.jeasy.random.FieldPredicates.named;

public class InnReachTransactionFixture {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_UUID = fromString("cfae4887-f8fb-4e4c-a5cc-34f74e353cf8");
  private static final EasyRandom transactionRandom;
  private static final EasyRandom transactionHoldRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("patronAgencyCode"), TestUtil::randomFiveCharacterCode)
      .randomize(named("itemAgencyCode"), TestUtil::randomFiveCharacterCode)
      .randomize(named("centralServer"), InnReachTransactionFixture::refCentralServer)
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("id"))
      .excludeField(named("contribution"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"))
      .excludeField(named("metadata"));

    transactionHoldRandom = new EasyRandom(params);
  }

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("type"), () -> TransactionType.values()[randomInteger(3)])
      .randomize(named("hold"), () -> null)
      .randomize(named("centralServer"), InnReachTransactionFixture::refCentralServer)
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("id"))
      .excludeField(named("contribution"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"))
      .excludeField(named("metadata"));

    transactionRandom = new EasyRandom(params);
  }

  public static CentralServer refCentralServer() {
    return TestUtil.refCentralServer(PRE_POPULATED_CENTRAL_SERVER_UUID);
  }

  public static InnReachTransaction createInnReachTransaction() {
    var transaction = transactionRandom.nextObject(InnReachTransaction.class);
    transaction.setHold(createTransactionHold(transaction.getType()));
    return transaction;
  }

  private static TransactionHold createTransactionHold(TransactionType type) {
    TransactionHold hold;
    switch (type) {
      case PATRON:
        hold = transactionHoldRandom.nextObject(TransactionPatronHold.class);
        break;
      case ITEM:
        hold = transactionHoldRandom.nextObject(TransactionItemHold.class);
        break;
      case LOCAL:
        hold = transactionHoldRandom.nextObject(TransactionLocalHold.class);
        break;
      default:
        hold = null;
    }
    return hold;
  }
}
