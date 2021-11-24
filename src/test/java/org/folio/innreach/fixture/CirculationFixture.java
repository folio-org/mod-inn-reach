package org.folio.innreach.fixture;

import static org.jeasy.random.FieldPredicates.named;

import static org.folio.innreach.fixture.TestUtil.randomInteger;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.text.StringRandomizer;

import org.folio.innreach.dto.ItemShippedDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.TransferRequestDTO;

@UtilityClass
public class CirculationFixture {

  private static final EasyRandom transactionHoldRandom;
  private static final EasyRandom transferRequestRandom;
  private static final EasyRandom itemShippedRandom;

  static {

    transactionHoldRandom = new EasyRandom(
      new EasyRandomParameters()
          .randomize(named("transactionTime"), () -> randomInteger(255))
          .randomize(named("patronId"), TestUtil::randomAlphanumeric32Max)
          .randomize(named("patronAgencyCode"), TestUtil::randomAlphanumeric5)
          .randomize(named("itemAgencyCode"), TestUtil::randomAlphanumeric5)
          .randomize(named("itemId"), TestUtil::randomAlphanumeric32Max)
          .randomize(named("pickupLocation"), () -> "a:b:c:d")
          .randomize(named("needBefore"), () -> randomInteger(255))
          .randomize(named("centralItemType"), () -> randomInteger(255))
          .randomize(named("centralPatronType"), () -> randomInteger(255))
          .randomize(named("patronName"), new StringRandomizer(32))
          .randomize(named("title"), new StringRandomizer(255))
          .randomize(named("author"), new StringRandomizer(255))
          .randomize(named("callNumber"), () -> String.valueOf(randomInteger(32)))
          .randomize(named("newItemId"), new StringRandomizer(32))
          .randomize(named("itemBarcode"), new StringRandomizer(32))
          .randomize(named("reason"), new StringRandomizer(32))
          .randomize(named("reasonCode"), () -> 7) // always 7
          .randomize(named("dueDateTime"), () -> randomInteger(255))
          .excludeField(named("id"))
          .excludeField(named("metadata"))
    );

    transferRequestRandom = new EasyRandom(
        new EasyRandomParameters()
            .randomize(named("transactionTime"), () -> randomInteger(255))
            .randomize(named("patronId"), TestUtil::randomAlphanumeric32Max)
            .randomize(named("patronAgencyCode"), TestUtil::randomAlphanumeric5)
            .randomize(named("itemAgencyCode"), TestUtil::randomAlphanumeric5)
            .randomize(named("itemId"), TestUtil::randomAlphanumeric32Max)
            .randomize(named("newItemId"), TestUtil::randomAlphanumeric32Max)
    );

    itemShippedRandom = new EasyRandom(
      new EasyRandomParameters()
        .randomize(named("itemBarcode"), new StringRandomizer(32))
        .randomize(named("callNumber"), new StringRandomizer(32))
        .randomize(named("itemAgencyCode"), TestUtil::randomAlphanumeric5)
        .randomize(named("itemId"), TestUtil::randomAlphanumeric32Max)
        .randomize(named("patronId"), TestUtil::randomAlphanumeric32Max)
        .randomize(named("centralItemType"), () -> randomInteger(255))
        .randomize(named("patronAgencyCode"), TestUtil::randomAlphanumeric5)
    );
  }

  public static TransactionHoldDTO createTransactionHoldDTO() {
    return transactionHoldRandom.nextObject(TransactionHoldDTO.class);
  }

  public static TransferRequestDTO createTransferRequestDTO() {
    return transferRequestRandom.nextObject(TransferRequestDTO.class);
  }

  public static ItemShippedDTO createItemShippedDTO() {
    return itemShippedRandom.nextObject(ItemShippedDTO.class);
  }

}
