package org.folio.innreach.fixture;

import static org.jeasy.random.FieldPredicates.named;

import static org.folio.innreach.fixture.TestUtil.randomInteger;

import java.time.OffsetDateTime;
import java.util.Locale;

import org.apache.commons.lang3.RandomStringUtils;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.entity.TransactionHold;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.entity.base.AuditableUser;

public class InnReachTransactionFixture {

  private static final EasyRandom transactionRandom;
  private static final EasyRandom transactionHoldRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("patronAgencyCode"), TestUtil::randomFiveCharacterCode)
      .randomize(named("itemAgencyCode"), TestUtil::randomFiveCharacterCode)
      .randomize(named("itemId"), InnReachTransactionFixture::randomId)
      .randomize(named("patronId"), InnReachTransactionFixture::randomId)
      .randomize(named("centralItemType"), () -> randomInteger(255))
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("id"))
      .excludeField(named("contribution"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"))
      .excludeField(named("metadata"));

    transactionHoldRandom = new EasyRandom(params);
  }

  private static String randomId() {
    return RandomStringUtils.random(randomInteger(1, 32), true, true).toLowerCase(Locale.ROOT);
  }

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("type"), () -> TransactionType.values()[randomInteger(3)])
      .randomize(named("hold"), () -> null)
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("id"))
      .excludeField(named("contribution"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"))
      .excludeField(named("metadata"));

    transactionRandom = new EasyRandom(params);
  }

  public static InnReachTransaction createInnReachTransaction() {
    var transaction = transactionRandom.nextObject(InnReachTransaction.class);
    transaction.setHold(createTransactionHold(transaction.getType()));
    return transaction;
  }

  public static InnReachTransaction createInnReachTransaction(TransactionType type) {
    var transaction = transactionRandom.nextObject(InnReachTransaction.class);
    transaction.setType(type);
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
        ((TransactionItemHold)hold).setCentralPatronType(randomInteger(255));
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
