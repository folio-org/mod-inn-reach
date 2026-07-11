package org.folio.innreach.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class TestJdbcHelper {

  private static final String DB_SCHEMA_PREFIX = "_mod_inn_reach";

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public long count(String tenantId, String tableName) {
    var sql = "SELECT COUNT(*) FROM %s%s.%s".formatted(tenantId, DB_SCHEMA_PREFIX, tableName);
    var count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
    return count != null ? count : 0;
  }

  public long countByStatus(String tenantId, String tableName, String status) {
    var sql = "SELECT COUNT(*) FROM %s%s.%s WHERE status = :status".formatted(tenantId, DB_SCHEMA_PREFIX, tableName);
    var count = jdbcTemplate.queryForObject(sql, Map.of("status", status), Long.class);
    return count != null ? count : 0;
  }

  public void executeSqlScript(String tenantId, String resourcePath) {
    var schema = tenantId + DB_SCHEMA_PREFIX + ".";
    var sql = readResource(resourcePath);
    sql = sql.replaceAll("(?i)((?:INTO|FROM|UPDATE|TABLE)\\s+)(\\w+)(?!\\.)",
      "$1" + schema + "$2");
    var statements = sql.split(";");
    for (var stmt : statements) {
      var trimmed = stmt.trim();
      if (!trimmed.isEmpty()) {
        jdbcTemplate.update(trimmed, new MapSqlParameterSource());
      }
    }
  }

  private static String readResource(String path) {
    try {
      var resource = new ClassPathResource(path);
      try (InputStream is = resource.getInputStream();
           BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        return reader.lines().collect(Collectors.joining("\n"));
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to read resource: " + path, e);
    }
  }
}
