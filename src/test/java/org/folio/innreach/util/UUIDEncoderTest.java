package org.folio.innreach.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class UUIDEncoderTest {

  @Test
  void shouldEncodeAndDecodeUUID() {
    var uuid = UUID.randomUUID();

    var encoded = UUIDEncoder.encode(uuid);

    assertNotNull(encoded);
    assertEquals(26, encoded.length());

    var decoded = UUIDEncoder.decode(encoded);

    assertEquals(uuid, decoded);
  }

}
