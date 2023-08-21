package org.folio.innreach.domain.dto.folio;

import lombok.Builder;

import java.time.Instant;

@Builder
public record UserToken(String accessToken, Instant accessTokenExpiration) {
}
