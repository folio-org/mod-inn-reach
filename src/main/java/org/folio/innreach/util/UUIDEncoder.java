package org.folio.innreach.util;

import java.nio.ByteBuffer;
import java.util.UUID;

import com.google.common.io.BaseEncoding;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Class to convert UUID to lowercase alphanumeric sequence in Base 32 encoding
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UUIDEncoder {

  private static final BaseEncoding ENCODER = BaseEncoding.base32().lowerCase().omitPadding();

  /**
   * Encodes UUID to lowercase alphanumeric character sequence
   *
   * @param uuid UUID to be encoded
   * @return encoded UUID
   */
  public static String encode(UUID uuid) {
    var byteBuffer = ByteBuffer.wrap(new byte[16])
      .putLong(uuid.getMostSignificantBits())
      .putLong(uuid.getLeastSignificantBits());

    return ENCODER.encode(byteBuffer.array());
  }

  /**
   * Decodes the given character sequence to UUID
   *
   * @param encodedUUID encoded UUID
   * @return decoded UUID
   */
  public static UUID decode(String encodedUUID) {
    var uuidBytes = ENCODER.decode(encodedUUID);
    var byteBuffer = ByteBuffer.wrap(uuidBytes);
    var firstLong = byteBuffer.getLong();
    var secondLong = byteBuffer.getLong();

    return new UUID(firstLong, secondLong);
  }

}
