package org.folio.innreach.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "inn-reach.client")
public record InnReachHttpClientProperties (

  /*
    * The connection time-out value in milliseconds for the HTTP client to establish a connection with the InnReach server.
   */
  @DefaultValue("180000") // 180000 milliseconds or 3 seconds time-out for connection to be established
  int connectTimeoutMs,

  /*
   * The read time-out value in milliseconds for the HTTP client to wait for data after a connection is established with the InnReach server.
   */
  @DefaultValue("120000") // 120000 milliseconds or 2 seconds time-out for waiting for data after connection is established
  int readTimeoutMs
) {
}
