package org.folio.innreach.fixture;

import java.util.UUID;

import org.folio.innreach.domain.entity.InnReachRecallUser;

public class InnReachRecallUserFixture {

  public static InnReachRecallUser createInnReachRecallUser() {
    var innReachRecallUser = new InnReachRecallUser();
    innReachRecallUser.setUserId(UUID.randomUUID());
    return innReachRecallUser;
  }

}
